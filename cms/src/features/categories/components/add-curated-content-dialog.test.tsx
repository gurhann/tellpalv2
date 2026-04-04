import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { contentReadViewModels } from "@/features/contents/test/fixtures";
import {
  categoryCurationItemViewModels,
  featuredSleepCategoryViewModel,
  featuredSleepEnglishLocalizationViewModel,
} from "@/features/categories/test/fixtures";

import { AddCuratedContentDialog } from "./add-curated-content-dialog";

const contentQueryMocks = vi.hoisted(() => ({
  useContentList: vi.fn(),
}));

const curationActionMocks = vi.hoisted(() => ({
  useCategoryCurationActions: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-list", () => ({
  useContentList: contentQueryMocks.useContentList,
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
  contentQueryMocks.useContentList.mockReset();
  curationActionMocks.useCategoryCurationActions.mockReset();

  contentQueryMocks.useContentList.mockReturnValue({
    contents: contentReadViewModels,
    isLoading: false,
    problem: null,
  });
  curationActionMocks.useCategoryCurationActions.mockReturnValue({
    addCuratedContent: makeMutationState(),
  });
});

describe("AddCuratedContentDialog", () => {
  it("filters eligible content by category type and published localization, then submits selected values", async () => {
    const addCuratedContent = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        categoryId: 7,
        languageCode: "en",
        contentId: 1,
        displayOrder: 0,
      }),
    });
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      addCuratedContent,
    });

    render(
      <AddCuratedContentDialog
        category={featuredSleepCategoryViewModel}
        existingItems={[]}
        localization={featuredSleepEnglishLocalizationViewModel}
        open
        onOpenChange={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("button", { name: /#1 story\.evening-garden/i }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: /#2 meditation\.rain-room/i }),
    ).not.toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: /#1 story\.evening-garden/i }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: /^add curated content$/i }),
    );

    await waitFor(() => {
      expect(addCuratedContent.mutateAsync).toHaveBeenCalledWith({
        contentId: 1,
        displayOrder: 0,
      });
    });
  });

  it("blocks duplicate content ids already present in the current session cache", async () => {
    const addCuratedContent = makeMutationState();
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      addCuratedContent,
    });

    render(
      <AddCuratedContentDialog
        category={featuredSleepCategoryViewModel}
        existingItems={[categoryCurationItemViewModels[0]]}
        localization={featuredSleepEnglishLocalizationViewModel}
        open
        onOpenChange={vi.fn()}
      />,
    );

    fireEvent.change(screen.getByLabelText(/content id/i), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByLabelText(/display order/i), {
      target: { value: "3" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /^add curated content$/i }),
    );

    expect(
      await screen.findByText(/already in the current session curation set/i),
    ).toBeVisible();
    expect(addCuratedContent.mutateAsync).not.toHaveBeenCalled();
  });
});
