import {
  useCallback,
  useEffect,
  useSyncExternalStore,
  type ReactNode,
} from "react";

import { adminAuthApi } from "@/features/auth/api/admin-auth";
import { tokenStorage } from "@/features/auth/lib/token-storage";
import { authSessionStore } from "@/features/auth/model/session-store";
import { AuthContext } from "@/features/auth/providers/auth-context";
import { configureApiClientAuth, resetApiClientAuth } from "@/lib/http/client";
import type { ApiProblemDetail, AdminSessionPayload } from "@/types/api";

function useAuthController() {
  const state = useSyncExternalStore(
    authSessionStore.subscribe,
    authSessionStore.getSnapshot,
    authSessionStore.getSnapshot,
  );

  const refreshSession = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();

    if (!refreshToken) {
      authSessionStore.clearSession();
      return null;
    }

    return authSessionStore.refresh(async () => {
      const session = await adminAuthApi.refresh({ refreshToken });
      tokenStorage.setRefreshToken(session.refreshToken);
      return session;
    });
  }, []);

  const bootstrapSession = useCallback(async () => {
    return authSessionStore.bootstrap(
      () => tokenStorage.getRefreshToken() !== null,
      refreshSession,
    );
  }, [refreshSession]);

  const setSession = useCallback((session: AdminSessionPayload) => {
    tokenStorage.setRefreshToken(session.refreshToken);
    authSessionStore.setSession(session);
  }, []);

  const clearSession = useCallback((problem?: ApiProblemDetail | null) => {
    tokenStorage.clearRefreshToken();
    authSessionStore.clearSession(problem);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();

    try {
      if (refreshToken) {
        await adminAuthApi.logout({ refreshToken });
      }
    } finally {
      clearSession();
    }
  }, [clearSession]);

  useEffect(() => {
    configureApiClientAuth({
      getAccessToken: () => authSessionStore.getSnapshot().session?.accessToken,
      refreshAccessToken: async () => {
        const session = await refreshSession();
        return session?.accessToken ?? null;
      },
      onUnauthorized: (problem) => {
        clearSession(problem);
      },
    });

    return () => {
      resetApiClientAuth();
    };
  }, [clearSession, refreshSession]);

  useEffect(() => {
    void bootstrapSession();
  }, [bootstrapSession]);

  return {
    ...state,
    isAuthenticated: state.status === "authenticated" && state.session !== null,
    bootstrapSession,
    refreshSession,
    setSession,
    clearSession,
    logout,
  };
}

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const value = useAuthController();

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
