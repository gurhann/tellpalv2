import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  freeAccessAdminApi,
  type GrantFreeAccessInput,
} from "@/features/free-access/api/free-access-admin";
import {
  mapAdminFreeAccessGrant,
  type FreeAccessGrantViewModel,
} from "@/features/free-access/model/free-access-view-model";
import { queryKeys } from "@/lib/query-keys";

type RevokeFreeAccessVariables = {
  accessKey: string;
  languageCode: string;
  contentId: number;
};

export function useFreeAccessActions() {
  const queryClient = useQueryClient();

  const grantFreeAccess = useMutation({
    mutationFn: async (input: GrantFreeAccessInput) => {
      const response = await freeAccessAdminApi.grantFreeAccess(input);
      return mapAdminFreeAccessGrant(response);
    },
    onSuccess: async (_entry: FreeAccessGrantViewModel) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.freeAccess.lists(),
      });
    },
  });

  const revokeFreeAccess = useMutation({
    mutationFn: async ({
      accessKey,
      languageCode,
      contentId,
    }: RevokeFreeAccessVariables) => {
      await freeAccessAdminApi.revokeFreeAccess(
        accessKey,
        languageCode,
        contentId,
      );
      return {
        accessKey,
        languageCode,
        contentId,
      };
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.freeAccess.lists(),
      });
    },
  });

  return {
    grantFreeAccess,
    revokeFreeAccess,
    isPending: grantFreeAccess.isPending || revokeFreeAccess.isPending,
  };
}
