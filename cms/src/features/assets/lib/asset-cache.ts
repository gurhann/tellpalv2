import type { QueryClient } from "@tanstack/react-query";

import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
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

export async function syncAssetCaches(
  queryClient: QueryClient,
  savedAsset: AssetViewModel,
) {
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
}
