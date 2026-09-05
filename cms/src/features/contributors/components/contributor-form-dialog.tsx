import { useState } from "react";
import { Controller } from "react-hook-form";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import { toastMutation, useZodForm } from "@/components/forms/form-utils";
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
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { localizeContributorProblem } from "@/features/contributors/lib/contributor-problems";
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
import { useI18n } from "@/i18n/locale-provider";

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

function getDialogCopy(
  mode: "create" | "rename",
  translate: (key: string) => string,
) {
  if (mode === "create") {
    return {
      title: translate("contributors.form.createTitle"),
      description: translate("contributors.form.createDescription"),
      submitLabel: translate("contributors.form.createSubmit"),
      pendingLabel: translate("contributors.form.createPending"),
      loading: translate("contributors.form.createPending"),
      success: translate("contributors.form.createSuccess"),
    };
  }

  return {
    title: translate("contributors.form.editTitle"),
    description: translate("contributors.form.editDescription"),
    submitLabel: translate("contributors.form.editSubmit"),
    pendingLabel: translate("contributors.form.editPending"),
    loading: translate("contributors.form.editPending"),
    success: translate("contributors.form.editSuccess"),
  };
}

function translateValidationError(
  message: string | undefined,
  translate: (key: string) => string,
) {
  switch (message) {
    case "Display name is required.":
      return translate("contributors.form.validation.displayNameRequired");
    case "Display name must be 120 characters or fewer.":
      return translate("contributors.form.validation.displayNameTooLong");
    case "Select at least one role.":
      return translate("contributors.form.validation.rolesRequired");
    default:
      return message;
  }
}

export function ContributorFormDialog(props: ContributorFormDialogProps) {
  const { t } = useI18n();
  const copy = getDialogCopy(props.mode, (key) => t(key as never));
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
          setProblem(localizeContributorProblem(error.problem, t));
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: t("contributors.form.genericError"),
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

        <DialogBody>
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
                {t("contributors.form.displayName")}
              </label>
              <Input
                id="contributor-display-name"
                placeholder={t("contributors.form.displayNamePlaceholder")}
                {...form.register("displayName")}
                disabled={contributorActions.isPending}
              />
              <p className="text-sm text-muted-foreground">
                {t("contributors.form.displayNameHint")}
              </p>
              <FieldError
                error={translateValidationError(
                  form.formState.errors.displayName?.message,
                  (key) => t(key as never),
                )}
              />
            </div>

            <fieldset className="space-y-3">
              <legend className="text-sm font-medium text-foreground">
                {t("contributors.form.roles")}
              </legend>
              <div className="grid gap-2 sm:grid-cols-2">
                {(
                  ["AUTHOR", "ILLUSTRATOR", "NARRATOR", "MUSICIAN"] as const
                ).map((role) => (
                  <Controller
                    key={role}
                    name="roles"
                    control={form.control}
                    render={({ field }) => (
                      <label className="flex min-h-11 items-center gap-2 rounded-md border border-border/70 px-3 text-sm">
                        <input
                          type="checkbox"
                          className="size-4 accent-primary"
                          checked={field.value.includes(role)}
                          disabled={contributorActions.isPending}
                          onChange={(event) =>
                            field.onChange(
                              event.target.checked
                                ? [...field.value, role]
                                : field.value.filter((item) => item !== role),
                            )
                          }
                        />
                        {t(`contributors.role.${role.toLowerCase()}` as never)}
                      </label>
                    )}
                  />
                ))}
              </div>
              <FieldError
                error={translateValidationError(
                  form.formState.errors.roles?.message,
                  (key) => t(key as never),
                )}
              />
            </fieldset>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={contributorActions.isPending}
              >
                {t("contributors.form.cancel")}
              </Button>
              <SubmitButton
                isPending={contributorActions.isPending}
                pendingLabel={copy.pendingLabel}
              >
                {copy.submitLabel}
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
