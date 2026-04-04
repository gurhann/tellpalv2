import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetViewModels } from "@/features/assets/test/fixtures";

import { AssetPickerDialog } from "./asset-picker-dialog";

const recentAssetsHookMocks = vi.hoisted(() => ({
  useRecentAssets: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-recent-assets", () => ({
  useRecentAssets: recentAssetsHookMocks.useRecentAssets,
}));

function makeRecentAssetsState() {
  return {
    assets: assetViewModels,
    isLoading: false,
    isFetching: false,
    problem: null,
    limit: 24,
    refetch: vi.fn(),
  };
}

beforeEach(() => {
  recentAssetsHookMocks.useRecentAssets.mockReset();
  recentAssetsHookMocks.useRecentAssets.mockReturnValue(
    makeRecentAssetsState(),
  );
});

describe("AssetPickerDialog", () => {
  it("filters recent assets by media type and selects one asset", () => {
    const onSelectAsset = vi.fn();
    const onOpenChange = vi.fn();

    render(
      <AssetPickerDialog
        description="Pick one image asset."
        mediaType="IMAGE"
        open
        title="Pick image asset"
        onOpenChange={onOpenChange}
        onSelectAsset={onSelectAsset}
      />,
    );

    expect(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    ).toBeVisible();
    expect(
      screen.queryByText("/content/audio/rain-room-en.wav"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /use asset/i }));

    expect(onSelectAsset).toHaveBeenCalledWith(assetViewModels[0]);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("applies local search to recent picker results", () => {
    render(
      <AssetPickerDialog
        description="Pick one archive asset."
        mediaType="ARCHIVE"
        open
        title="Pick archive asset"
        onOpenChange={vi.fn()}
        onSelectAsset={vi.fn()}
      />,
    );

    fireEvent.change(screen.getByLabelText(/search assets/i), {
      target: { value: "story.evening-garden" },
    });

    expect(
      screen.getByText("/content/packages/story.evening-garden.en.zip"),
    ).toBeVisible();
  });
});
