import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  originalAudioAssetViewModel,
  uploadedFirebaseAudioAssetResponse,
  phoneThumbnailAssetViewModel,
  uploadedFirebaseImageAssetResponse,
} from "@/features/assets/test/fixtures";

import { AssetPickerField } from "./asset-picker-field";

const assetDetailMocks = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

const previewHookMocks = vi.hoisted(() => ({
  useAssetPreview: vi.fn(),
}));

const uploadAssetHookMocks = vi.hoisted(() => ({
  useUploadAsset: vi.fn(),
  mutateAsync: vi.fn(),
  reset: vi.fn(),
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailMocks.useAssetDetail,
}));

vi.mock("@/features/assets/queries/use-asset-preview", () => ({
  useAssetPreview: previewHookMocks.useAssetPreview,
}));

vi.mock("@/features/assets/mutations/use-upload-asset", () => ({
  useUploadAsset: uploadAssetHookMocks.useUploadAsset,
}));

vi.mock("@/features/assets/components/asset-picker-dialog", () => ({
  AssetPickerDialog: ({
    open,
    onSelectAsset,
  }: {
    open: boolean;
    onSelectAsset: (asset: { id: number }) => void;
  }) =>
    open ? (
      <button
        type="button"
        onClick={() => onSelectAsset(phoneThumbnailAssetViewModel)}
      >
        Select mock asset
      </button>
    ) : null,
}));

vi.mock("@/features/assets/components/asset-upload-dialog", () => ({
  AssetUploadDialog: ({
    open,
    onUploaded,
  }: {
    open: boolean;
    onUploaded: (asset: { id: number }) => void;
  }) =>
    open ? (
      <button
        type="button"
        onClick={() =>
          onUploaded({ id: uploadedFirebaseImageAssetResponse.assetId })
        }
      >
        Finish upload
      </button>
    ) : null,
}));

function TestWrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return (
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MemoryRouter>
  );
}

beforeEach(() => {
  assetDetailMocks.useAssetDetail.mockReset();
  previewHookMocks.useAssetPreview.mockReset();
  uploadAssetHookMocks.useUploadAsset.mockReset();
  uploadAssetHookMocks.mutateAsync.mockReset();
  uploadAssetHookMocks.reset.mockReset();
  previewHookMocks.useAssetPreview.mockReturnValue({
    previewUrl: phoneThumbnailAssetViewModel.cachedDownloadUrl,
    previewStatus: "available",
    previewErrorMessage: null,
    isRefreshing: false,
    refreshPreview: vi.fn(),
  });
  uploadAssetHookMocks.useUploadAsset.mockImplementation(
    (options: { onSuccess?: (asset: { id: number }) => void } = {}) => ({
      mutateAsync: async (input: { kind: string }) => {
        uploadAssetHookMocks.mutateAsync(input);
        const uploadedAsset =
          input.kind === "ORIGINAL_AUDIO"
            ? { id: uploadedFirebaseAudioAssetResponse.assetId }
            : { id: uploadedFirebaseImageAssetResponse.assetId };

        await options.onSuccess?.(uploadedAsset);
        return uploadedAsset;
      },
      isPending: false,
      problem: null,
      reset: uploadAssetHookMocks.reset,
    }),
  );
});

function dropFiles(files: File[]) {
  return {
    dataTransfer: {
      files,
      types: ["Files"],
      dropEffect: "copy",
    },
  };
}

describe("AssetPickerField", () => {
  it("renders selected image preview while hiding manual id by default", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: phoneThumbnailAssetViewModel,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={phoneThumbnailAssetViewModel.id}
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(
      screen.getByRole("img", { name: /preview of asset #4/i }),
    ).toBeVisible();
    expect(screen.queryByLabelText(/manual asset id/i)).not.toBeInTheDocument();
  });

  it("renders selected audio player for audio assets", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: originalAudioAssetViewModel,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    previewHookMocks.useAssetPreview.mockReturnValue({
      previewUrl: "https://storage.test/audio-preview-1.mp3",
      previewStatus: "available",
      previewErrorMessage: null,
      isRefreshing: false,
      refreshPreview: vi.fn(),
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="audioMediaId"
          label="Audio asset"
          mediaType="AUDIO"
          pickerDescription="Pick audio asset"
          pickerTitle="Pick audio asset"
          value={originalAudioAssetViewModel.id}
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(
      screen.getByLabelText(/audio preview for asset #1/i),
    ).toHaveAttribute("src", "https://storage.test/audio-preview-1.mp3");
  });

  it("renders editor image previews as a portrait-focused cover card", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: phoneThumbnailAssetViewModel,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={phoneThumbnailAssetViewModel.id}
          variant="editor"
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(
      screen.getByTestId("asset-field-preview-editor-cover"),
    ).toBeVisible();
    expect(
      screen.getByTestId("asset-field-preview-editor-cover-stage"),
    ).toBeVisible();
    expect(screen.getByText(/portrait preview/i)).toBeVisible();
    expect(
      screen.getByTestId("asset-field-preview-editor-cover-actions"),
    ).toContainElement(screen.getByRole("button", { name: /upload new/i }));
    expect(
      screen.queryByText(/cover assets are shown in a portrait-focused frame/i),
    ).not.toBeInTheDocument();
  });

  it("opens a larger preview dialog when the cover image is clicked", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: phoneThumbnailAssetViewModel,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={phoneThumbnailAssetViewModel.id}
          variant="editor"
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    fireEvent.click(screen.getByTestId("asset-field-preview-image-button"));

    expect(screen.getByText(/kapak onizlemesi|gorsel onizleme/i)).toBeVisible();
    expect(screen.getByRole("dialog")).toBeVisible();
  });

  it("opens advanced mode before exposing manual asset input", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="illustrationMediaId"
          label="Illustration asset"
          mediaType="IMAGE"
          pickerDescription="Pick illustration asset"
          pickerTitle="Pick illustration asset"
          value={null}
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(screen.queryByLabelText(/manual asset id/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));

    expect(screen.getByLabelText(/manual asset id/i)).toBeVisible();
  });

  it("keeps debug helper copy hidden in editor variant until advanced is opened", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={null}
          variant="editor"
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(
      screen.queryByText(/media utility stays available/i),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /advanced/i }));

    expect(screen.getByText(/media utility remains available/i)).toBeVisible();
  });

  it("keeps selected audio editor actions inside the compact preview card", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: originalAudioAssetViewModel,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    previewHookMocks.useAssetPreview.mockReturnValue({
      previewUrl: "https://storage.test/audio-preview-1.mp3",
      previewStatus: "available",
      previewErrorMessage: null,
      isRefreshing: false,
      refreshPreview: vi.fn(),
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="audioMediaId"
          label="Audio asset"
          mediaType="AUDIO"
          pickerDescription="Pick audio asset"
          pickerTitle="Pick audio asset"
          value={originalAudioAssetViewModel.id}
          variant="editor"
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    expect(screen.getByTestId("asset-field-preview-actions")).toContainElement(
      screen.getByRole("button", { name: /upload new/i }),
    );
    expect(
      screen.getByLabelText(/audio preview for asset #1/i),
    ).toHaveAttribute("src", "https://storage.test/audio-preview-1.mp3");
  });

  it("updates the selected asset through the picker dialog", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    const onChange = vi.fn();

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={null}
          onChange={onChange}
        />
      </TestWrapper>,
    );

    fireEvent.click(screen.getByRole("button", { name: /browse existing/i }));
    fireEvent.click(screen.getByRole("button", { name: /select mock asset/i }));

    expect(onChange).toHaveBeenCalledWith(phoneThumbnailAssetViewModel.id);
  });

  it("selects the uploaded asset automatically after upload completes", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    const onChange = vi.fn();

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          value={null}
          onChange={onChange}
        />
      </TestWrapper>,
    );

    fireEvent.click(screen.getByRole("button", { name: /upload new/i }));
    fireEvent.click(screen.getByRole("button", { name: /finish upload/i }));

    expect(onChange).toHaveBeenCalledWith(
      uploadedFirebaseImageAssetResponse.assetId,
    );
  });

  it("uploads a dropped image file and selects the new asset", async () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    const onChange = vi.fn();

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          testId="cover-asset"
          value={null}
          onChange={onChange}
        />
      </TestWrapper>,
    );

    const droppedFile = new File(["image"], "cover.png", {
      type: "image/png",
    });
    fireEvent.drop(
      screen.getByTestId("cover-asset-dropzone"),
      dropFiles([droppedFile]),
    );

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledWith(
        uploadedFirebaseImageAssetResponse.assetId,
      );
    });
    expect(uploadAssetHookMocks.mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "ORIGINAL_IMAGE",
        file: expect.any(File),
        onProgress: expect.any(Function),
      }),
    );
  });

  it("uploads a dropped audio file and selects the new asset", async () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });
    const onChange = vi.fn();

    render(
      <TestWrapper>
        <AssetPickerField
          id="audioMediaId"
          label="Audio asset"
          mediaType="AUDIO"
          pickerDescription="Pick audio asset"
          pickerTitle="Pick audio asset"
          testId="audio-asset"
          value={null}
          onChange={onChange}
        />
      </TestWrapper>,
    );

    fireEvent.drop(
      screen.getByTestId("audio-asset-dropzone"),
      dropFiles([
        new File(["audio"], "narration.mp3", {
          type: "audio/mpeg",
        }),
      ]),
    );

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledWith(
        uploadedFirebaseAudioAssetResponse.assetId,
      );
    });
    expect(uploadAssetHookMocks.mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "ORIGINAL_AUDIO",
      }),
    );
  });

  it("rejects dropped files with the wrong media type", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          testId="cover-asset"
          value={null}
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    fireEvent.drop(
      screen.getByTestId("cover-asset-dropzone"),
      dropFiles([
        new File(["audio"], "narration.mp3", {
          type: "audio/mpeg",
        }),
      ]),
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      /accepts image files only/i,
    );
    expect(uploadAssetHookMocks.mutateAsync).not.toHaveBeenCalled();
  });

  it("does not direct-upload dropped files while disabled", () => {
    assetDetailMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    render(
      <TestWrapper>
        <AssetPickerField
          disabled
          id="coverMediaId"
          label="Cover asset"
          mediaType="IMAGE"
          pickerDescription="Pick cover asset"
          pickerTitle="Pick cover asset"
          testId="cover-asset"
          value={null}
          onChange={vi.fn()}
        />
      </TestWrapper>,
    );

    fireEvent.drop(
      screen.getByTestId("cover-asset-dropzone"),
      dropFiles([
        new File(["image"], "cover.png", {
          type: "image/png",
        }),
      ]),
    );

    expect(uploadAssetHookMocks.mutateAsync).not.toHaveBeenCalled();
  });
});
