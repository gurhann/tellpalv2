import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { FilterBar, FilterBarGroup } from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
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
import { useI18n } from "@/i18n/locale-provider";

export function CategoriesIndexRoute() {
  const { locale } = useI18n();
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const categoryListQuery = useCategoryList();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Kategori Stüdyosu",
          title: "Kategoriler",
          description:
            "Kategori kayıtlarını açın, oluşturun ve detay stüdyosuna geçin.",
          refresh: "Yenile",
          create: "Kategori oluştur",
          searchLabel: "Kategori ara",
          searchPlaceholder: "Slug veya yerelleştirme adına göre ara",
          filterTypes: "Hikâye / Sesli Hikâye / Meditasyon / Ninni",
          filterAccess: "Premium ve standart",
          filterState: "Aktif ve arşivlenmiş",
          createDialogTitle: "Kategori oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir kategori oluşturun. Kaydetme sonrası kayıt listesi ve detay önbelleği yenilenir, CMS yeni detay rotasını açar.",
        }
      : {
          eyebrow: "Category Studio",
          title: "Categories",
          description:
            "Open category records, create new ones, and move into the detail studio.",
          refresh: "Refresh",
          create: "Create category",
          searchLabel: "Search categories",
          searchPlaceholder: "Search by slug or localization name",
          filterTypes: "Story / Audio Story / Meditation / Lullaby",
          filterAccess: "Premium and standard",
          filterState: "Active and archived",
          createDialogTitle: "Create category",
          createDialogDescription:
            "Create a new category with base metadata. After save, the registry and detail caches refresh and the CMS opens the new detail route.",
        };

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
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
              {copy.refresh}
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        toolbar={
          <FilterBar aria-label={copy.title}>
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label={copy.searchLabel}
                  className="pl-8"
                  disabled
                  placeholder={copy.searchPlaceholder}
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterTypes}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterAccess}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterState}
              </div>
            </FilterBarGroup>
          </FilterBar>
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
            <DialogTitle>{copy.createDialogTitle}</DialogTitle>
            <DialogDescription>
              {copy.createDialogDescription}
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
