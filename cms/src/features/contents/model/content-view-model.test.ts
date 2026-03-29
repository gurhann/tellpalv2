import type {
  AdminContentLocalizationResponse,
  AdminContentReadResponse,
  AdminContentResponse,
} from "@/features/contents/api/content-admin";
import type {
  AdminStoryPageLocalizationResponse,
  AdminStoryPageResponse,
} from "@/features/contents/api/story-page-admin";
import {
  createContentReadViewModel,
  mapAdminContent,
  mapAdminContentRead,
  mapAdminContentLocalization,
  mapAdminContentSummaryToRead,
  mapAdminStoryPage,
  mapAdminStoryPageLocalization,
} from "@/features/contents/model/content-view-model";

describe("content view model mappers", () => {
  it("maps content summary fields and derived flags", () => {
    const dto: AdminContentResponse = {
      contentId: 14,
      type: "STORY",
      externalKey: "sleep-story",
      active: true,
      ageRange: 4,
      pageCount: 8,
    };

    expect(mapAdminContent(dto)).toEqual({
      id: 14,
      type: "STORY",
      typeLabel: "Story",
      externalKey: "sleep-story",
      active: true,
      ageRange: 4,
      pageCount: 8,
      supportsStoryPages: true,
      hasPages: true,
    });
  });

  it("maps content localization into a UI-friendly shape", () => {
    const dto: AdminContentLocalizationResponse = {
      contentId: 14,
      languageCode: "TR",
      title: "Uyku Hikayesi",
      description: "A calming bedtime story.",
      bodyText: null,
      coverMediaId: 21,
      audioMediaId: null,
      durationMinutes: 12,
      status: "PUBLISHED",
      processingStatus: "COMPLETED",
      publishedAt: "2026-03-29T10:00:00Z",
      visibleToMobile: true,
    };

    expect(mapAdminContentLocalization(dto)).toEqual({
      contentId: 14,
      languageCode: "tr",
      languageLabel: "Turkish",
      title: "Uyku Hikayesi",
      description: "A calming bedtime story.",
      bodyText: null,
      coverAssetId: 21,
      audioAssetId: null,
      durationMinutes: 12,
      status: "PUBLISHED",
      statusLabel: "Published",
      processingStatus: "COMPLETED",
      processingStatusLabel: "Completed",
      publishedAt: "2026-03-29T10:00:00Z",
      visibleToMobile: true,
      hasCoverAsset: true,
      hasAudioAsset: false,
      isPublished: true,
      isArchived: false,
      isProcessingComplete: true,
    });
  });

  it("maps content read responses with localization summaries", () => {
    const dto: AdminContentReadResponse = {
      contentId: 14,
      type: "STORY",
      externalKey: "sleep-story",
      active: true,
      ageRange: 4,
      pageCount: 8,
      localizations: [
        {
          contentId: 14,
          languageCode: "en",
          title: "Sleep Story",
          description: "Bedtime story",
          bodyText: null,
          coverMediaId: null,
          audioMediaId: null,
          durationMinutes: 12,
          status: "PUBLISHED",
          processingStatus: "COMPLETED",
          publishedAt: "2026-03-29T10:00:00Z",
          visibleToMobile: true,
        },
        {
          contentId: 14,
          languageCode: "tr",
          title: "Uyku Hikayesi",
          description: "Yatma zamani hikayesi",
          bodyText: null,
          coverMediaId: null,
          audioMediaId: null,
          durationMinutes: 12,
          status: "DRAFT",
          processingStatus: "PROCESSING",
          publishedAt: null,
          visibleToMobile: false,
        },
      ],
    };

    expect(mapAdminContentRead(dto)).toEqual({
      summary: {
        id: 14,
        type: "STORY",
        typeLabel: "Story",
        externalKey: "sleep-story",
        active: true,
        ageRange: 4,
        pageCount: 8,
        supportsStoryPages: true,
        hasPages: true,
      },
      localizations: [
        {
          contentId: 14,
          languageCode: "en",
          languageLabel: "English",
          title: "Sleep Story",
          description: "Bedtime story",
          bodyText: null,
          coverAssetId: null,
          audioAssetId: null,
          durationMinutes: 12,
          status: "PUBLISHED",
          statusLabel: "Published",
          processingStatus: "COMPLETED",
          processingStatusLabel: "Completed",
          publishedAt: "2026-03-29T10:00:00Z",
          visibleToMobile: true,
          hasCoverAsset: false,
          hasAudioAsset: false,
          isPublished: true,
          isArchived: false,
          isProcessingComplete: true,
        },
        {
          contentId: 14,
          languageCode: "tr",
          languageLabel: "Turkish",
          title: "Uyku Hikayesi",
          description: "Yatma zamani hikayesi",
          bodyText: null,
          coverAssetId: null,
          audioAssetId: null,
          durationMinutes: 12,
          status: "DRAFT",
          statusLabel: "Draft",
          processingStatus: "PROCESSING",
          processingStatusLabel: "Processing",
          publishedAt: null,
          visibleToMobile: false,
          hasCoverAsset: false,
          hasAudioAsset: false,
          isPublished: false,
          isArchived: false,
          isProcessingComplete: false,
        },
      ],
      primaryLocalization: {
        contentId: 14,
        languageCode: "en",
        languageLabel: "English",
        title: "Sleep Story",
        description: "Bedtime story",
        bodyText: null,
        coverAssetId: null,
        audioAssetId: null,
        durationMinutes: 12,
        status: "PUBLISHED",
        statusLabel: "Published",
        processingStatus: "COMPLETED",
        processingStatusLabel: "Completed",
        publishedAt: "2026-03-29T10:00:00Z",
        visibleToMobile: true,
        hasCoverAsset: false,
        hasAudioAsset: false,
        isPublished: true,
        isArchived: false,
        isProcessingComplete: true,
      },
      localizationCount: 2,
      publishedLocalizationCount: 1,
      processingCompleteLocalizationCount: 1,
      visibleToMobileLocalizationCount: 1,
    });
  });

  it("can hydrate a read view model from summary metadata and existing localizations", () => {
    const summaryDto: AdminContentResponse = {
      contentId: 21,
      type: "MEDITATION",
      externalKey: "meditation.rain-room",
      active: false,
      ageRange: 8,
      pageCount: null,
    };

    const localizations = [
      mapAdminContentLocalization({
        contentId: 21,
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
      }),
    ];

    expect(mapAdminContentSummaryToRead(summaryDto, localizations)).toEqual(
      createContentReadViewModel(
        {
          id: 21,
          type: "MEDITATION",
          typeLabel: "Meditation",
          externalKey: "meditation.rain-room",
          active: false,
          ageRange: 8,
          pageCount: null,
          supportsStoryPages: false,
          hasPages: false,
        },
        localizations,
      ),
    );
  });

  it("maps story pages and localized page data", () => {
    const pageDto: AdminStoryPageResponse = {
      contentId: 14,
      pageNumber: 3,
      illustrationMediaId: 55,
      localizationCount: 2,
    };

    const localizationDto: AdminStoryPageLocalizationResponse = {
      contentId: 14,
      pageNumber: 3,
      languageCode: "en",
      bodyText: "Drift into the quiet forest.",
      audioMediaId: 89,
    };

    expect(mapAdminStoryPage(pageDto)).toEqual({
      contentId: 14,
      pageNumber: 3,
      illustrationAssetId: 55,
      localizationCount: 2,
      hasIllustration: true,
      hasLocalizations: true,
    });

    expect(mapAdminStoryPageLocalization(localizationDto)).toEqual({
      contentId: 14,
      pageNumber: 3,
      languageCode: "en",
      languageLabel: "English",
      bodyText: "Drift into the quiet forest.",
      audioAssetId: 89,
      hasBodyText: true,
      hasAudioAsset: true,
    });
  });
});
