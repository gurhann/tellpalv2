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
import {
  categoryResponses,
  featuredSleepCategoryResponse,
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
  it("creates a category, updates metadata, and creates then updates a session-backed localization", async () => {
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

    await renderFreshApp({
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
      within(createDialog).getByRole("button", { name: /^create category$/i }),
    );

    expect(
      await screen.findByRole("heading", { name: /dream-lullabies/i }),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe("/categories/99");

    fireEvent.change(screen.getByLabelText(/slug/i), {
      target: { value: "dream-lullabies-v2" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/slug/i)).toHaveValue("dream-lullabies-v2");
    });

    fireEvent.click(
      screen.getByRole("button", { name: /create first localization/i }),
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
      await within(localizationDialog).findByRole("button", {
        name: /asset #4/i,
      }),
    );
    fireEvent.click(
      within(localizationDialog).getByRole("button", {
        name: /create localization/i,
      }),
    );

    const turkishTab = await screen.findByRole("tab", { name: /turkish/i });
    expect(turkishTab).toBeVisible();
    expect(await screen.findByDisplayValue("Dream Lullabies")).toBeVisible();

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Dream Lullabies Updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save localization/i }));

    await waitFor(() => {
      expect(screen.getByDisplayValue("Dream Lullabies Updated")).toBeVisible();
    });
  });

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
});
