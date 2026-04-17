import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { FreeAccessContentLink } from "@/features/free-access/components/free-access-content-link";
import { RevokeFreeAccessButton } from "@/features/free-access/components/revoke-free-access-button";
import type { FreeAccessGrantViewModel } from "@/features/free-access/model/free-access-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type FreeAccessTableProps = {
  entries: FreeAccessGrantViewModel[];
  contentLookup: Map<number, ContentReadViewModel>;
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  isDefaultScope: boolean;
  effectiveAccessKey: string;
  onRetry?: () => void;
};

function createColumns(
  locale: "en" | "tr",
  contentLookup: Map<number, ContentReadViewModel>,
): DataTableColumn<FreeAccessGrantViewModel>[] {
  return [
    {
      id: "accessKey",
      header: locale === "tr" ? "Access key" : "Access key",
      cell: (entry) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">{entry.accessKey}</p>
          <p className="text-xs text-muted-foreground">Grant #{entry.id}</p>
        </div>
      ),
    },
    {
      id: "content",
      header: locale === "tr" ? "Icerik" : "Content",
      cell: (entry) => (
        <FreeAccessContentLink
          content={contentLookup.get(entry.contentId) ?? null}
          contentId={entry.contentId}
        />
      ),
    },
    {
      id: "language",
      header: locale === "tr" ? "Dil" : "Language",
      cell: (entry) => (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {entry.languageLabel}
          </p>
          <p className="text-xs uppercase text-muted-foreground">
            {entry.languageCode}
          </p>
        </div>
      ),
    },
    {
      id: "actions",
      header: locale === "tr" ? "Aksiyon" : "Action",
      align: "right",
      cell: (entry) => <RevokeFreeAccessButton entry={entry} />,
    },
  ];
}

export function FreeAccessTable({
  entries,
  contentLookup,
  isLoading = false,
  problem = null,
  isDefaultScope,
  effectiveAccessKey,
  onRetry,
}: FreeAccessTableProps) {
  const { locale } = useI18n();
  const columns = createColumns(locale, contentLookup);

  return (
    <DataTable
      caption={
        locale === "tr" ? "Free access grant tablosu" : "Free access grant table"
      }
      columns={columns}
      emptyDescription={
        isDefaultScope
          ? locale === "tr"
            ? "Varsayilan access key seti icin henuz grant kaydi yok."
            : "No grants exist yet for the default access key set."
          : locale === "tr"
            ? `\`${effectiveAccessKey}\` key'i icin grant bulunamadi. Varsayilan sete otomatik donus yapilmadi.`
            : `No grants were found for \`${effectiveAccessKey}\`. The UI intentionally did not fall back to the default set.`
      }
      emptyTitle={
        isDefaultScope
          ? locale === "tr"
            ? "Varsayilan grant kaydi yok"
            : "No default grants"
          : locale === "tr"
            ? "Bu key icin grant yok"
            : "No grants for this key"
      }
      getRowId={(entry) => entry.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS free access grant kayitlarini admin API uzerinden istiyor."
          : "The CMS is requesting free access grant records from the admin API."
      }
      loadingTitle={
        locale === "tr"
          ? "Free access kayitlari yukleniyor"
          : "Loading free access grants"
      }
      onRetry={onRetry}
      problem={entries.length > 0 ? problem : problem}
      rows={entries}
    />
  );
}
