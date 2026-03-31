import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { firstStoryPageViewModel } from "@/features/story-pages/test/fixtures";

import { StoryPageForm } from "./story-page-form";

const recentImageAssetHookMocks = vi.hoisted(() => ({
  useRecentImageAssets: vi.fn(),
}));

vi.mock("@/features/story-pages/queries/use-recent-image-assets", () => ({
  useRecentImageAssets: recentImageAssetHookMocks.useRecentImageAssets,
}));

vi.mock("@/features/assets/api/asset-admin", () => ({
  assetAdminApi: {
    getAsset: vi.fn(),
  },
}));

describe("StoryPageForm", () => {
  it("saves page metadata with a recent image asset selection", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      illustrationMediaId: 4,
      localizationCount: 2,
    });

    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [
        {
          assetId: 4,
          provider: "LOCAL_STUB",
          objectPath: "/content/images/evening-garden-page-1.jpg",
          mediaType: "IMAGE",
          kind: "ORIGINAL_IMAGE",
        },
      ],
      isLoading: false,
    });
    vi.mocked(assetAdminApi.getAsset).mockResolvedValue({
      assetId: 4,
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

    render(
      <StoryPageForm storyPage={firstStoryPageViewModel} onSave={onSave} />,
    );

    fireEvent.click(screen.getByRole("button", { name: /asset #4/i }));
    fireEvent.click(
      screen.getByRole("button", { name: /save page metadata/i }),
    );

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        pageNumber: 1,
        illustrationMediaId: 4,
      });
    });
  });

  it("blocks non-image assets before submit", async () => {
    const onSave = vi.fn();

    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
    });
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

    render(
      <StoryPageForm storyPage={firstStoryPageViewModel} onSave={onSave} />,
    );

    fireEvent.change(screen.getByLabelText(/illustration asset id/i), {
      target: { value: "1" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /save page metadata/i }),
    );

    await waitFor(() => {
      expect(
        screen.getByText(/asset #1 must reference an image asset/i),
      ).toBeInTheDocument();
    });
    expect(onSave).not.toHaveBeenCalled();
  });
});
