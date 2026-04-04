import { useMutation, useQueryClient } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";
import type { AssetMetadataFormValues } from "@/features/assets/schema/asset-schema";
import { queryKeys } from "@/lib/query-keys";

function updateAssetListCache(
  records: AssetViewModel[] | undefined,
  savedAsset: AssetViewModel,
) {
  const currentRecords = records ?? [];
  const existingIndex = currentRecords.findIndex(
    (record) => record.id === savedAsset.id,
  );

  if (existingIndex === -1) {
    return currentRecords;
  }

  return currentRecords.map((record) =>
    record.id === savedAsset.id ? savedAsset : record,
  );
}

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
      const detailKey = queryKeys.assets.detail(savedAsset.id);

      queryClient.setQueriesData<AssetViewModel[]>(
        { queryKey: ["assets", "recent"] },
        (records) => updateAssetListCache(records, savedAsset),
      );
      queryClient.setQueryData<AssetViewModel>(detailKey, savedAsset);

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["assets", "recent"],
        }),
        queryClient.invalidateQueries({
          queryKey: detailKey,
        }),
      ]);
    },
  });
}
