import { useQuery } from "@tanstack/react-query";

import { assetProcessingAdminApi } from "@/features/assets/api/asset-processing-admin";
import {
  mapAdminAssetProcessing,
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

function isProcessingNotFoundProblem(problem: ApiProblemDetail | null) {
  return (
    problem?.status === 404 &&
    (problem.errorCode === "asset_processing_not_found" ||
      problem.path?.includes("/api/admin/media-processing/") === true)
  );
}

type UseProcessingStatusOptions = {
  contentId: number | null;
  languageCode?: string | null;
  enabled?: boolean;
};

export function useProcessingStatus({
  contentId,
  languageCode,
  enabled = true,
}: UseProcessingStatusOptions) {
  const normalizedLanguageCode = languageCode?.trim().toLowerCase() ?? "";
  const canQuery =
    enabled &&
    typeof contentId === "number" &&
    contentId > 0 &&
    normalizedLanguageCode.length > 0;
  const query = useQuery({
    queryKey: queryKeys.assets.processingStatus(
      contentId ?? 0,
      normalizedLanguageCode,
    ),
    enabled: canQuery,
    queryFn: async () => {
      const response = await assetProcessingAdminApi.getProcessingStatus(
        contentId as number,
        normalizedLanguageCode,
      );
      return mapAdminAssetProcessing(response);
    },
  });
  const problem = getApiProblem(
    query.error,
    "The processing status lookup could not be completed.",
  );

  return {
    ...query,
    job: (query.data ?? null) as AssetProcessingJobViewModel | null,
    problem,
    isNotScheduled: isProcessingNotFoundProblem(problem),
  };
}
