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
  const session = makeSession();
  const baseList: ContentReadResponse[] = [
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
      localizations: [
        {
          contentId: 2,
          languageCode: "de",
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
  ];
  let createdDetail: ContentReadResponse | null = null;

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
      };

      createdDetail = {
        ...createdDetail,
        externalKey: body.externalKey,
        ageRange: body.ageRange,
        active: body.active,
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
        processingStatus: body.processingStatus,
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

  await page.goto("/contents");

  await expect(
    page.getByRole("heading", { name: /content studio/i }),
  ).toBeVisible();

  await page.getByRole("button", { name: /^create content$/i }).click();
  await page.getByLabel(/content type/i).click();
  await page.getByRole("option", { name: /^lullaby$/i }).click();
  await page.getByLabel(/external key/i).fill("lullaby.smoke-harbor");
  await page.getByLabel(/age range/i).fill("3");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^create content$/i })
    .click();

  await expect(
    page.getByRole("heading", { name: /content #99/i }),
  ).toBeVisible();

  await page.getByLabel(/external key/i).fill("lullaby.smoke-harbor.v2");
  await page.getByRole("button", { name: /save metadata/i }).click();
  await expect(page.getByLabel(/external key/i)).toHaveValue(
    "lullaby.smoke-harbor.v2",
  );

  await page
    .getByRole("button", { name: /create first localization/i })
    .click();
  await page.getByLabel(/^title$/i).fill("Dream Harbor");
  await page.getByLabel(/audio asset id/i).fill("11");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /create localization/i })
    .click();

  await expect(
    page.getByRole("heading", { name: /dream harbor/i }),
  ).toBeVisible();
  await expect(page.getByRole("tab", { name: /turkish/i })).toBeVisible();

  await page.getByRole("button", { name: /publish locale/i }).click();

  await expect(
    page.getByRole("button", { name: /publish locale/i }),
  ).toBeDisabled();
  await expect(page.getByRole("tab", { name: /turkish/i })).toContainText(
    "Published",
  );
});
