---
title: 'Playback işleme hedeflerini güvenilir biçimde ayırma'
type: 'feature'
created: '2026-09-06'
status: 'done'
baseline_commit: 'NO_VCS'
review_loop_iteration: 0
context:
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Asset processing kaydı bugün zorunlu `(contentId, languageCode)` kimliği taşır. Bu, tam anlatım gibi locale'a ait işlemler için doğru olsa da tüm dillerde ortak olan ninni playback'inin aynı işinin her dil için tekrar planlanmasına ve durum değişikliklerinin yanlış localization'a yansımasına yol açar.

**Approach:** `asset` modülünde hedefi açıkça modellemek: `LOCALIZATION(contentId, languageCode)` ve `CONTENT(contentId)`. Kalıcı kurallar, public API, işçi/poller, storage yolu ve admin kontratı aynı typed hedefi taşıyacak; `content` yalnızca localization hedefli olayları kendi processing durumuna yansıtacaktır.

## Boundaries & Constraints

**Always:** Mevcut bütün satırlar `LOCALIZATION` hedefi olarak korunur. `LOCALIZATION` hedefi geçerli bir localization'a, `CONTENT` hedefi geçerli bir content'e bağlanır; yalnızca birincisinde dil bulunur. Her hedef için tek işlem kaydı korunur; aynı content altında farklı diller ve content-level hedef birbirini engellemez. State machine (`PENDING → PROCESSING → COMPLETED | FAILED`) ve mevcut hata/retry semantiği korunur. Modüller arası erişim sadece `asset.api` üzerinden kalır. Content-scope işlemin path'i dil adı veya boş/null segmenti içermeyen deterministik ortak bir konum kullanır.

**Ask First:** Mevcut localization-odaklı admin HTTP yollarının kaldırılması ya da mevcut istemcileri kıracak bir response değişikliği gerekirse dur ve sor. Bu story'nin ötesinde processing tamamlanınca mobil görünürlük, otomatik publish→schedule, bundle bütünlüğü veya public endpoint davranışı değiştirilmek istenirse dur ve sor.

**Never:** `AUDIO_STORY` enum/değerini veya kategori şemasını kaldırma; bu Story 1.6'nın kapsamıdır. `StoryNarration`, `LullabyPlayback`, ninni alanları ya da CMS formu ekleme; sonraki hikâyelerin kapsamıdır. Content modülüne asset processing durumunun kopyasını ekleme veya asset'in iç paketlerine erişim verme. Localization processing olayını CONTENT hedefi için uydurma bir dil ile yayınlama.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Locale işi | `LOCALIZATION(42, tr)` ile planlama | Kayıt ve generated dosyalar `tr` hedefinde kalır; sadece `tr` localization durumu olaydan etkilenir | Aynı hedef `PENDING` ise mevcut kaydın context'i yenilenir; `PROCESSING`/`COMPLETED` ise mevcut iş kuralı hatası döner |
| Ortak iş | `CONTENT(42)` ile planlama | Dil olmadan tek kayıt, ortak deterministic storage yolu ve `findByContent(42)` sonucu oluşur | Aynı content hedefinin yinelenmesi reddedilir |
| Geçersiz eşleşme | `LOCALIZATION` + boş dil veya `CONTENT` + dil | Persistans ve API hedefi reddeder | Anlaşılır validation/constraint hatası; kayıt oluşmaz |
| Hedef izolasyonu | Aynı content için `CONTENT`, `LOCALIZATION(tr)` ve `LOCALIZATION(en)`; biri fail | Sadece fail olan kaydın durumu değişir | CONTENT olayı hiçbir `content_localizations.processing_status` alanını güncellemez |

</frozen-after-approval>

## Code Map

- `be/src/main/resources/db/migration/V12__create_asset_processing_tables.sql` -- mevcut zorunlu composite localization FK ve unique anahtarın kaynağı; yeni migration eski satırları locale hedefi olarak korumalıdır.
- `be/src/main/resources/db/migration/V13__add_asset_processing_context_columns.sql` -- target scope'tan bağımsız job context alanlarının önceki deseni; mevcut context kuralları korunur.
- `be/src/main/java/com/tellpal/v2/asset/domain/AssetProcessing.java` -- aggregate; typed target ve nullable-only-for-content dil kuralının domain sahibi.
- `be/src/main/java/com/tellpal/v2/asset/domain/AssetProcessingRepository.java` ve `.../asset/infrastructure/persistence/{SpringDataAssetProcessingRepository,JpaAssetProcessingRepositoryAdapter}.java` -- scope'a göre sorgu/benzersizlik erişimleri.
- `be/src/main/java/com/tellpal/v2/asset/api/{AssetProcessingApi,AssetProcessingCommands,AssetProcessingRecord,AssetProcessingStatusChangedEvent}.java` -- content'in güvenle kullanacağı explicit target public sözleşmesi ve `findByContent` noktası.
- `be/src/main/java/com/tellpal/v2/asset/application/{AssetProcessingService,AssetProcessingMapper}.java` -- schedule/start/retry/complete/fail geçişleri ile target-aware mapping/event üretimi.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/processing/{AssetProcessingPoller,RegisteringAssetProcessingJobExecutor}.java` -- worker çağrılarının localization anahtarı varsayımını typed hedefe dönüştürme noktası.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetProcessingPathBuilder.java` -- locale ve ortak target için çakışmayan object-path kuralı.
- `be/src/main/java/com/tellpal/v2/asset/web/admin/{AssetProcessingAdminController,AdminAssetProcessingResponse}.java` -- scope'u açık request/response; eski locale yolları geriye uyumlu tutulur.
- `be/src/main/java/com/tellpal/v2/content/application/AssetProcessingStatusListener.java` -- sadece `LOCALIZATION` olayının localization processing durumuna çevrilmesini sağlar.
- `be/src/test/java/com/tellpal/v2/asset/{AssetProcessingIntegrationTest.java,web/admin/AssetProcessingAdminIntegrationTest.java}` ve `be/src/test/java/com/tellpal/v2/content/ContentManagementIntegrationTest.java` -- iki target, DB kuralları, retry/state geçişi ve event izolasyonu için genişletilecek mevcut kanıt.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/resources/db/migration/V22__scope_asset_processing_targets.sql` -- `target_scope` ekle, eski kayıtları `LOCALIZATION` olarak backfill et; scope/dil check'leri, localization ve content parent geçerliliği ile her target için ayrı unique indeksleri kur -- ortak playback kaydı dil localization'ına bağlı kalmasın.
- [x] `be/src/main/java/com/tellpal/v2/asset/domain/AssetProcessing.java` ve repository/persistans adaptörleri -- enum veya typed target value object ile hedef invariant'larını ve scope-bazlı sorguları uygula; `findByContent` ekle -- null dilin yanlışlıkla locale davranışı üretmesini önle.
- [x] `be/src/main/java/com/tellpal/v2/asset/api/` ve `.../asset/application/` -- tüm command, record, event, mapper ve state transition'larda explicit typed target kullan; `CONTENT` işlemi için dil içermeyen read/retry/complete/fail akışını ekle -- content modülünün kapsamı tahmin etmesini engelle.
- [x] `be/src/main/java/com/tellpal/v2/asset/infrastructure/{processing,storage}/` -- poller, executor ve output path üretimini target-aware yap; common output için dil içermeyen sabit segment kullan -- retry ve generated output çakışmalarını önle.
- [x] `be/src/main/java/com/tellpal/v2/asset/web/admin/` -- schedule/status/retry kontratlarında target scope'u görünür yap; mevcut localization URL'lerini uyumlu bırak ve content-scope için açık endpoint/parametre yolu ekle -- CMS'in yanlış hedef göndermesini önle.
- [x] `be/src/main/java/com/tellpal/v2/content/application/AssetProcessingStatusListener.java` -- CONTENT olayını localization yazmadan yok say -- ortak playback durumunun dile ait editoryal durumu değiştirmesini engelle.
- [x] `be/src/test/java/com/tellpal/v2/asset/` ve `be/src/test/java/com/tellpal/v2/content/` -- Flyway sonrası SQL constraint negatifleri, iki scope schedule/retry/complete/fail, `findByContent`, aynı content/dil izolasyonu, path ve CONTENT-event izolasyonu ekle -- davranış ve persistans güvence altına alınsın.
- [x] `be/src/test/java/com/tellpal/v2/TellPalApplicationTests.java` veya mevcut Modulith test konumu -- modül sınırı doğrulamasını çalışır durumda tut -- `content`in asset internal pakete bağımlı olmadığını koru.

**Acceptance Criteria:**
- Given mevcut bir asset processing veritabanı, when migration uygulanır, then tüm kayıtlar geçerli `LOCALIZATION(contentId, languageCode)` target'ına dönüşür ve mevcut iş durumları korunur.
- Given aynı content için bir ortak target ve birden fazla localization target'ı, when işlemler planlanır veya biri başarısız olur, then her target bağımsız kayda/duruma sahip olur ve yalnız ilgili target değişir.
- Given hedefi `CONTENT` olan bir durum değişikliği, when event content modülüne ulaşır, then hiçbir localization processing durumu yazılmaz.
- Given target/dil birleşimi kurala aykırı veya parent kaydı geçersiz, when API ya da doğrudan veritabanı yazımı denenir, then işlem reddedilir ve kısmi kayıt kalmaz.
- Given admin API veya `asset.api` üzerinden scope'lu işlem okunur ya da tekrar denenir, when `CONTENT(contentId)` kullanılır, then `findByContent(contentId)` ve content-scope akışı çalışır; eski locale odaklı akışlar işlevini korur.

## Design Notes

PostgreSQL koşullu foreign key sağlamadığından yalnız bir `CHECK (scope, language)` yeterli değildir. Migration, `CONTENT` satırını `contents(id)` ile, `LOCALIZATION` satırını mevcut `(content_id, language_code)` localization anahtarıyla doğrulayacak açık bir referans tasarımı kurmalıdır. Uygulama yalnız repository filtresine güvenmemelidir; eşzamanlı schedule çağrıları DB unique index ile korunmalıdır.

`AssetProcessingTarget` gibi tek bir typed değer, nullable `LanguageCode` parametrelerinin her katmana yayılmasından daha güvenlidir. Path builder target'ı almalı; örneğin locale target mevcut dil segmentini korurken common target sabit `shared` segmentini kullanmalıdır. Bu isim, migration ve testlerde aynı literal ile değil tek bir builder kuralıyla uygulanmalıdır.

## Verification

**Commands:**
- `cd be && ./mvnw test -Dtest=AssetProcessingIntegrationTest,AssetProcessingAdminIntegrationTest,ContentManagementIntegrationTest` -- expected: iki target için integration akışları ve event izolasyonu geçer.
- `cd be && ./mvnw test` -- expected: tüm backend testleri ve Flyway bootstrap geçer.
- `cd be && ./mvnw verify` -- expected: integration ve Spring Modulith doğrulaması geçer.

## Suggested Review Order

**Hedef modeli ve uygulama akışı**

- Typed hedef, scope ve dil invariant'larını tek değerde toplar.
  [`AssetProcessingTarget.java:6`](../../be/src/main/java/com/tellpal/v2/asset/api/AssetProcessingTarget.java#L6)

- Schedule/read/retry akışlarını aynı hedef üzerinden yürütür.
  [`AssetProcessingService.java:72`](../../be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java#L72)

- Ortak hedefin localization durumuna sızmasını engeller.
  [`AssetProcessingStatusListener.java:20`](../../be/src/main/java/com/tellpal/v2/content/application/AssetProcessingStatusListener.java#L20)

**Persistans ve referans bütünlüğü**

- Scope, dil ve parent kurallarını veritabanında zorunlu kılar.
  [`V22__scope_asset_processing_targets.sql:28`](../../be/src/main/resources/db/migration/V22__scope_asset_processing_targets.sql#L28)

- Localization anahtarı değişiminde processing referansını korur.
  [`V22__scope_asset_processing_targets.sql:67`](../../be/src/main/resources/db/migration/V22__scope_asset_processing_targets.sql#L67)

**Admin kontratı ve depolama**

- Eski localization URL'lerini korurken content endpoint'lerini ekler.
  [`AssetProcessingAdminController.java:151`](../../be/src/main/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminController.java#L151)

- Shared hedef için locale içermeyen deterministik path üretir.
  [`AssetProcessingPathBuilder.java:83`](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetProcessingPathBuilder.java#L83)

**Kanıt**

- İki scope'un state, worker output ve localization izolasyonunu doğrular.
  [`AssetProcessingIntegrationTest.java:189`](../../be/src/test/java/com/tellpal/v2/asset/AssetProcessingIntegrationTest.java#L189)

- Admin schedule/status/retry ve invalid target kontratlarını doğrular.
  [`AssetProcessingAdminIntegrationTest.java:140`](../../be/src/test/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminIntegrationTest.java#L140)

### Review Findings

- [x] [Review][Decision] Pending hedef yeniden planlama davranışı — Mevcut context yenileme davranışı korunuyor; spec matrisi bu karara göre güncellendi (`be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java:198-209`).
- [x] [Review][Patch] Localization parent kontrolü yarış koşuluna açık [be/src/main/resources/db/migration/V22__scope_asset_processing_targets.sql:28-85] — parent satırı `FOR KEY SHARE` ile doğrulama sırasında korunuyor.
- [x] [Review][Patch] Parent/unique constraint hataları transaction commit sonrasına taşınabildiği için kontrollü API hatası garanti edilmiyor [be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java:226-237] — kayıt `saveAndFlush` ile constraint kontrolü tamamlandıktan sonra sınıflandırılıyor.
- [x] [Review][Patch] Data-integrity hatalarının tümünü parent bulunamadı olarak sınıflandırmak yanlış 404 üretebilir [be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java:229-237] — yalnızca ilgili PostgreSQL foreign-key SQL state'i parent hatası olarak ele alınıyor.
- [x] [Review][Patch] Yeni content-scope endpoint'lerinde core OpenAPI hata cevapları eksik [be/src/main/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminController.java:83-115] — 400/401/403/404/409 cevapları tanımlandı.
- [x] [Review][Patch] Admin API kuralları yeni target scope ve content-scope endpoint'lerini belgelemiyor [be/docs/admin-api-rules.md:546-580] — scope alanları, varsayılanlar ve endpoint kuralları eklendi.
- [x] [Review][Patch] Migration backfill, geçersiz localization parent, scope başına unique index ve expired-lease recovery için gerekli negatif/entegrasyon testleri eksik [be/src/test/java/com/tellpal/v2/asset/AssetProcessingIntegrationTest.java:229-255] — ilgili entegrasyon senaryoları eklendi.
- [x] [Review][Defer] `externalKey` path segment'i separator/traversal karakterlerini normalize etmiyor [be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetProcessingPathBuilder.java:91-115] — deferred, pre-existing
