---
title: 'Paket 2: Normal geliştirme tüketicisine doğrulanmış görsel teslimatı'
type: feature
created: '2026-10-22'
status: done
baseline_commit: 85757698cda700429aa363b8680f58a76969534b
review_loop_iteration: 0
context:
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/standards/rest-api-documentation-standard.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
---

<frozen-after-approval reason="Kullanıcının 2026-10-22 tarihinde onayladığı üç paketlik uygulama planı">

## Amaç

Story 1.3'ün kullanacağı ortak görsel katmanını hazırla. Normal geliştirme
tüketicisi gerçek backend'den JPEG/PNG/WebP rendition alabilsin; mevcut detail
kapakta kontrollü bağlantı ve bağımsız page bileşeni çalışsın.

## Sınırlar

**Her zaman:** Feature-first domain/repository/use case/Cubit ve injectable DI.
CachedNetworkImage, mevcut seçim/decode/disposal sınırları. Yeni teslimat açık
local/test gate'i altında; varsayılan kapalı, prod/production reddedilir.
Gerçek akışta revoke kontrollü Firebase bearer, mevcut Library allowlist ve
aktif exact-language ücretsiz STORY + uygun retained LIVE birlikte gerekir.
Metadata veya subscriber fixture tam görsel yetkisi değildir.
Her byte/status isteği sunucu hesabı, generation/revision/digest/job ve seçimi
bağlar. Storage öncesi/sonrası erişim kontrolü; yalnız rendition, source fallback yok.
Tüm yanıtlar private,no-store; Range/redirect yok; gerçek MIME/SHA/byte/geometri.

Mobilde fresh admission cache'ten önce gelir. Hesap/dil/revizyon/digest/rendition/SHA
cache kimliğidir; token/URL değildir. Korumalı bytes ve metadata yalnız bellekte,
sınırlı sahiplikte kalır. Lease bitişi, ayrılma/arka plan veya erişim reddinde çizim
kalkar, işler iptal edilir ve provider/cache bırakılır. Manuel retry fresh admission
ister. Eski callback yeni attempt'i dolduramaz. Normal renderer QA istisnası almaz.

**Önce sor:** Yeni SDK/storage/cache framework veya ürün kapsamı değişikliği.
**Asla:** Canlı/Firebase/production DB/deploy/push; orijinal/eski kanıt yazımı;
QA endpoint'ini normal teslimat saymak; tam reader/audio/session/paywall geliştirmek;
Story 1.2 insan kabulünü veya Story 1.3'ü tamamlanmış saymak.

| Durum | Beklenen |
|---|---|
| Yetkili ücretsiz LIVE | Gerçek rendition; fresh admission ardından bounded çizim |
| Yanlış hesap/dil/revizyon veya withdrawal | Byte/cache üzerinden erişim yok; açık hata |
| Bozuk/yarım/MIME/SHA farklı çıktı | READY/promotion yok; eski callback atılır |
| Lease/iptal/geç yanıt | Çizim kalkar; cleanup tamamlanana kadar sahiplik korunur |

</frozen-after-approval>

## Kod haritası

- `be/src/main/java/com/tellpal/v2/asset/api/ReadingImageDeliveryApi.java` ve
  `asset/application/reading/ReadingImageDeliveryService.java`: descriptor/profile'a
  açık formatPolicy; mevcut pinned doğrulamayı paylaşan rendition-only giriş.
  QA canonical PNG davranışını koru; Paket 1 encoder/fingerprint'ine dokunma.
- `be/src/main/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonService.java:56`:
  lock→retained proof→free→retainedEligible kesitini yeniden kullan. Yeni
  `content/api/ReadingImageConsumerApi.java` ve ayrı ordinary service/properties;
  QA'nın tek id/tr/iki sayfa/PNG kısıtlarını taşıma.
- `be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java:50`
  allowlist; `ReadingImageComparisonTestService.java:28` before/after revoke
  kontrolü. Yeni user facade/controller `/api/library/reading-images` ailesini
  local/test gate altında sunsun. Bounded writer/Tomcat capability/no-store
  filtresini MIME parametresiyle paylaş; QA yolunun semantiği değişmesin.
- `app/mobile/lib/features/reading_images/domain/`: saf manifest/digest/validator
  ve SelectReadingImageRendition yeniden kullanılır. Profile policy eksik/null ise
  eski PNG_FIT; yeni preserve JPEG exact-copy veya PNG→WebP, source boyutları ve
  profile üst sınırları; unknown policy ret. Eski growth/floor-fit korunur.
- `app/mobile/lib/features/reading_images/consumer/`: ayrı typed model/data source,
  repository/use case/Cubit ve CachedNetworkImage bileşeni. QA repository/Cubit
  kullanılmaz. `core/network/library_image_cache.dart:49,486,694,716,909` lifecycle
  seam'i; file MemoryFileSystem ve tamamlanmış bellek CacheInfoRepository adapter'ı.
  NonStoringObjectProvider dosyayı decode öncesi silebildiğinden kullanılmaz.
- `app/mobile/lib/app/injection.dart` ve app composition: yalnız normal consumer
  kayıtları; QA config bağımlılıkları normal root'a girmez. Detail entegrasyonu
  feature bağımsız core capability/builder ile yapılır.
  `story_detail/presentation/widgets/story_detail_hero.dart:152` provider key/evict;
  `core/presentation/widgets/reading_detail_artwork_frame.dart` slot/fit sınırı.
  StoryDetail başka feature import etmez; boundary testi gevşetilmez.

## İşler ve kabul

- [x] Backend API/service/profile: ücretsiz retained LIVE için normal manifest,
  status, rendition-only bytes; açık kaynak/bütçe/lease ayarları ve fail-closed gate.
- [x] User controller/shared HTTP writer: doğrulanmış hesap scope, auth/erişim
  tekrar kontrolü, MIME ve bounded response; Türkçe OpenAPI ve önemli hatalar.
- [x] Mobil domain/consumer: policy parse/validator, fresh repository ve Cubit,
  bellekte aynı cache lifecycle, sınırlı decode ve cleanup.
- [x] App/detail composition: açık opt-in; varsayılan Library/detail davranışı,
  kilit/overlay/ses tercihi/retry/rota korunur; standalone page aynı bileşeni çizer.
- [x] `be/docs/evidence/image-package-2/`: kısa kabul kaydı. ADR-0024, proje hafızası
  ve `architecture.md` yalnız normal local/test tüketici dilimiyle güncellenir.

**Kabul:** Given yerel yeni JPEG/WebP LIVE, when normal tüketici ister, then
aynı doğrulanmış dosya gerçek HTTP ve normal widget'ta çalışır. Given eski profil,
when mevcut QA/detail kullanılır, then eski sözleşme korunur. Given cache'te dosya,
when fresh erişim reddedilir, then dosya çizilmez. Given eksik/bozuk kaynak veya
geç attempt, when sonuç döner, then erişim/promotion kazanmaz.

## Plan değişiklikleri

Kullanıcı planın tümünü uygulamayı onayladı; aynı kapsam için yeniden onay istenmez.
Paket 1 commit'i yukarıdadır. Önceden değiştirilmiş 88 dosya korunur. Paket 3 tek
backend–mobil kabulünü ve epic'e devri tamamlar; eski codec/telefon deneyi açılmaz.

## Teknik kararlar

Mevcut canonical digest için descriptor'ın objectKey/provider/generation/jobId
metadata'sı korunur; bunlar teslimat seçeneği/URL değildir. Keyfi path/profile/source
isteği kabul edilmez. Yeni projection/digest protokolü eklenmez. Sunucu accountScope'u
bearer'dan üretir; istemci konfigürasyonundaki hesapla karşılaştırır. Lease bir
saklama/yenileme hakkı değildir; sona erince temizle ve manuel retry dar davranışıdır.
Sınırlar açık geliştirme config'inden gelir; mevcut detail decode tavanı aşılmaz.
Yerel testte mock verifier + PostgreSQL/disk fixture kullanımı gerçek Firebase
kabulü diye sunulmaz. SDK mutlak storage deadline ve AMD64 yeterliliği açık kalır.

## Doğrulama

Hedefli PostgreSQL/API/gerçek HTTP testleri: auth/account/free/language/revision,
JPEG/WebP ve ikiden fazla page, bozuk/eksik/transport ayrımı, bütçe/cleanup/no-store.
Mobil gerçek CachedNetworkImage decode/promotion/warm, disk yazmama, policy/digest,
lease/iptal/late callback, detail opt-in/default ve page slot×DPR testleri.
Sonra bir `./mvnw verify`, Flutter test/analyze ve Story 1.1/1.2 regresyonları.
Ham log/medya Git dışında; kısa sonuçlar JSON/README'de. Korumalar
`/private/tmp/tellpal-package2-baseline.json`; yeni run dizini işaretçisi
`/private/tmp/tellpal-package2-run-path`. Yerel Docker: `DOCKER_HOST=unix:///Users/gurhankucuk/.colima/tellpal-image-local/docker.sock`,
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`. Native araç dizinleri
`/private/tmp/tellpal-package1-dependencies-path` ve Paket 1 run işaretçisinden
çözülür. Yerel doğrulama gerçek token veya Firebase çağrısı gerektirmez.

## Uygulama doğrulama kaydı

Normal local/test dilimi uygulandı. [Kabul matrisi ve sınırlar](../../be/docs/evidence/image-package-2/README.md)
ile [sayısal sonuçlar](../../be/docs/evidence/image-package-2/verification.json) ayrı kaydedildi.
Tam Maven verify geçti: 590 Surefire + 41 Failsafe, sıfır hata; 28 eski kapsam kapısı skip,
yeni backend consumer testlerinin 8/8'i çalıştı. Tam Flutter 654 test geçti; son dar decode
tavanı/disk-yazmama farkıyla 21 hedefli test ve analyzer tekrar geçti.
Eski PNG descriptor wire biçimi korunur. Protected 88, kaynak 41, P5c 124, P5f 7 ve Paket 1
kanıt 38 dosyası hash olarak aynıdır. Paket 3'ün tek backend–normal widget kabulü ayrıdır.

## Önerilen inceleme sırası

**Normal teslimat girişi**

- Normal endpoint yalnız hesap ve rendition kapsamıyla teslimat başlatır.
  [ReadingImageConsumerController.java:33](../../be/src/main/java/com/tellpal/v2/user/web/mobile/ReadingImageConsumerController.java#L33)

**Erişim ve LIVE bağı**

- Storage öncesi ve sonrası güncel erişim yeniden doğrulanır.
  [ReadingImageConsumerService.java:15](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingImageConsumerService.java#L15)

- Bearer hesabı ve revoke kontrolü istemci kapsamından bağımsızdır.
  [ReadingImageConsumerService.java:46](../../be/src/main/java/com/tellpal/v2/user/application/ReadingImageConsumerService.java#L46)

**Mobil sahiplik ve bırakma**

- Admission, lease, status ve gerçek cleanup aynı sahibin sınırındadır.
  [reading_image_consumer_cubit.dart:148](../../app/mobile/lib/features/reading_images/consumer/presentation/reading_image_consumer_cubit.dart#L148)

- Bellek cache ve geç kalan delegate gerçekten tamamlanır.
  [reading_image_memory_cache.dart:38](../../app/mobile/lib/features/reading_images/consumer/presentation/reading_image_memory_cache.dart#L38)

- Foreground ve route uygunluğu korunur; normal bounded provider çizilir.
  [reading_image_consumer_widget.dart:102](../../app/mobile/lib/features/reading_images/consumer/presentation/reading_image_consumer_widget.dart#L102)

**Uygulama bağlantısı**

- Mevcut detail yalnız açık geliştirme opt-in ile yeni tüketiciye bağlanır.
  [library_app.dart:27](../../app/mobile/lib/app/library_app.dart#L27)

**Format sözleşmesi**

- Eski PNG ve JPEG koruma/WebP politikaları ayrı doğrulanır.
  [validate_reading_image_manifest.dart:94](../../app/mobile/lib/features/reading_images/domain/use_case/validate_reading_image_manifest.dart#L94)

**Doğrulama ve sınırlar**

- Çalışan matris ve Paket 3 gerçek root kabulü sınırı birlikte okunur.
  [README.md:1](../../be/docs/evidence/image-package-2/README.md#L1)

- Yarışlar, erişim reddi ve gerçek cleanup davranışı sınanır.
  [reading_image_consumer_test.dart:112](../../app/mobile/test/reading_images/reading_image_consumer_test.dart#L112)
