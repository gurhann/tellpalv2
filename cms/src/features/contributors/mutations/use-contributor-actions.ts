import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  contributorAdminApi,
  type AdminContentContributorResponse,
  type AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContentContributor,
  mapAdminContributor,
  type ContentContributorViewModel,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
import type { ContributorFormValues } from "@/features/contributors/schema/contributor-schema";
import { queryKeys } from "@/lib/query-keys";

type UseContributorActionsOptions = {
  onCreateSuccess?: (contributor: AdminContributorResponse) => void;
  onRenameSuccess?: (contributor: AdminContributorResponse) => void;
  onAssignSuccess?: (assignment: AdminContentContributorResponse) => void;
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

function updateContentAssignmentsCache(
  assignments: ContentContributorViewModel[] | undefined,
  savedAssignment: AdminContentContributorResponse,
) {
  const nextAssignment = mapAdminContentContributor(savedAssignment);

  if (!assignments) {
    return [nextAssignment];
  }

  const existingIndex = assignments.findIndex(
    (assignment) =>
      assignment.contributorId === savedAssignment.contributorId &&
      assignment.role === savedAssignment.role &&
      assignment.languageCode === nextAssignment.languageCode,
  );

  if (existingIndex === -1) {
    return [...assignments, nextAssignment];
  }

  return assignments.map((assignment, index) =>
    index === existingIndex ? nextAssignment : assignment,
  );
}

export function useContributorActions({
  onCreateSuccess,
  onRenameSuccess,
  onAssignSuccess,
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
      queryClient.setQueryData<ContentContributorViewModel[]>(
        queryKeys.contributors.assignments(assignment.contentId),
        (assignments) => updateContentAssignmentsCache(assignments, assignment),
      );

      onAssignSuccess?.(assignment);
    },
  });

  return {
    createContributor,
    renameContributor,
    assignContributor,
    isPending:
      createContributor.isPending ||
      renameContributor.isPending ||
      assignContributor.isPending,
  };
}
