import { ArrowRight, CirclePlus, Layers3, LoaderCircle } from "lucide-react";
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
import { CategoryForm } from "@/features/categories/components/category-form";
import { CategoryLocalizationForm } from "@/features/categories/components/category-localization-form";
import { CategorySummaryCard } from "@/features/categories/components/category-summary-card";
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
    ? `Base metadata for ${category.slug} is now editable. Localization creation and update are live, but localization tabs remain session-backed until a dedicated admin read endpoint exists.`
    : hasValidCategoryId
      ? "The CMS is loading base category metadata from the admin API. Localization and curation surfaces stay mounted while the current read payload remains intentionally narrow."
      : "This route expects a valid numeric category id from the category registry.";

  const selectedLocalization =
    localizations.find(
      (localization) => localization.languageCode === selectedLanguageCode,
    ) ??
    localizations[0] ??
    null;

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
          description="Category localization creation and update are live, but this workspace can only show localizations created or updated in the current session because the backend has no admin localization read endpoint yet."
          title="Localization"
        >
          {selectedLocalization ? (
            <>
              <LanguageTabs
                items={tabItems}
                listLabel="Category localization tabs"
                value={selectedLocalization.languageCode}
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
              description="Create the first category localization to open a language workspace. Existing backend localizations are not readable yet, so only current-session changes appear here."
              title="No session localizations yet"
            />
          )}
        </FormSection>

        <Card
          className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5"
          id="curation"
        >
          <CardHeader>
            <CardTitle>Curation workspace preview</CardTitle>
            <CardDescription>
              Language-scoped category curation will live directly under this
              detail route.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Planned curation actions
              </p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Add content by id, adjust display order, and remove curated
                links from the selected category language workspace after
                localization editing settles.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                /categories/{parsedCategoryId}
              </span>
              <ArrowRight className="size-4" />
              <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                /categories/{parsedCategoryId}#curation
              </span>
            </div>
          </CardContent>
        </Card>
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
            <Button disabled type="button" variant="outline">
              Open curation
            </Button>
          </>
        }
        toolbar={renderToolbar()}
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Read Surface Constraints</CardTitle>
                <CardDescription>
                  Current backend category reads are intentionally narrower than
                  the target studio workspace.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  `GET /api/admin/categories/
                  {hasValidCategoryId ? parsedCategoryId : "{categoryId}"}` is
                  live, but today it returns only base metadata.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Localization create and update are live, but localization tabs
                  are session-backed until a dedicated admin read endpoint
                  exists.
                </div>
                <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                  <Layers3 className="size-3.5" />
                  Live read shell ready for curation expansion
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Localization Snapshot</CardTitle>
                <CardDescription>
                  Session-backed localization tabs keep current edits visible
                  even though the backend still lacks localization reads.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {localizations.length === 0
                    ? "No category localizations have been created or updated in this session yet."
                    : `${localizations.length} localization workspace${
                        localizations.length === 1 ? "" : "s"
                      } currently visible in this session.`}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Published localizations are the prerequisite for
                  language-scoped category curation in the next module.
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Next Category Tasks</CardTitle>
                <CardDescription>
                  After metadata and localization writes, category work expands
                  into language-scoped curation.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  `M07` expands this route into the full category curation
                  workspace with add, reorder, and remove flows.
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
              Create a language workspace for this category. Tabs in this task
              show localizations created or updated in the current CMS session.
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
