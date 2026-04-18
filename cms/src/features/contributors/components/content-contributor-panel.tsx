import { CirclePlus, ExternalLink } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import { AssignContributorDialog } from "@/features/contributors/components/assign-contributor-dialog";
import { UnassignContributorButton } from "@/features/contributors/components/unassign-contributor-button";
import { useContentContributorAssignments } from "@/features/contributors/queries/use-content-contributor-assignments";

type ContentContributorPanelProps = {
  content: ContentReadViewModel;
};

export function ContentContributorPanel({
  content,
}: ContentContributorPanelProps) {
  const [isAssignDialogOpen, setIsAssignDialogOpen] = useState(false);
  const assignmentQuery = useContentContributorAssignments(content.summary.id);
  const assignments = assignmentQuery.assignments;
  const assignmentCountLabel =
    assignments.length === 1
      ? "1 assignment"
      : `${assignments.length} assignments`;

  return (
    <>
      <div className="grid gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border/70 bg-muted/15 px-4 py-3">
          <div>
            <p className="text-sm font-medium text-foreground">
              Contributor credits
            </p>
            <p className="text-sm text-muted-foreground">
              {assignments.length > 0
                ? assignmentCountLabel
                : "Add registry contributors to this content record."}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button type="button" onClick={() => setIsAssignDialogOpen(true)}>
              <CirclePlus className="size-4" />
              Assign contributor
            </Button>
            <Button asChild type="button" variant="outline">
              <Link to="/contributors">
                <ExternalLink className="size-4" />
                Open registry
              </Link>
            </Button>
          </div>
        </div>

        {assignmentQuery.problem ? (
          <ProblemAlert problem={assignmentQuery.problem} />
        ) : assignmentQuery.isLoading ? (
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-8 text-sm text-muted-foreground">
            Loading contributor assignments from the admin API...
          </div>
        ) : assignments.length === 0 ? (
          <EmptyState
            action={
              <Button
                type="button"
                onClick={() => setIsAssignDialogOpen(true)}
              >
                <CirclePlus className="size-4" />
                Assign first contributor
              </Button>
            }
            description="Use the assignment dialog to add global or localized author, illustrator, narrator, or musician credits."
            title="No contributor assignments yet"
          />
        ) : (
          <div className="grid gap-3">
            {assignments.map((assignment) => (
              <div
                key={`${assignment.contributorId}-${assignment.role}-${assignment.languageCode ?? "global"}`}
                className="rounded-2xl border border-border/70 bg-background px-4 py-4"
              >
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div className="space-y-1">
                    <p className="text-sm font-medium leading-6 text-foreground">
                      {assignment.effectiveCreditName}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {assignment.displayName}
                    </p>
                  </div>
                  <div className="flex flex-col gap-3 lg:items-end">
                    <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                      <span className="inline-flex items-center rounded-full border border-border/70 bg-muted/15 px-3 py-1">
                        {assignment.roleLabel}
                      </span>
                      <span className="inline-flex items-center rounded-full border border-border/70 bg-muted/15 px-3 py-1">
                        {assignment.languageLabel}
                      </span>
                      <span className="inline-flex items-center rounded-full border border-border/70 bg-muted/15 px-3 py-1">
                        Sort {assignment.sortOrder}
                      </span>
                    </div>
                    <div>
                      <UnassignContributorButton assignment={assignment} />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {isAssignDialogOpen ? (
        <AssignContributorDialog
          content={content}
          existingAssignments={assignments}
          open={isAssignDialogOpen}
          onOpenChange={setIsAssignDialogOpen}
        />
      ) : null}
    </>
  );
}
