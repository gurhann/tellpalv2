import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { FreeAccessRoute } from "@/app/routes/free-access";
import { contentReadViewModels } from "@/features/contents/test/fixtures";
import { freeAccessViewModels } from "@/features/free-access/test/fixtures";

const freeAccessHookMocks = vi.hoisted(() => ({
  useFreeAccessList: vi.fn(),
}));
const contentHookMocks = vi.hoisted(() => ({
  useContentList: vi.fn(),
}));

vi.mock("@/features/free-access/queries/use-free-access-list", () => ({
  useFreeAccessList: freeAccessHookMocks.useFreeAccessList,
}));

vi.mock("@/features/contents/queries/use-content-list", () => ({
  useContentList: contentHookMocks.useContentList,
}));

function renderRoute() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/free-access"]}>
        <Routes>
          <Route path="/free-access" element={<FreeAccessRoute />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  freeAccessHookMocks.useFreeAccessList.mockReset();
  contentHookMocks.useContentList.mockReset();

  freeAccessHookMocks.useFreeAccessList.mockReturnValue({
    entries: freeAccessViewModels,
    isLoading: false,
    isFetching: false,
    isDefaultScope: true,
    effectiveAccessKey: "default",
    requestedAccessKey: "",
    problem: null,
    refetch: vi.fn(),
  });
  contentHookMocks.useContentList.mockReturnValue({
    contents: contentReadViewModels,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  });
});

describe("FreeAccessRoute", () => {
  it("renders the access key grants shell with default scope data", () => {
    renderRoute();

    expect(
      screen.getByRole("heading", { name: /access key grants/i, level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("")).toBeInTheDocument();
    expect(screen.getAllByText(/^default$/i).length).toBeGreaterThan(0);
    expect(screen.getByText("partner-spring")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /grant free access/i })).toBeEnabled();
  });

  it("renders the explicit unknown key empty state without falling back", () => {
    freeAccessHookMocks.useFreeAccessList.mockReturnValue({
      entries: [],
      isLoading: false,
      isFetching: false,
      isDefaultScope: false,
      effectiveAccessKey: "vip-missing",
      requestedAccessKey: "vip-missing",
      problem: null,
      refetch: vi.fn(),
    });

    renderRoute();

    expect(
      screen.getByRole("heading", { name: /no grants for this key/i }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/vip-missing/i).length).toBeGreaterThan(0);
    expect(
      screen.getByText(/did not fall back to the default set/i),
    ).toBeInTheDocument();
  });

  it("opens the grant dialog on demand", () => {
    renderRoute();

    fireEvent.click(screen.getByRole("button", { name: /grant free access/i }));

    expect(
      screen.getByRole("heading", { name: /grant free access/i }),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("default")).toBeInTheDocument();
  });
});
