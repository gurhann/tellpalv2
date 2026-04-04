import { CheckCircle2, Clock3, ShieldCheck } from "lucide-react";

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
  const readinessTitle = localization.isPublished
    ? "Published locale ready for curation"
    : "Localization still blocked from curation";
  const readinessDescription = localization.isPublished
    ? "This localization is published, so add and reorder flows can already attach to this language workspace."
    : "Publish this localization before language-scoped curation actions unlock.";
  const sessionTitle =
    curationItemCount > 0
      ? `${curationItemCount} curated row${curationItemCount === 1 ? "" : "s"}`
      : "No curated rows yet";
  const sessionDescription =
    curationItemCount > 0
      ? "Stored curated rows for this language are now visible and can be reordered below."
      : "Use the add dialog to create the first curated row for this language.";

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
          This workspace contains curated content rows scoped to{" "}
          {localization.languageLabel}. The category localization{" "}
          <span className="font-medium text-foreground">
            {localization.name}
          </span>{" "}
          stays separate from other languages.
        </p>
      </div>

      <div className="grid gap-3 md:grid-cols-4">
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
            Add, reorder, and remove actions affect only{" "}
            <span className="font-medium text-foreground">
              {localization.languageLabel}
            </span>{" "}
            curated rows for category{" "}
            <span className="font-medium text-foreground">#{category.id}</span>.
          </p>
        </div>

        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
          <div className="flex items-center gap-2 text-sm font-medium text-foreground">
            <CheckCircle2 className="size-4 text-primary" />
            Curation list
          </div>
          <p className="mt-2 text-sm font-medium text-foreground">
            {sessionTitle}
          </p>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            {sessionDescription}
          </p>
        </div>
      </div>

      <div className="rounded-3xl border border-dashed border-border/80 bg-muted/20 px-6 py-8 text-center">
        <h3 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          Hydrated curation is live
        </h3>
        <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          Add, list, reorder, and remove flows now operate inside this language
          workspace through the admin curation endpoints.
        </p>
      </div>
    </div>
  );
}
