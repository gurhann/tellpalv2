import {
  AlertTriangle,
  Image as ImageIcon,
  LoaderCircle,
  Music4,
} from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { useAssetPreview } from "@/features/assets/queries/use-asset-preview";
import { cn } from "@/lib/utils";

type AssetFieldPreviewProps = {
  asset: AssetViewModel;
  className?: string;
};

export function AssetFieldPreview({
  asset,
  className,
}: AssetFieldPreviewProps) {
  const preview = useAssetPreview(asset, true);
  const [failedPreviewUrl, setFailedPreviewUrl] = useState<string | null>(null);
  const hasMediaLoadError =
    preview.previewUrl !== null && failedPreviewUrl === preview.previewUrl;

  async function handleRetryPreview() {
    setFailedPreviewUrl(null);
    await preview.refreshPreview({ force: true });
  }

  return (
    <div
      className={cn(
        "rounded-2xl border border-border/70 bg-muted/20 p-4",
        className,
      )}
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0 space-y-1">
          <p className="break-all text-sm font-medium text-foreground">
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
        <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-background px-3 py-1 text-xs text-muted-foreground">
          {asset.previewKind === "image" ? (
            <ImageIcon className="size-3.5" />
          ) : asset.previewKind === "audio" ? (
            <Music4 className="size-3.5" />
          ) : (
            <AlertTriangle className="size-3.5" />
          )}
          {asset.previewKind === "image"
            ? "Image preview"
            : asset.previewKind === "audio"
              ? "Audio preview"
              : "Preview unavailable"}
        </span>
      </div>

      {!asset.isPreviewable ? (
        <p className="mt-3 text-sm text-muted-foreground">
          Preview is unavailable for archive assets in context fields.
        </p>
      ) : preview.previewStatus === "loading" ? (
        <div className="mt-4 flex items-center gap-2 rounded-2xl border border-dashed border-border/70 bg-background/90 px-4 py-5 text-sm text-muted-foreground">
          <LoaderCircle className="size-4 animate-spin" />
          Preparing a fresh signed preview URL...
        </div>
      ) : preview.previewStatus === "error" || hasMediaLoadError ? (
        <div className="mt-4 rounded-2xl border border-destructive/40 bg-destructive/5 p-4">
          <p className="text-sm font-medium text-destructive">
            Preview could not be loaded
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {hasMediaLoadError
              ? "The browser could not render the current signed URL."
              : (preview.previewErrorMessage ??
                "The preview request failed before the browser could load the asset.")}
          </p>
          <Button
            type="button"
            variant="outline"
            className="mt-3"
            onClick={() => void handleRetryPreview()}
            disabled={preview.isRefreshing}
          >
            Retry preview
          </Button>
        </div>
      ) : asset.previewKind === "image" && preview.previewUrl ? (
        <div className="mt-4 overflow-hidden rounded-2xl border border-border/70 bg-background/90 p-3">
          <div className="flex min-h-40 items-center justify-center rounded-xl border border-dashed border-border/70 bg-muted/15 p-3">
            <img
              alt={`Preview of asset #${asset.id}`}
              className="max-h-56 w-full rounded-lg object-contain"
              loading="lazy"
              onError={() => setFailedPreviewUrl(preview.previewUrl)}
              src={preview.previewUrl}
            />
          </div>
        </div>
      ) : asset.previewKind === "audio" && preview.previewUrl ? (
        <div className="mt-4 rounded-2xl border border-border/70 bg-background/90 p-3">
          <audio
            aria-label={`Audio preview for asset #${asset.id}`}
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
        <p className="mt-3 text-sm text-muted-foreground">
          No preview URL is ready yet for this asset.
        </p>
      )}
    </div>
  );
}
