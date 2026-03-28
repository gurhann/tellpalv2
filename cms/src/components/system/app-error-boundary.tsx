import * as React from "react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

type AppErrorBoundaryProps = {
  children: React.ReactNode;
};

type AppErrorBoundaryState = {
  error: Error | null;
};

export class AppErrorBoundary extends React.Component<
  AppErrorBoundaryProps,
  AppErrorBoundaryState
> {
  state: AppErrorBoundaryState = {
    error: null,
  };

  static getDerivedStateFromError(error: Error): AppErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("Unhandled application error", error, errorInfo);
  }

  handleTryAgain = () => {
    this.setState({ error: null });
  };

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
      <main className="min-h-screen bg-background px-6 py-16 text-foreground">
        <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-3xl items-center justify-center">
          <Card className="w-full border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
            <CardHeader>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                TellPal CMS
              </p>
              <CardTitle className="text-3xl">
                Unexpected application error
              </CardTitle>
              <CardDescription>
                The current view failed to render. You can retry the last render
                or reload the page to reset the full application state.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-muted-foreground">
                <p className="font-medium text-foreground">Error message</p>
                <p className="mt-2 break-words">{this.state.error.message}</p>
              </div>
            </CardContent>
            <CardFooter className="flex flex-wrap gap-3">
              <Button type="button" onClick={this.handleTryAgain}>
                Try render again
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => window.location.reload()}
              >
                Reload page
              </Button>
            </CardFooter>
          </Card>
        </div>
      </main>
    );
  }
}
