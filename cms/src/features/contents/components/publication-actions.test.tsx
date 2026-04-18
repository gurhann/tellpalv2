import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { meditationContentViewModel } from "@/features/contents/test/fixtures";

import { PublicationActions } from "./publication-actions";

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
