import type { ReactNode } from "react";

import { WorkspaceShell } from "@/components/workspace/workspace-shell";

type ContentPageShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  toolbar?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  aside?: ReactNode;
  className?: string;
  showHeader?: boolean;
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
  showHeader,
}: ContentPageShellProps) {
  return (
    <WorkspaceShell
      eyebrow={eyebrow}
      title={title}
      description={description}
      toolbar={toolbar}
      actions={actions}
      aside={aside}
      className={className}
      showHeader={showHeader}
    >
      {children}
    </WorkspaceShell>
  );
}
