import type { AdminContentReadResponse } from "@/features/contents/api/content-admin";
import {
  mapAdminContentRead,
  type ContentReadViewModel,
} from "@/features/contents/model/content-view-model";

export const storyContentReadResponse: AdminContentReadResponse = {
  contentId: 1,
  type: "STORY",
  externalKey: "story.evening-garden",
  active: true,
  ageRange: 5,
  pageCount: 2,
  localizations: [
    {
      contentId: 1,
      languageCode: "en",
      title: "Evening Garden",
      description: "A calm walk through a moonlit garden.",
      bodyText: null,
      coverMediaId: null,
      audioMediaId: null,
      durationMinutes: 8,
      status: "PUBLISHED",
      processingStatus: "COMPLETED",
      publishedAt: "2026-03-17T09:00:00Z",
      visibleToMobile: true,
    },
    {
      contentId: 1,
      languageCode: "tr",
      title: "Aksam Bahcesi",
      description: "Aksam icin sakin bir uyku hikayesi.",
      bodyText: null,
      coverMediaId: null,
      audioMediaId: null,
      durationMinutes: 8,
      status: "DRAFT",
      processingStatus: "PROCESSING",
      publishedAt: null,
      visibleToMobile: false,
    },
  ],
};

export const meditationContentReadResponse: AdminContentReadResponse = {
  contentId: 2,
  type: "MEDITATION",
  externalKey: "meditation.rain-room",
  active: true,
  ageRange: 8,
  pageCount: null,
  localizations: [
    {
      contentId: 2,
      languageCode: "de",
      title: "Regenraum Pause",
      description: "Kurze Atemubung mit Regenatmosphare.",
      bodyText: "Atme vier Takte lang ein und entspanne die Schultern.",
      coverMediaId: null,
      audioMediaId: 2,
      durationMinutes: 6,
      status: "DRAFT",
      processingStatus: "PROCESSING",
      publishedAt: null,
      visibleToMobile: false,
    },
    {
      contentId: 2,
      languageCode: "en",
      title: "Rain Room Reset",
      description: "A short breathing reset with rain ambience.",
      bodyText: "Breathe in for four counts and relax your shoulders.",
      coverMediaId: null,
      audioMediaId: 1,
      durationMinutes: 6,
      status: "DRAFT",
      processingStatus: "PENDING",
      publishedAt: null,
      visibleToMobile: false,
    },
  ],
};

export const inactiveContentReadResponse: AdminContentReadResponse = {
  contentId: 4,
  type: "LULLABY",
  externalKey: "lullaby.moon-softly",
  active: false,
  ageRange: 3,
  pageCount: null,
  localizations: [],
};

export const contentReadResponses: AdminContentReadResponse[] = [
  storyContentReadResponse,
  meditationContentReadResponse,
  inactiveContentReadResponse,
];

export const storyContentViewModel = mapAdminContentRead(
  storyContentReadResponse,
);
export const meditationContentViewModel = mapAdminContentRead(
  meditationContentReadResponse,
);
export const inactiveContentViewModel = mapAdminContentRead(
  inactiveContentReadResponse,
);
export const contentReadViewModels: ContentReadViewModel[] = [
  storyContentViewModel,
  meditationContentViewModel,
  inactiveContentViewModel,
];
