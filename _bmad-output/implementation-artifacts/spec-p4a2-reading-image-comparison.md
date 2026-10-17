---
title: 'P4a2: tek hikâyede kontrollü görsel karşılaştırma'
type: 'feature'
created: '2026-10-17'
status: 'done'
baseline_commit: '39fee0c9106cd908f1aaade41545cdf858a36237'
review_loop_iteration: 0
context:
  - '{project-root}/be/docs/reading-image-comparison.md'
---

<frozen-after-approval reason="kullanıcının onaylayacağı uygulama kapsamı">

## Amaç

**Sorun:** P4a1 seçim yapıyor; gerçek kaynak ile rendition aynı mobil alanda karşılaştırılamıyor.

**Yaklaşım:** Bağımsız local/test ekranında tek Türkçe QA kopyasının kapağı ve iki sıralı sayfasını karşılaştır. Backend kimliği doğrulasın; Firebase SDK sabit sürümü okusun. Normal Library/detail/reader değişmesin.

## Sınırlar

**Her zaman:** AGENTS, Flutter skill ve eşlik eden teslimat sözleşmesi. Tek QA UID; ücretsiz kopya ve aynı eligible LIVE; açık rol/profil/dil/revision/digest. Gerçek slot/DPR/fit ve P4a1 seçimi. `private,no-store`, yalnız bellek. Tek görünür medya işi; eski tamamlanmalar reddedilir.

**Bu taslakla önerilen karar:** TR71 kapak/ilk iki sayfanın canonical PNG adaylarını ayrı QA kopyası için seç; yalnız kopyaya ücretsiz test erişimi tanımla. `user → content::api` dar bağımlılığını ADR ile ekle. Onay, kaynak hikâyenin erişimini değiştirmez.

**Ayrı çalıştırma kararı:** Sayısal profiller, kaynak/bellek/süre/kontrol sınırları ve toplam bulut işlem/byte tavanı. Eksikse gerçek fixture, upload, admission ve yayın çalışmaz; yalnız geometri ölçümü yapılır.

**Yapma:** TR71 değişikliği, yeni abonelik hakkı, kategori ekleme, normal tüketici geçişi, disk/offline/session retention, original fallback, upscale, retry/storage süreci, dağıtım veya backfill. `.memlog.md`, çalışan süreçler ve legacy poller korunur.

## Sınır durumları

| Girdi / durum | Sonuç |
|---|---|
| Gerçek token, seçilmiş ücretsiz kopya, tam LIVE | Doğrulanmış descriptor ve yalnız o kimliğe ait byte. |
| Yanlış UID/dil/rol, kilitli içerik, eski LIVE | Teslimatı reddet; eski görseli temizle. |
| Eksik/bozuk çıktı veya yetersiz iki eksen | Açık hata; original seçme. |
| Ekran/hesap/arka plan/süre değişimi | İşi iptal et; geç sonucu at; dönüşte yeniden admission. |

</frozen-after-approval>

## Kod haritası

- `be/src/main/java/com/tellpal/v2/`: `user/application/LibraryHomeTestQueryService` gerçek auth örneği; `content/application/publication/ReadingPublicationService` retained LIVE; `asset/application/reading/` proof/availability. Mevcut yollar salt okunur dayanak; normal akış yeniden kullanılmaz.
- `app/mobile/lib/features/reading_images/`: P4a1 domain seçici/doğrulayıcı korunur; yeni data/presentation burada.
- `be/docs/evidence/p4-reading-images/p4a2-preflight.md`: 19 kaynak ve erişim engeli; bulut üretimi ve cihaz kabulü yok.

## Görevler ve kabul

- [x] `be/docs/adr/ADR-0024-reading-image-comparison.md`, `architecture.md`, `be/docs/project-memory.md`, ADR-0018: dar API yönünü ve diske yazmayan QA gösterim istisnasını kaydet.
- [x] `be/src/main/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestController.java`, `content/api/ReadingImageComparisonApi.java`, `asset/api/ReadingImageDeliveryApi.java`: sözleşmedeki kod haritasına göre admission, LIVE ve pinned teslimat ekle.
- [x] `app/mobile/lib/features/reading_images/`: repository/use-case, DTO/datasource, injectable Freezed Cubit, ekran/view/raster ekle; ayrı auto_route girişini `app/mobile/lib/app/` altında bağla.
- [x] `be/src/test/java/com/tellpal/v2/`, `app/mobile/test/reading_images/`: matrisi, yarışları, gerçek yerel HTTP/codec iptalini ve normal akış regresyonlarını sına.
- [x] `be/docs/evidence/p4-reading-images/verification.md`: kod kanıtını, geometriyi, gerçek deneme ve cihaz eksiklerini ayrı kaydet.

**Kabul:** Geçerli bağlam verildiğinde kol değiştirilirse aynı slot/fit/alignment kullanılır. Gerçek byte/decode sınırı aşılırsa kaynak bırakılır ve hata görünür. Çalıştırma kaydı yokken deneme açılırsa bulut işlemi yapılmaz. Cihaz yokken sonuç raporlanırsa kalite/performans kabulü açık kalır.

## Spec değişiklik kaydı

## Doğrulama

`be/`: `./mvnw verify`. `app/mobile/`: annotation değişiminden sonra `dart run build_runner build --delete-conflicting-outputs`, `flutter analyze`, `flutter test`. Gerçek HTTP, SHA/decode, erişim ve eşzamanlı değişim testleri gerekir. Fiziksel Android/iPhone kanıtı bunlarla tamamlanmış sayılmaz. Sayısal çalıştırma kararı sonrası seçilen rendition, iki byte tabanı, görüntü ve kaynak ölçümleri kaydedilir. P4b genel kaynak/yaşam döngüsü ve üretim profili açıktır.


## Önerilen inceleme sırası

**Erişim ve LIVE kimliği**

- Backend girişinden byte gönderimine kadar aynı yayın ve erişim kesitini izle.
  [ReadingImageComparisonTestController.java:55](../../be/src/main/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestController.java#L55)

- Son bearer doğrulamasından sonra DB uygunluğu yeniden denetlenir.
  [ReadingImageComparisonTestService.java:28](../../be/src/main/java/com/tellpal/v2/user/application/ReadingImageComparisonTestService.java#L28)

- Retained proof, desired işten ayrı tutulur; storage I/O transaction dışındadır.
  [ReadingImageComparisonService.java:41](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonService.java#L41)

**Doğrulanmış teslimat ve kaynak sınırı**

- Pinned byte doğrulanır; belirsiz transport sağlıklı LIVE kaydını kapatmaz.
  [ReadingImageDeliveryService.java:41](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingImageDeliveryService.java#L41)

- Buffer ve permit, tamamlanma veya gerçek deadline cleanup sınırında bırakılır.
  [ReadingImageComparisonResponseWriter.java:42](../../be/src/main/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonResponseWriter.java#L42)

**Flutter karşılaştırması**

- İlk LIVE anchor sabit kalır; eski async sonuç yeni seçime taşınmaz.
  [reading_image_comparison_cubit.dart:128](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L128)

- Painter native görüntüyü klonlamadan aynı fit ve merkez hizasında çizer.
  [reading_image_comparison_raster.dart:126](../../app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart#L126)

- HTTP payload kimliği korunur; gizli açma, cache veya yönlendirme yapılmaz.
  [reading_image_comparison_data_source.dart:34](../../app/mobile/lib/features/reading_images/data/data_source/reading_image_comparison_data_source.dart#L34)

**Çalıştırma kapısı ve doğrulama**

- Sayısal run ve güvenli ortam eksikse medya admission açılmaz.
  [ReadingImageComparisonProperties.java:23](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonProperties.java#L23)

- Harcanan okuma rezervasyonları restart veya başarısızlıkla sıfırlanmaz.
  [V37__add_reading_comparison_budget.sql:2](../../be/src/main/resources/db/migration/V37__add_reading_comparison_budget.sql#L2)

- Gerçek açık yavaş soket, encoded path ve cleanup sonrası ikinci aktarımı sınar.
  [ReadingImageComparisonTestControllerIT.java:33](../../be/src/test/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestControllerIT.java#L33)

- Gerçek PNG ile kol boyutları, LIVE anchor ve native sahiplik doğrulanır.
  [reading_image_comparison_loaded_test.dart:123](../../app/mobile/test/reading_images/reading_image_comparison_loaded_test.dart#L123)

- Bulgu-test matrisi ile cihaz ve gerçek çalışma sınırlarını birlikte oku.
  [verification.md:226](../../be/docs/evidence/p4-reading-images/verification.md#L226)
