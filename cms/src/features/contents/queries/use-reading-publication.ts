import { useCallback, useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  contentAdminApi,
  type ReadingPublicationStatus,
} from "@/features/contents/api/content-admin";
import { queryKeys } from "@/lib/query-keys";

export const readingPublicationPollInterval = (
  state?: string,
  failed = false,
) => (!failed && (state === "PENDING" || state === "PREPARING") ? 2000 : false);

/** Dil durumu ayrı cache'te tutulur; polling açık editörleri sıfırlamaz. */
export function useReadingPublication(
  contentId: number,
  language: string,
  enabled: boolean,
) {
  const client = useQueryClient();
  const queryKey = queryKeys.contents.readingPublication(contentId, language);
  const query = useQuery({
    queryKey,
    enabled,
    retry: false,
    queryFn: ({ signal }) =>
      contentAdminApi.getReadingPublication(contentId, language, signal),
    refetchInterval: (query) =>
      readingPublicationPollInterval(
        query.state.data?.state,
        query.state.status === "error",
      ),
  });
  const lastLive = useRef<string | null>(null);
  const live = query.data?.liveRevision;
  const liveKey = live
    ? `${query.data?.liveGeneration}:${live}:${query.data?.liveManifestDigest}`
    : null;
  useEffect(() => {
    if (liveKey && lastLive.current !== liveKey) {
      // Form reset'i yalnız temiz alanları yeniler; kullanıcının değişen seçimi korunur.
      void client.invalidateQueries({
        queryKey: queryKeys.contents.detail(contentId),
      });
    }
    lastLive.current = liveKey;
  }, [client, contentId, language, liveKey]);
  const acceptStatus = useCallback(
    (status: ReadingPublicationStatus) => {
      client.setQueryData(
        queryKeys.contents.readingPublication(contentId, language),
        status,
      );
    },
    [client, contentId, language],
  );
  const retry = useMutation({
    retry: false,
    mutationFn: () =>
      contentAdminApi.retryReadingPublication(contentId, language),
    onSuccess: (status) => {
      client.setQueryData(queryKey, status);
    },
  });
  return { ...query, retry, acceptStatus };
}
