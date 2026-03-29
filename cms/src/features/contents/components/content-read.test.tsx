import { fireEvent, render, screen } from "@testing-library/react";

import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { ContentListTable } from "./content-list-table";
import { ContentSummaryCard } from "./content-summary-card";

describe("content read components", () => {
  it("renders content list columns and row navigation callbacks", () => {
    const onContentSelect = vi.fn();

    render(
      <ContentListTable
        contents={[storyContentViewModel, inactiveContentViewModel]}
        onContentSelect={onContentSelect}
      />,
    );

    expect(
      screen.getByRole("columnheader", { name: /content/i }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /format/i })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: /state/i })).toBeVisible();
    expect(screen.getByText("Evening Garden")).toBeVisible();
    expect(screen.getByText("story.evening-garden")).toBeVisible();

    fireEvent.click(screen.getByText("Evening Garden"));

    expect(onContentSelect).toHaveBeenCalledWith(storyContentViewModel);
  });

  it("renders summary metadata for the live detail shell", () => {
    render(<ContentSummaryCard content={storyContentViewModel} />);

    expect(screen.getByText(/story \/ active \/ age range 5/i)).toBeVisible();
    expect(
      screen.getByText("External key: story.evening-garden"),
    ).toBeVisible();
    expect(screen.getByText("2 language workspaces prepared")).toBeVisible();
    expect(screen.getByText("1 published / 1 mobile visible")).toBeVisible();
    expect(screen.getByText("1 localization ready")).toBeVisible();
    expect(screen.getByText("2 story pages")).toBeVisible();
  });
});
