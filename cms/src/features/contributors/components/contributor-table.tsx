import { PencilLine, Trash2 } from "lucide-react";

import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import type { ContributorRole } from "@/features/contributors/api/contributor-admin";
import { useI18n } from "@/i18n/locale-provider";
import type { ApiProblemDetail } from "@/types/api";

type ContributorTableProps = {
  contributors: ContributorViewModel[];
  isLoading?: boolean;
  problem?: ApiProblemDetail | null;
  onRetry?: () => void;
  onRenameContributor?: (contributor: ContributorViewModel) => void;
  onDeleteContributor?: (contributor: ContributorViewModel) => void;
  onClearFilters?: () => void;
  isMutationPending?: boolean;
  locale?: "en" | "tr";
};

function createContributorColumns({
  onRenameContributor,
  onDeleteContributor,
  isMutationPending,
  locale = "en",
  translate,
}: Pick<
  ContributorTableProps,
  "onRenameContributor" | "onDeleteContributor" | "isMutationPending" | "locale"
> & { translate: (key: string) => string }) {
  const roleLabel = (role: ContributorRole) =>
    translate(`contributors.role.${role.toLowerCase()}`);
  return [
    {
      id: "contributor",
      header: translate("contributors.columnContributor"),
      cell: (contributor: ContributorViewModel) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">
            {contributor.displayName}
          </p>
          <p className="text-xs text-muted-foreground">
            {translate("contributors.idLabel").replace(
              "{id}",
              String(contributor.id),
            )}
          </p>
        </div>
      ),
    },
    {
      id: "initials",
      header: translate("contributors.columnInitials"),
      cell: (contributor: ContributorViewModel) => (
        <span className="rounded-full border border-border/70 bg-muted/25 px-3 py-1.5 text-sm font-medium">
          {contributor.initials}
        </span>
      ),
    },
    {
      id: "roles",
      header: translate("contributors.columnRoles"),
      cell: (contributor: ContributorViewModel) => (
        <div className="flex flex-wrap gap-1">
          {(contributor.roles ?? []).map((role) => (
            <span
              key={role}
              className="rounded-full border border-border/70 bg-muted/25 px-2 py-1 text-xs"
            >
              {roleLabel(role)}
            </span>
          ))}
        </div>
      ),
    },
    ...(onRenameContributor || onDeleteContributor
      ? [
          {
            id: "usage",
            header: translate("contributors.columnUsage"),
            cell: (contributor: ContributorViewModel) =>
              contributor.totalUsageCount ?? 0,
          },
          {
            id: "updatedAt",
            header: translate("contributors.columnUpdated"),
            cell: (contributor: ContributorViewModel) =>
              contributor.updatedAt
                ? new Intl.DateTimeFormat(locale).format(
                    new Date(contributor.updatedAt),
                  )
                : translate("app.notAvailable"),
          },
          {
            id: "actions",
            header: translate("contributors.actions"),
            align: "right" as const,
            cellClassName: "w-[1%]",
            cell: (contributor: ContributorViewModel) => (
              <div className="flex justify-end gap-2">
                {onRenameContributor ? (
                  <Button
                    aria-label={translate("contributors.renameAria").replace(
                      "{name}",
                      contributor.displayName,
                    )}
                    type="button"
                    variant="outline"
                    disabled={isMutationPending}
                    onClick={() => onRenameContributor(contributor)}
                  >
                    <PencilLine className="size-4" />
                    {translate("contributors.rename")}
                  </Button>
                ) : null}
                {onDeleteContributor ? (
                  <Button
                    aria-label={translate("contributors.deleteAria").replace(
                      "{name}",
                      contributor.displayName,
                    )}
                    type="button"
                    variant="outline"
                    disabled={isMutationPending}
                    onClick={() => onDeleteContributor(contributor)}
                  >
                    <Trash2 className="size-4" />
                    {translate("contributors.delete")}
                  </Button>
                ) : null}
              </div>
            ),
          },
        ]
      : []),
  ] satisfies DataTableColumn<ContributorViewModel>[];
}

export function ContributorTable({
  contributors,
  isLoading = false,
  problem = null,
  onRetry,
  onRenameContributor,
  onDeleteContributor,
  onClearFilters,
  isMutationPending = false,
  locale = "en",
}: ContributorTableProps) {
  const { t } = useI18n();
  const contributorColumns = createContributorColumns({
    onRenameContributor,
    onDeleteContributor,
    isMutationPending,
    locale,
    translate: (key) => t(key as never),
  });

  return (
    <DataTable
      caption={t("contributors.table")}
      columns={contributorColumns}
      emptyDescription={t("contributors.emptyDescription")}
      emptyAction={
        onClearFilters ? (
          <Button type="button" variant="outline" onClick={onClearFilters}>
            {t("contributors.clearFilters")}
          </Button>
        ) : undefined
      }
      emptyTitle={t("contributors.emptyTitle")}
      getRowId={(contributor) => contributor.id.toString()}
      isLoading={isLoading}
      loadingDescription={t("contributors.loadingDescription")}
      loadingTitle={t("contributors.loadingTitle")}
      onRetry={onRetry}
      problem={problem}
      rows={contributors}
      tableClassName="[&_td:first-child]:w-[34%]"
    />
  );
}
