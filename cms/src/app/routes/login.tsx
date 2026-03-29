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
import { LoginForm } from "@/features/auth/components/login-form";

export function LoginRoute() {
  const location = useLocation();

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
                  TellPal CMS
                </p>
                <h2 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
                  Editorial operations, one secure workspace
                </h2>
                <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
                  This workspace gives editors controlled access to content,
                  categories, assets, processing jobs, contributors, and
                  free-access rules.
                </CardDescription>
              </div>
            </CardHeader>
            <CardContent className="grid gap-4 text-sm text-muted-foreground sm:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  Session model
                </h2>
                <ul className="space-y-2">
                  <li className="flex items-start gap-2">
                    <ShieldCheck className="mt-0.5 size-4 shrink-0 text-primary" />
                    Access token stays memory-only on the client.
                  </li>
                  <li className="flex items-start gap-2">
                    <RefreshCcw className="mt-0.5 size-4 shrink-0 text-primary" />
                    Refresh restores the session during the next app boot.
                  </li>
                  <li className="flex items-start gap-2">
                    <LockKeyhole className="mt-0.5 size-4 shrink-0 text-primary" />
                    Unauthorized requests clear session state centrally.
                  </li>
                </ul>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  Current route target
                </h2>
                <p className="leading-6">
                  After a successful sign-in the app will continue to{" "}
                  <code>{targetPath}</code>. Return-to-target navigation is
                  preserved for protected routes.
                </p>
              </div>
            </CardContent>
          </Card>

          <LoginForm targetPath={targetPath} />
        </div>
      </div>
    </main>
  );
}
