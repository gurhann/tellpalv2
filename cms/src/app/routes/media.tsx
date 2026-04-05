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

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
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

  function handleOpenChange(nextOpen: boolean) {
    setIsDetailOpen(nextOpen);

    if (!nextOpen) {
      setSelectedAssetId(null);
    }
  }

  return (
    <>
      <ContentPageShell
        eyebrow="Media"
        title="Media Utility"
        description="Advanced registry, debug, preview, and manual upload surface for global assets. Normal editorial binding now happens inside content, category, and story-page editors."
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
              Refresh
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsUploadOpen(true)}
            >
              Upload asset
            </Button>
          </>
        }
        toolbar={
          <FilterBar aria-label="Asset library filters">
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label="Search asset library"
                  className="pl-8"
                  disabled
                  placeholder="Search by object path, asset id, or checksum"
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Images / Audio / Archives
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Utility view for registry, preview, and debug
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Showing {recentAssetsQuery.limit} recent assets
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description="Use this route for advanced inspection. Normal content/category/story workflows now upload and bind assets in place."
                title={`${assetCount} recent asset${
                  assetCount === 1 ? "" : "s"
                } loaded`}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Utility Summary</CardTitle>
                <CardDescription>
                  Inspect provider, media type, preview readiness, and cached
                  URL coverage before opening the asset detail sheet.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {recentAssetsQuery.isLoading
                    ? "The recent asset utility view is hydrating from the backend."
                    : `${imageCount} images, ${audioCount} audio files, and ${archiveCount} archives are available in the current recent window.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {cachedUrlCount === 0
                    ? "No cached download URLs are currently visible in the recent slice."
                    : `${cachedUrlCount} recent assets already carry a cached download URL snapshot.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Asset binding belongs in content, category, and story-page
                  editors. Keep this route for manual uploads, metadata
                  inspection, playback/preview, and cache debugging.
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
