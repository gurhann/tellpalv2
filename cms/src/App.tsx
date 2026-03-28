import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"

function App() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto flex min-h-screen max-w-6xl items-center px-6 py-16">
        <div className="grid w-full gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <Card className="border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
            <CardHeader className="gap-3">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
                TellPal CMS
              </p>
              <CardTitle className="text-4xl font-semibold tracking-tight sm:text-5xl">
                Tailwind and shadcn foundation is ready
              </CardTitle>
              <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
                This workspace now has Tailwind v4, the shadcn registry, CSS
                variables, import aliases, and the first UI primitives needed
                for the admin shell.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold">Installed now</h2>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li>Tailwind v4 via Vite plugin</li>
                  <li>shadcn components.json and utils</li>
                  <li>Button, Input, Card, Dialog, Table</li>
                  <li>Tabs, Sheet, Select, Sonner</li>
                </ul>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/50 p-4">
                <h2 className="mb-2 text-sm font-semibold">Next task focus</h2>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li>Route skeleton and app shell</li>
                  <li>Query client and env wiring</li>
                  <li>Protected navigation</li>
                  <li>Shared admin layout</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-2xl shadow-slate-950/5 backdrop-blur">
            <CardHeader>
              <CardTitle className="text-xl">Smoke input</CardTitle>
              <CardDescription>
                A minimal shadcn form surface to verify the design system is
                wired into the app.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Input value="cms@tellpal.app" readOnly aria-label="Workspace email" />
              <div className="flex flex-wrap gap-3">
                <Button type="button">Primary action</Button>
                <Button type="button" variant="outline">
                  Secondary action
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  )
}

export default App
