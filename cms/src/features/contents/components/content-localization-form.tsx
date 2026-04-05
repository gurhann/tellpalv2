import { useMemo } from "react";
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
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import type { AdminContentLocalizationResponse } from "@/features/contents/api/content-admin";
import {
  mapAdminContentLocalization,
  type ContentLocalizationViewModel,
  type ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
import { useContentLocalizationActions } from "@/features/contents/mutations/use-content-localization-actions";
import {
  createContentLocalizationSchema,
  localizationStatusOptions,
  mapLocalizationToFormValues,
  processingStatusOptions,
  type ContentLocalizationFormValues,
} from "@/features/contents/schema/content-localization-schema";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type LanguageOption = {
  code: string;
  label: string;
};

type ContentLocalizationFormProps = {
  content: ContentReadViewModel;
  mode: "create" | "update";
  initialValues: ContentLocalizationFormValues;
  localization?: ContentLocalizationViewModel;
  availableLanguages?: LanguageOption[];
  onSuccess?: (localization: AdminContentLocalizationResponse) => void;
  onCancel?: () => void;
};

function isProblemMappedToField(problem: ApiProblemDetail) {
  return (
    problem.errorCode === "content_localization_exists" ||
    Object.keys(getProblemFieldErrors(problem)).length > 0
  );
}

function getLanguageLabel(
  languageCode: string,
  availableLanguages: LanguageOption[],
  localization?: ContentLocalizationViewModel,
) {
  if (localization) {
    return localization.languageLabel;
  }

  return (
    availableLanguages.find((entry) => entry.code === languageCode)?.label ??
    languageCode.toUpperCase()
  );
}

function getGenericActionProblem(error: unknown): ApiProblemDetail {
  if (error instanceof Error && error.message.trim().length > 0) {
    return {
      type: "about:blank",
      title: "Request failed",
      status: 500,
      detail: error.message,
    };
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: "Localization changes could not be saved. Try again.",
  };
}

export function ContentLocalizationForm({
  content,
  mode,
  initialValues,
  localization,
  availableLanguages = [],
  onSuccess,
  onCancel,
}: ContentLocalizationFormProps) {
  const form = useZodForm<ContentLocalizationFormValues>({
    schema: useMemo(
      () => createContentLocalizationSchema(content.summary.type),
      [content.summary.type],
    ),
    defaultValues: initialValues,
  });
  const { saveLocalization } = useContentLocalizationActions(
    content.summary.id,
  );
  const status = form.watch("status");
  const saveProblem =
    saveLocalization.error instanceof ApiClientError
      ? saveLocalization.error.problem
      : null;
  const alertProblem =
    saveProblem && !isProblemMappedToField(saveProblem) ? saveProblem : null;
  const showStoryGuidance = content.summary.supportsStoryPages;
  const requiresBodyText =
    content.summary.type === "AUDIO_STORY" ||
    content.summary.type === "MEDITATION";
  const selectedLanguageCode = form.watch("languageCode");
  const selectedLanguageLabel = getLanguageLabel(
    selectedLanguageCode,
    availableLanguages,
    localization,
  );

  async function handleSubmit(values: ContentLocalizationFormValues) {
    form.clearErrors();
    saveLocalization.reset();

    try {
      const savedLocalization = await toastMutation(
        saveLocalization.mutateAsync({
          mode,
          values,
        }),
        {
          loading:
            mode === "create"
              ? "Creating localization..."
              : "Saving localization...",
          success:
            mode === "create" ? "Localization created." : "Localization saved.",
        },
      );

      const nextValues = mapLocalizationToFormValues(
        mapAdminContentLocalization(savedLocalization),
      );

      form.reset(nextValues);
      onSuccess?.(savedLocalization);
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "content_localization_exists") {
          form.setError("languageCode", {
            type: "server",
            message: "Language already exists for this content.",
          });
          return;
        }

        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: getGenericActionProblem(error).detail,
      });
    }
  }

  return (
    <form
      className="grid gap-5"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      {alertProblem ? <ProblemAlert problem={alertProblem} /> : null}
      <FieldError error={form.formState.errors.root?.serverError} />

      <div className="grid gap-5 md:grid-cols-2">
        {mode === "create" ? (
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">
              Language
            </label>
            <Controller
              control={form.control}
              name="languageCode"
              render={({ field }) => (
                <Select
                  disabled={saveLocalization.isPending}
                  value={field.value}
                  onValueChange={field.onChange}
                >
                  <SelectTrigger
                    aria-invalid={Boolean(form.formState.errors.languageCode)}
                    aria-label="Language"
                    className="w-full"
                  >
                    <SelectValue placeholder="Select language" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableLanguages.map((option) => (
                      <SelectItem key={option.code} value={option.code}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <FieldError error={form.formState.errors.languageCode} />
          </div>
        ) : (
          <div className="space-y-2">
            <p className="text-sm font-medium text-foreground">Language</p>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {selectedLanguageLabel}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Locale code: {selectedLanguageCode.toUpperCase()}
              </p>
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="title"
          >
            Title
          </label>
          <Input
            id="title"
            placeholder="Localized title"
            {...form.register("title")}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.title} />
        </div>

        <div className="space-y-2 md:col-span-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="description"
          >
            Description
          </label>
          <Textarea
            id="description"
            placeholder="Short editorial summary for this locale"
            {...form.register("description")}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.description} />
        </div>

        {showStoryGuidance ? (
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900 md:col-span-2">
            Story localizations keep narrative copy and per-page audio on the
            story pages child route. This form only edits locale metadata,
            status, and asset bindings that live at content level.
          </div>
        ) : (
          <>
            <div className="space-y-2 md:col-span-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="bodyText"
              >
                Body text
                {requiresBodyText ? (
                  <span className="ml-1 text-destructive">*</span>
                ) : null}
              </label>
              <Textarea
                id="bodyText"
                placeholder="Localized body copy"
                {...form.register("bodyText")}
                disabled={saveLocalization.isPending}
              />
              <FieldError error={form.formState.errors.bodyText} />
            </div>

            <Controller
              control={form.control}
              name="audioMediaId"
              render={({ field }) => (
                <AssetPickerField
                  description="Upload a new audio file here or browse existing `AUDIO` assets for this localization."
                  disabled={saveLocalization.isPending}
                  error={form.formState.errors.audioMediaId}
                  id="audioMediaId"
                  label="Audio asset *"
                  mediaType="AUDIO"
                  pickerDescription="Select a recent audio asset for this content localization. Uploading from this field stays available, and manual asset ids remain under Advanced."
                  pickerTitle="Pick localization audio asset"
                  placeholder="Required"
                  value={field.value}
                  onChange={field.onChange}
                />
              )}
            />
          </>
        )}

        <Controller
          control={form.control}
          name="coverMediaId"
          render={({ field }) => (
            <AssetPickerField
              description="Upload a cover image here or browse existing `IMAGE` assets without leaving this editor."
              disabled={saveLocalization.isPending}
              error={form.formState.errors.coverMediaId}
              id="coverMediaId"
              label="Cover asset"
              mediaType="IMAGE"
              pickerDescription="Select a recent image asset for the localized cover. Uploading from this field stays available, and manual asset ids remain under Advanced."
              pickerTitle="Pick localization cover asset"
              placeholder="Optional"
              value={field.value}
              onChange={field.onChange}
            />
          )}
        />

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="durationMinutes"
          >
            Duration minutes
          </label>
          <Input
            id="durationMinutes"
            inputMode="numeric"
            placeholder="Optional"
            type="number"
            {...form.register("durationMinutes", {
              setValueAs: (value) => {
                if (value === "" || value === null || value === undefined) {
                  return null;
                }

                return Number(value);
              },
            })}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.durationMinutes} />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">Status</label>
          <Controller
            control={form.control}
            name="status"
            render={({ field }) => (
              <Select
                disabled={saveLocalization.isPending}
                value={field.value}
                onValueChange={field.onChange}
              >
                <SelectTrigger
                  aria-invalid={Boolean(form.formState.errors.status)}
                  aria-label="Status"
                  className="w-full"
                >
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  {localizationStatusOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldError error={form.formState.errors.status} />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">
            Processing status
          </label>
          <Controller
            control={form.control}
            name="processingStatus"
            render={({ field }) => (
              <Select
                disabled={saveLocalization.isPending}
                value={field.value}
                onValueChange={field.onChange}
              >
                <SelectTrigger
                  aria-invalid={Boolean(form.formState.errors.processingStatus)}
                  aria-label="Processing status"
                  className="w-full"
                >
                  <SelectValue placeholder="Select processing status" />
                </SelectTrigger>
                <SelectContent>
                  {processingStatusOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          <FieldError error={form.formState.errors.processingStatus} />
        </div>

        {status === "PUBLISHED" ? (
          <div className="space-y-2 md:col-span-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="publishedAt"
            >
              Published at
            </label>
            <Input
              id="publishedAt"
              type="datetime-local"
              {...form.register("publishedAt")}
              disabled={saveLocalization.isPending}
            />
            <p className="text-sm text-muted-foreground">
              Publishing requires a timestamp. Use this when backfilling an
              existing locale or preparing a manual publish window.
            </p>
            <FieldError error={form.formState.errors.publishedAt} />
          </div>
        ) : null}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
        {onCancel ? (
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={saveLocalization.isPending}
          >
            Cancel
          </Button>
        ) : null}
        <SubmitButton
          isPending={saveLocalization.isPending}
          pendingLabel={
            mode === "create"
              ? "Creating localization..."
              : "Saving localization..."
          }
        >
          {mode === "create" ? "Create localization" : "Save localization"}
        </SubmitButton>
      </div>
    </form>
  );
}
