---
title: 'Story 1.7: CMS’te playback ve localization editörlerini ayrıştırma'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
baseline_commit: '6ba90bc5e4f3c3481b79cd4b9cb2bfb16bd1f386'
context:
  - 'AGENTS.md'
  - 'cms/AGENTS.md'
  - 'cms/docs/ui-standards.md'
  - 'cms/docs/ui-regression-task-list.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
  - '_bmad-output/implementation-artifacts/spec-1-6-canonical-audio-story-turunu-guvenle-kaldirma.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** CMS, içerik düzeyinde paylaşılan playback/kapak bilgileri ile dil düzeyinde değişen alanları aynı akışta yeterince ayırmıyor. Bu durum özellikle ninni playback’inin tekrar girilmesine, STORY narration’ın sayfa akışıyla karışmasına ve MEDITATION/LULLABY formlarında yanlış alanların görünmesine yol açıyor.

**Approach:** Mevcut admin API sözleşmelerini kullanarak tek bir içerik detay çalışma alanı içinde sahiplik düzeylerini görünür ve düzenlenebilir hale getir. Canonical içerik türleri üç seçenekle sınırlı kalacak; ortak asset/playback content editor’da, locale’a özgü veri localization editor’da kalacak.

## Boundaries & Constraints

**Always:** `STORY`, `MEDITATION`, `LULLABY` create/filter yüzeylerinde tek geçerli türlerdir; `AUDIO_STORY` hiçbir CMS akışında görünmez. STORY narration yalnız ilgili localization’da; sayfa metni, illüstrasyonu ve sayfa sesi korunarak ayrı anlatım alanında asset, süre ve read-only processing durumu ile gösterilir. STORY kaynak textless kapağı ve sesli deneyim kapağı content-level; localization cover yalnız dile özgü başlıklı okuma kapağıdır. LULLABY playback sesi, süresi, global MUSICIAN kredisi ve benzersiz/sıralı katalog enstrümanları tek content-level akışta yönetilir. LULLABY localization yalnız başlık ve yayın durumunu; MEDITATION localization ise dil-bazlı ses/metni gösterir ve cover göstermez. Asset picker, görünür etiket, klavye erişimi, loading/error feedback ve 390/768/1280/1440 viewport kuralları korunur.

**Ask First:** Hazır admin endpointlerinin response veya yetki sözleşmesi CMS ihtiyacını karşılamıyorsa, yeni backend endpointi, public/mobile response veya import davranışı eklemeden önce dur ve onay iste.

**Never:** `AUDIO_STORY` için compatibility alias, yeni canonical kayıt, toplu import veya migration yapma. Public/mobile discovery, arama/kategori endpointleri, asset URL çözümleme veya veri sahipliğini yeniden tasarlama. Aynı playback, musician, instrument ya da shared cover bilgisini locale formuna kopyalama.

## I/O & Edge-Case Matrix

| Senaryo | Girdi / Durum | Beklenen davranış | Hata |
|---|---|---|---|
| Story narration | STORY localization’da narration audio + süre | Ayrı anlatım alanı kaydeder; sayfa alanları değişmez | Eksik çift alan inline doğrulanır |
| Lullaby playback | LULLABY content, audio + süre + instrument codes | `/playback` ve `/instruments` sözleşmeleriyle ortak kayıt güncellenir | API problem’i ilgili section’da gösterilir; eski seçim korunur |
| Global musician | LULLABY contributor paneli | MUSICIAN ataması locale’sız global scope ile açılır | Locale seçimi sunulmaz |
| Type-specific form | LULLABY veya MEDITATION localization | LULLABY’de yalnız title/status; MEDITATION’da cover yok, ses/metin korunur | Yanlış alan submit payload’ına girmez |
| Missing playback | LULLABY playback/catalog response boş veya yükleniyor | Açıklayıcı empty/loading state ve tekrar deneme | Detay workspace çökmez |

</frozen-after-approval>

## Code Map

- `cms/src/features/contents/api/content-admin.ts` -- mevcut content/localization şemaları; `playback` ve narration read modelleri ile lullaby playback write API wrapper’ları.
- `cms/src/features/contents/model/content-view-model.ts`, `cms/src/features/contents/queries/use-content-detail.ts` -- admin read response’un playback/instrument verisine map edilmesi.
- `cms/src/features/contents/components/content-localization-form.tsx`, `schema/content-localization-schema.ts` -- STORY narration alanı; LULLABY title/status-only ve MEDITATION cover’sız görünürlük/payload kuralları.
- `cms/src/app/routes/contents/detail.tsx`, `components/content-form.tsx`, `components/content-textless-cover-form.tsx` -- content-level source/listening cover ve playback çalışma alanının tek baskın detail akışına yerleştirilmesi.
- `cms/src/features/contributors/components/content-contributor-panel.tsx`, `features/contributors/components/assign-contributor-dialog.tsx` -- LULLABY MUSICIAN global scope’unun korunması.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java`, `InstrumentCatalogAdminController.java`, `AdminContentReadResponse.java` -- tüketilecek hazır playback, instruments ve catalog endpoint sözleşmeleri.
- `cms/src/features/contents/**/*.test.*`, `cms/src/features/contributors/**/*.test.*`, `cms/e2e` veya mevcut visual regression config -- davranış, erişilebilirlik ve viewport regression noktaları.

## Tasks & Acceptance

**Execution:**
- [x] CMS API/schema/model/query katmanını playback, katalog enstrümanı ve nested processing verisini doğrulayacak şekilde genişlet -- backend sözleşmesini kaybetmeden tip güvenliği sağla.
- [x] Content detail içinde source/listening cover ile LULLABY playback, global musician ve ordered instrument editor’ünü tek content-level akışta oluştur -- tekrar eden locale girişini kaldır.
- [x] Localization formunu tür bazlı ayrıştır; STORY narration’ı ayrı alan olarak koru, LULLABY ve MEDITATION yanlış alanlarını gizle -- mevcut sayfa/story-page akışını bozma.
- [x] Component/interaction ve visual regression testleri ekle -- 390/768/1280/1440 genişliklerinde tek baskın akış, label, keyboard ve error/loading durumlarını kanıtla. Component/interaction testleri tamam; Playwright Chromium bu ortamda kurulu olmadığı için LULLABY detail visual baseline'ı çalıştırılamadı.

**Acceptance Criteria:**
- Given CMS create/filter, when tür seçilir, then yalnız STORY/MEDITATION/LULLABY görünür ve AUDIO_STORY gönderilemez.
- Given STORY localization, when narration düzenlenir, then asset/süre ayrı anlatım alanında kaydedilir, processing görünür ve page text/image/audio korunur.
- Given LULLABY content, when playback veya enstrüman değişir, then tek ortak kayıt güncellenir; MUSICIAN global kalır; localization yalnız title/status gösterir.
- Given MEDITATION localization, when form açılır, then dil-bazlı audio/text alanları görünür ve localization cover alanı yoktur.
- Given responsive CMS detail, when viewport değişir veya API hata/loading döner, then alanlar erişilebilir, açıklayıcı ve layout’u bozmadan çalışır.

## Spec Change Log

## Design Notes

Content detail ekranı tek bir “her şeyi düzenle” formuna dönüşmemeli; sahiplik sınırları section başlıkları ve yardımcı metinlerle görünür kalmalı. STORY için kaynak kapak, dinleme kapağı ve narration birbirinden ayrıdır: kaynak kapak story-page üretim kaynağıdır, dinleme kapağı audio deneyiminin ortak görselidir, narration ise seçili locale’ın tek parça sesidir. LULLABY playback paneli API’nin mevcut nested `playback` snapshot’ını kullanmalı; instrument seçiminde kullanıcıya locale label gösterilirken kaydetme stable code listesiyle yapılmalıdır. Sıralama için drag-and-drop zorunlu değildir; klavye ile erişilebilir yukarı/aşağı kontrolleri yeterlidir. MUSICIAN paneli mevcut contributor altyapısını yeniden kullanır, ancak LULLABY bağlamında language seçicisini global scope’a kilitler. Processing durumu editoryal bir input değil, backend’in read-only operasyonel sonucudur.

## Verification

**Commands:**
- `cd cms && npm run test` -- beklenen: content, localization, playback/instrument ve contributor regression testleri başarılı.
- `cd cms && npm run build` -- beklenen: TypeScript ve production build başarılı.
- `cd cms && npm run test:e2e:visual` -- beklenen: ilgili content detail ekranlarında 390/768/1280/1440 snapshot farkı yalnız onaylanan ayrışmayı gösterir.

**Manual checks (if no CLI):**
- STORY, LULLABY ve MEDITATION detaylarında content/localization sahiplik sınırlarını; keyboard navigation, loading/error ve mobil dar genişlik davranışını gözle doğrula.

## Suggested Review Order

**Content-level playback boundary**

- LULLABY detail ekranının ortak playback section’ını ve tek baskın akışa eklenmesini incele.
  [`detail.tsx:408`](../../cms/src/app/routes/contents/detail.tsx#L408)

- Playback, catalog labels ve sıralı stable-code seçimlerinin API sınırını doğrula.
  [`lullaby-playback-editor.tsx:37`](../../cms/src/features/contents/components/lullaby-playback-editor.tsx#L37)

- Admin response şemasının nullable locale label ve nested processing durumunu koruduğunu kontrol et.
  [`content-admin.ts:111`](../../cms/src/features/contents/api/content-admin.ts#L111)

**Localization ownership**

- Tür bazlı cover/media görünürlüğünü ve narration processing gösterimini incele.
  [`content-localization-form.tsx:260`](../../cms/src/features/contents/components/content-localization-form.tsx#L260)

- Payload’ın LULLABY title/status-only, STORY narration ve MEDITATION audio/text ayrımını doğrula.
  [`use-content-localization-actions.ts:72`](../../cms/src/features/contents/mutations/use-content-localization-actions.ts#L72)

**Global contributor scope**

- LULLABY panelinin yalnız global MUSICIAN rolünü açtığını kontrol et.
  [`content-contributor-panel.tsx:53`](../../cms/src/features/contributors/components/content-contributor-panel.tsx#L53)

- Add/edit akışında locale scope’unun gerçekten null’a kilitlendiğini incele.
  [`assign-contributor-dialog.tsx:183`](../../cms/src/features/contributors/components/assign-contributor-dialog.tsx#L183)

**Regression evidence**

- Playback snapshot’ın gerçek admin DTO’dan view model’e eksiksiz taşındığını doğrula.
  [`content-view-model.test.ts:227`](../../cms/src/features/contents/model/content-view-model.test.ts#L227)

- API URL/body/query sözleşmelerini ve global musician mutation’ını testlerle kontrol et.
  [`content-admin.test.ts:1`](../../cms/src/features/contents/api/content-admin.test.ts#L1)
