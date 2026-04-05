import { AlertTriangle, LoaderCircle, Table2 } from "lucide-react";
import type { KeyboardEvent, ReactNode } from "react";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useI18n } from "@/i18n/locale-provider";
import { cn } from "@/lib/utils";
import type { ApiProblemDetail } from "@/types/api";

export type DataTableColumnAlignment = "left" | "center" | "right";

export type DataTableColumn<TItem> = {
  id: string;
  header: ReactNode;
  cell: (item: TItem) => ReactNode;
  align?: DataTableColumnAlignment;
  headerClassName?: string;
  cellClassName?: string;
};

type DataTableProps<TItem> = {
  columns: DataTableColumn<TItem>[];
  rows: TItem[];
  getRowId: (item: TItem, index: number) => string;
  toolbar?: ReactNode;
  summary?: ReactNode;
  caption?: string;
  isLoading?: boolean;
  loadingTitle?: string;
  loadingDescription?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onRowClick?: (item: TItem) => void;
  rowClassName?: (item: TItem) => string | undefined;
  className?: string;
  tableClassName?: string;
};

function getAlignmentClassName(alignment: DataTableColumnAlignment = "left") {
  switch (alignment) {
    case "center":
      return "text-center";
    case "right":
      return "text-right";
    case "left":
    default:
      return "text-left";
  }
}

export function DataTable<TItem>({
  columns,
  rows,
  getRowId,
  toolbar,
  summary,
  caption,
  isLoading = false,
  loadingTitle,
  loadingDescription,
  emptyTitle,
  emptyDescription,
  emptyAction,
  problem,
  onRetry,
  onRowClick,
  rowClassName,
  className,
  tableClassName,
}: DataTableProps<TItem>) {
  const { t } = useI18n();
  const isInteractive = typeof onRowClick === "function";
  const resolvedLoadingTitle = loadingTitle ?? t("data.loadingTitle");
  const resolvedLoadingDescription =
    loadingDescription ?? t("data.loadingDescription");
  const resolvedEmptyTitle = emptyTitle ?? t("data.emptyTitle");
  const resolvedEmptyDescription =
    emptyDescription ?? t("data.emptyDescription");

  function handleRowKeyDown(
    event: KeyboardEvent<HTMLTableRowElement>,
    item: TItem,
  ) {
    if (!isInteractive) {
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onRowClick(item);
    }
  }

  return (
    <Card
      className={cn(
        "border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5",
        className,
      )}
    >
      {toolbar || summary ? (
        <CardHeader className="gap-4 border-b border-border/60 pb-4 sm:flex sm:flex-row sm:items-start sm:justify-between">
          {toolbar ? <div className="min-w-0 flex-1">{toolbar}</div> : null}
          {summary ? <div className="shrink-0">{summary}</div> : null}
        </CardHeader>
      ) : null}

      <CardContent className="p-4">
        {problem ? (
          <ProblemAlert
            actions={
              onRetry ? (
                <Button type="button" variant="outline" onClick={onRetry}>
                  {t("app.retry")}
                </Button>
              ) : null
            }
            className="mb-4"
            problem={problem}
          />
        ) : null}

        {isLoading ? (
          <div className="flex min-h-56 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border/80 bg-muted/25 px-6 py-10 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
              <LoaderCircle className="size-6 animate-spin" />
            </div>
            <div className="space-y-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                {resolvedLoadingTitle}
              </h2>
              <p className="max-w-xl text-sm leading-6 text-muted-foreground">
                {resolvedLoadingDescription}
              </p>
            </div>
          </div>
        ) : rows.length === 0 ? (
          <EmptyState
            action={emptyAction}
            className="min-h-56"
            description={resolvedEmptyDescription}
            icon={problem ? AlertTriangle : Table2}
            title={resolvedEmptyTitle}
          />
        ) : (
          <Table className={tableClassName}>
            {caption ? <caption className="sr-only">{caption}</caption> : null}
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                {columns.map((column) => (
                  <TableHead
                    key={column.id}
                    className={cn(
                      getAlignmentClassName(column.align),
                      column.headerClassName,
                    )}
                  >
                    {column.header}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row, index) => (
                <TableRow
                  key={getRowId(row, index)}
                  className={cn(
                    isInteractive &&
                      "cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50",
                    rowClassName?.(row),
                  )}
                  onClick={isInteractive ? () => onRowClick(row) : undefined}
                  onKeyDown={(event) => handleRowKeyDown(event, row)}
                  tabIndex={isInteractive ? 0 : undefined}
                >
                  {columns.map((column) => (
                    <TableCell
                      key={column.id}
                      className={cn(
                        getAlignmentClassName(column.align),
                        column.cellClassName,
                      )}
                    >
                      {column.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
