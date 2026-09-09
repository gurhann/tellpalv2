import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/contents";
const registryPath = "/api/admin/content-registry";

const contentTypeValues = [
  "STORY",
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
  listeningCoverMediaId?: number | null;
  listingCoverMediaId?: number | null;
};

export type UpsertContentLocalizationInput = {
  title: string;
  description?: string | null;
  bodyText?: string | null;
  coverMediaId?: number | null;
  audioMediaId?: number | null;
  durationMinutes?: number | null;
  status: ContentLocalizationStatus;
  processingStatus?: ContentProcessingStatus;
  publishedAt?: string | null;
  narration?: { audioMediaId: number; durationMinutes: number } | null;
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
  listeningCoverMediaId: z.number().int().positive().nullable(),
  listingCoverMediaId: z.number().int().positive().nullable(),
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
  narration: z.object({
    audioMediaId: z.number().int().positive(),
    durationMinutes: z.number().int().nonnegative(),
    processingStatus: contentProcessingStatusSchema.nullable(),
    processingError: z.string().nullable(),
  }).nullable().optional(),
});

export const adminContentReadResponseSchema = adminContentResponseSchema.extend(
  {
    playback: z
      .object({
        audioMediaId: z.number().int().positive(),
        durationMinutes: z.number().int().nonnegative(),
        processingStatus: contentProcessingStatusSchema.nullable(),
        processingError: z.string().nullable(),
        instruments: z
          .array(
            z.object({
              instrumentId: z.number().int().positive(),
              code: z.string().min(1),
              displayName: z.string().min(1).nullable(),
              displayOrder: z.number().int().nonnegative(),
            }),
          )
          .default([]),
      })
      .nullable()
      .optional(),
    localizations: z.array(adminContentLocalizationResponseSchema),
  },
);

const lullabyInstrumentSchema = z.object({
  instrumentId: z.number().int().positive(),
  code: z.string().min(1),
  displayName: z.string().min(1).nullable(),
  displayOrder: z.number().int().nonnegative(),
});
const instrumentCatalogOptionSchema = z.object({
  instrumentId: z.number().int().positive(),
  code: z.string().min(1),
  displayName: z.string().min(1),
});
const lullabyPlaybackResponseSchema = z.object({
  contentId: z.number().int().positive(),
  audioMediaId: z.number().int().positive(),
  durationMinutes: z.number().int().nonnegative(),
  processingStatus: contentProcessingStatusSchema.nullable(),
  processingError: z.string().nullable(),
});

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
export type LullabyInstrument = z.infer<typeof lullabyInstrumentSchema>;
export type InstrumentCatalogOption = z.infer<typeof instrumentCatalogOptionSchema>;
export type LullabyPlaybackResponse = z.infer<typeof lullabyPlaybackResponseSchema>;
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
  updateLullabyPlayback(
    contentId: number,
    input: { audioMediaId: number; durationMinutes: number },
  ) {
    return apiClient.put<LullabyPlaybackResponse>(`${basePath}/${contentId}/playback`, {
      body: input,
      responseSchema: lullabyPlaybackResponseSchema,
    });
  },
  listLullabyInstruments(contentId: number, languageCode?: string) {
    const query = languageCode
      ? `?languageCode=${encodeURIComponent(languageCode)}`
      : "";
    return apiClient.get<LullabyInstrument[]>(`${basePath}/${contentId}/instruments${query}`, {
      responseSchema: z.array(lullabyInstrumentSchema),
    });
  },
  replaceLullabyInstruments(
    contentId: number,
    instrumentCodes: string[],
    languageCode?: string,
  ) {
    const query = languageCode
      ? `?languageCode=${encodeURIComponent(languageCode)}`
      : "";
    return apiClient.put<LullabyInstrument[]>(`${basePath}/${contentId}/instruments${query}`, {
      body: { instrumentCodes },
      responseSchema: z.array(lullabyInstrumentSchema),
    });
  },
  listInstrumentCatalog(languageCode: string) {
    return apiClient.get<InstrumentCatalogOption[]>(
      `/api/admin/instrument-catalog?languageCode=${encodeURIComponent(languageCode)}`,
      { responseSchema: z.array(instrumentCatalogOptionSchema) },
    );
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
