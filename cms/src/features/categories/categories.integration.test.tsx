import { QueryClient } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type {
  AdminCategoryLocalizationResponse,
  AdminCategoryResponse,
  UpsertCategoryLocalizationInput,
} from "@/features/categories/api/category-admin";
import type { AdminCategoryContentResponse } from "@/features/categories/api/category-curation-admin";
import {
  categoryResponses,
  featuredSleepEnglishLocalizationResponse,
  featuredSleepCategoryResponse,
  featuredSleepTurkishLocalizationResponse,
} from "@/features/categories/test/fixtures";
import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import type { AdminSessionPayload } from "@/types/api";

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function makeSession(
  overrides: Partial<AdminSessionPayload> = {},
): AdminSessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-04-01T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-05-01T10:00:00Z",
    ...overrides,
  };
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

function problemResponse(
  status: number,
  title: string,
  detail: string,
  errorCode: string,
  path: string,
  extras: Record<string, unknown> = {},
) {
  return new Response(
    JSON.stringify({
      type: "about:blank",
      title,
      status,
      detail,
      errorCode,
      path,
      ...extras,
    }),
    {
      status,
      statusText: title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    },
  );
}

async function readRequestJson(init?: RequestInit) {
  if (!init?.body || typeof init.body !== "string") {
    return null;
  }

  return JSON.parse(init.body) as Record<string, unknown>;
}

async function renderFreshApp(options: {
  initialPath: string;
  fetchImplementation: typeof fetch;
}) {
  vi.resetModules();
  window.history.replaceState({}, "", options.initialPath);
  vi.stubGlobal("fetch", options.fetchImplementation);

  const { AppProviders } = await import("@/app/providers");
  const { default: App } = await import("@/App");
  const { authSessionStore } =
    await import("@/features/auth/model/session-store");
  const { queryClient } = await import("@/lib/query-client");

  authSessionStore.reset();
  queryClient.clear();

  let renderResult!: ReturnType<typeof render>;
  await act(async () => {
    renderResult = render(
      <AppProviders>
        <App />
      </AppProviders>,
    );
  });

  return {
    ...renderResult,
    authSessionStore,
    queryClient: queryClient as QueryClient,
  };
}

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(async () => {
  const modules = await Promise.allSettled([
    import("@/features/auth/model/session-store"),
    import("@/lib/query-client"),
  ]);

  for (const result of modules) {
    if (result.status === "fulfilled") {
      if ("authSessionStore" in result.value) {
        result.value.authSessionStore.reset();
      }

      if ("queryClient" in result.value) {
        result.value.queryClient.clear();
      }
    }
  }

  window.localStorage.clear();
  vi.unstubAllGlobals();
});

describe("Category integration", () => {
  it("creates a category, updates metadata, and keeps persisted localizations visible after refresh", async () => {
    const session = makeSession();
    const listRecords = cloneJson(categoryResponses);
    const imageAssets: AdminAssetResponse[] = [
      {
        assetId: 4,
        provider: "LOCAL_STUB",
        objectPath: "/content/images/dream-lullabies-cover.jpg",
        mediaType: "IMAGE",
        kind: "ORIGINAL_IMAGE",
        mimeType: "image/jpeg",
        byteSize: null,
        checksumSha256: null,
        cachedDownloadUrl: null,
        downloadUrlCachedAt: null,
        downloadUrlExpiresAt: null,
        createdAt: "2026-03-31T12:00:00Z",
        updatedAt: "2026-03-31T12:00:00Z",
      },
      {
        assetId: 5,
        provider: "LOCAL_STUB",
        objectPath: "/content/images/dream-lullabies-banner.jpg",
        mediaType: "IMAGE",
        kind: "ORIGINAL_IMAGE",
        mimeType: "image/jpeg",
        byteSize: null,
        checksumSha256: null,
        cachedDownloadUrl: null,
        downloadUrlCachedAt: null,
        downloadUrlExpiresAt: null,
        createdAt: "2026-03-31T12:00:00Z",
        updatedAt: "2026-03-31T12:00:00Z",
      },
    ];
    let createdCategory: AdminCategoryResponse | null = null;
    let savedLocalization: AdminCategoryLocalizationResponse | null = null;
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/categories" && method === "GET") {
          return jsonResponse(
            createdCategory ? [createdCategory, ...listRecords] : listRecords,
          );
        }

        if (url.pathname === "/api/admin/categories" && method === "POST") {
          const body = (await readRequestJson(init)) as {
            slug: string;
            type: AdminCategoryResponse["type"];
            premium: boolean;
            active: boolean;
          };

          createdCategory = {
            categoryId: 99,
            slug: body.slug,
            type: body.type,
            premium: body.premium,
            active: body.active,
          };

          return jsonResponse(createdCategory, 201);
        }

        if (url.pathname === "/api/admin/categories/99" && method === "GET") {
          return jsonResponse(createdCategory);
        }

        if (
          url.pathname === "/api/admin/categories/99/localizations" &&
          method === "GET"
        ) {
          return jsonResponse(savedLocalization ? [savedLocalization] : []);
        }

        if (url.pathname === "/api/admin/categories/99" && method === "PUT") {
          const body = (await readRequestJson(init)) as {
            slug: string;
            type: AdminCategoryResponse["type"];
            premium: boolean;
            active: boolean;
          };

          createdCategory = {
            categoryId: 99,
            slug: body.slug,
            type: body.type,
            premium: body.premium,
            active: body.active,
          };

          return jsonResponse(createdCategory);
        }

        if (
          url.pathname === "/api/admin/categories/99/localizations/tr" &&
          method === "POST"
        ) {
          const body = (await readRequestJson(
            init,
          )) as UpsertCategoryLocalizationInput;

          savedLocalization = {
            categoryId: 99,
            languageCode: "tr",
            name: body.name,
            description: body.description ?? null,
            imageMediaId: body.imageMediaId ?? null,
            status: body.status,
            publishedAt: body.publishedAt ?? null,
            published: body.status === "PUBLISHED",
          };

          return jsonResponse(savedLocalization);
        }

        if (
          url.pathname === "/api/admin/categories/99/localizations/tr" &&
          method === "PUT"
        ) {
          const body = (await readRequestJson(
            init,
          )) as UpsertCategoryLocalizationInput;

          savedLocalization = {
            categoryId: 99,
            languageCode: "tr",
            name: body.name,
            description: body.description ?? null,
            imageMediaId: body.imageMediaId ?? null,
            status: body.status,
            publishedAt: body.publishedAt ?? null,
            published: body.status === "PUBLISHED",
          };

          return jsonResponse(savedLocalization);
        }

        if (
          url.pathname ===
            "/api/admin/categories/99/localizations/tr/contents" &&
          method === "GET"
        ) {
          return jsonResponse([]);
        }

        if (url.pathname === "/api/admin/media" && method === "GET") {
          return jsonResponse(imageAssets);
        }

        if (url.pathname.startsWith("/api/admin/media/") && method === "GET") {
          const assetId = Number(url.pathname.split("/").pop());
          const asset = imageAssets.find((entry) => entry.assetId === assetId);

          if (!asset) {
            return problemResponse(
              404,
              "Asset not found",
              `Asset #${assetId} was not found.`,
              "asset_not_found",
              url.pathname,
            );
          }

          return jsonResponse(asset);
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    const initialApp = await renderFreshApp({
      initialPath: "/categories",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", {
      name: /^categories$/i,
      level: 1,
    });

    fireEvent.click(screen.getByRole("button", { name: /^create category$/i }));

    const createDialog = await screen.findByRole("dialog");
    fireEvent.change(within(createDialog).getByLabelText(/slug/i), {
      target: { value: "dream-lullabies" },
    });
    fireEvent.click(
      within(createDialog).getByRole("button", {
        name: /^create category$/i,
      }),
    );

    await waitFor(() => {
      expect(window.location.pathname).toBe("/categories/99");
    });
    expect(await screen.findByDisplayValue("dream-lullabies")).toBeVisible();
    expect(
      screen.queryByText(/localization snapshot/i),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /open curation lane/i }),
    ).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/slug/i), {
      target: { value: "dream-lullabies-v2" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/slug/i)).toHaveValue("dream-lullabies-v2");
    });

    fireEvent.click(
      screen.getAllByRole("button", { name: /create first localization/i })[0]!,
    );

    const localizationDialog = await screen.findByRole("dialog");
    fireEvent.change(within(localizationDialog).getByLabelText(/^name$/i), {
      target: { value: "Dream Lullabies" },
    });
    fireEvent.change(
      within(localizationDialog).getByLabelText(/description/i),
      {
        target: { value: "Soft lullaby picks for bedtime." },
      },
    );
    fireEvent.click(
      within(localizationDialog).getByRole("button", { name: /advanced/i }),
    );
    fireEvent.change(
      within(localizationDialog).getByLabelText(/image asset/i),
      {
        target: { value: "4" },
      },
    );
    fireEvent.click(
      within(localizationDialog).getByRole("button", {
        name: /create localization/i,
      }),
    );

    const turkishTabs = await screen.findAllByRole("tab", {
      name: /turkish/i,
    });
    expect(turkishTabs.length).toBeGreaterThan(0);
    expect(await screen.findByDisplayValue("Dream Lullabies")).toBeVisible();

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Dream Lullabies Updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save localization/i }));

    await waitFor(() => {
      expect(screen.getByDisplayValue("Dream Lullabies Updated")).toBeVisible();
    });

    await act(async () => {
      initialApp.unmount();
    });
    const refreshedApp = await renderFreshApp({
      initialPath: "/categories/99",
      fetchImplementation: fetchMock as typeof fetch,
    });

    const localizationTabList = await refreshedApp.findByRole("tablist", {
      name: /category localization tabs/i,
    });
    expect(
      within(localizationTabList).getByRole("tab", { name: /turkish/i }),
    ).toBeInTheDocument();
    expect(
      refreshedApp.getByDisplayValue("Dream Lullabies Updated"),
    ).toBeVisible();
  }, 15_000);

  it("maps duplicate slug validation onto the live create dialog", async () => {
    const session = makeSession();
    const listRecords = cloneJson([featuredSleepCategoryResponse]);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/categories" && method === "GET") {
          return jsonResponse(listRecords);
        }

        if (url.pathname === "/api/admin/categories" && method === "POST") {
          return problemResponse(
            409,
            "Duplicate category slug",
            "Category slug already exists.",
            "duplicate_category_slug",
            "/api/admin/categories",
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/categories",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", {
      name: /^categories$/i,
      level: 1,
    });

    fireEvent.click(screen.getByRole("button", { name: /^create category$/i }));

    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/slug/i), {
      target: { value: "featured-sleep" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: /^create category$/i }),
    );

    expect(
      await within(dialog).findByText("Slug is already in use."),
    ).toBeVisible();
  });

  it("hydrates curation rows, supports add reorder remove, and keeps localization tabs after refresh", async () => {
    const session = makeSession();
    const localizations = [
      featuredSleepEnglishLocalizationResponse,
      featuredSleepTurkishLocalizationResponse,
    ];
    let curationRows: AdminCategoryContentResponse[] = [
      {
        categoryId: 7,
        languageCode: "en",
        contentId: 1,
        displayOrder: 0,
        externalKey: "story.evening-garden",
        localizedTitle: "Evening Garden",
      },
    ];
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/categories/7" && method === "GET") {
          return jsonResponse(featuredSleepCategoryResponse);
        }

        if (
          url.pathname === "/api/admin/categories/7/localizations" &&
          method === "GET"
        ) {
          return jsonResponse(localizations);
        }

        if (
          url.pathname ===
            "/api/admin/categories/7/localizations/en/contents" &&
          method === "GET"
        ) {
          return jsonResponse(curationRows);
        }

        if (
          url.pathname ===
            "/api/admin/categories/7/localizations/en/eligible-contents" &&
          method === "GET"
        ) {
          return jsonResponse([
            {
              contentId: 11,
              externalKey: "story.starry-forest",
              localizedTitle: "Starry Forest",
              languageCode: "en",
              publishedAt: "2026-03-30T09:00:00Z",
            },
          ]);
        }

        if (
          url.pathname ===
            "/api/admin/categories/7/localizations/en/contents" &&
          method === "POST"
        ) {
          const body = (await readRequestJson(init)) as {
            contentId: number;
            displayOrder: number;
          };

          const nextRow = {
            categoryId: 7,
            languageCode: "en",
            contentId: body.contentId,
            displayOrder: body.displayOrder,
            externalKey: "story.starry-forest",
            localizedTitle: "Starry Forest",
          } satisfies AdminCategoryContentResponse;

          curationRows = [...curationRows, nextRow].sort(
            (left, right) => left.displayOrder - right.displayOrder,
          );

          return jsonResponse(nextRow, 201);
        }

        if (
          url.pathname ===
            "/api/admin/categories/7/localizations/en/contents/reorder" &&
          method === "PUT"
        ) {
          const body = (await readRequestJson(init)) as {
            items: Array<{
              contentId: number;
              displayOrder: number;
            }>;
          };

          const rowByContentId = new Map(
            curationRows.map((row) => [row.contentId, row]),
          );
          curationRows = body.items
            .map((item) => {
              const currentRow = rowByContentId.get(item.contentId);

              if (!currentRow) {
                throw new Error(
                  `Unexpected reorder item for content ${item.contentId}`,
                );
              }

              return {
                ...currentRow,
                displayOrder: item.displayOrder,
              };
            })
            .sort((left, right) => left.displayOrder - right.displayOrder);

          return jsonResponse(curationRows);
        }

        if (
          url.pathname ===
            "/api/admin/categories/7/localizations/en/contents/1" &&
          method === "DELETE"
        ) {
          curationRows = curationRows.filter((row) => row.contentId !== 1);
          return new Response(null, { status: 204 });
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    const initialApp = await renderFreshApp({
      initialPath: "/categories/7",
      fetchImplementation: fetchMock as typeof fetch,
    });

    const localizationTabList = await screen.findByRole("tablist", {
      name: /category localization tabs/i,
    });
    expect(
      within(localizationTabList).getByRole("tab", { name: /english/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeEnabled();
    await waitFor(() => {
      expect(
        screen.queryByRole("heading", {
          name: /loading curated content/i,
        }),
      ).not.toBeInTheDocument();
    });
    const curatedList = screen.getByRole("list", {
      name: /english curated content list/i,
    });
    expect(within(curatedList).getByText(/^Evening Garden$/i)).toBeVisible();

    fireEvent.click(
      screen.getByRole("button", { name: /add curated content/i }),
    );

    const addDialog = await screen.findByRole("dialog");
    fireEvent.click(
      await within(addDialog).findByRole("button", {
        name: /starry forest/i,
      }),
    );
    fireEvent.click(
      within(addDialog).getByRole("button", { name: /add curated content/i }),
    );

    await waitFor(() => {
      expect(within(curatedList).getByText(/^Starry Forest$/i)).toBeVisible();
    });
    await waitFor(() => {
      expect(
        screen.getByText(/drag rows to reorder english curated story items/i),
      ).toBeVisible();
    });

    const moveUpButton = screen.getByRole("button", {
      name: /move starry forest up/i,
    });
    await waitFor(() => {
      expect(moveUpButton).toBeEnabled();
    });
    await act(async () => {
      fireEvent.click(moveUpButton);
    });

    await waitFor(() => {
      expect(curationRows.map((row) => row.contentId)).toEqual([11, 1]);
      expect(curationRows.map((row) => row.displayOrder)).toEqual([0, 1]);
    });

    await act(async () => {
      initialApp.unmount();
    });

    const refreshedApp = await renderFreshApp({
      initialPath: "/categories/7",
      fetchImplementation: fetchMock as typeof fetch,
    });

    const refreshedLocalizationTabList = await refreshedApp.findByRole(
      "tablist",
      {
        name: /category localization tabs/i,
      },
    );
    expect(
      within(refreshedLocalizationTabList).getByRole("tab", {
        name: /english/i,
      }),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(
        refreshedApp.queryByRole("heading", {
          name: /loading curated content/i,
        }),
      ).not.toBeInTheDocument();
    });
    const refreshedCuratedList = refreshedApp.getByRole("list", {
      name: /english curated content list/i,
    });
    expect(
      within(refreshedCuratedList).getByText(/^Evening Garden$/i),
    ).toBeVisible();
    expect(
      within(refreshedCuratedList).getByText(/^Starry Forest$/i),
    ).toBeVisible();
    const refreshedRows = within(refreshedCuratedList).getAllByRole("listitem");
    expect(
      within(refreshedRows[0]!).getByText(/^Starry Forest$/i),
    ).toBeVisible();
    expect(
      within(refreshedRows[1]!).getByText(/^Evening Garden$/i),
    ).toBeVisible();

    const eveningGardenRow = within(refreshedCuratedList)
      .getAllByRole("listitem")
      .find((row) => within(row).queryByText(/^Evening Garden$/i));
    expect(eveningGardenRow).toBeDefined();
    fireEvent.click(
      within(eveningGardenRow!).getByRole("button", { name: /^remove$/i }),
    );
    expect(
      await refreshedApp.findByRole("heading", {
        name: /remove curated content/i,
      }),
    ).toBeVisible();
    fireEvent.click(
      refreshedApp.getByRole("button", { name: /^remove content$/i }),
    );

    await waitFor(() => {
      expect(
        within(refreshedCuratedList).queryByText(/^Evening Garden$/i),
      ).not.toBeInTheDocument();
    });

    await act(async () => {
      refreshedApp.unmount();
    });

    const afterRemovalApp = await renderFreshApp({
      initialPath: "/categories/7",
      fetchImplementation: fetchMock as typeof fetch,
    });

    const afterRemovalLocalizationTabList = await afterRemovalApp.findByRole(
      "tablist",
      {
        name: /category localization tabs/i,
      },
    );
    expect(
      within(afterRemovalLocalizationTabList).getByRole("tab", {
        name: /english/i,
      }),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(
        afterRemovalApp.queryByRole("heading", {
          name: /loading curated content/i,
        }),
      ).not.toBeInTheDocument();
    });
    const afterRemovalList = afterRemovalApp.getByRole("list", {
      name: /english curated content list/i,
    });
    expect(
      within(afterRemovalList).queryByText(/^Evening Garden$/i),
    ).not.toBeInTheDocument();
    expect(
      within(afterRemovalList).getByText(/^Starry Forest$/i),
    ).toBeVisible();
  }, 15_000);
});
