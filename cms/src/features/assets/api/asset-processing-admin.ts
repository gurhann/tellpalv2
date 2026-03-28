import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/media-processing";

const assetProcessingContentTypeValues = [
  "STORY",
  "AUDIO_STORY",
  "MEDITATION",
  "LULLABY",
] as const;
const assetProcessingStateValues = [
  "PENDING",
  "PROCESSING",
  "COMPLETED",
  "FAILED",
] as const;

export const assetProcessingContentTypeSchema = z.enum(
  assetProcessingContentTypeValues,
);
export const assetProcessingStateSchema = z.enum(assetProcessingStateValues);

export type AssetProcessingContentType = z.infer<
  typeof assetProcessingContentTypeSchema
>;
export type AssetProcessingState = z.infer<typeof assetProcessingStateSchema>;

export type ScheduleAssetProcessingInput = {
  contentId: number;
  languageCode: string;
  contentType: AssetProcessingContentType;
  externalKey: string;
  coverSourceAssetId?: number | null;
  audioSourceAssetId?: number | null;
  pageCount?: number | null;
};

export type RetryAssetProcessingInput = {
  contentType: AssetProcessingContentType;
  externalKey: string;
  coverSourceAssetId?: number | null;
  audioSourceAssetId?: number | null;
  pageCount?: number | null;
};

export const adminAssetProcessingResponseSchema = z.object({
  processingId: z.number().int().positive(),
  contentId: z.number().int().positive(),
  languageCode: z.string().min(1),
  contentType: assetProcessingContentTypeSchema,
  externalKey: z.string().min(1),
  coverSourceAssetId: z.number().int().positive().nullable(),
  audioSourceAssetId: z.number().int().positive().nullable(),
  pageCount: z.number().int().nonnegative().nullable(),
  status: assetProcessingStateSchema,
  attemptCount: z.number().int().nonnegative(),
  nextAttemptAt: z.string().min(1),
  leaseExpiresAt: z.string().nullable(),
  startedAt: z.string().nullable(),
  completedAt: z.string().nullable(),
  failedAt: z.string().nullable(),
  lastErrorCode: z.string().nullable(),
  lastErrorMessage: z.string().nullable(),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
});

export const adminAssetProcessingListResponseSchema = z.array(
  adminAssetProcessingResponseSchema,
);

export type AdminAssetProcessingResponse = z.infer<
  typeof adminAssetProcessingResponseSchema
>;

export const assetProcessingAdminApi = {
  scheduleProcessing(input: ScheduleAssetProcessingInput) {
    return apiClient.post<AdminAssetProcessingResponse>(basePath, {
      body: input,
      responseSchema: adminAssetProcessingResponseSchema,
    });
  },
  retryProcessing(
    contentId: number,
    languageCode: string,
    input: RetryAssetProcessingInput,
  ) {
    return apiClient.post<AdminAssetProcessingResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}/retry`,
      {
        body: input,
        responseSchema: adminAssetProcessingResponseSchema,
      },
    );
  },
  getProcessingStatus(contentId: number, languageCode: string) {
    return apiClient.get<AdminAssetProcessingResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}`,
      {
        responseSchema: adminAssetProcessingResponseSchema,
      },
    );
  },
  listRecentProcessing(limit = 20) {
    const search = new URLSearchParams({ limit: String(limit) });
    return apiClient.get<AdminAssetProcessingResponse[]>(
      `${basePath}?${search}`,
      {
        responseSchema: adminAssetProcessingListResponseSchema,
      },
    );
  },
};
