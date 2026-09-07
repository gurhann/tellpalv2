import { beforeEach, describe, expect, it, vi } from "vitest";

const apiClientMock = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
}));

vi.mock("@/lib/http/client", () => ({ apiClient: apiClientMock }));

import { contentAdminApi } from "./content-admin";

describe("content admin playback API", () => {
  beforeEach(() => {
    apiClientMock.get.mockReset();
    apiClientMock.put.mockReset();
  });

  it("builds the shared playback update request", async () => {
    apiClientMock.put.mockResolvedValue({});

    await contentAdminApi.updateLullabyPlayback(7, {
      audioMediaId: 90,
      durationMinutes: 12,
    });

    expect(apiClientMock.put).toHaveBeenCalledWith(
      "/api/admin/contents/7/playback",
      expect.objectContaining({
        body: { audioMediaId: 90, durationMinutes: 12 },
        responseSchema: expect.anything(),
      }),
    );
  });

  it("keeps locale labels in catalog and ordered instrument requests", async () => {
    apiClientMock.get.mockResolvedValue([]);
    apiClientMock.put.mockResolvedValue([]);

    await contentAdminApi.listInstrumentCatalog("tr");
    await contentAdminApi.listLullabyInstruments(7, "tr");
    await contentAdminApi.replaceLullabyInstruments(7, ["CELESTA", "BELL"], "tr");

    expect(apiClientMock.get).toHaveBeenNthCalledWith(
      1,
      "/api/admin/instrument-catalog?languageCode=tr",
      expect.objectContaining({ responseSchema: expect.anything() }),
    );
    expect(apiClientMock.get).toHaveBeenNthCalledWith(
      2,
      "/api/admin/contents/7/instruments?languageCode=tr",
      expect.objectContaining({ responseSchema: expect.anything() }),
    );
    expect(apiClientMock.put).toHaveBeenNthCalledWith(
      1,
      "/api/admin/contents/7/instruments?languageCode=tr",
      expect.objectContaining({
        body: { instrumentCodes: ["CELESTA", "BELL"] },
      }),
    );
  });
});
