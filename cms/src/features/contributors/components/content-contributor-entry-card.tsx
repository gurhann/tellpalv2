import { ArrowRight, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";

type ContentContributorEntryCardProps = {
  content: ContentReadViewModel | null;
};

export function ContentContributorEntryCard({
  content,
}: ContentContributorEntryCardProps) {
  return (
    <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
      <CardHeader>
        <CardTitle>Contributor Credits</CardTitle>
        <CardDescription>
          Open the shared contributor registry and reuse its records when
          assigning credits on content detail pages.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm text-muted-foreground">
          {content
            ? `Contributor records from the shared registry can be assigned to ${content.summary.externalKey} with role, language scope, credit name, and ordering metadata.`
            : "Open the shared contributor registry to manage the reusable contributor list."}
        </div>
        <Button asChild type="button" variant="outline">
          <Link to="/contributors">
            <UsersRound className="size-4" />
            Open contributor registry
            <ArrowRight className="size-4" />
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
