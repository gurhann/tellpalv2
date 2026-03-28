import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/contents";

const contentTypeValues = [
  "STORY",
  "AUDIO_STORY",
  "MEDITATION",
  "LULLABY",
] as const;

const localizationStatusValues = ["DRAFT", "PUBLISHED", "ARCHIVED"] as const;
const processingStatusValues = [
  "PENDING",
  "PROCESSING",
  "COMPLETED",
  "FAILED",
] as const;

export const contentTypeSchema = z.enum(contentTypeValues);
export const contentLocalizationStatusSchema = z.enum(localizationStatusValues);
export const contentProcessingStatusSchema = z.enum(processingStatusValues);

export type ContentType = z.infer<typeof contentTypeSchema>;
export type ContentLocalizationStatus = z.infer<
  typeof contentLocalizationStatusSchema
>;
export type ContentProcessingStatus = z.infer<
  typeof contentProcessingStatusSchema
>;

export type CreateContentInput = {
  type: ContentType;
  externalKey: string;
  ageRange?: number | null;
  active: boolean;
};

export type UpdateContentInput = {
  externalKey: string;
  ageRange?: number | null;
  active: boolean;
};

export type UpsertContentLocalizationInput = {
  title: string;
  description?: string | null;
  bodyText?: string | null;
  coverMediaId?: number | null;
  audioMediaId?: number | null;
  durationMinutes?: number | null;
  status: ContentLocalizationStatus;
  processingStatus: ContentProcessingStatus;
  publishedAt?: string | null;
};

export type UpdateContentLocalizationProcessingInput = {
  processingStatus: ContentProcessingStatus;
};

export type PublishContentLocalizationInput = {
  publishedAt?: string | null;
};

export const adminContentResponseSchema = z.object({
  contentId: z.number().int().positive(),
  type: contentTypeSchema,
  externalKey: z.string(),
  active: z.boolean(),
  ageRange: z.number().int().nonnegative().nullable(),
  pageCount: z.number().int().nonnegative().nullable(),
});

export const adminContentLocalizationResponseSchema = z.object({
  contentId: z.number().int().positive(),
  languageCode: z.string().min(1),
  title: z.string(),
  description: z.string().nullable(),
  bodyText: z.string().nullable(),
  coverMediaId: z.number().int().positive().nullable(),
  audioMediaId: z.number().int().positive().nullable(),
  durationMinutes: z.number().int().nonnegative().nullable(),
  status: contentLocalizationStatusSchema,
  processingStatus: contentProcessingStatusSchema,
  publishedAt: z.string().nullable(),
  visibleToMobile: z.boolean(),
});

export type AdminContentResponse = z.infer<typeof adminContentResponseSchema>;
export type AdminContentLocalizationResponse = z.infer<
  typeof adminContentLocalizationResponseSchema
>;

export const contentAdminBacklogDependencies = {
  listContents: "BG01",
  getContent: "BG01",
  deleteContent: "BG01",
} as const;

export const contentAdminApi = {
  createContent(input: CreateContentInput) {
    return apiClient.post<AdminContentResponse>(basePath, {
      body: input,
      responseSchema: adminContentResponseSchema,
    });
  },
  updateContent(contentId: number, input: UpdateContentInput) {
    return apiClient.put<AdminContentResponse>(`${basePath}/${contentId}`, {
      body: input,
      responseSchema: adminContentResponseSchema,
    });
  },
  createLocalization(
    contentId: number,
    languageCode: string,
    input: UpsertContentLocalizationInput,
  ) {
    return apiClient.post<AdminContentLocalizationResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}`,
      {
        body: input,
        responseSchema: adminContentLocalizationResponseSchema,
      },
    );
  },
  updateLocalization(
    contentId: number,
    languageCode: string,
    input: UpsertContentLocalizationInput,
  ) {
    return apiClient.put<AdminContentLocalizationResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}`,
      {
        body: input,
        responseSchema: adminContentLocalizationResponseSchema,
      },
    );
  },
  updateLocalizationProcessingStatus(
    contentId: number,
    languageCode: string,
    input: UpdateContentLocalizationProcessingInput,
  ) {
    return apiClient.patch<AdminContentLocalizationResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}/processing-status`,
      {
        body: input,
        responseSchema: adminContentLocalizationResponseSchema,
      },
    );
  },
  publishLocalization(
    contentId: number,
    languageCode: string,
    input?: PublishContentLocalizationInput,
  ) {
    return apiClient.post<AdminContentLocalizationResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}/publish`,
      {
        body: input,
        responseSchema: adminContentLocalizationResponseSchema,
      },
    );
  },
  archiveLocalization(contentId: number, languageCode: string) {
    return apiClient.post<AdminContentLocalizationResponse>(
      `${basePath}/${contentId}/localizations/${languageCode}/archive`,
      {
        responseSchema: adminContentLocalizationResponseSchema,
      },
    );
  },
};
