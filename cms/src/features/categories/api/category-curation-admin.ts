import { z } from "zod";

import { apiClient } from "@/lib/http/client";

function getBasePath(categoryId: number, languageCode: string) {
  return `/api/admin/categories/${categoryId}/localizations/${languageCode}/contents`;
}

function getEligibleContentPath(categoryId: number, languageCode: string) {
  return `/api/admin/categories/${categoryId}/localizations/${languageCode}/eligible-contents`;
}

export type AddCategoryContentInput = {
  contentId: number;
  displayOrder: number;
};

export type UpdateCategoryContentOrderInput = {
  displayOrder: number;
};

export type ListEligibleCategoryContentsInput = {
  query?: string;
  limit?: number;
};

export const adminCategoryContentResponseSchema = z.object({
  categoryId: z.number().int().positive(),
  languageCode: z.string().min(1),
  contentId: z.number().int().positive(),
  displayOrder: z.number().int().nonnegative(),
  externalKey: z.string().min(1),
  localizedTitle: z.string().nullable(),
});

export const adminCategoryContentListResponseSchema = z.array(
  adminCategoryContentResponseSchema,
);
export const adminEligibleCategoryContentResponseSchema = z.object({
  contentId: z.number().int().positive(),
  externalKey: z.string().min(1),
  localizedTitle: z.string().min(1),
  languageCode: z.string().min(1),
  publishedAt: z.string().nullable(),
});
export const adminEligibleCategoryContentListResponseSchema = z.array(
  adminEligibleCategoryContentResponseSchema,
);

export type AdminCategoryContentResponse = z.infer<
  typeof adminCategoryContentResponseSchema
>;
export type AdminEligibleCategoryContentResponse = z.infer<
  typeof adminEligibleCategoryContentResponseSchema
>;

function buildEligibleContentPath(
  categoryId: number,
  languageCode: string,
  input?: ListEligibleCategoryContentsInput,
) {
  const params = new URLSearchParams();

  if (input?.query?.trim()) {
    params.set("q", input.query.trim());
  }

  if (typeof input?.limit === "number") {
    params.set("limit", input.limit.toString());
  }

  const queryString = params.toString();
  const path = getEligibleContentPath(categoryId, languageCode);
  return queryString.length > 0 ? `${path}?${queryString}` : path;
}

export const categoryCurationAdminApi = {
  listCuratedContent(categoryId: number, languageCode: string) {
    return apiClient.get<AdminCategoryContentResponse[]>(
      getBasePath(categoryId, languageCode),
      {
        responseSchema: adminCategoryContentListResponseSchema,
      },
    );
  },
  listEligibleContents(
    categoryId: number,
    languageCode: string,
    input?: ListEligibleCategoryContentsInput,
  ) {
    return apiClient.get<AdminEligibleCategoryContentResponse[]>(
      buildEligibleContentPath(categoryId, languageCode, input),
      {
        responseSchema: adminEligibleCategoryContentListResponseSchema,
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
