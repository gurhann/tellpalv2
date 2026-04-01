import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type {
  AdminContentContributorResponse,
  AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  contentContributorViewModels,
  contributorViewModels,
} from "@/features/contributors/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useContributorActions } from "./use-contributor-actions";

const contributorAdminApiMock = vi.hoisted(() => ({
  createContributor: vi.fn(),
  renameContributor: vi.fn(),
  assignContributor: vi.fn(),
}));

vi.mock("@/features/contributors/api/contributor-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contributors/api/contributor-admin")
  >("@/features/contributors/api/contributor-admin");

  return {
    ...actual,
    contributorAdminApi: {
      ...actual.contributorAdminApi,
      createContributor: contributorAdminApiMock.createContributor,
      renameContributor: contributorAdminApiMock.renameContributor,
      assignContributor: contributorAdminApiMock.assignContributor,
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
  contributorAdminApiMock.createContributor.mockReset();
  contributorAdminApiMock.renameContributor.mockReset();
  contributorAdminApiMock.assignContributor.mockReset();
});

describe("useContributorActions", () => {
  it("creates a contributor and refreshes list/detail caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const createdContributor: AdminContributorResponse = {
      contributorId: 99,
      displayName: "Lina Hart",
    };
    const onCreateSuccess = vi.fn();
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.contributors.list({ limit: 12 }), [
      contributorViewModels[0],
      contributorViewModels[1],
    ]);
    contributorAdminApiMock.createContributor.mockResolvedValue(
      createdContributor,
    );

    const { result } = renderHook(
      () =>
        useContributorActions({
          onCreateSuccess,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.createContributor.mutateAsync({
        displayName: "  Lina Hart  ",
      });
    });

    expect(contributorAdminApiMock.createContributor).toHaveBeenCalledWith({
      displayName: "Lina Hart",
    });
    expect(
      queryClient.getQueryData(queryKeys.contributors.detail(99)),
    ).toMatchObject({
      id: 99,
      displayName: "Lina Hart",
    });
    expect(
      queryClient.getQueryData<Array<{ id: number; displayName: string }>>(
        queryKeys.contributors.list({ limit: 12 }),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          id: 99,
          displayName: "Lina Hart",
        }),
      ]),
    );
    expect(onCreateSuccess).toHaveBeenCalledWith(createdContributor);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contributors.lists(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contributors.detail(99),
    });
  });

  it("renames a contributor and updates cached registry entries", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const renamedContributor: AdminContributorResponse = {
      contributorId: 11,
      displayName: "Annie Case Updated",
    };

    queryClient.setQueryData(queryKeys.contributors.list({ limit: 12 }), [
      contributorViewModels[0],
      contributorViewModels[1],
    ]);
    queryClient.setQueryData(
      queryKeys.contributors.detail(11),
      contributorViewModels[0],
    );
    contributorAdminApiMock.renameContributor.mockResolvedValue(
      renamedContributor,
    );

    const { result } = renderHook(() => useContributorActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.renameContributor.mutateAsync({
        contributorId: 11,
        values: {
          displayName: " Annie Case Updated ",
        },
      });
    });

    expect(contributorAdminApiMock.renameContributor).toHaveBeenCalledWith(11, {
      displayName: "Annie Case Updated",
    });
    expect(
      queryClient.getQueryData<{ id: number; displayName: string }>(
        queryKeys.contributors.detail(11),
      ),
    ).toMatchObject({
      id: 11,
      displayName: "Annie Case Updated",
    });
    expect(
      queryClient.getQueryData<Array<{ id: number; displayName: string }>>(
        queryKeys.contributors.list({ limit: 12 }),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          id: 11,
          displayName: "Annie Case Updated",
        }),
      ]),
    );
  });

  it("assigns a contributor inside one content context and updates the local assignment cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const assignment: AdminContentContributorResponse = {
      contentId: 1,
      contributorId: 12,
      contributorDisplayName: "Milo Rivers",
      role: "NARRATOR",
      languageCode: "tr",
      creditName: "M. Rivers",
      sortOrder: 1,
    };
    const onAssignSuccess = vi.fn();

    queryClient.setQueryData(queryKeys.contributors.assignments(1), [
      contentContributorViewModels[0],
    ]);
    contributorAdminApiMock.assignContributor.mockResolvedValue(assignment);

    const { result } = renderHook(
      () =>
        useContributorActions({
          onAssignSuccess,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.assignContributor.mutateAsync({
        contentId: 1,
        values: {
          contributorId: 12,
          role: "NARRATOR",
          languageCode: "tr",
          creditName: "M. Rivers",
          sortOrder: 1,
        },
      });
    });

    expect(contributorAdminApiMock.assignContributor).toHaveBeenCalledWith(1, {
      contributorId: 12,
      role: "NARRATOR",
      languageCode: "tr",
      creditName: "M. Rivers",
      sortOrder: 1,
    });
    expect(
      queryClient.getQueryData<
        Array<{ contributorId: number; role: string; languageCode: string }>
      >(queryKeys.contributors.assignments(1)),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          contributorId: 11,
          role: "AUTHOR",
          languageCode: "en",
        }),
        expect.objectContaining({
          contributorId: 12,
          role: "NARRATOR",
          languageCode: "tr",
        }),
      ]),
    );
    expect(onAssignSuccess).toHaveBeenCalledWith(assignment);
  });

  it("stores global assignments with a null language scope in the local cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const assignment: AdminContentContributorResponse = {
      contentId: 1,
      contributorId: 13,
      contributorDisplayName: "Sena Yildiz",
      role: "ILLUSTRATOR",
      languageCode: null,
      creditName: null,
      sortOrder: 0,
    };

    contributorAdminApiMock.assignContributor.mockResolvedValue(assignment);

    const { result } = renderHook(() => useContributorActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.assignContributor.mutateAsync({
        contentId: 1,
        values: {
          contributorId: 13,
          role: "ILLUSTRATOR",
          languageCode: null,
          creditName: null,
          sortOrder: 0,
        },
      });
    });

    expect(contributorAdminApiMock.assignContributor).toHaveBeenCalledWith(1, {
      contributorId: 13,
      role: "ILLUSTRATOR",
      languageCode: null,
      creditName: null,
      sortOrder: 0,
    });
    expect(
      queryClient.getQueryData<
        Array<{
          contributorId: number;
          role: string;
          languageCode: string | null;
        }>
      >(queryKeys.contributors.assignments(1)),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          contributorId: 13,
          role: "ILLUSTRATOR",
          languageCode: null,
        }),
      ]),
    );
  });
});
