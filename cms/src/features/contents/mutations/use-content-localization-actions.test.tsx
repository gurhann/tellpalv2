import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminContentLocalizationResponse } from "@/features/contents/api/content-admin";
import {
  inactiveContentViewModel,
  storyContentViewModel,
  storyContentReadResponse,
} from "@/features/contents/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useContentLocalizationActions } from "./use-content-localization-actions";

const contentAdminApiMock = vi.hoisted(() => ({
  createLocalization: vi.fn(),
  updateLocalization: vi.fn(),
  publishLocalization: vi.fn(),
  archiveLocalization: vi.fn(),
}));

vi.mock("@/features/contents/api/content-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contents/api/content-admin")
  >("@/features/contents/api/content-admin");

  return {
    ...actual,
    contentAdminApi: {
      ...actual.contentAdminApi,
      createLocalization: contentAdminApiMock.createLocalization,
      updateLocalization: contentAdminApiMock.updateLocalization,
      publishLocalization: contentAdminApiMock.publishLocalization,
      archiveLocalization: contentAdminApiMock.archiveLocalization,
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

function makeLocalizationResponse(
  overrides: Partial<AdminContentLocalizationResponse> = {},
): AdminContentLocalizationResponse {
  return {
    ...storyContentReadResponse.localizations[1],
    ...overrides,
  };
}

beforeEach(() => {
  contentAdminApiMock.createLocalization.mockReset();
  contentAdminApiMock.updateLocalization.mockReset();
  contentAdminApiMock.publishLocalization.mockReset();
  contentAdminApiMock.archiveLocalization.mockReset();
});

describe("useContentLocalizationActions", () => {
  it("creates a localization and updates content list/detail caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
    const createdLocalization: AdminContentLocalizationResponse = {
      contentId: 4,
      languageCode: "en",
      title: "Moon Softly",
      description: "A soft lullaby for early sleep.",
      bodyText: "Sleep softly under a quiet moon.",
      coverMediaId: null,
      audioMediaId: 3,
      durationMinutes: 5,
      status: "DRAFT",
      processingStatus: "PENDING",
      publishedAt: null,
      visibleToMobile: false,
    };

    queryClient.setQueryData(queryKeys.contents.list(), [
      inactiveContentViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.contents.detail(4),
      inactiveContentViewModel,
    );
    contentAdminApiMock.createLocalization.mockResolvedValue(
      createdLocalization,
    );

    const { result } = renderHook(() => useContentLocalizationActions(4), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.saveLocalization.mutateAsync({
        mode: "create",
        values: {
          languageCode: "en",
          title: "Moon Softly",
          description: "A soft lullaby for early sleep.",
          bodyText: "Sleep softly under a quiet moon.",
          coverMediaId: null,
          audioMediaId: 3,
          durationMinutes: 5,
          status: "DRAFT",
          processingStatus: "PENDING",
          publishedAt: null,
        },
      });
    });

    expect(contentAdminApiMock.createLocalization).toHaveBeenCalledWith(
      4,
      "en",
      {
        title: "Moon Softly",
        description: "A soft lullaby for early sleep.",
        bodyText: "Sleep softly under a quiet moon.",
        coverMediaId: null,
        audioMediaId: 3,
        durationMinutes: 5,
        status: "DRAFT",
        processingStatus: "PENDING",
        publishedAt: null,
      },
    );
    expect(
      queryClient.getQueryData<{
        localizationCount: number;
        primaryLocalization: { title: string };
      }>(queryKeys.contents.detail(4)),
    ).toMatchObject({
      localizationCount: 1,
      primaryLocalization: {
        title: "Moon Softly",
      },
    });
    expect(
      queryClient.getQueryData<
        Array<{ summary: { id: number }; localizationCount: number }>
      >(queryKeys.contents.list()),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          summary: expect.objectContaining({ id: 4 }),
          localizationCount: 1,
        }),
      ]),
    );
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.lists(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.detail(4),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.localization(4, "en"),
    });
  });

  it("updates an existing localization in cache after save", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedLocalization = makeLocalizationResponse({
      languageCode: "en",
      title: "Evening Garden Updated",
      description: "Updated English locale",
      status: "PUBLISHED",
      processingStatus: "COMPLETED",
      publishedAt: "2026-03-28T10:30:00Z",
      visibleToMobile: true,
    });

    queryClient.setQueryData(queryKeys.contents.list(), [
      storyContentViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.contents.detail(1),
      storyContentViewModel,
    );
    contentAdminApiMock.updateLocalization.mockResolvedValue(
      updatedLocalization,
    );

    const { result } = renderHook(() => useContentLocalizationActions(1), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.saveLocalization.mutateAsync({
        mode: "update",
        values: {
          languageCode: "en",
          title: "Evening Garden Updated",
          description: "Updated English locale",
          bodyText: null,
          coverMediaId: null,
          audioMediaId: null,
          durationMinutes: 8,
          status: "PUBLISHED",
          processingStatus: "COMPLETED",
          publishedAt: "2026-03-28T13:30",
        },
      });
    });

    expect(contentAdminApiMock.updateLocalization).toHaveBeenCalledWith(
      1,
      "en",
      {
        title: "Evening Garden Updated",
        description: "Updated English locale",
        bodyText: null,
        coverMediaId: null,
        audioMediaId: null,
        durationMinutes: 8,
        status: "PUBLISHED",
        processingStatus: "COMPLETED",
        publishedAt: "2026-03-28T10:30:00.000Z",
      },
    );
    expect(
      queryClient.getQueryData<{
        localizations: Array<{ languageCode: string; title: string }>;
      }>(queryKeys.contents.detail(1)),
    ).toMatchObject({
      localizations: expect.arrayContaining([
        expect.objectContaining({
          languageCode: "en",
          title: "Evening Garden Updated",
        }),
      ]),
    });
  });

  it("publishes and archives a locale while keeping caches in sync", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const publishedLocalization = makeLocalizationResponse({
      status: "PUBLISHED",
      processingStatus: "COMPLETED",
      publishedAt: "2026-03-28T11:00:00Z",
      visibleToMobile: true,
    });
    const archivedLocalization = makeLocalizationResponse({
      status: "ARCHIVED",
      processingStatus: "COMPLETED",
      publishedAt: "2026-03-28T11:00:00Z",
      visibleToMobile: false,
    });

    queryClient.setQueryData(queryKeys.contents.list(), [
      storyContentViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.contents.detail(1),
      storyContentViewModel,
    );
    contentAdminApiMock.publishLocalization.mockResolvedValue(
      publishedLocalization,
    );
    contentAdminApiMock.archiveLocalization.mockResolvedValue(
      archivedLocalization,
    );

    const { result } = renderHook(() => useContentLocalizationActions(1), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.publishLocalization.mutateAsync({
        languageCode: "tr",
      });
      await result.current.archiveLocalization.mutateAsync("tr");
    });

    expect(contentAdminApiMock.publishLocalization).toHaveBeenCalledWith(
      1,
      "tr",
      {
        publishedAt: null,
      },
    );
    expect(contentAdminApiMock.archiveLocalization).toHaveBeenCalledWith(
      1,
      "tr",
    );
    expect(
      queryClient.getQueryData<{
        localizations: Array<{
          languageCode: string;
          status: string;
          visibleToMobile: boolean;
        }>;
      }>(queryKeys.contents.detail(1)),
    ).toMatchObject({
      localizations: expect.arrayContaining([
        expect.objectContaining({
          languageCode: "tr",
          status: "ARCHIVED",
          visibleToMobile: false,
        }),
      ]),
    });
  });
});
