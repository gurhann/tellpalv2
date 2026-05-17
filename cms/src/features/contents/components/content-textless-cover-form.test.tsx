import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { storyContentViewModel } from "@/features/contents/test/fixtures";

import { ContentTextlessCoverForm } from "./content-textless-cover-form";

const saveContentHookMock = vi.hoisted(() => ({
  useSaveContent: vi.fn(),
}));

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

const uploadAssetHookMock = vi.hoisted(() => ({
  useUploadAsset: vi.fn(),
}));

vi.mock("@/features/contents/mutations/use-save-content", () => ({
  useSaveContent: saveContentHookMock.useSaveContent,
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

function makeSaveMutationState(
  overrides: Partial<ReturnType<typeof vi.fn>> = {},
) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  saveContentHookMock.useSaveContent.mockReset();
  assetDetailHookMock.useAssetDetail.mockReset();
  uploadAssetHookMock.useUploadAsset.mockReset();
  vi.mocked(assetAdminApi.getAsset).mockReset();
  saveContentHookMock.useSaveContent.mockReturnValue(makeSaveMutationState());
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
    objectPath: `/content/images/source-cover-${assetId}.jpg`,
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

describe("ContentTextlessCoverForm", () => {
  it("saves a changed textless cover asset id while preserving metadata", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 1,
        type: "STORY",
        externalKey: "story.evening-garden",
        active: true,
        ageRange: 5,
        pageCount: 2,
        textlessCoverMediaId: 888,
      }),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(<ContentTextlessCoverForm content={storyContentViewModel} />);

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));
    fireEvent.change(screen.getByLabelText(/manual asset id/i), {
      target: { value: "888" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save source cover/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        type: "STORY",
        externalKey: "story.evening-garden",
        ageRange: 5,
        active: true,
        textlessCoverMediaId: 888,
      });
    });
  });

  it("rejects non-image asset ids before saving", async () => {
    const mutationState = makeSaveMutationState();
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);
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

    render(<ContentTextlessCoverForm content={storyContentViewModel} />);

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));
    fireEvent.change(screen.getByLabelText(/manual asset id/i), {
      target: { value: "55" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save source cover/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/must reference an image asset/i),
      ).toBeInTheDocument();
    });
    expect(mutationState.mutateAsync).not.toHaveBeenCalled();
  });
});
