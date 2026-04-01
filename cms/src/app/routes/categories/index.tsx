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
import { CategoryForm } from "@/features/categories/components/category-form";
import { CategoryListTable } from "@/features/categories/components/category-list-table";
import { useCategoryList } from "@/features/categories/queries/use-category-list";
import { getCreateCategoryFormDefaults } from "@/features/categories/schema/category-schema";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

export function CategoriesIndexRoute() {
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const categoryListQuery = useCategoryList();
  const categoryCount = categoryListQuery.categories.length;
  const activeCount = categoryListQuery.categories.filter(
    (category) => category.active,
  ).length;
  const premiumCount = categoryListQuery.categories.filter(
    (category) => category.premium,
  ).length;

  return (
    <>
      <ContentPageShell
        eyebrow="Category Studio"
        title="Categories"
        description="The category registry now reads directly from the admin API. Category creation is live, while search/filter activation and full localization reads still land in later tasks."
        actions={
          <>
            <Button
              type="button"
              variant="outline"
              onClick={() => void categoryListQuery.refetch()}
            >
              <RefreshCw
                className={`size-4 ${
                  categoryListQuery.isFetching ? "animate-spin" : ""
                }`}
              />
              Refresh
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              Create category
            </Button>
          </>
        }
        toolbar={
          <FilterBar aria-label="Category studio filters">
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label="Search categories"
                  className="pl-8"
                  disabled
                  placeholder="Search by slug or localization name"
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                All category types
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Premium and standard
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                Active and archived
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description="Live data is bound. Create is now active, while query params and client-side filtering are still deferred."
                title={`${categoryCount} categor${
                  categoryCount === 1 ? "y" : "ies"
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
                  The registry shell is backed by `GET /api/admin/categories`
                  and category creation is now live.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {categoryListQuery.isLoading
                    ? "The category registry is hydrating from the backend."
                    : `${activeCount} active categories and ${premiumCount} premium categories are available in the current environment.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Create submits invalidate the registry cache and route into
                  the new category detail workspace.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Row navigation opens the live base metadata view for each
                  category record without inventing localization data the
                  backend does not return yet.
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Planned Workspace</CardTitle>
                <CardDescription>
                  The live read shell is in place so the next category tasks can
                  focus on localization depth and curation without a layout
                  rewrite.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  Category localization create and update live on the detail
                  route in this task, but localization tabs remain
                  session-backed because no admin read endpoint exists yet.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  `M07` layers language-scoped curation into the detail route
                  once localization-aware category editing has settled.
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
        <CategoryListTable
          categories={categoryListQuery.categories}
          isLoading={categoryListQuery.isLoading}
          onCategorySelect={(category) =>
            navigate(`/categories/${category.id}`)
          }
          onRetry={() => void categoryListQuery.refetch()}
          problem={categoryListQuery.problem}
        />

        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>Studio Layout Ready</CardTitle>
            <CardDescription>
              The live route already reserves the editing surfaces needed for
              the remaining category tasks.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-3">
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Registry filters
              </p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Search, type, premium, and active filter controls are visible
                and deliberately inactive until query-param behavior is
                introduced.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Detail navigation
              </p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Each live list row navigates into `/categories/:categoryId`
                without changing the surrounding app shell.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Curation expansion
              </p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                The detail shell already reserves separate metadata,
                localization, and curation areas while base category reads stay
                live today.
              </p>
            </div>
          </CardContent>
        </Card>
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Create category</DialogTitle>
            <DialogDescription>
              Create a new category with base metadata. After save, the registry
              and detail caches refresh and the CMS opens the new detail route.
            </DialogDescription>
          </DialogHeader>

          <CategoryForm
            initialValues={getCreateCategoryFormDefaults()}
            mode="create"
            onCancel={() => setIsCreateDialogOpen(false)}
            onSuccess={(savedCategory) => {
              setIsCreateDialogOpen(false);
              navigate(`/categories/${savedCategory.categoryId}`);
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
