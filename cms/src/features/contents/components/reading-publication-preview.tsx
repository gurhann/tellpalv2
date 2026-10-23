import { useEffect, useRef, useState } from "react";
import { Eye, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  contentAdminApi,
  type ReadingPreviewImage,
  type ReadingPreviewManifest,
  type ReadingPublicationStatus,
} from "@/features/contents/api/content-admin";
import { useI18n } from "@/i18n/locale-provider";

type Props = {
  contentId: number;
  language: string;
  status: ReadingPublicationStatus;
  onStatusRefresh?: (status: ReadingPublicationStatus) => void;
};
export function ReadingPublicationPreview(props: Props) {
  const { locale } = useI18n();
  const [open, setOpen] = useState(false);
  const tr = locale === "tr";
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline">
          <Eye className="size-4" />
          {tr ? "Hazır görselleri önizle" : "Preview prepared images"}
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            {tr ? "Hazır okuma görselleri" : "Prepared reading images"}
          </DialogTitle>
          <DialogDescription>
            {props.status.state === "LIVE"
              ? tr
                ? "Yayında kullanılan doğrulanmış kapak ve sayfalar."
                : "Verified cover and pages used for publication."
              : tr
                ? "Önceki hazır görseller gösteriliyor. Yeni görseller henüz hazır değil."
                : "Showing previous prepared images. The new images are not ready yet."}
          </DialogDescription>
        </DialogHeader>
        <DialogBody>
          {open ? (
            <PreviewContent
              key={`${props.status.liveGeneration}:${props.status.liveRevision}:${props.status.liveManifestDigest}`}
              {...props}
            />
          ) : null}
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}
function PreviewContent({
  contentId,
  language,
  status,
  onStatusRefresh,
}: Props) {
  const { locale } = useI18n();
  const tr = locale === "tr";
  const [manifest, setManifest] = useState<ReadingPreviewManifest | null>(null);
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [selected, setSelected] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setFailed(false);
    setManifest(null);
    const load = async () => {
      const expected =
        attempt > 0
          ? await contentAdminApi.getReadingPublication(
              contentId,
              language,
              controller.signal,
            )
          : status;
      if (!active) return;
      if (attempt > 0) onStatusRefresh?.(expected);
      if (!expected.enabled || !expected.liveRevision)
        throw new Error("NO_ELIGIBLE_PREVIEW");
      const value = await contentAdminApi.getReadingPreview(
        contentId,
        language,
        controller.signal,
      );
      if (!active) return;
      if (
        value.identity.contentId !== contentId ||
        value.identity.language !== language ||
        value.identity.revision !== expected.liveRevision ||
        (expected.liveGeneration != null &&
          value.identity.liveGeneration !== expected.liveGeneration) ||
        (expected.liveManifestDigest != null &&
          value.identity.manifestDigest !== expected.liveManifestDigest)
      )
        throw new Error("STALE_PREVIEW");
      setManifest(value);
      setSelected(0);
    };
    void load().catch(() => {
      if (active) setFailed(true);
    });
    return () => {
      active = false;
      controller.abort();
    };
  }, [
    attempt,
    contentId,
    language,
    status.liveGeneration,
    status.liveRevision,
    status.liveManifestDigest,
    onStatusRefresh,
  ]);
  if (failed)
    return <PreviewError retry={() => setAttempt((value) => value + 1)} />;
  if (!manifest) return <Loading />;
  const images = manifest.images.filter(
    (image) => image.role !== "READING_DETAIL",
  );
  const image = images[selected];
  if (!image)
    return <PreviewError retry={() => setAttempt((value) => value + 1)} />;
  return (
    <div className="grid gap-4 pt-4">
      <div
        className="flex flex-wrap gap-2"
        aria-label={tr ? "Hazır görsel seçimi" : "Prepared image selection"}
      >
        {images.map((entry, index) => (
          <Button
            key={entry.renditionKey}
            type="button"
            size="sm"
            variant={selected === index ? "default" : "outline"}
            aria-pressed={selected === index}
            onClick={() => setSelected(index)}
          >
            {entry.role === "READING_COVER"
              ? tr
                ? "Kapak"
                : "Cover"
              : tr
                ? `Sayfa ${entry.pageNumber}`
                : `Page ${entry.pageNumber}`}
          </Button>
        ))}
      </div>
      <PreparedImage
        key={`${manifest.identity.jobId}:${image.renditionKey}`}
        identity={manifest.identity}
        image={image}
        onRetry={() => {
          setManifest(null);
          setAttempt((value) => value + 1);
        }}
      />
    </div>
  );
}
function PreparedImage({
  identity,
  image,
  onRetry,
}: {
  onRetry: () => void;
  identity: ReadingPreviewManifest["identity"];
  image: ReadingPreviewImage;
}) {
  const { locale } = useI18n();
  const tr = locale === "tr";
  const ownedUrl = useRef<string | null>(null);
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    let objectUrl: string | null = null;
    setUrl(null);
    setFailed(false);
    void contentAdminApi
      .downloadReadingPreview(identity, image, controller.signal)
      .then(({ blob }) => {
        if (!active) return;
        if (blob.size !== image.bytes || blob.type !== image.mime)
          throw new Error("PREVIEW_BYTES_MISMATCH");
        objectUrl = URL.createObjectURL(blob);
        ownedUrl.current = objectUrl;
        setUrl(objectUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
      controller.abort();
      if (objectUrl && ownedUrl.current === objectUrl) {
        URL.revokeObjectURL(objectUrl);
        ownedUrl.current = null;
      }
    };
  }, [identity, image]);
  const label =
    image.role === "READING_COVER"
      ? tr
        ? "Hazır kapak"
        : "Prepared cover"
      : tr
        ? `Hazır sayfa ${image.pageNumber}`
        : `Prepared page ${image.pageNumber}`;
  const saved = image.sourceBytes - image.bytes;
  return (
    <figure className="grid gap-3">
      <div className="grid min-h-56 place-items-center rounded-xl border border-border/60 bg-muted/20 p-3">
        {failed ? (
          <PreviewError retry={onRetry} />
        ) : url ? (
          <img
            src={url}
            alt={label}
            width={image.width}
            height={image.height}
            className="max-h-[48vh] w-auto max-w-full object-contain"
            onError={() => {
              if (ownedUrl.current) URL.revokeObjectURL(ownedUrl.current);
              ownedUrl.current = null;
              setFailed(true);
              setUrl(null);
            }}
          />
        ) : (
          <Loading />
        )}
      </div>
      <figcaption className="grid gap-1 text-sm text-muted-foreground">
        <p className="font-medium text-foreground">
          {label} · {image.mime.replace("image/", "").toUpperCase()} ·{" "}
          {image.width} × {image.height}
        </p>
        <p>
          {tr ? "Kaynak" : "Source"}: {formatBytes(image.sourceBytes, locale)} →{" "}
          {tr ? "Hazır" : "Prepared"}: {formatBytes(image.bytes, locale)}
        </p>
        <p>
          {saved > 0
            ? tr
              ? `%${Math.round((saved / image.sourceBytes) * 100)} daha küçük`
              : `${Math.round((saved / image.sourceBytes) * 100)}% smaller`
            : saved === 0 && image.mime === "image/jpeg"
              ? tr
                ? "JPEG değişmeden korundu · aynı boyut"
                : "JPEG preserved unchanged · same size"
              : saved === 0
                ? tr
                  ? "Aynı dosya boyutu"
                  : "Same file size"
                : tr
                  ? "Dosya boyutu arttı"
                  : "File size increased"}
        </p>
      </figcaption>
    </figure>
  );
}
function Loading() {
  return (
    <p
      role="status"
      className="flex items-center gap-2 py-6 text-sm text-muted-foreground"
    >
      <LoaderCircle className="size-4 animate-spin" />
      {useI18n().locale === "tr" ? "Görsel yükleniyor…" : "Loading image…"}
    </p>
  );
}
function PreviewError({ retry }: { retry: () => void }) {
  const tr = useI18n().locale === "tr";
  return (
    <div role="alert" className="grid gap-3 py-4 text-sm">
      <p>
        {tr
          ? "Görsel önizlemesi yüklenemedi. Tekrar deneyin."
          : "The image preview could not be loaded. Try again."}
      </p>
      <Button type="button" variant="outline" onClick={retry}>
        {tr ? "Önizlemeyi yeniden dene" : "Retry preview"}
      </Button>
    </div>
  );
}
function formatBytes(bytes: number, locale: string) {
  return `${new Intl.NumberFormat(locale).format(bytes)} ${locale === "tr" ? "bayt" : "bytes"}`;
}
