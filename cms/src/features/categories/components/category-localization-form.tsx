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
import type { AdminCategoryLocalizationResponse } from "@/features/categories/api/category-admin";
import type { CategoryLocalizationViewModel } from "@/features/categories/model/category-view-model";
import { useCategoryLocalizationActions } from "@/features/categories/mutations/use-category-localization-actions";
import {
  categoryLocalizationFormSchema,
  categoryLocalizationStatusOptions,
  mapCategoryLocalizationResponseToFormValues,
  type CategoryLocalizationFormValues,
} from "@/features/categories/schema/category-localization-schema";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import { useRecentImageAssets } from "@/features/story-pages/queries/use-recent-image-assets";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
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
  const form = useZodForm<CategoryLocalizationFormValues>({
    schema: useMemo(() => categoryLocalizationFormSchema, []),
    defaultValues: initialValues,
  });
  const { saveLocalization } = useCategoryLocalizationActions(categoryId);
  const recentImageAssetsQuery = useRecentImageAssets();
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
          loading:
            mode === "create"
              ? "Creating category localization..."
              : "Saving category localization...",
          success:
            mode === "create"
              ? "Category localization created."
              : "Category localization saved.",
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
            message: "Language already exists for this category.",
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
        message: "Category localization could not be saved. Try again.",
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
          <label className="text-sm font-medium text-foreground" htmlFor="name">
            Name
          </label>
          <Input
            id="name"
            placeholder="Localized category name"
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
            Description
          </label>
          <Textarea
            id="description"
            placeholder="Localized category description"
            {...form.register("description")}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.description} />
        </div>

        <div className="space-y-2">
          <label
            className="text-sm font-medium text-foreground"
            htmlFor="imageMediaId"
          >
            Image asset id
          </label>
          <Input
            id="imageMediaId"
            inputMode="numeric"
            placeholder="Optional"
            type="number"
            {...form.register("imageMediaId", {
              setValueAs: (value) => {
                if (value === "" || value === null || value === undefined) {
                  return null;
                }

                return Number(value);
              },
            })}
            disabled={saveLocalization.isPending}
          />
          <FieldError error={form.formState.errors.imageMediaId} />
          <p className="text-sm text-muted-foreground">
            Category images can reference only `IMAGE` assets.
          </p>
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
              Publishing requires a timestamp. Use this when a localization is
              editorially ready for language-scoped category curation.
            </p>
            <FieldError error={form.formState.errors.publishedAt} />
          </div>
        ) : null}
      </div>

      {recentImageAssetsQuery.assets.length > 0 ? (
        <div className="space-y-2 rounded-2xl border border-border/70 bg-muted/25 px-3 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Recent Image Assets
          </p>
          <div className="flex flex-wrap gap-2">
            {recentImageAssetsQuery.assets.map((asset) => (
              <Button
                key={asset.assetId}
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  form.setValue("imageMediaId", asset.assetId, {
                    shouldDirty: true,
                  })
                }
                disabled={saveLocalization.isPending}
              >
                Asset #{asset.assetId}
              </Button>
            ))}
          </div>
        </div>
      ) : null}

      {!recentImageAssetsQuery.isLoading &&
      recentImageAssetsQuery.assets.length === 0 ? (
        <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-3 py-3 text-sm text-muted-foreground">
          No recent image assets were found. Register an image asset in Media
          before saving this localization.
        </div>
      ) : null}

      <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
        <p className="text-sm font-medium text-foreground">
          Session-backed localization workspace
        </p>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          The backend has create and update localization endpoints, but no admin
          read endpoint for category localizations yet. Tabs in this task show
          localizations created or updated in the current CMS session.
        </p>
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
