import { AlertTriangle } from "lucide-react";

type MissingActionsNoteProps = {
  actionLabel: string;
  dependencyCode: string;
  description: string;
};

export function MissingActionsNote({
  actionLabel,
  dependencyCode,
  description,
}: MissingActionsNoteProps) {
  return (
    <div className="rounded-2xl border border-amber-300/60 bg-amber-50/80 px-4 py-3 text-sm text-amber-950 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-amber-100 text-amber-700">
          <AlertTriangle className="size-4" />
        </div>
        <div className="space-y-1">
          <p className="font-medium text-amber-950">
            {actionLabel} unavailable until {dependencyCode}
          </p>
          <p className="leading-6 text-amber-900/80">{description}</p>
        </div>
      </div>
    </div>
  );
}
