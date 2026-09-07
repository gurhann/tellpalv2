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
