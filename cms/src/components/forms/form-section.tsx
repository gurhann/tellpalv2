import { type ReactNode, useId } from "react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type FormSectionProps = {
  title: string;
  description?: string;
  actions?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
};

export function FormSection({
  title,
  description,
  actions,
  footer,
  children,
  className,
  contentClassName,
}: FormSectionProps) {
  const titleId = useId();

  return (
    <section aria-labelledby={titleId}>
      <Card
        className={cn(
          "border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5",
          className,
        )}
      >
        <CardHeader className="gap-4 border-b border-border/60 bg-muted/10 pb-4 sm:flex sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-2">
            <h2
              id={titleId}
              className="font-heading text-lg font-semibold tracking-tight"
            >
              {title}
            </h2>
            {description ? (
              <CardDescription className="max-w-3xl text-sm leading-6">
                {description}
              </CardDescription>
            ) : null}
          </div>

          {actions ? <div className="shrink-0">{actions}</div> : null}
        </CardHeader>

        <CardContent className={cn("grid gap-5 pt-5", contentClassName)}>
          {children}
        </CardContent>

        {footer ? (
          <div className="border-t border-border/60 bg-muted/30 px-4 py-4">
            {footer}
          </div>
        ) : null}
      </Card>
    </section>
  );
}
