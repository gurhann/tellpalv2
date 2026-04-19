import { ArrowLeft, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

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
import {
  LanguageTabs,
  type LanguageTabItem,
} from "@/components/language/language-tabs";
import { TaskRail } from "@/components/workspace/task-rail";
import { WorkspaceStatusPill } from "@/components/workspace/workspace-primitives";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import type {
  ContentReadViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import type { AdminStoryPageLocalizationResponse } from "@/features/contents/api/story-page-admin";
import { StoryPageLocalizationForm } from "@/features/story-pages/components/story-page-localization-form";
import { StoryPageTable } from "@/features/story-pages/components/story-page-table";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";
import { useStoryPageActions } from "@/features/story-pages/mutations/use-story-page-actions";
import {
  useStoryPage,
  useStoryPages,
} from "@/features/story-pages/queries/use-story-pages";
import { ApiClientError } from "@/lib/http/client";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

function getFallbackProblem(error: unknown, detail: string): ApiProblemDetail {
  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: error instanceof Error ? error.message : detail,
  };
}

function getLocalizationState(
  localization: StoryPageReadViewModel["primaryLocalization"],
) {
  if (!localization) {
    return {
      isReady: false,
      missingParts: ["body", "illustration", "audio"],
      tone: "warning" as const,
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
    tone: missingParts.length === 0 ? ("success" as const) : ("warning" as const),
  };
}

function buildLocalizationTabItems(
  content: ContentReadViewModel,
  storyPage: StoryPageReadViewModel,
) {
  return content.localizations.map((contentLocalization) => {
    const storyPageLocalization =
      storyPage.localizations.find(
        (localization) =>
          localization.languageCode === contentLocalization.languageCode,
      ) ?? null;
    const state = getLocalizationState(storyPageLocalization);

    return {
      code: contentLocalization.languageCode,
      label: contentLocalization.languageLabel,
      meta: state.isReady ? "Ready" : "Continue",
      description: state.isReady
        ? "Body, illustration, and audio are all present."
        : `Missing ${state.missingParts.join(", ")}.`,
      tone: state.tone,
    } satisfies LanguageTabItem;
  });
}

type EditStoryPageDialogProps = {
  content: ContentReadViewModel;
  pageNumber: number | null;
  preferredLanguageCode?: string | null;
  isPending: boolean;
  onClose: () => void;
  onUpsertLocalization: (input: {
    pageNumber: number;
    languageCode: string;
    bodyText: string | null;
    audioMediaId: number | null;
    illustrationMediaId: number;
  }) => Promise<AdminStoryPageLocalizationResponse>;
};

function EditStoryPageDialog({
  content,
  pageNumber,
  preferredLanguageCode = null,
  isPending,
  onClose,
  onUpsertLocalization,
}: EditStoryPageDialogProps) {
  const storyPageQuery = useStoryPage(content.summary.id, pageNumber);
  const resolvedPreferredLanguageCode = content.localizations.some(
    (localization) => localization.languageCode === preferredLanguageCode,
  )
    ? preferredLanguageCode
    : content.localizations[0]?.languageCode ?? null;
  const [activeLanguageCode, setActiveLanguageCode] = useState(
    resolvedPreferredLanguageCode ?? "",
  );

  if (pageNumber === null) {
    return null;
  }

  const storyPage = storyPageQuery.storyPage;
  const languageItems = storyPage
    ? buildLocalizationTabItems(content, storyPage)
    : [];
  const resolvedActiveLanguageCode =
    languageItems.find((item) => item.code === activeLanguageCode)?.code ??
    resolvedPreferredLanguageCode ??
    languageItems[0]?.code ??
    "";
  const activeLanguageLabel =
    content.localizations.find(
      (localization) => localization.languageCode === resolvedActiveLanguageCode,
    )?.languageLabel ?? resolvedActiveLanguageCode.toUpperCase();

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{`Page ${pageNumber} · ${activeLanguageLabel}`}</DialogTitle>
          <DialogDescription>
            Complete body, illustration, and audio for the selected locale.
          </DialogDescription>
        </DialogHeader>

        <DialogBody className="grid gap-6">
          {storyPageQuery.problem ? (
            <ProblemAlert problem={storyPageQuery.problem} />
          ) : null}

          {storyPageQuery.isLoading && !storyPage ? (
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-10 text-sm text-muted-foreground">
              Loading the latest story page payloads...
            </div>
          ) : null}

          {storyPage ? (
            <LanguageTabs
              items={languageItems}
              value={resolvedActiveLanguageCode}
              onValueChange={setActiveLanguageCode}
              emptyDescription="Create a content localization first. Story page payloads can only be edited for existing parent locales."
              emptyTitle="No parent locales available"
              renderContent={(item) => {
                const contentLocalization =
                  content.localizations.find(
                    (localization) =>
                      localization.languageCode === item.code,
                  ) ?? null;

                if (!contentLocalization) {
                  return (
                    <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-6 text-sm text-muted-foreground">
                      Story page payloads can open only after the parent
                      content locale exists for this language.
                    </div>
                  );
                }

                return (
                  <StoryPageLocalizationForm
                    key={`${storyPage.pageNumber}-${item.code}`}
                    contentLocalization={contentLocalization}
                    isPending={isPending}
                    storyPage={storyPage}
                    onSave={({
                      languageCode,
                      bodyText,
                      audioMediaId,
                      illustrationMediaId,
                    }) =>
                      onUpsertLocalization({
                        pageNumber: storyPage.pageNumber,
                        languageCode,
                        bodyText,
                        audioMediaId,
                        illustrationMediaId,
                      })
                    }
                  />
                );
              }}
            />
          ) : null}
        </DialogBody>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            Close editor
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

type DeleteStoryPageDialogProps = {
  storyPage: StoryPageReadViewModel | null;
  isPending: boolean;
  onClose: () => void;
  onDelete: (pageNumber: number) => Promise<number>;
};

function DeleteStoryPageDialog({
  storyPage,
  isPending,
  onClose,
  onDelete,
}: DeleteStoryPageDialogProps) {
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);

  async function handleDelete() {
    if (!storyPage) {
      return;
    }

    setProblem(null);

    try {
      await toastMutation(onDelete(storyPage.pageNumber), {
        loading: "Removing story page...",
        success: (pageNumber) => `Page ${pageNumber} removed.`,
      });

      onClose();
    } catch (error) {
      setProblem(
        getFallbackProblem(
          error,
          "The story page could not be removed from this content item.",
        ),
      );
    }
  }

  return (
    <Dialog
      open={storyPage !== null}
      onOpenChange={(open) => !open && onClose()}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Delete story page</DialogTitle>
          <DialogDescription>
            Page {storyPage?.pageNumber ?? "?"} will be removed. Following
            pages shift up to keep the story order contiguous.
          </DialogDescription>
        </DialogHeader>

        <DialogBody className="grid gap-4">
          {problem ? <ProblemAlert problem={problem} /> : null}

          <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-muted-foreground">
            Localized page payloads owned by this page are removed together
            with the page structure.
          </div>
        </DialogBody>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isPending}
          >
            Cancel
          </Button>
          <SubmitButton
            isPending={isPending}
            pendingLabel="Deleting page..."
            type="button"
            variant="destructive"
            onClick={() => void handleDelete()}
          >
            <Trash2 className="size-4" />
            Delete page
          </SubmitButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

type StoryPagesWorkspaceProps = {
  content: ContentReadViewModel;
  preferredLanguageCode?: string | null;
};

function StoryPagesWorkspace({
  content,
  preferredLanguageCode = null,
}: StoryPagesWorkspaceProps) {
  const { locale } = useI18n();
  const storyPagesQuery = useStoryPages(content.summary.id);
  const storyPageActions = useStoryPageActions({
    contentId: content.summary.id,
  });
  const [editingPageNumber, setEditingPageNumber] = useState<number | null>(
    null,
  );
  const [deletingStoryPage, setDeletingStoryPage] =
    useState<StoryPageReadViewModel | null>(null);

  const routeTitle = content.primaryLocalization?.title
    ? `Story Pages for ${content.primaryLocalization.title}`
    : `Story Pages for Content #${content.summary.id}`;
  const resolvedPreferredLocalization =
    content.localizations.find(
      (localization) => localization.languageCode === preferredLanguageCode,
    ) ??
    content.primaryLocalization ??
    content.localizations[0] ??
    null;
  const resolvedPreferredLanguageCode =
    resolvedPreferredLocalization?.languageCode ?? null;
  const preferredLanguageLabel =
    resolvedPreferredLocalization?.languageLabel ??
    (locale === "tr" ? "Dil secilmedi" : "No locale selected");
  const storyPages = storyPagesQuery.storyPages;
  const storyPageCount = storyPagesQuery.isSuccess
    ? storyPages.length
    : (content.summary.pageCount ?? 0);
  const selectedLocaleReadyCount = storyPages.filter((storyPage) =>
    getLocalizationState(
      storyPage.localizations.find(
        (localization) =>
          localization.languageCode === resolvedPreferredLanguageCode,
      ) ?? null,
    ).isReady,
  ).length;
  const fullyReadyPageCount = storyPages.filter((storyPage) =>
    content.localizations.every((contentLocalization) =>
      getLocalizationState(
        storyPage.localizations.find(
          (localization) =>
            localization.languageCode === contentLocalization.languageCode,
        ) ?? null,
      ).isReady,
    ),
  ).length;
  const isMutating = storyPageActions.isPending;
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Story Editor",
          description:
            "Sayfa listesini koruyun ve secili dilde eksik isleri hizlica tamamlayin.",
          readinessTitle: "Story readiness",
          readinessDescription:
            "Rail yalnizca bir sonraki editor kararini destekleyen sayfa durumlarini gosterir.",
          returnToContent: "Icerik detayina don",
          addPage: "Sayfa ekle",
          addFirstPage: "Ilk sayfayi ekle",
          activeLocale: "Aktif dil",
          activeLocaleHint: "Yeni sayfalar bu dil editoru ile acilir.",
          totalPages: "Toplam sayfa",
          selectedLocaleReady: "Secili dilde hazir",
          fullyReady: "Tam hazir",
          addLoading: "Sayfa ekleniyor...",
          addSuccess: "Page {page} created.",
        }
      : {
          eyebrow: "Story Editor",
          description:
            "Keep the page list primary and complete the next missing task in the selected locale.",
          readinessTitle: "Story readiness",
          readinessDescription:
            "The rail stays compact and only tracks the next operational decision.",
          returnToContent: "Return to content detail",
          addPage: "Add story page",
          addFirstPage: "Add first story page",
          activeLocale: "Active locale",
          activeLocaleHint: "New pages open in this locale editor.",
          totalPages: "Total pages",
          selectedLocaleReady: "Ready in selected locale",
          fullyReady: "Fully ready pages",
          addLoading: "Creating story page...",
          addSuccess: "Page {page} created.",
        };

  async function handleAddStoryPage(afterPageNumber: number | null) {
    const storyPage = await toastMutation(
      storyPageActions.addStoryPage.mutateAsync({
        afterPageNumber,
      }),
      {
        loading: copy.addLoading,
        success: (createdStoryPage) =>
          copy.addSuccess.replace("{page}", `${createdStoryPage.pageNumber}`),
      },
    );

    setEditingPageNumber(storyPage.pageNumber);
  }

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={routeTitle}
        description={copy.description}
        aside={
          <TaskRail
            title={copy.readinessTitle}
            description={copy.readinessDescription}
            stats={[
              {
                label: copy.totalPages,
                value: `${storyPageCount} total`,
              },
              {
                label: copy.selectedLocaleReady,
                value: `${selectedLocaleReadyCount} / ${storyPageCount}`,
                tone:
                  selectedLocaleReadyCount === storyPageCount
                    ? "success"
                    : "warning",
              },
              {
                label: copy.fullyReady,
                value: `${fullyReadyPageCount} / ${storyPageCount}`,
                tone:
                  fullyReadyPageCount === storyPageCount
                    ? "success"
                    : "warning",
              },
            ]}
          />
        }
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to={`/contents/${content.summary.id}`}>
                <ArrowLeft className="size-4" />
                {copy.returnToContent}
              </Link>
            </Button>
            <Button
              type="button"
              onClick={() => void handleAddStoryPage(null)}
              disabled={isMutating}
            >
              <Plus className="size-4" />
              {copy.addPage}
            </Button>
          </>
        }
        toolbar={
          <div className="flex flex-wrap items-center gap-2 rounded-[1.4rem] border border-border/70 bg-muted/15 px-4 py-3">
            <WorkspaceStatusPill tone="accent">
              {copy.activeLocale}
            </WorkspaceStatusPill>
            <WorkspaceStatusPill>{preferredLanguageLabel}</WorkspaceStatusPill>
            <span className="text-sm text-muted-foreground">
              {copy.activeLocaleHint}
            </span>
          </div>
        }
      >
        <StoryPageTable
          storyPages={storyPages}
          availableLocalizations={content.localizations.map((localization) => ({
            languageCode: localization.languageCode,
            languageLabel: localization.languageLabel,
          }))}
          selectedLanguageCode={resolvedPreferredLanguageCode}
          selectedLanguageLabel={preferredLanguageLabel}
          isLoading={storyPagesQuery.isLoading}
          problem={storyPagesQuery.problem}
          onRetry={() => void storyPagesQuery.refetch()}
          onEditStoryPage={(storyPage) =>
            setEditingPageNumber(storyPage.pageNumber)
          }
          onAddAfterStoryPage={(storyPage) =>
            void handleAddStoryPage(storyPage.pageNumber)
          }
          onDeleteStoryPage={setDeletingStoryPage}
          emptyAction={
            <Button
              type="button"
              onClick={() => void handleAddStoryPage(null)}
              disabled={isMutating}
            >
              <Plus className="size-4" />
              {copy.addFirstPage}
            </Button>
          }
          isMutationPending={isMutating}
        />
      </ContentPageShell>

      <EditStoryPageDialog
        key={editingPageNumber ?? "story-page-edit-closed"}
        content={content}
        pageNumber={editingPageNumber}
        preferredLanguageCode={resolvedPreferredLanguageCode}
        isPending={storyPageActions.isPending}
        onClose={() => setEditingPageNumber(null)}
        onUpsertLocalization={(input) =>
          storyPageActions.upsertStoryPageLocalization.mutateAsync({
            pageNumber: input.pageNumber,
            languageCode: input.languageCode,
            input: {
              bodyText: input.bodyText,
              audioMediaId: input.audioMediaId,
              illustrationMediaId: input.illustrationMediaId,
            },
          })
        }
      />

      <DeleteStoryPageDialog
        key={deletingStoryPage?.pageNumber ?? "story-page-delete-closed"}
        storyPage={deletingStoryPage}
        isPending={storyPageActions.removeStoryPage.isPending}
        onClose={() => setDeletingStoryPage(null)}
        onDelete={(pageNumber) =>
          storyPageActions.removeStoryPage.mutateAsync(pageNumber)
        }
      />
    </>
  );
}

export function StoryPagesRoute() {
  const { contentId = "" } = useParams();
  const [searchParams] = useSearchParams();
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;
  const preferredLanguageCode = searchParams.get("language");

  return (
    <StoryContentGuard contentId={hasValidContentId ? parsedContentId : null}>
      {(content) => (
        <StoryPagesWorkspace
          content={content}
          preferredLanguageCode={preferredLanguageCode}
        />
      )}
    </StoryContentGuard>
  );
}
