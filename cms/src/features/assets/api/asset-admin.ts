import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/media";

const assetProviderValues = ["FIREBASE_STORAGE", "LOCAL_STUB"] as const;
const assetKindValues = [
  "ORIGINAL_IMAGE",
  "ORIGINAL_AUDIO",
  "THUMBNAIL_PHONE",
  "THUMBNAIL_TABLET",
  "DETAIL_PHONE",
  "DETAIL_TABLET",
  "OPTIMIZED_AUDIO",
  "CONTENT_ZIP",
  "CONTENT_ZIP_PART1",
  "CONTENT_ZIP_PART2",
] as const;
const assetMediaTypeValues = ["IMAGE", "AUDIO", "ARCHIVE"] as const;

export const assetProviderSchema = z.enum(assetProviderValues);
export const assetKindSchema = z.enum(assetKindValues);
export const assetMediaTypeSchema = z.enum(assetMediaTypeValues);

export type AssetProvider = z.infer<typeof assetProviderSchema>;
export type AssetKind = z.infer<typeof assetKindSchema>;
export type AssetMediaType = z.infer<typeof assetMediaTypeSchema>;

export type RegisterMediaAssetInput = {
  provider: AssetProvider;
  objectPath: string;
  kind: AssetKind;
  mimeType?: string | null;
  byteSize?: number | null;
  checksumSha256?: string | null;
};

export type UpdateAssetMetadataInput = {
  mimeType?: string | null;
  byteSize?: number | null;
  checksumSha256?: string | null;
};

export const adminAssetResponseSchema = z.object({
  assetId: z.number().int().positive(),
  provider: assetProviderSchema,
  objectPath: z.string().min(1),
  mediaType: assetMediaTypeSchema,
  kind: assetKindSchema,
  mimeType: z.string().nullable(),
  byteSize: z.number().int().nonnegative().nullable(),
  checksumSha256: z.string().nullable(),
  cachedDownloadUrl: z.string().nullable(),
  downloadUrlCachedAt: z.string().nullable(),
  downloadUrlExpiresAt: z.string().nullable(),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
});

export const adminAssetListResponseSchema = z.array(adminAssetResponseSchema);

export type AdminAssetResponse = z.infer<typeof adminAssetResponseSchema>;

export const assetAdminApi = {
  registerAsset(input: RegisterMediaAssetInput) {
    return apiClient.post<AdminAssetResponse>(basePath, {
      body: input,
      responseSchema: adminAssetResponseSchema,
    });
  },
  listRecentAssets(limit = 20) {
    const search = new URLSearchParams({ limit: String(limit) });
    return apiClient.get<AdminAssetResponse[]>(`${basePath}?${search}`, {
      responseSchema: adminAssetListResponseSchema,
    });
  },
  getAsset(assetId: number) {
    return apiClient.get<AdminAssetResponse>(`${basePath}/${assetId}`, {
      responseSchema: adminAssetResponseSchema,
    });
  },
  updateAssetMetadata(assetId: number, input: UpdateAssetMetadataInput) {
    return apiClient.put<AdminAssetResponse>(
      `${basePath}/${assetId}/metadata`,
      {
        body: input,
        responseSchema: adminAssetResponseSchema,
      },
    );
  },
  refreshDownloadUrlCache(assetId: number) {
    return apiClient.post<AdminAssetResponse>(
      `${basePath}/${assetId}/download-url-cache/refresh`,
      {
        responseSchema: adminAssetResponseSchema,
      },
    );
  },
};
