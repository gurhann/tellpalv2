import { ArrowRight, Archive, BookOpenText, Layers3, Send } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

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
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

const localizationTabs: LanguageTabItem[] = [
  {
    code: "en",
    tone: "success",
    meta: "Published",
    description:
      "Live mobile copy, completed cover asset, and ready audio binding.",
  },
  {
    code: "tr",
    tone: "warning",
    meta: "Draft",
    description: "Editorial review in progress before publication.",
  },
  {
    code: "de",
    tone: "muted",
    meta: "Planned",
    description: "Slot reserved for the next localization rollout.",
  },
];

export function ContentDetailRoute() {
  const { contentId = "unknown" } = useParams();
  const [activeLanguage, setActiveLanguage] = useState(
    localizationTabs[0].code,
  );

  return (
    <ContentPageShell
      eyebrow="Editorial Core"
      title={`Content #${contentId}`}
      description="The detail route now has its own shared content shell: metadata toolbar, action region, localized workspace tabs, and an operations sidebar. Query-backed data will be plugged into this surface in the next task."
      actions={
        <>
          <Button asChild variant="outline">
            <Link to={`/contents/${contentId}/story-pages`}>
              <BookOpenText className="size-4" />
              Open story pages
            </Link>
          </Button>
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
      toolbar={
        <div className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Metadata
            </p>
            <p className="mt-2 text-sm font-medium text-foreground">
              Story / Active / Age 4-6
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              External key: `content.preview.{contentId}`
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Localization
            </p>
            <p className="mt-2 text-sm font-medium text-foreground">
              3 language workspaces prepared
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Tabs below will hydrate from `GET /api/admin/contents/{contentId}
              `.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Processing
            </p>
            <p className="mt-2 text-sm font-medium text-foreground">
              Audio and cover asset status live here next
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              The route shape already reserves the workflow snapshot panel.
            </p>
          </div>
        </div>
      }
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Operations Snapshot</CardTitle>
              <CardDescription>
                This sidebar is reserved for status, processing, and route-level
                decisions.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  Visibility
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Mobile-ready in English, draft in Turkish, planned in German.
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  Story structure
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  12 pages expected. Story page editing lives under the linked
                  child route.
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
                The shell stays stable while data, forms, and publish flows plug
                into it.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T02` attaches content detail and localization queries.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T03` activates metadata editing and server validation.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T04` and `M03-T05` will light up publish/archive and
                processing visibility.
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <LanguageTabs
        items={localizationTabs}
        listLabel="Content localization tabs"
        onValueChange={setActiveLanguage}
        renderContent={(item) => (
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_18rem]">
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>
                  {item.label ?? item.code.toUpperCase()} Workspace
                </CardTitle>
                <CardDescription>
                  This panel will host the localized content form, body copy,
                  and asset selectors.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
                  <p className="text-sm font-medium text-foreground">
                    Localized title
                  </p>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Placeholder copy for `{item.code}`. The real title and
                    description arrive with the detail query hook.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
                  <p className="text-sm font-medium text-foreground">
                    Body and description
                  </p>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Structured text editing and validation will be wired in the
                    form task.
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Locale Actions</CardTitle>
                <CardDescription>
                  Quick status changes and asset bindings will live beside each
                  language tab.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Cover asset selection
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Audio binding and duration
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  Publish and archive status
                </div>
              </CardContent>
            </Card>
          </div>
        )}
        value={activeLanguage}
      />

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Route Integration Preview</CardTitle>
          <CardDescription>
            The detail shell already connects the content route tree to the
            story pages child route.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
            /contents/{contentId}
          </span>
          <ArrowRight className="size-4" />
          <span className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1.5">
            /contents/{contentId}/story-pages
          </span>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
