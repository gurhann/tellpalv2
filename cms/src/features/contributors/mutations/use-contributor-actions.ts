import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  contributorAdminApi,
  type AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContributor,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
import type { ContributorFormValues } from "@/features/contributors/schema/contributor-schema";
import { queryKeys } from "@/lib/query-keys";

type UseContributorActionsOptions = {
  onCreateSuccess?: (contributor: AdminContributorResponse) => void;
  onRenameSuccess?: (contributor: AdminContributorResponse) => void;
};

function updateContributorListCache(
  records: ContributorViewModel[] | undefined,
  savedContributor: AdminContributorResponse,
) {
  const nextContributor = mapAdminContributor(savedContributor);

  if (!records) {
    return [nextContributor];
  }

  const existingIndex = records.findIndex(
    (contributor) => contributor.id === savedContributor.contributorId,
  );

  if (existingIndex === -1) {
    return [nextContributor, ...records].slice(0, Math.max(records.length, 1));
  }

  return records.map((contributor) =>
    contributor.id === savedContributor.contributorId
      ? nextContributor
      : contributor,
  );
}

export function useContributorActions({
  onCreateSuccess,
  onRenameSuccess,
}: UseContributorActionsOptions = {}) {
  const queryClient = useQueryClient();

  async function invalidateContributorQueries(contributorId?: number) {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.contributors.lists(),
      }),
      ...(typeof contributorId === "number"
        ? [
            queryClient.invalidateQueries({
              queryKey: queryKeys.contributors.detail(contributorId),
            }),
          ]
        : []),
    ]);
  }

  const createContributor = useMutation({
    mutationFn: async (values: ContributorFormValues) =>
      contributorAdminApi.createContributor({
        displayName: values.displayName.trim(),
      }),
    onSuccess: async (contributor) => {
      queryClient.setQueriesData<ContributorViewModel[]>(
        { queryKey: queryKeys.contributors.lists() },
        (records) => updateContributorListCache(records, contributor),
      );
      queryClient.setQueryData<ContributorViewModel>(
        queryKeys.contributors.detail(contributor.contributorId),
        mapAdminContributor(contributor),
      );

      await invalidateContributorQueries(contributor.contributorId);
      onCreateSuccess?.(contributor);
    },
  });

  const renameContributor = useMutation({
    mutationFn: async ({
      contributorId,
      values,
    }: {
      contributorId: number;
      values: ContributorFormValues;
    }) =>
      contributorAdminApi.renameContributor(contributorId, {
        displayName: values.displayName.trim(),
      }),
    onSuccess: async (contributor) => {
      queryClient.setQueriesData<ContributorViewModel[]>(
        { queryKey: queryKeys.contributors.lists() },
        (records) => updateContributorListCache(records, contributor),
      );
      queryClient.setQueryData<ContributorViewModel>(
        queryKeys.contributors.detail(contributor.contributorId),
        mapAdminContributor(contributor),
      );

      await invalidateContributorQueries(contributor.contributorId);
      onRenameSuccess?.(contributor);
    },
  });

  return {
    createContributor,
    renameContributor,
    isPending: createContributor.isPending || renameContributor.isPending,
  };
}
