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
import { supportedCmsLanguageOptions } from "@/lib/languages";
import type { LanguageBadgeTone } from "@/components/language/language-badge";

type ContentLocalizationTabsProps = {
  content: ContentReadViewModel;
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

function getLocalizationDescription(
  localization: ContentLocalizationViewModel,
) {
  const parts = [localization.statusLabel, localization.processingStatusLabel];

  if (localization.visibleToMobile) {
    parts.push("Mobile visible");
  }

  if (localization.hasAudioAsset) {
    parts.push("Audio attached");
  } else if (localization.hasCoverAsset) {
    parts.push("Cover attached");
  }

  return parts.join(" / ");
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
  onActiveLanguageChange,
}: ContentLocalizationTabsProps) {
  const [activeLanguage, setActiveLanguage] = useState(
    content.localizations[0]?.languageCode ?? "",
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
  const defaultCreateLanguageCode = availableLanguages[0]?.code ?? "en";
  const localizationTabs: LanguageTabItem[] = useMemo(
    () =>
      content.localizations.map((localization) => ({
        code: localization.languageCode,
        label: localization.languageLabel,
        tone: getLocalizationTone(localization),
        meta: localization.statusLabel,
        description: getLocalizationDescription(localization),
      })),
    [content.localizations],
  );
  const resolvedActiveLanguage =
    localizationTabs.find((item) => item.code === activeLanguage)?.code ??
    localizationTabs[0]?.code ??
    "";

  useEffect(() => {
    if (resolvedActiveLanguage) {
      onActiveLanguageChange?.(resolvedActiveLanguage);
    }
  }, [onActiveLanguageChange, resolvedActiveLanguage]);

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
              <Button
                type="button"
                onClick={() => setIsCreateDialogOpen(true)}
              >
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
      {availableLanguages.length > 0 ? (
        <div className="flex justify-end">
          <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
            <CirclePlus className="size-4" />
            Add localization
          </Button>
        </div>
      ) : null}

      <LanguageTabs
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
