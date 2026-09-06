---
title: 'Story 1.2: Hikâyeye dil-bazlı tam anlatım ekleme'
type: 'feature'
created: '2026-09-06'
status: 'done'
review_loop_iteration: 0
baseline_commit: '4a0dfba306400f5746e3daeca6d193f5343ed29e'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Sesli hikâye bugün ayrı bir `AUDIO_STORY` kaydı gibi modellenmeye elverişlidir; oysa aynı hikâyenin, aynı dildeki tam seslendirilmiş sürümüdür. Var olan sayfa bazlı seslendirmeler de tam anlatım için uygun bir yer değildir.

**Approach:** Canonical `STORY` içindeki her localization’a isteğe bağlı bir `StoryNarration` child ekle. Child yalnızca o dilin tek parça `AUDIO` asset referansı ve süresini taşır; işleme ve admin projeksiyonu `LOCALIZATION(contentId, languageCode)` hedefiyle bu anlatıma özel yürür.

## Boundaries & Constraints

**Always:** Tam anlatım yalnızca `STORY` localization’ında tutulur; asset ID pozitif, asset tipi `AUDIO`, süre sıfır veya pozitif olmalıdır. `StoryNarration` localization silinince orphan olarak silinir; aynı localization için en fazla bir tane bulunur. `ContentLocalization.audioMediaId` ve `StoryPageLocalization.audioMediaId` mevcut anlamlarını korur ve birbirinin yerine kullanılmaz. Anlatım ekleme/değiştirme sadece hedef dilin `LOCALIZATION` işini etkiler. Asset modülü işleme durumu ve türetilmiş çıktının sahibidir; content modülü URL ya da işleme çıktısı saklamaz. Anlatım işleme hatası okunabilir hikâyeyi, yayın durumunu veya başka bir dili gizlemez.

**Ask First:** Tam anlatımın kaldırılması için yeni bir UI/API davranışı, mevcut public/mobile endpointlerin anlatımı nasıl sunacağı, otomatik yayınlama veya `AUDIO_STORY` verisinin import/migration’ı gerekirse dur ve karar iste.

**Never:** Yeni canonical content türü ya da yeni `AUDIO_STORY` satırı oluşturma; `AUDIO_STORY` türünü bu hikâyede kaldırma. Sayfa bazlı seslerin şemasını/davranışını değiştirme. Ninni, meditasyon veya textless-cover sahipliği kapsamına girme. Bir anlatım hatasını genel localization `processingStatus`una yansıtarak hikâyenin okuma görünürlüğünü düşürme.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Anlatım kaydetme | Var olan `STORY` + `tr` localization, geçerli AUDIO ve süre | Bir `StoryNarration` kaydedilir; `LOCALIZATION(id,tr)` anlatım işi pending/yenilenebilir olur; admin yanıtı anlatım alanını taşır | N/A |
| Dil izolasyonu | `tr` anlatımı güncellenir, `en` anlatımı/işi vardır | Yalnızca `tr`nin anlatım kaynağı ve işi değişir | `en` kaydı ve durumu değişmez |
| Hatalı kaynak | IMAGE, negatif/0-altı ID veya negatif süre | Domain/app doğrulaması kaydı ve işlem isteğini reddeder | İstemciye 400; kalıcı kısmi değişiklik olmaz |
| Yanlış içerik | STORY olmayan localization veya olmayan localization | Anlatım eklenemez | Açık type/not-found hatası; iş planlanmaz |
| Mevcut sayfa sesi | Story page’de localized audio vardır | Page audio aynen kalır; full narration ile eşleştirilmez | N/A |
| Anlatım işi başarısız | Kaynak optimizasyonu hata verir | Admin anlatım alanında ilgili iş durumu/hata okunur; metin hikâyesi görünür kalır | Sadece anlatım tekrar denenebilir |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- localization tür kuralını ve aggregate child yaşam döngüsünü sahiplenir.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java` ve yeni `StoryNarration.java` -- anlatımın localization-scoped persistent domain modeli.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementCommands.java` / `ContentManagementService.java` -- narration komutları, asset doğrulaması ve asset public API koordinasyonu.
- `be/src/main/java/com/tellpal/v2/content/application/ContentAssetReferenceValidator.java` -- mevcut `AUDIO` türü doğrulamasının tekrar kullanımı.
- `be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryService.java`, `ContentAdminQueryMapper.java`, `content/api/AdminContentLocalizationView.java` -- admin okuma projeksiyonu.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java` ve DTO’ları -- localization request/response sözleşmesi.
- `be/src/main/java/com/tellpal/v2/asset/api/AssetProcessing*.java` ve `asset/application/AssetProcessingService.java` -- narrationa özel, localization hedefli iş kaydı ve yeniden planlama public API’si.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/**` -- processing persistence, worker, optimize edilmiş audio çıktısı ve target-safe path üretimi.
- `be/src/main/resources/db/migration/V23__*.sql` -- narration child ve narration iş ayrımını koruyan şema/migration.

## Tasks & Acceptance

**Execution:**

- [x] Content aggregate’e `StoryNarration` optional child’ını ve yalnızca `STORY` için create/update kuralını ekle; `audioMediaId` ile page-localization verisini değiştirmeden asset/süre doğrulamasını uygula.
- [x] Flyway migration ile narration tablosunu localization’a bire-bir bağla; pozitif medya/süre ve unique localization kısıtlarını ekle.
- [x] Localization yazma komutları ve CMS admin requestlerini narration alanını açık bir nested alan olarak kabul edecek biçimde genişlet; source asset `AUDIO` doğrulamasını `AssetRegistryApi` üzerinden yap.
- [x] Asset processing modelini aynı `LOCALIZATION` hedefinde anlatım işini diğer story render işinden ayıracak biçimde genişlet; completed/failed anlatım değiştirildiğinde güvenle tekrar planlanabilsin, optimize edilmiş audio üretilebilsin ve sadece narration durumu admin’e dönsün.
- [x] Processing event/listener akışını anlatımın lifecycle’ının genel localization görünürlüğünü değiştirmeyeceği şekilde güncelle; mevcut story sayfa/cover işlerinin davranışını koru.
- [x] Admin query/response’a `narration` (source asset ref, duration, asset-modülünden çözülen processing state/error) alanını ekle; canonical type `STORY` olarak kalır.
- [x] Domain, servis, controller ve Testcontainers entegrasyon testleriyle matristeki validasyon, dil izolasyonu, sayfa-sesi ayrımı, yeniden planlama ve görünürlük kurallarını kapsa; Modulith sınırlarını doğrula.

**Acceptance Criteria:**

- Given bir `STORY` localization, when editör geçerli tam anlatımı kaydeder veya günceller, then tek `StoryNarration` saklanır ve o locale için bağımsız narration processing isteği oluşur/güncellenir.
- Given narration processing failed, when localization daha önce okunabilir durumdaysa, then hikâye okunabilir kalır ve hata yalnızca narration admin durumunda görülür.
- Given STORY olmayan content, when narration gönderilirse, then istek reddedilir ve hiçbir processing kaydı oluşmaz.
- Given iki dilde anlatım, when biri değiştirilirse, then diğer dilin narration kaynakları, targetı ve processing durumu değişmez.
- Given story page audio, when full narration eklenirse, then page localization audio referansları ve page akışı etkilenmez.

## Design Notes

Bir localization hedefi tek başına yeterli kimlik değildir: story görselleri/paketleri ile optional narrationın hata ve yeniden deneme etkileri farklıdır. Bu nedenle asset tarafındaki iş ayrımı, aynı typed target altında narrationa ait lifecycle’ı açıkça temsil etmelidir. Content yalnızca source referansını ve süreyi tutar; admin’in gördüğü durum asset API’sinden çözümlenir. Bu ayrım, daha sonra mobile projeksiyonun `canonicalType: STORY` ile sesli deneyimi seçmesine alan açar; bu hikâye o public sözleşmeyi değiştirmez.

## Verification

**Commands:**

- `cd be && ./mvnw test` -- yeni domain/application/controller testleri ve mevcut davranışlar geçer.
- `cd be && ./mvnw verify` -- Flyway/Testcontainers ve Spring Modulith kontrolleri geçer.

## Suggested Review Order

**Domain contract**

- Canonical STORY aggregate narration child’ını ve localization izolasyonunu inceler.
  [`Content.java:126`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L126)

- One-to-one child yaşam döngüsü ve alan doğrulamasını kontrol eder.
  [`StoryNarration.java:15`](../../be/src/main/java/com/tellpal/v2/content/domain/StoryNarration.java#L15)

**Processing boundary**

- DELIVERY ve STORY_NARRATION işlerinin aynı hedefte nasıl ayrıldığını gösterir.
  [`AssetProcessingService.java:72`](../../be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java#L72)

- Narration worker’ının yalnızca optimize audio üretmesini doğrular.
  [`RegisteringAssetProcessingJobExecutor.java:52`](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java#L52)

**Schema and API**

- Veritabanı kısıtları, audio foreign key’i ve STORY trigger’ını inceler.
  [`V23__add_story_narrations.sql:1`](../../be/src/main/resources/db/migration/V23__add_story_narrations.sql#L1)

- Nested narration request doğrulamasını ve admin sözleşmesini kontrol eder.
  [`ContentAdminController.java:289`](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java#L289)

**CMS binding and verification**

- Story localization formundaki opsiyonel anlatım asset seçim alanını gösterir.
  [`content-localization-form.tsx:457`](../../cms/src/features/contents/components/content-localization-form.tsx#L457)

- Bağımsız processing, çıktı ve genel localization görünürlüğü testini doğrular.
  [`AssetProcessingIntegrationTest.java:261`](../../be/src/test/java/com/tellpal/v2/asset/AssetProcessingIntegrationTest.java#L261)
