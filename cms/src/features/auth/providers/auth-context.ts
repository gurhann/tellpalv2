import { createContext } from "react";

import type { AuthSessionState } from "@/features/auth/model/session-store";
import type { ApiProblemDetail, AdminSessionPayload } from "@/types/api";

export type AuthContextValue = AuthSessionState & {
  isAuthenticated: boolean;
  bootstrapSession: () => Promise<AdminSessionPayload | null>;
  refreshSession: () => Promise<AdminSessionPayload | null>;
  setSession: (session: AdminSessionPayload) => void;
  clearSession: (problem?: ApiProblemDetail | null) => void;
  logout: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);
