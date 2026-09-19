import { expect, test } from "@playwright/test";

test("keeps the CMS in light mode when the system prefers dark", async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await page.goto("/login");

  await expect(page.locator("html")).not.toHaveClass(/\bdark\b/);
  await expect(page.locator("html")).toHaveCSS("color-scheme", "light");
  await expect(page.locator("body")).toHaveCSS(
    "background-color",
    "oklch(1 0 0)",
  );
  await expect(
    page.getByRole("heading", { name: /sign in to tellpal cms/i }),
  ).toBeVisible();
});
