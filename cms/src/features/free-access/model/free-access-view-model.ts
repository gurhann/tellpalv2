import type { AdminContentFreeAccessResponse } from "@/features/free-access/api/free-access-admin";
import { mapLanguage } from "@/lib/languages";

export type FreeAccessGrantViewModel = {
  id: number;
  accessKey: string;
  contentId: number;
  languageCode: string;
  languageLabel: string;
};

export function mapAdminFreeAccessGrant(
  entry: AdminContentFreeAccessResponse,
): FreeAccessGrantViewModel {
  const language = mapLanguage(entry.languageCode);

  return {
    id: entry.freeAccessId,
    accessKey: entry.accessKey,
    contentId: entry.contentId,
    languageCode: language.code,
    languageLabel: language.label,
  };
}

export function mapAdminFreeAccessGrantList(
  entries: AdminContentFreeAccessResponse[],
): FreeAccessGrantViewModel[] {
  return entries.map(mapAdminFreeAccessGrant);
}
