import type {
  AssetMediaType,
  UploadableAssetKind,
} from "@/features/assets/api/asset-admin";

export function inferMimeType(file: File) {
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

export function formatByteSize(byteSize: number) {
  if (byteSize < 1024) {
    return `${byteSize} B`;
  }

  const kiloBytes = byteSize / 1024;
  if (kiloBytes < 1024) {
    return `${kiloBytes.toFixed(1)} KB`;
  }

  return `${(kiloBytes / 1024).toFixed(1)} MB`;
}

export function getUploadKindForMediaType(
  mediaType: AssetMediaType,
): UploadableAssetKind | null {
  if (mediaType === "IMAGE") {
    return "ORIGINAL_IMAGE";
  }
  if (mediaType === "AUDIO") {
    return "ORIGINAL_AUDIO";
  }
  return null;
}

export function getUploadKindForMimeType(
  mimeType: string,
): UploadableAssetKind | null {
  const normalizedMimeType = mimeType.toLowerCase();
  if (normalizedMimeType.startsWith("image/")) {
    return "ORIGINAL_IMAGE";
  }
  if (normalizedMimeType.startsWith("audio/")) {
    return "ORIGINAL_AUDIO";
  }
  return null;
}

export function getAcceptForUploadKind(kind: UploadableAssetKind) {
  return kind === "ORIGINAL_IMAGE" ? "image/*" : "audio/*";
}

export function uploadKindMatchesMimeType(
  kind: UploadableAssetKind,
  mimeType: string,
) {
  const normalizedMimeType = mimeType.toLowerCase();
  return kind === "ORIGINAL_IMAGE"
    ? normalizedMimeType.startsWith("image/")
    : normalizedMimeType.startsWith("audio/");
}

export function createTypedBrowserFile(file: File, mimeType: string) {
  return new File([file], file.name, {
    type: mimeType,
    lastModified: file.lastModified,
  });
}
