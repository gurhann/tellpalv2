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
import { AssetPreviewCard } from "@/features/assets/components/asset-preview-card";
import { RefreshDownloadUrlButton } from "@/features/assets/components/refresh-download-url-button";
import { useAssetDetail } from "@/features/assets/queries/use-asset-detail";
import { useI18n } from "@/i18n/locale-provider";

type AssetDetailSheetProps = {
  assetId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function AssetDetailSheet({
  assetId,
  open,
  onOpenChange,
}: AssetDetailSheetProps) {
  const { t, formatDateTime } = useI18n();
  const assetDetailQuery = useAssetDetail(open ? assetId : null);
  const asset = assetDetailQuery.asset;
  const formatTimestamp = (value: string | null) => {
    if (!value) {
      return t("assets.notAvailable");
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return formatDateTime(date, {
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        className="w-full overflow-y-auto sm:max-w-2xl"
        side="right"
      >
        <SheetHeader className="border-b border-border/60">
          <SheetTitle>
            {asset
              ? t("assets.table.assetId", { assetId: asset.id })
              : t("assets.assetDetailFallback")}
          </SheetTitle>
          <SheetDescription>
            {t("assets.cachedUrlDescription")}
          </SheetDescription>
        </SheetHeader>

        <div className="grid gap-5 p-4">
          {assetDetailQuery.isLoading ? (
            <EmptyState
              className="min-h-56"
              description={t("assets.loadingDetailDescription")}
              title={t("assets.loadingDetail")}
            />
          ) : assetDetailQuery.problem ? (
            <ProblemAlert problem={assetDetailQuery.problem} />
          ) : asset ? (
            <>
              <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 sm:grid-cols-2">
                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    {t("assets.assetIdentity")}
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
                    {t("assets.provider")}
                  </p>
                  <p className="mt-3 text-sm font-medium text-foreground">
                    {asset.providerLabel}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {t("assets.created", {
                      value: formatTimestamp(asset.createdAt),
                    })}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    {t("assets.cachedDownloadUrl")}
                  </p>
                  <p className="mt-3 flex items-center gap-2 text-sm font-medium text-foreground">
                    <Link2 className="size-4 text-muted-foreground" />
                    {asset.hasCachedDownloadUrl
                      ? t("assets.available")
                      : t("assets.notCached")}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {t("assets.expires", {
                      value: formatTimestamp(asset.downloadUrlExpiresAt),
                    })}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                    {t("assets.metadataSnapshot")}
                  </p>
                  <p className="mt-3 flex items-center gap-2 text-sm font-medium text-foreground">
                    <PackageOpen className="size-4 text-muted-foreground" />
                    {asset.hasMetadata
                      ? t("assets.metadataPresent")
                      : t("assets.metadataPending")}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {t("assets.updated", {
                      value: formatTimestamp(asset.updatedAt),
                    })}
                  </p>
                </div>
              </div>

              <AssetPreviewCard asset={asset} open={open} />

              <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
                <div className="mb-4 space-y-1">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    {t("assets.metadataTitle")}
                  </h2>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {t("assets.metadataDescription")}
                  </p>
                </div>

                <AssetMetadataForm asset={asset} />
              </div>

              <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
                <div className="mb-4 space-y-1">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    {t("assets.cachedUrlTitle")}
                  </h2>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {t("assets.cachedUrlDescription")}
                  </p>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm text-muted-foreground">
                    {t("assets.lastCached", {
                      value: formatTimestamp(asset.downloadUrlCachedAt),
                    })}
                  </div>
                  <RefreshDownloadUrlButton asset={asset} />
                </div>
              </div>
            </>
          ) : (
            <EmptyState
              className="min-h-56"
              description={t("assets.noAssetSelectedDescription")}
              title={t("assets.noAssetSelected")}
            />
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
