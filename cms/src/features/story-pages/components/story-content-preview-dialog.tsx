import {
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Image as ImageIcon,
  LoaderCircle,
  Pause,
  Pencil,
  Play,
  RefreshCw,
  Volume2,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
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
import type {
  ContentReadViewModel,
  StoryPageLocalizationViewModel,
  StoryPageReadViewModel,
} from "@/features/contents/model/content-view-model";
import { useAssetDetail } from "@/features/assets/queries/use-asset-detail";
import { useAssetPreview } from "@/features/assets/queries/use-asset-preview";
import type { ApiProblemDetail } from "@/types/api";

type StoryContentPreviewDialogProps = {
  content: ContentReadViewModel;
  storyPages: StoryPageReadViewModel[];
  languageCode: string | null;
  open: boolean;
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onOpenChange: (open: boolean) => void;
  onEditPage: (pageNumber: number) => void;
};

type PreviewIssue = {
  title: string;
  description: string;
  canRetry?: boolean;
};

function formatTime(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return "0:00";
  }

  const minutes = Math.floor(value / 60);
  const seconds = Math.floor(value % 60)
    .toString()
    .padStart(2, "0");

  return `${minutes}:${seconds}`;
}

function getSelectedLocalization(
  storyPage: StoryPageReadViewModel | null,
  languageCode: string | null,
): StoryPageLocalizationViewModel | null {
  if (!storyPage || !languageCode) {
    return null;
  }

  return (
    storyPage.localizations.find(
      (localization) => localization.languageCode === languageCode,
    ) ?? null
  );
}

function clampIndex(index: number, total: number) {
  if (total <= 0) {
    return 0;
  }

  return Math.min(Math.max(index, 0), total - 1);
}

export function StoryContentPreviewDialog({
  content,
  storyPages,
  languageCode,
  open,
  isLoading = false,
  problem = null,
  onRetry,
  onOpenChange,
  onEditPage,
}: StoryContentPreviewDialogProps) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const lastAudioKeyRef = useRef<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [hasFinished, setHasFinished] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [playbackProblem, setPlaybackProblem] = useState<string | null>(null);
  const orderedStoryPages = useMemo(
    () =>
      [...storyPages].sort(
        (firstStoryPage, secondStoryPage) =>
          firstStoryPage.pageNumber - secondStoryPage.pageNumber,
      ),
    [storyPages],
  );
  const activeStoryPage =
    orderedStoryPages[clampIndex(activeIndex, orderedStoryPages.length)] ??
    null;
  const activeLocalization = getSelectedLocalization(
    activeStoryPage,
    languageCode,
  );
  const selectedLanguageLabel =
    content.localizations.find(
      (localization) => localization.languageCode === languageCode,
    )?.languageLabel ??
    languageCode?.toUpperCase() ??
    "No locale";
  const illustrationAssetId = activeLocalization?.illustrationAssetId ?? null;
  const audioAssetId = activeLocalization?.audioAssetId ?? null;
  const imageDetail = useAssetDetail(open ? illustrationAssetId : null);
  const audioDetail = useAssetDetail(open ? audioAssetId : null);
  const imageAsset = imageDetail.asset;
  const audioAsset = audioDetail.asset;
  const imagePreview = useAssetPreview(
    imageAsset,
    open && imageAsset?.previewKind === "image",
  );
  const audioPreview = useAssetPreview(
    audioAsset,
    open && audioAsset?.previewKind === "audio",
  );
  const totalPages = orderedStoryPages.length;
  const activePageNumber = activeStoryPage?.pageNumber ?? null;
  const imagePreviewUrl = imagePreview.previewUrl;
  const audioPreviewUrl = audioPreview.previewUrl;

  const blockingIssue = (() => {
    if (problem) {
      return {
        title: "Story pages could not be loaded",
        description: problem.detail,
        canRetry: true,
      } satisfies PreviewIssue;
    }

    if (isLoading) {
      return null;
    }

    if (!languageCode) {
      return {
        title: "No preview locale selected",
        description: "Select a content localization before previewing pages.",
      } satisfies PreviewIssue;
    }

    if (!activeStoryPage) {
      return {
        title: "No story pages available",
        description: "Add story pages before opening the preview player.",
      } satisfies PreviewIssue;
    }

    if (!activeLocalization) {
      return {
        title: `Page ${activeStoryPage.pageNumber} is missing ${selectedLanguageLabel}`,
        description:
          "This page does not have a localization for the selected preview language.",
      } satisfies PreviewIssue;
    }

    if (!illustrationAssetId) {
      return {
        title: `Page ${activeStoryPage.pageNumber} is missing an illustration`,
        description: "Attach a localized illustration before preview playback.",
      } satisfies PreviewIssue;
    }

    if (!audioAssetId) {
      return {
        title: `Page ${activeStoryPage.pageNumber} is missing audio`,
        description: "Attach localized narration before preview playback.",
      } satisfies PreviewIssue;
    }

    if (imageDetail.problem) {
      return {
        title: "Illustration asset could not be loaded",
        description: imageDetail.problem.detail,
        canRetry: true,
      } satisfies PreviewIssue;
    }

    if (audioDetail.problem) {
      return {
        title: "Audio asset could not be loaded",
        description: audioDetail.problem.detail,
        canRetry: true,
      } satisfies PreviewIssue;
    }

    if (imageAsset && imageAsset.previewKind !== "image") {
      return {
        title: "Illustration asset is not an image",
        description: `Asset #${imageAsset.id} cannot render as the page illustration.`,
      } satisfies PreviewIssue;
    }

    if (audioAsset && audioAsset.previewKind !== "audio") {
      return {
        title: "Audio asset is not playable audio",
        description: `Asset #${audioAsset.id} cannot play as page narration.`,
      } satisfies PreviewIssue;
    }

    if (imagePreview.previewStatus === "error") {
      return {
        title: "Illustration preview could not be loaded",
        description:
          imagePreview.previewErrorMessage ??
          "Request a fresh preview token and try again.",
        canRetry: true,
      } satisfies PreviewIssue;
    }

    if (audioPreview.previewStatus === "error") {
      return {
        title: "Audio preview could not be loaded",
        description:
          audioPreview.previewErrorMessage ??
          "Request a fresh preview token and try again.",
        canRetry: true,
      } satisfies PreviewIssue;
    }

    return null;
  })();

  const isMediaLoading =
    open &&
    !blockingIssue &&
    (isLoading ||
      imageDetail.isLoading ||
      audioDetail.isLoading ||
      imagePreview.previewStatus === "loading" ||
      audioPreview.previewStatus === "loading");
  const canPlayActivePage =
    !blockingIssue &&
    !isMediaLoading &&
    Boolean(activeStoryPage && imagePreviewUrl && audioPreviewUrl);
  const progressValue =
    duration > 0
      ? Math.min(100, Math.max(0, (currentTime / duration) * 100))
      : 0;

  useEffect(() => {
    if (!open) {
      audioRef.current?.pause();
      setIsPlaying(false);
      return;
    }

    setActiveIndex(0);
    setIsPlaying(true);
    setHasFinished(false);
    setCurrentTime(0);
    setDuration(0);
    setPlaybackProblem(null);
    lastAudioKeyRef.current = null;
  }, [languageCode, open]);

  useEffect(() => {
    if (activeIndex > orderedStoryPages.length - 1) {
      setActiveIndex(clampIndex(activeIndex, orderedStoryPages.length));
    }
  }, [activeIndex, orderedStoryPages.length]);

  useEffect(() => {
    if (!blockingIssue) {
      return;
    }

    audioRef.current?.pause();
    setIsPlaying(false);
  }, [blockingIssue]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const audioKey =
      activePageNumber && audioPreviewUrl
        ? `${activePageNumber}:${audioPreviewUrl}`
        : null;

    if (!audioKey || lastAudioKeyRef.current === audioKey) {
      return;
    }

    lastAudioKeyRef.current = audioKey;
    setCurrentTime(0);
    setDuration(0);
    setPlaybackProblem(null);
    if (audioRef.current) {
      audioRef.current.currentTime = 0;
    }
  }, [activePageNumber, audioPreviewUrl, open]);

  useEffect(() => {
    if (!open || !isPlaying || !canPlayActivePage) {
      return;
    }

    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    try {
      const playResult = audio.play();
      if (playResult && typeof playResult.catch === "function") {
        void playResult.catch(() => {
          setIsPlaying(false);
          setPlaybackProblem(
            "The browser blocked autoplay. Press Play to start this page audio.",
          );
        });
      }
    } catch {
      setIsPlaying(false);
      setPlaybackProblem(
        "The browser blocked autoplay. Press Play to start this page audio.",
      );
    }
  }, [audioPreviewUrl, canPlayActivePage, isPlaying, open]);

  function goToPageIndex(nextIndex: number) {
    if (totalPages === 0) {
      return;
    }

    setActiveIndex(clampIndex(nextIndex, totalPages));
    setIsPlaying(true);
    setHasFinished(false);
    setPlaybackProblem(null);
  }

  function handlePlayPause() {
    if (isPlaying) {
      audioRef.current?.pause();
      setIsPlaying(false);
      return;
    }

    if (hasFinished) {
      setActiveIndex(0);
      setHasFinished(false);
    }

    setPlaybackProblem(null);
    setIsPlaying(true);
  }

  function handleAudioEnded() {
    if (activeIndex < totalPages - 1) {
      goToPageIndex(activeIndex + 1);
      return;
    }

    setIsPlaying(false);
    setHasFinished(true);
  }

  function handleLoadedMetadata() {
    const audio = audioRef.current;
    setDuration(audio?.duration ?? 0);
  }

  function handleTimeUpdate() {
    const audio = audioRef.current;
    setCurrentTime(audio?.currentTime ?? 0);
  }

  function handleSeek(value: string) {
    const nextTime = Number(value);
    if (!Number.isFinite(nextTime) || !audioRef.current) {
      return;
    }

    audioRef.current.currentTime = nextTime;
    setCurrentTime(nextTime);
  }

  async function handleRetry() {
    if (problem) {
      onRetry?.();
    }
    if (imageDetail.problem) {
      await imageDetail.refetch();
    }
    if (audioDetail.problem) {
      await audioDetail.refetch();
    }
    if (imagePreview.previewStatus === "error") {
      await imagePreview.refreshPreview({ force: true });
    }
    if (audioPreview.previewStatus === "error") {
      await audioPreview.refreshPreview({ force: true });
    }
    setPlaybackProblem(null);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-5xl">
        <DialogHeader>
          <div className="flex flex-col gap-3 pr-10 sm:flex-row sm:items-start sm:justify-between">
            <div className="space-y-1">
              <DialogTitle>Preview story</DialogTitle>
              <DialogDescription>
                {selectedLanguageLabel} pages play with their localized audio.
              </DialogDescription>
            </div>
            <div className="flex items-center gap-2 rounded-full border border-border/70 bg-muted/30 px-3 py-1 text-xs font-medium text-muted-foreground">
              <Volume2 className="size-3.5" />
              {activePageNumber
                ? `Page ${activePageNumber} of ${totalPages}`
                : `${totalPages} pages`}
            </div>
          </div>
        </DialogHeader>

        <DialogBody className="grid gap-4 pt-5">
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_14rem]">
            <div className="min-h-[22rem] rounded-2xl border border-border/70 bg-muted/20 p-3 sm:min-h-[30rem]">
              <div className="flex h-full min-h-[20rem] items-center justify-center rounded-xl border border-dashed border-border/70 bg-background/90 p-3 sm:min-h-[28rem]">
                {isMediaLoading ? (
                  <div className="flex flex-col items-center gap-3 text-sm text-muted-foreground">
                    <LoaderCircle className="size-6 animate-spin" />
                    Loading page media...
                  </div>
                ) : blockingIssue ? (
                  <EmptyState
                    action={
                      activePageNumber ? (
                        <Button
                          type="button"
                          onClick={() => onEditPage(activePageNumber)}
                        >
                          <Pencil className="size-4" />
                          Edit page
                        </Button>
                      ) : null
                    }
                    className="min-h-full w-full border-0 bg-transparent"
                    description={blockingIssue.description}
                    icon={AlertTriangle}
                    title={blockingIssue.title}
                  />
                ) : imagePreviewUrl && activePageNumber ? (
                  <img
                    alt={`Story page ${activePageNumber} illustration preview`}
                    className="max-h-[30rem] w-full rounded-xl object-contain"
                    src={imagePreviewUrl}
                  />
                ) : (
                  <EmptyState
                    className="min-h-full w-full border-0 bg-transparent"
                    description="The page image preview is not available yet."
                    icon={ImageIcon}
                    title="Preview unavailable"
                  />
                )}
              </div>
            </div>

            <div className="grid content-start gap-3 rounded-2xl border border-border/70 bg-card/95 p-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Active page
                </p>
                <p className="mt-1 text-lg font-semibold text-foreground">
                  {activePageNumber ? `Page ${activePageNumber}` : "No page"}
                </p>
                <p className="text-sm text-muted-foreground">
                  {activeLocalization?.hasBodyText
                    ? activeLocalization.bodyText
                    : "No body text for this preview locale."}
                </p>
              </div>

              <div className="grid gap-2">
                {orderedStoryPages.map((storyPage, index) => {
                  const localization = getSelectedLocalization(
                    storyPage,
                    languageCode,
                  );
                  const isActive = index === activeIndex;

                  return (
                    <button
                      key={storyPage.pageNumber}
                      type="button"
                      className={`flex items-center justify-between rounded-lg border px-3 py-2 text-left text-sm transition-colors ${
                        isActive
                          ? "border-primary/40 bg-primary/10 text-foreground"
                          : "border-border/70 bg-background hover:bg-muted"
                      }`}
                      onClick={() => goToPageIndex(index)}
                    >
                      <span>Page {storyPage.pageNumber}</span>
                      <span className="text-xs text-muted-foreground">
                        {localization?.hasAudioAsset &&
                        localization.hasIllustration
                          ? "Ready"
                          : "Needs media"}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <audio
            ref={audioRef}
            aria-label={
              activePageNumber
                ? `Audio preview for story page ${activePageNumber}`
                : "Story audio preview"
            }
            className="sr-only"
            onEnded={handleAudioEnded}
            onError={() => {
              setIsPlaying(false);
              setPlaybackProblem(
                "The browser could not play this page audio preview.",
              );
            }}
            onLoadedMetadata={handleLoadedMetadata}
            onTimeUpdate={handleTimeUpdate}
            preload="metadata"
            src={audioPreviewUrl ?? undefined}
          />

          <div className="rounded-2xl border border-border/70 bg-background p-4">
            <div className="grid gap-3">
              <div className="flex items-center gap-3">
                <span className="w-10 text-xs tabular-nums text-muted-foreground">
                  {formatTime(currentTime)}
                </span>
                <input
                  aria-label="Audio position"
                  className="h-2 min-w-0 flex-1 accent-primary"
                  disabled={!canPlayActivePage}
                  max={duration || 0}
                  min={0}
                  onChange={(event) => handleSeek(event.target.value)}
                  step={0.1}
                  type="range"
                  value={duration ? currentTime : 0}
                />
                <span className="w-10 text-right text-xs tabular-nums text-muted-foreground">
                  {formatTime(duration)}
                </span>
              </div>
              <div className="h-1 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-primary"
                  style={{ width: `${progressValue}%` }}
                />
              </div>
              {playbackProblem ? (
                <p className="text-sm text-muted-foreground" role="status">
                  {playbackProblem}
                </p>
              ) : null}
            </div>
          </div>
        </DialogBody>

        <DialogFooter className="items-center justify-between gap-3 sm:justify-between">
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={activeIndex <= 0 || totalPages === 0}
              onClick={() => goToPageIndex(activeIndex - 1)}
            >
              <ChevronLeft className="size-4" />
              Previous
            </Button>
            <Button
              type="button"
              disabled={!canPlayActivePage && !hasFinished}
              onClick={handlePlayPause}
            >
              {isPlaying ? (
                <Pause className="size-4" />
              ) : (
                <Play className="size-4" />
              )}
              {isPlaying ? "Pause" : hasFinished ? "Replay" : "Play"}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={activeIndex >= totalPages - 1 || totalPages === 0}
              onClick={() => goToPageIndex(activeIndex + 1)}
            >
              Next
              <ChevronRight className="size-4" />
            </Button>
            {blockingIssue?.canRetry ? (
              <Button
                type="button"
                variant="outline"
                onClick={() => void handleRetry()}
              >
                <RefreshCw className="size-4" />
                Retry preview
              </Button>
            ) : null}
          </div>
          <Button
            type="button"
            variant="outline"
            disabled={!activePageNumber}
            onClick={() => {
              if (activePageNumber) {
                onEditPage(activePageNumber);
              }
            }}
          >
            <Pencil className="size-4" />
            Edit page
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
