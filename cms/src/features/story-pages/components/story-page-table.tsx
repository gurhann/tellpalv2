import { Languages, Pencil, Plus, Trash2 } from "lucide-react";
import type { ReactNode } from "react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type {
  StoryPageLocalizationViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type StoryPageTableProps = {
  storyPages: StoryPageReadViewModel[];
  availableLocalizations: {
    languageCode: string;
    languageLabel: string;
  }[];
  selectedLanguageCode: string | null;
  selectedLanguageLabel: string;
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onEditStoryPage?: (storyPage: StoryPageReadViewModel) => void;
  onAddAfterStoryPage?: (storyPage: StoryPageReadViewModel) => void;
  onDeleteStoryPage?: (storyPage: StoryPageReadViewModel) => void;
  emptyAction?: ReactNode;
  isMutationPending?: boolean;
};

type LocalizationState = {
  isReady: boolean;
  missingParts: string[];
};

function getLocalizationState(
  localization: StoryPageLocalizationViewModel | null,
): LocalizationState {
  if (!localization) {
    return {
      isReady: false,
      missingParts: ["body", "illustration", "audio"],
    };
  }

  const missingParts = [
    localization.hasBodyText ? null : "body",
    localization.hasIllustration ? null : "illustration",
    localization.hasAudioAsset ? null : "audio",
  ].filter(Boolean) as string[];

  return {
    isReady: missingParts.length === 0,
    missingParts,
  };
}

function getLocalizationForLanguage(
  storyPage: StoryPageReadViewModel,
  languageCode: string | null,
) {
  if (!languageCode) {
    return storyPage.primaryLocalization;
  }

  return (
    storyPage.localizations.find(
      (localization) => localization.languageCode === languageCode,
    ) ?? null
  );
}

function createColumns({
  availableLocalizations,
  selectedLanguageCode,
  selectedLanguageLabel,
  isMutationPending,
  onEditStoryPage,
  onAddAfterStoryPage,
  onDeleteStoryPage,
  copy,
}: Pick<
  StoryPageTableProps,
  | "availableLocalizations"
  | "selectedLanguageCode"
  | "selectedLanguageLabel"
  | "isMutationPending"
  | "onEditStoryPage"
  | "onAddAfterStoryPage"
  | "onDeleteStoryPage"
> & {
  copy: {
    page: string;
    selectedStatus: string;
    allLocales: string;
    nextStep: string;
    actions: string;
    noLocalization: string;
    noLocalizationDetail: string;
    missingPrefix: string;
    ready: string;
    incomplete: string;
    createLocale: string;
    addBody: string;
    addIllustration: string;
    addAudio: string;
    readyForLocale: string;
    continueAction: string;
    editAction: string;
    addAfterAction: string;
    deleteAction: string;
  };
}) {
  return [
    {
      id: "page",
      header: copy.page,
      cell: (storyPage) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">
            Page {storyPage.pageNumber}
          </p>
          <p className="text-xs text-muted-foreground">
            Content #{storyPage.contentId}
          </p>
        </div>
      ),
    },
    {
      id: "selected-status",
      header: copy.selectedStatus.replace("{locale}", selectedLanguageLabel),
      cell: (storyPage) => {
        const localization = getLocalizationForLanguage(
          storyPage,
          selectedLanguageCode,
        );
        const state = getLocalizationState(localization);

        return (
          <div className="space-y-1">
            <p className="inline-flex items-center gap-2 text-sm font-medium text-foreground">
              <Languages className="size-4 text-primary" />
              {state.isReady ? copy.ready : copy.incomplete}
            </p>
            <p className="text-xs text-muted-foreground">
              {localization
                ? state.isReady
                  ? copy.readyForLocale
                  : `${copy.missingPrefix} ${state.missingParts.join(", ")}`
                : copy.noLocalizationDetail}
            </p>
          </div>
        );
      },
    },
    {
      id: "all-locales",
      header: copy.allLocales,
      cell: (storyPage) => {
        const readyLocales = availableLocalizations.filter(
          (available) =>
            getLocalizationState(
              storyPage.localizations.find(
                (localization) =>
                  localization.languageCode === available.languageCode,
              ) ?? null,
            ).isReady,
        );
        const missingCodes = availableLocalizations
          .filter(
            (available) =>
              !readyLocales.some(
                (readyLocale) =>
                  readyLocale.languageCode === available.languageCode,
              ),
          )
          .map((available) => available.languageCode.toUpperCase());

        return (
          <div className="space-y-1">
            <p className="text-sm font-medium text-foreground">
              {readyLocales.length} / {availableLocalizations.length} ready
            </p>
            <p className="text-xs text-muted-foreground">
              {missingCodes.length === 0
                ? copy.readyForLocale
                : `${copy.missingPrefix} ${missingCodes.join(", ")}`}
            </p>
          </div>
        );
      },
    },
    {
      id: "next-step",
      header: copy.nextStep,
      cell: (storyPage) => {
        const localization = getLocalizationForLanguage(
          storyPage,
          selectedLanguageCode,
        );
        const state = getLocalizationState(localization);
        let nextStep = copy.readyForLocale;

        if (!localization) {
          nextStep = copy.createLocale;
        } else if (state.missingParts.includes("body")) {
          nextStep = copy.addBody;
        } else if (state.missingParts.includes("illustration")) {
          nextStep = copy.addIllustration;
        } else if (state.missingParts.includes("audio")) {
          nextStep = copy.addAudio;
        }

        return <p className="text-sm text-foreground">{nextStep}</p>;
      },
    },
    {
      id: "actions",
      header: copy.actions,
      align: "right",
      cellClassName: "w-[1%]",
      cell: (storyPage) => {
        const localization = getLocalizationForLanguage(
          storyPage,
          selectedLanguageCode,
        );
        const state = getLocalizationState(localization);

        return (
          <div className="flex flex-wrap items-center justify-end gap-2">
            <Button
              aria-label={`${
                state.isReady ? copy.editAction : copy.continueAction
              } page ${storyPage.pageNumber}`}
              disabled={isMutationPending}
              size="sm"
              type="button"
              onClick={() => onEditStoryPage?.(storyPage)}
            >
              <Pencil className="size-4" />
              {state.isReady ? copy.editAction : copy.continueAction}
            </Button>
            <Button
              aria-label={`Add page after ${storyPage.pageNumber}`}
              disabled={isMutationPending}
              size="sm"
              type="button"
              variant="outline"
              onClick={() => onAddAfterStoryPage?.(storyPage)}
            >
              <Plus className="size-4" />
              {copy.addAfterAction}
            </Button>
            <Button
              aria-label={`Delete page ${storyPage.pageNumber}`}
              disabled={isMutationPending}
              size="sm"
              type="button"
              variant="ghost"
              onClick={() => onDeleteStoryPage?.(storyPage)}
            >
              <Trash2 className="size-4" />
              {copy.deleteAction}
            </Button>
          </div>
        );
      },
    },
  ] satisfies DataTableColumn<StoryPageReadViewModel>[];
}

export function StoryPageTable({
  storyPages,
  availableLocalizations,
  selectedLanguageCode,
  selectedLanguageLabel,
  isLoading = false,
  problem = null,
  onRetry,
  onEditStoryPage,
  onAddAfterStoryPage,
  onDeleteStoryPage,
  emptyAction,
  isMutationPending = false,
}: StoryPageTableProps) {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          page: "Sayfa",
          selectedStatus: "{locale} durumu",
          allLocales: "Tum locale kapsami",
          nextStep: "Siradaki is",
          actions: "Aksiyonlar",
          noLocalization: "Henuz hazir degil",
          noLocalizationDetail:
            "Bu dil icin sayfa payload'i henuz olusturulmadi.",
          missingPrefix: "Eksik:",
          ready: "Hazir",
          incomplete: "Eksik",
          createLocale: "Bu dil icin sayfa olustur",
          addBody: "Govde metni ekle",
          addIllustration: "Illustrasyon sec",
          addAudio: "Ses bagla",
          readyForLocale: "Secili dil icin hazir",
          continueAction: "Devam et",
          editAction: "Duzenle",
          addAfterAction: "Sonrasina ekle",
          deleteAction: "Sil",
        }
      : {
          page: "Page",
          selectedStatus: "{locale} status",
          allLocales: "All locale coverage",
          nextStep: "Next step",
          actions: "Actions",
          noLocalization: "Not ready yet",
          noLocalizationDetail:
            "No localized page payload exists yet for this language.",
          missingPrefix: "Missing:",
          ready: "Ready",
          incomplete: "Incomplete",
          createLocale: "Create selected locale page",
          addBody: "Add body copy",
          addIllustration: "Pick illustration",
          addAudio: "Attach audio",
          readyForLocale: "Ready for selected locale",
          continueAction: "Continue",
          editAction: "Edit",
          addAfterAction: "Add after",
          deleteAction: "Delete",
        };
  const columns = createColumns({
    availableLocalizations,
    selectedLanguageCode,
    selectedLanguageLabel,
    isMutationPending,
    onEditStoryPage,
    onAddAfterStoryPage,
    onDeleteStoryPage,
    copy,
  });

  if (problem && storyPages.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyAction={emptyAction}
        emptyDescription="The story page collection could not be loaded from the admin API."
        emptyTitle="Story page list unavailable"
        getRowId={(storyPage) => storyPage.pageNumber.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption="Story page table"
      columns={columns}
      emptyAction={emptyAction}
      emptyDescription="Add the first page and continue in the selected locale editor right away."
      emptyTitle="No story pages yet"
      getRowId={(storyPage) => storyPage.pageNumber.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting story page structure and localized page payload summaries from the admin API."
      loadingTitle="Loading story pages"
      onRetry={onRetry}
      problem={storyPages.length > 0 ? problem : null}
      rows={storyPages}
    />
  );
}
