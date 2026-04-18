import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";

import {
  FilterBar,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
} from "@/components/workspace/workspace-primitives";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContributorDeleteDialog } from "@/features/contributors/components/contributor-delete-dialog";
import { ContributorFormDialog } from "@/features/contributors/components/contributor-form-dialog";
import { ContributorTable } from "@/features/contributors/components/contributor-table";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { useContributors } from "@/features/contributors/queries/use-contributors";
import { useI18n } from "@/i18n/locale-provider";

const RECENT_CONTRIBUTOR_LIMIT = 12;

export function ContributorsRoute() {
  const { locale } = useI18n();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [selectedContributor, setSelectedContributor] =
    useState<ContributorViewModel | null>(null);
  const [deletingContributor, setDeletingContributor] =
    useState<ContributorViewModel | null>(null);
  const contributorQuery = useContributors(RECENT_CONTRIBUTOR_LIMIT);
  const contributorActions = useContributorActions();
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Katki Saglayan Kaydi",
          title: "Katki Saglayanlar",
          description:
            "Paylasilan katki saglayan kayitlarini arayin, olusturun, yeniden adlandirin ve silin.",
          refresh: "Yenile",
          create: "Katki saglayan olustur",
          filtersAria: "Katki saglayan filtreleri",
          searchLabel: "Katki saglayan ara",
          searchPlaceholder: "Gorunen ada gore ara",
          latest: `Son ${RECENT_CONTRIBUTOR_LIMIT} kayit`,
          railTitle: "Registry posture",
          railDescription:
            "Shared contributor records stay light in the registry so assignment work can happen from content detail.",
        }
      : {
          eyebrow: "Contributor Registry",
          title: "Contributors",
          description:
            "Search, create, rename, and delete shared contributor records.",
          refresh: "Refresh",
          create: "Create contributor",
          filtersAria: "Contributor filters",
          searchLabel: "Search contributors",
          searchPlaceholder: "Search by display name",
          latest: `Latest ${RECENT_CONTRIBUTOR_LIMIT} records`,
          railTitle: "Registry posture",
          railDescription:
            "Shared contributor records stay light in the registry so assignment work can happen from content detail.",
        };

  const filteredContributors = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return contributorQuery.contributors.filter((contributor) => {
      if (normalizedSearch.length === 0) {
        return true;
      }

      return contributor.displayName.toLowerCase().includes(normalizedSearch);
    });
  }, [contributorQuery.contributors, deferredSearch]);

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
                label: locale === "tr" ? "Toplam" : "Total",
                value: `${filteredContributors.length}`,
                tone: filteredContributors.length > 0 ? "success" : "default",
              },
              {
                label: locale === "tr" ? "Arama sonucu" : "Search result",
                value:
                  locale === "tr"
                    ? `${filteredContributors.length} kayit`
                    : `${filteredContributors.length} records`,
              },
            ]}
          >
            <WorkspaceInfoCard
              title={locale === "tr" ? "Kredi akisi" : "Credit workflow"}
              description={
                locale === "tr"
                  ? "Registry create, rename ve delete icin kalir; atama ve unassign akisi ise content detail uzerinden devam eder."
                  : "The registry stays focused on create, rename, and delete while assignment and unassign continue from content detail."
              }
              className="bg-background/80"
            >
              <WorkspaceKeyValueGrid
                items={[
                  {
                    label: locale === "tr" ? "Sonraki adim" : "Next step",
                    value:
                      locale === "tr"
                        ? "Content detail assignment"
                        : "Content detail assignment",
                    tone: "accent",
                  },
                ]}
              />
            </WorkspaceInfoCard>
          </TaskRail>
        }
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
                  placeholder={copy.searchPlaceholder}
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
            </FilterBarGroup>
            <FilterBarSummary
              title={
                locale === "tr"
                  ? `${filteredContributors.length} / ${contributorQuery.contributors.length} kayit`
                  : `${filteredContributors.length} / ${contributorQuery.contributors.length} records`
              }
              description={copy.latest}
            />
          </FilterBar>
        }
      >
        <ContributorTable
          contributors={filteredContributors}
          isLoading={contributorQuery.isLoading}
          isMutationPending={contributorActions.isPending}
          onDeleteContributor={setDeletingContributor}
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

      <ContributorDeleteDialog
        contributor={deletingContributor}
        open={deletingContributor !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeletingContributor(null);
          }
        }}
      />
    </>
  );
}
