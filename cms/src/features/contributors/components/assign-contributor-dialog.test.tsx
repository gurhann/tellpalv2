import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  contentContributorViewModels,
  contributorViewModels,
} from "@/features/contributors/test/fixtures";
import { storyContentViewModel } from "@/features/contents/test/fixtures";

import { AssignContributorDialog } from "./assign-contributor-dialog";

const contributorQueryMocks = vi.hoisted(() => ({
  useContributorPicker: vi.fn(),
}));
const contributorActionMocks = vi.hoisted(() => ({
  useContributorActions: vi.fn(),
}));

vi.mock("@/features/contributors/queries/use-contributors", () => ({
  useContributorPicker: contributorQueryMocks.useContributorPicker,
}));

vi.mock("@/features/contributors/mutations/use-contributor-actions", () => ({
  useContributorActions: contributorActionMocks.useContributorActions,
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

function makeMutation(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

function renderDialog(
  overrides: Partial<React.ComponentProps<typeof AssignContributorDialog>> = {},
) {
  return render(
    <AssignContributorDialog
      content={storyContentViewModel}
      existingAssignments={[]}
      open
      onOpenChange={vi.fn()}
      {...overrides}
    />,
    { wrapper: createWrapper() },
  );
}

beforeEach(() => {
  HTMLElement.prototype.scrollIntoView = vi.fn();
  contributorQueryMocks.useContributorPicker.mockReset();
  contributorActionMocks.useContributorActions.mockReset();
  contributorQueryMocks.useContributorPicker.mockReturnValue({
    contributors: contributorViewModels,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  });
  contributorActionMocks.useContributorActions.mockReturnValue({
    createContributor: makeMutation(),
    renameContributor: makeMutation(),
    assignContributor: makeMutation(),
    isPending: false,
  });
});

describe("AssignContributorDialog", () => {
  it("opens a role-scoped database-backed picker", () => {
    renderDialog({ role: "ILLUSTRATOR" });

    expect(contributorQueryMocks.useContributorPicker).toHaveBeenCalledWith({
      role: "ILLUSTRATOR",
      query: "",
      enabled: true,
    });
    expect(
      screen.getByRole("heading", { name: /assign contributor/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Choose an existing Illustrator/i),
    ).toBeInTheDocument();
  });

  it("selects a result row directly and lets the backend assign its order", async () => {
    const assignMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({}),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeMutation(),
      renameContributor: makeMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    renderDialog();
    fireEvent.click(screen.getByRole("option", { name: /annie case/i }));
    fireEvent.change(screen.getByLabelText(/credit name/i), {
      target: { value: " A. Case " },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    await waitFor(() =>
      expect(assignMutation.mutateAsync).toHaveBeenCalledWith({
        contentId: 1,
        values: {
          contributorId: 11,
          role: "AUTHOR",
          languageCode: null,
          creditName: "A. Case",
        },
      }),
    );
  });

  it("offers inline create and automatically assigns the new contributor", async () => {
    const createMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({
        contributorId: 99,
        displayName: "Lina Hart",
        roles: ["AUTHOR"],
      }),
    });
    const assignMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({}),
    });
    contributorQueryMocks.useContributorPicker.mockReturnValue({
      contributors: [],
      isLoading: false,
      isFetching: false,
      problem: null,
      refetch: vi.fn(),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: createMutation,
      renameContributor: makeMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    renderDialog();
    fireEvent.change(screen.getByLabelText(/search authors/i), {
      target: { value: " Lina Hart " },
    });
    fireEvent.click(screen.getByRole("button", { name: /create and assign/i }));

    await waitFor(() =>
      expect(createMutation.mutateAsync).toHaveBeenCalledWith({
        displayName: "Lina Hart",
        roles: ["AUTHOR"],
      }),
    );
    expect(assignMutation.mutateAsync).toHaveBeenCalledWith({
      contentId: 1,
      values: {
        contributorId: 99,
        role: "AUTHOR",
        languageCode: null,
        creditName: null,
      },
    });
  });

  it("keeps the created contributor context and exposes retry after assignment failure", async () => {
    const assignMutation = makeMutation({
      mutateAsync: vi
        .fn()
        .mockRejectedValueOnce(new Error("temporary failure"))
        .mockResolvedValueOnce({}),
    });
    const createMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({
        contributorId: 99,
        displayName: "Lina Hart",
        roles: ["AUTHOR"],
      }),
    });
    contributorQueryMocks.useContributorPicker.mockReturnValue({
      contributors: [],
      isLoading: false,
      isFetching: false,
      problem: null,
      refetch: vi.fn(),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: createMutation,
      renameContributor: makeMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    renderDialog();
    fireEvent.change(screen.getByLabelText(/search authors/i), {
      target: { value: "Lina Hart" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create and assign/i }));

    expect(await screen.findByRole("button", { name: /retry/i })).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: /retry/i }));

    await waitFor(() =>
      expect(assignMutation.mutateAsync).toHaveBeenCalledTimes(2),
    );
  });

  it("defaults narrator assignments to the first content language", () => {
    renderDialog({ role: "NARRATOR" });

    expect(contributorQueryMocks.useContributorPicker).toHaveBeenCalledWith({
      role: "NARRATOR",
      query: "",
      enabled: true,
    });
    fireEvent.click(screen.getByRole("option", { name: /Annie Case/ }));

    expect(screen.getByRole("combobox", { name: /scope/i })).toHaveTextContent(
      "English",
    );
  });

  it("still blocks duplicate role and language assignments", async () => {
    const assignMutation = makeMutation();
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeMutation(),
      renameContributor: makeMutation(),
      assignContributor: assignMutation,
      isPending: false,
    });

    renderDialog({ existingAssignments: [contentContributorViewModels[0]!] });
    fireEvent.click(screen.getByRole("option", { name: /annie case/i }));
    fireEvent.click(screen.getByLabelText(/credit scope/i));
    fireEvent.click(screen.getByRole("option", { name: "English" }));
    fireEvent.click(
      screen.getByRole("button", { name: /assign contributor/i }),
    );

    expect(
      await screen.findByText(/already has a author credit/i),
    ).toBeVisible();
    expect(assignMutation.mutateAsync).not.toHaveBeenCalled();
  });
});
