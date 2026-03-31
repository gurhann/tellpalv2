import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { storyContentViewModel } from "@/features/contents/test/fixtures";

import { ContentContributorEntryCard } from "./content-contributor-entry-card";

describe("ContentContributorEntryCard", () => {
  it("links content detail users into the shared contributor registry", () => {
    render(
      <MemoryRouter>
        <ContentContributorEntryCard content={storyContentViewModel} />
      </MemoryRouter>,
    );

    expect(screen.getByText(/contributor credits/i)).toBeVisible();
    expect(
      screen.getByRole("link", { name: /open contributor registry/i }),
    ).toHaveAttribute("href", "/contributors");
    expect(screen.getByText(/story\.evening-garden/i)).toBeVisible();
  });
});
