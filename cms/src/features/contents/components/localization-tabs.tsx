import { CirclePlus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import {
  LanguageTabs,
  type LanguageTabItem,
} from "@/components/language/language-tabs";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ContentLocalizationForm } from "@/features/contents/components/content-localization-form";
import { PublicationActions } from "@/features/contents/components/publication-actions";
import type {
  ContentLocalizationViewModel,
  ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
import {
  getCreateLocalizationFormDefaults,
  mapLocalizationToFormValues,
} from "@/features/contents/schema/content-localization-schema";
import { useI18n } from "@/i18n/locale-provider";
import { supportedCmsLanguageOptions } from "@/lib/languages";
import type { LanguageBadgeTone } from "@/components/language/language-badge";

type ContentLocalizationTabsProps = {
  content: ContentReadViewModel;
  initialLanguageCode?: string | null;
  onActiveLanguageChange?: (languageCode: string) => void;
};

function getLocalizationTone(
  localization: ContentLocalizationViewModel,
): LanguageBadgeTone {
  if (localization.isArchived) {
    return "muted";
  }

  if (localization.processingStatus === "FAILED") {
    return "destructive";
  }

  if (localization.isPublished && localization.isProcessingComplete) {
    return "success";
  }

  if (
    localization.status === "DRAFT" ||
    localization.processingStatus === "PENDING" ||
    localization.processingStatus === "PROCESSING"
  ) {
    return "warning";
  }

  return "info";
}

function LocalizationWorkspacePane({
  content,
  localization,
}: {
  content: ContentReadViewModel;
  localization: ContentLocalizationViewModel;
}) {
  const initialValues = useMemo(
    () => mapLocalizationToFormValues(localization),
    [localization],
  );
  const localizationFormKey = [
    localization.languageCode,
    localization.title,
    localization.description ?? "",
    localization.bodyText ?? "",
    localization.coverAssetId ?? "",
    localization.audioAssetId ?? "",
    localization.durationMinutes ?? "",
    localization.status,
    localization.processingStatus,
    localization.publishedAt ?? "",
    localization.narration?.audioAssetId ?? "",
    localization.narration?.durationMinutes ?? "",
    localization.narration?.processingStatus ?? "",
    localization.narration?.processingError ?? "",
  ].join("|");

  return (
    <div className="grid gap-4">
      <ContentLocalizationForm
        key={localizationFormKey}
        content={content}
        initialValues={initialValues}
        localization={localization}
        mode="update"
      />

      <PublicationActions content={content} localization={localization} />
    </div>
  );
}

export function ContentLocalizationTabs({
  content,
  initialLanguageCode,
  onActiveLanguageChange,
}: ContentLocalizationTabsProps) {
  const { locale } = useI18n();
  const normalizedInitialLanguageCode =
    supportedCmsLanguageOptions.find(
      (option) => option.code === initialLanguageCode?.toLowerCase(),
    )?.code ?? null;
  const [activeLanguage, setActiveLanguage] = useState(
    normalizedInitialLanguageCode ??
      content.localizations[0]?.languageCode ??
      "",
  );
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const existingLanguageCodes = useMemo(
    () => new Set(content.localizations.map((entry) => entry.languageCode)),
    [content.localizations],
  );
  const availableLanguages = useMemo(
    () =>
      supportedCmsLanguageOptions.filter(
        (option) => !existingLanguageCodes.has(option.code),
      ),
    [existingLanguageCodes],
  );
  const preferredCreateLanguageCode = normalizedInitialLanguageCode
    ? availableLanguages.find(
        (option) => option.code === normalizedInitialLanguageCode,
      )?.code
    : undefined;
  const defaultCreateLanguageCode =
    preferredCreateLanguageCode ?? availableLanguages[0]?.code ?? "en";
  const requestedLanguageIsMissing = Boolean(
    normalizedInitialLanguageCode &&
      !existingLanguageCodes.has(normalizedInitialLanguageCode),
  );
  const localizationTabs: LanguageTabItem[] = useMemo(
    () =>
      content.localizations.map((localization) => ({
        code: localization.languageCode,
        label: localization.languageLabel,
        tone: getLocalizationTone(localization),
        meta: localization.statusLabel,
      })),
    [content.localizations],
  );
  const resolvedActiveLanguage =
    localizationTabs.find((item) => item.code === activeLanguage)?.code ??
    localizationTabs[0]?.code ??
    "";

  useEffect(() => {
    if (normalizedInitialLanguageCode) {
      // Keep the local tab selection aligned with the URL-driven detail route.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActiveLanguage(normalizedInitialLanguageCode);
    }
  }, [normalizedInitialLanguageCode]);

  useEffect(() => {
    if (resolvedActiveLanguage && !requestedLanguageIsMissing) {
      onActiveLanguageChange?.(resolvedActiveLanguage);
    }
  }, [
    onActiveLanguageChange,
    requestedLanguageIsMissing,
    resolvedActiveLanguage,
  ]);

  function handleActiveLanguageChange(languageCode: string) {
    setActiveLanguage(languageCode);
    onActiveLanguageChange?.(languageCode);
  }

  if (content.localizations.length === 0) {
    return (
      <>
        <EmptyState
          action={
            availableLanguages.length > 0 ? (
              <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
                <CirclePlus className="size-4" />
                Create first localization
              </Button>
            ) : null
          }
          description="Add the first locale to unlock metadata editing, publication controls, and per-language processing visibility."
          title="No localizations yet"
        />

        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogContent className="sm:max-w-3xl">
            <DialogHeader>
              <DialogTitle>Create localization</DialogTitle>
              <DialogDescription>
                Create the first language workspace for this content record.
              </DialogDescription>
            </DialogHeader>

            <DialogBody>
              <ContentLocalizationForm
                key={defaultCreateLanguageCode}
                availableLanguages={availableLanguages}
                content={content}
                initialValues={getCreateLocalizationFormDefaults(
                  defaultCreateLanguageCode,
                )}
                mode="create"
                onCancel={() => setIsCreateDialogOpen(false)}
                onSuccess={(savedLocalization) => {
                  setIsCreateDialogOpen(false);
                  setActiveLanguage(
                    savedLocalization.languageCode.toLowerCase(),
                  );
                }}
              />
            </DialogBody>
          </DialogContent>
        </Dialog>
      </>
    );
  }

  return (
    <>
      {requestedLanguageIsMissing ? (
        <div
          className="flex flex-col gap-3 rounded-xl border border-amber-200 bg-amber-50 px-3 py-3 text-sm text-amber-950 sm:flex-row sm:items-center sm:justify-between"
          role="status"
        >
          <span>
            {normalizedInitialLanguageCode?.toUpperCase()} {locale === "tr"
              ? "bu içerik için henüz mevcut değil."
              : "is not available for this content yet."}
          </span>
          {availableLanguages.length > 0 ? (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => setIsCreateDialogOpen(true)}
            >
              {locale === "tr"
                ? `${normalizedInitialLanguageCode?.toUpperCase()} yerelleştirmesi oluştur`
                : `Create ${normalizedInitialLanguageCode?.toUpperCase()} localization`}
            </Button>
          ) : null}
        </div>
      ) : null}
      {availableLanguages.length > 0 ? (
        <div className="flex justify-end">
          <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
            <CirclePlus className="size-4" />
            Add localization
          </Button>
        </div>
      ) : null}

      <LanguageTabs
        compact
        items={localizationTabs}
        listLabel="Content localization tabs"
        onValueChange={handleActiveLanguageChange}
        renderContent={(item) => {
          const localization = content.localizations.find(
            (entry) => entry.languageCode === item.code,
          );

          if (!localization) {
            return null;
          }

          return (
            <LocalizationWorkspacePane
              content={content}
              localization={localization}
            />
          );
        }}
        value={resolvedActiveLanguage}
      />

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Add localization</DialogTitle>
            <DialogDescription>
              Create another language workspace for this content record. The new
              locale is added to the current detail cache after save.
            </DialogDescription>
          </DialogHeader>

          <DialogBody>
            <ContentLocalizationForm
              key={defaultCreateLanguageCode}
              availableLanguages={availableLanguages}
              content={content}
              initialValues={getCreateLocalizationFormDefaults(
                defaultCreateLanguageCode,
              )}
              mode="create"
              onCancel={() => setIsCreateDialogOpen(false)}
              onSuccess={(savedLocalization) => {
                setIsCreateDialogOpen(false);
                setActiveLanguage(savedLocalization.languageCode.toLowerCase());
              }}
            />
          </DialogBody>
        </DialogContent>
      </Dialog>
    </>
  );
}
