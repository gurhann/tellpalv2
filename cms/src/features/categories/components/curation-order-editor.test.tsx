import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  categoryCurationItemViewModels,
  featuredSleepCategoryViewModel,
  featuredSleepEnglishLocalizationViewModel,
  featuredSleepTurkishLocalizationViewModel,
} from "@/features/categories/test/fixtures";

import { CurationOrderEditor } from "./curation-order-editor";

const curationActionMocks = vi.hoisted(() => ({
  useCategoryCurationActions: vi.fn(),
}));

vi.mock(
  "@/features/categories/mutations/use-category-curation-actions",
  () => ({
    useCategoryCurationActions: curationActionMocks.useCategoryCurationActions,
  }),
);

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
  curationActionMocks.useCategoryCurationActions.mockReset();
  curationActionMocks.useCategoryCurationActions.mockReturnValue({
    updateCuratedContentOrder: makeMutationState(),
  });
});

describe("CurationOrderEditor", () => {
  it("updates display order for a hydrated curation row", async () => {
    const updateCuratedContentOrder = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        categoryId: 7,
        languageCode: "en",
        contentId: 10,
        displayOrder: 1,
      }),
    });
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      updateCuratedContentOrder,
    });

    render(
      <CurationOrderEditor
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        localization={featuredSleepEnglishLocalizationViewModel}
      />,
    );

    expect(screen.getByText(/^Evening Garden$/i)).toBeVisible();
    expect(screen.getByText(/^Moonlight Meadow$/i)).toBeVisible();

    fireEvent.change(screen.getByDisplayValue("2"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getAllByRole("button", { name: /save order/i })[1]!);

    await waitFor(() => {
      expect(updateCuratedContentOrder.mutateAsync).toHaveBeenCalledWith({
        contentId: 10,
        displayOrder: 1,
      });
    });
  });

  it("shows a locked state for unpublished localizations", () => {
    render(
      <CurationOrderEditor
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        localization={featuredSleepTurkishLocalizationViewModel}
      />,
    );

    expect(
      screen.getByText(/publish the turkish category localization/i),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: /save order/i }),
    ).not.toBeInTheDocument();
  });
});
