import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  categoryCurationItemViewModels,
  featuredSleepCategoryViewModel,
  featuredSleepEnglishLocalizationViewModel,
  featuredSleepTurkishLocalizationViewModel,
} from "@/features/categories/test/fixtures";

import { CuratedContentList } from "./curated-content-list";

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
    removeCuratedContent: makeMutationState(),
    reorderCuratedContent: makeMutationState(),
  });
});

describe("CuratedContentList", () => {
  it("renders one editable list surface with content identity and remove actions", () => {
    render(
      <CuratedContentList
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepEnglishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("list", { name: /english curated content list/i }),
    ).toBeVisible();
    expect(screen.getByText(/^Evening Garden$/i)).toBeVisible();
    expect(screen.getByText(/^Moonlight Meadow$/i)).toBeVisible();
    expect(screen.getAllByRole("button", { name: /^remove$/i })).toHaveLength(
      2,
    );
  });

  it("confirms and removes one curated row", async () => {
    const removeCuratedContent = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue(undefined),
    });
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      removeCuratedContent,
      reorderCuratedContent: makeMutationState(),
    });

    render(
      <CuratedContentList
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepEnglishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /^remove$/i })[0]!);

    expect(
      await screen.findByRole("heading", { name: /remove curated content/i }),
    ).toBeVisible();
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText(/^Evening Garden$/i)).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: /^remove content$/i }));

    await waitFor(() => {
      expect(removeCuratedContent.mutateAsync).toHaveBeenCalledWith({
        contentId: 1,
      });
    });
  });

  it("reorders curated rows through the keyboard drag handle", async () => {
    const reorderCuratedContent = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue(categoryCurationItemViewModels),
    });
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      removeCuratedContent: makeMutationState(),
      reorderCuratedContent,
    });

    render(
      <CuratedContentList
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepEnglishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    const handle = screen.getByRole("button", {
      name: /reorder moonlight meadow/i,
    });

    fireEvent.keyDown(handle, { key: "ArrowUp", code: "ArrowUp" });

    await waitFor(() => {
      expect(reorderCuratedContent.mutateAsync).toHaveBeenCalledWith({
        items: [
          {
            ...categoryCurationItemViewModels[1],
            displayOrder: 0,
          },
          {
            ...categoryCurationItemViewModels[0],
            displayOrder: 1,
          },
        ],
      });
    });
  });

  it("keeps drag handles disabled for unpublished localizations", () => {
    render(
      <CuratedContentList
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepTurkishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("button", { name: /reorder evening garden/i }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: /reorder moonlight meadow/i }),
    ).toBeDisabled();
  });
});
