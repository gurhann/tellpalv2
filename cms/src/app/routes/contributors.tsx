import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";

import { FilterBar, FilterBarGroup } from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
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
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Katkı Sağlayan Kaydı",
          title: "Katkı Sağlayanlar",
          description:
            "Katkı sağlayan kayıtlarını oluşturun ve yeniden adlandırın.",
          refresh: "Yenile",
          create: "Katkı sağlayan oluştur",
          filtersAria: "Katkı sağlayan filtreleri",
          searchLabel: "Katkı sağlayan ara",
          searchPlaceholder: "Görünen ada göre ara",
          latest: `Son ${RECENT_CONTRIBUTOR_LIMIT}`,
          missingActionLabel: "Katkı sağlayanı sil",
          missingActionDescription:
            "Admin API tarafında hâlâ contributor delete endpoint'i yok. Bu yüzden kayıt satırlarında yalnızca oluşturma ve yeniden adlandırma gösterilir.",
        }
      : {
          eyebrow: "Contributor Registry",
          title: "Contributors",
          description: "Create and rename shared contributor records.",
          refresh: "Refresh",
          create: "Create contributor",
          filtersAria: "Contributor filters",
          searchLabel: "Search contributors",
          searchPlaceholder: "Search by display name",
          latest: `Latest ${RECENT_CONTRIBUTOR_LIMIT}`,
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
            </FilterBarGroup>
          </FilterBar>
        }
      >
        <ContributorTable
          contributors={contributorQuery.contributors}
          isLoading={contributorQuery.isLoading}
          onRenameContributor={setSelectedContributor}
          onRetry={() => void contributorQuery.refetch()}
          problem={contributorQuery.problem}
        />
        <MissingActionsNote
          actionLabel={copy.missingActionLabel}
          description={copy.missingActionDescription}
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
