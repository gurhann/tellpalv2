import { ChevronDown, Search, Trash2, UploadCloud } from "lucide-react";
import { useState } from "react";
import type { FieldError as ReactHookFormFieldError } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type {
  AssetMediaType,
  UploadableAssetKind,
} from "@/features/assets/api/asset-admin";
import { AssetFieldPreview } from "@/features/assets/components/asset-field-preview";
import { AssetPickerDialog } from "@/features/assets/components/asset-picker-dialog";
import { AssetUploadDialog } from "@/features/assets/components/asset-upload-dialog";
import { useAssetDetail } from "@/features/assets/queries/use-asset-detail";
import { cn } from "@/lib/utils";

type AssetPickerFieldProps = {
  id: string;
  label: string;
  value: number | null;
  onChange: (value: number | null) => void;
  mediaType: AssetMediaType;
  error?: ReactHookFormFieldError;
  description?: string;
  disabled?: boolean;
  placeholder?: string;
  pickerTitle: string;
  pickerDescription: string;
};

export function AssetPickerField({
  id,
  label,
  value,
  onChange,
  mediaType,
  error,
  description,
  disabled = false,
  placeholder = "Optional",
  pickerTitle,
  pickerDescription,
}: AssetPickerFieldProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);
  const assetDetailQuery = useAssetDetail(value);
  const uploadKind =
    mediaType === "IMAGE"
      ? "ORIGINAL_IMAGE"
      : mediaType === "AUDIO"
        ? "ORIGINAL_AUDIO"
        : null;
  const selectedAsset = assetDetailQuery.asset;
  const selectedAssetHasWrongMediaType =
    selectedAsset !== null && selectedAsset.mediaType !== mediaType;
  const normalizedLabel = label.replaceAll("*", "").trim().toLowerCase();

  function handleManualValueChange(nextValue: string) {
    if (nextValue.trim().length === 0) {
      onChange(null);
      return;
    }

    const parsedValue = Number.parseInt(nextValue, 10);
    onChange(Number.isFinite(parsedValue) ? parsedValue : null);
  }

  function getUploadCopy(kind: UploadableAssetKind) {
    if (kind === "ORIGINAL_IMAGE") {
      return {
        title: `Upload ${normalizedLabel}`,
        description:
          "Upload a new image directly from this workspace. The uploaded asset will be selected automatically after finalize.",
      };
    }

    return {
      title: `Upload ${normalizedLabel}`,
      description:
        "Upload a new audio file directly from this workspace. The uploaded asset will be selected automatically after finalize.",
    };
  }

  return (
    <div className="space-y-3">
      <label className="text-sm font-medium text-foreground" htmlFor={id}>
        {label}
      </label>

      <div className="rounded-2xl border border-border/70 bg-card/95 p-4 shadow-sm">
        {value === null ? (
          <div className="rounded-2xl border border-dashed border-border/70 bg-muted/20 px-4 py-5">
            <p className="text-sm font-medium text-foreground">
              No asset selected
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Upload a new {mediaType.toLowerCase()} asset here or browse an
              existing one without leaving this editor.
            </p>
          </div>
        ) : assetDetailQuery.isLoading ? (
          <div className="rounded-2xl border border-dashed border-border/70 bg-muted/20 px-4 py-5 text-sm text-muted-foreground">
            Loading selected asset details...
          </div>
        ) : selectedAsset ? (
          <div className="space-y-3">
            <AssetFieldPreview asset={selectedAsset} />
            {selectedAssetHasWrongMediaType ? (
              <p className="text-sm text-destructive">
                Asset #{selectedAsset.id} is {selectedAsset.mediaTypeLabel}, but
                this field expects {mediaType.toLowerCase()} media.
              </p>
            ) : null}
          </div>
        ) : assetDetailQuery.problem ? (
          <div className="rounded-2xl border border-destructive/40 bg-destructive/5 px-4 py-5">
            <p className="text-sm font-medium text-destructive">
              Selected asset could not be loaded
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Keep the manual asset id only if you know it is valid, or replace
              it from this workspace.
            </p>
          </div>
        ) : null}

        <div className="mt-4 flex flex-wrap gap-2">
          {uploadKind ? (
            <Button
              type="button"
              variant="default"
              onClick={() => setIsUploadOpen(true)}
              disabled={disabled}
            >
              <UploadCloud className="size-4" />
              Upload new
            </Button>
          ) : null}
          <Button
            type="button"
            variant="outline"
            onClick={() => setIsDialogOpen(true)}
            disabled={disabled}
          >
            <Search className="size-4" />
            Browse existing
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => onChange(null)}
            disabled={disabled || value === null}
          >
            <Trash2 className="size-4" />
            Clear
          </Button>
          <Button
            type="button"
            variant="ghost"
            className="text-muted-foreground"
            onClick={() => setIsAdvancedOpen((current) => !current)}
            disabled={disabled}
          >
            <ChevronDown
              className={cn(
                "size-4 transition-transform",
                isAdvancedOpen ? "rotate-180" : "rotate-0",
              )}
            />
            Advanced
          </Button>
        </div>

        {description ? (
          <p className="mt-4 text-sm text-muted-foreground">{description}</p>
        ) : null}
        <p className="mt-2 text-xs text-muted-foreground">
          Normal editorial flows can upload and bind assets here. Media Utility
          stays available for registry, debug, and manual inspection work.
        </p>
      </div>

      {isAdvancedOpen ? (
        <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground" htmlFor={id}>
              Manual asset id
            </label>
            <Input
              id={id}
              inputMode="numeric"
              placeholder={placeholder}
              type="number"
              value={value ?? ""}
              onChange={(event) => handleManualValueChange(event.target.value)}
              disabled={disabled}
            />
            <div className="flex flex-col gap-2 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
              <p>
                Use this only when you already know the asset id. Upload and
                browse actions above remain the default workflow.
              </p>
              <a
                className="font-medium text-foreground underline underline-offset-4"
                href="/media"
              >
                Open Media Utility
              </a>
            </div>
          </div>
        </div>
      ) : null}

      <FieldError error={error} />

      {isDialogOpen ? (
        <AssetPickerDialog
          description={pickerDescription}
          mediaType={mediaType}
          open={isDialogOpen}
          selectedAssetId={value}
          title={pickerTitle}
          onOpenChange={setIsDialogOpen}
          onSelectAsset={(asset) => onChange(asset.id)}
        />
      ) : null}

      {isUploadOpen && uploadKind ? (
        <AssetUploadDialog
          description={getUploadCopy(uploadKind).description}
          fixedKind={uploadKind}
          open={isUploadOpen}
          title={getUploadCopy(uploadKind).title}
          onOpenChange={setIsUploadOpen}
          onUploaded={(asset) => onChange(asset.id)}
        />
      ) : null}
    </div>
  );
}
