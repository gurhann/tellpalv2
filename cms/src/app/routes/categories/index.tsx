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
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
} from "@/components/workspace/workspace-primitives";
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
          eyebrow: "Kategori Studyosu",
          title: "Kategoriler",
          description:
            "Kategori kayitlarini filtreleyin, olusturun ve detay studyosuna gecin.",
          refresh: "Yenile",
          create: "Kategori olustur",
          searchLabel: "Kategori ara",
          searchPlaceholder: "Slug'a gore ara",
          filterTypes: "Tum turler",
          filterAccess: "Tum erisim tipleri",
          filterState: "Tum durumlar",
          createDialogTitle: "Kategori olustur",
          createDialogDescription:
            "Temel metadata ile yeni bir kategori olusturun. Kaydetme sonrasi CMS yeni detay rotasini acar.",
          railTitle: "Category posture",
          railDescription:
            "Localization ve curation handoff'una gecmeden once tip, erisim ve aktiflik dengesini taranabilir tutun.",
          activeRecords: "Aktif kayit",
          premiumRecords: "Premium kayit",
          storyCategories: "Story kategori",
          notesTitle: "Kurasyon notlari",
          notesDescription:
            "Secilen kategori metadata lane, locale workspace ve curation lane ile ayni akista acilir.",
          resultLabel: "Arama sonucu",
          nextStepLabel: "Sonraki adim",
          nextStepValue: "Kategori detail workspace",
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
          railTitle: "Category posture",
          railDescription:
            "Keep type, access, and activation balance visible before moving into localization and curation lanes.",
          activeRecords: "Active records",
          premiumRecords: "Premium records",
          storyCategories: "Story categories",
          notesTitle: "Curation notes",
          notesDescription:
            "Selected categories open with metadata, locale workspace, and curation staying on one route.",
          resultLabel: "Search result",
          nextStepLabel: "Next step",
          nextStepValue: "Category detail workspace",
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

  const activeRecordCount = useMemo(
    () => filteredCategories.filter((category) => category.active).length,
    [filteredCategories],
  );
  const premiumRecordCount = useMemo(
    () => filteredCategories.filter((category) => category.premium).length,
    [filteredCategories],
  );
  const storyCategoryCount = useMemo(
    () =>
      filteredCategories.filter((category) => category.type === "STORY").length,
    [filteredCategories],
  );
  const filterSummaryTitle =
    locale === "tr"
      ? `${filteredCategories.length} / ${categoryListQuery.categories.length} kayit`
      : `${filteredCategories.length} / ${categoryListQuery.categories.length} records`;
  const filterSummaryDescription =
    locale === "tr"
      ? "Arama ve filtreler kategori registry gorunumunu aninda daraltir."
      : "Search and filters narrow the category registry immediately.";

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: copy.activeRecords,
                value: `${activeRecordCount} / ${filteredCategories.length}`,
                tone: activeRecordCount > 0 ? "success" : "default",
              },
              {
                label: copy.premiumRecords,
                value: `${premiumRecordCount} / ${filteredCategories.length}`,
              },
              {
                label: copy.storyCategories,
                value: `${storyCategoryCount} / ${filteredCategories.length}`,
              },
            ]}
          >
            <WorkspaceInfoCard
              title={copy.notesTitle}
              description={copy.notesDescription}
              className="bg-background/80"
            >
              <WorkspaceKeyValueGrid
                items={[
                  {
                    label: copy.resultLabel,
                    value:
                      locale === "tr"
                        ? `${filteredCategories.length} kayit`
                        : `${filteredCategories.length} records`,
                  },
                  {
                    label: copy.nextStepLabel,
                    value: copy.nextStepValue,
                    tone: "accent",
                  },
                ]}
              />
            </WorkspaceInfoCard>
          </TaskRail>
        }
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
              title={filterSummaryTitle}
              description={filterSummaryDescription}
            />
          </FilterBar>
        }
      >
        <CategoryListTable
          categories={filteredCategories}
          isLoading={categoryListQuery.isLoading}
          onCategorySelect={(category) => navigate(`/categories/${category.id}`)}
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
