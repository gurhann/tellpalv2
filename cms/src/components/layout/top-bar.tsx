import { Menu } from "lucide-react";
import { useMemo } from "react";
import { useLocation } from "react-router-dom";

import { getRouteMeta } from "@/app/navigation";
import { LocaleSwitcher } from "@/components/i18n/locale-switcher";
import { SideNav } from "@/components/layout/side-nav";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { LogoutButton } from "@/features/auth/components/logout-button";
import { useAuth } from "@/features/auth/providers/use-auth";
import { useI18n } from "@/i18n/locale-provider";

type TopBarProps = {
  mobileNavOpen: boolean;
  onMobileNavOpenChange: (open: boolean) => void;
};

export function TopBar({ mobileNavOpen, onMobileNavOpenChange }: TopBarProps) {
  const auth = useAuth();
  const location = useLocation();
  const { t } = useI18n();

  const routeMeta = useMemo(
    () => getRouteMeta(location.pathname),
    [location.pathname],
  );

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-border/70 bg-background/90 px-4 py-4 backdrop-blur sm:px-6 lg:px-8">
        <div className="flex items-center gap-3">
          <Button
            className="lg:hidden"
            size="icon-sm"
            type="button"
            variant="outline"
            onClick={() => onMobileNavOpenChange(true)}
          >
            <Menu className="size-4" />
            <span className="sr-only">{t("app.openNavigation")}</span>
          </Button>

          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              {t(routeMeta.eyebrowKey)}
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-2">
              <h2 className="text-xl font-semibold tracking-tight">
                {t(routeMeta.titleKey)}
              </h2>
              <p className="text-sm text-muted-foreground">
                {t(routeMeta.descriptionKey)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <LocaleSwitcher compact />
            {auth.session ? (
              <div className="hidden text-right sm:block">
                <p className="text-sm font-medium text-foreground">
                  {auth.session.username}
                </p>
                <p className="text-xs text-muted-foreground">
                  {auth.session.roleCodes.join(", ")}
                </p>
              </div>
            ) : null}

            <LogoutButton />
          </div>
        </div>
      </header>

      <Sheet open={mobileNavOpen} onOpenChange={onMobileNavOpenChange}>
        <SheetContent side="left" className="w-[320px] p-0">
          <SheetHeader className="sr-only">
            <SheetTitle>{t("app.cmsNavigation")}</SheetTitle>
          </SheetHeader>
          <SheetBody className="p-0">
            <SideNav onNavigate={() => onMobileNavOpenChange(false)} />
          </SheetBody>
        </SheetContent>
      </Sheet>
    </>
  );
}
