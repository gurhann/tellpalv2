import { useState } from "react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type { AdminContentRegistryItem } from "@/features/contents/api/content-admin";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type Props = {
  items: AdminContentRegistryItem[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onContentSelect?: (content: AdminContentRegistryItem) => void;
};

const labels: Record<string, { tr: string; en: string }> = {
  CONTENT_INACTIVE: { tr: "İçerik pasif", en: "Content is inactive" },
  LOCALIZATION_MISSING: {
    tr: "Seçili dil eksik",
    en: "Selected locale is missing",
  },
  DESCRIPTION_MISSING: { tr: "Açıklama eksik", en: "Description is missing" },
  COVER_MISSING: { tr: "Kapak görseli eksik", en: "Cover image is missing" },
  STORY_PAGES_MISSING: {
    tr: "Hikâye sayfası eksik",
    en: "Story pages are missing",
  },
  PAGE_LOCALIZATION_MISSING: {
    tr: "Sayfa dili eksik",
    en: "Page localization is missing",
  },
  PAGE_TEXT_MISSING: { tr: "Sayfa metni eksik", en: "Page text is missing" },
  PAGE_AUDIO_MISSING: { tr: "Sayfa sesi eksik", en: "Page audio is missing" },
  PAGE_ILLUSTRATION_MISSING: {
    tr: "Sayfa görseli eksik",
    en: "Page illustration is missing",
  },
  PROCESSING_NOT_COMPLETED: {
    tr: "İşleme tamamlanmadı",
    en: "Processing is not complete",
  },
};

export function ContentListTable({
  items,
  isLoading,
  problem,
  onRetry,
  onContentSelect,
}: Props) {
  const { locale, formatDateTime } = useI18n();
  const [expanded, setExpanded] = useState<number | null>(null);
  const columns: DataTableColumn<AdminContentRegistryItem>[] = [
    {
      id: "content",
      header: locale === "tr" ? "İçerik" : "Content",
      cell: (item) => (
        <div className="space-y-1">
          <p className="font-medium">
            {item.title ??
              (locale === "tr" ? "Yerelleştirme yok" : "No localization")}
          </p>
          <p className="text-xs text-muted-foreground">
            {item.externalKey} · #{item.contentId}
          </p>
        </div>
      ),
    },
    {
      id: "type",
      header: locale === "tr" ? "Tür" : "Type",
      cell: (item) => <span className="text-sm">{item.type}</span>,
    },
    {
      id: "readiness",
      header: locale === "tr" ? "Yayın durumu" : "Readiness",
      cell: (item) => {
        const status =
          item.readiness === "PUBLISHED"
            ? locale === "tr"
              ? "Yayında"
              : "Published"
            : item.readiness === "READY_TO_PUBLISH"
              ? locale === "tr"
                ? "Yayına hazır"
                : "Ready to publish"
              : locale === "tr"
                ? "Aksiyon gerekli"
                : "Action required";
        return (
          <div className="space-y-2">
            <span className="inline-flex rounded-full border border-border/70 bg-muted px-2.5 py-1 text-xs font-medium">
              {status}
            </span>
            {item.blockers.length ? (
              <div>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  aria-expanded={expanded === item.contentId}
                  onClick={(event) => {
                    event.stopPropagation();
                    setExpanded(
                      expanded === item.contentId ? null : item.contentId,
                    );
                  }}
                >
                  {item.blockers.length}{" "}
                  {locale === "tr" ? "yayın engeli" : "publish blockers"}
                </Button>
                {expanded === item.contentId ? (
                  <ul
                    className="mt-2 space-y-1 rounded-lg border bg-background p-2 text-xs"
                    onClick={(event) => event.stopPropagation()}
                  >
                    {item.blockers.map((blocker, index) => (
                      <li
                        key={`${blocker.code}-${blocker.pageNumber}-${index}`}
                      >
                        {labels[blocker.code]?.[locale] ?? blocker.code}
                        {blocker.pageNumber
                          ? ` · ${locale === "tr" ? "Sayfa" : "Page"} ${blocker.pageNumber}`
                          : ""}
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>
            ) : null}
          </div>
        );
      },
    },
    {
      id: "pages",
      header: locale === "tr" ? "Sayfalar" : "Pages",
      cell: (item) => (
        <span className="text-sm text-muted-foreground">
          {item.pageCount ?? "—"}
        </span>
      ),
    },
    {
      id: "updated",
      header: locale === "tr" ? "Son güncelleme" : "Last updated",
      cell: (item) => (
        <span className="text-sm text-muted-foreground">
          {formatDateTime(item.lastEditedAt, {
            dateStyle: "short",
            timeStyle: "short",
          })}
        </span>
      ),
    },
  ];
  return (
    <DataTable
      caption={locale === "tr" ? "İçerik kayıt tablosu" : "Content registry"}
      columns={columns}
      rows={items}
      getRowId={(item) => `${item.contentId}`}
      isLoading={isLoading}
      problem={problem}
      onRetry={onRetry}
      onRowClick={onContentSelect}
      emptyTitle={locale === "tr" ? "İçerik bulunamadı" : "No content found"}
      emptyDescription={
        locale === "tr"
          ? "Filtreleri değiştirin veya yeni içerik oluşturun."
          : "Change filters or create content."
      }
    />
  );
}
