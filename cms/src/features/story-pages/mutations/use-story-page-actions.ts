import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  storyPageAdminApi,
  type AddStoryPageInput,
  type AdminStoryPageLocalizationResponse,
  type AdminStoryPageResponse,
} from "@/features/contents/api/story-page-admin";
import { queryKeys } from "@/lib/query-keys";

type UseStoryPageActionsOptions = {
  contentId: number;
  onAddSuccess?: (storyPage: AdminStoryPageResponse) => void;
  onDeleteSuccess?: (pageNumber: number) => void;
  onLocalizationSuccess?: (
    localization: AdminStoryPageLocalizationResponse,
  ) => void;
};

export function useStoryPageActions({
  contentId,
  onAddSuccess,
  onDeleteSuccess,
  onLocalizationSuccess,
}: UseStoryPageActionsOptions) {
  const queryClient = useQueryClient();

  async function invalidateStoryPageQueries(pageNumber?: number) {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.contents.storyPages(contentId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.contents.detail(contentId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.contents.lists(),
      }),
      ...(typeof pageNumber === "number"
        ? [
            queryClient.invalidateQueries({
              queryKey: queryKeys.contents.storyPage(contentId, pageNumber),
            }),
          ]
        : []),
    ]);
  }

  const addStoryPage = useMutation({
    mutationFn: async (input: AddStoryPageInput) =>
      storyPageAdminApi.addStoryPage(contentId, input),
    onSuccess: async (storyPage) => {
      await invalidateStoryPageQueries(storyPage.pageNumber);
      onAddSuccess?.(storyPage);
    },
  });

  const removeStoryPage = useMutation({
    mutationFn: async (pageNumber: number) => {
      await storyPageAdminApi.removeStoryPage(contentId, pageNumber);
      return pageNumber;
    },
    onSuccess: async (pageNumber) => {
      await invalidateStoryPageQueries(pageNumber);
      onDeleteSuccess?.(pageNumber);
    },
  });

  const upsertStoryPageLocalization = useMutation({
    mutationFn: async ({
      pageNumber,
      languageCode,
      input,
    }: {
      pageNumber: number;
      languageCode: string;
      input: {
        bodyText?: string | null;
        audioMediaId?: number | null;
        illustrationMediaId: number;
      };
    }) =>
      storyPageAdminApi.upsertStoryPageLocalization(
        contentId,
        pageNumber,
        languageCode,
        input,
      ),
    onSuccess: async (localization) => {
      await Promise.all([
        invalidateStoryPageQueries(localization.pageNumber),
        queryClient.invalidateQueries({
          queryKey: queryKeys.contents.storyPageLocalization(
            contentId,
            localization.pageNumber,
            localization.languageCode,
          ),
        }),
      ]);
      onLocalizationSuccess?.(localization);
    },
  });

  return {
    addStoryPage,
    removeStoryPage,
    upsertStoryPageLocalization,
    isPending:
      addStoryPage.isPending ||
      removeStoryPage.isPending ||
      upsertStoryPageLocalization.isPending,
  };
}
