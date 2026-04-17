import { useEffect, useState } from "react";
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
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import type { AssetProcessingJobViewModel } from "@/features/assets/model/asset-view-model";
import { useProcessingActions } from "@/features/assets/mutations/use-processing-actions";
import {
  getRetryProcessingFormDefaults,
  retryProcessingFormSchema,
  type RetryProcessingFormValues,
} from "@/features/assets/schema/processing-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type RetryProcessingDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  job: AssetProcessingJobViewModel | null;
  onRetried?: (job: AssetProcessingJobViewModel) => void;
};

export function RetryProcessingDialog({
  open,
  onOpenChange,
  job,
  onRetried,
}: RetryProcessingDialogProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          title: "Processing retry",
          description:
            "FAILED kaydini ayni content ve localization baglaminda yeniden calistirin. Gerekirse source asset'leri degistirin.",
          content: "Icerik",
          language: "Dil",
          type: "Tur",
          attempts: "Deneme",
          lastError: "Son hata",
          coverLabel: "Cover source asset",
          coverDescription:
            "Ayni gorsel kaynagi kullanabilir veya yeni bir IMAGE asset secerek retry baglamini guncelleyebilirsiniz.",
          coverPickerTitle: "Retry cover source asset sec",
          coverPickerDescription:
            "Retry icin kullanilacak gorsel kaynagini secin.",
          audioLabel: "Audio source asset",
          audioDescription:
            "Story disi tiplerde audio source asset zorunludur. Gerekirse burada degistirin.",
          audioPickerTitle: "Retry audio source asset sec",
          audioPickerDescription:
            "Retry icin kullanilacak ses kaynagini secin.",
          audioRequired:
            "Story disi content tipleri icin audio source asset zorunludur.",
          retryLoading: "Processing retry baslatiliyor...",
          retryPending: "Retry baslatiliyor...",
          retrySuccess: "Processing retry baslatildi.",
          retryAction: "Retry processing",
          cancel: "Cancel",
          unavailable:
            "Retry dialogu yalnizca FAILED job kayitlari icin acilir.",
          optional: "Optional",
        }
      : {
          title: "Retry processing",
          description:
            "Run a FAILED record again in the same content and localization context. Update source assets when needed.",
          content: "Content",
          language: "Language",
          type: "Type",
          attempts: "Attempts",
          lastError: "Latest failure",
          coverLabel: "Cover source asset",
          coverDescription:
            "Keep the same image source or switch to another IMAGE asset before retrying.",
          coverPickerTitle: "Pick retry cover source asset",
          coverPickerDescription:
            "Choose the image source asset that should be used for this retry.",
          audioLabel: "Audio source asset",
          audioDescription:
            "Non-story content requires an audio source asset. Update it here when needed.",
          audioPickerTitle: "Pick retry audio source asset",
          audioPickerDescription:
            "Choose the audio source asset that should be used for this retry.",
          audioRequired:
            "Non-story content types require an audio source asset.",
          retryLoading: "Starting processing retry...",
          retryPending: "Retrying...",
          retrySuccess: "Processing retry started.",
          retryAction: "Retry processing",
          cancel: "Cancel",
          unavailable: "The retry dialog is available only for FAILED jobs.",
          optional: "Optional",
        };
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const form = useZodForm<RetryProcessingFormValues>({
    schema: retryProcessingFormSchema,
    defaultValues: getRetryProcessingFormDefaults(
      job?.coverAssetId,
      job?.audioAssetId,
    ),
  });
  const processingActions = useProcessingActions();
  const requiresAudio = job ? job.contentType !== "STORY" : false;

  useEffect(() => {
    if (!open) {
      return;
    }

    form.reset(
      getRetryProcessingFormDefaults(job?.coverAssetId, job?.audioAssetId),
    );
    setProblem(null);
  }, [form, job?.audioAssetId, job?.coverAssetId, open]);

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.clearErrors();
      setProblem(null);
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: RetryProcessingFormValues) {
    form.clearErrors();
    setProblem(null);

    if (!job || !job.canRetry) {
      form.setError("root.serverError", {
        type: "server",
        message: copy.unavailable,
      });
      return;
    }

    if (requiresAudio && values.audioSourceAssetId === null) {
      form.setError("audioSourceAssetId", {
        type: "manual",
        message: copy.audioRequired,
      });
      return;
    }

    try {
      const nextJob = await toastMutation(
        processingActions.retryProcessing.mutateAsync({
          contentId: job.contentId,
          languageCode: job.languageCode,
          input: {
            contentType: job.contentType,
            externalKey: job.externalKey,
            coverSourceAssetId: values.coverSourceAssetId,
            audioSourceAssetId: values.audioSourceAssetId,
            pageCount: job.contentType === "STORY" ? job.pageCount : undefined,
          },
        }),
        {
          loading: copy.retryLoading,
          success: copy.retrySuccess,
        },
      );

      onRetried?.(nextJob);
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
            : copy.retryLoading,
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
            {problem ? <ProblemAlert problem={problem} /> : null}
            <FieldError error={form.formState.errors.root?.serverError} />

            {!job || !job.canRetry ? (
              <ProblemAlert
                description={copy.unavailable}
                title={locale === "tr" ? "Retry kapali" : "Retry unavailable"}
              />
            ) : (
              <>
                <div className="grid gap-5 md:grid-cols-2">
                  <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                      {copy.content}
                    </p>
                    <p className="mt-2 text-sm font-medium text-foreground">
                      #{job.contentId}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {job.externalKey}
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                      {copy.language}
                    </p>
                    <p className="mt-2 text-sm font-medium text-foreground">
                      {job.languageLabel}
                    </p>
                    <p className="mt-1 text-sm uppercase text-muted-foreground">
                      {job.languageCode}
                    </p>
                  </div>

                  <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                      {copy.type}
                    </p>
                    <p className="mt-2 text-sm font-medium text-foreground">
                      {job.contentTypeLabel}
                    </p>
                    {job.pageCount !== null ? (
                      <p className="mt-1 text-sm text-muted-foreground">
                        Page count: {job.pageCount}
                      </p>
                    ) : null}
                  </div>

                  <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                      {copy.attempts}
                    </p>
                    <p className="mt-2 text-sm font-medium text-foreground">
                      {job.attemptCount}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {job.statusLabel}
                    </p>
                  </div>
                </div>

                {job.lastErrorCode || job.lastErrorMessage ? (
                  <div className="rounded-2xl border border-rose-200 bg-rose-50/60 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-rose-700">
                      {copy.lastError}
                    </p>
                    <p className="mt-2 text-sm font-medium text-rose-900">
                      {job.lastErrorCode ?? "No code"}
                    </p>
                    <p className="mt-1 text-sm leading-6 text-rose-800">
                      {job.lastErrorMessage ?? "No detailed message was stored."}
                    </p>
                  </div>
                ) : null}

                <div className="grid gap-5 md:grid-cols-2">
                  <Controller
                    control={form.control}
                    name="coverSourceAssetId"
                    render={({ field }) => (
                      <AssetPickerField
                        description={copy.coverDescription}
                        disabled={processingActions.isPending}
                        error={form.formState.errors.coverSourceAssetId}
                        id="retry-cover-source-asset-id"
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
                        id="retry-audio-source-asset-id"
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
              </>
            )}

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
                disabled={!job?.canRetry}
                isPending={processingActions.isPending}
                pendingLabel={copy.retryPending}
              >
                {copy.retryAction}
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
