import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { freeAccessResponses } from "@/features/free-access/test/fixtures";

import { useFreeAccessList } from "./use-free-access-list";

const freeAccessAdminApiMock = vi.hoisted(() => ({
  listFreeAccessEntries: vi.fn(),
}));

vi.mock("@/features/free-access/api/free-access-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/free-access/api/free-access-admin")
  >("@/features/free-access/api/free-access-admin");

  return {
    ...actual,
    freeAccessAdminApi: {
      ...actual.freeAccessAdminApi,
      listFreeAccessEntries: freeAccessAdminApiMock.listFreeAccessEntries,
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

beforeEach(() => {
  freeAccessAdminApiMock.listFreeAccessEntries.mockReset();
});

describe("useFreeAccessList", () => {
  it("treats a blank filter as the default scope without sending an access key", async () => {
    freeAccessAdminApiMock.listFreeAccessEntries.mockResolvedValue(
      freeAccessResponses,
    );

    const { result } = renderHook(() => useFreeAccessList("   "), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(freeAccessAdminApiMock.listFreeAccessEntries).toHaveBeenCalledWith(
      undefined,
    );
    expect(result.current.isDefaultScope).toBe(true);
    expect(result.current.effectiveAccessKey).toBe("default");
    expect(result.current.entries).toHaveLength(2);
  });

  it("sends explicit access keys without falling back to the default scope", async () => {
    freeAccessAdminApiMock.listFreeAccessEntries.mockResolvedValue([
      freeAccessResponses[1],
    ]);

    const { result } = renderHook(
      () => useFreeAccessList(" partner-spring "),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(freeAccessAdminApiMock.listFreeAccessEntries).toHaveBeenCalledWith(
      "partner-spring",
    );
    expect(result.current.isDefaultScope).toBe(false);
    expect(result.current.effectiveAccessKey).toBe("partner-spring");
    expect(result.current.entries[0]).toMatchObject({
      accessKey: "partner-spring",
      contentId: 2,
    });
  });
});
