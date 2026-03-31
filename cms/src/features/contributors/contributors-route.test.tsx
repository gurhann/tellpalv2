import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { contributorViewModels } from "@/features/contributors/test/fixtures";
import { ContributorsRoute } from "@/app/routes/contributors";

const contributorHookMocks = vi.hoisted(() => ({
  useContributors: vi.fn(),
}));

vi.mock("@/features/contributors/queries/use-contributors", () => ({
  useContributors: contributorHookMocks.useContributors,
}));

function makeContributorState(overrides: Record<string, unknown> = {}) {
  return {
    contributors: contributorViewModels,
    limit: 12,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
    ...overrides,
  };
}

function renderContributorRoute() {
  return render(
    <MemoryRouter initialEntries={["/contributors"]}>
      <Routes>
        <Route path="/contributors" element={<ContributorsRoute />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ContributorsRoute", () => {
  it("renders the live contributor registry shell", () => {
    contributorHookMocks.useContributors.mockReturnValue(
      makeContributorState(),
    );

    renderContributorRoute();

    expect(
      screen.getByRole("heading", { name: /^contributors$/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^refresh$/i })).toBeEnabled();
    expect(
      screen.getByRole("button", { name: /create contributor/i }),
    ).toBeDisabled();
    expect(screen.getByLabelText(/search contributors/i)).toBeDisabled();
    expect(screen.getByText("Annie Case")).toBeInTheDocument();
    expect(screen.getByText(/^Latest 12$/i)).toBeInTheDocument();
  });

  it("renders empty contributor state inline when no records exist", () => {
    contributorHookMocks.useContributors.mockReturnValue(
      makeContributorState({ contributors: [] }),
    );

    renderContributorRoute();

    expect(
      screen.getByRole("heading", { name: /no contributors yet/i }),
    ).toBeInTheDocument();
  });
});
