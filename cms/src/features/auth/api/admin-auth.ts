import { z } from "zod";

import { apiClient } from "@/lib/http/client";
import type { AdminSessionPayload } from "@/types/api";

const basePath = "/api/admin/auth";

export type AdminLoginInput = {
  username: string;
  password: string;
};

export type AdminRefreshInput = {
  refreshToken: string;
};

export type AdminLogoutInput = {
  refreshToken: string;
};

export const adminSessionPayloadSchema: z.ZodType<AdminSessionPayload> =
  z.object({
    adminUserId: z.number().int().positive(),
    username: z.string().min(1),
    roleCodes: z.array(z.string()),
    accessToken: z.string().min(1),
    accessTokenExpiresAt: z.string().min(1),
    refreshToken: z.string().min(1),
    refreshTokenExpiresAt: z.string().min(1),
  });

export const adminAuthApi = {
  login(input: AdminLoginInput) {
    return apiClient.post<AdminSessionPayload>(`${basePath}/login`, {
      auth: "none",
      body: input,
      responseSchema: adminSessionPayloadSchema,
    });
  },
  refresh(input: AdminRefreshInput) {
    return apiClient.post<AdminSessionPayload>(`${basePath}/refresh`, {
      auth: "none",
      body: input,
      responseSchema: adminSessionPayloadSchema,
    });
  },
  logout(input: AdminLogoutInput) {
    return apiClient.post<void>(`${basePath}/logout`, {
      auth: "none",
      body: input,
    });
  },
};
