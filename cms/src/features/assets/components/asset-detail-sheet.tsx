import { Link2, PackageOpen } from "lucide-react";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { AssetMetadataForm } from "@/features/assets/components/asset-metadata-form";
import { RefreshDownloadUrlButton } from "@/features/assets/components/refresh-download-url-button";
import { useAssetDetail } from "@/features/assets/queries/use-asset-detail";

type AssetDetailSheetProps = {
  assetId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function formatTimestamp(value: string | null) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("en-GB", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function AssetDetailSheet({
  assetId,
  open,
  onOpenChange,
}: AssetDetailSheetProps) {
  const assetDetailQuery = useAssetDetail(open ? assetId : null);
  const asset = assetDetailQuery.asset;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        className="w-full overflow-y-auto sm:max-w-2xl"
        side="right"
      >
        <SheetHeader className="border-b border-border/60">
          <SheetTitle>
            {asset ? `Asset #${asset.id}` : "Asset detail"}
          </SheetTitle>
          <SheetDescription>
            Inspect the selected asset, update its stored metadata, and refresh
            the cached download URL snapshot from this sheet.
          </SheetDescription>
        </SheetHeader>

        <div className="grid gap-5 p-4">
          {assetDetailQuery.isLoading ? (
            <EmptyState
              className="min-h-56"
              description="The CMS is requesting the selected asset record from the admin API."
              title="Loading asset detail"
            />
          ) : assetDetailQuery.problem ? (
            <ProblemAlert problem={assetDetailQuery.problem} />
          ) : asset ? (
            <>
              <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 sm:grid-cols-2">
                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    Asset identity
                  </p>
                  <p className="mt-3 break-all text-sm font-medium text-foreground">
                    {asset.objectPath}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {asset.kindLabel} / {asset.mediaTypeLabel}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    Provider
                  </p>
                  <p className="mt-3 text-sm font-medium text-foreground">
                    {asset.providerLabel}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Created {formatTimestamp(asset.createdAt)}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    Cached download URL
                  </p>
                  <p className="mt-3 flex items-center gap-2 text-sm font-medium text-foreground">
                    <Link2 className="size-4 text-muted-foreground" />
                    {asset.hasCachedDownloadUrl ? "Available" : "Not cached"}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Expires {formatTimestamp(asset.downloadUrlExpiresAt)}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    Metadata snapshot
                  </p>
                  <p className="mt-3 flex items-center gap-2 text-sm font-medium text-foreground">
                    <PackageOpen className="size-4 text-muted-foreground" />
                    {asset.hasMetadata
                      ? "Metadata present"
                      : "Metadata pending"}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Updated {formatTimestamp(asset.updatedAt)}
                  </p>
                </div>
              </div>

              <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
                <div className="mb-4 space-y-1">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    Metadata
                  </h2>
                  <p className="text-sm leading-6 text-muted-foreground">
                    Update MIME type, byte size, and checksum while keeping
                    provider and object path read-only in this task.
                  </p>
                </div>

                <AssetMetadataForm asset={asset} />
              </div>

              <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
                <div className="mb-4 space-y-1">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    Cached download URL
                  </h2>
                  <p className="text-sm leading-6 text-muted-foreground">
                    Refresh the stored signed URL snapshot without leaving the
                    asset detail workspace.
                  </p>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm text-muted-foreground">
                    Last cached: {formatTimestamp(asset.downloadUrlCachedAt)}
                  </div>
                  <RefreshDownloadUrlButton asset={asset} />
                </div>
              </div>
            </>
          ) : (
            <EmptyState
              className="min-h-56"
              description="Select an asset from the recent registry to inspect its metadata."
              title="No asset selected"
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
