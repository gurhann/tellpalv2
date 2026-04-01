import { fireEvent, render, screen } from "@testing-library/react";

import {
  categoryLocalizationViewModels,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";

import { CategoryCurationPanel } from "./category-curation-panel";

describe("CategoryCurationPanel", () => {
  it("renders a language-scoped curation workspace for the selected localization", () => {
    const onLanguageChange = vi.fn();

    render(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        localizations={categoryLocalizationViewModels}
        selectedLanguageCode="tr"
        onCreateLocalization={vi.fn()}
        onLanguageChange={onLanguageChange}
      />,
    );

    expect(
      screen.getByRole("tablist", { name: /category curation language tabs/i }),
    ).toBeVisible();
    expect(screen.getByText(/turkish curation workspace/i)).toBeInTheDocument();
    expect(
      screen.getByText(/cross-type curation is rejected by the backend/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeEnabled();
  });

  it("shows an empty state and inactive actions when no localization is available", () => {
    const onCreateLocalization = vi.fn();

    render(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        localizations={[]}
        selectedLanguageCode="en"
        onCreateLocalization={onCreateLocalization}
        onLanguageChange={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("heading", { name: /no curation language selected/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: /adjust order/i }),
    ).toBeDisabled();

    fireEvent.click(
      screen.getByRole("button", { name: /create first localization/i }),
    );

    expect(onCreateLocalization).toHaveBeenCalledTimes(1);
  });
});
