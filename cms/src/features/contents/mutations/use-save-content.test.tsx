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
      textlessCoverMediaId: null,
      listeningCoverMediaId: null,
    };
    const onSuccess = vi.fn();
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.contents.list(), [
      storyContentViewModel,
    ]);
    const registryKey = queryKeys.contents.registry({
      language: "tr",
      page: 0,
      size: 25,
    });
    queryClient.setQueryData(registryKey, {
      items: [],
      page: 0,
      size: 25,
      totalItems: 0,
    });
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
      queryKey: queryKeys.contents.registries(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.detail(99),
    });
    expect(queryClient.getQueryData(registryKey)).toEqual({
      items: [],
      page: 0,
      size: 25,
      totalItems: 0,
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
      textlessCoverMediaId: 701,
      listeningCoverMediaId: 702,
      listingCoverMediaId: null,
    };

    const existingRecord = {
      ...storyContentViewModel,
      playback: {
        audioAssetId: 44,
        durationMinutes: 3,
        processingStatus: "COMPLETED" as const,
        processingError: null,
        instruments: [],
      },
    };
    queryClient.setQueryData(queryKeys.contents.list(), [
      existingRecord,
      inactiveContentViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.contents.detail(1),
      existingRecord,
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
        textlessCoverMediaId: 701,
        listeningCoverMediaId: 702,
        listingCoverMediaId: null,
      });
    });

    const detailCache = queryClient.getQueryData<{
      summary: {
        externalKey: string;
        active: boolean;
        ageRange: number | null;
        listeningCoverAssetId: number | null;
        hasListeningCover: boolean;
      };
      localizations: unknown[];
      playback: { audioAssetId: number; durationMinutes: number } | null;
    }>(queryKeys.contents.detail(1));
    const listCache = queryClient.getQueryData<
      Array<{
        summary: {
          id: number;
          externalKey: string;
          active: boolean;
          listeningCoverAssetId: number | null;
          hasListeningCover: boolean;
        };
      }>
    >(queryKeys.contents.list());

    expect(contentAdminApiMock.updateContent).toHaveBeenCalledWith(1, {
      externalKey: "story.evening-garden.updated",
      ageRange: 6,
      active: false,
      textlessCoverMediaId: 701,
      listeningCoverMediaId: 702,
      listingCoverMediaId: null,
    });
    expect(detailCache).toMatchObject({
      summary: {
        externalKey: "story.evening-garden.updated",
        active: false,
        ageRange: 6,
        listeningCoverAssetId: 702,
        hasListeningCover: true,
      },
    });
    expect(detailCache?.localizations).toHaveLength(2);
    expect(detailCache?.playback).toMatchObject({
      audioAssetId: 44,
      durationMinutes: 3,
    });
    expect(listCache).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          summary: expect.objectContaining({
            id: 1,
            externalKey: "story.evening-garden.updated",
            active: false,
            listeningCoverAssetId: 702,
            hasListeningCover: true,
          }),
        }),
      ]),
    );
  });

  it("sends independently selected lullaby listing and playback covers", async () => {
    const queryClient = new QueryClient();
    contentAdminApiMock.updateContent.mockResolvedValue({
      contentId: 9,
      type: "LULLABY",
      externalKey: "lullaby.covers",
      active: true,
      ageRange: null,
      pageCount: null,
      textlessCoverMediaId: null,
      listingCoverMediaId: 701,
      listeningCoverMediaId: 702,
    });
    const { result } = renderHook(
      () => useSaveContent({ mode: "update", contentId: 9 }),
      { wrapper: createWrapper(queryClient) },
    );

    await act(async () => {
      await result.current.mutateAsync({
        type: "LULLABY",
        externalKey: "lullaby.covers",
        ageRange: null,
        active: true,
        textlessCoverMediaId: null,
        listingCoverMediaId: 701,
        listeningCoverMediaId: 702,
      });
    });

    expect(contentAdminApiMock.updateContent).toHaveBeenCalledWith(9, {
      externalKey: "lullaby.covers",
      ageRange: null,
      active: true,
      textlessCoverMediaId: null,
      listingCoverMediaId: 701,
      listeningCoverMediaId: 702,
    });
  });
});
