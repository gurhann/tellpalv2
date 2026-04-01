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

type CategoryReadResponse = {
  categoryId: number;
  type: "STORY" | "AUDIO_STORY" | "MEDITATION" | "LULLABY";
  slug: string;
  premium: boolean;
  active: boolean;
};

function makeSession(overrides: Partial<SessionPayload> = {}): SessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-04-01T12:00:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-05-01T12:00:00Z",
    ...overrides,
  };
}

test("category create and edit use content-aligned types", async ({ page }) => {
  const session = makeSession();
  const categories: CategoryReadResponse[] = [
    {
      categoryId: 7,
      type: "STORY",
      slug: "featured-sleep",
      premium: false,
      active: true,
    },
    {
      categoryId: 8,
      type: "MEDITATION",
      slug: "bedtime-meditations",
      premium: true,
      active: true,
    },
  ];
  let createdCategory: CategoryReadResponse | null = null;

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
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  await page.route("**/api/admin/categories", async (route) => {
    const request = route.request();

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          createdCategory ? [createdCategory, ...categories] : categories,
        ),
      });
      return;
    }

    if (request.method() === "POST") {
      const body = request.postDataJSON() as {
        slug: string;
        type: CategoryReadResponse["type"];
        premium: boolean;
        active: boolean;
      };

      createdCategory = {
        categoryId: 99,
        type: body.type,
        slug: body.slug,
        premium: body.premium,
        active: body.active,
      };

      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify(createdCategory),
      });
      return;
    }

    await route.fallback();
  });

  await page.route("**/api/admin/categories/99", async (route) => {
    const request = route.request();

    if (!createdCategory) {
      await route.abort();
      return;
    }

    if (request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(createdCategory),
      });
      return;
    }

    if (request.method() === "PUT") {
      const body = request.postDataJSON() as {
        slug: string;
        type: CategoryReadResponse["type"];
        premium: boolean;
        active: boolean;
      };

      createdCategory = {
        ...createdCategory,
        type: body.type,
        slug: body.slug,
        premium: body.premium,
        active: body.active,
      };

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(createdCategory),
      });
      return;
    }

    await route.fallback();
  });

  await page.goto("/login");

  await page.getByLabel(/username/i).fill("admin");
  await page.getByLabel(/password/i).fill("test1234");
  await page.getByRole("button", { name: /^sign in$/i }).click();

  await expect(
    page.getByRole("heading", { name: /content studio/i }),
  ).toBeVisible();

  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/categories") &&
        response.request().method() === "GET",
    ),
    page.getByRole("link", { name: /^categories/i }).click(),
  ]);

  await expect(
    page.getByRole("heading", { name: /^categories$/i, level: 1 }),
  ).toBeVisible();

  await page.getByRole("button", { name: /^create category$/i }).click();
  await page.getByRole("combobox", { name: /category type/i }).click();

  await expect(page.getByRole("option", { name: /^Story$/ })).toBeVisible();
  await expect(
    page.getByRole("option", { name: /^Audio Story$/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("option", { name: /^Meditation$/ }),
  ).toBeVisible();
  await expect(page.getByRole("option", { name: /^Lullaby$/ })).toBeVisible();
  await expect(
    page.getByRole("option", { name: /parent guidance/i }),
  ).toHaveCount(0);

  await page.getByRole("option", { name: /^Lullaby$/ }).click();
  await page.getByLabel(/slug/i).fill("calm-lullabies");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /^create category$/i })
    .click();

  await expect(
    page.getByRole("heading", { name: /calm-lullabies/i }),
  ).toBeVisible();
  await expect(page.getByText(/lullaby \/ standard \/ active/i)).toBeVisible();
  await expect(
    page.getByText(/only lullaby records will be accepted here/i),
  ).toBeVisible();

  await page.getByLabel(/slug/i).fill("calm-lullabies-v2");
  await page.getByRole("button", { name: /save metadata/i }).click();

  await expect(page.getByLabel(/slug/i)).toHaveValue("calm-lullabies-v2");
});
