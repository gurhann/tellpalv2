import { z } from "zod";

import { apiClient } from "@/lib/http/client";

const contributorRoleValues = [
  "AUTHOR",
  "ILLUSTRATOR",
  "NARRATOR",
  "MUSICIAN",
] as const;

export const contributorRoleSchema = z.enum(contributorRoleValues);

export type ContributorRole = z.infer<typeof contributorRoleSchema>;

export type CreateContributorInput = {
  displayName: string;
};

export type RenameContributorInput = {
  displayName: string;
};

export type AssignContentContributorInput = {
  contributorId: number;
  role: ContributorRole;
  languageCode?: string | null;
  creditName?: string | null;
  sortOrder: number;
};

export const adminContributorResponseSchema = z.object({
  contributorId: z.number().int().positive(),
  displayName: z.string().min(1),
});

export const adminContributorListResponseSchema = z.array(
  adminContributorResponseSchema,
);

export const adminContentContributorResponseSchema = z.object({
  contentId: z.number().int().positive(),
  contributorId: z.number().int().positive(),
  contributorDisplayName: z.string().min(1),
  role: contributorRoleSchema,
  languageCode: z.string().min(1).nullable(),
  creditName: z.string().nullable(),
  sortOrder: z.number().int().nonnegative(),
});

export type AdminContributorResponse = z.infer<
  typeof adminContributorResponseSchema
>;
export type AdminContentContributorResponse = z.infer<
  typeof adminContentContributorResponseSchema
>;

export const contributorAdminBacklogDependencies = {
  deleteContributor: "BG03",
  unassignContributor: "BG03",
} as const;

export const contributorAdminApi = {
  createContributor(input: CreateContributorInput) {
    return apiClient.post<AdminContributorResponse>("/api/admin/contributors", {
      body: input,
      responseSchema: adminContributorResponseSchema,
    });
  },
  listContributors(limit = 20) {
    const search = new URLSearchParams({ limit: String(limit) });
    return apiClient.get<AdminContributorResponse[]>(
      `/api/admin/contributors?${search}`,
      {
        responseSchema: adminContributorListResponseSchema,
      },
    );
  },
  renameContributor(contributorId: number, input: RenameContributorInput) {
    return apiClient.put<AdminContributorResponse>(
      `/api/admin/contributors/${contributorId}`,
      {
        body: input,
        responseSchema: adminContributorResponseSchema,
      },
    );
  },
  assignContributor(contentId: number, input: AssignContentContributorInput) {
    return apiClient.post<AdminContentContributorResponse>(
      `/api/admin/contents/${contentId}/contributors`,
      {
        body: input,
        responseSchema: adminContentContributorResponseSchema,
      },
    );
  },
};
