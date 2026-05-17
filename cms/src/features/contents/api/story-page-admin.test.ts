import {
  adminStoryPageReadResponseSchema,
  adminStoryPageResponseSchema,
} from "@/features/contents/api/story-page-admin";

describe("story-page admin schemas", () => {
  it("normalizes missing textless illustration fields to null", () => {
    expect(
      adminStoryPageResponseSchema.parse({
        contentId: 51,
        pageNumber: 2,
        localizationCount: 0,
      }),
    ).toEqual({
      contentId: 51,
      pageNumber: 2,
      textlessIllustrationMediaId: null,
      localizationCount: 0,
    });
  });

  it("accepts read responses when the backend omits null source image fields", () => {
    expect(
      adminStoryPageReadResponseSchema.parse({
        contentId: 51,
        pageNumber: 2,
        localizationCount: 1,
        localizations: [
          {
            contentId: 51,
            pageNumber: 2,
            languageCode: "en",
            bodyText: null,
            audioMediaId: null,
            illustrationMediaId: 77,
          },
        ],
      }),
    ).toEqual({
      contentId: 51,
      pageNumber: 2,
      textlessIllustrationMediaId: null,
      localizationCount: 1,
      localizations: [
        {
          contentId: 51,
          pageNumber: 2,
          languageCode: "en",
          bodyText: null,
          audioMediaId: null,
          illustrationMediaId: 77,
        },
      ],
    });
  });
});
