import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { LanguageBadge } from "@/components/language/language-badge";
import {
  LanguageTabs,
  type LanguageTabItem,
} from "@/components/language/language-tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { ApiProblemDetail } from "@/types/api";

type DemoRow = {
  id: string;
  title: string;
  language: string;
};

const demoColumns: DataTableColumn<DemoRow>[] = [
  {
    id: "title",
    header: "Title",
    cell: (row) => row.title,
  },
  {
    id: "language",
    header: "Language",
    cell: (row) => row.language,
  },
];

const demoRows: DemoRow[] = [
  {
    id: "row-1",
    title: "Sleep Story",
    language: "English",
  },
  {
    id: "row-2",
    title: "Meditation Pack",
    language: "Turkish",
  },
];

const problem: ApiProblemDetail = {
  type: "about:blank",
  title: "Request failed",
  status: 500,
  detail: "The current workspace could not load records.",
};

const languageItems: LanguageTabItem[] = [
  {
    code: "tr",
    tone: "success",
    meta: "Ready",
    description: "Primary editorial language.",
  },
  {
    code: "en",
    tone: "warning",
    meta: "Draft",
    description: "Needs localization review.",
  },
];

describe("Shared data and language patterns", () => {
  it("renders table headers, cells, and interactive rows", () => {
    const onRowClick = vi.fn();

    render(
      <DataTable
        columns={demoColumns}
        getRowId={(row) => row.id}
        onRowClick={onRowClick}
        rows={demoRows}
      />,
    );

    expect(screen.getByRole("columnheader", { name: "Title" })).toBeVisible();
    expect(screen.getByText("Sleep Story")).toBeVisible();

    fireEvent.click(screen.getByText("Sleep Story"));

    expect(onRowClick).toHaveBeenCalledWith(demoRows[0]);
  });

  it("renders loading, empty, and problem states through the same table shell", () => {
    const onRetry = vi.fn();

    const { rerender } = render(
      <DataTable
        columns={demoColumns}
        getRowId={(row) => row.id}
        isLoading
        rows={[]}
      />,
    );

    expect(
      screen.getByRole("heading", { name: /loading records/i }),
    ).toBeVisible();

    rerender(
      <DataTable
        columns={demoColumns}
        emptyDescription="Try a broader filter."
        emptyTitle="Nothing matched"
        getRowId={(row) => row.id}
        rows={[]}
      />,
    );

    expect(
      screen.getByRole("heading", { name: /nothing matched/i }),
    ).toBeVisible();

    rerender(
      <DataTable
        columns={demoColumns}
        getRowId={(row) => row.id}
        onRetry={onRetry}
        problem={problem}
        rows={demoRows}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "The current workspace could not load records.",
    );

    fireEvent.click(screen.getByRole("button", { name: /retry/i }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("renders a reusable filter bar layout", () => {
    render(
      <FilterBar aria-label="Content filters">
        <FilterBarGroup>
          <Input aria-label="Search records" placeholder="Search" />
          <Button type="button" variant="outline">
            Status
          </Button>
        </FilterBarGroup>
        <FilterBarActions>
          <Button type="button">Create record</Button>
        </FilterBarActions>
        <FilterBarSummary
          description="Filters update the current workspace immediately."
          title="24 records"
        />
      </FilterBar>,
    );

    expect(screen.getByLabelText("Search records")).toBeVisible();
    expect(screen.getByRole("button", { name: "Status" })).toBeVisible();
    expect(screen.getByText("24 records")).toBeVisible();
    expect(
      screen.getByRole("button", { name: /create record/i }),
    ).toBeVisible();
  });

  it("renders language badges with readable labels and tones", () => {
    render(<LanguageBadge code="tr" meta="Ready" tone="success" />);

    expect(screen.getByText("tr")).toBeVisible();
    expect(screen.getByText("Turkish")).toBeVisible();
    expect(screen.getByText("Ready")).toBeVisible();
  });

  it("switches between language tabs and renders localized content", () => {
    function LanguageTabsHarness() {
      const [value, setValue] = useState("tr");

      return (
        <LanguageTabs
          items={languageItems}
          renderContent={(item) => <div>{`${item.code} workspace`}</div>}
          value={value}
          onValueChange={setValue}
        />
      );
    }

    render(<LanguageTabsHarness />);

    expect(screen.getByText("tr workspace")).toBeVisible();

    fireEvent.mouseDown(screen.getByRole("tab", { name: /english/i }));

    expect(screen.getByText("en workspace")).toBeVisible();
  });
});
