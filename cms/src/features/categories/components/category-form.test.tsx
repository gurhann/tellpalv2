import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { CategoryForm } from "./category-form";

const saveCategoryHookMock = vi.hoisted(() => ({
  useSaveCategory: vi.fn(),
}));

vi.mock("@/features/categories/mutations/use-save-category", () => ({
  useSaveCategory: saveCategoryHookMock.useSaveCategory,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected category conflict",
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

function makeSaveMutationState(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  saveCategoryHookMock.useSaveCategory.mockReset();
  saveCategoryHookMock.useSaveCategory.mockReturnValue(makeSaveMutationState());
});

describe("CategoryForm", () => {
  it("validates the slug before submit", async () => {
    const mutationState = makeSaveMutationState();
    saveCategoryHookMock.useSaveCategory.mockReturnValue(mutationState);

    render(
      <CategoryForm
        initialValues={{
          type: "CONTENT",
          slug: "",
          premium: false,
          active: true,
        }}
        mode="create"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /create category/i }));

    expect(await screen.findByText("Slug is required.")).toBeInTheDocument();
    expect(mutationState.mutateAsync).not.toHaveBeenCalled();
  });

  it("maps duplicate slug conflicts onto the slug field", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            title: "Slug conflict",
            detail: "Category slug already exists",
            errorCode: "duplicate_category_slug",
          }),
        ),
      ),
    });
    saveCategoryHookMock.useSaveCategory.mockReturnValue(mutationState);

    render(
      <CategoryForm
        initialValues={{
          type: "CONTENT",
          slug: "",
          premium: false,
          active: true,
        }}
        mode="create"
      />,
    );

    fireEvent.change(screen.getByLabelText(/slug/i), {
      target: { value: "featured-sleep" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create category/i }));

    expect(await screen.findByText("Slug is already in use.")).toBeVisible();
  });

  it("submits editable metadata in update mode", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        categoryId: 7,
        type: "CONTENT",
        slug: "featured-sleep-updated",
        premium: true,
        active: false,
      }),
    });
    saveCategoryHookMock.useSaveCategory.mockReturnValue(mutationState);

    render(
      <CategoryForm
        categoryId={7}
        initialValues={{
          type: "CONTENT",
          slug: "featured-sleep",
          premium: false,
          active: true,
        }}
        mode="update"
      />,
    );

    fireEvent.change(screen.getByLabelText(/slug/i), {
      target: { value: "featured-sleep-updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        type: "CONTENT",
        slug: "featured-sleep-updated",
        premium: false,
        active: true,
      });
    });
  });
});
