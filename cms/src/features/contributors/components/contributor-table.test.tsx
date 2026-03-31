import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { contributorViewModels } from "@/features/contributors/test/fixtures";

import { ContributorTable } from "./contributor-table";

describe("ContributorTable", () => {
  it("renders contributor identity and assignment readiness columns", () => {
    render(<ContributorTable contributors={contributorViewModels} />);

    expect(
      screen.getByRole("columnheader", { name: /contributor/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /initials/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: /assignment readiness/i }),
    ).toBeVisible();
    expect(screen.getByText("Annie Case")).toBeVisible();
    expect(screen.getByText("AC")).toBeVisible();
    expect(screen.getAllByText(/ready for content credits/i)).toHaveLength(3);
  });

  it("shows retry-capable empty and problem states", () => {
    const onRetry = vi.fn();

    const { rerender } = render(
      <ContributorTable contributors={[]} onRetry={onRetry} problem={null} />,
    );

    expect(
      screen.getByRole("heading", { name: /no contributors yet/i }),
    ).toBeVisible();

    rerender(
      <ContributorTable
        contributors={[]}
        onRetry={onRetry}
        problem={{
          type: "about:blank",
          title: "Contributor list unavailable",
          status: 503,
          detail: "The registry request failed.",
        }}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /retry/i }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "The registry request failed.",
    );
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("routes rename actions through the supplied callback", () => {
    const onRenameContributor = vi.fn();

    render(
      <ContributorTable
        contributors={contributorViewModels}
        onRenameContributor={onRenameContributor}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: /rename/i })[0]!);

    expect(onRenameContributor).toHaveBeenCalledWith(contributorViewModels[0]);
  });
});
