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
            durationMinutes: null,
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
      screen.getByRole("columnheader", { name: /stories/i }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /pages/i })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /locale/i })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /readiness/i }),
    ).toBeVisible();
    expect(screen.getByText("Evening Garden")).toBeVisible();
    expect(screen.getByText(/story\.evening-garden/)).toBeVisible();
    expect(screen.getByText("2 pages")).toBeVisible();
    expect(screen.getByText("Ready to publish")).toBeVisible();

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

  it("keeps type-specific duration and blocker details inside the row", () => {
    const onContentSelect = vi.fn();

    render(
      <ContentListTable
        activeType="MEDITATION"
        items={[
          {
            contentId: 7,
            type: "MEDITATION",
            externalKey: "meditation.rain-room",
            pageCount: null,
            durationMinutes: 6,
            selectedLanguage: "tr",
            title: "Rain Room",
            readiness: "ACTION_REQUIRED",
            blockers: [
              { code: "COVER_MISSING", pageNumber: null },
              { code: "PAGE_TEXT_MISSING", pageNumber: 2 },
            ],
            lastEditedAt: "2026-03-17T09:00:00Z",
          },
        ]}
        onContentSelect={onContentSelect}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /duration/i }),
    ).toBeVisible();
    expect(screen.getByText("6 min")).toBeVisible();

    const blockerButton = screen.getByRole("button", {
      name: /show 2 publish blockers/i,
    });
    fireEvent.click(blockerButton);

    expect(
      screen.getByRole("region", { name: /publish blockers for turkish/i }),
    ).toHaveTextContent("Cover image is missing");
    expect(
      screen.getByRole("region", { name: /publish blockers for turkish/i }),
    ).toHaveTextContent("Page text is missing · Page 2");
    expect(screen.getByText("6 min")).toBeVisible();
    expect(onContentSelect).not.toHaveBeenCalled();
  });

  it("renders shared playback duration for lullaby registry rows", () => {
    render(
      <ContentListTable
        activeType="LULLABY"
        items={[
          {
            contentId: 8,
            type: "LULLABY",
            externalKey: "lullaby.moon-softly",
            pageCount: null,
            durationMinutes: 11,
            selectedLanguage: "tr",
            title: null,
            readiness: "PUBLISHED",
            blockers: [],
            lastEditedAt: "2026-09-16T09:00:00Z",
          },
        ]}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /duration/i }),
    ).toBeVisible();
    expect(screen.getByText("11 min")).toBeVisible();
  });
});
