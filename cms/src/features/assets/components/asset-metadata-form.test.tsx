import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/lib/http/client";
import { phoneThumbnailAssetViewModel } from "@/features/assets/test/fixtures";
import type { ApiProblemDetail } from "@/types/api";

import { AssetMetadataForm } from "./asset-metadata-form";

const updateAssetMetadataHookMock = vi.hoisted(() => ({
  useUpdateAssetMetadata: vi.fn(),
}));

vi.mock("@/features/assets/mutations/use-update-asset-metadata", () => ({
  useUpdateAssetMetadata: updateAssetMetadataHookMock.useUpdateAssetMetadata,
}));

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 409,
    detail: "Unexpected asset conflict",
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
  updateAssetMetadataHookMock.useUpdateAssetMetadata.mockReset();
  updateAssetMetadataHookMock.useUpdateAssetMetadata.mockReturnValue(
    makeMutationState(),
  );
});

describe("AssetMetadataForm", () => {
  it("validates byte size before submit", async () => {
    const mutationState = makeMutationState();
    updateAssetMetadataHookMock.useUpdateAssetMetadata.mockReturnValue(
      mutationState,
    );

    render(<AssetMetadataForm asset={phoneThumbnailAssetViewModel} />);

    fireEvent.change(screen.getByLabelText(/byte size/i), {
      target: { value: "-1" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    expect(
      await screen.findByText("Byte size must be zero or greater."),
    ).toBeInTheDocument();
    expect(mutationState.mutateAsync).not.toHaveBeenCalled();
  });

  it("submits normalized nullable metadata values", async () => {
    const mutationState = makeMutationState({
      mutateAsync: vi.fn().mockResolvedValue({
        ...phoneThumbnailAssetViewModel,
        mimeType: null,
        byteSize: null,
        checksumSha256: null,
      }),
    });
    updateAssetMetadataHookMock.useUpdateAssetMetadata.mockReturnValue(
      mutationState,
    );

    render(<AssetMetadataForm asset={phoneThumbnailAssetViewModel} />);

    fireEvent.change(screen.getByLabelText(/mime type/i), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByLabelText(/byte size/i), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByLabelText(/sha-256 checksum/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    await waitFor(() => {
      expect(mutationState.mutateAsync).toHaveBeenCalledWith({
        mimeType: null,
        byteSize: null,
        checksumSha256: null,
      });
    });
  });

  it("maps server validation problems back onto the form", async () => {
    const mutationState = makeMutationState({
      mutateAsync: vi.fn().mockRejectedValue(
        makeApiClientError(
          makeProblem({
            status: 400,
            title: "Validation failed",
            detail: "Asset metadata is invalid",
            errorCode: "validation_error",
            fieldErrors: {
              mimeType: "MIME type is invalid.",
            },
          }),
        ),
      ),
    });
    updateAssetMetadataHookMock.useUpdateAssetMetadata.mockReturnValue(
      mutationState,
    );

    render(<AssetMetadataForm asset={phoneThumbnailAssetViewModel} />);

    fireEvent.change(screen.getByLabelText(/mime type/i), {
      target: { value: "broken" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    expect(await screen.findByText("MIME type is invalid.")).toBeVisible();
  });
});
