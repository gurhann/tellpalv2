import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { contentAdminApi } from "@/features/contents/api/content-admin";
import { queryKeys } from "@/lib/query-keys";

export const readingPublicationPollInterval = (
  state?: string,
  failed = false,
) => (!failed && (state === "PENDING" || state === "PREPARING") ? 2000 : false);

/** Exact language feedback lives outside the detail cache so polling cannot reset editors. */
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
  const retry = useMutation({
    retry: false,
    mutationFn: () =>
      contentAdminApi.retryReadingPublication(contentId, language),
    onSuccess: (status) => {
      client.setQueryData(queryKey, status);
    },
  });
  return { ...query, retry };
}
