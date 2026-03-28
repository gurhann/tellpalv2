import { expect, test } from "@playwright/test";

test("login route and scaffold navigation work", async ({ page }) => {
  await page.goto("/login");

  await expect(
    page.getByRole("heading", { name: /login route is ready/i }),
  ).toBeVisible();

  await page.getByRole("button", { name: /enter scaffold workspace/i }).click();

  await expect(
    page.getByRole("heading", { name: /content studio/i }),
  ).toBeVisible();
});
