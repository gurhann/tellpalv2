import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { hasScaffoldSession } from "@/app/scaffold-session";

export function NotFoundRoute() {
  const target = hasScaffoldSession() ? "/contents" : "/login";

  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-3xl items-center justify-center">
        <Card className="w-full border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              TellPal CMS
            </p>
            <CardTitle className="text-3xl">Route not found</CardTitle>
            <CardDescription>
              The requested CMS route does not exist in the current skeleton.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to={target}>Go back</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
