import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
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
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentListTable } from "@/features/contents/components/content-list-table";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { getCreateContentFormDefaults } from "@/features/contents/schema/content-schema";
import { useI18n } from "@/i18n/locale-provider";

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
          eyebrow: "Editoryal Çekirdek",
          title: "İçerik Stüdyosu",
          description:
            "İçerik kayıtlarını açın, filtreleyin ve doğrudan görev odaklı detay çalışma alanına geçin.",
          refresh: "Yenile",
          create: "İçerik oluştur",
          searchLabel: "İçerik kayıtlarını ara",
          searchPlaceholder:
            "External key veya yerelleştirilmiş başlığa göre ara",
          filterTypes: "Tüm türler",
          filterStates: "Tüm durumlar",
          createDialogTitle: "İçerik oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir editoryal kayıt oluşturun. Kaydetme sonrası CMS yeni detay rotasını açar.",
        }
      : {
          eyebrow: "Editorial Core",
          title: "Content Studio",
          description:
            "Open, filter, and move directly into each task-focused content workspace.",
          refresh: "Refresh",
          create: "Create content",
          searchLabel: "Search content registry",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All types",
          filterStates: "All states",
          createDialogTitle: "Create content",
          createDialogDescription:
            "Create a new editorial record with base metadata. After save, the CMS opens the new detail route.",
        };

  const typeOptions = useMemo(() => {
    const nextOptions = new Set<string>();

    contentListQuery.contents.forEach((content) => {
      nextOptions.add(content.summary.typeLabel);
    });

    return ["ALL", ...Array.from(nextOptions)];
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

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
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
          <FilterBar aria-label={copy.title}>
            <FilterBarGroup>
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
            </FilterBarGroup>
            <FilterBarActions>
              {typeOptions.map((typeOption) => (
                <Button
                  key={typeOption}
                  type="button"
                  variant={selectedType === typeOption ? "secondary" : "outline"}
                  size="sm"
                  onClick={() => setSelectedType(typeOption)}
                >
                  {typeOption === "ALL" ? copy.filterTypes : typeOption}
                </Button>
              ))}
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
                    selectedState === stateOption.key ? "secondary" : "outline"
                  }
                  size="sm"
                  onClick={() => setSelectedState(stateOption.key)}
                >
                  {stateOption.label}
                </Button>
              ))}
            </FilterBarActions>
            <FilterBarSummary
              title={
                locale === "tr"
                  ? `${filteredContents.length} / ${contentListQuery.contents.length} kayıt`
                  : `${filteredContents.length} / ${contentListQuery.contents.length} records`
              }
              description={
                locale === "tr"
                  ? "Arama ve durum filtreleri içerik registry görünümünü anında daraltır."
                  : "Search and state filters narrow the content registry immediately."
              }
            />
          </FilterBar>
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
