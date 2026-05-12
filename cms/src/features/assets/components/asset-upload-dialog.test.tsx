import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  uploadedFirebaseAudioAssetResponse,
  uploadedFirebaseImageAssetResponse,
} from "@/features/assets/test/fixtures";

import { AssetUploadDialog } from "./asset-upload-dialog";

const uploadAssetHookMocks = vi.hoisted(() => ({
  useUploadAsset: vi.fn(),
  mutateAsync: vi.fn(),
  reset: vi.fn(),
}));

vi.mock("@/features/assets/mutations/use-upload-asset", () => ({
  useUploadAsset: uploadAssetHookMocks.useUploadAsset,
}));

beforeEach(() => {
  uploadAssetHookMocks.useUploadAsset.mockReset();
  uploadAssetHookMocks.mutateAsync.mockReset();
  uploadAssetHookMocks.reset.mockReset();
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

describe("AssetUploadDialog", () => {
  it("uses a dropped image file as the selected upload file", () => {
    render(
      <AssetUploadDialog open onOpenChange={vi.fn()} title="Upload asset" />,
    );

    fireEvent.drop(
      screen.getByTestId("asset-upload-dialog-dropzone"),
      dropFiles([
        new File(["image"], "bedtime-cover.jpg", {
          type: "image/jpeg",
        }),
      ]),
    );

    expect(screen.getByText("bedtime-cover.jpg")).toBeVisible();
    expect(screen.getByText(/image\/jpeg/i)).toBeVisible();
  });

  it("infers audio uploads from a dropped audio file in the unlocked dialog", async () => {
    const onUploaded = vi.fn();
    render(
      <AssetUploadDialog
        open
        title="Upload asset"
        onOpenChange={vi.fn()}
        onUploaded={onUploaded}
      />,
    );

    fireEvent.drop(
      screen.getByTestId("asset-upload-dialog-dropzone"),
      dropFiles([
        new File(["audio"], "sleep-story.mp3", {
          type: "audio/mpeg",
        }),
      ]),
    );
    fireEvent.click(screen.getByRole("button", { name: /^upload asset$/i }));

    await waitFor(() => {
      expect(uploadAssetHookMocks.mutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "ORIGINAL_AUDIO",
          file: expect.any(File),
          onProgress: expect.any(Function),
        }),
      );
    });
    expect(onUploaded).toHaveBeenCalledWith({
      id: uploadedFirebaseAudioAssetResponse.assetId,
    });
  });

  it("rejects an audio drop in a fixed image upload dialog", () => {
    render(
      <AssetUploadDialog
        fixedKind="ORIGINAL_IMAGE"
        open
        title="Upload image"
        onOpenChange={vi.fn()}
      />,
    );

    fireEvent.drop(
      screen.getByTestId("asset-upload-dialog-dropzone"),
      dropFiles([
        new File(["audio"], "sleep-story.mp3", {
          type: "audio/mpeg",
        }),
      ]),
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      /original image uploads require an image file/i,
    );
    expect(uploadAssetHookMocks.mutateAsync).not.toHaveBeenCalled();
  });
});
