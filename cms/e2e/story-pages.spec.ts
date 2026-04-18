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
  const tinyImageDataUrl =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8B9pQAAAAASUVORK5CYII=";
  const tinyAudioDataUrl =
    "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YQAAAAA=";
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

  await page.route(
    "**/api/admin/media/*/download-url-cache/refresh",
    async (route) => {
      const segments = new URL(route.request().url()).pathname.split("/");
      const assetId = Number(segments.at(-2));
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
            path: `/api/admin/media/${assetId}/download-url-cache/refresh`,
          }),
        });
        return;
      }

      asset.cachedDownloadUrl =
        asset.mediaType === "IMAGE" ? tinyImageDataUrl : tinyAudioDataUrl;
      asset.downloadUrlCachedAt = "2026-03-31T12:10:00Z";
      asset.downloadUrlExpiresAt = "2026-03-31T14:10:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(asset),
      });
    },
  );

  await page.goto("/contents/1/story-pages");

  await expect(
    page.getByRole("heading", { name: /story pages for evening garden/i }),
  ).toBeVisible();

  await page.getByRole("button", { name: /edit page 1/i }).click();
  const editorDialog = page.getByRole("dialog", { name: /edit story page/i });
  await editorDialog
    .getByRole("button", { name: /advanced/i })
    .nth(0)
    .click({ force: true });
  let illustrationField = editorDialog.getByLabel(/illustration asset id/i);

  await expect(editorDialog).toBeVisible();
  await expect(illustrationField).toHaveValue("41");

  await illustrationField.fill("51");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response
          .url()
          .includes("/api/admin/contents/1/story-pages/1/localizations/en") &&
        response.request().method() === "PUT",
    ),
    editorDialog.locator("form").evaluate((form) => {
      (form as HTMLFormElement).requestSubmit();
    }),
  ]);
  await expect(illustrationField).toHaveValue("51");

  await page.getByRole("tab", { name: /turkish/i }).click();
  await editorDialog
    .getByRole("button", { name: /advanced/i })
    .nth(0)
    .click({ force: true });
  illustrationField = editorDialog.getByLabel(/illustration asset id/i);
  await expect(illustrationField).toHaveValue("42");

  await illustrationField.fill("52");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response
          .url()
          .includes("/api/admin/contents/1/story-pages/1/localizations/tr") &&
        response.request().method() === "PUT",
    ),
    editorDialog.locator("form").evaluate((form) => {
      (form as HTMLFormElement).requestSubmit();
    }),
  ]);
  await expect(illustrationField).toHaveValue("52");

  await page.getByRole("tab", { name: /english/i }).click();
  await editorDialog
    .getByRole("button", { name: /advanced/i })
    .nth(0)
    .click({ force: true });
  illustrationField = editorDialog.getByLabel(/illustration asset id/i);
  await expect(illustrationField).toHaveValue("51");
});

test("story pages can be added, localized, and deleted in one editor flow", async ({
  page,
}) => {
  const session = makeSession();
  const tinyImageDataUrl =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8B9pQAAAAASUVORK5CYII=";
  const tinyAudioDataUrl =
    "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YQAAAAA=";
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
  const storyPages: StoryPageReadResponse[] = [
    {
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
    },
  ];
  const assets = [
    makeAsset(41, "IMAGE", "/content/story/evening-garden/en/page-1.jpg"),
    makeAsset(42, "IMAGE", "/content/story/evening-garden/tr/page-1.jpg"),
    makeAsset(52, "IMAGE", "/content/story/evening-garden/tr/page-2.jpg"),
    makeAsset(81, "AUDIO", "/content/story/evening-garden/en/page-1.mp3"),
    makeAsset(82, "AUDIO", "/content/story/evening-garden/tr/page-1.mp3"),
    makeAsset(84, "AUDIO", "/content/story/evening-garden/tr/page-2.mp3"),
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
      body: JSON.stringify({
        ...contentDetail,
        pageCount: storyPages.length,
      }),
    });
  });

  await page.route("**/api/admin/contents/1/story-pages", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(storyPages),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as { pageNumber: number };
      storyPages.push({
        contentId: 1,
        pageNumber: body.pageNumber,
        localizationCount: 0,
        localizations: [],
      });

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          contentId: 1,
          pageNumber: body.pageNumber,
          localizationCount: 0,
        }),
      });
      return;
    }

    await route.fallback();
  });

  await page.route("**/api/admin/contents/1/story-pages/2", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          storyPages.find((entry) => entry.pageNumber === 2),
        ),
      });
      return;
    }

    if (request.method() === "DELETE") {
      const nextIndex = storyPages.findIndex((entry) => entry.pageNumber === 2);

      if (nextIndex >= 0) {
        storyPages.splice(nextIndex, 1);
      }

      await route.fulfill({
        status: 204,
        body: "",
      });
      return;
    }

    await route.fallback();
  });

  await page.route(
    "**/api/admin/contents/1/story-pages/2/localizations/*",
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
      const languageCode = request.url().split("/").pop() ?? "tr";
      const storyPage = storyPages.find((entry) => entry.pageNumber === 2)!;
      const localization = {
        contentId: 1,
        pageNumber: 2,
        languageCode,
        bodyText: payload.bodyText ?? null,
        audioMediaId: payload.audioMediaId ?? null,
        illustrationMediaId: payload.illustrationMediaId,
      };

      storyPage.localizations.push(localization);
      storyPage.localizationCount = storyPage.localizations.length;

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(localization),
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

    await route.fulfill({
      status: asset ? 200 : 404,
      contentType: asset ? "application/json" : "application/problem+json",
      body: JSON.stringify(
        asset ?? {
          type: "about:blank",
          title: "Asset not found",
          status: 404,
          detail: `Asset ${assetId} was not found.`,
          errorCode: "asset_not_found",
          path: `/api/admin/media/${assetId}`,
        },
      ),
    });
  });

  await page.route(
    "**/api/admin/media/*/download-url-cache/refresh",
    async (route) => {
      const segments = new URL(route.request().url()).pathname.split("/");
      const assetId = Number(segments.at(-2));
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
            path: `/api/admin/media/${assetId}/download-url-cache/refresh`,
          }),
        });
        return;
      }

      asset.cachedDownloadUrl =
        asset.mediaType === "IMAGE" ? tinyImageDataUrl : tinyAudioDataUrl;
      asset.downloadUrlCachedAt = "2026-03-31T12:10:00Z";
      asset.downloadUrlExpiresAt = "2026-03-31T14:10:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(asset),
      });
    },
  );

  await page.goto("/contents/1/story-pages");

  await expect(
    page.getByRole("heading", { name: /story pages for evening garden/i }),
  ).toBeVisible();
  const storyPageTable = page.getByRole("table", { name: /story page table/i });

  await page.getByRole("button", { name: /^add story page$/i }).click();
  await page.getByLabel(/page number/i).fill("2");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^add story page$/i })
    .click();

  await expect(
    storyPageTable.getByText("Page 2", { exact: true }),
  ).toBeVisible();

  await storyPageTable
    .getByRole("button", { name: /^edit page 2$/i })
    .click();
  const createEditorDialog = page.getByRole("dialog", {
    name: /edit story page/i,
  });
  await createEditorDialog.getByRole("tab", { name: /turkish/i }).click();
  await createEditorDialog
    .getByRole("button", { name: /advanced/i })
    .nth(0)
    .click({ force: true });
  await createEditorDialog
    .getByRole("button", { name: /advanced/i })
    .nth(1)
    .click({ force: true });
  await createEditorDialog
    .getByLabel(/body text/i)
    .fill("Tilki gece bahcesindeki taslara yavasca yaklasir.");
  await createEditorDialog.getByLabel(/illustration asset id/i).fill("52");
  await createEditorDialog.getByLabel(/audio asset id/i).fill("84");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response
          .url()
          .includes("/api/admin/contents/1/story-pages/2/localizations/tr") &&
        response.request().method() === "PUT",
    ),
    createEditorDialog.locator("form").evaluate((form) => {
      (form as HTMLFormElement).requestSubmit();
    }),
  ]);

  await expect(
    createEditorDialog.getByLabel(/illustration asset id/i),
  ).toHaveValue("52");
  await page.getByRole("button", { name: /close editor/i }).click();

  await storyPageTable
    .getByRole("button", { name: /^delete page 2$/i })
    .click();
  await page.getByRole("button", { name: /delete page/i }).click();

  await expect(storyPageTable.getByText("Page 2", { exact: true })).toHaveCount(
    0,
  );
});
