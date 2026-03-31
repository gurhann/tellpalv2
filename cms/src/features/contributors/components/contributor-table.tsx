import { BadgeCheck, Signature } from "lucide-react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import type { ApiProblemDetail } from "@/types/api";

type ContributorTableProps = {
  contributors: ContributorViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
};

const contributorColumns: DataTableColumn<ContributorViewModel>[] = [
  {
    id: "contributor",
    header: "Contributor",
    cell: (contributor) => (
      <div className="space-y-1">
        <p className="font-medium text-foreground">{contributor.displayName}</p>
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
          Rename and per-content assignment flows land in the next contributor
          tasks.
        </p>
      </div>
    ),
  },
];

export function ContributorTable({
  contributors,
  isLoading = false,
  problem = null,
  onRetry,
}: ContributorTableProps) {
  return (
    <DataTable
      caption="Contributor table"
      columns={contributorColumns}
      emptyDescription="Create the first contributor in the next task or connect a backend environment that already has contributor records."
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
            Scan display names before create, rename, and assignment workflows
            are enabled.
          </p>
        </div>
      }
      tableClassName="[&_td:first-child]:w-[38%]"
    />
  );
}
