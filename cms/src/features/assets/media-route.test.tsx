import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetViewModels } from "@/features/assets/test/fixtures";

import { MediaRoute } from "@/app/routes/media";

const assetHookMocks = vi.hoisted(() => ({
  useRecentAssets: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-recent-assets", () => ({
  useRecentAssets: assetHookMocks.useRecentAssets,
}));

function makeAssetListState() {
  return {
    assets: assetViewModels,
    limit: 24,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  };
}

beforeEach(() => {
  assetHookMocks.useRecentAssets.mockReset();
  assetHookMocks.useRecentAssets.mockReturnValue(makeAssetListState());
});

describe("MediaRoute", () => {
  it("renders the live asset library shell", () => {
    render(<MediaRoute />);

    expect(
      screen.getByRole("heading", { name: /asset library/i, level: 1 }),
    ).toBeVisible();
    expect(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: /asset detail sheet/i }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: /shared asset picker/i }),
    ).toBeDisabled();
  });

  it("refreshes the live recent asset query on demand", () => {
    const state = makeAssetListState();
    assetHookMocks.useRecentAssets.mockReturnValue(state);

    render(<MediaRoute />);

    fireEvent.click(screen.getByRole("button", { name: /refresh/i }));

    expect(state.refetch).toHaveBeenCalledTimes(1);
  });
});
