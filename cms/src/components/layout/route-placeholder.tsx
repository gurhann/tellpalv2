import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";

type RoutePlaceholderProps = {
  eyebrow: string;
  title: string;
  description: string;
  highlights: string[];
};

export function RoutePlaceholder({
  eyebrow,
  title,
  description,
  highlights,
}: RoutePlaceholderProps) {
  return (
    <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
      <Card className="border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
        <CardHeader className="gap-3">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
            {eyebrow}
          </p>
          <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            {title}
          </h1>
          <CardDescription className="max-w-2xl text-sm leading-6 sm:text-base">
            {description}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="rounded-3xl border border-dashed border-border/80 bg-muted/30 p-5">
            <p className="text-sm font-medium text-foreground">
              This page is intentionally minimal.
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              The route shell is in place and can be expanded with module
              specific data, forms, and backend actions as the workspace grows.
            </p>
          </div>
        </CardContent>
      </Card>

      <Card className="border border-border/70 bg-card/95 shadow-xl shadow-slate-950/5">
        <CardHeader>
          <h2 className="font-heading text-xl font-medium">
            Available responsibilities
          </h2>
          <CardDescription>
            This route is intended to host the following capabilities.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ul className="space-y-3 text-sm text-muted-foreground">
            {highlights.map((highlight) => (
              <li
                key={highlight}
                className="rounded-2xl border border-border/70 bg-muted/40 px-4 py-3"
              >
                {highlight}
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
