import { Layers3, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

import { EmptyState } from "@/components/feedback/empty-state";
import { ProblemAlert } from "@/components/feedback/problem-alert";
import { FormSection } from "@/components/forms/form-section";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ContentForm } from "@/features/contents/components/content-form";
import { ContentLocalizationTabs } from "@/features/contents/components/localization-tabs";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import { ContentSummaryCard } from "@/features/contents/components/content-summary-card";
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
  const canOpenStoryPages = content?.summary.supportsStoryPages ?? false;
  const copy =
    locale === "tr"
      ? {
          detailFallbackTitle: "İçerik Detayı",
          detailStatus: "Detay Durumu",
          route: "Rota",
          inlineStates:
            "Satır içi yükleme, hata ve bulunamadı durumları bu kabuk içinde kalır.",
          invalidRoute: "Geçersiz rota",
          invalidContentRoute: "Geçersiz içerik rotası",
          invalidRouteDescription:
            "Geçerli bir detay çalışma alanı yüklemek için kayıttan bir içerik açın.",
          openRegistryDescription:
            "Geçerli bir içerik detay çalışma alanına ulaşmak için kayıttan bir içerik açın.",
          recordNotFound: "Kayıt bulunamadı",
          contentNotFound: "İçerik bulunamadı",
          metadataUnavailable: "Metadata kullanılamıyor",
          metadataLoading: "Metadata yükleniyor",
          routeMissing:
            "Bu rota içerik kaydından geçerli bir sayısal içerik kimliği bekler.",
          routeLoading:
            "CMS, içerik metadatasını ve yerelleştirme özetlerini admin API üzerinden yüklüyor. Detay sorgusu çözülür çözülmez metadata düzenleme ve dil çalışma alanları açılır.",
          routeLoaded: (externalKey: string) =>
            `${externalKey} için metadata düzenleme ve yerelleştirme çalışma alanları canlı. Yayınlama ve arşivleme aksiyonları artık her dil sekmesi içinde çalışıyor.`,
          notFoundDescription:
            "Admin API bu rota için bir içerik kaydı döndürmedi.",
          retryDescription:
            "Metadata ve yerelleştirme özetlerini geri getirmek için detay sorgusunu yeniden deneyin.",
          detailQueryDescription:
            "Detay kabuğu özet metadata ve yerelleştirme anlık görüntülerini istiyor.",
          returnToRegistry: "İçerik kaydına dön",
          loadingTitle: "İçerik detayı yükleniyor",
          loadingDescription:
            "CMS bu içerik kaydı için metadata ve yerelleştirme özetlerini istiyor.",
          contentNotFoundDescription:
            "İstenen içerik kaydı mevcut backend ortamında bulunmuyor.",
          detailUnavailable: "İçerik detayı kullanılamıyor",
          detailUnavailableDescription:
            "Bu içerik rotası için henüz detay payload'ı yok.",
          eyebrow: "Editoryal Çekirdek",
          operationsSnapshot: "Operasyon Özeti",
          operationsDescription:
            "İçerik detay sorgusu ve dil bazlı mutation çalışma alanından canlı durum özeti.",
          visibility: "Görünürlük",
          processing: "İşleme",
          storyStructure: "Hikaye Yapısı",
          visibilityDescription: (visible: number, total: number) =>
            `${total} yerelleştirmenin ${visible} tanesi mobilde görünür.`,
          visibilityPending:
            "Görünürlük sayıları detay sorgusu çözüldükten sonra görünür.",
          processingDescription: (ready: number, total: number) =>
            `${total} yerelleştirmenin ${ready} tanesinin işlemesi tamamlandı.`,
          processingPending:
            "İşleme sayıları detay sorgusu çözüldükten sonra görünür.",
          storyPagesCount: (count: number) =>
            `${count} hikaye sayfası alt rota altında canlı.`,
          storyPagesUnused: "Bu içerik türünde hikaye sayfaları kullanılmıyor.",
          storyPagesPending:
            "Hikaye yapısı detay sorgusu çözüldükten sonra görünür.",
          languageControls: "Dil bazlı düzenleme ve yayın kontrolü canlı",
          workspaceGuidance: "Çalışma Alanı Rehberi",
          workspaceDescription:
            "Bu çalışma alanı metadata düzenleme, yerelleştirme kontrolü, contributor kredileri ve hikaye sayfası erişimini tek rotada birleştirir.",
          metadataGuidance:
            "Temel metadata ve dil değişiklikleri artık alan bazlı validation mapping ile admin API üzerinden kaydediliyor.",
          publishGuidance:
            "Yayınlama ve arşivleme aksiyonları artık her dil sekmesi içinde canlı; backend conflict yüzeyleri de burada görünüyor.",
          storyEditorGuidance:
            "Hikaye kayıtları seçili dili koruyarak sayfa editörünü doğrudan bu ekrandan açabilir.",
          metadataTitle: "Metadata",
          metadataDescription:
            "Temel içerik metadata'sını güncelleyin. İçerik türü oluşturulduktan sonra sabittir; external key, age range ve aktiflik durumu ise burada değiştirilebilir.",
          contributorsTitle: "Katkı sağlayan atamaları",
          contributorsDescription:
            "Paylaşılan contributor kayıtlarını bu içeriğe rol, dil, görünen kredi adı ve sıralama metadatası ile atayın. Mevcut backend atamaları henüz okunamadığı için panel yalnızca bu oturumdaki atamaları izler.",
        }
      : {
          detailFallbackTitle: "Content Detail",
          detailStatus: "Detail Status",
          route: "Route",
          inlineStates:
            "Inline loading, error, and not-found states stay inside this shell.",
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
            "The CMS is loading content metadata and localization snapshots from the admin API. Metadata editing and locale workspaces become available as soon as the detail query resolves.",
          routeLoaded: (externalKey: string) =>
            `Metadata editing and localization workspaces are live for ${externalKey}. Publish and archive actions now run inside each language tab.`,
          notFoundDescription:
            "The admin API did not return a content record for this route.",
          retryDescription:
            "Retry the detail query to restore metadata and localization summaries.",
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
          operationsSnapshot: "Operations Snapshot",
          operationsDescription:
            "Live status summary from the content detail query and per-language mutation workspace.",
          visibility: "Visibility",
          processing: "Processing",
          storyStructure: "Story structure",
          visibilityDescription: (visible: number, total: number) =>
            `${visible} of ${total} localizations are mobile visible.`,
          visibilityPending:
            "Visibility counts appear after the detail query resolves.",
          processingDescription: (ready: number, total: number) =>
            `${ready} of ${total} localizations are processing complete.`,
          processingPending:
            "Processing counts appear after the detail query resolves.",
          storyPagesCount: (count: number) =>
            `${count} story page${count === 1 ? "" : "s"} live under the child route.`,
          storyPagesUnused: "Story pages are not used for this content type.",
          storyPagesPending:
            "Story structure appears after the detail query resolves.",
          languageControls: "Per-language edit and publication controls live",
          workspaceGuidance: "Workspace Guidance",
          workspaceDescription:
            "This workspace combines metadata editing, localization control, contributor credits, and story-page access in one route.",
          metadataGuidance:
            "Base metadata and locale changes now save through the admin API with field-level validation mapping.",
          publishGuidance:
            "Publish and archive actions now live inside each language tab, including backend conflict surfaces.",
          storyEditorGuidance:
            "Story records can open their page editor directly from this screen, preserving the currently selected language.",
          metadataTitle: "Metadata",
          metadataDescription:
            "Update the base content metadata. Content type is fixed after creation, while external key, age range, and active state can be changed here.",
          contributorsTitle: "Contributor assignments",
          contributorsDescription:
            "Assign shared contributor registry entries to this content item with role, language, display credit, and ordering metadata. Existing backend assignments are not readable yet, so the panel tracks current-session assignments.",
        };
  const routeTitle =
    content?.primaryLocalization?.title ??
    (hasValidContentId
      ? locale === "tr"
        ? `İçerik #${parsedContentId}`
        : `Content #${parsedContentId}`
      : copy.detailFallbackTitle);
  const routeDescription = content
    ? copy.routeLoaded(content.summary.externalKey)
    : hasValidContentId
      ? copy.routeLoading
      : copy.routeMissing;

  function renderToolbar() {
    if (content) {
      return <ContentSummaryCard content={content} />;
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
      <div className="grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3 md:col-span-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            {copy.detailStatus}
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">{title}</p>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
        <div className="rounded-2xl border border-border/70 bg-background px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            {copy.route}
          </p>
          <p className="mt-2 text-sm font-medium text-foreground">
            /contents/{contentId || "?"}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {copy.inlineStates}
          </p>
        </div>
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
      <ContentLocalizationTabs
        content={content}
        onActiveLanguageChange={setActiveStoryLanguageCode}
      />
    );
  }

  return (
    <ContentPageShell
      eyebrow={copy.eyebrow}
      title={routeTitle}
      description={routeDescription}
      actions={
        <StoryPageEntryLink
          canOpen={canOpenStoryPages}
          contentId={parsedContentId}
          preferredLanguageCode={storyPageLanguageCode}
        />
      }
      toolbar={renderToolbar()}
      aside={
        <>
          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>{copy.operationsSnapshot}</CardTitle>
              <CardDescription>{copy.operationsDescription}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  {copy.visibility}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? copy.visibilityDescription(
                        content.visibleToMobileLocalizationCount,
                        content.localizationCount,
                      )
                    : copy.visibilityPending}
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  {copy.processing}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? copy.processingDescription(
                        content.processingCompleteLocalizationCount,
                        content.localizationCount,
                      )
                    : copy.processingPending}
                </p>
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3">
                <p className="text-sm font-medium text-foreground">
                  {copy.storyStructure}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {content
                    ? content.summary.supportsStoryPages
                      ? copy.storyPagesCount(content.summary.pageCount ?? 0)
                      : copy.storyPagesUnused
                    : copy.storyPagesPending}
                </p>
              </div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/8 px-3 py-1 text-xs font-medium text-primary">
                <Layers3 className="size-3.5" />
                {copy.languageControls}
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 bg-card/95 shadow-lg shadow-slate-950/5">
            <CardHeader>
              <CardTitle>{copy.workspaceGuidance}</CardTitle>
              <CardDescription>{copy.workspaceDescription}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                {copy.metadataGuidance}
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                {copy.publishGuidance}
              </div>
              <div className="rounded-2xl border border-border/70 bg-muted/30 px-4 py-3">
                {copy.storyEditorGuidance}
              </div>
            </CardContent>
          </Card>
        </>
      }
    >
      {renderDetailContent()}

      {content ? (
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
      ) : null}

      {content ? (
        <FormSection
          description={copy.contributorsDescription}
          title={copy.contributorsTitle}
        >
          <ContentContributorPanel content={content} />
        </FormSection>
      ) : null}
    </ContentPageShell>
  );
}
