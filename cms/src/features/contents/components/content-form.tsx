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
import type { AdminContentResponse } from "@/features/contents/api/content-admin";
import { useSaveContent } from "@/features/contents/mutations/use-save-content";
import {
  contentFormSchema,
  contentTypeOptions,
  mapContentResponseToFormValues,
  type ContentFormValues,
} from "@/features/contents/schema/content-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type ContentFormProps = {
  mode: "create" | "update";
  contentId?: number;
  initialValues: ContentFormValues;
  onSuccess?: (content: AdminContentResponse) => void;
  onCancel?: () => void;
  submitLabel?: string;
  pendingLabel?: string;
};

function isProblemMappedToField(problem: ApiProblemDetail) {
  return (
    problem.errorCode === "duplicate_external_key" ||
    Object.keys(getProblemFieldErrors(problem)).length > 0
  );
}

export function ContentForm({
  mode,
  contentId,
  initialValues,
  onSuccess,
  onCancel,
  submitLabel,
  pendingLabel,
}: ContentFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          submitLabel: mode === "create" ? "Icerik olustur" : "Metadata kaydet",
          pendingLabel:
            mode === "create"
              ? "Icerik olusturuluyor..."
              : "Metadata kaydediliyor...",
          createLoading: "Icerik kaydi olusturuluyor...",
          updateLoading: "Icerik metadata'si kaydediliyor...",
          createSuccess: "Icerik kaydi olusturuldu.",
          updateSuccess: "Icerik metadata'si kaydedildi.",
          externalKeyDuplicate: "External key zaten kullaniliyor.",
          genericSaveError:
            "Icerik degisiklikleri kaydedilemedi. Tekrar deneyin.",
          contentType: "Icerik turu",
          selectContentType: "Icerik turu secin",
          contentTypeFixed:
            "Icerik turu olusturulduktan sonra sabittir. Bu form yalnizca external key, age range ve aktiflik durumunu gunceller.",
          externalKey: "External key",
          ageRange: "Yas araligi",
          optional: "Opsiyonel",
          availability: "Erisim durumu",
          active: "Aktif",
          inactive: "Pasif",
          inactiveHelp:
            "Pasif icerikler admin ekranlarinda gorunmeye devam eder.",
          cancel: "Iptal",
        }
      : {
          submitLabel: mode === "create" ? "Create content" : "Save metadata",
          pendingLabel:
            mode === "create" ? "Creating content..." : "Saving metadata...",
          createLoading: "Creating content record...",
          updateLoading: "Saving content metadata...",
          createSuccess: "Content record created.",
          updateSuccess: "Content metadata saved.",
          externalKeyDuplicate: "External key is already in use.",
          genericSaveError: "Content changes could not be saved. Try again.",
          contentType: "Content type",
          selectContentType: "Select content type",
          contentTypeFixed:
            "Content type is fixed after creation. This form updates external key, age range, and active state only.",
          externalKey: "External key",
          ageRange: "Age range",
          optional: "Optional",
          availability: "Availability",
          active: "Active",
          inactive: "Inactive",
          inactiveHelp: "Inactive content still appears in admin read screens.",
          cancel: "Cancel",
        };
  const form = useZodForm<ContentFormValues>({
    schema: contentFormSchema,
    defaultValues: initialValues,
  });
  const saveMutation = useSaveContent(
    mode === "create"
      ? {
          mode,
          onSuccess,
        }
      : {
          mode,
          contentId: contentId as number,
          onSuccess,
        },
  );
  const selectedType = form.watch("type");
  const saveProblem =
    saveMutation.error instanceof ApiClientError
      ? saveMutation.error.problem
      : null;
  const alertProblem =
    saveProblem && !isProblemMappedToField(saveProblem) ? saveProblem : null;

  async function handleSubmit(values: ContentFormValues) {
    form.clearErrors();
    saveMutation.reset();

    try {
      const savedContent = await toastMutation(
        saveMutation.mutateAsync(values),
        {
          loading: mode === "create" ? copy.createLoading : copy.updateLoading,
          success: mode === "create" ? copy.createSuccess : copy.updateSuccess,
        },
      );

      form.reset(mapContentResponseToFormValues(savedContent));
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "duplicate_external_key") {
          form.setError("externalKey", {
            type: "server",
            message: copy.externalKeyDuplicate,
          });
          return;
        }

        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: copy.genericSaveError,
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
              {copy.contentType}
            </label>
            <Controller
              control={form.control}
              name="type"
              render={({ field }) => (
                <Select
                  disabled={saveMutation.isPending}
                  value={field.value}
                  onValueChange={field.onChange}
                >
                  <SelectTrigger
                    aria-invalid={Boolean(form.formState.errors.type)}
                    aria-label={copy.contentType}
                    className="w-full"
                  >
                    <SelectValue placeholder={copy.selectContentType} />
                  </SelectTrigger>
                  <SelectContent>
                    {contentTypeOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <FieldError error={form.formState.errors.type} />
          </div>
        ) : (
          <div className="space-y-2">
            <p className="text-sm font-medium text-foreground">
              {copy.contentType}
            </p>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {contentTypeOptions.find(
                  (option) => option.value === selectedType,
                )?.label ?? selectedType}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {copy.contentTypeFixed}
              </p>
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="externalKey"
          >
            {copy.externalKey}
          </label>
          <Input
            id="externalKey"
            placeholder="story.evening-garden"
            {...form.register("externalKey")}
            disabled={saveMutation.isPending}
          />
          <FieldError error={form.formState.errors.externalKey} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="ageRange"
          >
            {copy.ageRange}
          </label>
          <Input
            id="ageRange"
            inputMode="numeric"
            min={0}
            placeholder={copy.optional}
            type="number"
            {...form.register("ageRange", {
              setValueAs: (value) => {
                if (value === "" || value === null || value === undefined) {
                  return null;
                }

                return Number(value);
              },
            })}
            disabled={saveMutation.isPending}
          />
          <FieldError error={form.formState.errors.ageRange} />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">
            {copy.availability}
          </label>
          <Controller
            control={form.control}
            name="active"
            render={({ field }) => (
              <Select
                disabled={saveMutation.isPending}
                value={field.value ? "true" : "false"}
                onValueChange={(value) => field.onChange(value === "true")}
              >
                <SelectTrigger
                  aria-label={copy.availability}
                  className="w-full"
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">{copy.active}</SelectItem>
                  <SelectItem value="false">{copy.inactive}</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
          <p className="text-sm text-muted-foreground">{copy.inactiveHelp}</p>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
        {onCancel ? (
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={saveMutation.isPending}
          >
            {copy.cancel}
          </Button>
        ) : null}
        <SubmitButton
          isPending={saveMutation.isPending}
          pendingLabel={pendingLabel ?? copy.pendingLabel}
        >
          {submitLabel ?? copy.submitLabel}
        </SubmitButton>
      </div>
    </form>
  );
}
