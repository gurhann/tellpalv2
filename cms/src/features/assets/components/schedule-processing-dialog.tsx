import { useEffect, useMemo, useState } from "react";
import { Controller } from "react-hook-form";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import type { AssetProcessingJobViewModel } from "@/features/assets/model/asset-view-model";
import { useProcessingActions } from "@/features/assets/mutations/use-processing-actions";
import {
  getScheduleProcessingFormDefaults,
  scheduleProcessingFormSchema,
  type ScheduleProcessingFormValues,
} from "@/features/assets/schema/processing-schema";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type ScheduleProcessingDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contents: ContentReadViewModel[];
  initialContentId?: number | null;
  initialLanguageCode?: string | null;
  contentProblem?: ApiProblemDetail | null;
  onScheduled?: (job: AssetProcessingJobViewModel) => void;
};

function getContentLabel(content: ContentReadViewModel) {
  return content.primaryLocalization?.title ?? content.summary.externalKey;
}

export function ScheduleProcessingDialog({
  open,
  onOpenChange,
  contents,
  initialContentId = null,
  initialLanguageCode = null,
  contentProblem = null,
  onScheduled,
}: ScheduleProcessingDialogProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          title: "Processing schedule et",
          description:
            "Bir content localization secin, ture bagli alanlari otomatik hydrate edin ve gerekliyse kaynak assetleri baglayin.",
          content: "Icerik",
          contentPlaceholder: "Icerik secin",
          language: "Hedef dil",
          languagePlaceholder: "Dil secin",
          type: "Content type",
          externalKey: "External key",
          pageCount: "Page count",
          noLocalizations:
            "Secilen icerik icin kullanilabilir localization bulunmuyor. Bu rota yalnizca mevcut localization'lari schedule eder.",
          noContent:
            "Liste kullanilabilir olur olmaz schedule formu acilacak. Simdilik content registry yüklenemedi.",
          coverLabel: "Cover source asset",
          coverDescription:
            "Yeni package akisi icin opsiyonel kapak kaynagi baglayin. IMAGE picker mevcut medya kitapligini kullanir.",
          coverPickerTitle: "Cover source asset sec",
          coverPickerDescription:
            "Schedule edilen localization icin kullanilacak gorsel kaynagini secin.",
          audioLabel: "Audio source asset",
          audioDescription:
            "Story disi tiplerde ses kaynagi zorunludur. AUDIO picker mevcut medya kitapligini kullanir.",
          audioPickerTitle: "Audio source asset sec",
          audioPickerDescription:
            "Schedule edilen localization icin kullanilacak ses kaynagini secin.",
          nonStoryAudioRequired:
            "Story disi content tipleri icin audio source asset zorunludur.",
          storyPageCountMissing:
            "Story processing icin page count gerekli, ancak secilen kayitta yok.",
          scheduleLoading: "Processing schedule ediliyor...",
          schedulePending: "Schedule ediliyor...",
          scheduleSuccess: "Processing job schedule edildi.",
          scheduleAction: "Schedule processing",
          cancel: "Cancel",
          optional: "Optional",
          readOnly: "Salt okunur, icerikten hydrate edilir.",
        }
      : {
          title: "Schedule processing",
          description:
            "Select one content localization, hydrate derived fields automatically, and bind source assets only when needed.",
          content: "Content",
          contentPlaceholder: "Select content",
          language: "Target language",
          languagePlaceholder: "Select language",
          type: "Content type",
          externalKey: "External key",
          pageCount: "Page count",
          noLocalizations:
            "The selected content has no available localizations. This route schedules only existing localizations.",
          noContent:
            "The form will open as soon as the registry is available. Right now the content registry could not be loaded.",
          coverLabel: "Cover source asset",
          coverDescription:
            "Optionally bind a cover source for the next packaging run. The IMAGE picker uses the existing media library.",
          coverPickerTitle: "Pick cover source asset",
          coverPickerDescription:
            "Choose the image source asset that should be used for this scheduled localization.",
          audioLabel: "Audio source asset",
          audioDescription:
            "Non-story content requires an audio source asset. The AUDIO picker uses the existing media library.",
          audioPickerTitle: "Pick audio source asset",
          audioPickerDescription:
            "Choose the audio source asset that should be used for this scheduled localization.",
          nonStoryAudioRequired:
            "Non-story content types require an audio source asset.",
          storyPageCountMissing:
            "Story processing requires a page count, but the selected record has none.",
          scheduleLoading: "Scheduling processing...",
          schedulePending: "Scheduling...",
          scheduleSuccess: "Processing job scheduled.",
          scheduleAction: "Schedule processing",
          cancel: "Cancel",
          optional: "Optional",
          readOnly: "Read-only, hydrated from the selected content.",
        };
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const form = useZodForm<ScheduleProcessingFormValues>({
    schema: scheduleProcessingFormSchema,
    defaultValues: getScheduleProcessingFormDefaults(
      initialContentId,
      initialLanguageCode,
    ),
  });
  const processingActions = useProcessingActions();
  const selectedContentId = form.watch("selectedContentId");
  const selectedContent = useMemo(
    () =>
      contents.find((content) => content.summary.id === selectedContentId) ??
      null,
    [contents, selectedContentId],
  );
  const languageOptions = selectedContent?.localizations ?? [];
  const requiresAudio = selectedContent
    ? !selectedContent.summary.supportsStoryPages
    : false;

  useEffect(() => {
    if (!open) {
      return;
    }

    form.reset(
      getScheduleProcessingFormDefaults(initialContentId, initialLanguageCode),
    );
    setProblem(null);
  }, [form, initialContentId, initialLanguageCode, open]);

  useEffect(() => {
    if (!selectedContent) {
      return;
    }

    const currentLanguageCode = form.getValues("languageCode");
    const languageStillExists = languageOptions.some(
      (localization) => localization.languageCode === currentLanguageCode,
    );

    if (!languageStillExists) {
      form.setValue("languageCode", languageOptions[0]?.languageCode ?? "", {
        shouldValidate: true,
      });
    }
  }, [form, languageOptions, selectedContent]);

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.clearErrors();
      setProblem(null);
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: ScheduleProcessingFormValues) {
    form.clearErrors();
    setProblem(null);

    if (!selectedContent) {
      form.setError("selectedContentId", {
        type: "manual",
        message: "Select a content record.",
      });
      return;
    }

    if (requiresAudio && values.audioSourceAssetId === null) {
      form.setError("audioSourceAssetId", {
        type: "manual",
        message: copy.nonStoryAudioRequired,
      });
      return;
    }

    if (
      selectedContent.summary.supportsStoryPages &&
      selectedContent.summary.pageCount === null
    ) {
      form.setError("selectedContentId", {
        type: "manual",
        message: copy.storyPageCountMissing,
      });
      return;
    }

    try {
      const job = await toastMutation(
        processingActions.scheduleProcessing.mutateAsync({
          contentId: selectedContent.summary.id,
          languageCode: values.languageCode,
          contentType: selectedContent.summary.type,
          externalKey: selectedContent.summary.externalKey,
          coverSourceAssetId: values.coverSourceAssetId,
          audioSourceAssetId: values.audioSourceAssetId,
          pageCount: selectedContent.summary.supportsStoryPages
            ? (selectedContent.summary.pageCount ?? 0)
            : undefined,
        }),
        {
          loading: copy.scheduleLoading,
          success: copy.scheduleSuccess,
        },
      );

      onScheduled?.(job);
      handleOpenChange(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        const mapped = applyProblemDetailToForm(form.setError, error.problem);

        if (!mapped) {
          setProblem(error.problem);
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message:
          error instanceof Error && error.message.trim().length > 0
            ? error.message
            : copy.scheduleLoading,
      });
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{copy.title}</DialogTitle>
          <DialogDescription>{copy.description}</DialogDescription>
        </DialogHeader>

        <DialogBody>
          <form
            className="grid gap-5"
            noValidate
            onSubmit={form.handleSubmit(handleSubmit)}
          >
            {contentProblem ? <ProblemAlert problem={contentProblem} /> : null}
            {problem ? <ProblemAlert problem={problem} /> : null}
            <FieldError error={form.formState.errors.root?.serverError} />

            <div className="grid gap-5 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  {copy.content}
                </label>
                <Controller
                  control={form.control}
                  name="selectedContentId"
                  render={({ field }) => (
                    <Select
                      value={
                        typeof field.value === "number"
                          ? field.value.toString()
                          : ""
                      }
                      onValueChange={(value) => field.onChange(Number(value))}
                    >
                      <SelectTrigger className="w-full" aria-label={copy.content}>
                        <SelectValue placeholder={copy.contentPlaceholder} />
                      </SelectTrigger>
                      <SelectContent>
                        {contents.map((content) => (
                          <SelectItem
                            key={content.summary.id}
                            value={content.summary.id.toString()}
                          >
                            {getContentLabel(content)} /{" "}
                            {content.summary.externalKey}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                <p className="text-sm text-muted-foreground">
                  {contents.length === 0 ? copy.noContent : copy.readOnly}
                </p>
                <FieldError error={form.formState.errors.selectedContentId} />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  {copy.language}
                </label>
                <Controller
                  control={form.control}
                  name="languageCode"
                  render={({ field }) => (
                    <Select
                      disabled={languageOptions.length === 0}
                      value={field.value}
                      onValueChange={field.onChange}
                    >
                      <SelectTrigger className="w-full" aria-label={copy.language}>
                        <SelectValue placeholder={copy.languagePlaceholder} />
                      </SelectTrigger>
                      <SelectContent>
                        {languageOptions.map((localization) => (
                          <SelectItem
                            key={localization.languageCode}
                            value={localization.languageCode}
                          >
                            {localization.languageLabel}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {selectedContent && languageOptions.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {copy.noLocalizations}
                  </p>
                ) : (
                  <p className="text-sm text-muted-foreground">{copy.readOnly}</p>
                )}
                <FieldError error={form.formState.errors.languageCode} />
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium text-foreground">{copy.type}</p>
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-3 text-sm text-foreground">
                  {selectedContent?.summary.typeLabel ?? "—"}
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium text-foreground">
                  {copy.externalKey}
                </p>
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-3 text-sm text-foreground">
                  {selectedContent?.summary.externalKey ?? "—"}
                </div>
              </div>

              {selectedContent?.summary.supportsStoryPages ? (
                <div className="space-y-2 md:col-span-2">
                  <p className="text-sm font-medium text-foreground">
                    {copy.pageCount}
                  </p>
                  <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-3 text-sm text-foreground">
                    {selectedContent.summary.pageCount ?? "—"}
                  </div>
                </div>
              ) : null}

              <Controller
                control={form.control}
                name="coverSourceAssetId"
                render={({ field }) => (
                  <AssetPickerField
                    description={copy.coverDescription}
                    disabled={processingActions.isPending}
                    error={form.formState.errors.coverSourceAssetId}
                    id="schedule-cover-source-asset-id"
                    label={copy.coverLabel}
                    mediaType="IMAGE"
                    pickerDescription={copy.coverPickerDescription}
                    pickerTitle={copy.coverPickerTitle}
                    placeholder={copy.optional}
                    value={field.value}
                    onChange={field.onChange}
                  />
                )}
              />

              <Controller
                control={form.control}
                name="audioSourceAssetId"
                render={({ field }) => (
                  <AssetPickerField
                    description={copy.audioDescription}
                    disabled={processingActions.isPending}
                    error={form.formState.errors.audioSourceAssetId}
                    id="schedule-audio-source-asset-id"
                    label={`${copy.audioLabel}${requiresAudio ? " *" : ""}`}
                    mediaType="AUDIO"
                    pickerDescription={copy.audioPickerDescription}
                    pickerTitle={copy.audioPickerTitle}
                    placeholder={copy.optional}
                    value={field.value}
                    onChange={field.onChange}
                  />
                )}
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={processingActions.isPending}
              >
                {copy.cancel}
              </Button>
              <SubmitButton
                isPending={processingActions.isPending}
                pendingLabel={copy.schedulePending}
              >
                {copy.scheduleAction}
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
