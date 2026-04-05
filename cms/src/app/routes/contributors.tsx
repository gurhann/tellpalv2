import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";

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
import { Input } from "@/components/ui/input";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContributorFormDialog } from "@/features/contributors/components/contributor-form-dialog";
import { ContributorTable } from "@/features/contributors/components/contributor-table";
import { MissingActionsNote } from "@/features/contributors/components/missing-actions-note";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributors } from "@/features/contributors/queries/use-contributors";
import { useI18n } from "@/i18n/locale-provider";

const RECENT_CONTRIBUTOR_LIMIT = 12;

export function ContributorsRoute() {
  const { locale } = useI18n();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [selectedContributor, setSelectedContributor] =
    useState<ContributorViewModel | null>(null);
  const contributorQuery = useContributors(RECENT_CONTRIBUTOR_LIMIT);
  const contributorCountLabel =
    locale === "tr"
      ? `${contributorQuery.contributors.length} katkı sağlayan yüklendi`
      : `${contributorQuery.contributors.length} contributor${
          contributorQuery.contributors.length === 1 ? "" : "s"
        } loaded`;
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Katkı Sağlayan Kaydı",
          title: "Katkı Sağlayanlar",
          description:
            "Katkı sağlayan kaydı artık en güncel backend kayıtlarını yüklüyor. Oluşturma ve yeniden adlandırma canlı; silme ise backend desteği gelene kadar özellikle kapalı.",
          refresh: "Yenile",
          create: "Katkı sağlayan oluştur",
          filtersAria: "Katkı sağlayan filtreleri",
          searchLabel: "Katkı sağlayan ara",
          searchPlaceholder: "Görünen ada göre ara",
          latest: `Son ${RECENT_CONTRIBUTOR_LIMIT}`,
          registryLive: "Kayıt + yeniden adlandırma canlı",
          summary:
            "Son liste canlı. Oluşturma ve yeniden adlandırma artık admin API'ye gider ve ortak kaydı yeniler.",
          notesTitle: "Kayıt Notları",
          notesDescription:
            "Bu ekran `GET /api/admin/contributors` üzerinden son kayıt limitiyle beslenir.",
          loading: "Katkı sağlayan kaydı backend üzerinden yükleniyor.",
          latestShown: `Hızlı editoryal erişim için son ${contributorQuery.limit} katkı sağlayan kaydı gösteriliyor.`,
          createRename:
            "Oluştur yeni bir kayıt dialog'u açar; yeniden adlandırma her satırdan mevcut görünen adı günceller.",
          contentAssignments:
            "İçerik bazlı atama içerik detay ekranından girer ve silme, backend delete endpoint'i gelene kadar kullanılamaz.",
          missingActionLabel: "Katkı sağlayanı sil",
          missingActionDescription:
            "Admin API tarafında hâlâ contributor delete endpoint'i yok. Bu yüzden kayıt satırlarında yalnızca oluşturma ve yeniden adlandırma gösterilir.",
        }
      : {
          eyebrow: "Contributor Registry",
          title: "Contributors",
          description:
            "The contributor registry now loads the latest backend records. Create and rename flows are live, while delete stays explicitly blocked until backend support lands.",
          refresh: "Refresh",
          create: "Create contributor",
          filtersAria: "Contributor filters",
          searchLabel: "Search contributors",
          searchPlaceholder: "Search by display name",
          latest: `Latest ${RECENT_CONTRIBUTOR_LIMIT}`,
          registryLive: "Registry + rename live",
          summary:
            "The recent list is live. Create and rename now post back to the admin API and refresh the shared registry.",
          notesTitle: "Registry Notes",
          notesDescription:
            "This shell is backed by `GET /api/admin/contributors` with a recent-list limit.",
          loading: "The contributor registry is hydrating from the backend.",
          latestShown: `The latest ${contributorQuery.limit} contributor records are shown here for quick editorial access.`,
          createRename:
            "Create opens a clean registry dialog; rename updates an existing display name in-place from each row.",
          contentAssignments:
            "Content-level assignment enters through the content detail route and delete remains unavailable until the backend exposes a delete endpoint.",
          missingActionLabel: "Delete contributor",
          missingActionDescription:
            "The admin API still has no contributor delete endpoint. Registry rows intentionally expose create and rename only.",
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
              onClick={() => void contributorQuery.refetch()}
            >
              <RefreshCw
                className={`size-4 ${
                  contributorQuery.isFetching ? "animate-spin" : ""
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
          <FilterBar aria-label={copy.filtersAria}>
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
                {copy.latest}
              </div>
              <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
                {copy.registryLive}
              </div>
            </FilterBarGroup>

            <FilterBarActions>
              <FilterBarSummary
                description={copy.summary}
                title={contributorCountLabel}
              />
            </FilterBarActions>
          </FilterBar>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>{copy.notesTitle}</CardTitle>
                <CardDescription>{copy.notesDescription}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {contributorQuery.isLoading ? copy.loading : copy.latestShown}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.createRename}
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  {copy.contentAssignments}
                </div>
                <MissingActionsNote
                  actionLabel={copy.missingActionLabel}
                  description={copy.missingActionDescription}
                />
              </CardContent>
            </Card>
          </>
        }
      >
        <ContributorTable
          contributors={contributorQuery.contributors}
          isLoading={contributorQuery.isLoading}
          onRenameContributor={setSelectedContributor}
          onRetry={() => void contributorQuery.refetch()}
          problem={contributorQuery.problem}
        />
      </ContentPageShell>

      {isCreateDialogOpen ? (
        <ContributorFormDialog
          mode="create"
          open={isCreateDialogOpen}
          onOpenChange={setIsCreateDialogOpen}
        />
      ) : null}

      {selectedContributor ? (
        <ContributorFormDialog
          contributor={selectedContributor}
          mode="rename"
          open
          onOpenChange={(open) => {
            if (!open) {
              setSelectedContributor(null);
            }
          }}
        />
      ) : null}
    </>
  );
}
