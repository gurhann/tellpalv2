import { ArrowLeft, ImageIcon, Layers3, Plus, Trash2 } from "lucide-react";
import type { FormEvent } from "react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import { toastMutation } from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
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
import type {
  ContentReadViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { StoryPageLocalizationForm } from "@/features/story-pages/components/story-page-localization-form";
import { StoryPageForm } from "@/features/story-pages/components/story-page-form";
import { StoryPageTable } from "@/features/story-pages/components/story-page-table";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import { useStoryPageActions } from "@/features/story-pages/mutations/use-story-page-actions";
import { useRecentImageAssets } from "@/features/story-pages/queries/use-recent-image-assets";
import {
  useStoryPage,
  useStoryPages,
} from "@/features/story-pages/queries/use-story-pages";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";
import type {
  AdminStoryPageLocalizationResponse,
  AdminStoryPageResponse,
} from "@/features/contents/api/story-page-admin";

type StoryPageFieldErrors = {
  pageNumber?: string;
  illustrationAssetId?: string;
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

function parseOptionalPositiveInteger(value: string, label: string) {
  const trimmed = value.trim();

  if (trimmed.length === 0) {
    return {
      error: null,
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
  onCreate: (input: {
    pageNumber: number;
    illustrationMediaId: number | null;
  }) => Promise<AdminStoryPageResponse>;
};

function CreateStoryPageDialog({
  open,
  isPending,
  onOpenChange,
  onCreate,
}: CreateStoryPageDialogProps) {
  const [pageNumber, setPageNumber] = useState("");
  const [illustrationAssetId, setIllustrationAssetId] = useState("");
  const [fieldErrors, setFieldErrors] = useState<StoryPageFieldErrors>({});
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const recentImageAssetsQuery = useRecentImageAssets({ enabled: open });

  function resetState() {
    setPageNumber("");
    setIllustrationAssetId("");
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
    const parsedIllustrationAssetId = parseOptionalPositiveInteger(
      illustrationAssetId,
      "Illustration asset id",
    );

    const nextFieldErrors: StoryPageFieldErrors = {
      pageNumber: parsedPageNumber.error ?? undefined,
      illustrationAssetId: parsedIllustrationAssetId.error ?? undefined,
    };

    if (nextFieldErrors.pageNumber || nextFieldErrors.illustrationAssetId) {
      setFieldErrors(nextFieldErrors);
      return;
    }

    const illustrationAssetError = await validateIllustrationAssetId(
      parsedIllustrationAssetId.value,
    );

    if (illustrationAssetError) {
      setFieldErrors({
        illustrationAssetId: illustrationAssetError,
      });
      return;
    }

    try {
      await toastMutation(
        onCreate({
          pageNumber: parsedPageNumber.value as number,
          illustrationMediaId: parsedIllustrationAssetId.value,
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
        if (
          serverFieldErrors.pageNumber ||
          serverFieldErrors.illustrationMediaId
        ) {
          setFieldErrors({
            pageNumber: serverFieldErrors.pageNumber,
            illustrationAssetId: serverFieldErrors.illustrationMediaId,
          });
          return;
        }

        if (error.problem.errorCode === "asset_media_type_mismatch") {
          setFieldErrors({
            illustrationAssetId: error.problem.detail,
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
            Create a new page number under this STORY record. Illustration media
            is optional at this stage.
          </DialogDescription>
        </DialogHeader>

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

          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="story-page-create-illustration"
            >
              Illustration asset id
            </label>
            <Input
              id="story-page-create-illustration"
              inputMode="numeric"
              min={1}
              placeholder="Optional"
              type="number"
              value={illustrationAssetId}
              onChange={(event) => setIllustrationAssetId(event.target.value)}
              disabled={isPending}
            />
            <FieldError error={fieldErrors.illustrationAssetId} />
            <p className="text-sm text-muted-foreground">
              Leave blank when the page structure is ready but the illustration
              asset has not been registered yet. When present, the asset must be
              an `IMAGE`.
            </p>
            {recentImageAssetsQuery.assets.length > 0 ? (
              <div className="space-y-2 rounded-2xl border border-border/70 bg-muted/25 px-3 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                  Recent Image Assets
                </p>
                <div className="flex flex-wrap gap-2">
                  {recentImageAssetsQuery.assets.map((asset) => (
                    <Button
                      key={asset.assetId}
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setIllustrationAssetId(String(asset.assetId))
                      }
                      disabled={isPending}
                    >
                      Asset #{asset.assetId}
                    </Button>
                  ))}
                </div>
              </div>
            ) : null}
            {!recentImageAssetsQuery.isLoading &&
            recentImageAssetsQuery.assets.length === 0 ? (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-3 py-3 text-sm text-muted-foreground">
                No recent image assets were found. Register an image asset in
                Media before linking story illustrations.
              </div>
            ) : null}
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
            <SubmitButton isPending={isPending} pendingLabel="Creating page...">
              Add story page
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

type EditStoryPageDialogProps = {
  content: ContentReadViewModel;
  pageNumber: number | null;
  isPending: boolean;
  onClose: () => void;
  onUpdateStoryPage: (input: {
    pageNumber: number;
    illustrationMediaId: number | null;
  }) => Promise<AdminStoryPageResponse>;
  onUpsertLocalization: (input: {
    pageNumber: number;
    languageCode: string;
    bodyText: string | null;
    audioMediaId: number | null;
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

  if (localization.hasBodyText && localization.hasAudioAsset) {
    return {
      meta: "Ready",
      description: "Body copy and audio binding are both present.",
      tone: "success" as const,
    };
  }

  if (!localization.hasBodyText && !localization.hasAudioAsset) {
    return {
      meta: "Incomplete",
      description: "This language is missing both body copy and audio.",
      tone: "warning" as const,
    };
  }

  return {
    meta: "Incomplete",
    description: localization.hasBodyText
      ? "Audio binding is still missing for this language."
      : "Narrative body copy is still missing for this language.",
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
  isPending,
  onClose,
  onUpdateStoryPage,
  onUpsertLocalization,
}: EditStoryPageDialogProps) {
  const storyPageQuery = useStoryPage(content.summary.id, pageNumber);
  const [activeLanguageCode, setActiveLanguageCode] = useState(
    content.localizations[0]?.languageCode ?? "",
  );

  if (pageNumber === null) {
    return null;
  }
  const storyPage = storyPageQuery.storyPage;

  const languageItems = storyPage
    ? buildLocalizationTabItems(content, storyPage)
    : [];

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Edit story page</DialogTitle>
          <DialogDescription>
            Update page metadata plus localized body/audio payloads for page{" "}
            {pageNumber}.
          </DialogDescription>
        </DialogHeader>

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
            <StoryPageForm
              storyPage={storyPage}
              isPending={isPending}
              onSave={onUpdateStoryPage}
            />

            <div className="space-y-4">
              <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
                <p className="text-sm font-medium text-foreground">
                  Localized page workspaces
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Story page localizations are available only for parent content
                  locales that already exist on the content detail route.
                </p>
              </div>

              <LanguageTabs
                items={languageItems}
                value={activeLanguageCode}
                onValueChange={setActiveLanguageCode}
                emptyDescription="Create a content localization first. Story page payloads can only be edited for existing parent locales."
                emptyTitle="No parent locales available"
                renderContent={(item) => {
                  const contentLocalization =
                    content.localizations.find(
                      (localization) => localization.languageCode === item.code,
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
                      onSave={({ languageCode, bodyText, audioMediaId }) =>
                        onUpsertLocalization({
                          pageNumber: storyPage.pageNumber,
                          languageCode,
                          bodyText,
                          audioMediaId,
                        })
                      }
                    />
                  );
                }}
              />
            </div>
          </div>
        ) : null}

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

        {problem ? <ProblemAlert problem={problem} /> : null}

        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-muted-foreground">
          Localized page payloads owned by this page are removed together with
          the page structure.
        </div>

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
};

function StoryPagesWorkspace({ content }: StoryPagesWorkspaceProps) {
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
  const illustrationCount = storyPages.filter(
    (storyPage) => storyPage.hasIllustration,
  ).length;
  const isMutating = storyPageActions.isPending;

  return (
    <>
      <ContentPageShell
        eyebrow="Story Editor"
        title={routeTitle}
        description="The story page collection is bound to the admin API. Page metadata, localized body copy, and audio bindings now share the same editor flow."
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to={`/contents/${content.summary.id}`}>
                <ArrowLeft className="size-4" />
                Return to content detail
              </Link>
            </Button>
            <Button
              type="button"
              onClick={() => setIsCreateDialogOpen(true)}
              disabled={isMutating}
            >
              <Plus className="size-4" />
              Add story page
            </Button>
          </>
        }
        toolbar={
          <div className="grid gap-3 md:grid-cols-3">
            <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Parent Content
              </p>
              <p className="mt-2 text-sm font-medium text-foreground">
                {content.summary.externalKey}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {content.summary.typeLabel} / {content.localizationCount}{" "}
                localization
                {content.localizationCount === 1 ? "" : "s"}
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Story Structure
              </p>
              <p className="mt-2 text-sm font-medium text-foreground">
                {storyPageCount} story page{storyPageCount === 1 ? "" : "s"}{" "}
                live
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {localizedStoryPageCount} page
                {localizedStoryPageCount === 1 ? "" : "s"} already carry at
                least one localized payload.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Illustration Coverage
              </p>
              <p className="mt-2 text-sm font-medium text-foreground">
                {illustrationCount} illustration
                {illustrationCount === 1 ? "" : "s"} attached
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Update page-level illustration references from the table
                actions.
              </p>
            </div>
          </div>
        }
        aside={
          <>
            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Story Route Access</CardTitle>
                <CardDescription>
                  This child route stays reserved for `STORY` content types
                  only.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  <p className="text-sm font-medium text-foreground">
                    Read contract
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Dedicated story-page GET endpoints now return page metadata
                    and localized page payload summaries.
                  </p>
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                  <p className="text-sm font-medium text-foreground">
                    Publication dependency
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Story publication still depends on every page having a
                    complete localization with body copy and audio in the target
                    language.
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Story Editor Coverage</CardTitle>
                <CardDescription>
                  Page structure and localization editing are both live in this
                  route now.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  Per-page language workspaces now inherit their allowed
                  languages from the parent content localizations.
                </div>
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  `M04-T04` will lock the full story-page editor flow with
                  integration and Playwright coverage.
                </div>
              </CardContent>
            </Card>
          </>
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
              Add first story page
            </Button>
          }
          isMutationPending={isMutating}
        />

        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardHeader>
            <CardTitle>Editor Surface</CardTitle>
            <CardDescription>
              Story page editing now covers page metadata plus per-language body
              and audio payloads.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3 lg:grid-cols-3">
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <Plus className="size-4 text-primary" />
                Add pages
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Reserve the next page number in the story structure with an
                optional illustration asset reference.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <ImageIcon className="size-4 text-primary" />
                Update illustration
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Adjust or clear page-level illustration asset links directly
                inside the page editor.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <Layers3 className="size-4 text-primary" />
                Locale payloads
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Each parent content locale gets its own story page workspace
                with body copy and audio binding validation.
              </p>
            </div>
          </CardContent>
        </Card>
      </ContentPageShell>

      <CreateStoryPageDialog
        open={isCreateDialogOpen}
        isPending={storyPageActions.addStoryPage.isPending}
        onOpenChange={setIsCreateDialogOpen}
        onCreate={(input) =>
          storyPageActions.addStoryPage.mutateAsync({
            pageNumber: input.pageNumber,
            illustrationMediaId: input.illustrationMediaId,
          })
        }
      />

      <EditStoryPageDialog
        key={editingPageNumber ?? "story-page-edit-closed"}
        content={content}
        pageNumber={editingPageNumber}
        isPending={storyPageActions.isPending}
        onClose={() => setEditingPageNumber(null)}
        onUpdateStoryPage={(input) =>
          storyPageActions.updateStoryPage.mutateAsync({
            pageNumber: input.pageNumber,
            input: {
              illustrationMediaId: input.illustrationMediaId,
            },
          })
        }
        onUpsertLocalization={(input) =>
          storyPageActions.upsertStoryPageLocalization.mutateAsync({
            pageNumber: input.pageNumber,
            languageCode: input.languageCode,
            input: {
              bodyText: input.bodyText,
              audioMediaId: input.audioMediaId,
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
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;

  return (
    <StoryContentGuard contentId={hasValidContentId ? parsedContentId : null}>
      {(content) => <StoryPagesWorkspace content={content} />}
    </StoryContentGuard>
  );
}
