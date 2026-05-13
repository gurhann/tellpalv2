import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { storyPageViewModels } from "@/features/story-pages/test/fixtures";

import { StoryContentPreviewDialog } from "./story-content-preview-dialog";

const assetDetailHookMocks = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));
const assetPreviewHookMocks = vi.hoisted(() => ({
  useAssetPreview: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMocks.useAssetDetail,
}));

vi.mock("@/features/assets/queries/use-asset-preview", () => ({
  useAssetPreview: assetPreviewHookMocks.useAssetPreview,
}));

function makeAsset(id: number, mediaType: "IMAGE" | "AUDIO"): AssetViewModel {
  const extension = mediaType === "IMAGE" ? "jpg" : "mp3";

  return mapAdminAsset({
    assetId: id,
    provider: "FIREBASE_STORAGE",
    objectPath: `/preview/story/${id}.${extension}`,
    mediaType,
    kind: mediaType === "IMAGE" ? "ORIGINAL_IMAGE" : "ORIGINAL_AUDIO",
    mimeType: mediaType === "IMAGE" ? "image/jpeg" : "audio/mpeg",
    byteSize: 1024,
    checksumSha256: null,
    cachedDownloadUrl: null,
    downloadUrlCachedAt: null,
    downloadUrlExpiresAt: null,
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
  } satisfies AdminAssetResponse);
}

const previewAssets = new Map<number, AssetViewModel>([
  [41, makeAsset(41, "IMAGE")],
  [42, makeAsset(42, "IMAGE")],
  [43, makeAsset(43, "IMAGE")],
  [81, makeAsset(81, "AUDIO")],
  [82, makeAsset(82, "AUDIO")],
  [83, makeAsset(83, "AUDIO")],
]);

function makeDetailState(assetId: number | null) {
  return {
    asset: assetId ? (previewAssets.get(assetId) ?? null) : null,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn().mockResolvedValue(undefined),
  };
}

function makePreviewState(asset: AssetViewModel | null, enabled: boolean) {
  return {
    previewUrl:
      enabled && asset
        ? `https://cdn.tellpal.test/assets/${asset.id}.${
            asset.previewKind === "audio" ? "mp3" : "jpg"
          }`
        : null,
    previewStatus: enabled && asset ? "available" : "unavailable",
    previewErrorMessage: null,
    isRefreshing: false,
    refreshPreview: vi.fn().mockResolvedValue(undefined),
  } as const;
}

function renderPreviewDialog(
  overrides: Partial<ComponentProps<typeof StoryContentPreviewDialog>> = {},
) {
  const props: ComponentProps<typeof StoryContentPreviewDialog> = {
    content: storyContentViewModel,
    storyPages: storyPageViewModels,
    languageCode: "en",
    open: true,
    isLoading: false,
    problem: null,
    onRetry: vi.fn(),
    onOpenChange: vi.fn(),
    onEditPage: vi.fn(),
    ...overrides,
  };

  render(<StoryContentPreviewDialog {...props} />);

  return props;
}

let playMock: ReturnType<typeof vi.fn>;
let pauseMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  playMock = vi.fn().mockResolvedValue(undefined);
  pauseMock = vi.fn();

  Object.defineProperty(window.HTMLMediaElement.prototype, "play", {
    configurable: true,
    value: playMock,
  });
  Object.defineProperty(window.HTMLMediaElement.prototype, "pause", {
    configurable: true,
    value: pauseMock,
  });
  Object.defineProperty(window.HTMLMediaElement.prototype, "duration", {
    configurable: true,
    get() {
      return 9;
    },
  });

  assetDetailHookMocks.useAssetDetail.mockReset();
  assetDetailHookMocks.useAssetDetail.mockImplementation(makeDetailState);
  assetPreviewHookMocks.useAssetPreview.mockReset();
  assetPreviewHookMocks.useAssetPreview.mockImplementation(makePreviewState);
});

describe("StoryContentPreviewDialog", () => {
  it("keeps modal body fixed while only the page list scrolls", () => {
    renderPreviewDialog();

    expect(document.querySelector('[data-slot="dialog-body"]')).toHaveClass(
      "overflow-hidden",
      "flex-1",
      "min-h-0",
    );
    expect(
      screen.getByRole("navigation", { name: /story preview pages/i }),
    ).toHaveClass("overflow-y-auto", "min-h-0", "flex-1");
    expect(screen.getByText(/active page/i).parentElement).toHaveClass(
      "shrink-0",
    );
    expect(
      screen.getByRole("img", {
        name: /story page 1 illustration preview/i,
      }),
    ).toHaveClass("h-full", "max-h-full", "object-contain");
  });

  it("loads the selected locale page illustration and audio preview", async () => {
    renderPreviewDialog();

    expect(
      screen.getByRole("img", {
        name: /story page 1 illustration preview/i,
      }),
    ).toHaveAttribute("src", "https://cdn.tellpal.test/assets/41.jpg");
    expect(
      screen.getByLabelText(/audio preview for story page 1/i),
    ).toHaveAttribute("src", "https://cdn.tellpal.test/assets/81.mp3");

    await waitFor(() => {
      expect(playMock).toHaveBeenCalled();
    });
  });

  it("moves to the next page when the active page audio ends", async () => {
    renderPreviewDialog();

    fireEvent.ended(screen.getByLabelText(/audio preview for story page 1/i));

    expect(
      await screen.findByRole("img", {
        name: /story page 2 illustration preview/i,
      }),
    ).toHaveAttribute("src", "https://cdn.tellpal.test/assets/43.jpg");
    expect(
      screen.getByLabelText(/audio preview for story page 2/i),
    ).toHaveAttribute("src", "https://cdn.tellpal.test/assets/83.mp3");
  });

  it("stops playback when the final page audio ends", async () => {
    renderPreviewDialog();

    fireEvent.click(screen.getByRole("button", { name: /^next$/i }));
    fireEvent.ended(
      await screen.findByLabelText(/audio preview for story page 2/i),
    );

    expect(screen.getByRole("button", { name: /replay/i })).toBeEnabled();
  });

  it("pauses and resumes the active page audio", async () => {
    renderPreviewDialog();

    await waitFor(() => {
      expect(playMock).toHaveBeenCalledTimes(1);
    });

    fireEvent.click(screen.getByRole("button", { name: /pause/i }));

    expect(pauseMock).toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /^play$/i }));

    await waitFor(() => {
      expect(playMock).toHaveBeenCalledTimes(2);
    });
  });

  it("changes pages with previous and next controls", async () => {
    renderPreviewDialog();

    expect(screen.getByRole("button", { name: /^previous$/i })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /^next$/i }));

    expect(
      await screen.findByRole("img", {
        name: /story page 2 illustration preview/i,
      }),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: /^next$/i })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /^previous$/i }));

    expect(
      await screen.findByRole("img", {
        name: /story page 1 illustration preview/i,
      }),
    ).toBeVisible();
  });

  it("stops on a page with missing localized audio and exposes edit handoff", () => {
    const onEditPage = vi.fn();
    const missingAudioPage = {
      ...storyPageViewModels[0]!,
      localizations: storyPageViewModels[0]!.localizations.map(
        (localization) =>
          localization.languageCode === "en"
            ? {
                ...localization,
                audioAssetId: null,
                hasAudioAsset: false,
              }
            : localization,
      ),
    };

    renderPreviewDialog({
      storyPages: [missingAudioPage],
      onEditPage,
    });

    expect(screen.getByText(/page 1 is missing audio/i)).toBeVisible();

    fireEvent.click(
      screen.getAllByRole("button", { name: /^edit page$/i })[0]!,
    );

    expect(onEditPage).toHaveBeenCalledWith(1);
  });

  it("shows a retry state when an audio preview token fails", () => {
    const refreshPreview = vi.fn().mockResolvedValue(undefined);
    assetPreviewHookMocks.useAssetPreview.mockImplementation(
      (asset: AssetViewModel | null, enabled: boolean) => {
        if (enabled && asset?.id === 81) {
          return {
            previewUrl: null,
            previewStatus: "error",
            previewErrorMessage: "Token expired",
            isRefreshing: false,
            refreshPreview,
          };
        }

        return makePreviewState(asset, enabled);
      },
    );

    renderPreviewDialog();

    expect(
      screen.getByText(/audio preview could not be loaded/i),
    ).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: /retry preview/i }));

    expect(refreshPreview).toHaveBeenCalledWith({ force: true });
  });
});
