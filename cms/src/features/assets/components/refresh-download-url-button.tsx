import { RefreshCw } from "lucide-react";

import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import { useRefreshAssetDownloadUrl } from "@/features/assets/mutations/use-refresh-asset-download-url";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";

type RefreshDownloadUrlButtonProps = {
  asset: AssetViewModel;
};

export function RefreshDownloadUrlButton({
  asset,
}: RefreshDownloadUrlButtonProps) {
  const refreshMutation = useRefreshAssetDownloadUrl();

  async function handleRefresh() {
    await toastMutation(refreshMutation.refreshAssetDownloadUrl(asset.id), {
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
