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
import { useI18n } from "@/i18n/locale-provider";
import { resolveLanguageLabel } from "@/lib/languages";

type UnassignContributorButtonProps = {
  assignment: ContentContributorViewModel;
};

export function UnassignContributorButton({
  assignment,
}: UnassignContributorButtonProps) {
  const [open, setOpen] = useState(false);
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const contributorActions = useContributorActions();
  const { locale, t } = useI18n();

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
          loading: t("contributors.unassign.loading"),
          success: t("contributors.unassign.success"),
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
            : t("contributors.unassign.error"),
      });
    }
  }

  return (
    <>
      <Button
        aria-label={t("contributors.unassign.aria", {
          name: assignment.effectiveCreditName,
        })}
        type="button"
        variant="ghost"
        size="sm"
        onClick={() => setOpen(true)}
      >
        {t("contributors.unassign.action")}
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
            <DialogTitle>{t("contributors.unassign.title")}</DialogTitle>
            <DialogDescription>
              {t("contributors.unassign.description")}
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
                {t("contributors.unassign.scope", {
                  role: t(
                    `contributors.role.${assignment.role.toLowerCase()}` as never,
                  ),
                  language: assignment.languageCode
                    ? resolveLanguageLabel(assignment.languageCode, locale)
                    : t("contributors.picker.allLanguages"),
                  sort: assignment.sortOrder,
                })}
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
              {t("contributors.unassign.cancel")}
            </Button>
            <SubmitButton
              type="button"
              variant="destructive"
              isPending={contributorActions.unassignContributor.isPending}
              pendingLabel={t("contributors.unassign.pending")}
              onClick={() => void handleUnassign()}
            >
              {t("contributors.unassign.confirm")}
            </SubmitButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
