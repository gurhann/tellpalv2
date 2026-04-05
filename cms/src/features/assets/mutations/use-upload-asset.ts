import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  assetAdminApi,
  type UploadableAssetKind,
} from "@/features/assets/api/asset-admin";
import { syncAssetCaches } from "@/features/assets/lib/asset-cache";
import { uploadFileToSignedUrl } from "@/features/assets/lib/upload-file-to-signed-url";
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
      const initiateResponse = await assetAdminApi.initiateAssetUpload({
        kind,
        fileName: file.name,
        mimeType: file.type,
        byteSize: file.size,
      });
      onProgress?.(5);
      await uploadFileToSignedUrl({
        uploadUrl: initiateResponse.uploadUrl,
        httpMethod: initiateResponse.httpMethod,
        requiredHeaders: initiateResponse.requiredHeaders,
        file,
        onProgress,
        signal,
      });
      const completedAsset = await assetAdminApi.completeAssetUpload({
        uploadToken: initiateResponse.uploadToken,
      });
      return mapAdminAsset(completedAsset);
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
