import { QueryClient } from "@tanstack/react-query";

type MaybeHttpError = {
  status?: number;
  response?: {
    status?: number;
  };
};

function getErrorStatus(error: unknown) {
  if (!error || typeof error !== "object") {
    return undefined;
  }

  const maybeHttpError = error as MaybeHttpError;

  return maybeHttpError.status ?? maybeHttpError.response?.status;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      retry(failureCount, error) {
        const status = getErrorStatus(error);

        if (status && [400, 401, 403, 404, 409, 422].includes(status)) {
          return false;
        }

        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});
