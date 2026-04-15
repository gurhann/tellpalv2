import { CirclePlus, LoaderCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { LanguageTabs } from "@/components/language/language-tabs";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CategoryCurationPanel } from "@/features/categories/components/category-curation-panel";
import { CategoryForm } from "@/features/categories/components/category-form";
import { CategoryLocalizationForm } from "@/features/categories/components/category-localization-form";
import { CategorySummaryCard } from "@/features/categories/components/category-summary-card";
import { useCategoryCuration } from "@/features/categories/queries/use-category-curation";
import { useCategoryDetail } from "@/features/categories/queries/use-category-detail";
import { useCategoryLocalizations } from "@/features/categories/queries/use-category-localizations";
import {
  getCreateCategoryLocalizationDefaults,
  mapCategoryLocalizationToFormValues,
} from "@/features/categories/schema/category-localization-schema";
import { mapCategoryReadToFormValues } from "@/features/categories/schema/category-schema";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useI18n } from "@/i18n/locale-provider";
import { getSupportedCmsLanguageOptions } from "@/lib/languages";

export function CategoryDetailRoute() {
  const { locale, t } = useI18n();
  const { categoryId = "" } = useParams();
  const parsedCategoryId = Number(categoryId);
  const hasValidCategoryId =
    Number.isInteger(parsedCategoryId) && parsedCategoryId > 0;
  const categoryQuery = useCategoryDetail(
    hasValidCategoryId ? parsedCategoryId : null,
  );
  const localizationQuery = useCategoryLocalizations(
    hasValidCategoryId ? parsedCategoryId : null,
  );
  const category = categoryQuery.category;
  const localizations = localizationQuery.localizations;
  const [selectedLanguageCode, setSelectedLanguageCode] = useState("en");
  const [isCreateLocalizationOpen, setIsCreateLocalizationOpen] =
    useState(false);
  const supportedLanguageOptions = getSupportedCmsLanguageOptions(locale);
  const copy =
    locale === "tr"
      ? {
          detailFallbackTitle: "Kategori Detayı",
          routeLoading:
            "CMS temel kategori metadata'sını admin API üzerinden yüklüyor. Yerelleştirme ve kürasyon yüzeyleri, mevcut okuma payload'ı dar kalsa bile ekranda kalır.",
          routeMissing:
            "Bu rota kategori kaydından geçerli bir sayısal kategori kimliği bekler.",
          routeLoaded: (slug: string) =>
            `${slug} için metadata, yerelleştirme ve kürasyon akışlarını yönetin.`,
          detailStatus: "Detay Durumu",
          route: "Rota",
          inlineStates:
            "Satır içi yükleme, hata ve bulunamadı durumları bu kabuk içinde kalır.",
          invalidRoute: "Geçersiz rota",
          categoryNotFound: "Kategori bulunamadı",
          metadataUnavailable: "Metadata kullanılamıyor",
          metadataLoading: "Metadata yükleniyor",
          invalidDescription:
            "Geçerli bir kategori detay çalışma alanı yüklemek için kayıttan bir kategori açın.",
          notFoundDescription:
            "Admin API bu rota için bir kategori kaydı döndürmedi.",
          retryDescription:
            "Temel kategori metadata'sını geri getirmek için detay sorgusunu yeniden deneyin.",
          detailQueryDescription:
            "Detay kabuğu mevcut temel kategori metadata'sını istiyor.",
          returnToRegistry: "Kategori kaydına dön",
          loadingTitle: "Kategori detayı yükleniyor",
          loadingDescription:
            "CMS bu kategori kaydı için güncel temel metadata'yı istiyor.",
          categoryNotFoundDescription:
            "İstenen kategori kaydı mevcut backend ortamında bulunmuyor.",
          detailUnavailable: "Kategori detayı kullanılamıyor",
          detailUnavailableDescription:
            "Bu kategori rotası için henüz detay payload'ı yok.",
          metadataTitle: "Metadata",
          metadataDescription:
            "Temel kategori metadata'sını güncelleyin. Kategori oluşturulduktan sonra tür sabit kalır; slug, premium ve aktiflik durumu burada değiştirilebilir.",
          localizationTitle: "Yerelleştirme",
          localizationDescription:
            "Kategori yerelleştirme oluşturma, güncelleme ve okuma artık canlı. Bu çalışma alanı artık kalıcı yerelleştirme sekmelerini admin API'den hydrate ediyor.",
          createLocalization: "Yerelleştirme oluştur",
          loadingLocalizations: "Yerelleştirmeler yükleniyor",
          loadingLocalizationsDescription:
            "CMS bu kategori için kalıcı yerelleştirme sekmelerini istiyor.",
          noLocalizationsTitle: "Henüz yerelleştirme yok",
          noLocalizationsDescription:
            "Bu kategori için kalıcı bir dil çalışma alanı açmak üzere ilk yerelleştirmeyi oluşturun.",
          createFirstLocalization: "İlk yerelleştirmeyi oluştur",
          localizationTabs: "Kategori yerelleştirme sekmeleri",
          openCuration: "Kürasyonu aç",
          categoryStudio: "Kategori Stüdyosu",
          categorySummary: "Kategori Özeti",
          summaryDescription:
            "Bu detay görünümü kategori metadata'sını, kalıcı yerelleştirme sekmelerini ve dil bazlı kürasyonu bir araya getirir.",
          summaryCardOne:
            "Stüdyo, yükleme, yeniden deneme ve bulunamadı durumlarında kabuğu kararlı tutabilmek için önce temel kategori metadata'sını yükler.",
          summaryCardTwo:
            "Yerelleştirme oluşturma, güncelleme ve liste okuma canlıdır; kürasyon da artık bu kalıcı dil çalışma alanlarını yansıtır. Kürasyon satırları seçili dil bazında hydrate olur.",
          snapshotTitle: "Yerelleştirme Özeti",
          snapshotDescription:
            "Kalıcı yerelleştirme sekmeleri artık admin API üzerinden hydrate olur ve yenilemeden sonra görünür kalır.",
          snapshotNone:
            "Bu kategori için henüz saklanan kategori yerelleştirmesi yok.",
          snapshotSome: (count: number) =>
            `${count} yerelleştirme çalışma alanı şu anda backend üzerinden hydrate ediliyor.`,
          snapshotRules:
            "Yayınlanmış yerelleştirmeler ekleme ve sıralama aksiyonları için ön koşuldur. Seçili dil yayınlanmamış olsa bile saklanan kürasyon satırları görünür kalır.",
          createDialogTitle: "Kategori yerelleştirmesi oluştur",
          createDialogDescription:
            "Bu kategori için bir dil çalışma alanı oluşturun. Kaydedilen yerelleştirmeler backend okumasında kalır ve yenilemeden sonra görünür kalır.",
        }
      : {
          detailFallbackTitle: "Category Detail",
          routeLoading:
            "Loading metadata, localization, and curation workflows.",
          routeMissing:
            "This route expects a valid numeric category id from the category registry.",
          routeLoaded: (slug: string) =>
            `Manage metadata, localization, and curation workflows for ${slug}.`,
          detailStatus: "Detail Status",
          route: "Route",
          inlineStates:
            "Inline loading, error, and not-found states stay inside this shell.",
          invalidRoute: "Invalid route",
          categoryNotFound: "Category not found",
          metadataUnavailable: "Metadata unavailable",
          metadataLoading: "Loading metadata",
          invalidDescription:
            "Open a category record from the registry to load a valid detail workspace.",
          notFoundDescription:
            "The admin API did not return a category record for this route.",
          retryDescription:
            "Retry the detail query to restore the base category metadata.",
          detailQueryDescription:
            "The detail shell is requesting the current base category metadata.",
          returnToRegistry: "Return to category registry",
          loadingTitle: "Loading category detail",
          loadingDescription:
            "The CMS is requesting the current base metadata for this category record.",
          categoryNotFoundDescription:
            "The requested category record does not exist in the current backend environment.",
          detailUnavailable: "Category detail unavailable",
          detailUnavailableDescription:
            "No detail payload is available for this category route yet.",
          metadataTitle: "Metadata",
          metadataDescription:
            "Update the base category metadata. Category creation is live from the registry, and this form now saves slug, premium, and active state through the admin API.",
          localizationTitle: "Localization",
          localizationDescription:
            "Category localization creation, update, and read are live. This workspace now hydrates persisted localization tabs from the admin API.",
          createLocalization: "Create localization",
          loadingLocalizations: "Loading localizations",
          loadingLocalizationsDescription:
            "The CMS is requesting persisted category localization tabs for this category.",
          noLocalizationsTitle: "No localizations yet",
          noLocalizationsDescription:
            "Create the first category localization to open a persistent language workspace for this category.",
          createFirstLocalization: "Create first localization",
          localizationTabs: "Category localization tabs",
          openCuration: "Open curation",
          categoryStudio: "Category Studio",
          categorySummary: "Category Summary",
          summaryDescription:
            "This detail view combines category metadata, persisted localization tabs, and language-scoped curation.",
          summaryCardOne:
            "Base category metadata loads first so the studio can keep the shell stable through loading, retry, and not-found states.",
          summaryCardTwo:
            "Localization create, update, and list reads are live, and curation now mirrors those persisted language workspaces. Curated rows hydrate per selected language.",
          snapshotTitle: "Localization Snapshot",
          snapshotDescription:
            "Persisted localization tabs now hydrate from the admin API and remain visible after refresh.",
          snapshotNone:
            "No category localizations are stored for this category yet.",
          snapshotSome: (count: number) =>
            `${count} localization workspace${
              count === 1 ? "" : "s"
            } currently hydrated from the backend.`,
          snapshotRules:
            "Published localizations are the prerequisite for add and reorder actions. Stored curated rows still remain visible even when the selected locale is not published.",
          createDialogTitle: "Create category localization",
          createDialogDescription:
            "Create a language workspace for this category. Saved localizations persist in backend reads and remain visible after refresh.",
        };
  const routeTitle =
    category?.slug ??
    (hasValidCategoryId
      ? locale === "tr"
        ? `Kategori #${parsedCategoryId}`
        : `Category #${parsedCategoryId}`
      : copy.detailFallbackTitle);
  const routeDescription = category
    ? copy.routeLoaded(category.slug)
    : hasValidCategoryId
      ? copy.routeLoading
      : copy.routeMissing;

  const resolvedLanguageCode =
    localizations.find(
      (localization) => localization.languageCode === selectedLanguageCode,
    )?.languageCode ??
    localizations[0]?.languageCode ??
    selectedLanguageCode;
  const selectedLocalization =
    localizations.find(
      (localization) => localization.languageCode === resolvedLanguageCode,
    ) ?? null;
  const curationQuery = useCategoryCuration(
    hasValidCategoryId ? parsedCategoryId : null,
    selectedLocalization?.languageCode ?? null,
  );
  const curationItems = curationQuery.items;

  const availableLanguageOptions = useMemo(
    () =>
      supportedLanguageOptions.filter(
        (option) =>
          !localizations.some(
            (localization) => localization.languageCode === option.code,
          ),
      ),
    [localizations, supportedLanguageOptions],
  );

  const tabItems = localizations.map((localization) => ({
    code: localization.languageCode,
    label: localization.languageLabel,
    tone: localization.isPublished
      ? ("success" as const)
      : ("default" as const),
    meta: localization.statusLabel,
    description: localization.hasImage
      ? locale === "tr"
        ? "Bu dil çalışma alanı için görsel hazır."
        : "Image ready for this language workspace."
      : locale === "tr"
        ? "Bu dil çalışma alanı için henüz görsel seçilmedi."
        : "No image selected yet for this language workspace.",
  }));

  function renderToolbar() {
    if (category) {
      return <CategorySummaryCard category={category} />;
    }

    const title = !hasValidCategoryId
      ? copy.invalidRoute
      : categoryQuery.isNotFound
        ? copy.categoryNotFound
        : categoryQuery.problem
          ? copy.metadataUnavailable
          : copy.metadataLoading;
    const description = !hasValidCategoryId
      ? copy.invalidDescription
      : categoryQuery.isNotFound
        ? copy.notFoundDescription
        : categoryQuery.problem
          ? copy.retryDescription
          : copy.detailQueryDescription;

    return (
      <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-border/70 bg-background px-4 py-3">
        <span className="text-sm font-semibold text-foreground">{title}</span>
        <span className="text-sm text-muted-foreground">{description}</span>
        <span className="text-sm text-muted-foreground">
          {copy.route}: /categories/{categoryId || "?"}
        </span>
      </div>
    );
  }

  function renderDetailContent() {
    if (!hasValidCategoryId) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">{copy.returnToRegistry}</Link>
            </Button>
          }
          description={copy.invalidDescription}
          title={copy.invalidRoute}
        />
      );
    }

    if (categoryQuery.isLoading) {
      return (
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
              <LoaderCircle className="size-6 animate-spin" />
            </div>
            <div className="space-y-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                {copy.loadingTitle}
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                {copy.loadingDescription}
              </p>
            </div>
          </CardContent>
        </Card>
      );
    }

    if (categoryQuery.isNotFound) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">{copy.returnToRegistry}</Link>
            </Button>
          }
          description={copy.categoryNotFoundDescription}
          title={copy.categoryNotFound}
        />
      );
    }

    if (categoryQuery.problem) {
      return (
        <ProblemAlert
          actions={
            <Button
              type="button"
              variant="outline"
              onClick={() => void categoryQuery.refetch()}
            >
              {t("app.retry")}
            </Button>
          }
          problem={categoryQuery.problem}
        />
      );
    }

    if (!category) {
      return (
        <EmptyState
          description={copy.detailUnavailableDescription}
          title={copy.detailUnavailable}
        />
      );
    }

    return (
      <>
        <FormSection
          description={copy.metadataDescription}
          title={copy.metadataTitle}
        >
          <CategoryForm
            categoryId={category.id}
            initialValues={mapCategoryReadToFormValues(category)}
            mode="update"
          />
        </FormSection>

        <FormSection
          actions={
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateLocalizationOpen(true)}
              disabled={availableLanguageOptions.length === 0}
            >
              <CirclePlus className="size-4" />
              {copy.createLocalization}
            </Button>
          }
          description={copy.localizationDescription}
          title={copy.localizationTitle}
        >
          {localizationQuery.isLoading && localizations.length === 0 ? (
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardContent className="flex min-h-52 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
                <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
                  <LoaderCircle className="size-6 animate-spin" />
                </div>
                <div className="space-y-2">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    {copy.loadingLocalizations}
                  </h2>
                  <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                    {copy.loadingLocalizationsDescription}
                  </p>
                </div>
              </CardContent>
            </Card>
          ) : selectedLocalization ? (
            <>
              {localizationQuery.problem ? (
                <ProblemAlert
                  actions={
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => void localizationQuery.refetch()}
                    >
                      {t("app.retry")}
                    </Button>
                  }
                  problem={localizationQuery.problem}
                />
              ) : null}

              <LanguageTabs
                items={tabItems}
                listLabel={copy.localizationTabs}
                value={resolvedLanguageCode}
                onValueChange={setSelectedLanguageCode}
              />

              <CategoryLocalizationForm
                key={`${category.id}-${selectedLocalization.languageCode}-${selectedLocalization.name}-${selectedLocalization.status}`}
                availableLanguages={supportedLanguageOptions}
                categoryId={category.id}
                initialValues={mapCategoryLocalizationToFormValues(
                  selectedLocalization,
                )}
                localization={selectedLocalization}
                mode="update"
              />
            </>
          ) : localizationQuery.problem ? (
            <ProblemAlert
              actions={
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => void localizationQuery.refetch()}
                >
                  {t("app.retry")}
                </Button>
              }
              problem={localizationQuery.problem}
            />
          ) : (
            <EmptyState
              action={
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsCreateLocalizationOpen(true)}
                >
                  {copy.createFirstLocalization}
                </Button>
              }
              description={copy.noLocalizationsDescription}
              title={copy.noLocalizationsTitle}
            />
          )}
        </FormSection>

        <CategoryCurationPanel
          category={category}
          curationItems={curationItems}
          curationIsLoading={curationQuery.isLoading}
          curationProblem={curationQuery.problem}
          localizations={localizations}
          selectedLocalization={selectedLocalization}
          selectedLanguageCode={resolvedLanguageCode}
          onCreateLocalization={() => setIsCreateLocalizationOpen(true)}
          onLanguageChange={setSelectedLanguageCode}
          onRetryCuration={() => void curationQuery.refetch()}
        />
      </>
    );
  }

  return (
    <>
      <ContentPageShell
        eyebrow={copy.categoryStudio}
        title={routeTitle}
        description={routeDescription}
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to="/categories">{copy.returnToRegistry}</Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateLocalizationOpen(true)}
              disabled={
                !category ||
                availableLanguageOptions.length === 0 ||
                categoryQuery.isLoading
              }
            >
              {copy.createLocalization}
            </Button>
            {selectedLocalization ? (
              <Button asChild type="button" variant="outline">
                <a href="#curation">{copy.openCuration}</a>
              </Button>
            ) : (
              <Button disabled type="button" variant="outline">
                {copy.openCuration}
              </Button>
            )}
          </>
        }
        toolbar={renderToolbar()}
      >
        {renderDetailContent()}
      </ContentPageShell>

      <Dialog
        open={isCreateLocalizationOpen}
        onOpenChange={setIsCreateLocalizationOpen}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{copy.createDialogTitle}</DialogTitle>
            <DialogDescription>
              {copy.createDialogDescription}
            </DialogDescription>
          </DialogHeader>

          <CategoryLocalizationForm
            availableLanguages={availableLanguageOptions}
            categoryId={parsedCategoryId}
            initialValues={getCreateCategoryLocalizationDefaults(
              availableLanguageOptions[0]?.code,
            )}
            mode="create"
            onCancel={() => setIsCreateLocalizationOpen(false)}
            onSuccess={(savedLocalization) => {
              setSelectedLanguageCode(
                savedLocalization.languageCode.toLowerCase(),
              );
              setIsCreateLocalizationOpen(false);
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
