import {
  CirclePlus,
  Layers3,
  RefreshCw,
  Search,
  ShieldAlert,
} from "lucide-react";

import { EmptyState } from "@/components/feedback/empty-state";
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
import { categoryAdminBacklogDependencies } from "@/features/categories/api/category-admin";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

export function CategoriesIndexRoute() {
  return (
    <ContentPageShell
      eyebrow="Category Studio"
      title="Categories"
      description="The category studio shell is ready, but the registry still waits on the missing admin list endpoint. Filters and create entry points stay visible so the full workspace shape can settle before live query wiring lands."
      actions={
        <>
          <Button disabled type="button" variant="outline">
            <RefreshCw className="size-4" />
            Refresh
          </Button>
          <Button disabled type="button">
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
              description="The list shell is intentionally static until BG02 adds GET /api/admin/categories. The rest of the studio layout is now fixed."
              title="Registry wiring blocked by BG02"
            />
          </FilterBarActions>
        </FilterBar>
      }
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Backend Gap</CardTitle>
              <CardDescription>
                Category studio depends on the missing list and delete admin
                endpoints.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-900">
                `GET /api/admin/categories` is still tracked under{" "}
                {categoryAdminBacklogDependencies.listCategories}. Until it
                lands, this route renders no fake registry rows.
              </div>
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-900">
                Delete stays blocked behind{" "}
                {categoryAdminBacklogDependencies.deleteCategory}, so the list
                shell exposes no destructive placeholder action.
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Planned Workspace</CardTitle>
              <CardDescription>
                The route shape is fixed so list, detail, and curation can
                expand without a later layout rewrite.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M06-T02` will bind live category list and base detail queries.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M06-T03` opens create, metadata edit, and localization forms.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                `M07` layers language-scoped curation into the detail route.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Layers3 className="size-3.5" />
                Shell, toolbar, and detail path are now reserved
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardContent className="px-6 py-6">
          <EmptyState
            action={
              <Button disabled type="button" variant="outline">
                Open first category
              </Button>
            }
            description="The CMS does not yet have a category list response to render here. Once BG02 lands, this area will show slug, type, premium state, active state, and entry points into each category detail workspace."
            icon={ShieldAlert}
            title="Category registry awaits BG02"
          />
        </CardContent>
      </Card>

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Studio Layout Ready</CardTitle>
          <CardDescription>
            The list route already reserves the major editing surfaces needed
            for the remaining category tasks.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Registry filters
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Search, type, premium, and active filter controls are visible and
              deliberately inactive until query wiring lands.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Detail navigation
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Each future list row will navigate into `/categories/:categoryId`
              without changing the surrounding app shell.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Curation expansion
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              The detail shell already reserves separate metadata, localization,
              and curation areas for later tasks.
            </p>
          </div>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
