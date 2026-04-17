import { z } from "zod";

export const scheduleProcessingFormSchema = z.object({
  selectedContentId: z
    .number({
      error: "Select a content record.",
    })
    .int("Select a valid content record.")
    .positive("Select a valid content record.")
    .nullable(),
  languageCode: z.string().trim().min(1, "Select a language."),
  coverSourceAssetId: z.number().int().positive().nullable(),
  audioSourceAssetId: z.number().int().positive().nullable(),
});

export type ScheduleProcessingFormValues = z.infer<
  typeof scheduleProcessingFormSchema
>;

export function getScheduleProcessingFormDefaults(
  selectedContentId?: number | null,
  languageCode?: string | null,
): ScheduleProcessingFormValues {
  return {
    selectedContentId: selectedContentId ?? null,
    languageCode: languageCode ?? "",
    coverSourceAssetId: null,
    audioSourceAssetId: null,
  };
}

export const retryProcessingFormSchema = z.object({
  coverSourceAssetId: z.number().int().positive().nullable(),
  audioSourceAssetId: z.number().int().positive().nullable(),
});

export type RetryProcessingFormValues = z.infer<
  typeof retryProcessingFormSchema
>;

export function getRetryProcessingFormDefaults(
  coverSourceAssetId?: number | null,
  audioSourceAssetId?: number | null,
): RetryProcessingFormValues {
  return {
    coverSourceAssetId: coverSourceAssetId ?? null,
    audioSourceAssetId: audioSourceAssetId ?? null,
  };
}
