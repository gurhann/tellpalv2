import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { firstStoryPageViewModel } from "@/features/story-pages/test/fixtures";

import { StoryPageTextlessIllustrationForm } from "./story-page-textless-illustration-form";

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

const uploadAssetHookMock = vi.hoisted(() => ({
  useUploadAsset: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", () => ({
  assetAdminApi: {
    getAsset: vi.fn(),
  },
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

vi.mock("@/features/assets/mutations/use-upload-asset", () => ({
  useUploadAsset: uploadAssetHookMock.useUploadAsset,
}));

beforeEach(() => {
  vi.mocked(assetAdminApi.getAsset).mockReset();
  assetDetailHookMock.useAssetDetail.mockReset();
  uploadAssetHookMock.useUploadAsset.mockReset();
  assetDetailHookMock.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
  });
  uploadAssetHookMock.useUploadAsset.mockReturnValue({
    mutateAsync: vi.fn(),
    isPending: false,
    problem: null,
    reset: vi.fn(),
  });
  vi.mocked(assetAdminApi.getAsset).mockImplementation(async (assetId) => ({
    assetId,
    provider: "LOCAL_STUB",
    objectPath: `/content/images/source-${assetId}.jpg`,
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
  }));
});

describe("StoryPageTextlessIllustrationForm", () => {
  it("saves a changed textless illustration asset id", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      textlessIllustrationMediaId: 777,
      localizationCount: 2,
    });

    render(
      <StoryPageTextlessIllustrationForm
        storyPage={firstStoryPageViewModel}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));
    fireEvent.change(screen.getByLabelText(/manual asset id/i), {
      target: { value: "777" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save source image/i }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        textlessIllustrationMediaId: 777,
      });
    });
  });

  it("clears the source image and reports dirty state", async () => {
    const onDirtyChange = vi.fn();
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      textlessIllustrationMediaId: null,
      localizationCount: 2,
    });

    render(
      <StoryPageTextlessIllustrationForm
        storyPage={firstStoryPageViewModel}
        onDirtyChange={onDirtyChange}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /clear/i }));

    await waitFor(() => {
      expect(onDirtyChange).toHaveBeenLastCalledWith(true);
    });

    fireEvent.click(screen.getByRole("button", { name: /save source image/i }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        textlessIllustrationMediaId: null,
      });
    });
    expect(onDirtyChange).toHaveBeenLastCalledWith(false);
  });

  it("rejects non-image asset ids before saving", async () => {
    const onSave = vi.fn();
    vi.mocked(assetAdminApi.getAsset).mockResolvedValue({
      assetId: 55,
      provider: "LOCAL_STUB",
      objectPath: "/content/audio/source.mp3",
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
      <StoryPageTextlessIllustrationForm
        storyPage={firstStoryPageViewModel}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));
    fireEvent.change(screen.getByLabelText(/manual asset id/i), {
      target: { value: "55" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save source image/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/must reference an image asset/i),
      ).toBeInTheDocument();
    });
    expect(onSave).not.toHaveBeenCalled();
  });
});
