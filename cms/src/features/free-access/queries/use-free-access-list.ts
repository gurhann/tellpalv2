import { useQuery } from "@tanstack/react-query";

import { freeAccessAdminApi } from "@/features/free-access/api/free-access-admin";
import {
  mapAdminFreeAccessGrantList,
  type FreeAccessGrantViewModel,
} from "@/features/free-access/model/free-access-view-model";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

function getApiProblem(error: unknown, fallbackDetail: string) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: error instanceof Error ? error.message : fallbackDetail,
  } satisfies ApiProblemDetail;
}

export function useFreeAccessList(accessKeyInput = "") {
  const normalizedAccessKey = accessKeyInput.trim();
  const isDefaultScope = normalizedAccessKey.length === 0;
  const effectiveAccessKey = isDefaultScope ? "default" : normalizedAccessKey;
  const query = useQuery({
    queryKey: queryKeys.freeAccess.list({ accessKey: effectiveAccessKey }),
    queryFn: async () => {
      const response = await freeAccessAdminApi.listFreeAccessEntries(
        isDefaultScope ? undefined : normalizedAccessKey,
      );
      return mapAdminFreeAccessGrantList(response);
    },
  });

  return {
    ...query,
    entries: query.data ?? ([] as FreeAccessGrantViewModel[]),
    requestedAccessKey: normalizedAccessKey,
    effectiveAccessKey,
    isDefaultScope,
    problem: getApiProblem(
      query.error,
      "The free access registry could not be loaded from the admin API.",
    ),
  };
}
