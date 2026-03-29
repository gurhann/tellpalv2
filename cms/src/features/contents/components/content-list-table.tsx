import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { FilterBarSummary } from "@/components/data/filter-bar";
import type { ContentReadViewModel } from "@/features/contents/model/content-view-model";
import type { ApiProblemDetail } from "@/types/api";

type ContentListTableProps = {
  contents: ContentReadViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onContentSelect?: (content: ContentReadViewModel) => void;
};

function getContentTitle(content: ContentReadViewModel) {
  return content.primaryLocalization?.title ?? "Untitled content";
}

function getLocalizationSummary(content: ContentReadViewModel) {
  if (content.localizationCount === 0) {
    return {
      title: "No localizations yet",
      detail: "Add the first language in the next content task.",
    };
  }

  const languageCodes = content.localizations
    .map((localization) => localization.languageCode.toUpperCase())
    .join(", ");

  return {
    title: `${content.localizationCount} locale${
      content.localizationCount === 1 ? "" : "s"
    }`,
    detail: languageCodes,
  };
}

const columns: DataTableColumn<ContentReadViewModel>[] = [
  {
    id: "content",
    header: "Content",
    cell: (content) => (
      <div className="space-y-1">
        <p className="font-medium text-foreground">
          {getContentTitle(content)}
        </p>
        <p className="text-xs text-muted-foreground">
          {content.summary.externalKey}
        </p>
      </div>
    ),
  },
  {
    id: "format",
    header: "Format",
    cell: (content) => (
      <span className="inline-flex rounded-full border border-border/70 bg-muted/35 px-2.5 py-1 text-xs font-medium tracking-tight text-foreground">
        {content.summary.typeLabel}
      </span>
    ),
  },
  {
    id: "state",
    header: "State",
    cell: (content) => (
      <span
        className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${
          content.summary.active
            ? "border-emerald-200 bg-emerald-50 text-emerald-700"
            : "border-border/70 bg-muted/35 text-muted-foreground"
        }`}
      >
        {content.summary.active ? "Active" : "Inactive"}
      </span>
    ),
  },
  {
    id: "localizations",
    header: "Localizations",
    cell: (content) => {
      const summary = getLocalizationSummary(content);

      return (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">{summary.title}</p>
          <p className="text-xs text-muted-foreground">{summary.detail}</p>
        </div>
      );
    },
  },
  {
    id: "pages",
    header: "Story Pages",
    cell: (content) => (
      <span className="text-sm text-muted-foreground">
        {content.summary.supportsStoryPages
          ? `${content.summary.pageCount ?? 0} page${
              content.summary.pageCount === 1 ? "" : "s"
            }`
          : "Not applicable"}
      </span>
    ),
  },
];

export function ContentListTable({
  contents,
  isLoading = false,
  problem = null,
  onRetry,
  onContentSelect,
}: ContentListTableProps) {
  const activeCount = contents.filter(
    (content) => content.summary.active,
  ).length;
  const inactiveCount = contents.length - activeCount;

  if (problem && contents.length === 0 && !isLoading) {
    return (
      <DataTable
        columns={columns}
        emptyDescription="The content list could not be loaded from the admin API."
        emptyTitle="Content list unavailable"
        getRowId={(content) => content.summary.id.toString()}
        onRetry={onRetry}
        problem={problem}
        rows={[]}
      />
    );
  }

  return (
    <DataTable
      caption="Content registry table"
      columns={columns}
      emptyDescription="No content records exist yet. Create the first content item in the next task."
      emptyTitle="No content records"
      getRowId={(content) => content.summary.id.toString()}
      isLoading={isLoading}
      loadingDescription="The CMS is requesting content metadata and localization snapshots from the admin API."
      loadingTitle="Loading content registry"
      onRetry={onRetry}
      onRowClick={onContentSelect}
      problem={contents.length > 0 ? problem : null}
      rows={contents}
      summary={
        <div className="space-y-1 text-right">
          <p className="text-sm font-medium tracking-tight text-foreground">
            {contents.length} record{contents.length === 1 ? "" : "s"}
          </p>
          <p className="text-xs text-muted-foreground">
            {activeCount} active / {inactiveCount} inactive
          </p>
        </div>
      }
      toolbar={
        <FilterBarSummary
          description="Live backend read data is now bound to the shared content registry table."
          title="Content registry"
        />
      }
    />
  );
}
