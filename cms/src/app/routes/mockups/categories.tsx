import { CirclePlus, Eye, Search } from "lucide-react";
import { useDeferredValue, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import {
  FilterBar,
  FilterBarActions,
  FilterBarGroup,
  FilterBarSummary,
} from "@/components/data/filter-bar";
import { DataTable, type DataTableColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { mockupCategoryRegistry } from "@/features/mockups/fixtures";
import { MockupInfoCard, MockupStatusPill } from "@/features/mockups/components/mockup-ui";
import type { MockupCategorySummary } from "@/features/mockups/types";
import { useI18n } from "@/i18n/locale-provider";

export function MockupCategoriesRoute() {
  const { locale } = useI18n();
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState<string>("ALL");
  const [selectedAccess, setSelectedAccess] = useState<"ALL" | "PREMIUM" | "STANDARD">(
    "ALL",
  );
  const [selectedState, setSelectedState] = useState<"ALL" | "ACTIVE" | "INACTIVE">(
    "ALL",
  );
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Variant A Mockups",
          title: "Category Studio Mockup",
          description:
            "The category registry mirrors the same calm Variant A shell while leaving the right rail available for localization and curation prerequisites.",
          backToIndex: "Mockup index",
          create: "Create category",
          searchLabel: "Search mockup categories",
          searchPlaceholder: "Search by slug",
          filterTypes: "All types",
          filterAccess: "All access",
          filterState: "All states",
          active: "Active",
          inactive: "Inactive",
          premium: "Premium",
          standard: "Standard",
          open: "Open workspace",
          demo: "Variant A demo",
          reference: "Reference item",
          summaryDescription:
            "The registry stays low-noise until an editor needs the detailed localization and curation lane.",
          createTitle: "Create category",
          createDescription:
            "This modal previews the staged Variant A creation pattern: base metadata now, localization and curation after save.",
          createClose: "Close",
          railTitle: "Curation posture",
          railDescription:
            "Prerequisites and curation readiness stay visible without crowding the registry table.",
          noteOne: "Category metadata and curation share the same learning model as content detail.",
          noteTwo:
            "Published locale prerequisites can remain in the rail instead of becoming an extra flow step.",
        }
      : {
          eyebrow: "Variant A Mockups",
          title: "Category Studio Mockup",
          description:
            "The category registry mirrors the same calm Variant A shell while leaving the right rail available for localization and curation prerequisites.",
          backToIndex: "Mockup index",
          create: "Create category",
          searchLabel: "Search mockup categories",
          searchPlaceholder: "Search by slug",
          filterTypes: "All types",
          filterAccess: "All access",
          filterState: "All states",
          active: "Active",
          inactive: "Inactive",
          premium: "Premium",
          standard: "Standard",
          open: "Open workspace",
          demo: "Variant A demo",
          reference: "Reference item",
          summaryDescription:
            "The registry stays low-noise until an editor needs the detailed localization and curation lane.",
          createTitle: "Create category",
          createDescription:
            "This modal previews the staged Variant A creation pattern: base metadata now, localization and curation after save.",
          createClose: "Close",
          railTitle: "Curation posture",
          railDescription:
            "Prerequisites and curation readiness stay visible without crowding the registry table.",
          noteOne: "Category metadata and curation share the same learning model as content detail.",
          noteTwo:
            "Published locale prerequisites can remain in the rail instead of becoming an extra flow step.",
        };

  const filteredCategories = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return mockupCategoryRegistry.filter((category) => {
      if (selectedType !== "ALL" && category.typeLabel !== selectedType) {
        return false;
      }

      if (selectedAccess === "PREMIUM" && !category.premium) {
        return false;
      }

      if (selectedAccess === "STANDARD" && category.premium) {
        return false;
      }

      if (selectedState === "ACTIVE" && !category.active) {
        return false;
      }

      if (selectedState === "INACTIVE" && category.active) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return category.slug.toLowerCase().includes(normalizedSearch);
    });
  }, [deferredSearch, selectedAccess, selectedState, selectedType]);

  const typeOptions = useMemo(() => {
    const nextOptions = new Set<string>();

    mockupCategoryRegistry.forEach((category) => {
      nextOptions.add(category.typeLabel);
    });

    return ["ALL", ...Array.from(nextOptions)];
  }, []);

  const columns: DataTableColumn<MockupCategorySummary>[] = [
    {
      id: "slug",
      header: "Category",
      cell: (category) => (
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-medium text-foreground">{category.slug}</p>
            <MockupStatusPill tone={category.isDemo ? "accent" : "default"}>
              {category.isDemo ? copy.demo : copy.reference}
            </MockupStatusPill>
          </div>
          <p className="text-xs text-muted-foreground">{category.note}</p>
        </div>
      ),
    },
    {
      id: "type",
      header: "Type",
      cell: (category) => (
        <MockupStatusPill tone="default">{category.typeLabel}</MockupStatusPill>
      ),
    },
    {
      id: "access",
      header: "Access",
      cell: (category) => (
        <MockupStatusPill tone={category.premium ? "warning" : "default"}>
          {category.premium ? copy.premium : copy.standard}
        </MockupStatusPill>
      ),
    },
    {
      id: "state",
      header: "State",
      cell: (category) => (
        <MockupStatusPill tone={category.active ? "success" : "default"}>
          {category.active ? copy.active : copy.inactive}
        </MockupStatusPill>
      ),
    },
    {
      id: "actions",
      header: "Actions",
      align: "right",
      cellClassName: "w-[1%]",
      cell: (category) =>
        category.isDemo ? (
          <Button asChild size="sm" type="button" variant="outline">
            <Link to="/labs/mockups/categories/demo-category">
              <Eye className="size-4" />
              {copy.open}
            </Link>
          </Button>
        ) : (
          <Button disabled size="sm" type="button" variant="ghost">
            {copy.reference}
          </Button>
        ),
    },
  ];

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to="/labs/mockups">{copy.backToIndex}</Link>
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: copy.premium,
                value: `${mockupCategoryRegistry.filter((category) => category.premium).length}`,
              },
              {
                label: copy.active,
                value: `${mockupCategoryRegistry.filter((category) => category.active).length}`,
                tone: "success",
              },
            ]}
          >
            <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
              <p>{copy.noteOne}</p>
              <p>{copy.noteTwo}</p>
            </div>
          </TaskRail>
        }
        toolbar={
          <FilterBar aria-label="Mockup category registry controls">
            <FilterBarGroup>
              <div className="relative min-w-[16rem] flex-1">
                <Search className="pointer-events-none absolute left-2.5 top-2 size-4 text-muted-foreground" />
                <Input
                  aria-label={copy.searchLabel}
                  className="pl-8"
                  placeholder={copy.searchPlaceholder}
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
            </FilterBarGroup>
            <FilterBarActions>
              {typeOptions.map((typeOption) => (
                <Button
                  key={typeOption}
                  size="sm"
                  type="button"
                  variant={selectedType === typeOption ? "secondary" : "outline"}
                  onClick={() => setSelectedType(typeOption)}
                >
                  {typeOption === "ALL" ? copy.filterTypes : typeOption}
                </Button>
              ))}
              {[
                { key: "ALL" as const, label: copy.filterAccess },
                { key: "PREMIUM" as const, label: copy.premium },
                { key: "STANDARD" as const, label: copy.standard },
              ].map((accessOption) => (
                <Button
                  key={accessOption.key}
                  size="sm"
                  type="button"
                  variant={
                    selectedAccess === accessOption.key ? "secondary" : "outline"
                  }
                  onClick={() => setSelectedAccess(accessOption.key)}
                >
                  {accessOption.label}
                </Button>
              ))}
              {[
                { key: "ALL" as const, label: copy.filterState },
                { key: "ACTIVE" as const, label: copy.active },
                { key: "INACTIVE" as const, label: copy.inactive },
              ].map((stateOption) => (
                <Button
                  key={stateOption.key}
                  size="sm"
                  type="button"
                  variant={
                    selectedState === stateOption.key ? "secondary" : "outline"
                  }
                  onClick={() => setSelectedState(stateOption.key)}
                >
                  {stateOption.label}
                </Button>
              ))}
            </FilterBarActions>
            <FilterBarSummary
              title={`${filteredCategories.length} / ${mockupCategoryRegistry.length} records`}
              description={copy.summaryDescription}
            />
          </FilterBar>
        }
      >
        <DataTable
          caption="Variant A category mockup registry"
          columns={columns}
          getRowId={(category) => category.id}
          rows={filteredCategories}
        />
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.createTitle}</DialogTitle>
            <DialogDescription>{copy.createDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-4">
            <MockupInfoCard
              description="The same shell can add language workspaces and curation after the first save."
              title="Metadata now, curation later"
            >
              <div className="grid gap-3 sm:grid-cols-3">
                <MockupStatusPill tone="accent">Slug + type</MockupStatusPill>
                <MockupStatusPill tone="success">Locale workspaces</MockupStatusPill>
                <MockupStatusPill tone="default">Curation in detail</MockupStatusPill>
              </div>
            </MockupInfoCard>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
              {copy.createClose}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
