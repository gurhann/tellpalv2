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
  roles: ContributorRole[];
};

export type RenameContributorInput = {
  displayName: string;
  roles: ContributorRole[];
};

export type ListContributorsInput = {
  query?: string;
  limit?: number;
};

export type ContributorRegistryQuery = {
  page?: number;
  size?: number;
  query?: string;
  role?: ContributorRole;
  enabled?: boolean;
};

export type AssignContentContributorInput = {
  contributorId: number;
  role: ContributorRole;
  languageCode?: string | null;
  creditName?: string | null;
};

export type UnassignContentContributorInput = {
  contributorId: number;
  role: ContributorRole;
  languageCode?: string | null;
};

export type UpdateContentContributorInput = {
  role: ContributorRole;
  languageCode?: string | null;
  creditName?: string | null;
};

export type ReorderContentContributorsInput = {
  role: ContributorRole;
  languageCode?: string | null;
  assignmentIds: number[];
};

export const adminContributorResponseSchema = z.object({
  contributorId: z.number().int().positive(),
  displayName: z.string().min(1),
  roles: z.array(contributorRoleSchema).default([]),
});

export const adminContributorListResponseSchema = z.array(
  adminContributorResponseSchema,
);

export const adminContributorRegistryItemSchema = z.object({
  contributorId: z.number().int().positive(),
  displayName: z.string().min(1),
  roles: z.array(contributorRoleSchema),
  totalUsageCount: z.number().int().nonnegative(),
  usageByRole: z.record(z.string(), z.number().int().nonnegative()),
  updatedAt: z.string().datetime({ offset: true }),
});
export const adminContributorRegistryPageSchema = z.object({
  items: z.array(adminContributorRegistryItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalItems: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
});
export type AdminContributorRegistryPage = z.infer<
  typeof adminContributorRegistryPageSchema
>;

export const adminContentContributorResponseSchema = z.object({
  assignmentId: z.number().int().positive(),
  contentId: z.number().int().positive(),
  contributorId: z.number().int().positive(),
  contributorDisplayName: z.string().min(1),
  contributorRoles: z.array(contributorRoleSchema).default([]),
  role: contributorRoleSchema,
  languageCode: z.string().min(1).nullable(),
  creditName: z.string().nullable(),
  sortOrder: z.number().int().nonnegative(),
});

export type AdminContributorResponse = z.infer<
  typeof adminContributorResponseSchema
>;
export type AdminContentContributorResponse = Omit<
  z.infer<typeof adminContentContributorResponseSchema>,
  "contributorRoles"
> & {
  /** Optional for compatibility with cached responses from before the snapshot field. */
  contributorRoles?: ContributorRole[];
};
export const adminContentContributorListResponseSchema = z.array(
  adminContentContributorResponseSchema,
);

export const contributorAdminApi = {
  createContributor(input: CreateContributorInput) {
    return apiClient.post<AdminContributorResponse>("/api/admin/contributors", {
      body: input,
      responseSchema: adminContributorResponseSchema,
    });
  },
  listContributors(input: ListContributorsInput = {}) {
    const search = new URLSearchParams({
      limit: String(input.limit ?? 20),
    });

    if (input.query?.trim()) {
      search.set("q", input.query.trim());
    }
    return apiClient.get<AdminContributorResponse[]>(
      `/api/admin/contributors?${search}`,
      {
        responseSchema: adminContributorListResponseSchema,
      },
    );
  },
  getContributor(contributorId: number) {
    return apiClient.get<AdminContributorResponse>(
      `/api/admin/contributors/${contributorId}`,
      { responseSchema: adminContributorResponseSchema },
    );
  },
  listContributorRegistry(input: ContributorRegistryQuery = {}) {
    const search = new URLSearchParams({
      page: String(input.page ?? 0),
      size: String(input.size ?? 25),
    });
    if (input.query?.trim()) search.set("q", input.query.trim());
    if (input.role) search.set("role", input.role);
    return apiClient.get<AdminContributorRegistryPage>(
      `/api/admin/contributor-registry?${search.toString()}`,
      { responseSchema: adminContributorRegistryPageSchema },
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
  deleteContributor(contributorId: number) {
    return apiClient.delete<void>(`/api/admin/contributors/${contributorId}`);
  },
  listContentContributors(contentId: number) {
    return apiClient.get<AdminContentContributorResponse[]>(
      `/api/admin/contents/${contentId}/contributors`,
      {
        responseSchema: adminContentContributorListResponseSchema,
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
  updateContributor(
    contentId: number,
    assignmentId: number,
    input: UpdateContentContributorInput,
  ) {
    return apiClient.put<AdminContentContributorResponse>(
      `/api/admin/contents/${contentId}/contributors/${assignmentId}`,
      {
        body: input,
        responseSchema: adminContentContributorResponseSchema,
      },
    );
  },
  reorderContentContributors(
    contentId: number,
    input: ReorderContentContributorsInput,
  ) {
    return apiClient.put<AdminContentContributorResponse[]>(
      `/api/admin/contents/${contentId}/contributors/reorder`,
      {
        body: input,
        responseSchema: adminContentContributorListResponseSchema,
      },
    );
  },
  unassignContributor(
    contentId: number,
    input: UnassignContentContributorInput,
  ) {
    const search = new URLSearchParams({
      contributorId: String(input.contributorId),
      role: input.role,
    });

    if (input.languageCode && input.languageCode.trim().length > 0) {
      search.set("languageCode", input.languageCode.trim());
    }

    return apiClient.delete<void>(
      `/api/admin/contents/${contentId}/contributors?${search.toString()}`,
    );
  },
};
