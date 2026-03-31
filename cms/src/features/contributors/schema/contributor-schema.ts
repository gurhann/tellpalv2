import { z } from "zod";

import type { AdminContributorResponse } from "@/features/contributors/api/contributor-admin";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";

export const contributorFormSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, "Display name is required.")
    .max(120, "Display name must be 120 characters or fewer."),
});

export type ContributorFormValues = z.infer<typeof contributorFormSchema>;

export function getCreateContributorFormDefaults(): ContributorFormValues {
  return {
    displayName: "",
  };
}

export function mapContributorToFormValues(
  contributor: ContributorViewModel | AdminContributorResponse,
): ContributorFormValues {
  return {
    displayName: contributor.displayName,
  };
}
