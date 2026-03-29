import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";

type ContentSummaryCardProps = {
  content: ContentReadViewModel;
};

function renderAgeRange(ageRange: number | null) {
  return ageRange === null ? "No age range set" : `Age range ${ageRange}`;
}

export function ContentSummaryCard({ content }: ContentSummaryCardProps) {
  return (
    <div className="grid gap-3 md:grid-cols-3">
      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Metadata
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          {content.summary.typeLabel} /{" "}
          {content.summary.active ? "Active" : "Inactive"} /{" "}
          {renderAgeRange(content.summary.ageRange)}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          External key: {content.summary.externalKey}
        </p>
      </div>

      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Localization
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          {content.localizationCount} language workspace
          {content.localizationCount === 1 ? "" : "s"} prepared
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          {content.publishedLocalizationCount} published /{" "}
          {content.visibleToMobileLocalizationCount} mobile visible
        </p>
      </div>

      <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Processing
        </p>
        <p className="mt-2 text-sm font-medium text-foreground">
          {content.processingCompleteLocalizationCount} localization
          {content.processingCompleteLocalizationCount === 1 ? "" : "s"} ready
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          {content.summary.supportsStoryPages
            ? `${content.summary.pageCount ?? 0} story page${
                content.summary.pageCount === 1 ? "" : "s"
              }`
            : "No story pages for this content type"}
        </p>
      </div>
    </div>
  );
}
