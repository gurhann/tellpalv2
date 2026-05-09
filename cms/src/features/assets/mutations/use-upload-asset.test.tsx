import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { uploadedFirebaseImageAssetResponse } from "@/features/assets/test/fixtures";

import { useUploadAsset } from "./use-upload-asset";

const assetAdminApiMock = vi.hoisted(() => ({
  uploadAsset: vi.fn(),
  initiateAssetUpload: vi.fn(),
  completeAssetUpload: vi.fn(),
  proxyAssetUpload: vi.fn(),
}));
const uploadFileToSignedUrlMock = vi.hoisted(() => vi.fn());

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      uploadAsset: assetAdminApiMock.uploadAsset,
      initiateAssetUpload: assetAdminApiMock.initiateAssetUpload,
      completeAssetUpload: assetAdminApiMock.completeAssetUpload,
      proxyAssetUpload: assetAdminApiMock.proxyAssetUpload,
    },
  };
});

vi.mock("@/features/assets/lib/upload-file-to-signed-url", () => ({
  uploadFileToSignedUrl: uploadFileToSignedUrlMock,
}));

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

beforeEach(() => {
  assetAdminApiMock.uploadAsset.mockReset();
  assetAdminApiMock.initiateAssetUpload.mockReset();
  assetAdminApiMock.completeAssetUpload.mockReset();
  assetAdminApiMock.proxyAssetUpload.mockReset();
  uploadFileToSignedUrlMock.mockReset();
});

describe("useUploadAsset", () => {
  it("uploads directly through the backend without signed Firebase transfer", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const file = new File(["image"], "cover.jpg", { type: "image/jpeg" });

    assetAdminApiMock.uploadAsset.mockResolvedValue(
      uploadedFirebaseImageAssetResponse,
    );

    const { result } = renderHook(() => useUploadAsset(), {
      wrapper: createWrapper(queryClient),
    });

    let uploadedAsset;
    await act(async () => {
      uploadedAsset = await result.current.mutateAsync({
        kind: "ORIGINAL_IMAGE",
        file,
      });
    });

    expect(assetAdminApiMock.uploadAsset).toHaveBeenCalledWith(
      {
        kind: "ORIGINAL_IMAGE",
        file,
      },
      {
        signal: undefined,
      },
    );
    expect(assetAdminApiMock.initiateAssetUpload).not.toHaveBeenCalled();
    expect(assetAdminApiMock.completeAssetUpload).not.toHaveBeenCalled();
    expect(assetAdminApiMock.proxyAssetUpload).not.toHaveBeenCalled();
    expect(uploadFileToSignedUrlMock).not.toHaveBeenCalled();
    expect(uploadedAsset?.id).toBe(12);
  });

  it("forwards abort signals to the backend upload request", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const file = new File(["image"], "cover.jpg", { type: "image/jpeg" });
    const abortController = new AbortController();

    assetAdminApiMock.uploadAsset.mockResolvedValue(
      uploadedFirebaseImageAssetResponse,
    );

    const { result } = renderHook(() => useUploadAsset(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.mutateAsync({
        kind: "ORIGINAL_IMAGE",
        file,
        signal: abortController.signal,
      });
    });

    expect(assetAdminApiMock.uploadAsset).toHaveBeenCalledWith(
      {
        kind: "ORIGINAL_IMAGE",
        file,
      },
      {
        signal: abortController.signal,
      },
    );
    expect(uploadFileToSignedUrlMock).not.toHaveBeenCalled();
  });

  it("keeps the deprecated signed URL flow available outside the default hook", async () => {
    const { uploadAssetThroughDeprecatedSignedUrlFlow } =
      await import("./use-upload-asset");
    const file = new File(["image"], "cover.jpg", { type: "image/jpeg" });

    assetAdminApiMock.initiateAssetUpload.mockResolvedValue({
      provider: "FIREBASE_STORAGE",
      objectPath: uploadedFirebaseImageAssetResponse.objectPath,
      uploadUrl: "https://firebase-storage.test/upload/cover.jpg",
      httpMethod: "PUT",
      requiredHeaders: {
        "Content-Type": "image/jpeg",
      },
      expiresAt: "2026-04-04T18:30:00Z",
      uploadToken: "upload-token-12",
    });
    uploadFileToSignedUrlMock.mockRejectedValue(new Error("Network failed"));
    assetAdminApiMock.proxyAssetUpload.mockResolvedValue(
      uploadedFirebaseImageAssetResponse,
    );

    const uploadedAsset = await uploadAssetThroughDeprecatedSignedUrlFlow({
      kind: "ORIGINAL_IMAGE",
      file,
    });

    expect(assetAdminApiMock.proxyAssetUpload).toHaveBeenCalledWith({
      uploadToken: "upload-token-12",
      file,
    });
    expect(uploadedAsset?.id).toBe(12);
  });
});
