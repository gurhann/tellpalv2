import { CirclePlus, RefreshCw, Search } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";
import {
  RegistryToolbar,
  RegistryToolbarGroup,
} from "@/components/data/registry-toolbar";
import { Pagination } from "@/components/data/pagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContributorDeleteDialog } from "@/features/contributors/components/contributor-delete-dialog";
import { ContributorFormDialog } from "@/features/contributors/components/contributor-form-dialog";
import { ContributorTable } from "@/features/contributors/components/contributor-table";
import type { ContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { useContributorActions } from "@/features/contributors/mutations/use-contributor-actions";
import { useContributorRegistry } from "@/features/contributors/queries/use-contributors";
import type { ContributorRole } from "@/features/contributors/api/contributor-admin";
import { useI18n } from "@/i18n/locale-provider";
const SIZE = 25;
const roles: ContributorRole[] = [
  "AUTHOR",
  "ILLUSTRATOR",
  "NARRATOR",
  "MUSICIAN",
];

function parsePage(value: string | null) {
  if (!value || !/^\d+$/.test(value)) return 0;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) ? parsed : 0;
}

export function ContributorsRoute() {
  const { locale, t } = useI18n();
  const [params, setParams] = useSearchParams();
  const [createOpen, setCreateOpen] = useState(false);
  const [selected, setSelected] = useState<ContributorViewModel | null>(null);
  const [deleting, setDeleting] = useState<ContributorViewModel | null>(null);
  const q = params.get("q") ?? "";
  const rawRole = params.get("role");
  const role = roles.includes(rawRole as ContributorRole)
    ? (rawRole as ContributorRole)
    : undefined;
  const page = parsePage(params.get("page"));
  const query = useContributorRegistry({ page, size: SIZE, query: q, role });
  useEffect(() => {
    if (!query.isSuccess || query.totalItems === 0 || query.totalPages === 0) {
      return;
    }
    if (page >= query.totalPages) {
      update({ page: String(query.totalPages - 1) });
    }
  }, [page, query.isSuccess, query.totalItems, query.totalPages]);
  const actions = useContributorActions();
  const copy = {
    title: t("contributors.title"),
    desc: t("contributors.description"),
    search: t("contributors.searchLabel"),
    placeholder: t("contributors.searchPlaceholder"),
    filter: t("contributors.roleLabel"),
    all: t("contributors.allRoles"),
    refresh: t("contributors.refresh"),
    create: t("contributors.create"),
    count: t("contributors.total", { count: query.totalItems }),
  };
  function update(next: Record<string, string | undefined>) {
    const nextParams = new URLSearchParams(params);
    Object.entries(next).forEach(([key, value]) =>
      value ? nextParams.set(key, value) : nextParams.delete(key),
    );
    setParams(nextParams);
  }
  return (
    <>
      <ContentPageShell
        eyebrow={copy.title}
        title={copy.title}
        description={copy.desc}
        actions={
          <>
            <Button variant="outline" onClick={() => void query.refetch()}>
              <RefreshCw
                className={query.isFetching ? "size-4 animate-spin" : "size-4"}
              />
              {copy.refresh}
            </Button>
            <Button onClick={() => setCreateOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        toolbar={
          <RegistryToolbar
            ariaLabel={copy.title}
            search={
              <div className="space-y-2">
                <label
                  htmlFor="contributor-search"
                  className="text-sm font-medium"
                >
                  {copy.search}
                </label>
                <div className="relative">
                  <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                  <Input
                    id="contributor-search"
                    className="pl-8"
                    placeholder={copy.placeholder}
                    value={q}
                    onChange={(e) =>
                      update({
                        q: e.target.value || undefined,
                        page: undefined,
                      })
                    }
                  />
                </div>
              </div>
            }
            filters={
              <RegistryToolbarGroup label={copy.filter}>
                <Select
                  value={role ?? "all"}
                  onValueChange={(value) =>
                    update({
                      role: value === "all" ? undefined : value,
                      page: undefined,
                    })
                  }
                >
                  <SelectTrigger className="w-48">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">{copy.all}</SelectItem>
                    {roles.map((item) => (
                      <SelectItem key={item} value={item}>
                        {t(`contributors.role.${item.toLowerCase()}` as never)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </RegistryToolbarGroup>
            }
            summaryTitle={copy.count}
            summaryDescription={t("contributors.page", { page: page + 1 })}
          />
        }
      >
        <ContributorTable
          contributors={query.contributors}
          isLoading={query.isLoading}
          isMutationPending={actions.isPending}
          onDeleteContributor={setDeleting}
          onRenameContributor={setSelected}
          onRetry={() => void query.refetch()}
          problem={query.problem}
          locale={locale}
          onClearFilters={
            q || role
              ? () => {
                  const nextParams = new URLSearchParams(params);
                  nextParams.delete("q");
                  nextParams.delete("role");
                  nextParams.delete("page");
                  setParams(nextParams);
                }
              : undefined
          }
        />
        <Pagination
          page={page}
          pageSize={SIZE}
          totalItems={query.totalItems}
          isLoading={query.isFetching}
          onPageChange={(next) => update({ page: String(next) })}
        />
      </ContentPageShell>
      {createOpen ? (
        <ContributorFormDialog
          mode="create"
          open
          onOpenChange={setCreateOpen}
        />
      ) : null}
      {selected ? (
        <ContributorFormDialog
          mode="rename"
          contributor={selected}
          open
          onOpenChange={(open) => !open && setSelected(null)}
        />
      ) : null}
      <ContributorDeleteDialog
        contributor={deleting}
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
    </>
  );
}
