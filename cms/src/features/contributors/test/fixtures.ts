import type { AdminContributorResponse } from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContributorList,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";

export const contributorResponses: AdminContributorResponse[] = [
  {
    contributorId: 11,
    displayName: "Annie Case",
  },
  {
    contributorId: 12,
    displayName: "Milo Rivers",
  },
  {
    contributorId: 13,
    displayName: "Sena Yildiz",
  },
];

export const contributorViewModels: ContributorViewModel[] =
  mapAdminContributorList(contributorResponses);
