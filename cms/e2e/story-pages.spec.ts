import { expect, test } from "@playwright/test";

type SessionPayload = {
  adminUserId: number;
  username: string;
  roleCodes: string[];
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

type ContentReadResponse = {
  contentId: number;
  type: "STORY" | "AUDIO_STORY" | "MEDITATION" | "LULLABY";
  externalKey: string;
  active: boolean;
  ageRange: number | null;
  pageCount: number | null;
  localizations: Array<{
    contentId: number;
    languageCode: string;
    title: string;
    description: string | null;
    bodyText: string | null;
    coverMediaId: number | null;
    audioMediaId: number | null;
    durationMinutes: number | null;
    status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
    processingStatus: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
    publishedAt: string | null;
    visibleToMobile: boolean;
  }>;
};

type StoryPageLocalizationResponse = {
  contentId: number;
  pageNumber: number;
  languageCode: string;
  bodyText: string | null;
  audioMediaId: number | null;
  illustrationMediaId: number;
};

type StoryPageReadResponse = {
  contentId: number;
  pageNumber: number;
  localizationCount: number;
  localizations: StoryPageLocalizationResponse[];
};

type AdminAssetResponse = {
  assetId: number;
  provider: "LOCAL_STUB";
  objectPath: string;
  mediaType: "IMAGE" | "AUDIO" | "ARCHIVE";
  kind:
    | "ORIGINAL_IMAGE"
    | "ORIGINAL_AUDIO"
    | "THUMBNAIL_PHONE"
    | "THUMBNAIL_TABLET"
    | "DETAIL_PHONE"
    | "DETAIL_TABLET"
    | "OPTIMIZED_AUDIO"
    | "CONTENT_ZIP"
    | "CONTENT_ZIP_PART1"
    | "CONTENT_ZIP_PART2";
  mimeType: string | null;
  byteSize: number | null;
  checksumSha256: string | null;
  cachedDownloadUrl: string | null;
  downloadUrlCachedAt: string | null;
  downloadUrlExpiresAt: string | null;
  createdAt: string;
  updatedAt: string;
};

function makeSession(overrides: Partial<SessionPayload> = {}): SessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-29T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-04-28T10:00:00Z",
    ...overrides,
  };
}

function makeAsset(
  assetId: number,
  mediaType: AdminAssetResponse["mediaType"],
  objectPath: string,
): AdminAssetResponse {
  return {
    assetId,
    provider: "LOCAL_STUB",
    objectPath,
    mediaType,
    kind: mediaType === "IMAGE" ? "ORIGINAL_IMAGE" : "ORIGINAL_AUDIO",
    mimeType: mediaType === "IMAGE" ? "image/jpeg" : "audio/mpeg",
    byteSize: null,
    checksumSha256: null,
    cachedDownloadUrl: null,
    downloadUrlCachedAt: null,
    downloadUrlExpiresAt: null,
    createdAt: "2026-03-31T12:00:00Z",
    updatedAt: "2026-03-31T12:00:00Z",
  };
}

test("story pages keep illustration assets per locale", async ({ page }) => {
  const session = makeSession();
  const contentDetail: ContentReadResponse = {
    contentId: 1,
    type: "STORY",
    externalKey: "story.evening-garden",
    active: true,
    ageRange: 5,
    pageCount: 1,
    localizations: [
      {
        contentId: 1,
        languageCode: "en",
        title: "Evening Garden",
        description: "A calm walk through a moonlit garden.",
        bodyText: null,
        coverMediaId: null,
        audioMediaId: null,
        durationMinutes: 8,
        status: "PUBLISHED",
        processingStatus: "COMPLETED",
        publishedAt: "2026-03-17T09:00:00Z",
        visibleToMobile: true,
      },
      {
        contentId: 1,
        languageCode: "tr",
        title: "Aksam Bahcesi",
        description: "Aksam icin sakin bir uyku hikayesi.",
        bodyText: null,
        coverMediaId: null,
        audioMediaId: null,
        durationMinutes: 8,
        status: "DRAFT",
        processingStatus: "PROCESSING",
        publishedAt: null,
        visibleToMobile: false,
      },
    ],
  };

  const storyPage: StoryPageReadResponse = {
    contentId: 1,
    pageNumber: 1,
    localizationCount: 2,
    localizations: [
      {
        contentId: 1,
        pageNumber: 1,
        languageCode: "en",
        bodyText: "Look at the moon over the garden gate.",
        audioMediaId: 81,
        illustrationMediaId: 41,
      },
      {
        contentId: 1,
        pageNumber: 1,
        languageCode: "tr",
        bodyText: "Bahce kapisinin ustundeki aya bak.",
        audioMediaId: 82,
        illustrationMediaId: 42,
      },
    ],
  };

  const assets = [
    makeAsset(41, "IMAGE", "/content/story/evening-garden/en/page-1-v1.jpg"),
    makeAsset(42, "IMAGE", "/content/story/evening-garden/tr/page-1-v1.jpg"),
    makeAsset(51, "IMAGE", "/content/story/evening-garden/en/page-1-v2.jpg"),
    makeAsset(52, "IMAGE", "/content/story/evening-garden/tr/page-1-v2.jpg"),
    makeAsset(81, "AUDIO", "/content/story/evening-garden/en/page-1.mp3"),
    makeAsset(82, "AUDIO", "/content/story/evening-garden/tr/page-1.mp3"),
  ];

  await page.addInitScript(() => {
    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");
  });

  await page.route("**/api/admin/auth/refresh", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(session),
    });
  });

  await page.route("**/api/admin/contents/1", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(contentDetail),
    });
  });

  await page.route("**/api/admin/contents/1/story-pages", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([storyPage]),
    });
  });

  await page.route("**/api/admin/contents/1/story-pages/1", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(storyPage),
      });
      return;
    }

    await route.fallback();
  });

  await page.route(
    "**/api/admin/contents/1/story-pages/1/localizations/*",
    async (route) => {
      const request = route.request();

      if (request.method() !== "PUT") {
        await route.fallback();
        return;
      }

      const payload = request.postDataJSON() as {
        bodyText?: string | null;
        audioMediaId?: number | null;
        illustrationMediaId: number;
      };
      const languageCode = request.url().split("/").pop() ?? "en";
      const localizationIndex = storyPage.localizations.findIndex(
        (localization) => localization.languageCode === languageCode,
      );
      const nextLocalization: StoryPageLocalizationResponse = {
        contentId: 1,
        pageNumber: 1,
        languageCode,
        bodyText: payload.bodyText ?? null,
        audioMediaId: payload.audioMediaId ?? null,
        illustrationMediaId: payload.illustrationMediaId,
      };

      if (localizationIndex >= 0) {
        storyPage.localizations[localizationIndex] = nextLocalization;
      } else {
        storyPage.localizations.push(nextLocalization);
      }

      storyPage.localizationCount = storyPage.localizations.length;

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(nextLocalization),
      });
    },
  );

  await page.route("**/api/admin/media?**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(assets),
    });
  });

  await page.route("**/api/admin/media/*", async (route) => {
    const assetId = Number(route.request().url().split("/").pop());
    const asset = assets.find((entry) => entry.assetId === assetId);

    if (!asset) {
      await route.fulfill({
        status: 404,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Asset not found",
          status: 404,
          detail: `Asset ${assetId} was not found.`,
          errorCode: "asset_not_found",
          path: `/api/admin/media/${assetId}`,
        }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(asset),
    });
  });

  await page.goto("/contents/1/story-pages");

  await expect(
    page.getByRole("heading", { name: /story pages for evening garden/i }),
  ).toBeVisible();

  await page.getByRole("button", { name: /edit/i }).click();

  const illustrationField = page.getByLabel(/illustration asset id/i);

  await expect(
    page.getByRole("heading", { name: /edit story page/i }),
  ).toBeVisible();
  await expect(illustrationField).toHaveValue("41");

  await illustrationField.fill("51");
  await page.getByRole("button", { name: /save page localization/i }).click();
  await expect(illustrationField).toHaveValue("51");

  await page.getByRole("tab", { name: /turkish/i }).click();
  await expect(illustrationField).toHaveValue("42");

  await illustrationField.fill("52");
  await page.getByRole("button", { name: /save page localization/i }).click();
  await expect(illustrationField).toHaveValue("52");

  await page.getByRole("tab", { name: /english/i }).click();
  await expect(illustrationField).toHaveValue("51");
});
