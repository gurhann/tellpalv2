export type JsonPrimitive = string | number | boolean | null;

export type JsonValue = JsonPrimitive | JsonObject | JsonArray;

export type JsonObject = {
  [key: string]: JsonValue;
};

export type JsonArray = JsonValue[];

export type ApiProblemFieldErrors = Record<string, string>;

export type ApiProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  errorCode?: string;
  requestId?: string;
  path?: string;
  adminUserId?: number;
  fieldErrors?: ApiProblemFieldErrors;
  [key: string]: unknown;
};

export type AdminSessionPayload = {
  adminUserId: number;
  username: string;
  roleCodes: string[];
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};
