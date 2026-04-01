import { ArrowRight, Languages, Layers3, SquareKanban } from "lucide-react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { categoryAdminBacklogDependencies } from "@/features/categories/api/category-admin";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

export function CategoryDetailRoute() {
  const { categoryId = "" } = useParams();
  const parsedCategoryId = Number(categoryId);
  const hasValidCategoryId =
    Number.isInteger(parsedCategoryId) && parsedCategoryId > 0;
  const routeTitle = hasValidCategoryId
    ? `Category #${parsedCategoryId}`
    : "Category Detail";
  const routeDescription = hasValidCategoryId
    ? "The category detail shell now reserves separate areas for metadata, localization workspaces, and language-scoped curation. Live query binding and edit flows land in the next category tasks."
    : "This route expects a valid numeric category id. Open the category registry once BG02 is available to reach a concrete detail workspace.";

  function renderToolbar() {
    return (
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Metadata
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            Base category editor reserved
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Slug, type, premium, and active state will appear here after detail
            query binding.
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Localizations
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            Dedicated language workspaces next
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Backend category detail currently omits localization payloads, so
            this shell reserves the area without inventing fake locale tabs.
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Curation
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            Language-scoped curation lands in M07
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Category content ordering and add/remove flows will attach to this
            route after the category studio query work settles.
          </p>
        </div>
      </div>
    );
  }

  return (
    <ContentPageShell
      eyebrow="Category Studio"
      title={routeTitle}
      description={routeDescription}
      actions={
        <>
          <Button asChild type="button" variant="outline">
            <Link to="/categories">Return to category registry</Link>
          </Button>
          <Button disabled type="button" variant="outline">
            Open curation
          </Button>
          <Button disabled type="button">
            Save metadata
          </Button>
        </>
      }
      toolbar={renderToolbar()}
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Read Surface Constraints</CardTitle>
              <CardDescription>
                Current backend category reads are narrower than the target
                studio workspace.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                `GET /api/admin/categories/
                {hasValidCategoryId ? parsedCategoryId : "{categoryId}"}`
                exists, but today it returns only base metadata.
              </div>
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-900">
                Category list and delete remain blocked behind{" "}
                {categoryAdminBacklogDependencies.listCategories}.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Layers3 className="size-3.5" />
                Detail shell ready for query and curation expansion
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Next Category Tasks</CardTitle>
              <CardDescription>
                After this shell task, category work moves into live query and
                write flows.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M06-T02` hydrates base metadata from backend reads and adds
                list/detail query states.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M06-T03` adds category create, metadata edit, and localization
                mutation flows.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M07` expands this route into the full curation workspace.
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      {!hasValidCategoryId ? (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">Return to category registry</Link>
            </Button>
          }
          description="Open a real category record from the registry when category list support lands. This route only accepts a positive numeric category id."
          title="Invalid category route"
        />
      ) : (
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Metadata Workspace Reserved</CardTitle>
              <CardDescription>
                The base editor surface is now fixed for category metadata and
                will bind to the existing single-category admin read in the next
                task.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-3 md:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
                <p className="text-sm font-medium text-foreground">
                  Core fields
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Slug, category type, premium state, and active state will live
                  in the left-side metadata form.
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
                <p className="text-sm font-medium text-foreground">
                  Route-level actions
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Save, delete gating, and curation handoff stay grouped in the
                  shared action region above this card.
                </p>
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Localization Workspaces Reserved</CardTitle>
              <CardDescription>
                Category detail reads do not yet hydrate localization payloads,
                so this task reserves the visual area without pretending to know
                which languages exist.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-3 md:grid-cols-2">
              <div className="rounded-2xl border border-dashed border-border/80 bg-muted/20 px-4 py-4">
                <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                  <Languages className="size-4 text-primary" />
                  Localization tabs land next
                </div>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Each future language workspace will carry `name`,
                  `description`, image selection, and publication status.
                </p>
              </div>
              <div className="rounded-2xl border border-dashed border-border/80 bg-muted/20 px-4 py-4">
                <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                  <SquareKanban className="size-4 text-primary" />
                  Publication-aware curation
                </div>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  The category curation panel will activate only after the
                  selected localization exists and is published.
                </p>
              </div>
            </CardContent>
          </Card>

          <Card
            className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5"
            id="curation"
          >
            <CardHeader>
              <CardTitle>Curation Workspace Preview</CardTitle>
              <CardDescription>
                Language-scoped category curation will live directly under this
                detail route.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
                <p className="text-sm font-medium text-foreground">
                  Planned curation actions
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Add content by id, adjust display order, and remove curated
                  links from the selected category language workspace.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
                <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                  /categories/{parsedCategoryId}
                </span>
                <ArrowRight className="size-4" />
                <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                  /categories/{parsedCategoryId}#curation
                </span>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </ContentPageShell>
  );
}
