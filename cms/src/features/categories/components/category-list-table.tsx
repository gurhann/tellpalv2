import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import type { CategorySummaryViewModel } from "@/features/categories/model/category-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type CategoryListTableProps = {
  categories: CategorySummaryViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onCategorySelect?: (category: CategorySummaryViewModel) => void;
};

export function CategoryListTable({
  categories,
  isLoading = false,
  problem = null,
  onRetry,
  onCategorySelect,
}: CategoryListTableProps) {
  const { locale } = useI18n();
  const columns: DataTableColumn<CategorySummaryViewModel>[] = [
    {
      id: "slug",
      header: locale === "tr" ? "Kategori" : "Category",
      cell: (category) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">{category.slug}</p>
          <p className="text-xs text-muted-foreground">
            {locale === "tr"
              ? `Kategori #${category.id}`
              : `Category #${category.id}`}
          </p>
        </div>
      ),
    },
    {
      id: "type",
      header: locale === "tr" ? "Tur" : "Type",
      cell: (category) => (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
          {category.typeLabel}
        </span>
      ),
    },
    {
      id: "access",
      header: locale === "tr" ? "Erisim" : "Access",
      cell: (category) => (
        <span
          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
            category.premium
              ? "border-amber-200 bg-amber-50 text-amber-800"
              : "border-border/70 bg-muted/35 text-muted-foreground"
          }`}
        >
          {category.premium
            ? "Premium"
            : locale === "tr"
              ? "Standart"
              : "Standard"}
        </span>
      ),
    },
    {
      id: "state",
      header: locale === "tr" ? "Durum" : "State",
      cell: (category) => (
        <span
          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
            category.active
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-border/70 bg-muted/35 text-muted-foreground"
          }`}
        >
          {category.active
            ? locale === "tr"
              ? "Aktif"
              : "Active"
            : locale === "tr"
              ? "Pasif"
              : "Inactive"}
        </span>
      ),
    },
  ];

  if (problem && categories.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription={
          locale === "tr"
            ? "Kategori kaydi admin API uzerinden yuklenemedi."
            : "The category registry could not be loaded from the admin API."
        }
        emptyTitle={
          locale === "tr"
            ? "Kategori kaydi kullanilamiyor"
            : "Category registry unavailable"
        }
        getRowId={(category) => category.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption={locale === "tr" ? "Kategori kayit tablosu" : "Category registry table"}
      columns={columns}
      emptyDescription={
        locale === "tr"
          ? "Henuz kategori yok. Icerikleri gruplamaya baslamak icin ilk kategoriyi olusturun."
          : "No categories exist yet. Create the first category to start grouping content."
      }
      emptyTitle={locale === "tr" ? "Kategori kaydi yok" : "No category records"}
      getRowId={(category) => category.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS, kategori metadata'sini admin API uzerinden istiyor."
          : "The CMS is requesting category metadata from the admin API."
      }
      loadingTitle={
        locale === "tr" ? "Kategori kaydi yukleniyor" : "Loading category registry"
      }
      onRetry={onRetry}
      onRowClick={onCategorySelect}
      problem={categories.length > 0 ? problem : null}
      rows={categories}
    />
  );
}
