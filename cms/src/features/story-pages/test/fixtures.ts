import type { AdminStoryPageReadResponse } from "@/features/contents/api/story-page-admin";
import {
  mapAdminStoryPageRead,
  type StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";

export const storyPageReadResponses: AdminStoryPageReadResponse[] = [
  {
    contentId: 1,
    pageNumber: 1,
    localizationCount: 2,
    localizations: [
      {
        contentId: 1,
        pageNumber: 1,
        languageCode: "en",
        bodyText: "Look at the moon over the garden gate.",
        audioMediaId: 81,
        illustrationMediaId: 41,
      },
      {
        contentId: 1,
        pageNumber: 1,
        languageCode: "tr",
        bodyText: "Bahce kapisinin ustundeki aya bak.",
        audioMediaId: 82,
        illustrationMediaId: 42,
      },
    ],
  },
  {
    contentId: 1,
    pageNumber: 2,
    localizationCount: 1,
    localizations: [
      {
        contentId: 1,
        pageNumber: 2,
        languageCode: "en",
        bodyText: "The fox curls into a soft bed of leaves.",
        audioMediaId: 83,
        illustrationMediaId: 43,
      },
    ],
  },
];

export const firstStoryPageReadResponse = storyPageReadResponses[0];

export const storyPageViewModels: StoryPageReadViewModel[] =
  storyPageReadResponses.map(mapAdminStoryPageRead);

export const firstStoryPageViewModel = storyPageViewModels[0];
