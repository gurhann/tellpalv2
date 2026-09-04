import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { contributorViewModels } from "@/features/contributors/test/fixtures";
import { ContributorsRoute } from "@/app/routes/contributors";

const contributorHookMocks = vi.hoisted(() => ({
  useContributors: vi.fn(),
  useContributorRegistry: vi.fn(),
}));
const contributorActionMocks = vi.hoisted(() => ({
  useContributorActions: vi.fn(),
}));

vi.mock("@/features/contributors/queries/use-contributors", () => ({
  useContributors: contributorHookMocks.useContributors,
  useContributorRegistry: contributorHookMocks.useContributorRegistry,
}));

vi.mock("@/features/contributors/mutations/use-contributor-actions", () => ({
  useContributorActions: contributorActionMocks.useContributorActions,
}));

function makeContributorState(overrides: Record<string, unknown> = {}) {
  return {
    contributors: contributorViewModels,
    limit: 12,
    page: 0,
    size: 25,
    totalPages: 1,
    totalItems: contributorViewModels.length,
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

function renderContributorRouteAt(entry: string) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/contributors" element={<ContributorsRoute />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ContributorsRoute", () => {
  it("renders the live contributor registry shell", () => {
    contributorHookMocks.useContributorRegistry.mockReturnValue(
      makeContributorState(),
    );
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      deleteContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      renameContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      isPending: false,
    });

    renderContributorRoute();

    expect(
      screen.getByRole("heading", { name: /^contributors$/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^refresh$/i })).toBeEnabled();
    expect(
      screen.getByRole("button", { name: /create contributor/i }),
    ).toBeEnabled();
    expect(screen.getByLabelText(/search contributors/i)).toBeEnabled();
    expect(screen.getByText("Annie Case")).toBeInTheDocument();
    expect(screen.getByText(/^3 records$/i)).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /rename/i })).toHaveLength(3);
    expect(screen.getAllByRole("button", { name: /delete/i })).toHaveLength(3);
  });

  it("renders empty contributor state inline when no records exist", () => {
    contributorHookMocks.useContributorRegistry.mockReturnValue(
      makeContributorState({ contributors: [] }),
    );
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      deleteContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      renameContributor: {
        mutateAsync: vi.fn(),
        isPending: false,
        reset: vi.fn(),
      },
      isPending: false,
    });

    renderContributorRoute();

    expect(
      screen.getByRole("heading", { name: /no contributors yet/i }),
    ).toBeInTheDocument();
  });

  it("passes URL filters and page to the registry query", () => {
    contributorHookMocks.useContributorRegistry.mockReturnValue(
      makeContributorState(),
    );
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: { mutateAsync: vi.fn(), isPending: false },
      deleteContributor: { mutateAsync: vi.fn(), isPending: false },
      renameContributor: { mutateAsync: vi.fn(), isPending: false },
      isPending: false,
    });

    renderContributorRouteAt("/contributors?q=annie&role=AUTHOR&page=2");

    expect(contributorHookMocks.useContributorRegistry).toHaveBeenCalledWith({
      page: 2,
      size: 25,
      query: "annie",
      role: "AUTHOR",
    });
  });

  it("resets the page when the search filter changes", async () => {
    contributorHookMocks.useContributorRegistry.mockReturnValue(
      makeContributorState(),
    );
    contributorActionMocks.useContributorActions.mockReturnValue({
      createContributor: { mutateAsync: vi.fn(), isPending: false },
      deleteContributor: { mutateAsync: vi.fn(), isPending: false },
      renameContributor: { mutateAsync: vi.fn(), isPending: false },
      isPending: false,
    });

    renderContributorRouteAt("/contributors?page=3");
    fireEvent.change(screen.getByLabelText(/search contributors/i), {
      target: { value: "annie" },
    });

    await waitFor(() =>
      expect(
        contributorHookMocks.useContributorRegistry,
      ).toHaveBeenLastCalledWith({
        page: 0,
        size: 25,
        query: "annie",
        role: undefined,
      }),
    );
  });
});
