import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  ReadingPreviewImage,
  ReadingPreviewManifest,
} from "./content-admin";

const identity: ReadingPreviewManifest["identity"] = {
  contentId: 17,
  language: "tr",
  liveGeneration: 12,
  revision: "a".repeat(64),
  manifestDigest: "b".repeat(64),
  jobId: "12345678-1234-4234-8234-123456789abc",
};
const image: ReadingPreviewImage = {
  role: "READING_PAGE",
  pageNumber: 3,
  renditionKey: "key/with +encoded?",
  sourceMime: "image/png",
  sourceBytes: 8,
  sourceWidth: 1,
  sourceHeight: 1,
  mime: "image/webp",
  bytes: 4,
  width: 1,
  height: 1,
};
const fetchMock = vi.fn<typeof fetch>();
beforeEach(() => {
  vi.resetModules();
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});
async function api() {
  const { configureApiClientAuth } = await import("@/lib/http/client");
  configureApiClientAuth({
    getAccessToken: () => "initial-bearer",
    refreshAccessToken: async () => "refreshed-bearer",
  });
  return (await import("./content-admin")).contentAdminApi;
}
describe("reading preview binary URL contract", () => {
  it("sends the complete identity and image selection with bearer and the original AbortSignal after refresh", async () => {
    fetchMock
      .mockResolvedValueOnce(
        new Response("{}", {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response("webp", {
          headers: { "Content-Type": "image/webp" },
        }),
      );
    const service = await api();
    const controller = new AbortController();
    const result = await service.downloadReadingPreview(
      identity,
      image,
      controller.signal,
    );
    expect(result.blob.size).toBe(4);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    for (const [index, call] of fetchMock.mock.calls.entries()) {
      const url = new URL(String(call[0]));
      const options = call[1]!;
      expect(url.pathname).toBe(
        "/api/admin/contents/17/reading-publication/preview/bytes",
      );
      expect(Object.fromEntries(url.searchParams)).toEqual({
        languageCode: "tr",
        liveGeneration: "12",
        revision: identity.revision,
        manifestDigest: identity.manifestDigest,
        jobId: identity.jobId,
        role: "READING_PAGE",
        pageNumber: "3",
        renditionKey: image.renditionKey,
      });
      expect(options.method).toBe("GET");
      expect(options.signal).toBe(controller.signal);
      expect(new Headers(options.headers).get("Authorization")).toBe(
        `Bearer ${index === 0 ? "initial-bearer" : "refreshed-bearer"}`,
      );
    }
  });
  it("propagates cancellation to the real download transport", async () => {
    fetchMock.mockImplementation(
      (_url, options) =>
        new Promise((_resolve, reject) => {
          options?.signal?.addEventListener("abort", () =>
            reject(new DOMException("Aborted", "AbortError")),
          );
        }),
    );
    const service = await api();
    const controller = new AbortController();
    const request = service.downloadReadingPreview(
      identity,
      image,
      controller.signal,
    );
    expect(fetchMock.mock.calls[0][1]?.signal).toBe(controller.signal);
    controller.abort();
    await expect(request).rejects.toMatchObject({ name: "AbortError" });
  });
});
