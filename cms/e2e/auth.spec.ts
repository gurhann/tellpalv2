import { expect, test } from "@playwright/test";

type SessionPayload = {
  adminUserId: number;
  username: string;
  roleCodes: string[];
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

function makeSession(overrides: Partial<SessionPayload> = {}): SessionPayload {
  return {
    adminUserId: 1,
    username: "bootstrap-admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-29T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-04-28T10:00:00Z",
    ...overrides,
  };
}

test("login, bootstrap refresh, and logout work in the browser", async ({
  page,
}) => {
  const loginSession = makeSession({
    accessToken: "access-token-login",
    refreshToken: "refresh-token-login",
  });
  const bootstrappedSession = makeSession({
    accessToken: "access-token-refresh",
    refreshToken: "refresh-token-refresh",
  });
  let refreshCalls = 0;

  await page.route("**/api/admin/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(loginSession),
    });
  });

  await page.route("**/api/admin/auth/refresh", async (route) => {
    refreshCalls += 1;
    await new Promise((resolve) => setTimeout(resolve, 120));

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(bootstrappedSession),
    });
  });

  await page.route("**/api/admin/auth/logout", async (route) => {
    await route.fulfill({
      status: 204,
      body: "",
    });
  });

  await page.goto("/login");

  await page.getByLabel(/username/i).fill("bootstrap-admin");
  await page.getByLabel(/password/i).fill("TellPalCms!2026");
  await page.getByRole("button", { name: /^sign in$/i }).click();

  await expect(
    page.getByRole("heading", { name: /content studio/i }),
  ).toBeVisible();

  await page.reload();

  await expect(
    page.getByRole("heading", { name: /restoring admin session/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /content studio/i }),
  ).toBeVisible();

  await page.getByRole("button", { name: /log out/i }).click();

  await expect(
    page.getByRole("heading", { name: /sign in to tellpal cms/i }),
  ).toBeVisible();
  await expect.poll(() => refreshCalls).toBeGreaterThanOrEqual(1);
});

test("expired refresh tokens redirect the user back to login", async ({
  page,
}) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("tellpal.cms.refresh-token", "expired-refresh");
  });

  await page.route("**/api/admin/auth/refresh", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/problem+json",
      body: JSON.stringify({
        type: "about:blank",
        title: "Authentication failed",
        status: 401,
        detail: "Refresh token is expired",
        errorCode: "auth_failed",
        path: "/api/admin/auth/refresh",
      }),
    });
  });

  await page.goto("/contents");

  await expect(
    page.getByRole("heading", { name: /sign in to tellpal cms/i }),
  ).toBeVisible();
  await expect
    .poll(() =>
      page.evaluate(() =>
        window.localStorage.getItem("tellpal.cms.refresh-token"),
      ),
    )
    .toBeNull();
});
