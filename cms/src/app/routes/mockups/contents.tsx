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
import { mockupContentRegistry } from "@/features/mockups/fixtures";
import { countProcessingComplete, countVisibleLocales } from "@/features/mockups/lib";
import { MockupInfoCard, MockupStatusPill } from "@/features/mockups/components/mockup-ui";
import type { MockupContentSummary } from "@/features/mockups/types";
import { useI18n } from "@/i18n/locale-provider";

function getStateTone(content: MockupContentSummary) {
  return content.active ? ("success" as const) : ("default" as const);
}

export function MockupContentsRoute() {
  const { locale } = useI18n();
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState<string>("ALL");
  const [selectedState, setSelectedState] = useState<"ALL" | "ACTIVE" | "INACTIVE">(
    "ALL",
  );
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const deferredSearch = useDeferredValue(search);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Variant A Mockups",
          title: "Content Studio Mockup",
          description:
            "Fixture tabanli registry ve detail akisi, sakin ana kolon ve readiness rail ile production benzeri bir Variant A demosu sunar.",
          backToLab: "Mockup index",
          create: "Create content",
          searchLabel: "Search mockup contents",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All types",
          filterState: "All states",
          active: "Active",
          inactive: "Inactive",
          records: "records",
          summaryDescription:
            "Filters stay intentionally light so the registry remains calm before the detail handoff.",
          createTitle: "Create content",
          createDescription:
            "This modal previews the Variant A creation posture: short metadata fields up front, locale workspaces after save, and the right rail left free for readiness context.",
          createOutcomeTitle: "Expected outcome",
          createOutcomeBody:
            "After save, editors land in the detail workspace with the first locale tab open and story-page handoff available for STORY records.",
          close: "Close",
          snapshotTitle: "Registry readiness",
          snapshotDescription:
            "The rail keeps overall release posture visible while the main lane stays focused on the registry.",
          visible: "Visible locales",
          processing: "Processing complete",
          demo: "Variant A demo",
          reference: "Reference item",
          open: "Open workspace",
          noteTitle: "Why this shell wins",
          noteOne:
            "The registry remains quiet until an editor commits to a detail route.",
          noteTwo:
            "Create stays available without competing with readiness context.",
          noteThree:
            "Story-specific actions remain off the list screen until the selected record needs them.",
          toolbarTitle: "Registry controls",
        }
      : {
          eyebrow: "Variant A Mockups",
          title: "Content Studio Mockup",
          description:
            "A fixture-backed registry and detail handoff that shows the calm main lane and readiness rail of Variant A in a production-like shell.",
          backToLab: "Mockup index",
          create: "Create content",
          searchLabel: "Search mockup contents",
          searchPlaceholder: "Search by external key or localized title",
          filterTypes: "All types",
          filterState: "All states",
          active: "Active",
          inactive: "Inactive",
          records: "records",
          summaryDescription:
            "Filters stay intentionally light so the registry remains calm before the detail handoff.",
          createTitle: "Create content",
          createDescription:
            "This modal previews the Variant A creation posture: short metadata fields up front, locale workspaces after save, and the right rail left free for readiness context.",
          createOutcomeTitle: "Expected outcome",
          createOutcomeBody:
            "After save, editors land in the detail workspace with the first locale tab open and story-page handoff available for STORY records.",
          close: "Close",
          snapshotTitle: "Registry readiness",
          snapshotDescription:
            "The rail keeps overall release posture visible while the main lane stays focused on the registry.",
          visible: "Visible locales",
          processing: "Processing complete",
          demo: "Variant A demo",
          reference: "Reference item",
          open: "Open workspace",
          noteTitle: "Why this shell wins",
          noteOne:
            "The registry remains quiet until an editor commits to a detail route.",
          noteTwo:
            "Create stays available without competing with readiness context.",
          noteThree:
            "Story-specific actions remain off the list screen until the selected record needs them.",
          toolbarTitle: "Registry controls",
        };

  const typeOptions = useMemo(() => {
    const nextOptions = new Set<string>();

    mockupContentRegistry.forEach((content) => {
      nextOptions.add(content.typeLabel);
    });

    return ["ALL", ...Array.from(nextOptions)];
  }, []);

  const filteredContents = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return mockupContentRegistry.filter((content) => {
      if (selectedType !== "ALL" && content.typeLabel !== selectedType) {
        return false;
      }

      if (selectedState === "ACTIVE" && !content.active) {
        return false;
      }

      if (selectedState === "INACTIVE" && content.active) {
        return false;
      }

      if (normalizedSearch.length === 0) {
        return true;
      }

      return (
        content.externalKey.toLowerCase().includes(normalizedSearch) ||
        content.locales.some((localeState) =>
          localeState.title.toLowerCase().includes(normalizedSearch),
        )
      );
    });
  }, [deferredSearch, selectedState, selectedType]);

  const visibleLocaleCount = mockupContentRegistry.reduce(
    (sum, content) => sum + countVisibleLocales(content.locales),
    0,
  );
  const processingCompleteCount = mockupContentRegistry.reduce(
    (sum, content) => sum + countProcessingComplete(content.locales),
    0,
  );
  const totalLocaleCount = mockupContentRegistry.reduce(
    (sum, content) => sum + content.locales.length,
    0,
  );

  const columns: DataTableColumn<MockupContentSummary>[] = [
    {
      id: "content",
      header: locale === "tr" ? "Content" : "Content",
      cell: (content) => (
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-medium text-foreground">
              {content.locales[0]?.title ?? content.externalKey}
            </p>
            <MockupStatusPill tone={content.isDemo ? "accent" : "default"}>
              {content.isDemo ? copy.demo : copy.reference}
            </MockupStatusPill>
          </div>
          <p className="text-xs text-muted-foreground">{content.externalKey}</p>
        </div>
      ),
    },
    {
      id: "type",
      header: locale === "tr" ? "Format" : "Format",
      cell: (content) => (
        <MockupStatusPill tone="default">{content.typeLabel}</MockupStatusPill>
      ),
    },
    {
      id: "state",
      header: locale === "tr" ? "State" : "State",
      cell: (content) => (
        <MockupStatusPill tone={getStateTone(content)}>
          {content.active ? copy.active : copy.inactive}
        </MockupStatusPill>
      ),
    },
    {
      id: "locales",
      header: locale === "tr" ? "Locale coverage" : "Locale coverage",
      cell: (content) => (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {countVisibleLocales(content.locales)} / {content.locales.length} {copy.visible}
          </p>
          <p className="text-xs text-muted-foreground">{content.note}</p>
        </div>
      ),
    },
    {
      id: "actions",
      header: locale === "tr" ? "Actions" : "Actions",
      align: "right",
      cellClassName: "w-[1%]",
      cell: (content) =>
        content.isDemo ? (
          <Button asChild size="sm" type="button" variant="outline">
            <Link to="/labs/mockups/contents/demo-content">
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
              <Link to="/labs/mockups">{copy.backToLab}</Link>
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.create}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.snapshotTitle}
            description={copy.snapshotDescription}
            stats={[
              {
                label: copy.visible,
                value: `${visibleLocaleCount} / ${totalLocaleCount}`,
                tone: visibleLocaleCount > 0 ? "success" : "warning",
              },
              {
                label: copy.processing,
                value: `${processingCompleteCount} / ${totalLocaleCount}`,
                tone:
                  processingCompleteCount === totalLocaleCount
                    ? "success"
                    : "warning",
              },
            ]}
          >
            <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
              <p className="font-medium text-foreground">{copy.noteTitle}</p>
              <p>{copy.noteOne}</p>
              <p>{copy.noteTwo}</p>
              <p>{copy.noteThree}</p>
            </div>
          </TaskRail>
        }
        toolbar={
          <FilterBar aria-label={copy.toolbarTitle}>
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
              title={`${filteredContents.length} / ${mockupContentRegistry.length} ${copy.records}`}
              description={copy.summaryDescription}
            />
          </FilterBar>
        }
      >
        <DataTable
          caption="Variant A content mockup registry"
          columns={columns}
          getRowId={(content) => content.id}
          rows={filteredContents}
        />
      </ContentPageShell>

      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{copy.createTitle}</DialogTitle>
            <DialogDescription>{copy.createDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-4">
            <MockupInfoCard
              title={copy.createOutcomeTitle}
              description={copy.createOutcomeBody}
            >
              <div className="grid gap-3 sm:grid-cols-3">
                <MockupStatusPill tone="accent">Metadata first</MockupStatusPill>
                <MockupStatusPill tone="success">Locale tabs after save</MockupStatusPill>
                <MockupStatusPill tone="default">Readiness stays in the rail</MockupStatusPill>
              </div>
            </MockupInfoCard>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
