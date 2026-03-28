import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail, AdminSessionPayload } from "@/types/api";

export type AuthSessionStatus =
  | "bootstrapping"
  | "authenticated"
  | "unauthenticated";

export type AuthSessionState = {
  status: AuthSessionStatus;
  isBootstrapped: boolean;
  session: AdminSessionPayload | null;
  lastProblem: ApiProblemDetail | null;
};

type RefreshRunner = () => Promise<AdminSessionPayload | null>;
type Listener = () => void;

const initialState: AuthSessionState = {
  status: "bootstrapping",
  isBootstrapped: false,
  session: null,
  lastProblem: null,
};

function toProblemDetail(error: unknown): ApiProblemDetail | null {
  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return null;
}

class AuthSessionStore {
  private state = initialState;
  private listeners = new Set<Listener>();
  private bootstrapPromise: Promise<AdminSessionPayload | null> | null = null;
  private refreshPromise: Promise<AdminSessionPayload | null> | null = null;

  subscribe = (listener: Listener) => {
    this.listeners.add(listener);

    return () => {
      this.listeners.delete(listener);
    };
  };

  getSnapshot = () => this.state;

  setSession = (session: AdminSessionPayload) => {
    this.setState({
      status: "authenticated",
      isBootstrapped: true,
      session,
      lastProblem: null,
    });
  };

  clearSession = (problem?: ApiProblemDetail | null) => {
    this.setState({
      status: "unauthenticated",
      isBootstrapped: true,
      session: null,
      lastProblem: problem ?? null,
    });
  };

  bootstrap = async (
    hasRefreshToken: () => boolean,
    refreshRunner: RefreshRunner,
  ) => {
    if (this.state.isBootstrapped) {
      return this.state.session;
    }

    if (this.bootstrapPromise) {
      return this.bootstrapPromise;
    }

    if (!hasRefreshToken()) {
      this.clearSession();
      return null;
    }

    this.setState({
      ...this.state,
      status: "bootstrapping",
      lastProblem: null,
    });

    this.bootstrapPromise = this.refresh(refreshRunner)
      .catch(() => null)
      .finally(() => {
        this.bootstrapPromise = null;
      });

    return this.bootstrapPromise;
  };

  refresh = async (refreshRunner: RefreshRunner) => {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.setState({
      ...this.state,
      status: "bootstrapping",
      lastProblem: null,
    });

    this.refreshPromise = (async () => {
      try {
        const session = await refreshRunner();

        if (!session) {
          this.clearSession();
          return null;
        }

        this.setSession(session);
        return session;
      } catch (error) {
        this.clearSession(toProblemDetail(error));
        return null;
      } finally {
        this.refreshPromise = null;
      }
    })();

    return this.refreshPromise;
  };

  reset = () => {
    this.bootstrapPromise = null;
    this.refreshPromise = null;
    this.state = initialState;
    this.emit();
  };

  private setState(nextState: AuthSessionState) {
    this.state = nextState;
    this.emit();
  }

  private emit() {
    for (const listener of this.listeners) {
      listener();
    }
  }
}

export const authSessionStore = new AuthSessionStore();
