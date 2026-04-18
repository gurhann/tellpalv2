import { useEffect } from "react";

import { Controller } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { Textarea } from "@/components/ui/textarea";
import { AssetPickerField } from "@/features/assets/components/asset-picker-field";
import type { AdminStoryPageLocalizationResponse } from "@/features/contents/api/story-page-admin";
import type {
  ContentLocalizationViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import {
  validateAudioAssetId,
  validateIllustrationAssetId,
} from "@/features/story-pages/lib/illustration-asset-validation";
import {
  getStoryPageLocalizationFormDefaults,
  mapStoryPageLocalizationToFormValues,
  storyPageLocalizationSchema,
  type StoryPageLocalizationFormValues,
} from "@/features/story-pages/schema/story-page-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";

type StoryPageLocalizationFormProps = {
  storyPage: StoryPageReadViewModel;
  contentLocalization: ContentLocalizationViewModel;
  isPending?: boolean;
  onSave: (input: {
    languageCode: string;
    bodyText: string | null;
    audioMediaId: number | null;
    illustrationMediaId: number;
  }) => Promise<AdminStoryPageLocalizationResponse>;
};

function getRootErrorMessage(
  error: unknown,
  genericMessage: string,
  missingParentMessage: string,
) {
  if (error instanceof ApiClientError) {
    if (error.problem.errorCode === "content_localization_not_found") {
      return missingParentMessage;
    }

    return error.problem.detail;
  }

  return genericMessage;
}

export function StoryPageLocalizationForm({
  storyPage,
  contentLocalization,
  isPending = false,
  onSave,
}: StoryPageLocalizationFormProps) {
  const { locale } = useI18n();
  let copy =
    locale === "tr"
      ? {
          missingParentLocalization:
            "Hikaye sayfalarını düzenlemeden önce bu dil için parent içerik yerelleştirmesini oluşturun.",
          genericSaveError: "Hikaye sayfası yerelleştirmesi kaydedilemedi.",
          illustrationRequired: "İllüstrasyon asset id zorunludur.",
          createLoading: "Sayfa yerelleştirmesi oluşturuluyor...",
          updateLoading: "Sayfa yerelleştirmesi kaydediliyor...",
          createSuccess: "Sayfa yerelleştirmesi oluşturuldu.",
          updateSuccess: "Sayfa yerelleştirmesi kaydedildi.",
          parentLocale: "Üst dil",
          bodyReady: "Gövde hazır",
          bodyMissing: "Gövde eksik",
          bodyMissingHelp:
            "Boş anlatı metni eksik kalır ve bu dilde hikaye yayınını engeller.",
          audioLinked: "Ses bağlı",
          audioMissing: "Ses eksik",
          audioMissingHelp:
            "Bu dil yayın için hazır olmadan önce ses alanı bir `AUDIO` asset'ine bağlanmalıdır.",
          illustrationLinked: "İllüstrasyon bağlı",
          illustrationMissing: "İllüstrasyon eksik",
          publishReady: "Bu sayfa dili yayın kontrolleri için hazır.",
          publishBlocked:
            "Hikaye yayını için hâlâ gövde metni, ses ve yerelleştirilmiş illüstrasyon gerekiyor.",
          bodyText: "Gövde metni",
          localizedNarrative: "Yerelleştirilmiş sayfa anlatısı",
          bodyHelp:
            "Boş gövde metni boş durum olarak kaydedilir ve bu dili yayın için eksik bırakır.",
          illustrationDescription:
            "Yerelleştirilmiş illüstrasyonu buradan yükleyin veya bu hikaye sayfası için mevcut `IMAGE` asset'lerini seçin.",
          illustrationLabel: "İllüstrasyon asset'i",
          illustrationPickerDescription:
            "Bu yerelleştirilmiş hikaye sayfası illüstrasyonu için son görsel asset'lerinden birini seçin. Bu alandan upload desteği sürer; manuel asset id girişi ise Advanced altında kalır.",
          illustrationPickerTitle: "Hikaye sayfası illüstrasyonu seç",
          audioDescription:
            "Sayfa editöründen çıkmadan bu sayfa için yerelleştirilmiş anlatımı yükleyin veya seçin.",
          audioLabel: "Ses asset'i",
          audioPickerDescription:
            "Bu yerelleştirilmiş hikaye sayfası için son ses asset'lerinden birini seçin. Bu alandan upload desteği sürer; manuel asset id girişi ise Advanced altında kalır.",
          audioPickerTitle: "Hikaye sayfası ses asset'ini seç",
          optional: "Opsiyonel",
          required: "Zorunlu",
          createLocalization: "Sayfa yerelleştirmesi oluştur",
          saveLocalization: "Sayfa yerelleştirmesini kaydet",
        }
      : {
          missingParentLocalization:
            "Create the parent content localization for this language before editing story pages.",
          genericSaveError: "The story page localization could not be saved.",
          illustrationRequired: "Illustration asset id is required.",
          createLoading: "Creating page localization...",
          updateLoading: "Saving page localization...",
          createSuccess: "Page localization created.",
          updateSuccess: "Page localization saved.",
          parentLocale: "Parent locale",
          bodyReady: "Body ready",
          bodyMissing: "Body missing",
          bodyMissingHelp:
            "Empty narrative copy stays incomplete and blocks story publication for this language.",
          audioLinked: "Audio linked",
          audioMissing: "Audio missing",
          audioMissingHelp:
            "Audio must reference an `AUDIO` asset before this locale is publication-ready.",
          illustrationLinked: "Illustration linked",
          illustrationMissing: "Illustration missing",
          publishReady: "This page locale is ready for publication checks.",
          publishBlocked:
            "Story publication still needs body copy, audio, and a localized illustration.",
          bodyText: "Body text",
          localizedNarrative: "Localized page narrative",
          bodyHelp:
            "Blank body text is saved as empty state and keeps this language incomplete for publication.",
          illustrationDescription:
            "Upload the localized illustration here or browse existing `IMAGE` assets for this story page.",
          illustrationLabel: "Illustration asset",
          illustrationPickerDescription:
            "Select a recent image asset for this localized story page illustration. Uploading from this field stays available, and manual asset ids remain under Advanced.",
          illustrationPickerTitle: "Pick story page illustration",
          audioDescription:
            "Upload or select localized narration for this page without leaving the story page editor.",
          audioLabel: "Audio asset",
          audioPickerDescription:
            "Select a recent audio asset for this localized story page. Uploading from this field stays available, and manual asset ids remain under Advanced.",
          audioPickerTitle: "Pick story page audio asset",
          optional: "Optional",
          required: "Required",
          createLocalization: "Create page localization",
          saveLocalization: "Save page localization",
        };
  if (locale === "tr") {
    copy = {
      missingParentLocalization:
        "Hikaye sayfalarini duzenlemeden once bu dil icin parent icerik yerellestirmesini olusturun.",
      genericSaveError: "Hikaye sayfasi yerellestirmesi kaydedilemedi.",
      illustrationRequired: "Illustrasyon asset id zorunludur.",
      createLoading: "Sayfa yerellestirmesi olusturuluyor...",
      updateLoading: "Sayfa yerellestirmesi kaydediliyor...",
      createSuccess: "Sayfa yerellestirmesi olusturuldu.",
      updateSuccess: "Sayfa yerellestirmesi kaydedildi.",
      parentLocale: "Ust dil",
      bodyReady: "Govde hazir",
      bodyMissing: "Govde eksik",
      bodyMissingHelp:
        "Bos anlati metni eksik kalir ve bu dilde hikaye yayinini engeller.",
      audioLinked: "Ses bagli",
      audioMissing: "Ses eksik",
      audioMissingHelp:
        "Bu dil yayin icin hazir olmadan once ses alani bir `AUDIO` asset'ine baglanmalidir.",
      illustrationLinked: "Illustrasyon bagli",
      illustrationMissing: "Illustrasyon eksik",
      publishReady: "Bu sayfa dili yayin kontrolleri icin hazir.",
      publishBlocked:
        "Hikaye yayini icin hala govde metni, ses ve yerellestirilmis illustrasyon gerekiyor.",
      bodyText: "Govde metni",
      localizedNarrative: "Yerellestirilmis sayfa anlatisi",
      bodyHelp:
        "Bos govde metni bos durum olarak kaydedilir ve bu dili yayin icin eksik birakir.",
      illustrationDescription:
        "Yerellestirilmis illustrasyonu buradan yukleyin veya bu hikaye sayfasi icin mevcut `IMAGE` asset'lerini secin.",
      illustrationLabel: "Illustrasyon asset'i",
      illustrationPickerDescription:
        "Bu yerellestirilmis hikaye sayfasi illustrasyonu icin son gorsel asset'lerinden birini secin. Bu alandan upload destegi surer; manuel asset id girisi ise Advanced altinda kalir.",
      illustrationPickerTitle: "Hikaye sayfasi illustrasyonu sec",
      audioDescription:
        "Sayfa editorunden cikmadan bu sayfa icin yerellestirilmis anlatimi yukleyin veya secin.",
      audioLabel: "Ses asset'i",
      audioPickerDescription:
        "Bu yerellestirilmis hikaye sayfasi icin son ses asset'lerinden birini secin. Bu alandan upload destegi surer; manuel asset id girisi ise Advanced altinda kalir.",
      audioPickerTitle: "Hikaye sayfasi ses asset'ini sec",
      optional: "Opsiyonel",
      required: "Zorunlu",
      createLocalization: "Sayfa yerellestirmesi olustur",
      saveLocalization: "Sayfa yerellestirmesini kaydet",
    };
  }
  const existingLocalization =
    storyPage.localizations.find(
      (localization) =>
        localization.languageCode === contentLocalization.languageCode,
    ) ?? null;
  const mode = existingLocalization ? "update" : "create";
  const form = useZodForm<StoryPageLocalizationFormValues>({
    schema: storyPageLocalizationSchema,
    defaultValues: existingLocalization
      ? mapStoryPageLocalizationToFormValues(existingLocalization)
      : getStoryPageLocalizationFormDefaults(contentLocalization.languageCode),
  });
  const bodyText = form.watch("bodyText");
  const audioMediaId = form.watch("audioMediaId");
  const illustrationMediaId = form.watch("illustrationMediaId");
  const hasBodyText = Boolean(bodyText?.trim());
  const hasAudioAsset = audioMediaId !== null;
  const hasIllustration = typeof illustrationMediaId === "number";
  const isReadyForPublish = hasBodyText && hasAudioAsset && hasIllustration;

  useEffect(() => {
    form.reset(
      existingLocalization
        ? mapStoryPageLocalizationToFormValues(existingLocalization)
        : getStoryPageLocalizationFormDefaults(
            contentLocalization.languageCode,
          ),
    );
  }, [contentLocalization.languageCode, existingLocalization, form]);

  async function handleSubmit(values: StoryPageLocalizationFormValues) {
    form.clearErrors();

    if (values.illustrationMediaId === null) {
      form.setError("illustrationMediaId", {
        type: "server",
        message: copy.illustrationRequired,
      });
      return;
    }

    const illustrationMediaId = values.illustrationMediaId;
    const illustrationAssetError =
      await validateIllustrationAssetId(illustrationMediaId);

    if (illustrationAssetError) {
      form.setError("illustrationMediaId", {
        type: "server",
        message: illustrationAssetError,
      });
      return;
    }

    const audioAssetError = await validateAudioAssetId(values.audioMediaId);

    if (audioAssetError) {
      form.setError("audioMediaId", {
        type: "server",
        message: audioAssetError,
      });
      return;
    }

    try {
      const savedLocalization = await toastMutation(
        onSave({
          languageCode: values.languageCode,
          bodyText: values.bodyText,
          audioMediaId: values.audioMediaId,
          illustrationMediaId,
        }),
        {
          loading: mode === "create" ? copy.createLoading : copy.updateLoading,
          success: mode === "create" ? copy.createSuccess : copy.updateSuccess,
        },
      );

      form.reset({
        languageCode: savedLocalization.languageCode,
        bodyText: savedLocalization.bodyText,
        audioMediaId: savedLocalization.audioMediaId,
        illustrationMediaId: savedLocalization.illustrationMediaId,
      });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "asset_media_type_mismatch") {
          const targetField = error.problem.detail.includes(
            "illustrationMediaId",
          )
            ? "illustrationMediaId"
            : "audioMediaId";
          form.setError(targetField, {
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
        message: getRootErrorMessage(
          error,
          copy.genericSaveError,
          copy.missingParentLocalization,
        ),
      });
    }
  }

  return (
    <form
      className="grid gap-5"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {contentLocalization.languageLabel}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {copy.parentLocale}: {contentLocalization.title}
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasBodyText ? copy.bodyReady : copy.bodyMissing}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {copy.bodyMissingHelp}
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasAudioAsset ? copy.audioLinked : copy.audioMissing}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {copy.audioMissingHelp}
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasIllustration
              ? copy.illustrationLinked
              : copy.illustrationMissing}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {isReadyForPublish ? copy.publishReady : copy.publishBlocked}
          </p>
        </div>
      </div>

      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor={`story-page-body-${contentLocalization.languageCode}`}
        >
          {copy.bodyText}
        </label>
        <Textarea
          id={`story-page-body-${contentLocalization.languageCode}`}
          placeholder={copy.localizedNarrative}
          {...form.register("bodyText")}
          disabled={isPending}
        />
        <p className="text-sm text-muted-foreground">{copy.bodyHelp}</p>
        <FieldError error={form.formState.errors.bodyText} />
      </div>

      <Controller
        control={form.control}
        name="illustrationMediaId"
        render={({ field }) => (
          <AssetPickerField
            advancedLabel="Advanced illustration asset options"
            description={copy.illustrationDescription}
            disabled={isPending}
            error={form.formState.errors.illustrationMediaId}
            id={`story-page-illustration-${contentLocalization.languageCode}`}
            label={copy.illustrationLabel}
            manualInputLabel={`${copy.illustrationLabel} id`}
            mediaType="IMAGE"
            pickerDescription={copy.illustrationPickerDescription}
            pickerTitle={copy.illustrationPickerTitle}
            placeholder={copy.required}
            testId={`story-page-${contentLocalization.languageCode}-illustration-asset`}
            value={field.value}
            onChange={field.onChange}
          />
        )}
      />

      <Controller
        control={form.control}
        name="audioMediaId"
        render={({ field }) => (
          <AssetPickerField
            advancedLabel="Advanced audio asset options"
            description={copy.audioDescription}
            disabled={isPending}
            error={form.formState.errors.audioMediaId}
            id={`story-page-audio-${contentLocalization.languageCode}`}
            label={copy.audioLabel}
            manualInputLabel={`${copy.audioLabel} id`}
            mediaType="AUDIO"
            pickerDescription={copy.audioPickerDescription}
            pickerTitle={copy.audioPickerTitle}
            placeholder={copy.optional}
            testId={`story-page-${contentLocalization.languageCode}-audio-asset`}
            value={field.value}
            onChange={field.onChange}
          />
        )}
      />

      <FieldError error={form.formState.errors.root?.serverError} />

      <div className="flex justify-end">
        <SubmitButton
          isPending={isPending}
          pendingLabel={
            mode === "create" ? copy.createLoading : copy.updateLoading
          }
        >
          {mode === "create" ? copy.createLocalization : copy.saveLocalization}
        </SubmitButton>
      </div>
    </form>
  );
}
