import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";

import {
  categoryCurationItemViewModels,
  categoryLocalizationViewModels,
  archivedCategoryViewModel,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";

import { CategoryCurationPanel } from "./category-curation-panel";
import { CategoryListTable } from "./category-list-table";
import { CategorySummaryCard } from "./category-summary-card";

function renderWithQueryClient(ui: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

describe("category read components", () => {
  it("renders category list columns and row navigation callbacks", () => {
    const onCategorySelect = vi.fn();

    render(
      <CategoryListTable
        categories={[featuredSleepCategoryViewModel, archivedCategoryViewModel]}
        onCategorySelect={onCategorySelect}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /category/i }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /type/i })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /access/i })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /state/i })).toBeVisible();
    expect(screen.getByText("featured-sleep")).toBeVisible();
    expect(screen.getByText("Category #7")).toBeVisible();

    fireEvent.click(screen.getByText("featured-sleep"));

    expect(onCategorySelect).toHaveBeenCalledWith(
      featuredSleepCategoryViewModel,
    );
  });

  it("renders summary metadata for the live detail shell", () => {
    render(<CategorySummaryCard category={featuredSleepCategoryViewModel} />);

    expect(screen.getByText("featured-sleep")).toBeVisible();
    expect(screen.getByText("Story")).toBeVisible();
    expect(screen.getByText("Standard")).toBeVisible();
    expect(screen.getByText("Active")).toBeVisible();
    expect(screen.getByText("Category #7")).toBeVisible();
  });

  it("renders a category curation shell tied to the selected localization", () => {
    renderWithQueryClient(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        curationItems={categoryCurationItemViewModels}
        curationIsLoading={false}
        curationProblem={null}
        localizations={categoryLocalizationViewModels}
        selectedLocalization={categoryLocalizationViewModels[0]}
        selectedLanguageCode="en"
        onCreateLocalization={vi.fn()}
        onLanguageChange={vi.fn()}
        onRetryCuration={vi.fn()}
      />,
    );

    expect(screen.getByText(/category curation workspace/i)).toBeVisible();
    expect(
      screen.getByText(/manage curated content for the selected localization/i),
    ).toBeVisible();
    expect(screen.getByText(/story items only/i)).toBeVisible();
  });
});
