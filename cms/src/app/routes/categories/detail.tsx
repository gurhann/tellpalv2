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
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
  WorkspaceMetricCard,
  WorkspaceStatusPill,
} from "@/components/workspace/workspace-primitives";
import { CategoryCurationPanel } from "@/features/categories/components/category-curation-panel";
import { CategoryForm } from "@/features/categories/components/category-form";
import { CategoryLocalizationForm } from "@/features/categories/components/category-localization-form";
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
          detailFallbackTitle: "Kategori Detayi",
          routeLoading:
            "Metadata, yerellestirme ve kurasyon calisma alanlari yukleniyor.",
          routeMissing:
            "Bu rota kategori kaydindan gecerli bir sayisal kategori kimligi bekler.",
          routeLoaded: (slug: string) =>
            `${slug} icin metadata, dil calisma alanlari ve kurasyon akislarini ayni yuzeyden yonetin.`,
          route: "Rota",
          invalidRoute: "Gecersiz rota",
          categoryNotFound: "Kategori bulunamadi",
          metadataUnavailable: "Metadata kullanilamiyor",
          metadataLoading: "Metadata yukleniyor",
          invalidDescription:
            "Gecerli bir kategori detay calisma alani yuklemek icin kayittan bir kategori acin.",
          notFoundDescription:
            "Admin API bu rota icin bir kategori kaydi dondurmedi.",
          retryDescription:
            "Temel kategori metadata'sini geri getirmek icin detay sorgusunu yeniden deneyin.",
          detailQueryDescription:
            "Detay kabugu mevcut temel kategori metadata'sini istiyor.",
          returnToRegistry: "Kategori kaydina don",
          loadingTitle: "Kategori detayi yukleniyor",
          loadingDescription:
            "CMS bu kategori kaydi icin guncel temel metadata'yi istiyor.",
          categoryNotFoundDescription:
            "Istenen kategori kaydi mevcut backend ortaminda bulunmuyor.",
          detailUnavailable: "Kategori detayi kullanilamiyor",
          detailUnavailableDescription:
            "Bu kategori rotasi icin henuz detay payload'i yok.",
          metadataTitle: "Metadata",
          metadataDescription:
            "Temel kategori metadata'sini guncelleyin. Kategori turu olusturulduktan sonra sabit kalir; slug, premium ve aktiflik durumu burada degistirilebilir.",
          localizationTitle: "Dil Calisma Alanlari",
          localizationDescription:
            "Her dil calisma alani yayin durumu, gorsel hazirligi ve duzenlenebilir icerigi ayni yuzeyde tutar.",
          createLocalization: "Yerellestirme olustur",
          loadingLocalizations: "Yerellestirmeler yukleniyor",
          loadingLocalizationsDescription:
            "CMS bu kategori icin kalici yerellestirme sekmelerini istiyor.",
          noLocalizationsTitle: "Henuz yerellestirme yok",
          noLocalizationsDescription:
            "Bu kategori icin ilk dil calisma alanini acmak uzere ilk yerellestirmeyi olusturun.",
          createFirstLocalization: "Ilk yerellestirmeyi olustur",
          localizationTabs: "Kategori yerellestirme sekmeleri",
          openCuration: "Kurasyon alanina git",
          categoryStudio: "Kategori Studyosu",
          workspaceHandoff: "Kategori handoff'u",
          workspaceHandoffDescription:
            "Metadata lane, dil calisma alani ve kurasyon lane ayni rota uzerinde kalir; secili dil baglami kaybolmaz.",
          selectedLocale: "Secili dil",
          localeFocus: "Dil odagi",
          curationPosture: "Kurasyon durusu",
          curationReady: "Kurasyona hazir",
          curationWaiting: "On kosul bekliyor",
          metadataLane: "Metadata Lane",
          localizationLane: "Dil Calisma Alani",
          curationLane: "Kurasyon Lane",
          localizationSnapshot: "Yerellestirme Ozeti",
          snapshotDescription:
            "Kalici yerellestirme sekmeleri admin API uzerinden hydrate olur ve yenileme sonrasi gorunur kalir.",
          snapshotNone:
            "Bu kategori icin henuz saklanan kategori yerellestirmesi yok.",
          snapshotSome: (count: number) =>
            `${count} dil calisma alani su anda backend uzerinden hydrate ediliyor.`,
          createDialogTitle: "Kategori yerellestirmesi olustur",
          createDialogDescription:
            "Bu kategori icin bir dil calisma alani olusturun. Kaydedilen yerellestirmeler backend okumasinda kalir ve yenileme sonrasi gorunur kalir.",
          typeFixed: "Tur sabit",
          stateEditable: "Bu alanda duzenlenir",
          publishedLocale: "Yayinlanmis dil",
          imageReady: "Gorsel hazir",
          curationItems: "Kurasyon ogeleri",
          noneYet: "Henuz yok",
        }
      : {
          detailFallbackTitle: "Category Detail",
          routeLoading:
            "Loading metadata, localization workspaces, and curation lanes.",
          routeMissing:
            "This route expects a valid numeric category id from the category registry.",
          routeLoaded: (slug: string) =>
            `Review metadata, locale workspaces, and curation for ${slug} on one route.`,
          route: "Route",
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
            "Update the base category metadata. Category type remains fixed after creation, while slug, premium, and active state stay editable here.",
          localizationTitle: "Locale workspaces",
          localizationDescription:
            "Each locale workspace keeps publication state, image readiness, and editable copy on the same surface.",
          createLocalization: "Create localization",
          loadingLocalizations: "Loading localizations",
          loadingLocalizationsDescription:
            "The CMS is requesting persisted category localization tabs for this category.",
          noLocalizationsTitle: "No localizations yet",
          noLocalizationsDescription:
            "Create the first category localization to open a persistent language workspace for this category.",
          createFirstLocalization: "Create first localization",
          localizationTabs: "Category localization tabs",
          openCuration: "Open curation lane",
          categoryStudio: "Category Studio",
          workspaceHandoff: "Workspace handoff",
          workspaceHandoffDescription:
            "Metadata, locale workspaces, and curation stay on one route so the selected language context never drops away.",
          selectedLocale: "Selected locale",
          localeFocus: "Locale focus",
          curationPosture: "Curation posture",
          curationReady: "Ready for curation",
          curationWaiting: "Waiting on prerequisite",
          metadataLane: "Metadata lane",
          localizationLane: "Localization workspace",
          curationLane: "Curation lane",
          localizationSnapshot: "Localization snapshot",
          snapshotDescription:
            "Persisted localization tabs hydrate from the admin API and remain visible after refresh.",
          snapshotNone:
            "No category localizations are stored for this category yet.",
          snapshotSome: (count: number) =>
            `${count} localization workspace${
              count === 1 ? "" : "s"
            } currently hydrated from the backend.`,
          createDialogTitle: "Create category localization",
          createDialogDescription:
            "Create a language workspace for this category. Saved localizations persist in backend reads and remain visible after refresh.",
          typeFixed: "Type fixed",
          stateEditable: "Editable here",
          publishedLocale: "Published locales",
          imageReady: "Image ready",
          curationItems: "Curation items",
          noneYet: "None yet",
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
        ? "Bu dil calisma alani icin gorsel hazir."
        : "Image ready for this locale workspace."
      : locale === "tr"
        ? "Bu dil calisma alani icin henuz gorsel secilmedi."
        : "No image selected yet for this locale workspace.",
  }));

  function renderToolbar() {
    if (category) {
      return (
        <div className="grid gap-4 rounded-[1.7rem] border border-border/70 bg-muted/15 p-4 lg:grid-cols-3">
          <WorkspaceMetricCard
            detail={copy.workspaceHandoffDescription}
            label={copy.metadataLane}
            tone="accent"
            value={category.typeLabel}
          />
          <WorkspaceMetricCard
            detail={selectedLocalization?.statusLabel}
            label={copy.selectedLocale}
            tone={
              selectedLocalization?.isPublished
                ? "success"
                : selectedLocalization
                  ? "warning"
                  : "default"
            }
            value={
              selectedLocalization?.languageLabel ??
              (locale === "tr" ? "Henuz yok" : "None yet")
            }
          />
          <WorkspaceMetricCard
            detail={selectedLocalization?.name ?? copy.snapshotDescription}
            label={copy.curationPosture}
            tone={
              selectedLocalization?.isPublished ? "success" : "warning"
            }
            value={
              selectedLocalization?.isPublished
                ? copy.curationReady
                : copy.curationWaiting
            }
          />
        </div>
      );
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
          <WorkspaceKeyValueGrid
            items={[
              { label: "Slug", value: category.slug },
              { label: copy.typeFixed, value: category.typeLabel, tone: "accent" },
              {
                label: copy.stateEditable,
                value:
                  locale === "tr"
                    ? "Slug, premium, aktiflik"
                    : "Slug, premium, active state",
              },
            ]}
          />
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
          contentClassName="gap-4"
          description={copy.localizationDescription}
          title={copy.localizationTitle}
        >
          <WorkspaceInfoCard
            title={copy.localizationSnapshot}
            description={copy.snapshotDescription}
          >
            <WorkspaceKeyValueGrid
              items={[
                {
                  label: copy.publishedLocale,
                  value: localizations
                    .filter((localization) => localization.isPublished)
                    .length.toString(),
                  tone:
                    localizations.some((localization) => localization.isPublished)
                      ? "success"
                      : "warning",
                },
                {
                  label: copy.imageReady,
                  value: localizations
                    .filter((localization) => localization.hasImage)
                    .length.toString(),
                  tone:
                    localizations.length > 0 &&
                    localizations.every((localization) => localization.hasImage)
                      ? "success"
                      : "warning",
                },
                {
                  label: copy.curationItems,
                  value: curationItems.length.toString(),
                  tone: curationItems.length > 0 ? "accent" : "default",
                },
              ]}
            />
          </WorkspaceInfoCard>

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

              <WorkspaceInfoCard
                title={`${copy.localeFocus}: ${selectedLocalization.languageLabel}`}
                description={selectedLocalization.name}
              >
                <div className="flex flex-wrap items-center gap-2">
                  <WorkspaceStatusPill
                    tone={selectedLocalization.isPublished ? "success" : "warning"}
                  >
                    {selectedLocalization.statusLabel}
                  </WorkspaceStatusPill>
                  <WorkspaceStatusPill
                    tone={selectedLocalization.hasImage ? "success" : "warning"}
                  >
                    {selectedLocalization.hasImage
                      ? copy.imageReady
                      : locale === "tr"
                        ? "Gorsel eksik"
                        : "Image missing"}
                  </WorkspaceStatusPill>
                </div>
              </WorkspaceInfoCard>

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

  function renderAside() {
    if (!category) {
      return null;
    }

    return (
      <TaskRail
        title={copy.localizationSnapshot}
        description={copy.snapshotDescription}
        stats={[
          {
            label: copy.localizationLane,
            value:
              localizations.length === 0
                ? copy.snapshotNone
                : copy.snapshotSome(localizations.length),
            tone: localizations.length > 0 ? "success" : "warning",
          },
          {
            label: copy.selectedLocale,
            value:
              selectedLocalization?.languageLabel ??
              (locale === "tr" ? "Henuz yok" : "None yet"),
          },
          {
            label: copy.curationLane,
            value:
              selectedLocalization?.isPublished
                ? copy.curationReady
                : copy.curationWaiting,
            tone:
              selectedLocalization?.isPublished ? "success" : "warning",
          },
        ]}
      >
        <div className="grid gap-4">
          <WorkspaceInfoCard
            title={copy.metadataLane}
            description={copy.workspaceHandoffDescription}
            className="bg-background/80"
          >
            <WorkspaceKeyValueGrid
              items={[
                { label: "Slug", value: category.slug },
                {
                  label: copy.publishedLocale,
                  value: localizations
                    .filter((localization) => localization.isPublished)
                    .length.toString(),
                  tone:
                    localizations.some((localization) => localization.isPublished)
                      ? "success"
                      : "warning",
                },
                {
                  label: copy.curationItems,
                  value:
                    curationItems.length > 0
                      ? curationItems.length.toString()
                      : copy.noneYet,
                  tone: curationItems.length > 0 ? "accent" : "default",
                },
              ]}
            />
          </WorkspaceInfoCard>
        </div>
      </TaskRail>
    );
  }

  return (
    <>
      <ContentPageShell
        eyebrow={copy.categoryStudio}
        title={routeTitle}
        description={routeDescription}
        aside={renderAside()}
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

          <DialogBody>
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
          </DialogBody>
        </DialogContent>
      </Dialog>
    </>
  );
}
