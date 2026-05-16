import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  storyPageAdminApi,
  type AddStoryPageInput,
  type AdminStoryPageLocalizationResponse,
  type AdminStoryPageResponse,
  type UpdateStoryPageInput,
} from "@/features/contents/api/story-page-admin";
import { queryKeys } from "@/lib/query-keys";

type UseStoryPageActionsOptions = {
  contentId: number;
  onAddSuccess?: (storyPage: AdminStoryPageResponse) => void;
  onUpdateSuccess?: (storyPage: AdminStoryPageResponse) => void;
  onDeleteSuccess?: (pageNumber: number) => void;
  onLocalizationSuccess?: (
    localization: AdminStoryPageLocalizationResponse,
  ) => void;
};

export function useStoryPageActions({
  contentId,
  onAddSuccess,
  onUpdateSuccess,
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

  const updateStoryPage = useMutation({
    mutationFn: async ({
      pageNumber,
      input,
    }: {
      pageNumber: number;
      input: UpdateStoryPageInput;
    }) => storyPageAdminApi.updateStoryPage(contentId, pageNumber, input),
    onSuccess: async (storyPage) => {
      await invalidateStoryPageQueries(storyPage.pageNumber);
      onUpdateSuccess?.(storyPage);
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

  const exportTextlessIllustrations = useMutation({
    mutationFn: async () => {
      const download =
        await storyPageAdminApi.exportTextlessIllustrations(contentId);
      const fileName =
        download.fileName ?? `content-${contentId}-textless-story-pages.zip`;
      const objectUrl = URL.createObjectURL(download.blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = fileName;
      document.body.append(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
      return fileName;
    },
  });

  return {
    addStoryPage,
    updateStoryPage,
    removeStoryPage,
    upsertStoryPageLocalization,
    exportTextlessIllustrations,
    isPending:
      addStoryPage.isPending ||
      updateStoryPage.isPending ||
      removeStoryPage.isPending ||
      upsertStoryPageLocalization.isPending,
  };
}
