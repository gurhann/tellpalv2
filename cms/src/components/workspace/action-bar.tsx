import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

type ActionBarProps = {
  title?: string;
  description?: string;
  children: ReactNode;
  meta?: ReactNode;
  className?: string;
};

export function ActionBar({
  title,
  description,
  children,
  meta,
  className,
}: ActionBarProps) {
  return (
    <section
      className={cn(
        "rounded-[1.75rem] border border-border/70 bg-muted/20 px-4 py-4 shadow-sm shadow-slate-950/5",
        className,
      )}
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 space-y-1.5">
          {title ? (
            <p className="text-sm font-semibold tracking-tight text-foreground">
              {title}
            </p>
          ) : null}
          {description ? (
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
              {description}
            </p>
          ) : null}
        </div>
        {meta ? <div className="shrink-0">{meta}</div> : null}
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-3">{children}</div>
    </section>
  );
}
