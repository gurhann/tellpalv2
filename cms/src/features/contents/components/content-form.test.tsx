import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { ContentForm } from "./content-form";

const saveContentHookMock = vi.hoisted(() => ({
  useSaveContent: vi.fn(),
}));

vi.mock("@/features/contents/mutations/use-save-content", () => ({
  useSaveContent: saveContentHookMock.useSaveContent,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected content conflict",
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

function makeSaveMutationState(
  overrides: Partial<ReturnType<typeof vi.fn>> = {},
) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  saveContentHookMock.useSaveContent.mockReset();
  saveContentHookMock.useSaveContent.mockReturnValue(makeSaveMutationState());
});

describe("ContentForm", () => {
  it("validates the external key before submit", async () => {
    const mutationState = makeSaveMutationState();
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "",
          ageRange: null,
          active: true,
        }}
        mode="create"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /create content/i }));

    expect(
      await screen.findByText("External key is required."),
    ).toBeInTheDocument();
    expect(mutationState.mutateAsync).not.toHaveBeenCalled();
  });

  it("renders story and non-story workflow guidance from the selected type", () => {
    const firstRender = render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "story.evening-garden",
          ageRange: 5,
          active: true,
        }}
        mode="create"
      />,
    );

    expect(screen.getByText("Story workflow")).toBeVisible();

    firstRender.unmount();

    render(
      <ContentForm
        initialValues={{
          type: "MEDITATION",
          externalKey: "meditation.rain-room",
          ageRange: 8,
          active: true,
        }}
        mode="update"
      />,
    );

    expect(screen.getByText("Non-story workflow")).toBeVisible();
    expect(
      screen.getByText(/content type is fixed after creation/i),
    ).toBeVisible();
  });

  it("maps duplicate external key conflicts onto the externalKey field", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            title: "External key conflict",
            detail: "Content external key already exists",
            errorCode: "duplicate_external_key",
          }),
        ),
      ),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "",
          ageRange: null,
          active: true,
        }}
        mode="create"
      />,
    );

    fireEvent.change(screen.getByLabelText(/external key/i), {
      target: { value: "story.evening-garden" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create content/i }));

    expect(
      await screen.findByText("External key is already in use."),
    ).toBeVisible();
  });

  it("submits editable metadata in update mode", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 1,
        type: "STORY",
        externalKey: "story.evening-garden.updated",
        active: false,
        ageRange: 6,
        pageCount: 2,
      }),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        contentId={1}
        initialValues={{
          type: "STORY",
          externalKey: "story.evening-garden",
          ageRange: 5,
          active: true,
        }}
        mode="update"
      />,
    );

    fireEvent.change(screen.getByLabelText(/external key/i), {
      target: { value: "story.evening-garden.updated" },
    });
    fireEvent.change(screen.getByLabelText(/age range/i), {
      target: { value: "6" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        type: "STORY",
        externalKey: "story.evening-garden.updated",
        ageRange: 6,
        active: true,
      });
    });
  });
});
