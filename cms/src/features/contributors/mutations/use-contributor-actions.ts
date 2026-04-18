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
  onDeleteSuccess?: (contributorId: number) => void;
  onAssignSuccess?: (contentId: number) => void;
  onUnassignSuccess?: (contentId: number) => void;
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
  onDeleteSuccess,
  onAssignSuccess,
  onUnassignSuccess,
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

  const deleteContributor = useMutation({
    mutationFn: async ({ contributorId }: { contributorId: number }) => {
      await contributorAdminApi.deleteContributor(contributorId);
      return contributorId;
    },
    onSuccess: async (contributorId) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.contributors.lists(),
        }),
        queryClient.removeQueries({
          queryKey: queryKeys.contributors.detail(contributorId),
        }),
      ]);
      onDeleteSuccess?.(contributorId);
    },
  });

  const assignContributor = useMutation({
    mutationFn: async ({
      contentId,
      values,
    }: {
      contentId: number;
      values: {
        contributorId: number;
        role: Parameters<
          typeof contributorAdminApi.assignContributor
        >[1]["role"];
        languageCode?: string | null;
        creditName: string | null;
        sortOrder: number;
      };
    }) => contributorAdminApi.assignContributor(contentId, values),
    onSuccess: async (assignment) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.contributors.assignments(assignment.contentId),
      });
      onAssignSuccess?.(assignment.contentId);
    },
  });

  const unassignContributor = useMutation({
    mutationFn: async ({
      contentId,
      values,
    }: {
      contentId: number;
      values: {
        contributorId: number;
        role: Parameters<
          typeof contributorAdminApi.unassignContributor
        >[1]["role"];
        languageCode?: string | null;
      };
    }) => {
      await contributorAdminApi.unassignContributor(contentId, values);
      return contentId;
    },
    onSuccess: async (contentId) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.contributors.assignments(contentId),
      });
      onUnassignSuccess?.(contentId);
    },
  });

  return {
    createContributor,
    renameContributor,
    deleteContributor,
    assignContributor,
    unassignContributor,
    isPending:
      createContributor.isPending ||
      renameContributor.isPending ||
      deleteContributor.isPending ||
      assignContributor.isPending ||
      unassignContributor.isPending,
  };
}
