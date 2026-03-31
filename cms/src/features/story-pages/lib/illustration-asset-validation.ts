import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { ApiClientError } from "@/lib/http/client";

async function validateMediaAssetId(
  assetId: number | null,
  expectedMediaType: "IMAGE" | "AUDIO",
  missingMessage: string,
) {
  if (assetId === null) {
    return null;
  }

  try {
    const asset = await assetAdminApi.getAsset(assetId);

    if (asset.mediaType !== expectedMediaType) {
      return `Asset #${assetId} must reference an ${expectedMediaType} asset.`;
    }

    return null;
  } catch (error) {
    if (error instanceof ApiClientError) {
      if (
        error.problem.status === 404 ||
        error.problem.errorCode === "asset_not_found" ||
        error.problem.errorCode === "media_asset_not_found"
      ) {
        return `Asset #${assetId} was not found.`;
      }

      return error.problem.detail;
    }

    return missingMessage;
  }
}

export async function validateIllustrationAssetId(assetId: number | null) {
  return validateMediaAssetId(
    assetId,
    "IMAGE",
    "The illustration asset could not be validated.",
  );
}

export async function validateAudioAssetId(assetId: number | null) {
  return validateMediaAssetId(
    assetId,
    "AUDIO",
    "The audio asset could not be validated.",
  );
}
