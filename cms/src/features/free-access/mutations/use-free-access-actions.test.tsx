import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  defaultFreeAccessResponse,
  partnerFreeAccessResponse,
} from "@/features/free-access/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useFreeAccessActions } from "./use-free-access-actions";

const freeAccessAdminApiMock = vi.hoisted(() => ({
  grantFreeAccess: vi.fn(),
  revokeFreeAccess: vi.fn(),
}));

vi.mock("@/features/free-access/api/free-access-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/free-access/api/free-access-admin")
  >("@/features/free-access/api/free-access-admin");

  return {
    ...actual,
    freeAccessAdminApi: {
      ...actual.freeAccessAdminApi,
      grantFreeAccess: freeAccessAdminApiMock.grantFreeAccess,
      revokeFreeAccess: freeAccessAdminApiMock.revokeFreeAccess,
    },
  };
});

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

beforeEach(() => {
  freeAccessAdminApiMock.grantFreeAccess.mockReset();
  freeAccessAdminApiMock.revokeFreeAccess.mockReset();
});

describe("useFreeAccessActions", () => {
  it("grants free access and invalidates free access lists", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    freeAccessAdminApiMock.grantFreeAccess.mockResolvedValue(
      defaultFreeAccessResponse,
    );

    const { result } = renderHook(() => useFreeAccessActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.grantFreeAccess.mutateAsync({
        accessKey: "default",
        contentId: 1,
        languageCode: "en",
      });
    });

    expect(freeAccessAdminApiMock.grantFreeAccess).toHaveBeenCalledWith({
      accessKey: "default",
      contentId: 1,
      languageCode: "en",
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.freeAccess.lists(),
    });
  });

  it("revokes free access and invalidates free access lists", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    freeAccessAdminApiMock.revokeFreeAccess.mockResolvedValue(undefined);

    const { result } = renderHook(() => useFreeAccessActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.revokeFreeAccess.mutateAsync({
        accessKey: partnerFreeAccessResponse.accessKey,
        languageCode: partnerFreeAccessResponse.languageCode,
        contentId: partnerFreeAccessResponse.contentId,
      });
    });

    expect(freeAccessAdminApiMock.revokeFreeAccess).toHaveBeenCalledWith(
      "partner-spring",
      "de",
      2,
    );
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.freeAccess.lists(),
    });
  });
});
