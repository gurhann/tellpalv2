import { useMutation, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";

import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { syncAssetCaches } from "@/features/assets/lib/asset-cache";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";

type RefreshDownloadUrlButtonProps = {
  asset: AssetViewModel;
};

export function RefreshDownloadUrlButton({
  asset,
}: RefreshDownloadUrlButtonProps) {
  const queryClient = useQueryClient();
  const refreshMutation = useMutation({
    mutationFn: async () => {
      const response = await assetAdminApi.refreshDownloadUrlCache(asset.id);
      return mapAdminAsset(response);
    },
    onSuccess: async (savedAsset) => {
      await syncAssetCaches(queryClient, savedAsset);
    },
  });

  async function handleRefresh() {
    await toastMutation(refreshMutation.mutateAsync(), {
      loading: "Refreshing cached download URL...",
      success: "Cached download URL refreshed.",
    });
  }

  return (
    <Button
      type="button"
      variant="outline"
      onClick={() => void handleRefresh()}
      disabled={refreshMutation.isPending}
    >
      <RefreshCw
        className={`size-4 ${refreshMutation.isPending ? "animate-spin" : ""}`}
      />
      Refresh cached URL
    </Button>
  );
}
