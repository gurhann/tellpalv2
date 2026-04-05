import { Controller } from "react-hook-form";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { AdminCategoryResponse } from "@/features/categories/api/category-admin";
import { useSaveCategory } from "@/features/categories/mutations/use-save-category";
import {
  categoryFormSchema,
  categoryTypeOptions,
  mapCategoryResponseToFormValues,
  type CategoryFormValues,
} from "@/features/categories/schema/category-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type CategoryFormProps = {
  mode: "create" | "update";
  categoryId?: number;
  initialValues: CategoryFormValues;
  onSuccess?: (category: AdminCategoryResponse) => void;
  onCancel?: () => void;
  submitLabel?: string;
  pendingLabel?: string;
};

function getCategoryTypeGuidance(
  type: CategoryFormValues["type"],
  locale: "en" | "tr",
) {
  const guidanceByType: Record<
    CategoryFormValues["type"],
    { title: string; description: string }
  > = {
    STORY: {
      title:
        locale === "tr" ? "Hikaye kategori akışı" : "Story category workflow",
      description:
        locale === "tr"
          ? "Hikaye kategorileri yalnızca STORY kayıtlarını küratör eder. Yerelleştirme çalışma alanları kategorinin kendisini yayınlar; kürasyon daha sonra yalnızca eşleşen dillerde hikaye içeriklerini kabul eder."
          : "Story categories curate only STORY records. Localization workspaces publish the category itself, and curation will later accept only story content in matching languages.",
    },
    AUDIO_STORY: {
      title:
        locale === "tr"
          ? "Sesli hikaye kategori akışı"
          : "Audio story category workflow",
      description:
        locale === "tr"
          ? "Sesli hikaye kategorileri AUDIO_STORY kayıtlarına ayrılmıştır. Editoryal gruplama ve gelecekteki kürasyon anlatımlı uzun form ses içerikleriyle sınırlı kalacaksa bunu kullanın."
          : "Audio story categories are reserved for AUDIO_STORY records. Use this when editorial grouping and future curation should stay limited to narrated long-form audio content.",
    },
    MEDITATION: {
      title:
        locale === "tr"
          ? "Meditasyon kategori akışı"
          : "Meditation category workflow",
      description:
        locale === "tr"
          ? "Meditasyon kategorileri yalnızca MEDITATION kayıtlarını küratör eder. Gelecekteki kürasyon ve keşif ekranları diğer içerik ailelerini karıştırmasın diye türü burada hizalı tutun."
          : "Meditation categories curate only MEDITATION records. Keep the type aligned here so future curation and discovery screens cannot mix in other content families.",
    },
    LULLABY: {
      title:
        locale === "tr" ? "Ninni kategori akışı" : "Lullaby category workflow",
      description:
        locale === "tr"
          ? "Ninni kategorileri yalnızca LULLABY kayıtlarını küratör eder. Yerelleştirme ve kürasyon desteklenen dillerde ninniye hazır editoryal koleksiyonlarla sınırlı kalır."
          : "Lullaby categories curate only LULLABY records. Localization and curation will stay scoped to lullaby-ready editorial collections across supported languages.",
    },
  };

  return guidanceByType[type];
}

function isProblemMappedToField(problem: ApiProblemDetail) {
  return (
    problem.errorCode === "duplicate_category_slug" ||
    Object.keys(getProblemFieldErrors(problem)).length > 0
  );
}

export function CategoryForm({
  mode,
  categoryId,
  initialValues,
  onSuccess,
  onCancel,
  submitLabel,
  pendingLabel,
}: CategoryFormProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          submitLabel:
            mode === "create" ? "Kategori oluştur" : "Metadata kaydet",
          pendingLabel:
            mode === "create"
              ? "Kategori oluşturuluyor..."
              : "Metadata kaydediliyor...",
          createLoading: "Kategori kaydı oluşturuluyor...",
          updateLoading: "Kategori metadata'sı kaydediliyor...",
          createSuccess: "Kategori kaydı oluşturuldu.",
          updateSuccess: "Kategori metadata'sı kaydedildi.",
          slugDuplicate: "Slug zaten kullanılıyor.",
          genericSaveError:
            "Kategori değişiklikleri kaydedilemedi. Tekrar deneyin.",
          categoryType: "Kategori türü",
          selectCategoryType: "Kategori türü seçin",
          typeFixed:
            "Kategori türü bu kategorinin hangi içerik ailesini küratör edebileceğini belirler. Bu form slug, premium ve aktiflik durumunu günceller; tür oluşturulduktan sonra sabit kalır.",
          slug: "Slug",
          access: "Erişim",
          standard: "Standart",
          premium: "Premium",
          premiumHelp:
            "Premium kategoriler admin listeleri ve detay ekranlarında yine de düzenlenebilir.",
          availability: "Erişim durumu",
          active: "Aktif",
          inactive: "Pasif",
          inactiveHelp:
            "Pasif kategoriler editoryal temizlik için admin okuma ekranlarında görünmeye devam eder.",
          cancel: "İptal",
        }
      : {
          submitLabel: mode === "create" ? "Create category" : "Save metadata",
          pendingLabel:
            mode === "create" ? "Creating category..." : "Saving metadata...",
          createLoading: "Creating category record...",
          updateLoading: "Saving category metadata...",
          createSuccess: "Category record created.",
          updateSuccess: "Category metadata saved.",
          slugDuplicate: "Slug is already in use.",
          genericSaveError: "Category changes could not be saved. Try again.",
          categoryType: "Category type",
          selectCategoryType: "Select category type",
          typeFixed:
            "Category type determines which content family this category is allowed to curate. This form updates slug, premium, and active state while keeping type fixed after creation.",
          slug: "Slug",
          access: "Access",
          standard: "Standard",
          premium: "Premium",
          premiumHelp:
            "Premium categories can still be edited in admin lists and detail views.",
          availability: "Availability",
          active: "Active",
          inactive: "Inactive",
          inactiveHelp:
            "Inactive categories still appear in admin read screens for editorial cleanup.",
          cancel: "Cancel",
        };
  const form = useZodForm<CategoryFormValues>({
    schema: categoryFormSchema,
    defaultValues: initialValues,
  });
  const saveMutation = useSaveCategory(
    mode === "create"
      ? {
          mode,
          onSuccess,
        }
      : {
          mode,
          categoryId: categoryId as number,
          onSuccess,
        },
  );
  const selectedType = form.watch("type");
  const guidance = getCategoryTypeGuidance(selectedType, locale);
  const saveProblem =
    saveMutation.error instanceof ApiClientError
      ? saveMutation.error.problem
      : null;
  const alertProblem =
    saveProblem && !isProblemMappedToField(saveProblem) ? saveProblem : null;

  async function handleSubmit(values: CategoryFormValues) {
    form.clearErrors();
    saveMutation.reset();

    try {
      const savedCategory = await toastMutation(
        saveMutation.mutateAsync(values),
        {
          loading: mode === "create" ? copy.createLoading : copy.updateLoading,
          success: mode === "create" ? copy.createSuccess : copy.updateSuccess,
        },
      );

      form.reset(mapCategoryResponseToFormValues(savedCategory));
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "duplicate_category_slug") {
          form.setError("slug", {
            type: "server",
            message: copy.slugDuplicate,
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
              {copy.categoryType}
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
                    aria-label={copy.categoryType}
                    className="w-full"
                  >
                    <SelectValue placeholder={copy.selectCategoryType} />
                  </SelectTrigger>
                  <SelectContent>
                    {categoryTypeOptions.map((option) => (
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
              {copy.categoryType}
            </p>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {categoryTypeOptions.find(
                  (option) => option.value === selectedType,
                )?.label ?? selectedType}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {copy.typeFixed}
              </p>
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground" htmlFor="slug">
            {copy.slug}
          </label>
          <Input
            id="slug"
            placeholder="featured-sleep"
            {...form.register("slug")}
            disabled={saveMutation.isPending}
          />
          <FieldError error={form.formState.errors.slug} />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">
            {copy.access}
          </label>
          <Controller
            control={form.control}
            name="premium"
            render={({ field }) => (
              <Select
                disabled={saveMutation.isPending}
                value={field.value ? "true" : "false"}
                onValueChange={(value) => field.onChange(value === "true")}
              >
                <SelectTrigger aria-label={copy.access} className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="false">{copy.standard}</SelectItem>
                  <SelectItem value="true">{copy.premium}</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
          <p className="text-sm text-muted-foreground">{copy.premiumHelp}</p>
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

      <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
        <p className="text-sm font-medium text-foreground">{guidance.title}</p>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          {guidance.description}
        </p>
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
