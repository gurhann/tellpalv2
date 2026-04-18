import { expect, test } from "@playwright/test";

import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
  visualViewports,
} from "./visual-test-helpers";

const contentRecord = {
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
      coverMediaId: 41,
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
      coverMediaId: 41,
      audioMediaId: null,
      durationMinutes: 8,
      status: "DRAFT",
      processingStatus: "PROCESSING",
      publishedAt: null,
      visibleToMobile: false,
    },
  ],
};

const contributorAssignments = [
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

const imageAsset = {
  assetId: 41,
  provider: "LOCAL_STUB",
  objectPath: "/local/manual/images/original/2026/04/evening-garden-cover.jpg",
  mediaType: "IMAGE",
  kind: "ORIGINAL_IMAGE",
  mimeType: "image/jpeg",
  byteSize: null,
  checksumSha256: null,
  cachedDownloadUrl:
    "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 240 360'><rect width='240' height='360' fill='%23f6efe3'/><rect x='24' y='24' width='192' height='312' rx='24' fill='%23d8ecf2'/><circle cx='70' cy='84' r='20' fill='%23f6c94c'/><rect x='68' y='132' width='104' height='84' rx='18' fill='%23ffffff'/><rect x='56' y='240' width='128' height='52' rx='18' fill='%23f4d8e7'/></svg>",
  downloadUrlCachedAt: "2026-04-18T09:00:00Z",
  downloadUrlExpiresAt: "2026-04-19T09:00:00Z",
  createdAt: "2026-04-18T09:00:00Z",
  updatedAt: "2026-04-18T09:00:00Z",
};

test.beforeEach(async ({ page }) => {
  await stabilizeVisualPage(page);
  await mockVisualAuth(page);
});

for (const viewport of visualViewports) {
  test(`content detail visual - ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    });

    await page.route("**/api/admin/contents/1", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contentRecord),
      });
    });

    await page.route("**/api/admin/contents/1/contributors", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contributorAssignments),
      });
    });

    await page.route("**/api/admin/media/41", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(imageAsset),
      });
    });

    await page.goto("/contents/1");
    await installVisualStyles(page);

    const contentDetail = page.locator("main");

    await expect(contentDetail).toBeVisible();
    await expect(contentDetail).toHaveScreenshot(
      `content-detail-${viewport.name}.png`,
      {
        animations: "disabled",
        caret: "hide",
      },
    );
  });
}
