import type {
  AdminContentResponse,
  ContentType,
} from "@/features/contents/api/content-admin";
import { contentTypeSchema } from "@/features/contents/api/content-admin";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { z } from "zod";

function parseAgeRange(value: unknown) {
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

export const contentTypeOptions: Array<{ value: ContentType; label: string }> =
  [
    { value: "STORY", label: "Story" },
    { value: "AUDIO_STORY", label: "Audio Story" },
    { value: "MEDITATION", label: "Meditation" },
    { value: "LULLABY", label: "Lullaby" },
  ];

export const contentFormSchema = z.object({
  type: contentTypeSchema,
  externalKey: z.string().trim().min(1, "External key is required."),
  ageRange: z.preprocess(
    parseAgeRange,
    z
      .number({
        error: "Age range must be a valid number.",
      })
      .int("Age range must be a whole number.")
      .nonnegative("Age range must not be negative.")
      .nullable(),
  ),
  active: z.boolean(),
});

export type ContentFormValues = z.infer<typeof contentFormSchema>;

export function getCreateContentFormDefaults(): ContentFormValues {
  return {
    type: "STORY",
    externalKey: "",
    ageRange: null,
    active: true,
  };
}

export function mapContentReadToFormValues(
  content: ContentReadViewModel,
): ContentFormValues {
  return {
    type: content.summary.type,
    externalKey: content.summary.externalKey,
    ageRange: content.summary.ageRange,
    active: content.summary.active,
  };
}

export function mapContentResponseToFormValues(
  content: AdminContentResponse,
): ContentFormValues {
  return {
    type: content.type,
    externalKey: content.externalKey,
    ageRange: content.ageRange,
    active: content.active,
  };
}
