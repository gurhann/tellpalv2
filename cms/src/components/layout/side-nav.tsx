import { NavLink } from "react-router-dom";

import {
  cmsNavigationItems,
  localMockupNavigationItems,
} from "@/app/navigation";
import { useI18n } from "@/i18n/locale-provider";
import { cn } from "@/lib/utils";

type SideNavProps = {
  onNavigate?: () => void;
};

export function SideNav({ onNavigate }: SideNavProps) {
  const { t } = useI18n();

  const renderNavigationItem = (item: (typeof cmsNavigationItems)[number]) => {
    const Icon = item.icon;

    return (
      <NavLink
        key={item.path}
        to={item.path}
        onClick={onNavigate}
        className={({ isActive }) =>
          cn(
            "flex items-start gap-3 rounded-2xl px-3 py-3 transition-colors",
            isActive
              ? "bg-primary text-primary-foreground shadow-lg shadow-primary/10"
              : "hover:bg-muted",
          )
        }
      >
        {({ isActive }) => (
          <>
            <div
              className={cn(
                "mt-0.5 inline-flex size-9 shrink-0 items-center justify-center rounded-xl border",
                isActive
                  ? "border-primary-foreground/20 bg-primary-foreground/10"
                  : "border-border/70 bg-background",
              )}
            >
              <Icon className="size-4" />
            </div>
            <div className="min-w-0">
              <div className="font-medium">{t(item.labelKey)}</div>
              <div
                className={cn(
                  "mt-1 text-xs leading-5",
                  isActive
                    ? "text-primary-foreground/80"
                    : "text-muted-foreground",
                )}
              >
                {t(item.descriptionKey)}
              </div>
            </div>
          </>
        )}
      </NavLink>
    );
  };

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border/70 px-5 py-6">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          {t("layout.brand")}
        </p>
        <h1 className="mt-2 text-lg font-semibold">
          {t("layout.workspaceTitle")}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("layout.workspaceDescription")}
        </p>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-4">
        {cmsNavigationItems.map(renderNavigationItem)}

        {localMockupNavigationItems.length > 0 ? (
          <div className="mt-6 border-t border-border/70 pt-4">
            <p className="px-3 pb-2 text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              {t("nav.localTools")}
            </p>
            <div className="space-y-1">
              {localMockupNavigationItems.map(renderNavigationItem)}
            </div>
          </div>
        ) : null}
      </nav>
    </div>
  );
}
