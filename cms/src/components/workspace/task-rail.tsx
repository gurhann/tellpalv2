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
  tone?: "default" | "success" | "warning" | "accent";
};

type TaskRailProps = {
  title: string;
  description?: string;
  stats?: TaskRailStat[];
  children?: ReactNode;
  className?: string;
  variant?: "default" | "detail";
};

function getToneClassName(tone: TaskRailStat["tone"] = "default") {
  switch (tone) {
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-900";
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-900";
    case "accent":
      return "border-sky-200 bg-sky-50 text-sky-900";
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
  variant = "default",
}: TaskRailProps) {
  const isDetail = variant === "detail";
  const visibleStats = isDetail ? stats.slice(0, 3) : stats;

  return (
    <Card
      className={cn(
        "border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5",
        isDetail && "shadow-md shadow-slate-950/5",
        className,
      )}
    >
      <CardHeader
        className={cn(
          "gap-2 border-b border-border/60",
          isDetail ? "px-4 py-4" : undefined,
        )}
      >
        <h2
          className={cn(
            "font-heading font-semibold tracking-tight text-foreground",
            isDetail ? "text-base" : "text-lg",
          )}
        >
          {title}
        </h2>
        {description ? (
          <CardDescription className={cn(isDetail ? "text-xs leading-5" : "text-sm leading-6")}>
            {description}
          </CardDescription>
        ) : null}
      </CardHeader>
      <CardContent className={cn("grid pt-5", isDetail ? "gap-3 px-4 pb-4" : "gap-4")}>
        {visibleStats.length > 0 ? (
          <div className="grid gap-3">
            {visibleStats.map((stat) => (
              <div
                key={stat.label}
                className={cn(
                  "border",
                  isDetail ? "rounded-xl px-3 py-2.5" : "rounded-2xl px-4 py-3",
                  getToneClassName(stat.tone),
                )}
              >
                <p
                  className={cn(
                    "font-semibold uppercase tracking-[0.18em] text-muted-foreground",
                    isDetail ? "text-[0.65rem]" : "text-xs",
                  )}
                >
                  {stat.label}
                </p>
                <p
                  className={cn(
                    "font-semibold tracking-tight",
                    isDetail ? "mt-1.5 text-sm" : "mt-2 text-base",
                  )}
                >
                  {stat.value}
                </p>
              </div>
            ))}
          </div>
        ) : null}
        {!isDetail ? children : null}
      </CardContent>
    </Card>
  );
}
