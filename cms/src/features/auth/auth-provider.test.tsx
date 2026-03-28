import {
  render,
  screen,
  waitFor,
  fireEvent,
  cleanup,
} from "@testing-library/react";
import { StrictMode, useEffect } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { tokenStorage } from "@/features/auth/lib/token-storage";
import { authSessionStore } from "@/features/auth/model/session-store";
import { useAuth } from "@/features/auth/providers/use-auth";

const adminAuthApiMock = vi.hoisted(() => ({
  refresh: vi.fn(),
  logout: vi.fn(),
}));

vi.mock("@/features/auth/api/admin-auth", () => ({
  adminAuthApi: adminAuthApiMock,
}));

function makeSession(
  overrides: Partial<import("@/types/api").AdminSessionPayload> = {},
) {
  return {
    adminUserId: 1,
    username: "cms-admin",
    roleCodes: ["EDITOR"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-28T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-03-29T10:00:00Z",
    ...overrides,
  };
}

beforeEach(() => {
  window.localStorage.clear();
  authSessionStore.reset();
  adminAuthApiMock.refresh.mockReset();
  adminAuthApiMock.logout.mockReset();
});

afterEach(() => {
  cleanup();
});

describe("AuthProvider", () => {
  it("bootstraps the session only once under strict mode", async () => {
    const session = makeSession();
    tokenStorage.setRefreshToken("bootstrap-refresh-token");
    adminAuthApiMock.refresh.mockResolvedValue(session);

    const { AuthProvider } =
      await import("@/features/auth/providers/auth-provider");

    function Probe() {
      const auth = useAuth();

      return <span>{auth.status}</span>;
    }

    render(
      <StrictMode>
        <AuthProvider>
          <Probe />
        </AuthProvider>
      </StrictMode>,
    );

    expect(await screen.findByText("authenticated")).toBeInTheDocument();
    expect(adminAuthApiMock.refresh).toHaveBeenCalledTimes(1);
    expect(tokenStorage.getRefreshToken()).toBe("refresh-token-next");
  });

  it("clears all token state on logout", async () => {
    const session = makeSession();
    tokenStorage.setRefreshToken(session.refreshToken);
    adminAuthApiMock.refresh.mockResolvedValue(session);
    adminAuthApiMock.logout.mockResolvedValue(undefined);

    const { AuthProvider } =
      await import("@/features/auth/providers/auth-provider");

    function LogoutHarness() {
      const auth = useAuth();
      const { logout, setSession, status } = auth;

      useEffect(() => {
        setSession(session);
      }, [setSession]);

      return (
        <>
          <span>{status}</span>
          <button type="button" onClick={() => void logout()}>
            Logout
          </button>
        </>
      );
    }

    render(
      <AuthProvider>
        <LogoutHarness />
      </AuthProvider>,
    );

    expect(await screen.findByText("authenticated")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /logout/i }));

    await waitFor(() => {
      expect(screen.getByText("unauthenticated")).toBeInTheDocument();
    });

    expect(adminAuthApiMock.logout).toHaveBeenCalledWith({
      refreshToken: session.refreshToken,
    });
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(authSessionStore.getSnapshot().session).toBeNull();
  });

  it("waits for a single refresh result across parallel refresh calls", async () => {
    const deferred = Promise.withResolvers<ReturnType<typeof makeSession>>();
    tokenStorage.setRefreshToken("parallel-refresh-token");
    adminAuthApiMock.refresh.mockReturnValue(deferred.promise);

    const { AuthProvider } =
      await import("@/features/auth/providers/auth-provider");

    function RefreshHarness() {
      const auth = useAuth();

      return (
        <>
          <span>{auth.status}</span>
          <button
            type="button"
            onClick={() => {
              void Promise.all([auth.refreshSession(), auth.refreshSession()]);
            }}
          >
            Refresh
          </button>
        </>
      );
    }

    render(
      <AuthProvider>
        <RefreshHarness />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("bootstrapping")).toBeInTheDocument();
    });

    deferred.resolve(
      makeSession({
        accessToken: "fresh-access-token",
        refreshToken: "fresh-refresh-token",
      }),
    );

    await screen.findByText("authenticated");

    adminAuthApiMock.refresh.mockClear();

    fireEvent.click(screen.getByRole("button", { name: /refresh/i }));

    await waitFor(() => {
      expect(adminAuthApiMock.refresh).toHaveBeenCalledTimes(1);
    });

    expect(tokenStorage.getRefreshToken()).toBe("fresh-refresh-token");
  });
});
