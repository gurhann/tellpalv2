import { useMemo } from "react";
import { useLocation } from "react-router-dom";

import { LockKeyhole, RefreshCcw, ShieldCheck } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { cmsShieldIcon as ShieldIcon } from "@/app/navigation";
import { LocaleSwitcher } from "@/components/i18n/locale-switcher";
import { LoginForm } from "@/features/auth/components/login-form";
import { useI18n } from "@/i18n/locale-provider";

export function LoginRoute() {
  const location = useLocation();
  const { t } = useI18n();

  const targetPath = useMemo(() => {
    const state = location.state as { from?: string } | null;
    return state?.from || "/contents";
  }, [location.state]);

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(241,245,249,0.96),_rgba(248,250,252,1)_45%,_rgba(255,255,255,1)_100%)] px-6 py-16 text-foreground">
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-5xl items-center">
        <div className="grid w-full gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <Card className="border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
            <CardHeader className="gap-4">
              <div className="inline-flex size-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
                <ShieldIcon className="size-5" />
              </div>
              <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                  {t("layout.brand")}
                </p>
                <h2 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
                  {t("auth.login.heroTitle")}
                </h2>
                <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
                  {t("auth.login.heroDescription")}
                </CardDescription>
              </div>
            </CardHeader>
            <CardContent className="grid gap-4 text-sm text-muted-foreground sm:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  {t("auth.login.sessionModelTitle")}
                </h2>
                <ul className="space-y-2">
                  <li className="flex items-start gap-2">
                    <ShieldCheck className="mt-0.5 size-4 shrink-0 text-primary" />
                    {t("auth.login.sessionModel.accessToken")}
                  </li>
                  <li className="flex items-start gap-2">
                    <RefreshCcw className="mt-0.5 size-4 shrink-0 text-primary" />
                    {t("auth.login.sessionModel.refresh")}
                  </li>
                  <li className="flex items-start gap-2">
                    <LockKeyhole className="mt-0.5 size-4 shrink-0 text-primary" />
                    {t("auth.login.sessionModel.unauthorized")}
                  </li>
                </ul>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  {t("auth.login.routeTargetTitle")}
                </h2>
                <p className="leading-6">
                  {t("auth.login.routeTargetDescription", {
                    targetPath,
                  })}
                </p>
              </div>
            </CardContent>
          </Card>

          <div className="space-y-4">
            <div className="flex justify-end">
              <LocaleSwitcher className="w-44" />
            </div>
            <LoginForm targetPath={targetPath} />
          </div>
        </div>
      </div>
    </main>
  );
}
