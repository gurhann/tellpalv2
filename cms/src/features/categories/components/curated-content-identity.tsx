import { cn } from "@/lib/utils";

type CuratedContentIdentityProps = {
  contentId: number;
  externalKey: string;
  languageLabel: string;
  localizedTitle: string | null;
  className?: string;
  metaClassName?: string;
};

export function CuratedContentIdentity({
  contentId,
  externalKey,
  languageLabel,
  localizedTitle,
  className,
  metaClassName,
}: CuratedContentIdentityProps) {
  const resolvedTitle =
    localizedTitle?.trim() || externalKey || `Content #${contentId}`;
  const metadata = [`#${contentId}`, externalKey, languageLabel].join(
    " \u00b7 ",
  );

  return (
    <div className={cn("space-y-1", className)}>
      <p className="font-medium text-foreground">{resolvedTitle}</p>
      <p className={cn("text-xs text-muted-foreground", metaClassName)}>
        {metadata}
      </p>
    </div>
  );
}
