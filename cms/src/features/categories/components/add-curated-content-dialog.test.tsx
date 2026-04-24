import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  categoryCurationItemViewModels,
  eligibleCategoryContentViewModels,
  featuredSleepCategoryViewModel,
  featuredSleepEnglishLocalizationViewModel,
} from "@/features/categories/test/fixtures";

import { AddCuratedContentDialog } from "./add-curated-content-dialog";

const eligibleContentsQueryMocks = vi.hoisted(() => ({
  useEligibleCategoryContents: vi.fn(),
}));

const curationActionMocks = vi.hoisted(() => ({
  useCategoryCurationActions: vi.fn(),
}));

vi.mock("@/features/categories/queries/use-eligible-category-contents", () => ({
  useEligibleCategoryContents:
    eligibleContentsQueryMocks.useEligibleCategoryContents,
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
  eligibleContentsQueryMocks.useEligibleCategoryContents.mockReset();
  curationActionMocks.useCategoryCurationActions.mockReset();

  eligibleContentsQueryMocks.useEligibleCategoryContents.mockReturnValue({
    items: eligibleCategoryContentViewModels,
    isLoading: false,
    problem: null,
  });
  curationActionMocks.useCategoryCurationActions.mockReturnValue({
    addCuratedContent: makeMutationState(),
  });
});

describe("AddCuratedContentDialog", () => {
  it("loads eligible content from the dedicated query and submits the selected content id", async () => {
    const addCuratedContent = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        categoryId: 7,
        languageCode: "en",
        contentId: 11,
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
      eligibleContentsQueryMocks.useEligibleCategoryContents,
    ).toHaveBeenCalledWith({
      categoryId: 7,
      languageCode: "en",
      search: "",
      enabled: true,
    });
    expect(screen.queryByLabelText(/content id/i)).not.toBeInTheDocument();
    expect(screen.getByText(/starry forest/i)).toBeVisible();
    expect(screen.getByText(/story\.starry-forest/i)).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: /starry forest/i }));
    expect(screen.getAllByText(/starry forest/i)).toHaveLength(2);
    fireEvent.click(
      screen.getByRole("button", { name: /^add curated content$/i }),
    );

    await waitFor(() => {
      expect(addCuratedContent.mutateAsync).toHaveBeenCalledWith({
        contentId: 11,
        displayOrder: 0,
      });
    });
  });

  it("blocks duplicate content ids already present in the current curation lane", async () => {
    const addCuratedContent = makeMutationState();
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      addCuratedContent,
    });
    eligibleContentsQueryMocks.useEligibleCategoryContents.mockReturnValue({
      items: [
        {
          ...eligibleCategoryContentViewModels[0]!,
          contentId: 1,
          externalKey: "story.evening-garden",
          localizedTitle: "Evening Garden",
        },
      ],
      isLoading: false,
      problem: null,
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

    fireEvent.click(screen.getByRole("button", { name: /evening garden/i }));
    fireEvent.change(screen.getByLabelText(/display order/i), {
      target: { value: "3" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /^add curated content$/i }),
    );

    expect(
      await screen.findByText(/already in the current curation lane/i),
    ).toBeVisible();
    expect(addCuratedContent.mutateAsync).not.toHaveBeenCalled();
  });

  it("renders an empty state when no eligible content is available", () => {
    eligibleContentsQueryMocks.useEligibleCategoryContents.mockReturnValue({
      items: [],
      isLoading: false,
      problem: null,
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

    expect(screen.getByText(/no eligible content found/i)).toBeVisible();
  });
});
