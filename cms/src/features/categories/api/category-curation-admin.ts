import { z } from "zod";

import { apiClient } from "@/lib/http/client";

function getBasePath(categoryId: number, languageCode: string) {
  return `/api/admin/categories/${categoryId}/localizations/${languageCode}/contents`;
}

export type AddCategoryContentInput = {
  contentId: number;
  displayOrder: number;
};

export type UpdateCategoryContentOrderInput = {
  displayOrder: number;
};

export const adminCategoryContentResponseSchema = z.object({
  categoryId: z.number().int().positive(),
  languageCode: z.string().min(1),
  contentId: z.number().int().positive(),
  displayOrder: z.number().int().nonnegative(),
});

export const adminCategoryContentListResponseSchema = z.array(
  adminCategoryContentResponseSchema,
);

export type AdminCategoryContentResponse = z.infer<
  typeof adminCategoryContentResponseSchema
>;

export const categoryCurationAdminApi = {
  listCuratedContent(categoryId: number, languageCode: string) {
    return apiClient.get<AdminCategoryContentResponse[]>(
      getBasePath(categoryId, languageCode),
      {
        responseSchema: adminCategoryContentListResponseSchema,
      },
    );
  },
  addCuratedContent(
    categoryId: number,
    languageCode: string,
    input: AddCategoryContentInput,
  ) {
    return apiClient.post<AdminCategoryContentResponse>(
      getBasePath(categoryId, languageCode),
      {
        body: input,
        responseSchema: adminCategoryContentResponseSchema,
      },
    );
  },
  updateCuratedContentOrder(
    categoryId: number,
    languageCode: string,
    contentId: number,
    input: UpdateCategoryContentOrderInput,
  ) {
    return apiClient.put<AdminCategoryContentResponse>(
      `${getBasePath(categoryId, languageCode)}/${contentId}`,
      {
        body: input,
        responseSchema: adminCategoryContentResponseSchema,
      },
    );
  },
  removeCuratedContent(
    categoryId: number,
    languageCode: string,
    contentId: number,
  ) {
    return apiClient.delete<void>(
      `${getBasePath(categoryId, languageCode)}/${contentId}`,
    );
  },
};
