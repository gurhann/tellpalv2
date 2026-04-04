import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { phoneThumbnailAssetViewModel } from "@/features/assets/test/fixtures";

import { AssetDetailSheet } from "./asset-detail-sheet";

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

vi.mock("@/features/assets/components/asset-metadata-form", () => ({
  AssetMetadataForm: () => <div>Asset metadata form stub</div>,
}));

vi.mock("@/features/assets/components/refresh-download-url-button", () => ({
  RefreshDownloadUrlButton: () => (
    <button type="button">Refresh cached URL</button>
  ),
}));

function makeDetailState(overrides: Record<string, unknown> = {}) {
  return {
    asset: phoneThumbnailAssetViewModel,
    isLoading: false,
    problem: null,
    isNotFound: false,
    ...overrides,
  };
}

beforeEach(() => {
  assetDetailHookMock.useAssetDetail.mockReset();
  assetDetailHookMock.useAssetDetail.mockReturnValue(makeDetailState());
});

describe("AssetDetailSheet", () => {
  it("renders asset metadata detail when one asset is selected", () => {
    render(<AssetDetailSheet assetId={4} open onOpenChange={vi.fn()} />);

    expect(screen.getByText(/asset #4/i)).toBeVisible();
    expect(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    ).toBeVisible();
    expect(screen.getByText(/asset metadata form stub/i)).toBeVisible();
  });

  it("renders a loading state while the selected asset is hydrating", () => {
    assetDetailHookMock.useAssetDetail.mockReturnValue(
      makeDetailState({
        asset: null,
        isLoading: true,
      }),
    );

    render(<AssetDetailSheet assetId={4} open onOpenChange={vi.fn()} />);

    expect(screen.getByText(/loading asset detail/i)).toBeVisible();
  });
});
