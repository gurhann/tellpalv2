import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { z } from "zod";

function parseOptionalPositiveNumber(value: unknown) {
  if (value === "" || value === undefined || value === null) {
    return null;
  }

  if (typeof value === "number") {
    return Number.isNaN(value) ? value : value;
  }

  if (typeof value === "string") {
    const trimmed = value.trim();

    if (trimmed.length === 0) {
      return null;
    }

    return Number(trimmed);
  }

  return value;
}

function parseOptionalText(value: unknown) {
  if (value === undefined || value === null) {
    return null;
  }

  if (typeof value !== "string") {
    return value;
  }

  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

export const assetMetadataFormSchema = z.object({
  mimeType: z.preprocess(parseOptionalText, z.string().nullable()),
  byteSize: z.preprocess(
    parseOptionalPositiveNumber,
    z
      .number({
        error: "Byte size must be a valid number.",
      })
      .int("Byte size must be a whole number.")
      .nonnegative("Byte size must be zero or greater.")
      .nullable(),
  ),
  checksumSha256: z.preprocess(parseOptionalText, z.string().nullable()),
});

export type AssetMetadataFormValues = z.infer<typeof assetMetadataFormSchema>;

export function mapAssetToMetadataFormValues(
  asset: AssetViewModel,
): AssetMetadataFormValues {
  return {
    mimeType: asset.mimeType,
    byteSize: asset.byteSize,
    checksumSha256: asset.checksumSha256,
  };
}
