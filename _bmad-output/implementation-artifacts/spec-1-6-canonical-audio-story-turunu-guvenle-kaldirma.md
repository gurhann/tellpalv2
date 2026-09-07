---
title: 'Story 1.6: Canonical AUDIO_STORY türünü güvenle kaldırma'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
baseline_commit: '9ade1a9fcafbc2f009479e900c9397c111d15aec'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - 'be/docs/adr/ADR-0007-category-type-aligns-with-content-type.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
  - '_bmad-output/implementation-artifacts/spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md'
  - '_bmad-output/specs/spec-story-audio-experience/SPEC.md'
  - '_bmad-output/specs/spec-story-audio-experience/audio-story-contract.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `AUDIO_STORY` hâlâ canonical content/category/processing türü olarak tutuluyor. Oysa sesli hikâye, aynı `STORY` içeriğinin localization bazlı narration deneyimidir; ayrı canonical kimlik ve kategori desteği çoğaltma ve veri geçişi riski yaratıyor.

**Approach:** Uygulama enum’larından ve CMS seçim yüzeylerinden canonical `AUDIO_STORY` değerini çıkar; mevcut `STORY`, `MEDITATION` ve `LULLABY` davranışlarını koru. Yeni Flyway migration’ı önce üç veri sınıfını read-only kontrol ederek legacy satır varsa anlaşılır hata ile durdurmalı, temiz veride constraint’leri daraltmalı.

## Boundaries & Constraints

**Always:** Migration’daki preflight, `contents.type`, `categories.type` ve `asset_processing.content_type` içindeki `AUDIO_STORY` satırlarını ayrı ayrı teşhis etmeli; başarısızlıkta hiçbir schema/data değişikliği yapmamalıdır. Temiz geçişte izin verilen canonical değerler yalnız `STORY`, `MEDITATION`, `LULLABY` olmalıdır. Nullable `asset_processing.content_type`, `page_count`, V22/V23 target-scope ve STORY narration kuralları korunur. API ve CMS `AUDIO_STORY` canonical create/update/filter seçimini göstermemeli veya kabul etmemelidir. Sesli hikâye deneyimi için `experienceType: AUDIO_STORY` projection sözleşmesi silinmez. Eski bağımsız audio-story kayıtlarının toplu importu bu story’nin dışındadır.

**Ask First:** Production’da legacy `AUDIO_STORY` satırı bulunur veya migration/test sırasında mevcut verinin silinmesi/dönüştürülmesi gerekirse dur ve kullanıcı onayı iste.

**Never:** Eski satırları otomatik silme, STORY’ye sessizce dönüştürme veya compatibility alias ekleme. Mobile/public response tasarımını, narration projection’ını, kategori/arama endpointlerini veya import pipeline’ını yeniden tasarlama.

## I/O & Edge-Case Matrix

| Senaryo | Girdi / Durum | Beklenen davranış | Hata |
|---|---|---|---|
| Legacy content | `contents.type = AUDIO_STORY` | Migration schema’ya dokunmadan durur | Hata `contents` sınıfını ve örnek kimliği belirtir |
| Legacy category/processing | İlgili tabloda `AUDIO_STORY` satırı | Migration schema’ya dokunmadan durur | Hata ilgili sınıfı belirtir |
| Temiz geçiş | Üç tabloda da blocker yok | V28 tamamlanır; üç check yalnız kalan canonical türleri kabul eder | N/A |
| Canonical API/CMS isteği | JSON/query/form `AUDIO_STORY` | 400; STORY/MEDITATION/LULLABY akışları çalışır | Enum/request validation hatası |

</frozen-after-approval>

## Code Map

- `be/src/main/resources/db/migration/V5__create_content_tables.sql:11-13`, `V17__align_category_types_with_content_types.sql:111-114`, `V13__add_asset_processing_context_columns.sql:9-21` -- daraltılacak eski check constraint’leri; yeni migration `V28__remove_audio_story_canonical_type.sql` preflight’i bunlardan önce çalıştırır.
- `be/src/main/resources/db/migration/V22__scope_asset_processing_targets.sql`, `V23__add_story_narrations.sql`, `V24__add_content_listening_cover.sql` -- korunacak target-scope, STORY narration ve cover kuralları.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentType.java`, `ContentApiType.java`, `category/domain/CategoryType.java`, `asset/api/AssetProcessingContentType.java`, `asset/domain/ProcessingContentType.java` -- canonical enum’lar ve processing tek-audio yardımcıları.
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java:592-598`, `asset/api/AssetProcessingCommands.java:281-284` -- localization body/audio ve asset processing doğrulamalarındaki AUDIO dalları.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java:325`, `category/web/admin/CategoryAdminController.java:189,204`, `asset/web/admin/AssetProcessingAdminController.java:157,198` -- enum binding ile 400 davranışı; ek whitelist icat edilmemeli.
- `cms/src/features/contents/{api, schema, model, components}`, `cms/src/features/categories/{api, schema, model}`, `cms/src/features/assets/{api, model}`, `cms/src/app/routes/contents/index.tsx` -- canonical type union/option/label ve localization koşulları; layout değişikliği yapmadan seçenekleri kaldır.
- `be/src/test/java/com/tellpal/v2/content/migration/`, `category/migration/`, `content/web/admin/ContentAdminIntegrationTest.java`, `category/web/admin/CategoryAdminIntegrationTest.java`, `content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java`, `cms/src/**/*.test.*` -- migration, API, registry ve CMS regression noktaları.
- `architecture.md:182,200`, `be/docs/project-memory.md:87`, `be/docs/admin-api-rules.md`, `be/docs/bootstrap-notes.md`, `be/docs/adr/ADR-0007-category-type-aligns-with-content-type.md` -- durable canonical tür dokümantasyonu; superseding ADR ve import kapsamı eklenmeli.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql` -- üç tabloyu read-only preflight ile kontrol et, temiz veride check constraint’leri daralt; scoped processing kurallarını koru.
- [x] Canonical Java enum/domain doğrulamaları -- `AUDIO_STORY` sabitini ve ilgili body/audio/tek-audio dallarını çıkar; kalan türlerin davranışını koru.
- [x] CMS type modelleri, schema’ları ve form koşulları -- canonical seçenek/etiketleri kaldır; sesli hikâye deneyimi için STORY narration mantığını koru.
- [x] Backend/CMS testleri -- her blocker sınıfında fail-fast ve veri/schema değişmezliğini, temiz geçişi, 400 API/CMS davranışını ve kalan tür regresyonlarını kanıtla.
- [x] `architecture.md`, `be/docs/project-memory.md`, `be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md` ve ilgili admin/bootstrap docs -- ADR-0007’yi supersede eden canonical kararını ve legacy import kapsam dışını belgele.

**Acceptance Criteria:**
- Given blocker satır, when V28 çalışır, then migration ilgili sınıfı açıkça belirterek başarısız olur ve satır/constraint değişmez.
- Given temiz veritabanı, when V28 tamamlanır, then `AUDIO_STORY` üç canonical constraint’te reddedilir; STORY processing/narration ve diğer scoped kurallar çalışır.
- Given admin API veya CMS canonical type isteği, when `AUDIO_STORY` gönderilir, then istek 400 olur; STORY/MEDITATION/LULLABY akışları etkilenmez.
- Given public/audio-story projection, when aynı STORY localization narration’ı okunur, then `experienceType: AUDIO_STORY` gibi projection desteği korunur.
- Given legacy bağımsız audio-story importu, when Story 1.6 değerlendirilir, then import yapılmaz ve kapsam dışı açıkça belgelenir.

## Design Notes

`AUDIO_STORY`’nin kaldırılması yalnız canonical write/read yüzeyine uygulanır. `STORY` localization narration’ı ve deneyim projection’ı ayrı kavramlar olduğundan, eski response uyumluluğu için kullanılan projection adı bu migration ile silinmez. Preflight’ın schema değişikliklerinden önce tek migration içinde bulunması, rollback gerektirmeden güvenli fail-fast davranışı sağlar.

## Verification

**Commands:**
- `cd be && ./mvnw test -Dtest=*AudioStory*Migration*,ContentAdminIntegrationTest,CategoryAdminIntegrationTest,JdbcContentRegistryReadRepositoryIntegrationTest` -- beklenen: blocker/clean migration, 400 API ve registry testleri başarılı.
- `cd cms && npm run test` -- beklenen: canonical type option/validation regression’ları başarılı.
- `cd be && ./mvnw verify` -- beklenen: tüm Flyway, Modulith ve backend testleri başarılı.
- `cd cms && npm run build` -- beklenen: CMS TypeScript/build başarılı.

## Suggested Review Order

**Canonical boundary and migration**

- V28 first performs class-specific read-only checks, then narrows constraints.
  [`V28__remove_audio_story_canonical_type.sql:1`](../../be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql#L1)

- Canonical content types now contain only the three supported editorial identities.
  [`ContentType.java:6`](../../be/src/main/java/com/tellpal/v2/content/domain/ContentType.java#L6)

- Durable ADR preserves the separate audio experience label without a canonical identity.
  [`ADR-0012-remove-audio-story-canonical-type.md:14`](../../be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md#L14)

**Validation and API behavior**

- Localization validation retains meditation text and meditation/lullaby audio rules.
  [`Content.java:567`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L567)

- Processing commands keep single-audio requirements scoped to meditation and lullaby.
  [`AssetProcessingCommands.java:281`](../../be/src/main/java/com/tellpal/v2/asset/api/AssetProcessingCommands.java#L281)

- Invalid legacy filter values are ignored by the CMS route instead of reaching the API.
  [`index.tsx:51`](../../cms/src/app/routes/contents/index.tsx#L51)

**Regression evidence**

- Migration tests cover blockers, schema immutability, existing rows, nullable processing, and scoped rules.
  [`CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java:36`](../../be/src/test/java/com/tellpal/v2/content/migration/CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java#L36)

- Public content/category filters reject the removed canonical value with HTTP 400.
  [`PublicDeliveryIntegrationTest.java:162`](../../be/src/test/java/com/tellpal/v2/content/web/mobile/PublicDeliveryIntegrationTest.java#L162)

- CMS create forms expose only Story, Meditation, and Lullaby.
  [`content-form.test.tsx:88`](../../cms/src/features/contents/components/content-form.test.tsx#L88)

### Review Findings

- [x] [Review][Patch] Admin/bootstrap dokümanlarında LULLABY için kaldırılmış `audioMediaId` zorunluluğunu düzelt; mevcut `Content` doğrulaması LULLABY localization’ını title/publication-only kabul ediyor. [`be/docs/admin-api-rules.md:221`](../../be/docs/admin-api-rules.md#L221)
- [x] [Review][Patch] Content oluşturma formunun `AUDIO_STORY` seçeneğini göstermediğini negatif assertion ile doğrula; mevcut test zaten bu assertion’ı içeriyordu. [`cms/src/features/contents/components/content-form.test.tsx`](../../cms/src/features/contents/components/content-form.test.tsx)
- [x] [Review][Patch] Content registry filtresinin yalnız dört geçerli seçeneği sunduğunu ve eski `?type=AUDIO_STORY` URL’sinin API’ye gönderilmediğini test et; filtre yüzeyinin negatif assertion’ı eklendi, URL normalizasyonu mevcut kodla korunuyor. [`cms/src/app/routes/contents/index.tsx:50`](../../cms/src/app/routes/contents/index.tsx#L50)
- [x] [Review][Patch] Admin content registry’de `type=AUDIO_STORY` sorgusunun 400 ile reddedildiğini regression testiyle kanıtla; mevcut `ContentAdminIntegrationTest` assertion’ı bu davranışı kapsıyor. [`be/src/main/java/com/tellpal/v2/content/web/admin/ContentRegistryAdminController.java:39`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentRegistryAdminController.java#L39)
- [x] [Review][Patch] Category update akışında `AUDIO_STORY` değerinin 400 ile reddedildiğini test et; mevcut integration testi bu davranışı kapsıyor. [`be/src/test/java/com/tellpal/v2/category/web/admin/CategoryAdminIntegrationTest.java`](../../be/src/test/java/com/tellpal/v2/category/web/admin/CategoryAdminIntegrationTest.java)
- [x] [Review][Patch] Asset-processing retry akışında `AUDIO_STORY` değerinin 400 ile reddedildiğini test et; mevcut integration testi schedule ve content retry akışlarını kapsıyor. [`be/src/test/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminIntegrationTest.java`](../../be/src/test/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminIntegrationTest.java)
- [x] [Review][Patch] CMS localization testlerinde LULLABY’nin title/publication-only alanlarını koruduğunu regression assertion ile doğrula; schema/form davranışı ve test eklendi. [`cms/src/features/contents/components/content-localization-form.test.tsx`](../../cms/src/features/contents/components/content-localization-form.test.tsx)
- [x] [Review][Patch] V28 temiz migration testini mevcut MEDITATION/LULLABY satırlarının ve V22/V23 scoped kurallarının migration sonrasında korunduğunu gösterecek şekilde genişlet. [`be/src/test/java/com/tellpal/v2/content/migration/CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java`](../../be/src/test/java/com/tellpal/v2/content/migration/CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java)
- [x] [Review][Patch] V28 processing blocker teşhisine `external_key` ekle; aynı content/language kapsamındaki satırların operasyonel olarak ayırt edilebilmesini sağla ve mesajı test et. [`be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql:35`](../../be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql#L35)
- [x] [Review][Patch] Kalan `Content.validateLocalizationFieldsForType` ve `requiresSingleAudioAsset` dalları için focused domain regression testleri ekle; mevcut ContentTest bu dalları zaten kapsıyor. [`be/src/main/java/com/tellpal/v2/content/domain/Content.java:565`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L565)
- [x] [Review][Patch] CMS media-processing yüzeylerinin `AUDIO_STORY` seçeneğini sunmadığını negatif unit/E2E assertion ile doğrula; içerik tipi union’ı ve mevcut content/category form assertion’ları seçeneği kapalı tutuyor. [`cms/src/features/assets/api/asset-processing-admin.ts:7`](../../cms/src/features/assets/api/asset-processing-admin.ts#L7)
- [x] [Review][Defer] CMS asset-processing request/response şemasında content-scoped job için nullable `languageCode` ve `targetScope/kind` alanlarının desteklenmemesi ayrı processing-contract çalışmasına bırakıldı [`cms/src/features/assets/api/asset-processing-admin.ts:29`](../../cms/src/features/assets/api/asset-processing-admin.ts#L29) — deferred, pre-existing
- [x] [Review][Defer] V13 `page_count` ve V22 parent/trigger invariant’larının migration regression kapsamının genişletilmesi ayrı schema-hardening çalışmasına bırakıldı [`be/src/test/java/com/tellpal/v2/content/migration/CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java:102`](../../be/src/test/java/com/tellpal/v2/content/migration/CanonicalAudioStoryTypeRemovalMigrationIntegrationTest.java#L102) — deferred, pre-existing
- [x] [Review][Defer] `ops/content-migration` çıktısındaki legacy `AUDIO_STORY` category map davranışı import-pipeline çalışmasına bırakıldı [`ops/content-migration/content_inventory.py:18`](../../ops/content-migration/content_inventory.py#L18) — deferred, pre-existing/out of scope
- [x] [Review][Defer] Public/mobile `experienceType: AUDIO_STORY` projection regression coverage sonraki endpoint/projection yol haritasına bırakıldı [`_bmad-output/implementation-artifacts/deferred-work.md`](../../_bmad-output/implementation-artifacts/deferred-work.md) — deferred, explicitly outside this story’s endpoint scope
- [x] [Review][Defer] V28’in ilk blocker sınıfında durup diğer sınıfları sonraki çalıştırmaya bırakması fail-fast tasarım tercihi olarak korundu [`be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql:7`](../../be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql#L7) — deferred, design choice
