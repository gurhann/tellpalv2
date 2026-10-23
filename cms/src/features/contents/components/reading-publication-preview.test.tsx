import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  contentAdminApi,
  type ReadingPreviewManifest,
} from "@/features/contents/api/content-admin";
import { ReadingPublicationPreview } from "./reading-publication-preview";

const identity = {
  contentId: 1,
  language: "tr",
  liveGeneration: 1,
  revision: "a".repeat(64),
  manifestDigest: "b".repeat(64),
  jobId: "12345678-1234-4234-8234-123456789abc",
};
const image = {
  role: "READING_COVER" as const,
  pageNumber: 0,
  renditionKey: "cover",
  sourceMime: "image/jpeg",
  sourceBytes: 4,
  sourceWidth: 80,
  sourceHeight: 60,
  mime: "image/jpeg" as const,
  bytes: 4,
  width: 80,
  height: 60,
};
const manifest: ReadingPreviewManifest = {
  identity,
  images: [
    image,
    { ...image, role: "READING_DETAIL", renditionKey: "detail" },
    { ...image, role: "READING_PAGE", pageNumber: 1, renditionKey: "page" },
  ],
};
const status = {
  enabled: true,
  contract: "INTERNAL_LOCAL_TEST" as const,
  state: "LIVE" as const,
  generation: 1,
  liveRevision: identity.revision,
  liveGeneration: 1,
  liveManifestDigest: identity.manifestDigest,
  diagnostic: null,
  retryable: false,
};
const props = { contentId: 1, language: "tr", status };
const create = vi.fn(() => "blob:prepared");
const revoke = vi.fn();
beforeEach(() => {
  vi.spyOn(contentAdminApi, "getReadingPreview").mockResolvedValue(manifest);
  vi.spyOn(contentAdminApi, "getReadingPublication").mockResolvedValue(status);
  vi.spyOn(contentAdminApi, "downloadReadingPreview").mockResolvedValue({
    blob: new Blob(["jpeg"], { type: "image/jpeg" }),
    fileName: null,
  });
  Object.defineProperty(URL, "createObjectURL", {
    configurable: true,
    value: create,
  });
  Object.defineProperty(URL, "revokeObjectURL", {
    configurable: true,
    value: revoke,
  });
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});
const open = () =>
  fireEvent.click(
    screen.getByRole("button", { name: "Preview prepared images" }),
  );
describe("prepared publication preview", () => {
  it("uses rendition bytes, describes equal JPEG honestly and revokes on close", async () => {
    render(<ReadingPublicationPreview {...props} />);
    open();
    await screen.findByRole("img", { name: "Prepared cover" });
    expect(
      screen.getByText("JPEG preserved unchanged · same size"),
    ).toBeVisible();
    expect(contentAdminApi.downloadReadingPreview).toHaveBeenCalledWith(
      identity,
      image,
      expect.any(AbortSignal),
    );
    fireEvent.click(screen.getByRole("button", { name: "Close", exact: true }));
    await waitFor(() => expect(revoke).toHaveBeenCalledWith("blob:prepared"));
  });
  it("aborts obsolete selection and ignores delayed bytes after unmount", async () => {
    let resolve!: (value: { blob: Blob; fileName: null }) => void;
    let signal: AbortSignal | undefined;
    vi.mocked(contentAdminApi.downloadReadingPreview).mockImplementation(
      (_identity, _image, current) => {
        signal = current;
        return new Promise((done) => {
          resolve = done;
        });
      },
    );
    const { unmount } = render(<ReadingPublicationPreview {...props} />);
    open();
    await waitFor(() =>
      expect(contentAdminApi.downloadReadingPreview).toHaveBeenCalled(),
    );
    unmount();
    expect(signal?.aborted).toBe(true);
    resolve({
      blob: new Blob(["jpeg"], { type: "image/jpeg" }),
      fileName: null,
    });
    await Promise.resolve();
    expect(create).not.toHaveBeenCalled();
  });
  it("shows network, mismatched byte and decode failures with explicit retry", async () => {
    vi.mocked(contentAdminApi.downloadReadingPreview).mockRejectedValueOnce(
      new Error("offline"),
    );
    render(<ReadingPublicationPreview {...props} />);
    open();
    await screen.findByRole("alert");
    fireEvent.click(screen.getByRole("button", { name: "Retry preview" }));
    const img = await screen.findByRole("img", { name: "Prepared cover" });
    fireEvent.error(img);
    expect(screen.queryByRole("img")).toBeNull();
    expect(revoke).toHaveBeenCalledWith("blob:prepared");
    vi.mocked(contentAdminApi.downloadReadingPreview).mockResolvedValueOnce({
      blob: new Blob(["wrong-size"], { type: "image/jpeg" }),
      fileName: null,
    });
    fireEvent.click(screen.getByRole("button", { name: "Retry preview" }));
    await screen.findByRole("alert");
    expect(screen.queryByRole("img")).toBeNull();
  });
  it.each([
    ["language", "en"],
    ["revision", "c".repeat(64)],
    ["liveGeneration", 2],
    ["manifestDigest", "d".repeat(64)],
  ])(
    "rejects mismatched %s without downloading or rendering",
    async (field, value) => {
      vi.mocked(contentAdminApi.getReadingPreview).mockResolvedValueOnce({
        ...manifest,
        identity: { ...identity, [field]: value },
      });
      render(<ReadingPublicationPreview {...props} />);
      open();
      await screen.findByRole("alert");
      expect(contentAdminApi.downloadReadingPreview).not.toHaveBeenCalled();
      expect(screen.queryByRole("img")).toBeNull();
    },
  );
  it("refreshes exact-language status on retry and accepts the new eligible LIVE", async () => {
    const newer = {
      ...identity,
      liveGeneration: 2,
      revision: "c".repeat(64),
      manifestDigest: "d".repeat(64),
      jobId: "22345678-1234-4234-8234-123456789abc",
    };
    const nextManifest = {
      ...manifest,
      identity: newer,
      images: manifest.images.map((entry) => ({
        ...entry,
        renditionKey: `new-${entry.renditionKey}`,
      })),
    };
    vi.mocked(contentAdminApi.getReadingPreview).mockResolvedValue(
      nextManifest,
    );
    vi.mocked(contentAdminApi.getReadingPublication).mockResolvedValue({
      ...status,
      liveGeneration: newer.liveGeneration,
      liveRevision: newer.revision,
      liveManifestDigest: newer.manifestDigest,
    });
    const refreshed = vi.fn();
    render(
      <ReadingPublicationPreview {...props} onStatusRefresh={refreshed} />,
    );
    open();
    await screen.findByRole("alert");
    expect(contentAdminApi.downloadReadingPreview).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry preview" }));
    await screen.findByRole("img", { name: "Prepared cover" });
    expect(contentAdminApi.getReadingPublication).toHaveBeenCalledWith(
      1,
      "tr",
      expect.any(AbortSignal),
    );
    expect(refreshed).toHaveBeenCalledWith(
      expect.objectContaining({ liveRevision: newer.revision }),
    );
    expect(contentAdminApi.downloadReadingPreview).toHaveBeenCalledWith(
      newer,
      nextManifest.images[0],
      expect.any(AbortSignal),
    );
    expect(contentAdminApi.downloadReadingPreview).toHaveBeenCalledTimes(1);
  });
});
