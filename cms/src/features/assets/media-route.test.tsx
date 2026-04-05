import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetViewModels } from "@/features/assets/test/fixtures";

import { MediaRoute } from "@/app/routes/media";

const assetHookMocks = vi.hoisted(() => ({
  useRecentAssets: vi.fn(),
  useAssetDetail: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-recent-assets", () => ({
  useRecentAssets: assetHookMocks.useRecentAssets,
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetHookMocks.useAssetDetail,
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

function makeAssetDetailState() {
  return {
    asset: assetViewModels[0],
    isLoading: false,
    problem: null,
    isNotFound: false,
  };
}

beforeEach(() => {
  assetHookMocks.useRecentAssets.mockReset();
  assetHookMocks.useAssetDetail.mockReset();
  assetHookMocks.useRecentAssets.mockReturnValue(makeAssetListState());
  assetHookMocks.useAssetDetail.mockReturnValue(makeAssetDetailState());
});

function renderWithQueryClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MediaRoute />
    </QueryClientProvider>,
  );
}

describe("MediaRoute", () => {
  it("renders the media utility shell", () => {
    renderWithQueryClient();

    expect(
      screen.getByRole("heading", { name: /media utility/i, level: 1 }),
    ).toBeVisible();
    expect(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: /upload asset/i })).toBeEnabled();
    expect(
      screen.getByText(/normal editorial binding now happens inside content/i),
    ).toBeVisible();
  });

  it("refreshes the live recent asset query on demand", () => {
    const state = makeAssetListState();
    assetHookMocks.useRecentAssets.mockReturnValue(state);

    renderWithQueryClient();

    fireEvent.click(screen.getByRole("button", { name: /refresh/i }));

    expect(state.refetch).toHaveBeenCalledTimes(1);
  });

  it("opens the detail sheet when an asset row is selected", () => {
    renderWithQueryClient();

    fireEvent.click(
      screen.getByText("/content/images/evening-garden-page-1.jpg"),
    );

    expect(screen.getAllByText(/asset #4/i).length).toBeGreaterThan(0);
    expect(assetHookMocks.useAssetDetail).toHaveBeenLastCalledWith(4);
  });

  it("opens the upload dialog on demand", () => {
    renderWithQueryClient();

    fireEvent.click(screen.getByRole("button", { name: /upload asset/i }));

    expect(
      screen.getByRole("heading", { name: /upload asset/i }),
    ).toBeVisible();
  });
});
