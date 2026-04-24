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
} from "@/features/categories/test/fixtures";

import { CurationTable } from "./curation-table";

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
  });
});

describe("CurationTable", () => {
  it("renders hydrated curated rows with content and order columns", () => {
    render(
      <CurationTable
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepEnglishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /content/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /display order/i }),
    ).toBeVisible();
    expect(screen.getByText(/^Evening Garden$/i)).toBeVisible();
    expect(screen.getByText(/^Moonlight Meadow$/i)).toBeVisible();
    expect(
      screen.getByText(/#1\s+·\s+story\.evening-garden\s+·\s+English/i),
    ).toBeVisible();
  });

  it("confirms and removes one curated row", async () => {
    const removeCuratedContent = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue(undefined),
    });
    curationActionMocks.useCategoryCurationActions.mockReturnValue({
      removeCuratedContent,
    });

    render(
      <CurationTable
        category={featuredSleepCategoryViewModel}
        items={categoryCurationItemViewModels}
        isLoading={false}
        localization={featuredSleepEnglishLocalizationViewModel}
        problem={null}
        onRetry={vi.fn()}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /remove/i })[0]!);

    expect(
      await screen.findByRole("heading", { name: /remove curated content/i }),
    ).toBeVisible();
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText(/^Evening Garden$/i)).toBeVisible();
    expect(within(dialog).getByText(/story\.evening-garden/i)).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: /^remove content$/i }));

    await waitFor(() => {
      expect(removeCuratedContent.mutateAsync).toHaveBeenCalledWith({
        contentId: 1,
      });
    });
  });
});
