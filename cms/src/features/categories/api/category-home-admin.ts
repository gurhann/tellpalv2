import { z } from "zod";
import { apiClient } from "@/lib/http/client";

const rowSchema = z.object({
  categoryId: z.number().int().positive(),
  slug: z.string(),
  name: z.string(),
  description: z.string().nullable(),
  type: z.literal("STORY"),
  active: z.boolean(),
  status: z.string(),
  displayOrder: z.number().int().nonnegative().nullable(),
  audience: z.enum(["ALL", "NON_SUBSCRIBER"]),
  contentLanguage: z.string().nullable(),
  version: z.number().int(),
});
const snapshotSchema = z.object({
  language: z.string(),
  revision: z.string(),
  rows: z.array(rowSchema),
});
export type HomeRow = z.infer<typeof rowSchema>;
export type HomeSnapshot = z.infer<typeof snapshotSchema>;
export type HomeConfiguration = Pick<
  HomeRow,
  "categoryId" | "displayOrder" | "audience" | "contentLanguage"
>;
export const categoryHomeAdminApi = {
  read(language: string) {
    return apiClient.get<HomeSnapshot>(
      `/api/admin/categories/home?lang=${language}`,
      { responseSchema: snapshotSchema },
    );
  },
  save(language: string, expectedRevision: string, rows: HomeConfiguration[]) {
    return apiClient.put<HomeSnapshot>(
      `/api/admin/categories/home?lang=${language}`,
      { body: { expectedRevision, rows }, responseSchema: snapshotSchema },
    );
  },
};
