import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { CategoryLocalizationForm } from "./category-localization-form";

const localizationActionHookMock = vi.hoisted(() => ({
  useCategoryLocalizationActions: vi.fn(),
}));

const recentImageAssetHookMocks = vi.hoisted(() => ({
  useRecentImageAssets: vi.fn(),
}));

const illustrationValidationMocks = vi.hoisted(() => ({
  validateIllustrationAssetId: vi.fn(),
}));

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock(
  "@/features/categories/mutations/use-category-localization-actions",
  () => ({
    useCategoryLocalizationActions:
      localizationActionHookMock.useCategoryLocalizationActions,
  }),
);

vi.mock("@/features/story-pages/queries/use-recent-image-assets", () => ({
  useRecentImageAssets: recentImageAssetHookMocks.useRecentImageAssets,
}));

vi.mock("@/features/story-pages/lib/illustration-asset-validation", () => ({
  validateIllustrationAssetId:
    illustrationValidationMocks.validateIllustrationAssetId,
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected localization conflict",
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

function makeLocalizationMutationState(
  overrides: Record<string, unknown> = {},
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
  localizationActionHookMock.useCategoryLocalizationActions.mockReset();
  recentImageAssetHookMocks.useRecentImageAssets.mockReset();
  illustrationValidationMocks.validateIllustrationAssetId.mockReset();
  assetDetailHookMock.useAssetDetail.mockReset();

  localizationActionHookMock.useCategoryLocalizationActions.mockReturnValue({
    saveLocalization: makeLocalizationMutationState(),
  });
  recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
    assets: [{ assetId: 4 }, { assetId: 5 }],
    isLoading: false,
  });
  illustrationValidationMocks.validateIllustrationAssetId.mockResolvedValue(
    null,
  );
  assetDetailHookMock.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
  });
});

describe("CategoryLocalizationForm", () => {
  it("validates required fields before submit", async () => {
    render(
      <CategoryLocalizationForm
        availableLanguages={[{ code: "en", label: "English" }]}
        categoryId={7}
        initialValues={{
          languageCode: "en",
          name: "",
          description: null,
          imageMediaId: null,
          status: "DRAFT",
          publishedAt: null,
        }}
        mode="create"
      />,
    );

    fireEvent.click(
      screen.getByRole("button", { name: /create localization/i }),
    );

    expect(await screen.findByText("Name is required.")).toBeInTheDocument();
  });

  it("maps duplicate localization conflicts onto the language field", async () => {
    const mutationState = makeLocalizationMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            title: "Localization conflict",
            detail: "Category localization already exists",
            errorCode: "category_localization_exists",
          }),
        ),
      ),
    });
    localizationActionHookMock.useCategoryLocalizationActions.mockReturnValue({
      saveLocalization: mutationState,
    });

    render(
      <CategoryLocalizationForm
        availableLanguages={[{ code: "en", label: "English" }]}
        categoryId={7}
        initialValues={{
          languageCode: "en",
          name: "Featured Sleep",
          description: null,
          imageMediaId: null,
          status: "DRAFT",
          publishedAt: null,
        }}
        mode="create"
      />,
    );

    fireEvent.click(
      screen.getByRole("button", { name: /create localization/i }),
    );

    expect(
      await screen.findByText("Language already exists for this category."),
    ).toBeVisible();
  });

  it("submits update payloads with the current language preserved", async () => {
    const mutationState = makeLocalizationMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        categoryId: 7,
        languageCode: "en",
        name: "Featured Sleep Updated",
        description: "Updated description",
        imageMediaId: 4,
        status: "DRAFT",
        publishedAt: null,
        published: false,
      }),
    });
    localizationActionHookMock.useCategoryLocalizationActions.mockReturnValue({
      saveLocalization: mutationState,
    });

    render(
      <CategoryLocalizationForm
        availableLanguages={[{ code: "en", label: "English" }]}
        categoryId={7}
        initialValues={{
          languageCode: "en",
          name: "Featured Sleep",
          description: null,
          imageMediaId: 4,
          status: "DRAFT",
          publishedAt: null,
        }}
        localization={{
          categoryId: 7,
          languageCode: "en",
          languageLabel: "English",
          name: "Featured Sleep",
          description: null,
          imageAssetId: 4,
          status: "DRAFT",
          statusLabel: "Draft",
          publishedAt: null,
          isPublished: false,
          hasImage: true,
        }}
        mode="update"
      />,
    );

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Featured Sleep Updated" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save localization/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        mode: "update",
        values: {
          languageCode: "en",
          name: "Featured Sleep Updated",
          description: null,
          imageMediaId: 4,
          status: "DRAFT",
          publishedAt: null,
        },
      });
    });
  });
});
