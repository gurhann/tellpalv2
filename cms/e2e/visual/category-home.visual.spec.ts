import { expect, test, type Page } from "@playwright/test";
import {
  installVisualStyles,
  mockVisualAuth,
  stabilizeVisualPage,
  visualViewports,
} from "./visual-test-helpers";
const row = (
  categoryId: number,
  slug: string,
  name: string,
  displayOrder: number | null,
) => ({
  categoryId,
  slug,
  name,
  description: "Stories to explore imagination and read together.",
  type: "STORY",
  active: true,
  status: "PUBLISHED",
  displayOrder,
  audience: "ALL",
  contentLanguage: null,
  version: 0,
});
async function fixture(page: Page) {
  const snapshots = {
    tr: {
      language: "tr",
      revision: "tr-1",
      rows: [
        row(1, "alpha", "Harika Maceralar", 1),
        row(2, "zeta", "Birlikte Okuyalım", 0),
        row(3, "english", "İngilizce Hikâyeler", 2),
        row(4, "empty", "Boş satır", null),
      ],
    },
    en: {
      language: "en",
      revision: "en-1",
      rows: [
        row(1, "alpha", "Wonderful Adventures", 0),
        row(2, "zeta", "Read Together", 1),
        row(3, "english", "English Stories", 2),
        row(4, "empty", "Empty row", null),
      ],
    },
  };
  let fail = false;
  const saves: { language: string; ids: number[] }[] = [];
  await stabilizeVisualPage(page);
  await mockVisualAuth(page);
  await page.route("**/api/admin/categories/home?*", async (route) => {
    const language = new URL(route.request().url()).searchParams.get(
      "lang",
    ) as keyof typeof snapshots;
    const snapshot = snapshots[language];
    if (route.request().method() === "PUT") {
      if (fail) {
        await route.fulfill({
          status: 409,
          contentType: "application/problem+json",
          body: JSON.stringify({
            type: "about:blank",
            status: 409,
            title: "Library order changed",
            detail: "Reload before saving.",
          }),
        });
        return;
      }
      const body = route.request().postDataJSON();
      expect(body.expectedRevision).toBe(snapshot.revision);
      snapshot.rows = snapshot.rows.map((r) => ({
        ...r,
        ...body.rows.find(
          (x: { categoryId: number }) => x.categoryId === r.categoryId,
        ),
        version: r.version + 1,
      }));
      snapshot.revision += "-saved";
      saves.push({
        language,
        ids: [...snapshot.rows]
          .filter((r) => r.displayOrder !== null)
          .sort((a, b) => a.displayOrder! - b.displayOrder!)
          .map((r) => r.categoryId),
      });
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(snapshot),
    });
  });
  return {
    snapshots,
    saves,
    setFailure: (value: boolean) => {
      fail = value;
    },
  };
}
for (const viewport of visualViewports) {
  test(`Library home order visual - ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await fixture(page);
    await page.goto("/categories/home?lang=tr");
    await installVisualStyles(page);
    await expect(
      page.getByRole("heading", { name: "Library order", exact: true }),
    ).toBeVisible();
    await page.evaluate(async () => {
      await document.fonts.load(
        '14px "Geist Variable"',
        "Türkçe Audience Selected language",
      );
      await document.fonts.ready;
    });
    await expect(page.locator("main")).toHaveScreenshot(
      `category-home-${viewport.name}.png`,
      { animations: "disabled", caret: "hide" },
    );
  });
}
test("Library save failure keeps draft, retry/reload persists Turkish and leaves English order independent", async ({
  page,
}) => {
  const data = await fixture(page);
  await page.goto("/categories/home?lang=tr");
  const list = page.getByRole("list", { name: "Library row order" });
  await expect(list.getByRole("listitem").first()).toContainText(
    "Birlikte Okuyalım",
  );
  await page
    .getByRole("button", { name: "Move up: Harika Maceralar", exact: true })
    .click();
  await expect(list.getByRole("listitem").first()).toContainText(
    "Harika Maceralar",
  );
  data.setFailure(true);
  await page.getByRole("button", { name: "Save order", exact: true }).click();
  await expect(
    page.getByText("Reload before saving.", { exact: true }),
  ).toBeVisible();
  await expect(list.getByRole("listitem").first()).toContainText(
    "Harika Maceralar",
  );
  expect(data.saves).toEqual([]);
  data.setFailure(false);
  await page.getByRole("button", { name: "Save order", exact: true }).click();
  await expect(page.getByText("Order saved.", { exact: true })).toBeVisible();
  expect(data.saves).toEqual([{ language: "tr", ids: [1, 2, 3] }]);
  expect(data.snapshots.en.revision).toBe("en-1");
  await page.getByRole("button", { name: "Reload", exact: true }).click();
  await expect(list.getByRole("listitem").first()).toContainText(
    "Harika Maceralar",
  );
  await page
    .getByRole("combobox", { name: "Presentation language", exact: true })
    .selectOption("en");
  await expect(list.getByRole("listitem").first()).toContainText(
    "Wonderful Adventures",
  );
  await page
    .getByRole("combobox", { name: "Presentation language", exact: true })
    .selectOption("tr");
  await expect(list.getByRole("listitem").first()).toContainText(
    "Harika Maceralar",
  );
});
