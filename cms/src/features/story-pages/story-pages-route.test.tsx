import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { StoryPagesRoute } from "@/app/routes/story-pages";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

const contentDetailHookMocks = vi.hoisted(() => ({
  useContentDetail: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentDetailHookMocks.useContentDetail,
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

    renderStoryRoute();

    expect(
      screen.getByRole("heading", { name: /story pages for evening garden/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/1");
    expect(
      screen.getByRole("button", { name: /add story page/i }),
    ).toBeDisabled();
    expect(screen.getByText(/2 story pages reserved/i)).toBeInTheDocument();
    expect(screen.getByText(/story page workspace ready/i)).toBeInTheDocument();
  });

  it("keeps non-story content out of the route shell", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: inactiveContentViewModel }),
    );

    renderStoryRoute("/contents/4/story-pages");

    expect(
      screen.getByRole("heading", { name: /story pages are unavailable/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/4");
    expect(
      screen.queryByText(/story page workspace ready/i),
    ).not.toBeInTheDocument();
  });
});
