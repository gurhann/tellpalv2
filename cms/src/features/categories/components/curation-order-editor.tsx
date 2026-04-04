import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type {
  CategoryCurationItemViewModel,
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";
import { useCategoryCurationActions } from "@/features/categories/mutations/use-category-curation-actions";
import { ApiClientError } from "@/lib/http/client";

type CurationOrderEditorProps = {
  category: CategorySummaryViewModel;
  items: CategoryCurationItemViewModel[];
  localization: CategoryLocalizationViewModel;
};

type CurationOrderRowProps = {
  disabled: boolean;
  item: CategoryCurationItemViewModel;
  items: CategoryCurationItemViewModel[];
  localization: CategoryLocalizationViewModel;
};

function CurationOrderRow({
  disabled,
  item,
  items,
  localization,
}: CurationOrderRowProps) {
  const [displayOrder, setDisplayOrder] = useState(
    item.displayOrder.toString(),
  );
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [problemMessage, setProblemMessage] = useState<string | null>(null);
  const { updateCuratedContentOrder } = useCategoryCurationActions({
    categoryId: item.categoryId,
    languageCode: localization.languageCode,
  });

  async function handleSave() {
    setFieldError(null);
    setProblemMessage(null);
    updateCuratedContentOrder.reset();

    const nextDisplayOrder = Number(displayOrder);

    if (!Number.isInteger(nextDisplayOrder) || nextDisplayOrder < 0) {
      setFieldError("Display order must be zero or greater.");
      return;
    }

    if (
      items.some(
        (candidate) =>
          candidate.contentId !== item.contentId &&
          candidate.displayOrder === nextDisplayOrder,
      )
    ) {
      setFieldError(
        "Display order is already used in the current session curation set.",
      );
      return;
    }

    try {
      await updateCuratedContentOrder.mutateAsync({
        contentId: item.contentId,
        displayOrder: nextDisplayOrder,
      });
    } catch (error) {
      if (error instanceof ApiClientError) {
        setProblemMessage(error.problem.detail);
        return;
      }

      setProblemMessage("Display order could not be updated. Try again.");
    }
  }

  const hasChanged = Number(displayOrder) !== item.displayOrder;

  return (
    <div className="rounded-2xl border border-border/70 bg-background px-4 py-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm font-medium text-foreground">
            Content #{item.contentId}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Current session row in the {localization.languageLabel} curation
            lane.
          </p>
        </div>

        <div className="grid gap-2 sm:grid-cols-[minmax(0,10rem)_auto]">
          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor={`curation-order-${item.contentId}`}
            >
              Display order
            </label>
            <Input
              id={`curation-order-${item.contentId}`}
              inputMode="numeric"
              min={0}
              type="number"
              value={displayOrder}
              onChange={(event) => setDisplayOrder(event.target.value)}
              disabled={disabled || updateCuratedContentOrder.isPending}
            />
            <FieldError
              error={fieldError ? { message: fieldError } : undefined}
            />
          </div>

          <div className="flex items-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => void handleSave()}
              disabled={
                disabled || updateCuratedContentOrder.isPending || !hasChanged
              }
            >
              Save order
            </Button>
          </div>
        </div>
      </div>

      {problemMessage ? (
        <div className="mt-4">
          <ProblemAlert
            description={problemMessage}
            title="Display order update failed"
          />
        </div>
      ) : null}
    </div>
  );
}

export function CurationOrderEditor({
  category,
  items,
  localization,
}: CurationOrderEditorProps) {
  const isLocked = !localization.isPublished;

  return (
    <div
      className="space-y-4 rounded-3xl border border-border/70 bg-muted/20 px-4 py-5"
      id={`curation-order-editor-${localization.languageCode}`}
    >
      <div>
        <h3 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          Display order editor
        </h3>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          Reorder only the current-session curated rows for{" "}
          {localization.languageLabel}. Existing backend curation rows stay
          hidden until the dedicated admin read endpoint lands in the next task.
        </p>
      </div>

      {isLocked ? (
        <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-4 py-4 text-sm text-muted-foreground">
          Publish the {localization.languageLabel} category localization before
          reordering curated {category.typeLabel.toLowerCase()} rows.
        </div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border/80 bg-background px-4 py-6 text-sm text-muted-foreground">
          No curated rows have been added in this session yet. Use `Add curated
          content` first, then adjust display order here.
        </div>
      ) : (
        <div className="grid gap-3">
          {items.map((item) => (
            <CurationOrderRow
              key={`${item.languageCode}-${item.contentId}-${item.displayOrder}`}
              disabled={isLocked}
              item={item}
              items={items}
              localization={localization}
            />
          ))}
        </div>
      )}
    </div>
  );
}
