---
title: 'Story 1.3: Kaynak ve dinleme kapaklarının ayrı sahipliği'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
baseline_commit: '722f4df2c952b78996f310e97407d1597be56f67'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
  - '_bmad-output/specs/spec-ortak-textless-kapak-sahipligi/SPEC.md'
  - '_bmad-output/specs/spec-ortak-textless-kapak-sahipligi/cover-ownership.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `STORY` içeriklerinde mevcut `textlessCoverMediaId`, normal hikâye kapağının yazısız çeviri/illüstrasyon kaynağıdır. Sesli hikâye deneyiminin kapağı bununla aynı değildir; tek alanda birleştirilirse çizerlerin kullandığı kaynak ile mobil dinleme görseli karışır. Ninni ve meditasyonun textless dinleme kapakları da localization içinde tekrarlanmamalıdır.

**Approach:** Mevcut `textlessCoverMediaId` alanını STORY kaynak kapağı olarak koru ve `STORY`, `MEDITATION`, `LULLABY` için ayrı, isteğe bağlı content-level `listeningCoverMediaId` referansı ekle. Admin/CMS yazma ve okuma sözleşmeleri iki alanı açıkça ayırsın; localization kapağı, anlatım, sayfa sesleri ve processing akışı değişmesin.

## Boundaries & Constraints

**Always:** `textlessCoverMediaId` yalnızca STORY kaynak kapağı anlamını korur; `ContentLocalization.coverMediaId` dile özgü başlıklı okuma kapağıdır. `listeningCoverMediaId` yalnızca STORY sesli deneyimi, MEDITATION ve LULLABY için geçerlidir; pozitif bir IMAGE asset olmalıdır ve tüm dillerce paylaşılır. Her alan isteğe bağlıdır; dinleme kapağının eksikliği mevcut yayınlanabilirliği veya asset processing durumunu değiştirmez. Content yalnızca asset ID saklar; URL/variant/processing çıktısı asset modülünden çözülür.

**Ask First:** Mevcut veride STORY dışı içeriklerde dolu `textlessCoverMediaId` bulunursa, bu legacy değerlerin korunması veya temizlenmesi için dur ve karar iste.

**Never:** `textlessCoverMediaId`yi dinleme kapağı olarak yeniden kullanma; localization tablosuna dinleme kapağı kopyalama; normal STORY kapağını content scope'a taşıma; `AUDIO_STORY` import/kaldırma, public/mobile endpoint değişikliği veya Story 1.4 playback işleme davranışını bu story'ye çekme.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| STORY iki kapak | STORY update with source and listening IMAGE IDs | Both IDs persist and are returned distinctly | N/A |
| Dinleme kapağı güncelleme | `tr` and `en` localizations exist | Only content listening cover changes; localized covers/narrations stay unchanged | N/A |
| Yanlış asset | Listening/source ID points to AUDIO or missing asset | No partial update | 400 with field-specific problem |
| STORY dışı kaynak kapak | Non-STORY tries to set non-null source cover | Existing source semantics are not silently reinterpreted | Halt if existing legacy data is found; otherwise reject new misuse |
| Dinleme kapağını kaldırma | Listening cover is null | Content stores null; publication and processing remain unchanged | N/A |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java:52-154` -- mevcut `textlessCoverMediaId` alanı ve aggregate mutator; ayrı listening cover alanı ve type-scoped invariant burada yaşar.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementCommands.java:35-65` -- content update command; yeni ID'nin pozitif normalizasyonu ve API command sınırı.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java:62-80` -- asset referansı doğrulama ve content update koordinasyonu; IMAGE doğrulamasını public asset API ile tekrar kullan.
- `be/src/main/java/com/tellpal/v2/content/api/ContentReference.java`, `AdminContentView.java`, `ContentApiMapper.java` -- cross-module/admin read projection'ları; iki content-level ID'yi karıştırmadan taşı.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java:250-276` -- PUT request/response contract'ı ve OpenAPI validation; field-specific 400 davranışını koru.
- `be/src/main/resources/db/migration/V19__add_content_textless_cover.sql` -- mevcut kaynak kapak migration'ı; yeni migration'da ayrı nullable kolonu, pozitif check ve partial index ekle.
- `cms/src/features/contents/api/content-admin.ts`, `schema/content-schema.ts`, `mutations/use-save-content.ts` -- admin DTO, Zod form modeli ve update payload; `listeningCoverMediaId`yi `textlessCoverMediaId`den ayrı bağla.
- `cms/src/features/contents/model/content-view-model.ts` -- content summary mapping; source ve listening asset ID/flag'lerini ayrı tut.
- `cms/src/features/contents/components/content-form.tsx` ve `cms/src/app/routes/contents/detail.tsx` -- content-level CMS editörü; dinleme kapağı yalnızca STORY/MEDITATION/LULLABY için görünür.
- `cms/src/features/contents/components/content-textless-cover-form.tsx` ve `cms/src/app/routes/story-pages.tsx` -- mevcut STORY kaynak kapağı/source-images akışı; listening kapağıyla birleştirme.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminControllerTest.java`, `ContentAdminIntegrationTest.java`, `cms/src/features/contents/mutations/use-save-content.test.tsx`, `content-form.test.tsx` -- sözleşme, izolasyon, validation ve form regression testleri.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/resources/db/migration/V{next}__add_content_listening_cover.sql` -- ayrı nullable ID kolonu, pozitif check ve index ekle; existing source cover verisine dokunma.
- [x] `be/src/main/java/com/tellpal/v2/content/{domain,application,api,web/admin}` -- listening cover alanını STORY/MEDITATION/LULLABY ile sınırla, IMAGE doğrula, admin response/request/projection'a ekle ve source/localization alanlarını koru.
- [x] `be/src/test/java/com/tellpal/v2/content/...` -- iki STORY kapağının ayrışmasını, dil izolasyonunu, yanlış asset/type reddini, clear davranışını ve migration kısıtlarını test et.
- [x] `cms/src/features/contents/{api,schema,model,mutations,components}` ve `cms/src/app/routes/contents/detail.tsx` -- content-level listening cover seçicisini ekle; kaynak kapağı source-images akışında bırak; desteklenmeyen türlerde alanı gizle.
- [x] `cms/src/features/contents/**/*.test.tsx` -- payload, prefill, type visibility ve source/listening ayrımı için regression testleri ekle.

**Acceptance Criteria:**
- Given a STORY with a source cover and listening cover, when either is updated, then the other ID and every localization cover remain unchanged.
- Given STORY localizations in two languages, when the listening cover is read or changed, then both languages expose the same listening cover without copying it into localization rows.
- Given a MEDITATION or LULLABY, when a valid IMAGE is assigned as listening cover, then it is returned at content scope and is not shown in localization forms.
- Given an AUDIO, missing, or non-positive asset ID, when a cover update is submitted, then the API returns 400 and persists no partial change.
- Given existing content with no listening cover, when it is read or published, then behavior remains unchanged and no processing job is created.

## Spec Change Log

- 2026-09-07: Review found that database and test coverage did not enforce the type-scoped ownership split or the two-cover distinction. Added migration checks, identical-ID rejection, LULLABY/AUDIO_STORY coverage, interactive picker coverage, and canonical admin documentation. KEEP: source, localization, and listening covers remain separate and optional; omitted cover fields follow the documented full-update semantics.

## Design Notes

The three concepts are deliberately named by ownership and use: source cover supports translation artwork, localization cover supports the reading experience, and listening cover supports audio-first surfaces. A generic `textlessCover` response field for the latter would recreate the ambiguity this story removes.

## Verification

**Commands:**
- `cd be && ./mvnw test` -- expected: backend tests, including migration and admin integration coverage, pass.
- `cd cms && npm run test` -- expected: CMS unit/integration tests pass.
- `cd cms && npm run build` -- expected: TypeScript and production build pass.

## Suggested Review Order

**Sahiplik ve domain kuralları**

- Üç kapak rolünü aggregate seviyesinde ayırır ve içerik türü invariantlarını uygular.
  [`Content.java:171`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L171)

- STORY kaynak kapağı ile dinleme kapağını veritabanında bağımsız ve güvenli tutar.
  [`V24__add_content_listening_cover.sql:5`](../../be/src/main/resources/db/migration/V24__add_content_listening_cover.sql#L5)

- Kararın kalıcı mimari gerekçesini ve localization sınırını kayda geçirir.
  [`ADR-0010-content-cover-ownership.md:14`](../../be/docs/adr/ADR-0010-content-cover-ownership.md#L14)

**Admin sözleşmesi ve doğrulama**

- Asset doğrulamasını iki ayrı alan için aynı transaction öncesi akışta yürütür.
  [`ContentManagementService.java:70`](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L70)

- Admin PUT response/request modelinde iki content-level ID’yi ayrı taşır.
  [`ContentAdminController.java:250`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java#L250)

**CMS bağlama**

- Dinleme kapağı seçicisini yalnızca STORY, MEDITATION ve LULLABY güncellemelerinde gösterir.
  [`content-form.tsx:140`](../../cms/src/features/contents/components/content-form.tsx#L140)

- Seçilen asset ID’sini source cover alanından bağımsız update payload’ına bağlar.
  [`use-save-content.ts:1`](../../cms/src/features/contents/mutations/use-save-content.ts#L1)

**Regression kapsamı**

- İki cover ID’sinin ayrışmasını, tür sınırlarını ve migration constraint’lerini doğrular.
  [`ContentAdminIntegrationTest.java:1`](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java#L1)

- CMS prefill, payload ve tür görünürlüğü için dinleme kapağı regression’larını kapsar.
  [`content-form.test.tsx:1`](../../cms/src/features/contents/components/content-form.test.tsx#L1)

### Review Findings

- [x] [Review][Patch] V24 migration’ı legacy STORY-dışı kaynak kapaklarını önce tespit edip açık hata ile durdurmalı [be/src/main/resources/db/migration/V24__add_content_listening_cover.sql:8] — seçilen politika veriyi otomatik değiştirmemeli; mevcut `MEDITATION`, `LULLABY` veya `AUDIO_STORY` satırları raporlanarak constraint eklenmeden önce migration durmalı. (high; kullanıcı kararı: seçenek 1)
- [x] [Review][Patch] Asset hataları dinleme kapağı alanına bağlanmıyor [be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java:243] — `asset_not_found` ve `asset_media_type_mismatch` response’ları `fieldErrors` üretmediği için CMS yalnızca genel hata gösteriyor; `textlessCoverMediaId` ve `listeningCoverMediaId` için alan bazlı hata eşlemesi eklenmeli. (medium)
- [x] [Review][Patch] CMS update fixture’ı yeni response alanlarını ve başarılı kaydı doğrulamıyor [cms/e2e/content.spec.ts:245] — mock PUT response’unda cover ID’leri eksik olduğundan Zod parse hatası oluşabiliyor; fixture iki alanı döndürmeli ve test başarılı kaydı/genel hata yokluğunu assert etmeli. (medium)
- [x] [Review][Patch] Cache regression testi dinleme kapağının kaydedildiğini gözlemlemiyor [cms/src/features/contents/mutations/use-save-content.test.tsx:182] — list/detail cache assertion’larına `listeningCoverAssetId` ve `hasListeningCover` eklenmeli. (low)
- [x] [Review][Patch] Form reset sonrası dinleme kapağının korunması test edilmiyor [cms/src/features/contents/components/content-form.test.tsx:201] — `mapContentResponseToFormValues` üzerinden save response sonrası picker’ın seçili ID’yi koruduğu doğrulanmalı. (low)
- [x] [Review][Patch] Content detail visual snapshot’ları yeni listening-cover alanını kapsamıyor [cms/e2e/visual/content-detail.visual.spec.ts:141] — dört viewport baseline’ı yeni editör yüzeyiyle güncellenmeli. (medium)
- [x] [Review][Patch] Admin list projection için listening-cover regression assertion’ı eksik [be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java:559] — `GET /api/admin/contents` listesinin `listeningCoverMediaId` taşıdığı doğrulanmalı. (low)
- [x] [Review][Patch] Metadata açıklaması formun artık cover alanlarını da güncellediğini söylemiyor [cms/src/features/contents/components/content-form.tsx:79] — Türkçe/İngilizce helper copy external key, age ve active ile sınırlı kalmamalı; listening cover da belirtilmeli. (low)
- [x] [Review][Defer] Tam PUT akışında eşzamanlı edit çakışması [cms/src/app/routes/contents/detail.tsx:445] — form iki cover ID’sini snapshot olarak gönderiyor; bu, mevcut metadata/source-cover güncelleme modelinin önceden var olan concurrency davranışıdır ve ayrı optimistic-locking/PATCH kararı gerektirir; deferred, pre-existing.
- [x] [Review][Defer] Upload tamamlanmadan metadata submit edilmesi [cms/src/features/assets/components/asset-picker-field.tsx:69] — genel asset picker upload UX’inin mevcut davranışıdır; upload-state ile parent submit koordinasyonu ayrı bir UX hardening işine deferred, pre-existing.
- [x] [Review][Defer] Eski planlama ve historical architecture belgelerinde tek cover sahipliği [ _bmad-output/specs/spec-shared-textless-cover/SPEC.md:29 ] — canonical Story 1.3 belgeleri düzeltilmiş olsa da eski kaynaklar supersede edilmeden duruyor; gelecekteki planlama tutarlılığı için deferred, pre-existing.
