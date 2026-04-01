import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { FilterBarSummary } from "@/components/data/filter-bar";
import type { CategorySummaryViewModel } from "@/features/categories/model/category-view-model";
import type { ApiProblemDetail } from "@/types/api";

type CategoryListTableProps = {
  categories: CategorySummaryViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onCategorySelect?: (category: CategorySummaryViewModel) => void;
};

const columns: DataTableColumn<CategorySummaryViewModel>[] = [
  {
    id: "slug",
    header: "Category",
    cell: (category) => (
      <div className="space-y-1">
        <p className="font-medium text-foreground">{category.slug}</p>
        <p className="text-xs text-muted-foreground">Category #{category.id}</p>
      </div>
    ),
  },
  {
    id: "type",
    header: "Type",
    cell: (category) => (
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
        {category.typeLabel}
      </span>
    ),
  },
  {
    id: "access",
    header: "Access",
    cell: (category) => (
      <span
        className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
          category.premium
            ? "border-amber-200 bg-amber-50 text-amber-800"
            : "border-border/70 bg-muted/35 text-muted-foreground"
        }`}
      >
        {category.premium ? "Premium" : "Standard"}
      </span>
    ),
  },
  {
    id: "state",
    header: "State",
    cell: (category) => (
      <span
        className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
          category.active
            ? "border-emerald-200 bg-emerald-50 text-emerald-700"
            : "border-border/70 bg-muted/35 text-muted-foreground"
        }`}
      >
        {category.active ? "Active" : "Inactive"}
      </span>
    ),
  },
];

export function CategoryListTable({
  categories,
  isLoading = false,
  problem = null,
  onRetry,
  onCategorySelect,
}: CategoryListTableProps) {
  const activeCount = categories.filter((category) => category.active).length;
  const premiumCount = categories.filter((category) => category.premium).length;

  if (problem && categories.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription="The category registry could not be loaded from the admin API."
        emptyTitle="Category registry unavailable"
        getRowId={(category) => category.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption="Category registry table"
      columns={columns}
      emptyDescription="No categories exist yet. Category creation opens in the next task."
      emptyTitle="No category records"
      getRowId={(category) => category.id.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting category metadata from the admin API."
      loadingTitle="Loading category registry"
      onRetry={onRetry}
      onRowClick={onCategorySelect}
      problem={categories.length > 0 ? problem : null}
      rows={categories}
      summary={
        <div className="space-y-1 text-right">
          <p className="text-sm font-medium tracking-tight text-foreground">
            {categories.length} categor{categories.length === 1 ? "y" : "ies"}
          </p>
          <p className="text-xs text-muted-foreground">
            {activeCount} active / {premiumCount} premium
          </p>
        </div>
      }
      toolbar={
        <FilterBarSummary
          description="Live backend read data is now bound to the shared category registry table."
          title="Category registry"
        />
      }
    />
  );
}
