import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  FileAudio,
  FileImage,
  Link2,
  Pause,
  Pencil,
  Play,
  Plus,
  Trash2,
  Upload,
  UserPlus,
} from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { LanguageTabs } from "@/components/language/language-tabs";
import { TaskRail } from "@/components/workspace/task-rail";
import { ContentPageShell } from "@/features/contents/components/content-page-shell";
import {
  mockupDemoContent,
  mockupReferenceLullaby,
  mockupReferenceMeditation,
  mockupStoryPages,
} from "@/features/mockups/fixtures";
import type { MockupContentSummary } from "@/features/mockups/types";
import {
  countProcessingComplete,
  countVisibleLocales,
  getMockupLanguageLabel,
  getReadinessTone,
} from "@/features/mockups/lib";
import { MockupStatusPill } from "@/features/mockups/components/mockup-ui";
import { useI18n } from "@/i18n/locale-provider";

type AssetDialogTarget =
  | { kind: "localized-cover" | "story-narration" | "listening-cover" }
  | { kind: "source-cover" }
  | { kind: "page-source"; pageNumber: number }
  | null;

type MockupLocaleDraft = {
  title: string;
  description: string;
  durationMinutes: string;
  statusLabel: string;
};

function MockupAssetPreview({
  kind,
  ready,
}: {
  kind: "image" | "audio";
  ready: boolean;
}) {
  if (!ready) {
    return (
      <div className="flex h-10 items-center rounded-lg border border-dashed border-border/70 bg-muted/20 px-3 text-xs text-muted-foreground">
        No asset selected
      </div>
    );
  }

  if (kind === "image") {
    return (
      <div
        aria-label="Compact image asset preview"
        className="relative h-16 w-12 shrink-0 overflow-hidden rounded-lg border border-border/70 bg-[#d8ecf2]"
      >
        <div className="absolute inset-x-1.5 bottom-1.5 h-6 rounded-md bg-white/85" />
        <div className="absolute left-2 top-2 size-2.5 rounded-full bg-[#f6c94c]" />
        <div className="absolute bottom-3 left-2.5 h-2 w-7 rounded-full bg-[#f4d8e7]" />
      </div>
    );
  }

  return (
    <div className="flex h-10 min-w-44 items-center gap-2 rounded-lg border border-border/70 bg-muted/20 px-2.5">
      <span className="grid size-6 shrink-0 place-items-center rounded-full bg-foreground text-background">
        <Play className="size-3 fill-current" />
      </span>
      <span className="flex h-5 flex-1 items-center gap-0.5" aria-hidden="true">
        {[3, 5, 8, 4, 7, 10, 6, 4, 8, 5, 3, 7].map((height, index) => (
          <span
            key={index}
            className="w-1 rounded-full bg-emerald-500/70"
            style={{ height: `${height * 2}px` }}
          />
        ))}
      </span>
      <span className="text-[0.7rem] tabular-nums text-muted-foreground">0:00</span>
    </div>
  );
}

function MockupStoryContentDetail() {
  const { locale } = useI18n();
  const [selectedLanguageCode, setSelectedLanguageCode] = useState(
    mockupDemoContent.locales[0]?.languageCode ?? "en",
  );
  const [assetDialogTarget, setAssetDialogTarget] =
    useState<AssetDialogTarget>(null);
  const [localeAssetOverrides, setLocaleAssetOverrides] = useState<
    Record<string, { cover?: boolean; narration?: boolean }>
  >({});
  const [listeningCoverLinked, setListeningCoverLinked] = useState(
    mockupDemoContent.hasListeningCover ?? false,
  );
  const [localeDrafts, setLocaleDrafts] = useState<
    Record<
      string,
      {
        title: string;
        description: string;
        durationMinutes: string;
        statusLabel: string;
      }
    >
  >({});
  const [savedLocaleCode, setSavedLocaleCode] = useState<string | null>(null);
  const [metadata, setMetadata] = useState({
    externalKey: mockupDemoContent.externalKey,
    ageRange: String(mockupDemoContent.ageRange),
    active: mockupDemoContent.active,
  });
  const [metadataSaved, setMetadataSaved] = useState(false);
  const [contributors, setContributors] = useState(
    mockupDemoContent.contributorAssignments,
  );
  const [contributorDialogTarget, setContributorDialogTarget] = useState<
    string | null
  >(null);
  const [localizationDialogOpen, setLocalizationDialogOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewPageNumber, setPreviewPageNumber] = useState(1);
  const [isPreviewPlaying, setIsPreviewPlaying] = useState(false);
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
          previewStory: "Hikâyeyi önizle",
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
          storyNarration: "Hikâye anlatımı",
          pageAudioNote: "Sayfa sesleri Hikâye sayfaları çalışma alanında yönetilir.",
          listeningCover: "Ortak dinleme kapağı",
          listeningCoverDescription: "Tüm dillerin paylaştığı dinleme deneyimi kapağı.",
          saveLocale: "Dil değişikliklerini kaydet",
          saved: "Kaydedildi",
          title: "Başlık",
          summary: "Açıklama",
          duration: "Süre (dakika)",
          status: "Durum",
          publishLocale: "Dili yayınla",
          archiveLocale: "Dili arşivle",
          addLocalization: "Dil ekle",
          addLocalizationTitle: "Yeni dil yerelleştirmesi",
          addLocalizationDescription: "Yeni yerelleştirme akışı burada başlatılır; desteklenen dil seçimi uygulama adımında açılır.",
          metadataSave: "Ortak metadata’yı kaydet",
          active: "Aktif",
          edit: "Düzenle",
          assignContributor: "Contributor ata",
          reorderUp: "Yukarı taşı",
          reorderDown: "Aşağı taşı",
          unassign: "Atamayı kaldır",
          contributorDialogTitle: "Contributor ataması",
          contributorDialogDescription: "Contributor seçimi ve rol/dil kapsamı mevcut contributor akışından tamamlanır.",
          previewTitle: "Hikâye önizlemesi",
          previousPage: "Önceki sayfa",
          nextPage: "Sonraki sayfa",
          play: "Oynat",
          pause: "Duraklat",
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
          previewStory: "Preview story",
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
          storyNarration: "Story narration",
          pageAudioNote: "Page audio is managed in the Story pages workspace.",
          listeningCover: "Shared listening cover",
          listeningCoverDescription: "The listening experience cover shared across locales.",
          saveLocale: "Save locale changes",
          saved: "Saved",
          title: "Title",
          summary: "Description",
          duration: "Duration (minutes)",
          status: "Status",
          publishLocale: "Publish locale",
          archiveLocale: "Archive locale",
          addLocalization: "Add locale",
          addLocalizationTitle: "New localization",
          addLocalizationDescription: "The localization flow starts here; supported language selection continues in the implementation step.",
          metadataSave: "Save shared metadata",
          active: "Active",
          edit: "Edit",
          assignContributor: "Assign contributor",
          reorderUp: "Move up",
          reorderDown: "Move down",
          unassign: "Unassign",
          contributorDialogTitle: "Contributor assignment",
          contributorDialogDescription: "Contributor selection and role/language scope continue in the existing contributor flow.",
          previewTitle: "Story preview",
          previousPage: "Previous page",
          nextPage: "Next page",
          play: "Play",
          pause: "Pause",
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
    selectedLocaleAssets.narration ?? selectedLocale.hasAudio ?? false;
  const selectedLocaleDraft =
    localeDrafts[selectedLanguageCode] ?? {
      title: selectedLocale.title,
      description: selectedLocale.description,
      durationMinutes: String(selectedLocale.durationMinutes ?? ""),
      statusLabel: selectedLocale.statusLabel,
    };
  const previewPage =
    mockupStoryPages.find((page) => page.pageNumber === previewPageNumber) ??
    mockupStoryPages[0];
  const previewLocalization = previewPage?.localizations.find(
    (pageLocale) => pageLocale.languageCode === selectedLanguageCode,
  ) ?? previewPage?.localizations[0];
  const contributorDialogAssignment = contributors.find(
    (assignment) => assignment.id === contributorDialogTarget,
  );
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
    } else if (assetDialogTarget.kind === "story-narration") {
      setLocaleAssetOverrides((current) => ({
        ...current,
        [selectedLanguageCode]: {
          ...current[selectedLanguageCode],
          narration: true,
        },
      }));
    } else if (assetDialogTarget.kind === "listening-cover") {
      setListeningCoverLinked(true);
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

  function updateLocaleDraft(
    field: keyof (typeof selectedLocaleDraft),
    value: string | boolean,
  ) {
    setLocaleDrafts((current) => ({
      ...current,
      [selectedLanguageCode]: {
        ...selectedLocaleDraft,
        [field]: value,
      },
    }));
    setSavedLocaleCode(null);
  }

  function selectLanguage(languageCode: string) {
    setSelectedLanguageCode(languageCode);
    setPreviewPageNumber(1);
    setIsPreviewPlaying(false);
  }

  function moveContributor(id: string, direction: "up" | "down") {
    setContributors((current) => {
      const index = current.findIndex((assignment) => assignment.id === id);
      const nextIndex = direction === "up" ? index - 1 : index + 1;
      if (index < 0 || nextIndex < 0 || nextIndex >= current.length) {
        return current;
      }
      const next = [...current];
      [next[index], next[nextIndex]] = [next[nextIndex], next[index]];
      return next.map((assignment, assignmentIndex) => ({
        ...assignment,
        order: assignmentIndex + 1,
      }));
    });
  }

  const sharedMetadataSection = (
    <FormSection title={copy.metadataTitle}>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-border/70 bg-background/80 p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex min-w-0 items-start gap-3">
              <FileImage className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
              <div className="min-w-0 space-y-1">
                <h3 className="text-sm font-semibold text-foreground">
                  {copy.listeningCover}
                </h3>
                <p className="text-sm leading-6 text-muted-foreground">
                  {copy.listeningCoverDescription}
                </p>
              </div>
            </div>
            <MockupStatusPill tone={listeningCoverLinked ? "success" : "warning"}>
              {listeningCoverLinked ? copy.ready : copy.pending}
            </MockupStatusPill>
          </div>
          <Button
            className="mt-4"
            size="sm"
            type="button"
            variant="outline"
            onClick={() => setAssetDialogTarget({ kind: "listening-cover" })}
          >
            <Upload className="size-4" />
            {listeningCoverLinked ? copy.manageAsset : copy.addAsset}
          </Button>
        </div>

        <div className="grid gap-3 rounded-2xl border border-border/70 bg-background/80 p-4 sm:grid-cols-2">
          <div className="grid gap-1.5 text-sm font-medium text-foreground sm:col-span-2">
            {copy.type}
            <MockupStatusPill tone="default">{mockupDemoContent.typeLabel}</MockupStatusPill>
          </div>
          <label className="grid gap-1.5 text-sm font-medium text-foreground">
            {copy.externalKey}
            <Input
              value={metadata.externalKey}
              onChange={(event) => {
                setMetadataSaved(false);
                setMetadata((current) => ({
                  ...current,
                  externalKey: event.target.value,
                }));
              }}
            />
          </label>
          <label className="grid gap-1.5 text-sm font-medium text-foreground">
            {copy.ageRange}
            <Input
              min="0"
              type="number"
              value={metadata.ageRange}
              onChange={(event) => {
                setMetadataSaved(false);
                setMetadata((current) => ({
                  ...current,
                  ageRange: event.target.value,
                }));
              }}
            />
          </label>
          <div className="flex items-center justify-between gap-3 sm:col-span-2">
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                setMetadataSaved(false);
                setMetadata((current) => ({ ...current, active: !current.active }));
              }}
            >
              {copy.active}: {metadata.active ? copy.yes : copy.no}
            </Button>
            <Button type="button" size="sm" onClick={() => setMetadataSaved(true)}>
              {metadataSaved ? copy.saved : copy.metadataSave}
            </Button>
          </div>
        </div>
      </div>
    </FormSection>
  );

  return (
    <ContentPageShell
      eyebrow={copy.eyebrow}
      title={selectedLocaleDraft.title}
      description={copy.description}
      showHeader={false}
      aside={
        <TaskRail
          title={copy.railTitle}
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
      {sharedMetadataSection}

      <FormSection
        actions={
          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            <Button asChild type="button" size="sm" variant="outline">
              <Link to="/labs/mockups/contents">{copy.backToRegistry}</Link>
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                setPreviewPageNumber(1);
                setPreviewOpen(true);
              }}
            >
              <Play className="size-4" />
              {copy.previewStory}
            </Button>
            <Button asChild type="button" size="sm">
              <Link
                to={`/labs/mockups/contents/demo-content/story-pages?language=${selectedLocale.languageCode}`}
              >
                <ArrowRight className="size-4" />
                {copy.openStoryPages}
              </Link>
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => setLocalizationDialogOpen(true)}
            >
              <Plus className="size-4" />
              {copy.addLocalization}
            </Button>
          </div>
        }
        title={copy.localizationTitle}
      >
        <LanguageTabs
          items={mockupDemoContent.locales.map((localeState) => ({
            code: localeState.languageCode,
            label: getMockupLanguageLabel(localeState.languageCode, locale),
            tone: getReadinessTone(localeState),
          }))}
          compact
          listLabel="Content locale workspaces"
          value={selectedLanguageCode}
          onValueChange={selectLanguage}
          renderContent={(item) => {
            const localeState =
              mockupDemoContent.locales.find(
                (candidate) => candidate.languageCode === item.code,
              ) ?? mockupDemoContent.locales[0];

            return (
              <div className="grid gap-4">
                <div className="grid gap-3 rounded-2xl border border-border/70 bg-background/80 p-4 md:grid-cols-2">
                  <label className="grid gap-1.5 text-sm font-medium text-foreground">
                    {copy.title}
                    <Input
                      value={selectedLocaleDraft.title}
                      onChange={(event) =>
                        updateLocaleDraft("title", event.target.value)
                      }
                    />
                  </label>
                  <label className="grid gap-1.5 text-sm font-medium text-foreground">
                    {copy.duration}
                    <Input
                      min="0"
                      type="number"
                      value={selectedLocaleDraft.durationMinutes}
                      onChange={(event) =>
                        updateLocaleDraft(
                          "durationMinutes",
                          event.target.value,
                        )
                      }
                    />
                  </label>
                  <label className="grid gap-1.5 text-sm font-medium text-foreground md:col-span-2">
                    {copy.summary}
                    <Textarea
                      value={selectedLocaleDraft.description}
                      onChange={(event) =>
                        updateLocaleDraft("description", event.target.value)
                      }
                    />
                  </label>
                  <div className="flex flex-wrap items-center gap-2 md:col-span-2">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        updateLocaleDraft(
                          "statusLabel",
                          selectedLocaleDraft.statusLabel === "Published"
                            ? "In review"
                            : "Published",
                        )
                      }
                    >
                      {copy.status}: {selectedLocaleDraft.statusLabel}
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        updateLocaleDraft(
                          "statusLabel",
                          selectedLocaleDraft.statusLabel === "Published"
                            ? "Archived"
                            : "Published",
                        )
                      }
                    >
                      {selectedLocaleDraft.statusLabel === "Published"
                        ? copy.archiveLocale
                        : copy.publishLocale}
                    </Button>
                    <MockupStatusPill
                      tone={
                        localeState.isProcessingComplete ? "success" : "warning"
                      }
                    >
                      {copy.processing}: {localeState.isProcessingComplete ? copy.complete : copy.inProgress}
                    </MockupStatusPill>
                    <Button
                      type="button"
                      size="sm"
                      onClick={() => setSavedLocaleCode(selectedLanguageCode)}
                    >
                      {savedLocaleCode === selectedLanguageCode ? copy.saved : copy.saveLocale}
                    </Button>
                  </div>
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
                  <div className="mt-3 flex items-center gap-3">
                    <MockupAssetPreview kind="image" ready={selectedHasCover} />
                    <p className="text-xs leading-5 text-muted-foreground">
                      {selectedHasCover ? "Selected image asset" : "No cover asset linked"}
                    </p>
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
                            {copy.storyNarration}
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
                  <div className="mt-3">
                    <MockupAssetPreview kind="audio" ready={selectedHasAudio} />
                  </div>
                  <Button
                      className="mt-3"
                      size="sm"
                      type="button"
                      variant="outline"
                      onClick={() =>
                        setAssetDialogTarget({ kind: "story-narration" })
                      }
                    >
                      <Upload className="size-4" />
                      {selectedHasAudio ? copy.manageAsset : copy.addAsset}
                    </Button>
                  </div>
                </div>
                <p className="text-xs leading-5 text-muted-foreground">
                  {copy.pageAudioNote}
                </p>
              </div>
            );
          }}
        />
      </FormSection>
      <FormSection
        actions={
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => setContributorDialogTarget("new")}
          >
            <UserPlus className="size-4" />
            {copy.assignContributor}
          </Button>
        }
        title={copy.contributorTitle}
      >
        <div className="divide-y overflow-hidden rounded-2xl border border-border/70 bg-card/70">
          {contributors.map((assignment, assignmentIndex) => (
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
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  aria-label={`${copy.edit}: ${assignment.name}`}
                  onClick={() => setContributorDialogTarget(assignment.id)}
                >
                  <Pencil className="size-4" />
                  {copy.edit}
                </Button>
                <Button
                  type="button"
                  size="icon"
                  variant="ghost"
                  aria-label={`${copy.reorderUp}: ${assignment.name}`}
                  disabled={assignmentIndex === 0}
                  onClick={() => moveContributor(assignment.id, "up")}
                >
                  <ArrowUp className="size-4" />
                </Button>
                <Button
                  type="button"
                  size="icon"
                  variant="ghost"
                  aria-label={`${copy.reorderDown}: ${assignment.name}`}
                  disabled={assignmentIndex === contributors.length - 1}
                  onClick={() => moveContributor(assignment.id, "down")}
                >
                  <ArrowDown className="size-4" />
                </Button>
                <Button
                  type="button"
                  size="icon"
                  variant="ghost"
                  aria-label={`${copy.unassign}: ${assignment.name}`}
                  onClick={() =>
                    setContributors((current) =>
                      current
                        .filter((candidate) => candidate.id !== assignment.id)
                        .map((candidate, index) => ({ ...candidate, order: index + 1 })),
                    )
                  }
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      </FormSection>

      <FormSection title={copy.sourceImagesTitle}>
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
            <div className="mt-3 flex items-center gap-3">
              <MockupAssetPreview kind="image" ready={sourceCoverLinked} />
              <p className="text-xs leading-5 text-muted-foreground">
                {sourceCoverLinked ? "Selected source image" : "No source image linked"}
              </p>
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
        open={previewOpen}
        onOpenChange={setPreviewOpen}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{copy.previewTitle}</DialogTitle>
            <DialogDescription>
              {selectedLocaleDraft.title} · {getMockupLanguageLabel(selectedLanguageCode, locale)}
            </DialogDescription>
          </DialogHeader>
          {previewLocalization ? (
            <DialogBody className="grid gap-4">
              <div className="grid gap-2 rounded-2xl border border-border/70 bg-muted/20 p-5">
                <div className="flex items-center justify-between gap-3">
                  <MockupStatusPill tone="accent">
                    Page {previewPage?.pageNumber} / {mockupStoryPages.length}
                  </MockupStatusPill>
                  <span className="text-xs text-muted-foreground">
                    {previewLocalization.statusLabel}
                  </span>
                </div>
                <h3 className="font-heading text-lg font-semibold tracking-tight text-foreground">
                  {previewLocalization.title}
                </h3>
                <p className="text-sm leading-6 text-muted-foreground">
                  {previewLocalization.description}
                </p>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={previewPageNumber <= 1}
                  onClick={() => setPreviewPageNumber((current) => Math.max(1, current - 1))}
                >
                  <ArrowLeft className="size-4" />
                  {copy.previousPage}
                </Button>
                <Button
                  type="button"
                  size="sm"
                  onClick={() => setIsPreviewPlaying((current) => !current)}
                >
                  {isPreviewPlaying ? <Pause className="size-4" /> : <Play className="size-4" />}
                  {isPreviewPlaying ? copy.pause : copy.play}
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={previewPageNumber >= mockupStoryPages.length}
                  onClick={() => setPreviewPageNumber((current) => Math.min(mockupStoryPages.length, current + 1))}
                >
                  {copy.nextPage}
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </DialogBody>
          ) : null}
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => setPreviewOpen(false)}>
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={localizationDialogOpen}
        onOpenChange={setLocalizationDialogOpen}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.addLocalizationTitle}</DialogTitle>
            <DialogDescription>{copy.addLocalizationDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-2 sm:grid-cols-2">
            {mockupDemoContent.locales.map((localeState) => (
              <Button key={localeState.languageCode} type="button" variant="outline" disabled>
                {getMockupLanguageLabel(localeState.languageCode, locale)} · {copy.saved}
              </Button>
            ))}
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => setLocalizationDialogOpen(false)}>
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={contributorDialogTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setContributorDialogTarget(null);
          }
        }}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.contributorDialogTitle}</DialogTitle>
            <DialogDescription>
              {contributorDialogAssignment?.name
                ? `${contributorDialogAssignment.name} · ${copy.contributorDialogDescription}`
                : copy.contributorDialogDescription}
            </DialogDescription>
          </DialogHeader>
          <DialogBody>
            <div className="rounded-xl border border-border/70 bg-muted/20 p-4 text-sm leading-6 text-muted-foreground">
              {copy.contributorDialogDescription}
            </div>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => setContributorDialogTarget(null)}>
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

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
              {assetDialogTarget?.kind === "story-narration"
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

function MockupReferenceContentDetail({
  content,
}: {
  content: MockupContentSummary;
}) {
  const { locale } = useI18n();
  const isLullaby = content.typeLabel === "LULLABY";
  const [selectedLanguageCode, setSelectedLanguageCode] = useState(
    content.locales[0]?.languageCode ?? "en",
  );
  const selectedLocale =
    content.locales.find(
      (localeState) => localeState.languageCode === selectedLanguageCode,
    ) ?? content.locales[0]!;
  const [localeDrafts, setLocaleDrafts] = useState<
    Record<string, MockupLocaleDraft>
  >(() =>
    Object.fromEntries(
      content.locales.map((localeState) => [
        localeState.languageCode,
        {
          title: localeState.title,
          description: localeState.description,
          durationMinutes: String(
            localeState.durationMinutes ?? content.playbackDurationMinutes ?? "",
          ),
          statusLabel: localeState.statusLabel,
        },
      ]),
    ),
  );
  const draft =
    localeDrafts[selectedLanguageCode] ??
    ({
      title: selectedLocale.title,
      description: selectedLocale.description,
      durationMinutes: String(
        selectedLocale.durationMinutes ?? content.playbackDurationMinutes ?? "",
      ),
      statusLabel: selectedLocale.statusLabel,
    } satisfies MockupLocaleDraft);
  const [metadata, setMetadata] = useState({
    externalKey: content.externalKey,
    ageRange: String(content.ageRange),
    active: content.active,
  });
  const [metadataSaved, setMetadataSaved] = useState(false);
  const [playbackDurationMinutes, setPlaybackDurationMinutes] = useState(
    String(content.playbackDurationMinutes ?? selectedLocale.durationMinutes ?? ""),
  );
  const [assets, setAssets] = useState({
    audio: selectedLocale.hasAudio ?? false,
    listeningCover: content.hasListeningCover ?? false,
    listingCover: content.hasListingCover ?? false,
    playbackCover: content.hasPlaybackCover ?? false,
  });
  const [assetTarget, setAssetTarget] = useState<
    "audio" | "listening-cover" | "listing-cover" | "playback-cover" | null
  >(null);
  const [savedLanguageCode, setSavedLanguageCode] = useState<string | null>(
    null,
  );
  const [localizationDialogOpen, setLocalizationDialogOpen] = useState(false);

  const copy =
    locale === "tr"
      ? {
          eyebrow: "İçerik detayı",
          back: "İçerik listesine dön",
          description: isLullaby
            ? "Ninniye ait ortak playback ve kapak varlıklarını, dil metadata’sından ayrı yönetin."
            : "Meditasyonun dil metadata’sını ve ortak dinleme kapağını aynı çalışma alanında yönetin.",
          localeWorkspace: "Dil çalışma alanı",
          localeDescription: isLullaby
            ? "Ninnilerde bu alan dil başlığı, durum ve görünürlük bilgisini taşır; playback içerik seviyesinde ortaktır."
            : "Meditasyonun seçili diline ait başlık, açıklama, ses ve süreyi düzenleyin.",
          title: "Başlık",
          descriptionLabel: "Açıklama",
          duration: isLullaby ? "Ortak playback süresi (dakika)" : "Süre (dakika)",
          audio: "Meditasyon ses asset’i",
          playback: "Ortak ninni playback",
          playbackDescription: "Bu ses ve süre tüm ninni dilleriyle paylaşılır.",
          playbackAudio: "Ortak playback ses asset’i",
          instruments: "Enstrüman seçimi",
          instrumentsDescription: "Katalogdan seçilen enstrümanları ve sıralarını yönetin.",
          manageInstruments: "Enstrümanları yönet",
          listeningCover: "Ortak dinleme kapağı",
          listeningCoverDescription: "Ses deneyiminde tüm dillerin paylaştığı kapak.",
          listingCover: "Listeleme kapağı (statik)",
          listingCoverDescription: "Ninni listeleri ve kartlarında kullanılan statik kapak.",
          playbackCover: "Playback kapağı (animasyonlu)",
          playbackCoverDescription: "Ninni playback ve detay deneyiminde kullanılan kapak.",
          metadata: "İçerik metadata’sı",
          metadataDescription: "Tüm diller tarafından paylaşılan alanlar.",
          metadataSave: "Ortak metadata’yı kaydet",
          type: "Tür",
          externalKey: "External key",
          ageRange: "Yaş aralığı",
          active: "Aktif",
          visible: "Mobil görünürlük",
          processing: "İşleme",
          railTitle: "Operasyon özeti",
          railDescription: "Yayın kararını destekleyen kısa göstergeler.",
          addLocale: "Dil ekle",
          addLocaleTitle: "Dil çalışma alanı ekle",
          addLocaleDescription:
            "Bu mockup, yeni bir dil çalışma alanı akışını gösterir. Seçenekler sonraki adıma hazırlık olarak sunulur.",
          complete: "Tamamlandı",
          inProgress: "Devam ediyor",
          yes: "Evet",
          no: "Hayır",
          status: "Durum",
          publish: "Dili yayınla",
          archive: "Dili arşivle",
          save: "Değişiklikleri kaydet",
          saved: "Kaydedildi",
          manageAsset: "Asset yönet",
          addAsset: "Asset ekle",
          chooseOrUpload: "Asset seç veya yükle",
          chooseExisting: "Mevcut asset seç",
          uploadImage: "Yeni görsel yükle",
          uploadAudio: "Yeni ses yükle",
          close: "Kapat",
        }
      : {
          eyebrow: "Content detail",
          back: "Back to contents",
          description: isLullaby
            ? "Manage shared lullaby playback and cover assets separately from locale metadata."
            : "Manage meditation locale metadata and the shared listening cover in one workspace.",
          localeWorkspace: "Locale workspace",
          localeDescription: isLullaby
            ? "For lullabies, this area holds locale title, status, and visibility; playback is shared at content level."
            : "Edit the selected meditation locale’s title, description, audio, and duration.",
          title: "Title",
          descriptionLabel: "Description",
          duration: isLullaby ? "Shared playback duration (minutes)" : "Duration (minutes)",
          audio: "Meditation audio asset",
          playback: "Shared lullaby playback",
          playbackDescription: "This audio and duration are shared by every lullaby locale.",
          playbackAudio: "Shared playback audio asset",
          instruments: "Instrument selection",
          instrumentsDescription: "Manage selected catalog instruments and their order.",
          manageInstruments: "Manage instruments",
          listeningCover: "Shared listening cover",
          listeningCoverDescription: "The cover shared by the audio experience across locales.",
          listingCover: "Listing cover (static)",
          listingCoverDescription: "The static cover used in lullaby lists and cards.",
          playbackCover: "Playback cover (animated)",
          playbackCoverDescription: "The cover used in lullaby playback and detail experiences.",
          metadata: "Content metadata",
          metadataDescription: "Fields shared across all locales.",
          metadataSave: "Save shared metadata",
          type: "Type",
          externalKey: "External key",
          ageRange: "Age range",
          active: "Active",
          visible: "Mobile visibility",
          processing: "Processing",
          railTitle: "Operational summary",
          railDescription: "A short set of live indicators for publication decisions.",
          addLocale: "Add locale",
          addLocaleTitle: "Add locale workspace",
          addLocaleDescription:
            "This mockup presents the new locale workspace flow. Options are shown as a preparation step for the next action.",
          complete: "Complete",
          inProgress: "In progress",
          yes: "Yes",
          no: "No",
          status: "Status",
          publish: "Publish locale",
          archive: "Archive locale",
          save: "Save changes",
          saved: "Saved",
          manageAsset: "Manage asset",
          addAsset: "Add asset",
          chooseOrUpload: "Choose or upload an asset",
          chooseExisting: "Choose existing asset",
          uploadImage: "Upload new image",
          uploadAudio: "Upload new audio",
          close: "Close",
        };

  function markAssetReady() {
    if (assetTarget) {
      const stateKey =
        assetTarget === "listening-cover"
          ? "listeningCover"
          : assetTarget === "listing-cover"
            ? "listingCover"
            : assetTarget === "playback-cover"
              ? "playbackCover"
              : "audio";
      setAssets((current) => ({ ...current, [stateKey]: true }));
    }
    setAssetTarget(null);
  }

  function updateDraft(
    field: keyof MockupLocaleDraft,
    value: string,
    languageCode = selectedLanguageCode,
  ) {
    setSavedLanguageCode(null);
    setLocaleDrafts((current) => ({
      ...current,
      [languageCode]: {
        ...(current[languageCode] ?? {
          title:
            content.locales.find((item) => item.languageCode === languageCode)
              ?.title ?? "",
          description:
            content.locales.find((item) => item.languageCode === languageCode)
              ?.description ?? "",
          durationMinutes: String(
            content.locales.find((item) => item.languageCode === languageCode)
              ?.durationMinutes ?? content.playbackDurationMinutes ?? "",
          ),
          statusLabel:
            content.locales.find((item) => item.languageCode === languageCode)
              ?.statusLabel ?? "Draft",
        }),
        [field]: value,
      },
    }));
  }

  function selectLanguage(languageCode: string) {
    setSelectedLanguageCode(languageCode);
    setSavedLanguageCode(null);
  }

  function assetReady(kind: "audio" | "listening-cover" | "listing-cover" | "playback-cover") {
    return assets[
      kind === "listening-cover"
        ? "listeningCover"
        : kind === "listing-cover"
          ? "listingCover"
          : kind === "playback-cover"
            ? "playbackCover"
            : "audio"
    ];
  }

  function renderAssetCard(
    kind: "audio" | "listening-cover" | "listing-cover" | "playback-cover",
    label: string,
    description?: string,
  ) {
    const ready = assetReady(kind);
    return (
      <div className="rounded-2xl border border-border/70 bg-background/80 p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex min-w-0 items-start gap-3">
            {kind === "audio" ? (
              <FileAudio className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
            ) : (
              <FileImage className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
            )}
            <div className="min-w-0 space-y-1">
              <h3 className="text-sm font-semibold text-foreground">{label}</h3>
              <p className="text-sm leading-6 text-muted-foreground">
                {description ?? (ready ? "Ready" : "Pending")}
              </p>
            </div>
          </div>
          <MockupStatusPill tone={ready ? "success" : "warning"}>
            {ready ? "Ready" : "Pending"}
          </MockupStatusPill>
        </div>
        <div className="mt-3 flex items-center gap-3">
          <MockupAssetPreview kind={kind === "audio" ? "audio" : "image"} ready={ready} />
          <p className="text-xs leading-5 text-muted-foreground">
            {ready ? "Selected asset" : "No asset selected"}
          </p>
        </div>
        <Button
          className="mt-4"
          size="sm"
          type="button"
          variant="outline"
          onClick={() => setAssetTarget(kind)}
        >
          <Upload className="size-4" />
          {ready ? copy.manageAsset : copy.addAsset}
        </Button>
      </div>
    );
  }

  const sharedMetadataSection = (
    <FormSection title={copy.metadata}>
      <div className="grid gap-3 md:grid-cols-2">
        <div className="grid gap-1.5 text-sm font-medium text-foreground">
          {copy.type}
          <MockupStatusPill tone="default">{content.typeLabel}</MockupStatusPill>
        </div>
        <label className="grid gap-1.5 text-sm font-medium text-foreground">
          {copy.externalKey}
          <Input
            value={metadata.externalKey}
            onChange={(event) => {
              setMetadataSaved(false);
              setSavedLanguageCode(null);
              setMetadata((current) => ({ ...current, externalKey: event.target.value }));
            }}
          />
        </label>
        <label className="grid gap-1.5 text-sm font-medium text-foreground">
          {copy.ageRange}
          <Input
            min="0"
            type="number"
            value={metadata.ageRange}
            onChange={(event) => {
              setMetadataSaved(false);
              setSavedLanguageCode(null);
              setMetadata((current) => ({ ...current, ageRange: event.target.value }));
            }}
          />
        </label>
        <Button
          className="justify-start"
          type="button"
          size="sm"
          variant="outline"
          onClick={() => {
            setMetadataSaved(false);
            setMetadata((current) => ({ ...current, active: !current.active }));
          }}
        >
          {copy.active}: {metadata.active ? copy.yes : copy.no}
        </Button>
      </div>
      <div className="flex justify-end border-t border-border/60 pt-4">
        <Button
          type="button"
          size="sm"
          onClick={() => setMetadataSaved(true)}
        >
          {metadataSaved ? copy.saved : copy.metadataSave}
        </Button>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        {!isLullaby ? renderAssetCard("listening-cover", copy.listeningCover, copy.listeningCoverDescription) : null}
        {isLullaby ? renderAssetCard("listing-cover", copy.listingCover, copy.listingCoverDescription) : null}
        {isLullaby ? renderAssetCard("playback-cover", copy.playbackCover, copy.playbackCoverDescription) : null}
      </div>
    </FormSection>
  );

  function getLocaleDraft(languageCode: string) {
    const localeState = content.locales.find(
      (item) => item.languageCode === languageCode,
    );

    return (
      localeDrafts[languageCode] ?? {
        title: localeState?.title ?? "",
        description: localeState?.description ?? "",
        durationMinutes: String(
          localeState?.durationMinutes ?? content.playbackDurationMinutes ?? "",
        ),
        statusLabel: localeState?.statusLabel ?? "Draft",
      }
    );
  }

  function renderLocaleEditor(languageCode: string) {
    const localeState = content.locales.find(
      (item) => item.languageCode === languageCode,
    );
    if (!localeState) return null;

    const localeDraft = getLocaleDraft(languageCode);

    return (
      <div className="grid gap-4">
        <div className="grid gap-4 md:grid-cols-2">
          <label className="grid gap-1.5 text-sm font-medium text-foreground">
            {copy.title}
            <Input
              value={localeDraft.title}
              onChange={(event) =>
                updateDraft("title", event.target.value, languageCode)
              }
            />
          </label>
          {!isLullaby ? (
            <label className="grid gap-1.5 text-sm font-medium text-foreground md:col-span-2">
              {copy.descriptionLabel}
              <Textarea
                value={localeDraft.description}
                onChange={(event) =>
                  updateDraft("description", event.target.value, languageCode)
                }
              />
            </label>
          ) : null}
        </div>

        {!isLullaby ? (
          <div className="grid gap-3 md:grid-cols-2">
            {renderAssetCard("audio", copy.audio)}
            <label className="grid gap-1.5 rounded-2xl border border-border/70 bg-background/80 p-4 text-sm font-medium text-foreground">
              {copy.duration}
              <Input
                min="0"
                type="number"
                value={localeDraft.durationMinutes}
                onChange={(event) =>
                  updateDraft("durationMinutes", event.target.value, languageCode)
                }
              />
            </label>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center gap-2 border-t border-border/60 pt-4">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() =>
              updateDraft(
                "statusLabel",
                localeDraft.statusLabel === "Published" ? "Archived" : "Published",
                languageCode,
              )
            }
          >
            {localeDraft.statusLabel === "Published" ? copy.archive : copy.publish}
          </Button>
          <MockupStatusPill
            tone={localeState.isProcessingComplete === false ? "warning" : "success"}
          >
            {copy.processing}: {localeState.isProcessingComplete === false ? copy.inProgress : copy.complete}
          </MockupStatusPill>
          <Button
            type="button"
            size="sm"
            onClick={() => setSavedLanguageCode(languageCode)}
          >
            {savedLanguageCode === languageCode ? copy.saved : copy.save}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <ContentPageShell
      eyebrow={copy.eyebrow}
      title={draft.title}
      description={copy.description}
      showHeader={false}
      aside={
        <TaskRail
          title={copy.railTitle}
          description={copy.railDescription}
          variant="detail"
          stats={[
            {
              label: copy.visible,
              value: `${content.locales.filter((item) => item.isVisibleToMobile).length} / ${content.locales.length}`,
              tone: content.locales.some((item) => item.isVisibleToMobile) ? "success" : "warning",
            },
            {
              label: copy.processing,
              value: selectedLocale.isProcessingComplete === false ? copy.inProgress : copy.complete,
              tone: selectedLocale.isProcessingComplete === false ? "warning" : "success",
            },
            {
              label: copy.duration,
              value: `${(isLullaby ? playbackDurationMinutes : draft.durationMinutes) || "—"} min`,
            },
          ]}
        />
      }
    >
      {sharedMetadataSection}

      <FormSection
        actions={
          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            <Button asChild type="button" size="sm" variant="outline">
              <Link to="/labs/mockups/contents">{copy.back}</Link>
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => setLocalizationDialogOpen(true)}
            >
              <Plus className="size-4" />
              {copy.addLocale}
            </Button>
            <MockupStatusPill tone={selectedLocale.isPublished ? "success" : "warning"}>
              {draft.statusLabel}
            </MockupStatusPill>
          </div>
        }
        title={copy.localeWorkspace}
      >
        <LanguageTabs
          compact
          items={content.locales.map((localeState) => ({
            code: localeState.languageCode,
            label: getMockupLanguageLabel(localeState.languageCode, locale),
            tone: getReadinessTone(localeState),
          }))}
          listLabel={`${content.typeLabel} locale workspaces`}
          value={selectedLanguageCode}
          onValueChange={selectLanguage}
          renderContent={(item) => renderLocaleEditor(item.code)}
        />
      </FormSection>

      {isLullaby ? (
        <FormSection title={copy.playback}>
          <div className="grid gap-3 md:grid-cols-2">
            {renderAssetCard("audio", copy.playbackAudio)}
            <label className="grid gap-1.5 rounded-2xl border border-border/70 bg-background/80 p-4 text-sm font-medium text-foreground">
              {copy.duration}
              <Input
                min="0"
                type="number"
                value={playbackDurationMinutes}
                onChange={(event) => {
                  setPlaybackDurationMinutes(event.target.value);
                }}
              />
            </label>
          </div>
          <div className="flex items-center justify-between gap-3 rounded-2xl border border-border/70 bg-background/80 p-4">
            <div className="min-w-0">
              <h3 className="text-sm font-semibold text-foreground">{copy.instruments}</h3>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">{copy.instrumentsDescription}</p>
            </div>
            <Button type="button" size="sm" variant="outline">
              {copy.manageInstruments}
            </Button>
          </div>
        </FormSection>
      ) : null}

      <Dialog
        open={localizationDialogOpen}
        onOpenChange={setLocalizationDialogOpen}
      >
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.addLocaleTitle}</DialogTitle>
            <DialogDescription>{copy.addLocaleDescription}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-3">
            {content.locales.map((localeState) => (
              <Button
                key={localeState.languageCode}
                type="button"
                variant="outline"
                disabled
              >
                {getMockupLanguageLabel(localeState.languageCode, locale)} · {copy.saved}
              </Button>
            ))}
          </DialogBody>
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => setLocalizationDialogOpen(false)}
            >
              {copy.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={assetTarget !== null} onOpenChange={(open) => !open && setAssetTarget(null)}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{copy.chooseOrUpload}</DialogTitle>
            <DialogDescription>{copy.chooseOrUpload}</DialogDescription>
          </DialogHeader>
          <DialogBody className="grid gap-3">
            <Button type="button" variant="outline" onClick={markAssetReady}>
              {assetTarget === "audio" ? copy.uploadAudio : copy.uploadImage}
            </Button>
            <Button type="button" onClick={markAssetReady}>{copy.chooseExisting}</Button>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => setAssetTarget(null)}>{copy.close}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ContentPageShell>
  );
}

export function MockupContentDetailRoute() {
  const { contentId } = useParams<{ contentId: string }>();
  const contentById: Record<string, MockupContentSummary> = {
    [mockupDemoContent.id]: mockupDemoContent,
    [mockupReferenceMeditation.id]: mockupReferenceMeditation,
    [mockupReferenceLullaby.id]: mockupReferenceLullaby,
  };
  const content = contentById[contentId ?? "demo-content"];

  if (!content || content.typeLabel === "STORY") {
    return <MockupStoryContentDetail />;
  }

  return <MockupReferenceContentDetail content={content} />;
}
