import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { AdminContentResponse } from "@/features/contents/api/content-admin";
import { contentAdminApi } from "@/features/contents/api/content-admin";
import {
  mapAdminContentSummaryToRead,
  type ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
import type { ContentFormValues } from "@/features/contents/schema/content-schema";
import { queryKeys } from "@/lib/query-keys";

type UseSaveContentOptions =
  | {
      mode: "create";
      onSuccess?: (content: AdminContentResponse) => void;
    }
  | {
      mode: "update";
      contentId: number;
      onSuccess?: (content: AdminContentResponse) => void;
    };

function updateContentListCache(
  records: ContentReadViewModel[] | undefined,
  savedContent: AdminContentResponse,
) {
  const nextRecord = mapAdminContentSummaryToRead(savedContent);

  if (!records) {
    return [nextRecord];
  }

  const existingIndex = records.findIndex(
    (record) => record.summary.id === savedContent.contentId,
  );

  if (existingIndex === -1) {
    return [nextRecord, ...records];
  }

  return records.map((record) =>
    record.summary.id === savedContent.contentId
      ? mapAdminContentSummaryToRead(savedContent, record.localizations)
      : record,
  );
}

function updateContentDetailCache(
  existingRecord: ContentReadViewModel | undefined,
  savedContent: AdminContentResponse,
) {
  return mapAdminContentSummaryToRead(
    savedContent,
    existingRecord?.localizations,
  );
}

export function useSaveContent(options: UseSaveContentOptions) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (values: ContentFormValues) => {
      if (options.mode === "create") {
        return contentAdminApi.createContent({
          type: values.type,
          externalKey: values.externalKey.trim(),
          ageRange: values.ageRange,
          active: values.active,
        });
      }

      return contentAdminApi.updateContent(options.contentId, {
        externalKey: values.externalKey.trim(),
        ageRange: values.ageRange,
        active: values.active,
      });
    },
    onSuccess: async (savedContent) => {
      const detailKey = queryKeys.contents.detail(savedContent.contentId);

      queryClient.setQueriesData<ContentReadViewModel[]>(
        { queryKey: queryKeys.contents.lists() },
        (records) => updateContentListCache(records, savedContent),
      );
      queryClient.setQueryData<ContentReadViewModel>(detailKey, (record) =>
        updateContentDetailCache(record, savedContent),
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.contents.lists(),
        }),
        queryClient.invalidateQueries({
          queryKey: detailKey,
        }),
      ]);

      options.onSuccess?.(savedContent);
    },
  });
}
