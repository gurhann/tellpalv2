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

type UseRecentAudioAssetsOptions = {
  enabled?: boolean;
  limit?: number;
};

export function useRecentAudioAssets(
  options: UseRecentAudioAssetsOptions = {},
) {
  const { enabled = true, limit = 12 } = options;

  const query = useQuery({
    queryKey: queryKeys.assets.recent({ limit, mediaType: "AUDIO" }),
    enabled,
    queryFn: async () => {
      const assets = await assetAdminApi.listRecentAssets(limit);
      return assets.filter((asset) => asset.mediaType === "AUDIO");
    },
  });

  return {
    ...query,
    assets: (query.data ?? []) as AdminAssetResponse[],
    problem: getApiProblem(
      query.error,
      "Recent audio assets could not be loaded from the admin API.",
    ),
  };
}
