import { RotateCcw } from "lucide-react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type { AssetProcessingJobViewModel } from "@/features/assets/model/asset-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type ProcessingJobTableProps = {
  jobs: AssetProcessingJobViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onRetryJob?: (job: AssetProcessingJobViewModel) => void;
  onJobSelect?: (job: AssetProcessingJobViewModel) => void;
};

function getStatusClassName(status: AssetProcessingJobViewModel["status"]) {
  switch (status) {
    case "COMPLETED":
      return "border-emerald-200 bg-emerald-50 text-emerald-800";
    case "FAILED":
      return "border-rose-200 bg-rose-50 text-rose-800";
    case "PROCESSING":
      return "border-sky-200 bg-sky-50 text-sky-800";
    case "PENDING":
    default:
      return "border-amber-200 bg-amber-50 text-amber-800";
  }
}

function createColumns(
  locale: "en" | "tr",
  formatDateTime: ReturnType<typeof useI18n>["formatDateTime"],
  onRetryJob?: (job: AssetProcessingJobViewModel) => void,
): DataTableColumn<AssetProcessingJobViewModel>[] {
  return [
    {
      id: "content",
      header: locale === "tr" ? "Icerik" : "Content",
      cell: (job) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">#{job.contentId}</p>
          <p className="text-xs text-muted-foreground">{job.externalKey}</p>
        </div>
      ),
    },
    {
      id: "language",
      header: locale === "tr" ? "Dil" : "Language",
      cell: (job) => (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {job.languageLabel}
          </p>
          <p className="text-xs uppercase text-muted-foreground">
            {job.languageCode}
          </p>
        </div>
      ),
    },
    {
      id: "status",
      header: locale === "tr" ? "Durum" : "Status",
      cell: (job) => (
        <span
          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${getStatusClassName(
            job.status,
          )}`}
        >
          {job.statusLabel}
        </span>
      ),
    },
    {
      id: "type",
      header: locale === "tr" ? "Tur" : "Type",
      cell: (job) => (
        <span className="inline-flex rounded-full border border-border/70 bg-muted/30 px-2.5 py-1 text-xs font-medium text-foreground">
          {job.contentTypeLabel}
        </span>
      ),
    },
    {
      id: "attempts",
      header: locale === "tr" ? "Deneme" : "Attempts",
      align: "center",
      cell: (job) => (
        <div className="space-y-1 text-center">
          <p className="text-sm font-medium text-foreground">
            {job.attemptCount}
          </p>
          <p className="text-xs text-muted-foreground">
            {job.hasFailure
              ? locale === "tr"
                ? "sonuc: basarisiz"
                : "last result: failed"
              : job.isRunning
                ? locale === "tr"
                  ? "aktif islem"
                  : "active run"
                : locale === "tr"
                  ? "bekliyor"
                  : "queued"}
          </p>
        </div>
      ),
    },
    {
      id: "updatedAt",
      header: locale === "tr" ? "Guncellendi" : "Updated",
      cell: (job) => (
        <div className="space-y-1">
          <p className="text-sm text-foreground">
            {formatDateTime(job.updatedAt, {
              year: "numeric",
              month: "short",
              day: "2-digit",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
          <p className="text-xs text-muted-foreground">
            {job.completedAt
              ? locale === "tr"
                ? "tamamlandi"
                : "completed"
              : job.failedAt
                ? locale === "tr"
                  ? "hata kaydi var"
                  : "failure recorded"
                : locale === "tr"
                  ? "son heartbeat"
                  : "latest heartbeat"}
          </p>
        </div>
      ),
    },
    {
      id: "error",
      header: locale === "tr" ? "Hata" : "Error",
      cell: (job) => (
        <div className="space-y-1">
          <p className="text-sm text-foreground">
            {job.lastErrorCode ??
              (locale === "tr" ? "Yok" : "None recorded")}
          </p>
          <p className="line-clamp-2 text-xs text-muted-foreground">
            {job.lastErrorMessage ??
              (locale === "tr"
                ? "Son islem icin hata mesaji kaydedilmedi."
                : "No error message was stored for the latest attempt.")}
          </p>
        </div>
      ),
    },
    {
      id: "actions",
      header: locale === "tr" ? "Aksiyon" : "Action",
      align: "right",
      cell: (job) => (
        <Button
          type="button"
          variant={job.canRetry ? "outline" : "ghost"}
          size="sm"
          disabled={!job.canRetry}
          title={
            job.canRetry
              ? locale === "tr"
                ? "Islemi yeniden dene"
                : "Retry this processing job"
              : locale === "tr"
                ? "Yeniden deneme yalnizca FAILED kayitlari icin aciktir."
                : "Retry is available only for FAILED jobs."
          }
          onClick={(event) => {
            event.stopPropagation();
            if (job.canRetry) {
              onRetryJob?.(job);
            }
          }}
        >
          <RotateCcw className="size-4" />
          {locale === "tr" ? "Retry" : "Retry"}
        </Button>
      ),
    },
  ];
}

export function ProcessingJobTable({
  jobs,
  isLoading = false,
  problem = null,
  onRetry,
  onRetryJob,
  onJobSelect,
}: ProcessingJobTableProps) {
  const { locale, formatDateTime } = useI18n();
  const columns = createColumns(locale, formatDateTime, onRetryJob);

  return (
    <DataTable
      caption={
        locale === "tr"
          ? "Son medya isleme kayitlari"
          : "Recent media processing jobs"
      }
      columns={columns}
      emptyDescription={
        locale === "tr"
          ? "Henuz isleme kaydi yok. Yeni bir localization schedule ederek ilk isi baslatin."
          : "No processing jobs exist yet. Schedule a localization to create the first job."
      }
      emptyTitle={
        locale === "tr" ? "Isleme kaydi yok" : "No processing jobs"
      }
      getRowId={(job) => job.id.toString()}
      isLoading={isLoading}
      loadingDescription={
        locale === "tr"
          ? "CMS son isleme kayitlarini admin API uzerinden istiyor."
          : "The CMS is requesting recent processing jobs from the admin API."
      }
      loadingTitle={
        locale === "tr"
          ? "Isleme kayitlari yukleniyor"
          : "Loading processing jobs"
      }
      onRetry={onRetry}
      onRowClick={onJobSelect}
      problem={jobs.length > 0 ? problem : problem}
      rowClassName={(job) =>
        job.hasFailure ? "bg-rose-50/40 hover:bg-rose-50/70" : undefined
      }
      rows={jobs}
    />
  );
}
