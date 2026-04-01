import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useMemo, useState, type ReactNode } from "react";
import { RouterProvider, createMemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { cmsRoutes } from "@/app/router";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import {
  AuthContext,
  type AuthContextValue,
} from "@/features/auth/providers/auth-context";
import type { AuthSessionState } from "@/features/auth/model/session-store";
import type { AdminSessionPayload, ApiProblemDetail } from "@/types/api";

const contentHookMocks = vi.hoisted(() => ({
  useContentList: vi.fn(),
  useContentDetail: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-list", () => ({
  useContentList: contentHookMocks.useContentList,
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentHookMocks.useContentDetail,
}));

function makeSession(
  overrides: Partial<AdminSessionPayload> = {},
): AdminSessionPayload {
  return {
    adminUserId: 7,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-28T10:00:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-03-29T10:00:00Z",
    ...overrides,
  };
}

function makeContentListState() {
  return {
    contents: [storyContentViewModel],
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  };
}

function makeContentDetailState() {
  return {
    content: storyContentViewModel,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn(),
  };
}

type TestAuthProviderProps = {
  children: ReactNode;
  initialState: AuthSessionState;
  logoutImpl?: () => Promise<void>;
};

function TestAuthProvider({
  children,
  initialState,
  logoutImpl,
}: TestAuthProviderProps) {
  const [state, setState] = useState(initialState);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      isAuthenticated:
        state.status === "authenticated" && state.session !== null,
      bootstrapSession: async () => state.session,
      refreshSession: async () => state.session,
      setSession: (session: AdminSessionPayload) => {
        setState({
          status: "authenticated",
          isBootstrapped: true,
          session,
          lastProblem: null,
        });
      },
      clearSession: (problem?: ApiProblemDetail | null) => {
        setState({
          status: "unauthenticated",
          isBootstrapped: true,
          session: null,
          lastProblem: problem ?? null,
        });
      },
      logout: async () => {
        if (logoutImpl) {
          await logoutImpl();
        }

        setState({
          status: "unauthenticated",
          isBootstrapped: true,
          session: null,
          lastProblem: null,
        });
      },
    }),
    [logoutImpl, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function renderRouter(options: {
  initialEntries?: string[];
  authState: AuthSessionState;
  logoutImpl?: () => Promise<void>;
}) {
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

  const router = createMemoryRouter(cmsRoutes, {
    initialEntries: options.initialEntries ?? ["/contents"],
  });

  return {
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <TestAuthProvider
          initialState={options.authState}
          logoutImpl={options.logoutImpl}
        >
          <RouterProvider router={router} />
        </TestAuthProvider>
      </QueryClientProvider>,
    ),
  };
}

beforeEach(() => {
  contentHookMocks.useContentList.mockReset();
  contentHookMocks.useContentDetail.mockReset();
  contentHookMocks.useContentList.mockReturnValue(makeContentListState());
  contentHookMocks.useContentDetail.mockReturnValue(makeContentDetailState());
});

describe("CMS router auth flow", () => {
  it("redirects protected routes to login when the session is missing", async () => {
    renderRouter({
      initialEntries: ["/contents"],
      authState: {
        status: "unauthenticated",
        isBootstrapped: true,
        session: null,
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", { name: /sign in to tellpal cms/i }),
    ).toBeInTheDocument();
  });

  it("shows a loading screen while auth bootstrap is still in progress", async () => {
    renderRouter({
      initialEntries: ["/contents"],
      authState: {
        status: "bootstrapping",
        isBootstrapped: false,
        session: null,
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", { name: /restoring admin session/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: /sign in to tellpal cms/i }),
    ).not.toBeInTheDocument();
  });

  it("redirects authenticated users away from /login to /contents", async () => {
    renderRouter({
      initialEntries: ["/login"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", { name: /content studio/i }),
    ).toBeInTheDocument();
  });

  it("renders live content detail data for /contents/:contentId", async () => {
    renderRouter({
      initialEntries: ["/contents/42"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", { name: /evening garden/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /open story pages/i }),
    ).toHaveAttribute("href", "/contents/42/story-pages");
    expect(
      screen.getByRole("tablist", { name: /content localization tabs/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /save localization/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /publish locale/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /turkish/i })).toBeInTheDocument();
  });

  it("renders the category studio shell for /categories", async () => {
    renderRouter({
      initialEntries: ["/categories"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", {
        name: /^categories$/i,
        level: 1,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/category registry awaits bg02/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create category/i }),
    ).toBeDisabled();
  });

  it("renders the category detail shell for /categories/:categoryId", async () => {
    renderRouter({
      initialEntries: ["/categories/9"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", { name: /category #9/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /open curation/i }),
    ).toBeDisabled();
    expect(
      screen.getByText(/localization workspaces reserved/i),
    ).toBeInTheDocument();
  });

  it("logs out from the top bar and returns the user to /login", async () => {
    const logoutImpl = vi.fn().mockResolvedValue(undefined);

    renderRouter({
      initialEntries: ["/contents"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
      logoutImpl,
    });

    fireEvent.click(await screen.findByRole("button", { name: /log out/i }));

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /sign in to tellpal cms/i }),
      ).toBeInTheDocument();
    });

    expect(logoutImpl).toHaveBeenCalledTimes(1);
  });
});
