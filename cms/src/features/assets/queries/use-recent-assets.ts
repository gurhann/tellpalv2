import { useQuery } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import {
  mapAdminAssetList,
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

export function useRecentAssets(limit = 24) {
  const query = useQuery({
    queryKey: queryKeys.assets.recent({ limit }),
    queryFn: async () => {
      const response = await assetAdminApi.listRecentAssets(limit);
      return mapAdminAssetList(response);
    },
  });

  return {
    ...query,
    limit,
    assets: query.data ?? ([] as AssetViewModel[]),
    problem: getApiProblem(
      query.error,
      "The recent asset registry could not be loaded from the admin API.",
    ),
  };
}
