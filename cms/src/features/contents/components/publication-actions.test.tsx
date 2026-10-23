import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useState } from "react";
import {
  contentAdminApi,
  type ReadingPublicationStatus,
} from "@/features/contents/api/content-admin";

import {
  meditationContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { PublicationActions } from "./publication-actions";

const reading = vi.hoisted(() => ({ useReadingPublication: vi.fn() }));
vi.mock("@/features/contents/queries/use-reading-publication", () => ({
  useReadingPublication: reading.useReadingPublication,
}));
const contentLocalizationActionsMock = vi.hoisted(() => ({
  useContentLocalizationActions: vi.fn(),
}));

vi.mock(
  "@/features/contents/mutations/use-content-localization-actions",
  () => ({
    useContentLocalizationActions:
      contentLocalizationActionsMock.useContentLocalizationActions,
  }),
);

function makeMutationState(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn().mockResolvedValue(undefined),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  reading.useReadingPublication.mockReset();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReset();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue({
    publishLocalization: makeMutationState(),
    archiveLocalization: makeMutationState(),
    saveLocalization: makeMutationState(),
  });
});

it("keeps the parent preview dialog open when retry adopts a newer LIVE and closes it on language change", async () => {
  const oldStatus: ReadingPublicationStatus = {
    enabled: true,
    contract: "INTERNAL_LOCAL_TEST",
    state: "LIVE",
    generation: 1,
    liveGeneration: 1,
    liveRevision: "a".repeat(64),
    liveManifestDigest: "b".repeat(64),
    diagnostic: null,
    retryable: false,
  };
  const newStatus = {
    ...oldStatus,
    generation: 2,
    liveGeneration: 2,
    liveRevision: "c".repeat(64),
    liveManifestDigest: "d".repeat(64),
  };
  const newIdentity = {
    contentId: storyContentViewModel.summary.id,
    language: "en",
    liveGeneration: 2,
    revision: newStatus.liveRevision!,
    manifestDigest: newStatus.liveManifestDigest!,
    jobId: "12345678-1234-4234-8234-123456789abc",
  };
  const newImage = {
    role: "READING_COVER" as const,
    pageNumber: 0,
    renditionKey: "new-cover",
    sourceMime: "image/jpeg",
    sourceBytes: 4,
    sourceWidth: 80,
    sourceHeight: 60,
    mime: "image/jpeg" as const,
    bytes: 4,
    width: 80,
    height: 60,
  };
  reading.useReadingPublication.mockImplementation(() => {
    const [data, setData] = useState(oldStatus);
    return {
      data,
      acceptStatus: setData,
      retry: { isPending: false },
      isLoading: false,
      isError: false,
    };
  });
  const preview = vi
    .spyOn(contentAdminApi, "getReadingPreview")
    .mockResolvedValue({ identity: newIdentity, images: [newImage] });
  const status = vi
    .spyOn(contentAdminApi, "getReadingPublication")
    .mockResolvedValue(newStatus);
  const bytes = vi
    .spyOn(contentAdminApi, "downloadReadingPreview")
    .mockResolvedValue({
      blob: new Blob(["jpeg"], { type: "image/jpeg" }),
      fileName: null,
    });
  const create = vi
    .spyOn(URL, "createObjectURL")
    .mockReturnValue("blob:new-live");
  const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
  try {
    const { rerender } = render(
      <PublicationActions
        content={storyContentViewModel}
        localization={storyContentViewModel.localizations[0]}
      />,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Preview prepared images" }),
    );
    await screen.findByRole("alert");
    expect(bytes).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry preview" }));
    expect(
      await screen.findByRole("img", { name: "Prepared cover" }),
    ).toHaveAttribute("src", "blob:new-live");
    expect(screen.getByRole("dialog")).toBeVisible();
    expect(status).toHaveBeenCalledWith(
      storyContentViewModel.summary.id,
      "en",
      expect.any(AbortSignal),
    );
    expect(bytes).toHaveBeenCalledWith(
      newIdentity,
      newImage,
      expect.any(AbortSignal),
    );
    expect(bytes).toHaveBeenCalledTimes(1);
    rerender(
      <PublicationActions
        content={storyContentViewModel}
        localization={storyContentViewModel.localizations[1]}
      />,
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(revoke).toHaveBeenCalledWith("blob:new-live");
  } finally {
    preview.mockRestore();
    status.mockRestore();
    bytes.mockRestore();
    create.mockRestore();
    revoke.mockRestore();
  }
});

describe("PublicationActions", () => {
  it("renders only gating context and action buttons without summary grids", async () => {
    const publishLocalization = makeMutationState();
    const archiveLocalization = makeMutationState();
    contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue(
      {
        publishLocalization,
        archiveLocalization,
        saveLocalization: makeMutationState(),
      },
    );

    render(
      <PublicationActions
        content={meditationContentViewModel}
        localization={meditationContentViewModel.localizations[0]}
      />,
    );

    expect(screen.getByText("Publishing")).toBeVisible();
    expect(
      screen.getByText(/publishing is meaningful once this locale metadata/i),
    ).toBeVisible();
    expect(screen.getByText("Not yet published")).toBeVisible();
    expect(screen.queryByText(/^status$/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/^processing$/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/mobile visibility/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /publish locale/i }));
    fireEvent.click(screen.getByRole("button", { name: /archive locale/i }));

    await waitFor(() => {
      expect(publishLocalization.mutateAsync).toHaveBeenCalledWith({
        languageCode: meditationContentViewModel.localizations[0].languageCode,
      });
      expect(archiveLocalization.mutateAsync).toHaveBeenCalledWith(
        meditationContentViewModel.localizations[0].languageCode,
      );
    });
  });
});

it("shows truthful pending, retained failure, explicit retry and automatic verified adoption", () => {
  const retry = vi.fn();
  const state = (value: string) => ({
    data: {
      enabled: true,
      state: value,
      liveRevision: value === "PENDING" ? null : "a".repeat(64),
      retryable: value === "FAILED",
      diagnostic: value === "FAILED" ? "SOURCE_NOT_ADMITTED" : null,
    },
    retry: { mutate: retry, isPending: false, isError: false },
    isLoading: false,
    isError: false,
  });
  reading.useReadingPublication.mockReturnValue(state("PENDING"));
  const props = {
    content: storyContentViewModel,
    localization: storyContentViewModel.localizations[0],
  };
  const { rerender } = render(<PublicationActions {...props} />);
  expect(screen.getByText("Reading preparation pending")).toBeVisible();
  expect(
    screen.getByText(/images are prepared automatically in local.test/i),
  ).toBeVisible();
  reading.useReadingPublication.mockReturnValue(state("FAILED"));
  rerender(<PublicationActions {...props} />);
  expect(
    screen.getByText(/previous prepared images remain in use/i),
  ).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Retry reading preparation" }),
  );
  expect(retry).toHaveBeenCalledOnce();
  reading.useReadingPublication.mockReturnValue(state("LIVE"));
  rerender(<PublicationActions {...props} />);
  expect(screen.getByText("Reading images ready")).toBeVisible();
  expect(screen.queryByText(/previous prepared images/i)).toBeNull();
});

it.each(["NOT_REQUESTED", "WITHDRAWN", "FAILED"])(
  "allows an editorially published STORY to Publish when enabled reading is %s",
  async (state) => {
    const publish = makeMutationState();
    contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue(
      {
        publishLocalization: publish,
        archiveLocalization: makeMutationState(),
        saveLocalization: makeMutationState(),
      },
    );
    reading.useReadingPublication.mockReturnValue({
      data: { enabled: true, state },
      isLoading: false,
      isError: false,
      retry: { isPending: false },
    });
    const localization = {
      ...storyContentViewModel.localizations[0],
      isPublished: true,
      isArchived: false,
    };
    render(
      <PublicationActions
        content={storyContentViewModel}
        localization={localization}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: /publish locale/i }));
    await waitFor(() =>
      expect(publish.mutateAsync).toHaveBeenCalledWith({
        languageCode: localization.languageCode,
      }),
    );
  },
);

it.each(["LIVE", "PENDING", "PREPARING", "DISABLED"])(
  "keeps duplicate or legacy editorial Publish disabled for reading %s",
  (state) => {
    reading.useReadingPublication.mockReturnValue({
      data: { enabled: state !== "DISABLED", state },
      isLoading: false,
      isError: false,
      retry: { isPending: false },
    });
    const localization = {
      ...storyContentViewModel.localizations[0],
      isPublished: true,
      isArchived: false,
    };
    render(
      <PublicationActions
        content={storyContentViewModel}
        localization={localization}
      />,
    );
    expect(
      screen.getByRole("button", { name: /publish locale/i }),
    ).toBeDisabled();
  },
);
