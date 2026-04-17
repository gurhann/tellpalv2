import { z } from "zod";

export const grantFreeAccessFormSchema = z.object({
  accessKey: z.string().trim().min(1, "Enter an access key."),
  contentId: z
    .number({
      error: "Select a content record.",
    })
    .int("Select a valid content record.")
    .positive("Select a valid content record.")
    .nullable(),
  languageCode: z.string().trim().min(1, "Select a language."),
});

export type GrantFreeAccessFormValues = z.infer<
  typeof grantFreeAccessFormSchema
>;

export function getGrantFreeAccessFormDefaults(): GrantFreeAccessFormValues {
  return {
    accessKey: "default",
    contentId: null,
    languageCode: "",
  };
}
