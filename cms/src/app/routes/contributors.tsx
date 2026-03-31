import { CirclePlus, RefreshCw, Search, UsersRound } from "lucide-react";

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
import { ContributorTable } from "@/features/contributors/components/contributor-table";
import { useContributors } from "@/features/contributors/queries/use-contributors";

const RECENT_CONTRIBUTOR_LIMIT = 12;

export function ContributorsRoute() {
  const contributorQuery = useContributors(RECENT_CONTRIBUTOR_LIMIT);

  return (
    <ContentPageShell
      eyebrow="Contributor Registry"
      title="Contributors"
      description="The contributor registry now loads the latest backend records. This task stays list-first; create, rename, and content assignment flows land next."
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
          <Button disabled type="button">
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
              Detail-less registry
            </div>
          </FilterBarGroup>

          <FilterBarActions>
            <FilterBarSummary
              description="The recent list is live. Search, create, rename, and content assignment behaviors unlock in the next contributor tasks."
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
                Rows stay read-only in this task so create and rename behavior
                can be introduced cleanly in `M05-T02`.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Content-level assignment enters through the content detail route
                in `M05-T03`.
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Planned Workflow</CardTitle>
              <CardDescription>
                The contributor module grows in three steps after this route
                shell.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M05-T02` enables create and rename dialogs directly from this
                registry.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M05-T03` adds per-content assignment with role, language,
                credit name, and sort order.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <UsersRound className="size-3.5" />
                Shared registry is live
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <ContributorTable
        contributors={contributorQuery.contributors}
        isLoading={contributorQuery.isLoading}
        onRetry={() => void contributorQuery.refetch()}
        problem={contributorQuery.problem}
      />
    </ContentPageShell>
  );
}
