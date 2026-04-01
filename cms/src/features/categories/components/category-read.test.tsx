import { fireEvent, render, screen } from "@testing-library/react";

import {
  archivedCategoryViewModel,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";

import { CategoryListTable } from "./category-list-table";
import { CategorySummaryCard } from "./category-summary-card";

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

    expect(screen.getByText(/story \/ standard \/ active/i)).toBeVisible();
    expect(screen.getByText("featured-sleep")).toBeVisible();
    expect(screen.getByText("Category #7")).toBeVisible();
    expect(screen.getByText(/base category detail live/i)).toBeVisible();
  });
});
