import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { firstStoryPageViewModel } from "@/features/story-pages/test/fixtures";

import { StoryPageLocalizationForm } from "./story-page-localization-form";

const recentAudioAssetHookMocks = vi.hoisted(() => ({
  useRecentAudioAssets: vi.fn(),
}));

vi.mock("@/features/story-pages/queries/use-recent-audio-assets", () => ({
  useRecentAudioAssets: recentAudioAssetHookMocks.useRecentAudioAssets,
}));

vi.mock("@/features/assets/api/asset-admin", () => ({
  assetAdminApi: {
    getAsset: vi.fn(),
  },
}));

describe("StoryPageLocalizationForm", () => {
  it("normalizes empty body text to null when saving a page locale", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "en",
      bodyText: null,
      audioMediaId: null,
    });

    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
    });

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
      });
    });
  });

  it("uses recent audio assets for the language payload", async () => {
    const onSave = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "tr",
      bodyText: "Bahce kapisinin ustundeki aya bak.",
      audioMediaId: 3,
    });

    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [
        {
          assetId: 3,
          provider: "LOCAL_STUB",
          objectPath: "/content/audio/midnight-river-en.mp3",
          mediaType: "AUDIO",
          kind: "ORIGINAL_AUDIO",
        },
      ],
      isLoading: false,
    });
    vi.mocked(assetAdminApi.getAsset).mockResolvedValue({
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
    });

    render(
      <StoryPageLocalizationForm
        contentLocalization={storyContentViewModel.localizations[1]!}
        storyPage={firstStoryPageViewModel}
        onSave={onSave}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /asset #3/i }));
    fireEvent.click(
      screen.getByRole("button", { name: /save page localization/i }),
    );

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith({
        languageCode: "tr",
        bodyText: "Bahce kapisinin ustundeki aya bak.",
        audioMediaId: 3,
      });
    });
  });
});
