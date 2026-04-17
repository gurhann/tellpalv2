import type { ComponentPropsWithoutRef, ReactNode } from "react";

import { cn } from "@/lib/utils";

type FilterBarProps = ComponentPropsWithoutRef<"section"> & {
  children: ReactNode;
};

type FilterBarGroupProps = ComponentPropsWithoutRef<"div"> & {
  children: ReactNode;
};

type FilterBarSummaryProps = ComponentPropsWithoutRef<"div"> & {
  title?: string;
  description?: string;
  children?: ReactNode;
};

export function FilterBar({ children, className, ...props }: FilterBarProps) {
  return (
    <section
      className={cn(
        "rounded-[1.75rem] border border-border/70 bg-muted/20 px-4 py-4 shadow-sm shadow-slate-950/5",
        className,
      )}
      {...props}
    >
      <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        {children}
      </div>
    </section>
  );
}

export function FilterBarGroup({
  children,
  className,
  ...props
}: FilterBarGroupProps) {
  return (
    <div
      className={cn(
        "flex min-w-0 flex-1 flex-wrap items-center gap-3",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function FilterBarActions({
  children,
  className,
  ...props
}: FilterBarGroupProps) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-3 xl:justify-end",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function FilterBarSummary({
  title,
  description,
  children,
  className,
  ...props
}: FilterBarSummaryProps) {
  return (
    <div className={cn("min-w-0 space-y-1", className)} {...props}>
      {title ? (
        <p className="text-sm font-medium tracking-tight text-foreground">
          {title}
        </p>
      ) : null}
      {description ? (
        <p className="text-sm leading-6 text-muted-foreground">{description}</p>
      ) : null}
      {children}
    </div>
  );
}
