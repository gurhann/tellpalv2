import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
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
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState<string>("ALL");
  const [selectedAccess, setSelectedAccess] = useState<
    "ALL" | "PREMIUM" | "STANDARD"
  >("ALL");
  const [selectedState, setSelectedState] = useState<
    "ALL" | "ACTIVE" | "INACTIVE"
  >("ALL");
  const categoryListQuery = useCategoryList();
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Kategori Stüdyosu",
          title: "Kategoriler",
          description:
            "Kategori kayıtlarını filtreleyin, oluşturun ve görev odaklı detay stüdyosuna geçin.",
          refresh: "Yenile",
          create: "Kategori oluştur",
          searchLabel: "Kategori ara",
          searchPlaceholder: "Slug’a göre ara",
          filterTypes: "Tüm türler",
          filterAccess: "Tüm erişim tipleri",
          filterState: "Tüm durumlar",
          createDialogTitle: "Kategori oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir kategori oluşturun. Kaydetme sonrası CMS yeni detay rotasını açar.",
        }
      : {
          eyebrow: "Category Studio",
          title: "Categories",
          description:
            "Filter category records, create new ones, and move into the detail studio.",
          refresh: "Refresh",
          create: "Create category",
          searchLabel: "Search categories",
          searchPlaceholder: "Search by slug",
          filterTypes: "All types",
          filterAccess: "All access types",
          filterState: "All states",
          createDialogTitle: "Create category",
          createDialogDescription:
            "Create a new category with base metadata. After save, the CMS opens the new detail route.",
        };

  const typeOptions = useMemo(() => {
    const nextOptions = new Set<string>();

    categoryListQuery.categories.forEach((category) => {
      nextOptions.add(category.typeLabel);
    });

    return ["ALL", ...Array.from(nextOptions)];
  }, [categoryListQuery.categories]);

  const filteredCategories = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return categoryListQuery.categories.filter((category) => {
      if (selectedType !== "ALL" && category.typeLabel !== selectedType) {
        return false;
      }

      if (selectedAccess === "PREMIUM" && !category.premium) {
        return false;
      }

      if (selectedAccess === "STANDARD" && category.premium) {
        return false;
      }

      if (selectedState === "ACTIVE" && !category.active) {
        return false;
      }

      if (selectedState === "INACTIVE" && category.active) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return category.slug.toLowerCase().includes(normalizedSearch);
    });
  }, [
    categoryListQuery.categories,
    deferredSearch,
    selectedAccess,
    selectedState,
    selectedType,
  ]);

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
                  placeholder={copy.searchPlaceholder}
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
            </FilterBarGroup>
            <FilterBarActions>
              {typeOptions.map((typeOption) => (
                <Button
                  key={typeOption}
                  type="button"
                  variant={selectedType === typeOption ? "secondary" : "outline"}
                  size="sm"
                  onClick={() => setSelectedType(typeOption)}
                >
                  {typeOption === "ALL" ? copy.filterTypes : typeOption}
                </Button>
              ))}
              {[
                {
                  key: "ALL" as const,
                  label: copy.filterAccess,
                },
                {
                  key: "PREMIUM" as const,
                  label: "Premium",
                },
                {
                  key: "STANDARD" as const,
                  label: locale === "tr" ? "Standart" : "Standard",
                },
              ].map((accessOption) => (
                <Button
                  key={accessOption.key}
                  type="button"
                  variant={
                    selectedAccess === accessOption.key
                      ? "secondary"
                      : "outline"
                  }
                  size="sm"
                  onClick={() => setSelectedAccess(accessOption.key)}
                >
                  {accessOption.label}
                </Button>
              ))}
              {[
                {
                  key: "ALL" as const,
                  label: copy.filterState,
                },
                {
                  key: "ACTIVE" as const,
                  label: locale === "tr" ? "Aktif" : "Active",
                },
                {
                  key: "INACTIVE" as const,
                  label: locale === "tr" ? "Pasif" : "Inactive",
                },
              ].map((stateOption) => (
                <Button
                  key={stateOption.key}
                  type="button"
                  variant={
                    selectedState === stateOption.key ? "secondary" : "outline"
                  }
                  size="sm"
                  onClick={() => setSelectedState(stateOption.key)}
                >
                  {stateOption.label}
                </Button>
              ))}
            </FilterBarActions>
            <FilterBarSummary
              title={
                locale === "tr"
                  ? `${filteredCategories.length} / ${categoryListQuery.categories.length} kayıt`
                  : `${filteredCategories.length} / ${categoryListQuery.categories.length} records`
              }
              description={
                locale === "tr"
                  ? "Arama ve filtreler kategori registry görünümünü anında daraltır."
                  : "Search and filters narrow the category registry immediately."
              }
            />
          </FilterBar>
        }
      >
        <CategoryListTable
          categories={filteredCategories}
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

          <DialogBody>
            <CategoryForm
              initialValues={getCreateCategoryFormDefaults()}
              mode="create"
              onCancel={() => setIsCreateDialogOpen(false)}
              onSuccess={(savedCategory) => {
                setIsCreateDialogOpen(false);
                navigate(`/categories/${savedCategory.categoryId}`);
              }}
            />
          </DialogBody>
        </DialogContent>
      </Dialog>
    </>
  );
}
