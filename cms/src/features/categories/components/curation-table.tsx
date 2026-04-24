import { useState } from "react";
import { Trash2 } from "lucide-react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
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
import type { ApiProblemDetail } from "@/types/api";

type CurationTableProps = {
  category: CategorySummaryViewModel;
  items: CategoryCurationItemViewModel[];
  isLoading: boolean;
  localization: CategoryLocalizationViewModel;
  problem: ApiProblemDetail | null;
  onRetry: () => void;
};

export function CurationTable({
  category,
  items,
  isLoading,
  localization,
  problem,
  onRetry,
}: CurationTableProps) {
  const [pendingRemoval, setPendingRemoval] =
    useState<CategoryCurationItemViewModel | null>(null);
  const [removeProblemMessage, setRemoveProblemMessage] = useState<
    string | null
  >(null);
  const { removeCuratedContent } = useCategoryCurationActions({
    categoryId: category.id,
    languageCode: localization.languageCode,
  });

  const columns: DataTableColumn<CategoryCurationItemViewModel>[] = [
    {
      id: "content",
      header: "Content",
      cell: (item) => (
        <CuratedContentIdentity
          contentId={item.contentId}
          externalKey={item.externalKey}
          languageLabel={localization.languageLabel}
          localizedTitle={item.localizedTitle}
        />
      ),
    },
    {
      id: "order",
      header: "Display order",
      cell: (item) => (
        <span className="font-medium text-foreground">{item.displayOrder}</span>
      ),
      align: "center",
    },
    {
      id: "actions",
      header: "Actions",
      cell: (item) => (
        <div className="flex justify-end">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setRemoveProblemMessage(null);
              removeCuratedContent.reset();
              setPendingRemoval(item);
            }}
            disabled={removeCuratedContent.isPending}
          >
            <Trash2 className="size-4" />
            Remove
          </Button>
        </div>
      ),
      align: "right",
      cellClassName: "w-32",
    },
  ];

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

  return (
    <>
      <DataTable
        columns={columns}
        rows={items}
        getRowId={(item) => `${item.languageCode}-${item.contentId}`}
        isLoading={isLoading}
        problem={problem}
        onRetry={onRetry}
        loadingTitle="Loading curated content"
        loadingDescription={`The CMS is requesting the current ${localization.languageLabel} curation lane for category #${category.id}.`}
        emptyTitle="No curated content yet"
        emptyDescription={`No ${localization.languageLabel} curated ${category.typeLabel.toLowerCase()} rows are stored for this category yet.`}
        caption={`${localization.languageLabel} curated content table`}
      />

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
