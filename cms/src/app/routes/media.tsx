import { RefreshCw, Search } from "lucide-react";
import { useState } from "react";

import { FilterBar, FilterBarGroup } from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AssetUploadDialog } from "@/features/assets/components/asset-upload-dialog";
import { AssetDetailSheet } from "@/features/assets/components/asset-detail-sheet";
import { AssetTable } from "@/features/assets/components/asset-table";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useI18n } from "@/i18n/locale-provider";

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
  const { locale } = useI18n();
  const [selectedAssetId, setSelectedAssetId] = useState<number | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const recentAssetsQuery = useRecentAssets(RECENT_ASSET_LIMIT);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Medya",
          title: "Medya Aracı",
          description:
            "Asset yükleyin, listeleyin ve detay metadata'sını düzenleyin.",
          refresh: "Yenile",
          upload: "Asset yükle",
          searchLabel: "Asset kütüphanesinde ara",
          searchPlaceholder: "Object path, asset id veya checksum ile ara",
          filterTypes: "Görsel / Ses / Arşiv",
        }
      : {
          eyebrow: "Media",
          title: "Media Utility",
          description: "Upload assets, inspect them, and edit asset metadata.",
          refresh: "Refresh",
          upload: "Upload asset",
          searchLabel: "Search asset library",
          searchPlaceholder: "Search by object path, asset id, or checksum",
          filterTypes: "Images / Audio / Archives",
        };

  function handleOpenChange(nextOpen: boolean) {
    setIsDetailOpen(nextOpen);

    if (!nextOpen) {
      setSelectedAssetId(null);
    }
  }

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        actions={
          <>
            <Button
              type="button"
              variant="outline"
              onClick={() => void recentAssetsQuery.refetch()}
            >
              <RefreshCw
                className={`size-4 ${
                  recentAssetsQuery.isFetching ? "animate-spin" : ""
                }`}
              />
              {copy.refresh}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsUploadOpen(true)}
            >
              {copy.upload}
            </Button>
          </>
        }
        toolbar={
          <FilterBar aria-label={copy.title}>
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label={copy.searchLabel}
                  className="pl-8"
                  disabled
                  placeholder={copy.searchPlaceholder}
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterTypes}
              </div>
            </FilterBarGroup>
          </FilterBar>
        }
      >
        <AssetTable
          assets={recentAssetsQuery.assets}
          isLoading={recentAssetsQuery.isLoading}
          onAssetSelect={(asset) => {
            setSelectedAssetId(asset.id);
            setIsDetailOpen(true);
          }}
          onRetry={() => void recentAssetsQuery.refetch()}
          problem={recentAssetsQuery.problem}
        />
      </ContentPageShell>

      <AssetDetailSheet
        assetId={selectedAssetId}
        open={isDetailOpen}
        onOpenChange={handleOpenChange}
      />
      <AssetUploadDialog
        open={isUploadOpen}
        onOpenChange={setIsUploadOpen}
        onUploaded={(asset) => {
          setSelectedAssetId(asset.id);
          setIsDetailOpen(true);
        }}
      />
    </>
  );
}
