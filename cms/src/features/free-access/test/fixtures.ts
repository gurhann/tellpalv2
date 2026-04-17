import type { AdminContentFreeAccessResponse } from "@/features/free-access/api/free-access-admin";
import {
  mapAdminFreeAccessGrant,
  type FreeAccessGrantViewModel,
} from "@/features/free-access/model/free-access-view-model";

export const defaultFreeAccessResponse: AdminContentFreeAccessResponse = {
  freeAccessId: 501,
  accessKey: "default",
  contentId: 1,
  languageCode: "en",
};

export const partnerFreeAccessResponse: AdminContentFreeAccessResponse = {
  freeAccessId: 502,
  accessKey: "partner-spring",
  contentId: 2,
  languageCode: "de",
};

export const freeAccessResponses: AdminContentFreeAccessResponse[] = [
  defaultFreeAccessResponse,
  partnerFreeAccessResponse,
];

export const defaultFreeAccessViewModel = mapAdminFreeAccessGrant(
  defaultFreeAccessResponse,
);
export const partnerFreeAccessViewModel = mapAdminFreeAccessGrant(
  partnerFreeAccessResponse,
);

export const freeAccessViewModels: FreeAccessGrantViewModel[] = [
  defaultFreeAccessViewModel,
  partnerFreeAccessViewModel,
];
