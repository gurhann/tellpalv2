import { useDeferredValue, useMemo, useState } from "react";
import { Controller } from "react-hook-form";
import { EmptyState } from "@/components/feedback/empty-state";
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
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import type { ContributorRole } from "@/features/contributors/api/contributor-admin";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { useContributorPicker } from "@/features/contributors/queries/use-contributors";
import {
  contentContributorFormSchema,
  getAssignContributorFormDefaults,
  validateLocalContentContributorAssignment,
  type ContentContributorFormValues,
} from "@/features/contributors/schema/content-contributor-schema";
import { ApiClientError } from "@/lib/http/client";
import { getProblemMessage } from "@/lib/http/problem-details";
import { useI18n } from "@/i18n/locale-provider";

type Props = {
  content: ContentReadViewModel;
  existingAssignments: ContentContributorViewModel[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  role?: ContributorRole;
};

const GLOBAL_SCOPE_SELECT_VALUE = "__global__";

export function AssignContributorDialog({
  content,
  existingAssignments,
  open,
  onOpenChange,
  role = "AUTHOR",
}: Props) {
  const { t } = useI18n();
  const roleLabel = t(`contributors.role.${role.toLowerCase()}` as never);
  const [search, setSearch] = useState("");
  const [problemMessage, setProblemMessage] = useState<string | null>(null);
  const [created, setCreated] = useState<{
    id: number;
    displayName: string;
  } | null>(null);
  const [selectedContributor, setSelectedContributor] = useState<{
    id: number;
    displayName: string;
  } | null>(null);
  const [retryValues, setRetryValues] =
    useState<ContentContributorFormValues | null>(null);
  const deferredSearch = useDeferredValue(search);
  const query = useContributorPicker({
    role,
    query: deferredSearch,
    enabled: open,
  });
  const actions = useContributorActions();
  const languageOptions = content.localizations.map((l) => ({
    value: l.languageCode,
    label: l.languageLabel,
  }));
  const initialValues = useMemo(
    () => ({
      ...getAssignContributorFormDefaults(),
      role,
      languageCode:
        role === "NARRATOR" ? (languageOptions[0]?.value ?? null) : null,
    }),
    [role, content.localizations],
  );
  const form = useZodForm<ContentContributorFormValues>({
    schema: contentContributorFormSchema,
    defaultValues: initialValues,
  });
  const values = form.watch();
  const selected =
    query.contributors.find((c) => c.id === values.contributorId) ??
    selectedContributor ??
    created;
  function close(next: boolean) {
    if (!next) {
      form.reset(initialValues);
      setSearch("");
      setProblemMessage(null);
      setCreated(null);
      setSelectedContributor(null);
      setRetryValues(null);
    }
    onOpenChange(next);
  }
  async function assign(v: ContentContributorFormValues) {
    setProblemMessage(null);
    try {
      await toastMutation(
        actions.assignContributor.mutateAsync({
          contentId: content.summary.id,
          values: {
            contributorId: v.contributorId,
            role: v.role,
            languageCode: v.languageCode,
            creditName: v.creditName.trim() || null,
          },
        }),
        {
          loading: t("contributors.picker.assignPending"),
          success: t("contributors.picker.assignSuccess"),
        },
      );
      setRetryValues(null);
      close(false);
    } catch (e) {
      setRetryValues(v);
      setProblemMessage(
        e instanceof ApiClientError
          ? getProblemMessage(e.problem)
          : t("contributors.picker.assignError"),
      );
    }
  }
  async function submit(v: ContentContributorFormValues) {
    form.clearErrors();
    const local = validateLocalContentContributorAssignment(
      v,
      existingAssignments,
    );
    if (local) {
      form.setError(local.field, { type: "manual", message: local.message });
      return;
    }
    await assign(v);
  }
  async function createAndAssign() {
    const displayName = search.trim();
    if (!displayName) {
      form.setError("contributorId", {
        type: "manual",
        message: t("contributors.picker.nameRequired"),
      });
      return;
    }
    try {
      const contributor = await actions.createContributor.mutateAsync({
        displayName,
        roles: [role],
      });
      const next = {
        ...values,
        contributorId: contributor.contributorId,
        role,
      };
      setCreated({
        id: contributor.contributorId,
        displayName: contributor.displayName,
      });
      form.setValue("contributorId", contributor.contributorId);
      await assign(next);
    } catch (e) {
      if (e instanceof ApiClientError && e.problem.status === 409) {
        const id = Number(e.problem.existingContributorId);
        if (id > 0) {
          form.setValue("contributorId", id);
          setCreated({ id, displayName });
          setSelectedContributor({ id, displayName });
          setProblemMessage(t("contributors.picker.duplicateUseExisting"));
          return;
        }
      }
      setProblemMessage(
        e instanceof ApiClientError
          ? getProblemMessage(e.problem)
          : t("contributors.picker.createError"),
      );
    }
  }
  const hasResults = query.contributors.length > 0;
  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("contributors.picker.title")}</DialogTitle>
          <DialogDescription>
            {t("contributors.picker.description", {
              role: roleLabel,
            })}
          </DialogDescription>
        </DialogHeader>
        <DialogBody>
          <div className="grid gap-5">
            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                htmlFor="content-contributor-search"
              >
                {t("contributors.picker.searchLabel", { role: roleLabel })}
              </label>
              <Input
                id="content-contributor-search"
                aria-label={t("contributors.picker.searchLabel", {
                  role: roleLabel,
                })}
                placeholder={t("contributors.picker.searchPlaceholder")}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                disabled={actions.isPending}
              />
            </div>
            {query.problem ? (
              <ProblemAlert problem={query.problem} />
            ) : query.isLoading ? (
              <div className="rounded-2xl border px-4 py-8 text-sm text-muted-foreground">
                {t("contributors.picker.loading")}
              </div>
            ) : !hasResults ? (
              <EmptyState
                title={t("contributors.picker.emptyTitle")}
                description={t("contributors.picker.emptyDescription", {
                  name: search.trim(),
                  role: roleLabel,
                })}
                action={
                  search.trim() ? (
                    <Button
                      type="button"
                      onClick={createAndAssign}
                      disabled={actions.isPending}
                    >
                      {t("contributors.picker.createAndAssign")}
                    </Button>
                  ) : undefined
                }
              />
            ) : (
              <>
                <div
                  className="grid gap-2"
                  role="listbox"
                  aria-label={t("contributors.picker.resultsLabel")}
                >
                  {query.contributors.map((c) => (
                    <button
                      type="button"
                      role="option"
                      aria-selected={values.contributorId === c.id}
                      key={c.id}
                      onClick={() => {
                        form.setValue("contributorId", c.id);
                        setSelectedContributor({
                          id: c.id,
                          displayName: c.displayName,
                        });
                      }}
                      className="flex min-h-11 items-center justify-between rounded-lg border border-border/70 px-3 text-left"
                    >
                      <span className="font-medium">{c.displayName}</span>
                      <span className="text-xs text-muted-foreground">
                        {roleLabel}
                      </span>
                    </button>
                  ))}
                </div>
                {selected ? (
                  <form
                    className="grid gap-4"
                    onSubmit={form.handleSubmit(submit)}
                    noValidate
                  >
                    <p className="text-sm text-muted-foreground">
                      {selected.displayName}
                    </p>
                    <FieldError error={form.formState.errors.contributorId} />
                    <div className="space-y-2">
                      <label
                        className="text-sm font-medium"
                        htmlFor="content-contributor-scope"
                      >
                        {t("contributors.picker.scope")}
                      </label>
                      <Controller
                        control={form.control}
                        name="languageCode"
                        render={({ field }) => (
                          <Select
                            value={field.value ?? GLOBAL_SCOPE_SELECT_VALUE}
                            onValueChange={(value) =>
                              field.onChange(
                                value === GLOBAL_SCOPE_SELECT_VALUE
                                  ? null
                                  : value,
                              )
                            }
                          >
                            <SelectTrigger
                              id="content-contributor-scope"
                              aria-label={t("contributors.picker.scope")}
                            >
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value={GLOBAL_SCOPE_SELECT_VALUE}>
                                {t("contributors.picker.allLanguages")}
                              </SelectItem>
                              {languageOptions.map((option) => (
                                <SelectItem
                                  key={option.value}
                                  value={option.value}
                                >
                                  {option.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                    </div>
                    <label
                      className="text-sm font-medium"
                      htmlFor="content-contributor-credit-name"
                    >
                      {t("contributors.picker.creditName")}
                    </label>
                    <Input
                      id="content-contributor-credit-name"
                      placeholder={t(
                        "contributors.picker.creditNamePlaceholder",
                      )}
                      {...form.register("creditName")}
                    />
                    <p className="text-sm text-muted-foreground">
                      {(values.languageCode
                        ? languageOptions.find(
                            (option) => option.value === values.languageCode,
                          )?.label
                        : t("contributors.picker.allLanguages")) ??
                        t("contributors.picker.allLanguages")}{" "}
                      · {t("contributors.picker.scopeHint")}
                    </p>
                    <DialogFooter>
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => close(false)}
                      >
                        {t("contributors.picker.cancel")}
                      </Button>
                      <SubmitButton
                        isPending={actions.assignContributor.isPending}
                        pendingLabel={t("contributors.picker.assignPending")}
                      >
                        {t("contributors.picker.assign")}
                      </SubmitButton>
                    </DialogFooter>
                  </form>
                ) : null}
              </>
            )}
            {problemMessage ? (
              <ProblemAlert
                description={problemMessage}
                title={t("contributors.picker.errorTitle")}
              />
            ) : null}
            {retryValues ? (
              <Button
                type="button"
                variant="outline"
                onClick={() => assign(retryValues)}
                disabled={actions.assignContributor.isPending}
              >
                {t("contributors.picker.retry")}
              </Button>
            ) : null}
          </div>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
