import { Outlet } from "react-router-dom"
import { useState } from "react"

import { SideNav } from "@/components/layout/side-nav"
import { TopBar } from "@/components/layout/top-bar"

export function AppShell() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="grid min-h-screen lg:grid-cols-[280px_1fr]">
        <aside className="hidden border-r border-border/70 bg-muted/30 lg:block">
          <SideNav />
        </aside>

        <div className="flex min-h-screen flex-col">
          <TopBar
            mobileNavOpen={mobileNavOpen}
            onMobileNavOpenChange={setMobileNavOpen}
          />
          <main className="flex-1 px-4 py-6 sm:px-6 lg:px-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
