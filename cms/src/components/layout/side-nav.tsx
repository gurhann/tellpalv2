import { NavLink } from "react-router-dom";

import { cmsNavigationItems } from "@/app/navigation";
import { cn } from "@/lib/utils";

type SideNavProps = {
  onNavigate?: () => void;
};

export function SideNav({ onNavigate }: SideNavProps) {
  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border/70 px-5 py-6">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          TellPal CMS
        </p>
        <h1 className="mt-2 text-lg font-semibold">Editorial Workspace</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Route skeleton for the CMS shell, navigation, and protected layout.
        </p>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-4">
        {cmsNavigationItems.map((item) => {
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
                    <div className="font-medium">{item.label}</div>
                    <div
                      className={cn(
                        "mt-1 text-xs leading-5",
                        isActive
                          ? "text-primary-foreground/80"
                          : "text-muted-foreground",
                      )}
                    >
                      {item.description}
                    </div>
                  </div>
                </>
              )}
            </NavLink>
          );
        })}
      </nav>
    </div>
  );
}
