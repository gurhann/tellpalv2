type UploadFileToSignedUrlOptions = {
  uploadUrl: string;
  httpMethod: string;
  requiredHeaders: Record<string, string>;
  file: File;
  onProgress?: (progress: number) => void;
  signal?: AbortSignal;
};

export function uploadFileToSignedUrl({
  uploadUrl,
  httpMethod,
  requiredHeaders,
  file,
  onProgress,
  signal,
}: UploadFileToSignedUrlOptions) {
  return new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(httpMethod, uploadUrl, true);

    Object.entries(requiredHeaders).forEach(([header, value]) => {
      xhr.setRequestHeader(header, value);
    });

    xhr.upload.addEventListener("progress", (event) => {
      if (!event.lengthComputable) {
        return;
      }
      onProgress?.(Math.round((event.loaded / event.total) * 100));
    });

    xhr.addEventListener("load", () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.(100);
        resolve();
        return;
      }

      reject(
        new Error(`Signed upload request failed with status ${xhr.status}.`),
      );
    });

    xhr.addEventListener("error", () => {
      reject(
        new Error(
          "Signed upload request failed before the browser could complete the transfer.",
        ),
      );
    });

    xhr.addEventListener("abort", () => {
      reject(new Error("Signed upload request was aborted."));
    });

    if (signal) {
      signal.addEventListener("abort", () => xhr.abort(), { once: true });
    }

    xhr.send(file);
  });
}
