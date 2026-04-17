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
import { useFreeAccessActions } from "@/features/free-access/mutations/use-free-access-actions";
import type { FreeAccessGrantViewModel } from "@/features/free-access/model/free-access-view-model";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type RevokeFreeAccessButtonProps = {
  entry: FreeAccessGrantViewModel;
};

export function RevokeFreeAccessButton({
  entry,
}: RevokeFreeAccessButtonProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          trigger: "Revoke",
          title: "Free access grant'i kaldir",
          description:
            "Bu tam grant silinecek. Ayni access key icin diger localization veya content kayitlari korunur.",
          loading: "Grant kaldiriliyor...",
          pending: "Kaldiriliyor...",
          success: "Free access grant kaldirildi.",
          cancel: "Cancel",
          confirm: "Revoke grant",
        }
      : {
          trigger: "Revoke",
          title: "Revoke free access grant",
          description:
            "This exact grant will be removed. Other content or localization records under the same access key remain untouched.",
          loading: "Revoking free access grant...",
          pending: "Revoking...",
          success: "Free access grant revoked.",
          cancel: "Cancel",
          confirm: "Revoke grant",
        };
  const [open, setOpen] = useState(false);
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const freeAccessActions = useFreeAccessActions();

  async function handleRevoke() {
    setProblem(null);

    try {
      await toastMutation(
        freeAccessActions.revokeFreeAccess.mutateAsync({
          accessKey: entry.accessKey,
          languageCode: entry.languageCode,
          contentId: entry.contentId,
        }),
        {
          loading: copy.loading,
          success: copy.success,
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
            : "The free access grant could not be revoked.",
      });
    }
  }

  return (
    <>
      <Button type="button" variant="ghost" size="sm" onClick={() => setOpen(true)}>
        {copy.trigger}
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{copy.title}</DialogTitle>
            <DialogDescription>{copy.description}</DialogDescription>
          </DialogHeader>

          <DialogBody className="space-y-4">
            {problem ? <ProblemAlert problem={problem} /> : null}

            <div className="rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm">
              <p className="font-medium text-foreground">
                {entry.accessKey} / #{entry.contentId}
              </p>
              <p className="mt-1 text-muted-foreground">
                {entry.languageLabel} ({entry.languageCode.toUpperCase()})
              </p>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={freeAccessActions.revokeFreeAccess.isPending}
            >
              {copy.cancel}
            </Button>
            <SubmitButton
              variant="destructive"
              isPending={freeAccessActions.revokeFreeAccess.isPending}
              pendingLabel={copy.pending}
              onClick={() => void handleRevoke()}
              type="button"
            >
              {copy.confirm}
            </SubmitButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
