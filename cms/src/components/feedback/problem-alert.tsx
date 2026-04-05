import { CircleAlert } from "lucide-react";
import type { ReactNode } from "react";

import {
  getProblemFieldErrors,
  getProblemMessage,
} from "@/lib/http/problem-details";
import { useI18n } from "@/i18n/locale-provider";
import { cn } from "@/lib/utils";
import type { ApiProblemDetail } from "@/types/api";

type ProblemAlertProps = {
  problem?: ApiProblemDetail | null;
  title?: string;
  description?: string;
  actions?: ReactNode;
  className?: string;
};

export function ProblemAlert({
  problem,
  title,
  description,
  actions,
  className,
}: ProblemAlertProps) {
  const { t } = useI18n();
  const resolvedTitle = problem?.title || title;
  const resolvedDescription = problem
    ? getProblemMessage(problem)
    : description;

  if (!resolvedTitle && !resolvedDescription) {
    return null;
  }

  const fieldErrors = Object.entries(getProblemFieldErrors(problem));

  return (
    <div
      className={cn(
        "flex gap-3 rounded-2xl border border-destructive/25 bg-destructive/8 px-4 py-4 text-sm text-destructive shadow-sm",
        className,
      )}
      role="alert"
    >
      <CircleAlert className="mt-0.5 size-5 shrink-0" />

      <div className="min-w-0 flex-1 space-y-2">
        {resolvedTitle ? (
          <p className="font-semibold tracking-tight">{resolvedTitle}</p>
        ) : null}

        {resolvedDescription &&
        (!resolvedTitle || resolvedDescription !== resolvedTitle) ? (
          <p className="leading-6 text-destructive/90">{resolvedDescription}</p>
        ) : null}

        {fieldErrors.length > 0 ? (
          <ul className="space-y-1 text-destructive/90">
            {fieldErrors.map(([field, message]) => (
              <li key={`${field}-${message}`}>
                <span className="font-medium">{field}:</span> {message}
              </li>
            ))}
          </ul>
        ) : null}

        {problem?.requestId ? (
          <p className="text-xs uppercase tracking-[0.18em] text-destructive/70">
            {t("app.requestId")} {problem.requestId}
          </p>
        ) : null}
      </div>

      {actions ? <div className="shrink-0">{actions}</div> : null}
    </div>
  );
}
