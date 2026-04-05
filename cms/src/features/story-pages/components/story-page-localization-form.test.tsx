import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { firstStoryPageViewModel } from "@/features/story-pages/test/fixtures";

import { StoryPageLocalizationForm } from "./story-page-localization-form";

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", () => ({
  assetAdminApi: {
    getAsset: vi.fn(),
  },
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

beforeEach(() => {
  assetDetailHookMock.useAssetDetail.mockReset();
  assetDetailHookMock.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
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
    fireEvent.change(screen.getByLabelText(/audio asset/i), {
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
    fireEvent.change(screen.getByLabelText(/illustration asset/i), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByLabelText(/audio asset/i), {
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
});
