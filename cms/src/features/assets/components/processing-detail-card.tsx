import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";
import type { AssetProcessingJobViewModel } from "@/features/assets/model/asset-view-model";

type ProcessingDetailCardProps = {
  job: AssetProcessingJobViewModel | null;
  hasSelection: boolean;
  isLoading?: boolean;
  isNotScheduled?: boolean;
  problem?: ApiProblemDetail | null;
  onRetryJob?: (job: AssetProcessingJobViewModel) => void;
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

export function ProcessingDetailCard({
  job,
  hasSelection,
  isLoading = false,
  isNotScheduled = false,
  problem = null,
  onRetryJob,
}: ProcessingDetailCardProps) {
  const { locale, formatDateTime } = useI18n();

  return (
    <section className="rounded-[1.75rem] border border-border/70 bg-card/95 p-5 shadow-lg shadow-slate-950/5">
      <div className="space-y-1">
        <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          {locale === "tr" ? "Lookup sonucu" : "Lookup result"}
        </h2>
        <p className="text-sm leading-6 text-muted-foreground">
          {locale === "tr"
            ? "Belirli bir localization icin canli processing durumunu ve retry baglamini buradan inceleyin."
            : "Inspect the live processing state and retry context for one localization."}
        </p>
      </div>

      <div className="mt-5">
        {!hasSelection ? (
          <EmptyState
            className="min-h-56"
            description={
              locale === "tr"
                ? "Bir content id ve dil secerek lookup calistirin ya da tablodan bir kayit secin."
                : "Run a lookup with content id and language, or select a row from the table."
            }
            title={
              locale === "tr" ? "Henuz sorgu yok" : "No lookup yet"
            }
          />
        ) : isLoading ? (
          <EmptyState
            className="min-h-56"
            description={
              locale === "tr"
                ? "Secilen localization icin guncel processing kaydi isteniyor."
                : "Requesting the latest processing record for the selected localization."
            }
            title={
              locale === "tr"
                ? "Durum sorgusu yukleniyor"
                : "Loading processing status"
            }
          />
        ) : isNotScheduled ? (
          <EmptyState
            className="min-h-56"
            description={
              locale === "tr"
                ? "Bu localization icin henuz asset processing kaydi olusturulmamis. Schedule dialogu ile yeni is acabilirsiniz."
                : "No asset processing record exists yet for this localization. Use the schedule dialog to create one."
            }
            title={
              locale === "tr"
                ? "Henuz schedule edilmedi"
                : "Not scheduled yet"
            }
          />
        ) : problem ? (
          <ProblemAlert problem={problem} />
        ) : job ? (
          <div className="grid gap-4">
            <div className="flex flex-wrap items-center gap-2">
              <span
                className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${getStatusClassName(
                  job.status,
                )}`}
              >
                {job.statusLabel}
              </span>
              <span className="inline-flex rounded-full border border-border/70 bg-muted/25 px-2.5 py-1 text-xs font-medium text-foreground">
                {job.contentTypeLabel}
              </span>
              <span className="inline-flex rounded-full border border-border/70 bg-muted/25 px-2.5 py-1 text-xs text-muted-foreground">
                {job.languageCode.toUpperCase()}
              </span>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  {locale === "tr" ? "Kimlik" : "Identity"}
                </p>
                <p className="mt-2 text-sm font-medium text-foreground">
                  #{job.contentId} / {job.externalKey}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {job.languageLabel}
                </p>
              </div>

              <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  {locale === "tr" ? "Deneme" : "Attempts"}
                </p>
                <p className="mt-2 text-sm font-medium text-foreground">
                  {job.attemptCount}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {locale === "tr"
                    ? `Sonraki deneme: ${formatDateTime(job.nextAttemptAt, {
                        year: "numeric",
                        month: "short",
                        day: "2-digit",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}`
                    : `Next attempt: ${formatDateTime(job.nextAttemptAt, {
                        year: "numeric",
                        month: "short",
                        day: "2-digit",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}`}
                </p>
              </div>

              <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  {locale === "tr" ? "Kaynak assetler" : "Source assets"}
                </p>
                <p className="mt-2 text-sm text-foreground">
                  Cover:{" "}
                  {job.coverAssetId === null
                    ? locale === "tr"
                      ? "bagli degil"
                      : "not bound"
                    : `#${job.coverAssetId}`}
                </p>
                <p className="mt-1 text-sm text-foreground">
                  Audio:{" "}
                  {job.audioAssetId === null
                    ? locale === "tr"
                      ? "bagli degil"
                      : "not bound"
                    : `#${job.audioAssetId}`}
                </p>
                {job.pageCount !== null ? (
                  <p className="mt-1 text-sm text-muted-foreground">
                    {locale === "tr"
                      ? `Sayfa sayisi: ${job.pageCount}`
                      : `Page count: ${job.pageCount}`}
                  </p>
                ) : null}
              </div>

              <div className="rounded-2xl border border-border/70 bg-muted/20 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  {locale === "tr" ? "Zamanlar" : "Timestamps"}
                </p>
                <p className="mt-2 text-sm text-foreground">
                  {locale === "tr" ? "Guncellendi" : "Updated"}:{" "}
                  {formatDateTime(job.updatedAt, {
                    year: "numeric",
                    month: "short",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </p>
                {job.startedAt ? (
                  <p className="mt-1 text-sm text-muted-foreground">
                    {locale === "tr" ? "Basladi" : "Started"}:{" "}
                    {formatDateTime(job.startedAt, {
                      year: "numeric",
                      month: "short",
                      day: "2-digit",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                ) : null}
                {job.completedAt ? (
                  <p className="mt-1 text-sm text-muted-foreground">
                    {locale === "tr" ? "Tamamlandi" : "Completed"}:{" "}
                    {formatDateTime(job.completedAt, {
                      year: "numeric",
                      month: "short",
                      day: "2-digit",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                ) : null}
              </div>
            </div>

            {job.lastErrorCode || job.lastErrorMessage ? (
              <div className="rounded-2xl border border-rose-200 bg-rose-50/60 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-rose-700">
                  {locale === "tr" ? "Son hata" : "Latest failure"}
                </p>
                <p className="mt-2 text-sm font-medium text-rose-900">
                  {job.lastErrorCode ??
                    (locale === "tr" ? "Kod yok" : "No code")}
                </p>
                <p className="mt-1 text-sm leading-6 text-rose-800">
                  {job.lastErrorMessage ??
                    (locale === "tr"
                      ? "Detay mesaj kaydedilmedi."
                      : "No detailed message was recorded.")}
                </p>
              </div>
            ) : null}

            {job.canRetry ? (
              <div className="flex justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onRetryJob?.(job)}
                >
                  {locale === "tr" ? "Retry dialogunu ac" : "Open retry dialog"}
                </Button>
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </section>
  );
}
