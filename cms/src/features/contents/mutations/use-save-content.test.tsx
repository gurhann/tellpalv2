import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminContentResponse } from "@/features/contents/api/content-admin";
import {
  storyContentViewModel,
  inactiveContentViewModel,
} from "@/features/contents/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useSaveContent } from "./use-save-content";

const contentAdminApiMock = vi.hoisted(() => ({
  createContent: vi.fn(),
  updateContent: vi.fn(),
}));

vi.mock("@/features/contents/api/content-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contents/api/content-admin")
  >("@/features/contents/api/content-admin");

  return {
    ...actual,
    contentAdminApi: {
      ...actual.contentAdminApi,
      createContent: contentAdminApiMock.createContent,
      updateContent: contentAdminApiMock.updateContent,
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
  contentAdminApiMock.createContent.mockReset();
  contentAdminApiMock.updateContent.mockReset();
});

describe("useSaveContent", () => {
  it("creates content and updates list/detail caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const createdContent: AdminContentResponse = {
      contentId: 99,
      type: "LULLABY",
      externalKey: "lullaby.new-moon",
      active: true,
      ageRange: 3,
      pageCount: null,
    };
    const onSuccess = vi.fn();
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.contents.list(), [
      storyContentViewModel,
    ]);
    contentAdminApiMock.createContent.mockResolvedValue(createdContent);

    const { result } = renderHook(
      () =>
        useSaveContent({
          mode: "create",
          onSuccess,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.mutateAsync({
        type: "LULLABY",
        externalKey: "lullaby.new-moon",
        ageRange: 3,
        active: true,
      });
    });

    expect(contentAdminApiMock.createContent).toHaveBeenCalledWith({
      type: "LULLABY",
      externalKey: "lullaby.new-moon",
      ageRange: 3,
      active: true,
    });
    expect(
      queryClient.getQueryData(queryKeys.contents.detail(99)),
    ).toMatchObject({
      summary: {
        id: 99,
        externalKey: "lullaby.new-moon",
        type: "LULLABY",
      },
    });
    expect(
      queryClient.getQueryData<Array<{ summary: { id: number } }>>(
        queryKeys.contents.list(),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          summary: expect.objectContaining({ id: 99 }),
        }),
      ]),
    );
    expect(onSuccess).toHaveBeenCalledWith(createdContent);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.lists(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.detail(99),
    });
  });

  it("updates content metadata and preserves existing localizations in cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedContent: AdminContentResponse = {
      contentId: 1,
      type: "STORY",
      externalKey: "story.evening-garden.updated",
      active: false,
      ageRange: 6,
      pageCount: 2,
    };

    queryClient.setQueryData(queryKeys.contents.list(), [
      storyContentViewModel,
      inactiveContentViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.contents.detail(1),
      storyContentViewModel,
    );
    contentAdminApiMock.updateContent.mockResolvedValue(updatedContent);

    const { result } = renderHook(
      () =>
        useSaveContent({
          mode: "update",
          contentId: 1,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.mutateAsync({
        type: "STORY",
        externalKey: "story.evening-garden.updated",
        ageRange: 6,
        active: false,
      });
    });

    const detailCache = queryClient.getQueryData<{
      summary: {
        externalKey: string;
        active: boolean;
        ageRange: number | null;
      };
      localizations: unknown[];
    }>(queryKeys.contents.detail(1));
    const listCache = queryClient.getQueryData<
      Array<{ summary: { id: number; externalKey: string; active: boolean } }>
    >(queryKeys.contents.list());

    expect(contentAdminApiMock.updateContent).toHaveBeenCalledWith(1, {
      externalKey: "story.evening-garden.updated",
      ageRange: 6,
      active: false,
    });
    expect(detailCache).toMatchObject({
      summary: {
        externalKey: "story.evening-garden.updated",
        active: false,
        ageRange: 6,
      },
    });
    expect(detailCache?.localizations).toHaveLength(2);
    expect(listCache).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          summary: expect.objectContaining({
            id: 1,
            externalKey: "story.evening-garden.updated",
            active: false,
          }),
        }),
      ]),
    );
  });
});
