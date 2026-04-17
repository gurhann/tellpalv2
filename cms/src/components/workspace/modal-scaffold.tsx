import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

type ModalScaffoldProps = {
  header?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
  bodyClassName?: string;
  className?: string;
};

export function ModalScaffold({
  header,
  footer,
  children,
  bodyClassName,
  className,
}: ModalScaffoldProps) {
  return (
    <div
      className={cn(
        "grid min-h-0 grid-rows-[auto_minmax(0,1fr)_auto]",
        className,
      )}
    >
      {header}
      <div className={cn("min-h-0 overflow-y-auto px-5 pb-5", bodyClassName)}>
        {children}
      </div>
      {footer}
    </div>
  );
}
