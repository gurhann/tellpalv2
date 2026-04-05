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

type AssetResponse = {
  assetId: number;
  provider: "FIREBASE_STORAGE" | "LOCAL_STUB";
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
    accessTokenExpiresAt: "2026-04-04T18:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-05-04T18:00:00Z",
    ...overrides,
  };
}

function makeAssets(): AssetResponse[] {
  return [
    {
      assetId: 4,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/evening-garden-page-1.jpg",
      mediaType: "IMAGE",
      kind: "THUMBNAIL_PHONE",
      mimeType: "image/jpeg",
      byteSize: 98_112,
      checksumSha256: "image-checksum-4",
      cachedDownloadUrl:
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8B9pQAAAAASUVORK5CYII=",
      downloadUrlCachedAt: "2026-04-05T17:00:00Z",
      downloadUrlExpiresAt: "2026-04-05T19:00:00Z",
      createdAt: "2026-03-31T10:45:00Z",
      updatedAt: "2026-04-05T17:00:00Z",
    },
    {
      assetId: 1,
      provider: "LOCAL_STUB",
      objectPath: "/content/audio/rain-room-en.wav",
      mediaType: "AUDIO",
      kind: "ORIGINAL_AUDIO",
      mimeType: "audio/wav",
      byteSize: 5_242_880,
      checksumSha256: "audio-checksum-1",
      cachedDownloadUrl: null,
      downloadUrlCachedAt: null,
      downloadUrlExpiresAt: null,
      createdAt: "2026-03-30T09:00:00Z",
      updatedAt: "2026-03-30T09:00:00Z",
    },
    {
      assetId: 9,
      provider: "FIREBASE_STORAGE",
      objectPath: "/content/packages/story.evening-garden.en.zip",
      mediaType: "ARCHIVE",
      kind: "CONTENT_ZIP",
      mimeType: "application/zip",
      byteSize: 2_129_920,
      checksumSha256: null,
      cachedDownloadUrl: "https://cdn.tellpal.test/assets/9",
      downloadUrlCachedAt: "2026-03-31T11:30:00Z",
      downloadUrlExpiresAt: "2026-03-31T13:30:00Z",
      createdAt: "2026-03-31T11:25:00Z",
      updatedAt: "2026-03-31T11:30:00Z",
    },
  ];
}

test("media utility supports detail edit and cached URL refresh in the browser", async ({
  page,
}) => {
  const session = makeSession();
  const assets = makeAssets();
  const tinyAudioDataUrl =
    "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YQAAAAA=";
  const uploadedAsset: AssetResponse = {
    assetId: 11,
    provider: "FIREBASE_STORAGE",
    objectPath:
      "/local/manual/audio/original/2026/04/asset-11-bedtime-breeze.wav",
    mediaType: "AUDIO",
    kind: "ORIGINAL_AUDIO",
    mimeType: "audio/wav",
    byteSize: 8_192,
    checksumSha256: null,
    cachedDownloadUrl: null,
    downloadUrlCachedAt: null,
    downloadUrlExpiresAt: null,
    createdAt: "2026-04-04T18:20:00Z",
    updatedAt: "2026-04-04T18:20:00Z",
  };

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

  await page.route("**/api/admin/media?**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(assets),
    });
  });

  await page.route("**/api/admin/media/uploads", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        provider: "FIREBASE_STORAGE",
        objectPath: uploadedAsset.objectPath,
        uploadUrl:
          "https://firebase-storage.test/upload/local/manual/audio/original/2026/04/asset-11-bedtime-breeze.wav",
        httpMethod: "PUT",
        requiredHeaders: {
          "Content-Type": "audio/wav",
        },
        expiresAt: "2026-04-04T18:30:00Z",
        uploadToken: "upload-token-11",
      }),
    });
  });

  await page.route("https://firebase-storage.test/upload/**", async (route) => {
    await route.fulfill({
      status: 200,
      body: "",
    });
  });

  await page.route("**/api/admin/media/uploads/complete", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }

    assets.unshift(uploadedAsset);

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(uploadedAsset),
    });
  });

  await page.route("**/api/admin/media/11", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(assets.find((asset) => asset.assetId === 11)),
    });
  });

  await page.route("**/api/admin/media/4", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(assets.find((asset) => asset.assetId === 4)),
    });
  });

  await page.route("**/api/admin/media/11/metadata", async (route) => {
    if (route.request().method() !== "PUT") {
      await route.fallback();
      return;
    }

    const body = route.request().postDataJSON() as {
      mimeType?: string | null;
      byteSize?: number | null;
      checksumSha256?: string | null;
    };
    const nextAsset = assets.find((asset) => asset.assetId === 11);

    if (!nextAsset) {
      throw new Error("Asset #11 was not found.");
    }

    nextAsset.mimeType = body.mimeType ?? null;
    nextAsset.byteSize = body.byteSize ?? null;
    nextAsset.checksumSha256 = body.checksumSha256 ?? null;
    nextAsset.updatedAt = "2026-04-04T18:10:00Z";

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(nextAsset),
    });
  });

  await page.route(
    "**/api/admin/media/11/download-url-cache/refresh",
    async (route) => {
      if (route.request().method() !== "POST") {
        await route.fallback();
        return;
      }

      const nextAsset = assets.find((asset) => asset.assetId === 11);

      if (!nextAsset) {
        throw new Error("Asset #11 was not found.");
      }

      nextAsset.cachedDownloadUrl = tinyAudioDataUrl;
      nextAsset.downloadUrlCachedAt = "2026-04-05T18:15:00Z";
      nextAsset.downloadUrlExpiresAt = "2026-04-05T19:15:00Z";
      nextAsset.updatedAt = "2026-04-05T18:15:00Z";

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(nextAsset),
      });
    },
  );

  await page.goto("/media");

  await expect(
    page.getByRole("heading", { name: /^media utility$/i, level: 1 }),
  ).toBeVisible();

  await page
    .getByRole("table")
    .getByText("/content/images/evening-garden-page-1.jpg")
    .click();
  await expect(
    page.getByRole("img", { name: /preview of asset #4/i }),
  ).toBeVisible();
  await page.getByRole("button", { name: /close/i }).click();

  await page.getByRole("button", { name: /upload asset/i }).click();
  await page.getByLabel(/asset kind/i).click();
  await page.getByRole("option", { name: /original audio/i }).click();
  await page.getByLabel(/file/i).setInputFiles({
    name: "bedtime-breeze.wav",
    mimeType: "audio/wav",
    buffer: Buffer.from("audio"),
  });
  await page.getByRole("button", { name: /^upload asset$/i }).click();

  await expect(page.getByRole("heading", { name: /asset #11/i })).toBeVisible();
  await expect(page.getByLabel(/audio preview for asset #11/i)).toBeVisible();

  await page.getByLabel(/mime type/i).fill("audio/wav");
  await page.getByLabel(/byte size/i).fill("5243001");
  await page.getByLabel(/sha-256 checksum/i).fill("audio-checksum-11-refresh");
  await page.getByRole("button", { name: /save metadata/i }).click();

  await expect(page.getByLabel(/mime type/i)).toHaveValue("audio/wav");

  await page
    .getByRole("button", { name: /refresh cached url/i })
    .nth(1)
    .click();

  await expect(page.getByText(/^available$/i)).toBeVisible();
  await expect(page.getByLabel(/audio preview for asset #11/i)).toBeVisible();
  await expect(
    page.getByText(
      /3 recent assets already carry a cached download url snapshot\./i,
    ),
  ).toBeVisible();

  await page.reload();

  await expect(
    page.getByRole("heading", { name: /^media utility$/i, level: 1 }),
  ).toBeVisible();
  await page
    .getByRole("table", { name: /recent asset registry/i })
    .getByText(uploadedAsset.objectPath)
    .click();
  await expect(page.getByLabel(/mime type/i)).toHaveValue("audio/wav");
  await expect(page.getByText(/^available$/i)).toBeVisible();
  await expect(page.getByLabel(/audio preview for asset #11/i)).toBeVisible();
});
