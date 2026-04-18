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
import { useI18n } from "@/i18n/locale-provider";
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

export function ContentLocalizationForm({
  content,
  mode,
  initialValues,
  localization,
  availableLanguages = [],
  onSuccess,
  onCancel,
}: ContentLocalizationFormProps) {
  const { locale } = useI18n();
  let copy =
    locale === "tr"
      ? {
          genericSaveError:
            "Yerelleştirme değişiklikleri kaydedilemedi. Tekrar deneyin.",
          createLoading: "Yerelleştirme oluşturuluyor...",
          updateLoading: "Yerelleştirme kaydediliyor...",
          createSuccess: "Yerelleştirme oluşturuldu.",
          updateSuccess: "Yerelleştirme kaydedildi.",
          languageExists: "Bu içerik için bu dil zaten var.",
          language: "Dil",
          selectLanguage: "Dil seçin",
          localeCode: "Dil kodu",
          title: "Başlık",
          localizedTitle: "Yerelleştirilmiş başlık",
          description: "Açıklama",
          localizedDescription: "Bu dil için kısa editoryal özet",
          storyGuidance:
            "Hikaye yerelleştirmeleri anlatı metnini ve sayfa bazlı sesi story pages alt rotasında tutar. Bu form yalnızca içerik seviyesinde yaşayan dil metadata'sını, durumu ve asset bağlantılarını düzenler.",
          bodyText: "Gövde metni",
          localizedBody: "Yerelleştirilmiş gövde metni",
          audioDescription:
            "Bu yerelleştirme için yeni bir ses dosyası yükleyin veya mevcut `AUDIO` asset'leri buradan seçin.",
          audioLabel: "Ses asset'i *",
          audioPickerDescription:
            "Bu içerik yerelleştirmesi için son ses asset'lerinden birini seçin. Bu alandan upload desteği sürer; manuel asset id girişi ise Advanced altında kalır.",
          audioPickerTitle: "Yerelleştirme ses asset'i seç",
          coverDescription:
            "Kapak görselini buradan yükleyin veya editörden çıkmadan mevcut `IMAGE` asset'lerini seçin.",
          coverLabel: "Kapak asset'i",
          coverPickerDescription:
            "Yerelleştirilmiş kapak için son görsel asset'lerinden birini seçin. Bu alandan upload desteği sürer; manuel asset id girişi ise Advanced altında kalır.",
          coverPickerTitle: "Yerelleştirme kapak asset'i seç",
          durationMinutes: "Süre (dakika)",
          status: "Durum",
          selectStatus: "Durum seçin",
          processingStatus: "İşleme durumu",
          selectProcessingStatus: "İşleme durumu seçin",
          publishedAt: "Yayınlanma zamanı",
          publishedHelp:
            "Yayınlama için zaman damgası gerekir. Mevcut bir dil kaydını geriye dönük doldururken veya manuel yayın penceresi hazırlarken bunu kullanın.",
          cancel: "İptal",
          createLocalization: "Yerelleştirme oluştur",
          saveLocalization: "Yerelleştirmeyi kaydet",
          optional: "Opsiyonel",
          required: "Zorunlu",
        }
      : {
          genericSaveError:
            "Localization changes could not be saved. Try again.",
          createLoading: "Creating localization...",
          updateLoading: "Saving localization...",
          createSuccess: "Localization created.",
          updateSuccess: "Localization saved.",
          languageExists: "Language already exists for this content.",
          language: "Language",
          selectLanguage: "Select language",
          localeCode: "Locale code",
          title: "Title",
          localizedTitle: "Localized title",
          description: "Description",
          localizedDescription: "Short editorial summary for this locale",
          storyGuidance:
            "Story localizations keep narrative copy and per-page audio on the story pages child route. This form only edits locale metadata, status, and asset bindings that live at content level.",
          bodyText: "Body text",
          localizedBody: "Localized body copy",
          audioDescription:
            "Upload a new audio file here or browse existing `AUDIO` assets for this localization.",
          audioLabel: "Audio asset *",
          audioPickerDescription:
            "Select a recent audio asset for this content localization. Uploading from this field stays available, and manual asset ids remain under Advanced.",
          audioPickerTitle: "Pick localization audio asset",
          coverDescription:
            "Upload a cover image here or browse existing `IMAGE` assets without leaving this editor.",
          coverLabel: "Cover asset",
          coverPickerDescription:
            "Select a recent image asset for the localized cover. Uploading from this field stays available, and manual asset ids remain under Advanced.",
          coverPickerTitle: "Pick localization cover asset",
          durationMinutes: "Duration minutes",
          status: "Status",
          selectStatus: "Select status",
          processingStatus: "Processing status",
          selectProcessingStatus: "Select processing status",
          publishedAt: "Published at",
          publishedHelp:
            "Publishing requires a timestamp. Use this when backfilling an existing locale or preparing a manual publish window.",
          cancel: "Cancel",
          createLocalization: "Create localization",
          saveLocalization: "Save localization",
          optional: "Optional",
          required: "Required",
        };
  if (locale === "tr") {
    copy = {
      genericSaveError:
        "Yerellestirme degisiklikleri kaydedilemedi. Tekrar deneyin.",
      createLoading: "Yerellestirme olusturuluyor...",
      updateLoading: "Yerellestirme kaydediliyor...",
      createSuccess: "Yerellestirme olusturuldu.",
      updateSuccess: "Yerellestirme kaydedildi.",
      languageExists: "Bu icerik icin bu dil zaten var.",
      language: "Dil",
      selectLanguage: "Dil secin",
      localeCode: "Dil kodu",
      title: "Baslik",
      localizedTitle: "Yerellestirilmis baslik",
      description: "Aciklama",
      localizedDescription: "Bu dil icin kisa editoryal ozet",
      storyGuidance:
        "Hikaye yerellestirmeleri anlati metnini ve sayfa bazli sesi story pages alt rotasinda tutar. Bu form yalnizca icerik seviyesinde yasayan dil metadata'sini, durumu ve asset baglantilarini duzenler.",
      bodyText: "Govde metni",
      localizedBody: "Yerellestirilmis govde metni",
      audioDescription:
        "Bu yerellestirme icin yeni bir ses dosyasi yukleyin veya mevcut `AUDIO` asset'leri buradan secin.",
      audioLabel: "Ses asset'i *",
      audioPickerDescription:
        "Bu icerik yerellestirmesi icin son ses asset'lerinden birini secin. Bu alandan upload destegi surer; manuel asset id girisi ise Advanced altinda kalir.",
      audioPickerTitle: "Yerellestirme ses asset'i sec",
      coverDescription:
        "Kapak gorselini buradan yukleyin veya editorden cikmadan mevcut `IMAGE` asset'lerini secin.",
      coverLabel: "Kapak asset'i",
      coverPickerDescription:
        "Yerellestirilmis kapak icin son gorsel asset'lerinden birini secin. Bu alandan upload destegi surer; manuel asset id girisi ise Advanced altinda kalir.",
      coverPickerTitle: "Yerellestirme kapak asset'i sec",
      durationMinutes: "Sure (dakika)",
      status: "Durum",
      selectStatus: "Durum secin",
      processingStatus: "Isleme durumu",
      selectProcessingStatus: "Isleme durumu secin",
      publishedAt: "Yayinlanma zamani",
      publishedHelp:
        "Yayinlama icin zaman damgasi gerekir. Mevcut bir dil kaydini geriye donuk doldururken veya manuel yayin penceresi hazirlarken bunu kullanin.",
      cancel: "Iptal",
      createLocalization: "Yerellestirme olustur",
      saveLocalization: "Yerellestirmeyi kaydet",
      optional: "Opsiyonel",
      required: "Zorunlu",
    };
  }
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
          loading: mode === "create" ? copy.createLoading : copy.updateLoading,
          success: mode === "create" ? copy.createSuccess : copy.updateSuccess,
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
            message: copy.languageExists,
          });
          return;
        }

        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message:
          error instanceof Error && error.message.trim().length > 0
            ? error.message
            : copy.genericSaveError,
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
              {copy.language}
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
                    aria-label={copy.language}
                    className="w-full"
                  >
                    <SelectValue placeholder={copy.selectLanguage} />
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
            <p className="text-sm font-medium text-foreground">
              {copy.language}
            </p>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {selectedLanguageLabel}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {copy.localeCode}: {selectedLanguageCode.toUpperCase()}
              </p>
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="title"
          >
            {copy.title}
          </label>
          <Input
            id="title"
            placeholder={copy.localizedTitle}
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
            {copy.description}
          </label>
          <Textarea
            id="description"
            placeholder={copy.localizedDescription}
            {...form.register("description")}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.description} />
        </div>

        {content.summary.supportsStoryPages ? null : (
          <>
            <div className="space-y-2 md:col-span-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="bodyText"
              >
                {copy.bodyText}
                {requiresBodyText ? (
                  <span className="ml-1 text-destructive">*</span>
                ) : null}
              </label>
              <Textarea
                id="bodyText"
                placeholder={copy.localizedBody}
                {...form.register("bodyText")}
                disabled={saveLocalization.isPending}
              />
              <FieldError error={form.formState.errors.bodyText} />
            </div>

            <div
              className="md:col-span-2"
              data-testid="content-localization-audio-row"
            >
              <Controller
                control={form.control}
                name="audioMediaId"
                render={({ field }) => (
                  <AssetPickerField
                    advancedLabel="Advanced audio asset options"
                    description={copy.audioDescription}
                    disabled={saveLocalization.isPending}
                    error={form.formState.errors.audioMediaId}
                    id="audioMediaId"
                    label={copy.audioLabel}
                    manualInputLabel={`${copy.audioLabel.replace("*", "").trim()} id`}
                    mediaType="AUDIO"
                    pickerDescription={copy.audioPickerDescription}
                    pickerTitle={copy.audioPickerTitle}
                    placeholder={copy.required}
                    testId="content-localization-audio-asset"
                    value={field.value}
                    variant="editor"
                    onChange={field.onChange}
                  />
                )}
              />
            </div>
          </>
        )}

        <div
          className="grid gap-5 md:col-span-2 xl:grid-cols-[minmax(0,1.1fr)_minmax(20rem,0.9fr)]"
          data-testid="content-localization-cover-layout"
        >
          <div data-testid="content-localization-cover-row">
            <Controller
              control={form.control}
              name="coverMediaId"
              render={({ field }) => (
                <AssetPickerField
                  advancedLabel="Advanced cover asset options"
                  description={copy.coverDescription}
                  disabled={saveLocalization.isPending}
                  error={form.formState.errors.coverMediaId}
                  id="coverMediaId"
                  label={copy.coverLabel}
                  manualInputLabel={`${copy.coverLabel} id`}
                  mediaType="IMAGE"
                  pickerDescription={copy.coverPickerDescription}
                  pickerTitle={copy.coverPickerTitle}
                  placeholder={copy.optional}
                  testId="content-localization-cover-asset"
                  value={field.value}
                  variant="editor"
                  onChange={field.onChange}
                />
              )}
            />
          </div>

          <div
            className="space-y-5 rounded-2xl border border-border/70 bg-muted/10 p-4"
            data-testid="content-localization-metadata-row"
          >
            <div className="space-y-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="durationMinutes"
              >
                {copy.durationMinutes}
              </label>
              <Input
                id="durationMinutes"
                inputMode="numeric"
                placeholder={copy.optional}
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
              <label className="text-sm font-medium text-foreground">
                {copy.status}
              </label>
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
                      aria-label={copy.status}
                      className="w-full"
                    >
                      <SelectValue placeholder={copy.selectStatus} />
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
                {copy.processingStatus}
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
                      aria-label={copy.processingStatus}
                      className="w-full"
                    >
                      <SelectValue placeholder={copy.selectProcessingStatus} />
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
              <div className="space-y-2">
                <label
                  className="text-sm font-medium text-foreground"
                  htmlFor="publishedAt"
                >
                  {copy.publishedAt}
                </label>
                <Input
                  id="publishedAt"
                  type="datetime-local"
                  {...form.register("publishedAt")}
                  disabled={saveLocalization.isPending}
                />
                <p className="text-sm text-muted-foreground">
                  {copy.publishedHelp}
                </p>
                <FieldError error={form.formState.errors.publishedAt} />
              </div>
            ) : null}
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
        {onCancel ? (
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={saveLocalization.isPending}
          >
            {copy.cancel}
          </Button>
        ) : null}
        <SubmitButton
          isPending={saveLocalization.isPending}
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
