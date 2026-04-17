import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  assetProcessingAdminApi,
  type RetryAssetProcessingInput,
  type ScheduleAssetProcessingInput,
} from "@/features/assets/api/asset-processing-admin";
import {
  mapAdminAssetProcessing,
  type AssetProcessingJobViewModel,
} from "@/features/assets/model/asset-view-model";
import { queryKeys } from "@/lib/query-keys";

type RetryProcessingVariables = {
  contentId: number;
  languageCode: string;
  input: RetryAssetProcessingInput;
};

async function syncProcessingCaches(
  queryClient: ReturnType<typeof useQueryClient>,
  job: AssetProcessingJobViewModel,
) {
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: queryKeys.assets.processingRecentRoot(),
    }),
    queryClient.invalidateQueries({
      queryKey: queryKeys.assets.processingStatus(
        job.contentId,
        job.languageCode,
      ),
    }),
  ]);
}

export function useProcessingActions() {
  const queryClient = useQueryClient();

  const scheduleProcessing = useMutation({
    mutationFn: async (input: ScheduleAssetProcessingInput) => {
      const response = await assetProcessingAdminApi.scheduleProcessing(input);
      return mapAdminAssetProcessing(response);
    },
    onSuccess: async (job) => {
      await syncProcessingCaches(queryClient, job);
    },
  });

  const retryProcessing = useMutation({
    mutationFn: async ({
      contentId,
      languageCode,
      input,
    }: RetryProcessingVariables) => {
      const response = await assetProcessingAdminApi.retryProcessing(
        contentId,
        languageCode,
        input,
      );
      return mapAdminAssetProcessing(response);
    },
    onSuccess: async (job) => {
      await syncProcessingCaches(queryClient, job);
    },
  });

  return {
    scheduleProcessing,
    retryProcessing,
    isPending: scheduleProcessing.isPending || retryProcessing.isPending,
  };
}
