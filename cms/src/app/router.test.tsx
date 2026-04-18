import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useMemo, useState, type ReactNode } from "react";
import { RouterProvider, createMemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { cmsRoutes } from "@/app/router";
import {
  archivedCategoryViewModel,
  bedtimeMeditationCategoryViewModel,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";
import {
  inactiveContentViewModel,
  meditationContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";
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

const categoryHookMocks = vi.hoisted(() => ({
  useCategoryList: vi.fn(),
  useCategoryDetail: vi.fn(),
  useCategoryLocalizations: vi.fn(),
}));

vi.mock("@/features/contents/queries/use-content-list", () => ({
  useContentList: contentHookMocks.useContentList,
}));

vi.mock("@/features/contents/queries/use-content-detail", () => ({
  useContentDetail: contentHookMocks.useContentDetail,
}));

vi.mock("@/features/categories/queries/use-category-list", () => ({
  useCategoryList: categoryHookMocks.useCategoryList,
}));

vi.mock("@/features/categories/queries/use-category-detail", () => ({
  useCategoryDetail: categoryHookMocks.useCategoryDetail,
}));

vi.mock("@/features/categories/queries/use-category-localizations", () => ({
  useCategoryLocalizations: categoryHookMocks.useCategoryLocalizations,
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
    contents: [
      storyContentViewModel,
      meditationContentViewModel,
      inactiveContentViewModel,
    ],
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

function makeCategoryListState() {
  return {
    categories: [
      featuredSleepCategoryViewModel,
      bedtimeMeditationCategoryViewModel,
      archivedCategoryViewModel,
    ],
    isLoading: false,
    isFetching: false,
    problem: null,
    refetch: vi.fn(),
  };
}

function makeCategoryDetailState() {
  return {
    category: archivedCategoryViewModel,
    isLoading: false,
    problem: null,
    isNotFound: false,
    refetch: vi.fn(),
  };
}

function makeCategoryLocalizationState() {
  return {
    localizations: [],
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
  categoryHookMocks.useCategoryList.mockReset();
  categoryHookMocks.useCategoryDetail.mockReset();
  categoryHookMocks.useCategoryLocalizations.mockReset();
  contentHookMocks.useContentList.mockReturnValue(makeContentListState());
  contentHookMocks.useContentDetail.mockReturnValue(makeContentDetailState());
  categoryHookMocks.useCategoryList.mockReturnValue(makeCategoryListState());
  categoryHookMocks.useCategoryDetail.mockReturnValue(
    makeCategoryDetailState(),
  );
  categoryHookMocks.useCategoryLocalizations.mockReturnValue(
    makeCategoryLocalizationState(),
  );
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
    expect(screen.getByText("Content type")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /^meditation$/i }),
    ).toBeInTheDocument();
    expect(screen.queryByText(/editorial notes/i)).not.toBeInTheDocument();
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
      screen.getAllByRole("link", { name: /open story pages/i })[0],
    ).toHaveAttribute("href", "/contents/42/story-pages?language=en");
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
    expect(screen.queryByText(/locale notes/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/contributor notes/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/content profile/i)).not.toBeInTheDocument();
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
    expect(screen.getByText("quiet-nights")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create category/i }),
    ).toBeEnabled();
    expect(screen.getByText("Category type")).toBeInTheDocument();
    expect(screen.getAllByText("Access").length).toBeGreaterThan(0);
    expect(screen.queryByText(/curation notes/i)).not.toBeInTheDocument();
  });

  it("filters content registry rows by type and state", async () => {
    renderRouter({
      initialEntries: ["/contents"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    await screen.findByRole("heading", { name: /content studio/i });

    fireEvent.click(screen.getByRole("button", { name: /^meditation$/i }));

    expect(screen.getByText("Regenraum Pause")).toBeInTheDocument();
    expect(screen.queryByText("Evening Garden")).not.toBeInTheDocument();
    expect(
      screen.getByText(/Meditation \| All states \| 1 \/ 3 records/i),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^inactive$/i }));

    expect(screen.queryByText("Rain Room Reset")).not.toBeInTheDocument();
    expect(
      screen.getByText(/Meditation \| Inactive \| 0 \/ 3 records/i),
    ).toBeInTheDocument();
  });

  it("filters category registry rows by type, access, and state", async () => {
    renderRouter({
      initialEntries: ["/categories"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    await screen.findByRole("heading", {
      name: /^categories$/i,
      level: 1,
    });

    fireEvent.click(screen.getByRole("button", { name: /^meditation$/i }));
    fireEvent.click(screen.getByRole("button", { name: /^premium$/i }));

    expect(screen.getByText("bedtime-meditations")).toBeInTheDocument();
    expect(screen.queryByText("featured-sleep")).not.toBeInTheDocument();
    expect(
      screen.getByText(
        /Meditation \| Premium \| All states \| 1 \/ 3 records/i,
      ),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^inactive$/i }));

    expect(screen.queryByText("bedtime-meditations")).not.toBeInTheDocument();
    expect(
      screen.getByText(/Meditation \| Premium \| Inactive \| 0 \/ 3 records/i),
    ).toBeInTheDocument();
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
      await screen.findByRole("heading", { name: /quiet-nights/i }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("quiet-nights").length).toBeGreaterThan(0);
    expect(
      screen.getByRole("button", { name: /open curation/i }),
    ).toBeDisabled();
    expect(screen.getByText(/no localizations yet/i)).toBeInTheDocument();
    expect(screen.queryByText(/snapshot notes/i)).not.toBeInTheDocument();
  });

  it("renders hidden authenticated UI lab routes without adding them to navigation", async () => {
    renderRouter({
      initialEntries: ["/labs/ui/contents"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", {
        name: /content studio prototypes/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/three coded directions for the content registry/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /ui labs/i }),
    ).not.toBeInTheDocument();
  });

  it("renders hidden authenticated Variant A mockup routes and supports deep links", async () => {
    renderRouter({
      initialEntries: ["/labs/mockups/contents/demo-content/story-pages?language=tr"],
      authState: {
        status: "authenticated",
        isBootstrapped: true,
        session: makeSession(),
        lastProblem: null,
      },
    });

    expect(
      await screen.findByRole("heading", {
        name: /story page editor mockup/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getAllByText(/preferred locale/i).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getByRole("button", { name: /add story page/i }),
    ).toBeEnabled();
    expect(
      screen.queryByRole("link", { name: /variant a mockups/i }),
    ).not.toBeInTheDocument();
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
