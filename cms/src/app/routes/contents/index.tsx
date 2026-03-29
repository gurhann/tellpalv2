import { CirclePlus, RefreshCw, Search, Sparkles } from "lucide-react";
import { useNavigate } from "react-router-dom";

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
import { ContentListTable } from "@/features/contents/components/content-list-table";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";

export function ContentsIndexRoute() {
  const navigate = useNavigate();
  const contentListQuery = useContentList();
  const contentCount = contentListQuery.contents.length;
  const activeCount = contentListQuery.contents.filter(
    (content) => content.summary.active,
  ).length;
  const inactiveCount = contentCount - activeCount;

  return (
    <ContentPageShell
      eyebrow="Editorial Core"
      title="Content Studio"
      description="The content registry now reads directly from the admin API. Filtering stays visual-only in this task, while create and edit flows remain disabled until the next content milestones."
      actions={
        <>
          <Button
            type="button"
            variant="outline"
            onClick={() => void contentListQuery.refetch()}
          >
            <RefreshCw
              className={`size-4 ${
                contentListQuery.isFetching ? "animate-spin" : ""
              }`}
            />
            Refresh
          </Button>
          <Button disabled type="button">
            <CirclePlus className="size-4" />
            Create content
          </Button>
        </>
      }
      toolbar={
        <FilterBar>
          <FilterBarGroup>
            <div className="relative min-w-[16rem] flex-1">
              <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
              <Input
                aria-label="Search content registry"
                className="pl-8"
                disabled
                placeholder="Search by external key or localized title"
                value=""
              />
            </div>
            <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
              All content types
            </div>
            <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
              Active and archived
            </div>
          </FilterBarGroup>

          <FilterBarActions>
            <FilterBarSummary
              description="Live data is bound. Query params, client-side filtering, and create flows are deferred to later content tasks."
              title={`${contentCount} content record${
                contentCount === 1 ? "" : "s"
              } loaded`}
            />
          </FilterBarActions>
        </FilterBar>
      }
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Workspace Notes</CardTitle>
              <CardDescription>
                The registry shell is now backed by `GET /api/admin/contents`.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                {contentListQuery.isLoading
                  ? "The content registry is hydrating from the backend."
                  : `${activeCount} active and ${inactiveCount} inactive records are available in the current environment.`}
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Row navigation now opens the live detail route for each content
                record.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                The create action stays disabled until metadata forms and server
                mutations land in `M03-T03`.
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Editorial Intent</CardTitle>
              <CardDescription>
                This list is optimized for scanning type, lifecycle state, and
                localization coverage before entering detail workspaces.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                Search and filter controls stay intentionally inactive in this
                task so the read path can settle before query-param behavior is
                introduced.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Sparkles className="size-3.5" />
                Shared toolbar, summary, and navigation now run on live data
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <ContentListTable
        contents={contentListQuery.contents}
        isLoading={contentListQuery.isLoading}
        onContentSelect={(content) =>
          navigate(`/contents/${content.summary.id}`)
        }
        onRetry={() => void contentListQuery.refetch()}
        problem={contentListQuery.problem}
      />

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Next Up</CardTitle>
          <CardDescription>
            Query binding is now live. The next content tasks can focus on
            editing, validation, and publication workflows without reworking the
            layout contract.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">Metadata form</p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              `M03-T03` will activate the create flow and editable metadata
              fields.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Localization actions
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              `M03-T04` will wire publish, archive, and validation behavior on
              each language workspace.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Processing visibility
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              `M03-T05` will surface richer asset and processing diagnostics in
              the detail shell.
            </p>
          </div>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
