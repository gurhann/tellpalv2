import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { LoginRoute } from "@/app/routes/login";
import {
  AuthContext,
  type AuthContextValue,
} from "@/features/auth/providers/auth-context";
import { ApiClientError } from "@/lib/http/client";
import type { AdminSessionPayload, ApiProblemDetail } from "@/types/api";

const adminAuthApiMock = vi.hoisted(() => ({
  login: vi.fn(),
}));

vi.mock("@/features/auth/api/admin-auth", () => ({
  adminAuthApi: adminAuthApiMock,
}));

function makeSession(
  overrides: Partial<AdminSessionPayload> = {},
): AdminSessionPayload {
  return {
    adminUserId: 7,
    username: "bootstrap-admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-28T10:00:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-03-29T10:00:00Z",
    ...overrides,
  };
}

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Authentication failed",
    status: 401,
    detail: "Invalid admin credentials",
    ...overrides,
  };
}

function makeApiClientError(problem: ApiProblemDetail) {
  return new ApiClientError(
    problem,
    new Response(JSON.stringify(problem), {
      status: problem.status,
      statusText: problem.title,
      headers: {
        "Content-Type": "application/json",
      },
    }),
  );
}

function createAuthValue(overrides: Partial<AuthContextValue> = {}) {
  return {
    status: "unauthenticated",
    isBootstrapped: true,
    session: null,
    lastProblem: null,
    isAuthenticated: false,
    bootstrapSession: vi.fn(),
    refreshSession: vi.fn(),
    setSession: vi.fn(),
    clearSession: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  } satisfies AuthContextValue;
}

function renderLoginRoute(options?: {
  authValue?: AuthContextValue;
  initialEntries?: Array<string | { pathname: string; state?: unknown }>;
}) {
  const authValue = options?.authValue ?? createAuthValue();
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  const router = createMemoryRouter(
    [
      {
        path: "/login",
        element: <LoginRoute />,
      },
      {
        path: "/contents",
        element: <h2>Contents page</h2>,
      },
      {
        path: "/contributors",
        element: <h2>Contributors page</h2>,
      },
    ],
    {
      initialEntries: options?.initialEntries ?? ["/login"],
    },
  );

  const renderResult = render(
    <AuthContext.Provider value={authValue}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AuthContext.Provider>,
  );

  return {
    ...renderResult,
    authValue,
    router,
  };
}

beforeEach(() => {
  window.localStorage.clear();
  adminAuthApiMock.login.mockReset();
});

describe("LoginForm", () => {
  it("validates required username and password fields on the client", async () => {
    renderLoginRoute();

    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(
      await screen.findByText("Username is required."),
    ).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();
    expect(adminAuthApiMock.login).not.toHaveBeenCalled();
  });

  it("shows a credential-specific message for 401 responses", async () => {
    adminAuthApiMock.login.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          status: 401,
          title: "Authentication failed",
          detail: "Invalid admin credentials",
        }),
      ),
    );

    renderLoginRoute();

    fireEvent.change(screen.getByLabelText(/username/i), {
      target: { value: "bootstrap-admin" },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: "wrong-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(
      await screen.findByText("Incorrect credentials"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Username or password is incorrect. Check the credentials and try again.",
      ),
    ).toBeInTheDocument();
  });

  it("shows a disabled-account message for 403 responses", async () => {
    adminAuthApiMock.login.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          status: 403,
          title: "Admin user disabled",
          detail: "Admin user 7 is disabled",
          errorCode: "admin_disabled",
        }),
      ),
    );

    renderLoginRoute();

    fireEvent.change(screen.getByLabelText(/username/i), {
      target: { value: "bootstrap-admin" },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: "TellPalCms!2026" },
    });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByText("Account disabled")).toBeInTheDocument();
    expect(
      screen.getByText(
        "This admin account is disabled. Contact an operator to restore access.",
      ),
    ).toBeInTheDocument();
  });

  it("stores the session and lands on /contents after a successful login", async () => {
    const session = makeSession();
    const authValue = createAuthValue();
    adminAuthApiMock.login.mockResolvedValue(session);

    renderLoginRoute({ authValue });

    fireEvent.change(screen.getByLabelText(/username/i), {
      target: { value: session.username },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: "TellPalCms!2026" },
    });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await screen.findByRole("heading", { name: /contents page/i });

    expect(authValue.setSession).toHaveBeenCalledWith(session);
    expect(window.localStorage.getItem("tellpal.cms.scaffold-session")).toBe(
      "active",
    );
  });

  it("returns to the requested route after a successful login", async () => {
    const session = makeSession();
    adminAuthApiMock.login.mockResolvedValue(session);

    renderLoginRoute({
      initialEntries: [
        {
          pathname: "/login",
          state: {
            from: "/contributors",
          },
        },
      ],
    });

    fireEvent.change(screen.getByLabelText(/username/i), {
      target: { value: session.username },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: "TellPalCms!2026" },
    });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /contributors page/i }),
      ).toBeInTheDocument();
    });
  });
});
