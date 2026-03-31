import { describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { ApiClientError } from "@/lib/http/client";

import { validateIllustrationAssetId } from "./illustration-asset-validation";

vi.mock("@/features/assets/api/asset-admin", () => ({
  assetAdminApi: {
    getAsset: vi.fn(),
  },
}));

describe("validateIllustrationAssetId", () => {
  it("allows empty asset references", async () => {
    await expect(validateIllustrationAssetId(null)).resolves.toBeNull();
  });

  it("accepts image assets", async () => {
    vi.mocked(assetAdminApi.getAsset).mockResolvedValue({
      assetId: 41,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/evening-garden-page-1.jpg",
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
    });

    await expect(validateIllustrationAssetId(41)).resolves.toBeNull();
  });

  it("rejects non-image assets before submit", async () => {
    vi.mocked(assetAdminApi.getAsset).mockResolvedValue({
      assetId: 1,
      provider: "LOCAL_STUB",
      objectPath: "/content/audio/rain-room-en.mp3",
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
    });

    await expect(validateIllustrationAssetId(1)).resolves.toBe(
      "Asset #1 must reference an IMAGE asset.",
    );
  });

  it("maps missing assets to a field-level message", async () => {
    vi.mocked(assetAdminApi.getAsset).mockRejectedValue(
      new ApiClientError(
        {
          type: "about:blank",
          title: "Asset not found",
          status: 404,
          detail: "Asset #999 was not found.",
          errorCode: "media_asset_not_found",
        },
        new Response(JSON.stringify({}), {
          status: 404,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(validateIllustrationAssetId(999)).resolves.toBe(
      "Asset #999 was not found.",
    );
  });
});
