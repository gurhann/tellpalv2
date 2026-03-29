import {
  ArrowRight,
  Archive,
  BookOpenText,
  Layers3,
  LoaderCircle,
  Send,
} from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { type LanguageBadgeTone } from "@/components/language/language-badge";
import {
  LanguageTabs,
  type LanguageTabItem,
} from "@/components/language/language-tabs";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentSummaryCard } from "@/features/contents/components/content-summary-card";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import type { ContentLocalizationViewModel } from "@/features/contents/model/content-view-model";
import { useContentDetail } from "@/features/contents/queries/use-content-detail";
import { mapContentReadToFormValues } from "@/features/contents/schema/content-schema";

function getLocalizationTone(
  localization: ContentLocalizationViewModel,
): LanguageBadgeTone {
  if (localization.isArchived) {
    return "muted";
  }

  if (localization.processingStatus === "FAILED") {
    return "destructive";
  }

  if (localization.isPublished && localization.isProcessingComplete) {
    return "success";
  }

  if (
    localization.status === "DRAFT" ||
    localization.processingStatus === "PENDING" ||
    localization.processingStatus === "PROCESSING"
  ) {
    return "warning";
  }

  return "info";
}

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

function getLocalizationDescription(
  localization: ContentLocalizationViewModel,
) {
  const parts = [localization.statusLabel, localization.processingStatusLabel];

  if (localization.visibleToMobile) {
    parts.push("Mobile visible");
  }

  if (localization.hasAudioAsset) {
    parts.push("Audio attached");
  } else if (localization.hasCoverAsset) {
    parts.push("Cover attached");
  }

  return parts.join(" / ");
}

function getBodyTextSummary(
  localization: ContentLocalizationViewModel,
  supportsStoryPages: boolean,
) {
  if (localization.bodyText) {
    return localization.bodyText;
  }

  if (supportsStoryPages) {
    return "Story copy is authored on the story pages route for this language.";
  }

  return "No body text has been provided for this localization yet.";
}

export function ContentDetailRoute() {
  const { contentId = "" } = useParams();
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;
  const contentQuery = useContentDetail(
    hasValidContentId ? parsedContentId : null,
  );
  const [activeLanguage, setActiveLanguage] = useState("");
  const content = contentQuery.content;
  const localizationTabs: LanguageTabItem[] = content
    ? content.localizations.map((localization) => ({
        code: localization.languageCode,
        label: localization.languageLabel,
        tone: getLocalizationTone(localization),
        meta: localization.statusLabel,
        description: getLocalizationDescription(localization),
      }))
    : [];
  const resolvedActiveLanguage =
    localizationTabs.find((item) => item.code === activeLanguage)?.code ??
    localizationTabs[0]?.code ??
    "";
  const canOpenStoryPages = content?.summary.supportsStoryPages ?? false;
  const routeTitle =
    content?.primaryLocalization?.title ??
    (hasValidContentId ? `Content #${parsedContentId}` : "Content Detail");
  const routeDescription = content
    ? `Metadata editing is now live for ${content.summary.externalKey}. Localization and publication mutations remain disabled until later content tasks.`
    : hasValidContentId
      ? "The CMS is loading content metadata and localization snapshots from the admin API. Metadata editing becomes available as soon as the detail query resolves."
      : "This route expects a valid numeric content id from the content registry.";

  function renderToolbar() {
    if (content) {
      return <ContentSummaryCard content={content} />;
    }

    const title = !hasValidContentId
      ? "Invalid route"
      : contentQuery.isNotFound
        ? "Record not found"
        : contentQuery.problem
          ? "Metadata unavailable"
          : "Loading metadata";
    const description = !hasValidContentId
      ? "Open a content record from the registry to load a valid detail workspace."
      : contentQuery.isNotFound
        ? "The admin API did not return a content record for this route."
        : contentQuery.problem
          ? "Retry the detail query to restore metadata and localization summaries."
          : "The detail shell is requesting summary metadata and localization snapshots.";

    return (
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3 md:col-span-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Detail Status
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">{title}</p>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            Route
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            /contents/{contentId || "?"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Inline loading, error, and not-found states stay inside this shell.
          </p>
        </div>
      </div>
    );
  }

  function renderLocalizationWorkspace(item: LanguageTabItem) {
    const localization = content?.localizations.find(
      (entry) => entry.languageCode === item.code,
    );

    if (!content || !localization) {
      return null;
    }

    return (
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>{localization.title}</CardTitle>
            <CardDescription>
              Read-only localization snapshot from the content detail query.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Description</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {localization.description ??
                  "No description has been provided."}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Body text</p>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {getBodyTextSummary(
                  localization,
                  content.summary.supportsStoryPages,
                )}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">Publishing</p>
              <dl className="mt-2 space-y-2 text-sm text-muted-foreground">
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
              </dl>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
              <p className="text-sm font-medium text-foreground">
                Assets and visibility
              </p>
              <dl className="mt-2 space-y-2 text-sm text-muted-foreground">
                <div className="flex items-center justify-between gap-3">
                  <dt>Cover asset</dt>
                  <dd className="font-medium text-foreground">
                    {localization.hasCoverAsset
                      ? `Attached (#${localization.coverAssetId})`
                      : "Missing"}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Audio asset</dt>
                  <dd className="font-medium text-foreground">
                    {localization.hasAudioAsset
                      ? `Attached (#${localization.audioAssetId})`
                      : "Missing"}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Mobile visibility</dt>
                  <dd className="font-medium text-foreground">
                    {localization.visibleToMobile ? "Visible" : "Hidden"}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Duration</dt>
                  <dd className="font-medium text-foreground">
                    {localization.durationMinutes === null
                      ? "Not set"
                      : `${localization.durationMinutes} min`}
                  </dd>
                </div>
              </dl>
            </div>
          </CardContent>
        </Card>

        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>Locale Actions</CardTitle>
            <CardDescription>
              Mutation entry points remain visible but disabled in this
              read-only task.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm text-muted-foreground">
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              Publish state: {localization.statusLabel}
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              Processing state: {localization.processingStatusLabel}
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
              Asset presence:{" "}
              {localization.hasCoverAsset || localization.hasAudioAsset
                ? "Bindings detected"
                : "No asset bindings yet"}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  function renderDetailContent() {
    if (!hasValidContentId) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/contents">Return to content registry</Link>
            </Button>
          }
          description="Open a record from the content registry to reach a valid content detail workspace."
          title="Invalid content route"
        />
      );
    }

    if (contentQuery.isLoading) {
      return (
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
              <LoaderCircle className="size-6 animate-spin" />
            </div>
            <div className="space-y-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                Loading content detail
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                The CMS is requesting metadata and localization snapshots for
                this content record.
              </p>
            </div>
          </CardContent>
        </Card>
      );
    }

    if (contentQuery.isNotFound) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/contents">Return to content registry</Link>
            </Button>
          }
          description="The requested content record does not exist in the current backend environment."
          title="Content not found"
        />
      );
    }

    if (contentQuery.problem) {
      return (
        <ProblemAlert
          actions={
            <Button
              type="button"
              variant="outline"
              onClick={() => void contentQuery.refetch()}
            >
              Retry
            </Button>
          }
          problem={contentQuery.problem}
        />
      );
    }

    if (!content) {
      return (
        <EmptyState
          description="No detail payload is available for this content route yet."
          title="Content detail unavailable"
        />
      );
    }

    if (content.localizations.length === 0) {
      return (
        <EmptyState
          description="This content record exists, but it does not have any localization snapshots yet."
          title="No localizations yet"
        />
      );
    }

    return (
      <LanguageTabs
        items={localizationTabs}
        listLabel="Content localization tabs"
        onValueChange={setActiveLanguage}
        renderContent={renderLocalizationWorkspace}
        value={resolvedActiveLanguage}
      />
    );
  }

  return (
    <ContentPageShell
      eyebrow="Editorial Core"
      title={routeTitle}
      description={routeDescription}
      actions={
        <>
          {canOpenStoryPages ? (
            <Button asChild variant="outline">
              <Link to={`/contents/${parsedContentId}/story-pages`}>
                <BookOpenText className="size-4" />
                Open story pages
              </Link>
            </Button>
          ) : (
            <Button disabled type="button" variant="outline">
              <BookOpenText className="size-4" />
              Story pages unavailable
            </Button>
          )}
          <Button disabled type="button" variant="outline">
            <Archive className="size-4" />
            Archive locale
          </Button>
          <Button disabled type="button">
            <Send className="size-4" />
            Publish locale
          </Button>
        </>
      }
      toolbar={renderToolbar()}
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Operations Snapshot</CardTitle>
              <CardDescription>
                Read-only status summary from the content detail query.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  Visibility
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? `${content.visibleToMobileLocalizationCount} of ${content.localizationCount} localizations are mobile visible.`
                    : "Visibility counts appear after the detail query resolves."}
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  Processing
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? `${content.processingCompleteLocalizationCount} of ${content.localizationCount} localizations are processing complete.`
                    : "Processing counts appear after the detail query resolves."}
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  Story structure
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? content.summary.supportsStoryPages
                      ? `${content.summary.pageCount ?? 0} story page${
                          content.summary.pageCount === 1 ? "" : "s"
                        } live under the child route.`
                      : "Story pages are not used for this content type."
                    : "Story structure appears after the detail query resolves."}
                </p>
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Layers3 className="size-3.5" />
                Shared detail action region ready
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Next Detail Tasks</CardTitle>
              <CardDescription>
                Metadata editing is live. The next tasks expand localization
                mutation and richer diagnostics.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Base metadata now saves through the admin API with field-level
                validation and conflict handling.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T04` wires publish, archive, and localization mutation
                flows.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T05` expands processing visibility around assets and
                operational follow-up.
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      {renderDetailContent()}

      {content ? (
        <FormSection
          description="Update the base content metadata. Content type is fixed after creation, while external key, age range, and active state can be changed here."
          title="Metadata"
        >
          <ContentForm
            key={`${content.summary.id}-${content.summary.externalKey}-${content.summary.ageRange}-${content.summary.active}`}
            contentId={content.summary.id}
            initialValues={mapContentReadToFormValues(content)}
            mode="update"
          />
        </FormSection>
      ) : null}

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Route Integration Preview</CardTitle>
          <CardDescription>
            The detail shell stays connected to the story pages child route
            while this task focuses only on read/query binding.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
            /contents/{hasValidContentId ? parsedContentId : "?"}
          </span>
          <ArrowRight className="size-4" />
          <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
            /contents/{hasValidContentId ? parsedContentId : "?"}/story-pages
          </span>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
