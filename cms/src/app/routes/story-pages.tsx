import { ArrowLeft, BookOpenText, Layers3, Plus, Rows3 } from "lucide-react";
import { Link, useParams } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";

export function StoryPagesRoute() {
  const { contentId = "" } = useParams();
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;

  return (
    <StoryContentGuard contentId={hasValidContentId ? parsedContentId : null}>
      {(content) => {
        const routeTitle = content.primaryLocalization?.title
          ? `Story Pages for ${content.primaryLocalization.title}`
          : `Story Pages for Content #${content.summary.id}`;
        const storyPageCount = content.summary.pageCount ?? 0;

        return (
          <ContentPageShell
            eyebrow="Story Editor"
            title={routeTitle}
            description="This STORY-only child route is ready for page ordering, illustration assignment, and localized page editing. Live page collections remain blocked until the backend exposes story-page read endpoints."
            actions={
              <>
                <Button asChild type="button" variant="outline">
                  <Link to={`/contents/${content.summary.id}`}>
                    <ArrowLeft className="size-4" />
                    Return to content detail
                  </Link>
                </Button>
                <Button disabled type="button">
                  <Plus className="size-4" />
                  Add story page
                </Button>
              </>
            }
            toolbar={
              <div className="grid gap-3 md:grid-cols-3">
                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                    Parent Content
                  </p>
                  <p className="mt-2 text-sm font-medium text-foreground">
                    {content.summary.externalKey}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {content.summary.typeLabel} / {content.localizationCount}{" "}
                    localization
                    {content.localizationCount === 1 ? "" : "s"}
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                    Story Structure
                  </p>
                  <p className="mt-2 text-sm font-medium text-foreground">
                    {storyPageCount} story page{storyPageCount === 1 ? "" : "s"}{" "}
                    reserved
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    The current backend detail query exposes only aggregate page
                    count.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                    Publication
                  </p>
                  <p className="mt-2 text-sm font-medium text-foreground">
                    {content.publishedLocalizationCount} published,{" "}
                    {content.processingCompleteLocalizationCount} processing
                    complete
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Story publication still depends on per-page localization
                    completeness in each language.
                  </p>
                </div>
              </div>
            }
            aside={
              <>
                <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
                  <CardHeader>
                    <CardTitle>Story Route Access</CardTitle>
                    <CardDescription>
                      This child route is intentionally limited to `STORY`
                      content types.
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                      <p className="text-sm font-medium text-foreground">
                        Parent type
                      </p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {content.summary.typeLabel} records can own page
                        structure and per-page localizations.
                      </p>
                    </div>
                    <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                      <p className="text-sm font-medium text-foreground">
                        Read gap
                      </p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        No admin GET endpoint exists yet for story-page lists or
                        individual page detail payloads.
                      </p>
                    </div>
                  </CardContent>
                </Card>

                <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
                  <CardHeader>
                    <CardTitle>Next Story Tasks</CardTitle>
                    <CardDescription>
                      This route now owns story-page-specific access and shell
                      layout.
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3 text-sm text-muted-foreground">
                    <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                      `M04-T02` will bind page collections and ordering once a
                      read contract exists.
                    </div>
                    <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                      `M04-T03` will attach illustration pickers and page
                      localization editors to this shell.
                    </div>
                  </CardContent>
                </Card>
              </>
            }
          >
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Story Page Workspace Ready</CardTitle>
                <CardDescription>
                  The content detail route now hands STORY records off to a
                  dedicated child workspace instead of a generic placeholder.
                </CardDescription>
              </CardHeader>
              <CardContent className="grid gap-3 lg:grid-cols-3">
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
                  <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Rows3 className="size-4 text-primary" />
                    Page ordering
                  </div>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    Reordering and page-count hydration will land here when the
                    backend exposes a story-page collection read.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
                  <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <BookOpenText className="size-4 text-primary" />
                    Page payloads
                  </div>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    Each story page will manage illustration references and
                    localized body/audio payloads separately from content-level
                    metadata.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
                  <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Layers3 className="size-4 text-primary" />
                    Publish readiness
                  </div>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    Story publish still depends on every page having a complete
                    localization in the target language.
                  </p>
                </div>
              </CardContent>
            </Card>
          </ContentPageShell>
        );
      }}
    </StoryContentGuard>
  );
}
