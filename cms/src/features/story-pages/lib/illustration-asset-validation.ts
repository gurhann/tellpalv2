import { assetAdminApi } from "@/features/assets/api/asset-admin";
import { ApiClientError } from "@/lib/http/client";

export async function validateIllustrationAssetId(assetId: number | null) {
  if (assetId === null) {
    return null;
  }

  try {
    const asset = await assetAdminApi.getAsset(assetId);

    if (asset.mediaType !== "IMAGE") {
      return `Asset #${assetId} must reference an IMAGE asset.`;
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

    return "The illustration asset could not be validated.";
  }
}
