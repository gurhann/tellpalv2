import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { FilterBarSummary } from "@/components/data/filter-bar";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import type { ApiProblemDetail } from "@/types/api";

type AssetTableProps = {
  assets: AssetViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
};

const columns: DataTableColumn<AssetViewModel>[] = [
  {
    id: "asset",
    header: "Asset",
    cell: (asset) => (
      <div className="space-y-1">
        <p className="font-medium text-foreground break-all">
          {asset.objectPath}
        </p>
        <p className="text-xs text-muted-foreground">Asset #{asset.id}</p>
      </div>
    ),
  },
  {
    id: "kind",
    header: "Kind",
    cell: (asset) => (
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
        {asset.kindLabel}
      </span>
    ),
  },
  {
    id: "provider",
    header: "Provider",
    cell: (asset) => (
      <span className="text-sm text-muted-foreground">
        {asset.providerLabel}
      </span>
    ),
  },
  {
    id: "mediaType",
    header: "Media Type",
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
    header: "MIME Type",
    cell: (asset) => (
      <div className="space-y-1">
        <p className="text-sm text-foreground">
          {asset.mimeType ?? "Metadata pending"}
        </p>
        <p className="text-xs text-muted-foreground">
          {asset.byteSize === null
            ? "Byte size unavailable"
            : `${asset.byteSize.toLocaleString()} bytes`}
        </p>
      </div>
    ),
  },
];

export function AssetTable({
  assets,
  isLoading = false,
  problem = null,
  onRetry,
}: AssetTableProps) {
  const cachedUrlCount = assets.filter(
    (asset) => asset.hasCachedDownloadUrl,
  ).length;
  const imageCount = assets.filter(
    (asset) => asset.mediaType === "IMAGE",
  ).length;

  if (problem && assets.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription="The recent asset registry could not be loaded from the admin API."
        emptyTitle="Asset registry unavailable"
        getRowId={(asset) => asset.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption="Recent asset registry"
      columns={columns}
      emptyDescription="No media assets are available yet. Asset registration and picker flows arrive in the next asset tasks."
      emptyTitle="No media assets yet"
      getRowId={(asset) => asset.id.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting recent media asset metadata from the admin API."
      loadingTitle="Loading asset registry"
      onRetry={onRetry}
      problem={assets.length > 0 ? problem : null}
      rows={assets}
      summary={
        <div className="space-y-1 text-right">
          <p className="text-sm font-medium tracking-tight text-foreground">
            {assets.length} asset{assets.length === 1 ? "" : "s"}
          </p>
          <p className="text-xs text-muted-foreground">
            {imageCount} images / {cachedUrlCount} cached URLs
          </p>
        </div>
      }
      toolbar={
        <FilterBarSummary
          description="Recent asset metadata is now bound to the shared library table."
          title="Asset registry"
        />
      }
    />
  );
}
