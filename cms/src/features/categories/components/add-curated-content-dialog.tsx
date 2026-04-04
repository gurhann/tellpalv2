import { useMemo, useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyApiClientErrorToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import type {
  CategoryCurationItemViewModel,
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";
import { useCategoryCurationActions } from "@/features/categories/mutations/use-category-curation-actions";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { z } from "zod";

const addCuratedContentSchema = z.object({
  contentId: z
    .number({
      error: "Content id is required.",
    })
    .int("Content id must be a whole number.")
    .positive("Content id must be positive."),
  displayOrder: z
    .number({
      error: "Display order is required.",
    })
    .int("Display order must be a whole number.")
    .min(0, "Display order must not be negative."),
});

type AddCuratedContentFormValues = z.infer<typeof addCuratedContentSchema>;

type AddCuratedContentDialogProps = {
  category: CategorySummaryViewModel;
  existingItems: CategoryCurationItemViewModel[];
  localization: CategoryLocalizationViewModel;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function getDefaultValues(
  existingItems: CategoryCurationItemViewModel[],
): AddCuratedContentFormValues {
  const nextDisplayOrder =
    existingItems.length === 0
      ? 0
      : Math.max(...existingItems.map((item) => item.displayOrder)) + 1;

  return {
    contentId: 0,
    displayOrder: nextDisplayOrder,
  };
}

function getEligibleContents(
  contents: ContentReadViewModel[],
  category: CategorySummaryViewModel,
  localization: CategoryLocalizationViewModel,
) {
  return contents
    .filter((content) => {
      if (!content.summary.active || content.summary.type !== category.type) {
        return false;
      }

      return content.localizations.some(
        (contentLocalization) =>
          contentLocalization.languageCode === localization.languageCode &&
          contentLocalization.isPublished,
      );
    })
    .sort((left, right) =>
      left.summary.externalKey.localeCompare(right.summary.externalKey),
    );
}

export function AddCuratedContentDialog({
  category,
  existingItems,
  localization,
  open,
  onOpenChange,
}: AddCuratedContentDialogProps) {
  const [problemMessage, setProblemMessage] = useState<string | null>(null);
  const defaultValues = useMemo(
    () => getDefaultValues(existingItems),
    [existingItems],
  );
  const form = useZodForm<AddCuratedContentFormValues>({
    schema: addCuratedContentSchema,
    defaultValues,
  });
  const contentListQuery = useContentList();
  const { addCuratedContent } = useCategoryCurationActions({
    categoryId: category.id,
    languageCode: localization.languageCode,
  });
  const eligibleContents = getEligibleContents(
    contentListQuery.contents,
    category,
    localization,
  );
  const existingContentIds = new Set(
    existingItems.map((item) => item.contentId),
  );
  const suggestedContents = eligibleContents.filter(
    (content) => !existingContentIds.has(content.summary.id),
  );

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.reset(getDefaultValues(existingItems));
      form.clearErrors();
      setProblemMessage(null);
      addCuratedContent.reset();
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: AddCuratedContentFormValues) {
    form.clearErrors();
    setProblemMessage(null);
    addCuratedContent.reset();

    if (existingContentIds.has(values.contentId)) {
      form.setError("contentId", {
        type: "manual",
        message: "This content is already in the current curation lane.",
      });
      return;
    }

    if (
      existingItems.some((item) => item.displayOrder === values.displayOrder)
    ) {
      form.setError("displayOrder", {
        type: "manual",
        message: "Display order is already used in the current curation lane.",
      });
      return;
    }

    try {
      await toastMutation(addCuratedContent.mutateAsync(values), {
        loading: "Adding curated content...",
        success: "Curated content added.",
      });

      handleOpenChange(false);
    } catch (error) {
      const problem = applyApiClientErrorToForm(form.setError, error);

      if (problem) {
        switch (problem.errorCode) {
          case "category_content_type_mismatch":
          case "content_not_found":
          case "content_inactive":
          case "content_localization_not_published":
          case "category_content_not_found":
            form.setError("contentId", {
              type: "server",
              message: problem.detail,
            });
            return;
          default:
            break;
        }
      }

      setProblemMessage(
        error instanceof Error
          ? error.message
          : "Curated content could not be added. Try again.",
      );
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Add curated content</DialogTitle>
          <DialogDescription>
            Add one published {category.typeLabel.toLowerCase()} record to the{" "}
            {localization.languageLabel} category lane. This dialog validates
            against the currently hydrated curation rows for this language.
          </DialogDescription>
        </DialogHeader>

        <form
          className="grid gap-5"
          noValidate
          onSubmit={form.handleSubmit(handleSubmit)}
        >
          {problemMessage ? (
            <ProblemAlert
              description={problemMessage}
              title="Curated content could not be added"
            />
          ) : null}

          <div className="grid gap-5 md:grid-cols-2">
            <div className="space-y-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="category-curation-content-id"
              >
                Content id
              </label>
              <Input
                id="category-curation-content-id"
                inputMode="numeric"
                min={1}
                type="number"
                {...form.register("contentId", {
                  setValueAs: (value) => Number(value),
                })}
                disabled={addCuratedContent.isPending}
              />
              <p className="text-sm text-muted-foreground">
                Provide a published {localization.languageLabel} content record
                whose type matches {category.typeLabel}.
              </p>
              <FieldError error={form.formState.errors.contentId} />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="category-curation-display-order"
              >
                Display order
              </label>
              <Input
                id="category-curation-display-order"
                inputMode="numeric"
                min={0}
                type="number"
                {...form.register("displayOrder", {
                  setValueAs: (value) => Number(value),
                })}
                disabled={addCuratedContent.isPending}
              />
              <p className="text-sm text-muted-foreground">
                Display orders must stay unique within the{" "}
                {localization.languageLabel} curation lane.
              </p>
              <FieldError error={form.formState.errors.displayOrder} />
            </div>
          </div>

          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Eligible published {category.typeLabel.toLowerCase()} records in{" "}
              {localization.languageLabel}
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              The picker uses the shared content registry because category
              curation has no dedicated search endpoint yet. You can still enter
              a known content id manually.
            </p>

            {contentListQuery.problem ? (
              <div className="mt-4">
                <ProblemAlert problem={contentListQuery.problem} />
              </div>
            ) : null}

            {contentListQuery.isLoading ? (
              <p className="mt-4 text-sm text-muted-foreground">
                Loading content registry...
              </p>
            ) : suggestedContents.length > 0 ? (
              <div className="mt-4 flex flex-wrap gap-2">
                {suggestedContents.map((content) => (
                  <Button
                    key={content.summary.id}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      form.setValue("contentId", content.summary.id, {
                        shouldDirty: true,
                        shouldValidate: true,
                      })
                    }
                    disabled={addCuratedContent.isPending}
                  >
                    #{content.summary.id} {content.summary.externalKey}
                  </Button>
                ))}
              </div>
            ) : (
              <p className="mt-4 text-sm text-muted-foreground">
                No recent published {category.typeLabel.toLowerCase()} records
                were found for {localization.languageLabel}. Manual content id
                entry remains available.
              </p>
            )}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={addCuratedContent.isPending}
            >
              Cancel
            </Button>
            <SubmitButton
              isPending={addCuratedContent.isPending}
              pendingLabel="Adding curated content..."
            >
              Add curated content
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
