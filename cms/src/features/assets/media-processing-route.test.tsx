import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { MediaProcessingRoute } from "@/app/routes/media-processing";
import { contentReadViewModels } from "@/features/contents/test/fixtures";
import {
  failedProcessingViewModel,
  processingViewModel,
  processingViewModels,
} from "@/features/assets/test/fixtures";

const processingHookMocks = vi.hoisted(() => ({
  useRecentProcessingJobs: vi.fn(),
  useProcessingStatus: vi.fn(),
}));
const contentHookMocks = vi.hoisted(() => ({
  useContentList: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-recent-processing-jobs", () => ({
  useRecentProcessingJobs: processingHookMocks.useRecentProcessingJobs,
}));

vi.mock("@/features/assets/queries/use-processing-status", () => ({
  useProcessingStatus: processingHookMocks.useProcessingStatus,
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
      <MemoryRouter initialEntries={["/media-processing"]}>
        <Routes>
          <Route
            path="/media-processing"
            element={<MediaProcessingRoute />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  processingHookMocks.useRecentProcessingJobs.mockReset();
  processingHookMocks.useProcessingStatus.mockReset();
  contentHookMocks.useContentList.mockReset();

  processingHookMocks.useRecentProcessingJobs.mockReturnValue({
    jobs: processingViewModels,
    limit: 20,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  });
  processingHookMocks.useProcessingStatus.mockImplementation(
    ({
      contentId,
      languageCode,
    }: {
      contentId: number | null;
      languageCode?: string | null;
    }) => ({
      job:
        contentId === 1 && languageCode === "en"
          ? failedProcessingViewModel
          : contentId === 2 && languageCode === "de"
            ? processingViewModel
            : null,
      isLoading: false,
      isFetching: false,
      isNotScheduled: false,
      problem: null,
      refetch: vi.fn(),
    }),
  );
  contentHookMocks.useContentList.mockReturnValue({
    contents: contentReadViewModels,
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  });
});

describe("MediaProcessingRoute", () => {
  it("renders the processing console shell with recent jobs", () => {
    renderRoute();

    expect(
      screen.getByRole("heading", { name: /processing console/i, level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByText("story.evening-garden")).toBeInTheDocument();
    expect(screen.getByText("meditation.rain-room")).toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: /retry/i }).length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: /schedule processing/i })).toBeEnabled();
  });

  it("refreshes the recent processing query on demand", () => {
    const state = {
      jobs: processingViewModels,
      limit: 20,
      isLoading: false,
      isFetching: false,
      problem: null,
      refetch: vi.fn(),
    };
    processingHookMocks.useRecentProcessingJobs.mockReturnValue(state);

    renderRoute();

    fireEvent.click(screen.getByRole("button", { name: /^refresh$/i }));

    expect(state.refetch).toHaveBeenCalledTimes(1);
  });

  it("shows not scheduled feedback when the lookup query returns no processing record", () => {
    processingHookMocks.useProcessingStatus.mockReturnValue({
      job: null,
      isLoading: false,
      isFetching: false,
      isNotScheduled: true,
      problem: {
        type: "about:blank",
        title: "Processing not found",
        status: 404,
        detail: "No processing record exists yet.",
      },
      refetch: vi.fn(),
    });

    renderRoute();

    fireEvent.change(screen.getByLabelText(/content id/i), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByLabelText(/select language/i));
    fireEvent.click(screen.getByRole("option", { name: /english/i }));
    fireEvent.click(screen.getByRole("button", { name: /lookup status/i }));

    expect(
      screen.getByRole("heading", { name: /not scheduled yet/i }),
    ).toBeInTheDocument();
  });

  it("opens the schedule dialog on demand", () => {
    renderRoute();

    fireEvent.click(screen.getByRole("button", { name: /schedule processing/i }));

    expect(
      screen.getByRole("heading", { name: /schedule processing/i }),
    ).toBeInTheDocument();
  });
});
