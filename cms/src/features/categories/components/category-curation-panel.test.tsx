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
        curationIsLoading={false}
        curationProblem={null}
        localizations={categoryLocalizationViewModels}
        selectedLocalization={categoryLocalizationViewModels[0]}
        selectedLanguageCode="en"
        onCreateLocalization={vi.fn()}
        onLanguageChange={onLanguageChange}
        onRetryCuration={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("tablist", { name: /category curation language tabs/i }),
    ).toBeVisible();
    expect(screen.getByText(/english curation workspace/i)).toBeInTheDocument();
    expect(screen.getByText(/story items only/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeEnabled();
    expect(
      screen.queryByRole("button", { name: /adjust order/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("list", { name: /english curated content list/i }),
    ).toBeVisible();
    expect(screen.queryByText(/display order editor/i)).not.toBeInTheDocument();
  });

  it("shows an empty state and inactive actions when no localization is available", () => {
    const onCreateLocalization = vi.fn();

    renderWithQueryClient(
      <CategoryCurationPanel
        category={featuredSleepCategoryViewModel}
        curationItems={[]}
        curationIsLoading={false}
        curationProblem={null}
        localizations={[]}
        selectedLocalization={null}
        selectedLanguageCode="en"
        onCreateLocalization={onCreateLocalization}
        onLanguageChange={vi.fn()}
        onRetryCuration={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("heading", { name: /no curation language selected/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: /adjust order/i }),
    ).not.toBeInTheDocument();

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
        curationIsLoading={false}
        curationProblem={null}
        localizations={categoryLocalizationViewModels}
        selectedLocalization={categoryLocalizationViewModels[1]}
        selectedLanguageCode="tr"
        onCreateLocalization={vi.fn()}
        onLanguageChange={vi.fn()}
        onRetryCuration={vi.fn()}
      />,
    );

    expect(
      screen.getByRole("button", { name: /add curated content/i }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: /adjust order/i }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/no curated content yet/i)).toBeVisible();
    expect(
      screen.queryByRole("button", { name: /reorder evening garden/i }),
    ).not.toBeInTheDocument();
  });
});
