import { useDeferredValue, useMemo, useState } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
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
  EligibleCategoryContentViewModel,
} from "@/features/categories/model/category-view-model";
import { useCategoryCurationActions } from "@/features/categories/mutations/use-category-curation-actions";
import { useEligibleCategoryContents } from "@/features/categories/queries/use-eligible-category-contents";
import { z } from "zod";

const addCuratedContentSchema = z.object({
  selectedContentId: z
    .number({
      error: "Select a content record to add.",
    })
    .int("Select a valid content record.")
    .positive("Select a valid content record.")
    .nullable()
    .refine((value) => value !== null, {
      message: "Select a content record to add.",
    }),
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

type SelectedContentSummary = {
  contentId: number;
  externalKey: string;
  localizedTitle: string;
  languageLabel: string;
};

function getDefaultValues(
  existingItems: CategoryCurationItemViewModel[],
): AddCuratedContentFormValues {
  const nextDisplayOrder =
    existingItems.length === 0
      ? 0
      : Math.max(...existingItems.map((item) => item.displayOrder)) + 1;

  return {
    selectedContentId: null,
    displayOrder: nextDisplayOrder,
  };
}

function toSelectedContentSummary(
  content: EligibleCategoryContentViewModel,
): SelectedContentSummary {
  return {
    contentId: content.contentId,
    externalKey: content.externalKey,
    localizedTitle: content.localizedTitle,
    languageLabel: content.languageLabel,
  };
}

function formatPublishedAt(publishedAt: string | null) {
  if (!publishedAt) {
    return "Published";
  }

  return `Published ${new Date(publishedAt).toLocaleString()}`;
}

export function AddCuratedContentDialog({
  category,
  existingItems,
  localization,
  open,
  onOpenChange,
}: AddCuratedContentDialogProps) {
  const [problemMessage, setProblemMessage] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [selectedContentSummary, setSelectedContentSummary] =
    useState<SelectedContentSummary | null>(null);
  const deferredSearch = useDeferredValue(search);
  const defaultValues = useMemo(
    () => getDefaultValues(existingItems),
    [existingItems],
  );
  const form = useZodForm<AddCuratedContentFormValues>({
    schema: addCuratedContentSchema,
    defaultValues,
  });
  const { addCuratedContent } = useCategoryCurationActions({
    categoryId: category.id,
    languageCode: localization.languageCode,
  });
  const eligibleContentsQuery = useEligibleCategoryContents({
    categoryId: category.id,
    languageCode: localization.languageCode,
    search: deferredSearch,
    enabled: open,
  });
  const existingContentIds = new Set(
    existingItems.map((item) => item.contentId),
  );
  const selectedContentId = form.watch("selectedContentId");

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.reset(getDefaultValues(existingItems));
      form.clearErrors();
      setProblemMessage(null);
      setSearch("");
      setSelectedContentSummary(null);
      addCuratedContent.reset();
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: AddCuratedContentFormValues) {
    form.clearErrors();
    setProblemMessage(null);
    addCuratedContent.reset();

    if (values.selectedContentId === null) {
      form.setError("selectedContentId", {
        type: "manual",
        message: "Select a content record to add.",
      });
      return;
    }

    if (existingContentIds.has(values.selectedContentId)) {
      form.setError("selectedContentId", {
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
      await toastMutation(
        addCuratedContent.mutateAsync({
          contentId: values.selectedContentId,
          displayOrder: values.displayOrder,
        }),
        {
          loading: "Adding curated content...",
          success: "Curated content added.",
        },
      );

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
            form.setError("selectedContentId", {
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
            Select one published {category.typeLabel.toLowerCase()} record for
            the {localization.languageLabel} category lane. Only addable
            content for this type and language is listed here.
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

          <div className="grid gap-5 md:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
            <div className="space-y-3">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="category-curation-search"
              >
                Search eligible content
              </label>
              <Input
                id="category-curation-search"
                aria-label="Search eligible content"
                placeholder={`Search ${category.typeLabel.toLowerCase()} title, external key, or content id`}
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                disabled={addCuratedContent.isPending}
              />
              <p className="text-sm text-muted-foreground">
                Results are limited to published {localization.languageLabel}{" "}
                {category.typeLabel.toLowerCase()} records that are not already
                in this lane.
              </p>
              <FieldError error={form.formState.errors.selectedContentId} />
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

          {selectedContentSummary ? (
            <div className="rounded-2xl border border-primary/20 bg-primary/5 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Selected content
              </p>
              <p className="mt-2 text-sm font-medium text-foreground">
                {selectedContentSummary.localizedTitle}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                #{selectedContentSummary.contentId} ·{" "}
                {selectedContentSummary.externalKey} ·{" "}
                {selectedContentSummary.languageLabel}
              </p>
            </div>
          ) : null}

          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Eligible published {category.typeLabel.toLowerCase()} records in{" "}
              {localization.languageLabel}
            </p>

            {eligibleContentsQuery.problem ? (
              <div className="mt-4">
                <ProblemAlert problem={eligibleContentsQuery.problem} />
              </div>
            ) : eligibleContentsQuery.isLoading ? (
              <EmptyState
                className="mt-4 min-h-44"
                title="Loading eligible content"
                description={`The CMS is requesting published ${localization.languageLabel} ${category.typeLabel.toLowerCase()} candidates from the admin API.`}
              />
            ) : eligibleContentsQuery.items.length > 0 ? (
              <div className="mt-4 grid max-h-[22rem] gap-3 overflow-y-auto pr-1">
                {eligibleContentsQuery.items.map((content) => {
                  const isSelected = content.contentId === selectedContentId;

                  return (
                    <button
                      key={content.contentId}
                      type="button"
                      className={`rounded-2xl border px-4 py-4 text-left shadow-sm transition-colors ${
                        isSelected
                          ? "border-primary/30 bg-primary/5"
                          : "border-border/70 bg-card/90 hover:border-primary/20"
                      }`}
                      onClick={() => {
                        form.setValue("selectedContentId", content.contentId, {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                        setSelectedContentSummary(
                          toSelectedContentSummary(content),
                        );
                      }}
                      disabled={addCuratedContent.isPending}
                    >
                      <p className="text-sm font-medium text-foreground">
                        {content.localizedTitle}
                      </p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        #{content.contentId} · {content.externalKey}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {formatPublishedAt(content.publishedAt)}
                      </p>
                    </button>
                  );
                })}
              </div>
            ) : (
              <EmptyState
                className="mt-4 min-h-44"
                title="No eligible content found"
                description={
                  deferredSearch.trim().length > 0
                    ? `No published ${category.typeLabel.toLowerCase()} records matched the current search for ${localization.languageLabel}.`
                    : `No published ${category.typeLabel.toLowerCase()} records are currently available for ${localization.languageLabel} in this curation lane.`
                }
              />
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
              disabled={selectedContentId === null}
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
