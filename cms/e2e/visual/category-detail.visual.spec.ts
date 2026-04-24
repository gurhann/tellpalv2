import { expect, test } from "@playwright/test";

import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
  visualViewports,
} from "./visual-test-helpers";

const categoryRecord = {
  categoryId: 7,
  slug: "quiet-nights",
  type: "STORY",
  premium: false,
  active: true,
};

const localizations = [
  {
    categoryId: 7,
    languageCode: "en",
    name: "Quiet Nights",
    description: "Soft stories curated for bedtime.",
    imageMediaId: null,
    status: "PUBLISHED",
    publishedAt: "2026-04-16T20:00:00Z",
    published: true,
  },
  {
    categoryId: 7,
    languageCode: "tr",
    name: "Sessiz Geceler",
    description: "Uyku zamani icin secilen sakin hikayeler.",
    imageMediaId: null,
    status: "DRAFT",
    publishedAt: null,
    published: false,
  },
];

const curatedContent = [
  {
    categoryId: 7,
    languageCode: "en",
    contentId: 11,
    displayOrder: 0,
    externalKey: "story.ecenin-fasulye-deneyi",
    localizedTitle: "Ecenin Fasulye Deneyi",
  },
];

for (const viewport of visualViewports) {
  test(`category detail visual - ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    });

    await stabilizeVisualPage(page);
    await mockVisualAuth(page);

    await page.route("**/api/admin/categories/7", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(categoryRecord),
      });
    });

    await page.route(
      "**/api/admin/categories/7/localizations",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(localizations),
        });
      },
    );

    await page.route(
      "**/api/admin/categories/7/localizations/en/contents",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(curatedContent),
        });
      },
    );

    await page.goto("/categories/7");
    await installVisualStyles(page);

    const categoryDetail = page.locator("main");

    await expect(categoryDetail).toBeVisible();
    await expect(categoryDetail).toHaveScreenshot(
      `category-detail-${viewport.name}.png`,
      {
        animations: "disabled",
        caret: "hide",
      },
    );
  });
}
