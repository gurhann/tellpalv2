import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const basePath = "/api/admin/free-access";

export type GrantFreeAccessInput = {
  accessKey: string;
  contentId: number;
  languageCode: string;
};

export const adminContentFreeAccessResponseSchema = z.object({
  freeAccessId: z.number().int().positive(),
  accessKey: z.string().min(1),
  contentId: z.number().int().positive(),
  languageCode: z.string().min(1),
});

export const adminContentFreeAccessListResponseSchema = z.array(
  adminContentFreeAccessResponseSchema,
);

export type AdminContentFreeAccessResponse = z.infer<
  typeof adminContentFreeAccessResponseSchema
>;

export const freeAccessAdminApi = {
  grantFreeAccess(input: GrantFreeAccessInput) {
    return apiClient.post<AdminContentFreeAccessResponse>(basePath, {
      body: input,
      responseSchema: adminContentFreeAccessResponseSchema,
    });
  },
  listFreeAccessEntries(accessKey?: string) {
    const search = new URLSearchParams();

    if (accessKey) {
      search.set("accessKey", accessKey);
    }

    const path = search.size > 0 ? `${basePath}?${search}` : basePath;

    return apiClient.get<AdminContentFreeAccessResponse[]>(path, {
      responseSchema: adminContentFreeAccessListResponseSchema,
    });
  },
  revokeFreeAccess(accessKey: string, languageCode: string, contentId: number) {
    return apiClient.delete<void>(
      `${basePath}/${accessKey}/languages/${languageCode}/contents/${contentId}`,
    );
  },
};
