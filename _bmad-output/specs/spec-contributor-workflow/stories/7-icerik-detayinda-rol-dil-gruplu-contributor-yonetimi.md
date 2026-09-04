---
title: 'İçerik detayında rol ve dil gruplu contributor yönetimi'
type: 'feature'
created: '2026-09-04'
status: 'done'
baseline_commit: '491b6cdab86f884ee4239e490b51bf31f433a5e9'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/contributor-model-and-workflows.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/stories/4-contributor-atama-ve-siralama-kontratini-guclendir.md'
  - '{project-root}/cms/AGENTS.md'
  - '{project-root}/cms/docs/ui-standards.md'
---

## Intent

**Problem:** İçerik detayındaki contributor alanı tek bir genel atama eylemiyle çalışıyor; mevcut krediler rol ve dil kapsamına göre anlaşılır gruplar halinde sunulmuyor. Editör doğru role contributor eklemek, mevcut kaydı kaldırmak ve aynı grup içinde sıralamak için yeterli bağlama sahip değil.

**Approach:** Contributor bölümünü seçili dil çalışma alanının altına taşı ve rollere göre grupla. Her rol başlığında role-scoped picker açan doğrudan ekleme eylemi göster; mevcut kayıtları dil/global alt gruplarında listele. Kaldırma ve grup-içi reorder işlemlerini mevcut backend kontratlarını kullanarak bağla.

## Boundaries & Constraints

**Always:** Roller AUTHOR, ILLUSTRATOR, NARRATOR ve MUSICIAN olarak ayrı başlıklarda gösterilir. Her assignment yalnızca kendi role/dil grubunda sıralanır. Yeni kayıt backend tarafından grubun sonuna eklenir. Reorder yalnız aynı grubun assignment ID permütasyonuyla yapılır; optimistic hata durumunda önceki sıra geri yüklenir ve inline hata gösterilir. NARRATOR varsayılan olarak seçili içerik dilini, diğer roller global kapsamı kullanır. Görünen metinler TR/EN i18n anahtarlarından gelir. Mobil/public API davranışı değişmez.

**Ask First:** Backend assignment veya reorder sözleşmesini değiştirmek; rol/dil grupları arasında taşıma; bağımsız contributor detay sayfası; yeni rol türleri; public/mobile API; sürükle-bırak kütüphanesi eklemek.

**Never:** Client-side sortOrder hesaplayıp kalıcılaştırmak; farklı rol veya dil grubuna sessizce taşımak; role uygun olmayan picker açmak; mevcut assignment kurallarını bypass etmek; contributor alanını açıklamasız genel bir not kartına dönüştürmek.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Rol grupları | İçerikte farklı roller ve kapsamlar vardır | Her rol başlığı altında global/lokal alt gruplar görünür | Boş rol grubu için role özel ekleme eylemi korunur |
| Role özel ekleme | Editör Yazar ekle eylemini seçer | Picker AUTHOR bağlamıyla açılır ve yeni kayıt doğru gruba atanır | API problemi bölüm içinde gösterilir |
| Dil varsayılanı | Seçili locale `tr`, rol NARRATOR | Yeni narrator `tr` kapsamıyla başlar | Geçersiz locale form hatası olur |
| Kaldırma | Mevcut assignment seçilir | Kaldırma onayla yapılır | Conflict sonrası mevcut veri korunur |
| Grup-içi reorder | Aynı rol/dil grubunda en az iki kayıt vardır | Klavye veya sürükle-bırak ile sıra değişir | Reorder başarısızsa eski sıra geri gelir |
| Grup sınırı | Başka rol/dil grubuna taşıma denenir | İşlem reddedilir; gruplar değişmez | Inline açıklayıcı hata gösterilir |

## Code Map

- `cms/src/features/contributors/components/content-contributor-panel.tsx` -- rol/dil gruplu görünüm, role özel ekleme ve mevcut assignment eylemleri.
- `cms/src/features/contributors/components/assign-contributor-dialog.tsx` -- role prop ile picker açılışı ve varsayılan kapsam.
- `cms/src/features/contributors/components/unassign-contributor-button.tsx` -- kaldırma eylemi ve onay akışı.
- `cms/src/features/contributors/mutations/use-contributor-actions.ts` -- assignment update/reorder mutation orkestrasyonu.
- `cms/src/features/contributors/api/contributor-admin.ts` -- mevcut assignment ve reorder HTTP sözleşmeleri.
- `cms/src/features/contents/components/content-detail-page-shell.tsx` -- contributor bölümünün locale workspace sonrasındaki yerleşimi.
- `cms/src/i18n/messages.ts` -- rol başlıkları, grup açıklamaları, reorder ve hata metinleri.

## Tasks & Acceptance

**Execution:**

- [x] `content-contributor-panel.tsx` -- assignment'ları rol ve dil kapsamına göre grupla; her gruba role özel ekle eylemi ekle.
- [x] Content detail composition -- contributor bölümünü seçili locale workspace'in hemen sonrasına yerleştir.
- [x] Assignment actions -- kaldırma ve mevcut API ile grup-içi reorder akışlarını bağla; optimistic rollback ekle.
- [x] `messages.ts` -- TR/EN rol, grup, reorder ve hata metinlerini ekle.
- [x] Component, integration ve visual coverage -- rol grupları, varsayılan diller, reorder, hata rollback'i ve responsive yerleşimi doğrula.

**Acceptance Criteria:**

- Given içerikte contributor assignment'ları vardır, when içerik detayı açılır, then kayıtlar rol ve dil/global kapsam grupları altında gösterilir.
- Given editör bir rol grubundaki ekle eylemini seçer, when picker açılır, then picker doğru role bağlamıyla çalışır ve atama aynı grubun sonuna eklenir.
- Given NARRATOR için seçili locale vardır, when yeni atama başlatılır, then seçili dil varsayılan kapsam olur; diğer roller global başlar.
- Given aynı grupta en az iki assignment vardır, when editör klavye veya sürükle-bırak ile sıralar, then yalnız o grubun sırası değişir ve backend sonucu yansır.
- Given reorder veya assignment başarısızdır, when hata döner, then önceki görünüm korunur/geri yüklenir ve inline hata gösterilir.
- Given kullanıcı Türkçe veya İngilizce kullanır, when contributor paneli ve eylemleri görünür, then görünen metinler i18n kaynaklarından gelir.

## Design Notes

Reorder ilk sürümde sürükle-bırak zorunluluğu olmadan, her satırda erişilebilir yukarı/aşağı kontrolleriyle uygulanır; böylece klavye ve dar ekran kullanımı eksiksiz kalır. Assignment düzenleme backend update kontratı bulunmadığı için bu story dışında tutulur; sonraki story’de ayrı bir edit kontratı olarak ele alınacaktır. Backend'in sıra ve grup kuralları tek doğruluk kaynağıdır; istemci yalnızca geçici görünümü günceller ve hata durumunda sorgu verisine döner.

## Verification

**Commands:**

- `cd cms && .\\node_modules\\.bin\\vitest.cmd run src/features/contributors/components/content-contributor-panel.test.tsx src/features/contributors/contributors.integration.test.tsx` -- expected: grouped panel and assignment flows pass.
- `cd cms && .\\node_modules\\.bin\\tsc.cmd -b` -- expected: TypeScript succeeds.
- `cd cms && .\\node_modules\\.bin\\vite.cmd build` -- expected: production build succeeds.
- `cd cms && .\\node_modules\\.bin\\playwright.cmd test e2e/visual/content-detail.visual.spec.ts --workers=1` -- expected: responsive placement and grouped panel snapshots pass.

## Suggested Review Order

**Panel composition and grouping**

- Rol/dil gruplarını ve optimistic grup-içi reorder akışını giriş noktasında inceleyin.
  [`content-contributor-panel.tsx:26`](../../../../cms/src/features/contributors/components/content-contributor-panel.tsx#L26)

- Seçili locale’in contributor paneline nasıl aktarıldığını doğrulayın.
  [`detail.tsx:402`](../../../../cms/src/app/routes/contents/detail.tsx#L402)

**Assignment contract and actions**

- Reorder isteğinin yalnızca role, dil kapsamı ve assignment ID permütasyonu taşıdığını kontrol edin.
  [`contributor-admin.ts:171`](../../../../cms/src/features/contributors/api/contributor-admin.ts#L171)

- Mutation başarı/invalidasyon davranışını panel optimistic state’iyle birlikte okuyun.
  [`use-contributor-actions.ts:180`](../../../../cms/src/features/contributors/mutations/use-contributor-actions.ts#L180)

- Role picker ve narrator locale varsayılanını inceleyin.
  [`assign-contributor-dialog.tsx:48`](../../../../cms/src/features/contributors/components/assign-contributor-dialog.tsx#L48)

- Kaldırma onay akışının tüm görünen metinleri i18n üzerinden kullandığını doğrulayın.
  [`unassign-contributor-button.tsx:49`](../../../../cms/src/features/contributors/components/unassign-contributor-button.tsx#L49)

**Verification**

- Grup izolasyonu ve başarısız reorder rollback testlerini inceleyin.
  [`content-contributor-panel.test.tsx:109`](../../../../cms/src/features/contributors/components/content-contributor-panel.test.tsx#L109)

- Seçili narrator dili ve mutation invalidasyon testlerini kontrol edin.
  [`assign-contributor-dialog.test.tsx:225`](../../../../cms/src/features/contributors/components/assign-contributor-dialog.test.tsx#L225)

  [`use-contributor-actions.test.tsx:367`](../../../../cms/src/features/contributors/mutations/use-contributor-actions.test.tsx#L367)
