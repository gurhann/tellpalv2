import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { SubmitButton } from "@/components/forms/submit-button";
import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type UnassignContributorButtonProps = {
  assignment: ContentContributorViewModel;
};

export function UnassignContributorButton({
  assignment,
}: UnassignContributorButtonProps) {
  const [open, setOpen] = useState(false);
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const contributorActions = useContributorActions();

  async function handleUnassign() {
    setProblem(null);

    try {
      await toastMutation(
        contributorActions.unassignContributor.mutateAsync({
          contentId: assignment.contentId,
          values: {
            contributorId: assignment.contributorId,
            role: assignment.role,
            languageCode: assignment.languageCode,
          },
        }),
        {
          loading: "Removing contributor assignment...",
          success: "Contributor assignment removed.",
        },
      );

      setOpen(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        setProblem(error.problem);
        return;
      }

      setProblem({
        type: "about:blank",
        title: "Request failed",
        status: 500,
        detail:
          error instanceof Error
            ? error.message
            : "The contributor assignment could not be removed.",
      });
    }
  }

  return (
    <>
      <Button
        aria-label={`Unassign ${assignment.effectiveCreditName}`}
        type="button"
        variant="ghost"
        size="sm"
        onClick={() => setOpen(true)}
      >
        Unassign
      </Button>

      <Dialog
        open={open}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) {
            setProblem(null);
          }
          setOpen(nextOpen);
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Unassign contributor</DialogTitle>
            <DialogDescription>
              This removes only the selected role and language scope from the
              current content item.
            </DialogDescription>
          </DialogHeader>

          <DialogBody className="space-y-4">
            {problem ? <ProblemAlert problem={problem} /> : null}

            <div className="rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm">
              <p className="font-medium text-foreground">
                {assignment.effectiveCreditName}
              </p>
              <p className="mt-1 text-muted-foreground">
                {assignment.displayName}
              </p>
              <p className="mt-3 text-muted-foreground">
                {`${assignment.roleLabel} / ${assignment.languageLabel} / Sort ${assignment.sortOrder}`}
              </p>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={contributorActions.unassignContributor.isPending}
            >
              Cancel
            </Button>
            <SubmitButton
              type="button"
              variant="destructive"
              isPending={contributorActions.unassignContributor.isPending}
              pendingLabel="Removing..."
              onClick={() => void handleUnassign()}
            >
              Unassign contributor
            </SubmitButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
