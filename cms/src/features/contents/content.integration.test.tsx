import { QueryClient } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type {
  AdminContentReadResponse,
  AdminContentResponse,
  UpsertContentLocalizationInput,
} from "@/features/contents/api/content-admin";
import {
  contentReadResponses,
  meditationContentReadResponse,
  storyContentReadResponse,
} from "@/features/contents/test/fixtures";
import type { AdminSessionPayload } from "@/types/api";

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function makeSession(
  overrides: Partial<AdminSessionPayload> = {},
): AdminSessionPayload {
  return {
    adminUserId: 1,
    username: "admin",
    roleCodes: ["ADMIN"],
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-03-29T10:00:00Z",
    refreshToken: "refresh-token-next",
    refreshTokenExpiresAt: "2026-04-28T10:00:00Z",
    ...overrides,
  };
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

function problemResponse(
  status: number,
  title: string,
  detail: string,
  errorCode: string,
  path: string,
  extras: Record<string, unknown> = {},
) {
  return new Response(
    JSON.stringify({
      type: "about:blank",
      title,
      status,
      detail,
      errorCode,
      path,
      ...extras,
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

async function readRequestJson(init?: RequestInit) {
  if (!init?.body || typeof init.body !== "string") {
    return null;
  }

  return JSON.parse(init.body) as Record<string, unknown>;
}

async function renderFreshApp(options: {
  initialPath: string;
  fetchImplementation: typeof fetch;
}) {
  vi.resetModules();
  window.history.replaceState({}, "", options.initialPath);
  vi.stubGlobal("fetch", options.fetchImplementation);

  const { AppProviders } = await import("@/app/providers");
  const { default: App } = await import("@/App");
  const { authSessionStore } =
    await import("@/features/auth/model/session-store");
  const { queryClient } = await import("@/lib/query-client");

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

describe("Content integration", () => {
  it("creates content from the live registry flow and navigates into the detail route", async () => {
    const session = makeSession();
    const listRecords = cloneJson(contentReadResponses);
    let createdRecord: AdminContentReadResponse | null = null;
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents" && method === "GET") {
          return jsonResponse(
            createdRecord ? [createdRecord, ...listRecords] : listRecords,
          );
        }

        if (url.pathname === "/api/admin/contents" && method === "POST") {
          const body = (await readRequestJson(init)) as {
            type: string;
            externalKey: string;
            ageRange: number | null;
            active: boolean;
          };

          const createdContent: AdminContentResponse = {
            contentId: 99,
            type: body.type as AdminContentResponse["type"],
            externalKey: body.externalKey,
            ageRange: body.ageRange,
            active: body.active,
            pageCount: body.type === "STORY" ? 0 : null,
          };
          createdRecord = {
            ...createdContent,
            localizations: [],
          };

          return jsonResponse(createdContent);
        }

        if (url.pathname === "/api/admin/contents/99" && method === "GET") {
          return jsonResponse(createdRecord);
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /content studio/i });

    fireEvent.click(screen.getByRole("button", { name: /^create content$/i }));

    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/external key/i), {
      target: { value: "story.integration-spark" },
    });
    fireEvent.change(within(dialog).getByLabelText(/age range/i), {
      target: { value: "4" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: /^create content$/i }),
    );

    expect(
      await screen.findByRole("heading", { name: /content #99/i }),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe("/contents/99");
    expect(screen.getByDisplayValue("story.integration-spark")).toBeVisible();
  });

  it("maps backend validation errors onto content create fields in the real app flow", async () => {
    const session = makeSession();
    const listRecords = cloneJson(contentReadResponses);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents" && method === "GET") {
          return jsonResponse(listRecords);
        }

        if (url.pathname === "/api/admin/contents" && method === "POST") {
          return problemResponse(
            400,
            "Validation failed",
            "Request validation failed.",
            "validation_error",
            "/api/admin/contents",
            {
              fieldErrors: {
                externalKey: "External key already violates local policy.",
              },
            },
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /content studio/i });

    fireEvent.click(screen.getByRole("button", { name: /^create content$/i }));

    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/external key/i), {
      target: { value: "story.integration-spark" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: /^create content$/i }),
    );

    expect(
      await within(dialog).findByText(
        "External key already violates local policy.",
      ),
    ).toBeVisible();
  });

  it("redirects back to login when a protected content request returns 401", async () => {
    const session = makeSession();
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents" && method === "GET") {
          return problemResponse(
            401,
            "Authentication failed",
            "Access token is expired.",
            "auth_failed",
            "/api/admin/contents",
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

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

  it("shows access denied feedback when metadata save returns 403", async () => {
    const session = makeSession();
    const detailRecord = cloneJson(storyContentReadResponse);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents/1" && method === "GET") {
          return jsonResponse(detailRecord);
        }

        if (url.pathname === "/api/admin/contents/1" && method === "PUT") {
          return problemResponse(
            403,
            "Access denied",
            "You do not have permission to update content metadata.",
            "access_denied",
            "/api/admin/contents/1",
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/1",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /evening garden/i });

    fireEvent.change(screen.getByLabelText(/external key/i), {
      target: { value: "story.evening-garden.secured" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save metadata/i }));

    expect(
      await screen.findAllByText(
        "You do not have permission to update content metadata.",
      ),
    ).not.toHaveLength(0);
  });

  it("updates a localization and publishes it through the live detail route", async () => {
    const session = makeSession();
    const detailRecord = cloneJson(meditationContentReadResponse);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents/2" && method === "GET") {
          return jsonResponse(detailRecord);
        }

        if (
          url.pathname === "/api/admin/contents/2/localizations/de" &&
          method === "PUT"
        ) {
          const body = (await readRequestJson(
            init,
          )) as UpsertContentLocalizationInput;
          const nextLocalization = {
            ...detailRecord.localizations[0],
            title: body.title,
            description: body.description ?? null,
            bodyText: body.bodyText ?? null,
            coverMediaId: body.coverMediaId ?? null,
            audioMediaId: body.audioMediaId ?? null,
            durationMinutes: body.durationMinutes ?? null,
            status: body.status,
            processingStatus: body.processingStatus,
            publishedAt: body.publishedAt ?? null,
          };
          detailRecord.localizations[0] = nextLocalization;

          return jsonResponse(nextLocalization);
        }

        if (
          url.pathname === "/api/admin/contents/2/localizations/de/publish" &&
          method === "POST"
        ) {
          detailRecord.localizations[0] = {
            ...detailRecord.localizations[0],
            status: "PUBLISHED",
            processingStatus: "COMPLETED",
            publishedAt: "2026-03-29T09:00:00Z",
            visibleToMobile: true,
          };

          return jsonResponse(detailRecord.localizations[0]);
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/2",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /regenraum pause/i });

    fireEvent.change(screen.getByLabelText(/^title$/i), {
      target: { value: "Regenraum Fokus" },
    });
    fireEvent.click(screen.getByRole("button", { name: /save localization/i }));

    await waitFor(() => {
      expect(screen.getByDisplayValue("Regenraum Fokus")).toBeVisible();
    });

    fireEvent.click(screen.getByRole("button", { name: /publish locale/i }));

    expect(await screen.findAllByText("Published")).not.toHaveLength(0);
    expect(screen.getByText("Visible")).toBeVisible();
  });

  it("surfaces publish conflicts inline when the backend rejects a story publish", async () => {
    const session = makeSession();
    const detailRecord = cloneJson(storyContentReadResponse);
    detailRecord.localizations[0] = {
      ...detailRecord.localizations[0],
      status: "DRAFT",
      publishedAt: null,
      visibleToMobile: false,
    };
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents/1" && method === "GET") {
          return jsonResponse(detailRecord);
        }

        if (
          url.pathname === "/api/admin/contents/1/localizations/en/publish" &&
          method === "POST"
        ) {
          return problemResponse(
            409,
            "Story pages incomplete",
            "Story pages are incomplete for English publish.",
            "content_state_conflict",
            "/api/admin/contents/1/localizations/en/publish",
          );
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/1",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /evening garden/i });

    fireEvent.click(screen.getByRole("button", { name: /publish locale/i }));

    expect(
      await screen.findAllByText(
        "Story pages are incomplete for English publish.",
      ),
    ).not.toHaveLength(0);
  });
});
