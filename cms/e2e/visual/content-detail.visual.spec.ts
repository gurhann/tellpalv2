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
  textlessCoverMediaId: 41,
  listeningCoverMediaId: 42,
  listingCoverMediaId: null,
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
    assignmentId: 101,
    contentId: 1,
    contributorId: 11,
    contributorDisplayName: "Annie Case",
    role: "AUTHOR",
    languageCode: "en",
    creditName: null,
    sortOrder: 0,
  },
  {
    assignmentId: 102,
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

for (const state of ["DISABLED", "PENDING", "FAILED", "LIVE"] as const) {
  for (const viewport of visualViewports) {
    test(`content detail visual - ${state} - ${viewport.name}`, async ({
      page,
    }) => {
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

      await page.route(
        "**/api/admin/contents/1/contributors",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(contributorAssignments),
          });
        },
      );

      await page.route("**/api/admin/media/41", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(imageAsset),
        });
      });

      await page.route("**/api/admin/media/42", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ...imageAsset, assetId: 42 }),
        });
      });

      await page.route("**/api/admin/media/*/content-token", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            previewUrl: imageAsset.cachedDownloadUrl,
            expiresAt: "2027-04-19T09:00:00Z",
          }),
        });
      });

      await page.route(
        "**/api/admin/contents/1/reading-publication?*",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              enabled: state !== "DISABLED",
              contract: "INTERNAL_LOCAL_TEST",
              state,
              generation: state === "DISABLED" ? null : 1,
              liveRevision:
                state === "LIVE" || state === "FAILED" ? "a".repeat(64) : null,
              diagnostic: state === "FAILED" ? "SOURCE_NOT_ADMITTED" : null,
              retryable: state === "FAILED",
            }),
          });
        },
      );
      const imageBase64 = await page.evaluate(() => {
        const canvas = document.createElement("canvas");
        canvas.width = 240;
        canvas.height = 360;
        const paint = canvas.getContext("2d")!;
        paint.fillStyle = "#d8ecf2";
        paint.fillRect(0, 0, 240, 360);
        paint.fillStyle = "#f6c94c";
        paint.beginPath();
        paint.arc(65, 80, 25, 0, Math.PI * 2);
        paint.fill();
        paint.fillStyle = "#ffffff";
        paint.fillRect(65, 140, 110, 85);
        return canvas.toDataURL("image/png").split(",")[1];
      });
      const preparedBytes = Buffer.from(imageBase64, "base64");
      const previewImage = {
        role: "READING_COVER",
        pageNumber: 0,
        renditionKey: "cover",
        sourceMime: "image/png",
        sourceBytes: 5000,
        sourceWidth: 240,
        sourceHeight: 360,
        mime: "image/png",
        bytes: preparedBytes.length,
        width: 240,
        height: 360,
      };
      await page.route(
        "**/api/admin/contents/1/reading-publication/preview?*",
        (route) =>
          route.fulfill({
            json: {
              identity: {
                contentId: 1,
                language: "tr",
                liveGeneration: 1,
                revision: "a".repeat(64),
                manifestDigest: "b".repeat(64),
                jobId: "12345678-1234-4234-8234-123456789abc",
              },
              images: [
                previewImage,
                {
                  ...previewImage,
                  role: "READING_DETAIL",
                  renditionKey: "detail",
                },
                {
                  ...previewImage,
                  role: "READING_PAGE",
                  pageNumber: 1,
                  renditionKey: "page",
                },
              ],
            },
          }),
      );
      await page.route(
        "**/api/admin/contents/1/reading-publication/preview/bytes?*",
        (route) =>
          route.fulfill({ contentType: "image/png", body: preparedBytes }),
      );
      await page.goto("/contents/1?language=tr");
      await expect(
        page.getByRole("button", { name: "Publish locale", exact: true }),
      ).toBeEnabled();
      await installVisualStyles(page);

      const contentDetail = page.locator("body");

      await expect(contentDetail).toBeVisible();
      await expect(
        page.getByRole("heading", { name: "Locale workspace", exact: true }),
      ).toBeVisible();
      if (state !== "DISABLED")
        await expect(
          page.getByText(
            "Images are prepared automatically in local/test. Original files are preserved.",
          ),
        ).toBeVisible();
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth),
      ).toBeLessThanOrEqual(viewport.width);
      await expect(contentDetail).toHaveScreenshot(
        state === "DISABLED"
          ? `content-detail-${viewport.name}.png`
          : `content-detail-reading-${state.toLowerCase()}-${viewport.name}.png`,
        {
          animations: "disabled",
          caret: "hide",
        },
      );
      if (state === "LIVE") {
        await page
          .getByRole("button", { name: "Preview prepared images", exact: true })
          .click();
        const dialog = page.getByRole("dialog", {
          name: "Prepared reading images",
          exact: true,
        });
        await expect(
          dialog.getByRole("img", { name: "Prepared cover", exact: true }),
        ).toBeVisible();
        await expect(dialog).toHaveScreenshot(
          `reading-publication-preview-${viewport.name}.png`,
          { animations: "disabled", caret: "hide" },
        );
        await page.keyboard.press("Escape");
      }
      if (state === "FAILED") {
        await page
          .getByLabel("Title", { exact: true })
          .fill("Unsaved title during retry");
        await page.route(
          "**/api/admin/contents/1/reading-publication/retry?*",
          async (route) => {
            expect(
              new URL(route.request().url()).searchParams.get("languageCode"),
            ).toBe("tr");
            await route.fulfill({
              status: 200,
              contentType: "application/json",
              body: JSON.stringify({
                enabled: true,
                contract: "INTERNAL_LOCAL_TEST",
                state: "PENDING",
                generation: 1,
                liveRevision: "a".repeat(64),
                diagnostic: null,
                retryable: false,
              }),
            });
          },
        );
        await page
          .getByRole("button", { name: "Retry reading preparation" })
          .click();
        await expect(
          page.getByText("Reading preparation pending"),
        ).toBeVisible();
        await expect(page.getByLabel("Title", { exact: true })).toHaveValue(
          "Unsaved title during retry",
        );
        expect(new URL(page.url()).searchParams.get("language")).toBe("tr");
      }
      if (state === "PENDING") {
        await page
          .getByLabel("Title", { exact: true })
          .fill("Unsaved title during adoption");
        await page.route(
          "**/api/admin/contents/1/reading-publication?*",
          async (route) =>
            route.fulfill({
              status: 200,
              contentType: "application/json",
              body: JSON.stringify({
                enabled: true,
                contract: "INTERNAL_LOCAL_TEST",
                state: "LIVE",
                generation: 1,
                liveRevision: "a".repeat(64),
                diagnostic: null,
                retryable: false,
              }),
            }),
        );
        await expect(page.getByText("Reading images ready")).toBeVisible({
          timeout: 6000,
        });
        await expect(page.getByLabel("Title", { exact: true })).toHaveValue(
          "Unsaved title during adoption",
        );
      }
    });
  }
}
