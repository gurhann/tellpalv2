import {
  ArrowRight,
  Languages,
  Layers3,
  LoaderCircle,
  SquareKanban,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CategorySummaryCard } from "@/features/categories/components/category-summary-card";
import { useCategoryDetail } from "@/features/categories/queries/use-category-detail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

export function CategoryDetailRoute() {
  const { categoryId = "" } = useParams();
  const parsedCategoryId = Number(categoryId);
  const hasValidCategoryId =
    Number.isInteger(parsedCategoryId) && parsedCategoryId > 0;
  const categoryQuery = useCategoryDetail(
    hasValidCategoryId ? parsedCategoryId : null,
  );
  const category = categoryQuery.category;
  const routeTitle = hasValidCategoryId
    ? `Category #${parsedCategoryId}`
    : "Category Detail";
  const routeDescription = category
    ? `Base metadata for ${category.slug} is now loaded from the admin API. Localization and curation surfaces stay explicit placeholders until broader category reads arrive.`
    : hasValidCategoryId
      ? "The CMS is loading base category metadata from the admin API. Localization and curation surfaces stay mounted while the current read payload remains intentionally narrow."
      : "This route expects a valid numeric category id from the category registry.";

  function renderToolbar() {
    if (category) {
      return <CategorySummaryCard category={category} />;
    }

    const title = !hasValidCategoryId
      ? "Invalid route"
      : categoryQuery.isNotFound
        ? "Category not found"
        : categoryQuery.problem
          ? "Metadata unavailable"
          : "Loading metadata";
    const description = !hasValidCategoryId
      ? "Open a category record from the registry to load a valid detail workspace."
      : categoryQuery.isNotFound
        ? "The admin API did not return a category record for this route."
        : categoryQuery.problem
          ? "Retry the detail query to restore the base category metadata."
          : "The detail shell is requesting the current base category metadata.";

    return (
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3 md:col-span-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Detail Status
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">{title}</p>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Route
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            /categories/{categoryId || "?"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Inline loading, error, and not-found states stay inside this shell.
          </p>
        </div>
      </div>
    );
  }

  function renderDetailContent() {
    if (!hasValidCategoryId) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">Return to category registry</Link>
            </Button>
          }
          description="Open a category record from the registry to reach a valid category detail workspace."
          title="Invalid category route"
        />
      );
    }

    if (categoryQuery.isLoading) {
      return (
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
              <LoaderCircle className="size-6 animate-spin" />
            </div>
            <div className="space-y-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                Loading category detail
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                The CMS is requesting the current base metadata for this
                category record.
              </p>
            </div>
          </CardContent>
        </Card>
      );
    }

    if (categoryQuery.isNotFound) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">Return to category registry</Link>
            </Button>
          }
          description="The requested category record does not exist in the current backend environment."
          title="Category not found"
        />
      );
    }

    if (categoryQuery.problem) {
      return (
        <ProblemAlert
          actions={
            <Button
              type="button"
              variant="outline"
              onClick={() => void categoryQuery.refetch()}
            >
              Retry
            </Button>
          }
          problem={categoryQuery.problem}
        />
      );
    }

    if (!category) {
      return (
        <EmptyState
          description="No detail payload is available for this category route yet."
          title="Category detail unavailable"
        />
      );
    }

    return (
      <>
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>Base metadata</CardTitle>
            <CardDescription>
              Current admin reads expose only category-level metadata. Write
              actions and localization workspaces land next.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Slug</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {category.slug}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Type</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {category.typeLabel}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Access</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {category.premium ? "Premium" : "Standard"}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Lifecycle</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {category.active ? "Active" : "Inactive"}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>Localization workspaces pending</CardTitle>
            <CardDescription>
              Category detail reads still omit localization payloads, so this
              task keeps the workspace honest instead of synthesizing locale
              tabs.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-dashed border-border/80 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <Languages className="size-4 text-primary" />
                Localization data not hydrated yet
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                The current admin detail response does not include localized
                `name`, `description`, image selection, or publication status.
              </p>
            </div>
            <div className="rounded-2xl border border-dashed border-border/80 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <SquareKanban className="size-4 text-primary" />
                Curation waits on locale context
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Category curation will activate after localization editing is
                live and the chosen language exists.
              </p>
            </div>
          </CardContent>
        </Card>

        <Card
          className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5"
          id="curation"
        >
          <CardHeader>
            <CardTitle>Curation workspace preview</CardTitle>
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
                Current backend category reads are intentionally narrower than
                the target studio workspace.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                `GET /api/admin/categories/
                {hasValidCategoryId ? parsedCategoryId : "{categoryId}"}` is
                live, but today it returns only base metadata.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                Localization payloads and curated content collections are still
                separate gaps, so this route keeps those areas explicit instead
                of inventing fake data.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Layers3 className="size-3.5" />
                Live read shell ready for write and curation expansion
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Next Category Tasks</CardTitle>
              <CardDescription>
                After live read binding, category work moves into write flows
                and curation.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M06-T03` adds category create, metadata edit, and localization
                mutation flows on top of this live read surface.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M07` expands this route into the full language-scoped curation
                workspace.
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      {renderDetailContent()}
    </ContentPageShell>
  );
}
