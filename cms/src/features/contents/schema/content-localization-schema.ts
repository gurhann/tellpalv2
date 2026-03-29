import type {
  ContentLocalizationStatus,
  ContentProcessingStatus,
  ContentType,
} from "@/features/contents/api/content-admin";
import type { ContentLocalizationViewModel } from "@/features/contents/model/content-view-model";
import { normalizeLanguageCode } from "@/lib/languages";
import { z } from "zod";

function trimToNull(value: unknown) {
  if (typeof value !== "string") {
    return value;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function parseNullableInteger(value: unknown) {
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

function parsePublishedAt(value: unknown) {
  if (value === "" || value === undefined || value === null) {
    return null;
  }

  if (typeof value !== "string") {
    return value;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function formatDateTimeLocalValue(value: string | null) {
  if (!value) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const offsetMinutes = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - offsetMinutes * 60_000);

  return localDate.toISOString().slice(0, 16);
}

export const localizationStatusOptions: Array<{
  value: ContentLocalizationStatus;
  label: string;
}> = [
  { value: "DRAFT", label: "Draft" },
  { value: "PUBLISHED", label: "Published" },
  { value: "ARCHIVED", label: "Archived" },
];

export const processingStatusOptions: Array<{
  value: ContentProcessingStatus;
  label: string;
}> = [
  { value: "PENDING", label: "Pending" },
  { value: "PROCESSING", label: "Processing" },
  { value: "COMPLETED", label: "Completed" },
  { value: "FAILED", label: "Failed" },
];

export type ContentLocalizationFormValues = {
  languageCode: string;
  title: string;
  description: string | null;
  bodyText: string | null;
  coverMediaId: number | null;
  audioMediaId: number | null;
  durationMinutes: number | null;
  status: ContentLocalizationStatus;
  processingStatus: ContentProcessingStatus;
  publishedAt: string | null;
};

export function createContentLocalizationSchema(contentType: ContentType) {
  return z
    .object({
      languageCode: z
        .string()
        .trim()
        .min(1, "Language is required.")
        .transform(normalizeLanguageCode),
      title: z.string().trim().min(1, "Title is required."),
      description: z.preprocess(trimToNull, z.string().nullable()),
      bodyText: z.preprocess(trimToNull, z.string().nullable()),
      coverMediaId: z.preprocess(
        parseNullableInteger,
        z
          .number({
            error: "Cover asset id must be a valid number.",
          })
          .int("Cover asset id must be a whole number.")
          .positive("Cover asset id must be positive.")
          .nullable(),
      ),
      audioMediaId: z.preprocess(
        parseNullableInteger,
        z
          .number({
            error: "Audio asset id must be a valid number.",
          })
          .int("Audio asset id must be a whole number.")
          .positive("Audio asset id must be positive.")
          .nullable(),
      ),
      durationMinutes: z.preprocess(
        parseNullableInteger,
        z
          .number({
            error: "Duration must be a valid number.",
          })
          .int("Duration must be a whole number.")
          .nonnegative("Duration must not be negative.")
          .nullable(),
      ),
      status: z.enum(["DRAFT", "PUBLISHED", "ARCHIVED"]),
      processingStatus: z.enum([
        "PENDING",
        "PROCESSING",
        "COMPLETED",
        "FAILED",
      ]),
      publishedAt: z.preprocess(parsePublishedAt, z.string().nullable()),
    })
    .superRefine((values, ctx) => {
      if (values.publishedAt) {
        const publishedAt = new Date(values.publishedAt);

        if (Number.isNaN(publishedAt.getTime())) {
          ctx.addIssue({
            code: "custom",
            message: "Published at must be a valid timestamp.",
            path: ["publishedAt"],
          });
        }
      }

      if (values.status === "PUBLISHED" && !values.publishedAt) {
        ctx.addIssue({
          code: "custom",
          message: "Published at is required when status is Published.",
          path: ["publishedAt"],
        });
      }

      if (contentType === "STORY") {
        if (values.bodyText) {
          ctx.addIssue({
            code: "custom",
            message: "Story localizations store narrative text on story pages.",
            path: ["bodyText"],
          });
        }

        if (values.audioMediaId !== null) {
          ctx.addIssue({
            code: "custom",
            message:
              "Story localizations do not accept a single content-level audio asset.",
            path: ["audioMediaId"],
          });
        }
      } else {
        if (
          (contentType === "AUDIO_STORY" || contentType === "MEDITATION") &&
          !values.bodyText
        ) {
          ctx.addIssue({
            code: "custom",
            message: "Body text is required for this content type.",
            path: ["bodyText"],
          });
        }

        if (values.audioMediaId === null) {
          ctx.addIssue({
            code: "custom",
            message: "Audio asset id is required for this content type.",
            path: ["audioMediaId"],
          });
        }
      }
    });
}

export function getCreateLocalizationFormDefaults(
  languageCode: string,
): ContentLocalizationFormValues {
  return {
    languageCode: normalizeLanguageCode(languageCode),
    title: "",
    description: null,
    bodyText: null,
    coverMediaId: null,
    audioMediaId: null,
    durationMinutes: null,
    status: "DRAFT",
    processingStatus: "PENDING",
    publishedAt: null,
  };
}

export function mapLocalizationToFormValues(
  localization: ContentLocalizationViewModel,
): ContentLocalizationFormValues {
  return {
    languageCode: normalizeLanguageCode(localization.languageCode),
    title: localization.title,
    description: localization.description,
    bodyText: localization.bodyText,
    coverMediaId: localization.coverAssetId,
    audioMediaId: localization.audioAssetId,
    durationMinutes: localization.durationMinutes,
    status: localization.status,
    processingStatus: localization.processingStatus,
    publishedAt: formatDateTimeLocalValue(localization.publishedAt),
  };
}

export function toPublishedAtPayload(value: string | null) {
  if (!value) {
    return null;
  }

  const publishedAt = new Date(value);

  if (Number.isNaN(publishedAt.getTime())) {
    return value;
  }

  return publishedAt.toISOString();
}
