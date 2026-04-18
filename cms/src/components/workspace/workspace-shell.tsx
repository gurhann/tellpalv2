import type { ReactNode } from "react";

import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type WorkspaceShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
  toolbar?: ReactNode;
  aside?: ReactNode;
  children: ReactNode;
  className?: string;
};

export function WorkspaceShell({
  eyebrow,
  title,
  description,
  actions,
  toolbar,
  aside,
  children,
  className,
}: WorkspaceShellProps) {
  return (
    <div
      className={cn(
        "grid gap-5 xl:grid-cols-[minmax(0,1fr)_18.5rem] xl:items-start",
        className,
      )}
    >
      <div className={cn("min-w-0 space-y-5", !aside && "xl:col-span-2")}>
        <Card className="border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
          <CardHeader className="gap-3 border-b border-border/60 bg-[radial-gradient(circle_at_top_left,_rgba(246,243,236,0.9),_rgba(255,255,255,0)_45%)] px-5 py-5 sm:flex sm:flex-row sm:items-start sm:justify-between sm:px-6">
            <div className="min-w-0 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-[0.28em] text-muted-foreground">
                {eyebrow}
              </p>
              <div className="space-y-1.5">
                <h1 className="font-heading text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
                  {title}
                </h1>
                <CardDescription className="max-w-3xl text-sm leading-6">
                  {description}
                </CardDescription>
              </div>
            </div>

            {actions ? (
              <CardAction className="col-auto row-auto mt-1 w-full justify-self-stretch sm:w-auto">
                <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                  {actions}
                </div>
              </CardAction>
            ) : null}
          </CardHeader>

          {toolbar ? (
            <CardContent className="bg-muted/10 px-5 py-4 sm:px-6">
              {toolbar}
            </CardContent>
          ) : null}
        </Card>

        <div className="space-y-5">{children}</div>
      </div>

      {aside ? (
        <aside className="space-y-6 xl:sticky xl:top-24">{aside}</aside>
      ) : null}
    </div>
  );
}
