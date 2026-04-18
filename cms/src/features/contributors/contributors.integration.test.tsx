import { QueryClient } from "@tanstack/react-query";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type {
  AdminContentContributorResponse,
  AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  contentContributorResponses,
  contributorResponses,
} from "@/features/contributors/test/fixtures";
import { storyContentReadResponse } from "@/features/contents/test/fixtures";
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
  HTMLElement.prototype.scrollIntoView = vi.fn();
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

describe("Contributor integration", () => {
  it(
    "creates, renames, and deletes contributors from the live registry route",
    async () => {
      const session = makeSession();
      const contributors = cloneJson(contributorResponses);
      const fetchMock = vi
        .fn<typeof fetch>()
        .mockImplementation(async (input, init) => {
          const url = new URL(typeof input === "string" ? input : input.url);
          const method = init?.method ?? "GET";

          if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
            return jsonResponse(session);
          }

          if (url.pathname === "/api/admin/contributors" && method === "GET") {
            return jsonResponse(contributors);
          }

          if (url.pathname === "/api/admin/contributors" && method === "POST") {
            const body = (await readRequestJson(init)) as {
              displayName: string;
            };
            const createdContributor: AdminContributorResponse = {
              contributorId: 99,
              displayName: body.displayName,
            };
            contributors.unshift(createdContributor);
            return jsonResponse(createdContributor, 201);
          }

          if (
            url.pathname.startsWith("/api/admin/contributors/") &&
            method === "PUT"
          ) {
            const contributorId = Number(url.pathname.split("/").at(-1));
            const body = (await readRequestJson(init)) as {
              displayName: string;
            };
            const contributor = contributors.find(
              (candidate) => candidate.contributorId === contributorId,
            );

            if (!contributor) {
              throw new Error(`Missing contributor ${contributorId}`);
            }

            contributor.displayName = body.displayName;
            return jsonResponse(contributor);
          }

          if (
            url.pathname.startsWith("/api/admin/contributors/") &&
            method === "DELETE"
          ) {
            const contributorId = Number(url.pathname.split("/").at(-1));
            const contributorIndex = contributors.findIndex(
              (candidate) => candidate.contributorId === contributorId,
            );

            contributors.splice(contributorIndex, 1);
            return new Response(null, { status: 204 });
          }

          throw new Error(`Unexpected request to ${method} ${url.pathname}`);
        });

      window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

      await renderFreshApp({
        initialPath: "/contributors",
        fetchImplementation: fetchMock as typeof fetch,
      });

      await screen.findByRole("heading", { name: /^contributors$/i, level: 1 });

      fireEvent.click(
        screen.getByRole("button", { name: /^create contributor$/i }),
      );

      const createDialog = await screen.findByRole("dialog");
      fireEvent.change(within(createDialog).getByLabelText(/display name/i), {
        target: { value: " Lina Hart " },
      });
      fireEvent.click(
        within(createDialog).getByRole("button", {
          name: /^create contributor$/i,
        }),
      );

      expect(await screen.findByText("Lina Hart")).toBeVisible();

      const contributorRow = screen.getByText("Annie Case").closest("tr");
      expect(contributorRow).not.toBeNull();

      fireEvent.click(
        within(contributorRow as HTMLTableRowElement).getByRole("button", {
          name: /rename/i,
        }),
      );

      const renameDialog = await screen.findByRole("dialog");
      fireEvent.change(within(renameDialog).getByLabelText(/display name/i), {
        target: { value: " Annie Case Updated " },
      });
      fireEvent.click(
        within(renameDialog).getByRole("button", { name: /^save rename$/i }),
      );

      expect(await screen.findByText("Annie Case Updated")).toBeVisible();

      const deleteRow = screen.getByText("Milo Rivers").closest("tr");
      expect(deleteRow).not.toBeNull();

      fireEvent.click(
        within(deleteRow as HTMLTableRowElement).getByRole("button", {
          name: /^delete milo rivers$/i,
        }),
      );

      const deleteDialog = await screen.findByRole("dialog");
      fireEvent.click(
        within(deleteDialog).getByRole("button", {
          name: /^delete contributor$/i,
        }),
      );

      expect(await screen.findByText("Annie Case Updated")).toBeVisible();
      expect(screen.queryByText("Milo Rivers")).not.toBeInTheDocument();
    },
    15_000,
  );

  it("loads persisted assignments, assigns a contributor, and unassigns a contributor from content detail", async () => {
    const session = makeSession();
    const content = cloneJson(storyContentReadResponse);
    const contributors = cloneJson(contributorResponses);
    const assignments = cloneJson(contentContributorResponses);
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input, init) => {
        const url = new URL(typeof input === "string" ? input : input.url);
        const method = init?.method ?? "GET";

        if (url.pathname === "/api/admin/auth/refresh" && method === "POST") {
          return jsonResponse(session);
        }

        if (url.pathname === "/api/admin/contents/1" && method === "GET") {
          return jsonResponse(content);
        }

        if (url.pathname === "/api/admin/contributors" && method === "GET") {
          return jsonResponse(contributors);
        }

        if (
          url.pathname === "/api/admin/contents/1/contributors" &&
          method === "GET"
        ) {
          return jsonResponse(assignments);
        }

        if (
          url.pathname === "/api/admin/contents/1/contributors" &&
          method === "POST"
        ) {
          const body = (await readRequestJson(init)) as {
            contributorId: number;
            role: AdminContentContributorResponse["role"];
            languageCode?: string | null;
            creditName?: string | null;
            sortOrder: number;
          };
          const contributor = contributors.find(
            (candidate) => candidate.contributorId === body.contributorId,
          );

          if (!contributor) {
            throw new Error(`Missing contributor ${body.contributorId}`);
          }

          const assignment: AdminContentContributorResponse = {
            contentId: 1,
            contributorId: contributor.contributorId,
            contributorDisplayName: contributor.displayName,
            role: body.role,
            languageCode: body.languageCode ?? null,
            creditName: body.creditName ?? null,
            sortOrder: body.sortOrder,
          };
          assignments.push(assignment);

          return jsonResponse(assignment, 201);
        }

        if (
          url.pathname === "/api/admin/contents/1/contributors" &&
          method === "DELETE"
        ) {
          const contributorId = Number(
            url.searchParams.get("contributorId") ?? "0",
          );
          const role = url.searchParams.get("role");
          const languageCode = url.searchParams.get("languageCode");
          const assignmentIndex = assignments.findIndex(
            (candidate) =>
              candidate.contributorId === contributorId &&
              candidate.role === role &&
              candidate.languageCode === (languageCode ?? null),
          );

          assignments.splice(assignmentIndex, 1);
          return new Response(null, { status: 204 });
        }

        throw new Error(`Unexpected request to ${method} ${url.pathname}`);
      });

    window.localStorage.setItem("tellpal.cms.refresh-token", "seed-refresh");

    await renderFreshApp({
      initialPath: "/contents/1",
      fetchImplementation: fetchMock as typeof fetch,
    });

    await screen.findByRole("heading", { name: /evening garden/i });
    expect(await screen.findByText("M. Rivers")).toBeVisible();

    fireEvent.click(
      screen.getByRole("button", { name: /^assign contributor$/i }),
    );

    const dialog = await screen.findByRole("dialog");
    const contributorSelect = await within(dialog).findByRole("combobox", {
      name: /^contributor$/i,
    });
    fireEvent.click(contributorSelect);
    fireEvent.click(
      within(screen.getByRole("listbox")).getByText("Sena Yildiz"),
    );
    fireEvent.click(
      within(dialog).getByRole("button", { name: /^assign contributor$/i }),
    );

    expect((await screen.findAllByText("Sena Yildiz")).length).toBeGreaterThan(
      0,
    );

    const unassignButtons = screen.getAllByRole("button", {
      name: /^unassign m\. rivers$/i,
    });
    fireEvent.click(unassignButtons[unassignButtons.length - 1]!);

    const unassignDialog = await screen.findByRole("dialog");
    fireEvent.click(
      within(unassignDialog).getByRole("button", {
        name: /^unassign contributor$/i,
      }),
    );

    expect((await screen.findAllByText("Sena Yildiz")).length).toBeGreaterThan(
      0,
    );
    expect(
      assignments.some((assignment) => assignment.creditName === "M. Rivers"),
    ).toBe(false);
  });
});
