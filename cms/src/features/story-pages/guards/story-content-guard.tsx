import { BookOpenText, LoaderCircle } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { useContentDetail } from "@/features/contents/queries/use-content-detail";

type StoryContentGuardProps = {
  contentId: number | null;
  children: (content: ContentReadViewModel) => ReactNode;
};

export function StoryContentGuard({
  contentId,
  children,
}: StoryContentGuardProps) {
  const hasValidContentId =
    typeof contentId === "number" &&
    Number.isInteger(contentId) &&
    contentId > 0;
  const contentQuery = useContentDetail(hasValidContentId ? contentId : null);

  if (!hasValidContentId) {
    return (
      <EmptyState
        action={
          <Button asChild type="button" variant="outline">
            <Link to="/contents">Return to content registry</Link>
          </Button>
        }
        description="Story page editing requires a valid parent content id."
        icon={BookOpenText}
        title="Invalid story page route"
      />
    );
  }

  if (contentQuery.isLoading) {
    return (
      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardContent className="flex min-h-72 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
            <LoaderCircle className="size-6 animate-spin" />
          </div>
          <div className="space-y-2">
            <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
              Loading story editor
            </h2>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
              The CMS is checking the parent content type before the story page
              workspace opens.
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
        description="The parent content record could not be found in the current backend environment."
        icon={BookOpenText}
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

  const content = contentQuery.content;

  if (!content) {
    return (
      <EmptyState
        description="The parent content record is unavailable, so the story editor cannot open yet."
        icon={BookOpenText}
        title="Story editor unavailable"
      />
    );
  }

  if (!content.summary.supportsStoryPages) {
    return (
      <EmptyState
        action={
          <Button asChild type="button" variant="outline">
            <Link to={`/contents/${content.summary.id}`}>
              Return to content detail
            </Link>
          </Button>
        }
        description={`${content.summary.typeLabel} records do not use story pages. Continue editing localized content from the parent detail workspace.`}
        icon={BookOpenText}
        title="Story pages are unavailable"
      />
    );
  }

  return children(content);
}
