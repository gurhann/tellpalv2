import type { ReactNode } from "react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type TaskRailStat = {
  label: string;
  value: string;
  tone?: "default" | "success" | "warning";
};

type TaskRailProps = {
  title: string;
  description?: string;
  stats?: TaskRailStat[];
  children?: ReactNode;
  className?: string;
};

function getToneClassName(tone: TaskRailStat["tone"] = "default") {
  switch (tone) {
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-900";
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-900";
    case "default":
    default:
      return "border-border/70 bg-background text-foreground";
  }
}

export function TaskRail({
  title,
  description,
  stats = [],
  children,
  className,
}: TaskRailProps) {
  return (
    <Card
      className={cn(
        "border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5",
        className,
      )}
    >
      <CardHeader className="gap-2 border-b border-border/60">
        <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
          {title}
        </h2>
        {description ? (
          <CardDescription className="text-sm leading-6">
            {description}
          </CardDescription>
        ) : null}
      </CardHeader>
      <CardContent className="grid gap-4 pt-5">
        {stats.length > 0 ? (
          <div className="grid gap-3">
            {stats.map((stat) => (
              <div
                key={stat.label}
                className={cn(
                  "rounded-2xl border px-4 py-3",
                  getToneClassName(stat.tone),
                )}
              >
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  {stat.label}
                </p>
                <p className="mt-2 text-base font-semibold tracking-tight">
                  {stat.value}
                </p>
              </div>
            ))}
          </div>
        ) : null}
        {children}
      </CardContent>
    </Card>
  );
}
