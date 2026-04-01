import { z } from "zod";

import {
  contributorRoleSchema,
  type ContributorRole,
} from "@/features/contributors/api/contributor-admin";
import {
  GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL,
  type ContentContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
import { mapLanguage, normalizeLanguageCode } from "@/lib/languages";

export const contributorRoleOptions: Array<{
  value: ContributorRole;
  label: string;
}> = [
  {
    value: "AUTHOR",
    label: "Author",
  },
  {
    value: "ILLUSTRATOR",
    label: "Illustrator",
  },
  {
    value: "NARRATOR",
    label: "Narrator",
  },
  {
    value: "MUSICIAN",
    label: "Musician",
  },
];

export const contentContributorFormSchema = z.object({
  contributorId: z
    .number({ error: "Contributor is required." })
    .int()
    .positive("Contributor is required."),
  role: contributorRoleSchema,
  languageCode: z.string().nullable(),
  creditName: z
    .string()
    .max(120, "Credit name must be 120 characters or fewer."),
  sortOrder: z
    .number({ error: "Sort order is required." })
    .int("Sort order must be a whole number.")
    .min(0, "Sort order must be zero or greater."),
});

export type ContentContributorFormValues = z.infer<
  typeof contentContributorFormSchema
>;

export function getAssignContributorFormDefaults(): ContentContributorFormValues {
  return {
    contributorId: 0,
    role: "AUTHOR",
    languageCode: null,
    creditName: "",
    sortOrder: 0,
  };
}

export function getContributorRoleLabel(role: ContributorRole) {
  return (
    contributorRoleOptions.find((option) => option.value === role)?.label ??
    role
  );
}

export function normalizeContributorLanguageScope(
  languageCode: string | null | undefined,
) {
  if (languageCode === null || languageCode === undefined) {
    return null;
  }

  const normalizedLanguageCode = normalizeLanguageCode(languageCode);

  return normalizedLanguageCode || null;
}

export function getContributorLanguageScopeLabel(
  languageCode: string | null | undefined,
) {
  const normalizedLanguageCode =
    normalizeContributorLanguageScope(languageCode);

  if (normalizedLanguageCode === null) {
    return GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL;
  }

  return mapLanguage(normalizedLanguageCode).label;
}

export function validateLocalContentContributorAssignment(
  values: ContentContributorFormValues,
  existingAssignments: ContentContributorViewModel[],
) {
  const normalizedLanguageCode = normalizeContributorLanguageScope(
    values.languageCode,
  );
  const duplicateCredit = existingAssignments.find(
    (assignment) =>
      assignment.contributorId === values.contributorId &&
      assignment.role === values.role &&
      assignment.languageCode === normalizedLanguageCode,
  );

  if (duplicateCredit) {
    const roleLabel = getContributorRoleLabel(values.role);
    const languageLabel = getContributorLanguageScopeLabel(values.languageCode);

    return {
      field: "contributorId" as const,
      message: `${duplicateCredit.displayName} already has a ${roleLabel} credit in ${languageLabel}.`,
    };
  }

  const duplicateSortOrder = existingAssignments.find(
    (assignment) =>
      assignment.role === values.role &&
      assignment.languageCode === normalizedLanguageCode &&
      assignment.sortOrder === values.sortOrder,
  );

  if (duplicateSortOrder) {
    const roleLabel = getContributorRoleLabel(values.role);
    const languageLabel = getContributorLanguageScopeLabel(values.languageCode);

    return {
      field: "sortOrder" as const,
      message: `Sort order ${values.sortOrder} is already used for ${roleLabel} credits in ${languageLabel}.`,
    };
  }

  return null;
}
