import { CheckCircle2, Clock3, ShieldCheck } from "lucide-react";

import type {
  CategoryLocalizationViewModel,
  CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";

type CategoryLanguageWorkspaceProps = {
  category: CategorySummaryViewModel;
  localization: CategoryLocalizationViewModel;
};

export function CategoryLanguageWorkspace({
  category,
  localization,
}: CategoryLanguageWorkspaceProps) {
  const readinessTitle = localization.isPublished
    ? "Published locale ready for curation"
    : "Localization still blocked from curation";
  const readinessDescription = localization.isPublished
    ? "This localization is published, so add, reorder, and remove flows can attach to this language workspace next."
    : "Publish this localization before language-scoped curation is enabled in the next task.";

  return (
    <div
      className="grid gap-4"
      id={`curation-workspace-${localization.languageCode}`}
    >
      <div className="rounded-2xl border border-border/70 bg-background px-4 py-4">
        <p className="text-sm font-medium text-foreground">
          {localization.languageLabel} curation workspace
        </p>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          This workspace is reserved for curated content rows scoped to{" "}
          {localization.languageLabel}. The category localization{" "}
          <span className="font-medium text-foreground">
            {localization.name}
          </span>{" "}
          stays separate from other languages.
        </p>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
          <div className="flex items-center gap-2 text-sm font-medium text-foreground">
            <ShieldCheck className="size-4 text-primary" />
            Type gate
          </div>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Only{" "}
            <span className="font-medium text-foreground">
              {category.typeLabel}
            </span>{" "}
            records will be accepted here. Cross-type curation is rejected by
            the backend.
          </p>
        </div>

        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
          <div className="flex items-center gap-2 text-sm font-medium text-foreground">
            <Clock3 className="size-4 text-primary" />
            Readiness
          </div>
          <p className="mt-2 text-sm font-medium text-foreground">
            {readinessTitle}
          </p>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            {readinessDescription}
          </p>
        </div>

        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
          <div className="flex items-center gap-2 text-sm font-medium text-foreground">
            <CheckCircle2 className="size-4 text-primary" />
            Workspace scope
          </div>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Add, reorder, and remove flows in `M07-T02` and `M07-T03` will act
            only on{" "}
            <span className="font-medium text-foreground">
              {localization.languageLabel}
            </span>{" "}
            curated rows for category{" "}
            <span className="font-medium text-foreground">#{category.id}</span>.
          </p>
        </div>
      </div>

      <div className="rounded-3xl border border-dashed border-border/80 bg-muted/20 px-6 py-8 text-center">
        <h3 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          Curated content list lands next
        </h3>
        <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          This language workspace is now reserved inside category detail. The
          next tasks bind add, reorder, and list queries without changing the
          surrounding layout again.
        </p>
      </div>
    </div>
  );
}
