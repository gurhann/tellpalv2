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

function getCategoryTypeGuidance(type: CategoryFormValues["type"]) {
  const guidanceByType: Record<
    CategoryFormValues["type"],
    { title: string; description: string }
  > = {
    STORY: {
      title: "Story category workflow",
      description:
        "Story categories curate only STORY records. Localization workspaces publish the category itself, and curation will later accept only story content in matching languages.",
    },
    AUDIO_STORY: {
      title: "Audio story category workflow",
      description:
        "Audio story categories are reserved for AUDIO_STORY records. Use this when editorial grouping and future curation should stay limited to narrated long-form audio content.",
    },
    MEDITATION: {
      title: "Meditation category workflow",
      description:
        "Meditation categories curate only MEDITATION records. Keep the type aligned here so future curation and discovery screens cannot mix in other content families.",
    },
    LULLABY: {
      title: "Lullaby category workflow",
      description:
        "Lullaby categories curate only LULLABY records. Localization and curation will stay scoped to lullaby-ready editorial collections across supported languages.",
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
  submitLabel = mode === "create" ? "Create category" : "Save metadata",
  pendingLabel = mode === "create"
    ? "Creating category..."
    : "Saving metadata...",
}: CategoryFormProps) {
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
  const guidance = getCategoryTypeGuidance(selectedType);
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
          loading:
            mode === "create"
              ? "Creating category record..."
              : "Saving category metadata...",
          success:
            mode === "create"
              ? "Category record created."
              : "Category metadata saved.",
        },
      );

      form.reset(mapCategoryResponseToFormValues(savedCategory));
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "duplicate_category_slug") {
          form.setError("slug", {
            type: "server",
            message: "Slug is already in use.",
          });
          return;
        }

        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: "Category changes could not be saved. Try again.",
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
              Category type
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
                    aria-label="Category type"
                    className="w-full"
                  >
                    <SelectValue placeholder="Select category type" />
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
            <p className="text-sm font-medium text-foreground">Category type</p>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              <p className="text-sm font-medium text-foreground">
                {categoryTypeOptions.find(
                  (option) => option.value === selectedType,
                )?.label ?? selectedType}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Category type determines which content family this category is
                allowed to curate. This form updates slug, premium, and active
                state while keeping type fixed after creation.
              </p>
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground" htmlFor="slug">
            Slug
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
          <label className="text-sm font-medium text-foreground">Access</label>
          <Controller
            control={form.control}
            name="premium"
            render={({ field }) => (
              <Select
                disabled={saveMutation.isPending}
                value={field.value ? "true" : "false"}
                onValueChange={(value) => field.onChange(value === "true")}
              >
                <SelectTrigger aria-label="Access" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="false">Standard</SelectItem>
                  <SelectItem value="true">Premium</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
          <p className="text-sm text-muted-foreground">
            Premium categories can still be edited in admin lists and detail
            views.
          </p>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-foreground">
            Availability
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
                <SelectTrigger aria-label="Availability" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">Active</SelectItem>
                  <SelectItem value="false">Inactive</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
          <p className="text-sm text-muted-foreground">
            Inactive categories still appear in admin read screens for editorial
            cleanup.
          </p>
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
            Cancel
          </Button>
        ) : null}
        <SubmitButton
          isPending={saveMutation.isPending}
          pendingLabel={pendingLabel}
        >
          {submitLabel}
        </SubmitButton>
      </div>
    </form>
  );
}
