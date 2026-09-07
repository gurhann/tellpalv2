---
title: 'Story 1.4: Ninninin ortak playback ve başlık-only localization modeli'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 1
baseline_commit: '72b33c8a7911f848e974accac9831232c63bb587'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
  - '_bmad-output/implementation-artifacts/spec-1-3-ortak-textless-kapak-sahipligi.md'
  - '_bmad-output/specs/spec-lullaby-shared-audio-and-instruments/SPEC.md'
  - '_bmad-output/specs/spec-lullaby-shared-audio-and-instruments/lullaby-data-and-api-contract.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `LULLABY` localization kayıtları bugün aynı ses ve süreyi her dilde tekrar taşıyabiliyor. Sözsüz ninnide değişen alan başlıktır; ortak playback ve global müzisyen kredisi içerik düzeyinde tek bir kaynağa sahip olmalıdır.

**Approach:** `Content` altında tekil `LullabyPlayback` child’ı ile ortak `AUDIO` asset’i ve süreyi sakla. Mevcut `Content.listeningCoverMediaId` ortak textless kapağın tek sahibi olarak kalır; localization yalnızca başlık ve yayın durumunu yönetir. Ortak değişiklikler `CONTENT(contentId)` delivery işini planlar.

## Boundaries & Constraints

**Always:** Playback yalnız `LULLABY` için geçerlidir; audio asset pozitif ve `AUDIO`, süre sıfır veya pozitif olmalıdır. Playback kaydı opsiyoneldir ve başlık localization’ından bağımsızdır; audio/süre localization satırlarına kopyalanmaz. LULLABY localization write yalnız başlık, yayın durumu ve yayın zamanını kabul eder; processing durumu client-owned değildir ve playback’in `CONTENT` işinden türetilir. `listeningCoverMediaId` yeniden modellenmez veya playback içine ikinci kez yazılmaz. `MUSICIAN` contributor ataması ninni için global olmalı (`languageCode = null`); mevcut contributor API ve sıralama davranışı korunur. Admin content read ortak playback’i ve content-scope processing durumunu bir kez döndürür; public/mobile response ve enstrüman seçimi bu story’nin konusu değildir.

**Ask First:** Yok. Eski LULLABY localization kayıtlarında aynı audio/süre değerleri varsa migration bunları tek playback’e backfill edebilir; değerler çelişiyorsa veya title-only kuralını ihlal eden alanlar varsa migration açık hata ile durmalıdır; veri sessizce silinmemelidir.

**Never:** Yeni `AUDIO_STORY` content üretme/kaldırma; mobil/public endpoint değiştirme; enstrüman kataloğu veya sıralı ninni-enstrüman ilişkisi ekleme; localization-level audio/cover/duration/body/description alanlarını ninni için koruma; ortak kapağı yeni bir playback cover kolonu ile çoğaltma.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Playback kaydetme | LULLABY + geçerli AUDIO + süre | Tek playback persist edilir, content delivery `PENDING` planlanır | N/A |
| Localization yazımı | LULLABY title/status ile create veya update | Başarılı olur; diğer locale alanları null kalır | Body, description, cover, audio veya duration gönderimi 400; kısmi kayıt yok |
| Yanlış playback | IMAGE, eksik asset, negatif ID/süre | Playback değişmez, processing planlanmaz | Alan bazlı 400 |
| Global müzisyen | LULLABY + MUSICIAN + dil kodu | Atama reddedilir; null dil kapsamı kabul edilir | Açık 400 |
| Legacy migration | Birden fazla locale | Eşit ortak değerler tek playback’e taşınır; çelişki migration’ı durdurur | ID’leri belirten fail-fast hata |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- LULLABY child yaşam döngüsü, localization title-only kuralı ve global MUSICIAN kapsamı.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java` -- LULLABY için ortak alanların locale satırında tutulmasını engelleyen güncelleme sınırı.
- `be/src/main/java/com/tellpal/v2/content/domain/LullabyPlayback.java` -- yeni tekil audio/süre child’ı; `Content` ile cascade/orphan yaşam döngüsü.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java` ve `ContentManagementCommands.java` -- playback command, `AssetRegistryApi` AUDIO doğrulaması ve `AssetProcessingApi` content target schedule/retry akışı.
- `be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java`, `AdminContentView.java`, `AdminContentReadResponse.java` -- ortak playback ve processing snapshot’ının admin projection’ı.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java` -- content-level playback endpoint/request/response; mevcut localization endpointleri korunur.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java` ve `Content.java` -- LULLABY MUSICIAN assignment için global scope invariant’ı.
- `be/src/main/java/com/tellpal/v2/asset/api/AssetProcessingTarget.java`, `AssetProcessingService.java`, `AssetProcessingStatusListener.java` -- `CONTENT(contentId)` delivery işinin ortak path/state izolasyonu; completed/failed source değişiminde güvenli reschedule.
- `be/src/main/resources/db/migration/V25__add_lullaby_playback.sql` -- playback tablosu, LULLABY/content FK-trigger’ları, title-only ve global MUSICIAN DB kuralları, legacy preflight/backfill.
- `be/src/test/java/com/tellpal/v2/content/{domain,ContentManagementIntegrationTest.java}`, `.../web/admin/ContentAdminIntegrationTest.java`, `.../migration/` -- domain, HTTP, processing, localization izolasyonu ve migration kanıtı.

## Tasks & Acceptance

**Execution:**
- [x] `V25__add_lullaby_playback.sql` -- tekil playback tablosunu ve legacy güvenlik/backfill kurallarını ekle; title-only ve global musician DB invariant’larını koru.
- [x] `Content`, `ContentLocalization`, `LullabyPlayback` -- ortak audio/süre sahipliğini ve LULLABY type kurallarını aggregate seviyesinde uygula.
- [x] `ContentManagementService`, commands, controller ve admin projections -- content-level playback yazma/okuma, AUDIO validation, LULLABY title-only request kuralları ve content processing snapshot’ını ekle.
- [x] `AssetProcessingService` akışı -- playback source değişince CONTENT delivery işini pending/reschedule yap; localization processing durumuna yazma.
- [x] Contributor assignment akışı -- LULLABY MUSICIAN için locale scope’u reddet, global atamayı ve mevcut endpointleri koru.
- [x] Backend domain, Testcontainers migration ve admin HTTP testleri -- matristeki geçerli/yanlış asset, locale izolasyonu, legacy migration ve processing senaryolarını kanıtla.

**Acceptance Criteria:**
- Given bir LULLABY ve iki localization, when ortak audio veya süre güncellenir, then tek playback değişir, locale kayıtları yalnız başlık/yayın durumunu taşır ve content delivery hedefi yenilenir.
- Given LULLABY localization write, when body, description, cover, audio veya duration gönderilir, then API 400 döner ve kalıcı kısmi değişiklik oluşmaz.
- Given MUSICIAN contributor, when LULLABY’ye dil koduyla atanır, then atama reddedilir; global atama tüm diller için tek kredi olarak okunur.
- Given admin content read, when LULLABY okunur, then ortak playback ve processing bilgisi bir kez, localization başlıkları ayrı döner.
- Given legacy LULLABY rows, when V25 çalışır, then eşit ortak değerler güvenle backfill edilir; çelişkili veya kayıplı dönüşüm gerektiren veri açık hata ile migration’ı durdurur.

## Design Notes

Story 1.3’te kararlaştırılan `listeningCoverMediaId` playback’in ortak kapağıdır; `LullabyPlayback` içine tekrar yazılmaz. Bu, ninni kapağı ile STORY kaynak kapağının yeniden birleşmesini ve iki content-level cover ID’sinin farklı anlamlar taşımasını önler. Mobil/public projection daha sonra bu admin modelinden türetilecektir.

## Verification

**Commands:**
- `cd be && ./mvnw test -Dtest=ContentTest,ContentManagementIntegrationTest,ContentAdminIntegrationTest,ContentCoverOwnershipMigrationIntegrationTest` -- expected: domain, HTTP, processing and migration coverage passes.
- `cd be && ./mvnw verify` -- expected: full Flyway and Spring Modulith verification passes.

## Review Findings

- [x] [Review][Patch] Legacy ve doğrudan SQL playback kayıtlarında AUDIO türü doğrulaması eksikti; V25 preflight ve insert/update trigger’ı ile kapatıldı.
- [x] [Review][Patch] Migration sonrası eski locale DELIVERY işleri shared playback’e taşınmadan kalabiliyordu; V25 bunları temizleyip tek CONTENT PENDING işi oluşturuyor.
- [x] [Review][Patch] Playback source değişiminde external key, eksik processing kaydı ve tamamlanmış/başarısız job yenileme yolları kapsanıyordu; ortak reschedule akışı düzeltildi.
- [x] [Review][Patch] LULLABY localization’da boş body/description ve type transition bypass’ları engellendi; domain ve DB katmanlarına aynı invariant eklendi.
- [x] [Review][Patch] Playback endpoint’inde auth ve processing-conflict response dokümantasyonu/409 eşlemesi tamamlandı.
- [x] [Review][Defer] Public/mobile registry ve asset-bundle tüketicilerinin content-scope processing’e geçirilmesi bu story’nin açık sınırı dışında; mobil/public endpoint çalışması sonraki yol haritasına bırakıldı.
- [x] [Review][Defer] Listening cover’ın opsiyonel kalması ile mevcut non-STORY worker’ın cover zorunluluğu ayrı bir asset-processing kararı gerektiriyor; playback modelini değiştirmeden sonraki processing çalışmasına bırakıldı.
- [x] [Review][Patch] Aggregate dışı localization yazımlarında LULLABY processing status’ı client-owned kalabiliyor [be/src/main/java/com/tellpal/v2/content/domain/Content.java:228]
- [x] [Review][Patch] Legacy LULLABY CONTENT processing kaydı backfill edilen playback source’u ile çelişirse migration sessizce eski kaydı koruyor [be/src/main/resources/db/migration/V25__add_lullaby_playback.sql:139]
- [x] [Review][Patch] Eşzamanlı ilk playback yazımları unique content_id ihlalini 500 olarak yüzeye çıkarabiliyor [be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java:193]
- [x] [Review][Patch] LULLABY global MUSICIAN kapsamı için admin API seviyesinde hata sözleşmesi ve başarılı readback testi eksik [be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java:329]

## Suggested Review Order

**Playback sözleşmesi ve aggregate sınırı**

- Content-level endpoint, tek playback yazımını ve processing planlamasını başlatır.
  [`ContentAdminController.java:152`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java#L152)

- AUDIO doğrulaması sonrası playback değişikliği ortak CONTENT delivery işine bağlanır.
  [`ContentManagementService.java:193`](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L193)

- Aggregate yalnız LULLABY playback’ini kabul eder ve localization alanlarını title-only sınırlar.
  [`Content.java:142`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L142)

- Tekil audio/süre child’ı content sahipliğini JPA seviyesinde korur.
  [`LullabyPlayback.java:19`](../../be/src/main/java/com/tellpal/v2/content/domain/LullabyPlayback.java#L19)

**Migration ve veritabanı invariant’ları**

- Legacy veriyi önce doğrular, eşit locale değerlerini tek playback’e backfill eder.
  [`V25__add_lullaby_playback.sql:1`](../../be/src/main/resources/db/migration/V25__add_lullaby_playback.sql#L1)

- Migration eski locale işlerini temizleyip shared CONTENT PENDING snapshot’ı oluşturur.
  [`V25__add_lullaby_playback.sql:118`](../../be/src/main/resources/db/migration/V25__add_lullaby_playback.sql#L118)

- Doğrudan SQL playback değişikliklerinde content type ve AUDIO referansı korunur.
  [`V25__add_lullaby_playback.sql:158`](../../be/src/main/resources/db/migration/V25__add_lullaby_playback.sql#L158)

**Projection ve processing**

- Admin read ortak playback’i ve tek processing snapshot’ını localization’lardan bağımsız map eder.
  [`ContentAdminQueryMapper.java:26`](../../be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java#L26)

- Shared source değişiklikleri tamamlanmış/başarısız işleri güvenli biçimde yeniden planlar.
  [`AssetProcessingService.java:225`](../../be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java#L225)

**Kanıt ve regression testleri**

- Legacy backfill, stale-job temizliği ve doğrudan SQL kuralları Testcontainers ile doğrulanır.
  [`LullabyPlaybackMigrationIntegrationTest.java:34`](../../be/src/test/java/com/tellpal/v2/content/migration/LullabyPlaybackMigrationIntegrationTest.java#L34)

- İki localization arasında tek playback ve title-only HTTP sözleşmesi korunur.
  [`ContentAdminIntegrationTest.java:888`](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java#L888)

- Processing source değişikliğinin yeni PENDING job ürettiği integration seviyesinde kanıtlanır.
  [`AssetProcessingIntegrationTest.java:207`](../../be/src/test/java/com/tellpal/v2/asset/AssetProcessingIntegrationTest.java#L207)
