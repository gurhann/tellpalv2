import { expect, test } from "@playwright/test";
import { readFile } from "node:fs/promises";
import { join } from "node:path";

test("normal CMS uploads, one Publish and verified prepared previews", async ({
  page,
}) => {
  const directory = process.env.TELLPAL_CMS_ACCEPTANCE_DIRECTORY;
  const contentId = process.env.TELLPAL_CMS_ACCEPTANCE_CONTENT;
  test.skip(
    !directory || !contentId,
    "Ayrı gerçek yerel backend kabulünde çalışır.",
  );
  const session = JSON.parse(
    await readFile(join(directory!, "session.json"), "utf8"),
  );
  await page.addInitScript(() =>
    localStorage.setItem(
      "tellpal.cms.refresh-token",
      "synthetic-local-refresh",
    ),
  );
  await page.route("**/api/admin/auth/refresh", (route) =>
    route.fulfill({ json: session }),
  );
  await page.emulateMedia({ reducedMotion: "reduce", colorScheme: "light" });
  await page.setViewportSize({ width: 1280, height: 900 });
  const upload = async (
    root: ReturnType<typeof page.getByTestId>,
    file: string,
  ) => {
    await root.getByRole("button", { name: "Upload new", exact: true }).click();
    const dialog = page.getByRole("dialog", { name: /^Upload / });
    await dialog
      .getByLabel("File", { exact: true })
      .setInputFiles(join(directory!, file));
    await dialog
      .getByRole("button", { name: "Upload asset", exact: true })
      .click();
    await expect(dialog).not.toBeVisible();
  };
  await page.goto(`/contents/${contentId}/story-pages?language=tr`);
  await page.getByRole("button", { name: /edit page 1/i }).click();
  await upload(
    page.getByTestId("story-page-tr-illustration-asset"),
    "page.png",
  );
  await page.getByRole("button", { name: /save page localization/i }).click();
  await page.getByRole("button", { name: /close editor/i }).click();
  await page.goto(`/contents/${contentId}?language=tr`);
  await upload(
    page.getByTestId("content-localization-cover-asset"),
    "cover.jpg",
  );
  await page
    .getByRole("button", { name: "Save localization", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Save localization", exact: true }),
  ).toBeEnabled();
  let publishes = 0;
  page.on("request", (request) => {
    if (
      request.url().includes(`/localizations/tr/publish`) &&
      request.method() === "POST"
    )
      publishes++;
  });
  await page
    .getByRole("button", { name: "Publish locale", exact: true })
    .click();
  await page
    .getByLabel("Title", { exact: true })
    .fill("Unsaved text remains during preparation");
  await expect(
    page.getByText("Reading images ready", { exact: true }),
  ).toBeVisible({ timeout: 30_000 });
  await expect(page.getByLabel("Title", { exact: true })).toHaveValue(
    "Unsaved text remains during preparation",
  );
  expect(new URL(page.url()).searchParams.get("language")).toBe("tr");
  expect(publishes).toBe(1);
  const preview = page.getByRole("button", {
    name: "Preview prepared images",
    exact: true,
  });
  await preview.focus();
  await page.keyboard.press("Enter");
  const dialog = page.getByRole("dialog", {
    name: "Prepared reading images",
    exact: true,
  });
  const assertDecoded = async (alt: string) => {
    const image = dialog.getByRole("img", { name: alt, exact: true });
    await expect(image).toBeVisible();
    expect(
      await image.evaluate(async (img) => {
        const raster = img as HTMLImageElement;
        await raster.decode();
        return raster.naturalWidth;
      }),
    ).toBe(80);
  };
  await assertDecoded("Prepared cover");
  await expect(
    dialog.getByText("JPEG preserved unchanged · same size"),
  ).toBeVisible();
  await dialog.getByRole("button", { name: "Page 1", exact: true }).click();
  await assertDecoded("Prepared page 1");
  await expect(dialog.getByText(/WEBP/)).toBeVisible();
  for (const width of [1280, 1440]) {
    await page.setViewportSize({ width, height: width === 1280 ? 900 : 1024 });
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBeLessThanOrEqual(width);
    await page.screenshot({
      path: join(directory!, `prepared-dialog-${width}.png`),
    });
  }
  await page.keyboard.press("Escape");
  await expect(dialog).not.toBeVisible();
  await expect(preview).toBeFocused();
  const response = await page.request.get(
    `${process.env.TELLPAL_CMS_ACCEPTANCE_BACKEND}/api/admin/contents/${contentId}/reading-publication/preview?languageCode=tr`,
    { headers: { Authorization: `Bearer ${session.accessToken}` } },
  );
  const manifest = await response.json();
  expect(response.headers()["cache-control"]).toBe("private,no-store");
  for (const image of manifest.images) {
    const params = new URLSearchParams({
      languageCode: "tr",
      liveGeneration: String(manifest.identity.liveGeneration),
      revision: manifest.identity.revision,
      manifestDigest: manifest.identity.manifestDigest,
      jobId: manifest.identity.jobId,
      role: image.role,
      pageNumber: String(image.pageNumber),
      renditionKey: image.renditionKey,
    });
    const bytes = await page.request.get(
      `${process.env.TELLPAL_CMS_ACCEPTANCE_BACKEND}/api/admin/contents/${contentId}/reading-publication/preview/bytes?${params}`,
      { headers: { Authorization: `Bearer ${session.accessToken}` } },
    );
    expect(bytes.status()).toBe(200);
    expect((await bytes.body()).length).toBe(image.bytes);
  }
  const oldJob = manifest.identity.jobId;
  await page
    .getByRole("button", { name: "Save localization", exact: true })
    .click();
  await expect
    .poll(
      async () => {
        const result = await page.request.get(
          `${process.env.TELLPAL_CMS_ACCEPTANCE_BACKEND}/api/admin/contents/${contentId}/reading-publication?languageCode=tr`,
          { headers: { Authorization: `Bearer ${session.accessToken}` } },
        );
        return (await result.json()).state;
      },
      { timeout: 30_000 },
    )
    .toBe("LIVE");
  const reused = await page.request.get(
    `${process.env.TELLPAL_CMS_ACCEPTANCE_BACKEND}/api/admin/contents/${contentId}/reading-publication/preview?languageCode=tr`,
    { headers: { Authorization: `Bearer ${session.accessToken}` } },
  );
  expect((await reused.json()).identity.jobId).toBe(oldJob);
});
