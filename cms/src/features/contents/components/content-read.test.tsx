import { fireEvent, render, screen } from "@testing-library/react";

import { storyContentViewModel } from "@/features/contents/test/fixtures";

import { ContentListTable } from "./content-list-table";
import { ContentSummaryCard } from "./content-summary-card";

describe("content read components", () => {
  it("renders content list columns and row navigation callbacks", () => {
    const onContentSelect = vi.fn();

    render(
      <ContentListTable
        items={[
          {
            contentId: storyContentViewModel.summary.id,
            type: "STORY",
            externalKey: storyContentViewModel.summary.externalKey,
            pageCount: 2,
            selectedLanguage: "tr",
            title: "Evening Garden",
            readiness: "READY_TO_PUBLISH",
            blockers: [],
            lastEditedAt: "2026-03-17T09:00:00Z",
          },
        ]}
        onContentSelect={onContentSelect}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /content/i }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /type/i })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /readiness/i }),
    ).toBeVisible();
    expect(screen.getByText("Evening Garden")).toBeVisible();
    expect(screen.getByText(/story\.evening-garden/)).toBeVisible();

    fireEvent.click(screen.getByText("Evening Garden"));

    expect(onContentSelect).toHaveBeenCalledWith(
      expect.objectContaining({ contentId: storyContentViewModel.summary.id }),
    );
  });

  it("renders summary metadata for the live detail shell", () => {
    render(<ContentSummaryCard content={storyContentViewModel} />);

    expect(screen.getByText("story.evening-garden")).toBeVisible();
    expect(screen.getByText("Story")).toBeVisible();
    expect(screen.getByText("Active")).toBeVisible();
    expect(screen.getByText("Age 5")).toBeVisible();
    expect(screen.getByText("2 locales")).toBeVisible();
    expect(screen.getByText("2 pages")).toBeVisible();
  });
});
