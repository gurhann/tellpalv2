import { RefreshCw, Search, Sparkles } from "lucide-react";

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
import { AssetTable } from "@/features/assets/components/asset-table";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
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

  return (
    <ContentPageShell
      eyebrow="Media"
      title="Asset Library"
      description="The asset registry now reads directly from the admin API. Recent media assets can be scanned here before the next task adds detail editing, signed URL refresh, and shared picker flows."
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
            Asset detail sheet
          </Button>
          <Button type="button" variant="outline" disabled>
            Shared asset picker
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
              description="Live data is bound. Search, filter, detail sheet, and shared picker workflows remain intentionally staged for the next asset tasks."
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
              <CardTitle>Workspace Notes</CardTitle>
              <CardDescription>
                The library shell is backed by `GET /api/admin/media?limit=...`
                and is optimized for scanning provider, kind, and MIME coverage
                before deeper asset operations land.
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
                Content, category, and story page forms will later reuse the
                shared picker primitives introduced in this module.
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Next Asset Work</CardTitle>
              <CardDescription>
                This task intentionally stops at live recent-read coverage so
                the upcoming asset tasks can add mutation UX without a route
                rewrite.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M08-T02` adds the asset detail sheet and metadata form, driven
                by `GET /api/admin/media/:assetId`.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M08-T03` layers signed URL refresh and a reusable asset picker
                dialog on top of this registry.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Sparkles className="size-3.5" />
                Recent list query is live; detail and picker surfaces are staged
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <AssetTable
        assets={recentAssetsQuery.assets}
        isLoading={recentAssetsQuery.isLoading}
        onRetry={() => void recentAssetsQuery.refetch()}
        problem={recentAssetsQuery.problem}
      />

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Upcoming Asset Surfaces</CardTitle>
          <CardDescription>
            The live registry now anchors the shells that the next asset tasks
            will activate.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">Detail sheet</p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Row-driven detail inspection and metadata editing land in the next
              task without changing the surrounding route shell.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Signed URL refresh
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Cached download URL refresh controls will sit beside individual
              asset records after the detail sheet is live.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Shared picker entry
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Content, category, and story-page forms will open this module as a
              reusable picker instead of inventing asset lookups locally.
            </p>
          </div>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
