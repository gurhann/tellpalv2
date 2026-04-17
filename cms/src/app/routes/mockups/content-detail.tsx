import { ArrowRight, BookOpenText, Link2 } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { FormSection } from "@/components/forms/form-section";
import { Button } from "@/components/ui/button";
import { LanguageTabs } from "@/components/language/language-tabs";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { mockupDemoContent } from "@/features/mockups/fixtures";
import {
  countProcessingComplete,
  countVisibleLocales,
  getMockupLanguageLabel,
  getReadinessTone,
} from "@/features/mockups/lib";
import {
  MockupInfoCard,
  MockupKeyValueGrid,
  MockupMetricCard,
  MockupStatusPill,
} from "@/features/mockups/components/mockup-ui";
import { useI18n } from "@/i18n/locale-provider";

export function MockupContentDetailRoute() {
  const { locale } = useI18n();
  const [selectedLanguageCode, setSelectedLanguageCode] = useState(
    mockupDemoContent.locales[0]?.languageCode ?? "en",
  );
  const selectedLocale =
    mockupDemoContent.locales.find(
      (localeState) => localeState.languageCode === selectedLanguageCode,
    ) ?? mockupDemoContent.locales[0];
  const visibleLocaleCount = countVisibleLocales(mockupDemoContent.locales);
  const processingCompleteCount = countProcessingComplete(mockupDemoContent.locales);
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Variant A Mockups",
          backToRegistry: "Back to registry",
          openStoryPages: "Open story page mockup",
          description:
            "Variant A detail shell keeps metadata, localization, contributors, and the story-page handoff in one calm editorial lane.",
          toolbarTitle: "Workspace handoff",
          toolbarDescription:
            "This summary strip keeps the story-specific next step visible without moving editors away from the current record.",
          localizationTitle: "Localization workspaces",
          localizationDescription:
            "Each locale keeps publication, processing, and asset posture on one tab so editors do not bounce between sub-pages.",
          metadataTitle: "Metadata lane",
          metadataDescription:
            "Variant A gives metadata a quiet, low-noise surface while the rail carries release posture.",
          contributorTitle: "Contributor assignments",
          contributorDescription:
            "Credits remain visible in the main lane because they belong to the editorial review cycle, not the operational rail.",
          railTitle: "Readiness rail",
          railDescription:
            "The right rail stays dense and glanceable, leaving the main lane free for authorship work.",
          visible: "Visible locales",
          processing: "Processing complete",
          pages: "Story pages",
          whyItWorks: "Why it works",
          whyOne: "Editors can review release state without leaving the detail route.",
          whyTwo: "Locale tabs carry all high-context decisions on one surface.",
          whyThree:
            "Story page handoff preserves the selected language instead of forcing re-selection.",
          nextAction: "Selected locale handoff",
          nextActionBody:
            "The story editor link below carries the currently active locale so the next surface opens in the same language context.",
          fixedField: "Fixed after create",
          editableField: "Editable in the lane",
          localeStatus: "Locale status",
          storyPagesLabel: "story pages",
          coverReady: "Cover ready",
          audioReady: "Audio ready",
          mobileVisible: "Mobile visible",
          contributorOrder: "Display order",
        }
      : {
          eyebrow: "Variant A Mockups",
          backToRegistry: "Back to registry",
          openStoryPages: "Open story page mockup",
          description:
            "Variant A detail shell keeps metadata, localization, contributors, and the story-page handoff in one calm editorial lane.",
          toolbarTitle: "Workspace handoff",
          toolbarDescription:
            "This summary strip keeps the story-specific next step visible without moving editors away from the current record.",
          localizationTitle: "Localization workspaces",
          localizationDescription:
            "Each locale keeps publication, processing, and asset posture on one tab so editors do not bounce between sub-pages.",
          metadataTitle: "Metadata lane",
          metadataDescription:
            "Variant A gives metadata a quiet, low-noise surface while the rail carries release posture.",
          contributorTitle: "Contributor assignments",
          contributorDescription:
            "Credits remain visible in the main lane because they belong to the editorial review cycle, not the operational rail.",
          railTitle: "Readiness rail",
          railDescription:
            "The right rail stays dense and glanceable, leaving the main lane free for authorship work.",
          visible: "Visible locales",
          processing: "Processing complete",
          pages: "Story pages",
          whyItWorks: "Why it works",
          whyOne: "Editors can review release state without leaving the detail route.",
          whyTwo: "Locale tabs carry all high-context decisions on one surface.",
          whyThree:
            "Story page handoff preserves the selected language instead of forcing re-selection.",
          nextAction: "Selected locale handoff",
          nextActionBody:
            "The story editor link below carries the currently active locale so the next surface opens in the same language context.",
          fixedField: "Fixed after create",
          editableField: "Editable in the lane",
          localeStatus: "Locale status",
          storyPagesLabel: "story pages",
          coverReady: "Cover ready",
          audioReady: "Audio ready",
          mobileVisible: "Mobile visible",
          contributorOrder: "Display order",
        };

  const toolbar = useMemo(
    () => (
      <div className="grid gap-4 rounded-[1.7rem] border border-border/70 bg-muted/15 p-4 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="space-y-2">
          <p className="text-sm font-semibold tracking-tight text-foreground">
            {copy.toolbarTitle}
          </p>
          <p className="text-sm leading-6 text-muted-foreground">
            {copy.toolbarDescription}
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <MockupStatusPill tone="accent">
              {selectedLocale.statusLabel}
            </MockupStatusPill>
            <MockupStatusPill tone={selectedLocale.isPublished ? "success" : "warning"}>
              {selectedLocale.isPublished ? copy.mobileVisible : copy.localeStatus}
            </MockupStatusPill>
          </div>
        </div>
        <MockupInfoCard
          title={copy.nextAction}
          description={copy.nextActionBody}
          className="bg-background/85"
        >
          <Button asChild type="button" variant="outline">
            <Link
              to={`/labs/mockups/contents/demo-content/story-pages?language=${selectedLocale.languageCode}`}
            >
              <BookOpenText className="size-4" />
              {copy.openStoryPages}
            </Link>
          </Button>
        </MockupInfoCard>
      </div>
    ),
    [copy.nextAction, copy.nextActionBody, copy.openStoryPages, copy.toolbarDescription, copy.toolbarTitle, copy.localeStatus, copy.mobileVisible, selectedLocale.isPublished, selectedLocale.languageCode, selectedLocale.statusLabel],
  );

  return (
    <ContentPageShell
      eyebrow={copy.eyebrow}
      title={selectedLocale.title}
      description={copy.description}
      actions={
        <>
          <Button asChild type="button" variant="outline">
            <Link to="/labs/mockups/contents">{copy.backToRegistry}</Link>
          </Button>
          <Button asChild type="button">
            <Link
              to={`/labs/mockups/contents/demo-content/story-pages?language=${selectedLocale.languageCode}`}
            >
              <ArrowRight className="size-4" />
              {copy.openStoryPages}
            </Link>
          </Button>
        </>
      }
      aside={
        <TaskRail
          title={copy.railTitle}
          description={copy.railDescription}
          stats={[
            {
              label: copy.visible,
              value: `${visibleLocaleCount} / ${mockupDemoContent.locales.length}`,
              tone: visibleLocaleCount > 0 ? "success" : "warning",
            },
            {
              label: copy.processing,
              value: `${processingCompleteCount} / ${mockupDemoContent.locales.length}`,
              tone:
                processingCompleteCount === mockupDemoContent.locales.length
                  ? "success"
                  : "warning",
            },
            {
              label: copy.pages,
              value: `${mockupDemoContent.pageCount ?? 0} ${copy.storyPagesLabel}`,
            },
          ]}
        >
          <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
            <p className="font-medium text-foreground">{copy.whyItWorks}</p>
            <p>{copy.whyOne}</p>
            <p>{copy.whyTwo}</p>
            <p>{copy.whyThree}</p>
          </div>
        </TaskRail>
      }
      toolbar={toolbar}
    >
      <FormSection
        description={copy.localizationDescription}
        title={copy.localizationTitle}
      >
        <LanguageTabs
          items={mockupDemoContent.locales.map((localeState) => ({
            code: localeState.languageCode,
            label: getMockupLanguageLabel(localeState.languageCode, locale),
            meta: localeState.statusLabel,
            tone: getReadinessTone(localeState),
            description: localeState.note,
          }))}
          listLabel="Mockup content locale tabs"
          value={selectedLanguageCode}
          onValueChange={setSelectedLanguageCode}
          renderContent={(item) => {
            const localeState =
              mockupDemoContent.locales.find(
                (candidate) => candidate.languageCode === item.code,
              ) ?? mockupDemoContent.locales[0];

            return (
              <div className="grid gap-4">
                <div className="grid gap-3 md:grid-cols-3">
                  <MockupMetricCard
                    detail={localeState.statusLabel}
                    label={copy.localeStatus}
                    tone={getReadinessTone(localeState)}
                    value={localeState.title}
                  />
                  <MockupMetricCard
                    detail={localeState.note}
                    label={copy.coverReady}
                    tone={localeState.hasCover ? "success" : "warning"}
                    value={localeState.hasCover ? "Ready" : "Pending"}
                  />
                  <MockupMetricCard
                    detail={localeState.description}
                    label={copy.audioReady}
                    tone={localeState.hasAudio ? "success" : "warning"}
                    value={localeState.hasAudio ? "Ready" : "Missing"}
                  />
                </div>
                <MockupInfoCard
                  description={localeState.note}
                  title={localeState.title}
                >
                  <MockupKeyValueGrid
                    items={[
                      {
                        label: copy.mobileVisible,
                        tone: localeState.isVisibleToMobile ? "success" : "warning",
                        value: localeState.isVisibleToMobile ? "Yes" : "No",
                      },
                      {
                        label: copy.processing,
                        tone:
                          localeState.isProcessingComplete ? "success" : "warning",
                        value: localeState.isProcessingComplete
                          ? "Complete"
                          : "In progress",
                      },
                      {
                        label: copy.localeStatus,
                        tone: getReadinessTone(localeState),
                        value: localeState.statusLabel,
                      },
                    ]}
                  />
                </MockupInfoCard>
              </div>
            );
          }}
        />
      </FormSection>

      <FormSection
        description={copy.metadataDescription}
        title={copy.metadataTitle}
      >
        <MockupKeyValueGrid
          items={[
            { label: "Type", value: mockupDemoContent.typeLabel, tone: "default" },
            {
              label: copy.fixedField,
              value: "Type, initial content shape",
              tone: "accent",
            },
            {
              label: copy.editableField,
              value: "External key, age range, active state",
              tone: "default",
            },
            {
              label: "External key",
              value: mockupDemoContent.externalKey,
              tone: "default",
            },
            {
              label: "Age range",
              value: `${mockupDemoContent.ageRange}+`,
              tone: "default",
            },
            {
              label: "Story handoff",
              value: mockupDemoContent.primaryAction,
              tone: "accent",
            },
          ]}
        />
      </FormSection>

      <FormSection
        description={copy.contributorDescription}
        title={copy.contributorTitle}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {mockupDemoContent.contributorAssignments.map((assignment) => (
            <MockupInfoCard
              key={assignment.id}
              description={assignment.note}
              title={assignment.creditName}
            >
              <div className="flex flex-wrap items-center gap-2">
                <MockupStatusPill tone="default">{assignment.role}</MockupStatusPill>
                <MockupStatusPill tone="accent">
                  {getMockupLanguageLabel(assignment.languageCode, locale)}
                </MockupStatusPill>
              </div>
              <div className="rounded-[1.2rem] border border-border/70 bg-background px-4 py-3 text-sm leading-6 text-muted-foreground">
                <p className="font-medium text-foreground">{assignment.name}</p>
                <p className="mt-1 flex items-center gap-2">
                  <Link2 className="size-4 text-primary" />
                  {copy.contributorOrder}: {assignment.order}
                </p>
              </div>
            </MockupInfoCard>
          ))}
        </div>
      </FormSection>
    </ContentPageShell>
  );
}
