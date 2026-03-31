import { useEffect } from "react";

import { FieldError } from "@/components/forms/field-error";
import { SubmitButton } from "@/components/forms/submit-button";
import {
  applyProblemDetailToForm,
  toastMutation,
  useZodForm,
} from "@/components/forms/form-utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";
import type { StoryPageReadViewModel } from "@/features/contents/model/content-view-model";
import { validateIllustrationAssetId } from "@/features/story-pages/lib/illustration-asset-validation";
import { useRecentImageAssets } from "@/features/story-pages/queries/use-recent-image-assets";
import {
  mapStoryPageToFormValues,
  storyPageSchema,
  type StoryPageFormValues,
} from "@/features/story-pages/schema/story-page-schema";
import { ApiClientError } from "@/lib/http/client";

type StoryPageFormProps = {
  storyPage: StoryPageReadViewModel;
  isPending?: boolean;
  onSave: (input: {
    pageNumber: number;
    illustrationMediaId: number | null;
  }) => Promise<AdminStoryPageResponse>;
};

export function StoryPageForm({
  storyPage,
  isPending = false,
  onSave,
}: StoryPageFormProps) {
  const form = useZodForm<StoryPageFormValues>({
    schema: storyPageSchema,
    defaultValues: mapStoryPageToFormValues(storyPage),
  });
  const recentImageAssetsQuery = useRecentImageAssets();

  useEffect(() => {
    form.reset(mapStoryPageToFormValues(storyPage));
  }, [form, storyPage]);

  async function handleSubmit(values: StoryPageFormValues) {
    form.clearErrors();

    const illustrationAssetError = await validateIllustrationAssetId(
      values.illustrationMediaId,
    );

    if (illustrationAssetError) {
      form.setError("illustrationMediaId", {
        type: "server",
        message: illustrationAssetError,
      });
      return;
    }

    try {
      const savedStoryPage = await toastMutation(
        onSave({
          pageNumber: storyPage.pageNumber,
          illustrationMediaId: values.illustrationMediaId,
        }),
        {
          loading: "Saving page metadata...",
          success: (saved) => `Page ${saved.pageNumber} metadata saved.`,
        },
      );

      form.reset({
        illustrationMediaId: savedStoryPage.illustrationMediaId,
      });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "asset_media_type_mismatch") {
          form.setError("illustrationMediaId", {
            type: "server",
            message: error.problem.detail,
          });
          return;
        }

        applyProblemDetailToForm(form.setError, error.problem);
        return;
      }

      form.setError("root.serverError", {
        type: "server",
        message: "The story page metadata could not be saved.",
      });
    }
  }

  return (
    <form
      className="grid gap-4"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      <div className="grid gap-3 md:grid-cols-2">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            Page {storyPage.pageNumber}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Illustration references live at page level. Narrative copy and audio
            stay in the language workspaces below.
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {storyPage.localizationCount} localization
            {storyPage.localizationCount === 1 ? "" : "s"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Existing language payloads stay attached while you update the page
            illustration.
          </p>
        </div>
      </div>

      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor="story-page-editor-illustration"
        >
          Illustration asset id
        </label>
        <Input
          id="story-page-editor-illustration"
          inputMode="numeric"
          placeholder="Optional"
          type="number"
          {...form.register("illustrationMediaId", {
            setValueAs: (value) => {
              if (value === "" || value === undefined || value === null) {
                return null;
              }

              return Number(value);
            },
          })}
          disabled={isPending}
        />
        <FieldError error={form.formState.errors.illustrationMediaId} />
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
                    form.setValue("illustrationMediaId", asset.assetId, {
                      shouldDirty: true,
                    })
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
            No recent image assets were found. Register an image asset in Media
            before saving illustration changes.
          </div>
        ) : null}
      </div>

      <FieldError error={form.formState.errors.root?.serverError} />

      <div className="flex justify-end">
        <SubmitButton isPending={isPending} pendingLabel="Saving metadata...">
          Save page metadata
        </SubmitButton>
      </div>
    </form>
  );
}
