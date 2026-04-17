import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { processingResponse } from "@/features/assets/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useProcessingStatus } from "./use-processing-status";

const assetProcessingAdminApiMock = vi.hoisted(() => ({
  getProcessingStatus: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-processing-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-processing-admin")
  >("@/features/assets/api/asset-processing-admin");

  return {
    ...actual,
    assetProcessingAdminApi: {
      ...actual.assetProcessingAdminApi,
      getProcessingStatus: assetProcessingAdminApiMock.getProcessingStatus,
    },
  };
});

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 404,
    detail: "Processing record missing",
    ...overrides,
  };
}

function makeApiClientError(problem: ApiProblemDetail) {
  return new ApiClientError(
    problem,
    new Response(JSON.stringify(problem), {
      status: problem.status,
      statusText: problem.title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    }),
  );
}

beforeEach(() => {
  assetProcessingAdminApiMock.getProcessingStatus.mockReset();
});

describe("useProcessingStatus", () => {
  it("loads and maps one processing record", async () => {
    assetProcessingAdminApiMock.getProcessingStatus.mockResolvedValue(
      processingResponse,
    );

    const { result } = renderHook(
      () =>
        useProcessingStatus({
          contentId: 2,
          languageCode: "de",
        }),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(assetProcessingAdminApiMock.getProcessingStatus).toHaveBeenCalledWith(
      2,
      "de",
    );
    expect(result.current.job).toMatchObject({
      contentId: 2,
      externalKey: "meditation.rain-room",
      status: "PROCESSING",
    });
    expect(result.current.isNotScheduled).toBe(false);
    expect(result.current.problem).toBeNull();
  });

  it("maps 404 processing gaps to not scheduled state", async () => {
    assetProcessingAdminApiMock.getProcessingStatus.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          title: "Processing not found",
          detail: "No processing record exists yet.",
          errorCode: "asset_processing_not_found",
          path: "/api/admin/media-processing/2/localizations/de",
        }),
      ),
    );

    const { result } = renderHook(
      () =>
        useProcessingStatus({
          contentId: 2,
          languageCode: "de",
        }),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotScheduled).toBe(true);
    expect(result.current.problem?.status).toBe(404);
  });

  it("keeps generic failures separate from not scheduled handling", async () => {
    assetProcessingAdminApiMock.getProcessingStatus.mockRejectedValue(
      new Error("Processing lookup timeout"),
    );

    const { result } = renderHook(
      () =>
        useProcessingStatus({
          contentId: 1,
          languageCode: "en",
        }),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotScheduled).toBe(false);
    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Processing lookup timeout",
    });
  });
});
