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

type CategoryLocalizationResponse = {
  categoryId: number;
  languageCode: string;
  name: string;
  description: string | null;
  imageMediaId: number | null;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  publishedAt: string | null;
  published: boolean;
};

type CategoryCurationResponse = {
  categoryId: number;
  languageCode: string;
  contentId: number;
  displayOrder: number;
};

type ContentReadResponse = {
  contentId: number;
  type: "STORY" | "AUDIO_STORY" | "MEDITATION" | "LULLABY";
  externalKey: string;
  active: boolean;
  ageRange: number;
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
  const tinyImageDataUrl =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8B9pQAAAAASUVORK5CYII=";
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

  await page.route("**/api/admin/auth/login", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

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

  await page.route(
    "**/api/admin/categories/99/localizations",
    async (route) => {
      if (route.request().method() !== "GET") {
        await route.fallback();
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(createdLocalization ? [createdLocalization] : []),
      });
    },
  );

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

  await page.route(
    "**/api/admin/categories/99/localizations/tr/contents",
    async (route) => {
      if (route.request().method() !== "GET") {
        await route.fallback();
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      });
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

  await page.route(
    "**/api/admin/media/*/download-url-cache/refresh",
    async (route) => {
      const segments = new URL(route.request().url()).pathname.split("/");
      const assetId = Number(segments.at(-2));
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
            path: `/api/admin/media/${assetId}/download-url-cache/refresh`,
          }),
        });
        return;
      }

      asset.cachedDownloadUrl = tinyImageDataUrl;
      asset.downloadUrlCachedAt = "2026-04-01T12:10:00Z";
      asset.downloadUrlExpiresAt = "2026-04-01T14:10:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(asset),
      });
    },
  );

  await page.goto("/categories");

  await expect(
    page.getByRole("heading", { name: /^categories$/i, level: 1 }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /^story$/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /^premium$/i }),
  ).toBeVisible();
  await page.getByRole("button", { name: /^meditation$/i }).click();
  await page.getByRole("button", { name: /^premium$/i }).click();
  await expect(page.getByText("bedtime-meditations")).toBeVisible();
  await expect(page.getByText("featured-sleep")).toHaveCount(0);
  await expect(
    page.getByText(/Meditation \| Premium \| All states \| 1 \/ 2 records/i),
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
  await expect(
    page.getByText(/review metadata, locale workspaces, and curation/i),
  ).toBeVisible();
  await expect(
    page.getByText(/slug, premium, active state/i),
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
  const categoryLocalizationDialog = page.getByRole("dialog");
  await categoryLocalizationDialog
    .getByRole("button", { name: /advanced/i })
    .click({ force: true });
  await categoryLocalizationDialog.getByLabel(/image asset/i).fill("4");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/categories/99/localizations/tr") &&
        response.request().method() === "POST",
    ),
    categoryLocalizationDialog.locator("form").evaluate((form) => {
      (form as HTMLFormElement).requestSubmit();
    }),
  ]);

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
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/admin/categories/99/localizations/tr") &&
        response.request().method() === "PUT",
    ),
    page.getByRole("button", { name: /save localization/i }).click(),
  ]);

  await expect(page.locator('input[name="name"]')).toHaveValue(
    "Calm Lullabies Updated",
  );

  await page.reload();

  await expect(
    page
      .getByRole("tablist", { name: /category localization tabs/i })
      .getByRole("tab", { name: /turkish/i }),
  ).toBeVisible();
  await expect(page.locator('input[name="name"]')).toHaveValue(
    "Calm Lullabies Updated",
  );
});

test("category curation add reorder remove survives refresh with hydrated localizations", async ({
  page,
}) => {
  const session = makeSession();
  const tinyImageDataUrl =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8B9pQAAAAASUVORK5CYII=";
  const category: CategoryReadResponse = {
    categoryId: 7,
    type: "STORY",
    slug: "featured-sleep",
    premium: false,
    active: true,
  };
  const localizations: CategoryLocalizationResponse[] = [
    {
      categoryId: 7,
      languageCode: "en",
      name: "Featured Sleep",
      description: "Curated sleep stories for bedtime.",
      imageMediaId: 4,
      status: "PUBLISHED",
      publishedAt: "2026-03-29T11:00:00Z",
      published: true,
    },
    {
      categoryId: 7,
      languageCode: "tr",
      name: "One Cikan Uyku",
      description: "Uyku zamani icin secilmis hikayeler.",
      imageMediaId: null,
      status: "DRAFT",
      publishedAt: null,
      published: false,
    },
  ];
  let curationRows: CategoryCurationResponse[] = [
    {
      categoryId: 7,
      languageCode: "en",
      contentId: 1,
      displayOrder: 0,
    },
  ];
  const contentRegistry: ContentReadResponse[] = [
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
      contentId: 11,
      type: "STORY",
      externalKey: "story.starry-forest",
      active: true,
      ageRange: 6,
      pageCount: 3,
      localizations: [
        {
          contentId: 11,
          languageCode: "en",
          title: "Starry Forest",
          description: "A calm walk beneath the stars.",
          bodyText: null,
          coverMediaId: null,
          audioMediaId: null,
          durationMinutes: 7,
          status: "PUBLISHED",
          processingStatus: "COMPLETED",
          publishedAt: "2026-03-19T09:00:00Z",
          visibleToMobile: true,
        },
      ],
    },
  ];
  const imageAssets = [
    {
      assetId: 4,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/featured-sleep-cover.jpg",
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
      objectPath: "/content/images/featured-sleep-banner.jpg",
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

  await page.route("**/api/admin/auth/login", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

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

  await page.route("**/api/admin/categories", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([category]),
    });
  });

  await page.route("**/api/admin/categories/7", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(category),
    });
  });

  await page.route("**/api/admin/categories/7/localizations", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(localizations),
    });
  });

  await page.route(
    "**/api/admin/categories/7/localizations/en/contents",
    async (route) => {
      const request = route.request();

      if (request.method() === "GET") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(curationRows),
        });
        return;
      }

      if (request.method() === "POST") {
        const body = request.postDataJSON() as {
          contentId: number;
          displayOrder: number;
        };
        const nextRow: CategoryCurationResponse = {
          categoryId: 7,
          languageCode: "en",
          contentId: body.contentId,
          displayOrder: body.displayOrder,
        };

        curationRows = [...curationRows, nextRow].sort(
          (left, right) => left.displayOrder - right.displayOrder,
        );

        await route.fulfill({
          status: 201,
          contentType: "application/json",
          body: JSON.stringify(nextRow),
        });
        return;
      }

      await route.fallback();
    },
  );

  await page.route(
    "**/api/admin/categories/7/localizations/en/contents/11",
    async (route) => {
      if (route.request().method() !== "PUT") {
        await route.fallback();
        return;
      }

      const body = route.request().postDataJSON() as { displayOrder: number };
      const nextRow: CategoryCurationResponse = {
        categoryId: 7,
        languageCode: "en",
        contentId: 11,
        displayOrder: body.displayOrder,
      };

      curationRows = curationRows
        .map((row) => (row.contentId === 11 ? nextRow : row))
        .sort((left, right) => left.displayOrder - right.displayOrder);

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(nextRow),
      });
    },
  );

  await page.route(
    "**/api/admin/categories/7/localizations/en/contents/1",
    async (route) => {
      if (route.request().method() !== "DELETE") {
        await route.fallback();
        return;
      }

      curationRows = curationRows.filter((row) => row.contentId !== 1);
      await route.fulfill({
        status: 204,
        body: "",
      });
    },
  );

  await page.route(
    "**/api/admin/categories/7/localizations/en/eligible-contents**",
    async (route) => {
      if (route.request().method() !== "GET") {
        await route.fallback();
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            contentId: 11,
            externalKey: "story.starry-forest",
            localizedTitle: "Starry Forest",
            languageCode: "en",
            publishedAt: "2026-03-30T09:00:00Z",
          },
        ]),
      });
    },
  );

  await page.route("**/api/admin/contents", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(contentRegistry),
    });
  });

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

  await page.route(
    "**/api/admin/media/*/download-url-cache/refresh",
    async (route) => {
      const segments = new URL(route.request().url()).pathname.split("/");
      const assetId = Number(segments.at(-2));
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
            path: `/api/admin/media/${assetId}/download-url-cache/refresh`,
          }),
        });
        return;
      }

      asset.cachedDownloadUrl = tinyImageDataUrl;
      asset.downloadUrlCachedAt = "2026-04-01T12:10:00Z";
      asset.downloadUrlExpiresAt = "2026-04-01T14:10:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(asset),
      });
    },
  );

  await page.goto("/categories/7");
  await expect(
    page.getByRole("heading", { name: /sign in to tellpal cms/i }),
  ).toBeVisible();
  await page.getByLabel(/username/i).fill("admin");
  await page.getByLabel(/password/i).fill("test1234");
  await Promise.all([
    page.waitForResponse("**/api/admin/auth/login"),
    page.getByRole("button", { name: /^sign in$/i }).click(),
  ]);
  await expect(
    page.getByRole("heading", { name: /^contents$/i, level: 2 }),
  ).toBeVisible();
  await page.evaluate(() => {
    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");
  });
  await page
    .getByRole("link", { name: /categories category metadata and curation/i })
    .click();
  await expect(
    page.getByRole("heading", { name: /^categories$/i, level: 1 }),
  ).toBeVisible();
  await page.getByText("featured-sleep").click();

  const curationTable = page.getByRole("table", {
    name: /english curated content table/i,
  });

  await expect(
    page
      .getByRole("tablist", { name: /category localization tabs/i })
      .getByRole("tab", { name: /english/i }),
  ).toBeVisible();
  await expect(curationTable.getByText(/^Content #1$/)).toBeVisible();
  await expect(
    page.getByRole("button", { name: /add curated content/i }),
  ).toBeEnabled();
  await expect(curationTable.getByText(/^Content #1$/)).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /loading curated content/i }),
  ).toHaveCount(0);

  const addCuratedContentButton = page
    .getByRole("button", { name: /add curated content/i })
    .first();
  await expect(addCuratedContentButton).toBeEnabled();
  await addCuratedContentButton.click({ force: true });
  await expect(
    page.getByRole("heading", { name: /add curated content/i }),
  ).toBeVisible();
  await page.getByRole("button", { name: /starry forest/i }).click();
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response
          .url()
          .includes("/api/admin/categories/7/localizations/en/contents") &&
        response.request().method() === "POST",
    ),
    page
      .getByRole("dialog")
      .locator("form")
      .evaluate((form) => {
        (form as HTMLFormElement).requestSubmit();
      }),
  ]);

  await expect(curationTable.getByText(/^Content #11$/)).toBeVisible();

  await page.locator("#curation-order-11").fill("2");
  await page
    .getByRole("button", { name: /save order/i })
    .nth(1)
    .click();
  await expect(page.locator("#curation-order-11")).toHaveValue("2");

  await page.reload();

  await expect(
    page
      .getByRole("tablist", { name: /category localization tabs/i })
      .getByRole("tab", { name: /english/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /loading curated content/i }),
  ).toHaveCount(0);
  await expect(curationTable.getByText(/^Content #1$/)).toBeVisible();
  await expect(curationTable.getByText(/^Content #11$/)).toBeVisible();
  await expect(page.locator("#curation-order-11")).toHaveValue("2");

  await curationTable
    .getByRole("button", { name: /^remove$/i })
    .first()
    .click();
  await expect(
    page.getByRole("heading", { name: /remove curated content/i }),
  ).toBeVisible();
  await page.getByRole("button", { name: /^remove content$/i }).click();

  await expect(curationTable.getByText(/^Content #1$/)).toHaveCount(0);

  await page.reload();

  await expect(
    page
      .getByRole("tablist", { name: /category localization tabs/i })
      .getByRole("tab", { name: /english/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /loading curated content/i }),
  ).toHaveCount(0);
  await expect(curationTable.getByText(/^Content #1$/)).toHaveCount(0);
  await expect(curationTable.getByText(/^Content #11$/)).toBeVisible();
});
