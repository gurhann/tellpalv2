import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { meditationContentViewModel, storyContentViewModel } from "@/features/contents/test/fixtures";

import { PublicationActions } from "./publication-actions";

const reading = vi.hoisted(() => ({ useReadingPublication: vi.fn() }));
vi.mock("@/features/contents/queries/use-reading-publication", () => ({ useReadingPublication: reading.useReadingPublication }));
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
  contentLocalizationActionsMock.useContentLocalizationActions.mockReset();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue({
    publishLocalization: makeMutationState(),
    archiveLocalization: makeMutationState(),
    saveLocalization: makeMutationState(),
  });
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
  const state = (value: string) => ({ data: { enabled: true, state: value, liveRevision: value === "PENDING" ? null : "a".repeat(64), retryable: value === "FAILED", diagnostic: value === "FAILED" ? "SOURCE_NOT_ADMITTED" : null }, retry: { mutate: retry, isPending: false, isError: false }, isLoading: false, isError: false });
  reading.useReadingPublication.mockReturnValue(state("PENDING"));
  const props = { content: storyContentViewModel, localization: storyContentViewModel.localizations[0] };
  const { rerender } = render(<PublicationActions {...props} />);
  expect(screen.getByText("Reading preparation pending")).toBeVisible();
  expect(screen.getByText(/local.test reading contract/i)).toBeVisible();
  reading.useReadingPublication.mockReturnValue(state("FAILED"));rerender(<PublicationActions {...props} />);
  expect(screen.getByText(/previous verified revision remains eligible/i)).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Retry reading preparation" }));expect(retry).toHaveBeenCalledOnce();
  reading.useReadingPublication.mockReturnValue(state("LIVE"));rerender(<PublicationActions {...props} />);
  expect(screen.getByText("Verified reading revision adopted")).toBeVisible();expect(screen.queryByText(/previous verified revision/i)).toBeNull();
});

it.each(["NOT_REQUESTED", "WITHDRAWN", "FAILED"])("allows an editorially published STORY to Publish when enabled reading is %s", async (state) => {
  const publish = makeMutationState();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue({ publishLocalization: publish, archiveLocalization: makeMutationState(), saveLocalization: makeMutationState() });
  reading.useReadingPublication.mockReturnValue({ data: { enabled: true, state }, isLoading: false, isError: false, retry: { isPending: false } });
  const localization = { ...storyContentViewModel.localizations[0], isPublished: true, isArchived: false };
  render(<PublicationActions content={storyContentViewModel} localization={localization} />);
  fireEvent.click(screen.getByRole("button", { name: /publish locale/i }));
  await waitFor(() => expect(publish.mutateAsync).toHaveBeenCalledWith({ languageCode: localization.languageCode }));
});

it.each(["LIVE", "PENDING", "PREPARING", "DISABLED"])("keeps duplicate or legacy editorial Publish disabled for reading %s", (state) => {
  reading.useReadingPublication.mockReturnValue({ data: { enabled: state !== "DISABLED", state }, isLoading: false, isError: false, retry: { isPending: false } });
  const localization = { ...storyContentViewModel.localizations[0], isPublished: true, isArchived: false };
  render(<PublicationActions content={storyContentViewModel} localization={localization} />);
  expect(screen.getByRole("button", { name: /publish locale/i })).toBeDisabled();
});
