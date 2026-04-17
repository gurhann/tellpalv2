import { useEffect, useMemo, useState } from "react";
import { Controller } from "react-hook-form";

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
import { useFreeAccessActions } from "@/features/free-access/mutations/use-free-access-actions";
import type { FreeAccessGrantViewModel } from "@/features/free-access/model/free-access-view-model";
import {
  getGrantFreeAccessFormDefaults,
  grantFreeAccessFormSchema,
  type GrantFreeAccessFormValues,
} from "@/features/free-access/schema/free-access-schema";
import { useI18n } from "@/i18n/locale-provider";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

type GrantFreeAccessDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contents: ContentReadViewModel[];
  contentProblem?: ApiProblemDetail | null;
  onGranted?: (grant: FreeAccessGrantViewModel) => void;
};

function getContentLabel(content: ContentReadViewModel) {
  return content.primaryLocalization?.title ?? content.summary.externalKey;
}

export function GrantFreeAccessDialog({
  open,
  onOpenChange,
  contents,
  contentProblem = null,
  onGranted,
}: GrantFreeAccessDialogProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          title: "Free access grant ver",
          description:
            "Bir access key, content ve mevcut localization secerek tam grant kaydi olusturun.",
          accessKey: "Access key",
          content: "Icerik",
          contentPlaceholder: "Icerik secin",
          language: "Dil",
          languagePlaceholder: "Dil secin",
          accessKeyHelp:
            "Bos submit normalize edilmez. Bu form access key degerini explicit gonderir.",
          languageHelp:
            "Dil secimi, secilen icerigin mevcut localization'lari ile sinirlidir.",
          noContent:
            "Grant olusturmak icin once content registry'nin yuklenmesi gerekir.",
          noLocalization:
            "Secilen icerikte localization yok. Bu nedenle grant icin dil secilemez.",
          loading: "Free access grant kaydediliyor...",
          pending: "Kaydediliyor...",
          success: "Free access grant olusturuldu.",
          action: "Grant free access",
          cancel: "Cancel",
        }
      : {
          title: "Grant free access",
          description:
            "Create one exact grant by choosing an access key, content, and one existing localization.",
          accessKey: "Access key",
          content: "Content",
          contentPlaceholder: "Select content",
          language: "Language",
          languagePlaceholder: "Select language",
          accessKeyHelp:
            "Blank submits are not normalized. This form always sends an explicit access key value.",
          languageHelp:
            "Language selection is limited to the existing localizations of the chosen content.",
          noContent:
            "The content registry must load before a grant can be created.",
          noLocalization:
            "The selected content has no localizations, so no grant language can be selected.",
          loading: "Saving free access grant...",
          pending: "Saving...",
          success: "Free access grant created.",
          action: "Grant free access",
          cancel: "Cancel",
        };
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const form = useZodForm<GrantFreeAccessFormValues>({
    schema: grantFreeAccessFormSchema,
    defaultValues: getGrantFreeAccessFormDefaults(),
  });
  const freeAccessActions = useFreeAccessActions();
  const selectedContentId = form.watch("contentId");
  const selectedContent = useMemo(
    () =>
      contents.find((content) => content.summary.id === selectedContentId) ??
      null,
    [contents, selectedContentId],
  );
  const languageOptions = selectedContent?.localizations ?? [];

  useEffect(() => {
    if (!open) {
      return;
    }

    form.reset(getGrantFreeAccessFormDefaults());
    setProblem(null);
  }, [form, open]);

  useEffect(() => {
    if (!selectedContent) {
      return;
    }

    const currentLanguageCode = form.getValues("languageCode");
    const languageStillExists = languageOptions.some(
      (localization) => localization.languageCode === currentLanguageCode,
    );

    if (!languageStillExists) {
      form.setValue("languageCode", languageOptions[0]?.languageCode ?? "", {
        shouldValidate: true,
      });
    }
  }, [form, languageOptions, selectedContent]);

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      form.clearErrors();
      setProblem(null);
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(values: GrantFreeAccessFormValues) {
    form.clearErrors();
    setProblem(null);

    if (values.contentId === null) {
      form.setError("contentId", {
        type: "manual",
        message: "Select a content record.",
      });
      return;
    }

    try {
      const grant = await toastMutation(
        freeAccessActions.grantFreeAccess.mutateAsync({
          accessKey: values.accessKey.trim(),
          contentId: values.contentId,
          languageCode: values.languageCode,
        }),
        {
          loading: copy.loading,
          success: copy.success,
        },
      );

      onGranted?.(grant);
      handleOpenChange(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        const mapped = applyProblemDetailToForm(form.setError, error.problem);

        if (!mapped) {
          setProblem(error.problem);
        }
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message:
          error instanceof Error && error.message.trim().length > 0
            ? error.message
            : copy.loading,
      });
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl">
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
            {contentProblem ? <ProblemAlert problem={contentProblem} /> : null}
            {problem ? <ProblemAlert problem={problem} /> : null}
            <FieldError error={form.formState.errors.root?.serverError} />

            <div className="grid gap-5 md:grid-cols-2">
              <div className="space-y-2 md:col-span-2">
                <label
                  className="text-sm font-medium text-foreground"
                  htmlFor="grant-free-access-key"
                >
                  {copy.accessKey}
                </label>
                <Input
                  id="grant-free-access-key"
                  {...form.register("accessKey")}
                  disabled={freeAccessActions.isPending}
                />
                <p className="text-sm text-muted-foreground">
                  {copy.accessKeyHelp}
                </p>
                <FieldError error={form.formState.errors.accessKey} />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  {copy.content}
                </label>
                <Controller
                  control={form.control}
                  name="contentId"
                  render={({ field }) => (
                    <Select
                      value={
                        typeof field.value === "number"
                          ? field.value.toString()
                          : ""
                      }
                      onValueChange={(value) => field.onChange(Number(value))}
                    >
                      <SelectTrigger className="w-full" aria-label={copy.content}>
                        <SelectValue placeholder={copy.contentPlaceholder} />
                      </SelectTrigger>
                      <SelectContent>
                        {contents.map((content) => (
                          <SelectItem
                            key={content.summary.id}
                            value={content.summary.id.toString()}
                          >
                            {getContentLabel(content)} /{" "}
                            {content.summary.externalKey}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                <p className="text-sm text-muted-foreground">
                  {contents.length === 0 ? copy.noContent : copy.languageHelp}
                </p>
                <FieldError error={form.formState.errors.contentId} />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">
                  {copy.language}
                </label>
                <Controller
                  control={form.control}
                  name="languageCode"
                  render={({ field }) => (
                    <Select
                      disabled={languageOptions.length === 0}
                      value={field.value}
                      onValueChange={field.onChange}
                    >
                      <SelectTrigger className="w-full" aria-label={copy.language}>
                        <SelectValue placeholder={copy.languagePlaceholder} />
                      </SelectTrigger>
                      <SelectContent>
                        {languageOptions.map((localization) => (
                          <SelectItem
                            key={localization.languageCode}
                            value={localization.languageCode}
                          >
                            {localization.languageLabel}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                <p className="text-sm text-muted-foreground">
                  {selectedContent && languageOptions.length === 0
                    ? copy.noLocalization
                    : copy.languageHelp}
                </p>
                <FieldError error={form.formState.errors.languageCode} />
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={freeAccessActions.isPending}
              >
                {copy.cancel}
              </Button>
              <SubmitButton
                isPending={freeAccessActions.isPending}
                pendingLabel={copy.pending}
              >
                {copy.action}
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
