---
title: 'İçerik contributor assignment düzenleme'
type: 'feature'
created: '2026-09-04'
status: 'done'
baseline_commit: '158358c40b912fc365cd5302c894f3b71fa5962a'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-assignment-edit/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-assignment-edit/assignment-edit-contract.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/adr/ADR-0009-registry-read-pagination.md'
  - '{project-root}/cms/AGENTS.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 7 ile içerik detayında contributor assignment kayıtları rol ve dil grupları altında yönetiliyor; ancak mevcut kayıt üzerinde kredi adı veya kapsam değişikliği yapılamıyor. Editör küçük bir düzeltme için atamayı kaldırıp yeniden oluşturmak zorunda kalıyor.

**Approach:** Contributor kimliğini değiştirmeden mevcut assignment için kredi adı, rol ve dil kapsamı düzenleme akışı sağla. Güncellemeyi assignment ID ile adreslenen backend kontratı üzerinden doğrula; rol veya kapsam değişiminde hedef grubu backend sırasına göre yeniden oluştur ve CMS panelini güncel veriye bağla.

## Boundaries & Constraints

**Always:** Düzenleme `assignmentId` ile hedeflenir. `creditName` trim edilir ve boşsa `null` olur. `role`, contributor profilindeki rollerden biri olmalıdır. `languageCode`, `null` veya içerikte mevcut geçerli bir dil olmalıdır. Rol/dil değişiminde duplicate kontrolü ve hedef grubun son sırası backend transaction’ında belirlenir; istemci `sortOrder` göndermez. Başarıda assignment sorgusu yenilenir. Hata dialog içinde kalır, mevcut değerler korunur ve yeniden deneme mümkün olur. Görünen metinler TR/EN i18n anahtarlarından gelir.

**Ask First:** Contributor kimliğini değiştirme, hedef grupta mevcut sırayı koruma, yeni rol ekleme veya public/mobile API davranışını değiştirme bu story kapsamına alınmaz.

**Never:** Contributor profilini assignment endpoint’i üzerinden düzenlemek; client-side `sortOrder` kalıcılaştırmak; rol uygunluğu veya duplicate kurallarını bypass etmek; assignmentı sessizce silip yeniden oluşturmak.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Kredi adı | Mevcut assignment ve yeni kredi adı | Trim edilmiş değer aynı assignment ID üzerinde görünür | Form/API hatası inline gösterilir |
| Rol/kapsam değişimi | Geçerli hedef rol ve dil | Assignment hedef grubun sonuna taşınır; eski grup normalize edilir | Profil rolü yoksa veya duplicate varsa mevcut değer korunur |
| Geçersiz dil | İçerikte bulunmayan dil kodu | Kalıcı değişiklik yapılmaz | 400 Problem Details dialog içinde görünür |
| Assignment bulunamadı | Geçersiz assignment ID | Kalıcı değişiklik yapılmaz | 404 Problem Details gösterilir |
| Eşzamanlı değişiklik | Başka editör aynı assignmentı değiştirmiştir | Backend güncel aggregate üzerinde karar verir | Conflict sonrası dialog açık kalır ve tekrar denenebilir |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java:20` -- assignment alanları ve normalize davranışı; kontrollü update metodu burada yaşamalı.
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java:268` -- mevcut rol uygunluğu, duplicate ve grup sırası kuralları; update için assignment bulma ve hedef grup yardımcıları burada yeniden kullanılmalı.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementCommands.java:56` -- mevcut assignment command normalizasyonu; yeni update command aynı ID/rol/language doğrulama stilini izlemeli.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java:169` -- assign/reorder transaction orkestrasyonu; update transaction’ı aynı aggregate sahipliği ve mapper akışını kullanmalı.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java:150` -- admin assignment endpointleri ve OpenAPI annotation stili; yeni PUT endpoint’i burada, mevcut exception handler biçimiyle eklenmeli.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java:119` -- assignment not-found/problem detail dönüşleri; update hataları aynı HTTP hata sözleşmesine bağlanmalı.
- `cms/src/features/contributors/components/content-contributor-panel.tsx:26` -- gruplu satır görünümü; düzenle eylemi ve dialog açılış bağlamı burada bağlanmalı.
- `cms/src/features/contributors/components/assign-contributor-dialog.tsx:51` -- mevcut form, rol ve scope varsayılanları; edit formu ortak alan davranışını burada kopyalamadan ayırmalı.
- `cms/src/features/contributors/components/contributor-form-dialog.tsx:87` -- Dialog + `useZodForm` + ProblemAlert + SubmitButton için mevcut form örneği.
- `cms/src/features/contributors/mutations/use-contributor-actions.ts:120` -- assignment mutation invalidasyon kalıbı; update mutation aynı query key’i yenilemeli.
- `cms/src/features/contributors/api/contributor-admin.ts:79` -- assignment response schema ve endpoint tanımları; update request/response şeması buraya eklenmeli.
- `cms/src/i18n/messages.ts:84` -- picker ve contributor mesajları; düzenleme, pending, hata ve scope metinleri iki locale eklenmeli.

## Tasks & Acceptance

**Execution:**

- [x] `be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java` ve `Content.java` -- assignment ID ile kontrollü alan güncelleme ve hedef grup append kurallarını ekle -- domain invariant’ları aggregate içinde tut.
- [x] `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementCommands.java` ve `ContributorManagementService.java` -- update command ve transaction orkestrasyonunu ekle -- rol/language/duplicate doğrulamasını backend’de uygula.
- [x] `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java` -- `PUT /api/admin/contents/{contentId}/contributors/{assignmentId}` endpoint’i ve OpenAPI hata cevaplarını ekle -- admin kontratını görünür kıl.
- [x] `cms/src/features/contributors/api/contributor-admin.ts` ve `use-contributor-actions.ts` -- update request, response ve mutation invalidasyonunu bağla -- CMS backend kontratını kullanmalı.
- [x] `cms/src/features/contributors/components/edit-content-contributor-dialog.tsx` -- mevcut değerlerle açılan, TR/EN doğrulamalı edit formunu oluştur -- hata halinde açık ve yeniden denenebilir kalmalı.
- [x] `cms/src/features/contributors/components/content-contributor-panel.tsx` ve `cms/src/i18n/messages.ts` -- satır düzenle eylemini ve tüm görünen metinleri bağla -- başarıda grupları yenile, hata durumunu koru.
- [x] Backend domain/controller/integration ile CMS component/mutation testlerini ekle -- I/O matrisindeki başarı, conflict, invalid language ve rollback davranışlarını kanıtla.

**Acceptance Criteria:**

- Given mevcut assignment vardır, when editör düzenlemeyi açar, then form kredi adı, rol ve dil kapsamını mevcut değerlerle gösterir.
- Given geçerli kredi adı kaydedilir, when API başarılı döner, then aynı `assignmentId` güncel kredi adıyla görünür.
- Given rol veya dil kapsamı değiştirilir, when API başarılı döner, then assignment hedef grubun sonuna eklenir ve eski grup sırası normalize edilir.
- Given contributor profilinde rol yoktur veya hedef grupta duplicate vardır, when editör kaydeder, then backend reddeder ve dialog inline hata ile açık kalır.
- Given dil kodu içerikte yoktur veya assignment bulunamaz, when editör kaydeder, then Problem Details gösterilir ve mevcut assignment değişmez.
- Given kullanıcı Türkçe veya İngilizce kullanır, when düzenleme akışı görünür, then tüm görünen metinler i18n anahtarlarından gelir.

## Spec Change Log

## Design Notes

Contributor kimliği immutable kalır; başka bir kişiye geçiş Story 7’deki kaldırma + yeni atama akışıdır. Rol/dil değişimi grup taşımasıdır ve hedef grubun sonuna eklenir. CMS dialogu mutation süresince kilitlenir; başarıda assignment query invalidation, hatada açık form ve inline Problem Details kullanılır.

## Verification

**Commands:**

- `cd be && .\\mvnw.cmd test` -- expected: domain, controller ve application testleri başarılı.
- `cd cms && .\\node_modules\\.bin\\tsc.cmd -b` -- expected: TypeScript başarılı.
- `cd cms && .\\node_modules\\.bin\\vitest.cmd run src/features/contributors/components/edit-content-contributor-dialog.test.tsx src/features/contributors/mutations/use-contributor-actions.test.tsx` -- expected: dialog ve mutation testleri başarılı.
- `cd cms && .\\node_modules\\.bin\\vite.cmd build` -- expected: üretim derlemesi başarılı.

**Executed evidence:**

- Backend compile succeeded with Maven 3.9.12.
- `ContentTest`, `ContributorAdminControllerTest`, and `ContributorAdminIntegrationTest` succeeded: 54 tests passed, including localized-scope persistence, invalid-language rollback, and missing-assignment Problem Details.
- CMS targeted contributor tests succeeded: 13 tests passed; TypeScript/Vite build and targeted ESLint checks succeeded.
- Full CMS suite was not used as a gate because pre-existing auth integration tests fail and hang in this environment; no Story 8 test failed.

## Suggested Review Order

**Domain invariants and persistence**

- Assignment ID, contributor identity, duplicate checks, and target-group ordering are owned by the aggregate.
  [`Content.java:360`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L360)

- Assignment fields are updated through one controlled normalization method.
  [`ContentContributor.java:85`](../../be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java#L85)

- The transaction locks the content aggregate before applying and flushing the edit.
  [`ContributorManagementService.java:191`](../../be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java#L191)

**Admin API contract**

- The assignment-ID PUT endpoint exposes the immutable-identity update contract and Problem Details responses.
  [`ContributorAdminController.java:164`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java#L164)

- Missing assignment IDs map to the admin API’s 404 error shape.
  [`ContentAdminExceptionHandler.java:131`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java#L131)

**CMS editing flow**

- The dialog keeps contributor identity readonly while allowing role, scope, and credit-name edits.
  [`edit-content-contributor-dialog.tsx:49`](../../cms/src/features/contributors/components/edit-content-contributor-dialog.tsx#L49)

- The panel exposes the edit action beside existing reorder and unassign controls.
  [`content-contributor-panel.tsx:194`](../../cms/src/features/contributors/components/content-contributor-panel.tsx#L194)

- Mutation success refreshes the assignment query; failures remain recoverable in the open dialog.
  [`use-contributor-actions.ts:182`](../../cms/src/features/contributors/mutations/use-contributor-actions.ts#L182)

**Supporting contract and verification**

- Frontend request typing and response validation mirror the backend assignment endpoint.
  [`contributor-admin.ts:177`](../../cms/src/features/contributors/api/contributor-admin.ts#L177)

- Domain tests cover identity preservation, group movement, duplicate rejection, and blank-credit normalization.
  [`ContentTest.java:95`](../../be/src/test/java/com/tellpal/v2/content/domain/ContentTest.java#L95)

- Integration tests prove localized scope persistence, invalid-language rollback, and missing-assignment handling.
  [`ContributorAdminIntegrationTest.java:731`](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java#L731)

- Panel tests cover opening current values, successful submission, and recoverable update failures.
  [`content-contributor-panel.test.tsx:204`](../../cms/src/features/contributors/components/content-contributor-panel.test.tsx#L204)
