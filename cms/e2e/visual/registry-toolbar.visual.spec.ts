import { expect, test } from "@playwright/test";

import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
  visualViewports,
} from "./visual-test-helpers";

const contentRecords = [
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
        languageCode: "en",
        title: "Rain Room Reset",
        description: "A short breathing reset with rain ambience.",
        bodyText: "Breathe in for four counts and relax your shoulders.",
        coverMediaId: null,
        audioMediaId: 1,
        durationMinutes: 6,
        status: "DRAFT",
        processingStatus: "PENDING",
        publishedAt: null,
        visibleToMobile: false,
      },
    ],
  },
  {
    contentId: 3,
    type: "LULLABY",
    externalKey: "lullaby.moon-softly",
    active: false,
    ageRange: 3,
    pageCount: null,
    localizations: [],
  },
];

const categoryRecords = [
  {
    categoryId: 7,
    slug: "featured-sleep",
    type: "STORY",
    premium: false,
    active: true,
  },
  {
    categoryId: 8,
    slug: "bedtime-meditations",
    type: "MEDITATION",
    premium: true,
    active: true,
  },
  {
    categoryId: 9,
    slug: "quiet-nights",
    type: "LULLABY",
    premium: false,
    active: false,
  },
];

test.beforeEach(async ({ page }) => {
  await stabilizeVisualPage(page);
  await mockVisualAuth(page);
});

for (const viewport of visualViewports) {
  test(`contents registry toolbar visual - ${viewport.name}`, async ({
    page,
  }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    });

    await page.route("**/api/admin/contents", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contentRecords),
      });
    });

    await page.goto("/contents");
    await installVisualStyles(page);

    const toolbar = page.locator('section[aria-label="Content registry filters"]');

    await expect(toolbar).toBeVisible();
    await expect(toolbar).toHaveScreenshot(
      `contents-registry-toolbar-${viewport.name}.png`,
      {
        animations: "disabled",
        caret: "hide",
      },
    );
  });

  test(`categories registry toolbar visual - ${viewport.name}`, async ({
    page,
  }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    });

    await page.route("**/api/admin/categories", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(categoryRecords),
      });
    });

    await page.goto("/categories");
    await installVisualStyles(page);

    const toolbar = page.locator(
      'section[aria-label="Category registry filters"]',
    );

    await expect(toolbar).toBeVisible();
    await expect(toolbar).toHaveScreenshot(
      `categories-registry-toolbar-${viewport.name}.png`,
      {
        animations: "disabled",
        caret: "hide",
      },
    );
  });
}
