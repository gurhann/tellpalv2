import { Menu } from "lucide-react"
import { useMemo } from "react"
import { useLocation, useNavigate } from "react-router-dom"

import { getRouteMeta } from "@/app/navigation"
import { clearScaffoldSession } from "@/app/scaffold-session"
import { SideNav } from "@/components/layout/side-nav"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"

type TopBarProps = {
  mobileNavOpen: boolean
  onMobileNavOpenChange: (open: boolean) => void
}

export function TopBar({
  mobileNavOpen,
  onMobileNavOpenChange,
}: TopBarProps) {
  const location = useLocation()
  const navigate = useNavigate()

  const routeMeta = useMemo(
    () => getRouteMeta(location.pathname),
    [location.pathname]
  )

  function handleSignOut() {
    clearScaffoldSession()
    navigate("/login", { replace: true })
  }

  return (
    <>
      <header className="border-b border-border/70 bg-background/90 px-4 py-4 backdrop-blur sm:px-6 lg:px-8">
        <div className="flex items-center gap-3">
          <Button
            className="lg:hidden"
            size="icon-sm"
            type="button"
            variant="outline"
            onClick={() => onMobileNavOpenChange(true)}
          >
            <Menu className="size-4" />
            <span className="sr-only">Open navigation</span>
          </Button>

          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              {routeMeta.eyebrow}
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-2">
              <h2 className="text-xl font-semibold tracking-tight">
                {routeMeta.title}
              </h2>
              <p className="text-sm text-muted-foreground">
                {routeMeta.description}
              </p>
            </div>
          </div>

          <Button type="button" variant="outline" onClick={handleSignOut}>
            Exit scaffold
          </Button>
        </div>
      </header>

      <Sheet open={mobileNavOpen} onOpenChange={onMobileNavOpenChange}>
        <SheetContent side="left" className="w-[320px] p-0">
          <SheetHeader className="sr-only">
            <SheetTitle>CMS Navigation</SheetTitle>
          </SheetHeader>
          <SideNav onNavigate={() => onMobileNavOpenChange(false)} />
        </SheetContent>
      </Sheet>
    </>
  )
}
