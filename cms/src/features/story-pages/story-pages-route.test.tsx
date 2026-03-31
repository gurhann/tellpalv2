import { render, screen } from "@testing-library/react";
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
}));
const storyPageMutationMocks = vi.hoisted(() => ({
  useStoryPageActions: vi.fn(),
}));
const recentImageAssetHookMocks = vi.hoisted(() => ({
  useRecentImageAssets: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentDetailHookMocks.useContentDetail,
}));

vi.mock("@/features/story-pages/queries/use-story-pages", () => ({
  useStoryPages: storyPageHookMocks.useStoryPages,
}));

vi.mock("@/features/story-pages/mutations/use-story-page-actions", () => ({
  useStoryPageActions: storyPageMutationMocks.useStoryPageActions,
}));

vi.mock("@/features/story-pages/queries/use-recent-image-assets", () => ({
  useRecentImageAssets: recentImageAssetHookMocks.useRecentImageAssets,
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
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
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
    expect(screen.getByText(/2 story pages live/i)).toBeInTheDocument();
    expect(screen.getByText(/page 1/i)).toBeInTheDocument();
    expect(screen.getByText(/asset #41/i)).toBeInTheDocument();
    expect(screen.getAllByText(/story page collection/i)).not.toHaveLength(0);
  });

  it("keeps non-story content out of the route shell", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: inactiveContentViewModel }),
    );
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
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
});
