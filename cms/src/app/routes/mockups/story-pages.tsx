import { ArrowLeft, CirclePlus, Pencil } from "lucide-react";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

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
import { LanguageTabs } from "@/components/language/language-tabs";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { mockupDemoContent, mockupStoryPages } from "@/features/mockups/fixtures";
import {
  countIllustrationReady,
  getMockupLanguageLabel,
  getReadinessTone,
} from "@/features/mockups/lib";
import {
  MockupInfoCard,
  MockupKeyValueGrid,
  MockupMetricCard,
  MockupStatusPill,
} from "@/features/mockups/components/mockup-ui";
import type { MockupStoryPage } from "@/features/mockups/types";
import { useI18n } from "@/i18n/locale-provider";

type EditStoryPageDialogProps = {
  page: MockupStoryPage | null;
  preferredLanguageCode: string;
  onClose: () => void;
};

function EditStoryPageDialog({
  page,
  preferredLanguageCode,
  onClose,
}: EditStoryPageDialogProps) {
  const { locale } = useI18n();
  const [activeLanguageCode, setActiveLanguageCode] = useState(preferredLanguageCode);
  const copy =
    locale === "tr"
      ? {
          title: "Edit story page",
          description:
            "The Variant A editor modal stays large and scroll-safe so longer page payloads never collapse into a drawer.",
          localeStatus: "Locale status",
          copyBody: "Body copy",
          audio: "Audio asset",
          illustration: "Illustration",
          close: "Close editor",
        }
      : {
          title: "Edit story page",
          description:
            "The Variant A editor modal stays large and scroll-safe so longer page payloads never collapse into a drawer.",
          localeStatus: "Locale status",
          copyBody: "Body copy",
          audio: "Audio asset",
          illustration: "Illustration",
          close: "Close editor",
        };

  if (!page) {
    return null;
  }

  const resolvedLanguageCode =
    page.localizations.find((localization) => localization.languageCode === activeLanguageCode)
      ?.languageCode ?? page.localizations[0]?.languageCode ?? "en";

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-5xl">
        <DialogHeader>
          <DialogTitle>
            {copy.title} #{page.pageNumber}
          </DialogTitle>
          <DialogDescription>{copy.description}</DialogDescription>
        </DialogHeader>
        <DialogBody className="grid gap-6">
          <div className="grid gap-4 md:grid-cols-3">
            <MockupMetricCard
              detail={page.summary}
              label="Page"
              tone="accent"
              value={`#${page.pageNumber}`}
            />
            <MockupMetricCard
              detail={page.note}
              label="Locales"
              tone="default"
              value={`${page.localizations.length} configured`}
            />
            <MockupMetricCard
              detail="The modal starts with the preferred locale carried from content detail."
              label="Entry focus"
              tone="success"
              value={getMockupLanguageLabel(resolvedLanguageCode, locale)}
            />
          </div>

          <LanguageTabs
            items={page.localizations.map((localization) => ({
              code: localization.languageCode,
              label: getMockupLanguageLabel(localization.languageCode, locale),
              meta: localization.statusLabel,
              tone: getReadinessTone(localization),
              description: localization.note,
            }))}
            listLabel="Story page localization tabs"
            value={resolvedLanguageCode}
            onValueChange={setActiveLanguageCode}
            renderContent={(item) => {
              const localization =
                page.localizations.find(
                  (candidate) => candidate.languageCode === item.code,
                ) ?? page.localizations[0];

              return (
                <div className="grid gap-4">
                  <MockupInfoCard
                    description={localization.description}
                    title={localization.title}
                  >
                    <MockupKeyValueGrid
                      items={[
                        {
                          label: copy.localeStatus,
                          tone: getReadinessTone(localization),
                          value: localization.statusLabel,
                        },
                        {
                          label: copy.copyBody,
                          tone: localization.hasBodyText ? "success" : "warning",
                          value: localization.hasBodyText ? "Ready" : "Missing",
                        },
                        {
                          label: copy.audio,
                          tone: localization.hasAudio ? "success" : "warning",
                          value: localization.hasAudio ? "Ready" : "Missing",
                        },
                        {
                          label: copy.illustration,
                          tone: localization.hasIllustration ? "success" : "warning",
                          value: localization.hasIllustration ? "Ready" : "Missing",
                        },
                      ]}
                    />
                  </MockupInfoCard>
                </div>
              );
            }}
          />
        </DialogBody>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {copy.close}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function MockupStoryPagesRoute() {
  const { locale } = useI18n();
  const [searchParams] = useSearchParams();
  const preferredLanguageCode = searchParams.get("language") ?? "en";
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [editingPage, setEditingPage] = useState<MockupStoryPage | null>(null);
  const preferredLanguageLabel = getMockupLanguageLabel(preferredLanguageCode, locale);
  const localizedPageCount = mockupStoryPages.filter((page) =>
    page.localizations.some((localization) => localization.hasBodyText),
  ).length;
  const completeIllustrationCoverageCount = mockupStoryPages.filter(
    (page) => countIllustrationReady(page.localizations) === page.localizations.length,
  ).length;
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Variant A Mockups",
          title: "Story Page Editor Mockup",
          description:
            "The editor stays table-first and opens a large modal for localized payload work, preserving the preferred locale carried from content detail.",
          backToContent: "Back to content detail",
          addPage: "Add story page",
          readinessTitle: "Story readiness",
          readinessDescription:
            "Variant A keeps page structure primary while the rail compresses readiness density borrowed from the stronger parts of Variant C.",
          pages: "Pages",
          localized: "Localized pages",
          illustrationCoverage: "Illustration coverage",
          createTitle: "Add story page",
          createDescription:
            "The create flow remains lightweight: add structure first, then enter the large modal to complete locale payloads.",
          createClose: "Close",
          preferredLocale: "Preferred locale",
          toolbarDescription:
            "The table remains primary, while the locale handoff stays visible in the summary strip.",
          summaryTitle: "Story page table",
          page: "Page",
          localeCoverage: "Locale coverage",
          illustration: "Illustrations",
          note: "Editorial note",
          actions: "Actions",
          openEditor: "Edit page",
        }
      : {
          eyebrow: "Variant A Mockups",
          title: "Story Page Editor Mockup",
          description:
            "The editor stays table-first and opens a large modal for localized payload work, preserving the preferred locale carried from content detail.",
          backToContent: "Back to content detail",
          addPage: "Add story page",
          readinessTitle: "Story readiness",
          readinessDescription:
            "Variant A keeps page structure primary while the rail compresses readiness density borrowed from the stronger parts of Variant C.",
          pages: "Pages",
          localized: "Localized pages",
          illustrationCoverage: "Illustration coverage",
          createTitle: "Add story page",
          createDescription:
            "The create flow remains lightweight: add structure first, then enter the large modal to complete locale payloads.",
          createClose: "Close",
          preferredLocale: "Preferred locale",
          toolbarDescription:
            "The table remains primary, while the locale handoff stays visible in the summary strip.",
          summaryTitle: "Story page table",
          page: "Page",
          localeCoverage: "Locale coverage",
          illustration: "Illustrations",
          note: "Editorial note",
          actions: "Actions",
          openEditor: "Edit page",
        };

  const columns: DataTableColumn<MockupStoryPage>[] = [
    {
      id: "page",
      header: copy.page,
      cell: (page) => (
        <div className="space-y-1">
          <p className="font-medium text-foreground">Page {page.pageNumber}</p>
          <p className="text-xs text-muted-foreground">{page.summary}</p>
        </div>
      ),
    },
    {
      id: "locales",
      header: copy.localeCoverage,
      cell: (page) => (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {page.localizations.filter((localization) => localization.hasBodyText).length} /{" "}
            {page.localizations.length} ready
          </p>
          <p className="text-xs text-muted-foreground">
            {page.localizations
              .map((localization) => localization.languageCode.toUpperCase())
              .join(", ")}
          </p>
        </div>
      ),
    },
    {
      id: "illustrations",
      header: copy.illustration,
      cell: (page) => (
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {countIllustrationReady(page.localizations)} / {page.localizations.length} ready
          </p>
          <p className="text-xs text-muted-foreground">{page.note}</p>
        </div>
      ),
    },
    {
      id: "actions",
      header: copy.actions,
      align: "right",
      cellClassName: "w-[1%]",
      cell: (page) => (
        <Button size="sm" type="button" variant="outline" onClick={() => setEditingPage(page)}>
          <Pencil className="size-4" />
          {copy.openEditor}
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
              <Link to="/labs/mockups/contents/demo-content">
                <ArrowLeft className="size-4" />
                {copy.backToContent}
              </Link>
            </Button>
            <Button type="button" onClick={() => setIsCreateDialogOpen(true)}>
              <CirclePlus className="size-4" />
              {copy.addPage}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.readinessTitle}
            description={copy.readinessDescription}
            stats={[
              {
                label: copy.pages,
                value: `${mockupStoryPages.length} total`,
              },
              {
                label: copy.localized,
                value: `${localizedPageCount} / ${mockupStoryPages.length}`,
                tone:
                  localizedPageCount === mockupStoryPages.length
                    ? "success"
                    : "warning",
              },
              {
                label: copy.illustrationCoverage,
                value: `${completeIllustrationCoverageCount} / ${mockupStoryPages.length}`,
                tone:
                  completeIllustrationCoverageCount === mockupStoryPages.length
                    ? "success"
                    : "warning",
              },
            ]}
          >
            <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
              <p className="font-medium text-foreground">{copy.preferredLocale}</p>
              <p>{preferredLanguageLabel}</p>
              <p>
                The editor route was opened from {mockupDemoContent.externalKey} with
                locale focus preserved.
              </p>
            </div>
          </TaskRail>
        }
        toolbar={
          <div className="flex flex-wrap items-center gap-2 rounded-[1.7rem] border border-border/70 bg-muted/15 px-4 py-4">
            <MockupStatusPill tone="accent">{copy.preferredLocale}</MockupStatusPill>
            <MockupStatusPill tone="default">{preferredLanguageLabel}</MockupStatusPill>
            <span className="text-sm text-muted-foreground">{copy.toolbarDescription}</span>
          </div>
        }
      >
        <DataTable
          caption={copy.summaryTitle}
          columns={columns}
          getRowId={(page) => page.id}
          rows={mockupStoryPages}
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
              description="The first edit happens in the modal so structure and payload work stay connected."
              title="Structure first"
            >
              <MockupKeyValueGrid
                items={[
                  { label: "Next page number", value: "5", tone: "accent" },
                  { label: "Inherited locales", value: "English, Turkish", tone: "default" },
                  { label: "Open in modal", value: "Immediately after create", tone: "success" },
                ]}
              />
            </MockupInfoCard>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
              {copy.createClose}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <EditStoryPageDialog
        key={editingPage?.id ? `${editingPage.id}-${preferredLanguageCode}` : "story-page-editor-closed"}
        page={editingPage}
        preferredLanguageCode={preferredLanguageCode}
        onClose={() => setEditingPage(null)}
      />
    </>
  );
}
