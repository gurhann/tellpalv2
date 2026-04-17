import { CirclePlus, RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { FreeAccessTable } from "@/features/free-access/components/free-access-table";
import { GrantFreeAccessDialog } from "@/features/free-access/components/grant-free-access-dialog";
import { useFreeAccessList } from "@/features/free-access/queries/use-free-access-list";
import { useI18n } from "@/i18n/locale-provider";

export function FreeAccessRoute() {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Free access",
          title: "Access Key Grants",
          description:
            "Access key bazli grant kayitlarini filtreleyin, yeni grant verin ve exact kayitlari revoke edin.",
          refresh: "Yenile",
          grant: "Grant free access",
          filterLabel: "Access key filtresi",
          filterPlaceholder: "default veya ozel bir access key yazin",
          apply: "Filtreyi uygula",
          clear: "Temizle",
          defaultScopeDescription:
            "Bos filtre varsayilan seti gosterir ve API'ye bos accessKey gondermez.",
          explicitScopeDescription:
            "Bilinmeyen bir key icin bos tablo gosterilir. Arayuz varsayilan sete geri donmez.",
          railTitle: "Grant semantics",
          railDescription:
            "Liste filtresi ve grant formu farkli davranir: liste bosken default set gosterilir, form ise explicit access key gonderir.",
        }
      : {
          eyebrow: "Free Access",
          title: "Access Key Grants",
          description:
            "Filter grants by access key, create new grants, and revoke exact records without leaving the table.",
          refresh: "Refresh",
          grant: "Grant free access",
          filterLabel: "Access key filter",
          filterPlaceholder: "Enter default or any custom access key",
          apply: "Apply filter",
          clear: "Clear",
          defaultScopeDescription:
            "A blank filter shows the default set and does not send an empty accessKey parameter to the API.",
          explicitScopeDescription:
            "Unknown keys render an empty table. The UI intentionally does not fall back to the default set.",
          railTitle: "Grant semantics",
          railDescription:
            "List filters and grant creation behave differently: a blank list filter shows the default set, while the grant form always submits an explicit access key.",
        };
  const [filterInput, setFilterInput] = useState("");
  const [appliedFilter, setAppliedFilter] = useState("");
  const [isGrantOpen, setIsGrantOpen] = useState(false);
  const freeAccessQuery = useFreeAccessList(appliedFilter);
  const contentListQuery = useContentList();
  const contentLookup = useMemo(
    () =>
      new Map(
        contentListQuery.contents.map((content) => [content.summary.id, content]),
      ),
    [contentListQuery.contents],
  );
  const distinctContentCount = useMemo(
    () => new Set(freeAccessQuery.entries.map((entry) => entry.contentId)).size,
    [freeAccessQuery.entries],
  );

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
              onClick={() => {
                void freeAccessQuery.refetch();
                void contentListQuery.refetch();
              }}
            >
              <RefreshCw
                className={`size-4 ${
                  freeAccessQuery.isFetching || contentListQuery.isFetching
                    ? "animate-spin"
                    : ""
                }`}
              />
              {copy.refresh}
            </Button>
            <Button type="button" onClick={() => setIsGrantOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.grant}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: locale === "tr" ? "Scope" : "Scope",
                value: freeAccessQuery.isDefaultScope
                  ? "default"
                  : freeAccessQuery.effectiveAccessKey,
                tone: freeAccessQuery.isDefaultScope ? "success" : "default",
              },
              {
                label: locale === "tr" ? "Grant" : "Grants",
                value: freeAccessQuery.entries.length.toString(),
                tone: "default",
              },
              {
                label: locale === "tr" ? "Content" : "Contents",
                value: distinctContentCount.toString(),
                tone: "default",
              },
            ]}
          >
            <div className="space-y-3 text-sm leading-6 text-muted-foreground">
              <p>
                {freeAccessQuery.isDefaultScope
                  ? copy.defaultScopeDescription
                  : copy.explicitScopeDescription}
              </p>
            </div>
          </TaskRail>
        }
        toolbar={
          <FilterBar aria-label={copy.filterLabel}>
            <FilterBarGroup>
              <div className="min-w-[16rem] flex-1">
                <label className="sr-only" htmlFor="free-access-filter">
                  {copy.filterLabel}
                </label>
                <Input
                  id="free-access-filter"
                  placeholder={copy.filterPlaceholder}
                  value={filterInput}
                  onChange={(event) => setFilterInput(event.target.value)}
                />
              </div>
            </FilterBarGroup>
            <FilterBarActions>
              <Button type="button" variant="outline" onClick={() => setAppliedFilter(filterInput)}>
                {copy.apply}
              </Button>
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setFilterInput("");
                  setAppliedFilter("");
                }}
              >
                {copy.clear}
              </Button>
            </FilterBarActions>
            <FilterBarSummary
              title={
                freeAccessQuery.isDefaultScope
                  ? "default"
                  : freeAccessQuery.effectiveAccessKey
              }
              description={
                freeAccessQuery.isDefaultScope
                  ? copy.defaultScopeDescription
                  : copy.explicitScopeDescription
              }
            />
          </FilterBar>
        }
      >
        <FreeAccessTable
          contentLookup={contentLookup}
          effectiveAccessKey={freeAccessQuery.effectiveAccessKey}
          entries={freeAccessQuery.entries}
          isDefaultScope={freeAccessQuery.isDefaultScope}
          isLoading={freeAccessQuery.isLoading}
          onRetry={() => void freeAccessQuery.refetch()}
          problem={freeAccessQuery.problem}
        />
      </ContentPageShell>

      {isGrantOpen ? (
        <GrantFreeAccessDialog
          contentProblem={contentListQuery.problem}
          contents={contentListQuery.contents}
          open={isGrantOpen}
          onGranted={() => {
            void freeAccessQuery.refetch();
          }}
          onOpenChange={setIsGrantOpen}
        />
      ) : null}
    </>
  );
}
