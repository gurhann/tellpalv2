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

import {
  assetResponses,
  originalAudioAssetResponse,
} from "@/features/assets/test/fixtures";
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
    accessTokenExpiresAt: "2026-04-04T18:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-05-04T18:00:00Z",
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

describe("Asset integration", () => {
  it("hydrates the live asset library, saves metadata, and refreshes cached download URLs", async () => {
    const session = makeSession();
    const assets = cloneJson(assetResponses);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/media" && method === "GET") {
          return jsonResponse(assets);
        }

        if (url.pathname === "/api/admin/media/1" && method === "GET") {
          return jsonResponse(
            assets.find((asset) => asset.assetId === 1) ?? null,
          );
        }

        if (
          url.pathname === "/api/admin/media/1/metadata" &&
          method === "PUT"
        ) {
          const body = (await readRequestJson(init)) as {
            mimeType?: string | null;
            byteSize?: number | null;
            checksumSha256?: string | null;
          };
          const nextAsset = assets.find((asset) => asset.assetId === 1);

          if (!nextAsset) {
            throw new Error("Audio asset #1 was not found.");
          }

          nextAsset.mimeType = body.mimeType ?? null;
          nextAsset.byteSize = body.byteSize ?? null;
          nextAsset.checksumSha256 = body.checksumSha256 ?? null;
          nextAsset.updatedAt = "2026-04-04T18:10:00Z";

          return jsonResponse(nextAsset);
        }

        if (
          url.pathname === "/api/admin/media/1/download-url-cache/refresh" &&
          method === "POST"
        ) {
          const nextAsset = assets.find((asset) => asset.assetId === 1);

          if (!nextAsset) {
            throw new Error("Audio asset #1 was not found.");
          }

          nextAsset.cachedDownloadUrl = "https://cdn.tellpal.test/assets/1";
          nextAsset.downloadUrlCachedAt = "2026-04-04T18:15:00Z";
          nextAsset.downloadUrlExpiresAt = "2026-04-04T19:15:00Z";
          nextAsset.updatedAt = "2026-04-04T18:15:00Z";

          return jsonResponse(nextAsset);
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/media",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", {
      name: /^asset library$/i,
      level: 1,
    });

    const recentAssetTable = screen.getByRole("table", {
      name: /recent asset registry/i,
    });

    fireEvent.click(
      within(recentAssetTable).getByText(originalAudioAssetResponse.objectPath),
    );

    expect(
      await screen.findByRole("heading", { name: /asset #1/i }),
    ).toBeVisible();

    fireEvent.change(await screen.findByLabelText(/mime type/i), {
      target: { value: "audio/mpeg" },
    });
    fireEvent.change(screen.getByLabelText(/byte size/i), {
      target: { value: "5243001" },
    });
    fireEvent.change(screen.getByLabelText(/sha-256 checksum/i), {
      target: { value: "audio-checksum-1-refresh" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/mime type/i)).toHaveValue("audio/mpeg");
    });

    fireEvent.click(
      screen.getAllByRole("button", { name: /refresh cached url/i })[1]!,
    );

    await waitFor(() => {
      expect(screen.getByText(/^available$/i)).toBeVisible();
    });

    expect(screen.getByText(/last cached: 04 apr 2026/i)).toBeVisible();
  }, 15_000);
});
