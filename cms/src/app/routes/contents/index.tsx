import { CirclePlus, RefreshCw, Search } from "lucide-react";
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
        description="Browse content by format, lifecycle state, localization coverage, and story-page count. Create opens a new record and sends you directly into its detail workspace."
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
                description="The registry reflects live backend data and supports direct navigation into each content workspace."
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
                <CardTitle>Registry Summary</CardTitle>
                <CardDescription>
                  Use this screen to scan the editorial catalog before opening
                  localization, contributor, and story-page workflows.
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
