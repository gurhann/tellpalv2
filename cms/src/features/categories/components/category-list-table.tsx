import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { FilterBarSummary } from "@/components/data/filter-bar";
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
  const activeCount = categories.filter((category) => category.active).length;
  const premiumCount = categories.filter((category) => category.premium).length;
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
      header: locale === "tr" ? "Tür" : "Type",
      cell: (category) => (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
          {category.typeLabel}
        </span>
      ),
    },
    {
      id: "access",
      header: locale === "tr" ? "Erişim" : "Access",
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
            ? "Kategori kaydı admin API üzerinden yüklenemedi."
            : "The category registry could not be loaded from the admin API."
        }
        emptyTitle={
          locale === "tr"
            ? "Kategori kaydı kullanılamıyor"
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
      caption={
        locale === "tr" ? "Kategori kayıt tablosu" : "Category registry table"
      }
      columns={columns}
      emptyDescription={
        locale === "tr"
          ? "Henüz kategori yok. İçerikleri gruplamaya başlamak için ilk kategoriyi oluşturun."
          : "No categories exist yet. Create the first category to start grouping content."
      }
      emptyTitle={
        locale === "tr" ? "Kategori kaydı yok" : "No category records"
      }
      getRowId={(category) => category.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS, kategori metadatasını admin API üzerinden istiyor."
          : "The CMS is requesting category metadata from the admin API."
      }
      loadingTitle={
        locale === "tr"
          ? "Kategori kaydı yükleniyor"
          : "Loading category registry"
      }
      onRetry={onRetry}
      onRowClick={onCategorySelect}
      problem={categories.length > 0 ? problem : null}
      rows={categories}
      summary={
        <div className="space-y-1 text-right">
          <p className="text-sm font-medium tracking-tight text-foreground">
            {locale === "tr"
              ? `${categories.length} kategori`
              : `${categories.length} categor${categories.length === 1 ? "y" : "ies"}`}
          </p>
          <p className="text-xs text-muted-foreground">
            {locale === "tr"
              ? `${activeCount} aktif / ${premiumCount} premium`
              : `${activeCount} active / ${premiumCount} premium`}
          </p>
        </div>
      }
      toolbar={
        <FilterBarSummary
          description={
            locale === "tr"
              ? "Canlı backend okuma verisi artık bu ortak kategori tablosuna bağlı."
              : "Live backend read data is now bound to the shared category registry table."
          }
          title={locale === "tr" ? "Kategori kaydı" : "Category registry"}
        />
      }
    />
  );
}
