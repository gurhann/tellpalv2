import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";

import {
  categoryCurationItemViewModels,
  categoryLocalizationViewModels,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";

import { CategoryCurationPanel } from "./category-curation-panel";

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

describe("CategoryCurationPanel", () => {
  it("renders a language-scoped curation workspace for the selected localization", () => {
    const onLanguageChange = vi.fn();

    renderWithQueryClient(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        curationItems={categoryCurationItemViewModels}
        localizations={categoryLocalizationViewModels}
        selectedLocalization={categoryLocalizationViewModels[0]}
        selectedLanguageCode="en"
        onCreateLocalization={vi.fn()}
        onLanguageChange={onLanguageChange}
      />,
    );

    expect(
      screen.getByRole("tablist", { name: /category curation language tabs/i }),
    ).toBeVisible();
    expect(screen.getByText(/english curation workspace/i)).toBeInTheDocument();
    expect(
      screen.getByText(/cross-type curation is rejected by the backend/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeEnabled();
    expect(screen.getByRole("link", { name: /adjust order/i })).toBeEnabled();
  });

  it("shows an empty state and inactive actions when no localization is available", () => {
    const onCreateLocalization = vi.fn();

    renderWithQueryClient(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        curationItems={[]}
        localizations={[]}
        selectedLocalization={null}
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

  it("keeps curation actions disabled for draft localizations", () => {
    renderWithQueryClient(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        curationItems={[]}
        localizations={categoryLocalizationViewModels}
        selectedLocalization={categoryLocalizationViewModels[1]}
        selectedLanguageCode="tr"
        onCreateLocalization={vi.fn()}
        onLanguageChange={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: /adjust order/i }),
    ).toBeDisabled();
  });
});
