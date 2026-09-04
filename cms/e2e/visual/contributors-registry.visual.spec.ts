import { expect, test } from "@playwright/test";

import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
  visualViewports,
} from "./visual-test-helpers";

const registryPage = {
  items: [
    {
      contributorId: 1,
      displayName: "Ayşe Yılmaz",
      roles: ["AUTHOR"],
      totalUsageCount: 8,
      usageByRole: { AUTHOR: 8 },
      updatedAt: "2026-04-17T12:00:00Z",
    },
    {
      contributorId: 2,
      displayName: "Mehmet Kaya",
      roles: ["NARRATOR", "MUSICIAN"],
      totalUsageCount: 4,
      usageByRole: { NARRATOR: 3, MUSICIAN: 1 },
      updatedAt: "2026-04-16T12:00:00Z",
    },
  ],
  page: 0,
  size: 25,
  totalItems: 2,
  totalPages: 1,
};

for (const viewport of visualViewports) {
  test(`contributors registry visual - ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({
      width: viewport.width,
      height: viewport.height,
    });
    await stabilizeVisualPage(page);
    await mockVisualAuth(page);
    await page.route("**/api/admin/contributor-registry**", async (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(registryPage),
      }),
    );
    await page.goto("/contributors");
    await page
      .getByRole("main")
      .getByRole("heading", { name: /^contributors$/i })
      .waitFor();
    await installVisualStyles(page);
    await expect(page).toHaveScreenshot(
      `contributors-registry-${viewport.name}.png`,
      { fullPage: true },
    );
  });
}
