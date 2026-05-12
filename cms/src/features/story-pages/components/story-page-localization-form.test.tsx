import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { firstStoryPageViewModel } from "@/features/story-pages/test/fixtures";

import { StoryPageLocalizationForm } from "./story-page-localization-form";

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
});

describe("StoryPageLocalizationForm", () => {
  it("normalizes empty body text to null when saving a page locale", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "en",
      bodyText: null,
      audioMediaId: null,
      illustrationMediaId: 41,
    });

    vi.mocked(assetAdminApi.getAsset).mockImplementation(async (assetId) => ({
      assetId,
      provider: "LOCAL_STUB",
      objectPath: `/content/assets/${assetId}`,
      mediaType: "IMAGE",
      kind: "ILLUSTRATION",
      mimeType: "image/jpeg",
      byteSize: null,
      checksumSha256: null,
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-03-31T12:00:00Z",
      updatedAt: "2026-03-31T12:00:00Z",
    }));

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[0]!}
        storyPage={firstStoryPageViewModel}
        onSave={onSave}
      />,
    );

    fireEvent.change(screen.getByLabelText(/body text/i), {
      target: { value: "" },
    });
    fireEvent.click(screen.getAllByRole("button", { name: /advanced/i })[1]!);
    fireEvent.change(screen.getByLabelText(/audio asset id/i), {
      target: { value: "" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /save page localization/i }),
    );

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        languageCode: "en",
        bodyText: null,
        audioMediaId: null,
        illustrationMediaId: 41,
      });
    });
  });

  it("uses recent audio and image assets for the language payload", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "tr",
      bodyText: "Bahce kapisinin ustundeki aya bak.",
      audioMediaId: 3,
      illustrationMediaId: 4,
    });

    vi.mocked(assetAdminApi.getAsset).mockImplementation(async (assetId) => {
      if (assetId === 3) {
        return {
          assetId: 3,
          provider: "LOCAL_STUB",
          objectPath: "/content/audio/midnight-river-en.mp3",
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
        };
      }

      return {
        assetId,
        provider: "LOCAL_STUB",
        objectPath: "/content/images/evening-garden-page-1.jpg",
        mediaType: "IMAGE",
        kind: "ILLUSTRATION",
        mimeType: "image/jpeg",
        byteSize: null,
        checksumSha256: null,
        cachedDownloadUrl: null,
        downloadUrlCachedAt: null,
        downloadUrlExpiresAt: null,
        createdAt: "2026-03-31T12:00:00Z",
        updatedAt: "2026-03-31T12:00:00Z",
      };
    });

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[1]!}
        storyPage={firstStoryPageViewModel}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /advanced/i })[0]!);
    fireEvent.click(screen.getAllByRole("button", { name: /advanced/i })[1]!);
    fireEvent.change(screen.getByLabelText(/illustration asset id/i), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByLabelText(/audio asset id/i), {
      target: { value: "3" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /save page localization/i }),
    );

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        languageCode: "tr",
        bodyText: "Bahce kapisinin ustundeki aya bak.",
        audioMediaId: 3,
        illustrationMediaId: 4,
      });
    });
  });

  it("groups illustration and audio fields into one compact media workspace", () => {
    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[0]!}
        storyPage={firstStoryPageViewModel}
        onSave={vi.fn()}
      />,
    );

    const mediaGroup = screen.getByTestId("story-page-media-assets");

    expect(mediaGroup).toHaveClass(
      "md:grid-cols-[minmax(0,1.15fr)_minmax(18rem,0.85fr)]",
    );
    expect(mediaGroup).toContainElement(
      screen.getByTestId("story-page-en-illustration-asset"),
    );
    expect(mediaGroup).toContainElement(
      screen.getByTestId("story-page-en-illustration-asset-dropzone"),
    );
    expect(mediaGroup).toContainElement(
      screen.getByTestId("story-page-en-audio-asset"),
    );
    expect(mediaGroup).toContainElement(
      screen.getByTestId("story-page-en-audio-asset-dropzone"),
    );
  });

  it("reports dirty state when body text changes", async () => {
    const onDirtyChange = vi.fn();

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[0]!}
        storyPage={firstStoryPageViewModel}
        onDirtyChange={onDirtyChange}
        onSave={vi.fn()}
      />,
    );

    fireEvent.change(screen.getByLabelText(/body text/i), {
      target: { value: "A changed page draft." },
    });

    await waitFor(() => {
      expect(onDirtyChange).toHaveBeenCalledWith("en", true);
    });
  });

  it("reports dirty state when an asset field changes", async () => {
    const onDirtyChange = vi.fn();

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[0]!}
        storyPage={firstStoryPageViewModel}
        onDirtyChange={onDirtyChange}
        onSave={vi.fn()}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /advanced/i })[0]!);
    fireEvent.change(screen.getByLabelText(/illustration asset id/i), {
      target: { value: "44" },
    });

    await waitFor(() => {
      expect(onDirtyChange).toHaveBeenCalledWith("en", true);
    });
  });

  it("clears dirty state after a successful save reset", async () => {
    const onDirtyChange = vi.fn();
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "en",
      bodyText: "Saved page body.",
      audioMediaId: 81,
      illustrationMediaId: 41,
    });

    vi.mocked(assetAdminApi.getAsset).mockImplementation(async (assetId) => ({
      assetId,
      provider: "LOCAL_STUB",
      objectPath: `/content/assets/${assetId}`,
      mediaType: assetId === 81 ? "AUDIO" : "IMAGE",
      kind: assetId === 81 ? "ORIGINAL_AUDIO" : "ORIGINAL_IMAGE",
      mimeType: assetId === 81 ? "audio/mpeg" : "image/jpeg",
      byteSize: null,
      checksumSha256: null,
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-03-31T12:00:00Z",
      updatedAt: "2026-03-31T12:00:00Z",
    }));

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[0]!}
        storyPage={firstStoryPageViewModel}
        onDirtyChange={onDirtyChange}
        onSave={onSave}
      />,
    );

    fireEvent.change(screen.getByLabelText(/body text/i), {
      target: { value: "Saved page body." },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /save page localization/i }),
    );

    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
      expect(onDirtyChange).toHaveBeenCalledWith("en", false);
    });
  });
});
