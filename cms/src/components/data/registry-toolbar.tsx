import type { ComponentPropsWithoutRef, ReactNode } from "react";

import {
  FilterBar,
  FilterBarSection,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { cn } from "@/lib/utils";

type RegistryToolbarProps = ComponentPropsWithoutRef<"section"> & {
  ariaLabel: string;
  search: ReactNode;
  filters: ReactNode;
  summaryTitle?: string;
  summaryDescription?: string;
  actions?: ReactNode;
};

type RegistryToolbarGroupProps = ComponentPropsWithoutRef<"div"> & {
  children: ReactNode;
  label: ReactNode;
};

export function RegistryToolbar({
  ariaLabel,
  search,
  filters,
  summaryTitle,
  summaryDescription,
  actions,
  className,
  ...props
}: RegistryToolbarProps) {
  return (
    <FilterBar
      aria-label={ariaLabel}
      className={cn("px-5 py-5", className)}
      {...props}
    >
      <div
        className="grid w-full gap-4 xl:grid-cols-[minmax(22rem,26rem)_minmax(0,1fr)]"
        data-slot="registry-toolbar-layout"
      >
        <div className="min-w-0" data-slot="registry-toolbar-search">
          {search}
        </div>

        <div className="min-w-0 space-y-4" data-slot="registry-toolbar-main">
          {actions ? (
            <div
              className="flex flex-wrap items-center justify-start gap-2 xl:justify-end"
              data-slot="registry-toolbar-actions"
            >
              {actions}
            </div>
          ) : null}

          <div
            className="flex flex-wrap items-start gap-4 xl:gap-6"
            data-slot="registry-toolbar-filters"
          >
            {filters}
          </div>

          {summaryTitle || summaryDescription ? (
            <div
              className="border-t border-border/60 pt-4"
              data-slot="registry-toolbar-summary"
            >
              <FilterBarSummary
                title={summaryTitle}
                description={summaryDescription}
              />
            </div>
          ) : null}
        </div>
      </div>
    </FilterBar>
  );
}

export function RegistryToolbarGroup({
  children,
  label,
  className,
  ...props
}: RegistryToolbarGroupProps) {
  return (
    <FilterBarSection className={className} label={label} {...props}>
      {children}
    </FilterBarSection>
  );
}
