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
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type ContributorDeleteDialogProps = {
  contributor: ContributorViewModel | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function ContributorDeleteDialog({
  contributor,
  open,
  onOpenChange,
}: ContributorDeleteDialogProps) {
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const contributorActions = useContributorActions();

  async function handleDelete() {
    if (!contributor) {
      return;
    }

    setProblem(null);

    try {
      await toastMutation(
        contributorActions.deleteContributor.mutateAsync({
          contributorId: contributor.id,
        }),
        {
          loading: "Deleting contributor...",
          success: "Contributor deleted.",
          error: (error) => {
            if (
              error instanceof ApiClientError &&
              error.problem.errorCode === "contributor_in_use"
            ) {
              return "Remove the contributor from content assignments first.";
            }

            return "The contributor could not be deleted.";
          },
        },
      );

      onOpenChange(false);
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
            : "The contributor could not be deleted.",
      });
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          setProblem(null);
        }
        onOpenChange(nextOpen);
      }}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Delete contributor</DialogTitle>
          <DialogDescription>
            This removes the shared contributor record. Content assignments must
            be cleared first.
          </DialogDescription>
        </DialogHeader>

        <DialogBody className="space-y-4">
          {problem ? <ProblemAlert problem={problem} /> : null}

          {contributor ? (
            <div className="rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm">
              <p className="font-medium text-foreground">
                {contributor.displayName}
              </p>
              <p className="mt-1 text-muted-foreground">
                Contributor #{contributor.id}
              </p>
            </div>
          ) : null}
        </DialogBody>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={contributorActions.deleteContributor.isPending}
          >
            Cancel
          </Button>
          <SubmitButton
            type="button"
            variant="destructive"
            isPending={contributorActions.deleteContributor.isPending}
            pendingLabel="Deleting..."
            onClick={() => void handleDelete()}
          >
            Delete contributor
          </SubmitButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
