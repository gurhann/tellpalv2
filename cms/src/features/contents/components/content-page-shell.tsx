import type { ReactNode } from "react";

import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type ContentPageShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  toolbar?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  aside?: ReactNode;
  className?: string;
};

export function ContentPageShell({
  eyebrow,
  title,
  description,
  toolbar,
  actions,
  children,
  aside,
  className,
}: ContentPageShellProps) {
  return (
    <div
      className={cn("grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]", className)}
    >
      <Card className="border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5 xl:col-span-2">
        <CardHeader className="gap-4 border-b border-border/60 bg-gradient-to-br from-background via-background to-muted/30 sm:flex sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              {eyebrow}
            </p>
            <div className="space-y-2">
              <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
                {title}
              </h1>
              <CardDescription className="max-w-3xl text-sm leading-6 sm:text-base">
                {description}
              </CardDescription>
            </div>
          </div>

          {actions ? (
            <CardAction className="col-auto row-auto mt-1 w-full justify-self-stretch sm:w-auto">
              <div className="flex flex-wrap items-center justify-start gap-2 sm:justify-end">
                {actions}
              </div>
            </CardAction>
          ) : null}
        </CardHeader>

        {toolbar ? (
          <CardContent className="border-t border-border/40 bg-muted/10 py-5">
            {toolbar}
          </CardContent>
        ) : null}
      </Card>

      <div className={cn("space-y-6", !aside && "xl:col-span-2")}>
        {children}
      </div>
      {aside ? <div className="space-y-6">{aside}</div> : null}
    </div>
  );
}
