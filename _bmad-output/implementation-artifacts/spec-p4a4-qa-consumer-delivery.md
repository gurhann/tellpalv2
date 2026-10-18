---
title: 'P4a4: ortak detail/reader widget’larında QA teslimatı'
type: 'feature'
created: '2026-10-18'
status: 'done'
baseline_commit: '339aee45a99e4fcda27132a2d9e62946267d142e'
approval: '2026-10-18: A — Planı onayla, yerel uygulamaya geç'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4a4-consumer-delivery-investigation.md'
---

<frozen-after-approval reason="Somut P4a4 planı onay bekliyor">

## Amaç

**Sorun:** P4a3 ölçümü hazır; normal görsel tüketicileri P3 byte hattını kullanmıyor.

**Yaklaşım:** Normal detail/reader görsel yerleşimlerini paylaş; ayrı QA rotaları bunları mevcut #94 PNG’leriyle kullansın. Gerçek Library→reader geçişi veya ürün metadatası/ses entegrasyonu değildir.

## Sınırlar

**Daima:** Kanonik QA UID, local/test, ücretsiz Türkçe iki sayfa, sunucunun güncel uygun retained LIVE’ı; tam kimlik/rol/sıra ve no-store korunur. Tek akış/Cubit/anchor; route yalnız ID taşır. Normal ses/cache/gezinme ve eski kayıtlar korunur.

**Önce sor:** Gerçek auth/storage/cihaz çalışması, yeni sayısal pencere/limit veya metadata/ses/erişim genişlemesi.

**Yapma:** Üretim, yeni encode/admission/publish, backfill/silme; yerel optimize PNG/WebP’yi LIVE sayma. Sahte kategori/URL/grant, original fallback, gizli DPR düşürme, yeni transport/retry yok. PENDING/poller/Library değişmez. P0/P4b/fiziksel kabul kapanmaz.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| QA kapalı/yetkisiz | Normal akış korunur; kapalı giriş I/O açmaz, ret medya byte’ı vermez. |
| Detail→iki sayfa→geri | Gerçek slot/DPR/fit; aynı anchor; rol binding’iyle en küçük yeterli aday. |
| Resize/yetersizlik | Yeniden seçim veya tipli shortage; fallback yok. |
| Bozuk/eksik kimlik/çıktı | Açık ret; legacy provider çağrısı yok. |
| Route/hesap/background/lease/status/LIVE değişimi | Görsel temizlenir; eski iş biter; sessiz yeni anchor yok. |
| QA ses alanı | “Bu QA denemesinde ses kapalı”; AudioPlayer/grant çağrısı yok. |

</frozen-after-approval>

## Kod haritası

Mobil yollar `app/mobile/` altındadır. Ayrıntılı dosya/satır, API ve kanıt haritası context belgesindedir.

- `lib/features/story_detail/presentation/view/story_detail_view.dart:159`, `lib/story_player_page.dart:347`: yalnız sunum çıkarılır; medya sahipleri korunur.
- `lib/core/presentation/widgets/reading_image_viewport.dart:18`: mevcut ölçüm.
- `lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:15`: mevcut seçim, gate, anchor ve teardown.
- `lib/app/reading_image_comparison_router.dart:8`: izole QA route/codegen kapsamı.

## Görevler ve kabul

- [x] `lib/core/presentation/widgets/reading_{detail_artwork,page}_frame.dart`: normal ekranların gerçek yerleşimini çıkar; mevcut tüketicileri bağla. Yeni feature importu yok.
- [x] `lib/app/reading_image_consumer_qa/` altında context’teki flow/detail/reader/binding dosyalarını ekle; mevcut QA router/app/DI ile açık opt-in bağla. Tek mevcut Cubit; eski iş bitmeden yeni decode yok; QA prefetch/precache/disk yok.
- [x] Context’teki Flutter testleri ve iki mevcut backend IT dosyası: matris, gerçek codec/loopback HTTP, native kapanış ve normal yol regresyonlarını sınar.
- [x] `be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md`, ADR-0024, `be/docs/project-memory.md`, `architecture.md`, `be/docs/reading-image-comparison.md`: yalnız bu QA sunum istisnasını kaydet.

**Kabul:** Given yeni QA rotaları, when yerel kanıt tamamlanır, then ortak gerçek widget→seçici→rendition→paint izi ve sıfırlanan sahiplik kanıtlanır. Normal metadata, ses, Library görünürlüğü ve genel tüketici geçişi tamamlandı denmez.

## Spec değişiklik kaydı

## Doğrulama

Context’teki Flutter/analyzer/codegen ve PostgreSQL/HTTP komutları uygulandı; ham kayıtlar MAC_EK’te yeni klasördedir. Gerçek #94 çalışması ayrı güncel kimlik/pencere/çağrı/byte onayı bekler; eski sayaçlar sıfırlanmaz. Tarihsel kanıt güncel admission değildir. Yerel doğrulama sonucu aşağıdadır; gerçek çalışma yapılmadı.

## Yerel uygulama kaydı — 2026-10-18

Somut plan kullanıcı tarafından onaylandı. Özel çalışma kökü: `/Volumes/MAC_EK/tellpal-qa-private/p4a4-consumers-20261018-fb8ed1cb`. Yeni test kaynak kopyaları, codegen/build çıktıları ve loglar bu kökte tutulur; eski QA klasörlerine yazılmaz. Yalnız gerekli kaynak/fixture/config kopyalanır; credential/token dosyası alınmaz. Mevcut SDK/cache kullanılır, kurulum veya gerçek Firebase çağrısı yapılmaz.

`protected-before.private.json`, başlangıç commit’ini, görev yollarını, 3209 korunan dosyanın ve onaylı bloğun hash’lerini tutar. Context inceleme belgesi değişmez. Görev dışı kirli `.memlog.md`, `main_image_experiment.dart`, önceki yerel lossless kaynakları ve kanıtları korunur. `operational-tools.private.json` mevcut Flutter/JDK/SDK yollarını verir; Flutter için kayıtlı Java yolu backend Java 25 gereksinimi sayılmaz. Backend test ortamı ayrıca doğrulanır. Testler yalnız izole yerel PostgreSQL ve fake/pinned disk storage kullanabilir.

Doğrulama sonunda her matris satırını gerçekten çalışıp geçen testle eşleştir; kaynak kimliği, test/analyzer/codegen/IT log yolları ve koruma kontrolünü bu spec’e ekle. Atlanan veya ortam nedeniyle çalışmayan IT açık kalır; mock sonucu onun yerine geçmez. `story_key` yoktur; sprint eşitlemesi uygulanmaz.

### Yerel doğrulama sonucu

- Mobil: `mobile-regression-03.log`, **226/226 geçti**; `mobile-analyze-02.log`, 13 dosyada sorun yok; `mobile-codegen-01.log`, yalnız ilgili router çıktısı alındı. Mobil kaynak SHA: `5984872d73fc62cf367bc443ad98056b2b4f41233c8e2ab03fa36ea64d4ee7e7`. `mobile-source-identity.private.json` repo/özel kopya eşitliğini tutar.
- Backend: `backend-test-02.log`, **30/30 geçti**, hata/atlama yok: `ReadingImageComparisonServiceIT` 20, `ReadingImageComparisonTestControllerIT` 8, `ApplicationModulesTest` 2. Mevcut native Colima ve önceden bulunan PostgreSQL/Ryuk imajları kullanıldı. Wrapper çalıştırma biti kapalı olduğu için `sh ./mvnw -o -Dtest=ReadingImageComparisonServiceIT,ReadingImageComparisonTestControllerIT,ApplicationModulesTest test` uygulandı; ilk izin hatası `backend-test-01.log` içinde korunur. İsteğe bağlı Docker ortam tanı kontrolü kapatıldı; hiçbir uygulama/DB/HTTP testi atlanmadı. Gerçek PostgreSQL 15 üzerinde V1–V37 migration ve gerçek Tomcat HTTP çalıştı. Storage yalnız sentetik pinned disk fixture’dır; backend üretim kodu değişmedi.
- `mobile-new-tests-05.log` içindeki fazla HTTP isteği route animasyonunda eski slotun seçim göndermesiydi. Güncel route kontrolü eklendi. Sonraki yeni 24 test ve tam 226 test geçti. İlk eksik test destek kopyası ve geçici derleme hataları dahil eski loglar korunur.

| Onaylı matris satırı | Çalışıp geçen kanıt |
|---|---|
| QA kapalı/yetkisiz | `kapalı consumer QA girişi I/O açmaz`; HTTP `unauthorized` ret testi; backend `realBearerIsRequiredAndEveryAuthErrorIsNoStore`, `disabledRunRejectsBeforeAnyFirebaseCall`. |
| Detail→iki sayfa→geri | `gerçek HTTP/codec: detail → iki sayfa → geri → bounded close aynı anchor`: gerçek router/frame/painter, tek Cubit/manifest, tam dört rendition teslimi, rol/page bağları ve payload byte eşitliği. Backend `consumerDetailAndOrderedPagesShareOneRetainedManifestWithoutOpeningPublicSurfaces`, `actualHttpPreservesDetailAndOrderedPageSelectionsWithOneIdentityAndNoStore`. |
| Resize/yetersizlik | `gerçek ortak frame ölçümü: tall/wide, kesirli DPR, resize, text-scale ve shortage`; `consumer tam pageId/sıra eşleşmeden byte istemez; shortage tipli kalır`; mevcut selector ve viewport testleri. |
| Bozuk/eksik kimlik/çıktı | HTTP consumer ret matrisi: language/role/order/revision/digest/profile/generation/missing/corrupt. Byte öncesi kimlik reddi; bozuk payload paint edilmez; canonical/legacy fallback yok. |
| Route/hesap/background/lease/status/LIVE | Gerçek router akışı; beş consumer lifecycle testi; bekleyen HTTP bounded close ve eski native Future gate testi. Backend `replacedLiveRefusesOldConsumerAnchorBeforeAndAfterStorage` iki kesit; `closedEditorialAccessRefusesConsumerBytesAtBothDbCuts` altı durum; retained ve kalıcı bütçe regresyonları. |
| QA ses alanı | Gerçek router testi açık ses kapalı metnini ve ses/grant/prefetch çağrısı yokluğunu denetler. Normal detail/navigation/reader testleri aynı tam komutta geçti. |

`git diff --check` temizdir. `mobile-protection-audit.private.json` 3206 değişmez dosyayı, üç gerekçeli ek görev yolunu ve onaylı blok/context eşitliğini kaydeder; başlangıç 3209 dosyalık makbuz yeniden yazılmaz. Nihai ortak koruma ve backend kaynak denetimi ayrıca kaydedilir. Bu testler fiziksel cihaz, gerçek #94 teslimatı, toplam RSS/GPU veya genel üretim profili kanıtı değildir.

### İnceleme sonrası doğrulama

Üç bağımsız inceleme [inceleme kaydındadır](spec-p4a4-qa-consumer-delivery-review.md). İki mekanik test boşluğu kapatıldı: gerçek parent binding paused/resumed bağlantısı ve bekleyen native iş sırasında UI close/drain sırası. Özel kopyada observer/await kaldırma mutasyonları ilgili testleri beklenen assertion ile bozdu; kaynak geri getirildi. Ürün koduna ek patch gerekmedi.

Nihai mobil sonuç: `mobile-regression-05.log` **228/228 geçti**; `mobile-analyze-04.log` temiz. Nihai kaynak SHA: `5bfac569ac3de1264f67cd47701f3170a1108e54fa403b708027cdc740962ef6`. Yeni kimlik ve koruma makbuzları `mobile-source-identity-review-patch-02.private.json`, `mobile-protection-audit-review-patch-02.private.json`; eski makbuzlar korunur. `implementation-protection-audit.private.json` ilk tam backend koşusunun XML sonuçlarını, kopya eşitliğini ve 3206 korunan dosyayı doğrular.

İnceleme sonrası backend yeniden koşusu `backend-test-03.log`: **30/30 geçti**, hata/atlama yok. Nihai matrisin route/background satırı ayrıca gerçek parent binding testi ve UI close/native drain testiyle kapsandı. `final-protection-audit.private.json` son XML sonuçları ile mobil/backend kopya eşitliğini, onaylı blok/context ve 3206 korunan dosyayı denetler. Önceki bütün denemeler korunur.

## Önerilen inceleme sırası

**QA akışı ve sahiplik**

- Tek parent, sabit anchor ve gerçek drain tamamlanana kadar kapanış sahipliği.
  [flow_screen.dart:24](../../app/mobile/lib/app/reading_image_consumer_qa/flow_screen.dart#L24)

- Yalnız güncel slot seçim yapar; eski route tekrar teslimat veya paint üretemez.
  [consumer_image_slot.dart:42](../../app/mobile/lib/app/reading_image_consumer_qa/consumer_image_slot.dart#L42)

- Rol ve page bağı, mevcut Cubit ve seçici üzerinden doğrulanır.
  [reading_image_comparison_cubit.dart:86](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L86)

**Ortak sunum ve normal tüketiciler**

- Detail gerçek alanını, clip ve overlay yuvalarını paylaşır.
  [reading_detail_artwork_frame.dart:8](../../app/mobile/lib/core/presentation/widgets/reading_detail_artwork_frame.dart#L8)

- Reader gerçek alanını, contain davranışını ve kontrol yuvalarını paylaşır.
  [reading_page_frame.dart:6](../../app/mobile/lib/core/presentation/widgets/reading_page_frame.dart#L6)

- Normal detail, mevcut metadata ve medya sahibini ortak frame’e verir.
  [story_detail_view.dart:165](../../app/mobile/lib/features/story_detail/presentation/view/story_detail_view.dart#L165)

- Normal reader ses, provider ve ön yükleme sahiplerini korur.
  [story_player_page.dart:349](../../app/mobile/lib/story_player_page.dart#L349)

**Giriş ve kapsam**

- Yeni QA açık opt-in ister; eski karşılaştırma varsayılandır.
  [reading_image_comparison_app.dart:10](../../app/mobile/lib/app/reading_image_comparison_app.dart#L10)

- Dar sunum istisnası, gerçek çalışma ve genel tüketici geçişinden ayrılır.
  [ADR-0024-reading-image-comparison.md:37](../../be/docs/adr/ADR-0024-reading-image-comparison.md#L37)

**Kanıt ve regresyon**

- Gerçek router/HTTP/codec; kimlik, byte ve çizim aynı izde doğrulanır.
  [reading_image_consumer_qa_test.dart:196](../../app/mobile/test/reading_images/reading_image_consumer_qa_test.dart#L196)

- Binding olayları ve UI kapanışı doğrudan parent üzerinden sınanır.
  [reading_image_consumer_qa_test.dart:401](../../app/mobile/test/reading_images/reading_image_consumer_qa_test.dart#L401)

- Gerçek PostgreSQL, aynı manifest ve normal yüzeylerden dışlanmayı kanıtlar.
  [ReadingImageComparisonServiceIT.java:259](../../be/src/test/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonServiceIT.java#L259)

- Gerçek HTTP rol/sıra/kimliği ve no-store teslimatını korur.
  [ReadingImageComparisonTestControllerIT.java:65](../../be/src/test/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestControllerIT.java#L65)

- Üç bağımsız inceleme, iki patch ve mutasyon kanıtı burada kayıtlıdır.
  [spec-p4a4-qa-consumer-delivery-review.md:1](spec-p4a4-qa-consumer-delivery-review.md#L1)
