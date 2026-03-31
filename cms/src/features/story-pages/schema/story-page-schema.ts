import { normalizeLanguageCode } from "@/lib/languages";
import { z } from "zod";

import type { StoryPageLocalizationViewModel } from "@/features/contents/model/content-view-model";

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

export type StoryPageLocalizationFormValues = {
  languageCode: string;
  bodyText: string | null;
  audioMediaId: number | null;
  illustrationMediaId: number | null;
};

export const storyPageLocalizationSchema = z.object({
  languageCode: z
    .string()
    .trim()
    .min(1, "Language is required.")
    .transform(normalizeLanguageCode),
  bodyText: z.preprocess(trimToNull, z.string().nullable()),
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
  illustrationMediaId: z.preprocess(
    parseNullableInteger,
    z
      .number({
        error: "Illustration asset id must be a valid number.",
      })
      .int("Illustration asset id must be a whole number.")
      .positive("Illustration asset id must be positive."),
  ),
});

export function getStoryPageLocalizationFormDefaults(
  languageCode: string,
): StoryPageLocalizationFormValues {
  return {
    languageCode: normalizeLanguageCode(languageCode),
    bodyText: null,
    audioMediaId: null,
    illustrationMediaId: null,
  };
}

export function mapStoryPageLocalizationToFormValues(
  localization: StoryPageLocalizationViewModel,
): StoryPageLocalizationFormValues {
  return {
    languageCode: normalizeLanguageCode(localization.languageCode),
    bodyText: localization.bodyText,
    audioMediaId: localization.audioAssetId,
    illustrationMediaId: localization.illustrationAssetId,
  };
}
