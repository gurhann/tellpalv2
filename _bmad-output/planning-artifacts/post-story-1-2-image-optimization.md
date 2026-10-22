# Publish-time image optimization follow-up

Status: planning started on 2026-10-08; [draft specification and implementation phases](../specs/spec-publish-time-image-optimization/SPEC.md) are available for discussion. The human approved new-publication enforcement first and existing content through a separate controlled transition. Numeric profiles and the readiness contract remain open.
Requested by the human on 2026-10-07; implementation is outside Story 1.2.

Published content must use optimized cover and content images rather than serving raw uploaded image files to consumers. Plan optimization as part of content publication. Evaluate resolution reduction and image compression, with separate quality and size targets appropriate to covers and content pages, including phone and tablet use.

The planning work must establish representative before/after file sizes, rendered quality and legibility, target resolutions, compression/format choices, and publication behavior when optimization fails. Inspect the existing asset/publication contracts before choosing processing or storage architecture. Determine how existing published images will be handled and how optimized outputs receive correct dimensions, MIME types, byte sizes and checksums. Keep revision consistency and ordinary versus candidate access intact.

No numeric resolution, quality target, implementation architecture or delivery date is selected by this note. No existing media is changed. The approved iOS native-test capture envelope increase from 8 to 16 MiB only enables unmodified SDK screenshot evidence; it does not implement or satisfy content-image optimization.

Track this follow-up from [mobile epics](epics-mobile.md) and [Story 1.2](../implementation-artifacts/mobile/spec-1-2-open-the-selected-story-detail-from-library.md). The draft records current pipeline gaps, representative evaluation, verification criteria and phased rollout. The approved [first local JPEG experiment](../specs/spec-publish-time-image-optimization/p0-small-jpeg-findings.md) produced 63 source/output pairs: source-resolution q85 increases the six matched control files' total bytes by 42.30%, so forced re-encoding is not an accepted optimization policy. High-resolution/page sources, native quality/resource measurements and small-source/readiness decisions remain open before long implementation. No production profile or delivery behavior changed.


## Geliştirme kapanışı ve devir — 2026-10-23

[Görsel altyapısı geliştirme kabulü](../../be/docs/evidence/image-infrastructure-acceptance/README.md)
tamamlandı. Paket 1'in 41 kaynak/metadata/Linux kanıtı ve Paket 2'nin tam testleri
korundu. Paket 3'ün tek gerçek yerel Spring → normal `LibraryApp` detail/sayfa koşusu
JPEG/WebP HTTP, rendition, byte/SHA, engine decode ve gecikmeli ayrılma cleanup'ını doğruladı.
Ayrı görsel deney zinciri kapandı; uygulama işi Mobil Epic 1 / Story 1.3'e devredildi.

Story 1.1 `done`, Story 1.2 `review`, Story 1.3 `backlog` kaldı. Story 1.2 insan kabulü
açık olarak kaydedildi. Story 1.3 typed görsel API/repository/use case/bileşenini
kullanır; protected-start ve ilk sayfa audio sözleşmesini, ses tercihini ve uygulama
medya sahipliğini kendi kapsamında tamamlar. Story 1.4 sayfa geçişlerini sahiplenir.
[Onaylı önerinin üretim işleri](sprint-change-proposal-2026-10-22.md#7-epice-döndükten-sonra-zorunlu-kalan-üretim-işleri)
sürer. Yerel kabul üretim etkinleştirmesi veya mevcut arşivin optimize edildiği anlamına gelmez.
