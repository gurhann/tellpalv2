import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { StoryPagesRoute } from "@/app/routes/story-pages";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";
import { storyPageViewModels } from "@/features/story-pages/test/fixtures";

const contentDetailHookMocks = vi.hoisted(() => ({
  useContentDetail: vi.fn(),
}));
const storyPageHookMocks = vi.hoisted(() => ({
  useStoryPages: vi.fn(),
  useStoryPage: vi.fn(),
}));
const storyPageMutationMocks = vi.hoisted(() => ({
  useStoryPageActions: vi.fn(),
}));
const recentImageAssetHookMocks = vi.hoisted(() => ({
  useRecentImageAssets: vi.fn(),
}));
const recentAudioAssetHookMocks = vi.hoisted(() => ({
  useRecentAudioAssets: vi.fn(),
}));
const assetDetailHookMocks = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentDetailHookMocks.useContentDetail,
}));

vi.mock("@/features/story-pages/queries/use-story-pages", () => ({
  useStoryPages: storyPageHookMocks.useStoryPages,
  useStoryPage: storyPageHookMocks.useStoryPage,
}));

vi.mock("@/features/story-pages/mutations/use-story-page-actions", () => ({
  useStoryPageActions: storyPageMutationMocks.useStoryPageActions,
}));

vi.mock("@/features/story-pages/queries/use-recent-image-assets", () => ({
  useRecentImageAssets: recentImageAssetHookMocks.useRecentImageAssets,
}));

vi.mock("@/features/story-pages/queries/use-recent-audio-assets", () => ({
  useRecentAudioAssets: recentAudioAssetHookMocks.useRecentAudioAssets,
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMocks.useAssetDetail,
}));

function makeDetailState(overrides: Record<string, unknown> = {}) {
  return {
    content: storyContentViewModel,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn(),
    ...overrides,
  };
}

function makeStoryPageState(overrides: Record<string, unknown> = {}) {
  return {
    storyPages: storyPageViewModels,
    isLoading: false,
    isFetching: false,
    isSuccess: true,
    problem: null,
    refetch: vi.fn(),
    ...overrides,
  };
}

function renderStoryRoute(initialEntry = "/contents/1/story-pages") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route
          path="/contents/:contentId/story-pages"
          element={<StoryPagesRoute />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("StoryPagesRoute", () => {
  it("renders the story-only route shell for STORY content", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockReturnValue({
      storyPage: storyPageViewModels[0],
      isLoading: false,
      problem: null,
    });
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    expect(
      screen.getByRole("heading", { name: /story pages for evening garden/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/1");
    expect(
      screen.getByRole("button", { name: /add story page/i }),
    ).toBeEnabled();
    expect(screen.getByText(/total pages/i)).toBeInTheDocument();
    expect(screen.getByText(/2 localized/i)).toBeInTheDocument();
    expect(screen.getByText(/page 1/i)).toBeInTheDocument();
    expect(screen.getByText(/story\.evening-garden/i)).toBeInTheDocument();
    expect(screen.getByText(/ready illustrations/i)).toBeInTheDocument();
    expect(screen.getByText(/3 localized illustrations/i)).toBeInTheDocument();
    expect(screen.getAllByText(/2 locales/i).length).toBeGreaterThan(0);
    expect(screen.queryByText(/structure notes/i)).not.toBeInTheDocument();
  });

  it("keeps non-story content out of the route shell", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: inactiveContentViewModel }),
    );
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockReturnValue({
      storyPage: storyPageViewModels[0],
      isLoading: false,
      problem: null,
    });
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute("/contents/4/story-pages");

    expect(
      screen.getByRole("heading", { name: /story pages are unavailable/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/4");
    expect(
      screen.queryByText(/story page collection/i),
    ).not.toBeInTheDocument();
  });

  it("opens the page editor with parent language workspaces", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockReturnValue({
      storyPage: storyPageViewModels[0],
      isLoading: false,
      problem: null,
    });
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    fireEvent.click(
      screen.getByRole("button", { name: /edit page 1/i }),
    );

    expect(
      screen.getByRole("heading", { name: /edit story page/i }),
    ).toBeInTheDocument();
    expect(screen.getByText("Localized page workspaces")).toBeInTheDocument();
    expect(screen.getAllByText(/english/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/turkish/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/illustration asset/i).length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByRole("button", { name: /save page localization/i }).length,
    ).toBeGreaterThan(0);
  });

  it("opens the page editor focused on the locale requested by the content detail route", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockReturnValue({
      storyPage: storyPageViewModels[0],
      isLoading: false,
      problem: null,
    });
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute("/contents/1/story-pages?language=tr");

    fireEvent.click(
      screen.getByRole("button", { name: /edit page 1/i }),
    );

    expect(screen.getByText(/parent locale: aksam bahcesi/i)).toBeVisible();
  });
});
