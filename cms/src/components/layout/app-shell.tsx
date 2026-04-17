import { Outlet } from "react-router-dom";
import { useState } from "react";

import { SideNav } from "@/components/layout/side-nav";
import { TopBar } from "@/components/layout/top-bar";

export function AppShell() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,_rgba(249,247,242,0.92),_rgba(255,255,255,1)_22rem)] text-foreground">
      <div className="grid min-h-screen lg:grid-cols-[296px_1fr]">
        <aside className="hidden border-r border-border/70 bg-[linear-gradient(180deg,_rgba(246,243,236,0.96),_rgba(255,255,255,0.98))] lg:block">
          <SideNav />
        </aside>

        <div className="flex min-h-screen min-w-0 flex-col">
          <TopBar
            mobileNavOpen={mobileNavOpen}
            onMobileNavOpenChange={setMobileNavOpen}
          />
          <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
