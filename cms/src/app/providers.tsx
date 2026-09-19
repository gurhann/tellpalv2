import { useEffect, type ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";

import { AuthProvider } from "@/features/auth/providers/auth-provider";
import { LocaleProvider } from "@/i18n/locale-provider";
import { Toaster } from "@/components/ui/sonner";
import { appEnv } from "@/lib/env";
import { queryClient } from "@/lib/query-client";

type AppProvidersProps = {
  children: ReactNode;
};

export function AppProviders({ children }: AppProvidersProps) {
  useEffect(() => {
    document.title = appEnv.VITE_APP_TITLE;
  }, []);

  return (
    <LocaleProvider>
      <AuthProvider>
        <QueryClientProvider client={queryClient}>
          {children}
          <Toaster closeButton position="top-right" richColors />
        </QueryClientProvider>
      </AuthProvider>
    </LocaleProvider>
  );
}
