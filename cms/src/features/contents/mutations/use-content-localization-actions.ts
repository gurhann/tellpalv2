import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  contentAdminApi,
  type AdminContentLocalizationResponse,
} from "@/features/contents/api/content-admin";
import {
  createContentReadViewModel,
  mapAdminContentLocalization,
  type ContentLocalizationViewModel,
  type ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
import type { ContentLocalizationFormValues } from "@/features/contents/schema/content-localization-schema";
import { toPublishedAtPayload } from "@/features/contents/schema/content-localization-schema";
import { queryKeys } from "@/lib/query-keys";

type SaveLocalizationVariables = {
  mode: "create" | "update";
  values: ContentLocalizationFormValues;
};

type PublishLocalizationVariables = {
  languageCode: string;
  publishedAt?: string | null;
};

function upsertLocalizationInRecord(
  record: ContentReadViewModel | undefined,
  localization: ContentLocalizationViewModel,
) {
  if (!record) {
    return record;
  }

  const existingIndex = record.localizations.findIndex(
    (entry) => entry.languageCode === localization.languageCode,
  );
  const nextLocalizations =
    existingIndex === -1
      ? [...record.localizations, localization]
      : record.localizations.map((entry, index) =>
          index === existingIndex ? localization : entry,
        );

  return createContentReadViewModel(record.summary, nextLocalizations);
}

function updateContentListRecords(
  records: ContentReadViewModel[] | undefined,
  localization: ContentLocalizationViewModel,
) {
  if (!records) {
    return records;
  }

  return records.map((record) =>
    record.summary.id === localization.contentId
      ? (upsertLocalizationInRecord(
          record,
          localization,
        ) as ContentReadViewModel)
      : record,
  );
}

function toLocalizationPayload(values: ContentLocalizationFormValues) {
  return {
    title: values.title.trim(),
    description: values.description,
    bodyText: values.bodyText,
    coverMediaId: values.coverMediaId,
    audioMediaId: values.audioMediaId,
    durationMinutes: values.durationMinutes,
    status: values.status,
    processingStatus: values.processingStatus,
    publishedAt: toPublishedAtPayload(values.publishedAt),
  };
}

export function useContentLocalizationActions(contentId: number) {
  const queryClient = useQueryClient();

  async function syncLocalizationCaches(
    response: AdminContentLocalizationResponse,
  ) {
    const localization = mapAdminContentLocalization(response);
    const detailKey = queryKeys.contents.detail(contentId);
    const localizationKey = queryKeys.contents.localization(
      contentId,
      localization.languageCode,
    );

    queryClient.setQueryData<ContentReadViewModel>(detailKey, (record) =>
      upsertLocalizationInRecord(record, localization),
    );
    queryClient.setQueriesData<ContentReadViewModel[]>(
      { queryKey: queryKeys.contents.lists() },
      (records) => updateContentListRecords(records, localization),
    );
    queryClient.setQueryData(localizationKey, localization);

    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.contents.lists(),
      }),
      queryClient.invalidateQueries({
        queryKey: detailKey,
      }),
      queryClient.invalidateQueries({
        queryKey: localizationKey,
      }),
    ]);
  }

  const saveLocalization = useMutation({
    mutationFn: async ({ mode, values }: SaveLocalizationVariables) => {
      const payload = toLocalizationPayload(values);

      if (mode === "create") {
        return contentAdminApi.createLocalization(
          contentId,
          values.languageCode,
          payload,
        );
      }

      return contentAdminApi.updateLocalization(
        contentId,
        values.languageCode,
        payload,
      );
    },
    onSuccess: syncLocalizationCaches,
  });

  const publishLocalization = useMutation({
    mutationFn: async ({
      languageCode,
      publishedAt,
    }: PublishLocalizationVariables) =>
      contentAdminApi.publishLocalization(contentId, languageCode, {
        publishedAt: toPublishedAtPayload(publishedAt ?? null),
      }),
    onSuccess: syncLocalizationCaches,
  });

  const archiveLocalization = useMutation({
    mutationFn: async (languageCode: string) =>
      contentAdminApi.archiveLocalization(contentId, languageCode),
    onSuccess: syncLocalizationCaches,
  });

  return {
    saveLocalization,
    publishLocalization,
    archiveLocalization,
  };
}
