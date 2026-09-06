import { describe, expect, it } from "vitest";

import { toLocalizationPayload } from "./use-content-localization-actions";

describe("toLocalizationPayload", () => {
  it("serializes a complete story narration as a nested object", () => {
    expect(
      toLocalizationPayload({
        languageCode: "en",
        title: "Evening Garden",
        description: null,
        bodyText: null,
        coverMediaId: null,
        audioMediaId: null,
        durationMinutes: 8,
        status: "PUBLISHED",
        processingStatus: "PENDING",
        publishedAt: "2026-03-17T09:00:00",
        narrationAudioMediaId: 901,
        narrationDurationMinutes: 8,
      }),
    ).toMatchObject({
      narration: { audioMediaId: 901, durationMinutes: 8 },
    });
  });

  it("omits an incomplete narration pair", () => {
    expect(
      toLocalizationPayload({
        languageCode: "en",
        title: "Evening Garden",
        description: null,
        bodyText: null,
        coverMediaId: null,
        audioMediaId: null,
        durationMinutes: 8,
        status: "DRAFT",
        processingStatus: "PENDING",
        publishedAt: null,
        narrationAudioMediaId: 901,
        narrationDurationMinutes: null,
      }),
    ).not.toHaveProperty("narration");
  });
});
