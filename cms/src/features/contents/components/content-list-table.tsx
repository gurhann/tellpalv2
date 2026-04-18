import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type ContentListTableProps = {
  contents: ContentReadViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onContentSelect?: (content: ContentReadViewModel) => void;
};

function getContentTitle(content: ContentReadViewModel) {
  return content.primaryLocalization?.title ?? "Untitled content";
}

function getLocalizationSummary(
  content: ContentReadViewModel,
  locale: "en" | "tr",
) {
  if (content.localizationCount === 0) {
    return {
      title: locale === "tr" ? "Henuz yerellestirme yok" : "No localizations yet",
      detail:
        locale === "tr"
          ? "Ilk dil calisma alanini eklemek icin kaydi acin."
          : "Open the record to add the first language workspace.",
    };
  }

  const languageCodes = content.localizations
    .map((localization) => localization.languageCode.toUpperCase())
    .join(", ");

  return {
    title:
      locale === "tr"
        ? `${content.localizationCount} dil`
        : `${content.localizationCount} locale${
            content.localizationCount === 1 ? "" : "s"
          }`,
    detail: languageCodes,
  };
}

export function ContentListTable({
  contents,
  isLoading = false,
  problem = null,
  onRetry,
  onContentSelect,
}: ContentListTableProps) {
  const { locale } = useI18n();
  const columns: DataTableColumn<ContentReadViewModel>[] = [
    {
      id: "content",
      header: locale === "tr" ? "Icerik" : "Content",
      cell: (content) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">
            {getContentTitle(content)}
          </p>
          <p className="text-xs text-muted-foreground">
            {content.summary.externalKey}
          </p>
        </div>
      ),
    },
    {
      id: "format",
      header: locale === "tr" ? "Tur" : "Format",
      cell: (content) => (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
          {content.summary.typeLabel}
        </span>
      ),
    },
    {
      id: "state",
      header: locale === "tr" ? "Durum" : "State",
      cell: (content) => (
        <span
          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
            content.summary.active
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-border/70 bg-muted/35 text-muted-foreground"
          }`}
        >
          {content.summary.active
            ? locale === "tr"
              ? "Aktif"
              : "Active"
            : locale === "tr"
              ? "Pasif"
              : "Inactive"}
        </span>
      ),
    },
    {
      id: "localizations",
      header: locale === "tr" ? "Yerellestirmeler" : "Localizations",
      cell: (content) => {
        const summary = getLocalizationSummary(content, locale);

        return (
          <div className="space-y-1">
            <p className="text-sm font-medium text-foreground">
              {summary.title}
            </p>
            <p className="text-xs text-muted-foreground">{summary.detail}</p>
          </div>
        );
      },
    },
    {
      id: "pages",
      header: locale === "tr" ? "Hikaye Sayfalari" : "Story Pages",
      cell: (content) => (
        <span className="text-sm text-muted-foreground">
          {content.summary.supportsStoryPages
            ? locale === "tr"
              ? `${content.summary.pageCount ?? 0} sayfa`
              : `${content.summary.pageCount ?? 0} page${
                  content.summary.pageCount === 1 ? "" : "s"
                }`
            : locale === "tr"
              ? "Uygulanmaz"
              : "Not applicable"}
        </span>
      ),
    },
  ];

  if (problem && contents.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription={
          locale === "tr"
            ? "Icerik listesi admin API uzerinden yuklenemedi."
            : "The content list could not be loaded from the admin API."
        }
        emptyTitle={
          locale === "tr" ? "Icerik listesi kullanilamiyor" : "Content list unavailable"
        }
        getRowId={(content) => content.summary.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption={locale === "tr" ? "Icerik kayit tablosu" : "Content registry table"}
      columns={columns}
      emptyDescription={
        locale === "tr"
          ? "Henuz icerik kaydi yok. Editoryal katalogu baslatmak icin ilk icerigi olusturun."
          : "No content records exist yet. Create the first content item to start the editorial catalog."
      }
      emptyTitle={locale === "tr" ? "Icerik kaydi yok" : "No content records"}
      getRowId={(content) => content.summary.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS, icerik metadata'sini ve yerellestirme ozetlerini admin API uzerinden istiyor."
          : "The CMS is requesting content metadata and localization snapshots from the admin API."
      }
      loadingTitle={
        locale === "tr" ? "Icerik kaydi yukleniyor" : "Loading content registry"
      }
      onRetry={onRetry}
      onRowClick={onContentSelect}
      problem={contents.length > 0 ? problem : null}
      rows={contents}
    />
  );
}
