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
      title:
        locale === "tr" ? "Henüz yerelleştirme yok" : "No localizations yet",
      detail:
        locale === "tr"
          ? "İlk dil çalışma alanını eklemek için kaydı açın."
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
      header: locale === "tr" ? "İçerik" : "Content",
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
      header: locale === "tr" ? "Tür" : "Format",
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
      header: locale === "tr" ? "Yerelleştirmeler" : "Localizations",
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
      header: locale === "tr" ? "Hikaye Sayfaları" : "Story Pages",
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
            ? "İçerik listesi admin API üzerinden yüklenemedi."
            : "The content list could not be loaded from the admin API."
        }
        emptyTitle={
          locale === "tr"
            ? "İçerik listesi kullanılamıyor"
            : "Content list unavailable"
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
      caption={
        locale === "tr" ? "İçerik kayıt tablosu" : "Content registry table"
      }
      columns={columns}
      emptyDescription={
        locale === "tr"
          ? "Henüz içerik kaydı yok. Editoryal kataloğu başlatmak için ilk içeriği oluşturun."
          : "No content records exist yet. Create the first content item to start the editorial catalog."
      }
      emptyTitle={locale === "tr" ? "İçerik kaydı yok" : "No content records"}
      getRowId={(content) => content.summary.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS, içerik metadatasını ve yerelleştirme özetlerini admin API üzerinden istiyor."
          : "The CMS is requesting content metadata and localization snapshots from the admin API."
      }
      loadingTitle={
        locale === "tr" ? "İçerik kaydı yükleniyor" : "Loading content registry"
      }
      onRetry={onRetry}
      onRowClick={onContentSelect}
      problem={contents.length > 0 ? problem : null}
      rows={contents}
    />
  );
}
