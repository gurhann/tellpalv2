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
      }, "STORY"),
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
      }, "STORY"),
    ).not.toHaveProperty("narration");
  });

  it("omits non-localized fields from lullaby payloads", () => {
    expect(
      toLocalizationPayload({
        languageCode: "tr", title: "Dandini", description: "ignored", bodyText: "ignored",
        coverMediaId: 3, audioMediaId: 4, durationMinutes: 5, status: "DRAFT",
        processingStatus: "PENDING", publishedAt: null,
      }, "LULLABY"),
    ).toEqual({ title: "Dandini", status: "DRAFT", publishedAt: null });
  });
});
