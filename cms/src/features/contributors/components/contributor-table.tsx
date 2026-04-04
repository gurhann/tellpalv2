import { BadgeCheck, PencilLine, Signature } from "lucide-react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import type { ApiProblemDetail } from "@/types/api";

type ContributorTableProps = {
  contributors: ContributorViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onRenameContributor?: (contributor: ContributorViewModel) => void;
  isMutationPending?: boolean;
};

function createContributorColumns({
  onRenameContributor,
  isMutationPending,
}: Pick<ContributorTableProps, "onRenameContributor" | "isMutationPending">) {
  return [
    {
      id: "contributor",
      header: "Contributor",
      cell: (contributor) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">
            {contributor.displayName}
          </p>
          <p className="text-xs text-muted-foreground">
            Contributor #{contributor.id}
          </p>
        </div>
      ),
    },
    {
      id: "initials",
      header: "Initials",
      cell: (contributor) => (
        <div className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/25 px-3 py-1.5 text-sm font-medium text-foreground">
          <Signature className="size-4 text-primary" />
          {contributor.initials}
        </div>
      ),
    },
    {
      id: "readiness",
      header: "Assignment Readiness",
      cell: () => (
        <div className="space-y-1">
          <p className="inline-flex items-center gap-2 text-sm font-medium text-foreground">
            <BadgeCheck className="size-4 text-primary" />
            Ready for content credits
          </p>
          <p className="text-xs text-muted-foreground">
            Create and rename are live, and the same registry is reused in
            content credit dialogs.
          </p>
        </div>
      ),
    },
    ...(onRenameContributor
      ? [
          {
            id: "actions",
            header: "Actions",
            align: "right" as const,
            cellClassName: "w-[1%]",
            cell: (contributor: ContributorViewModel) => (
              <div className="flex justify-end">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isMutationPending}
                  onClick={() => onRenameContributor(contributor)}
                >
                  <PencilLine className="size-4" />
                  Rename
                </Button>
              </div>
            ),
          },
        ]
      : []),
  ] satisfies DataTableColumn<ContributorViewModel>[];
}

export function ContributorTable({
  contributors,
  isLoading = false,
  problem = null,
  onRetry,
  onRenameContributor,
  isMutationPending = false,
}: ContributorTableProps) {
  const contributorColumns = createContributorColumns({
    onRenameContributor,
    isMutationPending,
  });

  return (
    <DataTable
      caption="Contributor table"
      columns={contributorColumns}
      emptyDescription="Create the first contributor or connect a backend environment that already has contributor records."
      emptyTitle="No contributors yet"
      getRowId={(contributor) => contributor.id.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting the most recent contributor registry entries from the admin API."
      loadingTitle="Loading contributors"
      onRetry={onRetry}
      problem={problem}
      rows={contributors}
      summary={
        <div className="space-y-1 text-right">
          <p className="text-sm font-medium tracking-tight text-foreground">
            {contributors.length} contributor
            {contributors.length === 1 ? "" : "s"}
          </p>
          <p className="text-xs text-muted-foreground">
            Recent registry snapshot
          </p>
        </div>
      }
      toolbar={
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            Shared contributor registry
          </p>
          <p className="text-sm text-muted-foreground">
            Scan display names before opening create, rename, and assignment
            workflows.
          </p>
        </div>
      }
      tableClassName="[&_td:first-child]:w-[34%]"
    />
  );
}
