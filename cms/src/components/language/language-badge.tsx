import { cn } from "@/lib/utils";

export type LanguageBadgeTone =
  | "default"
  | "muted"
  | "success"
  | "warning"
  | "destructive"
  | "info";

type LanguageBadgeProps = {
  code: string;
  label?: string;
  tone?: LanguageBadgeTone;
  compact?: boolean;
  meta?: string;
  className?: string;
};

const languageLabels: Record<string, string> = {
  tr: "Turkish",
  en: "English",
  es: "Spanish",
  pt: "Portuguese",
  de: "German",
};

function getToneClassName(tone: LanguageBadgeTone) {
  switch (tone) {
    case "muted":
      return "border-border/70 bg-muted text-muted-foreground";
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-700";
    case "destructive":
      return "border-destructive/20 bg-destructive/8 text-destructive";
    case "info":
      return "border-sky-200 bg-sky-50 text-sky-700";
    case "default":
    default:
      return "border-primary/15 bg-primary/8 text-primary";
  }
}

function resolveLanguageLabel(code: string) {
  return languageLabels[code.toLowerCase()] ?? code.toUpperCase();
}

export function LanguageBadge({
  code,
  label,
  tone = "default",
  compact = false,
  meta,
  className,
}: LanguageBadgeProps) {
  const resolvedLabel = label ?? resolveLanguageLabel(code);

  return (
    <span
      className={cn(
        "inline-flex items-center gap-2 rounded-full border px-2.5 py-1 text-xs font-medium tracking-tight",
        getToneClassName(tone),
        compact && "gap-1.5 px-2 py-0.5",
        className,
      )}
    >
      <span className="font-semibold uppercase tracking-[0.18em]">{code}</span>
      <span className={cn("truncate", compact && "sr-only")}>
        {resolvedLabel}
      </span>
      {meta ? (
        <span
          className={cn(
            "rounded-full bg-background/70 px-1.5 py-0.5 text-[0.7rem] font-medium",
            compact && "hidden",
          )}
        >
          {meta}
        </span>
      ) : null}
    </span>
  );
}
