import { CheckCircle2, Layers3, LayoutPanelLeft, Rows4 } from "lucide-react";
import { Link } from "react-router-dom";

import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ActionBar } from "@/components/workspace/action-bar";
import { TaskRail } from "@/components/workspace/task-rail";
import { WorkspaceShell } from "@/components/workspace/workspace-shell";

type PrototypeVariant = {
  id: string;
  name: string;
  summary: string;
  highlights: string[];
  winner?: boolean;
};

type ScoreRow = {
  criterion: string;
  variantA: number;
  variantB: number;
  variantC: number;
};

type UiLabPageProps = {
  topic: string;
  description: string;
  recommendation: string;
  variants: PrototypeVariant[];
  scoreRows: ScoreRow[];
  winnerHref?: string;
  winnerLabel?: string;
};

function getVariantIcon(id: string) {
  switch (id) {
    case "A":
      return LayoutPanelLeft;
    case "B":
      return Rows4;
    case "C":
    default:
      return Layers3;
  }
}

function scoreTotal(rowKey: "variantA" | "variantB" | "variantC", rows: ScoreRow[]) {
  return rows.reduce((sum, row) => sum + row[rowKey], 0);
}

export function UiLabPage({
  topic,
  description,
  recommendation,
  variants,
  scoreRows,
  winnerHref,
  winnerLabel,
}: UiLabPageProps) {
  const totalA = scoreTotal("variantA", scoreRows);
  const totalB = scoreTotal("variantB", scoreRows);
  const totalC = scoreTotal("variantC", scoreRows);

  return (
    <WorkspaceShell
      eyebrow="UI Labs"
      title={topic}
      description={description}
      aside={
        <TaskRail
          title="Decision matrix"
          description="The production rollout keeps the fixed scoring rule from the redesign plan."
          stats={[
            {
              label: "Variant A",
              value: `${totalA} pts`,
              tone: totalA >= totalB && totalA >= totalC ? "success" : "default",
            },
            {
              label: "Variant B",
              value: `${totalB} pts`,
              tone: totalB >= totalA && totalB >= totalC ? "success" : "default",
            },
            {
              label: "Variant C",
              value: `${totalC} pts`,
              tone: totalC >= totalA && totalC >= totalB ? "success" : "default",
            },
          ]}
        >
          <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
            <p className="font-medium text-foreground">{recommendation}</p>
            <p>
              Tie rule stays fixed: if A and B tie, keep A shell with B flow
              accents. If A and C tie, keep A shell with C drawer pattern.
            </p>
          </div>
        </TaskRail>
      }
    >
      <ActionBar
        title="Prototype scope"
        description="These routes use fixtures only. They compare information hierarchy and flow without touching live mutations."
        meta={
          winnerHref && winnerLabel ? (
            <Button asChild type="button" variant="outline">
              <Link to={winnerHref}>{winnerLabel}</Link>
            </Button>
          ) : null
        }
      >
        <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 text-sm text-muted-foreground">
          Desktop-first
        </span>
        <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 text-sm text-muted-foreground">
          Scroll-safe overlays
        </span>
        <span className="inline-flex rounded-full border border-border/70 bg-background px-3 py-1.5 text-sm text-muted-foreground">
          Hidden authenticated route
        </span>
      </ActionBar>

      <div className="grid gap-5 xl:grid-cols-3">
        {variants.map((variant) => {
          const Icon = getVariantIcon(variant.id);

          return (
            <Card
              key={variant.id}
              className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5"
            >
              <CardHeader className="gap-3 border-b border-border/60 bg-muted/10">
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-2">
                    <div className="inline-flex size-11 items-center justify-center rounded-2xl border border-border/70 bg-background text-foreground">
                      <Icon className="size-5" />
                    </div>
                    <h2 className="font-heading text-xl font-semibold tracking-tight text-foreground">
                      {variant.name}
                    </h2>
                  </div>
                  {variant.winner ? (
                    <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                      <CheckCircle2 className="size-3.5" />
                      Winner
                    </span>
                  ) : null}
                </div>
                <CardDescription className="text-sm leading-6">
                  {variant.summary}
                </CardDescription>
              </CardHeader>
              <CardContent className="grid gap-3 pt-5">
                {variant.highlights.map((highlight) => (
                  <div
                    key={highlight}
                    className="rounded-2xl border border-border/70 bg-background px-4 py-3 text-sm leading-6 text-muted-foreground"
                  >
                    {highlight}
                  </div>
                ))}
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader className="gap-2 border-b border-border/60 bg-muted/10">
          <h2 className="font-heading text-xl font-semibold tracking-tight text-foreground">
            Scoring matrix
          </h2>
          <CardDescription>
            Higher is better. The same matrix is used to choose the final production direction.
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-5">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border/60 text-muted-foreground">
                  <th className="pb-3 pr-4 font-medium">Criterion</th>
                  <th className="pb-3 pr-4 font-medium">A</th>
                  <th className="pb-3 pr-4 font-medium">B</th>
                  <th className="pb-3 font-medium">C</th>
                </tr>
              </thead>
              <tbody>
                {scoreRows.map((row) => (
                  <tr key={row.criterion} className="border-b border-border/40">
                    <td className="py-3 pr-4 text-foreground">{row.criterion}</td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {row.variantA}
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {row.variantB}
                    </td>
                    <td className="py-3 text-muted-foreground">{row.variantC}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </WorkspaceShell>
  );
}
