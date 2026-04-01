import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  contentContributorViewModels,
  contributorViewModels,
  globalContentContributorViewModel,
} from "@/features/contributors/test/fixtures";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { AssignContributorDialog } from "./assign-contributor-dialog";

const contributorQueryMocks = vi.hoisted(() => ({
  useContributors: vi.fn(),
}));
const contributorActionMocks = vi.hoisted(() => ({
  useContributorActions: vi.fn(),
}));

vi.mock("@/features/contributors/queries/use-contributors", () => ({
  useContributors: contributorQueryMocks.useContributors,
}));

vi.mock("@/features/contributors/mutations/use-contributor-actions", () => ({
  useContributorActions: contributorActionMocks.useContributorActions,
}));

function createWrapper() {
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

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

function makeAssignMutation(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  HTMLElement.prototype.scrollIntoView = vi.fn();
  contributorQueryMocks.useContributors.mockReset();
  contributorActionMocks.useContributorActions.mockReset();
  contributorQueryMocks.useContributors.mockReturnValue({
    contributors: contributorViewModels,
    limit: 12,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  });
  contributorActionMocks.useContributorActions.mockReturnValue({
    createContributor: makeAssignMutation(),
    renameContributor: makeAssignMutation(),
    assignContributor: makeAssignMutation(),
    isPending: false,
  });
});

describe("AssignContributorDialog", () => {
  it("shows an empty state when the recent contributor registry is empty", () => {
    contributorQueryMocks.useContributors.mockReturnValue({
      contributors: [],
      limit: 12,
      isLoading: false,
      isFetching: false,
      problem: null,
      refetch: vi.fn(),
    });

    render(
      <AssignContributorDialog
        content={storyContentViewModel}
        existingAssignments={[]}
        open
        onOpenChange={vi.fn()}
      />,
      { wrapper: createWrapper() },
    );

    expect(
      screen.getByRole("heading", { name: /no contributors available/i }),
    ).toBeInTheDocument();
  });

  it("submits a content contributor assignment with role, language, credit name, and sort order", async () => {
    const assignMutation = makeAssignMutation({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 1,
        contributorId: 11,
        contributorDisplayName: "Annie Case",
        role: "AUTHOR",
        languageCode: "en",
        creditName: "A. Case",
        sortOrder: 2,
      }),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeAssignMutation(),
      renameContributor: makeAssignMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    render(
      <AssignContributorDialog
        content={storyContentViewModel}
        existingAssignments={[]}
        open
        onOpenChange={vi.fn()}
      />,
      { wrapper: createWrapper() },
    );

    fireEvent.click(screen.getByLabelText(/^contributor$/i));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Annie Case"),
    );
    fireEvent.click(screen.getByLabelText(/contributor scope/i));
    fireEvent.click(within(screen.getByRole("listbox")).getByText("English"));
    fireEvent.change(screen.getByLabelText(/sort order/i), {
      target: { value: "2" },
    });
    fireEvent.change(screen.getByLabelText(/credit name/i), {
      target: { value: " A. Case " },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    await waitFor(() => {
      expect(assignMutation.mutateAsync).toHaveBeenCalledWith({
        contentId: 1,
        values: {
          contributorId: 11,
          role: "AUTHOR",
          languageCode: "en",
          creditName: "A. Case",
          sortOrder: 2,
        },
      });
    });
  });

  it("submits a global assignment with languageCode null", async () => {
    const assignMutation = makeAssignMutation({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 4,
        contributorId: 13,
        contributorDisplayName: "Sena Yildiz",
        role: "ILLUSTRATOR",
        languageCode: null,
        creditName: null,
        sortOrder: 0,
      }),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeAssignMutation(),
      renameContributor: makeAssignMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    render(
      <AssignContributorDialog
        content={inactiveContentViewModel}
        existingAssignments={[]}
        open
        onOpenChange={vi.fn()}
      />,
      { wrapper: createWrapper() },
    );

    expect(
      screen.getByText(
        /all languages creates one localization-independent credit/i,
      ),
    ).toBeVisible();

    fireEvent.click(screen.getByLabelText(/^contributor$/i));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Sena Yildiz"),
    );
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    await waitFor(() => {
      expect(assignMutation.mutateAsync).toHaveBeenCalledWith({
        contentId: 4,
        values: {
          contributorId: 13,
          role: "AUTHOR",
          languageCode: null,
          creditName: null,
          sortOrder: 0,
        },
      });
    });
  });

  it("blocks duplicate role/language credits using current-session assignments", async () => {
    const assignMutation = makeAssignMutation();
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeAssignMutation(),
      renameContributor: makeAssignMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    render(
      <AssignContributorDialog
        content={storyContentViewModel}
        existingAssignments={[contentContributorViewModels[0]!]}
        open
        onOpenChange={vi.fn()}
      />,
      { wrapper: createWrapper() },
    );

    fireEvent.click(screen.getByLabelText(/^contributor$/i));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Annie Case"),
    );
    fireEvent.click(screen.getByLabelText(/contributor scope/i));
    fireEvent.click(within(screen.getByRole("listbox")).getByText("English"));
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    expect(
      await screen.findByText(/already has a author credit in english/i),
    ).toBeVisible();
    expect(assignMutation.mutateAsync).not.toHaveBeenCalled();
  });

  it("blocks duplicate global credits using the all languages scope", async () => {
    const assignMutation = makeAssignMutation();
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeAssignMutation(),
      renameContributor: makeAssignMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    render(
      <AssignContributorDialog
        content={inactiveContentViewModel}
        existingAssignments={[globalContentContributorViewModel]}
        open
        onOpenChange={vi.fn()}
      />,
      { wrapper: createWrapper() },
    );

    fireEvent.click(screen.getByLabelText(/^contributor$/i));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Sena Yildiz"),
    );
    fireEvent.click(screen.getByLabelText(/contributor role/i));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Illustrator"),
    );
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    expect(
      await screen.findByText(
        /already has a illustrator credit in all languages/i,
      ),
    ).toBeVisible();
    expect(assignMutation.mutateAsync).not.toHaveBeenCalled();
  });
});
