import type { ApiProblemDetail, ApiProblemFieldErrors } from "@/types/api";

type ResponseLike = Pick<Response, "status" | "statusText" | "url">;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readString(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }

  const normalized = value.trim();
  return normalized.length > 0 ? normalized : undefined;
}

function readNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value)
    ? value
    : undefined;
}

function readFieldErrors(value: unknown): ApiProblemFieldErrors | undefined {
  if (!isRecord(value)) {
    return undefined;
  }

  const entries = Object.entries(value).filter(
    (entry): entry is [string, string] => typeof entry[1] === "string",
  );

  if (entries.length === 0) {
    return undefined;
  }

  return Object.fromEntries(entries);
}

function readPathFromUrl(url: string): string | undefined {
  try {
    return new URL(url).pathname;
  } catch {
    return undefined;
  }
}

export function toApiProblemDetail(
  input: unknown,
  response: ResponseLike,
): ApiProblemDetail {
  const body = isRecord(input) ? input : {};

  const problem: ApiProblemDetail = {
    type: readString(body.type) ?? "about:blank",
    title: (readString(body.title) ?? response.statusText) || "Request failed",
    status: readNumber(body.status) ?? response.status,
    detail:
      readString(body.detail) ??
      `Request failed with status ${response.status}.`,
  };

  const instance = readString(body.instance);
  if (instance) {
    problem.instance = instance;
  }

  const errorCode = readString(body.errorCode);
  if (errorCode) {
    problem.errorCode = errorCode;
  }

  const requestId = readString(body.requestId);
  if (requestId) {
    problem.requestId = requestId;
  }

  const path = readString(body.path) ?? readPathFromUrl(response.url);
  if (path) {
    problem.path = path;
  }

  const adminUserId = readNumber(body.adminUserId);
  if (adminUserId !== undefined) {
    problem.adminUserId = adminUserId;
  }

  const fieldErrors = readFieldErrors(body.fieldErrors);
  if (fieldErrors) {
    problem.fieldErrors = fieldErrors;
  }

  for (const [key, value] of Object.entries(body)) {
    if (!(key in problem)) {
      problem[key] = value;
    }
  }

  return problem;
}

export function getProblemMessage(
  problem: ApiProblemDetail | null | undefined,
) {
  return problem?.detail || problem?.title || "Something went wrong.";
}

export function getProblemFieldErrors(
  problem: ApiProblemDetail | null | undefined,
) {
  return problem?.fieldErrors ?? {};
}
