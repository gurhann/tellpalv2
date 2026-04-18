import { RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
} from "@/components/workspace/workspace-primitives";
import { AssetDetailSheet } from "@/features/assets/components/asset-detail-sheet";
import { AssetTable } from "@/features/assets/components/asset-table";
import { AssetUploadDialog } from "@/features/assets/components/asset-upload-dialog";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useI18n } from "@/i18n/locale-provider";

const RECENT_ASSET_LIMIT = 24;

export function MediaRoute() {
  const { locale } = useI18n();
  const [selectedAssetId, setSelectedAssetId] = useState<number | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [selectedMediaType, setSelectedMediaType] =
    useState<"ALL" | "IMAGE" | "AUDIO" | "ARCHIVE">("ALL");
  const recentAssetsQuery = useRecentAssets(RECENT_ASSET_LIMIT);
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Medya",
          title: "Medya Utility",
          description:
            "Asset yukleyin, filtreleyin ve detay metadata'sini scroll-safe drawer icinde inceleyin.",
          refresh: "Yenile",
          upload: "Asset yukle",
          searchLabel: "Asset kutuphanesinde ara",
          searchPlaceholder: "Object path, asset id veya checksum ile ara",
          filterTypes: "Tum medya tipleri",
          railTitle: "Library posture",
          railDescription:
            "Upload, preview ve metadata duzenleme ayni utility yuzeyinde toplanir.",
          imageAssets: "Gorsel",
          audioAssets: "Ses",
          archiveAssets: "Arsiv",
          notesTitle: "Operator notlari",
          notesDescription:
            "Normal editorial akislar asset baglamayi alan icinden yapar; bu utility ise registry, debug ve metadata duzeltme icin kalir.",
          resultLabel: "Filtre sonucu",
          nextStepLabel: "Secim sonrasi",
          nextStepValue: "Detail drawer",
        }
      : {
          eyebrow: "Media",
          title: "Media Utility",
          description:
            "Upload assets, filter them, and inspect metadata in a scroll-safe detail drawer.",
          refresh: "Refresh",
          upload: "Upload asset",
          searchLabel: "Search asset library",
          searchPlaceholder: "Search by object path, asset id, or checksum",
          filterTypes: "All media types",
          railTitle: "Library posture",
          railDescription:
            "Upload, preview, and metadata correction stay on one utility surface.",
          imageAssets: "Images",
          audioAssets: "Audio",
          archiveAssets: "Archives",
          notesTitle: "Operator notes",
          notesDescription:
            "Editorial flows can bind assets from field-level pickers; this utility remains for registry, debug, and metadata repair work.",
          resultLabel: "Filtered result",
          nextStepLabel: "After selection",
          nextStepValue: "Detail drawer",
        };

  const filteredAssets = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return recentAssetsQuery.assets.filter((asset) => {
      if (
        selectedMediaType !== "ALL" &&
        asset.mediaType !== selectedMediaType
      ) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        asset.objectPath.toLowerCase().includes(normalizedSearch) ||
        asset.id.toString().includes(normalizedSearch) ||
        (asset.checksumSha256 ?? "").toLowerCase().includes(normalizedSearch)
      );
    });
  }, [deferredSearch, recentAssetsQuery.assets, selectedMediaType]);

  const imageCount = useMemo(
    () => filteredAssets.filter((asset) => asset.mediaType === "IMAGE").length,
    [filteredAssets],
  );
  const audioCount = useMemo(
    () => filteredAssets.filter((asset) => asset.mediaType === "AUDIO").length,
    [filteredAssets],
  );
  const archiveCount = useMemo(
    () => filteredAssets.filter((asset) => asset.mediaType === "ARCHIVE").length,
    [filteredAssets],
  );
  const filterSummaryTitle =
    locale === "tr"
      ? `${filteredAssets.length} / ${recentAssetsQuery.assets.length} asset`
      : `${filteredAssets.length} / ${recentAssetsQuery.assets.length} assets`;
  const filterSummaryDescription =
    locale === "tr"
      ? "Arama ve medya tipi filtreleri asset tablosunu aninda gunceller."
      : "Search and media-type filters update the asset table immediately.";

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
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: copy.imageAssets,
                value: `${imageCount}`,
                tone: imageCount > 0 ? "success" : "default",
              },
              {
                label: copy.audioAssets,
                value: `${audioCount}`,
                tone: audioCount > 0 ? "success" : "default",
              },
              {
                label: copy.archiveAssets,
                value: `${archiveCount}`,
              },
            ]}
          >
            <WorkspaceInfoCard
              title={copy.notesTitle}
              description={copy.notesDescription}
              className="bg-background/80"
            >
              <WorkspaceKeyValueGrid
                items={[
                  {
                    label: copy.resultLabel,
                    value:
                      locale === "tr"
                        ? `${filteredAssets.length} asset`
                        : `${filteredAssets.length} assets`,
                  },
                  {
                    label: copy.nextStepLabel,
                    value: copy.nextStepValue,
                    tone: "accent",
                  },
                ]}
              />
            </WorkspaceInfoCard>
          </TaskRail>
        }
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
                  placeholder={copy.searchPlaceholder}
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
            </FilterBarGroup>
            <FilterBarActions>
              {[
                {
                  key: "ALL" as const,
                  label: copy.filterTypes,
                },
                {
                  key: "IMAGE" as const,
                  label: locale === "tr" ? "Gorsel" : "Images",
                },
                {
                  key: "AUDIO" as const,
                  label: locale === "tr" ? "Ses" : "Audio",
                },
                {
                  key: "ARCHIVE" as const,
                  label: locale === "tr" ? "Arsiv" : "Archives",
                },
              ].map((mediaOption) => (
                <Button
                  key={mediaOption.key}
                  type="button"
                  variant={
                    selectedMediaType === mediaOption.key
                      ? "secondary"
                      : "outline"
                  }
                  size="sm"
                  onClick={() => setSelectedMediaType(mediaOption.key)}
                >
                  {mediaOption.label}
                </Button>
              ))}
            </FilterBarActions>
            <FilterBarSummary
              title={filterSummaryTitle}
              description={filterSummaryDescription}
            />
          </FilterBar>
        }
      >
        <AssetTable
          assets={filteredAssets}
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
