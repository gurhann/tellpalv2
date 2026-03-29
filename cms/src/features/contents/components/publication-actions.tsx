import { Archive, LoaderCircle, Send } from "lucide-react";
import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { Button } from "@/components/ui/button";
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
    <FormSection
      title="Publication Controls"
      description="Status, processing, and mobile visibility are derived from the backend rules for this locale."
    >
      {actionProblem ? <ProblemAlert problem={actionProblem} /> : null}

      <div className="grid gap-3">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
          <dl className="space-y-3 text-sm text-muted-foreground">
            <div className="flex items-center justify-between gap-3">
              <dt>Status</dt>
              <dd className="font-medium text-foreground">
                {localization.statusLabel}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt>Processing</dt>
              <dd className="font-medium text-foreground">
                {localization.processingStatusLabel}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt>Published at</dt>
              <dd className="font-medium text-foreground">
                {formatPublishedAt(localization.publishedAt)}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt>Mobile visibility</dt>
              <dd className="font-medium text-foreground">
                {localization.visibleToMobile ? "Visible" : "Hidden"}
              </dd>
            </div>
          </dl>
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

        <div className="rounded-2xl border border-border/70 bg-background px-4 py-4 text-sm leading-6 text-muted-foreground">
          {content.summary.supportsStoryPages
            ? "Story publishes depend on localized story pages being complete. If required page text or page audio is missing, the backend can reject publish with content_state_conflict."
            : "Non-story publishes depend on localization completeness and processing readiness. Missing body text, audio bindings, or incomplete processing keep mobile visibility off."}
        </div>
      </div>
    </FormSection>
  );
}
