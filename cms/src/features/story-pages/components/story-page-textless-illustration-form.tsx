import { useEffect } from "react";

import { Controller } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { WorkspaceStatusPill } from "@/components/workspace/workspace-primitives";
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";
import type { StoryPageReadViewModel } from "@/features/contents/model/content-view-model";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import {
  storyPageTextlessIllustrationSchema,
  type StoryPageTextlessIllustrationFormValues,
} from "@/features/story-pages/schema/story-page-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";

type StoryPageTextlessIllustrationFormProps = {
  storyPage: StoryPageReadViewModel;
  isPending?: boolean;
  onDirtyChange?: (isDirty: boolean) => void;
  onSave: (input: {
    textlessIllustrationMediaId: number | null;
  }) => Promise<AdminStoryPageResponse>;
};

export function StoryPageTextlessIllustrationForm({
  storyPage,
  isPending = false,
  onDirtyChange,
  onSave,
}: StoryPageTextlessIllustrationFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          title: "Yazisiz kaynak illustrasyon",
          description:
            "Bu gorsel dile bagli degildir. Yeni ceviriler icin yazisiz sayfa kaynagi olarak kullanilir ve yayin hazirligini engellemez.",
          linked: "Kaynak gorsel bagli",
          missing: "Kaynak gorsel eksik",
          pickerLabel: "Yazisiz illustrasyon asset'i",
          pickerTitle: "Yazisiz hikaye sayfasi gorseli sec",
          pickerDescription:
            "Bu sayfanin dil bagimsiz, uzerinde yazi olmayan kaynak gorselini secin veya yukleyin.",
          save: "Kaynak gorseli kaydet",
          saving: "Kaynak gorsel kaydediliyor...",
          saved: "Kaynak gorsel kaydedildi.",
          genericSaveError: "Kaynak gorsel kaydedilemedi.",
        }
      : {
          title: "Textless/source illustration",
          description:
            "This page-level image is language-independent. It is used as the clean source for future translations and does not block publishing readiness.",
          linked: "Source image linked",
          missing: "Source image missing",
          pickerLabel: "Textless illustration asset",
          pickerTitle: "Pick textless story page image",
          pickerDescription:
            "Select or upload the language-independent source image for this story page.",
          save: "Save source image",
          saving: "Saving source image...",
          saved: "Source image saved.",
          genericSaveError: "The source image could not be saved.",
        };
  const form = useZodForm<StoryPageTextlessIllustrationFormValues>({
    schema: storyPageTextlessIllustrationSchema,
    defaultValues: {
      textlessIllustrationMediaId: storyPage.textlessIllustrationAssetId,
    },
  });
  const selectedAssetId = form.watch("textlessIllustrationMediaId");
  const isFormDirty = form.formState.isDirty;

  useEffect(() => {
    form.reset({
      textlessIllustrationMediaId: storyPage.textlessIllustrationAssetId,
    });
    onDirtyChange?.(false);
  }, [
    form,
    onDirtyChange,
    storyPage.pageNumber,
    storyPage.textlessIllustrationAssetId,
  ]);

  useEffect(() => {
    onDirtyChange?.(isFormDirty);
  }, [isFormDirty, onDirtyChange]);

  async function handleSubmit(values: StoryPageTextlessIllustrationFormValues) {
    form.clearErrors();

    if (values.textlessIllustrationMediaId !== null) {
      const assetError = await validateIllustrationAssetId(
        values.textlessIllustrationMediaId,
      );

      if (assetError) {
        form.setError("textlessIllustrationMediaId", {
          type: "server",
          message: assetError,
        });
        return;
      }
    }

    try {
      const savedStoryPage = await toastMutation(onSave(values), {
        loading: copy.saving,
        success: copy.saved,
      });

      form.reset({
        textlessIllustrationMediaId: savedStoryPage.textlessIllustrationMediaId,
      });
      onDirtyChange?.(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "asset_media_type_mismatch") {
          form.setError("textlessIllustrationMediaId", {
            type: "server",
            message: error.problem.detail,
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
      className="grid gap-4 rounded-2xl border border-border/70 bg-muted/10 p-4"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-1">
          <h3 className="text-sm font-semibold text-foreground">
            {copy.title}
          </h3>
          <p className="max-w-2xl text-sm text-muted-foreground">
            {copy.description}
          </p>
        </div>
        <WorkspaceStatusPill
          tone={selectedAssetId === null ? "warning" : "success"}
        >
          {selectedAssetId === null ? copy.missing : copy.linked}
        </WorkspaceStatusPill>
      </div>

      <Controller
        control={form.control}
        name="textlessIllustrationMediaId"
        render={({ field, fieldState }) => (
          <AssetPickerField
            id={`page-${storyPage.pageNumber}-textless-illustration-asset`}
            label={copy.pickerLabel}
            mediaType="IMAGE"
            value={field.value}
            onChange={field.onChange}
            pickerTitle={copy.pickerTitle}
            pickerDescription={copy.pickerDescription}
            description={copy.pickerDescription}
            disabled={isPending || form.formState.isSubmitting}
            error={fieldState.error}
            variant="editor"
            testId="story-page-textless-illustration"
          />
        )}
      />

      {form.formState.errors.root?.serverError ? (
        <FieldError error={form.formState.errors.root.serverError} />
      ) : null}

      <div className="flex justify-end">
        <SubmitButton
          isPending={form.formState.isSubmitting || isPending}
          pendingLabel={copy.saving}
          type="submit"
        >
          {copy.save}
        </SubmitButton>
      </div>
    </form>
  );
}
