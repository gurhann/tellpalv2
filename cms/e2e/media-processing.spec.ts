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

type ProcessingResponse = {
  processingId: number;
  contentId: number;
  languageCode: string;
  contentType: "STORY" | "AUDIO_STORY" | "MEDITATION" | "LULLABY";
  externalKey: string;
  coverSourceAssetId: number | null;
  audioSourceAssetId: number | null;
  pageCount: number | null;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  attemptCount: number;
  nextAttemptAt: string;
  leaseExpiresAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

function makeSession(overrides: Partial<SessionPayload> = {}): SessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-04-17T18:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-05-17T18:00:00Z",
    ...overrides,
  };
}

function makeContents(): ContentReadResponse[] {
  return [
    {
      contentId: 1,
      type: "STORY",
      externalKey: "story.evening-garden",
      active: true,
      ageRange: 5,
      pageCount: 2,
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
          processingStatus: "FAILED",
          publishedAt: "2026-04-17T08:00:00Z",
          visibleToMobile: false,
        },
        {
          contentId: 1,
          languageCode: "tr",
          title: "Aksam Bahcesi",
          description: "Sakin bir gece hikayesi.",
          bodyText: null,
          coverMediaId: null,
          audioMediaId: null,
          durationMinutes: 8,
          status: "DRAFT",
          processingStatus: "PENDING",
          publishedAt: null,
          visibleToMobile: false,
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
      localizations: [
        {
          contentId: 2,
          languageCode: "de",
          title: "Regenraum Pause",
          description: "Kurze Atemubung mit Regenatmosphare.",
          bodyText: "Atme vier Takte lang ein.",
          coverMediaId: 12,
          audioMediaId: 11,
          durationMinutes: 6,
          status: "DRAFT",
          processingStatus: "PROCESSING",
          publishedAt: null,
          visibleToMobile: false,
        },
      ],
    },
  ];
}

test("media processing supports lookup, retry, and schedule flows", async ({
  page,
}) => {
  const session = makeSession();
  const contents = makeContents();
  const recentJobs: ProcessingResponse[] = [
    {
      processingId: 77,
      contentId: 1,
      languageCode: "en",
      contentType: "STORY",
      externalKey: "story.evening-garden",
      coverSourceAssetId: 12,
      audioSourceAssetId: null,
      pageCount: 2,
      status: "FAILED",
      attemptCount: 3,
      nextAttemptAt: "2026-04-17T09:30:00Z",
      leaseExpiresAt: null,
      startedAt: "2026-04-17T09:18:00Z",
      completedAt: null,
      failedAt: "2026-04-17T09:20:00Z",
      lastErrorCode: "zip_generation_failed",
      lastErrorMessage: "The package writer could not seal the archive.",
      createdAt: "2026-04-17T09:00:00Z",
      updatedAt: "2026-04-17T09:20:00Z",
    },
    {
      processingId: 78,
      contentId: 2,
      languageCode: "de",
      contentType: "MEDITATION",
      externalKey: "meditation.rain-room",
      coverSourceAssetId: 12,
      audioSourceAssetId: 11,
      pageCount: null,
      status: "PROCESSING",
      attemptCount: 1,
      nextAttemptAt: "2026-04-17T10:00:00Z",
      leaseExpiresAt: "2026-04-17T09:40:00Z",
      startedAt: "2026-04-17T09:24:00Z",
      completedAt: null,
      failedAt: null,
      lastErrorCode: null,
      lastErrorMessage: null,
      createdAt: "2026-04-17T09:22:00Z",
      updatedAt: "2026-04-17T09:25:00Z",
    },
  ];
  const statusRegistry = new Map<string, ProcessingResponse>([
    ["1:en", recentJobs[0]],
    ["2:de", recentJobs[1]],
  ]);

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

  await page.route("**/api/admin/contents", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(contents),
    });
  });

  await page.route("**/api/admin/media-processing?**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(recentJobs),
    });
  });

  await page.route("**/api/admin/media-processing/*/localizations/*", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (request.method() !== "GET") {
      await route.fallback();
      return;
    }

    const parts = url.pathname.split("/");
    const contentId = Number(parts[4]);
    const languageCode = parts[6];
    const record = statusRegistry.get(`${contentId}:${languageCode}`);

    if (!record) {
      await route.fulfill({
        status: 404,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Processing not found",
          status: 404,
          detail: "No processing record exists yet.",
          errorCode: "asset_processing_not_found",
          path: url.pathname,
        }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(record),
    });
  });

  await page.route("**/api/admin/media-processing/*/localizations/*/retry", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

    const nextRetry: ProcessingResponse = {
      ...recentJobs[0],
      status: "PENDING",
      attemptCount: recentJobs[0].attemptCount + 1,
      failedAt: null,
      lastErrorCode: null,
      lastErrorMessage: null,
      updatedAt: "2026-04-17T09:32:00Z",
      nextAttemptAt: "2026-04-17T09:35:00Z",
    };

    recentJobs[0] = nextRetry;
    statusRegistry.set("1:en", nextRetry);

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(nextRetry),
    });
  });

  await page.route("**/api/admin/media-processing", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

    const nextScheduled: ProcessingResponse = {
      processingId: 91,
      contentId: 1,
      languageCode: "tr",
      contentType: "STORY",
      externalKey: "story.evening-garden",
      coverSourceAssetId: null,
      audioSourceAssetId: null,
      pageCount: 2,
      status: "PENDING",
      attemptCount: 0,
      nextAttemptAt: "2026-04-17T09:42:00Z",
      leaseExpiresAt: null,
      startedAt: null,
      completedAt: null,
      failedAt: null,
      lastErrorCode: null,
      lastErrorMessage: null,
      createdAt: "2026-04-17T09:40:00Z",
      updatedAt: "2026-04-17T09:40:00Z",
    };

    recentJobs.unshift(nextScheduled);
    statusRegistry.set("1:tr", nextScheduled);

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(nextScheduled),
    });
  });

  await page.goto("/media-processing");

  await expect(
    page.getByRole("heading", { name: /^processing console$/i }),
  ).toBeVisible();
  await expect(page.getByText("story.evening-garden")).toBeVisible();

  await page
    .getByRole("table", { name: /recent media processing jobs/i })
    .getByText("story.evening-garden")
    .click();

  await expect(page.getByText(/zip_generation_failed/i).first()).toBeVisible();
  await expect(page.getByRole("button", { name: /^retry$/i }).first()).toBeVisible();

  await page.getByRole("button", { name: /schedule processing/i }).first().click();
  const scheduleDialog = page.getByRole("dialog");
  await expect(
    scheduleDialog.getByRole("heading", { name: /^schedule processing$/i }),
  ).toBeVisible();
  await scheduleDialog.getByLabel(/content/i).click();
  await page.getByRole("option", { name: /evening garden/i }).click();
  await scheduleDialog.getByLabel(/target language/i).click();
  await page.getByRole("option", { name: /turkish/i }).click();
  await Promise.all([
    page.waitForResponse((response) =>
      response.url().endsWith("/api/admin/media-processing"),
    ),
    scheduleDialog
      .getByRole("button", { name: /^schedule processing$/i })
      .dispatchEvent("click"),
  ]);

  await expect(page.getByText(/^tr$/i).first()).toBeVisible();
  await expect(page.getByText(/not scheduled yet/i)).toHaveCount(0);
});
