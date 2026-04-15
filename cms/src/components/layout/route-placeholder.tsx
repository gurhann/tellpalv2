import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { useI18n } from "@/i18n/locale-provider";

type RoutePlaceholderProps = {
  eyebrow: string;
  title: string;
  description: string;
  highlights: string[];
};

export function RoutePlaceholder({
  eyebrow,
  title,
  description,
}: RoutePlaceholderProps) {
  const { t } = useI18n();

  return (
    <Card className="border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
      <CardHeader className="gap-3">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          {eyebrow}
        </p>
        <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
          {title}
        </h1>
        <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
          {description}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="rounded-3xl border border-dashed border-border/80 bg-muted/30 p-5">
          <p className="text-sm font-medium text-foreground">
            {t("placeholder.minimalTitle")}
          </p>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            {t("placeholder.minimalDescription")}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
