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
import type {
  ContentReadViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { StoryPageTable } from "@/features/story-pages/components/story-page-table";
import { StoryContentGuard } from "@/features/story-pages/guards/story-content-guard";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import { useStoryPageActions } from "@/features/story-pages/mutations/use-story-page-actions";
import { useRecentImageAssets } from "@/features/story-pages/queries/use-recent-image-assets";
import { useStoryPages } from "@/features/story-pages/queries/use-story-pages";
import { ApiClientError } from "@/lib/http/client";
import { getProblemFieldErrors } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";
import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";

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
  storyPage: StoryPageReadViewModel | null;
  isPending: boolean;
  onClose: () => void;
  onUpdate: (input: {
    pageNumber: number;
    illustrationMediaId: number | null;
  }) => Promise<AdminStoryPageResponse>;
};

function EditStoryPageDialog({
  storyPage,
  isPending,
  onClose,
  onUpdate,
}: EditStoryPageDialogProps) {
  const [illustrationAssetId, setIllustrationAssetId] = useState(
    storyPage?.illustrationAssetId?.toString() ?? "",
  );
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [problem, setProblem] = useState<ApiProblemDetail | null>(null);
  const recentImageAssetsQuery = useRecentImageAssets({
    enabled: storyPage !== null,
  });

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!storyPage) {
      return;
    }

    setFieldError(undefined);
    setProblem(null);

    const parsedIllustrationAssetId = parseOptionalPositiveInteger(
      illustrationAssetId,
      "Illustration asset id",
    );

    if (parsedIllustrationAssetId.error) {
      setFieldError(parsedIllustrationAssetId.error);
      return;
    }

    const illustrationAssetError = await validateIllustrationAssetId(
      parsedIllustrationAssetId.value,
    );

    if (illustrationAssetError) {
      setFieldError(illustrationAssetError);
      return;
    }

    try {
      await toastMutation(
        onUpdate({
          pageNumber: storyPage.pageNumber,
          illustrationMediaId: parsedIllustrationAssetId.value,
        }),
        {
          loading: "Saving story page...",
          success: (updatedStoryPage) =>
            `Page ${updatedStoryPage.pageNumber} saved.`,
        },
      );

      onClose();
    } catch (error) {
      if (error instanceof ApiClientError) {
        const serverFieldErrors = getProblemFieldErrors(error.problem);
        if (serverFieldErrors.illustrationMediaId) {
          setFieldError(serverFieldErrors.illustrationMediaId);
          return;
        }

        if (error.problem.errorCode === "asset_media_type_mismatch") {
          setFieldError(error.problem.detail);
          return;
        }
      }

      setProblem(
        getFallbackProblem(error, "The story page changes could not be saved."),
      );
    }
  }

  return (
    <Dialog
      open={storyPage !== null}
      onOpenChange={(open) => !open && onClose()}
    >
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Edit story page</DialogTitle>
          <DialogDescription>
            Update illustration metadata for page {storyPage?.pageNumber ?? "?"}
            . Localized body and audio editing land in the next task.
          </DialogDescription>
        </DialogHeader>

        <form className="grid gap-4" onSubmit={handleSubmit}>
          {problem ? <ProblemAlert problem={problem} /> : null}

          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
            <p className="text-sm font-medium text-foreground">
              Page {storyPage?.pageNumber ?? "?"}
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Page numbers are fixed after creation. This action only updates
              the illustration asset reference.
            </p>
          </div>

          <div className="space-y-2">
            <label
              className="text-sm font-medium text-foreground"
              htmlFor="story-page-edit-illustration"
            >
              Illustration asset id
            </label>
            <Input
              id="story-page-edit-illustration"
              inputMode="numeric"
              min={1}
              placeholder="Optional"
              type="number"
              value={illustrationAssetId}
              onChange={(event) => setIllustrationAssetId(event.target.value)}
              disabled={isPending}
            />
            <FieldError error={fieldError} />
            <p className="text-sm text-muted-foreground">
              Illustration links can reference only `IMAGE` assets.
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
                Media before saving illustration changes.
              </div>
            ) : null}
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
            <SubmitButton isPending={isPending} pendingLabel="Saving page...">
              Save page
            </SubmitButton>
          </DialogFooter>
        </form>
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
  const [editingStoryPage, setEditingStoryPage] =
    useState<StoryPageReadViewModel | null>(null);
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
        description="The story page collection is now bound to the admin API. Page add, illustration update, and delete flows are live; localized body and audio editors land next."
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
                    complete localization in the target language.
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
              <CardHeader>
                <CardTitle>Next Story Tasks</CardTitle>
                <CardDescription>
                  Collection and metadata mutations are live. Rich editors land
                  in the next step.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                  `M04-T03` adds per-page localization forms for body text and
                  audio bindings.
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
          onEditStoryPage={setEditingStoryPage}
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
            <CardTitle>Page Mutation Surface</CardTitle>
            <CardDescription>
              This task keeps story-page editing intentionally narrow: page
              number creation, illustration reference updates, and page removal.
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
                from the table actions.
              </p>
            </div>
            <div className="rounded-2xl border border-border/70 bg-muted/20 px-4 py-4">
              <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                <Layers3 className="size-4 text-primary" />
                Invalidation
              </div>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Add, update, and delete flows invalidate story-page, content
                detail, and content list queries together.
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
        key={editingStoryPage?.pageNumber ?? "story-page-edit-closed"}
        storyPage={editingStoryPage}
        isPending={storyPageActions.updateStoryPage.isPending}
        onClose={() => setEditingStoryPage(null)}
        onUpdate={(input) =>
          storyPageActions.updateStoryPage.mutateAsync({
            pageNumber: input.pageNumber,
            input: {
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
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;

  return (
    <StoryContentGuard contentId={hasValidContentId ? parsedContentId : null}>
      {(content) => <StoryPagesWorkspace content={content} />}
    </StoryContentGuard>
  );
}
