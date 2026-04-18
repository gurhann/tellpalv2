import { expect, test } from "@playwright/test";

import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
} from "./visual-test-helpers";

const detailViewports = [
  { name: "tablet", width: 768, height: 1024 },
  { name: "laptop", width: 1280, height: 900 },
  { name: "desktop", width: 1440, height: 1024 },
] as const;

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

test.beforeEach(async ({ page }) => {
  await stabilizeVisualPage(page);
  await mockVisualAuth(page);
});

for (const viewport of detailViewports) {
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
