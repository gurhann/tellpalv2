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

test("category create, edit, and localize use content-aligned types", async ({
  page,
}) => {
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
  let createdLocalization: {
    categoryId: number;
    languageCode: string;
    name: string;
    description: string | null;
    imageMediaId: number | null;
    status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
    publishedAt: string | null;
    published: boolean;
  } | null = null;
  const imageAssets = [
    {
      assetId: 4,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/calm-lullabies-cover.jpg",
      mediaType: "IMAGE",
      kind: "ORIGINAL_IMAGE",
      mimeType: "image/jpeg",
      byteSize: null,
      checksumSha256: null,
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-04-01T12:00:00Z",
      updatedAt: "2026-04-01T12:00:00Z",
    },
    {
      assetId: 5,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/calm-lullabies-detail.jpg",
      mediaType: "IMAGE",
      kind: "ORIGINAL_IMAGE",
      mimeType: "image/jpeg",
      byteSize: null,
      checksumSha256: null,
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-04-01T12:00:00Z",
      updatedAt: "2026-04-01T12:00:00Z",
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

  await page.route(
    "**/api/admin/categories/99/localizations/tr",
    async (route) => {
      const request = route.request();

      if (request.method() === "POST" || request.method() === "PUT") {
        const body = request.postDataJSON() as {
          name: string;
          description?: string | null;
          imageMediaId?: number | null;
          status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
          publishedAt?: string | null;
        };

        createdLocalization = {
          categoryId: 99,
          languageCode: "tr",
          name: body.name,
          description: body.description ?? null,
          imageMediaId: body.imageMediaId ?? null,
          status: body.status,
          publishedAt: body.publishedAt ?? null,
          published: body.status === "PUBLISHED",
        };

        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(createdLocalization),
        });
        return;
      }

      await route.fallback();
    },
  );

  await page.route("**/api/admin/media**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(imageAssets),
    });
  });

  await page.route("**/api/admin/media/*", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    const assetId = Number(route.request().url().split("/").pop());
    const asset = imageAssets.find((entry) => entry.assetId === assetId);

    if (!asset) {
      await route.fulfill({
        status: 404,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Asset not found",
          status: 404,
          detail: `Asset #${assetId} was not found.`,
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

  await page.goto("/categories");

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
    page.getByText(/category type determines which content family/i),
  ).toBeVisible();

  await page.getByLabel(/slug/i).fill("calm-lullabies-v2");
  await page.getByRole("button", { name: /save metadata/i }).click();

  await expect(page.getByLabel(/slug/i)).toHaveValue("calm-lullabies-v2");

  await page
    .getByRole("button", { name: /create first localization/i })
    .first()
    .click();

  await expect(
    page.getByRole("heading", { name: /create category localization/i }),
  ).toBeVisible();
  await page.locator('input[name="name"]').fill("Calm Lullabies");
  await page
    .locator('textarea[name="description"]')
    .fill("Soft editorial picks for bedtime.");
  await page.locator('input[name="imageMediaId"]').fill("4");
  await page.getByRole("button", { name: /create localization/i }).click();

  await expect(
    page
      .getByRole("tablist", { name: /category localization tabs/i })
      .getByRole("tab", { name: /turkish/i }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("tablist", { name: /category curation language tabs/i })
      .getByRole("tab", { name: /turkish/i }),
  ).toBeVisible();
  await expect(page.locator('input[name="name"]')).toHaveValue(
    "Calm Lullabies",
  );

  await page.getByLabel(/^name$/i).fill("Calm Lullabies Updated");
  await page.getByRole("button", { name: /save localization/i }).click();

  await expect(page.locator('input[name="name"]')).toHaveValue(
    "Calm Lullabies Updated",
  );
  await expect(createdLocalization?.name).toBe("Calm Lullabies Updated");
});
