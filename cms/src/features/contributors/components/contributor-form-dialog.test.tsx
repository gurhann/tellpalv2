import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { ContributorFormDialog } from "./contributor-form-dialog";

const contributorActionMocks = vi.hoisted(() => ({
  useContributorActions: vi.fn(),
}));

vi.mock("@/features/contributors/mutations/use-contributor-actions", () => ({
  useContributorActions: contributorActionMocks.useContributorActions,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected contributor conflict",
    ...overrides,
  };
}

function makeApiClientError(problem: ApiProblemDetail) {
  return new ApiClientError(
    problem,
    new Response(JSON.stringify(problem), {
      status: problem.status,
      statusText: problem.title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    }),
  );
}

function makeMutationState(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  contributorActionMocks.useContributorActions.mockReset();
  contributorActionMocks.useContributorActions.mockReturnValue({
    createContributor: makeMutationState(),
    renameContributor: makeMutationState(),
    isPending: false,
  });
});

describe("ContributorFormDialog", () => {
  it("validates display name before submit in create mode", async () => {
    const createMutation = makeMutationState();
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: createMutation,
      renameContributor: makeMutationState(),
      isPending: false,
    });

    render(<ContributorFormDialog mode="create" open onOpenChange={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/display name/i), {
      target: { value: "   " },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /create contributor/i }),
    );

    expect(
      await screen.findByText("Display name is required."),
    ).toBeInTheDocument();
    expect(createMutation.mutateAsync).not.toHaveBeenCalled();
  });

  it("submits trimmed display names when renaming a contributor", async () => {
    const renameMutation = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        contributorId: 11,
        displayName: "Annie Case Updated",
      }),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeMutationState(),
      renameContributor: renameMutation,
      isPending: false,
    });

    render(
      <ContributorFormDialog
        contributor={{
          id: 11,
          displayName: "Annie Case",
          initials: "AC",
        }}
        mode="rename"
        open
        onOpenChange={vi.fn()}
      />,
    );

    fireEvent.change(screen.getByLabelText(/display name/i), {
      target: { value: " Annie Case Updated " },
    });
    fireEvent.click(screen.getByRole("button", { name: /save rename/i }));

    await waitFor(() => {
      expect(renameMutation.mutateAsync).toHaveBeenCalledWith({
        contributorId: 11,
        values: {
          displayName: "Annie Case Updated",
        },
      });
    });
  });

  it("shows a 404 contributor problem inline during rename", async () => {
    const renameMutation = makeMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            status: 404,
            title: "Contributor not found",
            detail: "Contributor 11 was not found.",
            errorCode: "contributor_not_found",
          }),
        ),
      ),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: makeMutationState(),
      renameContributor: renameMutation,
      isPending: false,
    });

    render(
      <ContributorFormDialog
        contributor={{
          id: 11,
          displayName: "Annie Case",
          initials: "AC",
        }}
        mode="rename"
        open
        onOpenChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /save rename/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Contributor not found",
    );
  });

  it("shows a conflict problem inline during create", async () => {
    const createMutation = makeMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            status: 409,
            title: "Contributor conflict",
            detail: "Contributor display name already exists.",
          }),
        ),
      ),
    });
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: createMutation,
      renameContributor: makeMutationState(),
      isPending: false,
    });

    render(<ContributorFormDialog mode="create" open onOpenChange={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/display name/i), {
      target: { value: "Annie Case" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /create contributor/i }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Contributor display name already exists.",
    );
  });
});
