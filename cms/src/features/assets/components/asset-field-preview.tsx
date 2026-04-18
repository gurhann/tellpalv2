import {
  AlertTriangle,
  Image as ImageIcon,
  LoaderCircle,
  Music4,
} from "lucide-react";
import type { ReactNode } from "react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { useAssetPreview } from "@/features/assets/queries/use-asset-preview";
import { useI18n } from "@/i18n/locale-provider";
import { cn } from "@/lib/utils";

type AssetFieldPreviewProps = {
  asset: AssetViewModel;
  className?: string;
  actions?: ReactNode;
  variant?: "default" | "editor";
};

export function AssetFieldPreview({
  asset,
  className,
  actions,
  variant = "default",
}: AssetFieldPreviewProps) {
  const { t } = useI18n();
  const preview = useAssetPreview(asset, true);
  const [failedPreviewUrl, setFailedPreviewUrl] = useState<string | null>(null);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const hasMediaLoadError =
    preview.previewUrl !== null && failedPreviewUrl === preview.previewUrl;
  const isEditor = variant === "editor";
  const isEditorImage = isEditor && asset.previewKind === "image";

  async function handleRetryPreview() {
    setFailedPreviewUrl(null);
    await preview.refreshPreview({ force: true });
  }

  function renderPreviewStatus() {
    if (!asset.isPreviewable) {
      return (
        <p className="text-sm text-muted-foreground">
          {t("assets.previewUnavailableArchiveDescription")}
        </p>
      );
    }

    if (preview.previewStatus === "loading") {
      return (
        <div className="flex items-center gap-2 rounded-2xl border border-dashed border-border/70 bg-background/90 px-4 py-5 text-sm text-muted-foreground">
          <LoaderCircle className="size-4 animate-spin" />
          {t("assets.previewLoadingDescription")}
        </div>
      );
    }

    if (preview.previewStatus === "error" || hasMediaLoadError) {
      return (
        <div className="rounded-2xl border border-destructive/40 bg-destructive/5 p-4">
          <p className="text-sm font-medium text-destructive">
            {t("assets.previewLoadFailedTitle")}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {hasMediaLoadError
              ? t("assets.previewLoadBrowserDescription")
              : (preview.previewErrorMessage ??
                t("assets.previewLoadFailedDescription"))}
          </p>
          <Button
            type="button"
            variant="outline"
            className="mt-3"
            onClick={() => void handleRetryPreview()}
            disabled={preview.isRefreshing}
          >
            {t("assets.retryPreview")}
          </Button>
        </div>
      );
    }

    if (asset.previewKind === "image" && preview.previewUrl) {
      return (
        <button
          type="button"
          className="group relative block"
          data-testid="asset-field-preview-image-button"
          onClick={() => setIsLightboxOpen(true)}
        >
          <img
            alt={t("assets.imageAlt", { assetId: asset.id })}
            className={cn(
              "h-auto rounded-[1.25rem] object-contain shadow-sm transition-transform duration-150 group-hover:scale-[1.01]",
              isEditorImage
                ? "max-h-[18rem] w-auto max-w-full bg-white"
                : isEditor
                  ? "w-full max-h-40"
                  : "w-full max-h-56",
            )}
            loading="lazy"
            onError={() => setFailedPreviewUrl(preview.previewUrl)}
            src={preview.previewUrl}
          />
          <span className="pointer-events-none absolute inset-x-3 bottom-3 rounded-full bg-black/70 px-3 py-1 text-xs font-medium text-white opacity-0 transition-opacity duration-150 group-hover:opacity-100">
            Buyutmek icin tikla
          </span>
        </button>
      );
    }

    if (asset.previewKind === "audio" && preview.previewUrl) {
      return (
        <div className="rounded-2xl border border-border/70 bg-background/90 p-3">
          <audio
            aria-label={t("assets.audioAria", { assetId: asset.id })}
            className="w-full"
            controls
            onError={() => setFailedPreviewUrl(preview.previewUrl)}
            preload="metadata"
          >
            <source
              src={preview.previewUrl}
              type={asset.mimeType ?? undefined}
            />
          </audio>
        </div>
      );
    }

    return (
      <p className="text-sm text-muted-foreground">
        {t("assets.previewUnavailableDescription")}
      </p>
    );
  }

  if (isEditorImage) {
    return (
      <>
        <div
          className={cn(
            "rounded-2xl border border-border/70 bg-background p-4",
            className,
          )}
          data-testid="asset-field-preview-editor-cover"
        >
          <div className="grid gap-4 md:grid-cols-[minmax(13rem,15rem)_1fr] md:items-start">
            <div
              className="mx-auto w-full max-w-[15rem]"
              data-testid="asset-field-preview-editor-cover-stage"
            >
              <div className="flex min-h-[19rem] items-center justify-center rounded-[1.5rem] bg-background px-4 py-5">
                {renderPreviewStatus()}
              </div>
            </div>

            <div className="min-w-0 space-y-3">
              <div className="space-y-1">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                  Mobile cover preview
                </p>
                <p className="break-all text-sm font-medium text-foreground">
                  {asset.objectPath}
                </p>
              </div>

              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                  Asset #{asset.id}
                </span>
                <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                  {asset.kindLabel}
                </span>
                <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                  {asset.providerLabel}
                </span>
                <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-emerald-700">
                  {t("assets.previewKind.image")}
                </span>
              </div>

              {actions ? (
                <div
                  className="flex flex-wrap gap-2"
                  data-testid="asset-field-preview-editor-cover-actions"
                >
                  {actions}
                </div>
              ) : (
                <p className="text-sm leading-6 text-muted-foreground">
                  Cover assets are shown in a portrait-focused frame so square
                  and phone-ready images read closer to their in-app
                  composition.
                </p>
              )}
            </div>
          </div>
        </div>
        {preview.previewUrl ? (
          <Dialog open={isLightboxOpen} onOpenChange={setIsLightboxOpen}>
            <DialogContent className="max-w-[min(92vw,64rem)] bg-background p-0 sm:max-w-[min(92vw,64rem)]">
              <DialogHeader>
                <DialogTitle>Kapak onizlemesi</DialogTitle>
                <DialogDescription>
                  Asset #{asset.id} icin buyutulmus mobil kapak gorunumu.
                </DialogDescription>
              </DialogHeader>
              <DialogBody className="px-5 pb-5">
                <div className="flex max-h-[75vh] items-center justify-center overflow-auto rounded-[1.5rem] bg-muted/10 p-4">
                  <img
                    alt={t("assets.imageAlt", { assetId: asset.id })}
                    className="h-auto max-h-[68vh] w-auto max-w-full rounded-[1.25rem] object-contain shadow-sm"
                    src={preview.previewUrl}
                  />
                </div>
              </DialogBody>
            </DialogContent>
          </Dialog>
        ) : null}
      </>
    );
  }

  return (
    <>
      <div
        className={cn(
          "rounded-2xl border border-border/70 bg-muted/20",
          isEditor ? "p-3" : "p-4",
          className,
        )}
      >
        <div
          className={cn(
            "flex flex-col sm:flex-row sm:items-start sm:justify-between",
            isEditor ? "gap-2.5" : "gap-3",
          )}
        >
          <div className="min-w-0 space-y-1">
            <p
              className={cn(
                "break-all font-medium text-foreground",
                isEditor ? "text-xs leading-5" : "text-sm",
              )}
            >
              {asset.objectPath}
            </p>
            <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
              <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                Asset #{asset.id}
              </span>
              <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                {asset.kindLabel}
              </span>
              <span className="rounded-full border border-border/70 bg-background px-2.5 py-1">
                {asset.providerLabel}
              </span>
            </div>
          </div>
          <span
            className={cn(
              "inline-flex items-center gap-2 rounded-full border border-border/70 bg-background text-xs text-muted-foreground",
              isEditor ? "px-2.5 py-1" : "px-3 py-1",
            )}
          >
            {asset.previewKind === "image" ? (
              <ImageIcon className="size-3.5" />
            ) : asset.previewKind === "audio" ? (
              <Music4 className="size-3.5" />
            ) : (
              <AlertTriangle className="size-3.5" />
            )}
            {asset.previewKind === "image"
              ? t("assets.previewKind.image")
              : asset.previewKind === "audio"
                ? t("assets.previewKind.audio")
                : t("assets.previewKind.unavailable")}
          </span>
        </div>

        {asset.previewKind === "image" && preview.previewUrl ? (
          <div
            className={cn(
              "mt-4 overflow-hidden rounded-2xl bg-background/90",
              isEditor ? "p-2" : "border border-border/70 p-3",
            )}
          >
            <div
              className={cn(
                "flex items-center justify-center rounded-xl border border-dashed border-border/70 bg-muted/15",
                isEditor ? "min-h-28 p-2.5" : "min-h-40 p-3",
              )}
            >
              {renderPreviewStatus()}
            </div>
          </div>
        ) : asset.previewKind === "audio" && preview.previewUrl ? (
          <div className="mt-4 rounded-2xl border border-border/70 bg-background/90 p-3">
            <audio
              aria-label={t("assets.audioAria", { assetId: asset.id })}
              className="w-full"
              controls
              onError={() => setFailedPreviewUrl(preview.previewUrl)}
              preload="metadata"
            >
              <source
                src={preview.previewUrl}
                type={asset.mimeType ?? undefined}
              />
            </audio>
          </div>
        ) : (
          <div className="mt-4">{renderPreviewStatus()}</div>
        )}
      </div>

      {asset.previewKind === "image" && preview.previewUrl ? (
        <Dialog open={isLightboxOpen} onOpenChange={setIsLightboxOpen}>
          <DialogContent className="max-w-[min(92vw,64rem)] bg-background p-0 sm:max-w-[min(92vw,64rem)]">
            <DialogHeader>
              <DialogTitle>Gorsel onizleme</DialogTitle>
              <DialogDescription>
                Asset #{asset.id} icin buyutulmus gorsel gorunumu.
              </DialogDescription>
            </DialogHeader>
            <DialogBody className="px-5 pb-5">
              <div className="flex max-h-[75vh] items-center justify-center overflow-auto rounded-[1.5rem] bg-muted/10 p-4">
                <img
                  alt={t("assets.imageAlt", { assetId: asset.id })}
                  className="h-auto max-h-[68vh] w-auto max-w-full rounded-[1.25rem] object-contain shadow-sm"
                  src={preview.previewUrl}
                />
              </div>
            </DialogBody>
          </DialogContent>
        </Dialog>
      ) : null}
    </>
  );
}
