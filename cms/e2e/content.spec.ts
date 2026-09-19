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
  type: "STORY" | "MEDITATION" | "LULLABY";
  externalKey: string;
  active: boolean;
  ageRange: number | null;
  pageCount: number | null;
  textlessCoverMediaId: number | null;
  listeningCoverMediaId: number | null;
  listingCoverMediaId: number | null;
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

test("create, edit, and publish flows work in the browser", async ({
  page,
}) => {
  const session = makeSession({
    accessTokenExpiresAt: "2026-05-29T10:00:00Z",
    refreshTokenExpiresAt: "2026-06-28T10:00:00Z",
  });
  const tinyAudioDataUrl =
    "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YQAAAAA=";
  const mediaAssets = [
    {
      assetId: 11,
      provider: "LOCAL_STUB",
      objectPath: "/content/audio/dream-harbor-tr.mp3",
      mediaType: "AUDIO",
      kind: "ORIGINAL_AUDIO",
      mimeType: "audio/mpeg",
      byteSize: null,
      checksumSha256: null,
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-03-31T12:00:00Z",
      updatedAt: "2026-03-31T12:00:00Z",
    },
  ];
  const baseList: ContentReadResponse[] = [
    {
      contentId: 1,
      type: "STORY",
      externalKey: "story.evening-garden",
      active: true,
      ageRange: 5,
      pageCount: 2,
      textlessCoverMediaId: null,
      listeningCoverMediaId: null,
      listingCoverMediaId: null,
      localizations: [
        {
          contentId: 1,
          languageCode: "tr",
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
      ],
    },
    {
      contentId: 2,
      type: "MEDITATION",
      externalKey: "meditation.rain-room",
      active: true,
      ageRange: 8,
      pageCount: null,
      textlessCoverMediaId: null,
      listeningCoverMediaId: null,
      listingCoverMediaId: null,
      localizations: [
        {
          contentId: 2,
          languageCode: "tr",
          title: "Regenraum Pause",
          description: "Kurze Atemubung mit Regenatmosphare.",
          bodyText: "Atme vier Takte lang ein und entspanne die Schultern.",
          coverMediaId: null,
          audioMediaId: 2,
          durationMinutes: 6,
          status: "DRAFT",
          processingStatus: "PROCESSING",
          publishedAt: null,
          visibleToMobile: false,
        },
      ],
    },
    {
      contentId: 3,
      type: "LULLABY",
      externalKey: "lullaby.moon-softly",
      active: true,
      ageRange: 3,
      pageCount: null,
      textlessCoverMediaId: null,
      listeningCoverMediaId: null,
      listingCoverMediaId: null,
      playback: {
        audioMediaId: 3,
        durationMinutes: 11,
        processingStatus: "COMPLETED",
        processingError: null,
        instruments: [],
      },
      localizations: [],
    },
  ];
  let createdDetail: ContentReadResponse | null = null;

  await page.route("**/api/admin/content-registry**", async (route) => {
    const url = new URL(route.request().url());
    const type = url.searchParams.get("type") ?? "STORY";
    const language = url.searchParams.get("language") ?? "tr";
    const readiness = url.searchParams.get("readiness");
    const page = Number(url.searchParams.get("page") ?? "0");
    const size = Number(url.searchParams.get("size") ?? "25");
    const query = (url.searchParams.get("q") ?? "").trim().toLowerCase();
    const records = createdDetail ? [createdDetail, ...baseList] : baseList;
    const filteredItems = records
      .filter((record) => record.type === type)
      .filter((record) => {
        if (!readiness) return true;
        return record.contentId === 1
          ? readiness === "PUBLISHED"
          : readiness === "ACTION_REQUIRED";
      })
      .filter((record) => {
        if (!query) return true;
        const localization = record.localizations.find(
          (candidate) => candidate.languageCode === language,
        );
        return [
          record.externalKey,
          localization?.title,
          `${record.contentId}`,
        ].some((value) => value?.toLowerCase().includes(query));
      })
      .map((record) => {
        const localization = record.localizations.find(
          (candidate) => candidate.languageCode === language,
        );

        return {
          contentId: record.contentId,
          type: record.type,
          externalKey: record.externalKey,
          pageCount: record.pageCount,
          durationMinutes:
            record.type === "LULLABY"
              ? record.playback?.durationMinutes ?? null
              : localization?.durationMinutes ?? null,
          selectedLanguage: language,
          title: localization?.title ?? null,
          readiness: record.contentId === 1 ? "PUBLISHED" : "ACTION_REQUIRED",
          blockers:
            record.contentId === 1
              ? []
              : [{ code: "LOCALIZATION_MISSING", pageNumber: null }],
          lastEditedAt: "2026-03-17T09:00:00Z",
        };
      });
    const items = filteredItems.slice(page * size, page * size + size);

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        items,
        page,
        size,
        totalItems: filteredItems.length,
      }),
    });
  });

  await page.route("**/api/admin/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(session),
    });
  });

  await page.route("**/api/admin/auth/refresh", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(session),
    });
  });

  await page.route("**/api/admin/contents", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          createdDetail ? [createdDetail, ...baseList] : baseList,
        ),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as {
        type: ContentReadResponse["type"];
        externalKey: string;
        ageRange: number | null;
        active: boolean;
      };

      createdDetail = {
        contentId: 99,
        type: body.type,
        externalKey: body.externalKey,
        active: body.active,
        ageRange: body.ageRange,
        pageCount: null,
        textlessCoverMediaId: null,
        listeningCoverMediaId: null,
        listingCoverMediaId: null,
        localizations: [],
      };

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          contentId: 99,
          type: body.type,
          externalKey: body.externalKey,
          ageRange: body.ageRange,
          active: body.active,
          pageCount: null,
          textlessCoverMediaId: null,
          listeningCoverMediaId: null,
          listingCoverMediaId: null,
        }),
      });
      return;
    }

    await route.fallback();
  });

  await page.route("**/api/admin/contents/99", async (route) => {
    const request = route.request();

    if (!createdDetail) {
      await route.fulfill({
        status: 404,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Content not found",
          status: 404,
          detail: "Content 99 was not found.",
          errorCode: "content_not_found",
          path: "/api/admin/contents/99",
        }),
      });
      return;
    }

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(createdDetail),
      });
      return;
    }

    if (request.method() === "PUT") {
      const body = request.postDataJSON() as {
        externalKey: string;
        ageRange: number | null;
        active: boolean;
        textlessCoverMediaId: number | null;
        listeningCoverMediaId: number | null;
      };

      createdDetail = {
        ...createdDetail,
        externalKey: body.externalKey,
        ageRange: body.ageRange,
        active: body.active,
        textlessCoverMediaId: body.textlessCoverMediaId,
        listeningCoverMediaId: body.listeningCoverMediaId,
      };

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          contentId: createdDetail.contentId,
          type: createdDetail.type,
          externalKey: createdDetail.externalKey,
          ageRange: createdDetail.ageRange,
          active: createdDetail.active,
          pageCount: createdDetail.pageCount,
          textlessCoverMediaId: createdDetail.textlessCoverMediaId,
          listeningCoverMediaId: createdDetail.listeningCoverMediaId,
          listingCoverMediaId: createdDetail.listingCoverMediaId,
        }),
      });
      return;
    }

    await route.fallback();
  });

  await page.route(
    "**/api/admin/contents/99/localizations/tr",
    async (route) => {
      const request = route.request();

      if (!createdDetail || request.method() !== "POST") {
        await route.fallback();
        return;
      }

      const body = request.postDataJSON() as {
        title: string;
        description?: string | null;
        bodyText?: string | null;
        coverMediaId?: number | null;
        audioMediaId?: number | null;
        durationMinutes?: number | null;
        status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
        processingStatus: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
        publishedAt?: string | null;
      };

      const localization = {
        contentId: 99,
        languageCode: "tr",
        title: body.title,
        description: body.description ?? null,
        bodyText: body.bodyText ?? null,
        coverMediaId: body.coverMediaId ?? null,
        audioMediaId: body.audioMediaId ?? null,
        durationMinutes: body.durationMinutes ?? null,
        status: body.status,
        processingStatus: body.processingStatus ?? "PENDING",
        publishedAt: body.publishedAt ?? null,
        visibleToMobile: false,
      };

      createdDetail = {
        ...createdDetail,
        localizations: [localization],
      };

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(localization),
      });
    },
  );

  await page.route(
    "**/api/admin/contents/99/localizations/tr/publish",
    async (route) => {
      if (!createdDetail) {
        await route.fallback();
        return;
      }

      const localization = {
        ...createdDetail.localizations[0],
        status: "PUBLISHED" as const,
        publishedAt: "2026-03-29T09:30:00Z",
        visibleToMobile: false,
      };

      createdDetail = {
        ...createdDetail,
        localizations: [localization],
      };

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
      body: JSON.stringify(mediaAssets),
    });
  });

  await page.route("**/api/admin/contributors", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  await page.route("**/api/admin/contents/99/contributors", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  await page.route("**/api/admin/contents/99/instruments**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  await page.route("**/api/admin/instrument-catalog**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  await page.route("**/api/admin/media/*", async (route) => {
    const assetId = Number(route.request().url().split("/").pop());
    const asset = mediaAssets.find((entry) => entry.assetId === assetId);

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
      const asset = mediaAssets.find((entry) => entry.assetId === assetId);

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

      asset.cachedDownloadUrl = tinyAudioDataUrl;
      asset.downloadUrlCachedAt = "2026-03-31T12:15:00Z";
      asset.downloadUrlExpiresAt = "2026-03-31T14:15:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(asset),
      });
    },
  );

  await page.goto("/login");
  await page.getByLabel(/username/i).fill("admin");
  await page.getByLabel(/password/i).fill("test1234");
  await page.getByRole("button", { name: /^sign in$/i }).click();
  await expect(
    page.getByRole("heading", { name: /^contents$/i, level: 1 }),
  ).toBeVisible();
  await expect(page.getByRole("tab", { name: /^stories/i })).toBeVisible();
  await expect(page.getByRole("tab", { name: /^meditations/i })).toBeVisible();

  await page.getByRole("tab", { name: /^meditations/i }).click();
  await expect(page.getByText("Regenraum Pause")).toBeVisible();
  await expect(page.getByText("Evening Garden")).toHaveCount(0);
  await expect(page.getByText(/Meditations · 1 records · TR/i)).toBeVisible();

  await page.getByRole("tab", { name: /^lullabies/i }).click();
  await expect(page.getByText("11 min")).toBeVisible();

  await page.getByRole("button", { name: /^create content$/i }).click();
  const contentDialog = page.getByRole("dialog");
  await contentDialog.getByLabel(/content type/i).click();
  await page.getByRole("option", { name: /^lullaby$/i }).click();
  await contentDialog
    .getByLabel(/external key/i)
    .fill("lullaby.smoke-harbor");
  await contentDialog.getByLabel(/age range/i).fill("3");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contents/99") &&
        response.request().method() === "GET",
    ),
    contentDialog
      .getByRole("button", { name: /^create content$/i })
      .click(),
  ]);

  await expect(
    page.getByRole("heading", { name: /locale workspace/i }),
  ).toBeVisible();
  await expect(page.getByTestId("lullaby-playback-editor")).toBeVisible();

  const metadataRegion = page.getByRole("region", { name: /^metadata$/i });
  await metadataRegion
    .getByLabel(/external key/i)
    .fill("lullaby.smoke-harbor.v2");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contents/99") &&
        response.request().method() === "PUT",
    ),
    metadataRegion
      .getByRole("button", { name: /save metadata/i })
      .evaluate((button) => {
        (button as HTMLButtonElement).click();
      }),
  ]);
  await expect(metadataRegion.getByLabel(/external key/i)).toHaveValue(
    "lullaby.smoke-harbor.v2",
  );
  await expect(
    page.getByText(/content changes could not be saved/i),
  ).toHaveCount(0);

  await page
    .getByRole("region", { name: /locale workspace/i })
    .getByRole("button", { name: /create first localization/i })
    .click();
  const localizationDialog = page.getByRole("dialog");
  await localizationDialog.getByLabel(/^title$/i).fill("Dream Harbor");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contents/99/localizations/tr") &&
        response.request().method() === "POST",
    ),
    localizationDialog.locator("form").evaluate((form) => {
      (form as HTMLFormElement).requestSubmit();
    }),
  ]);

  await expect(
    page
      .getByRole("region", { name: /locale workspace/i })
      .getByLabel(/^title$/i),
  ).toHaveValue("Dream Harbor");
  await expect(page.getByRole("tab", { name: /turkish/i })).toBeVisible();

  await page.getByRole("button", { name: /publish locale/i }).click();

  await expect(
    page.getByRole("button", { name: /publish locale/i }),
  ).toBeDisabled();
  await expect(page.getByRole("tab", { name: /turkish/i })).toContainText(
    "Published",
  );
});
