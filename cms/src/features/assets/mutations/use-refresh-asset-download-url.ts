import { useMutation, useQueryClient } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { syncAssetCaches } from "@/features/assets/lib/asset-cache";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

function getApiProblem(error: unknown) {
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
    detail:
      error instanceof Error
        ? error.message
        : "The cached download URL could not be refreshed.",
  } satisfies ApiProblemDetail;
}

export function useRefreshAssetDownloadUrl() {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: async (assetId: number): Promise<AssetViewModel> => {
      const response = await assetAdminApi.refreshDownloadUrlCache(assetId);
      return mapAdminAsset(response);
    },
    onSuccess: async (savedAsset) => {
      await syncAssetCaches(queryClient, savedAsset);
    },
  });

  return {
    ...mutation,
    refreshAssetDownloadUrl: mutation.mutateAsync,
    problem: getApiProblem(mutation.error),
  };
}
