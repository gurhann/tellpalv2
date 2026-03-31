import { useQuery } from "@tanstack/react-query";

import {
  assetAdminApi,
  type AdminAssetResponse,
} from "@/features/assets/api/asset-admin";
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

type UseRecentImageAssetsOptions = {
  enabled?: boolean;
  limit?: number;
};

export function useRecentImageAssets(
  options: UseRecentImageAssetsOptions = {},
) {
  const { enabled = true, limit = 12 } = options;

  const query = useQuery({
    queryKey: queryKeys.assets.recent({ limit, mediaType: "IMAGE" }),
    enabled,
    queryFn: async () => {
      const assets = await assetAdminApi.listRecentAssets(limit);
      return assets.filter((asset) => asset.mediaType === "IMAGE");
    },
  });

  return {
    ...query,
    assets: (query.data ?? []) as AdminAssetResponse[],
    problem: getApiProblem(
      query.error,
      "Recent image assets could not be loaded from the admin API.",
    ),
  };
}
