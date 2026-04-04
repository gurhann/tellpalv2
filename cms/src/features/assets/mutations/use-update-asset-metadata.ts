import { useMutation, useQueryClient } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { mapAdminAsset } from "@/features/assets/model/asset-view-model";
import { syncAssetCaches } from "@/features/assets/lib/asset-cache";
import type { AssetMetadataFormValues } from "@/features/assets/schema/asset-schema";

export function useUpdateAssetMetadata(assetId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (values: AssetMetadataFormValues) => {
      const response = await assetAdminApi.updateAssetMetadata(assetId, {
        mimeType: values.mimeType,
        byteSize: values.byteSize,
        checksumSha256: values.checksumSha256,
      });

      return mapAdminAsset(response);
    },
    onSuccess: async (savedAsset) => {
      await syncAssetCaches(queryClient, savedAsset);
    },
  });
}
