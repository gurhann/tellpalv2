import { ArrowRight, FolderTree } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

import { FormSection } from "@/components/forms/form-section";
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
import { mockupDemoCategory } from "@/features/mockups/fixtures";
import { getMockupLanguageLabel, getReadinessTone } from "@/features/mockups/lib";
import {
  MockupInfoCard,
  MockupKeyValueGrid,
  MockupMetricCard,
  MockupStatusPill,
} from "@/features/mockups/components/mockup-ui";
import { useI18n } from "@/i18n/locale-provider";

export function MockupCategoryDetailRoute() {
  const { locale } = useI18n();
  const [selectedLanguageCode, setSelectedLanguageCode] = useState(
    mockupDemoCategory.locales[0]?.languageCode ?? "en",
  );
  const [isCreateLocalizationOpen, setIsCreateLocalizationOpen] = useState(false);
  const selectedLocalization =
    mockupDemoCategory.locales.find(
      (localization) => localization.languageCode === selectedLanguageCode,
    ) ?? mockupDemoCategory.locales[0];
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Variant A Mockups",
          description:
            "Variant A keeps category metadata, locale workspaces, and curation on one detail route while prerequisites remain in the rail.",
          backToRegistry: "Back to registry",
          createLocalization: "Create localization",
          metadataTitle: "Metadata lane",
          metadataDescription:
            "Category metadata remains low-noise so curation context can stay visible without competing with the form.",
          localizationTitle: "Localization workspaces",
          localizationDescription:
            "Each language workspace carries image readiness and publication state before editors enter curation ordering.",
          curationTitle: "Curation lane",
          curationDescription:
            "The main lane keeps the current curated set visible while the rail explains prerequisite logic.",
          railTitle: "Prerequisites",
          railDescription:
            "The rail keeps publication and image rules glanceable without forcing a staged wizard.",
          published: "Published locale",
          imageReady: "Image ready",
          curationItems: "Curation items",
          ruleTitle: "Why Variant A wins",
          createLocalizationTitle: "Create localization",
          createLocalizationDescription:
            "This modal previews the next step after saving base metadata: add a language workspace, then continue curation on the same route.",
          close: "Close",
          displayOrder: "Display order",
          localeStatus: "Locale status",
          openCuration: "Review curation posture",
        }
      : {
          eyebrow: "Variant A Mockups",
          description:
            "Variant A keeps category metadata, locale workspaces, and curation on one detail route while prerequisites remain in the rail.",
          backToRegistry: "Back to registry",
          createLocalization: "Create localization",
          metadataTitle: "Metadata lane",
          metadataDescription:
            "Category metadata remains low-noise so curation context can stay visible without competing with the form.",
          localizationTitle: "Localization workspaces",
          localizationDescription:
            "Each language workspace carries image readiness and publication state before editors enter curation ordering.",
          curationTitle: "Curation lane",
          curationDescription:
            "The main lane keeps the current curated set visible while the rail explains prerequisite logic.",
          railTitle: "Prerequisites",
          railDescription:
            "The rail keeps publication and image rules glanceable without forcing a staged wizard.",
          published: "Published locale",
          imageReady: "Image ready",
          curationItems: "Curation items",
          ruleTitle: "Why Variant A wins",
          createLocalizationTitle: "Create localization",
          createLocalizationDescription:
            "This modal previews the next step after saving base metadata: add a language workspace, then continue curation on the same route.",
          close: "Close",
          displayOrder: "Display order",
          localeStatus: "Locale status",
          openCuration: "Review curation posture",
        };

  return (
    <>
      <ContentPageShell
        eyebrow={copy.eyebrow}
        title={mockupDemoCategory.slug}
        description={copy.description}
        actions={
          <>
            <Button asChild type="button" variant="outline">
              <Link to="/labs/mockups/categories">{copy.backToRegistry}</Link>
            </Button>
            <Button type="button" variant="outline" onClick={() => setIsCreateLocalizationOpen(true)}>
              {copy.createLocalization}
            </Button>
            <Button type="button" variant="outline">
              <FolderTree className="size-4" />
              {copy.openCuration}
            </Button>
          </>
        }
        aside={
          <TaskRail
            title={copy.railTitle}
            description={copy.railDescription}
            stats={[
              {
                label: copy.published,
                value: mockupDemoCategory.locales
                  .filter((localization) => localization.isPublished)
                  .length.toString(),
                tone: "success",
              },
              {
                label: copy.imageReady,
                value: mockupDemoCategory.locales
                  .filter((localization) => localization.hasIllustration)
                  .length.toString(),
                tone:
                  mockupDemoCategory.locales.every(
                    (localization) => localization.hasIllustration,
                  )
                    ? "success"
                    : "warning",
              },
              {
                label: copy.curationItems,
                value: mockupDemoCategory.curationItems.length.toString(),
              },
            ]}
          >
            <div className="grid gap-3 rounded-2xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
              <p className="font-medium text-foreground">{copy.ruleTitle}</p>
              {mockupDemoCategory.guardrails.map((guardrail) => (
                <p key={guardrail}>{guardrail}</p>
              ))}
            </div>
          </TaskRail>
        }
        toolbar={
          <div className="grid gap-4 rounded-[1.7rem] border border-border/70 bg-muted/15 p-4 lg:grid-cols-3">
            <MockupMetricCard
              detail="Type is fixed after creation so curation logic stays stable."
              label="Type"
              tone="default"
              value={mockupDemoCategory.typeLabel}
            />
            <MockupMetricCard
              detail={selectedLocalization.note}
              label={copy.localeStatus}
              tone={getReadinessTone(selectedLocalization)}
              value={selectedLocalization.statusLabel}
            />
            <MockupMetricCard
              detail="The selected locale drives the visible curation note set."
              label="Locale focus"
              tone="accent"
              value={getMockupLanguageLabel(selectedLocalization.languageCode, locale)}
            />
          </div>
        }
      >
        <FormSection
          description={copy.metadataDescription}
          title={copy.metadataTitle}
        >
          <MockupKeyValueGrid
            items={[
              { label: "Slug", value: mockupDemoCategory.slug, tone: "default" },
              { label: "Type", value: mockupDemoCategory.typeLabel, tone: "accent" },
              {
                label: "Access",
                value: mockupDemoCategory.premium ? "Premium" : "Standard",
                tone: mockupDemoCategory.premium ? "warning" : "default",
              },
              {
                label: "State",
                value: mockupDemoCategory.active ? "Active" : "Inactive",
                tone: mockupDemoCategory.active ? "success" : "default",
              },
            ]}
          />
        </FormSection>

        <FormSection
          actions={
            <Button type="button" variant="outline" onClick={() => setIsCreateLocalizationOpen(true)}>
              <ArrowRight className="size-4" />
              {copy.createLocalization}
            </Button>
          }
          description={copy.localizationDescription}
          title={copy.localizationTitle}
        >
          <LanguageTabs
            items={mockupDemoCategory.locales.map((localization) => ({
              code: localization.languageCode,
              label: getMockupLanguageLabel(localization.languageCode, locale),
              meta: localization.statusLabel,
              tone: getReadinessTone(localization),
              description: localization.note,
            }))}
            listLabel="Mockup category locale tabs"
            value={selectedLanguageCode}
            onValueChange={setSelectedLanguageCode}
            renderContent={(item) => {
              const localization =
                mockupDemoCategory.locales.find(
                  (candidate) => candidate.languageCode === item.code,
                ) ?? mockupDemoCategory.locales[0];

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
                          label: copy.published,
                          tone: localization.isPublished ? "success" : "warning",
                          value: localization.isPublished ? "Yes" : "No",
                        },
                        {
                          label: copy.imageReady,
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
        </FormSection>

        <FormSection
          description={copy.curationDescription}
          title={copy.curationTitle}
        >
          <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
            {mockupDemoCategory.curationItems
              .filter(
                (item) =>
                  item.languageCode === selectedLocalization.languageCode ||
                  item.languageCode === "en",
              )
              .map((item) => (
                <MockupInfoCard
                  key={item.id}
                  description={item.reason}
                  title={item.contentTitle}
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <MockupStatusPill tone="default">{item.contentType}</MockupStatusPill>
                    <MockupStatusPill tone="accent">
                      {getMockupLanguageLabel(item.languageCode, locale)}
                    </MockupStatusPill>
                  </div>
                  <div className="rounded-[1.2rem] border border-border/70 bg-background px-4 py-3 text-sm leading-6 text-muted-foreground">
                    <p className="font-medium text-foreground">{item.statusLabel}</p>
                    <p className="mt-1">
                      {copy.displayOrder}: {item.displayOrder}
                    </p>
                  </div>
                </MockupInfoCard>
              ))}
          </div>
        </FormSection>
      </ContentPageShell>

      <Dialog
        open={isCreateLocalizationOpen}
        onOpenChange={setIsCreateLocalizationOpen}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.createLocalizationTitle}</DialogTitle>
            <DialogDescription>{copy.createLocalizationDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-4">
            <MockupInfoCard
              description="The locale workspace appears inside the same route so editors can continue directly into curation."
              title="No route switch required"
            >
              <MockupKeyValueGrid
                items={[
                  { label: "Step 1", value: "Choose language", tone: "accent" },
                  { label: "Step 2", value: "Add image + copy", tone: "default" },
                  { label: "Step 3", value: "Continue curation", tone: "success" },
                ]}
              />
            </MockupInfoCard>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsCreateLocalizationOpen(false)}>
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
