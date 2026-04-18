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

import type { AdminStoryPageReadResponse } from "@/features/contents/api/story-page-admin";
import { storyContentReadResponse } from "@/features/contents/test/fixtures";
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
    accessTokenExpiresAt: "2026-03-29T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-04-28T10:00:00Z",
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

describe("Story pages integration", () => {
  it("adds and removes story pages through the live route shell", async () => {
    const session = makeSession();
    const detailRecord = cloneJson(storyContentReadResponse);
    let storyPages: AdminStoryPageReadResponse[] = [
      {
        contentId: 1,
        pageNumber: 1,
        localizationCount: 2,
        localizations: [
          {
            contentId: 1,
            pageNumber: 1,
            languageCode: "en",
            bodyText: "Look at the moon over the garden gate.",
            audioMediaId: 81,
            illustrationMediaId: 41,
          },
          {
            contentId: 1,
            pageNumber: 1,
            languageCode: "tr",
            bodyText: "Bahce kapisinin ustundeki aya bak.",
            audioMediaId: 82,
            illustrationMediaId: 42,
          },
        ],
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

        if (url.pathname === "/api/admin/contents/1" && method === "GET") {
          return jsonResponse({
            ...detailRecord,
            pageCount: storyPages.length,
          });
        }

        if (url.pathname === "/api/admin/contents/1/story-pages") {
          if (method === "GET") {
            return jsonResponse(storyPages);
          }

          if (method === "POST") {
            const body = (await readRequestJson(init)) as {
              pageNumber: number;
            };
            const createdPage: AdminStoryPageReadResponse = {
              contentId: 1,
              pageNumber: body.pageNumber,
              localizationCount: 0,
              localizations: [],
            };
            storyPages = [...storyPages, createdPage];

            return jsonResponse({
              contentId: 1,
              pageNumber: body.pageNumber,
              localizationCount: 0,
            });
          }
        }

        if (url.pathname === "/api/admin/contents/1/story-pages/2") {
          if (method === "DELETE") {
            storyPages = storyPages.filter(
              (storyPage) => storyPage.pageNumber !== 2,
            );
            return new Response(null, { status: 204 });
          }

          if (method === "GET") {
            return jsonResponse(
              storyPages.find((storyPage) => storyPage.pageNumber === 2),
            );
          }
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/1/story-pages",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", {
      name: /story pages for evening garden/i,
    });

    fireEvent.click(screen.getByRole("button", { name: /^add story page$/i }));

    const createDialog = await screen.findByRole("dialog");
    fireEvent.change(within(createDialog).getByLabelText(/page number/i), {
      target: { value: "2" },
    });
    fireEvent.click(
      within(createDialog).getByRole("button", { name: /^add story page$/i }),
    );

    await waitFor(() => {
      expect(screen.getByText("Page 2")).toBeVisible();
    });

    fireEvent.click(
      screen.getByRole("button", { name: /^delete page 2$/i }),
    );

    const deleteDialog = await screen.findByRole("dialog");
    fireEvent.click(
      within(deleteDialog).getByRole("button", { name: /delete page/i }),
    );

    await waitFor(() => {
      expect(screen.queryByText("Page 2")).not.toBeInTheDocument();
    });
  }, 15_000);

  it("creates a missing localized page payload from the route editor", async () => {
    const session = makeSession();
    const detailRecord = cloneJson(storyContentReadResponse);
    const storyPageRecord: AdminStoryPageReadResponse = {
      contentId: 1,
      pageNumber: 2,
      localizationCount: 1,
      localizations: [
        {
          contentId: 1,
          pageNumber: 2,
          languageCode: "en",
          bodyText: "The fox curls into a soft bed of leaves.",
          audioMediaId: 83,
          illustrationMediaId: 43,
        },
      ],
    };
    let lastLocalizationPayload: Record<string, unknown> | null = null;
    const assets = [
      {
        assetId: 43,
        provider: "LOCAL_STUB",
        objectPath: "/content/story/evening-garden/en/page-2.jpg",
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
        assetId: 52,
        provider: "LOCAL_STUB",
        objectPath: "/content/story/evening-garden/tr/page-2.jpg",
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
        assetId: 83,
        provider: "LOCAL_STUB",
        objectPath: "/content/story/evening-garden/en/page-2.mp3",
        mediaType: "AUDIO",
        kind: "ORIGINAL_AUDIO",
        mimeType: "audio/mpeg",
        byteSize: null,
        checksumSha256: null,
        cachedDownloadUrl: null,
        downloadUrlCachedAt: null,
        downloadUrlExpiresAt: null,
        createdAt: "2026-03-31T12:00:00Z",
        updatedAt: "2026-03-31T12:00:00Z",
      },
      {
        assetId: 84,
        provider: "LOCAL_STUB",
        objectPath: "/content/story/evening-garden/tr/page-2.mp3",
        mediaType: "AUDIO",
        kind: "ORIGINAL_AUDIO",
        mimeType: "audio/mpeg",
        byteSize: null,
        checksumSha256: null,
        cachedDownloadUrl: null,
        downloadUrlCachedAt: null,
        downloadUrlExpiresAt: null,
        createdAt: "2026-03-31T12:00:00Z",
        updatedAt: "2026-03-31T12:00:00Z",
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

        if (url.pathname === "/api/admin/contents/1" && method === "GET") {
          return jsonResponse({
            ...detailRecord,
            pageCount: 1,
          });
        }

        if (
          url.pathname === "/api/admin/contents/1/story-pages" &&
          method === "GET"
        ) {
          return jsonResponse([storyPageRecord]);
        }

        if (
          url.pathname === "/api/admin/contents/1/story-pages/2" &&
          method === "GET"
        ) {
          return jsonResponse(storyPageRecord);
        }

        if (
          url.pathname ===
            "/api/admin/contents/1/story-pages/2/localizations/tr" &&
          method === "PUT"
        ) {
          lastLocalizationPayload = await readRequestJson(init);
          const localization = {
            contentId: 1,
            pageNumber: 2,
            languageCode: "tr",
            bodyText: lastLocalizationPayload?.bodyText as string,
            audioMediaId: lastLocalizationPayload?.audioMediaId as number,
            illustrationMediaId:
              lastLocalizationPayload?.illustrationMediaId as number,
          };
          storyPageRecord.localizations = [
            ...storyPageRecord.localizations,
            localization,
          ];
          storyPageRecord.localizationCount =
            storyPageRecord.localizations.length;

          return jsonResponse(localization);
        }

        if (url.pathname === "/api/admin/media" && method === "GET") {
          return jsonResponse(assets);
        }

        if (url.pathname.startsWith("/api/admin/media/") && method === "GET") {
          const assetId = Number(url.pathname.split("/").pop());
          return jsonResponse(
            assets.find((asset) => asset.assetId === assetId) ?? null,
            assets.some((asset) => asset.assetId === assetId) ? 200 : 404,
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/1/story-pages",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", {
      name: /story pages for evening garden/i,
    });
    await screen.findByText("Page 2");

    fireEvent.click(screen.getByRole("button", { name: /^edit page 2$/i }));

    const editorDialog = await screen.findByRole("dialog");
    const turkishTab = await within(editorDialog).findByRole("tab", {
      name: /turkish/i,
    });
    const turkishPanelId = turkishTab.getAttribute("aria-controls");

    expect(turkishPanelId).toBeTruthy();

    await act(async () => {
      fireEvent.mouseDown(turkishTab);
    });

    await waitFor(() => {
      expect(turkishTab).toHaveAttribute("aria-selected", "true");
    });

    const turkishPanel = editorDialog.querySelector(
      `#${turkishPanelId}`,
    ) as HTMLElement | null;

    expect(turkishPanel).not.toBeNull();
    const turkishForm = turkishPanel?.querySelector("form") ?? null;

    expect(turkishForm).not.toBeNull();

    fireEvent.change(within(turkishForm!).getByLabelText(/body text/i), {
      target: { value: "Tilki gece bahcesindeki taslara yavasca yaklasir." },
    });
    fireEvent.click(
      within(turkishForm!).getAllByRole("button", { name: /advanced/i })[0]!,
    );
    fireEvent.click(
      within(turkishForm!).getAllByRole("button", { name: /advanced/i })[1]!,
    );
    fireEvent.change(
      within(turkishForm!).getByLabelText(/illustration asset id/i),
      {
        target: { value: "52" },
      },
    );
    fireEvent.change(within(turkishForm!).getByLabelText(/audio asset id/i), {
      target: { value: "84" },
    });
    fireEvent.click(
      within(turkishForm!).getByRole("button", {
        name: /create page localization/i,
      }),
    );

    await waitFor(() => {
      expect(lastLocalizationPayload).toEqual({
        bodyText: "Tilki gece bahcesindeki taslara yavasca yaklasir.",
        audioMediaId: 84,
        illustrationMediaId: 52,
      });
    });

    expect(
      await within(turkishForm!).findByDisplayValue(
        "Tilki gece bahcesindeki taslara yavasca yaklasir.",
      ),
    ).toBeVisible();
    expect(
      within(editorDialog).getByText(/illustration linked/i),
    ).toBeVisible();
  });
});
