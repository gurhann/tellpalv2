import type { ZodType } from "zod";

import { appEnv } from "@/lib/env";
import type { ApiProblemDetail, JsonValue } from "@/types/api";

import { parseJsonWithSchema, readJsonBody } from "./json";
import { getProblemMessage, toApiProblemDetail } from "./problem-details";

export type ApiRequestAuthMode = "required" | "optional" | "none";

export type ApiRequestOptions<TResponse> = {
  path: string;
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: JsonValue;
  headers?: HeadersInit;
  auth?: ApiRequestAuthMode;
  responseSchema?: ZodType<TResponse>;
  signal?: AbortSignal;
};

export type ApiClientConfig = {
  baseUrl: string;
  fetch?: typeof fetch;
  getAccessToken?: () => string | null | undefined;
  refreshAccessToken?: () => Promise<string | null | undefined>;
  onUnauthorized?: (problem: ApiProblemDetail) => void;
};

export type ApiClientAuthConfig = Pick<
  ApiClientConfig,
  "getAccessToken" | "refreshAccessToken" | "onUnauthorized"
>;

export class ApiClientError extends Error {
  readonly status: number;
  readonly problem: ApiProblemDetail;
  readonly response: Response;

  constructor(problem: ApiProblemDetail, response: Response) {
    super(getProblemMessage(problem));
    this.name = "ApiClientError";
    this.status = problem.status;
    this.problem = problem;
    this.response = response;
  }
}

export class ApiResponseParseError extends Error {
  readonly status: number;
  readonly url: string;

  constructor(message: string, response: Pick<Response, "status" | "url">) {
    super(message);
    this.name = "ApiResponseParseError";
    this.status = response.status;
    this.url = response.url;
  }
}

type NormalizedRequestOptions<TResponse> = Omit<
  ApiRequestOptions<TResponse>,
  "method"
> & {
  method: NonNullable<ApiRequestOptions<TResponse>["method"]>;
};

function createAuthBridge(): Required<ApiClientAuthConfig> {
  return {
    getAccessToken: () => null,
    refreshAccessToken: async () => null,
    onUnauthorized: () => {},
  };
}

export function createApiClient(config: ApiClientConfig) {
  const fetchImplementation = config.fetch ?? fetch;
  let refreshInFlight: Promise<string | null> | null = null;

  async function request<TResponse>(
    requestOptions: ApiRequestOptions<TResponse>,
  ): Promise<TResponse> {
    const normalizedOptions: NormalizedRequestOptions<TResponse> = {
      auth: "required",
      ...requestOptions,
      method: requestOptions.method ?? "GET",
    };

    return executeRequest(normalizedOptions);
  }

  async function executeRequest<TResponse>(
    requestOptions: NormalizedRequestOptions<TResponse>,
    overrideAccessToken?: string | null,
    hasRetriedAfterRefresh = false,
  ): Promise<TResponse> {
    const url = new URL(requestOptions.path, config.baseUrl);
    const headers = new Headers(requestOptions.headers);
    headers.set("Accept", "application/json");

    const accessToken =
      overrideAccessToken ?? config.getAccessToken?.() ?? null;

    if (requestOptions.auth !== "none" && accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }

    let body: string | undefined;
    if (requestOptions.body !== undefined) {
      headers.set("Content-Type", "application/json");
      body = JSON.stringify(requestOptions.body);
    }

    const response = await fetchImplementation(url.toString(), {
      method: requestOptions.method,
      headers,
      body,
      signal: requestOptions.signal,
    });

    if (
      response.status === 401 &&
      requestOptions.auth !== "none" &&
      !hasRetriedAfterRefresh &&
      config.refreshAccessToken
    ) {
      const refreshedAccessToken = await refreshAccessTokenOnce();

      if (refreshedAccessToken) {
        return executeRequest(requestOptions, refreshedAccessToken, true);
      }
    }

    let responseBody: unknown | null = null;

    try {
      responseBody = await readJsonBody(response);
    } catch {
      throw new ApiResponseParseError(
        `Response from ${url.pathname} did not contain valid JSON.`,
        response,
      );
    }

    if (!response.ok) {
      const problem = toApiProblemDetail(responseBody, response);

      if (response.status === 401) {
        config.onUnauthorized?.(problem);
      }

      throw new ApiClientError(problem, response);
    }

    if (responseBody === null) {
      return undefined as TResponse;
    }

    if (!requestOptions.responseSchema) {
      return responseBody as TResponse;
    }

    try {
      return parseJsonWithSchema(
        responseBody,
        requestOptions.responseSchema,
        `Response from ${url.pathname}`,
      );
    } catch {
      throw new ApiResponseParseError(
        `Response from ${url.pathname} did not match the expected schema.`,
        response,
      );
    }
  }

  async function refreshAccessTokenOnce() {
    if (!config.refreshAccessToken) {
      return null;
    }

    if (!refreshInFlight) {
      refreshInFlight = Promise.resolve(config.refreshAccessToken())
        .then((accessToken) => accessToken ?? null)
        .catch(() => null)
        .finally(() => {
          refreshInFlight = null;
        });
    }

    return refreshInFlight;
  }

  return {
    request,
    get<TResponse>(
      path: string,
      options?: Omit<ApiRequestOptions<TResponse>, "path" | "method">,
    ) {
      return request<TResponse>({ ...options, path, method: "GET" });
    },
    post<TResponse>(
      path: string,
      options?: Omit<ApiRequestOptions<TResponse>, "path" | "method">,
    ) {
      return request<TResponse>({ ...options, path, method: "POST" });
    },
    put<TResponse>(
      path: string,
      options?: Omit<ApiRequestOptions<TResponse>, "path" | "method">,
    ) {
      return request<TResponse>({ ...options, path, method: "PUT" });
    },
    patch<TResponse>(
      path: string,
      options?: Omit<ApiRequestOptions<TResponse>, "path" | "method">,
    ) {
      return request<TResponse>({ ...options, path, method: "PATCH" });
    },
    delete<TResponse>(
      path: string,
      options?: Omit<ApiRequestOptions<TResponse>, "path" | "method">,
    ) {
      return request<TResponse>({ ...options, path, method: "DELETE" });
    },
  };
}

const apiClientAuthBridge = createAuthBridge();

export function configureApiClientAuth(config: Partial<ApiClientAuthConfig>) {
  Object.assign(apiClientAuthBridge, config);
}

export function resetApiClientAuth() {
  Object.assign(apiClientAuthBridge, createAuthBridge());
}

export const apiClient = createApiClient({
  baseUrl: appEnv.VITE_API_BASE_URL,
  getAccessToken: () => apiClientAuthBridge.getAccessToken(),
  refreshAccessToken: () => apiClientAuthBridge.refreshAccessToken(),
  onUnauthorized: (problem) => apiClientAuthBridge.onUnauthorized(problem),
});
