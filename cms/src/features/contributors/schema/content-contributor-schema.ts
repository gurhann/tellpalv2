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
});

export type ContentContributorFormValues = z.infer<
  typeof contentContributorFormSchema
>;

export const editContentContributorFormSchema = z.object({
  role: contributorRoleSchema,
  languageCode: z.string().nullable(),
  creditName: z
    .string()
    .max(120, "Credit name must be 120 characters or fewer."),
});

export type EditContentContributorFormValues = z.infer<
  typeof editContentContributorFormSchema
>;

export function getAssignContributorFormDefaults(): ContentContributorFormValues {
  return {
    contributorId: 0,
    role: "AUTHOR",
    languageCode: null,
    creditName: "",
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

  return null;
}
