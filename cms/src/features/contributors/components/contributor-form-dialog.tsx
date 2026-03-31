import { useState } from "react";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import { toastMutation, useZodForm } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import {
  contributorFormSchema,
  getCreateContributorFormDefaults,
  mapContributorToFormValues,
  type ContributorFormValues,
} from "@/features/contributors/schema/contributor-schema";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type ContributorFormDialogProps =
  | {
      mode: "create";
      open: boolean;
      onOpenChange: (open: boolean) => void;
    }
  | {
      mode: "rename";
      open: boolean;
      onOpenChange: (open: boolean) => void;
      contributor: ContributorViewModel;
    };

function getDialogCopy(mode: "create" | "rename") {
  if (mode === "create") {
    return {
      title: "Create contributor",
      description:
        "Register a new contributor in the shared editorial registry. Assignment to specific content records lands in the next contributor task.",
      submitLabel: "Create contributor",
      pendingLabel: "Creating contributor...",
      loading: "Creating contributor...",
      success: "Contributor created.",
    };
  }

  return {
    title: "Rename contributor",
    description:
      "Update the shared display name shown across contributor pickers and future credit assignment workflows.",
    submitLabel: "Save rename",
    pendingLabel: "Saving rename...",
    loading: "Saving contributor rename...",
    success: "Contributor renamed.",
  };
}

export function ContributorFormDialog(props: ContributorFormDialogProps) {
  const copy = getDialogCopy(props.mode);
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const form = useZodForm<ContributorFormValues>({
    schema: contributorFormSchema,
    defaultValues:
      props.mode === "create"
        ? getCreateContributorFormDefaults()
        : mapContributorToFormValues(props.contributor),
  });
  const contributorActions = useContributorActions({
    onCreateSuccess: () => {
      handleOpenChange(false);
    },
    onRenameSuccess: () => {
      handleOpenChange(false);
    },
  });

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.clearErrors();
      setProblem(null);
    }

    props.onOpenChange(nextOpen);
  }

  async function handleSubmit(values: ContributorFormValues) {
    form.clearErrors();
    setProblem(null);

    try {
      if (props.mode === "create") {
        await toastMutation(
          contributorActions.createContributor.mutateAsync(values),
          {
            loading: copy.loading,
            success: copy.success,
          },
        );
      } else {
        await toastMutation(
          contributorActions.renameContributor.mutateAsync({
            contributorId: props.contributor.id,
            values,
          }),
          {
            loading: copy.loading,
            success: copy.success,
          },
        );
      }
    } catch (error) {
      if (error instanceof ApiClientError) {
        const fieldErrors = getProblemFieldErrors(error.problem);

        if (Object.keys(fieldErrors).length > 0) {
          for (const [field, message] of Object.entries(fieldErrors)) {
            form.setError(field as keyof ContributorFormValues, {
              type: "server",
              message,
            });
          }
        } else {
          setProblem(error.problem);
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: "Contributor changes could not be saved. Try again.",
      });
    }
  }

  return (
    <Dialog open={props.open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{copy.title}</DialogTitle>
          <DialogDescription>{copy.description}</DialogDescription>
        </DialogHeader>

        <form
          className="grid gap-5"
          noValidate
          onSubmit={form.handleSubmit(handleSubmit)}
        >
          {problem ? <ProblemAlert problem={problem} /> : null}
          <FieldError error={form.formState.errors.root?.serverError} />

          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="contributor-display-name"
            >
              Display name
            </label>
            <Input
              id="contributor-display-name"
              placeholder="Annie Case"
              {...form.register("displayName")}
              disabled={contributorActions.isPending}
            />
            <p className="text-sm text-muted-foreground">
              Names are trimmed before submit and appear in the shared
              contributor registry immediately after save.
            </p>
            <FieldError error={form.formState.errors.displayName} />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={contributorActions.isPending}
            >
              Cancel
            </Button>
            <SubmitButton
              isPending={contributorActions.isPending}
              pendingLabel={copy.pendingLabel}
            >
              {copy.submitLabel}
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
