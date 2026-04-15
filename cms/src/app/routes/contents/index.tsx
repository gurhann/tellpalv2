import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { FilterBar, FilterBarGroup } from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
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
  const { locale } = useI18n();
  const navigate = useNavigate();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const contentListQuery = useContentList();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Editoryal Çekirdek",
          title: "İçerik Stüdyosu",
          description:
            "İçerik kayıtlarını açın, oluşturun ve doğrudan detay çalışma alanına geçin.",
          refresh: "Yenile",
          create: "İçerik oluştur",
          searchLabel: "İçerik kayıtlarını ara",
          searchPlaceholder:
            "External key veya yerelleştirilmiş başlığa göre ara",
          filterTypes: "Tüm içerik türleri",
          filterStates: "Aktif ve arşivlenmiş",
          createDialogTitle: "İçerik oluştur",
          createDialogDescription:
            "Temel metadata ile yeni bir editoryal kayıt oluşturun. Kaydetme sonrası kayıt listesi ve detay önbelleği yenilenir, CMS yeni detay rotasını açar.",
        }
      : {
          eyebrow: "Editorial Core",
          title: "Content Studio",
          description:
            "Open, create, and move directly into each content detail workspace.",
          refresh: "Refresh",
          create: "Create content",
          searchLabel: "Search content registry",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All content types",
          filterStates: "Active and archived",
          createDialogTitle: "Create content",
          createDialogDescription:
            "Create a new editorial record with base metadata. After save, the registry and detail caches refresh and the CMS opens the new detail route.",
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
          </FilterBar>
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
