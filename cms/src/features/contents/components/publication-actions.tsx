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

export function PublicationActions({
  content,
  localization,
}: PublicationActionsProps) {
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
          success: copy.publishSuccess,
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

        <div className="grid gap-3 sm:grid-cols-2">
          <Button
            type="button"
            onClick={() => void handlePublish()}
            disabled={
              isMutating ||
              (localization.isPublished && !localization.isArchived)
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
