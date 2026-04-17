import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ProcessingDetailCard } from "@/features/assets/components/processing-detail-card";
import { ProcessingJobTable } from "@/features/assets/components/processing-job-table";
import { ProcessingStatusSearch } from "@/features/assets/components/processing-status-search";
import { RetryProcessingDialog } from "@/features/assets/components/retry-processing-dialog";
import { ScheduleProcessingDialog } from "@/features/assets/components/schedule-processing-dialog";
import type { AssetProcessingJobViewModel } from "@/features/assets/model/asset-view-model";
import { useProcessingStatus } from "@/features/assets/queries/use-processing-status";
import { useRecentProcessingJobs } from "@/features/assets/queries/use-recent-processing-jobs";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { useContentList } from "@/features/contents/queries/use-content-list";
import { useI18n } from "@/i18n/locale-provider";
import { TaskRail } from "@/components/workspace/task-rail";

const RECENT_PROCESSING_LIMIT = 20;

type ProcessingFilter = "ALL" | "FAILED" | "ACTIVE" | "COMPLETED";

export function MediaProcessingRoute() {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Medya isleme",
          title: "Processing Console",
          description:
            "Son package islerini izleyin, bir localization icin canli durumu sorgulayin ve schedule veya retry operasyonlarini buradan yonetin.",
          refresh: "Yenile",
          schedule: "Schedule processing",
          filtersAria: "Processing filtreleri",
          searchLabel: "Processing kayitlarini ara",
          searchPlaceholder:
            "External key, content id, dil veya hata koduna gore ara",
          summaryDescription: `Son ${RECENT_PROCESSING_LIMIT} processing kaydi`,
          lookupTitle: "Status lookup",
          lookupDescription:
            "Belirli bir content ve localization icin canli kaydi sorgulayin.",
          latestTitle: "Canli konsol",
          latestDescription:
            "Recent queue, retry ve lookup aksiyonlari tek yerde tutulur.",
          invalidLookup:
            "Lookup icin gecerli bir content id ve dil secin.",
          filterAll: "Tum durumlar",
          filterFailed: "Failed",
          filterActive: "Active",
          filterCompleted: "Completed",
        }
      : {
          eyebrow: "Media Processing",
          title: "Processing Console",
          description:
            "Track recent packaging jobs, inspect one localization live, and run schedule or retry operations from the same workspace.",
          refresh: "Refresh",
          schedule: "Schedule processing",
          filtersAria: "Processing filters",
          searchLabel: "Search processing jobs",
          searchPlaceholder:
            "Search by external key, content id, language, or error code",
          summaryDescription: `Latest ${RECENT_PROCESSING_LIMIT} processing records`,
          lookupTitle: "Status lookup",
          lookupDescription:
            "Query the live record for one content and localization pair.",
          latestTitle: "Console snapshot",
          latestDescription:
            "Recent queue, retry, and lookup actions stay in one operational panel.",
          invalidLookup:
            "Enter a valid content id and language before running lookup.",
          filterAll: "All states",
          filterFailed: "Failed",
          filterActive: "Active",
          filterCompleted: "Completed",
        };
  const [isScheduleOpen, setIsScheduleOpen] = useState(false);
  const [retryJob, setRetryJob] = useState<AssetProcessingJobViewModel | null>(
    null,
  );
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<ProcessingFilter>("ALL");
  const [lookupContentId, setLookupContentId] = useState("");
  const [lookupLanguageCode, setLookupLanguageCode] = useState("");
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [lookupTarget, setLookupTarget] = useState<{
    contentId: number;
    languageCode: string;
  } | null>(null);
  const recentJobsQuery = useRecentProcessingJobs(RECENT_PROCESSING_LIMIT);
  const contentListQuery = useContentList();
  const statusQuery = useProcessingStatus({
    contentId: lookupTarget?.contentId ?? null,
    languageCode: lookupTarget?.languageCode ?? null,
    enabled: lookupTarget !== null,
  });
  const deferredSearch = useDeferredValue(search);
  const stats = useMemo(() => {
    const failedCount = recentJobsQuery.jobs.filter((job) => job.hasFailure).length;
    const activeCount = recentJobsQuery.jobs.filter(
      (job) => job.status === "PENDING" || job.status === "PROCESSING",
    ).length;
    const completedCount = recentJobsQuery.jobs.filter(
      (job) => job.status === "COMPLETED",
    ).length;

    return {
      failedCount,
      activeCount,
      completedCount,
    };
  }, [recentJobsQuery.jobs]);
  const filteredJobs = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return recentJobsQuery.jobs.filter((job) => {
      if (filter === "FAILED" && !job.hasFailure) {
        return false;
      }

      if (
        filter === "ACTIVE" &&
        job.status !== "PENDING" &&
        job.status !== "PROCESSING"
      ) {
        return false;
      }

      if (filter === "COMPLETED" && job.status !== "COMPLETED") {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        job.externalKey.toLowerCase().includes(normalizedSearch) ||
        job.contentId.toString().includes(normalizedSearch) ||
        job.languageCode.toLowerCase().includes(normalizedSearch) ||
        job.contentTypeLabel.toLowerCase().includes(normalizedSearch) ||
        (job.lastErrorCode ?? "").toLowerCase().includes(normalizedSearch)
      );
    });
  }, [deferredSearch, filter, recentJobsQuery.jobs]);

  function syncLookupWithJob(job: AssetProcessingJobViewModel) {
    setLookupContentId(job.contentId.toString());
    setLookupLanguageCode(job.languageCode);
    setLookupError(null);
    setLookupTarget({
      contentId: job.contentId,
      languageCode: job.languageCode,
    });
  }

  function handleLookupSubmit() {
    const parsedContentId = Number.parseInt(lookupContentId.trim(), 10);

    if (!Number.isFinite(parsedContentId) || lookupLanguageCode.trim().length === 0) {
      setLookupError(copy.invalidLookup);
      return;
    }

    setLookupError(null);
    setLookupTarget({
      contentId: parsedContentId,
      languageCode: lookupLanguageCode.trim().toLowerCase(),
    });
  }

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
                void recentJobsQuery.refetch();
                if (lookupTarget) {
                  void statusQuery.refetch();
                }
              }}
            >
              <RefreshCw
                className={`size-4 ${
                  recentJobsQuery.isFetching || statusQuery.isFetching
                    ? "animate-spin"
                    : ""
                }`}
              />
              {copy.refresh}
            </Button>
            <Button type="button" onClick={() => setIsScheduleOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.schedule}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.latestTitle}
            description={copy.latestDescription}
            stats={[
              {
                label: locale === "tr" ? "Failed" : "Failed",
                value: stats.failedCount.toString(),
                tone: stats.failedCount > 0 ? "warning" : "default",
              },
              {
                label: locale === "tr" ? "Active" : "Active",
                value: stats.activeCount.toString(),
                tone: stats.activeCount > 0 ? "default" : "success",
              },
              {
                label: locale === "tr" ? "Completed" : "Completed",
                value: stats.completedCount.toString(),
                tone: stats.completedCount > 0 ? "success" : "default",
              },
            ]}
          >
            <div className="space-y-4">
              <div className="space-y-1">
                <h3 className="text-sm font-semibold text-foreground">
                  {copy.lookupTitle}
                </h3>
                <p className="text-sm leading-6 text-muted-foreground">
                  {copy.lookupDescription}
                </p>
              </div>

              {lookupError ? (
                <ProblemAlert
                  description={lookupError}
                  title={locale === "tr" ? "Lookup gecersiz" : "Invalid lookup"}
                />
              ) : null}

              <ProcessingStatusSearch
                contentId={lookupContentId}
                languageCode={lookupLanguageCode}
                isPending={statusQuery.isFetching}
                onContentIdChange={setLookupContentId}
                onLanguageCodeChange={setLookupLanguageCode}
                onSubmit={handleLookupSubmit}
              />
            </div>
          </TaskRail>
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
            <FilterBarActions>
              {[
                { key: "ALL" as const, label: copy.filterAll },
                { key: "FAILED" as const, label: copy.filterFailed },
                { key: "ACTIVE" as const, label: copy.filterActive },
                { key: "COMPLETED" as const, label: copy.filterCompleted },
              ].map((option) => (
                <Button
                  key={option.key}
                  type="button"
                  variant={filter === option.key ? "secondary" : "outline"}
                  size="sm"
                  onClick={() => setFilter(option.key)}
                >
                  {option.label}
                </Button>
              ))}
            </FilterBarActions>
            <FilterBarSummary
              title={`${filteredJobs.length} / ${recentJobsQuery.jobs.length}`}
              description={copy.summaryDescription}
            />
          </FilterBar>
        }
      >
        <ProcessingJobTable
          isLoading={recentJobsQuery.isLoading}
          jobs={filteredJobs}
          onJobSelect={syncLookupWithJob}
          onRetry={() => void recentJobsQuery.refetch()}
          onRetryJob={setRetryJob}
          problem={recentJobsQuery.problem}
        />

        <ProcessingDetailCard
          hasSelection={lookupTarget !== null}
          isLoading={statusQuery.isLoading}
          isNotScheduled={statusQuery.isNotScheduled}
          job={statusQuery.job}
          onRetryJob={setRetryJob}
          problem={statusQuery.isNotScheduled ? null : statusQuery.problem}
        />
      </ContentPageShell>

      {isScheduleOpen ? (
        <ScheduleProcessingDialog
          contentProblem={contentListQuery.problem}
          contents={contentListQuery.contents}
          initialContentId={lookupTarget?.contentId ?? null}
          initialLanguageCode={lookupTarget?.languageCode ?? null}
          open={isScheduleOpen}
          onOpenChange={setIsScheduleOpen}
          onScheduled={(job) => {
            syncLookupWithJob(job);
          }}
        />
      ) : null}

      {retryJob ? (
        <RetryProcessingDialog
          job={retryJob}
          open
          onOpenChange={(open) => {
            if (!open) {
              setRetryJob(null);
            }
          }}
          onRetried={(job) => {
            syncLookupWithJob(job);
          }}
        />
      ) : null}
    </>
  );
}
