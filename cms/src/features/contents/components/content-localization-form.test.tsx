import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  meditationContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";
import {
  getCreateLocalizationFormDefaults,
  mapLocalizationToFormValues,
} from "@/features/contents/schema/content-localization-schema";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { ContentLocalizationForm } from "./content-localization-form";

const contentLocalizationActionsMock = vi.hoisted(() => ({
  useContentLocalizationActions: vi.fn(),
}));

const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock(
  "@/features/contents/mutations/use-content-localization-actions",
  () => ({
    useContentLocalizationActions:
      contentLocalizationActionsMock.useContentLocalizationActions,
  }),
);

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected localization conflict",
    ...overrides,
  };
}

function makeApiClientError(problem: ApiProblemDetail) {
  return new ApiClientError(
    problem,
    new Response(JSON.stringify(problem), {
      status: problem.status,
      statusText: problem.title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    }),
  );
}

function makeMutationState(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  contentLocalizationActionsMock.useContentLocalizationActions.mockReset();
  assetDetailHookMock.useAssetDetail.mockReset();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue({
    saveLocalization: makeMutationState(),
    publishLocalization: makeMutationState(),
    archiveLocalization: makeMutationState(),
  });
  assetDetailHookMock.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
  });
});

describe("ContentLocalizationForm", () => {
  it("hides story-only body and audio inputs", () => {
    render(
      <ContentLocalizationForm
        content={storyContentViewModel}
        initialValues={mapLocalizationToFormValues(
          storyContentViewModel.localizations[0],
        )}
        localization={storyContentViewModel.localizations[0]}
        mode="update"
      />,
    );

    expect(screen.queryByLabelText(/body text/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/audio asset/i)).not.toBeInTheDocument();
  });

  it("shows non-story body and audio inputs for meditation locales", () => {
    render(
      <ContentLocalizationForm
        content={meditationContentViewModel}
        initialValues={mapLocalizationToFormValues(
          meditationContentViewModel.localizations[0],
        )}
        localization={meditationContentViewModel.localizations[0]}
        mode="update"
      />,
    );

    expect(screen.getByLabelText(/body text/i)).toBeVisible();
    expect(screen.getByText(/audio asset/i)).toBeVisible();
    expect(screen.queryByLabelText(/manual asset id/i)).not.toBeInTheDocument();
  });

  it("requires publishedAt when the localization status is published", async () => {
    const saveLocalization = makeMutationState();
    contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue(
      {
        saveLocalization,
        publishLocalization: makeMutationState(),
        archiveLocalization: makeMutationState(),
      },
    );

    render(
      <ContentLocalizationForm
        availableLanguages={[
          { code: "en", label: "English" },
          { code: "tr", label: "Turkish" },
        ]}
        content={meditationContentViewModel}
        initialValues={{
          ...getCreateLocalizationFormDefaults("en"),
          title: "Rain Room Reset",
          bodyText: "Breathe in for four counts and relax your shoulders.",
          audioMediaId: 1,
          status: "PUBLISHED",
          processingStatus: "COMPLETED",
          publishedAt: null,
        }}
        mode="create"
      />,
    );

    fireEvent.click(
      screen.getByRole("button", { name: /create localization/i }),
    );

    expect(
      await screen.findByText(
        "Published at is required when status is Published.",
      ),
    ).toBeVisible();
    expect(saveLocalization.mutateAsync).not.toHaveBeenCalled();
  });

  it("maps localization existence conflicts onto the language field", async () => {
    const saveLocalization = makeMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            title: "Localization exists",
            detail: "Localization already exists",
            errorCode: "content_localization_exists",
          }),
        ),
      ),
    });
    contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue(
      {
        saveLocalization,
        publishLocalization: makeMutationState(),
        archiveLocalization: makeMutationState(),
      },
    );

    render(
      <ContentLocalizationForm
        availableLanguages={[
          { code: "es", label: "Spanish" },
          { code: "pt", label: "Portuguese" },
        ]}
        content={meditationContentViewModel}
        initialValues={{
          ...getCreateLocalizationFormDefaults("es"),
          title: "Sala de lluvia",
          bodyText: "Respira y suelta la tension.",
          audioMediaId: 4,
        }}
        mode="create"
      />,
    );

    fireEvent.click(
      screen.getByRole("button", { name: /create localization/i }),
    );

    expect(
      await screen.findByText("Language already exists for this content."),
    ).toBeVisible();
  });

  it("submits transformed localization values in update mode", async () => {
    const saveLocalization = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 2,
        languageCode: "de",
        title: "Regenraum Pause",
        description: "Kurze Atemubung mit Regenatmosphare.",
        bodyText: "Atme vier Takte lang ein und entspanne die Schultern.",
        coverMediaId: 8,
        audioMediaId: 2,
        durationMinutes: 7,
        status: "PUBLISHED",
        processingStatus: "COMPLETED",
        publishedAt: "2026-03-29T08:30:00Z",
        visibleToMobile: true,
      }),
    });
    contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue(
      {
        saveLocalization,
        publishLocalization: makeMutationState(),
        archiveLocalization: makeMutationState(),
      },
    );

    render(
      <ContentLocalizationForm
        content={meditationContentViewModel}
        initialValues={mapLocalizationToFormValues(
          meditationContentViewModel.localizations[0],
        )}
        localization={meditationContentViewModel.localizations[0]}
        mode="update"
      />,
    );

    fireEvent.change(screen.getByLabelText(/title/i), {
      target: { value: "Regenraum Pause" },
    });
    fireEvent.click(screen.getAllByRole("button", { name: /advanced/i })[1]!);
    fireEvent.change(screen.getByLabelText(/cover asset id/i), {
      target: { value: "8" },
    });
    fireEvent.change(screen.getByLabelText(/duration minutes/i), {
      target: { value: "7" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save localization/i }));

    await waitFor(() => {
      expect(saveLocalization.mutateAsync).toHaveBeenCalledWith({
        mode: "update",
        values: expect.objectContaining({
          languageCode: "de",
          title: "Regenraum Pause",
          coverMediaId: 8,
          durationMinutes: 7,
        }),
      });
    });
  });
});
