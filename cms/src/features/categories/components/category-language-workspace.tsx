import type {
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";

type CategoryLanguageWorkspaceProps = {
  category: CategorySummaryViewModel;
  curationItemCount: number;
  localization: CategoryLocalizationViewModel;
};

export function CategoryLanguageWorkspace({
  category,
  curationItemCount,
  localization,
}: CategoryLanguageWorkspaceProps) {
  return (
    <div
      className="rounded-2xl border border-border/70 bg-background px-4 py-4"
      id={`curation-workspace-${localization.languageCode}`}
    >
      <p className="text-sm font-medium text-foreground">
        {localization.languageLabel} curation workspace
      </p>
      <p className="mt-2 text-sm text-muted-foreground">
        {category.typeLabel} items only. {curationItemCount} curated row
        {curationItemCount === 1 ? "" : "s"} currently loaded.
      </p>
    </div>
  );
}
