import { LoaderCircle, UploadCloud } from "lucide-react";
import { useMemo, useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
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
import type { UploadableAssetKind } from "@/features/assets/api/asset-admin";
import { useI18n } from "@/i18n/locale-provider";

type AssetUploadDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploaded?: (asset: AssetViewModel) => void;
  fixedKind?: UploadableAssetKind;
  title?: string;
  description?: string;
};

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
  fixedKind,
  title = "Upload asset",
  description = "Upload original image or audio files directly into Firebase Storage. The backend signs the upload request, and finalize registers the new asset in the media library.",
}: AssetUploadDialogProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          defaultTitle: "Asset yükle",
          defaultDescription:
            "Orijinal görsel veya ses dosyalarını doğrudan Firebase Storage'a yükleyin. Backend yükleme isteğini imzalar ve finalize adımı yeni asset'i medya kütüphanesine kaydeder.",
          kind: "Asset türü",
          file: "Dosya",
          chooseFile: "Yüklemeye başlamadan önce bir dosya seçin.",
          unsupportedMime:
            "Tarayıcı seçilen dosya için desteklenen bir MIME türü çıkaramadı.",
          imageRequired:
            "Orijinal görsel yüklemeleri bir görsel dosyası gerektirir.",
          audioRequired: "Orijinal ses yüklemeleri bir ses dosyası gerektirir.",
          progress: "Yükleme ilerlemesi",
          invalidTitle: "Yükleme isteği geçersiz",
          cancel: "İptal",
          uploading: "Yükleniyor",
          upload: "Asset yükle",
          unknownMime: "Bilinmeyen MIME türü",
        }
      : {
          defaultTitle: "Upload asset",
          defaultDescription:
            "Upload original image or audio files directly into Firebase Storage. The backend signs the upload request, and finalize registers the new asset in the media library.",
          kind: "Asset kind",
          file: "File",
          chooseFile: "Choose a file before starting the upload.",
          unsupportedMime:
            "The browser could not infer a supported MIME type from the selected file.",
          imageRequired: "Original image uploads require an image file.",
          audioRequired: "Original audio uploads require an audio file.",
          progress: "Upload progress",
          invalidTitle: "Upload request is invalid",
          cancel: "Cancel",
          uploading: "Uploading",
          upload: "Upload asset",
          unknownMime: "Unknown MIME type",
        };
  const assetKindOptions = useMemo(
    () => [
      {
        value: "ORIGINAL_IMAGE" as const,
        label: locale === "tr" ? "Orijinal görsel" : "Original image",
        accept: "image/*",
        helper:
          locale === "tr"
            ? "Kaynak illüstrasyonları, kapakları veya ham görsel çalışmaları yükleyin."
            : "Upload source illustrations, covers, or raw artwork.",
      },
      {
        value: "ORIGINAL_AUDIO" as const,
        label: locale === "tr" ? "Orijinal ses" : "Original audio",
        accept: "audio/*",
        helper:
          locale === "tr"
            ? "Kaynak anlatımı veya özgün uzun form ses dosyasını yükleyin."
            : "Upload source narration or original long-form audio.",
      },
    ],
    [locale],
  );
  const [selectedKind, setSelectedKind] = useState<
    "ORIGINAL_IMAGE" | "ORIGINAL_AUDIO"
  >(fixedKind ?? "ORIGINAL_IMAGE");
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
    [assetKindOptions, selectedKind],
  );
  const kindIsLocked = fixedKind !== undefined;

  function resetDialogState() {
    setSelectedKind(fixedKind ?? "ORIGINAL_IMAGE");
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
      setValidationMessage(copy.chooseFile);
      return;
    }

    const mimeType = inferMimeType(selectedFile);
    if (!mimeType) {
      setValidationMessage(copy.unsupportedMime);
      return;
    }

    if (
      selectedKind === "ORIGINAL_IMAGE" &&
      !mimeType.toLowerCase().startsWith("image/")
    ) {
      setValidationMessage(copy.imageRequired);
      return;
    }

    if (
      selectedKind === "ORIGINAL_AUDIO" &&
      !mimeType.toLowerCase().startsWith("audio/")
    ) {
      setValidationMessage(copy.audioRequired);
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
          <DialogTitle>
            {title === "Upload asset" ? copy.defaultTitle : title}
          </DialogTitle>
          <DialogDescription>
            {description ===
            "Upload original image or audio files directly into Firebase Storage. The backend signs the upload request, and finalize registers the new asset in the media library."
              ? copy.defaultDescription
              : description}
          </DialogDescription>
        </DialogHeader>

        <DialogBody className="space-y-4">
          {kindIsLocked ? (
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
              <p className="font-medium text-foreground">
                {selectedKindOption.label}
              </p>
              <p className="mt-1">{selectedKindOption.helper}</p>
            </div>
          ) : (
            <div className="grid gap-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="asset-upload-kind"
              >
                {copy.kind}
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
          )}

          <div className="grid gap-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="asset-upload-file"
            >
              {copy.file}
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
                {inferMimeType(selectedFile) ?? copy.unknownMime} /{" "}
                {formatByteSize(selectedFile.size)}
              </p>
            </div>
          ) : null}

          {uploadMutation.isPending ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm text-muted-foreground">
                <span>{copy.progress}</span>
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
              title={copy.invalidTitle}
            />
          ) : null}

          {uploadMutation.problem ? (
            <ProblemAlert problem={uploadMutation.problem} />
          ) : null}
        </DialogBody>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={uploadMutation.isPending}
          >
            {copy.cancel}
          </Button>
          <Button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!selectedFile || uploadMutation.isPending}
          >
            {uploadMutation.isPending ? (
              <>
                <LoaderCircle className="size-4 animate-spin" />
                {copy.uploading}
              </>
            ) : (
              <>
                <UploadCloud className="size-4" />
                {copy.upload}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
