import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import {
  meditationContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";

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

describe("StoryContentGuard", () => {
  it("shows an invalid route state when the parent content id is missing", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: null }),
    );

    render(
      <MemoryRouter>
        <StoryContentGuard contentId={null}>
          {() => <div>story shell</div>}
        </StoryContentGuard>
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: /invalid story page route/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content registry/i }),
    ).toHaveAttribute("href", "/contents");
  });

  it("renders the child workspace when the parent content supports story pages", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());

    render(
      <MemoryRouter>
        <StoryContentGuard contentId={storyContentViewModel.summary.id}>
          {(content) => <div>{content.summary.externalKey}</div>}
        </StoryContentGuard>
      </MemoryRouter>,
    );

    expect(screen.getByText("story.evening-garden")).toBeInTheDocument();
  });

  it("blocks non-story parent content and links back to content detail", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: meditationContentViewModel }),
    );

    render(
      <MemoryRouter>
        <StoryContentGuard contentId={meditationContentViewModel.summary.id}>
          {() => <div>story shell</div>}
        </StoryContentGuard>
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: /story pages are unavailable/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/meditation records do not use story pages/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/2");
  });

  it("surfaces API problems and retries the content query", () => {
    const refetch = vi.fn();

    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({
        content: null,
        problem: {
          type: "about:blank",
          title: "Request failed",
          status: 500,
          detail: "Backend unavailable.",
        },
        refetch,
      }),
    );

    render(
      <MemoryRouter>
        <StoryContentGuard contentId={storyContentViewModel.summary.id}>
          {() => <div>story shell</div>}
        </StoryContentGuard>
      </MemoryRouter>,
    );

    expect(screen.getByRole("alert")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /retry/i }));
    expect(refetch).toHaveBeenCalledTimes(1);
  });
});
