import { Archive, LoaderCircle, Send } from "lucide-react";
import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type {
  ContentLocalizationViewModel,
  ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
import { useContentLocalizationActions } from "@/features/contents/mutations/use-content-localization-actions";
import { useReadingPublication } from "@/features/contents/queries/use-reading-publication";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type PublicationActionsProps = {
  content: ContentReadViewModel;
  localization: ContentLocalizationViewModel;
};

function getGenericActionProblem(error: unknown): ApiProblemDetail {
  if (error instanceof Error && error.message.trim().length > 0) {
    return {
      type: "about:blank",
      title: "Request failed",
      status: 500,
      detail: error.message,
    };
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: "Publication action could not be completed. Try again.",
  };
}

export function PublicationActions(props: PublicationActionsProps) {
  return props.content.summary.supportsStoryPages ? (
    <ReadingPublicationActions {...props} />
  ) : (
    <PublicationActionBody {...props} />
  );
}

function ReadingPublicationActions(props: PublicationActionsProps) {
  const reading = useReadingPublication(
    props.content.summary.id,
    props.localization.languageCode,
    true,
  );
  return <PublicationActionBody {...props} reading={reading} />;
}

function PublicationActionBody({
  content,
  localization,
  reading,
}: PublicationActionsProps & {
  reading?: ReturnType<typeof useReadingPublication>;
}) {
  const { formatDateTime, locale } = useI18n();
  const { publishLocalization, archiveLocalization } =
    useContentLocalizationActions(content.summary.id);
  const [actionProblem, setActionProblem] = useState<ApiProblemDetail | null>(
    null,
  );
  const isPublishing = publishLocalization.isPending;
  const isArchiving = archiveLocalization.isPending;
  const isMutating = isPublishing || isArchiving;
  const copy =
    locale === "tr"
      ? {
          title: "Yayinlama",
          description:
            "Bu dil kaydi icin yayin aksiyonlarini buradan tamamlayin.",
          publishLoading: `${localization.languageLabel} dili yayinlaniyor...`,
          publishSuccess: `${localization.languageLabel} dili yayina alindi.`,
          archiveLoading: `${localization.languageLabel} dili arsivleniyor...`,
          archiveSuccess: `${localization.languageLabel} dili arsivlendi.`,
          publishLabel: "Dili yayina al",
          archiveLabel: "Dili arsivle",
          publishedAt: "Yayinlandi",
          notPublished: "Henuz yayinlanmadi",
          storyGate:
            "Yayinlama, yerellestirilmis hikaye sayfalari ve isleme tamamlandiginda anlamli hale gelir.",
          defaultGate:
            "Yayinlama, bu dil metadata'si ve isleme adimlari hazir oldugunda anlamli hale gelir.",
        }
      : {
          title: "Publishing",
          description: "Complete publication actions for this locale here.",
          publishLoading: `Publishing ${localization.languageLabel} locale...`,
          publishSuccess: `${localization.languageLabel} locale published.`,
          archiveLoading: `Archiving ${localization.languageLabel} locale...`,
          archiveSuccess: `${localization.languageLabel} locale archived.`,
          publishLabel: "Publish locale",
          archiveLabel: "Archive locale",
          publishedAt: "Published",
          notPublished: "Not yet published",
          storyGate:
            "Publishing is meaningful once localized story pages and processing are complete.",
          defaultGate:
            "Publishing is meaningful once this locale metadata and processing steps are ready.",
        };
  const publishedAtLabel = localization.publishedAt
    ? formatDateTime(localization.publishedAt, {
        year: "numeric",
        month: "short",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      })
    : copy.notPublished;

  async function handlePublish() {
    setActionProblem(null);
    publishLocalization.reset();

    try {
      await toastMutation(
        publishLocalization.mutateAsync({
          languageCode: localization.languageCode,
        }),
        {
          loading: copy.publishLoading,
          success: reading?.data?.enabled
            ? locale === "tr"
              ? "Okuma hazırlığı istendi."
              : "Reading preparation requested."
            : copy.publishSuccess,
        },
      );
    } catch (error) {
      setActionProblem(
        error instanceof ApiClientError
          ? error.problem
          : getGenericActionProblem(error),
      );
    }
  }

  async function handleArchive() {
    setActionProblem(null);
    archiveLocalization.reset();

    try {
      await toastMutation(
        archiveLocalization.mutateAsync(localization.languageCode),
        {
          loading: copy.archiveLoading,
          success: copy.archiveSuccess,
        },
      );
    } catch (error) {
      setActionProblem(
        error instanceof ApiClientError
          ? error.problem
          : getGenericActionProblem(error),
      );
    }
  }

  return (
    <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
      <CardHeader className="gap-2 border-b border-border/60 bg-muted/10 pb-4">
        <CardTitle>{copy.title}</CardTitle>
        <CardDescription>{copy.description}</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4 pt-5">
        {actionProblem ? <ProblemAlert problem={actionProblem} /> : null}

        <div className="rounded-2xl border border-border/70 bg-muted/15 px-4 py-3">
          <p className="text-sm leading-6 text-muted-foreground">
            {content.summary.supportsStoryPages
              ? copy.storyGate
              : copy.defaultGate}
          </p>
          <p className="mt-3 text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">
            {copy.publishedAt}
          </p>
          <p className="mt-1 text-sm font-medium text-foreground">
            {publishedAtLabel}
          </p>
        </div>

        {reading?.isError ? (
          <div role="alert" className="grid gap-2 text-sm text-destructive">
            <p>
              {locale === "tr"
                ? "Okuma hazırlığı durumu alınamadı."
                : "Reading preparation status could not be loaded."}
            </p>
            <Button
              type="button"
              variant="outline"
              onClick={() => void reading.refetch()}
            >
              {locale === "tr" ? "Durumu yenile" : "Refresh status"}
            </Button>
          </div>
        ) : null}
        {reading?.data?.enabled ? (
          <div className="grid gap-2 text-sm" role="status" aria-live="polite">
            <p className="font-medium">
              {readingStateLabel(reading.data.state, locale === "tr")}
            </p>
            <p className="text-muted-foreground">
              {locale === "tr"
                ? "Yerel/test okuma sözleşmesi; mobil dağıtım durumunu değiştirmez."
                : "Local/test reading contract; ordinary mobile delivery is unchanged."}
            </p>
            {reading.data.liveRevision && reading.data.state !== "LIVE" ? (
              <p>
                {locale === "tr"
                  ? "Önceki doğrulanmış revizyon uygunluğunu koruyor."
                  : "The previous verified revision remains eligible."}
              </p>
            ) : null}
            {reading.data.diagnostic ? (
              <p>
                {reading.data.diagnostic === "SOURCE_NOT_ADMITTED"
                  ? locale === "tr"
                    ? "Yeni kabul edilmiş değişmez görsel kimliklerini seçin ve yeniden deneyin."
                    : "Select newly admitted immutable image IDs, then retry."
                  : locale === "tr"
                    ? "Güncel okuma görselleri doğrulanamadı. Kaynakları kontrol edip yeniden deneyin."
                    : "Current reading images could not be verified. Check the sources and retry."}
              </p>
            ) : null}
            {reading.data.retryable ? (
              <Button
                type="button"
                variant="outline"
                disabled={reading.retry.isPending}
                onClick={() => reading.retry.mutate()}
              >
                {locale === "tr"
                  ? "Okuma hazırlığını yeniden dene"
                  : "Retry reading preparation"}
              </Button>
            ) : null}
            {reading.retry.isError ? (
              <p role="alert">
                {locale === "tr"
                  ? "Yeniden deneme isteği tamamlanamadı."
                  : "The retry request could not be completed."}
              </p>
            ) : null}
          </div>
        ) : null}

        <div className="grid gap-3 sm:grid-cols-2">
          <Button
            type="button"
            onClick={() => void handlePublish()}
            disabled={
              isMutating ||
              reading?.isLoading ||
              reading?.isError ||
              (localization.isPublished &&
                !localization.isArchived &&
                !(
                  reading?.data?.enabled &&
                  ["NOT_REQUESTED", "WITHDRAWN", "FAILED"].includes(reading.data.state)
                ))
            }
          >
            {isPublishing ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <Send className="size-4" />
            )}
            {copy.publishLabel}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => void handleArchive()}
            disabled={isMutating || localization.isArchived}
          >
            {isArchiving ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <Archive className="size-4" />
            )}
            {copy.archiveLabel}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function readingStateLabel(state: string, turkish: boolean) {
  const labels: Record<string, [string, string]> = {
    NOT_REQUESTED: [
      "Okuma hazırlığı istenmedi",
      "Reading preparation not requested",
    ],
    PENDING: ["Okuma hazırlığı bekliyor", "Reading preparation pending"],
    PREPARING: ["Okuma görselleri hazırlanıyor", "Preparing reading images"],
    LIVE: [
      "Doğrulanmış okuma revizyonu benimsendi",
      "Verified reading revision adopted",
    ],
    FAILED: ["Okuma hazırlığı başarısız", "Reading preparation failed"],
    WITHDRAWN: [
      "Okuma uygunluğu geri çekildi",
      "Reading eligibility withdrawn",
    ],
  };
  return labels[state]?.[turkish ? 0 : 1] ?? "";
}
