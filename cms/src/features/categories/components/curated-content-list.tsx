import {
  closestCenter,
  DndContext,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  ArrowDown,
  ArrowUp,
  GripVertical,
  RefreshCcw,
  Trash2,
} from "lucide-react";
import { useMemo, useState, type KeyboardEvent } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import { CuratedContentIdentity } from "@/features/categories/components/curated-content-identity";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type {
  CategoryCurationItemViewModel,
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";
import { useCategoryCurationActions } from "@/features/categories/mutations/use-category-curation-actions";
import { cn } from "@/lib/utils";
import type { ApiProblemDetail } from "@/types/api";

type CuratedContentListProps = {
  category: CategorySummaryViewModel;
  items: CategoryCurationItemViewModel[];
  isLoading: boolean;
  localization: CategoryLocalizationViewModel;
  problem: ApiProblemDetail | null;
  onRetry: () => void;
};

type SortableCuratedContentRowProps = {
  currentIndex: number;
  disabled: boolean;
  item: CategoryCurationItemViewModel;
  localization: CategoryLocalizationViewModel;
  onMoveByKeyboard: (
    item: CategoryCurationItemViewModel,
    direction: "up" | "down",
  ) => void;
  onRemove: (item: CategoryCurationItemViewModel) => void;
  pending: boolean;
  totalCount: number;
};

function sortCurationItems(items: CategoryCurationItemViewModel[]) {
  return [...items].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder;
    }

    return left.contentId - right.contentId;
  });
}

function SortableCuratedContentRow({
  currentIndex,
  disabled,
  item,
  localization,
  onMoveByKeyboard,
  onRemove,
  pending,
  totalCount,
}: SortableCuratedContentRowProps) {
  const {
    attributes,
    isDragging,
    listeners,
    setActivatorNodeRef,
    setNodeRef,
    transform,
    transition,
  } = useSortable({
    id: item.contentId,
    disabled,
  });

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    listeners?.onKeyDown?.(event);

    if (disabled || pending) {
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      onMoveByKeyboard(item, "up");
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      onMoveByKeyboard(item, "down");
    }
  }

  return (
    <li
      ref={setNodeRef}
      className={cn(
        "flex items-center gap-3 rounded-2xl border border-border/70 bg-background px-4 py-4 shadow-sm transition-shadow",
        isDragging && "shadow-lg ring-1 ring-primary/20",
      )}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
      }}
    >
      <Button
        ref={setActivatorNodeRef}
        type="button"
        variant="ghost"
        size="icon"
        className="size-9 shrink-0 rounded-xl text-muted-foreground"
        aria-label={`Reorder ${item.localizedTitle?.trim() || item.externalKey}`}
        disabled={disabled || pending}
        {...attributes}
        {...listeners}
        onKeyDown={handleKeyDown}
      >
        <GripVertical className="size-4" />
      </Button>

      <CuratedContentIdentity
        className="min-w-0 flex-1"
        contentId={item.contentId}
        externalKey={item.externalKey}
        languageLabel={localization.languageLabel}
        localizedTitle={item.localizedTitle}
      />

      <div className="hidden min-w-20 shrink-0 text-right sm:block">
        <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
          Order
        </p>
        <p className="mt-1 font-medium text-foreground">{item.displayOrder}</p>
      </div>

      <div className="hidden items-center gap-1 sm:flex">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="size-8 rounded-lg text-muted-foreground"
          aria-label={`Move ${item.localizedTitle?.trim() || item.externalKey} up`}
          disabled={disabled || pending || currentIndex === 0}
          onClick={() => onMoveByKeyboard(item, "up")}
        >
          <ArrowUp className="size-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="size-8 rounded-lg text-muted-foreground"
          aria-label={`Move ${item.localizedTitle?.trim() || item.externalKey} down`}
          disabled={disabled || pending || currentIndex === totalCount - 1}
          onClick={() => onMoveByKeyboard(item, "down")}
        >
          <ArrowDown className="size-4" />
        </Button>
      </div>

      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={pending}
        onClick={() => onRemove(item)}
      >
        <Trash2 className="size-4" />
        Remove
      </Button>
    </li>
  );
}

export function CuratedContentList({
  category,
  items,
  isLoading,
  localization,
  problem,
  onRetry,
}: CuratedContentListProps) {
  const [pendingRemoval, setPendingRemoval] =
    useState<CategoryCurationItemViewModel | null>(null);
  const [removeProblemMessage, setRemoveProblemMessage] = useState<
    string | null
  >(null);
  const sortedItems = useMemo(() => sortCurationItems(items), [items]);
  const { removeCuratedContent, reorderCuratedContent } =
    useCategoryCurationActions({
      categoryId: category.id,
      languageCode: localization.languageCode,
    });
  const isPending =
    removeCuratedContent.isPending || reorderCuratedContent.isPending;
  const dragEnabled =
    localization.isPublished && sortedItems.length > 1 && !isPending;
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 6,
      },
    }),
  );

  async function submitReorder(nextItems: CategoryCurationItemViewModel[]) {
    try {
      await toastMutation(
        reorderCuratedContent.mutateAsync({
          items: nextItems,
        }),
        {
          loading: "Saving curated order...",
          success: "Curated order updated.",
        },
      );
    } catch {
      // The mutation hook restores the previous server state on failure.
    }
  }

  async function handleConfirmRemove() {
    if (!pendingRemoval) {
      return;
    }

    setRemoveProblemMessage(null);
    removeCuratedContent.reset();

    try {
      await toastMutation(
        removeCuratedContent.mutateAsync({
          contentId: pendingRemoval.contentId,
        }),
        {
          loading: "Removing curated content...",
          success: "Curated content removed.",
        },
      );
      setPendingRemoval(null);
    } catch (error) {
      setRemoveProblemMessage(
        error instanceof Error
          ? error.message
          : "Curated content could not be removed. Try again.",
      );
    }
  }

  function handleDialogOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      setPendingRemoval(null);
      setRemoveProblemMessage(null);
      removeCuratedContent.reset();
    }
  }

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;

    if (
      !dragEnabled ||
      over === null ||
      active.id === over.id ||
      typeof active.id !== "number" ||
      typeof over.id !== "number"
    ) {
      return;
    }

    const activeIndex = sortedItems.findIndex(
      (item) => item.contentId === active.id,
    );
    const overIndex = sortedItems.findIndex(
      (item) => item.contentId === over.id,
    );

    if (activeIndex === -1 || overIndex === -1) {
      return;
    }

    const reorderedItems = arrayMove(sortedItems, activeIndex, overIndex).map(
      (item, index) => ({
        ...item,
        displayOrder: index,
      }),
    );

    await submitReorder(reorderedItems);
  }

  async function handleKeyboardReorder(
    item: CategoryCurationItemViewModel,
    direction: "up" | "down",
  ) {
    if (!dragEnabled) {
      return;
    }

    const currentIndex = sortedItems.findIndex(
      (candidate) => candidate.contentId === item.contentId,
    );

    if (currentIndex === -1) {
      return;
    }

    const targetIndex =
      direction === "up" ? currentIndex - 1 : currentIndex + 1;

    if (targetIndex < 0 || targetIndex >= sortedItems.length) {
      return;
    }

    const reorderedItems = arrayMove(
      sortedItems,
      currentIndex,
      targetIndex,
    ).map((candidate, index) => ({
      ...candidate,
      displayOrder: index,
    }));

    await submitReorder(reorderedItems);
  }

  return (
    <>
      <section className="space-y-4 rounded-3xl border border-border/70 bg-muted/20 px-4 py-5">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h3 className="font-heading text-lg font-semibold tracking-tight text-foreground">
              Curated content
            </h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {dragEnabled
                ? `Drag rows to reorder ${localization.languageLabel} curated ${category.typeLabel.toLowerCase()} items.`
                : `${sortedItems.length} curated row${sortedItems.length === 1 ? "" : "s"} loaded for ${localization.languageLabel}.`}
            </p>
          </div>

          {sortedItems.length > 0 ? (
            <div className="rounded-full border border-border/70 bg-background px-3 py-1 text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
              {sortedItems.length} item{sortedItems.length === 1 ? "" : "s"}
            </div>
          ) : null}
        </div>

        {problem ? (
          <ProblemAlert
            problem={problem}
            actions={
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onRetry}
              >
                <RefreshCcw className="size-4" />
                Retry
              </Button>
            }
          />
        ) : isLoading ? (
          <EmptyState
            className="min-h-44"
            title="Loading curated content"
            description={`The CMS is requesting the current ${localization.languageLabel} curation lane for category #${category.id}.`}
          />
        ) : sortedItems.length === 0 ? (
          <EmptyState
            className="min-h-44"
            title="No curated content yet"
            description={`No ${localization.languageLabel} curated ${category.typeLabel.toLowerCase()} rows are stored for this category yet.`}
          />
        ) : (
          <DndContext
            collisionDetection={closestCenter}
            onDragEnd={(event) => void handleDragEnd(event)}
            sensors={sensors}
          >
            <SortableContext
              items={sortedItems.map((item) => item.contentId)}
              strategy={verticalListSortingStrategy}
            >
              <ol
                className="grid gap-3"
                aria-label={`${localization.languageLabel} curated content list`}
              >
                {sortedItems.map((item, index) => (
                  <SortableCuratedContentRow
                    key={`${item.languageCode}-${item.contentId}`}
                    currentIndex={index}
                    disabled={!dragEnabled}
                    item={item}
                    localization={localization}
                    onMoveByKeyboard={(record, direction) => {
                      void handleKeyboardReorder(record, direction);
                    }}
                    onRemove={(record) => {
                      setRemoveProblemMessage(null);
                      removeCuratedContent.reset();
                      setPendingRemoval(record);
                    }}
                    pending={isPending}
                    totalCount={sortedItems.length}
                  />
                ))}
              </ol>
            </SortableContext>
          </DndContext>
        )}
      </section>

      <Dialog
        open={pendingRemoval !== null}
        onOpenChange={handleDialogOpenChange}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Remove curated content</DialogTitle>
            <DialogDescription>
              Remove{" "}
              {pendingRemoval?.localizedTitle?.trim() ||
                pendingRemoval?.externalKey ||
                `content #${pendingRemoval?.contentId ?? "?"}`}{" "}
              from the {localization.languageLabel} curation lane for category #
              {category.id}.
            </DialogDescription>
          </DialogHeader>

          <DialogBody className="grid gap-4">
            {pendingRemoval ? (
              <div className="rounded-2xl border border-border/70 bg-muted/15 px-4 py-4">
                <CuratedContentIdentity
                  contentId={pendingRemoval.contentId}
                  externalKey={pendingRemoval.externalKey}
                  languageLabel={localization.languageLabel}
                  localizedTitle={pendingRemoval.localizedTitle}
                />
              </div>
            ) : null}
            {removeProblemMessage ? (
              <ProblemAlert
                description={removeProblemMessage}
                title="Curated content could not be removed"
              />
            ) : null}
          </DialogBody>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleDialogOpenChange(false)}
              disabled={removeCuratedContent.isPending}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="destructive"
              onClick={() => void handleConfirmRemove()}
              disabled={removeCuratedContent.isPending}
            >
              Remove content
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
