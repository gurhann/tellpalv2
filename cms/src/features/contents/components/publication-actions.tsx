import { Archive, LoaderCircle, Send } from "lucide-react";
import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
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
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";
import { toastMutation } from "@/components/forms/form-utils";

type PublicationActionsProps = {
  content: ContentReadViewModel;
  localization: ContentLocalizationViewModel;
};

function formatPublishedAt(publishedAt: string | null) {
  if (!publishedAt) {
    return "Not published";
  }

  const timestamp = new Date(publishedAt);

  if (Number.isNaN(timestamp.getTime())) {
    return publishedAt;
  }

  return `${timestamp.toISOString().slice(0, 16).replace("T", " ")} UTC`;
}

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
  const { publishLocalization, archiveLocalization } =
    useContentLocalizationActions(content.summary.id);
  const [actionProblem, setActionProblem] = useState<ApiProblemDetail | null>(
    null,
  );
  const isPublishing = publishLocalization.isPending;
  const isArchiving = archiveLocalization.isPending;
  const isMutating = isPublishing || isArchiving;

  async function handlePublish() {
    setActionProblem(null);
    publishLocalization.reset();

    try {
      await toastMutation(
        publishLocalization.mutateAsync({
          languageCode: localization.languageCode,
        }),
        {
          loading: `Publishing ${localization.languageLabel} locale...`,
          success: `${localization.languageLabel} locale published.`,
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
          loading: `Archiving ${localization.languageLabel} locale...`,
          success: `${localization.languageLabel} locale archived.`,
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
        <CardTitle>Publishing</CardTitle>
        <CardDescription>
          Review readiness, then publish or archive this locale.
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4 pt-5">
        {actionProblem ? <ProblemAlert problem={actionProblem} /> : null}

        <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
          <dl className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border border-border/60 bg-background px-3 py-3">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Status
              </dt>
              <dd className="mt-2 text-sm font-medium text-foreground">
                {localization.statusLabel}
              </dd>
            </div>
            <div className="rounded-xl border border-border/60 bg-background px-3 py-3">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Processing
              </dt>
              <dd className="mt-2 text-sm font-medium text-foreground">
                {localization.processingStatusLabel}
              </dd>
            </div>
            <div className="rounded-xl border border-border/60 bg-background px-3 py-3">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Mobile visibility
              </dt>
              <dd className="mt-2 text-sm font-medium text-foreground">
                {localization.visibleToMobile ? "Visible" : "Hidden"}
              </dd>
            </div>
            <div className="rounded-xl border border-border/60 bg-background px-3 py-3">
              <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Published at
              </dt>
              <dd className="mt-2 text-sm font-medium text-foreground">
                {formatPublishedAt(localization.publishedAt)}
              </dd>
            </div>
          </dl>

          <p className="text-sm leading-6 text-muted-foreground">
            {content.summary.supportsStoryPages
              ? "Publishing stays locked until localized story pages and processing are complete."
              : "Publishing stays locked until localization completeness and processing are ready."}
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
            Publish locale
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
            Archive locale
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
