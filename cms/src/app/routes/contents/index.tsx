import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
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
  const { locale, formatNumber } = useI18n();
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const contentListQuery = useContentList();
  const contentCount = contentListQuery.contents.length;
  const activeCount = contentListQuery.contents.filter(
    (content) => content.summary.active,
  ).length;
  const inactiveCount = contentCount - activeCount;
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Editoryal Çekirdek",
          title: "İçerik Stüdyosu",
          description:
            "İçerikleri format, yaşam döngüsü durumu, yerelleştirme kapsamı ve hikâye sayfa sayısına göre inceleyin. Oluşturma akışı yeni kaydı açar ve sizi doğrudan detay çalışma alanına götürür.",
          refresh: "Yenile",
          create: "İçerik oluştur",
          searchLabel: "İçerik kayıtlarını ara",
          searchPlaceholder:
            "External key veya yerelleştirilmiş başlığa göre ara",
          filterTypes: "Tüm içerik türleri",
          filterStates: "Aktif ve arşivlenmiş",
          summaryDescription:
            "Kayıt listesi canlı backend verisini yansıtır ve her içerik çalışma alanına doğrudan geçiş sağlar.",
          registrySummary: "Kayıt özeti",
          registrySummaryDescription:
            "Yerelleştirme, katkıda bulunan ve hikâye sayfası akışlarını açmadan önce editoryal kataloğu tarayın.",
          loadingRegistry: "İçerik kaydı backend'den yükleniyor.",
          loadedRegistry: `${formatNumber(activeCount)} aktif ve ${formatNumber(inactiveCount)} pasif kayıt mevcut.`,
          infoOne:
            "Satır navigasyonu her içerik kaydının canlı metadata editörünü ve yerelleştirme çalışma alanını açar.",
          infoTwo:
            "Oluşturma sonrası kayıt listesi önbelleği yenilenir ve yeni detay rotasına yönlendirilirsiniz.",
          createDialogTitle: "İçerik oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir editoryal kayıt oluşturun. Kaydetme sonrası kayıt listesi ve detay önbelleği yenilenir, CMS yeni detay rotasını açar.",
          countLoaded: `${formatNumber(contentCount)} içerik kaydı yüklendi`,
        }
      : {
          eyebrow: "Editorial Core",
          title: "Content Studio",
          description:
            "Browse content by format, lifecycle state, localization coverage, and story-page count. Create opens a new record and sends you directly into its detail workspace.",
          refresh: "Refresh",
          create: "Create content",
          searchLabel: "Search content registry",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All content types",
          filterStates: "Active and archived",
          summaryDescription:
            "The registry reflects live backend data and supports direct navigation into each content workspace.",
          registrySummary: "Registry Summary",
          registrySummaryDescription:
            "Use this screen to scan the editorial catalog before opening localization, contributor, and story-page workflows.",
          loadingRegistry:
            "The content registry is hydrating from the backend.",
          loadedRegistry: `${formatNumber(activeCount)} active and ${formatNumber(inactiveCount)} inactive records are available in the current environment.`,
          infoOne:
            "Row navigation opens the live metadata editor and localization workspace for each content record.",
          infoTwo:
            "Create submits immediately invalidate the list cache and redirect into the new detail route.",
          createDialogTitle: "Create content",
          createDialogDescription:
            "Create a new editorial record with base metadata. After save, the registry and detail caches refresh and the CMS opens the new detail route.",
          countLoaded: `${formatNumber(contentCount)} content record${contentCount === 1 ? "" : "s"} loaded`,
        };

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
          <FilterBar>
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label={copy.searchLabel}
                  className="pl-8"
                  disabled
                  placeholder={copy.searchPlaceholder}
                  value=""
                />
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterTypes}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.filterStates}
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description={copy.summaryDescription}
                title={copy.countLoaded}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>{copy.registrySummary}</CardTitle>
                <CardDescription>
                  {copy.registrySummaryDescription}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {contentListQuery.isLoading
                    ? copy.loadingRegistry
                    : copy.loadedRegistry}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.infoOne}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.infoTwo}
                </div>
              </CardContent>
            </Card>
          </>
        }
      >
        <ContentListTable
          contents={contentListQuery.contents}
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

          <ContentForm
            initialValues={getCreateContentFormDefaults()}
            mode="create"
            onCancel={() => setIsCreateDialogOpen(false)}
            onSuccess={(savedContent) => {
              setIsCreateDialogOpen(false);
              navigate(`/contents/${savedContent.contentId}`);
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
