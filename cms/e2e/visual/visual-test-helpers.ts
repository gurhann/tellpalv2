import type { Page } from "@playwright/test";

type SessionPayload = {
  adminUserId: number;
  username: string;
  roleCodes: string[];
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

export const visualViewports = [
  { name: "mobile", width: 390, height: 844 },
  { name: "tablet", width: 768, height: 1024 },
  { name: "laptop", width: 1280, height: 900 },
  { name: "desktop", width: 1440, height: 1024 },
] as const;

export function makeVisualSession(): SessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-04-18T12:00:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-05-18T12:00:00Z",
  };
}

export async function stabilizeVisualPage(page: Page) {
  await page.addInitScript(({ fixedTime }) => {
    const RealDate = Date;

    class FixedDate extends RealDate {
      constructor(...args: ConstructorParameters<typeof Date>) {
        if (args.length === 0) {
          super(fixedTime);
          return;
        }

        super(...args);
      }

      static now() {
        return fixedTime;
      }
    }

    // @ts-expect-error test harness override
    window.Date = FixedDate;
    window.localStorage.setItem("tellpal.cms.refresh-token", "visual-refresh");
  }, { fixedTime: new Date("2026-04-18T10:00:00Z").valueOf() });

  await page.emulateMedia({ reducedMotion: "reduce", colorScheme: "light" });
}

export async function installVisualStyles(page: Page) {
  await page.addStyleTag({
    content: `
      *,
      *::before,
      *::after {
        animation: none !important;
        transition: none !important;
        caret-color: transparent !important;
      }
    `,
  });
}

export async function mockVisualAuth(page: Page) {
  const session = makeVisualSession();

  await page.route("**/api/admin/auth/refresh", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(session),
    });
  });
}
