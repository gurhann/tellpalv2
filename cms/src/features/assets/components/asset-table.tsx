import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type AssetTableProps = {
  assets: AssetViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onAssetSelect?: (asset: AssetViewModel) => void;
};

function createColumns(
  t: ReturnType<typeof useI18n>["t"],
  formatBytes: ReturnType<typeof useI18n>["formatBytes"],
): DataTableColumn<AssetViewModel>[] {
  return [
    {
      id: "asset",
      header: t("assets.table.asset"),
      cell: (asset) => (
        <div className="space-y-1">
          <p className="break-all font-medium text-foreground">
            {asset.objectPath}
          </p>
          <p className="text-xs text-muted-foreground">
            {t("assets.table.assetId", { assetId: asset.id })}
          </p>
        </div>
      ),
    },
    {
      id: "kind",
      header: t("assets.table.kind"),
      cell: (asset) => (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
          {asset.kindLabel}
        </span>
      ),
    },
    {
      id: "provider",
      header: t("assets.table.provider"),
      cell: (asset) => (
        <span className="text-sm text-muted-foreground">
          {asset.providerLabel}
        </span>
      ),
    },
    {
      id: "mediaType",
      header: t("assets.table.mediaType"),
      cell: (asset) => (
        <span
          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
            asset.mediaType === "IMAGE"
              ? "border-sky-200 bg-sky-50 text-sky-800"
              : asset.mediaType === "AUDIO"
                ? "border-violet-200 bg-violet-50 text-violet-800"
                : "border-amber-200 bg-amber-50 text-amber-800"
          }`}
        >
          {asset.mediaTypeLabel}
        </span>
      ),
    },
    {
      id: "mimeType",
      header: t("assets.table.mimeType"),
      cell: (asset) => (
        <div className="space-y-1">
          <p className="text-sm text-foreground">
            {asset.mimeType ?? t("assets.table.metadataPending")}
          </p>
          <p className="text-xs text-muted-foreground">
            {asset.byteSize === null
              ? t("assets.table.byteSizeUnavailable")
              : formatBytes(asset.byteSize)}
          </p>
        </div>
      ),
    },
  ];
}

export function AssetTable({
  assets,
  isLoading = false,
  problem = null,
  onRetry,
  onAssetSelect,
}: AssetTableProps) {
  const { t, formatBytes } = useI18n();
  const columns = createColumns(t, formatBytes);

  if (problem && assets.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription={t("assets.table.errorDescription")}
        emptyTitle={t("assets.table.errorTitle")}
        getRowId={(asset) => asset.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption={t("assets.table.toolbarTitle")}
      columns={columns}
      emptyDescription={t("assets.table.emptyDescription")}
      emptyTitle={t("assets.table.emptyTitle")}
      getRowId={(asset) => asset.id.toString()}
      isLoading={isLoading}
      loadingDescription={t("assets.table.loadingDescription")}
      loadingTitle={t("assets.table.loadingTitle")}
      onRetry={onRetry}
      onRowClick={onAssetSelect}
      problem={assets.length > 0 ? problem : null}
      rows={assets}
    />
  );
}
