import { LoaderCircle, ShieldCheck } from "lucide-react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { useI18n } from "@/i18n/locale-provider";

type AuthLoadingScreenProps = {
  title?: string;
  description?: string;
};

export function AuthLoadingScreen({
  title,
  description,
}: AuthLoadingScreenProps) {
  const { t } = useI18n();
  const resolvedTitle = title ?? t("auth.loading.title");
  const resolvedDescription = description ?? t("auth.loading.description");

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(241,245,249,0.96),_rgba(248,250,252,1)_45%,_rgba(255,255,255,1)_100%)] px-6 py-16 text-foreground">
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-3xl items-center justify-center">
        <Card className="w-full border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
          <CardHeader className="gap-4">
            <div className="inline-flex size-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
              <ShieldCheck className="size-5" />
            </div>
            <div className="space-y-2">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                {t("layout.brand")}
              </p>
              <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
                {resolvedTitle}
              </h1>
              <CardDescription className="text-sm leading-6 sm:text-base">
                {resolvedDescription}
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-3 rounded-2xl border border-border/70 bg-muted/40 px-4 py-4 text-sm text-muted-foreground">
              <LoaderCircle className="size-4 animate-spin text-primary" />
              <span>{t("auth.loading.wait")}</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
