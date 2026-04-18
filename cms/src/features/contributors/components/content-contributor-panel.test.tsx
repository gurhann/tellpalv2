import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import {
  contentContributorViewModels,
  globalContentContributorViewModel,
} from "@/features/contributors/test/fixtures";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { ContentContributorPanel } from "./content-contributor-panel";

const contributorHookMocks = vi.hoisted(() => ({
  useContentContributorAssignments: vi.fn(),
}));

vi.mock(
  "@/features/contributors/queries/use-content-contributor-assignments",
  () => ({
    useContentContributorAssignments:
      contributorHookMocks.useContentContributorAssignments,
  }),
);

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };
}

describe("ContentContributorPanel", () => {
  it("renders contributor assignments loaded from the backend query", () => {
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: [
        globalContentContributorViewModel,
        ...contentContributorViewModels,
      ],
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper(),
    });

    expect(screen.getByText(/contributor assignments/i)).toBeVisible();
    expect(screen.getAllByText("Annie Case")).toHaveLength(2);
    expect(screen.getByText("M. Rivers")).toBeVisible();
    expect(screen.getByText("Author")).toBeVisible();
    expect(screen.getByText("Turkish")).toBeVisible();
    expect(screen.getByText("All languages")).toBeVisible();
    expect(
      screen.getAllByRole("button", { name: /unassign/i }),
    ).toHaveLength(3);
  });

  it("keeps assignment enabled when the content has no localizations", () => {
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: [],
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={inactiveContentViewModel} />, {
      wrapper: createWrapper(),
    });

    expect(
      screen.getByRole("button", { name: /assign contributor/i }),
    ).toBeEnabled();
    expect(
      screen.getByText(
        /global or localized author, illustrator, narrator, or musician credits/i,
      ),
    ).toBeVisible();
  });
});
