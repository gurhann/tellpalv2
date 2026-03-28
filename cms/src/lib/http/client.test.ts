import { afterEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";

import {
  ApiClientError,
  ApiResponseParseError,
  createApiClient,
} from "@/lib/http/client";

function jsonResponse(
  body: unknown,
  init: ResponseInit & { url?: string } = {},
) {
  const { url = "http://api.test/resource", ...responseInit } = init;
  const response = new Response(JSON.stringify(body), {
    ...responseInit,
    headers: {
      "Content-Type": "application/json",
      ...(responseInit.headers ?? {}),
    },
  });

  Object.defineProperty(response, "url", {
    configurable: true,
    value: url,
  });

  return response;
}

function createDeferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;

  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });

  return { promise, resolve, reject };
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("createApiClient", () => {
  it("sends authorized requests through the shared request entry point", async () => {
    const fetchSpy = vi.fn(async () =>
      jsonResponse({ ok: true }, { status: 200 }),
    );

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
      getAccessToken: () => "access-token",
    });

    const response = await client.get("/api/admin/contents", {
      responseSchema: z.object({ ok: z.literal(true) }),
    });

    expect(response).toEqual({ ok: true });
    expect(fetchSpy).toHaveBeenCalledTimes(1);

    const [requestUrl, requestInit] = fetchSpy.mock.calls[0];
    expect(requestUrl).toBe("http://api.test/api/admin/contents");

    const headers = new Headers(requestInit?.headers);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(headers.get("Accept")).toBe("application/json");
  });

  it("supports unauthenticated requests through the same client", async () => {
    const fetchSpy = vi.fn(async () =>
      jsonResponse({ username: "cms-admin" }, { status: 200 }),
    );

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
      getAccessToken: () => "ignored-token",
    });

    const response = await client.post("/api/admin/auth/login", {
      auth: "none",
      body: {
        username: "cms-admin",
        password: "secret",
      },
      responseSchema: z.object({ username: z.string() }),
    });

    expect(response).toEqual({ username: "cms-admin" });

    const [, requestInit] = fetchSpy.mock.calls[0];
    const headers = new Headers(requestInit?.headers);

    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("Content-Type")).toBe("application/json");
  });

  it("retries concurrent 401 responses with a single refresh flow", async () => {
    const accessTokenStore = { current: "expired-token" };
    const refresh = createDeferred<string>();

    const refreshAccessToken = vi.fn(async () => {
      const nextToken = await refresh.promise;
      accessTokenStore.current = nextToken;
      return nextToken;
    });

    const fetchSpy = vi.fn(async (_input: unknown, init?: RequestInit) => {
      const authorization = new Headers(init?.headers).get("Authorization");

      if (authorization === "Bearer expired-token") {
        return jsonResponse(
          {
            title: "Authentication failed",
            detail: "Access token expired",
            errorCode: "auth_failed",
          },
          { status: 401, statusText: "Unauthorized" },
        );
      }

      if (authorization === "Bearer fresh-token") {
        return jsonResponse({ ok: true }, { status: 200 });
      }

      throw new Error(`Unexpected authorization header: ${authorization}`);
    });

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
      getAccessToken: () => accessTokenStore.current,
      refreshAccessToken,
    });

    const requestSchema = z.object({ ok: z.literal(true) });
    const firstRequest = client.get("/api/admin/contents", {
      responseSchema: requestSchema,
    });
    const secondRequest = client.get("/api/admin/contents", {
      responseSchema: requestSchema,
    });

    await vi.waitFor(() => {
      expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    });

    refresh.resolve("fresh-token");

    await expect(Promise.all([firstRequest, secondRequest])).resolves.toEqual([
      { ok: true },
      { ok: true },
    ]);

    expect(fetchSpy).toHaveBeenCalledTimes(4);
    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
  });

  it("maps problem details into a typed API error", async () => {
    const fetchSpy = vi.fn(async () =>
      jsonResponse(
        {
          title: "Validation failed",
          detail: "Request validation failed",
          status: 400,
          errorCode: "validation_error",
          fieldErrors: {
            username: "username is required",
          },
          requestId: "req-123",
          path: "/api/admin/auth/login",
        },
        { status: 400, statusText: "Bad Request" },
      ),
    );

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
    });

    await expect(
      client.post("/api/admin/auth/login", {
        auth: "none",
        body: {
          username: "",
          password: "",
        },
      }),
    ).rejects.toEqual(
      expect.objectContaining({
        name: "ApiClientError",
        status: 400,
        problem: expect.objectContaining({
          errorCode: "validation_error",
          requestId: "req-123",
          path: "/api/admin/auth/login",
          fieldErrors: {
            username: "username is required",
          },
        }),
      }),
    );
  });

  it("fails fast when the success payload does not match the expected schema", async () => {
    const fetchSpy = vi.fn(async () =>
      jsonResponse({ contentId: "not-a-number" }, { status: 200 }),
    );

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
    });

    await expect(
      client.get("/api/admin/contents/42", {
        responseSchema: z.object({ contentId: z.number() }),
      }),
    ).rejects.toBeInstanceOf(ApiResponseParseError);
  });

  it("notifies the auth boundary when refresh cannot recover a 401", async () => {
    const onUnauthorized = vi.fn();

    const fetchSpy = vi.fn(async () =>
      jsonResponse(
        {
          title: "Authentication failed",
          detail: "Session has expired",
          errorCode: "auth_failed",
        },
        { status: 401, statusText: "Unauthorized" },
      ),
    );

    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchSpy as unknown as typeof fetch,
      getAccessToken: () => "expired-token",
      refreshAccessToken: async () => null,
      onUnauthorized,
    });

    await expect(client.get("/api/admin/contents")).rejects.toBeInstanceOf(
      ApiClientError,
    );

    expect(onUnauthorized).toHaveBeenCalledWith(
      expect.objectContaining({
        errorCode: "auth_failed",
        detail: "Session has expired",
      }),
    );
  });
});
