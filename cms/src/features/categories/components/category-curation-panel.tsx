import { CirclePlus } from "lucide-react";
import { useState } from "react";

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
import { AddCuratedContentDialog } from "@/features/categories/components/add-curated-content-dialog";
import { CategoryLanguageWorkspace } from "@/features/categories/components/category-language-workspace";
import { CuratedContentList } from "@/features/categories/components/curated-content-list";
import type {
  CategoryCurationItemViewModel,
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";
import type { ApiProblemDetail } from "@/types/api";

type CategoryCurationPanelProps = {
  category: CategorySummaryViewModel;
  curationItems: CategoryCurationItemViewModel[];
  curationIsLoading: boolean;
  curationProblem: ApiProblemDetail | null;
  localizations: CategoryLocalizationViewModel[];
  selectedLocalization: CategoryLocalizationViewModel | null;
  selectedLanguageCode: string;
  onLanguageChange: (languageCode: string) => void;
  onCreateLocalization: () => void;
  onRetryCuration: () => void;
};

export function CategoryCurationPanel({
  category,
  curationItems,
  curationIsLoading,
  curationProblem,
  localizations,
  selectedLocalization,
  selectedLanguageCode,
  onLanguageChange,
  onCreateLocalization,
  onRetryCuration,
}: CategoryCurationPanelProps) {
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const canAddCuratedContent = Boolean(selectedLocalization?.isPublished);

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
              Manage curated content for the selected localization.
            </CardDescription>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!canAddCuratedContent}
              onClick={() => setIsAddDialogOpen(true)}
              type="button"
              variant="outline"
            >
              <CirclePlus className="size-4" />
              Add curated content
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
                    curationItemCount={
                      item.code === selectedLocalization?.languageCode
                        ? curationItems.length
                        : 0
                    }
                    localization={localization}
                  />
                );
              }}
              value={selectedLocalization?.languageCode ?? selectedLanguageCode}
              onValueChange={onLanguageChange}
            />

            {selectedLocalization ? (
              <div className="space-y-5">
                {!selectedLocalization.isPublished ? (
                  <p className="text-sm text-muted-foreground">
                    Publish this localization before adding, removing, or
                    reordering curated content.
                  </p>
                ) : null}
                <CuratedContentList
                  category={category}
                  items={curationItems}
                  isLoading={curationIsLoading}
                  localization={selectedLocalization}
                  problem={curationProblem}
                  onRetry={onRetryCuration}
                />
              </div>
            ) : null}
          </>
        )}
      </CardContent>

      {isAddDialogOpen && selectedLocalization ? (
        <AddCuratedContentDialog
          category={category}
          existingItems={curationItems}
          localization={selectedLocalization}
          open={isAddDialogOpen}
          onOpenChange={setIsAddDialogOpen}
        />
      ) : null}
    </Card>
  );
}
