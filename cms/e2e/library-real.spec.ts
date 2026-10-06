import { expect, test } from "@playwright/test";
import fs from "node:fs";
const runtimePath = process.env.LIBRARY_RUNTIME_FILE;
const tokenPath = process.env.LIBRARY_ID_TOKEN_FILE;
test.use({ trace: "off", video: "off" });
test("real administered fixture saves Turkish row order independently and survives reload", async ({
  page,
  request,
}) => {
  test.skip(
    !runtimePath || !tokenPath,
    "Requires explicitly prepared disposable Library/Firebase runtime",
  );
  const runtime = JSON.parse(fs.readFileSync(runtimePath!, "utf8"));
  const token = fs.readFileSync(tokenPath!, "utf8").trim();
  const base = "http://127.0.0.1:8082";
  const login = await request.post(base + "/api/admin/auth/login", {
    data: {
      username: runtime.admin_username,
      password: runtime.admin_password,
    },
  });
  expect(login.status()).toBe(200);
  const session = await login.json();
  const adminHeaders = { Authorization: "Bearer " + session.accessToken };
  const mobileHeaders = { Authorization: "Bearer " + token };
  const initialSnapshot = await (
    await request.get(base + "/api/admin/categories/home?lang=tr", {
      headers: adminHeaders,
    })
  ).json();
  const initialOrder = [5, 6, 2, 1, 3, 4];
  const reset = await request.put(base + "/api/admin/categories/home?lang=tr", {
    headers: adminHeaders,
    data: {
      expectedRevision: initialSnapshot.revision,
      rows: initialSnapshot.rows.map((r: any) => ({
        categoryId: r.categoryId,
        displayOrder: initialOrder.includes(r.categoryId)
          ? initialOrder.indexOf(r.categoryId)
          : r.displayOrder,
        audience: r.audience,
        contentLanguage: r.contentLanguage,
      })),
    },
  });
  expect(reset.status()).toBe(200);
  const beforeTR = await (
    await request.get(base + "/api/test/library?lang=tr", {
      headers: mobileHeaders,
    })
  ).json();
  const beforeEN = await (
    await request.get(base + "/api/test/library?lang=en", {
      headers: mobileHeaders,
    })
  ).json();
  await page.addInitScript((refresh) => {
    if (!window.localStorage.getItem("tellpal.cms.refresh-token"))
      if (!window.localStorage.getItem("tellpal.cms.refresh-token"))
        window.localStorage.setItem("tellpal.cms.refresh-token", refresh);
    window.localStorage.setItem("tellpal.cms.locale", "en");
  }, session.refreshToken);
  await page.goto("/categories/home?lang=tr");
  await expect(
    page.getByRole("heading", { name: "Library order", exact: true }),
  ).toBeVisible();
  for (let i = 0; i < 2; i++)
    await page
      .getByRole("button", {
        name: "Move up: İngilizce Hikâyeler",
        exact: true,
      })
      .click();
  const concurrent = await (
    await request.get(base + "/api/admin/categories/home?lang=tr", {
      headers: adminHeaders,
    })
  ).json();
  const conflicting = await request.put(
    base + "/api/admin/categories/home?lang=tr",
    {
      headers: adminHeaders,
      data: {
        expectedRevision: concurrent.revision,
        rows: concurrent.rows.map((r: any) => ({
          categoryId: r.categoryId,
          displayOrder: r.displayOrder,
          audience: r.audience,
          contentLanguage: r.contentLanguage,
        })),
      },
    },
  );
  expect(conflicting.status()).toBe(200);
  await page.getByRole("button", { name: "Save order", exact: true }).click();
  await expect(
    page.getByText("Library order changed; reload before saving", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("list", { name: "Library row order" })
      .getByRole("listitem")
      .nth(2),
  ).toContainText("İngilizce Hikâyeler");
  await page.getByRole("button", { name: "Reload", exact: true }).click();
  await expect(
    page.getByRole("button", {
      name: "Move up: İngilizce Hikâyeler",
      exact: true,
    }),
  ).toBeEnabled();
  for (let i = 0; i < 2; i++)
    await page
      .getByRole("button", {
        name: "Move up: İngilizce Hikâyeler",
        exact: true,
      })
      .click();
  await page.getByRole("button", { name: "Save order", exact: true }).click();
  await expect(page.getByText("Order saved.", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Reload", exact: true }).click();
  await page.reload();
  const list = page.getByRole("list", { name: "Library row order" });
  await expect(list.getByRole("listitem").nth(2)).toContainText(
    "İngilizce Hikâyeler",
  );
  const afterTR = await (
    await request.get(base + "/api/test/library?lang=tr", {
      headers: mobileHeaders,
    })
  ).json();
  const afterEN = await (
    await request.get(base + "/api/test/library?lang=en", {
      headers: mobileHeaders,
    })
  ).json();
  const summary = (catalog: any) =>
    catalog.rows.map((row: any) => ({
      categoryId: row.categoryId,
      contentLanguage: row.contentLanguage,
      items: row.items.map((i: any) => ({
        contentId: i.contentId,
        title: i.title,
        isFree: i.isFree,
        isLocked: i.isLocked,
        phoneAssetId: i.covers.phone?.assetId,
        tabletAssetId: i.covers.tablet?.assetId,
      })),
    }));
  expect(afterTR.rows.map((r: any) => r.categoryId)).toEqual(
    beforeTR.audience === "SUBSCRIBER" ? [3, 2, 1] : [3, 2, 1, 4],
  );
  expect(summary(afterEN)).toEqual(summary(beforeEN));
  for (const row of beforeTR.rows)
    expect(
      summary(afterTR).find((r: any) => r.categoryId === row.categoryId),
    ).toEqual(
      summary(beforeTR).find((r: any) => r.categoryId === row.categoryId),
    );
  const saved = await request.get(base + "/api/admin/categories/home?lang=tr", {
    headers: adminHeaders,
  });
  expect(saved.status()).toBe(200);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.locator("main").screenshot({
    path: "../_bmad-output/implementation-artifacts/mobile/library-1-1-cms-real.png",
  });
  const artifact = {
    timestamp: new Date().toISOString(),
    source: "real CMS + backend + Firebase",
    beforeTR: summary(beforeTR),
    afterTR: summary(afterTR),
    englishUnchanged: true,
    realConflictRecovery: true,
    cardOrdersUnchanged: true,
  };
  fs.writeFileSync(
    "../_bmad-output/implementation-artifacts/mobile/library-1-1-cms-real.json",
    JSON.stringify(artifact, null, 2) + "\n",
  );
});

test("real Library audience and explicit language selections persist and change server visibility", async ({
  page,
  request,
}) => {
  test.skip(
    !runtimePath || !tokenPath,
    "Requires prepared disposable Library/Firebase runtime",
  );
  const runtime = JSON.parse(fs.readFileSync(runtimePath!, "utf8"));
  const token = fs.readFileSync(tokenPath!, "utf8").trim();
  const base = "http://127.0.0.1:8082";
  const login = await request.post(base + "/api/admin/auth/login", {
    data: {
      username: runtime.admin_username,
      password: runtime.admin_password,
    },
  });
  expect(login.status()).toBe(200);
  const session = await login.json();
  const adminHeaders = { Authorization: "Bearer " + session.accessToken };
  const mobileHeaders = { Authorization: "Bearer " + token };
  const catalog = async () => {
    const response = await request.get(base + "/api/test/library?lang=tr", {
      headers: mobileHeaders,
    });
    expect(response.status()).toBe(200);
    return response.json();
  };
  const snapshot = async () => {
    const response = await request.get(
      base + "/api/admin/categories/home?lang=tr",
      { headers: adminHeaders },
    );
    expect(response.status()).toBe(200);
    return response.json();
  };
  const original = await snapshot();
  const originalCatalog = await catalog();
  expect(
    original.rows.find((r: any) => r.categoryId === 3).contentLanguage,
  ).toBe("en");
  expect(original.rows.find((r: any) => r.categoryId === 4).audience).toBe(
    "NON_SUBSCRIBER",
  );
  await page.addInitScript((refresh) => {
    if (!window.localStorage.getItem("tellpal.cms.refresh-token"))
      window.localStorage.setItem("tellpal.cms.refresh-token", refresh);
    window.localStorage.setItem("tellpal.cms.locale", "en");
  }, session.refreshToken);
  await page.goto("/categories/home?lang=tr");
  const mapped = page.getByRole("combobox", {
    name: "İngilizce Hikâyeler content language",
    exact: true,
  });
  const audience = page.getByRole("combobox", {
    name: "Premium Hikâyeleri Keşfet audience",
    exact: true,
  });
  const saveAndReload = async () => {
    await page.getByRole("button", { name: "Save order", exact: true }).click();
    await expect(page.getByText("Order saved.", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Reload", exact: true }).click();
    await page.reload();
    await expect(mapped).toBeVisible();
  };
  try {
    await mapped.selectOption("");
    await audience.selectOption("ALL");
    await saveAndReload();
    await expect(mapped).toHaveValue("");
    await expect(audience).toHaveValue("ALL");
    const persisted = await snapshot();
    expect(
      persisted.rows.find((r: any) => r.categoryId === 3).contentLanguage,
    ).toBeNull();
    expect(persisted.rows.find((r: any) => r.categoryId === 4).audience).toBe(
      "ALL",
    );
    const changed = await catalog();
    expect(changed.rows.some((r: any) => r.categoryId === 3)).toBe(false);
    expect(changed.rows.some((r: any) => r.categoryId === 4)).toBe(true);
    if (changed.audience === "SUBSCRIBER")
      expect(
        changed.rows
          .flatMap((r: any) => r.items)
          .every((i: any) => !i.isLocked),
      ).toBe(true);
    await mapped.selectOption("en");
    await audience.selectOption("NON_SUBSCRIBER");
    await saveAndReload();
    await expect(mapped).toHaveValue("en");
    await expect(audience).toHaveValue("NON_SUBSCRIBER");
    const restored = await catalog();
    expect(restored.rows.map((r: any) => r.categoryId)).toEqual(
      originalCatalog.rows.map((r: any) => r.categoryId),
    );
    expect(
      restored.rows
        .find((r: any) => r.categoryId === 3)
        .items.map((i: any) => i.contentId),
    ).toEqual([9, 10]);
    if (restored.audience === "SUBSCRIBER")
      expect(restored.rows.some((r: any) => r.categoryId === 4)).toBe(false);
    fs.writeFileSync(
      `../_bmad-output/implementation-artifacts/mobile/library-1-1-cms-selections-${restored.audience.toLowerCase()}.json`,
      JSON.stringify(
        {
          timestamp: new Date().toISOString(),
          source:
            "actual CMS user selections + persisted backend + real Firebase identity",
          audience: restored.audience,
          changedRows: changed.rows.map((r: any) => r.categoryId),
          restoredRows: restored.rows.map((r: any) => r.categoryId),
          languageMappingRoundTrip: true,
          audienceRoundTrip: true,
          serverAudienceVisibility: true,
          restoredFinalFixture: true,
        },
        null,
        2,
      ) + "\n",
    );
  } finally {
    // Restore the exact starting configuration even when a regression assertion fails.
    const current = await snapshot();
    const reset = await request.put(
      base + "/api/admin/categories/home?lang=tr",
      {
        headers: adminHeaders,
        data: {
          expectedRevision: current.revision,
          rows: original.rows.map((r: any) => ({
            categoryId: r.categoryId,
            displayOrder: r.displayOrder,
            audience: r.audience,
            contentLanguage: r.contentLanguage,
          })),
        },
      },
    );
    expect(reset.status()).toBe(200);
  }
});

test("real Show in Library checkbox persists opt-out/in and changes only Turkish catalog visibility", async ({
  page,
  request,
}) => {
  test.skip(
    !runtimePath || !tokenPath,
    "Requires disposable real Library/Firebase runtime",
  );
  const runtime = JSON.parse(fs.readFileSync(runtimePath!, "utf8"));
  const token = fs.readFileSync(tokenPath!, "utf8").trim();
  const base = "http://127.0.0.1:8082";
  const login = await request.post(base + "/api/admin/auth/login", {
    data: {
      username: runtime.admin_username,
      password: runtime.admin_password,
    },
  });
  expect(login.status()).toBe(200);
  const session = await login.json();
  const adminHeaders = { Authorization: "Bearer " + session.accessToken };
  const mobileHeaders = { Authorization: "Bearer " + token };
  const snapshot = async () =>
    (
      await request.get(base + "/api/admin/categories/home?lang=tr", {
        headers: adminHeaders,
      })
    ).json();
  const catalog = async (lang: string) =>
    (
      await request.get(base + "/api/test/library?lang=" + lang, {
        headers: mobileHeaders,
      })
    ).json();
  const original = await snapshot();
  const trBefore = await catalog("tr");
  const enBefore = await catalog("en");
  const ids = (value: any) =>
    value.rows.map((row: any) => ({
      categoryId: row.categoryId,
      items: row.items.map((item: any) => item.contentId),
    }));
  await page.addInitScript((refresh) => {
    if (!window.localStorage.getItem("tellpal.cms.refresh-token")) {
      window.localStorage.setItem("tellpal.cms.refresh-token", refresh);
    }
    window.localStorage.setItem("tellpal.cms.locale", "en");
  }, session.refreshToken);
  await page.goto("/categories/home?lang=tr");
  const checkbox = page.getByRole("checkbox", {
    name: "İngilizce Hikâyeler Show in Library",
    exact: true,
  });
  const saveReload = async () => {
    await page.getByRole("button", { name: "Save order", exact: true }).click();
    await expect(page.getByText("Order saved.", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Reload", exact: true }).click();
    await page.reload();
    await expect(checkbox).toBeVisible();
  };
  try {
    await expect(checkbox).toBeChecked();
    await checkbox.uncheck();
    await saveReload();
    await expect(checkbox).not.toBeChecked();
    expect(
      (await snapshot()).rows.find((row: any) => row.categoryId === 3)
        .displayOrder,
    ).toBeNull();
    expect(
      (await catalog("tr")).rows.some((row: any) => row.categoryId === 3),
    ).toBe(false);
    expect(ids(await catalog("en"))).toEqual(ids(enBefore));
    await checkbox.check();
    await saveReload();
    await expect(checkbox).toBeChecked();
    const restored = await catalog("tr");
    expect(ids(restored).find((row: any) => row.categoryId === 3)).toEqual(
      ids(trBefore).find((row: any) => row.categoryId === 3),
    );
    expect(ids(await catalog("en"))).toEqual(ids(enBefore));
    fs.writeFileSync(
      `../_bmad-output/implementation-artifacts/mobile/library-1-1-cms-opt-in-${restored.audience.toLowerCase()}.json`,
      JSON.stringify(
        {
          timestamp: new Date().toISOString(),
          source: "actual CMS checkbox + persisted backend + real Firebase",
          audience: restored.audience,
          optOutHidden: true,
          optInVisible: true,
          reloadCheckboxRoundTrip: true,
          englishUnchanged: true,
          cardOrderUnchanged: true,
        },
        null,
        2,
      ) + "\n",
    );
  } finally {
    const current = await snapshot();
    const reset = await request.put(
      base + "/api/admin/categories/home?lang=tr",
      {
        headers: adminHeaders,
        data: {
          expectedRevision: current.revision,
          rows: original.rows.map((row: any) => ({
            categoryId: row.categoryId,
            displayOrder: row.displayOrder,
            audience: row.audience,
            contentLanguage: row.contentLanguage,
          })),
        },
      },
    );
    expect(reset.status()).toBe(200);
  }
});
