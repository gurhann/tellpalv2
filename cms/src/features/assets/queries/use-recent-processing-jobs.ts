import { useQuery } from "@tanstack/react-query";

import { assetProcessingAdminApi } from "@/features/assets/api/asset-processing-admin";
import {
  mapAdminAssetProcessingList,
  type AssetProcessingJobViewModel,
} from "@/features/assets/model/asset-view-model";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

function getApiProblem(error: unknown, fallbackDetail: string) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: error instanceof Error ? error.message : fallbackDetail,
  } satisfies ApiProblemDetail;
}

export function useRecentProcessingJobs(limit = 20) {
  const query = useQuery({
    queryKey: queryKeys.assets.processingRecent({ limit }),
    queryFn: async () => {
      const response = await assetProcessingAdminApi.listRecentProcessing(limit);
      return mapAdminAssetProcessingList(response);
    },
  });

  return {
    ...query,
    limit,
    jobs: query.data ?? ([] as AssetProcessingJobViewModel[]),
    problem: getApiProblem(
      query.error,
      "The processing console could not load recent jobs from the admin API.",
    ),
  };
}
