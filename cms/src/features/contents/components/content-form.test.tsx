import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { ContentForm } from "./content-form";

const saveContentHookMock = vi.hoisted(() => ({
  useSaveContent: vi.fn(),
}));

vi.mock("@/features/contents/mutations/use-save-content", () => ({
  useSaveContent: saveContentHookMock.useSaveContent,
}));

vi.mock("@/features/assets/components/asset-picker-field", () => ({
  AssetPickerField: ({
    label,
    onChange,
    value,
    testId,
    error,
  }: {
    label: string;
    onChange: (value: number | null) => void;
    value: number | null;
    testId?: string;
    error?: { message?: string };
  }) => (
    <div data-testid={testId}>
      <span>{label}</span>
      <span data-testid={testId ? `${testId}-value` : undefined}>
        {value ?? "none"}
      </span>
      <button
        type="button"
        data-testid={testId ? `${testId}-select` : undefined}
        onClick={() => onChange(702)}
      >
        Select asset 702
      </button>
      {error?.message ? <span>{error.message}</span> : null}
    </div>
  ),
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected content conflict",
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

function makeSaveMutationState(
  overrides: Partial<ReturnType<typeof vi.fn>> = {},
) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  saveContentHookMock.useSaveContent.mockReset();
  saveContentHookMock.useSaveContent.mockReturnValue(makeSaveMutationState());
});

describe("ContentForm", () => {
  it("shows only canonical content type options", async () => {
    render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "",
          ageRange: null,
          active: true,
          listeningCoverMediaId: null,
        }}
        mode="create"
      />,
    );

    fireEvent.click(screen.getByRole("combobox", { name: /content type/i }));

    expect(await screen.findByRole("option", { name: "Story" })).toBeVisible();
    expect(screen.getByRole("option", { name: "Meditation" })).toBeVisible();
    expect(screen.getByRole("option", { name: "Lullaby" })).toBeVisible();
    expect(
      screen.queryByRole("option", { name: "Audio Story" }),
    ).not.toBeInTheDocument();
  });

  it("validates the external key before submit", async () => {
    const mutationState = makeSaveMutationState();
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "",
          ageRange: null,
          active: true,
          listeningCoverMediaId: null,
        }}
        mode="create"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /create content/i }));

    expect(
      await screen.findByText("External key is required."),
    ).toBeInTheDocument();
    expect(mutationState.mutateAsync).not.toHaveBeenCalled();
  });

  it("keeps the fixed type helper in update mode", () => {
    const firstRender = render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "story.evening-garden",
          ageRange: 5,
          active: true,
          textlessCoverMediaId: null,
          listeningCoverMediaId: null,
        }}
        mode="create"
      />,
    );

    expect(screen.queryByText(/story workflow/i)).not.toBeInTheDocument();

    firstRender.unmount();

    render(
      <ContentForm
        initialValues={{
          type: "MEDITATION",
          externalKey: "meditation.rain-room",
          ageRange: 8,
          active: true,
          textlessCoverMediaId: null,
          listeningCoverMediaId: null,
        }}
        mode="update"
      />,
    );

    expect(
      screen.getByText(/content type is fixed after creation/i),
    ).toBeVisible();
  });

  it("maps duplicate external key conflicts onto the externalKey field", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            title: "External key conflict",
            detail: "Content external key already exists",
            errorCode: "duplicate_external_key",
          }),
        ),
      ),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        initialValues={{
          type: "STORY",
          externalKey: "",
          ageRange: null,
          active: true,
          listeningCoverMediaId: null,
        }}
        mode="create"
      />,
    );

    fireEvent.change(screen.getByLabelText(/external key/i), {
      target: { value: "story.evening-garden" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create content/i }));

    expect(
      await screen.findByText("External key is already in use."),
    ).toBeVisible();
  });

  it("shows the listening cover selector only for supported content types", () => {
    const renderUpdateForm = (type: "STORY" | "MEDITATION" | "LULLABY") =>
      render(
        <ContentForm
          contentId={1}
          initialValues={{
            type,
            externalKey: `${type.toLowerCase()}.evening-garden`,
            ageRange: 5,
            active: true,
            textlessCoverMediaId: null,
            listeningCoverMediaId: null,
          }}
          mode="update"
        />,
      );

    const storyRender = renderUpdateForm("STORY");

    expect(screen.getByText("Listening cover")).toBeVisible();

    storyRender.unmount();
    const meditationRender = renderUpdateForm("MEDITATION");
    expect(screen.getByText("Listening cover")).toBeVisible();

    meditationRender.unmount();
    const lullabyRender = renderUpdateForm("LULLABY");
    expect(screen.getByText("Listing cover (static)")).toBeVisible();
    expect(screen.getByText("Playback cover (animated)")).toBeVisible();

    lullabyRender.unmount();
  });

  it("submits editable metadata and the selected listening cover in update mode", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        contentId: 1,
        type: "STORY",
        externalKey: "story.evening-garden.updated",
        active: false,
        ageRange: 6,
        pageCount: 2,
        textlessCoverMediaId: null,
        listeningCoverMediaId: 702,
      }),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);

    render(
      <ContentForm
        contentId={1}
        initialValues={{
          type: "STORY",
          externalKey: "story.evening-garden",
          ageRange: 5,
          active: true,
          textlessCoverMediaId: null,
          listeningCoverMediaId: null,
        }}
        mode="update"
      />,
    );

    fireEvent.click(screen.getByTestId("content-listening-cover-select"));
    fireEvent.change(screen.getByLabelText(/external key/i), {
      target: { value: "story.evening-garden.updated" },
    });
    fireEvent.change(screen.getByLabelText(/age range/i), {
      target: { value: "6" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        type: "STORY",
        externalKey: "story.evening-garden.updated",
        ageRange: 6,
        active: true,
        textlessCoverMediaId: null,
        listeningCoverMediaId: 702,
      });
    });
    expect(
      screen.getByTestId("content-listening-cover-value"),
    ).toHaveTextContent("702");
  });

  it("keeps lullaby listing and playback covers independently selectable", () => {
    render(
      <ContentForm
        contentId={1}
        initialValues={{
          type: "LULLABY",
          externalKey: "lullaby.evening-garden",
          ageRange: 5,
          active: true,
          textlessCoverMediaId: null,
          listingCoverMediaId: 701,
          listeningCoverMediaId: 702,
        }}
        mode="update"
      />,
    );

    expect(screen.getByTestId("content-listing-cover-value")).toHaveTextContent("701");
    expect(screen.getByTestId("content-listening-cover-value")).toHaveTextContent("702");
  });

  it("preserves existing listing cover and submits both lullaby cover pickers", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockResolvedValue({}),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);
    render(
      <ContentForm contentId={1} mode="update" initialValues={{
        type: "LULLABY", externalKey: "lullaby.covers", ageRange: null, active: true,
        textlessCoverMediaId: null, listingCoverMediaId: 701, listeningCoverMediaId: 702,
      }} />,
    );

    fireEvent.click(screen.getByTestId("content-listing-cover-select"));
    fireEvent.click(screen.getByTestId("content-listening-cover-select"));
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => expect(mutationState.mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({ listingCoverMediaId: 702, listeningCoverMediaId: 702 }),
    ));
  });

  it("maps a listing cover API error onto the lullaby picker", async () => {
    const mutationState = makeSaveMutationState({
      mutateAsync: vi.fn().mockRejectedValue(makeApiClientError(makeProblem({
        status: 400,
        fieldErrors: { listingCoverMediaId: "Listing cover must be an image." },
      }))),
    });
    saveContentHookMock.useSaveContent.mockReturnValue(mutationState);
    render(<ContentForm contentId={1} mode="update" initialValues={{
      type: "LULLABY", externalKey: "lullaby.covers", ageRange: null, active: true,
      textlessCoverMediaId: null, listingCoverMediaId: 701, listeningCoverMediaId: 702,
    }} />);
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));
    expect(await screen.findByText("Listing cover must be an image.")).toBeVisible();
  });
});
