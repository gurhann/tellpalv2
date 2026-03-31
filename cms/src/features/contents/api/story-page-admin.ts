import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const pageNumberSchema = z.number().int().positive();

export type AddStoryPageInput = {
  pageNumber: number;
  illustrationMediaId?: number | null;
};

export type UpdateStoryPageInput = {
  illustrationMediaId?: number | null;
};

export type UpsertStoryPageLocalizationInput = {
  bodyText?: string | null;
  audioMediaId?: number | null;
};

export const adminStoryPageResponseSchema = z.object({
  contentId: z.number().int().positive(),
  pageNumber: pageNumberSchema,
  illustrationMediaId: z.number().int().positive().nullable(),
  localizationCount: z.number().int().nonnegative(),
});

export const adminStoryPageLocalizationResponseSchema = z.object({
  contentId: z.number().int().positive(),
  pageNumber: pageNumberSchema,
  languageCode: z.string().min(1),
  bodyText: z.string().nullable(),
  audioMediaId: z.number().int().positive().nullable(),
});

export const adminStoryPageReadResponseSchema = adminStoryPageResponseSchema
  .extend({
    localizations: z.array(adminStoryPageLocalizationResponseSchema),
  })
  .strict();

export const adminStoryPageReadListResponseSchema = z.array(
  adminStoryPageReadResponseSchema,
);

export type AdminStoryPageResponse = z.infer<
  typeof adminStoryPageResponseSchema
>;
export type AdminStoryPageReadResponse = z.infer<
  typeof adminStoryPageReadResponseSchema
>;
export type AdminStoryPageLocalizationResponse = z.infer<
  typeof adminStoryPageLocalizationResponseSchema
>;

function getBasePath(contentId: number) {
  return `/api/admin/contents/${contentId}/story-pages`;
}

export const storyPageAdminApi = {
  listStoryPages(contentId: number) {
    return apiClient.get<AdminStoryPageReadResponse[]>(getBasePath(contentId), {
      responseSchema: adminStoryPageReadListResponseSchema,
    });
  },
  getStoryPage(contentId: number, pageNumber: number) {
    return apiClient.get<AdminStoryPageReadResponse>(
      `${getBasePath(contentId)}/${pageNumber}`,
      {
        responseSchema: adminStoryPageReadResponseSchema,
      },
    );
  },
  addStoryPage(contentId: number, input: AddStoryPageInput) {
    return apiClient.post<AdminStoryPageResponse>(getBasePath(contentId), {
      body: input,
      responseSchema: adminStoryPageResponseSchema,
    });
  },
  updateStoryPage(
    contentId: number,
    pageNumber: number,
    input: UpdateStoryPageInput,
  ) {
    return apiClient.put<AdminStoryPageResponse>(
      `${getBasePath(contentId)}/${pageNumber}`,
      {
        body: input,
        responseSchema: adminStoryPageResponseSchema,
      },
    );
  },
  removeStoryPage(contentId: number, pageNumber: number) {
    return apiClient.delete<void>(`${getBasePath(contentId)}/${pageNumber}`);
  },
  upsertStoryPageLocalization(
    contentId: number,
    pageNumber: number,
    languageCode: string,
    input: UpsertStoryPageLocalizationInput,
  ) {
    return apiClient.put<AdminStoryPageLocalizationResponse>(
      `${getBasePath(contentId)}/${pageNumber}/localizations/${languageCode}`,
      {
        body: input,
        responseSchema: adminStoryPageLocalizationResponseSchema,
      },
    );
  },
};
