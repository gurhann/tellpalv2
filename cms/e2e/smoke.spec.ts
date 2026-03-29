import { expect, test } from "@playwright/test";

test("unauthenticated users land on the login route", async ({ page }) => {
  await page.goto("/login");

  await expect(
    page.getByRole("heading", { name: /sign in to tellpal cms/i }),
  ).toBeVisible();
});
