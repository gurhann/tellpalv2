import { LoaderCircle } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { TaskRail } from "@/components/workspace/task-rail";
import {
  WorkspaceStatusPill,
} from "@/components/workspace/workspace-primitives";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentLocalizationTabs } from "@/features/contents/components/localization-tabs";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { StoryPageEntryLink } from "@/features/contents/components/story-page-entry-link";
import { useContentDetail } from "@/features/contents/queries/use-content-detail";
import { mapContentReadToFormValues } from "@/features/contents/schema/content-schema";
import { ContentContributorPanel } from "@/features/contributors/components/content-contributor-panel";
import { useI18n } from "@/i18n/locale-provider";

export function ContentDetailRoute() {
  const { locale, t } = useI18n();
  const { contentId = "" } = useParams();
  const parsedContentId = Number(contentId);
  const hasValidContentId =
    Number.isInteger(parsedContentId) && parsedContentId > 0;
  const contentQuery = useContentDetail(
    hasValidContentId ? parsedContentId : null,
  );
  const [activeStoryLanguageCode, setActiveStoryLanguageCode] = useState<
    string | null
  >(null);
  const content = contentQuery.content;
  const storyPageLanguageCode =
    activeStoryLanguageCode ??
    content?.primaryLocalization?.languageCode ??
    null;
  const selectedLocalization =
    content?.localizations.find(
      (localization) => localization.languageCode === storyPageLanguageCode,
    ) ??
    content?.primaryLocalization ??
    null;
  const canOpenStoryPages = content?.summary.supportsStoryPages ?? false;
  const copy =
    locale === "tr"
      ? {
          detailFallbackTitle: "Icerik Detayi",
          route: "Rota",
          invalidRoute: "Gecersiz rota",
          invalidContentRoute: "Gecersiz icerik rotasi",
          invalidRouteDescription:
            "Gecerli bir detay calisma alani yuklemek icin kayittan bir icerik acin.",
          openRegistryDescription:
            "Gecerli bir icerik detay calisma alanina ulasmak icin kayittan bir icerik acin.",
          recordNotFound: "Kayit bulunamadi",
          contentNotFound: "Icerik bulunamadi",
          metadataUnavailable: "Metadata kullanilamiyor",
          metadataLoading: "Metadata yukleniyor",
          routeMissing:
            "Bu rota icerik kaydindan gecerli bir sayisal icerik kimligi bekler.",
          routeLoading:
            "Metadata, dil calisma alanlari ve contributor akislari yukleniyor.",
          routeLoaded: (externalKey: string) =>
            `${externalKey} icin bir dil secin, icerigi guncelleyin ve hazir oldugunda yayina alin.`,
          notFoundDescription:
            "Admin API bu rota icin bir icerik kaydi dondurmedi.",
          retryDescription:
            "Metadata ve dil ozetlerini geri getirmek icin detay sorgusunu yeniden deneyin.",
          detailQueryDescription:
            "Detay kabugu ozet metadata ve yerellestirme anlik goruntulerini istiyor.",
          returnToRegistry: "Icerik kaydina don",
          loadingTitle: "Icerik detayi yukleniyor",
          loadingDescription:
            "CMS bu icerik kaydi icin metadata ve yerellestirme ozetlerini istiyor.",
          contentNotFoundDescription:
            "Istenen icerik kaydi mevcut backend ortaminda bulunmuyor.",
          detailUnavailable: "Icerik detayi kullanilamiyor",
          detailUnavailableDescription:
            "Bu icerik rotasi icin henuz detay payload'i yok.",
          eyebrow: "Editoryal Cekirdek",
          selectedLocale: "Secili dil",
          contentType: "Tur",
          lifecycle: "Durum",
          openStoryPages: "Hikaye sayfalarini ac",
          storyPagesUnavailable: "Hikaye sayfalari kullanilamiyor",
          operationsSnapshot: "Hazirlik Rayi",
          operationsDescription:
            "Sag ray yalnizca yayin karari icin gereken canli durumu gosterir.",
          visibility: "Mobil hazirlik",
          processing: "Isleme hazirligi",
          storyStructure: "Hikaye sayfalari",
          readinessCount: (ready: number, total: number) =>
            `${ready}/${total} hazir`,
          storyPagesCount: (count: number) =>
            `${count} sayfa`,
          storyPagesUnused: "Kullanilmiyor",
          localeWorkspace: "Dil Calisma Alani",
          workspaceDescription:
            "Yerellestirme bu ekrandaki ana calisma alani olarak kalir; secili dil, form ve yayin aksiyonlari ayni baglamda birlikte gorunur.",
          mobileVisible: "Mobil gorunur",
          notVisible: "Gizli",
          metadataTitle: "Metadata",
          metadataDescription:
            "Temel icerik metadata'sini guncelleyin. Icerik turu olusturulduktan sonra sabittir; external key, yas araligi ve aktiflik durumu burada degistirilebilir.",
          contributorsTitle: "Contributor Atamalari",
          contributorsDescription:
            "Paylasilan contributor kayitlarini bu icerige rol, dil, gorunen kredi adi ve siralama metadatasi ile baglayin.",
        }
      : {
          detailFallbackTitle: "Content Detail",
          route: "Route",
          invalidRoute: "Invalid route",
          invalidContentRoute: "Invalid content route",
          invalidRouteDescription:
            "Open a content record from the registry to load a valid detail workspace.",
          openRegistryDescription:
            "Open a record from the content registry to reach a valid content detail workspace.",
          recordNotFound: "Record not found",
          contentNotFound: "Content not found",
          metadataUnavailable: "Metadata unavailable",
          metadataLoading: "Loading metadata",
          routeMissing:
            "This route expects a valid numeric content id from the content registry.",
          routeLoading:
            "Loading metadata, locale workspaces, and contributor workflows.",
          routeLoaded: (externalKey: string) =>
            `Choose a locale for ${externalKey}, update the record, and publish when it is ready.`,
          notFoundDescription:
            "The admin API did not return a content record for this route.",
          retryDescription:
            "Retry the detail query to restore metadata and locale summaries.",
          detailQueryDescription:
            "The detail shell is requesting summary metadata and localization snapshots.",
          returnToRegistry: "Return to content registry",
          loadingTitle: "Loading content detail",
          loadingDescription:
            "The CMS is requesting metadata and localization snapshots for this content record.",
          contentNotFoundDescription:
            "The requested content record does not exist in the current backend environment.",
          detailUnavailable: "Content detail unavailable",
          detailUnavailableDescription:
            "No detail payload is available for this content route yet.",
          eyebrow: "Editorial Core",
          selectedLocale: "Selected locale",
          contentType: "Type",
          lifecycle: "Status",
          openStoryPages: "Open story pages",
          storyPagesUnavailable: "Story pages unavailable",
          operationsSnapshot: "Readiness rail",
          operationsDescription:
            "The rail stays limited to the live checks needed before publishing.",
          visibility: "Mobile readiness",
          processing: "Processing readiness",
          storyStructure: "Story pages",
          readinessCount: (ready: number, total: number) =>
            `${ready}/${total} ready`,
          storyPagesCount: (count: number) =>
            `${count} pages`,
          storyPagesUnused: "Not used",
          localeWorkspace: "Locale workspace",
          workspaceDescription:
            "Localization remains the primary workspace on this screen, with the selected locale, form, and publication controls visible together.",
          mobileVisible: "Mobile visible",
          notVisible: "Hidden",
          metadataTitle: "Metadata",
          metadataDescription:
            "Update the base content metadata. Content type is fixed after creation, while external key, age range, and active state can be changed here.",
          contributorsTitle: "Contributor assignments",
          contributorsDescription:
            "Assign shared contributor registry entries to this content item with role, language, display credit, and ordering metadata.",
        };
  const routeTitle =
    content?.primaryLocalization?.title ??
    (hasValidContentId
      ? locale === "tr"
        ? `Icerik #${parsedContentId}`
        : `Content #${parsedContentId}`
      : copy.detailFallbackTitle);
  const routeDescription = content
    ? copy.routeLoaded(content.summary.externalKey)
    : hasValidContentId
      ? copy.routeLoading
      : copy.routeMissing;

  function renderToolbar() {
    if (content) {
      const selectedLocaleLabel =
        selectedLocalization?.languageLabel ??
        (locale === "tr" ? "Henuz secilmedi" : "Not selected");

      return (
        <div className="flex flex-col gap-3 rounded-[1.4rem] border border-border/70 bg-background/80 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                {copy.contentType}
              </span>
              <WorkspaceStatusPill tone="accent">
                {content.summary.typeLabel}
              </WorkspaceStatusPill>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                {copy.selectedLocale}
              </span>
              <WorkspaceStatusPill
                tone={
                  selectedLocalization?.isPublished
                    ? "success"
                    : selectedLocalization
                      ? "warning"
                      : "default"
                }
              >
                {selectedLocaleLabel}
              </WorkspaceStatusPill>
            </div>
          </div>

          <div className="shrink-0">
            <StoryPageEntryLink
              canOpen={canOpenStoryPages}
              contentId={parsedContentId}
              label={copy.openStoryPages}
              preferredLanguageCode={storyPageLanguageCode}
              unavailableLabel={copy.storyPagesUnavailable}
            />
          </div>
        </div>
      );
    }

    const title = !hasValidContentId
      ? copy.invalidRoute
      : contentQuery.isNotFound
        ? copy.recordNotFound
        : contentQuery.problem
          ? copy.metadataUnavailable
          : copy.metadataLoading;
    const description = !hasValidContentId
      ? copy.invalidRouteDescription
      : contentQuery.isNotFound
        ? copy.notFoundDescription
        : contentQuery.problem
          ? copy.retryDescription
          : copy.detailQueryDescription;

    return (
      <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-border/70 bg-background px-4 py-3">
        <span className="text-sm font-semibold text-foreground">{title}</span>
        <span className="text-sm text-muted-foreground">{description}</span>
        <span className="text-sm text-muted-foreground">
          {copy.route}: /contents/{contentId || "?"}
        </span>
      </div>
    );
  }

  function renderDetailContent() {
    if (!hasValidContentId) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/contents">{copy.returnToRegistry}</Link>
            </Button>
          }
          description={copy.openRegistryDescription}
          title={copy.invalidContentRoute}
        />
      );
    }

    if (contentQuery.isLoading) {
      return (
        <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
          <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-background text-primary shadow-sm ring-1 ring-border/70">
              <LoaderCircle className="size-6 animate-spin" />
            </div>
            <div className="space-y-2">
              <h2 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                {copy.loadingTitle}
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                {copy.loadingDescription}
              </p>
            </div>
          </CardContent>
        </Card>
      );
    }

    if (contentQuery.isNotFound) {
      return (
        <EmptyState
          action={
            <Button asChild type="button" variant="outline">
              <Link to="/contents">{copy.returnToRegistry}</Link>
            </Button>
          }
          description={copy.contentNotFoundDescription}
          title={copy.contentNotFound}
        />
      );
    }

    if (contentQuery.problem) {
      return (
        <ProblemAlert
          actions={
            <Button
              type="button"
              variant="outline"
              onClick={() => void contentQuery.refetch()}
            >
              {t("app.retry")}
            </Button>
          }
          problem={contentQuery.problem}
        />
      );
    }

    if (!content) {
      return (
        <EmptyState
          description={copy.detailUnavailableDescription}
          title={copy.detailUnavailable}
        />
      );
    }

    return (
      <>
        <FormSection
          contentClassName="gap-4"
          description={copy.workspaceDescription}
          title={copy.localeWorkspace}
        >
          <ContentLocalizationTabs
            content={content}
            onActiveLanguageChange={setActiveStoryLanguageCode}
          />
        </FormSection>

        <FormSection
          description={copy.metadataDescription}
          title={copy.metadataTitle}
        >
          <ContentForm
            key={`${content.summary.id}-${content.summary.externalKey}-${content.summary.ageRange}-${content.summary.active}`}
            contentId={content.summary.id}
            initialValues={mapContentReadToFormValues(content)}
            mode="update"
          />
        </FormSection>

        <FormSection
          description={copy.contributorsDescription}
          title={copy.contributorsTitle}
        >
          <ContentContributorPanel content={content} />
        </FormSection>
      </>
    );
  }

  function renderAside() {
    if (!content) {
      return null;
    }

    return (
      <TaskRail
        title={copy.operationsSnapshot}
        description={copy.operationsDescription}
        variant="detail"
        stats={[
          {
            label: copy.visibility,
            value: copy.readinessCount(
              content.visibleToMobileLocalizationCount,
              content.localizationCount,
            ),
            tone:
              content.visibleToMobileLocalizationCount > 0
                ? "success"
                : "warning",
          },
          {
            label: copy.processing,
            value: copy.readinessCount(
              content.processingCompleteLocalizationCount,
              content.localizationCount,
            ),
            tone:
              content.processingCompleteLocalizationCount ===
              content.localizationCount
                ? "success"
                : "warning",
          },
          {
            label: copy.storyStructure,
            value: content.summary.supportsStoryPages
              ? copy.storyPagesCount(content.summary.pageCount ?? 0)
              : copy.storyPagesUnused,
          },
        ]}
      />
    );
  }

  return (
    <ContentPageShell
      eyebrow={copy.eyebrow}
      title={routeTitle}
      description={routeDescription}
      toolbar={renderToolbar()}
      aside={renderAside()}
    >
      {renderDetailContent()}
    </ContentPageShell>
  );
}
