import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { AuthLoadingScreen } from "@/features/auth/components/auth-loading-screen";
import { useAuth } from "@/features/auth/providers/use-auth";
import { useI18n } from "@/i18n/locale-provider";

export function NotFoundRoute() {
  const auth = useAuth();
  const { t } = useI18n();

  if (!auth.isBootstrapped && !auth.session) {
    return <AuthLoadingScreen />;
  }

  const target = auth.session ? "/contents" : "/login";

  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-3xl items-center justify-center">
        <Card className="w-full border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              {t("layout.brand")}
            </p>
            <CardTitle className="text-3xl">{t("notFound.title")}</CardTitle>
            <CardDescription>{t("notFound.description")}</CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to={target}>{t("app.goBack")}</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
