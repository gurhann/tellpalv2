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
import { useSaveContent } from "@/features/contents/mutations/use-save-content";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import {
  contentTextlessCoverSchema,
  type ContentTextlessCoverFormValues,
} from "@/features/contents/schema/content-schema";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";

type ContentTextlessCoverFormProps = {
  content: ContentReadViewModel;
};

export function ContentTextlessCoverForm({
  content,
}: ContentTextlessCoverFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          title: "Yazisiz kaynak kapak",
          description:
            "Bu kapak dile bagli degildir. Yeni cevirilerde kapak tasarimi icin temiz kaynak olarak kullanilir ve yayin hazirligini engellemez.",
          linked: "Kaynak kapak bagli",
          missing: "Kaynak kapak eksik",
          pickerLabel: "Yazisiz kapak asset'i",
          pickerTitle: "Yazisiz hikaye kapagi sec",
          pickerDescription:
            "Hikayenin dil bagimsiz, uzerinde yazi olmayan kapak gorselini secin veya yukleyin.",
          save: "Kaynak kapagi kaydet",
          saving: "Kaynak kapak kaydediliyor...",
          saved: "Kaynak kapak kaydedildi.",
          genericSaveError: "Kaynak kapak kaydedilemedi.",
        }
      : {
          title: "Textless/source cover",
          description:
            "This content-level cover is language-independent. It is used as the clean source for future cover translations and does not block publishing readiness.",
          linked: "Source cover linked",
          missing: "Source cover missing",
          pickerLabel: "Textless cover asset",
          pickerTitle: "Pick textless story cover",
          pickerDescription:
            "Select or upload the language-independent cover image for this story.",
          save: "Save source cover",
          saving: "Saving source cover...",
          saved: "Source cover saved.",
          genericSaveError: "The source cover could not be saved.",
        };
  const form = useZodForm<ContentTextlessCoverFormValues>({
    schema: contentTextlessCoverSchema,
    defaultValues: {
      textlessCoverMediaId: content.summary.textlessCoverAssetId,
    },
  });
  const saveMutation = useSaveContent({
    mode: "update",
    contentId: content.summary.id,
  });
  const selectedAssetId = form.watch("textlessCoverMediaId");

  useEffect(() => {
    form.reset({
      textlessCoverMediaId: content.summary.textlessCoverAssetId,
    });
  }, [content.summary.id, content.summary.textlessCoverAssetId, form]);

  async function handleSubmit(values: ContentTextlessCoverFormValues) {
    form.clearErrors();
    saveMutation.reset();

    if (values.textlessCoverMediaId !== null) {
      const assetError = await validateIllustrationAssetId(
        values.textlessCoverMediaId ?? null,
      );

      if (assetError) {
        form.setError("textlessCoverMediaId", {
          type: "server",
          message: assetError,
        });
        return;
      }
    }

    try {
      const savedContent = await toastMutation(
        saveMutation.mutateAsync({
          type: content.summary.type,
          externalKey: content.summary.externalKey,
          ageRange: content.summary.ageRange,
          active: content.summary.active,
          textlessCoverMediaId: values.textlessCoverMediaId ?? null,
        }),
        {
          loading: copy.saving,
          success: copy.saved,
        },
      );

      form.reset({
        textlessCoverMediaId: savedContent.textlessCoverMediaId,
      });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "asset_media_type_mismatch") {
          form.setError("textlessCoverMediaId", {
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
      className="grid gap-4"
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
        name="textlessCoverMediaId"
        render={({ field, fieldState }) => (
          <AssetPickerField
            id={`content-${content.summary.id}-textless-cover-asset`}
            label={copy.pickerLabel}
            mediaType="IMAGE"
            value={field.value ?? null}
            onChange={field.onChange}
            pickerTitle={copy.pickerTitle}
            pickerDescription={copy.pickerDescription}
            description={copy.pickerDescription}
            disabled={saveMutation.isPending || form.formState.isSubmitting}
            error={fieldState.error}
            variant="editor"
            testId="content-textless-cover"
          />
        )}
      />

      {form.formState.errors.root?.serverError ? (
        <FieldError error={form.formState.errors.root.serverError} />
      ) : null}

      <div className="flex justify-end">
        <SubmitButton
          isPending={form.formState.isSubmitting || saveMutation.isPending}
          pendingLabel={copy.saving}
          type="submit"
        >
          {copy.save}
        </SubmitButton>
      </div>
    </form>
  );
}
