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
            mode === "create" ? "Kategori olustur" : "Metadata kaydet",
          pendingLabel:
            mode === "create"
              ? "Kategori olusturuluyor..."
              : "Metadata kaydediliyor...",
          createLoading: "Kategori kaydi olusturuluyor...",
          updateLoading: "Kategori metadata'si kaydediliyor...",
          createSuccess: "Kategori kaydi olusturuldu.",
          updateSuccess: "Kategori metadata'si kaydedildi.",
          slugDuplicate: "Slug zaten kullaniliyor.",
          genericSaveError:
            "Kategori degisiklikleri kaydedilemedi. Tekrar deneyin.",
          categoryType: "Kategori turu",
          selectCategoryType: "Kategori turu secin",
          typeFixed:
            "Kategori turu olusturulduktan sonra sabittir. Bu form slug, premium ve aktiflik durumunu gunceller.",
          slug: "Slug",
          access: "Erisim",
          standard: "Standart",
          premium: "Premium",
          premiumHelp:
            "Premium kategoriler admin ekranlarinda duzenlenmeye devam eder.",
          availability: "Erisim durumu",
          active: "Aktif",
          inactive: "Pasif",
          inactiveHelp:
            "Pasif kategoriler admin ekranlarinda gorunmeye devam eder.",
          cancel: "Iptal",
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
            "Category type is fixed after creation. This form updates slug, premium, and active state only.",
          slug: "Slug",
          access: "Access",
          standard: "Standard",
          premium: "Premium",
          premiumHelp:
            "Premium categories can still be edited in admin screens.",
          availability: "Availability",
          active: "Active",
          inactive: "Inactive",
          inactiveHelp:
            "Inactive categories still appear in admin read screens.",
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
