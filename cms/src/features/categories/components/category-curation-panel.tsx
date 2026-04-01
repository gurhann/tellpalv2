import { ArrowRight, CirclePlus, ListFilter } from "lucide-react";

import { EmptyState } from "@/components/feedback/empty-state";
import { LanguageTabs } from "@/components/language/language-tabs";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CategoryLanguageWorkspace } from "@/features/categories/components/category-language-workspace";
import type {
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";

type CategoryCurationPanelProps = {
  category: CategorySummaryViewModel;
  localizations: CategoryLocalizationViewModel[];
  selectedLanguageCode: string;
  onLanguageChange: (languageCode: string) => void;
  onCreateLocalization: () => void;
};

export function CategoryCurationPanel({
  category,
  localizations,
  selectedLanguageCode,
  onLanguageChange,
  onCreateLocalization,
}: CategoryCurationPanelProps) {
  const selectedLocalization =
    localizations.find(
      (localization) => localization.languageCode === selectedLanguageCode,
    ) ??
    localizations[0] ??
    null;

  const tabItems = localizations.map((localization) => ({
    code: localization.languageCode,
    label: localization.languageLabel,
    tone: localization.isPublished
      ? ("success" as const)
      : ("warning" as const),
    meta: localization.statusLabel,
    description: localization.isPublished
      ? "Ready for language-scoped curation."
      : "Publish this locale before curation unlocks.",
  }));

  return (
    <Card
      className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5"
      id="curation"
    >
      <CardHeader>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <CardTitle>Category curation workspace</CardTitle>
            <CardDescription>
              Each localization owns its own curation lane. The shell below is
              now language-scoped so add, reorder, and remove flows can land in
              place during the next tasks.
            </CardDescription>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!selectedLocalization}
              type="button"
              variant="outline"
            >
              <CirclePlus className="size-4" />
              Add curated content
            </Button>
            <Button
              disabled={!selectedLocalization}
              type="button"
              variant="outline"
            >
              <ListFilter className="size-4" />
              Adjust order
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-5">
        {localizations.length === 0 ? (
          <EmptyState
            action={
              <Button
                type="button"
                variant="outline"
                onClick={onCreateLocalization}
              >
                Create first localization
              </Button>
            }
            description="Create or surface the first category localization before preparing language-scoped curation. Without a selected locale, curation actions stay inactive."
            title="No curation language selected"
          />
        ) : (
          <>
            <LanguageTabs
              items={tabItems}
              listLabel="Category curation language tabs"
              renderContent={(item) => {
                const localization = localizations.find(
                  (entry) => entry.languageCode === item.code,
                );

                if (!localization) {
                  return null;
                }

                return (
                  <CategoryLanguageWorkspace
                    category={category}
                    localization={localization}
                  />
                );
              }}
              value={selectedLocalization?.languageCode ?? selectedLanguageCode}
              onValueChange={onLanguageChange}
            />

            <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                /categories/{category.id}
              </span>
              <ArrowRight className="size-4" />
              <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
                /categories/{category.id}#curation-workspace-
                {selectedLocalization?.languageCode ?? selectedLanguageCode}
              </span>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
