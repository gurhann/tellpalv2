import { CirclePlus, RefreshCw, RotateCcw, Search } from "lucide-react";
import { useDeferredValue, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import {
  RegistryToolbar,
  RegistryToolbarGroup,
} from "@/components/data/registry-toolbar";
import { Pagination } from "@/components/data/pagination";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentListTable } from "@/features/contents/components/content-list-table";
import {
  contentRegistryReadinessSchema,
  contentTypeSchema,
  type ContentRegistryReadiness,
  type ContentType,
} from "@/features/contents/api/content-admin";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import {
  useContentRegistry,
  useContentRegistryCounts,
} from "@/features/contents/queries/use-content-registry";
import { getCreateContentFormDefaults } from "@/features/contents/schema/content-schema";
import { useI18n } from "@/i18n/locale-provider";
import {
  resolveLanguageLabel,
  supportedCmsLanguageOptions,
} from "@/lib/languages";

const contentTypes: ContentType[] = ["STORY", "MEDITATION", "LULLABY"];
const readinessFilters: Array<ContentRegistryReadiness | "ALL"> = [
  "ALL",
  "ACTION_REQUIRED",
  "READY_TO_PUBLISH",
  "PUBLISHED",
];
const PAGE_SIZE = 25;

function getTypeLabel(type: ContentType, uiLocale: "tr" | "en") {
  const labels: Record<ContentType, { tr: string; en: string }> = {
    STORY: { tr: "Hikâyeler", en: "Stories" },
    MEDITATION: { tr: "Meditasyonlar", en: "Meditations" },
    LULLABY: { tr: "Ninniler", en: "Lullabies" },
  };

  return labels[type][uiLocale];
}

function getReadinessLabel(
  readiness: ContentRegistryReadiness | "ALL",
  uiLocale: "tr" | "en",
) {
  const labels: Record<
    ContentRegistryReadiness | "ALL",
    { tr: string; en: string }
  > = {
    ALL: { tr: "Tüm durumlar", en: "All statuses" },
    ACTION_REQUIRED: { tr: "Aksiyon gerekli", en: "Action required" },
    READY_TO_PUBLISH: { tr: "Yayına hazır", en: "Ready to publish" },
    PUBLISHED: { tr: "Yayında", en: "Published" },
  };

  return labels[readiness][uiLocale];
}

function parseLanguage(value: string | null) {
  return supportedCmsLanguageOptions.some((option) => option.code === value)
    ? value!
    : "tr";
}

function parsePage(value: string | null) {
  const page = Number(value);
  return Number.isSafeInteger(page) && page >= 0 ? page : 0;
}

export function ContentsIndexRoute() {
  const { locale } = useI18n();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const language = parseLanguage(searchParams.get("language"));
  const requestedType = contentTypeSchema.safeParse(searchParams.get("type"));
  const activeType: ContentType = requestedType.success
    ? requestedType.data
    : "STORY";
  const requestedReadiness = contentRegistryReadinessSchema.safeParse(
    searchParams.get("readiness"),
  );
  const readiness: ContentRegistryReadiness | "ALL" = requestedReadiness.success
    ? requestedReadiness.data
    : "ALL";
  const querySearch = searchParams.get("q") ?? "";
  const [search, setSearch] = useState(querySearch);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const deferredSearch = useDeferredValue(search);
  const page = parsePage(searchParams.get("page"));

  useEffect(() => {
    // The URL is the source of truth when navigation changes the query.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSearch(querySearch);
  }, [querySearch]);

  const registry = useContentRegistry({
    language,
    type: activeType,
    readiness: readiness === "ALL" ? undefined : readiness,
    q: deferredSearch,
    page,
    size: PAGE_SIZE,
  });
  const typeCounts = useContentRegistryCounts({
    language,
    readiness: readiness === "ALL" ? undefined : readiness,
    q: deferredSearch,
  });

  function update(changes: Record<string, string | null>) {
    const next = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => {
      if (value === null || value === "") {
        next.delete(key);
      } else {
        next.set(key, value);
      }
    });
    if (!("page" in changes)) {
      next.delete("page");
    }
    setSearchParams(next);
  }

  const hasActiveFilters =
    search.trim().length > 0 || language !== "tr" || readiness !== "ALL";
  const resetFilters = () => {
    setSearch("");
    update({ language: "tr", readiness: null, q: null });
  };

  const totalItems = registry.registry?.totalItems ?? 0;
  const summaryTitle =
    locale === "tr"
      ? `${getTypeLabel(activeType, locale)} · ${totalItems} içerik · ${language.toUpperCase()}`
      : `${getTypeLabel(activeType, locale)} · ${totalItems} records · ${language.toUpperCase()}`;
  const summaryDescription =
    locale === "tr"
      ? "Son güncellenen içerikler önce gelir."
      : "Most recently edited content appears first.";

  return (
    <>
      <ContentPageShell
        eyebrow={locale === "tr" ? "İçerik operasyonu" : "Content operations"}
        title={locale === "tr" ? "İçerikler" : "Contents"}
        description={
          locale === "tr"
            ? "İçeriği bulun, seçili dilde yayın durumunu görün ve düzenleyiciye geçin."
            : "Find content, read its selected-locale readiness, and move into the editor."
        }
        actions={
          <>
            <Button
              variant="outline"
              type="button"
              onClick={() => void registry.refetch()}
            >
              <RefreshCw
                className={
                  registry.isFetching ? "size-4 animate-spin" : "size-4"
                }
              />
              {locale === "tr" ? "Yenile" : "Refresh"}
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {locale === "tr" ? "İçerik oluştur" : "Create content"}
            </Button>
          </>
        }
        toolbar={
          <div className="space-y-4">
            <div className="border-b border-border/60 pb-1">
              <Tabs
                value={activeType}
                onValueChange={(value) => {
                  if (contentTypeSchema.safeParse(value).success) {
                    update({ type: value });
                  }
                }}
              >
                <TabsList
                  aria-label={locale === "tr" ? "İçerik türü" : "Content type"}
                  className="w-full flex-wrap justify-start gap-x-1 gap-y-1 sm:w-auto"
                  variant="line"
                >
                  {contentTypes.map((type) => (
                    <TabsTrigger
                      key={type}
                      value={type}
                      className="flex-none gap-2 px-3 py-2"
                    >
                      <span>{getTypeLabel(type, locale)}</span>
                      <span
                        aria-hidden="true"
                        className="rounded-full bg-muted px-1.5 py-0.5 text-[0.7rem] font-semibold text-muted-foreground"
                      >
                        {typeCounts[type] ?? "—"}
                      </span>
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>
            </div>

            <RegistryToolbar
              ariaLabel={
                locale === "tr" ? "İçerik filtreleri" : "Content filters"
              }
              search={
                <RegistryToolbarGroup
                  className="w-full"
                  label={
                    locale === "tr" ? "İçeriklerde ara" : "Search contents"
                  }
                >
                  <div className="relative min-w-[16rem] flex-1">
                    <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                    <Input
                      aria-label={
                        locale === "tr" ? "İçeriklerde ara" : "Search contents"
                      }
                      className="pl-8"
                      placeholder={
                        locale === "tr"
                          ? "Başlık, anahtar veya ID ile ara"
                          : "Search by title, key, or ID"
                      }
                      value={search}
                      onChange={(event) => {
                        const value = event.target.value;
                        setSearch(value);
                        update({ q: value.trim() || null });
                      }}
                    />
                  </div>
                </RegistryToolbarGroup>
              }
              filters={
                <>
                  <RegistryToolbarGroup
                    label={locale === "tr" ? "Dil" : "Language"}
                  >
                    <Select
                      value={language}
                      onValueChange={(value) => update({ language: value })}
                    >
                      <SelectTrigger
                        aria-label={locale === "tr" ? "Dil" : "Language"}
                        className="w-[10rem]"
                      >
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {supportedCmsLanguageOptions.map((option) => (
                          <SelectItem key={option.code} value={option.code}>
                            {resolveLanguageLabel(option.code, locale)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </RegistryToolbarGroup>
                  <RegistryToolbarGroup
                    label={locale === "tr" ? "Yayın durumu" : "Readiness"}
                  >
                    <Select
                      value={readiness}
                      onValueChange={(value) => {
                        if (
                          value === "ALL" ||
                          contentRegistryReadinessSchema.safeParse(value)
                            .success
                        ) {
                          update({ readiness: value === "ALL" ? null : value });
                        }
                      }}
                    >
                      <SelectTrigger
                        aria-label={
                          locale === "tr" ? "Yayın durumu" : "Readiness"
                        }
                        className="w-[12rem]"
                      >
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {readinessFilters.map((value) => (
                          <SelectItem key={value} value={value}>
                            {getReadinessLabel(value, locale)}
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
                      {locale === "tr" ? "Filtreleri temizle" : "Clear filters"}
                    </Button>
                  ) : null}
                </>
              }
              summaryTitle={summaryTitle}
              summaryDescription={summaryDescription}
            />
          </div>
        }
      >
        <ContentListTable
          key={`${activeType}-${language}-${readiness}-${deferredSearch}`}
          activeType={activeType}
          items={registry.registry?.items ?? []}
          isLoading={registry.isLoading}
          problem={registry.problem}
          onRetry={() => void registry.refetch()}
          onResetFilters={hasActiveFilters ? resetFilters : undefined}
          onContentSelect={(item) => {
            const next = new URLSearchParams(searchParams);
            next.set("type", activeType);
            next.set("language", language);
            navigate(`/contents/${item.contentId}?${next.toString()}`);
          }}
        />
        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          totalItems={registry.registry?.totalItems ?? 0}
          isLoading={registry.isFetching}
          onPageChange={(nextPage) => update({ page: `${nextPage}` })}
        />
      </ContentPageShell>
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {locale === "tr" ? "İçerik oluştur" : "Create content"}
            </DialogTitle>
            <DialogDescription>
              {locale === "tr"
                ? "Yeni editoryal kaydı oluşturun."
                : "Create a new editorial record."}
            </DialogDescription>
          </DialogHeader>
          <DialogBody>
            <ContentForm
              initialValues={getCreateContentFormDefaults()}
              mode="create"
              onCancel={() => setIsCreateDialogOpen(false)}
              onSuccess={(content) => {
                setIsCreateDialogOpen(false);
                navigate(`/contents/${content.contentId}?language=${language}`);
              }}
            />
          </DialogBody>
        </DialogContent>
      </Dialog>
    </>
  );
}
