import { QueryClient } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  assetResponses,
  uploadedFirebaseImageAssetResponse,
} from "@/features/assets/test/fixtures";
import type { AdminSessionPayload } from "@/types/api";

const uploadHelperMock = vi.hoisted(() => vi.fn());

vi.mock("@/features/assets/lib/upload-file-to-signed-url", () => ({
  uploadFileToSignedUrl: uploadHelperMock,
}));

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
  uploadHelperMock.mockReset();
  uploadHelperMock.mockImplementation(
    async (options?: { onProgress?: (value: number) => void }) => {
      options?.onProgress?.(100);
    },
  );
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
  it("hydrates the media utility, saves metadata, and refreshes cached download URLs", async () => {
    const session = makeSession();
    const assets = cloneJson(assetResponses);
    const uploadedAsset = cloneJson(uploadedFirebaseImageAssetResponse);
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

        if (url.pathname === "/api/admin/media/12" && method === "GET") {
          return jsonResponse(
            assets.find((asset) => asset.assetId === 12) ?? null,
          );
        }

        if (url.pathname === "/api/admin/media/uploads" && method === "POST") {
          return jsonResponse({
            provider: "FIREBASE_STORAGE",
            objectPath: uploadedAsset.objectPath,
            uploadUrl:
              "https://firebase-storage.test/upload/local/manual/images/original/2026/04/asset-12-bedtime-cover.jpg",
            httpMethod: "PUT",
            requiredHeaders: {
              "Content-Type": "image/jpeg",
            },
            expiresAt: "2026-04-04T18:30:00Z",
            uploadToken: "upload-token-12",
          });
        }

        if (
          url.pathname === "/api/admin/media/uploads/complete" &&
          method === "POST"
        ) {
          assets.unshift(uploadedAsset);
          return jsonResponse(uploadedAsset);
        }

        if (
          url.pathname === "/api/admin/media/12/metadata" &&
          method === "PUT"
        ) {
          const body = (await readRequestJson(init)) as {
            mimeType?: string | null;
            byteSize?: number | null;
            checksumSha256?: string | null;
          };
          const nextAsset = assets.find((asset) => asset.assetId === 12);

          if (!nextAsset) {
            throw new Error("Uploaded image asset #12 was not found.");
          }

          nextAsset.mimeType = body.mimeType ?? null;
          nextAsset.byteSize = body.byteSize ?? null;
          nextAsset.checksumSha256 = body.checksumSha256 ?? null;
          nextAsset.updatedAt = "2026-04-04T18:10:00Z";

          return jsonResponse(nextAsset);
        }

        if (
          url.pathname === "/api/admin/media/12/download-url-cache/refresh" &&
          method === "POST"
        ) {
          const nextAsset = assets.find((asset) => asset.assetId === 12);

          if (!nextAsset) {
            throw new Error("Uploaded image asset #12 was not found.");
          }

          nextAsset.cachedDownloadUrl =
            "https://storage.googleapis.com/tellpal-prod/local/manual/images/original/2026/04/asset-12-bedtime-cover.jpg";
          nextAsset.downloadUrlCachedAt = "2026-04-05T18:15:00Z";
          nextAsset.downloadUrlExpiresAt = "2026-04-05T19:15:00Z";
          nextAsset.updatedAt = "2026-04-05T18:15:00Z";

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
      name: /^media utility$/i,
      level: 1,
    });

    fireEvent.click(screen.getByRole("button", { name: /upload asset/i }));

    const fileInput = screen.getByLabelText(/file/i);
    const uploadFile = new File(["image"], "bedtime-cover.jpg", {
      type: "image/jpeg",
    });
    fireEvent.change(fileInput, { target: { files: [uploadFile] } });
    fireEvent.click(screen.getByRole("button", { name: /^upload asset$/i }));

    expect(
      await screen.findByRole("heading", { name: /asset #12/i }),
    ).toBeVisible();
    expect(uploadHelperMock).toHaveBeenCalledTimes(1);
    expect(
      await screen.findAllByText(uploadedFirebaseImageAssetResponse.objectPath),
    ).toHaveLength(2);
    expect(
      await screen.findByRole("img", { name: /preview of asset #12/i }),
    ).toHaveAttribute(
      "src",
      "https://storage.googleapis.com/tellpal-prod/local/manual/images/original/2026/04/asset-12-bedtime-cover.jpg",
    );
    expect(
      fetchMock.mock.calls.some(([input, init]) => {
        const url = new URL(typeof input === "string" ? input : input.url);

        return (
          url.pathname === "/api/admin/media/12/download-url-cache/refresh" &&
          init?.method === "POST"
        );
      }),
    ).toBe(true);

    fireEvent.change(await screen.findByLabelText(/mime type/i), {
      target: { value: "image/jpeg" },
    });
    fireEvent.change(screen.getByLabelText(/byte size/i), {
      target: { value: "4096" },
    });
    fireEvent.change(screen.getByLabelText(/sha-256 checksum/i), {
      target: { value: "image-checksum-12-refresh" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/mime type/i)).toHaveValue("image/jpeg");
    });

    fireEvent.click(
      screen.getAllByRole("button", { name: /refresh cached url/i })[1]!,
    );

    await waitFor(() => {
      expect(screen.getByText(/^available$/i)).toBeVisible();
    });

    expect(screen.getByText(/last cached: 05 apr 2026/i)).toBeVisible();
  }, 15_000);
});
