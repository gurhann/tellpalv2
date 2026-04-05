import { RefreshCw, Search } from "lucide-react";
import { useState } from "react";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { AssetUploadDialog } from "@/features/assets/components/asset-upload-dialog";
import { AssetDetailSheet } from "@/features/assets/components/asset-detail-sheet";
import { AssetTable } from "@/features/assets/components/asset-table";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useI18n } from "@/i18n/locale-provider";

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
  const { locale, formatNumber } = useI18n();
  const [selectedAssetId, setSelectedAssetId] = useState<number | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const recentAssetsQuery = useRecentAssets(RECENT_ASSET_LIMIT);
  const assetCount = recentAssetsQuery.assets.length;
  const imageCount = recentAssetsQuery.assets.filter(
    (asset) => asset.mediaType === "IMAGE",
  ).length;
  const audioCount = recentAssetsQuery.assets.filter(
    (asset) => asset.mediaType === "AUDIO",
  ).length;
  const archiveCount = recentAssetsQuery.assets.filter(
    (asset) => asset.mediaType === "ARCHIVE",
  ).length;
  const cachedUrlCount = recentAssetsQuery.assets.filter(
    (asset) => asset.hasCachedDownloadUrl,
  ).length;
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Medya",
          title: "Medya Aracı",
          description:
            "Global asset'ler için gelişmiş kayıt, hata ayıklama, önizleme ve manuel yükleme yüzeyi. Normal editoryal bağlama artık içerik, kategori ve hikâye sayfası editörlerinde yapılır.",
          refresh: "Yenile",
          upload: "Asset yükle",
          searchLabel: "Asset kütüphanesinde ara",
          searchPlaceholder: "Object path, asset id veya checksum ile ara",
          filterTypes: "Görsel / Ses / Arşiv",
          filterUtility: "Kayıt, önizleme ve hata ayıklama görünümü",
          filterRecent: `${formatNumber(recentAssetsQuery.limit)} son asset gösteriliyor`,
          summaryDescription:
            "Bu rotayı gelişmiş inceleme için kullanın. Normal içerik/kategori/hikâye akışları artık asset'i yerinde yükler ve bağlar.",
          countLoaded: `${formatNumber(assetCount)} son asset yüklendi`,
          utilitySummary: "Araç özeti",
          utilitySummaryDescription:
            "Asset detay sayfasını açmadan önce provider, medya türü, önizleme durumu ve önbelleklenmiş URL kapsamını inceleyin.",
          loadingAssets: "Son asset görünümü backend'den yükleniyor.",
          loadedAssets: `${formatNumber(imageCount)} görsel, ${formatNumber(audioCount)} ses dosyası ve ${formatNumber(archiveCount)} arşiv mevcut.`,
          noCached:
            "Son pencerede şu anda önbelleklenmiş indirme URL'si görünmüyor.",
          hasCached: `${formatNumber(cachedUrlCount)} son asset zaten önbelleklenmiş indirme URL özeti taşıyor.`,
          utilityUse:
            "Asset bağlama içerik, kategori ve hikâye sayfası editörlerinde yapılır. Bu rotayı manuel yükleme, metadata inceleme, oynatma/önizleme ve önbellek hata ayıklaması için kullanın.",
        }
      : {
          eyebrow: "Media",
          title: "Media Utility",
          description:
            "Advanced registry, debug, preview, and manual upload surface for global assets. Normal editorial binding now happens inside content, category, and story-page editors.",
          refresh: "Refresh",
          upload: "Upload asset",
          searchLabel: "Search asset library",
          searchPlaceholder: "Search by object path, asset id, or checksum",
          filterTypes: "Images / Audio / Archives",
          filterUtility: "Utility view for registry, preview, and debug",
          filterRecent: `Showing ${formatNumber(recentAssetsQuery.limit)} recent assets`,
          summaryDescription:
            "Use this route for advanced inspection. Normal content/category/story workflows now upload and bind assets in place.",
          countLoaded: `${formatNumber(assetCount)} recent asset${assetCount === 1 ? "" : "s"} loaded`,
          utilitySummary: "Utility Summary",
          utilitySummaryDescription:
            "Inspect provider, media type, preview readiness, and cached URL coverage before opening the asset detail sheet.",
          loadingAssets:
            "The recent asset utility view is hydrating from the backend.",
          loadedAssets: `${formatNumber(imageCount)} images, ${formatNumber(audioCount)} audio files, and ${formatNumber(archiveCount)} archives are available in the current recent window.`,
          noCached:
            "No cached download URLs are currently visible in the recent slice.",
          hasCached: `${formatNumber(cachedUrlCount)} recent assets already carry a cached download URL snapshot.`,
          utilityUse:
            "Asset binding belongs in content, category, and story-page editors. Keep this route for manual uploads, metadata inspection, playback/preview, and cache debugging.",
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
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterUtility}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterRecent}
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description={copy.summaryDescription}
                title={copy.countLoaded}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>{copy.utilitySummary}</CardTitle>
                <CardDescription>
                  {copy.utilitySummaryDescription}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {recentAssetsQuery.isLoading
                    ? copy.loadingAssets
                    : copy.loadedAssets}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {cachedUrlCount === 0 ? copy.noCached : copy.hasCached}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.utilityUse}
                </div>
              </CardContent>
            </Card>
          </>
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
