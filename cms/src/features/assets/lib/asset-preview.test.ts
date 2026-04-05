import {
  ASSET_PREVIEW_EXPIRY_BUFFER_MS,
  getAssetPreviewKind,
  hasUsableCachedDownloadUrl,
  shouldRefreshAssetPreviewUrl,
} from "@/features/assets/lib/asset-preview";

describe("asset preview helpers", () => {
  it("maps media types to preview kinds", () => {
    expect(getAssetPreviewKind("IMAGE")).toBe("image");
    expect(getAssetPreviewKind("AUDIO")).toBe("audio");
    expect(getAssetPreviewKind("ARCHIVE")).toBe("none");
  });

  it("treats missing or expiring cached URLs as unusable", () => {
    const now = Date.parse("2026-04-05T12:00:00Z");

    expect(
      hasUsableCachedDownloadUrl(
        {
          mediaType: "IMAGE",
          cachedDownloadUrl: null,
          downloadUrlExpiresAt: null,
        },
        now,
      ),
    ).toBe(false);

    expect(
      hasUsableCachedDownloadUrl(
        {
          mediaType: "IMAGE",
          cachedDownloadUrl: "https://cdn.tellpal.test/assets/4",
          downloadUrlExpiresAt: "2026-04-05T12:04:59Z",
        },
        now,
      ),
    ).toBe(false);

    expect(
      hasUsableCachedDownloadUrl(
        {
          mediaType: "IMAGE",
          cachedDownloadUrl: "https://cdn.tellpal.test/assets/4",
          downloadUrlExpiresAt: new Date(
            now + ASSET_PREVIEW_EXPIRY_BUFFER_MS + 1_000,
          ).toISOString(),
        },
        now,
      ),
    ).toBe(true);
  });

  it("requires preview refresh only for previewable assets with unusable URLs", () => {
    const now = Date.parse("2026-04-05T12:00:00Z");

    expect(
      shouldRefreshAssetPreviewUrl(
        {
          mediaType: "ARCHIVE",
          cachedDownloadUrl: "https://cdn.tellpal.test/assets/9",
          downloadUrlExpiresAt: null,
        },
        now,
      ),
    ).toBe(false);

    expect(
      shouldRefreshAssetPreviewUrl(
        {
          mediaType: "AUDIO",
          cachedDownloadUrl: "https://cdn.tellpal.test/assets/1",
          downloadUrlExpiresAt: "2026-04-05T12:03:00Z",
        },
        now,
      ),
    ).toBe(true);
  });
});
