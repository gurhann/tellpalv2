import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  contentAdminApi,
  type AdminContentLocalizationResponse,
  type ContentType,
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
  contentType: ContentType;
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

  return createContentReadViewModel(
    record.summary,
    nextLocalizations,
    record.playback,
  );
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

export function toLocalizationPayload(
  values: ContentLocalizationFormValues,
  contentType: ContentType,
) {
  const common = {
    title: values.title.trim(),
    status: values.status,
    publishedAt: toPublishedAtPayload(values.publishedAt),
  };
  if (contentType === "LULLABY") return common;
  if (contentType === "STORY") {
    return {
      ...common,
      description: values.description,
      coverMediaId: values.coverMediaId,
      durationMinutes: values.durationMinutes,
      processingStatus: values.processingStatus,
      ...(values.narrationAudioMediaId != null &&
      values.narrationDurationMinutes != null
        ? {
            narration: {
              audioMediaId: values.narrationAudioMediaId,
              durationMinutes: values.narrationDurationMinutes,
            },
          }
        : {}),
    };
  }
  return {
    ...common,
    description: values.description,
    bodyText: values.bodyText,
    audioMediaId: values.audioMediaId,
    durationMinutes: values.durationMinutes,
    processingStatus: values.processingStatus,
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
    mutationFn: async ({ mode, values, contentType }: SaveLocalizationVariables) => {
      const payload = toLocalizationPayload(values, contentType);

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
