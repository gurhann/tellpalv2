import { ArrowRight, FileAudio, FileImage, Link2, Upload } from "lucide-react";
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
import { mockupDemoContent } from "@/features/mockups/fixtures";
import { mockupStoryPages } from "@/features/mockups/fixtures";
import {
  countProcessingComplete,
  countVisibleLocales,
  getMockupLanguageLabel,
  getReadinessTone,
} from "@/features/mockups/lib";
import {
  MockupKeyValueGrid,
  MockupStatusPill,
} from "@/features/mockups/components/mockup-ui";
import { useI18n } from "@/i18n/locale-provider";

type AssetDialogTarget =
  | { kind: "localized-cover" | "localized-audio" }
  | { kind: "source-cover" }
  | { kind: "page-source"; pageNumber: number }
  | null;

export function MockupContentDetailRoute() {
  const { locale } = useI18n();
  const [selectedLanguageCode, setSelectedLanguageCode] = useState(
    mockupDemoContent.locales[0]?.languageCode ?? "en",
  );
  const [assetDialogTarget, setAssetDialogTarget] =
    useState<AssetDialogTarget>(null);
  const [localeAssetOverrides, setLocaleAssetOverrides] = useState<
    Record<string, { cover?: boolean; audio?: boolean }>
  >({});
  const [sourceCoverLinked, setSourceCoverLinked] = useState(
    mockupDemoContent.hasTextlessCover ?? false,
  );
  const [sourcePageOverrides, setSourcePageOverrides] = useState<
    Record<string, boolean>
  >({});
  const selectedLocale =
    mockupDemoContent.locales.find(
      (localeState) => localeState.languageCode === selectedLanguageCode,
    ) ?? mockupDemoContent.locales[0];
  const visibleLocaleCount = countVisibleLocales(mockupDemoContent.locales);
  const processingCompleteCount = countProcessingComplete(
    mockupDemoContent.locales,
  );
  const copy =
    locale === "tr"
      ? {
          eyebrow: "İçerik detayı",
          backToRegistry: "Kayıt listesine dön",
          openStoryPages: "Hikâye sayfa mockup’ını aç",
          description:
            "Seçili dili düzenleyin; ortak metadata’yı ve yayın bağlamını aynı çalışma alanında takip edin.",
          localizationTitle: "Dil çalışma alanı",
          localizationDescription:
            "Bir dili seçtiğinizde o dile ait içerik, asset ve yayın durumu burada görünür.",
          metadataTitle: "İçerik metadata’sı",
          metadataDescription:
            "Tüm diller tarafından paylaşılan içerik seviyesindeki alanlar.",
          contributorTitle: "Contributor atamaları",
          contributorDescription:
            "İçeriğin kredi ve rol atamalarını tek listede yönetin.",
          railTitle: "Operasyon özeti",
          railDescription: "Yayın kararını destekleyen kısa göstergeler.",
          visible: "Mobilde görünür",
          processing: "İşleme tamamlandı",
          pages: "Hikâye sayfaları",
          storyPagesLabel: "sayfa",
          mobileVisible: "Mobil görünürlük",
          contributorOrder: "Sıra",
          ready: "Hazır",
          pending: "Bekliyor",
          yes: "Evet",
          no: "Hayır",
          complete: "Tamamlandı",
          inProgress: "Devam ediyor",
          type: "Tür",
          externalKey: "External key",
          ageRange: "Yaş aralığı",
          localizedCover: "Yerelleştirilmiş kapak",
          localizedAudio: "Yerelleştirme sesi",
          manageAsset: "Asset yönet",
          addAsset: "Asset ekle",
          sourceImagesTitle: "Kaynak görseller",
          sourceImagesDescription:
            "Dil bağımsız yazısız kapak ve sayfa görsellerini bu içerik akışından yönetin.",
          sourceCoverTitle: "Yazısız kaynak kapak",
          sourceCoverLinked: "Kaynak kapak bağlı",
          sourceCoverMissing: "Kaynak kapak eksik",
          pageSourcesTitle: "Yazısız sayfa görselleri",
          pageSourcesDescription:
            "Çeviri ve illüstrasyon handoff’u için sayfa kaynaklarını kontrol edin.",
          sourcePagesLinked: "sayfa kaynağı bağlı",
          editSource: "Kaynağı düzenle",
          assetDialogTitle: "Asset seç veya yükle",
          assetDialogDescription:
            "Bu mockup, mevcut asset seçme ve yeni dosya yükleme aksiyonlarının içerik detayında nasıl konumlanacağını gösterir.",
          chooseExisting: "Mevcut asset seç",
          uploadNewImage: "Yeni görsel yükle",
          uploadNewAudio: "Yeni ses yükle",
          close: "Kapat",
        }
      : {
          eyebrow: "Content detail",
          backToRegistry: "Back to registry",
          openStoryPages: "Open story page mockup",
          description:
            "Edit the selected locale while keeping shared metadata and publication context in one workspace.",
          localizationTitle: "Locale workspace",
          localizationDescription:
            "Select a locale to review its content, assets, and publication state.",
          metadataTitle: "Content metadata",
          metadataDescription:
            "Content-level fields shared across all locales.",
          contributorTitle: "Contributor assignments",
          contributorDescription:
            "Review this content’s credits and role assignments in one compact list.",
          railTitle: "Operational summary",
          railDescription:
            "A short set of live indicators for publication decisions.",
          visible: "Mobile visibility",
          processing: "Processing complete",
          pages: "Story pages",
          storyPagesLabel: "pages",
          mobileVisible: "Mobile visibility",
          contributorOrder: "Order",
          ready: "Ready",
          pending: "Pending",
          yes: "Yes",
          no: "No",
          complete: "Complete",
          inProgress: "In progress",
          type: "Type",
          externalKey: "External key",
          ageRange: "Age range",
          localizedCover: "Localized cover",
          localizedAudio: "Localized audio",
          manageAsset: "Manage asset",
          addAsset: "Add asset",
          sourceImagesTitle: "Source images",
          sourceImagesDescription:
            "Manage the language-independent textless cover and page images from this content flow.",
          sourceCoverTitle: "Textless/source cover",
          sourceCoverLinked: "Source cover linked",
          sourceCoverMissing: "Source cover missing",
          pageSourcesTitle: "Textless page illustrations",
          pageSourcesDescription:
            "Review page-level sources for translation and illustration handoff.",
          sourcePagesLinked: "page sources linked",
          editSource: "Edit source",
          assetDialogTitle: "Choose or upload an asset",
          assetDialogDescription:
            "This mockup shows where existing-asset selection and new-file upload belong in the content detail flow.",
          chooseExisting: "Choose existing asset",
          uploadNewImage: "Upload new image",
          uploadNewAudio: "Upload new audio",
          close: "Close",
        };

  const selectedLocaleAssets = localeAssetOverrides[selectedLanguageCode] ?? {};
  const selectedHasCover =
    selectedLocaleAssets.cover ?? selectedLocale.hasCover ?? false;
  const selectedHasAudio =
    selectedLocaleAssets.audio ?? selectedLocale.hasAudio ?? false;
  const sourcePageCount = mockupStoryPages.filter(
    (page) => sourcePageOverrides[page.id] ?? page.hasTextlessSource ?? false,
  ).length;

  function handleMockupAssetSelection() {
    if (!assetDialogTarget) {
      return;
    }

    if (assetDialogTarget.kind === "localized-cover") {
      setLocaleAssetOverrides((current) => ({
        ...current,
        [selectedLanguageCode]: {
          ...current[selectedLanguageCode],
          cover: true,
        },
      }));
    } else if (assetDialogTarget.kind === "localized-audio") {
      setLocaleAssetOverrides((current) => ({
        ...current,
        [selectedLanguageCode]: {
          ...current[selectedLanguageCode],
          audio: true,
        },
      }));
    } else if (assetDialogTarget.kind === "source-cover") {
      setSourceCoverLinked(true);
    } else if (assetDialogTarget.kind === "page-source") {
      const page = mockupStoryPages.find(
        (candidate) => candidate.pageNumber === assetDialogTarget.pageNumber,
      );

      if (page) {
        setSourcePageOverrides((current) => ({
          ...current,
          [page.id]: true,
        }));
      }
    }

    setAssetDialogTarget(null);
  }

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
          variant="detail"
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
        />
      }
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
          }))}
          listLabel="Content locale workspaces"
          value={selectedLanguageCode}
          onValueChange={setSelectedLanguageCode}
          renderContent={(item) => {
            const localeState =
              mockupDemoContent.locales.find(
                (candidate) => candidate.languageCode === item.code,
              ) ?? mockupDemoContent.locales[0];

            return (
              <div className="grid gap-4">
                <div className="flex flex-col gap-3 rounded-2xl border border-border/70 bg-card/70 p-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0 space-y-1">
                    <h3 className="font-heading text-base font-semibold tracking-tight text-foreground">
                      {localeState.title}
                    </h3>
                    <p className="text-sm leading-6 text-muted-foreground">
                      {localeState.description}
                    </p>
                  </div>
                  <MockupStatusPill tone={getReadinessTone(localeState)}>
                    {localeState.statusLabel}
                  </MockupStatusPill>
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <div className="rounded-2xl border border-border/70 bg-background/80 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex min-w-0 items-start gap-3">
                        <FileImage className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0 space-y-1">
                          <p className="text-sm font-medium text-foreground">
                            {copy.localizedCover}
                          </p>
                          <p className="text-xs leading-5 text-muted-foreground">
                            {selectedHasCover ? copy.ready : copy.pending}
                          </p>
                        </div>
                      </div>
                      <MockupStatusPill
                        tone={selectedHasCover ? "success" : "warning"}
                      >
                        {selectedHasCover ? copy.ready : copy.pending}
                      </MockupStatusPill>
                    </div>
                    <Button
                      className="mt-3"
                      size="sm"
                      type="button"
                      variant="outline"
                      onClick={() =>
                        setAssetDialogTarget({ kind: "localized-cover" })
                      }
                    >
                      <Upload className="size-4" />
                      {selectedHasCover ? copy.manageAsset : copy.addAsset}
                    </Button>
                  </div>

                  <div className="rounded-2xl border border-border/70 bg-background/80 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex min-w-0 items-start gap-3">
                        <FileAudio className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0 space-y-1">
                          <p className="text-sm font-medium text-foreground">
                            {copy.localizedAudio}
                          </p>
                          <p className="text-xs leading-5 text-muted-foreground">
                            {selectedHasAudio ? copy.ready : copy.pending}
                          </p>
                        </div>
                      </div>
                      <MockupStatusPill
                        tone={selectedHasAudio ? "success" : "warning"}
                      >
                        {selectedHasAudio ? copy.ready : copy.pending}
                      </MockupStatusPill>
                    </div>
                    <Button
                      className="mt-3"
                      size="sm"
                      type="button"
                      variant="outline"
                      onClick={() =>
                        setAssetDialogTarget({ kind: "localized-audio" })
                      }
                    >
                      <Upload className="size-4" />
                      {selectedHasAudio ? copy.manageAsset : copy.addAsset}
                    </Button>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 border-t border-border/60 pt-3">
                  <MockupStatusPill
                    tone={localeState.isVisibleToMobile ? "success" : "warning"}
                  >
                    {copy.mobileVisible}:{" "}
                    {localeState.isVisibleToMobile ? copy.yes : copy.no}
                  </MockupStatusPill>
                  <MockupStatusPill
                    tone={
                      localeState.isProcessingComplete ? "success" : "warning"
                    }
                  >
                    {copy.processing}:{" "}
                    {localeState.isProcessingComplete
                      ? copy.complete
                      : copy.inProgress}
                  </MockupStatusPill>
                </div>
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
            {
              label: copy.type,
              value: mockupDemoContent.typeLabel,
              tone: "default",
            },
            {
              label: copy.externalKey,
              value: mockupDemoContent.externalKey,
              tone: "default",
            },
            {
              label: copy.ageRange,
              value: `${mockupDemoContent.ageRange}+`,
              tone: "default",
            },
          ]}
        />
      </FormSection>

      <FormSection
        description={copy.contributorDescription}
        title={copy.contributorTitle}
      >
        <div className="divide-y overflow-hidden rounded-2xl border border-border/70 bg-card/70">
          {mockupDemoContent.contributorAssignments.map((assignment) => (
            <div
              key={assignment.id}
              className="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <p className="font-medium text-foreground">
                  {assignment.creditName}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {assignment.name}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                <MockupStatusPill tone="default">
                  {assignment.role}
                </MockupStatusPill>
                <MockupStatusPill tone="accent">
                  {getMockupLanguageLabel(assignment.languageCode, locale)}
                </MockupStatusPill>
                <span className="inline-flex items-center gap-1">
                  <Link2 className="size-4" />
                  {copy.contributorOrder}: {assignment.order}
                </span>
              </div>
            </div>
          ))}
        </div>
      </FormSection>

      <FormSection
        description={copy.sourceImagesDescription}
        title={copy.sourceImagesTitle}
      >
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-2xl border border-border/70 bg-card/70 p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 items-start gap-3">
                <FileImage className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div className="min-w-0 space-y-1">
                  <h3 className="text-sm font-semibold text-foreground">
                    {copy.sourceCoverTitle}
                  </h3>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {sourceCoverLinked
                      ? copy.sourceCoverLinked
                      : copy.sourceCoverMissing}
                  </p>
                </div>
              </div>
              <MockupStatusPill
                tone={sourceCoverLinked ? "success" : "warning"}
              >
                {sourceCoverLinked ? copy.ready : copy.pending}
              </MockupStatusPill>
            </div>
            <Button
              className="mt-4"
              size="sm"
              type="button"
              variant="outline"
              onClick={() => setAssetDialogTarget({ kind: "source-cover" })}
            >
              <Upload className="size-4" />
              {copy.manageAsset}
            </Button>
          </div>

          <div className="rounded-2xl border border-border/70 bg-card/70 p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 items-start gap-3">
                <FileImage className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                <div className="min-w-0 space-y-1">
                  <h3 className="text-sm font-semibold text-foreground">
                    {copy.pageSourcesTitle}
                  </h3>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {sourcePageCount} / {mockupStoryPages.length}{" "}
                    {copy.sourcePagesLinked}
                  </p>
                </div>
              </div>
              <MockupStatusPill
                tone={
                  sourcePageCount === mockupStoryPages.length
                    ? "success"
                    : "warning"
                }
              >
                {sourcePageCount} / {mockupStoryPages.length}
              </MockupStatusPill>
            </div>
            <p className="mt-3 text-xs leading-5 text-muted-foreground">
              {copy.pageSourcesDescription}
            </p>
            <div className="mt-4 divide-y rounded-xl border border-border/70 bg-background/70">
              {mockupStoryPages.map((page) => {
                const isLinked =
                  sourcePageOverrides[page.id] ??
                  page.hasTextlessSource ??
                  false;

                return (
                  <div
                    key={page.id}
                    className="flex items-center justify-between gap-3 px-3 py-3"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-foreground">
                        Page {page.pageNumber}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        {isLinked ? copy.ready : copy.pending}
                      </p>
                    </div>
                    <Button
                      size="sm"
                      type="button"
                      variant="ghost"
                      onClick={() =>
                        setAssetDialogTarget({
                          kind: "page-source",
                          pageNumber: page.pageNumber,
                        })
                      }
                    >
                      {copy.editSource}
                    </Button>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </FormSection>

      <Dialog
        open={assetDialogTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setAssetDialogTarget(null);
          }
        }}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.assetDialogTitle}</DialogTitle>
            <DialogDescription>{copy.assetDialogDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={handleMockupAssetSelection}
            >
              {assetDialogTarget?.kind === "localized-audio"
                ? copy.uploadNewAudio
                : copy.uploadNewImage}
            </Button>
            <Button type="button" onClick={handleMockupAssetSelection}>
              {copy.chooseExisting}
            </Button>
          </DialogBody>
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => setAssetDialogTarget(null)}
            >
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ContentPageShell>
  );
}
