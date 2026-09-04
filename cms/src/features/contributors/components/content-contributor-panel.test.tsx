import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
const contributorActionMocks = vi.hoisted(() => ({
  useContributorActions: vi.fn(),
}));

vi.mock(
  "@/features/contributors/queries/use-content-contributor-assignments",
  () => ({
    useContentContributorAssignments:
      contributorHookMocks.useContentContributorAssignments,
  }),
);
vi.mock("@/features/contributors/mutations/use-contributor-actions", () => ({
  useContributorActions: contributorActionMocks.useContributorActions,
}));

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
  beforeEach(() => {
    contributorActionMocks.useContributorActions.mockReturnValue({
      reorderContributors: { mutateAsync: vi.fn().mockResolvedValue([]) },
      unassignContributor: { isPending: false, mutateAsync: vi.fn() },
      updateContributor: {
        isPending: false,
        mutateAsync: vi.fn().mockResolvedValue(contentContributorViewModels[0]),
      },
      isPending: false,
    });
  });

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

    expect(
      screen.queryByRole("heading", { name: /contributor assignments/i }),
    ).not.toBeInTheDocument();
    expect(screen.getAllByText("Annie Case")).toHaveLength(2);
    expect(screen.getByText("M. Rivers")).toBeVisible();
    expect(screen.getByText("Author")).toBeVisible();
    expect(screen.getByText("Turkish")).toBeVisible();
    expect(screen.getByText("All languages")).toBeVisible();
    expect(screen.getAllByRole("button", { name: /unassign/i })).toHaveLength(
      3,
    );
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

    expect(screen.getByRole("heading", { name: "Author" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Illustrator" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Narrator" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Musician" })).toBeVisible();
    expect(screen.getAllByRole("button", { name: /add$/i })).toHaveLength(4);
  });

  it("groups assignments by role and reorders only within the matching scope", async () => {
    const reorder = vi.fn().mockResolvedValue([
      { ...contentContributorViewModels[1], sortOrder: 0 },
      { ...contentContributorViewModels[0], sortOrder: 1 },
    ]);
    contributorActionMocks.useContributorActions.mockReturnValue({
      reorderContributors: { mutateAsync: reorder },
      unassignContributor: { isPending: false, mutateAsync: vi.fn() },
      updateContributor: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    const groupedAssignments = [
      contentContributorViewModels[0]!,
      {
        ...contentContributorViewModels[0]!,
        assignmentId: 999,
        contributorId: 99,
        displayName: "Second Author",
        effectiveCreditName: "Second Author",
      },
      ...contentContributorViewModels.slice(1),
    ];
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: groupedAssignments,
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper(),
    });

    expect(screen.getByRole("heading", { name: "Author" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Narrator" })).toBeVisible();
    fireEvent.click(
      screen.getAllByRole("button", { name: /move assignment down/i })[0]!,
    );

    await waitFor(() =>
      expect(reorder).toHaveBeenCalledWith(
        expect.objectContaining({
          role: "AUTHOR",
          languageCode: "en",
          assignmentIds: [999, 101],
        }),
      ),
    );
  });

  it("restores the previous order and reports failed reorder", async () => {
    const reorder = vi.fn().mockRejectedValue(new Error("network"));
    contributorActionMocks.useContributorActions.mockReturnValue({
      reorderContributors: { mutateAsync: reorder, isPending: false },
      unassignContributor: { isPending: false, mutateAsync: vi.fn() },
      updateContributor: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    const groupedAssignments = [
      contentContributorViewModels[0]!,
      {
        ...contentContributorViewModels[0]!,
        assignmentId: 999,
        contributorId: 99,
        displayName: "Second Author",
        effectiveCreditName: "Second Author",
      },
    ];
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: groupedAssignments,
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper(),
    });
    fireEvent.click(
      screen.getAllByRole("button", { name: /move assignment down/i })[0]!,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /previous order was restored/i,
    );
    const authorSection = screen.getByRole("region", { name: "Author" });
    const sectionText = authorSection.textContent ?? "";
    expect(sectionText.indexOf("Annie Case")).toBeLessThan(
      sectionText.indexOf("Second Author"),
    );
  });

  it("opens the assignment editor with the existing credit values", async () => {
    const update = vi.fn().mockResolvedValue(contentContributorViewModels[1]);
    contributorActionMocks.useContributorActions.mockReturnValue({
      reorderContributors: { mutateAsync: vi.fn().mockResolvedValue([]) },
      unassignContributor: { isPending: false, mutateAsync: vi.fn() },
      updateContributor: { isPending: false, mutateAsync: update },
      isPending: false,
    });
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: [
        ...contentContributorViewModels,
        globalContentContributorViewModel,
      ],
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper(),
    });

    fireEvent.click(
      screen.getAllByRole("button", {
        name: /edit contributor assignment/i,
      })[2]!,
    );

    expect(
      await screen.findByRole("heading", {
        name: /edit contributor assignment/i,
      }),
    ).toBeVisible();
    expect(screen.getByDisplayValue("M. Rivers")).toBeVisible();
    expect(screen.getAllByText("Milo Rivers").length).toBeGreaterThanOrEqual(2);

    fireEvent.change(screen.getByDisplayValue("M. Rivers"), {
      target: { value: "Milo Updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save assignment/i }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        expect.objectContaining({
          contentId: 1,
          assignmentId: 102,
          values: expect.objectContaining({ creditName: "Milo Updated" }),
        }),
      ),
    );
  });

  it("keeps the assignment editor open when the update fails", async () => {
    const update = vi.fn().mockRejectedValue(new Error("network"));
    contributorActionMocks.useContributorActions.mockReturnValue({
      reorderContributors: { mutateAsync: vi.fn().mockResolvedValue([]) },
      unassignContributor: { isPending: false, mutateAsync: vi.fn() },
      updateContributor: { isPending: false, mutateAsync: update },
      isPending: false,
    });
    contributorHookMocks.useContentContributorAssignments.mockReturnValue({
      assignments: contentContributorViewModels,
      isLoading: false,
      problem: null,
    });

    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper(),
    });
    fireEvent.click(
      screen.getAllByRole("button", {
        name: /edit contributor assignment/i,
      })[1]!,
    );
    fireEvent.click(screen.getByRole("button", { name: /save assignment/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/network/i);
    expect(
      screen.getByRole("heading", { name: /edit contributor assignment/i }),
    ).toBeVisible();
  });
});
