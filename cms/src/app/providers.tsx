import { useEffect, type ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";

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
    <ThemeProvider
      attribute="class"
      defaultTheme={appEnv.VITE_DEFAULT_THEME}
      disableTransitionOnChange
      enableSystem
    >
      <QueryClientProvider client={queryClient}>
        {children}
        <Toaster closeButton position="top-right" richColors />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
