import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { useI18n } from "@/i18n/locale-provider";

type ContentSummaryCardProps = {
  content: ContentReadViewModel;
};

export function ContentSummaryCard({ content }: ContentSummaryCardProps) {
  const { locale } = useI18n();

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-border/70 bg-background px-4 py-3">
      <span className="text-sm font-semibold text-foreground">
        {content.summary.externalKey}
      </span>
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-foreground">
        {content.summary.typeLabel}
      </span>
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-muted-foreground">
        {content.summary.active
          ? locale === "tr"
            ? "Aktif"
            : "Active"
          : locale === "tr"
            ? "Pasif"
            : "Inactive"}
      </span>
      {content.summary.ageRange !== null ? (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium text-muted-foreground">
          {locale === "tr"
            ? `Yaş ${content.summary.ageRange}`
            : `Age ${content.summary.ageRange}`}
        </span>
      ) : null}
      <span className="text-sm text-muted-foreground">
        {locale === "tr"
          ? `${content.localizationCount} dil`
          : `${content.localizationCount} locale${
              content.localizationCount === 1 ? "" : "s"
            }`}
      </span>
      {content.summary.supportsStoryPages ? (
        <span className="text-sm text-muted-foreground">
          {locale === "tr"
            ? `${content.summary.pageCount ?? 0} sayfa`
            : `${content.summary.pageCount ?? 0} page${
                content.summary.pageCount === 1 ? "" : "s"
              }`}
        </span>
      ) : null}
    </div>
  );
}
