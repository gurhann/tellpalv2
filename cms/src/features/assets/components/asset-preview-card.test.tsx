import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  contentArchiveAssetViewModel,
  originalAudioAssetViewModel,
  phoneThumbnailAssetViewModel,
} from "@/features/assets/test/fixtures";

import { AssetPreviewCard } from "./asset-preview-card";

const assetPreviewHookMock = vi.hoisted(() => ({
  useAssetPreview: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-asset-preview", () => ({
  useAssetPreview: assetPreviewHookMock.useAssetPreview,
}));

function makePreviewState(overrides: Record<string, unknown> = {}) {
  return {
    previewUrl: "https://cdn.tellpal.test/assets/4",
    previewStatus: "available",
    previewErrorMessage: null,
    isRefreshing: false,
    refreshPreview: vi.fn(),
    ...overrides,
  };
}

beforeEach(() => {
  assetPreviewHookMock.useAssetPreview.mockReset();
  assetPreviewHookMock.useAssetPreview.mockReturnValue(makePreviewState());
});

describe("AssetPreviewCard", () => {
  it("renders inline image previews for image assets", () => {
    render(<AssetPreviewCard asset={phoneThumbnailAssetViewModel} open />);

    expect(
      screen.getByRole("img", { name: /preview of asset #4/i }),
    ).toBeVisible();
  });

  it("renders native audio controls for audio assets", () => {
    assetPreviewHookMock.useAssetPreview.mockReturnValue(
      makePreviewState({
        previewUrl: "https://cdn.tellpal.test/assets/1.wav",
      }),
    );

    render(<AssetPreviewCard asset={originalAudioAssetViewModel} open />);

    expect(screen.getByLabelText(/audio preview for asset #1/i)).toBeVisible();
  });

  it("keeps archive assets in an unavailable preview state", () => {
    assetPreviewHookMock.useAssetPreview.mockReturnValue(
      makePreviewState({
        previewUrl: null,
        previewStatus: "unavailable",
      }),
    );

    render(<AssetPreviewCard asset={contentArchiveAssetViewModel} open />);

    expect(
      screen.getByText(/preview unavailable for archive assets/i),
    ).toBeVisible();
  });

  it("allows retrying preview refresh after a media load failure", () => {
    const refreshPreview = vi.fn();

    assetPreviewHookMock.useAssetPreview.mockReturnValue(
      makePreviewState({
        refreshPreview,
      }),
    );

    render(<AssetPreviewCard asset={phoneThumbnailAssetViewModel} open />);

    fireEvent.error(screen.getByRole("img", { name: /preview of asset #4/i }));
    fireEvent.click(screen.getByRole("button", { name: /retry preview/i }));

    expect(refreshPreview).toHaveBeenCalledWith({ force: true });
  });
});
