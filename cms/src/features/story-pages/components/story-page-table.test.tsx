import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { storyPageViewModels } from "@/features/story-pages/test/fixtures";

import { StoryPageTable } from "./story-page-table";

describe("StoryPageTable", () => {
  it("renders page number, localization, and illustration coverage columns", () => {
    render(
      <StoryPageTable
        storyPages={storyPageViewModels}
        availableLocalizations={[
          { languageCode: "en", languageLabel: "English" },
          { languageCode: "tr", languageLabel: "Turkish" },
        ]}
        selectedLanguageCode="en"
        selectedLanguageLabel="English"
      />,
    );

    expect(screen.getByRole("columnheader", { name: /page/i })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /all locale coverage/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /english status/i }),
    ).toBeVisible();
    expect(screen.getByText("Page 1")).toBeVisible();
    expect(screen.getAllByText("Ready")).not.toHaveLength(0);
    expect(screen.getByText("2 / 2 ready")).toBeVisible();
    expect(screen.getAllByText(/ready for selected locale/i)).not.toHaveLength(
      0,
    );
  });

  it("routes row actions through the supplied callbacks", () => {
    const onEditStoryPage = vi.fn();
    const onAddAfterStoryPage = vi.fn();
    const onDeleteStoryPage = vi.fn();

    render(
      <StoryPageTable
        storyPages={storyPageViewModels}
        availableLocalizations={[
          { languageCode: "en", languageLabel: "English" },
          { languageCode: "tr", languageLabel: "Turkish" },
        ]}
        selectedLanguageCode="en"
        selectedLanguageLabel="English"
        onEditStoryPage={onEditStoryPage}
        onAddAfterStoryPage={onAddAfterStoryPage}
        onDeleteStoryPage={onDeleteStoryPage}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /edit/i })[0]!);
    fireEvent.click(
      screen.getAllByRole("button", { name: /add page after/i })[0]!,
    );
    fireEvent.click(screen.getAllByRole("button", { name: /delete/i })[0]!);

    expect(onEditStoryPage).toHaveBeenCalledWith(storyPageViewModels[0]);
    expect(onAddAfterStoryPage).toHaveBeenCalledWith(storyPageViewModels[0]);
    expect(onDeleteStoryPage).toHaveBeenCalledWith(storyPageViewModels[0]);
  });
});
