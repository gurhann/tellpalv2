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

type ContributorRecord = {
  contributorId: number;
  displayName: string;
};

type ContentContributorRecord = {
  contentId: number;
  contributorId: number;
  contributorDisplayName: string;
  role: "AUTHOR" | "ILLUSTRATOR" | "NARRATOR" | "MUSICIAN";
  languageCode: string | null;
  creditName: string | null;
  sortOrder: number;
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

test("contributor registry and assignment flows support delete and unassign", async ({
  page,
}) => {
  const session = makeSession();
  const contributors: ContributorRecord[] = [
    {
      contributorId: 11,
      displayName: "Annie Case",
    },
    {
      contributorId: 12,
      displayName: "Milo Rivers",
    },
  ];
  const assignments: ContentContributorRecord[] = [
    {
      contentId: 1,
      contributorId: 11,
      contributorDisplayName: "Annie Case",
      role: "AUTHOR",
      languageCode: "en",
      creditName: null,
      sortOrder: 0,
    },
    {
      contentId: 1,
      contributorId: 12,
      contributorDisplayName: "Milo Rivers",
      role: "NARRATOR",
      languageCode: "tr",
      creditName: "M. Rivers",
      sortOrder: 1,
    },
  ];
  const content: ContentReadResponse = {
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

    if (request.method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([content]),
    });
  });

  await page.route("**/api/admin/contributors*", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contributors),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as { displayName: string };
      const createdContributor = {
        contributorId: 99,
        displayName: body.displayName,
      };
      contributors.unshift(createdContributor);

      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify(createdContributor),
      });
      return;
    }

    await route.fallback();
  });

  await page.route("**/api/admin/contributors/*", async (route) => {
    const request = route.request();
    const contributorId = Number(
      new URL(request.url()).pathname.split("/").at(-1),
    );

    if (request.method() === "PUT") {
      const contributor = contributors.find(
        (candidate) => candidate.contributorId === contributorId,
      );

      if (!contributor) {
        await route.abort();
        return;
      }

      const body = request.postDataJSON() as { displayName: string };
      contributor.displayName = body.displayName;

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contributor),
      });
      return;
    }

    if (request.method() === "DELETE") {
      const contributorIndex = contributors.findIndex(
        (candidate) => candidate.contributorId === contributorId,
      );

      contributors.splice(contributorIndex, 1);

      await route.fulfill({
        status: 204,
      });
      return;
    }

    await route.fallback();
  });

  await page.route("**/api/admin/contents/1", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(content),
    });
  });

  await page.route("**/api/admin/contents/1/contributors", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(assignments),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as {
        contributorId: number;
        role: ContentContributorRecord["role"];
        languageCode?: string | null;
        creditName?: string | null;
        sortOrder: number;
      };
      const contributor = contributors.find(
        (candidate) => candidate.contributorId === body.contributorId,
      );

      if (!contributor) {
        await route.abort();
        return;
      }

      const assignment: ContentContributorRecord = {
        contentId: 1,
        contributorId: contributor.contributorId,
        contributorDisplayName: contributor.displayName,
        role: body.role,
        languageCode: body.languageCode ?? null,
        creditName: body.creditName ?? null,
        sortOrder: body.sortOrder,
      };
      assignments.push(assignment);

      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify(assignment),
      });
      return;
    }

    if (request.method() === "DELETE") {
      const requestUrl = new URL(request.url());
      const contributorId = Number(
        requestUrl.searchParams.get("contributorId") ?? "0",
      );
      const role = requestUrl.searchParams.get("role");
      const languageCode = requestUrl.searchParams.get("languageCode");
      const assignmentIndex = assignments.findIndex(
        (candidate) =>
          candidate.contributorId === contributorId &&
          candidate.role === role &&
          candidate.languageCode === (languageCode ?? null),
      );

      assignments.splice(assignmentIndex, 1);

      await route.fulfill({
        status: 204,
      });
      return;
    }

    await route.fallback();
  });

  await page.goto("/login");

  await page.getByLabel(/username/i).fill("admin");
  await page.getByLabel(/password/i).fill("test1234");

  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contents") &&
        response.request().method() === "GET",
    ),
    page.getByRole("button", { name: /^sign in$/i }).click(),
  ]);

  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contributors") &&
        response.request().method() === "GET",
    ),
    page.getByRole("link", { name: /^contributors/i }).click(),
  ]);

  await expect(
    page.getByRole("heading", { name: /^contributors$/i, level: 1 }),
  ).toBeVisible();

  const createContributorButton = page.getByRole("button", {
    name: /^create contributor$/i,
  });
  await expect(createContributorButton).toBeVisible();
  await page.waitForTimeout(250);
  await createContributorButton.click({ force: true });
  await page.getByLabel(/display name/i).fill("Lina Hart");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^create contributor$/i })
    .click();

  await expect(page.getByText("Lina Hart").first()).toBeVisible();

  const annieRow = page.locator("tr").filter({ hasText: "Annie Case" });
  await annieRow.getByRole("button", { name: /rename/i }).click();
  await page.getByLabel(/display name/i).fill("Annie Case Updated");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^save rename$/i })
    .click();

  await expect(page.getByText("Annie Case Updated")).toBeVisible();

  const miloRow = page.locator("tr").filter({ hasText: "Milo Rivers" });
  await miloRow
    .getByRole("button", { name: /^delete milo rivers$/i })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^delete contributor$/i })
    .click();

  await expect(page.getByText("Milo Rivers")).toHaveCount(0);

  await page.getByRole("link", { name: /^contents/i }).click();
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/contents/1") &&
        response.request().method() === "GET",
    ),
    page.getByRole("row", { name: /evening garden/i }).click(),
  ]);

  await expect(page.getByText("M. Rivers")).toBeVisible();

  await page
    .locator("main")
    .getByRole("button", { name: /^assign contributor$/i })
    .click();
  await page.getByRole("combobox", { name: /^contributor$/i }).click();
  await page.getByRole("option", { name: "Lina Hart" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^assign contributor$/i })
    .click();

  await expect(page.getByText("Lina Hart").first()).toBeVisible();

  const unassignButtons = page
    .locator("main")
    .getByRole("button", { name: /^unassign m\. rivers$/i });
  await unassignButtons.last().click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^unassign contributor$/i })
    .click();

  await expect(page.getByText("M. Rivers")).toHaveCount(0);
});
