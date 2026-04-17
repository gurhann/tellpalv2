import type { ReactNode } from "react";

import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type MockupStatusTone = "default" | "success" | "warning" | "accent" | "info";

type MockupStatusPillProps = {
  children: ReactNode;
  tone?: MockupStatusTone;
  className?: string;
};

type MockupMetricCardProps = {
  label: string;
  value: string;
  detail?: string;
  tone?: MockupStatusTone;
};

type MockupInfoCardProps = {
  title: string;
  description?: string;
  children?: ReactNode;
  className?: string;
};

function getToneClassName(tone: MockupStatusTone = "default") {
  switch (tone) {
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-800";
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-800";
    case "accent":
      return "border-sky-200 bg-sky-50 text-sky-800";
    case "info":
      return "border-sky-200 bg-sky-50 text-sky-800";
    case "default":
    default:
      return "border-border/70 bg-background text-foreground";
  }
}

export function MockupStatusPill({
  children,
  tone = "default",
  className,
}: MockupStatusPillProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold tracking-tight",
        getToneClassName(tone),
        className,
      )}
    >
      {children}
    </span>
  );
}

export function MockupMetricCard({
  label,
  value,
  detail,
  tone = "default",
}: MockupMetricCardProps) {
  return (
    <div className={cn("rounded-[1.35rem] border px-4 py-3", getToneClassName(tone))}>
      <p className="text-[0.7rem] font-semibold uppercase tracking-[0.2em] text-muted-foreground">
        {label}
      </p>
      <p className="mt-2 text-base font-semibold tracking-tight">{value}</p>
      {detail ? (
        <p className="mt-1 text-sm leading-6 text-muted-foreground">{detail}</p>
      ) : null}
    </div>
  );
}

export function MockupInfoCard({
  title,
  description,
  children,
  className,
}: MockupInfoCardProps) {
  return (
    <Card
      className={cn(
        "border border-border/70 bg-[linear-gradient(180deg,_rgba(255,255,255,0.98),_rgba(246,243,236,0.9))] shadow-lg shadow-slate-950/5",
        className,
      )}
    >
      <CardHeader className="gap-2 border-b border-border/60 bg-background/60">
        <h3 className="font-heading text-base font-semibold tracking-tight text-foreground">
          {title}
        </h3>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>
      {children ? <CardContent className="grid gap-3 pt-4">{children}</CardContent> : null}
    </Card>
  );
}

export function MockupKeyValueGrid({
  items,
}: {
  items: { label: string; value: string; tone?: MockupStatusTone }[];
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      {items.map((item) => (
        <div
          key={item.label}
          className={cn(
            "rounded-[1.2rem] border px-4 py-3",
            getToneClassName(item.tone),
          )}
        >
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
            {item.label}
          </p>
          <p className="mt-2 text-sm font-medium leading-6 text-foreground">{item.value}</p>
        </div>
      ))}
    </div>
  );
}
