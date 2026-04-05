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
import { useI18n } from "@/i18n/locale-provider";

export function CategoriesIndexRoute() {
  const { locale, formatNumber } = useI18n();
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
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Kategori Stüdyosu",
          title: "Kategoriler",
          description:
            "Kategori kaydı artık doğrudan yönetici API'sinden okunuyor. Kategori türleri içerik aileleriyle hizalı olduğu için her kategori yalnızca eşleşen Hikâye, Sesli Hikâye, Meditasyon veya Ninni kayıtlarını kürate eder.",
          refresh: "Yenile",
          create: "Kategori oluştur",
          searchLabel: "Kategori ara",
          searchPlaceholder: "Slug veya yerelleştirme adına göre ara",
          filterTypes: "Hikâye / Sesli Hikâye / Meditasyon / Ninni",
          filterAccess: "Premium ve standart",
          filterState: "Aktif ve arşivlenmiş",
          summaryDescription:
            "Kayıt listesi canlı backend verisini yansıtır ve her kategoriyi kendi detay stüdyosuna açar.",
          countLoaded: `${formatNumber(categoryCount)} kategori yüklendi`,
          registrySummary: "Kayıt özeti",
          registrySummaryDescription:
            "Kategori türleri içerik aileleriyle hizalı kalır; böylece her kayıt yalnızca eşleşen içeriği kürate edebilir.",
          loadingRegistry: "Kategori kaydı backend'den yükleniyor.",
          loadedRegistry: `${formatNumber(activeCount)} aktif kategori ve ${formatNumber(premiumCount)} premium kategori mevcut.`,
          infoOne:
            "Oluşturma sonrası kayıt listesi önbelleği yenilenir ve yeni kategori detay çalışma alanına geçilir.",
          infoTwo:
            "Satır navigasyonu, backend'in henüz döndürmediği yerelleştirme verisini uydurmadan canlı temel metadata görünümünü açar.",
          coverageTitle: "Stüdyo kapsamı",
          coverageDescription:
            "Kategoriler kayıt gezintisi, metadata düzenleme, yerelleştirme yönetimi ve tür güvenli kürasyon akışlarını destekler.",
          coverageOne:
            "Kategori yerelleştirme oluşturma, güncelleme ve okuma canlıdır; sekmeler yenileme sonrası korunur.",
          coverageTwo:
            "Kürate içerik seçimi seçili kategori türü ve dil çalışma alanıyla sınırlı kalır.",
          createDialogTitle: "Kategori oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir kategori oluşturun. Kaydetme sonrası kayıt listesi ve detay önbelleği yenilenir, CMS yeni detay rotasını açar.",
        }
      : {
          eyebrow: "Category Studio",
          title: "Categories",
          description:
            "The category registry now reads directly from the admin API. Category types are aligned to content families, so each category can later curate only matching Story, Audio Story, Meditation, or Lullaby records.",
          refresh: "Refresh",
          create: "Create category",
          searchLabel: "Search categories",
          searchPlaceholder: "Search by slug or localization name",
          filterTypes: "Story / Audio Story / Meditation / Lullaby",
          filterAccess: "Premium and standard",
          filterState: "Active and archived",
          summaryDescription:
            "The registry reflects live backend data and opens each category into its own detail studio.",
          countLoaded: `${formatNumber(categoryCount)} categor${categoryCount === 1 ? "y" : "ies"} loaded`,
          registrySummary: "Registry Summary",
          registrySummaryDescription:
            "Category types stay aligned to content families so each record can later curate only matching content.",
          loadingRegistry:
            "The category registry is hydrating from the backend.",
          loadedRegistry: `${formatNumber(activeCount)} active categories and ${formatNumber(premiumCount)} premium categories are available in the current environment.`,
          infoOne:
            "Create submits invalidate the registry cache and route into the new category detail workspace, where each category type stays matched to one content family.",
          infoTwo:
            "Row navigation opens the live base metadata view for each category record without inventing localization data the backend does not return yet.",
          coverageTitle: "Studio Coverage",
          coverageDescription:
            "Categories support registry browsing, metadata editing, localization management, and type-safe curation workflows.",
          coverageOne:
            "Category localization create, update, and read are live on the detail route, so localization tabs now persist across refresh and reopen.",
          coverageTwo:
            "Curated content remains constrained to the selected category type and language workspace.",
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

            <FilterBarActions>
              <FilterBarSummary
                description={copy.summaryDescription}
                title={copy.countLoaded}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>{copy.registrySummary}</CardTitle>
                <CardDescription>
                  {copy.registrySummaryDescription}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {categoryListQuery.isLoading
                    ? copy.loadingRegistry
                    : copy.loadedRegistry}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.infoOne}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.infoTwo}
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>{copy.coverageTitle}</CardTitle>
                <CardDescription>{copy.coverageDescription}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  {copy.coverageOne}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
                  {copy.coverageTwo}
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
