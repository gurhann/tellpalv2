import { LogOut, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/providers/use-auth";
import { useI18n } from "@/i18n/locale-provider";

export function LogoutButton() {
  const auth = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);
  const { t } = useI18n();

  async function handleLogout() {
    if (isPending) {
      return;
    }

    setIsPending(true);

    try {
      await auth.logout();
    } catch {
      // Session state is still cleared locally inside the auth provider.
    } finally {
      queryClient.clear();
      navigate("/login", { replace: true });
    }
  }

  return (
    <Button
      aria-busy={isPending}
      disabled={isPending}
      type="button"
      variant="outline"
      onClick={() => void handleLogout()}
    >
      {isPending ? (
        <LoaderCircle className="size-4 animate-spin" />
      ) : (
        <LogOut className="size-4" />
      )}
      <span>{isPending ? t("auth.logout.pending") : t("auth.logout")}</span>
    </Button>
  );
}
