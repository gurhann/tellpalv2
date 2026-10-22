# ADR-0022: Isolated STORY Reading Image Preparation Engine

## Status

Accepted implementation boundary on 2026-10-16 through explicit P2 decisions and approval of the [P2 specification](../../../_bmad-output/implementation-artifacts/spec-p2-verified-reading-image-preparation.md). The bounded engine is implemented and its tests/packaged conversion pass; workflow reviews are complete. [Evidence](../evidence/p2-reading-preparation/verification.md) distinguishes engine checks, the subsequent authorized storage-only live proof, and unapproved production profiles/deployment.

The later bounded local/test Publish orchestration is recorded separately in [ADR-0023](ADR-0023-reading-publication-orchestration.md); it does not admit production profiles or ordinary consumer rollout.

## Context

ADR-0021 fixes the desired publication/readiness contract. The existing page experiment cannot supply that contract: manifest publication and job completion are separate, profile/source identity is incomplete, retained page FKs depend on mutable rows and ordinary object uploads can overwrite paths. Existing experiment profiles and P0 quality observations do not admit a production STORY reading profile.

The human selected three bounded P2 decisions: implement the engine with explicit local test profiles while production profiles remain disabled; accept only narrow supported image semantics; and expose internal preparation evidence without new delivery rights or deletion. The human then approved the concrete implementation specification with “A”.

## Decision

- Keep the initial scope exact-language STORY/READING: localized reading cover/detail and the complete ordered localized page set. `content` captures scalar bindings; `asset` captures actual bytes, profiles, work, attempts and verified immutable manifests. No new asset-to-content module dependency or shared media aggregate is needed.
- Implement an additive internal engine. Existing listing, page experiment, legacy flags, manual-ready endpoints and consumers preserve their contracts. P2 READY is captured-media evidence, not current publication generation, LIVE promotion or access authorization. P3 remains responsible for durable Publish intent, mutation invalidation and eligible current-generation adoption.
- Require explicit test-profile admission and reject production activation. Numerical test settings and synthetic growth evidence cannot become production defaults. No production profile is admitted by this ADR. PNG is the first engine output format; geometry remains explicit and no-upscale. Encoding PNG is lossless, but resizing removes detail and has no blanket quality approval.
- Java ImageIO PNG writer'ında `compressionQuality=1.0` sıkıştırmayı kapatır; `0.0` en güçlü DEFLATE sıkıştırmasını seçer. Yerel/test adayında `0.0` kullanılır. Encoder descriptor ve gerçek child aynı ayarı tek kaynaktan alır. Ayar değişikliği yeni profil fingerprint'i ve rendition kimliği üretir; eski descriptor taşıyan `PENDING` iş dönüşümden önce drift nedeniyle reddedilir. Mevcut geometri, RGB/metadata kabulü ve süreç sınırları korunur.
- Admit only the implemented, explicitly described subset of single-frame opaque 8-bit sRGB PNG/JPEG with identity orientation. Reject unsupported metadata/color conversion, alpha, EXIF/mirroring, animation, malformed containers and source shortage. Do not inherit the legacy white matte or silently discard unsupported semantics. Broader admission requires qualification and a new complete profile identity.
- Hash actual retained source bytes and complete effective role/profile descriptors, including exact encoder/runtime, normalization, geometry, admission and operational settings. Use generation-pinned source reads and conditional immutable writes. Source IDs, path existence, profile labels and registry metadata alone cannot prove freshness or readiness.
- Persist the captured complete revision and durable attempt ownership. Finalize the complete manifest and owning job atomically under source/profile/job/attempt/lease checks and a PROCESSING precondition. Lease time is refreshed after lock acquisition and before commit. First terminal outcome wins; identical replay is read-only proof; contradiction or stale callbacks cannot mutate terminal state or replacement work. Explicit retry creates a new claimed attempt. Conflict success requires full immutable equality.
- Run conversion outside DB transactions in a supervised child JVM. Explicit byte/pixel/full-raster, heap/direct/native/process, temporary-space, time, concurrency and thread settings participate in admission and identity. Parent admission checks actual maximum and available heap; one monotonic revision budget bounds capture, storage, revalidation and finalization without changing captured profile identity. Observe process termination before releasing local capacity. Lease expiry and cancelling a future do not prove teardown. Test/runtime constraints do not establish production peak-memory or distributed-capacity qualification.
- Independently reread stored object versions and verify complete membership, actual format/storage MIME, byte count, SHA-256, exact dimensions and full decode. Integrity/availability state is separate from historical manifest identity. Retained page/localization provenance is scalar and survives editable row removal.
- Configure the reading-only SDK clone with the SDK default non-idempotent no-retry handler for both operation classifications, preserving the original legacy client. `maxAttempts=1` alone does not prevent inner HTTP media resumptions. Include the policy, adapter/handler implementation identity and honest timeout semantics in new profile fingerprints. Socket read timeouts are inactivity limits; between-call monotonic checks cannot preempt a continuously progressing blocking read. Absolute in-flight duration, including slow trickles/authentication, remains an explicit production-qualification gap. No new storage process/IPC architecture is admitted by this SDK-only correction.
- Preserve originals, retained revisions and isolated orphan ownership. Add no public/test delivery endpoint, signed URL, HTTP cache policy, session right, renewal/retention interval or cleanup job. No cloud production mutation, deployment, backfill or deletion is authorized.

## Paket 1: Açık JPEG/WebP koruma politikası (2026-10-22)

Onaylı [Paket 1 spec’i](../../../_bmad-output/implementation-artifacts/spec-image-package-1-preparation.md),
yerel/test ana P2/P3 motoruna `PRESERVE_JPEG_OR_LOSSLESS_WEBP` politikasını ekler.
Eski 16 alanlı `TestProfile` ve eksik/null JSON politika alanı `PNG_FIT` olarak kalır.
Koruma modunda width/height üst sınırdır; kaynak boyutları değişmez. JPEG aynı
byte’larla kopyalanır; PNG’den yeni kayıpsız WebP üretilir. Hazır P5c/P5g adayları
encode girdisi değildir.

Kaynak yalnız opak RGB8 veya bütün alpha örnekleri 255 olan RGBA8 PNG ve baseline
RGB8 JPEG olabilir. Alpha kanalının varlığı tek başına ret değildir; tek bir şeffaf
piksel ret üretir. Yalnız profilsiz RGB veya SHA’sı pinli 536 byte P3 ICC kabul edilir.
ICC/XMP ve taşıma öneki ayrılmış TIFF korunur; orientation, animasyon, desteklenmeyen
metadata/container semantiği ve trailing byte reddedilir. Renk dönüşümü, resize,
matte ve orientation düzeltmesi yapılmaz.

Native encoder resmi libwebp 1.6.0 `cwebp`/`webpmux` araçlarıdır. Ham RGB’den
profilsiz canonical PNG yazılır; `-lossless -exact -q 100 -m 6 -metadata none`
ve tek thread kullanılır. ICC/EXIF/XMP byte’ları webpmux ile eklenir. Bağımsız
TwelveMonkeys 3.15.3 raw destination decoder tam RGB/metadata ve VP8L payload
sonunun tam tüketimini doğrular. Stored reread yeniden pinli kaynağı okur;
çıktının kendi SHA/MIME/geometrisi yanında kaynak eşliği de zorunludur.

Profil gerçek motor class’larını, decoder JAR’larını, JVM binary’lerini, iki resmi
native binary SHA/sürümünü ve bütün ayarları içerir. Yeni kimlik eski pending işi
dönüşümden önce reddeder. Eşit veya büyüyen JPEG/WebP de mevcut `allowGrowth` ve
dolu evidence kuralına tabidir. Java child ve gözlenen native descendant RSS
toplamı, deadline, disk/log sınırı ve gerçek süreç ağacı kill/exit denetimi korunur.
RSS örneklemesi sürekli peak ölçümü değildir.

Koruma politikası yerel kaynak kodundan derlenen POSIX guardian kullanır. Worker
`setsid` ile işe ait süreç grubundadır; guardian worker PID’sini grup durana kadar
reap etmez. Lifeline kapanışı veya ani worker ölümü yalnız bu gruba SIGSTOP/SIGKILL
uygular. Linux subreaper bütün orphan çocukları `waitpid` ile toplar; macOS kernel
grubunda çalışabilir süreç kalmadığı doğrulanır. Guardian gerçek çıkmadan kapasite
ve dizin bırakılmaz. Kaynak/binary SHA, derleyici sürümü ve derleme flag’leri profile
girer. Legacy `PNG_FIT` tek Java child kullanır; native araç/guardian dizini gerekmez.
XMP’de TIFF namespace’indeki orientation attribute/element biçimleri de reddedilir;
XML DTD/dış entity/ağ erişimi kapalıdır. PNG pHYs yatay/dikey yoğunluğu eşit olmalıdır.
Backend CI aynı job’da pinli araçları kurar; zorunlu native marker eksik araçta skip
üretmek yerine testi düşürür. Yerel opt-in corpus ve eski deney gate’leri korunur.

Linux arm64/amd64 resmi arşivleri SHA-pinli kurulum betiğiyle aynı deployment JRE’sine
paketlenir. Bu yalnız paketlemedir; üretim profili kapalı kalır. SDK-only storage
akışının mutlak in-flight deadline açığı devam eder. Yeni storage mimarisi, canlı
Firebase/DB erişimi, deploy, P4b5 ve normal mobil/CMS tüketicisi bu kapsamda yoktur.
Gerçek doğrulama sonuçları [Paket 1 kanıtında](../evidence/image-package-1/README.md) tutulur.

## Consequences

The engine can be tested with actual local objects, Java encoders and PostgreSQL races while production admission stays closed. Repeated identical work can reuse only independently verified immutable output; a larger PNG requires explicit profile growth admission and evidence. A failed candidate leaves historical verified media intact but grants no visibility to the first publication.

Kayıpsız piksel doğrulaması ve byte verimliliği bağımsız kabul ölçütleridir. Doğru format, boyut ve tam decode tek başına sıkıştırma kazancı kanıtlamaz. [PNG sıkıştırma kanıtı](../evidence/png-lossless-compression/README.md), eski/yeni paketli child çıktılarının tüm RGB piksellerini ve gerçek byte boyutlarını ayrı karşılaştırır; süreç/decode süresi saf encode süresi sayılmaz. Bu düzeltme üretim, fiziksel kalite ve eşit/büyüyen çıktı kabul kapılarını açmaz; eski çıktı, manifest ve retained LIVE yeniden yazılmaz.

Preparation snapshots are not a substitute for P3 current-source/generation comparison. The API must state that boundary so callers cannot promote captured bytes merely because they are READY. Unsupported real sources fail explicitly rather than being silently normalized.

The host and official deployment-base JRE need separate runtime evidence. Local filesystem or mocked GCS tests do not establish live Firebase generation/precondition behavior. Missing or skipped evidence must remain explicit. Full production profile/resource/device and protected-delivery qualification remain open.

The [storage diagnostic](../reading-image-storage-probe.md) and [original failed body-stall evidence](../evidence/p2-storage-probe/focused-tests-attempt-4.txt) motivated the approved SDK-only correction. A diagnostic process watchdog can contain its own run; it does not supply an absolute production storage deadline or qualify slow-trickle behavior. A subsequent separately authorized [live storage diagnostic](../evidence/p2-storage-probe/live-gcs-2026-10-16.md) passed the selected synthetic create/conflict/pinned-decode cases on 2026-10-16. It changes no implementation policy, production profile or P3 admission; future live invocations remain separately authorized.

## Alternatives Considered

- Reuse the existing experiment's 1440/q85/white-matte defaults: rejected; they are not admitted production reading profiles or complete P1 descriptors.
- Add preparation directly to Publish/CMS now: deferred to P3 to keep this slice testable without changing publication semantics or access.
- Run conversion in an interruptible application thread: rejected; a timeout/cancellation does not prove decoder teardown or isolate heap failure.
- Treat a matching path/registry/job flag as success: rejected; independent versioned stored-byte proof and full conflict equality are required.
- Select a universal 1×/0.75× or JPEG/WebP setting from P0: rejected; acceptance is example/geometry-specific and sharpness/raw-evidence limits persist.

## Related Files or Modules

- [P1 contract](../image-revision-readiness-contract.md), [ADR-0021](ADR-0021-image-revision-readiness-contract.md)
- [Implementation investigation and decision record](../../../_bmad-output/implementation-artifacts/image-optimization/p2-preimplementation-investigation.md)
- [Actual codec preflight evidence](../evidence/p2-imageio-preflight/README.md)
- `content::api`, `asset::api`, asset reading preparation/storage/persistence/process implementation
- ADR-0001/0002/0006/0008/0010/0017/0019

## Supersedes / Superseded By

None. This implements a bounded test-only portion of ADR-0021 without migrating existing experiment, listing or publication contracts. Production qualification and P3 remain separate.
