import { ArrowLeft, Plus, Trash2 } from "lucide-react";
import type { FormEvent } from "react";
import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
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
import { Input } from "@/components/ui/input";
import {
  LanguageTabs,
  type LanguageTabItem,
} from "@/components/language/language-tabs";
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
  WorkspaceMetricCard,
  WorkspaceStatusPill,
} from "@/components/workspace/workspace-primitives";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import type {
  ContentReadViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import type {
  AdminStoryPageLocalizationResponse,
  AdminStoryPageResponse,
} from "@/features/contents/api/story-page-admin";
import { StoryPageLocalizationForm } from "@/features/story-pages/components/story-page-localization-form";
import { StoryPageTable } from "@/features/story-pages/components/story-page-table";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";
import { useStoryPageActions } from "@/features/story-pages/mutations/use-story-page-actions";
import {
  useStoryPage,
  useStoryPages,
} from "@/features/story-pages/queries/use-story-pages";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type StoryPageFieldErrors = {
  pageNumber?: string;
};

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

function parseRequiredPositiveInteger(value: string, label: string) {
  const trimmed = value.trim();

  if (trimmed.length === 0) {
    return {
      error: `${label} is required.`,
      value: null,
    };
  }

  const parsed = Number(trimmed);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return {
      error: `${label} must be a positive integer.`,
      value: null,
    };
  }

  return {
    error: null,
    value: parsed,
  };
}

type CreateStoryPageDialogProps = {
  open: boolean;
  isPending: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (input: { pageNumber: number }) => Promise<AdminStoryPageResponse>;
};

function CreateStoryPageDialog({
  open,
  isPending,
  onOpenChange,
  onCreate,
}: CreateStoryPageDialogProps) {
  const [pageNumber, setPageNumber] = useState("");
  const [fieldErrors, setFieldErrors] = useState<StoryPageFieldErrors>({});
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);

  function resetState() {
    setPageNumber("");
    setFieldErrors({});
    setProblem(null);
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      resetState();
    }

    onOpenChange(nextOpen);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setProblem(null);

    const parsedPageNumber = parseRequiredPositiveInteger(
      pageNumber,
      "Page number",
    );

    if (parsedPageNumber.error) {
      setFieldErrors({
        pageNumber: parsedPageNumber.error,
      });
      return;
    }

    try {
      await toastMutation(
        onCreate({
          pageNumber: parsedPageNumber.value as number,
        }),
        {
          loading: "Creating story page...",
          success: (storyPage) => `Page ${storyPage.pageNumber} created.`,
        },
      );

      handleOpenChange(false);
    } catch (error) {
      if (error instanceof ApiClientError) {
        const serverFieldErrors = getProblemFieldErrors(error.problem);
        if (serverFieldErrors.pageNumber) {
          setFieldErrors({
            pageNumber: serverFieldErrors.pageNumber,
          });
          return;
        }
      }

      setProblem(
        getFallbackProblem(
          error,
          "The story page could not be created for this content item.",
        ),
      );
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Add story page</DialogTitle>
          <DialogDescription>
            Create a new page number under this STORY record. Localized
            illustrations are linked inside each language workspace after the
            page exists.
          </DialogDescription>
        </DialogHeader>

        <DialogBody>
          <form className="grid gap-4" onSubmit={handleSubmit}>
            {problem ? <ProblemAlert problem={problem} /> : null}

            <div className="space-y-2">
              <label
                className="text-sm font-medium text-foreground"
                htmlFor="story-page-create-number"
              >
                Page number
              </label>
              <Input
                id="story-page-create-number"
                inputMode="numeric"
                min={1}
                placeholder="1"
                type="number"
                value={pageNumber}
                onChange={(event) => setPageNumber(event.target.value)}
                disabled={isPending}
              />
              <FieldError error={fieldErrors.pageNumber} />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
                disabled={isPending}
              >
                Cancel
              </Button>
              <SubmitButton
                isPending={isPending}
                pendingLabel="Creating page..."
              >
                Add story page
              </SubmitButton>
            </DialogFooter>
          </form>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
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

function describeLocalizationState(
  localization: StoryPageReadViewModel["primaryLocalization"],
) {
  if (!localization) {
    return {
      meta: "Missing",
      description: "No localized page payload exists yet for this language.",
      tone: "muted" as const,
    };
  }

  const missingParts = [
    localization.hasBodyText ? null : "body copy",
    localization.hasAudioAsset ? null : "audio",
    localization.hasIllustration ? null : "illustration",
  ].filter(Boolean) as string[];

  if (missingParts.length === 0) {
    return {
      meta: "Ready",
      description: "Body copy, audio, and illustration are all present.",
      tone: "success" as const,
    };
  }

  return {
    meta: "Incomplete",
    description: `Missing ${missingParts.join(", ")} for this language.`,
    tone: "warning" as const,
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
    const state = describeLocalizationState(storyPageLocalization);

    return {
      code: contentLocalization.languageCode,
      label: contentLocalization.languageLabel,
      meta: state.meta,
      description: state.description,
      tone: state.tone,
    } satisfies LanguageTabItem;
  });
}

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
    : null;
  const [activeLanguageCode, setActiveLanguageCode] = useState(
    resolvedPreferredLanguageCode ??
      content.localizations[0]?.languageCode ??
      "",
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

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Edit story page</DialogTitle>
          <DialogDescription>
            Manage localized body, audio, and illustration payloads for page{" "}
            {pageNumber}.
          </DialogDescription>
        </DialogHeader>

        <DialogBody className="grid gap-6">
          {storyPageQuery.problem ? (
            <ProblemAlert problem={storyPageQuery.problem} />
          ) : null}

          {storyPageQuery.isLoading && !storyPage ? (
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-10 text-sm text-muted-foreground">
              Loading the latest story page metadata and localized page
              payloads...
            </div>
          ) : null}

          {storyPage ? (
            <div className="grid gap-6">
              <div className="grid gap-3 md:grid-cols-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  <p className="text-sm font-medium text-foreground">
                    Page {storyPage.pageNumber}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Page numbers remain stable after creation.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  <p className="text-sm font-medium text-foreground">
                    {storyPage.localizationCount} localization
                    {storyPage.localizationCount === 1 ? "" : "s"}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Language workspaces inherit their allowed locales from the
                    parent content detail route.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  <p className="text-sm font-medium text-foreground">
                    {storyPage.illustratedLocalizationCount} localized
                    illustration
                    {storyPage.illustratedLocalizationCount === 1 ? "" : "s"}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Each language now carries its own illustration asset
                    reference.
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
                  <p className="text-sm font-medium text-foreground">
                    Localized page workspaces
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Story page localizations are available only for parent
                    content locales that already exist on the content detail
                    route.
                  </p>
                </div>

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
              </div>
            </div>
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
            Page {storyPage?.pageNumber ?? "?"} will be removed from this STORY
            record. This action updates the aggregate page count immediately.
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
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [editingPageNumber, setEditingPageNumber] = useState<number | null>(
    null,
  );
  const [deletingStoryPage, setDeletingStoryPage] =
    useState<StoryPageReadViewModel | null>(null);

  const routeTitle = content.primaryLocalization?.title
    ? `Story Pages for ${content.primaryLocalization.title}`
    : `Story Pages for Content #${content.summary.id}`;
  const storyPages = storyPagesQuery.storyPages;
  const storyPageCount = storyPagesQuery.isSuccess
    ? storyPages.length
    : (content.summary.pageCount ?? 0);
  const localizedStoryPageCount = storyPages.filter(
    (storyPage) => storyPage.localizationCount > 0,
  ).length;
  const illustratedLocalizationCount = storyPages.reduce(
    (count, storyPage) => count + storyPage.illustratedLocalizationCount,
    0,
  );
  const isMutating = storyPageActions.isPending;
  const completeCoverageCount = storyPages.filter(
    (storyPage) => storyPage.hasCompleteIllustrationCoverage,
  ).length;
  const preferredLanguageLabel =
    content.localizations.find(
      (localization) => localization.languageCode === preferredLanguageCode,
    )?.languageLabel ??
    content.primaryLocalization?.languageLabel ??
    (locale === "tr" ? "Dil secilmedi" : "No locale selected");
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Story Editor",
          description:
            "Sayfa yapisini ve yerellestirilmis payload'lari ayni editor yuzeyinde yonetin.",
          readinessTitle: "Story readiness",
          readinessDescription:
            "Yapiyi sabit tutarken body, audio ve illustrasyon kapsamını secili dil baglamiyla izleyin.",
          returnToContent: "Icerik detayina don",
          addPage: "Sayfa ekle",
          storyPages: "Story pages",
          localizedPages: "Yerellestirilmis sayfalar",
          illustrationCoverage: "Illustrasyon kapsami",
          localeHandoff: "Locale handoff",
          localeHandoffDescription:
            "Bu editor parent content detail rotasindan gelen dil baglamini korur.",
          toolbarLocale: "Aktif dil",
          toolbarPages: "Toplam sayfa",
          toolbarCoverage: "Hazir illustrasyon",
        }
      : {
          eyebrow: "Story Editor",
          description:
            "Manage page structure and localized payloads on one editor surface.",
          readinessTitle: "Story readiness",
          readinessDescription:
            "Keep the structure stable while tracking body, audio, and illustration coverage in the current locale context.",
          returnToContent: "Return to content detail",
          addPage: "Add story page",
          storyPages: "Story pages",
          localizedPages: "Localized pages",
          illustrationCoverage: "Illustration coverage",
          localeHandoff: "Locale handoff",
          localeHandoffDescription:
            "This editor preserves the language context carried from the parent content detail route.",
          toolbarLocale: "Active locale",
          toolbarPages: "Total pages",
          toolbarCoverage: "Ready illustrations",
        };

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
                label: copy.storyPages,
                value: `${storyPageCount} total`,
              },
              {
                label: copy.localizedPages,
                value: `${localizedStoryPageCount} with at least one locale`,
                tone:
                  localizedStoryPageCount === storyPageCount
                    ? "success"
                    : "warning",
              },
              {
                label: copy.illustrationCoverage,
                value: `${completeCoverageCount} fully complete`,
                tone:
                  completeCoverageCount === storyPageCount
                    ? "success"
                    : "warning",
              },
            ]}
          >
            <WorkspaceInfoCard
              title={copy.localeHandoff}
              description={copy.localeHandoffDescription}
              className="bg-background/80"
            >
              <WorkspaceKeyValueGrid
                items={[
                  {
                    label: copy.toolbarLocale,
                    value: preferredLanguageLabel,
                    tone: "accent",
                  },
                  {
                    label: copy.storyPages,
                    value: `${storyPageCount}`,
                  },
                ]}
              />
            </WorkspaceInfoCard>
          </TaskRail>
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
              onClick={() => setIsCreateDialogOpen(true)}
              disabled={isMutating}
            >
              <Plus className="size-4" />
              {copy.addPage}
            </Button>
          </>
        }
        toolbar={
          <div className="grid gap-4 rounded-[1.7rem] border border-border/70 bg-muted/15 p-4 lg:grid-cols-[1.15fr_0.85fr]">
            <div className="grid gap-3 md:grid-cols-3">
              <WorkspaceMetricCard
                label={copy.toolbarLocale}
                value={preferredLanguageLabel}
                detail={content.summary.externalKey}
                tone="accent"
              />
              <WorkspaceMetricCard
                label={copy.toolbarPages}
                value={`${storyPageCount}`}
                detail={`${localizedStoryPageCount} localized`}
                tone={
                  localizedStoryPageCount === storyPageCount
                    ? "success"
                    : "warning"
                }
              />
              <WorkspaceMetricCard
                label={copy.toolbarCoverage}
                value={`${completeCoverageCount}`}
                detail={`${illustratedLocalizationCount} localized illustrations`}
                tone={
                  completeCoverageCount === storyPageCount
                    ? "success"
                    : "warning"
                }
              />
            </div>
            <WorkspaceInfoCard
              title={copy.localeHandoff}
              description={copy.localeHandoffDescription}
              className="bg-background/85"
            >
              <div className="flex flex-wrap items-center gap-2">
                <WorkspaceStatusPill tone="accent">
                  {content.summary.typeLabel}
                </WorkspaceStatusPill>
                <WorkspaceStatusPill
                  tone={content.summary.active ? "success" : "default"}
                >
                  {content.summary.active ? "Active" : "Inactive"}
                </WorkspaceStatusPill>
                <WorkspaceStatusPill tone="default">
                  {preferredLanguageLabel}
                </WorkspaceStatusPill>
              </div>
            </WorkspaceInfoCard>
          </div>
        }
      >
        <StoryPageTable
          storyPages={storyPages}
          isLoading={storyPagesQuery.isLoading}
          problem={storyPagesQuery.problem}
          onRetry={() => void storyPagesQuery.refetch()}
          onEditStoryPage={(storyPage) =>
            setEditingPageNumber(storyPage.pageNumber)
          }
          onDeleteStoryPage={setDeletingStoryPage}
          emptyAction={
            <Button
              type="button"
              onClick={() => setIsCreateDialogOpen(true)}
              disabled={isMutating}
            >
              <Plus className="size-4" />
              {locale === "tr" ? "Ilk sayfayi ekle" : "Add first story page"}
            </Button>
          }
          isMutationPending={isMutating}
        />
      </ContentPageShell>

      <CreateStoryPageDialog
        open={isCreateDialogOpen}
        isPending={storyPageActions.addStoryPage.isPending}
        onOpenChange={setIsCreateDialogOpen}
        onCreate={(input) =>
          storyPageActions.addStoryPage.mutateAsync({
            pageNumber: input.pageNumber,
          })
        }
      />

      <EditStoryPageDialog
        key={editingPageNumber ?? "story-page-edit-closed"}
        content={content}
        pageNumber={editingPageNumber}
        preferredLanguageCode={preferredLanguageCode}
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
