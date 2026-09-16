import { useState } from "react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { WorkspaceStatusPill } from "@/components/workspace/workspace-primitives";
import {
  type AdminContentRegistryItem,
  type ContentType,
} from "@/features/contents/api/content-admin";
import { useI18n } from "@/i18n/locale-provider";
import { resolveLanguageLabel } from "@/lib/languages";
import type { ApiProblemDetail } from "@/types/api";

type Props = {
  activeType?: ContentType;
  items: AdminContentRegistryItem[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onResetFilters?: () => void;
  onContentSelect?: (content: AdminContentRegistryItem) => void;
};

const blockerLabels: Record<string, { tr: string; en: string }> = {
  CONTENT_INACTIVE: { tr: "İçerik pasif", en: "Content is inactive" },
  LOCALIZATION_MISSING: {
    tr: "Seçili dil eksik",
    en: "Selected locale is missing",
  },
  DESCRIPTION_MISSING: { tr: "Açıklama eksik", en: "Description is missing" },
  COVER_MISSING: { tr: "Kapak görseli eksik", en: "Cover image is missing" },
  BODY_TEXT_MISSING: { tr: "Gövde metni eksik", en: "Body text is missing" },
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

function getTypeLabel(type: ContentType, locale: "tr" | "en") {
  return {
    STORY: { tr: "Hikâyeler", en: "Stories" },
    MEDITATION: { tr: "Meditasyonlar", en: "Meditations" },
    LULLABY: { tr: "Ninniler", en: "Lullabies" },
  }[type][locale];
}

function getReadinessLabel(
  readiness: AdminContentRegistryItem["readiness"],
  locale: "tr" | "en",
) {
  return {
    ACTION_REQUIRED: { tr: "Aksiyon gerekli", en: "Action required" },
    READY_TO_PUBLISH: { tr: "Yayına hazır", en: "Ready to publish" },
    PUBLISHED: { tr: "Yayında", en: "Published" },
  }[readiness][locale];
}

function getReadinessTone(readiness: AdminContentRegistryItem["readiness"]) {
  if (readiness === "PUBLISHED") return "success" as const;
  if (readiness === "READY_TO_PUBLISH") return "accent" as const;
  return "warning" as const;
}

function formatDuration(durationMinutes: number | null, locale: "tr" | "en") {
  return durationMinutes == null
    ? "—"
    : `${durationMinutes} ${locale === "tr" ? "dk" : "min"}`;
}

function BlockerDisclosure({
  item,
  locale,
  expanded,
  onToggle,
}: {
  item: AdminContentRegistryItem;
  locale: "tr" | "en";
  expanded: boolean;
  onToggle: () => void;
}) {
  const blockersId = `content-blockers-${item.contentId}`;
  const languageLabel = resolveLanguageLabel(item.selectedLanguage, locale);

  return (
    <>
      <Button
        type="button"
        size="sm"
        variant="ghost"
        className="w-fit justify-start px-0 text-xs text-amber-800 hover:bg-transparent hover:underline"
        aria-controls={blockersId}
        aria-expanded={expanded}
        aria-label={
          expanded
            ? locale === "tr"
              ? "Yayın engellerini gizle"
              : "Hide publish blockers"
            : locale === "tr"
              ? `${item.blockers.length} yayın engelini göster`
              : `Show ${item.blockers.length} publish blockers`
        }
        onClick={(event) => {
          event.stopPropagation();
          onToggle();
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.stopPropagation();
          }
        }}
      >
        {expanded
          ? locale === "tr"
            ? "Engelleri gizle"
            : "Hide blockers"
          : `${item.blockers.length} ${locale === "tr" ? "yayın engeli" : "blockers"}`}
      </Button>
      <div
        id={blockersId}
        role="region"
        aria-label={
          locale === "tr"
            ? `${languageLabel} yayın engelleri`
            : `Publish blockers for ${languageLabel}`
        }
        hidden={!expanded}
        className="grid gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-950"
        onClick={(event) => event.stopPropagation()}
        onKeyDown={(event) => event.stopPropagation()}
      >
        <p className="font-semibold">
          {locale === "tr"
            ? `${languageLabel} için yayın engelleri`
            : `Publish blockers for ${languageLabel}`}
        </p>
        <ul className="grid gap-1.5 pl-4 leading-5">
          {item.blockers.map((blocker, index) => (
            <li key={`${blocker.code}-${blocker.pageNumber}-${index}`}>
              {blockerLabels[blocker.code]?.[locale] ?? blocker.code}
              {blocker.pageNumber
                ? ` · ${locale === "tr" ? "Sayfa" : "Page"} ${blocker.pageNumber}`
                : ""}
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}

export function ContentListTable({
  activeType = "STORY",
  items,
  isLoading,
  problem,
  onRetry,
  onResetFilters,
  onContentSelect,
}: Props) {
  const { locale } = useI18n();
  const [expanded, setExpanded] = useState<number | null>(null);
  const columns: DataTableColumn<AdminContentRegistryItem>[] = [
    {
      id: "content",
      header: getTypeLabel(activeType, locale),
      cellClassName: "min-w-0 whitespace-normal",
      cell: (item) => (
        <div className="space-y-1">
          <p className="font-medium">
            {item.title ??
              (locale === "tr" ? "Seçili dil yok" : "No selected locale")}
          </p>
          <p className="text-xs text-muted-foreground">
            {item.externalKey} · #{item.contentId}
          </p>
        </div>
      ),
    },
    {
      id: "type-specific",
      header:
        activeType === "STORY"
          ? locale === "tr"
            ? "Sayfalar"
            : "Pages"
          : locale === "tr"
            ? "Süre"
            : "Duration",
      headerClassName: "hidden sm:table-cell",
      cellClassName: "hidden sm:table-cell",
      cell: (item) =>
        activeType === "STORY"
          ? `${item.pageCount ?? "—"} ${locale === "tr" ? "sayfa" : "pages"}`
          : formatDuration(item.durationMinutes, locale),
    },
    {
      id: "locale",
      header: locale === "tr" ? "Dil" : "Locale",
      headerClassName: "hidden sm:table-cell",
      cellClassName: "hidden sm:table-cell",
      cell: (item) => (
        <div className="space-y-1">
          <p className="text-sm font-medium">
            {resolveLanguageLabel(item.selectedLanguage, locale)}
          </p>
          <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
            {item.selectedLanguage}
          </p>
        </div>
      ),
    },
    {
      id: "readiness",
      header: locale === "tr" ? "Yayın durumu" : "Readiness",
      cellClassName: "whitespace-normal",
      cell: (item) => (
        <div className="grid gap-2">
          <WorkspaceStatusPill tone={getReadinessTone(item.readiness)}>
            {getReadinessLabel(item.readiness, locale)}
          </WorkspaceStatusPill>
          {item.blockers.length > 0 ? (
            <BlockerDisclosure
              expanded={expanded === item.contentId}
              item={item}
              locale={locale}
              onToggle={() =>
                setExpanded((current) =>
                  current === item.contentId ? null : item.contentId,
                )
              }
            />
          ) : (
            <p className="text-xs leading-5 text-muted-foreground">
              {item.readiness === "PUBLISHED"
                ? locale === "tr"
                  ? "Mobilde görünür"
                  : "Visible on mobile"
                : locale === "tr"
                  ? "Yayınlamayı bekliyor"
                  : "Waiting to publish"}
            </p>
          )}
        </div>
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
      emptyAction={
        onResetFilters ? (
          <Button type="button" variant="outline" onClick={onResetFilters}>
            {locale === "tr" ? "Filtreleri temizle" : "Clear filters"}
          </Button>
        ) : null
      }
      emptyTitle={locale === "tr" ? "İçerik bulunamadı" : "No content found"}
      emptyDescription={
        locale === "tr"
          ? "Bu tür, dil veya yayın durumu için eşleşen içerik yok."
          : "No content matches this type, locale, or readiness state."
      }
    />
  );
}
