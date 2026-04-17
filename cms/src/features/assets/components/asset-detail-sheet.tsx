import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { DetailDrawer } from "@/components/workspace/detail-drawer";
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
    <DetailDrawer
      open={open}
      onOpenChange={onOpenChange}
      title={
        asset
          ? t("assets.table.assetId", { assetId: asset.id })
          : t("assets.assetDetailFallback")
      }
      description={t("assets.cachedUrlDescription")}
      className="w-full sm:max-w-2xl"
    >
      <div className="grid gap-5 pt-5">
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
              <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                <div className="flex flex-wrap gap-2 text-sm">
                  <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 font-medium text-foreground">
                    {asset.kindLabel}
                  </span>
                  <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 font-medium text-foreground">
                    {asset.mediaTypeLabel}
                  </span>
                  <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 text-muted-foreground">
                    {asset.providerLabel}
                  </span>
                  <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 text-muted-foreground">
                    {asset.hasCachedDownloadUrl
                      ? t("assets.available")
                      : t("assets.notCached")}
                  </span>
                </div>
                <p className="mt-3 break-all text-sm font-medium text-foreground">
                  {asset.objectPath}
                </p>
                <p className="mt-2 text-sm text-muted-foreground">
                  {t("assets.created", {
                    value: formatTimestamp(asset.createdAt),
                  })}{" "}
                  •{" "}
                  {t("assets.updated", {
                    value: formatTimestamp(asset.updatedAt),
                  })}
                </p>
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
    </DetailDrawer>
  );
}
