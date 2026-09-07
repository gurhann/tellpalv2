---
title: 'Story 1.5: Ninni için katalogdan sıralı enstrüman seçme'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'f3188df5bf7c2ad6439f5eb0e2485192704ba76b'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
  - '_bmad-output/implementation-artifacts/spec-1-4-ninninin-ortak-playbackini-ve-baslik-only-localizationini.md'
  - '_bmad-output/specs/spec-lullaby-shared-audio-and-instruments/lullaby-data-and-api-contract.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Ninninin enstrüman bilgisi modellenmiyor. Serbest metin veya contributor kullanımı aynı ninninin diller arasında farklı görünmesine ve güvenilir sıralı bilgi üretilememesine yol açar.

**Approach:** `content` modülünde kararlı kodlu, locale bazlı etiketli bir katalog kur. `Content` altında `LullabyInstrument` bağlantılarıyla ninni seçimlerini benzersiz ve sıfırdan başlayan `displayOrder` ile atomik olarak değiştir; admin okumasında kod, locale etiketi ve sıra birlikte dönsün.

## Boundaries & Constraints

**Always:** Seçim yalnız `LULLABY` content için geçerlidir ve localization satırlarına kopyalanmaz. Komut kararlı katalog `code` değerleriyle çalışır; bilinmeyen veya duplicate code reddedilir. Güncelleme eksiksiz, tekrarsız permutation olmalı; doğrulama hatasında mevcut seçimler korunmalıdır. `displayOrder` sıfırdan başlar ve benzersizdir. Katalog kodu, locale ve görünen ad ayrı tablolardadır. `MUSICIAN` contributor ayrı kavramdır. V26 Türkçe seed listesi: `Çelesta`, `Bell`, `Keman`, `Rhodes`, `Glockenspiel`, `Arp`, `Vibrafon`, `Yaylı Orkestra`.

**Ask First:** Yok. Katalog emeklilik/silme işlemleri, katalog CRUD yüzeyi ve CMS ekranı deferred-work’e taşınmıştır; bu story yalnız seed, locale read ve ninni selection/reorder sözleşmesini teslim eder.

**Never:** Public/mobile response, arama/kategori endpointleri veya CMS görsel akışı değiştirilmez. Localization başlık-only kuralı bozulmaz. Serbest metin, contributor ID’si veya dile göre farklı ninni seçimi eklenmez. `LullabyPlayback` audio/süre/kapağının sahipliği yeniden modellenmez.

## I/O & Edge-Case Matrix

| Senaryo | Girdi / Durum | Beklenen davranış | Hata |
|---|---|---|---|
| Katalog okuma | Desteklenen locale | `code` + `displayName` stabil sırada döner | Locale etiketi yoksa doğrulama hatası |
| Seçim | LULLABY + `[code1, code2]` | Linkler 0,1 sırasıyla yazılır | Ninni dışı content reddedilir |
| Geçersiz değişiklik | Bilinmeyen/duplicate/eksik permutation | Önceki seçimler korunur | 400/çatışma hatası |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java:59-152,334-469` -- LULLABY child sahipliği ve atomik selection/reorder invariant’ları.
- `be/src/main/java/com/tellpal/v2/content/domain/LullabyPlayback.java:18-51` -- ortak audio/süre sınırı; enstrümanlar bunu çoğaltmadan content-level kalmalı.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java:191-245` -- transaction ve playback write-lock kalıbı.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementCommands.java:28-34`, `ContentManagementResults.java:61-92` -- selection command/result uzantıları.
- `be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java:24-82`, `AdminContentView.java:8-42`, `ContentAdminController.java:152-170` -- admin playback projection’ına ordered instrument read/write ekleme noktaları.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentRepository.java:38-45`, `SpringDataContentRepository.java:64-78` -- eşzamanlı aggregate yükleme/lock.
- `be/src/main/resources/db/migration/V25__add_lullaby_playback.sql` -- yeni `V26__add_lullaby_instrument_catalog.sql` için LULLABY trigger/check örneği.
- `be/src/main/java/com/tellpal/v2/shared/domain/LanguageCode.java:13-20` -- desteklenen locale kümesi.
- `be/src/test/java/com/tellpal/v2/content/domain/ContentTest.java:239-289`, `.../web/admin/ContentAdminIntegrationTest.java:888-1000` -- domain ve admin HTTP regression uzantıları.

## Tasks & Acceptance

**Execution:**
- [x] `InstrumentCatalog`, `InstrumentCatalogLocalization`, `LullabyInstrument` -- katalog identity/locale label ve content-level ordered link domain’ini oluştur.
- [x] `V26__add_lullaby_instrument_catalog.sql` -- üç tabloyu, LULLABY ownership trigger’ını, duplicate/order/FK kısıtlarını ve sekiz Türkçe seed kaydını ekle.
- [x] `Content` ve persistence adapter’ları -- code listesiyle atomik replace/reorder; unknown/duplicate doğrulaması.
- [x] `ContentManagementService`, admin command/result/mapper/controller -- `PUT /api/admin/contents/{contentId}/instruments` ve locale etiketli katalog read sözleşmesini ekle; public/mobile endpoint açma.
- [x] Domain, admin HTTP ve migration testleri -- çoklu seçim, sıra, locale label, duplicate, rollback ve MUSICIAN ayrımını kanıtla.

**Acceptance Criteria:**
- Given desteklenen locale, when katalog okunur, then her seçenek kararlı `code` ve o locale’a ait `displayName` ile döner; ad localization’a kopyalanmaz.
- Given LULLABY, when `[codeA, codeB]` kaydedilir, then content-level ilişkiler `displayOrder` 0 ve 1 ile kalıcılaşır ve admin read aynı sırayı döner.
- Given bilinmeyen, duplicate veya eksik permutation, when seçim güncellenir, then istek reddedilir ve önceki geçerli seçimler değişmez.
- Given aynı ninnide MUSICIAN contributor, when instrument seçimi yapılır, then contributor kaydı değişmez ve kavramlar ayrı response alanlarında kalır.

## Design Notes

Seçim ilişkilerini `Content` aggregate’ine bağlamak Story 1.4’teki ortak playback ve global MUSICIAN sahipliğiyle aynı dil-bağımsız sınırı korur. Katalog emeklilik/silme davranışı ve katalog yönetim ekranı sonraki çalışmaya bırakılır; mevcut seçimlerin çözümleme sözleşmesi değişmez.

## Verification

**Commands:**
- `cd be && ./mvnw test -Dtest=ContentTest,ContentManagementIntegrationTest,ContentAdminIntegrationTest,InstrumentCatalogMigrationIntegrationTest` -- beklenen: domain, admin HTTP ve V26 migration senaryoları başarılı.
- `cd be && ./mvnw verify` -- beklenen: tüm Flyway, Spring Modulith ve backend regression kontrolleri başarılı.

## Suggested Review Order

**Selection transaction and validation**

- Locale labels are resolved before links are cleared, preserving the previous selection on 400 responses.
  [`ContentManagementService.java:249`](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L249)

- The aggregate owns zero-based ordered links and rejects non-lullaby usage at the domain boundary.
  [`Content.java:172`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L172)

- The admin PUT accepts stable codes and passes the requested locale into the atomic application operation.
  [`ContentAdminController.java:229`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java#L229)

**Persistence and catalog identity**

- V26 separates code identity, locale labels, and content-level links with database ownership constraints.
  [`V26__add_lullaby_instrument_catalog.sql:1`](../../be/src/main/resources/db/migration/V26__add_lullaby_instrument_catalog.sql#L1)

- Catalog entities keep language-independent codes separate from localized display names.
  [`InstrumentCatalog.java:26`](../../be/src/main/java/com/tellpal/v2/content/domain/InstrumentCatalog.java#L26)

**Admin read projection and verification**

- Shared playback admin output includes the persisted ordered instrument projection when playback exists.
  [`ContentAdminQueryMapper.java:29`](../../be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java#L29)

- Migration tests verify all Turkish seeds, direct SQL invariants, and LULLABY type-transition protection.
  [`InstrumentCatalogMigrationIntegrationTest.java:20`](../../be/src/test/java/com/tellpal/v2/content/migration/InstrumentCatalogMigrationIntegrationTest.java#L20)

- Integration tests cover reorder, invalid-update preservation, locale-label failures, and playback projection.
  [`ContentAdminIntegrationTest.java:1009`](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java#L1009)
