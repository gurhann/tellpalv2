import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/categories";

const categoryTypeValues = [
  "STORY",
  "AUDIO_STORY",
  "MEDITATION",
  "LULLABY",
] as const;
const categoryLocalizationStatusValues = [
  "DRAFT",
  "PUBLISHED",
  "ARCHIVED",
] as const;

export const categoryTypeSchema = z.enum(categoryTypeValues);
export const categoryLocalizationStatusSchema = z.enum(
  categoryLocalizationStatusValues,
);

export type CategoryType = z.infer<typeof categoryTypeSchema>;
export type CategoryLocalizationStatus = z.infer<
  typeof categoryLocalizationStatusSchema
>;

export type CreateCategoryInput = {
  slug: string;
  type: CategoryType;
  premium: boolean;
  active: boolean;
};

export type UpdateCategoryInput = CreateCategoryInput;

export type UpsertCategoryLocalizationInput = {
  name: string;
  description?: string | null;
  imageMediaId?: number | null;
  status: CategoryLocalizationStatus;
  publishedAt?: string | null;
};

export const adminCategoryResponseSchema = z.object({
  categoryId: z.number().int().positive(),
  type: categoryTypeSchema,
  slug: z.string().min(1),
  premium: z.boolean(),
  active: z.boolean(),
});

export const adminCategoryLocalizationResponseSchema = z.object({
  categoryId: z.number().int().positive(),
  languageCode: z.string().min(1),
  name: z.string().min(1),
  description: z.string().nullable(),
  imageMediaId: z.number().int().positive().nullable(),
  status: categoryLocalizationStatusSchema,
  publishedAt: z.string().nullable(),
  published: z.boolean(),
});

export type AdminCategoryResponse = z.infer<typeof adminCategoryResponseSchema>;
export type AdminCategoryLocalizationResponse = z.infer<
  typeof adminCategoryLocalizationResponseSchema
>;
const adminCategoryListResponseSchema = z.array(adminCategoryResponseSchema);
const adminCategoryLocalizationListResponseSchema = z.array(
  adminCategoryLocalizationResponseSchema,
);

export const categoryAdminApi = {
  listCategories() {
    return apiClient.get<AdminCategoryResponse[]>(basePath, {
      responseSchema: adminCategoryListResponseSchema,
    });
  },
  createCategory(input: CreateCategoryInput) {
    return apiClient.post<AdminCategoryResponse>(basePath, {
      body: input,
      responseSchema: adminCategoryResponseSchema,
    });
  },
  getCategory(categoryId: number) {
    return apiClient.get<AdminCategoryResponse>(`${basePath}/${categoryId}`, {
      responseSchema: adminCategoryResponseSchema,
    });
  },
  listLocalizations(categoryId: number) {
    return apiClient.get<AdminCategoryLocalizationResponse[]>(
      `${basePath}/${categoryId}/localizations`,
      {
        responseSchema: adminCategoryLocalizationListResponseSchema,
      },
    );
  },
  updateCategory(categoryId: number, input: UpdateCategoryInput) {
    return apiClient.put<AdminCategoryResponse>(`${basePath}/${categoryId}`, {
      body: input,
      responseSchema: adminCategoryResponseSchema,
    });
  },
  createLocalization(
    categoryId: number,
    languageCode: string,
    input: UpsertCategoryLocalizationInput,
  ) {
    return apiClient.post<AdminCategoryLocalizationResponse>(
      `${basePath}/${categoryId}/localizations/${languageCode}`,
      {
        body: input,
        responseSchema: adminCategoryLocalizationResponseSchema,
      },
    );
  },
  updateLocalization(
    categoryId: number,
    languageCode: string,
    input: UpsertCategoryLocalizationInput,
  ) {
    return apiClient.put<AdminCategoryLocalizationResponse>(
      `${basePath}/${categoryId}/localizations/${languageCode}`,
      {
        body: input,
        responseSchema: adminCategoryLocalizationResponseSchema,
      },
    );
  },
};
