import { z } from "zod";

import {
  contributorRoleSchema,
  type AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";

export const contributorFormSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, "Display name is required.")
    .max(120, "Display name must be 120 characters or fewer."),
  roles: z.array(contributorRoleSchema).min(1, "Select at least one role."),
});

export type ContributorFormValues = z.infer<typeof contributorFormSchema>;

export function getCreateContributorFormDefaults(): ContributorFormValues {
  return {
    displayName: "",
    roles: ["AUTHOR"],
  };
}

export function mapContributorToFormValues(
  contributor: ContributorViewModel | AdminContributorResponse,
): ContributorFormValues {
  return {
    displayName: contributor.displayName,
    roles:
      "roles" in contributor && contributor.roles?.length
        ? contributor.roles
        : ["AUTHOR"],
  };
}
