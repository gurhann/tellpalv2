import {
  ChevronDown,
  LoaderCircle,
  Search,
  Trash2,
  UploadCloud,
} from "lucide-react";
import { useState, type DragEvent } from "react";
import type { FieldError as ReactHookFormFieldError } from "react-hook-form";

import { ProblemAlert } from "@/components/feedback/problem-alert";
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
import {
  createTypedBrowserFile,
  getUploadKindForMediaType,
  inferMimeType,
  uploadKindMatchesMimeType,
} from "@/features/assets/lib/asset-upload-file";
import { useUploadAsset } from "@/features/assets/mutations/use-upload-asset";
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
  advancedLabel?: string;
  manualInputLabel?: string;
  testId?: string;
  variant?: "default" | "editor";
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
  advancedLabel,
  manualInputLabel,
  testId,
  variant = "default",
}: AssetPickerFieldProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);
  const [isDropActive, setIsDropActive] = useState(false);
  const [directUploadProgress, setDirectUploadProgress] = useState(0);
  const [directUploadMessage, setDirectUploadMessage] = useState<string | null>(
    null,
  );
  const assetDetailQuery = useAssetDetail(value);
  const uploadKind = getUploadKindForMediaType(mediaType);
  const directUploadMutation = useUploadAsset({
    onSuccess: async (asset) => {
      onChange(asset.id);
      setDirectUploadMessage(null);
    },
  });
  const selectedAsset = assetDetailQuery.asset;
  const selectedAssetHasWrongMediaType =
    selectedAsset !== null && selectedAsset.mediaType !== mediaType;
  const normalizedLabel = label.replaceAll("*", "").trim().toLowerCase();
  const resolvedAdvancedLabel =
    advancedLabel ?? `Advanced ${normalizedLabel} options`;
  const resolvedManualInputLabel = manualInputLabel ?? "Manual asset id";
  const isEditor = variant === "editor";
  const shouldInlineEditorActions = isEditor && selectedAsset !== null;
  const useInlineSelectedEditorSurface = isEditor && selectedAsset !== null;
  const isDirectUploadPending = directUploadMutation.isPending;
  const isFieldDisabled = disabled || isDirectUploadPending;
  const expectedFileLabel =
    mediaType === "IMAGE" ? "image" : mediaType === "AUDIO" ? "audio" : "media";

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

  function hasDraggedFiles(dataTransfer: DataTransfer) {
    return Array.from(dataTransfer.types).includes("Files");
  }

  function getDropValidationMessage(files: File[]) {
    if (!uploadKind) {
      return "This field does not support direct uploads.";
    }

    if (files.length === 0) {
      return `Drop one ${expectedFileLabel} file to upload.`;
    }

    if (files.length > 1) {
      return `Drop one ${expectedFileLabel} file at a time.`;
    }

    const file = files[0]!;
    const mimeType = inferMimeType(file);
    if (!mimeType) {
      return "The browser could not infer a supported MIME type from the dropped file.";
    }

    if (!uploadKindMatchesMimeType(uploadKind, mimeType)) {
      return `This field accepts ${expectedFileLabel} files only.`;
    }

    return null;
  }

  function handleDragEnter(event: DragEvent<HTMLDivElement>) {
    if (
      isFieldDisabled ||
      !uploadKind ||
      !hasDraggedFiles(event.dataTransfer)
    ) {
      return;
    }

    event.preventDefault();
    setIsDropActive(true);
  }

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    if (
      isFieldDisabled ||
      !uploadKind ||
      !hasDraggedFiles(event.dataTransfer)
    ) {
      return;
    }

    event.preventDefault();
    event.dataTransfer.dropEffect = "copy";
    setIsDropActive(true);
  }

  function handleDragLeave(event: DragEvent<HTMLDivElement>) {
    const nextTarget = event.relatedTarget;
    if (
      nextTarget instanceof Node &&
      event.currentTarget.contains(nextTarget)
    ) {
      return;
    }

    setIsDropActive(false);
  }

  async function handleDrop(event: DragEvent<HTMLDivElement>) {
    if (isFieldDisabled || !uploadKind) {
      return;
    }

    event.preventDefault();
    setIsDropActive(false);
    directUploadMutation.reset();

    const files = Array.from(event.dataTransfer.files);
    const validationMessage = getDropValidationMessage(files);
    if (validationMessage) {
      setDirectUploadMessage(validationMessage);
      return;
    }

    const file = files[0]!;
    const mimeType = inferMimeType(file)!;
    setDirectUploadMessage(null);
    setDirectUploadProgress(0);
    try {
      await directUploadMutation.mutateAsync({
        file: createTypedBrowserFile(file, mimeType),
        kind: uploadKind,
        onProgress: setDirectUploadProgress,
      });
    } catch {
      // The mutation exposes its problem state inline.
    }
  }

  const actionButtons = (
    <>
      {uploadKind ? (
        <Button
          type="button"
          variant="default"
          onClick={() => setIsUploadOpen(true)}
          disabled={isFieldDisabled}
        >
          <UploadCloud className="size-4" />
          Upload new
        </Button>
      ) : null}
      <Button
        type="button"
        variant="outline"
        onClick={() => setIsDialogOpen(true)}
        disabled={isFieldDisabled}
      >
        <Search className="size-4" />
        Browse existing
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={() => onChange(null)}
        disabled={isFieldDisabled || value === null}
      >
        <Trash2 className="size-4" />
        Clear
      </Button>
      <Button
        type="button"
        variant="ghost"
        data-testid={testId ? `${testId}-advanced` : undefined}
        title={resolvedAdvancedLabel}
        className="text-muted-foreground"
        onClick={() => setIsAdvancedOpen((current) => !current)}
        disabled={isFieldDisabled}
      >
        <ChevronDown
          className={cn(
            "size-4 transition-transform",
            isAdvancedOpen ? "rotate-180" : "rotate-0",
          )}
        />
        Advanced
      </Button>
    </>
  );

  return (
    <div className="space-y-3" data-testid={testId}>
      <label className="text-sm font-medium text-foreground" htmlFor={id}>
        {label}
      </label>

      <div
        aria-busy={isDirectUploadPending}
        className={cn(
          "rounded-2xl border border-border/70 bg-card/95 shadow-sm transition-colors",
          useInlineSelectedEditorSurface
            ? "border-0 bg-transparent p-0 shadow-none"
            : isEditor
              ? "p-3"
              : "p-4",
          isDropActive && "border-primary bg-primary/5 ring-2 ring-primary/25",
        )}
        data-testid={testId ? `${testId}-dropzone` : undefined}
        onDragEnter={handleDragEnter}
        onDragLeave={handleDragLeave}
        onDragOver={handleDragOver}
        onDrop={(event) => void handleDrop(event)}
      >
        {value === null ? (
          <div
            className={cn(
              "rounded-2xl border border-dashed border-border/70 bg-muted/20",
              isEditor ? "px-3 py-4" : "px-4 py-5",
            )}
          >
            <p className="text-sm font-medium text-foreground">
              No asset selected
            </p>
            <p
              className={cn(
                "mt-1 text-muted-foreground",
                isEditor ? "text-xs leading-5" : "text-sm",
              )}
            >
              Upload a new {mediaType.toLowerCase()} asset here or browse an
              existing one without leaving this editor.
            </p>
            <p
              className={cn(
                "mt-2 text-muted-foreground",
                isEditor ? "text-xs leading-5" : "text-sm",
              )}
            >
              Drop one {expectedFileLabel} file here to upload it immediately.
            </p>
          </div>
        ) : assetDetailQuery.isLoading ? (
          <div
            className={cn(
              "rounded-2xl border border-dashed border-border/70 bg-muted/20 text-muted-foreground",
              isEditor ? "px-3 py-4 text-xs leading-5" : "px-4 py-5 text-sm",
            )}
          >
            Loading selected asset details...
          </div>
        ) : selectedAsset ? (
          <div className="space-y-3">
            <AssetFieldPreview
              actions={shouldInlineEditorActions ? actionButtons : undefined}
              asset={selectedAsset}
              variant={variant}
            />
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

        {!shouldInlineEditorActions ? (
          <div className={cn("mt-4 flex flex-wrap gap-2", isEditor && "mt-3")}>
            {actionButtons}
          </div>
        ) : null}

        {isDirectUploadPending ? (
          <div className="mt-3 space-y-2 rounded-2xl border border-border/70 bg-muted/20 px-3 py-3">
            <div className="flex items-center justify-between gap-3 text-xs text-muted-foreground">
              <span className="inline-flex items-center gap-2">
                <LoaderCircle className="size-4 animate-spin" />
                Uploading dropped {expectedFileLabel}
              </span>
              <span>{directUploadProgress}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-[width]"
                style={{ width: `${directUploadProgress}%` }}
              />
            </div>
          </div>
        ) : null}

        {directUploadMessage ? (
          <p className="mt-3 text-sm text-destructive" role="alert">
            {directUploadMessage}
          </p>
        ) : null}

        {directUploadMutation.problem ? (
          <ProblemAlert
            className="mt-3"
            problem={directUploadMutation.problem}
          />
        ) : null}

        {description && !shouldInlineEditorActions ? (
          <p
            className={cn(
              "mt-4 text-muted-foreground",
              isEditor ? "text-xs leading-5" : "text-sm",
            )}
          >
            {description}
          </p>
        ) : null}
        {!isEditor ? (
          <p className="mt-2 text-xs text-muted-foreground">
            Normal editorial flows can upload and bind assets here. Media
            Utility stays available for registry, debug, and manual inspection
            work.
          </p>
        ) : null}
      </div>

      {isAdvancedOpen ? (
        <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground" htmlFor={id}>
              {resolvedManualInputLabel}
            </label>
            <Input
              id={id}
              data-testid={testId ? `${testId}-input` : undefined}
              inputMode="numeric"
              placeholder={placeholder}
              type="number"
              value={value ?? ""}
              onChange={(event) => handleManualValueChange(event.target.value)}
              disabled={isFieldDisabled}
            />
            <div className="flex flex-col gap-2 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
              <p>
                Use this only when you already know the asset id. Upload and
                browse actions above remain the default workflow.
              </p>
              {isEditor ? (
                <p className="text-xs">
                  Media Utility remains available for registry, debug, and
                  manual inspection work.
                </p>
              ) : null}
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
