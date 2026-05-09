import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  assetAdminApi,
  type UploadableAssetKind,
} from "@/features/assets/api/asset-admin";
import { syncAssetCaches } from "@/features/assets/lib/asset-cache";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type UploadAssetInput = {
  file: File;
  kind: UploadableAssetKind;
  onProgress?: (progress: number) => void;
  signal?: AbortSignal;
};

type UseUploadAssetOptions = {
  onSuccess?: (asset: AssetViewModel) => void | Promise<void>;
};

function getApiProblem(error: unknown) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return {
    type: "about:blank",
    title: "Upload failed",
    status: 500,
    detail:
      error instanceof Error
        ? error.message
        : "The asset upload could not be completed.",
  } satisfies ApiProblemDetail;
}

export function useUploadAsset(options: UseUploadAssetOptions = {}) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: async ({
      file,
      kind,
      onProgress,
      signal,
    }: UploadAssetInput): Promise<AssetViewModel> => {
      onProgress?.(5);
      const uploadedAsset = await assetAdminApi.uploadAsset(
        {
          kind,
          file,
        },
        {
          signal,
        },
      );
      onProgress?.(100);
      return mapAdminAsset(uploadedAsset);
    },
    onSuccess: async (asset) => {
      await syncAssetCaches(queryClient, asset);
      await options.onSuccess?.(asset);
    },
  });

  return {
    ...mutation,
    uploadedAsset: mutation.data ?? null,
    problem: getApiProblem(mutation.error),
  };
}

export async function uploadAssetThroughDeprecatedSignedUrlFlow({
  file,
  kind,
  onProgress,
  signal,
}: UploadAssetInput): Promise<AssetViewModel> {
  const { uploadFileToSignedUrl } =
    await import("@/features/assets/lib/upload-file-to-signed-url");
  const initiateResponse = await assetAdminApi.initiateAssetUpload({
    kind,
    fileName: file.name,
    mimeType: file.type,
    byteSize: file.size,
  });
  onProgress?.(5);
  let completedAsset;
  try {
    await uploadFileToSignedUrl({
      uploadUrl: initiateResponse.uploadUrl,
      httpMethod: initiateResponse.httpMethod,
      requiredHeaders: initiateResponse.requiredHeaders,
      file,
      onProgress,
      signal,
    });
    completedAsset = await assetAdminApi.completeAssetUpload({
      uploadToken: initiateResponse.uploadToken,
    });
  } catch (error) {
    if (signal?.aborted) {
      throw error;
    }
    completedAsset = await assetAdminApi.proxyAssetUpload({
      uploadToken: initiateResponse.uploadToken,
      file,
    });
  }
  return mapAdminAsset(completedAsset);
}
