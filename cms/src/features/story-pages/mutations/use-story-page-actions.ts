import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  storyPageAdminApi,
  type AddStoryPageInput,
  type AdminStoryPageResponse,
  type UpdateStoryPageInput,
} from "@/features/contents/api/story-page-admin";
import { queryKeys } from "@/lib/query-keys";

type UseStoryPageActionsOptions = {
  contentId: number;
  onAddSuccess?: (storyPage: AdminStoryPageResponse) => void;
  onUpdateSuccess?: (storyPage: AdminStoryPageResponse) => void;
  onDeleteSuccess?: (pageNumber: number) => void;
};

export function useStoryPageActions({
  contentId,
  onAddSuccess,
  onUpdateSuccess,
  onDeleteSuccess,
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

  return {
    addStoryPage,
    updateStoryPage,
    removeStoryPage,
    isPending:
      addStoryPage.isPending ||
      updateStoryPage.isPending ||
      removeStoryPage.isPending,
  };
}
