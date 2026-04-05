import { LoaderCircle, UploadCloud } from "lucide-react";
import { useMemo, useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useUploadAsset } from "@/features/assets/mutations/use-upload-asset";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";

type AssetUploadDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploaded?: (asset: AssetViewModel) => void;
};

const assetKindOptions = [
  {
    value: "ORIGINAL_IMAGE" as const,
    label: "Original image",
    accept: "image/*",
    helper: "Upload source illustrations, covers, or raw artwork.",
  },
  {
    value: "ORIGINAL_AUDIO" as const,
    label: "Original audio",
    accept: "audio/*",
    helper: "Upload source narration or original long-form audio.",
  },
];

function inferMimeType(file: File) {
  if (file.type) {
    return file.type;
  }

  const normalizedName = file.name.trim().toLowerCase();
  if (normalizedName.endsWith(".jpg") || normalizedName.endsWith(".jpeg")) {
    return "image/jpeg";
  }
  if (normalizedName.endsWith(".png")) {
    return "image/png";
  }
  if (normalizedName.endsWith(".webp")) {
    return "image/webp";
  }
  if (normalizedName.endsWith(".mp3")) {
    return "audio/mpeg";
  }
  if (normalizedName.endsWith(".wav")) {
    return "audio/wav";
  }
  if (normalizedName.endsWith(".m4a")) {
    return "audio/mp4";
  }
  if (normalizedName.endsWith(".ogg")) {
    return "audio/ogg";
  }
  return null;
}

function formatByteSize(byteSize: number) {
  if (byteSize < 1024) {
    return `${byteSize} B`;
  }

  const kiloBytes = byteSize / 1024;
  if (kiloBytes < 1024) {
    return `${kiloBytes.toFixed(1)} KB`;
  }

  return `${(kiloBytes / 1024).toFixed(1)} MB`;
}

export function AssetUploadDialog({
  open,
  onOpenChange,
  onUploaded,
}: AssetUploadDialogProps) {
  const [selectedKind, setSelectedKind] = useState<
    "ORIGINAL_IMAGE" | "ORIGINAL_AUDIO"
  >("ORIGINAL_IMAGE");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [validationMessage, setValidationMessage] = useState<string | null>(
    null,
  );
  const uploadMutation = useUploadAsset({
    onSuccess: async (asset) => {
      onUploaded?.(asset);
      resetDialogState();
      onOpenChange(false);
    },
  });
  const resetUploadMutation = uploadMutation.reset;
  const selectedKindOption = useMemo(
    () => assetKindOptions.find((option) => option.value === selectedKind)!,
    [selectedKind],
  );

  function resetDialogState() {
    setSelectedKind("ORIGINAL_IMAGE");
    setSelectedFile(null);
    setProgress(0);
    setValidationMessage(null);
    resetUploadMutation();
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      resetDialogState();
    }
    onOpenChange(nextOpen);
  }

  async function handleSubmit() {
    if (!selectedFile) {
      setValidationMessage("Choose a file before starting the upload.");
      return;
    }

    const mimeType = inferMimeType(selectedFile);
    if (!mimeType) {
      setValidationMessage(
        "The browser could not infer a supported MIME type from the selected file.",
      );
      return;
    }

    if (
      selectedKind === "ORIGINAL_IMAGE" &&
      !mimeType.toLowerCase().startsWith("image/")
    ) {
      setValidationMessage("Original image uploads require an image file.");
      return;
    }

    if (
      selectedKind === "ORIGINAL_AUDIO" &&
      !mimeType.toLowerCase().startsWith("audio/")
    ) {
      setValidationMessage("Original audio uploads require an audio file.");
      return;
    }

    setValidationMessage(null);
    setProgress(0);
    const browserFile = new File([selectedFile], selectedFile.name, {
      type: mimeType,
      lastModified: selectedFile.lastModified,
    });
    await uploadMutation.mutateAsync({
      file: browserFile,
      kind: selectedKind,
      onProgress: setProgress,
    });
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Upload asset</DialogTitle>
          <DialogDescription>
            Upload original image or audio files directly into Firebase Storage.
            The backend signs the upload request, and finalize registers the new
            asset in the media library.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="grid gap-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="asset-upload-kind"
            >
              Asset kind
            </label>
            <Select
              value={selectedKind}
              onValueChange={(value) =>
                setSelectedKind(value as "ORIGINAL_IMAGE" | "ORIGINAL_AUDIO")
              }
            >
              <SelectTrigger id="asset-upload-kind" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {assetKindOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-sm text-muted-foreground">
              {selectedKindOption.helper}
            </p>
          </div>

          <div className="grid gap-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="asset-upload-file"
            >
              File
            </label>
            <Input
              id="asset-upload-file"
              accept={selectedKindOption.accept}
              disabled={uploadMutation.isPending}
              onChange={(event) => {
                setSelectedFile(event.target.files?.[0] ?? null);
                setValidationMessage(null);
              }}
              type="file"
            />
          </div>

          {selectedFile ? (
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
              <p className="font-medium text-foreground">{selectedFile.name}</p>
              <p className="mt-1">
                {inferMimeType(selectedFile) ?? "Unknown MIME type"} /{" "}
                {formatByteSize(selectedFile.size)}
              </p>
            </div>
          ) : null}

          {uploadMutation.isPending ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm text-muted-foreground">
                <span>Upload progress</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-primary transition-[width]"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          ) : null}

          {validationMessage ? (
            <ProblemAlert
              description={validationMessage}
              title="Upload request is invalid"
            />
          ) : null}

          {uploadMutation.problem ? (
            <ProblemAlert problem={uploadMutation.problem} />
          ) : null}
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={uploadMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!selectedFile || uploadMutation.isPending}
          >
            {uploadMutation.isPending ? (
              <>
                <LoaderCircle className="size-4 animate-spin" />
                Uploading
              </>
            ) : (
              <>
                <UploadCloud className="size-4" />
                Upload asset
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
