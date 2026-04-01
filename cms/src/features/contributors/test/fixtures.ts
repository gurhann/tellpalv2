import type {
  AdminContentContributorResponse,
  AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContentContributor,
  mapAdminContributorList,
  type ContentContributorViewModel,
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

export const contentContributorResponses: AdminContentContributorResponse[] = [
  {
    contentId: 1,
    contributorId: 11,
    contributorDisplayName: "Annie Case",
    role: "AUTHOR",
    languageCode: "en",
    creditName: null,
    sortOrder: 0,
  },
  {
    contentId: 1,
    contributorId: 12,
    contributorDisplayName: "Milo Rivers",
    role: "NARRATOR",
    languageCode: "tr",
    creditName: "M. Rivers",
    sortOrder: 1,
  },
];

export const contentContributorViewModels: ContentContributorViewModel[] =
  contentContributorResponses.map(mapAdminContentContributor);

export const globalContentContributorResponse: AdminContentContributorResponse =
  {
    contentId: 1,
    contributorId: 13,
    contributorDisplayName: "Sena Yildiz",
    role: "ILLUSTRATOR",
    languageCode: null,
    creditName: null,
    sortOrder: 0,
  };

export const globalContentContributorViewModel: ContentContributorViewModel =
  mapAdminContentContributor(globalContentContributorResponse);
