import type {
  AdminContentLocalizationResponse,
  AdminContentReadResponse,
  AdminContentResponse,
  ContentLocalizationStatus,
  ContentProcessingStatus,
  ContentType,
} from "@/features/contents/api/content-admin";
import type {
  AdminStoryPageLocalizationResponse,
  AdminStoryPageReadResponse,
  AdminStoryPageResponse,
} from "@/features/contents/api/story-page-admin";
import { mapLanguage } from "@/lib/languages";

const contentTypeLabels: Record<ContentType, string> = {
  STORY: "Story",
  AUDIO_STORY: "Audio Story",
  MEDITATION: "Meditation",
  LULLABY: "Lullaby",
};

const localizationStatusLabels: Record<ContentLocalizationStatus, string> = {
  DRAFT: "Draft",
  PUBLISHED: "Published",
  ARCHIVED: "Archived",
};

const processingStatusLabels: Record<ContentProcessingStatus, string> = {
  PENDING: "Pending",
  PROCESSING: "Processing",
  COMPLETED: "Completed",
  FAILED: "Failed",
};

export type ContentSummaryViewModel = {
  id: number;
  type: ContentType;
  typeLabel: string;
  externalKey: string;
  active: boolean;
  ageRange: number | null;
  pageCount: number | null;
  supportsStoryPages: boolean;
  hasPages: boolean;
};

export type ContentLocalizationViewModel = {
  contentId: number;
  languageCode: string;
  languageLabel: string;
  title: string;
  description: string | null;
  bodyText: string | null;
  coverAssetId: number | null;
  audioAssetId: number | null;
  durationMinutes: number | null;
  status: ContentLocalizationStatus;
  statusLabel: string;
  processingStatus: ContentProcessingStatus;
  processingStatusLabel: string;
  publishedAt: string | null;
  visibleToMobile: boolean;
  hasCoverAsset: boolean;
  hasAudioAsset: boolean;
  isPublished: boolean;
  isArchived: boolean;
  isProcessingComplete: boolean;
};

export type ContentReadViewModel = {
  summary: ContentSummaryViewModel;
  localizations: ContentLocalizationViewModel[];
  primaryLocalization: ContentLocalizationViewModel | null;
  localizationCount: number;
  publishedLocalizationCount: number;
  processingCompleteLocalizationCount: number;
  visibleToMobileLocalizationCount: number;
};

export type StoryPageViewModel = {
  contentId: number;
  pageNumber: number;
  illustrationAssetId: number | null;
  localizationCount: number;
  hasIllustration: boolean;
  hasLocalizations: boolean;
};

export type StoryPageLocalizationViewModel = {
  contentId: number;
  pageNumber: number;
  languageCode: string;
  languageLabel: string;
  bodyText: string | null;
  audioAssetId: number | null;
  hasBodyText: boolean;
  hasAudioAsset: boolean;
};

export type StoryPageReadViewModel = StoryPageViewModel & {
  localizations: StoryPageLocalizationViewModel[];
  primaryLocalization: StoryPageLocalizationViewModel | null;
  languageCodes: string[];
};

export function createContentReadViewModel(
  summary: ContentSummaryViewModel,
  localizations: ContentLocalizationViewModel[] = [],
): ContentReadViewModel {
  return {
    summary,
    localizations,
    primaryLocalization: localizations[0] ?? null,
    localizationCount: localizations.length,
    publishedLocalizationCount: localizations.filter(
      (localization) => localization.isPublished,
    ).length,
    processingCompleteLocalizationCount: localizations.filter(
      (localization) => localization.isProcessingComplete,
    ).length,
    visibleToMobileLocalizationCount: localizations.filter(
      (localization) => localization.visibleToMobile,
    ).length,
  };
}

export function mapAdminContent(
  content: AdminContentResponse,
): ContentSummaryViewModel {
  return {
    id: content.contentId,
    type: content.type,
    typeLabel: contentTypeLabels[content.type],
    externalKey: content.externalKey,
    active: content.active,
    ageRange: content.ageRange,
    pageCount: content.pageCount,
    supportsStoryPages: content.type === "STORY",
    hasPages: (content.pageCount ?? 0) > 0,
  };
}

export function mapAdminContentList(
  contents: AdminContentResponse[],
): ContentSummaryViewModel[] {
  return contents.map(mapAdminContent);
}

export function mapAdminContentRead(
  content: AdminContentReadResponse,
): ContentReadViewModel {
  const localizations = content.localizations.map(mapAdminContentLocalization);

  return createContentReadViewModel(mapAdminContent(content), localizations);
}

export function mapAdminContentReadList(
  contents: AdminContentReadResponse[],
): ContentReadViewModel[] {
  return contents.map(mapAdminContentRead);
}

export function mapAdminContentSummaryToRead(
  content: AdminContentResponse,
  localizations: ContentLocalizationViewModel[] = [],
): ContentReadViewModel {
  return createContentReadViewModel(mapAdminContent(content), localizations);
}

export function mapAdminContentLocalization(
  localization: AdminContentLocalizationResponse,
): ContentLocalizationViewModel {
  const language = mapLanguage(localization.languageCode);

  return {
    contentId: localization.contentId,
    languageCode: language.code,
    languageLabel: language.label,
    title: localization.title,
    description: localization.description,
    bodyText: localization.bodyText,
    coverAssetId: localization.coverMediaId,
    audioAssetId: localization.audioMediaId,
    durationMinutes: localization.durationMinutes,
    status: localization.status,
    statusLabel: localizationStatusLabels[localization.status],
    processingStatus: localization.processingStatus,
    processingStatusLabel:
      processingStatusLabels[localization.processingStatus],
    publishedAt: localization.publishedAt,
    visibleToMobile: localization.visibleToMobile,
    hasCoverAsset: localization.coverMediaId !== null,
    hasAudioAsset: localization.audioMediaId !== null,
    isPublished: localization.status === "PUBLISHED",
    isArchived: localization.status === "ARCHIVED",
    isProcessingComplete: localization.processingStatus === "COMPLETED",
  };
}

export function mapAdminStoryPage(
  storyPage: AdminStoryPageResponse,
): StoryPageViewModel {
  return {
    contentId: storyPage.contentId,
    pageNumber: storyPage.pageNumber,
    illustrationAssetId: storyPage.illustrationMediaId,
    localizationCount: storyPage.localizationCount,
    hasIllustration: storyPage.illustrationMediaId !== null,
    hasLocalizations: storyPage.localizationCount > 0,
  };
}

export function mapAdminStoryPageList(
  storyPages: AdminStoryPageResponse[],
): StoryPageViewModel[] {
  return storyPages.map(mapAdminStoryPage);
}

export function createStoryPageReadViewModel(
  storyPage: StoryPageViewModel,
  localizations: StoryPageLocalizationViewModel[] = [],
): StoryPageReadViewModel {
  return {
    ...storyPage,
    localizations,
    primaryLocalization: localizations[0] ?? null,
    languageCodes: localizations.map(
      (localization) => localization.languageCode,
    ),
  };
}

export function mapAdminStoryPageRead(
  storyPage: AdminStoryPageReadResponse,
): StoryPageReadViewModel {
  return createStoryPageReadViewModel(
    mapAdminStoryPage(storyPage),
    storyPage.localizations.map(mapAdminStoryPageLocalization),
  );
}

export function mapAdminStoryPageReadList(
  storyPages: AdminStoryPageReadResponse[],
): StoryPageReadViewModel[] {
  return storyPages.map(mapAdminStoryPageRead);
}

export function mapAdminStoryPageLocalization(
  localization: AdminStoryPageLocalizationResponse,
): StoryPageLocalizationViewModel {
  const language = mapLanguage(localization.languageCode);

  return {
    contentId: localization.contentId,
    pageNumber: localization.pageNumber,
    languageCode: language.code,
    languageLabel: language.label,
    bodyText: localization.bodyText,
    audioAssetId: localization.audioMediaId,
    hasBodyText: Boolean(localization.bodyText?.trim()),
    hasAudioAsset: localization.audioMediaId !== null,
  };
}
