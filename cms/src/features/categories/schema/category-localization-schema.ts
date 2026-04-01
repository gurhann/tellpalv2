import {
  categoryLocalizationStatusSchema,
  type AdminCategoryLocalizationResponse,
  type CategoryLocalizationStatus,
} from "@/features/categories/api/category-admin";
import type { CategoryLocalizationViewModel } from "@/features/categories/model/category-view-model";
import { supportedCmsLanguageOptions } from "@/lib/languages";
import { z } from "zod";

function parseOptionalPositiveNumber(value: unknown) {
  if (value === "" || value === undefined || value === null) {
    return null;
  }

  if (typeof value === "number") {
    return Number.isNaN(value) ? value : value;
  }

  if (typeof value === "string") {
    const trimmed = value.trim();

    if (trimmed.length === 0) {
      return null;
    }

    return Number(trimmed);
  }

  return value;
}

function parseOptionalText(value: unknown) {
  if (value === undefined || value === null) {
    return null;
  }

  if (typeof value !== "string") {
    return value;
  }

  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

export const categoryLocalizationStatusOptions: Array<{
  value: CategoryLocalizationStatus;
  label: string;
}> = [
  { value: "DRAFT", label: "Draft" },
  { value: "PUBLISHED", label: "Published" },
  { value: "ARCHIVED", label: "Archived" },
];

export const categoryLocalizationFormSchema = z
  .object({
    languageCode: z.string().trim().min(1, "Language is required."),
    name: z.string().trim().min(1, "Name is required."),
    description: z.preprocess(parseOptionalText, z.string().nullable()),
    imageMediaId: z.preprocess(
      parseOptionalPositiveNumber,
      z
        .number({
          error: "Image asset id must be a valid number.",
        })
        .int("Image asset id must be a whole number.")
        .positive("Image asset id must be positive.")
        .nullable(),
    ),
    status: categoryLocalizationStatusSchema,
    publishedAt: z.string().nullable(),
  })
  .superRefine((values, context) => {
    if (
      values.status === "PUBLISHED" &&
      (!values.publishedAt || values.publishedAt.trim().length === 0)
    ) {
      context.addIssue({
        code: "custom",
        path: ["publishedAt"],
        message: "Published at is required when status is Published.",
      });
    }
  });

export type CategoryLocalizationFormValues = z.infer<
  typeof categoryLocalizationFormSchema
>;

export function getCreateCategoryLocalizationDefaults(
  languageCode = supportedCmsLanguageOptions[0]?.code ?? "en",
): CategoryLocalizationFormValues {
  return {
    languageCode,
    name: "",
    description: null,
    imageMediaId: null,
    status: "DRAFT",
    publishedAt: null,
  };
}

function toDateTimeLocalValue(value: string | null) {
  if (!value) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

export function toPublishedAtPayload(value: string | null) {
  if (!value || value.trim().length === 0) {
    return null;
  }

  return new Date(value).toISOString();
}

export function mapCategoryLocalizationToFormValues(
  localization: CategoryLocalizationViewModel,
): CategoryLocalizationFormValues {
  return {
    languageCode: localization.languageCode,
    name: localization.name,
    description: localization.description,
    imageMediaId: localization.imageAssetId,
    status: localization.status,
    publishedAt: toDateTimeLocalValue(localization.publishedAt),
  };
}

export function mapCategoryLocalizationResponseToFormValues(
  localization: AdminCategoryLocalizationResponse,
): CategoryLocalizationFormValues {
  return {
    languageCode: localization.languageCode.toLowerCase(),
    name: localization.name,
    description: localization.description,
    imageMediaId: localization.imageMediaId,
    status: localization.status,
    publishedAt: toDateTimeLocalValue(localization.publishedAt),
  };
}
