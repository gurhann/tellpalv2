import type { CategorySummaryViewModel } from "@/features/categories/model/category-view-model";
import { useI18n } from "@/i18n/locale-provider";

type CategorySummaryCardProps = {
  category: CategorySummaryViewModel;
};

export function CategorySummaryCard({ category }: CategorySummaryCardProps) {
  const { locale } = useI18n();

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-border/70 bg-background px-4 py-3">
      <span className="text-sm font-semibold text-foreground">
        {category.slug}
      </span>
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-foreground">
        {category.typeLabel}
      </span>
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-muted-foreground">
        {category.premium
          ? "Premium"
          : locale === "tr"
            ? "Standart"
            : "Standard"}
      </span>
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-muted-foreground">
        {category.active
          ? locale === "tr"
            ? "Aktif"
            : "Active"
          : locale === "tr"
            ? "Pasif"
            : "Inactive"}
      </span>
      <span className="text-sm text-muted-foreground">
        {locale === "tr"
          ? `Kategori #${category.id}`
          : `Category #${category.id}`}
      </span>
    </div>
  );
}
