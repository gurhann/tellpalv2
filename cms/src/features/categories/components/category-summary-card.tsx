import type { CategorySummaryViewModel } from "@/features/categories/model/category-view-model";

type CategorySummaryCardProps = {
  category: CategorySummaryViewModel;
};

export function CategorySummaryCard({ category }: CategorySummaryCardProps) {
  return (
    <div className="grid gap-3 md:grid-cols-3">
      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Metadata
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          {category.typeLabel} / {category.premium ? "Premium" : "Standard"} /{" "}
          {category.active ? "Active" : "Inactive"}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          Category #{category.id}
        </p>
      </div>

      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Slug
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          {category.slug}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          Live base metadata is now hydrated from `GET /api/admin/categories/
          {category.id}`.
        </p>
      </div>

      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Read scope
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          Base category detail live
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          Localization and curation payloads are not included in the current
          admin read response yet.
        </p>
      </div>
    </div>
  );
}
