import { QueryClient } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminSessionPayload } from "@/types/api";

type Deferred<T> = {
  promise: Promise<T>;
  resolve: (value: T) => void;
  reject: (reason?: unknown) => void;
};

function createDeferred<T>(): Deferred<T> {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;

  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });

  return {
    promise,
    resolve,
    reject,
  };
}

function makeSession(
  overrides: Partial<AdminSessionPayload> = {},
): AdminSessionPayload {
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

function problemResponse(
  status: number,
  title: string,
  detail: string,
  errorCode: string,
) {
  return new Response(
    JSON.stringify({
      type: "about:blank",
      title,
      status,
      detail,
      errorCode,
      path: "/api/admin/auth/refresh",
    }),
    {
      status,
      statusText: title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    },
  );
}

async function renderFreshApp(options: {
  initialPath: string;
  fetchImplementation: typeof fetch;
}) {
  vi.resetModules();
  window.history.replaceState({}, "", options.initialPath);
  vi.stubGlobal("fetch", options.fetchImplementation);

  const [
    { AppProviders },
    { default: App },
    { authSessionStore },
    { queryClient },
  ] = await Promise.all([
    import("@/app/providers"),
    import("@/App"),
    import("@/features/auth/model/session-store"),
    import("@/lib/query-client"),
  ]);

  authSessionStore.reset();
  queryClient.clear();

  let renderResult!: ReturnType<typeof render>;
  await act(async () => {
    renderResult = render(
      <AppProviders>
        <App />
      </AppProviders>,
    );
  });

  return {
    ...renderResult,
    authSessionStore,
    queryClient: queryClient as QueryClient,
  };
}

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(async () => {
  const modules = await Promise.allSettled([
    import("@/features/auth/model/session-store"),
    import("@/lib/query-client"),
  ]);

  for (const result of modules) {
    if (result.status === "fulfilled") {
      if ("authSessionStore" in result.value) {
        result.value.authSessionStore.reset();
      }

      if ("queryClient" in result.value) {
        result.value.queryClient.clear();
      }
    }
  }

  window.localStorage.clear();
  vi.unstubAllGlobals();
});

describe("Auth integration", () => {
  it(
    "logs in from the real app flow and lands on /contents",
    async () => {
      const session = makeSession();
      const fetchMock = vi
        .fn<typeof fetch>()
        .mockImplementation(async (input, init) => {
          const url = new URL(String(input));

          expect(url.pathname).toBe("/api/admin/auth/login");
          expect(init?.method).toBe("POST");

          return new Response(JSON.stringify(session), {
            status: 200,
            headers: {
              "Content-Type": "application/json",
            },
          });
        });

      await renderFreshApp({
        initialPath: "/login",
        fetchImplementation: fetchMock as typeof fetch,
      });

      fireEvent.change(screen.getByLabelText(/username/i), {
        target: { value: session.username },
      });
      fireEvent.change(screen.getByLabelText(/password/i), {
        target: { value: "TellPalCms!2026" },
      });
      fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

      expect(
        await screen.findByRole("heading", { name: /content studio/i }),
      ).toBeInTheDocument();
      expect(window.localStorage.getItem("tellpal.cms.refresh-token")).toBe(
        session.refreshToken,
      );
      expect(
        fetchMock.mock.calls.filter(([input, init]) => {
          const url = new URL(typeof input === "string" ? input : input.url);

          return (
            url.pathname === "/api/admin/auth/login" && init?.method === "POST"
          );
        }),
      ).toHaveLength(1);
    },
    15_000,
  );

  it("bootstraps the session from a stored refresh token without flickering to login", async () => {
    const deferred = createDeferred<Response>();
    const session = makeSession({
      accessToken: "bootstrapped-access-token",
      refreshToken: "bootstrapped-refresh-token",
    });
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input) => {
        const url = new URL(String(input));

        if (url.pathname === "/api/admin/auth/refresh") {
          return deferred.promise;
        }

        throw new Error(`Unexpected request to ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents",
      fetchImplementation: fetchMock as typeof fetch,
    });

    expect(
      await screen.findByRole("heading", { name: /restoring admin session/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: /sign in to tellpal cms/i }),
    ).not.toBeInTheDocument();

    await act(async () => {
      deferred.resolve(
        new Response(JSON.stringify(session), {
          status: 200,
          headers: {
            "Content-Type": "application/json",
          },
        }),
      );
    });

    expect(
      await screen.findByRole("heading", { name: /content studio/i }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem("tellpal.cms.refresh-token")).toBe(
      session.refreshToken,
    );
  });

  it("logs out from the protected shell and clears the stored refresh token", async () => {
    const bootstrappedSession = makeSession({
      accessToken: "restored-access-token",
      refreshToken: "restored-refresh-token",
    });
    const logoutRequests: string[] = [];
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(String(input));

        if (url.pathname === "/api/admin/auth/refresh") {
          return new Response(JSON.stringify(bootstrappedSession), {
            status: 200,
            headers: {
              "Content-Type": "application/json",
            },
          });
        }

        if (url.pathname === "/api/admin/auth/logout") {
          logoutRequests.push(String(init?.body ?? ""));
          return new Response(null, {
            status: 204,
          });
        }

        throw new Error(`Unexpected request to ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /content studio/i });

    fireEvent.click(screen.getByRole("button", { name: /log out/i }));

    expect(
      await screen.findByRole("heading", { name: /sign in to tellpal cms/i }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem("tellpal.cms.refresh-token")).toBeNull();
    expect(logoutRequests).toHaveLength(1);
    expect(logoutRequests[0]).toContain("restored-refresh-token");
  });

  it("redirects to login and clears the stale refresh token when bootstrap refresh is expired", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input) => {
        const url = new URL(String(input));

        if (url.pathname === "/api/admin/auth/refresh") {
          return problemResponse(
            401,
            "Authentication failed",
            "Refresh token is expired",
            "auth_failed",
          );
        }

        throw new Error(`Unexpected request to ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "expired-refresh");

    await renderFreshApp({
      initialPath: "/contents",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /sign in to tellpal cms/i }),
      ).toBeInTheDocument();
    });

    expect(window.localStorage.getItem("tellpal.cms.refresh-token")).toBeNull();
  });
});
