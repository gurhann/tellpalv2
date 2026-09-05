import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  contributorAdminApi,
  type AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  contentContributorViewModels,
  contributorViewModels,
} from "@/features/contributors/test/fixtures";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { ApiClientError } from "@/lib/http/client";

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

afterEach(() => {
  vi.restoreAllMocks();
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

  it("keeps long picker results in a bounded scroll region", () => {
    renderDialog();

    expect(screen.getByTestId("contributor-picker-results")).toHaveClass(
      "max-h-56",
      "overflow-y-auto",
    );
    fireEvent.click(screen.getByRole("option", { name: /annie case/i }));
    expect(
      screen.getByRole("button", { name: /assign contributor/i }),
    ).toBeInTheDocument();
  });

  it.each([390, 768, 1280, 1440])(
    "keeps long contributor names inside the picker at %i px",
    (width) => {
      Object.defineProperty(window, "innerWidth", {
        configurable: true,
        value: width,
      });
      contributorQueryMocks.useContributorPicker.mockReturnValue({
        contributors: [
          {
            id: 77,
            displayName:
              "A contributor name with an intentionally unbroken identifier 000000000000000000000000000000000000",
            initials: "AC",
          },
        ],
        isLoading: false,
        isFetching: false,
        problem: null,
        refetch: vi.fn(),
      });

      renderDialog();

      expect(screen.getByTestId("contributor-picker-results")).toHaveClass(
        "min-w-0",
      );
      expect(
        screen.getByRole("option", { name: /intentionally unbroken/i }),
      ).toHaveClass("min-w-0");
      fireEvent.click(
        screen.getByRole("option", { name: /intentionally unbroken/i }),
      );
      expect(
        screen.getByRole("button", { name: /assign contributor/i }),
      ).toBeVisible();
    },
  );

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

  it("requires explicit confirmation before adding a missing role to an existing profile", async () => {
    const duplicateProblem = {
      type: "about:blank",
      title: "Duplicate contributor display name",
      status: 409,
      detail: "Contributor display name already exists",
      errorCode: "duplicate_contributor_display_name",
      existingContributorId: 12,
    };
    const createMutation = makeMutation({
      mutateAsync: vi
        .fn()
        .mockRejectedValue(
          new ApiClientError(
            duplicateProblem,
            new Response(null, { status: 409, statusText: "Conflict" }),
          ),
        ),
    });
    const renameMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({
        contributorId: 12,
        displayName: "Milo Rivers",
        roles: ["NARRATOR", "AUTHOR"],
      }),
    });
    const assignMutation = makeMutation({
      mutateAsync: vi.fn().mockResolvedValue({ contentId: 1 }),
    });
    const existing: AdminContributorResponse = {
      contributorId: 12,
      displayName: "Milo Rivers",
      roles: ["NARRATOR"],
    };
    vi.spyOn(contributorAdminApi, "getContributor").mockResolvedValue(existing);
    contributorQueryMocks.useContributorPicker.mockReturnValue({
      contributors: [],
      isLoading: false,
      isFetching: false,
      problem: null,
      refetch: vi.fn(),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: createMutation,
      renameContributor: renameMutation,
      assignContributor: assignMutation,
      isPending: false,
    });

    renderDialog();
    fireEvent.change(screen.getByLabelText(/search authors/i), {
      target: { value: "Milo Rivers" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create and assign/i }));

    const addRoleButton = await screen.findByRole("button", {
      name: /add role and assign/i,
    });
    expect(contributorAdminApi.getContributor).toHaveBeenCalledWith(12);
    expect(renameMutation.mutateAsync).not.toHaveBeenCalled();
    expect(assignMutation.mutateAsync).not.toHaveBeenCalled();

    fireEvent.click(addRoleButton);

    await waitFor(() =>
      expect(renameMutation.mutateAsync).toHaveBeenCalledWith({
        contributorId: 12,
        values: {
          displayName: "Milo Rivers",
          roles: ["NARRATOR", "AUTHOR"],
        },
      }),
    );
    expect(assignMutation.mutateAsync).toHaveBeenCalledWith({
      contentId: 1,
      values: expect.objectContaining({ contributorId: 12, role: "AUTHOR" }),
    });
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

  it("uses the active content language for narrator assignments", () => {
    renderDialog({ role: "NARRATOR", initialLanguageCode: "tr" });
    fireEvent.click(screen.getByRole("option", { name: /Annie Case/ }));

    expect(screen.getByRole("combobox", { name: /scope/i })).toHaveTextContent(
      "Turkish",
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
