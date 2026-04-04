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
import { AssetDetailSheet } from "@/features/assets/components/asset-detail-sheet";
import { AssetTable } from "@/features/assets/components/asset-table";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
  const [selectedAssetId, setSelectedAssetId] = useState<number | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
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
        title="Asset Library"
        description="The asset registry now reads directly from the admin API. Recent media assets can be scanned here, and row selection now opens a live metadata detail sheet."
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
            <Button type="button" disabled>
              Select a row to inspect detail
            </Button>
            <Button type="button" variant="outline" disabled>
              Shared picker lives in forms
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
                Firebase Storage and Local Stub
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Showing {recentAssetsQuery.limit} recent assets
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description="The registry reflects recent backend assets and opens each selected row in the detail sheet."
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
                <CardTitle>Library Summary</CardTitle>
                <CardDescription>
                  Scan provider, media type, and cached URL coverage before
                  opening the asset detail sheet.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {recentAssetsQuery.isLoading
                    ? "The recent asset registry is hydrating from the backend."
                    : `${imageCount} images, ${audioCount} audio files, and ${archiveCount} archives are available in the current recent window.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {cachedUrlCount === 0
                    ? "No cached download URLs are currently visible in the recent slice."
                    : `${cachedUrlCount} recent assets already carry a cached download URL snapshot.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Selecting a row now opens the live asset detail sheet.
                  Content, category, and story-page forms now reuse this module
                  through the shared asset picker field.
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
    </>
  );
}
