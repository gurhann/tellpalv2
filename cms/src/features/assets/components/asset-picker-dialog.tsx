import { useDeferredValue, useMemo, useState } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import type { AssetMediaType } from "@/features/assets/api/asset-admin";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { useRecentAssets } from "@/features/assets/queries/use-recent-assets";
import { useI18n } from "@/i18n/locale-provider";

type AssetPickerDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mediaType: AssetMediaType;
  selectedAssetId?: number | null;
  onSelectAsset: (asset: AssetViewModel) => void;
  title: string;
  description: string;
  limit?: number;
};

function getEmptyCopy(mediaType: AssetMediaType, locale: "en" | "tr") {
  switch (mediaType) {
    case "IMAGE":
      return {
        title:
          locale === "tr" ? "Son görsel asset yok" : "No recent image assets",
        description:
          locale === "tr"
            ? "Bu çalışma alanından yeni bir görsel yükleyin veya gelişmiş inceleme için Medya Aracı'nı açın."
            : "Upload a new image from this workspace or open Media Utility for advanced inspection.",
      };
    case "AUDIO":
      return {
        title: locale === "tr" ? "Son ses asset yok" : "No recent audio assets",
        description:
          locale === "tr"
            ? "Bu çalışma alanından yeni bir ses dosyası yükleyin veya gelişmiş inceleme için Medya Aracı'nı açın."
            : "Upload a new audio file from this workspace or open Media Utility for advanced inspection.",
      };
    case "ARCHIVE":
      return {
        title:
          locale === "tr" ? "Son arşiv asset yok" : "No recent archive assets",
        description:
          locale === "tr"
            ? "Gelişmiş inceleme gerektiğinde arşiv asset'leri Medya Aracı üzerinden yönetilir."
            : "Archive assets are managed from Media Utility when advanced inspection is needed.",
      };
  }
}

export function AssetPickerDialog({
  open,
  onOpenChange,
  mediaType,
  selectedAssetId = null,
  onSelectAsset,
  title,
  description,
  limit = 24,
}: AssetPickerDialogProps) {
  const { locale } = useI18n();
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const recentAssetsQuery = useRecentAssets({ limit, enabled: open });
  const filteredAssets = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return recentAssetsQuery.assets.filter((asset) => {
      if (asset.mediaType !== mediaType) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        asset.objectPath.toLowerCase().includes(normalizedSearch) ||
        asset.kindLabel.toLowerCase().includes(normalizedSearch) ||
        asset.providerLabel.toLowerCase().includes(normalizedSearch) ||
        asset.id.toString().includes(normalizedSearch)
      );
    });
  }, [deferredSearch, mediaType, recentAssetsQuery.assets]);
  const emptyCopy = getEmptyCopy(mediaType, locale);

  function handleSelect(asset: AssetViewModel) {
    onSelectAsset(asset);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <DialogBody className="grid gap-4">
          <Input
            aria-label={locale === "tr" ? "Asset ara" : "Search assets"}
            placeholder={
              locale === "tr"
                ? "Asset id, object path, provider veya türe göre filtrele"
                : "Filter by asset id, object path, provider, or kind"
            }
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />

          {recentAssetsQuery.problem ? (
            <ProblemAlert problem={recentAssetsQuery.problem} />
          ) : recentAssetsQuery.isLoading ? (
            <EmptyState
              className="min-h-56"
              description={
                locale === "tr"
                  ? "Picker, son asset kayıtlarını yönetici API'sinden istiyor."
                  : "The picker is requesting recent assets from the admin API."
              }
              title={
                locale === "tr"
                  ? "Son asset'ler yükleniyor"
                  : "Loading recent assets"
              }
            />
          ) : filteredAssets.length === 0 ? (
            <EmptyState
              className="min-h-56"
              description={
                deferredSearch.trim().length > 0
                  ? locale === "tr"
                    ? "Mevcut aramayla eşleşen son asset yok."
                    : "No recent assets match the current search."
                  : emptyCopy.description
              }
              title={
                deferredSearch.trim().length > 0
                  ? locale === "tr"
                    ? "Eşleşen asset yok"
                    : "No matching assets"
                  : emptyCopy.title
              }
            />
          ) : (
            <div className="grid max-h-[28rem] gap-3 overflow-y-auto pr-1">
              {filteredAssets.map((asset) => {
                const isSelected = asset.id === selectedAssetId;

                return (
                  <div
                    key={asset.id}
                    className={`rounded-2xl border px-4 py-4 shadow-sm transition-colors ${
                      isSelected
                        ? "border-primary/30 bg-primary/5"
                        : "border-border/70 bg-card/90"
                    }`}
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="min-w-0 space-y-2">
                        <p className="break-all text-sm font-medium text-foreground">
                          {asset.objectPath}
                        </p>
                        <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                          <span className="rounded-full border border-border/70 bg-muted/35 px-2.5 py-1">
                            {locale === "tr"
                              ? `Asset #${asset.id}`
                              : `Asset #${asset.id}`}
                          </span>
                          <span className="rounded-full border border-border/70 bg-muted/35 px-2.5 py-1">
                            {asset.kindLabel}
                          </span>
                          <span className="rounded-full border border-border/70 bg-muted/35 px-2.5 py-1">
                            {asset.providerLabel}
                          </span>
                          <span className="rounded-full border border-border/70 bg-muted/35 px-2.5 py-1">
                            {asset.mimeType ??
                              (locale === "tr"
                                ? "MIME bekleniyor"
                                : "MIME pending")}
                          </span>
                        </div>
                      </div>

                      <Button
                        type="button"
                        variant={isSelected ? "default" : "outline"}
                        onClick={() => handleSelect(asset)}
                      >
                        {isSelected
                          ? locale === "tr"
                            ? "Seçildi"
                            : "Selected"
                          : locale === "tr"
                            ? "Asset'i kullan"
                            : "Use asset"}
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
