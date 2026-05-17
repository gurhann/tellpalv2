import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { StoryPagesRoute } from "@/app/routes/story-pages";
import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";
import { storyPageViewModels } from "@/features/story-pages/test/fixtures";

const contentDetailHookMocks = vi.hoisted(() => ({
  useContentDetail: vi.fn(),
}));
const storyPageHookMocks = vi.hoisted(() => ({
  useStoryPages: vi.fn(),
  useStoryPage: vi.fn(),
}));
const storyPageMutationMocks = vi.hoisted(() => ({
  useStoryPageActions: vi.fn(),
}));
const contentMutationMocks = vi.hoisted(() => ({
  useSaveContent: vi.fn(),
}));
const recentImageAssetHookMocks = vi.hoisted(() => ({
  useRecentImageAssets: vi.fn(),
}));
const recentAudioAssetHookMocks = vi.hoisted(() => ({
  useRecentAudioAssets: vi.fn(),
}));
const assetDetailHookMocks = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));
const assetPreviewHookMocks = vi.hoisted(() => ({
  useAssetPreview: vi.fn(),
}));
const uploadAssetHookMocks = vi.hoisted(() => ({
  useUploadAsset: vi.fn(),
}));
const assetAdminApiMocks = vi.hoisted(() => ({
  getAsset: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentDetailHookMocks.useContentDetail,
}));

vi.mock("@/features/story-pages/queries/use-story-pages", () => ({
  useStoryPages: storyPageHookMocks.useStoryPages,
  useStoryPage: storyPageHookMocks.useStoryPage,
}));

vi.mock("@/features/story-pages/mutations/use-story-page-actions", () => ({
  useStoryPageActions: storyPageMutationMocks.useStoryPageActions,
}));

vi.mock("@/features/contents/mutations/use-save-content", () => ({
  useSaveContent: contentMutationMocks.useSaveContent,
}));

vi.mock("@/features/story-pages/queries/use-recent-image-assets", () => ({
  useRecentImageAssets: recentImageAssetHookMocks.useRecentImageAssets,
}));

vi.mock("@/features/story-pages/queries/use-recent-audio-assets", () => ({
  useRecentAudioAssets: recentAudioAssetHookMocks.useRecentAudioAssets,
}));

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMocks.useAssetDetail,
}));

vi.mock("@/features/assets/queries/use-asset-preview", () => ({
  useAssetPreview: assetPreviewHookMocks.useAssetPreview,
}));

vi.mock("@/features/assets/mutations/use-upload-asset", () => ({
  useUploadAsset: uploadAssetHookMocks.useUploadAsset,
}));

vi.mock("@/features/assets/api/asset-admin", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/features/assets/api/asset-admin")>();

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      getAsset: assetAdminApiMocks.getAsset,
    },
  };
});

function makeDetailState(overrides: Record<string, unknown> = {}) {
  return {
    content: storyContentViewModel,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn(),
    ...overrides,
  };
}

function makeStoryPageState(overrides: Record<string, unknown> = {}) {
  return {
    storyPages: storyPageViewModels,
    isLoading: false,
    isFetching: false,
    isSuccess: true,
    problem: null,
    refetch: vi.fn(),
    ...overrides,
  };
}

function makeStoryPageQuery(pageNumber?: number | null) {
  const matchingStoryPage = storyPageViewModels.find(
    (storyPage) => storyPage.pageNumber === pageNumber,
  );

  if (matchingStoryPage) {
    return matchingStoryPage;
  }

  if (pageNumber === 3) {
    return {
      ...storyPageViewModels[0],
      pageNumber: 3,
      textlessIllustrationAssetId: null,
      hasTextlessIllustration: false,
      localizationCount: 0,
      localizations: [],
      primaryLocalization: null,
      languageCodes: [],
      illustratedLocalizationCount: 0,
      missingIllustrationCount: 0,
      hasCompleteIllustrationCoverage: false,
    };
  }

  return storyPageViewModels[0];
}

function makeStoryPagesWithThree() {
  return [...storyPageViewModels, makeStoryPageQuery(3)];
}

function makeAsset(id: number, mediaType: "IMAGE" | "AUDIO"): AssetViewModel {
  const extension = mediaType === "IMAGE" ? "jpg" : "mp3";

  return mapAdminAsset({
    assetId: id,
    provider: "FIREBASE_STORAGE",
    objectPath: `/preview/story/${id}.${extension}`,
    mediaType,
    kind: mediaType === "IMAGE" ? "ORIGINAL_IMAGE" : "ORIGINAL_AUDIO",
    mimeType: mediaType === "IMAGE" ? "image/jpeg" : "audio/mpeg",
    byteSize: 1024,
    checksumSha256: null,
    cachedDownloadUrl: null,
    downloadUrlCachedAt: null,
    downloadUrlExpiresAt: null,
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
  } satisfies AdminAssetResponse);
}

const previewAssets = new Map<number, AssetViewModel>([
  [41, makeAsset(41, "IMAGE")],
  [42, makeAsset(42, "IMAGE")],
  [43, makeAsset(43, "IMAGE")],
  [501, makeAsset(501, "IMAGE")],
  [81, makeAsset(81, "AUDIO")],
  [82, makeAsset(82, "AUDIO")],
  [83, makeAsset(83, "AUDIO")],
]);

function makeAssetDetailState(assetId: number | null) {
  return {
    asset: assetId ? (previewAssets.get(assetId) ?? null) : null,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn().mockResolvedValue(undefined),
  };
}

function makeAssetPreviewState(asset: AssetViewModel | null, enabled: boolean) {
  return {
    previewUrl:
      enabled && asset
        ? `https://cdn.tellpal.test/assets/${asset.id}.${
            asset.previewKind === "audio" ? "mp3" : "jpg"
          }`
        : null,
    previewStatus: enabled && asset ? "available" : "unavailable",
    previewErrorMessage: null,
    isRefreshing: false,
    refreshPreview: vi.fn().mockResolvedValue(undefined),
  } as const;
}

function mockStoryRouteDependencies({
  storyPages = storyPageViewModels,
  upsertStoryPageLocalization = vi.fn(),
}: {
  storyPages?: typeof storyPageViewModels;
  upsertStoryPageLocalization?: ReturnType<typeof vi.fn>;
} = {}) {
  contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
  storyPageHookMocks.useStoryPages.mockReturnValue(
    makeStoryPageState({ storyPages }),
  );
  storyPageHookMocks.useStoryPage.mockImplementation(
    (_contentId, pageNumber) => ({
      storyPage: makeStoryPageQuery(pageNumber),
      isLoading: false,
      problem: null,
    }),
  );
  storyPageMutationMocks.useStoryPageActions.mockReturnValue({
    addStoryPage: { isPending: false, mutateAsync: vi.fn() },
    updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
    removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
    exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
    upsertStoryPageLocalization: {
      isPending: false,
      mutateAsync: upsertStoryPageLocalization,
    },
    isPending: false,
  });
  recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
    assets: [],
    isLoading: false,
    isSuccess: true,
    problem: null,
  });
  recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
    assets: [],
    isLoading: false,
    isSuccess: true,
    problem: null,
  });
  assetDetailHookMocks.useAssetDetail.mockImplementation(makeAssetDetailState);
  assetPreviewHookMocks.useAssetPreview.mockImplementation(
    makeAssetPreviewState,
  );

  return { upsertStoryPageLocalization };
}

function renderStoryRoute(initialEntry = "/contents/1/story-pages") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route
          path="/contents/:contentId/story-pages"
          element={
            <>
              <StoryPagesRoute />
              <LocationProbe />
            </>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

function LocationProbe() {
  const location = useLocation();

  return (
    <span data-testid="location">
      {location.pathname}
      {location.search}
    </span>
  );
}

beforeEach(() => {
  Object.defineProperty(window.HTMLMediaElement.prototype, "play", {
    configurable: true,
    value: vi.fn().mockResolvedValue(undefined),
  });
  Object.defineProperty(window.HTMLMediaElement.prototype, "pause", {
    configurable: true,
    value: vi.fn(),
  });
  assetDetailHookMocks.useAssetDetail.mockReset();
  assetDetailHookMocks.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn().mockResolvedValue(undefined),
  });
  assetPreviewHookMocks.useAssetPreview.mockReset();
  assetPreviewHookMocks.useAssetPreview.mockImplementation(
    makeAssetPreviewState,
  );
  uploadAssetHookMocks.useUploadAsset.mockReset();
  uploadAssetHookMocks.useUploadAsset.mockReturnValue({
    mutateAsync: vi.fn(),
    isPending: false,
    problem: null,
    reset: vi.fn(),
  });
  assetAdminApiMocks.getAsset.mockReset();
  assetAdminApiMocks.getAsset.mockImplementation(async (assetId: number) => ({
    assetId,
    provider: "FIREBASE_STORAGE",
    objectPath: `/preview/story/${assetId}.jpg`,
    mediaType: "IMAGE",
    kind: "ORIGINAL_IMAGE",
    mimeType: "image/jpeg",
    byteSize: 1024,
    checksumSha256: null,
    cachedDownloadUrl: null,
    downloadUrlCachedAt: null,
    downloadUrlExpiresAt: null,
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
  }));
  contentMutationMocks.useSaveContent.mockReset();
  contentMutationMocks.useSaveContent.mockReturnValue({
    mutateAsync: vi.fn().mockResolvedValue({
      ...storyContentViewModel.summary,
      contentId: storyContentViewModel.summary.id,
      type: storyContentViewModel.summary.type,
      externalKey: storyContentViewModel.summary.externalKey,
      ageRange: storyContentViewModel.summary.ageRange,
      active: storyContentViewModel.summary.active,
      textlessCoverMediaId: storyContentViewModel.summary.textlessCoverAssetId,
      pageCount: storyContentViewModel.summary.pageCount,
      createdAt: storyContentViewModel.summary.createdAt,
      updatedAt: storyContentViewModel.summary.updatedAt,
    }),
    isPending: false,
    problem: null,
    reset: vi.fn(),
  });
});

describe("StoryPagesRoute", () => {
  it("renders the story-only route shell for STORY content", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    expect(
      screen.getByRole("heading", { name: /story pages for evening garden/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/1");
    expect(
      screen.getByRole("button", { name: /preview story/i }),
    ).toBeEnabled();
    expect(
      screen.getByRole("button", { name: /export source images/i }),
    ).toBeEnabled();
    expect(
      screen.getByRole("button", { name: /add story page/i }),
    ).toBeEnabled();
    expect(screen.getByText(/^Active locale$/i)).toBeInTheDocument();
    expect(
      screen.getByRole("combobox", { name: /change active locale/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/ready in selected locale/i)).toBeInTheDocument();
    expect(screen.getByText(/page 1/i)).toBeInTheDocument();
    expect(screen.getByText(/english status/i)).toBeInTheDocument();
    expect(screen.getByText(/textless source/i)).toBeInTheDocument();
    expect(screen.getByText(/source image linked/i)).toBeInTheDocument();
    expect(screen.getAllByText(/source image missing/i).length).toBeGreaterThan(
      0,
    );
    expect(screen.getByText(/all locale coverage/i)).toBeInTheDocument();
    expect(screen.queryByText(/locale handoff/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/story readiness/i)).toBeInTheDocument();
  });

  it("keeps non-story content out of the route shell", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(
      makeDetailState({ content: inactiveContentViewModel }),
    );
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute("/contents/4/story-pages");

    expect(
      screen.getByRole("heading", { name: /story pages are unavailable/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /return to content detail/i }),
    ).toHaveAttribute("href", "/contents/4");
    expect(
      screen.queryByText(/story page collection/i),
    ).not.toBeInTheDocument();
  });

  it("enables source export when only the textless story cover is linked", () => {
    const storyPagesWithoutSourceImages = storyPageViewModels.map(
      (storyPage) => ({
        ...storyPage,
        textlessIllustrationAssetId: null,
        hasTextlessIllustration: false,
      }),
    );
    mockStoryRouteDependencies({ storyPages: storyPagesWithoutSourceImages });

    renderStoryRoute();

    expect(
      screen.getByRole("button", { name: /export source images/i }),
    ).toBeEnabled();
  });

  it("opens source image management from the route view query", () => {
    mockStoryRouteDependencies();

    renderStoryRoute("/contents/1/story-pages?view=source-images&page=2");

    expect(screen.getByRole("tab", { name: /source images/i })).toHaveAttribute(
      "data-state",
      "active",
    );
    expect(
      screen.getByRole("heading", { name: /^source images$/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /textless cover/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: /textless\/source illustration/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /page 2/i })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
  });

  it("saves the selected source page image from the source manager", async () => {
    const updateStoryPage = vi.fn().mockResolvedValue({
      pageNumber: 2,
      textlessIllustrationMediaId: 777,
    });
    mockStoryRouteDependencies();
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: updateStoryPage },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });

    renderStoryRoute("/contents/1/story-pages?view=source-images&page=2");

    fireEvent.click(
      screen.getByTestId("story-page-textless-illustration-advanced"),
    );
    fireEvent.change(
      screen.getByTestId("story-page-textless-illustration-input"),
      {
        target: { value: "777" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: /save source image/i }));

    await waitFor(() => {
      expect(updateStoryPage).toHaveBeenCalledWith({
        pageNumber: 2,
        input: {
          textlessIllustrationMediaId: 777,
        },
      });
    });
  });

  it("confirms before leaving a dirty source page image edit", async () => {
    mockStoryRouteDependencies();

    renderStoryRoute("/contents/1/story-pages?view=source-images&page=1");

    fireEvent.click(
      screen.getByTestId("story-page-textless-illustration-advanced"),
    );
    fireEvent.change(
      screen.getByTestId("story-page-textless-illustration-input"),
      {
        target: { value: "777" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: /page 2/i }));

    expect(
      screen.getByRole("heading", { name: /unsaved source image change/i }),
    ).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: /discard changes and continue/i }),
    );

    await waitFor(() => {
      expect(screen.getByTestId("location")).toHaveTextContent(
        "/contents/1/story-pages?view=source-images&page=2",
      );
    });
  });

  it("opens the page editor with parent language workspaces", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 1/i }));

    expect(
      screen.getByRole("heading", { name: /page 1 .* english/i }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/english/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/turkish/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/illustration asset/i).length).toBeGreaterThan(
      0,
    );
    expect(
      screen.queryByRole("heading", { name: /textless\/source illustration/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: /save page localization/i }).length,
    ).toBeGreaterThan(0);
  });

  it("opens the page editor focused on the locale requested by the content detail route", () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute("/contents/1/story-pages?language=tr");

    fireEvent.click(screen.getByRole("button", { name: /edit page 1/i }));

    expect(screen.getByText(/parent locale: aksam bahcesi/i)).toBeVisible();
  });

  it("changes the active story page language from the workspace toolbar", async () => {
    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: vi.fn() },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    fireEvent.click(
      screen.getByRole("combobox", { name: /change active locale/i }),
    );
    fireEvent.click(await screen.findByRole("option", { name: /turkish/i }));

    expect(
      screen.getByRole("combobox", { name: /change active locale/i }),
    ).toHaveTextContent(/turkish/i);
    expect(screen.getByText(/turkish status/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /edit page 1/i }));

    expect(screen.getByText(/parent locale: aksam bahcesi/i)).toBeVisible();
  });

  it("shows previous and next controls in the page editor header", () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 2/i }));

    expect(
      screen.getByRole("heading", { name: /page 2 .* english/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /previous page/i }),
    ).toBeEnabled();
    expect(screen.getByRole("button", { name: /next page/i })).toBeEnabled();
  });

  it("moves to the previous and next story pages without closing the editor", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 2/i }));
    fireEvent.click(screen.getByRole("button", { name: /previous page/i }));

    expect(
      await screen.findByRole("heading", { name: /page 1 .* english/i }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /next page/i }));
    fireEvent.click(screen.getByRole("button", { name: /next page/i }));

    expect(
      await screen.findByRole("heading", { name: /page 3 .* english/i }),
    ).toBeInTheDocument();
  });

  it("disables boundary navigation controls", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 1/i }));

    expect(
      screen.getByRole("button", { name: /previous page/i }),
    ).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /next page/i }));
    fireEvent.click(screen.getByRole("button", { name: /next page/i }));

    expect(
      await screen.findByRole("heading", { name: /page 3 .* english/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /next page/i })).toBeDisabled();
  });

  it("keeps the active editor language while navigating story pages", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute("/contents/1/story-pages?language=tr");

    fireEvent.click(screen.getByRole("button", { name: /continue page 2/i }));

    expect(
      screen.getByRole("heading", { name: /page 2 .* turkish/i }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /next page/i }));

    expect(
      await screen.findByRole("heading", { name: /page 3 .* turkish/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/parent locale: aksam bahcesi/i)).toBeVisible();
  });

  it("opens the editor from the page query param and clears page on close", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute("/contents/1/story-pages?language=tr&page=2");

    expect(
      await screen.findByRole("heading", { name: /page 2 .* turkish/i }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /close editor/i }));

    await waitFor(() => {
      expect(
        screen.queryByRole("heading", { name: /page 2 .* turkish/i }),
      ).not.toBeInTheDocument();
    });
    expect(screen.getByTestId("location")).toHaveTextContent(
      "/contents/1/story-pages?language=tr",
    );
  });

  it("opens the current story page editor from the route preview", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute("/contents/1/story-pages?language=en");

    fireEvent.click(screen.getByRole("button", { name: /preview story/i }));

    expect(
      await screen.findByRole("heading", { name: /preview story/i }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^edit page$/i }));

    expect(
      await screen.findByRole("heading", { name: /page 1 .* english/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: /preview story/i }),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(
      "/contents/1/story-pages?language=en&page=1",
    );
  });

  it("asks before discarding dirty editor changes during page navigation", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 2/i }));
    fireEvent.change(screen.getAllByLabelText(/body text/i)[0]!, {
      target: { value: "Unsaved page draft." },
    });

    fireEvent.click(screen.getByRole("button", { name: /next page/i }));

    expect(
      await screen.findByRole("heading", {
        name: /discard unsaved changes/i,
      }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /stay on page/i }));

    expect(
      screen.getByRole("heading", { name: /page 2 .* english/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: /discard unsaved changes/i }),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /next page/i }));
    fireEvent.click(
      screen.getByRole("button", { name: /discard changes and continue/i }),
    );

    expect(
      await screen.findByRole("heading", { name: /page 3 .* english/i }),
    ).toBeInTheDocument();
  });

  it("asks before discarding dirty editor changes when closing the editor", async () => {
    mockStoryRouteDependencies({ storyPages: makeStoryPagesWithThree() });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /edit page 2/i }));
    fireEvent.change(screen.getAllByLabelText(/body text/i)[0]!, {
      target: { value: "Unsaved page draft." },
    });

    fireEvent.click(screen.getByRole("button", { name: /close editor/i }));

    expect(
      await screen.findByRole("heading", {
        name: /discard unsaved changes/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/discard them before closing the editor/i),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /stay on page/i }));

    expect(
      screen.getByRole("heading", { name: /page 2 .* english/i }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /close editor/i }));
    fireEvent.click(
      screen.getByRole("button", { name: /discard changes and close/i }),
    );

    await waitFor(() => {
      expect(
        screen.queryByRole("heading", { name: /page 2 .* english/i }),
      ).not.toBeInTheDocument();
    });
  });

  it("creates a page immediately and opens the editor", async () => {
    const addStoryPage = vi.fn().mockResolvedValue({
      contentId: 1,
      pageNumber: 3,
      textlessIllustrationMediaId: null,
      localizationCount: 0,
    });

    contentDetailHookMocks.useContentDetail.mockReturnValue(makeDetailState());
    storyPageHookMocks.useStoryPages.mockReturnValue(makeStoryPageState());
    storyPageHookMocks.useStoryPage.mockImplementation(
      (_contentId, pageNumber) => ({
        storyPage: makeStoryPageQuery(pageNumber),
        isLoading: false,
        problem: null,
      }),
    );
    storyPageMutationMocks.useStoryPageActions.mockReturnValue({
      addStoryPage: { isPending: false, mutateAsync: addStoryPage },
      updateStoryPage: { isPending: false, mutateAsync: vi.fn() },
      removeStoryPage: { isPending: false, mutateAsync: vi.fn() },
      exportTextlessIllustrations: { isPending: false, mutateAsync: vi.fn() },
      upsertStoryPageLocalization: { isPending: false, mutateAsync: vi.fn() },
      isPending: false,
    });
    recentImageAssetHookMocks.useRecentImageAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    recentAudioAssetHookMocks.useRecentAudioAssets.mockReturnValue({
      assets: [],
      isLoading: false,
      isSuccess: true,
      problem: null,
    });
    assetDetailHookMocks.useAssetDetail.mockReturnValue({
      asset: null,
      isLoading: false,
      problem: null,
      isNotFound: false,
    });

    renderStoryRoute();

    fireEvent.click(screen.getByRole("button", { name: /^add story page$/i }));

    expect(addStoryPage).toHaveBeenCalledWith({ afterPageNumber: null });
    expect(
      await screen.findByRole("heading", { name: /page 3 .* english/i }),
    ).toBeInTheDocument();
  });
});
