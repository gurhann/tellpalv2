import { useMemo, useState } from "react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import type { ContributorRole } from "@/features/contributors/api/contributor-admin";
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { localizeContributorProblem } from "@/features/contributors/lib/contributor-problems";
import {
  contributorRoleOptions,
  editContentContributorFormSchema,
  type EditContentContributorFormValues,
} from "@/features/contributors/schema/content-contributor-schema";
import { ApiClientError } from "@/lib/http/client";
import { resolveLanguageLabel } from "@/lib/languages";
import type { ApiProblemDetail } from "@/types/api";
import { useI18n } from "@/i18n/locale-provider";

type Props = {
  assignment: ContentContributorViewModel;
  content: ContentReadViewModel;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const GLOBAL_SCOPE_SELECT_VALUE = "__global__";

export function EditContentContributorDialog({
  assignment,
  content,
  open,
  onOpenChange,
}: Props) {
  const { locale, t } = useI18n();
  const actions = useContributorActions();
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const languageOptions = useMemo(
    () =>
      content.localizations.map((localization) => ({
        value: localization.languageCode,
        label: resolveLanguageLabel(localization.languageCode, locale),
      })),
    [content.localizations, locale],
  );
  const form = useZodForm<EditContentContributorFormValues>({
    schema: editContentContributorFormSchema,
    defaultValues: {
      role: assignment.role,
      languageCode: assignment.languageCode,
      creditName: assignment.creditName ?? "",
    },
  });
  const languageValue = form.watch("languageCode");
  const supportedRoles = assignment.contributorRoles ?? [assignment.role];
  const roleOptions = contributorRoleOptions.filter((option) =>
    supportedRoles.includes(option.value),
  );
  const currentRoleIsSupported = supportedRoles.includes(assignment.role);

  function close(nextOpen: boolean) {
    if (!nextOpen) {
      setProblem(null);
      form.reset({
        role: assignment.role,
        languageCode: assignment.languageCode,
        creditName: assignment.creditName ?? "",
      });
    }
    onOpenChange(nextOpen);
  }

  async function submit(values: EditContentContributorFormValues) {
    setProblem(null);
    try {
      await toastMutation(
        actions.updateContributor.mutateAsync({
          contentId: assignment.contentId,
          assignmentId: assignment.assignmentId,
          values: {
            role: values.role,
            languageCode: values.languageCode,
            creditName: values.creditName.trim() || null,
          },
        }),
        {
          loading: t("contributors.edit.pending"),
          success: t("contributors.edit.success"),
        },
      );
      close(false);
    } catch (error) {
      setProblem(
        error instanceof ApiClientError
          ? localizeContributorProblem(error.problem, t)
          : {
              type: "about:blank",
              title: t("contributors.edit.errorTitle"),
              status: 500,
              detail:
                error instanceof Error
                  ? error.message
                  : t("contributors.edit.error"),
            },
      );
    }
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("contributors.edit.title")}</DialogTitle>
          <DialogDescription>
            {t("contributors.edit.description")}
          </DialogDescription>
        </DialogHeader>
        <DialogBody>
          <form
            className="grid gap-4"
            onSubmit={form.handleSubmit(submit)}
            noValidate
          >
            <div className="rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm">
              <p className="font-medium text-foreground">
                {assignment.effectiveCreditName}
              </p>
              <p className="mt-1 text-muted-foreground">
                {assignment.displayName}
              </p>
              <p className="mt-2 text-xs text-muted-foreground">
                {t("contributors.edit.identityHint")}
              </p>
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                htmlFor="edit-contributor-role"
              >
                {t("contributors.edit.role")}
              </label>
              {currentRoleIsSupported ? (
                <Controller
                  control={form.control}
                  name="role"
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={(value) =>
                        field.onChange(value as ContributorRole)
                      }
                    >
                      <SelectTrigger id="edit-contributor-role">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {roleOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {t(
                              `contributors.role.${option.value.toLowerCase()}` as never,
                            )}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              ) : (
                <output
                  id="edit-contributor-role"
                  className="rounded-xl border border-border/70 bg-muted/20 px-3 py-2 text-sm text-muted-foreground"
                  aria-readonly="true"
                >
                  {t(
                    `contributors.role.${assignment.role.toLowerCase()}` as never,
                  )}
                </output>
              )}
              <FieldError error={form.formState.errors.role} />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                htmlFor="edit-contributor-scope"
              >
                {t("contributors.edit.scope")}
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
                    <SelectTrigger id="edit-contributor-scope">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={GLOBAL_SCOPE_SELECT_VALUE}>
                        {t("contributors.picker.allLanguages")}
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
              <FieldError error={form.formState.errors.languageCode} />
              <p className="text-xs text-muted-foreground">
                {(languageValue
                  ? languageOptions.find(
                      (option) => option.value === languageValue,
                    )?.label
                  : t("contributors.picker.allLanguages")) ??
                  t("contributors.picker.allLanguages")}
                · {t("contributors.picker.scopeHint")}
              </p>
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                htmlFor="edit-contributor-credit-name"
              >
                {t("contributors.edit.creditName")}
              </label>
              <Input
                id="edit-contributor-credit-name"
                {...form.register("creditName")}
              />
              <FieldError error={form.formState.errors.creditName} />
            </div>

            {problem ? <ProblemAlert problem={problem} /> : null}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => close(false)}
                disabled={actions.updateContributor.isPending}
              >
                {t("contributors.edit.cancel")}
              </Button>
              <SubmitButton
                isPending={actions.updateContributor.isPending}
                pendingLabel={t("contributors.edit.pending")}
              >
                {t("contributors.edit.save")}
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
