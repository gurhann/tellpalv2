import { CirclePlus, LoaderCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { LanguageTabs } from "@/components/language/language-tabs";
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
import { supportedCmsLanguageOptions } from "@/lib/languages";

export function CategoryDetailRoute() {
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
  const routeTitle =
    category?.slug ??
    (hasValidCategoryId ? `Category #${parsedCategoryId}` : "Category Detail");
  const routeDescription = category
    ? `Base metadata for ${category.slug} is now editable. This category type is content-aligned, so curation stays limited to matching ${category.typeLabel} records. Localization tabs and curated rows now both hydrate from the admin API.`
    : hasValidCategoryId
      ? "The CMS is loading base category metadata from the admin API. Localization and curation surfaces stay mounted while the current read payload remains intentionally narrow."
      : "This route expects a valid numeric category id from the category registry.";

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
      supportedCmsLanguageOptions.filter(
        (option) =>
          !localizations.some(
            (localization) => localization.languageCode === option.code,
          ),
      ),
    [localizations],
  );

  const tabItems = localizations.map((localization) => ({
    code: localization.languageCode,
    label: localization.languageLabel,
    tone: localization.isPublished
      ? ("success" as const)
      : ("default" as const),
    meta: localization.statusLabel,
    description: localization.hasImage
      ? "Image ready for this language workspace."
      : "No image selected yet for this language workspace.",
  }));

  function renderToolbar() {
    if (category) {
      return <CategorySummaryCard category={category} />;
    }

    const title = !hasValidCategoryId
      ? "Invalid route"
      : categoryQuery.isNotFound
        ? "Category not found"
        : categoryQuery.problem
          ? "Metadata unavailable"
          : "Loading metadata";
    const description = !hasValidCategoryId
      ? "Open a category record from the registry to load a valid detail workspace."
      : categoryQuery.isNotFound
        ? "The admin API did not return a category record for this route."
        : categoryQuery.problem
          ? "Retry the detail query to restore the base category metadata."
          : "The detail shell is requesting the current base category metadata.";

    return (
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3 md:col-span-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Detail Status
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">{title}</p>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Route
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            /categories/{categoryId || "?"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Inline loading, error, and not-found states stay inside this shell.
          </p>
        </div>
      </div>
    );
  }

  function renderDetailContent() {
    if (!hasValidCategoryId) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/categories">Return to category registry</Link>
            </Button>
          }
          description="Open a category record from the registry to reach a valid category detail workspace."
          title="Invalid category route"
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
                Loading category detail
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                The CMS is requesting the current base metadata for this
                category record.
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
              <Link to="/categories">Return to category registry</Link>
            </Button>
          }
          description="The requested category record does not exist in the current backend environment."
          title="Category not found"
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
              Retry
            </Button>
          }
          problem={categoryQuery.problem}
        />
      );
    }

    if (!category) {
      return (
        <EmptyState
          description="No detail payload is available for this category route yet."
          title="Category detail unavailable"
        />
      );
    }

    return (
      <>
        <FormSection
          description="Update the base category metadata. Category creation is live from the registry, and this form now saves slug, premium, and active state through the admin API."
          title="Metadata"
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
              Create localization
            </Button>
          }
          description="Category localization creation, update, and read are live. This workspace now hydrates persisted localization tabs from the admin API."
          title="Localization"
        >
          {localizationQuery.isLoading && localizations.length === 0 ? (
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardContent className="flex min-h-52 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
                <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
                  <LoaderCircle className="size-6 animate-spin" />
                </div>
                <div className="space-y-2">
                  <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                    Loading localizations
                  </h2>
                  <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                    The CMS is requesting persisted category localization tabs
                    for this category.
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
                      Retry
                    </Button>
                  }
                  problem={localizationQuery.problem}
                />
              ) : null}

              <LanguageTabs
                items={tabItems}
                listLabel="Category localization tabs"
                value={resolvedLanguageCode}
                onValueChange={setSelectedLanguageCode}
              />

              <CategoryLocalizationForm
                key={`${category.id}-${selectedLocalization.languageCode}-${selectedLocalization.name}-${selectedLocalization.status}`}
                availableLanguages={supportedCmsLanguageOptions}
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
                  Retry
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
                  Create first localization
                </Button>
              }
              description="Create the first category localization to open a persistent language workspace for this category."
              title="No localizations yet"
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
        eyebrow="Category Studio"
        title={routeTitle}
        description={routeDescription}
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to="/categories">Return to category registry</Link>
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
              Create localization
            </Button>
            {selectedLocalization ? (
              <Button asChild type="button" variant="outline">
                <a href="#curation">Open curation</a>
              </Button>
            ) : (
              <Button disabled type="button" variant="outline">
                Open curation
              </Button>
            )}
          </>
        }
        toolbar={renderToolbar()}
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Category Summary</CardTitle>
                <CardDescription>
                  This detail view combines category metadata, persisted
                  localization tabs, and language-scoped curation.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Base category metadata loads first so the studio can keep the
                  shell stable through loading, retry, and not-found states.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Localization create, update, and list reads are live, and
                  curation now mirrors those persisted language workspaces.
                  Curated rows hydrate per selected language.
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Localization Snapshot</CardTitle>
                <CardDescription>
                  Persisted localization tabs now hydrate from the admin API and
                  remain visible after refresh.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {localizations.length === 0
                    ? "No category localizations are stored for this category yet."
                    : `${localizations.length} localization workspace${
                        localizations.length === 1 ? "" : "s"
                      } currently hydrated from the backend.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Published localizations are the prerequisite for add and
                  reorder actions. Stored curated rows still remain visible even
                  when the selected locale is not published.
                </div>
              </CardContent>
            </Card>
          </>
        }
      >
        {renderDetailContent()}
      </ContentPageShell>

      <Dialog
        open={isCreateLocalizationOpen}
        onOpenChange={setIsCreateLocalizationOpen}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Create category localization</DialogTitle>
            <DialogDescription>
              Create a language workspace for this category. Saved localizations
              persist in backend reads and remain visible after refresh.
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
