import { ChevronRight, CirclePlus, RotateCcw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import {
  RegistryToolbar,
  RegistryToolbarGroup,
} from "@/components/data/registry-toolbar";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import {
  MockupInfoCard,
  MockupStatusPill,
} from "@/features/mockups/components/mockup-ui";
import { mockupContentRegistry } from "@/features/mockups/fixtures";
import type { MockupContentSummary } from "@/features/mockups/types";
import { useI18n } from "@/i18n/locale-provider";

type ContentType = "STORY" | "MEDITATION" | "LULLABY";
type ContentTab = ContentType;
type ReadinessFilter =
  "ALL" | "ACTION_REQUIRED" | "READY_TO_PUBLISH" | "PUBLISHED";
type LanguageCode = "tr" | "en";

const typeOrder: ContentTab[] = ["STORY", "MEDITATION", "LULLABY"];
const languageOptions: LanguageCode[] = ["tr", "en"];

function getTypeLabel(type: ContentType, locale: "tr" | "en") {
  const labels: Record<ContentType, { tr: string; en: string }> = {
    STORY: { tr: "Hikâyeler", en: "Stories" },
    MEDITATION: { tr: "Meditasyonlar", en: "Meditations" },
    LULLABY: { tr: "Ninniler", en: "Lullabies" },
  };

  return labels[type][locale];
}

function getReadinessLabel(readiness: ReadinessFilter, locale: "tr" | "en") {
  const labels: Record<ReadinessFilter, { tr: string; en: string }> = {
    ALL: { tr: "Tüm durumlar", en: "All statuses" },
    ACTION_REQUIRED: { tr: "Aksiyon gerekli", en: "Action required" },
    READY_TO_PUBLISH: { tr: "Yayına hazır", en: "Ready to publish" },
    PUBLISHED: { tr: "Yayında", en: "Published" },
  };

  return labels[readiness][locale];
}

function getLocalizedState(
  content: MockupContentSummary,
  language: LanguageCode,
) {
  return content.locales.find(
    (localeState) => localeState.languageCode === language,
  );
}

function getLanguageLabel(language: LanguageCode, locale: "tr" | "en") {
  return language === "tr"
    ? locale === "tr"
      ? "Türkçe"
      : "Turkish"
    : locale === "tr"
      ? "İngilizce"
      : "English";
}

function getBlockers(
  content: MockupContentSummary,
  language: LanguageCode,
  locale: "tr" | "en",
) {
  const copy =
    locale === "tr"
      ? {
          inactive: "İçerik pasif",
          missingLocale: "Seçili dil lokalizasyonu yok",
          missingTitle: "Başlık eksik",
          missingCover: "Kapak görseli eksik",
          missingText: "Metin eksik",
          missingAudio: "Ses dosyası eksik",
          missingIllustration: "Görsel eksik",
          processing: "İşleme tamamlanmadı",
        }
      : {
          inactive: "Content is inactive",
          missingLocale: "Selected locale is missing",
          missingTitle: "Title is missing",
          missingCover: "Cover image is missing",
          missingText: "Text is missing",
          missingAudio: "Audio is missing",
          missingIllustration: "Illustration is missing",
          processing: "Processing is not complete",
        };
  const blockers: string[] = [];
  const localizedState = getLocalizedState(content, language);

  if (!content.active) blockers.push(copy.inactive);
  if (!localizedState) {
    blockers.push(copy.missingLocale);
    return blockers;
  }

  if (!localizedState.title.trim()) blockers.push(copy.missingTitle);

  if (content.typeLabel === "STORY") {
    if (localizedState.hasCover === false) blockers.push(copy.missingCover);
    if (localizedState.hasBodyText === false) blockers.push(copy.missingText);
    if (localizedState.hasAudio === false) blockers.push(copy.missingAudio);
    if (localizedState.hasIllustration === false) {
      blockers.push(copy.missingIllustration);
    }
  }

  if (
    content.typeLabel === "MEDITATION" &&
    localizedState.hasBodyText === false
  ) {
    blockers.push(copy.missingText);
  }
  if (localizedState.isProcessingComplete === false) {
    blockers.push(copy.processing);
  }

  return blockers;
}

function getReadiness(
  content: MockupContentSummary,
  language: LanguageCode,
  locale: "tr" | "en",
): Exclude<ReadinessFilter, "ALL"> {
  const localizedState = getLocalizedState(content, language);
  const blockers = getBlockers(content, language, locale);

  if (blockers.length > 0) return "ACTION_REQUIRED";
  if (localizedState?.isPublished) return "PUBLISHED";
  return "READY_TO_PUBLISH";
}

function getReadinessTone(readiness: Exclude<ReadinessFilter, "ALL">) {
  switch (readiness) {
    case "PUBLISHED":
      return "success" as const;
    case "READY_TO_PUBLISH":
      return "accent" as const;
    case "ACTION_REQUIRED":
    default:
      return "warning" as const;
  }
}

function getTitle(content: MockupContentSummary, language: LanguageCode) {
  return getLocalizedState(content, language)?.title || content.externalKey;
}

function getCopy(locale: "tr" | "en") {
  return locale === "tr"
    ? {
        eyebrow: "İçerik operasyonu",
        title: "İçerikler",
        description:
          "İçeriği bul, seçili dilde yayın durumunu gör ve düzenleyiciye geç.",
        backToLab: "Mockup’lara dön",
        create: "İçerik oluştur",
        searchLabel: "İçeriklerde ara",
        searchPlaceholder: "Başlık, anahtar veya ID ile ara",
        typeLabel: "İçerik türü",
        languageLabel: "Dil",
        readinessLabel: "Yayın durumu",
        reset: "Filtreleri temizle",
        records: "içerik",
        languages: "dil",
        pages: "sayfa",
        duration: "Süre",
        noLocalization: "Seçili dil yok",
        mobileVisible: "Mobilde görünür",
        waiting: "Yayınlamayı bekliyor",
        blockers: "engeli gör",
        hideBlockers: "Engelleri gizle",
        blockersFor: (language: string) => `${language} için yayın engelleri`,
        actionCount: "aksiyon gerekli",
        updatedFirst: "Son güncellenen önce",
        reference: "Referans kaydı",
        demo: "Demo kayıt",
        createTitle: "İçerik oluştur",
        createDescription:
          "Bu mockup, metadata adımından sonra seçili dil bağlamını koruyan sade bir içerik oluşturma girişini gösterir.",
        createOutcomeTitle: "Akış notu",
        createOutcomeBody:
          "Kaydetme sonrasında editör ilgili içerik detayına geçer; tür sekmeleri ve liste filtreleri bu ekranda kalır.",
        close: "Kapat",
        tableCaption: "İçerik registry mockup’ı",
        toolbarLabel: "İçerik kayıt kontrolleri",
      }
    : {
        eyebrow: "Content operations",
        title: "Contents",
        description:
          "Find content, read its selected-locale readiness, and move into the editor.",
        backToLab: "Back to mockups",
        create: "Create content",
        searchLabel: "Search contents",
        searchPlaceholder: "Search by title, key, or ID",
        typeLabel: "Content type",
        languageLabel: "Language",
        readinessLabel: "Readiness",
        reset: "Clear filters",
        records: "records",
        languages: "locales",
        pages: "pages",
        duration: "Duration",
        noLocalization: "No selected locale",
        mobileVisible: "Visible on mobile",
        waiting: "Waiting to publish",
        blockers: "blockers",
        hideBlockers: "Hide blockers",
        blockersFor: (language: string) => `Publish blockers for ${language}`,
        actionCount: "need action",
        updatedFirst: "Most recently edited first",
        reference: "Reference item",
        demo: "Demo record",
        createTitle: "Create content",
        createDescription:
          "This mockup shows a quiet metadata entry that preserves the selected locale before the editor handoff.",
        createOutcomeTitle: "Flow note",
        createOutcomeBody:
          "After save, editors move to the content detail route; type tabs and registry filters stay on this screen.",
        close: "Close",
        tableCaption: "Content registry mockup",
        toolbarLabel: "Content registry controls",
      };
}

function TypeTabs({
  value,
  counts,
  locale,
  onValueChange,
}: {
  value: ContentTab;
  counts: Record<ContentTab, number>;
  locale: "tr" | "en";
  onValueChange: (value: ContentTab) => void;
}) {
  const copy = getCopy(locale);

  return (
    <Tabs
      value={value}
      onValueChange={(nextValue) => onValueChange(nextValue as ContentTab)}
    >
      <TabsList
        aria-label={copy.typeLabel}
        className="w-full flex-wrap justify-start gap-x-1 gap-y-1 sm:w-auto"
        variant="line"
      >
        {typeOrder.map((type) => (
          <TabsTrigger
            key={type}
            className="flex-none gap-2 px-3 py-2"
            onClick={() => onValueChange(type)}
            value={type}
          >
            <span>{getTypeLabel(type, locale)}</span>
            <span
              aria-hidden="true"
              className="rounded-full bg-muted px-1.5 py-0.5 text-[0.7rem] font-semibold text-muted-foreground"
            >
              {counts[type]}
            </span>
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  );
}

function ReadinessCell({
  content,
  language,
  locale,
  expanded,
  onToggle,
}: {
  content: MockupContentSummary;
  language: LanguageCode;
  locale: "tr" | "en";
  expanded: boolean;
  onToggle: (contentId: string) => void;
}) {
  const copy = getCopy(locale);
  const readiness = getReadiness(content, language, locale);
  const blockers = getBlockers(content, language, locale);
  const localizedState = getLocalizedState(content, language);
  const detail =
    readiness === "PUBLISHED"
      ? copy.mobileVisible
      : readiness === "READY_TO_PUBLISH"
        ? copy.waiting
        : blockers.length === 1
          ? blockers[0]
          : `${blockers.length} ${copy.blockers}`;

  return (
    <div className="grid gap-2">
      <MockupStatusPill tone={getReadinessTone(readiness)}>
        {getReadinessLabel(readiness, locale)}
      </MockupStatusPill>
      {blockers.length > 0 ? (
        <>
          <Button
            aria-controls={expanded ? `blockers-${content.id}` : undefined}
            aria-expanded={expanded}
            className="w-fit justify-start px-0 text-xs text-amber-800 hover:bg-transparent hover:underline"
            type="button"
            variant="ghost"
            size="xs"
            onClick={(event) => {
              event.stopPropagation();
              onToggle(content.id);
            }}
          >
            {expanded
              ? copy.hideBlockers
              : `${blockers.length} ${copy.blockers}`}
          </Button>
          {expanded ? (
            <div
              className="grid gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-950"
              id={`blockers-${content.id}`}
              onClick={(event) => event.stopPropagation()}
              role="region"
              aria-label={copy.blockersFor(getLanguageLabel(language, locale))}
            >
              <p className="font-semibold">
                {copy.blockersFor(getLanguageLabel(language, locale))}
              </p>
              <ul className="grid gap-1.5 pl-4 leading-5">
                {blockers.map((blocker) => (
                  <li key={blocker}>{blocker}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </>
      ) : (
        <p className="text-xs leading-5 text-muted-foreground">{detail}</p>
      )}
      {!localizedState ? (
        <span className="text-xs text-muted-foreground">
          {copy.noLocalization}
        </span>
      ) : null}
    </div>
  );
}

export function MockupContentsRoute() {
  const { locale } = useI18n();
  const navigate = useNavigate();
  const copy = getCopy(locale);
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState<ContentTab>("STORY");
  const [selectedLanguage, setSelectedLanguage] = useState<LanguageCode>("tr");
  const [selectedReadiness, setSelectedReadiness] =
    useState<ReadinessFilter>("ALL");
  const [expanded, setExpanded] = useState<string | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const deferredSearch = useDeferredValue(search);

  const typeCounts = useMemo(() => {
    const counts = Object.fromEntries(
      typeOrder.map((type) => [type, 0]),
    ) as Record<ContentTab, number>;

    mockupContentRegistry.forEach((content) => {
      counts[content.typeLabel] += 1;
    });

    return counts;
  }, []);

  const filteredContents = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return mockupContentRegistry.filter((content) => {
      if (content.typeLabel !== selectedType) {
        return false;
      }

      if (
        selectedReadiness !== "ALL" &&
        getReadiness(content, selectedLanguage, locale) !== selectedReadiness
      ) {
        return false;
      }

      if (normalizedSearch.length === 0) return true;

      const localizedTitle = getTitle(content, selectedLanguage).toLowerCase();
      return (
        localizedTitle.includes(normalizedSearch) ||
        content.externalKey.toLowerCase().includes(normalizedSearch) ||
        content.id.toLowerCase().includes(normalizedSearch)
      );
    });
  }, [
    deferredSearch,
    locale,
    selectedLanguage,
    selectedReadiness,
    selectedType,
  ]);

  const actionRequiredCount = filteredContents.filter(
    (content) =>
      getReadiness(content, selectedLanguage, locale) === "ACTION_REQUIRED",
  ).length;
  const hasActiveFilters =
    search.trim().length > 0 ||
    selectedLanguage !== "tr" ||
    selectedReadiness !== "ALL";

  const resetFilters = () => {
    setSearch("");
    setSelectedLanguage("tr");
    setSelectedReadiness("ALL");
    setExpanded(null);
  };

  const getCompactTypeMeta = (content: MockupContentSummary) => {
    return selectedType === "STORY"
      ? `${content.pageCount ?? "—"} ${copy.pages}`
      : null;
  };
  const formatDuration = (content: MockupContentSummary) => {
    const durationMinutes =
      content.typeLabel === "MEDITATION"
        ? getLocalizedState(content, selectedLanguage)?.durationMinutes
        : content.playbackDurationMinutes;

    return durationMinutes == null
      ? "—"
      : `${durationMinutes} ${locale === "tr" ? "dk" : "min"}`;
  };

  const identityColumn: DataTableColumn<MockupContentSummary> = {
    id: "content",
    header: getTypeLabel(selectedType, locale),
    cellClassName: "min-w-0 whitespace-normal",
    cell: (content) => (
      <div className="space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-medium text-foreground">
            {getTitle(content, selectedLanguage)}
          </p>
          <MockupStatusPill tone={content.isDemo ? "accent" : "default"}>
            {content.isDemo ? copy.demo : copy.reference}
          </MockupStatusPill>
          {selectedType === "STORY" ? (
            <ChevronRight
              aria-hidden="true"
              className="size-4 text-muted-foreground"
            />
          ) : null}
        </div>
        <p className="text-xs text-muted-foreground">
          {content.externalKey} · #{content.id}
        </p>
        {getCompactTypeMeta(content) ? (
          <p className="text-xs text-muted-foreground sm:hidden">
            {getCompactTypeMeta(content)}
          </p>
        ) : null}
      </div>
    ),
  };
  const coverageColumn: DataTableColumn<MockupContentSummary> = {
    id: "coverage",
    header: locale === "tr" ? "Dil kapsamı" : "Locale coverage",
    headerClassName: "hidden sm:table-cell",
    cellClassName: "hidden sm:table-cell",
    cell: (content) => {
      const localizedState = getLocalizedState(content, selectedLanguage);

      return (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {content.locales.length} {copy.languages}
          </p>
          <p className="text-xs text-muted-foreground">
            {selectedLanguage.toUpperCase()} ·{" "}
            {localizedState
              ? getLanguageLabel(selectedLanguage, locale)
              : copy.noLocalization}
          </p>
        </div>
      );
    },
  };
  const readinessColumn: DataTableColumn<MockupContentSummary> = {
    id: "readiness",
    header: locale === "tr" ? "Yayın durumu" : "Readiness",
    cellClassName: "whitespace-normal",
    cell: (content) => (
      <ReadinessCell
        content={content}
        expanded={expanded === content.id}
        language={selectedLanguage}
        locale={locale}
        onToggle={(contentId) =>
          setExpanded((current) => (current === contentId ? null : contentId))
        }
      />
    ),
  };
  const columns: DataTableColumn<MockupContentSummary>[] =
    selectedType === "STORY"
      ? [
          identityColumn,
          {
            id: "pages",
            header: locale === "tr" ? "Sayfalar" : "Pages",
            headerClassName: "hidden sm:table-cell",
            cellClassName: "hidden sm:table-cell",
            cell: (content) => `${content.pageCount ?? "—"} ${copy.pages}`,
          },
          coverageColumn,
          readinessColumn,
        ]
      : [
          identityColumn,
          {
            id: "duration",
            header: copy.duration,
            headerClassName: "hidden sm:table-cell",
            cellClassName: "hidden sm:table-cell",
            cell: (content) => formatDuration(content),
          },
          coverageColumn,
          readinessColumn,
        ];

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to="/labs/mockups">{copy.backToLab}</Link>
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        toolbar={
          <div className="space-y-4" data-testid="contents-registry-mockup">
            <div className="border-b border-border/60 pb-1">
              <TypeTabs
                counts={typeCounts}
                locale={locale}
                onValueChange={(value) => {
                  setSelectedType(value);
                  setExpanded(null);
                }}
                value={selectedType}
              />
            </div>

            <RegistryToolbar
              ariaLabel={copy.toolbarLabel}
              search={
                <RegistryToolbarGroup
                  className="w-full"
                  label={copy.searchLabel}
                >
                  <div className="relative min-w-[16rem] flex-1">
                    <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                    <Input
                      aria-label={copy.searchLabel}
                      className="pl-8"
                      placeholder={copy.searchPlaceholder}
                      value={search}
                      onChange={(event) => {
                        setSearch(event.target.value);
                        setExpanded(null);
                      }}
                    />
                  </div>
                </RegistryToolbarGroup>
              }
              filters={
                <>
                  <RegistryToolbarGroup label={copy.languageLabel}>
                    <Select
                      value={selectedLanguage}
                      onValueChange={(value) => {
                        setSelectedLanguage(value as LanguageCode);
                        setExpanded(null);
                      }}
                    >
                      <SelectTrigger
                        aria-label={copy.languageLabel}
                        className="w-[9.5rem]"
                      >
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {languageOptions.map((language) => (
                          <SelectItem key={language} value={language}>
                            {getLanguageLabel(language, locale)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </RegistryToolbarGroup>
                  <RegistryToolbarGroup label={copy.readinessLabel}>
                    <Select
                      value={selectedReadiness}
                      onValueChange={(value) => {
                        setSelectedReadiness(value as ReadinessFilter);
                        setExpanded(null);
                      }}
                    >
                      <SelectTrigger
                        aria-label={copy.readinessLabel}
                        className="w-[11.5rem]"
                      >
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {(
                          [
                            "ALL",
                            "ACTION_REQUIRED",
                            "READY_TO_PUBLISH",
                            "PUBLISHED",
                          ] as ReadinessFilter[]
                        ).map((readiness) => (
                          <SelectItem key={readiness} value={readiness}>
                            {getReadinessLabel(readiness, locale)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </RegistryToolbarGroup>
                  {hasActiveFilters ? (
                    <Button
                      className="self-end"
                      type="button"
                      variant="ghost"
                      onClick={resetFilters}
                    >
                      <RotateCcw className="size-4" />
                      {copy.reset}
                    </Button>
                  ) : null}
                </>
              }
              summaryTitle={`${filteredContents.length} ${copy.records} · ${selectedLanguage.toUpperCase()}`}
              summaryDescription={`${actionRequiredCount} ${copy.actionCount} · ${copy.updatedFirst}`}
            />
          </div>
        }
      >
        <div data-testid="contents-type-table">
          <DataTable
            caption={copy.tableCaption}
            columns={columns}
            getRowId={(content) => content.id}
            onRowClick={(content) =>
              navigate(`/labs/mockups/contents/${content.id}`)
            }
            rows={filteredContents}
          />
        </div>
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{copy.createTitle}</DialogTitle>
            <DialogDescription>{copy.createDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-4">
            <MockupInfoCard
              title={copy.createOutcomeTitle}
              description={copy.createOutcomeBody}
            />
          </DialogBody>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateDialogOpen(false)}
            >
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
