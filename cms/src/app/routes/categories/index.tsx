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
        description="The category registry now reads directly from the admin API. Category types are aligned to content families, so each category can later curate only matching Story, Audio Story, Meditation, or Lullaby records."
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
                Story / Audio Story / Meditation / Lullaby
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
                description="The registry reflects live backend data and opens each category into its own detail studio."
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
                <CardTitle>Registry Summary</CardTitle>
                <CardDescription>
                  Category types stay aligned to content families so each record
                  can later curate only matching content.
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
                  the new category detail workspace, where each category type
                  stays matched to one content family.
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
                <CardTitle>Studio Coverage</CardTitle>
                <CardDescription>
                  Categories support registry browsing, metadata editing,
                  localization management, and type-safe curation workflows.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  Category localization create, update, and read are live on the
                  detail route, so localization tabs now persist across refresh
                  and reopen.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  Curated content remains constrained to the selected category
                  type and language workspace.
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
