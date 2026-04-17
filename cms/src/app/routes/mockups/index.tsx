import { ArrowRight, BookOpenText, FolderKanban, PanelsTopLeft } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { TaskRail } from "@/components/workspace/task-rail";
import { WorkspaceShell } from "@/components/workspace/workspace-shell";
import { useI18n } from "@/i18n/locale-provider";

type MockupEntry = {
  title: string;
  description: string;
  href: string;
  stats: string[];
  icon: typeof BookOpenText;
};

export function MockupLabsIndexRoute() {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "UI Labs",
          title: "Variant A Mockup Lab",
          description:
            "High-fidelity, clickable fixture routes that show how the winning Variant A shell will feel before production screens are touched.",
          railTitle: "Scope",
          railDescription:
            "These routes stay authenticated and hidden from navigation, but remain deep-linkable for review and implementation alignment.",
          railOne: "Content Studio registry + detail + locale-preserving story handoff",
          railTwo: "Category Studio registry + detail with localization and curation",
          railThree: "Story Page Editor with table-first layout and large modal editing",
          open: "Open mockup",
        }
      : {
          eyebrow: "UI Labs",
          title: "Variant A Mockup Lab",
          description:
            "High-fidelity, clickable fixture routes that show how the winning Variant A shell will feel before production screens are touched.",
          railTitle: "Scope",
          railDescription:
            "These routes stay authenticated and hidden from navigation, but remain deep-linkable for review and implementation alignment.",
          railOne: "Content Studio registry + detail + locale-preserving story handoff",
          railTwo: "Category Studio registry + detail with localization and curation",
          railThree: "Story Page Editor with table-first layout and large modal editing",
          open: "Open mockup",
        };

  const entries: MockupEntry[] = [
    {
      title: "Content Studio",
      description:
        "Registry, detail lane, readiness rail, and story-page handoff in the calm Variant A shell.",
      href: "/labs/mockups/contents",
      stats: ["Table-first registry", "Locale tabs", "Story handoff"],
      icon: BookOpenText,
    },
    {
      title: "Category Studio",
      description:
        "Shared shell language with localized category workspaces and curation prerequisites in the rail.",
      href: "/labs/mockups/categories",
      stats: ["Shared shell language", "Curation lane", "Prerequisite rail"],
      icon: FolderKanban,
    },
    {
      title: "Story Page Editor",
      description:
        "Variant A table-first editing flow with a large, scroll-safe modal and preserved locale focus.",
      href: "/labs/mockups/contents/demo-content/story-pages",
      stats: ["Large modal", "Locale carry-over", "Dense readiness rail"],
      icon: PanelsTopLeft,
    },
  ];

  return (
    <WorkspaceShell
      eyebrow={copy.eyebrow}
      title={copy.title}
      description={copy.description}
      aside={
        <TaskRail title={copy.railTitle} description={copy.railDescription}>
          <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
            <p>{copy.railOne}</p>
            <p>{copy.railTwo}</p>
            <p>{copy.railThree}</p>
          </div>
        </TaskRail>
      }
    >
      <div className="grid gap-5 xl:grid-cols-3">
        {entries.map((entry) => {
          const Icon = entry.icon;

          return (
            <Card
              key={entry.href}
              className="border border-border/70 bg-[linear-gradient(180deg,_rgba(255,255,255,0.98),_rgba(246,243,236,0.88))] shadow-lg shadow-slate-950/5"
            >
              <CardHeader className="gap-3 border-b border-border/60 bg-background/60">
                <div className="inline-flex size-11 items-center justify-center rounded-2xl border border-border/70 bg-background text-foreground">
                  <Icon className="size-5" />
                </div>
                <h2 className="font-heading text-xl font-semibold tracking-tight text-foreground">
                  {entry.title}
                </h2>
                <CardDescription>{entry.description}</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4 pt-5">
                <div className="flex flex-wrap gap-2">
                  {entry.stats.map((stat) => (
                    <span
                      key={stat}
                      className="inline-flex rounded-full border border-border/70 bg-background px-2.5 py-1 text-xs font-medium text-muted-foreground"
                    >
                      {stat}
                    </span>
                  ))}
                </div>
                <Button asChild className="justify-between" type="button" variant="outline">
                  <Link to={entry.href}>
                    {copy.open}
                    <ArrowRight className="size-4" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </WorkspaceShell>
  );
}
