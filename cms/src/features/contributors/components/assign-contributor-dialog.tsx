import { Controller } from "react-hook-form";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
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
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { useContributors } from "@/features/contributors/queries/use-contributors";
import {
  contentContributorFormSchema,
  contributorRoleOptions,
  getAssignContributorFormDefaults,
  getContributorLanguageScopeLabel,
  validateLocalContentContributorAssignment,
  type ContentContributorFormValues,
} from "@/features/contributors/schema/content-contributor-schema";
import { ApiClientError } from "@/lib/http/client";
import { getProblemMessage } from "@/lib/http/problem-details";
import { useMemo, useState } from "react";

type AssignContributorDialogProps = {
  content: ContentReadViewModel;
  existingAssignments: ContentContributorViewModel[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const RECENT_CONTRIBUTOR_LIMIT = 12;
const GLOBAL_SCOPE_SELECT_VALUE = "__global__";

export function AssignContributorDialog({
  content,
  existingAssignments,
  open,
  onOpenChange,
}: AssignContributorDialogProps) {
  const [problemMessage, setProblemMessage] = useState<string | null>(null);
  const contributorListQuery = useContributors(RECENT_CONTRIBUTOR_LIMIT);
  const contributorActions = useContributorActions();
  const languageOptions = content.localizations.map((localization) => ({
    value: localization.languageCode,
    label: localization.languageLabel,
  }));
  const initialValues = useMemo(() => getAssignContributorFormDefaults(), []);
  const form = useZodForm<ContentContributorFormValues>({
    schema: contentContributorFormSchema,
    defaultValues: initialValues,
  });

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.reset(initialValues);
      form.clearErrors();
      setProblemMessage(null);
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: ContentContributorFormValues) {
    form.clearErrors();
    setProblemMessage(null);

    const localValidationError = validateLocalContentContributorAssignment(
      values,
      existingAssignments,
    );

    if (localValidationError) {
      form.setError(localValidationError.field, {
        type: "manual",
        message: localValidationError.message,
      });
      return;
    }

    try {
      await toastMutation(
        contributorActions.assignContributor.mutateAsync({
          contentId: content.summary.id,
          values: {
            contributorId: values.contributorId,
            role: values.role,
            languageCode: values.languageCode,
            creditName: values.creditName.trim() || null,
            sortOrder: values.sortOrder,
          },
        }),
        {
          loading: "Assigning contributor...",
          success: "Contributor assigned.",
        },
      );

      handleOpenChange(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        const mappedFieldError = applyProblemDetailToForm(
          form.setError,
          error.problem,
        );

        if (!mappedFieldError) {
          setProblemMessage(getProblemMessage(error.problem));
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: "Contributor assignment could not be saved. Try again.",
      });
    }
  }

  const hasContributors = contributorListQuery.contributors.length > 0;
  const languageScopeLabel = getContributorLanguageScopeLabel(
    form.watch("languageCode"),
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Assign contributor</DialogTitle>
          <DialogDescription>
            Add a contributor credit for this content item. The picker uses the
            recent contributor registry because the backend does not offer
            search yet.
          </DialogDescription>
        </DialogHeader>

        <DialogBody>
          {contributorListQuery.isLoading ? (
            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-8 text-sm text-muted-foreground">
              Loading recent contributors from the shared registry...
            </div>
          ) : !hasContributors ? (
            <EmptyState
              action={
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => handleOpenChange(false)}
                >
                  Close
                </Button>
              }
              description="Create a contributor in the shared registry first, then reopen this dialog."
              title="No contributors available"
            />
          ) : (
            <form
              className="grid gap-5"
              noValidate
              onSubmit={form.handleSubmit(handleSubmit)}
            >
            {problemMessage ? (
              <ProblemAlert
                description={problemMessage}
                title="Contributor assignment failed"
              />
            ) : null}
            <FieldError error={form.formState.errors.root?.serverError} />

            <div className="grid gap-5 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  Contributor
                </label>
                <Controller
                  control={form.control}
                  name="contributorId"
                  render={({ field }) => (
                    <Select
                      value={field.value > 0 ? field.value.toString() : ""}
                      onValueChange={(value) => field.onChange(Number(value))}
                    >
                      <SelectTrigger
                        aria-label="Contributor"
                        aria-invalid={Boolean(
                          form.formState.errors.contributorId,
                        )}
                        className="w-full"
                      >
                        <SelectValue placeholder="Select contributor" />
                      </SelectTrigger>
                      <SelectContent>
                        {contributorListQuery.contributors.map(
                          (contributor) => (
                            <SelectItem
                              key={contributor.id}
                              value={contributor.id.toString()}
                            >
                              {contributor.displayName}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  )}
                />
                <FieldError error={form.formState.errors.contributorId} />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  Role
                </label>
                <Controller
                  control={form.control}
                  name="role"
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger
                        aria-label="Contributor role"
                        aria-invalid={Boolean(form.formState.errors.role)}
                        className="w-full"
                      >
                        <SelectValue placeholder="Select role" />
                      </SelectTrigger>
                      <SelectContent>
                        {contributorRoleOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                <FieldError error={form.formState.errors.role} />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  Scope
                </label>
                <Controller
                  control={form.control}
                  name="languageCode"
                  render={({ field }) => (
                    <Select
                      value={field.value ?? GLOBAL_SCOPE_SELECT_VALUE}
                      onValueChange={(value) =>
                        field.onChange(
                          value === GLOBAL_SCOPE_SELECT_VALUE ? null : value,
                        )
                      }
                    >
                      <SelectTrigger
                        aria-label="Contributor scope"
                        aria-invalid={Boolean(
                          form.formState.errors.languageCode,
                        )}
                        className="w-full"
                      >
                        <SelectValue placeholder="Select scope" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={GLOBAL_SCOPE_SELECT_VALUE}>
                          All languages
                        </SelectItem>
                        {languageOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                <p className="text-sm text-muted-foreground">
                  {languageScopeLabel === GLOBAL_CONTRIBUTOR_LANGUAGE_LABEL
                    ? "All languages creates one localization-independent credit."
                    : `${languageScopeLabel} creates a localized contributor credit.`}
                </p>
                <FieldError error={form.formState.errors.languageCode} />
              </div>

              <div className="space-y-2">
                <label
                  className="text-sm font-medium text-foreground"
                  htmlFor="content-contributor-sort-order"
                >
                  Sort order
                </label>
                <Input
                  id="content-contributor-sort-order"
                  inputMode="numeric"
                  min={0}
                  type="number"
                  {...form.register("sortOrder", {
                    setValueAs: (value) => Number(value),
                  })}
                />
                <FieldError error={form.formState.errors.sortOrder} />
              </div>
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="content-contributor-credit-name"
              >
                Credit name
              </label>
              <Input
                id="content-contributor-credit-name"
                placeholder="Optional credit override"
                {...form.register("creditName")}
              />
              <p className="text-sm text-muted-foreground">
                Leave blank to use the contributor display name. Blank values
                are trimmed to `null` before submit.
              </p>
              <FieldError error={form.formState.errors.creditName} />
            </div>

            <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4 text-sm text-muted-foreground">
              Global credits are available even when this content has no
              localizations. Localized credits remain limited to the languages
              shown in the content detail view.
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={contributorActions.assignContributor.isPending}
              >
                Cancel
              </Button>
              <SubmitButton
                isPending={contributorActions.assignContributor.isPending}
                pendingLabel="Assigning contributor..."
              >
                Assign contributor
              </SubmitButton>
            </DialogFooter>
            </form>
          )}
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
