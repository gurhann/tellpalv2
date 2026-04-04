import { useQuery } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import {
  mapAdminAsset,
  type AssetViewModel,
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

function isAssetNotFoundProblem(problem: ApiProblemDetail | null) {
  return (
    problem?.status === 404 &&
    (problem.errorCode === "asset_not_found" ||
      problem.path?.includes("/api/admin/media/") === true)
  );
}

export function useAssetDetail(assetId: number | null) {
  const query = useQuery({
    queryKey: queryKeys.assets.detail(assetId ?? 0),
    enabled: typeof assetId === "number" && assetId > 0,
    queryFn: async () => {
      const response = await assetAdminApi.getAsset(assetId as number);
      return mapAdminAsset(response);
    },
  });

  const problem = getApiProblem(
    query.error,
    "The selected asset could not be loaded from the admin API.",
  );

  return {
    ...query,
    asset: (query.data ?? null) as AssetViewModel | null,
    problem,
    isNotFound: isAssetNotFoundProblem(problem),
  };
}
