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
import type { AdminCategoryLocalizationResponse } from "@/features/categories/api/category-admin";
import type { CategoryLocalizationViewModel } from "@/features/categories/model/category-view-model";
import { useCategoryLocalizationActions } from "@/features/categories/mutations/use-category-localization-actions";
import {
  categoryLocalizationFormSchema,
  categoryLocalizationStatusOptions,
  mapCategoryLocalizationResponseToFormValues,
  type CategoryLocalizationFormValues,
} from "@/features/categories/schema/category-localization-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import type { ApiProblemDetail } from "@/types/api";

type LanguageOption = {
  code: string;
  label: string;
};

type CategoryLocalizationFormProps = {
  categoryId: number;
  mode: "create" | "update";
  initialValues: CategoryLocalizationFormValues;
  localization?: CategoryLocalizationViewModel;
  availableLanguages?: LanguageOption[];
  onSuccess?: (localization: AdminCategoryLocalizationResponse) => void;
  onCancel?: () => void;
};

function isProblemMappedToField(problem: ApiProblemDetail) {
  return (
    problem.errorCode === "category_localization_exists" ||
    problem.errorCode === "asset_media_type_mismatch" ||
    Object.keys(getProblemFieldErrors(problem)).length > 0
  );
}

function getLanguageLabel(
  languageCode: string,
  availableLanguages: LanguageOption[],
  localization?: CategoryLocalizationViewModel,
) {
  if (localization) {
    return localization.languageLabel;
  }

  return (
    availableLanguages.find((entry) => entry.code === languageCode)?.label ??
    languageCode.toUpperCase()
  );
}

export function CategoryLocalizationForm({
  categoryId,
  mode,
  initialValues,
  localization,
  availableLanguages = [],
  onSuccess,
  onCancel,
}: CategoryLocalizationFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          createLoading: "Kategori yerellestirmesi olusturuluyor...",
          updateLoading: "Kategori yerellestirmesi kaydediliyor...",
          createSuccess: "Kategori yerellestirmesi olusturuldu.",
          updateSuccess: "Kategori yerellestirmesi kaydedildi.",
          languageExists: "Bu kategori icin bu dil zaten var.",
          genericSaveError:
            "Kategori yerellestirmesi kaydedilemedi. Tekrar deneyin.",
          language: "Dil",
          selectLanguage: "Dil secin",
          localeCode: "Dil kodu",
          name: "Ad",
          localizedName: "Yerellestirilmis kategori adi",
          description: "Aciklama",
          localizedDescription: "Yerellestirilmis kategori aciklamasi",
          imageDescription:
            "Kategori gorselini bu yerellestirme ekraninda yukleyin veya secin.",
          imageLabel: "Gorsel asset'i",
          imagePickerDescription:
            "Bu kategori yerellestirmesi icin son gorsel asset'lerinden birini secin.",
          imagePickerTitle: "Kategori gorsel asset'ini sec",
          status: "Durum",
          selectStatus: "Durum secin",
          publishedAt: "Yayinlanma zamani",
          publishedHelp:
            "Yayinlama icin zaman damgasi gerekir. Bu alan sadece published durumda kullanilir.",
          cancel: "Iptal",
          createLocalization: "Yerellestirme olustur",
          saveLocalization: "Yerellestirmeyi kaydet",
          optional: "Opsiyonel",
        }
      : {
          createLoading: "Creating category localization...",
          updateLoading: "Saving category localization...",
          createSuccess: "Category localization created.",
          updateSuccess: "Category localization saved.",
          languageExists: "Language already exists for this category.",
          genericSaveError:
            "Category localization could not be saved. Try again.",
          language: "Language",
          selectLanguage: "Select language",
          localeCode: "Locale code",
          name: "Name",
          localizedName: "Localized category name",
          description: "Description",
          localizedDescription: "Localized category description",
          imageDescription:
            "Upload or browse the category image directly in this editor.",
          imageLabel: "Image asset",
          imagePickerDescription:
            "Select a recent image asset for this category localization.",
          imagePickerTitle: "Pick category image asset",
          status: "Status",
          selectStatus: "Select status",
          publishedAt: "Published at",
          publishedHelp:
            "Publishing requires a timestamp. Use this only for published localizations.",
          cancel: "Cancel",
          createLocalization: "Create localization",
          saveLocalization: "Save localization",
          optional: "Optional",
        };
  const form = useZodForm<CategoryLocalizationFormValues>({
    schema: useMemo(() => categoryLocalizationFormSchema, []),
    defaultValues: initialValues,
  });
  const { saveLocalization } = useCategoryLocalizationActions(categoryId);
  const status = form.watch("status");
  const selectedLanguageCode = form.watch("languageCode");
  const selectedLanguageLabel = getLanguageLabel(
    selectedLanguageCode,
    availableLanguages,
    localization,
  );
  const saveProblem =
    saveLocalization.error instanceof ApiClientError
      ? saveLocalization.error.problem
      : null;
  const alertProblem =
    saveProblem && !isProblemMappedToField(saveProblem) ? saveProblem : null;

  async function handleSubmit(values: CategoryLocalizationFormValues) {
    form.clearErrors();
    saveLocalization.reset();

    const imageAssetError = await validateIllustrationAssetId(
      values.imageMediaId,
    );

    if (imageAssetError) {
      form.setError("imageMediaId", {
        type: "server",
        message: imageAssetError,
      });
      return;
    }

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

      form.reset(
        mapCategoryLocalizationResponseToFormValues(savedLocalization),
      );
      onSuccess?.(savedLocalization);
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "category_localization_exists") {
          form.setError("languageCode", {
            type: "server",
            message: copy.languageExists,
          });
          return;
        }

        if (error.problem.errorCode === "asset_media_type_mismatch") {
          form.setError("imageMediaId", {
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
          <label className="text-sm font-medium text-foreground" htmlFor="name">
            {copy.name}
          </label>
          <Input
            id="name"
            placeholder={copy.localizedName}
            {...form.register("name")}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.name} />
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

        <div
          className="grid gap-5 md:col-span-2 xl:grid-cols-[minmax(0,1.05fr)_minmax(19rem,0.95fr)]"
          data-testid="category-localization-cover-layout"
        >
          <div data-testid="category-localization-image-row">
            <Controller
              control={form.control}
              name="imageMediaId"
              render={({ field }) => (
                <AssetPickerField
                  advancedLabel="Advanced image asset options"
                  description={copy.imageDescription}
                  disabled={saveLocalization.isPending}
                  error={form.formState.errors.imageMediaId}
                  id="imageMediaId"
                  label={copy.imageLabel}
                  manualInputLabel={`${copy.imageLabel} id`}
                  mediaType="IMAGE"
                  pickerDescription={copy.imagePickerDescription}
                  pickerTitle={copy.imagePickerTitle}
                  placeholder={copy.optional}
                  testId="category-localization-image-asset"
                  value={field.value}
                  variant="editor"
                  onChange={field.onChange}
                />
              )}
            />
          </div>

          <div
            className="space-y-5 rounded-2xl border border-border/70 bg-muted/10 p-4"
            data-testid="category-localization-metadata-row"
          >
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
                      {categoryLocalizationStatusOptions.map((option) => (
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
