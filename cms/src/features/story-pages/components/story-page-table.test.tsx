import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { storyPageViewModels } from "@/features/story-pages/test/fixtures";

import { StoryPageTable } from "./story-page-table";

describe("StoryPageTable", () => {
  it("renders page number, illustration, and localization summary columns", () => {
    render(<StoryPageTable storyPages={storyPageViewModels} />);

    expect(screen.getByRole("columnheader", { name: /page/i })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /illustration/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /localizations/i }),
    ).toBeVisible();
    expect(screen.getByText("Page 1")).toBeVisible();
    expect(screen.getByText("Asset #41")).toBeVisible();
    expect(screen.getByText("2 locales")).toBeVisible();
  });

  it("routes row actions through the supplied callbacks", () => {
    const onEditStoryPage = vi.fn();
    const onDeleteStoryPage = vi.fn();

    render(
      <StoryPageTable
        storyPages={storyPageViewModels}
        onEditStoryPage={onEditStoryPage}
        onDeleteStoryPage={onDeleteStoryPage}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /edit/i })[0]!);
    fireEvent.click(screen.getAllByRole("button", { name: /delete/i })[0]!);

    expect(onEditStoryPage).toHaveBeenCalledWith(storyPageViewModels[0]);
    expect(onDeleteStoryPage).toHaveBeenCalledWith(storyPageViewModels[0]);
  });
});
