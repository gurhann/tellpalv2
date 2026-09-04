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
  onUpdateSuccess?: (contentId: number) => void;
  onUnassignSuccess?: (contentId: number) => void;
};

function updateLegacyList(
  records: ContributorViewModel[] | undefined,
  saved: AdminContributorResponse,
) {
  const next = mapAdminContributor(saved);
  if (!records) return [next];
  const index = records.findIndex((item) => item.id === saved.contributorId);
  if (index < 0) return [next, ...records];
  return records.map((item, itemIndex) => (itemIndex === index ? next : item));
}

export function useContributorActions({
  onCreateSuccess,
  onRenameSuccess,
  onDeleteSuccess,
  onAssignSuccess,
  onUpdateSuccess,
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
        roles: values.roles,
      }),
    onSuccess: async (contributor) => {
      queryClient.setQueriesData(
        { queryKey: queryKeys.contributors.lists() },
        (current: unknown) =>
          Array.isArray(current)
            ? updateLegacyList(current as ContributorViewModel[], contributor)
            : current,
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
        roles: values.roles,
      }),
    onSuccess: async (contributor) => {
      queryClient.setQueriesData(
        { queryKey: queryKeys.contributors.lists() },
        (current: unknown) =>
          Array.isArray(current)
            ? updateLegacyList(current as ContributorViewModel[], contributor)
            : current,
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
        queryClient.invalidateQueries({
          queryKey: queryKeys.contributors.list(),
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

  const updateContributor = useMutation({
    mutationFn: async ({
      contentId,
      assignmentId,
      values,
    }: {
      contentId: number;
      assignmentId: number;
      values: Parameters<typeof contributorAdminApi.updateContributor>[2];
    }) =>
      contributorAdminApi.updateContributor(contentId, assignmentId, values),
    onSuccess: async (assignment) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.contributors.assignments(assignment.contentId),
      });
      onUpdateSuccess?.(assignment.contentId);
    },
  });

  const reorderContributors = useMutation({
    mutationFn: async ({
      contentId,
      role,
      languageCode,
      assignmentIds,
    }: {
      contentId: number;
      role: Parameters<
        typeof contributorAdminApi.reorderContentContributors
      >[1]["role"];
      languageCode?: string | null;
      assignmentIds: number[];
    }) =>
      contributorAdminApi.reorderContentContributors(contentId, {
        role,
        languageCode,
        assignmentIds,
      }),
    onSuccess: async (assignments) => {
      const contentId = assignments[0]?.contentId;
      if (contentId) {
        await queryClient.invalidateQueries({
          queryKey: queryKeys.contributors.assignments(contentId),
        });
      }
    },
  });

  return {
    createContributor,
    renameContributor,
    deleteContributor,
    assignContributor,
    unassignContributor,
    updateContributor,
    reorderContributors,
    isPending:
      createContributor.isPending ||
      renameContributor.isPending ||
      deleteContributor.isPending ||
      assignContributor.isPending ||
      unassignContributor.isPending ||
      updateContributor.isPending ||
      reorderContributors.isPending,
  };
}
