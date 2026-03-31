import { CirclePlus, RefreshCw, Search, UsersRound } from "lucide-react";
import { useState } from "react";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContributorFormDialog } from "@/features/contributors/components/contributor-form-dialog";
import { ContributorTable } from "@/features/contributors/components/contributor-table";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributors } from "@/features/contributors/queries/use-contributors";

const RECENT_CONTRIBUTOR_LIMIT = 12;

export function ContributorsRoute() {
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [selectedContributor, setSelectedContributor] =
    useState<ContributorViewModel | null>(null);
  const contributorQuery = useContributors(RECENT_CONTRIBUTOR_LIMIT);

  return (
    <>
      <ContentPageShell
        eyebrow="Contributor Registry"
        title="Contributors"
        description="The contributor registry now loads the latest backend records. Create and rename flows are live, while content assignment lands next."
        actions={
          <>
            <Button
              type="button"
              variant="outline"
              onClick={() => void contributorQuery.refetch()}
            >
              <RefreshCw
                className={`size-4 ${
                  contributorQuery.isFetching ? "animate-spin" : ""
                }`}
              />
              Refresh
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              Create contributor
            </Button>
          </>
        }
        toolbar={
          <FilterBar aria-label="Contributor filters">
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label="Search contributors"
                  className="pl-8"
                  disabled
                  placeholder="Search by display name"
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Latest {RECENT_CONTRIBUTOR_LIMIT}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Registry + rename live
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description="The recent list is live. Create and rename now post back to the admin API and refresh the shared registry."
                title={`${contributorQuery.contributors.length} contributor${
                  contributorQuery.contributors.length === 1 ? "" : "s"
                } loaded`}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Registry Notes</CardTitle>
                <CardDescription>
                  This shell is backed by `GET /api/admin/contributors` with a
                  recent-list limit.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {contributorQuery.isLoading
                    ? "The contributor registry is hydrating from the backend."
                    : `The latest ${contributorQuery.limit} contributor slots are reserved for quick editorial scanning.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Create opens a clean registry dialog; rename updates an
                  existing display name in-place from each row.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Content-level assignment enters through the content detail
                  route in `M05-T03`.
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Planned Workflow</CardTitle>
                <CardDescription>
                  The contributor module grows in two steps after this task.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  `M05-T03` adds per-content assignment with role, language,
                  credit name, and sort order.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  `M05-T04` adds gap-aware placeholders for delete and unassign
                  until backend endpoints arrive.
                </div>
                <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                  <UsersRound className="size-3.5" />
                  Shared registry create + rename are live
                </div>
              </CardContent>
            </Card>
          </>
        }
      >
        <ContributorTable
          contributors={contributorQuery.contributors}
          isLoading={contributorQuery.isLoading}
          onRenameContributor={setSelectedContributor}
          onRetry={() => void contributorQuery.refetch()}
          problem={contributorQuery.problem}
        />
      </ContentPageShell>

      {isCreateDialogOpen ? (
        <ContributorFormDialog
          mode="create"
          open={isCreateDialogOpen}
          onOpenChange={setIsCreateDialogOpen}
        />
      ) : null}

      {selectedContributor ? (
        <ContributorFormDialog
          contributor={selectedContributor}
          mode="rename"
          open
          onOpenChange={(open) => {
            if (!open) {
              setSelectedContributor(null);
            }
          }}
        />
      ) : null}
    </>
  );
}
