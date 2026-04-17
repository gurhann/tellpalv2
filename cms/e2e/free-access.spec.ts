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

test("free access supports grant, filtered listing, and revoke flows", async ({
  page,
}) => {
  const session = makeSession();
  const contents = [
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
          publishedAt: "2026-04-17T08:00:00Z",
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
  const grants = [
    {
      freeAccessId: 501,
      accessKey: "default",
      contentId: 1,
      languageCode: "en",
    },
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

  await page.route("**/api/admin/free-access**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (request.method() === "GET") {
      const accessKey = url.searchParams.get("accessKey");
      const filtered = accessKey
        ? grants.filter((grant) => grant.accessKey === accessKey)
        : grants.filter((grant) => grant.accessKey === "default");

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(filtered),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as {
        accessKey: string;
        contentId: number;
        languageCode: string;
      };
      const nextGrant = {
        freeAccessId: 777,
        accessKey: body.accessKey,
        contentId: body.contentId,
        languageCode: body.languageCode,
      };

      grants.push(nextGrant);

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(nextGrant),
      });
      return;
    }

    await route.fallback();
  });

  await page.route(
    "**/api/admin/free-access/*/languages/*/contents/*",
    async (route) => {
      if (route.request().method() !== "DELETE") {
        await route.fallback();
        return;
      }

      const url = new URL(route.request().url());
      const parts = url.pathname.split("/");
      const accessKey = parts[4];
      const languageCode = parts[6];
      const contentId = Number(parts[8]);
      const index = grants.findIndex(
        (grant) =>
          grant.accessKey === accessKey &&
          grant.languageCode === languageCode &&
          grant.contentId === contentId,
      );

      if (index >= 0) {
        grants.splice(index, 1);
      }

      await route.fulfill({
        status: 204,
        body: "",
      });
    },
  );

  await page.goto("/free-access");

  await expect(
    page.getByRole("heading", { name: /access key grants/i }),
  ).toBeVisible();
  await expect(page.getByText("default").first()).toBeVisible();

  await page.getByRole("button", { name: /grant free access/i }).click();
  const grantDialog = page.getByRole("dialog");
  await expect(
    grantDialog.getByRole("heading", { name: /^grant free access$/i }),
  ).toBeVisible();
  await grantDialog.getByLabel(/content/i).click();
  await page.getByRole("option", { name: /regenraum pause/i }).click();
  await grantDialog.getByLabel(/^language$/i).click();
  await page.getByRole("option", { name: /german/i }).click();
  await grantDialog.getByLabel(/access key/i).fill("partner-spring");
  await grantDialog
    .getByRole("button", { name: /^grant free access$/i })
    .click();

  await page.getByPlaceholder(/enter default or any custom access key/i).fill(
    "partner-spring",
  );
  await page.getByRole("button", { name: /apply filter/i }).click();
  await expect(page.getByText("partner-spring").first()).toBeVisible();
  await expect(page.getByText("Regenraum Pause")).toBeVisible();

  await page.getByRole("button", { name: /^revoke$/i }).click();
  const revokeDialog = page.getByRole("dialog");
  await expect(
    revokeDialog.getByRole("heading", { name: /revoke free access grant/i }),
  ).toBeVisible();
  await revokeDialog.getByRole("button", { name: /revoke grant/i }).click();

  await expect(
    page.getByRole("heading", { name: /no grants for this key/i }),
  ).toBeVisible();
});
