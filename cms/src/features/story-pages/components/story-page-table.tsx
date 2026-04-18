import { Languages, Pencil, Sparkles, Trash2 } from "lucide-react";
import type { ReactNode } from "react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type { StoryPageReadViewModel } from "@/features/contents/model/content-view-model";
import type { ApiProblemDetail } from "@/types/api";

type StoryPageTableProps = {
  storyPages: StoryPageReadViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onEditStoryPage?: (storyPage: StoryPageReadViewModel) => void;
  onDeleteStoryPage?: (storyPage: StoryPageReadViewModel) => void;
  emptyAction?: ReactNode;
  isMutationPending?: boolean;
};

function getLocalizationSummary(storyPage: StoryPageReadViewModel) {
  if (storyPage.localizationCount === 0) {
    return {
      title: "No localizations yet",
      detail: "Open Edit to create localized page payloads.",
    };
  }

  return {
    title: `${storyPage.localizationCount} locale${
      storyPage.localizationCount === 1 ? "" : "s"
    }`,
    detail: storyPage.localizations
      .map((localization) => localization.languageCode.toUpperCase())
      .join(", "),
  };
}

function createColumns({
  onEditStoryPage,
  onDeleteStoryPage,
  isMutationPending,
}: Pick<
  StoryPageTableProps,
  "onEditStoryPage" | "onDeleteStoryPage" | "isMutationPending"
>) {
  return [
    {
      id: "page",
      header: "Page",
      cell: (storyPage) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">
            Page {storyPage.pageNumber}
          </p>
          <p className="text-xs text-muted-foreground">
            Content #{storyPage.contentId}
          </p>
        </div>
      ),
    },
    {
      id: "localizations",
      header: "Localizations",
      cell: (storyPage) => (
        <div className="space-y-1">
          <p className="inline-flex items-center gap-2 text-sm font-medium text-foreground">
            <Languages className="size-4 text-primary" />
            {getLocalizationSummary(storyPage).title}
          </p>
          <p className="text-xs text-muted-foreground">
            {getLocalizationSummary(storyPage).detail}
          </p>
        </div>
      ),
    },
    {
      id: "illustrations",
      header: "Illustration Coverage",
      cell: (storyPage) => {
        const missingCodes = storyPage.localizations
          .filter((localization) => !localization.hasIllustration)
          .map((localization) => localization.languageCode.toUpperCase());

        return (
          <div className="space-y-1">
            <p className="inline-flex items-center gap-2 text-sm font-medium text-foreground">
              <Sparkles className="size-4 text-primary" />
              {storyPage.illustratedLocalizationCount} /{" "}
              {storyPage.localizationCount} locales
            </p>
            <p className="text-xs text-muted-foreground">
              {missingCodes.length === 0
                ? "All localized page workspaces have their own illustration."
                : `Missing illustrations for ${missingCodes.join(", ")}.`}
            </p>
          </div>
        );
      },
    },
    {
      id: "actions",
      header: "Actions",
      align: "right",
      cellClassName: "w-[1%]",
      cell: (storyPage) => (
        <div className="flex items-center justify-end gap-2">
          <Button
            aria-label={`Edit page ${storyPage.pageNumber}`}
            disabled={isMutationPending}
            type="button"
            variant="outline"
            onClick={() => onEditStoryPage?.(storyPage)}
          >
            <Pencil className="size-4" />
            Edit
          </Button>
          <Button
            aria-label={`Delete page ${storyPage.pageNumber}`}
            disabled={isMutationPending}
            type="button"
            variant="ghost"
            onClick={() => onDeleteStoryPage?.(storyPage)}
          >
            <Trash2 className="size-4" />
            Delete
          </Button>
        </div>
      ),
    },
  ] satisfies DataTableColumn<StoryPageReadViewModel>[];
}

export function StoryPageTable({
  storyPages,
  isLoading = false,
  problem = null,
  onRetry,
  onEditStoryPage,
  onDeleteStoryPage,
  emptyAction,
  isMutationPending = false,
}: StoryPageTableProps) {
  const columns = createColumns({
    onEditStoryPage,
    onDeleteStoryPage,
    isMutationPending,
  });

  if (problem && storyPages.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyAction={emptyAction}
        emptyDescription="The story page collection could not be loaded from the admin API."
        emptyTitle="Story page list unavailable"
        getRowId={(storyPage) => storyPage.pageNumber.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption="Story page table"
      columns={columns}
      emptyAction={emptyAction}
      emptyDescription="No story pages exist yet. Add the first page to start building the story structure."
      emptyTitle="No story pages yet"
      getRowId={(storyPage) => storyPage.pageNumber.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting story page structure and localized page payload summaries from the admin API."
      loadingTitle="Loading story pages"
      onRetry={onRetry}
      problem={storyPages.length > 0 ? problem : null}
      rows={storyPages}
    />
  );
}
