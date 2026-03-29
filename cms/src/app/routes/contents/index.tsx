import { CirclePlus, RefreshCw, Search, Sparkles } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";

type ContentShellRow = {
  id: number;
  typeLabel: string;
  externalKey: string;
  title: string;
  activeLabel: string;
  localeSummary: string;
  pageSummary: string;
};

const previewRows: ContentShellRow[] = [
  {
    id: 42,
    typeLabel: "Story",
    externalKey: "story.evening-garden",
    title: "Evening Garden",
    activeLabel: "Active",
    localeSummary: "3 locales ready",
    pageSummary: "12 story pages",
  },
  {
    id: 57,
    typeLabel: "Meditation",
    externalKey: "meditation.rain-room",
    title: "Rain Room Reset",
    activeLabel: "Draft",
    localeSummary: "2 locales in progress",
    pageSummary: "No story pages",
  },
  {
    id: 63,
    typeLabel: "Lullaby",
    externalKey: "lullaby.moon-softly",
    title: "Moon Softly",
    activeLabel: "Archived",
    localeSummary: "1 locale archived",
    pageSummary: "No story pages",
  },
];

const columns: DataTableColumn<ContentShellRow>[] = [
  {
    id: "content",
    header: "Content",
    cell: (item) => (
      <div className="space-y-1">
        <p className="font-medium text-foreground">{item.title}</p>
        <p className="text-xs text-muted-foreground">{item.externalKey}</p>
      </div>
    ),
  },
  {
    id: "format",
    header: "Format",
    cell: (item) => (
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
        {item.typeLabel}
      </span>
    ),
  },
  {
    id: "locales",
    header: "Locales",
    cell: (item) => (
      <div className="space-y-1">
        <p className="text-sm font-medium text-foreground">
          {item.localeSummary}
        </p>
        <p className="text-xs text-muted-foreground">Preview shell only</p>
      </div>
    ),
  },
  {
    id: "state",
    header: "State",
    cell: (item) => (
      <span className="inline-flex rounded-full border border-primary/15 bg-primary/8 px-2.5 py-1 text-xs font-medium text-primary">
        {item.activeLabel}
      </span>
    ),
  },
  {
    id: "pages",
    header: "Story Pages",
    cell: (item) => (
      <span className="text-sm text-muted-foreground">{item.pageSummary}</span>
    ),
  },
];

export function ContentsIndexRoute() {
  const navigate = useNavigate();

  return (
    <ContentPageShell
      eyebrow="Editorial Core"
      title="Content Studio"
      description="The content workspace is now routed through a dedicated feature shell. Query hooks, create flows, and persistence come in the next tasks, but the page structure, toolbar, and action region are already fixed."
      actions={
        <>
          <Button disabled type="button" variant="outline">
            <RefreshCw className="size-4" />
            Refresh
          </Button>
          <Button disabled type="button">
            <CirclePlus className="size-4" />
            Create content
          </Button>
        </>
      }
      toolbar={
        <FilterBar>
          <FilterBarGroup>
            <div className="relative min-w-[16rem] flex-1">
              <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
              <Input
                aria-label="Search content shell"
                className="pl-8"
                placeholder="Search by external key or localized title"
                readOnly
                value=""
              />
            </div>
            <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
              All content types
            </div>
            <div className="inline-flex h-8 items-center rounded-lg border border-border/70 bg-background px-2.5 text-sm text-muted-foreground">
              Active and archived
            </div>
          </FilterBarGroup>

          <FilterBarActions>
            <FilterBarSummary
              description="This toolbar is the shared list action surface. M03-T02 will bind it to real query params."
              title="Filter and create entry points"
            />
          </FilterBarActions>
        </FilterBar>
      }
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Workspace Notes</CardTitle>
              <CardDescription>
                This route now owns the list toolbar and content navigation
                structure.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T02` will replace preview rows with backend-backed content
                queries.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                `M03-T03` will activate the create action and metadata form.
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                Row navigation is already aligned with the detail route shell.
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>Editorial Intent</CardTitle>
              <CardDescription>
                The shell is designed around fast scanning and quick entry into
                detail workspaces.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                Preview records are static on purpose. They exist only to make
                the table layout testable before live queries land.
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Sparkles className="size-3.5" />
                Shared toolbar and action region ready
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      <DataTable
        caption="Content shell preview table"
        columns={columns}
        emptyDescription="The list query will be attached in the next task. For now, the route shows stable preview rows so the layout can be reviewed."
        emptyTitle="No preview rows"
        getRowId={(item) => item.id.toString()}
        onRowClick={(item) => navigate(`/contents/${item.id}`)}
        rows={previewRows}
        summary={
          <div className="space-y-1 text-right">
            <p className="text-sm font-medium tracking-tight text-foreground">
              3 preview records
            </p>
            <p className="text-xs text-muted-foreground">
              Click a row to open the detail shell
            </p>
          </div>
        }
        toolbar={
          <FilterBarSummary
            description="The columns, row navigation, and summary slot are in place for live data."
            title="Content registry shell"
          />
        }
      />

      <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
        <CardHeader>
          <CardTitle>Next Up</CardTitle>
          <CardDescription>
            The next content task attaches real list/detail queries without
            changing this layout contract.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              List query hook
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              The preview table will hydrate from `GET /api/admin/contents`.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Detail summary card
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Row clicks already lead into the future detail query surface.
            </p>
          </div>
          <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-4">
            <p className="text-sm font-medium text-foreground">
              Mutation toolbar
            </p>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Create and refresh buttons will become live once content forms are
              connected.
            </p>
          </div>
        </CardContent>
      </Card>
    </ContentPageShell>
  );
}
