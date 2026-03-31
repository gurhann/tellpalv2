import { ArrowRight, Layers3, LoaderCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentLocalizationTabs } from "@/features/contents/components/localization-tabs";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContentSummaryCard } from "@/features/contents/components/content-summary-card";
import { StoryPageEntryLink } from "@/features/contents/components/story-page-entry-link";
import { ContentContributorEntryCard } from "@/features/contributors/components/content-contributor-entry-card";
import { useContentDetail } from "@/features/contents/queries/use-content-detail";
import { mapContentReadToFormValues } from "@/features/contents/schema/content-schema";

export function ContentDetailRoute() {
  const { contentId = "" } = useParams();
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;
  const contentQuery = useContentDetail(
    hasValidContentId ? parsedContentId : null,
  );
  const content = contentQuery.content;
  const canOpenStoryPages = content?.summary.supportsStoryPages ?? false;
  const routeTitle =
    content?.primaryLocalization?.title ??
    (hasValidContentId ? `Content #${parsedContentId}` : "Content Detail");
  const routeDescription = content
    ? `Metadata editing and localization workspaces are live for ${content.summary.externalKey}. Publish and archive actions now run inside each language tab.`
    : hasValidContentId
      ? "The CMS is loading content metadata and localization snapshots from the admin API. Metadata editing and locale workspaces become available as soon as the detail query resolves."
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

    return <ContentLocalizationTabs content={content} />;
  }

  return (
    <ContentPageShell
      eyebrow="Editorial Core"
      title={routeTitle}
      description={routeDescription}
      actions={
        <StoryPageEntryLink
          canOpen={canOpenStoryPages}
          contentId={parsedContentId}
        />
      }
      toolbar={renderToolbar()}
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Operations Snapshot</CardTitle>
              <CardDescription>
                Live status summary from the content detail query and
                per-language mutation workspace.
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
                Per-language edit and publication controls live
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Next Detail Tasks</CardTitle>
              <CardDescription>
                Metadata and localization editing are live. The next task
                expands deeper processing diagnostics around assets and
                follow-up.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Base metadata and locale changes now save through the admin API
                with field-level validation mapping.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Publish and archive actions now live inside each language tab,
                including backend conflict surfaces.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T05` expands processing visibility around assets and
                operational follow-up.
              </div>
            </CardContent>
          </Card>

          <ContentContributorEntryCard content={content} />
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
            while this task focuses on localization editing and publication
            control.
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
