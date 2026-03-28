import { useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { enableScaffoldSession } from "@/app/scaffold-session";
import { cmsShieldIcon as ShieldIcon } from "@/app/navigation";

export function LoginRoute() {
  const navigate = useNavigate();
  const location = useLocation();

  const targetPath = useMemo(() => {
    const state = location.state as { from?: string } | null;
    return state?.from || "/contents";
  }, [location.state]);

  function handleEnterWorkspace() {
    enableScaffoldSession();
    navigate(targetPath, { replace: true });
  }

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
                <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
                  Login route is ready
                </h1>
                <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
                  This is the route skeleton login screen. Real authentication
                  will replace the temporary scaffold session in the next task.
                </CardDescription>
              </div>
            </CardHeader>
            <CardContent className="grid gap-4 text-sm text-muted-foreground sm:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  This route already proves
                </h2>
                <ul className="space-y-2">
                  <li>Protected routes redirect to login</li>
                  <li>Active router setup is working</li>
                  <li>Return-to-target navigation is supported</li>
                </ul>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold text-foreground">
                  What changes in M02
                </h2>
                <ul className="space-y-2">
                  <li>Real backend login request</li>
                  <li>Refresh token lifecycle</li>
                  <li>Persistent session bootstrap</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
            <CardHeader>
              <h2 className="font-heading text-xl font-medium">
                Temporary route access
              </h2>
              <CardDescription>
                Use this only to inspect the shell and route skeleton before the
                real auth flow exists.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Button
                className="w-full"
                type="button"
                onClick={handleEnterWorkspace}
              >
                Enter scaffold workspace
              </Button>
              <p className="text-sm text-muted-foreground">
                After sign-in, you will land on <code>{targetPath}</code>.
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  );
}
