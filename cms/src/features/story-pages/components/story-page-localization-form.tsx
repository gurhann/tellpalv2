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
import { Textarea } from "@/components/ui/textarea";
import type { AdminStoryPageLocalizationResponse } from "@/features/contents/api/story-page-admin";
import type {
  ContentLocalizationViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import {
  validateAudioAssetId,
  validateIllustrationAssetId,
} from "@/features/story-pages/lib/illustration-asset-validation";
import { useRecentAudioAssets } from "@/features/story-pages/queries/use-recent-audio-assets";
import { useRecentImageAssets } from "@/features/story-pages/queries/use-recent-image-assets";
import {
  getStoryPageLocalizationFormDefaults,
  mapStoryPageLocalizationToFormValues,
  storyPageLocalizationSchema,
  type StoryPageLocalizationFormValues,
} from "@/features/story-pages/schema/story-page-schema";
import { ApiClientError } from "@/lib/http/client";

type StoryPageLocalizationFormProps = {
  storyPage: StoryPageReadViewModel;
  contentLocalization: ContentLocalizationViewModel;
  isPending?: boolean;
  onSave: (input: {
    languageCode: string;
    bodyText: string | null;
    audioMediaId: number | null;
    illustrationMediaId: number;
  }) => Promise<AdminStoryPageLocalizationResponse>;
};

function getRootErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.problem.errorCode === "content_localization_not_found") {
      return "Create the parent content localization for this language before editing story pages.";
    }

    return error.problem.detail;
  }

  return "The story page localization could not be saved.";
}

export function StoryPageLocalizationForm({
  storyPage,
  contentLocalization,
  isPending = false,
  onSave,
}: StoryPageLocalizationFormProps) {
  const existingLocalization =
    storyPage.localizations.find(
      (localization) =>
        localization.languageCode === contentLocalization.languageCode,
    ) ?? null;
  const mode = existingLocalization ? "update" : "create";
  const form = useZodForm<StoryPageLocalizationFormValues>({
    schema: storyPageLocalizationSchema,
    defaultValues: existingLocalization
      ? mapStoryPageLocalizationToFormValues(existingLocalization)
      : getStoryPageLocalizationFormDefaults(contentLocalization.languageCode),
  });
  const recentAudioAssetsQuery = useRecentAudioAssets();
  const recentImageAssetsQuery = useRecentImageAssets();
  const bodyText = form.watch("bodyText");
  const audioMediaId = form.watch("audioMediaId");
  const illustrationMediaId = form.watch("illustrationMediaId");
  const hasBodyText = Boolean(bodyText?.trim());
  const hasAudioAsset = audioMediaId !== null;
  const hasIllustration = typeof illustrationMediaId === "number";
  const isReadyForPublish = hasBodyText && hasAudioAsset && hasIllustration;

  useEffect(() => {
    form.reset(
      existingLocalization
        ? mapStoryPageLocalizationToFormValues(existingLocalization)
        : getStoryPageLocalizationFormDefaults(
            contentLocalization.languageCode,
          ),
    );
  }, [contentLocalization.languageCode, existingLocalization, form]);

  async function handleSubmit(values: StoryPageLocalizationFormValues) {
    form.clearErrors();

    if (values.illustrationMediaId === null) {
      form.setError("illustrationMediaId", {
        type: "server",
        message: "Illustration asset id is required.",
      });
      return;
    }

    const illustrationMediaId = values.illustrationMediaId;
    const illustrationAssetError =
      await validateIllustrationAssetId(illustrationMediaId);

    if (illustrationAssetError) {
      form.setError("illustrationMediaId", {
        type: "server",
        message: illustrationAssetError,
      });
      return;
    }

    const audioAssetError = await validateAudioAssetId(values.audioMediaId);

    if (audioAssetError) {
      form.setError("audioMediaId", {
        type: "server",
        message: audioAssetError,
      });
      return;
    }

    try {
      const savedLocalization = await toastMutation(
        onSave({
          languageCode: values.languageCode,
          bodyText: values.bodyText,
          audioMediaId: values.audioMediaId,
          illustrationMediaId,
        }),
        {
          loading:
            mode === "create"
              ? "Creating page localization..."
              : "Saving page localization...",
          success:
            mode === "create"
              ? "Page localization created."
              : "Page localization saved.",
        },
      );

      form.reset({
        languageCode: savedLocalization.languageCode,
        bodyText: savedLocalization.bodyText,
        audioMediaId: savedLocalization.audioMediaId,
        illustrationMediaId: savedLocalization.illustrationMediaId,
      });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.problem.errorCode === "asset_media_type_mismatch") {
          const targetField = error.problem.detail.includes(
            "illustrationMediaId",
          )
            ? "illustrationMediaId"
            : "audioMediaId";
          form.setError(targetField, {
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
        message: getRootErrorMessage(error),
      });
    }
  }

  return (
    <form
      className="grid gap-5"
      noValidate
      onSubmit={form.handleSubmit(handleSubmit)}
    >
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {contentLocalization.languageLabel}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Parent locale: {contentLocalization.title}
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasBodyText ? "Body ready" : "Body missing"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Empty narrative copy stays incomplete and blocks story publication
            for this language.
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasAudioAsset ? "Audio linked" : "Audio missing"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Audio must reference an `AUDIO` asset before this locale is
            publication-ready.
          </p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
          <p className="text-sm font-medium text-foreground">
            {hasIllustration ? "Illustration linked" : "Illustration missing"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {isReadyForPublish
              ? "This page locale is ready for publication checks."
              : "Story publication still needs body copy, audio, and a localized illustration."}
          </p>
        </div>
      </div>

      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor={`story-page-body-${contentLocalization.languageCode}`}
        >
          Body text
        </label>
        <Textarea
          id={`story-page-body-${contentLocalization.languageCode}`}
          placeholder="Localized page narrative"
          {...form.register("bodyText")}
          disabled={isPending}
        />
        <p className="text-sm text-muted-foreground">
          Blank body text is saved as empty state and keeps this language
          incomplete for publication.
        </p>
        <FieldError error={form.formState.errors.bodyText} />
      </div>

      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor={`story-page-illustration-${contentLocalization.languageCode}`}
        >
          Illustration asset id
        </label>
        <Input
          id={`story-page-illustration-${contentLocalization.languageCode}`}
          inputMode="numeric"
          placeholder="Required"
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
          Story page illustrations are language-scoped and must reference
          `IMAGE` assets.
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
            before saving this locale.
          </div>
        ) : null}
      </div>

      <div className="space-y-2">
        <label
          className="text-sm font-medium text-foreground"
          htmlFor={`story-page-audio-${contentLocalization.languageCode}`}
        >
          Audio asset id
        </label>
        <Input
          id={`story-page-audio-${contentLocalization.languageCode}`}
          inputMode="numeric"
          placeholder="Optional"
          type="number"
          {...form.register("audioMediaId", {
            setValueAs: (value) => {
              if (value === "" || value === undefined || value === null) {
                return null;
              }

              return Number(value);
            },
          })}
          disabled={isPending}
        />
        <FieldError error={form.formState.errors.audioMediaId} />
        <p className="text-sm text-muted-foreground">
          Story page audio links can reference only `AUDIO` assets.
        </p>
        {recentAudioAssetsQuery.assets.length > 0 ? (
          <div className="space-y-2 rounded-2xl border border-border/70 bg-muted/25 px-3 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Recent Audio Assets
            </p>
            <div className="flex flex-wrap gap-2">
              {recentAudioAssetsQuery.assets.map((asset) => (
                <Button
                  key={asset.assetId}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    form.setValue("audioMediaId", asset.assetId, {
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
        {!recentAudioAssetsQuery.isLoading &&
        recentAudioAssetsQuery.assets.length === 0 ? (
          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-3 py-3 text-sm text-muted-foreground">
            No recent audio assets were found. Register an audio asset in Media
            before saving this language payload.
          </div>
        ) : null}
      </div>

      <FieldError error={form.formState.errors.root?.serverError} />

      <div className="flex justify-end">
        <SubmitButton
          isPending={isPending}
          pendingLabel={
            mode === "create"
              ? "Creating localization..."
              : "Saving localization..."
          }
        >
          {mode === "create"
            ? "Create page localization"
            : "Save page localization"}
        </SubmitButton>
      </div>
    </form>
  );
}
