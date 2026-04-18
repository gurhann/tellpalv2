import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

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
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentListTable } from "@/features/contents/components/content-list-table";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { getCreateContentFormDefaults } from "@/features/contents/schema/content-schema";
import { useI18n } from "@/i18n/locale-provider";
import {
  buildRegistryFilterSummary,
  sortRegistryTypeLabels,
} from "@/lib/registry-filters";

export function ContentsIndexRoute() {
  const { locale } = useI18n();
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState<string>("ALL");
  const [selectedState, setSelectedState] = useState<
    "ALL" | "ACTIVE" | "INACTIVE"
  >("ALL");
  const contentListQuery = useContentList();
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Editoryal Cekirdek",
          title: "Icerik Studyosu",
          description:
            "Icerik kayitlarini acin, filtreleyin ve gorev odakli detay calisma alanina gecin.",
          refresh: "Yenile",
          create: "Icerik olustur",
          searchLabel: "Icerik kayitlarini ara",
          filtersAria: "Icerik registry filtreleri",
          searchGroupLabel: "Arama",
          typeGroupLabel: "Icerik turu",
          stateGroupLabel: "Durum",
          searchPlaceholder:
            "External key veya yerellestirilmis basliga gore ara",
          filterTypes: "Tum turler",
          filterStates: "Tum durumlar",
          createDialogTitle: "Icerik olustur",
          createDialogDescription:
            "Temel metadata ile yeni bir editoryal kayit olusturun. Kaydetme sonrasi CMS yeni detay rotasini acar.",
          railTitle: "Registry snapshot",
          railDescription:
            "Liste hafif kalirken yayin ve story hazirligini sag tarafta koruyun.",
          activeRecords: "Aktif kayit",
          storyWorkspaces: "Story workspace",
          localeCoverage: "Dil kapsami",
        }
      : {
          eyebrow: "Editorial Core",
          title: "Content Studio",
          description:
            "Open, filter, and move directly into each task-focused content workspace.",
          refresh: "Refresh",
          create: "Create content",
          searchLabel: "Search content registry",
          filtersAria: "Content registry filters",
          searchGroupLabel: "Search",
          typeGroupLabel: "Content type",
          stateGroupLabel: "State",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All types",
          filterStates: "All states",
          createDialogTitle: "Create content",
          createDialogDescription:
            "Create a new editorial record with base metadata. After save, the CMS opens the new detail route.",
          railTitle: "Registry snapshot",
          railDescription:
            "Keep release posture and story readiness visible while the main lane stays quiet.",
          activeRecords: "Active records",
          storyWorkspaces: "Story workspaces",
          localeCoverage: "Locale coverage",
        };

  const typeOptions = useMemo(() => {
    const nextOptions = new Set<string>();

    contentListQuery.contents.forEach((content) => {
      nextOptions.add(content.summary.typeLabel);
    });

    return ["ALL", ...sortRegistryTypeLabels(Array.from(nextOptions))];
  }, [contentListQuery.contents]);

  const filteredContents = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return contentListQuery.contents.filter((content) => {
      if (
        selectedType !== "ALL" &&
        content.summary.typeLabel !== selectedType
      ) {
        return false;
      }

      if (selectedState === "ACTIVE" && !content.summary.active) {
        return false;
      }

      if (selectedState === "INACTIVE" && content.summary.active) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        content.summary.externalKey.toLowerCase().includes(normalizedSearch) ||
        content.localizations.some((localization) =>
          localization.title.toLowerCase().includes(normalizedSearch),
        )
      );
    });
  }, [contentListQuery.contents, deferredSearch, selectedState, selectedType]);

  const activeRecordCount = useMemo(
    () => filteredContents.filter((content) => content.summary.active).length,
    [filteredContents],
  );
  const storyWorkspaceCount = useMemo(
    () =>
      filteredContents.filter((content) => content.summary.supportsStoryPages)
        .length,
    [filteredContents],
  );
  const filteredLocaleCount = useMemo(
    () =>
      filteredContents.reduce(
        (sum, content) => sum + content.localizationCount,
        0,
      ),
    [filteredContents],
  );
  const filteredVisibleLocaleCount = useMemo(
    () =>
      filteredContents.reduce(
        (sum, content) => sum + content.visibleToMobileLocalizationCount,
        0,
      ),
    [filteredContents],
  );
  const selectedStateLabel =
    selectedState === "ALL"
      ? copy.filterStates
      : selectedState === "ACTIVE"
        ? locale === "tr"
          ? "Aktif"
          : "Active"
        : locale === "tr"
          ? "Pasif"
          : "Inactive";
  const filterSummaryTitle = buildRegistryFilterSummary({
    locale,
    filteredCount: filteredContents.length,
    totalCount: contentListQuery.contents.length,
    selectedType,
    allTypesLabel: copy.filterTypes,
    selectedStateLabel,
  });
  const filterSummaryDescription =
    locale === "tr"
      ? "Arama, tur ve durum filtreleri registry gorunumunu aninda daraltir."
      : "Search, type, and state filters narrow the registry immediately.";

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: copy.activeRecords,
                value: `${activeRecordCount} / ${filteredContents.length}`,
                tone: activeRecordCount > 0 ? "success" : "default",
              },
              {
                label: copy.storyWorkspaces,
                value: `${storyWorkspaceCount} / ${filteredContents.length}`,
              },
              {
                label: copy.localeCoverage,
                value: `${filteredVisibleLocaleCount} / ${filteredLocaleCount}`,
                tone:
                  filteredVisibleLocaleCount === filteredLocaleCount &&
                  filteredLocaleCount > 0
                    ? "success"
                    : "warning",
              },
            ]}
          />
        }
        actions={
          <>
            <Button
              type="button"
              variant="outline"
              onClick={() => void contentListQuery.refetch()}
            >
              <RefreshCw
                className={`size-4 ${
                  contentListQuery.isFetching ? "animate-spin" : ""
                }`}
              />
              {copy.refresh}
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        toolbar={
          <RegistryToolbar
            ariaLabel={copy.filtersAria}
            search={
              <RegistryToolbarGroup className="w-full" label={copy.searchGroupLabel}>
                <div className="relative min-w-[16rem] flex-1">
                  <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                  <Input
                    aria-label={copy.searchLabel}
                    className="pl-8"
                    placeholder={copy.searchPlaceholder}
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </div>
              </RegistryToolbarGroup>
            }
            filters={
              <>
                <RegistryToolbarGroup label={copy.typeGroupLabel}>
                <div className="flex flex-wrap items-center gap-2">
                  {typeOptions.map((typeOption) => (
                    <Button
                      key={typeOption}
                      type="button"
                      variant={
                        selectedType === typeOption ? "secondary" : "outline"
                      }
                      size="sm"
                      aria-pressed={selectedType === typeOption}
                      onClick={() => setSelectedType(typeOption)}
                    >
                      {typeOption === "ALL" ? copy.filterTypes : typeOption}
                    </Button>
                  ))}
                </div>
                </RegistryToolbarGroup>
                <RegistryToolbarGroup label={copy.stateGroupLabel}>
                <div className="flex flex-wrap items-center gap-2">
                  {[
                    {
                      key: "ALL" as const,
                      label: copy.filterStates,
                    },
                    {
                      key: "ACTIVE" as const,
                      label: locale === "tr" ? "Aktif" : "Active",
                    },
                    {
                      key: "INACTIVE" as const,
                      label: locale === "tr" ? "Pasif" : "Inactive",
                    },
                  ].map((stateOption) => (
                    <Button
                      key={stateOption.key}
                      type="button"
                      variant={
                        selectedState === stateOption.key
                          ? "secondary"
                          : "outline"
                      }
                      size="sm"
                      aria-pressed={selectedState === stateOption.key}
                      onClick={() => setSelectedState(stateOption.key)}
                    >
                      {stateOption.label}
                    </Button>
                  ))}
                </div>
                </RegistryToolbarGroup>
              </>
            }
            summaryTitle={filterSummaryTitle}
            summaryDescription={filterSummaryDescription}
          />
        }
      >
        <ContentListTable
          contents={filteredContents}
          isLoading={contentListQuery.isLoading}
          onContentSelect={(content) =>
            navigate(`/contents/${content.summary.id}`)
          }
          onRetry={() => void contentListQuery.refetch()}
          problem={contentListQuery.problem}
        />
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{copy.createDialogTitle}</DialogTitle>
            <DialogDescription>
              {copy.createDialogDescription}
            </DialogDescription>
          </DialogHeader>

          <DialogBody>
            <ContentForm
              initialValues={getCreateContentFormDefaults()}
              mode="create"
              onCancel={() => setIsCreateDialogOpen(false)}
              onSuccess={(savedContent) => {
                setIsCreateDialogOpen(false);
                navigate(`/contents/${savedContent.contentId}`);
              }}
            />
          </DialogBody>
        </DialogContent>
      </Dialog>
    </>
  );
}
