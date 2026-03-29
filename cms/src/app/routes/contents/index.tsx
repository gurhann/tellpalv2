import { CirclePlus, RefreshCw, Search, Sparkles } from "lucide-react";
import { useState } from "react";
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentListTable } from "@/features/contents/components/content-list-table";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { getCreateContentFormDefaults } from "@/features/contents/schema/content-schema";

export function ContentsIndexRoute() {
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const contentListQuery = useContentList();
  const contentCount = contentListQuery.contents.length;
  const activeCount = contentListQuery.contents.filter(
    (content) => content.summary.active,
  ).length;
  const inactiveCount = contentCount - activeCount;

  return (
    <>
      <ContentPageShell
        eyebrow="Editorial Core"
        title="Content Studio"
        description="The content registry now reads directly from the admin API. Search and filter controls stay visual-only, while create and metadata edit flows are now live."
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
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
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
                description="Live data is bound. Query params and client-side filtering are still deferred, but content creation now runs through the admin API."
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
                  The registry shell is backed by `GET /api/admin/contents` and
                  can now create new content records.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {contentListQuery.isLoading
                    ? "The content registry is hydrating from the backend."
                    : `${activeCount} active and ${inactiveCount} inactive records are available in the current environment.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Row navigation opens the live metadata editor and localization
                  workspace for each content record.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Create submits immediately invalidate the list cache and
                  redirect into the new detail route.
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
                  task so the write path can settle before query-param behavior
                  is introduced.
                </div>
                <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                  <Sparkles className="size-3.5" />
                  Shared toolbar, summary, navigation, and create flow are live
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
              Create and metadata editing are now live. The next content tasks
              can focus on localization mutation, publication controls, and
              processing diagnostics.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-3">
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
                `M03-T05` will surface richer asset and processing diagnostics
                in the detail shell.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Filter activation
              </p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Search and filter controls will become functional after the
                write path settles.
              </p>
            </div>
          </CardContent>
        </Card>
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Create content</DialogTitle>
            <DialogDescription>
              Create a new editorial record with base metadata. After save, the
              registry and detail caches refresh and the CMS opens the new
              detail route.
            </DialogDescription>
          </DialogHeader>

          <ContentForm
            initialValues={getCreateContentFormDefaults()}
            mode="create"
            onCancel={() => setIsCreateDialogOpen(false)}
            onSuccess={(savedContent) => {
              setIsCreateDialogOpen(false);
              navigate(`/contents/${savedContent.contentId}`);
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
