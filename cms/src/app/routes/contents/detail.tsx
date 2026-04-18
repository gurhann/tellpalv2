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
  WorkspaceInfoCard,
  WorkspaceKeyValueGrid,
  WorkspaceMetricCard,
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
            `${externalKey} icin dil hazirligini, metadata'yi ve kredi atamalarini ayni yuzeyden yonetin.`,
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
          workspaceHandoff: "Calisma alani handoff'u",
          workspaceHandoffDescription:
            "Secili dil baglami korunur; metadata, yayin durumu ve hikaye duzenleme bir sonraki adima dagilmadan ilerler.",
          selectedLocale: "Secili dil",
          releasePosture: "Yayin durusu",
          storyHandoff: "Hikaye handoff'u",
          storyHandoffReady:
            "Hikaye sayfalari secili dille dogrudan acilmaya hazir.",
          storyHandoffUnavailable:
            "Bu icerik tipi hikaye sayfasi calisma alani kullanmiyor.",
          openStoryPages: "Hikaye sayfalarini ac",
          storyPagesUnavailable: "Hikaye sayfalari kullanilamiyor",
          operationsSnapshot: "Hazirlik Rayi",
          operationsDescription:
            "Sag ray canli dil hazirligini, islem durumunu ve hikaye handoff'unu taranabilir tutar.",
          visibility: "Gorunurluk",
          processing: "Isleme",
          storyStructure: "Hikaye Yapisi",
          visibilityDescription: (visible: number, total: number) =>
            `${total} yerellestirmenin ${visible} tanesi mobilde gorunur.`,
          processingDescription: (ready: number, total: number) =>
            `${total} yerellestirmenin ${ready} tanesinin islemesi tamamlandi.`,
          storyPagesCount: (count: number) =>
            `${count} hikaye sayfasi alt rota altinda canli.`,
          storyPagesUnused: "Bu icerik turunde hikaye sayfalari kullanilmiyor.",
          localeWorkspace: "Dil Calisma Alani",
          localeNotes: "Dil Notlari",
          workspaceDescription:
            "Sekmeler yayin durumu, gorunurluk ve islem hazirligini tek yuzeyde toplar.",
          metadataGuidance:
            "Metadata alani icerik tipini sabit tutar; external key, yas araligi ve aktiflik durumu burada yonetilir.",
          publishGuidance:
            "Yayinlama ve arsivleme aksiyonlari her dil sekmesinde canli; conflict geri bildirimleri ayni baglamda gorunur.",
          storyEditorGuidance:
            "Hikaye icerikleri secili dili koruyarak sayfa editorunu dogrudan bu ekrandan acar.",
          contributorGuidance:
            "Contributor kredileri artik gercek backend atamalarini okur; bu yuzden kredi sirasi ve dil kapsami ayni review dongusunde kalir.",
          contentProfile: "Icerik Profili",
          contributorNotes: "Contributor Notlari",
          fixedAfterCreate: "Olusturma sonrasi sabit",
          editableInLane: "Bu alanda duzenlenebilir",
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
            `Review locale readiness, metadata, and credits for ${externalKey} in one editorial workspace.`,
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
          workspaceHandoff: "Workspace handoff",
          workspaceHandoffDescription:
            "The selected locale remains in context so metadata review, publication checks, and the story editor handoff stay on one surface.",
          selectedLocale: "Selected locale",
          releasePosture: "Release posture",
          storyHandoff: "Story handoff",
          storyHandoffReady:
            "Story pages are ready to open in the current language context.",
          storyHandoffUnavailable:
            "This content type does not use the story page workspace.",
          openStoryPages: "Open story pages",
          storyPagesUnavailable: "Story pages unavailable",
          operationsSnapshot: "Readiness rail",
          operationsDescription:
            "The right rail keeps locale readiness, processing, and story structure glanceable while the main lane stays focused on editorial work.",
          visibility: "Visibility",
          processing: "Processing",
          storyStructure: "Story structure",
          visibilityDescription: (visible: number, total: number) =>
            `${visible} of ${total} localizations are mobile visible.`,
          processingDescription: (ready: number, total: number) =>
            `${ready} of ${total} localizations are processing complete.`,
          storyPagesCount: (count: number) =>
            `${count} story page${count === 1 ? "" : "s"} live under the child route.`,
          storyPagesUnused: "Story pages are not used for this content type.",
          localeWorkspace: "Locale workspace",
          localeNotes: "Locale notes",
          workspaceDescription:
            "Tabs keep publication state, visibility, and processing posture in one place.",
          metadataGuidance:
            "Metadata keeps content type fixed while external key, age range, and activity remain editable here.",
          publishGuidance:
            "Publish and archive actions now live inside each language tab, including backend conflict surfaces.",
          storyEditorGuidance:
            "Story records can open their page editor directly from this screen, preserving the currently selected language.",
          contributorGuidance:
            "Contributor credits now read from persisted backend assignments, so ordering and language scope stay inside the same editorial review loop.",
          contentProfile: "Content profile",
          contributorNotes: "Contributor notes",
          fixedAfterCreate: "Fixed after create",
          editableInLane: "Editable in lane",
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
      return (
        <div className="grid gap-4 rounded-[1.7rem] border border-border/70 bg-muted/15 p-4 lg:grid-cols-[1.15fr_0.85fr]">
          <div className="grid gap-4">
            <div className="space-y-2">
              <p className="text-sm font-semibold tracking-tight text-foreground">
                {copy.workspaceHandoff}
              </p>
              <p className="text-sm leading-6 text-muted-foreground">
                {copy.workspaceHandoffDescription}
              </p>
              <div className="flex flex-wrap items-center gap-2">
                <WorkspaceStatusPill tone="accent">
                  {content.summary.typeLabel}
                </WorkspaceStatusPill>
                <WorkspaceStatusPill
                  tone={content.summary.active ? "success" : "default"}
                >
                  {content.summary.active
                    ? locale === "tr"
                      ? "Aktif"
                      : "Active"
                    : locale === "tr"
                      ? "Pasif"
                      : "Inactive"}
                </WorkspaceStatusPill>
                {selectedLocalization ? (
                  <WorkspaceStatusPill
                    tone={selectedLocalization.isPublished ? "success" : "warning"}
                  >
                    {selectedLocalization.languageLabel}:{" "}
                    {selectedLocalization.statusLabel}
                  </WorkspaceStatusPill>
                ) : null}
              </div>
            </div>
            <div className="grid gap-3 md:grid-cols-3">
              <WorkspaceMetricCard
                detail={selectedLocalization?.statusLabel}
                label={copy.selectedLocale}
                tone={
                  selectedLocalization?.isPublished
                    ? "success"
                    : selectedLocalization
                      ? "warning"
                      : "default"
                }
                value={
                  selectedLocalization?.languageLabel ??
                  (locale === "tr" ? "Henuz secilmedi" : "Not selected yet")
                }
              />
              <WorkspaceMetricCard
                detail={copy.publishGuidance}
                label={copy.releasePosture}
                tone={
                  selectedLocalization?.isPublished
                    ? "success"
                    : selectedLocalization
                      ? "warning"
                      : "default"
                }
                value={
                  selectedLocalization?.processingStatusLabel ??
                  (locale === "tr" ? "Bekliyor" : "Pending")
                }
              />
              <WorkspaceMetricCard
                detail={
                  content.summary.supportsStoryPages
                    ? copy.storyHandoffReady
                    : copy.storyHandoffUnavailable
                }
                label={copy.storyHandoff}
                tone={content.summary.supportsStoryPages ? "accent" : "default"}
                value={
                  content.summary.supportsStoryPages
                    ? selectedLocalization?.languageLabel ??
                      (locale === "tr" ? "Dil secin" : "Choose locale")
                    : locale === "tr"
                      ? "Kullanilmiyor"
                      : "Not used"
                }
              />
            </div>
          </div>
          <WorkspaceInfoCard
            title={copy.storyHandoff}
            description={copy.storyEditorGuidance}
            className="bg-background/85"
          >
            <StoryPageEntryLink
              canOpen={canOpenStoryPages}
              contentId={parsedContentId}
              label={copy.openStoryPages}
              preferredLanguageCode={storyPageLanguageCode}
              unavailableLabel={copy.storyPagesUnavailable}
            />
          </WorkspaceInfoCard>
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
          {selectedLocalization ? (
            <div className="grid gap-4">
              <WorkspaceInfoCard
                title={`${copy.selectedLocale}: ${selectedLocalization.languageLabel}`}
                description={selectedLocalization.title}
              >
                <WorkspaceKeyValueGrid
                  items={[
                    {
                      label: copy.releasePosture,
                      value: selectedLocalization.statusLabel,
                      tone: selectedLocalization.isPublished
                        ? "success"
                        : "warning",
                    },
                    {
                      label: copy.processing,
                      value: selectedLocalization.processingStatusLabel,
                      tone:
                        selectedLocalization.processingStatus === "COMPLETED"
                          ? "success"
                          : "warning",
                    },
                    {
                      label: copy.visibility,
                      value: selectedLocalization.visibleToMobile
                        ? copy.mobileVisible
                        : copy.notVisible,
                      tone: selectedLocalization.visibleToMobile
                        ? "success"
                        : "warning",
                    },
                  ]}
                />
              </WorkspaceInfoCard>
              <WorkspaceInfoCard
                title={copy.storyHandoff}
                description={
                  content.summary.supportsStoryPages
                    ? copy.storyHandoffReady
                    : copy.storyHandoffUnavailable
                }
              >
                <StoryPageEntryLink
                  canOpen={canOpenStoryPages}
                  contentId={parsedContentId}
                  label={copy.openStoryPages}
                  preferredLanguageCode={storyPageLanguageCode}
                  unavailableLabel={copy.storyPagesUnavailable}
                />
              </WorkspaceInfoCard>
            </div>
          ) : null}

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
        stats={[
          {
            label: copy.visibility,
            value: copy.visibilityDescription(
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
            value: copy.processingDescription(
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
      >
        <div className="grid gap-4">
          <WorkspaceInfoCard
            title={copy.localeNotes}
            description={copy.workspaceDescription}
            className="bg-background/80"
          >
            <p className="text-sm leading-6 text-muted-foreground">
              {copy.publishGuidance}
            </p>
          </WorkspaceInfoCard>
          <WorkspaceInfoCard
            title={copy.contentProfile}
            description={copy.metadataGuidance}
            className="bg-background/80"
          >
            <WorkspaceKeyValueGrid
              items={[
                {
                  label: copy.fixedAfterCreate,
                  value: content.summary.typeLabel,
                  tone: "accent",
                },
                {
                  label: copy.editableInLane,
                  value:
                    locale === "tr"
                      ? "External key, yas araligi, aktiflik"
                      : "External key, age range, active state",
                },
                {
                  label: copy.storyStructure,
                  value: content.summary.supportsStoryPages
                    ? copy.storyPagesCount(content.summary.pageCount ?? 0)
                    : copy.storyPagesUnused,
                  tone: content.summary.supportsStoryPages ? "accent" : "default",
                },
              ]}
            />
          </WorkspaceInfoCard>
          <WorkspaceInfoCard
            title={copy.contributorNotes}
            description={copy.contributorGuidance}
            className="bg-background/80"
          />
        </div>
      </TaskRail>
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
