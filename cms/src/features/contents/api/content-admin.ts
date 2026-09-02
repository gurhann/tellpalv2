import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/contents";
const registryPath = "/api/admin/content-registry";

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

export const contentRegistryReadinessSchema = z.enum([
  "ACTION_REQUIRED",
  "READY_TO_PUBLISH",
  "PUBLISHED",
]);
export type ContentRegistryReadiness = z.infer<
  typeof contentRegistryReadinessSchema
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
  textlessCoverMediaId?: number | null;
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
  textlessCoverMediaId: z.number().int().positive().nullable(),
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

export const adminContentReadResponseSchema = adminContentResponseSchema.extend(
  {
    localizations: z.array(adminContentLocalizationResponseSchema),
  },
);

export const adminContentReadListResponseSchema = z.array(
  adminContentReadResponseSchema,
);

export const adminContentRegistryBlockerSchema = z.object({
  code: z.string(),
  pageNumber: z.number().int().positive().nullable(),
});
export const adminContentRegistryItemSchema = z.object({
  contentId: z.number().int().positive(),
  type: contentTypeSchema,
  externalKey: z.string(),
  pageCount: z.number().int().nonnegative().nullable(),
  selectedLanguage: z.string().min(1),
  title: z.string().nullable(),
  readiness: contentRegistryReadinessSchema,
  blockers: z.array(adminContentRegistryBlockerSchema),
  lastEditedAt: z.string(),
});
export const adminContentRegistryPageSchema = z.object({
  items: z.array(adminContentRegistryItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalItems: z.number().int().nonnegative(),
});

export type AdminContentResponse = z.infer<typeof adminContentResponseSchema>;
export type AdminContentLocalizationResponse = z.infer<
  typeof adminContentLocalizationResponseSchema
>;
export type AdminContentReadResponse = z.infer<
  typeof adminContentReadResponseSchema
>;
export type AdminContentRegistryPage = z.infer<
  typeof adminContentRegistryPageSchema
>;
export type AdminContentRegistryItem = z.infer<
  typeof adminContentRegistryItemSchema
>;
export type ContentRegistryQuery = {
  language: string;
  type?: ContentType;
  readiness?: ContentRegistryReadiness;
  q?: string;
  page?: number;
  size?: number;
};

export const contentAdminBacklogDependencies = {
  listContents: "BG01",
  getContent: "BG01",
  deleteContent: "BG01",
} as const;

export const contentAdminApi = {
  listContents() {
    return apiClient.get<AdminContentReadResponse[]>(basePath, {
      responseSchema: adminContentReadListResponseSchema,
    });
  },
  listRegistry(params: ContentRegistryQuery) {
    const searchParams = new URLSearchParams({ language: params.language });
    if (params.type) searchParams.set("type", params.type);
    if (params.readiness) searchParams.set("readiness", params.readiness);
    if (params.q?.trim()) searchParams.set("q", params.q.trim());
    if (params.page !== undefined) searchParams.set("page", `${params.page}`);
    if (params.size !== undefined) searchParams.set("size", `${params.size}`);
    return apiClient.get<AdminContentRegistryPage>(
      `${registryPath}?${searchParams.toString()}`,
      { responseSchema: adminContentRegistryPageSchema },
    );
  },
  getContent(contentId: number) {
    return apiClient.get<AdminContentReadResponse>(`${basePath}/${contentId}`, {
      responseSchema: adminContentReadResponseSchema,
    });
  },
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
