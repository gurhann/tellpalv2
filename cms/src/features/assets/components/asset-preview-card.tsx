import {
  AlertTriangle,
  Image as ImageIcon,
  Music4,
  RefreshCw,
} from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import { Button } from "@/components/ui/button";
import { useAssetPreview } from "@/features/assets/queries/use-asset-preview";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";

type AssetPreviewCardProps = {
  asset: AssetViewModel;
  open: boolean;
};

function formatTimestamp(value: string | null) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("en-GB", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function AssetPreviewCard({ asset, open }: AssetPreviewCardProps) {
  const [failedPreviewUrl, setFailedPreviewUrl] = useState<string | null>(null);
  const preview = useAssetPreview(asset, open);
  const hasMediaLoadError =
    preview.previewUrl !== null && failedPreviewUrl === preview.previewUrl;

  async function handleRetryPreview() {
    setFailedPreviewUrl(null);
    await preview.refreshPreview({ force: true });
  }

  return (
    <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
      <div className="mb-4 space-y-1">
        <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          Preview
        </h2>
        <p className="text-sm leading-6 text-muted-foreground">
          Inspect image assets inline or play audio assets directly from the
          signed Firebase download URL snapshot.
        </p>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1">
          {asset.previewKind === "image" ? (
            <ImageIcon className="size-4" />
          ) : asset.previewKind === "audio" ? (
            <Music4 className="size-4" />
          ) : (
            <AlertTriangle className="size-4" />
          )}
          {asset.previewKind === "image"
            ? "Image preview"
            : asset.previewKind === "audio"
              ? "Audio preview"
              : "Preview unavailable"}
        </span>
        <span>
          Last refreshed: {formatTimestamp(asset.downloadUrlCachedAt)}
        </span>
        <span>Expires: {formatTimestamp(asset.downloadUrlExpiresAt)}</span>
      </div>

      {!asset.isPreviewable ? (
        <EmptyState
          className="min-h-56 rounded-2xl border border-dashed border-border/70 bg-muted/20"
          description="Archive assets stay inspectable through metadata and cached download URL controls, but they do not render inline previews."
          title="Preview unavailable for archive assets"
        />
      ) : preview.previewStatus === "loading" ? (
        <EmptyState
          className="min-h-56 rounded-2xl border border-dashed border-border/70 bg-muted/20"
          description="The CMS is preparing a fresh signed URL before rendering the asset preview."
          title="Loading preview"
        />
      ) : preview.previewStatus === "error" || hasMediaLoadError ? (
        <div className="rounded-2xl border border-destructive/40 bg-destructive/5 p-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 size-4 text-destructive" />
            <div className="min-w-0 flex-1 space-y-2">
              <div className="font-medium text-destructive">
                Preview could not be loaded
              </div>
              <p className="text-sm leading-6 text-muted-foreground">
                {hasMediaLoadError
                  ? "The browser could not render the signed asset URL. Refresh the preview to request one more signed URL snapshot."
                  : (preview.previewErrorMessage ??
                    "The preview request failed before the browser could load the asset.")}
              </p>
              <Button
                type="button"
                variant="outline"
                onClick={() => void handleRetryPreview()}
                disabled={preview.isRefreshing}
              >
                <RefreshCw
                  className={`size-4 ${preview.isRefreshing ? "animate-spin" : ""}`}
                />
                Retry preview
              </Button>
            </div>
          </div>
        </div>
      ) : asset.previewKind === "image" && preview.previewUrl ? (
        <div className="overflow-hidden rounded-2xl border border-border/70 bg-muted/20 p-4">
          <div className="flex min-h-72 items-center justify-center rounded-2xl border border-dashed border-border/70 bg-background/90 p-4">
            <img
              alt={`Preview of asset #${asset.id}`}
              className="max-h-[26rem] w-full rounded-xl object-contain"
              loading="lazy"
              onError={() => setFailedPreviewUrl(preview.previewUrl)}
              src={preview.previewUrl}
            />
          </div>
        </div>
      ) : asset.previewKind === "audio" && preview.previewUrl ? (
        <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
          <div className="rounded-2xl border border-dashed border-border/70 bg-background/90 p-5">
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
        </div>
      ) : (
        <EmptyState
          className="min-h-56 rounded-2xl border border-dashed border-border/70 bg-muted/20"
          description="No preview URL is currently ready for this asset."
          title="Preview unavailable"
        />
      )}
    </div>
  );
}
