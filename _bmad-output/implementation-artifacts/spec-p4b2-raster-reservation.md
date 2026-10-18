---
title: 'P4b2: QA raster kapasitesini native çağrıdan önce ayır'
type: 'bugfix'
created: '2026-10-18'
status: 'done'
baseline_commit: '6c37af44921cb85de220bee504b8fe0dfe3dfaf6'
approval: '2026-10-18: A — somut plan ve gerekli özel kaynak/test kopyası onaylandı'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b2-raster-reservation-investigation.md'
---

<frozen-after-approval reason="Kullanıcıya sunulan somut P4b2 planı">

## Amaç

QA seri gate'i bitince raster bellekte kalabilir. İkinci owner'ın mevcut ret kontrolü
native image üretildikten sonra çalışır. Mevcut tek-owner sınırı ilk native çağrıdan
önce ayrılsın; pending işten resident raster'ın dispose'una kadar tutulsun.

## Sınırlar

**Daima:** Aynı isolate'daki bütün decoder çağrıları tek alanı paylaşsın. Mevcut
`maxOwnedImages == 1`, limitler, doğrulama ve P4b1 drain korunsun. Dolu kapasite mevcut
ret yolunu kullansın. Başkasının rezervasyonu bırakılamaz. Eski kayıtlar korunsun.

**Önce sor:** Sayısal üretim bütçesi, genel tüketici/politika değişikliği veya gerçek ortam işlemi.

**Yapma:** Yeni scheduler/DI/transport/IPC/retry/timeout, bekleme, fallback, bulut/cihaz koşusu.
Tek-parent QA'da kanıtlanmış çakışma veya RSS/GPU sınırı iddia edilmez.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Kapasite boş | İlk native çağrı öncesi tek slot ve tahmini çıktı byte'ı ayrılır. |
| Pending/live owner var | İkinci native çağrı başlamaz; ilk owner korunur. |
| Başarı | Rezervasyon raster'a geçer; native cleanup sırasında erken bırakılmaz. |
| Hata/iptal | Gerçek native sonlanma ve cleanup sonrası bırakılır. |
| Farklı run/limit; double-dispose | Dolu alan sıfırlanmaz; başka sahibin tutarı değişmez. |
| Lifecycle/close | Eski epoch boyanmaz; sonlanınca slot/byte sıfırdır. |

</frozen-after-approval>

## Kod haritası

[İnceleme](image-optimization/p4b2-raster-reservation-investigation.md): gerçek boşluk,
satır haritası, yeniden kullanım ve koruma kapsamı. Native değişiklik yalnız mevcut
`ReadingComparisonRaster` içindedir; Cubit/DI/router salt okunur kalır.

## Görevler ve kabul

- [x] `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart`: private ön rezervasyon, sahiplik devri ve idempotent bırakma; salt okunur slot/byte ve native başlangıç gözlemi.
- [x] `app/mobile/test/reading_images/reading_image_comparison_raster_test.dart`: matris, erken native çağrı, hata/iptal ve farklı owner testleri.
- [x] `app/mobile/test/reading_images/reading_image_comparison_loaded_test.dart`: aynı gate ile iki gerçek Cubit/native raster kanıtı.
- [x] `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart`: gerçek loopback, farklı gate ve lifecycle/parent regresyonları.
- [x] `be/docs/evidence/p4b2-raster-reservation/verification.md`: ilk hata, final test, mutasyon, hash ve inceleme kanıtı.

Given ilk raster bellekteyken, when ikinci owner aynı veya farklı gate ile yüklerse,
then byte teslim edilse bile native decode başlamaz; ilk owner bırakıldıktan sonra
yalnız açık yeni seçim başarılı olabilir.

## Spec değişiklik kaydı

## Tasarım notları

Mevcut raster sınırında private ledger doğrudan çağrıları da kapsar. Tahmin
`BigInt(width) × height × 4` olur; yeni üretim sayısı değildir. Native buffer çağrı
sayacı resetlenmez; yalnız farkı gözlenir. Final sızıntı sayacı önleyici ret kanıtı değildir.

## Doğrulama

İncelemedeki dar MAC_EK kaynak/test kopyası ve log kapsamı plan onayına dahildir.
Mevcut SDK/cache ile `flutter test --no-pub test/reading_images test/story_detail`,
izlenen tam küme ve `flutter analyze --no-pub` geçsin. İncelemedeki mutasyonlar
assertion'ları bozsun; kaynak geri alınsın. Korunan hash'ler, kaynak eşitliği ve
`git diff --check` doğrulansın. Üç BMad incelemesi sonrası yalnız görev dosyaları
yerel commit'e girsin. Fiziksel kalite ve genel P4b kabulü açık kalır.

## Onaylı uygulama devri

Uygulayıcı kaynak değişikliğini, özel kopyayı, yerel test/mutasyon/analyzer ve kanıt
raporunu tamamlar. Root sonra üç incelemeyi ve yerel commit'i yürütür. Model
oturumdan devralınır. Sprint anahtarı yoktur. İnceleme belgesi salt okunur kalır.

Onaylı özel kökte `receipts/protected-before.private.json`, 3272 repo, 72 eski run,
192 eski P4a5 ve 296 eski P4b1 kanıt/kaynak dosyasını mühürler. SDK yolları yalnız
`receipts/operational-tools.private.json` içindedir. Önceki özel kökler değişmez.
Görev kaynaklarının özel kopyayla hash eşitliği ve import/fixture kapanışı doğrulanır.
Yeni log/makbuzlar ayrı numaralı CREATE_NEW dosyalardır; ham başarısızlıklar korunur.

## Tamamlanan doğrulama

Son kaynakta 474 test ve temiz analyzer; sekiz mutasyon hatayı yakaladı. Üç bağımsız
incelemedeki tek test boşluğu kapandı. Eski kayıtlar ve frozen intent korundu.
Fiziksel bellek/kalite ve genel P4b kabulü açık kalır.

## Önerilen inceleme sırası

**Native ön kabul**

- Dolu kapasite, ilk native buffer çağrısından önce reddedilir.
  [reading_image_comparison_raster.dart:69](../../app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart#L69)

**Sahiplik ve bırakma**

- Başarıda token raster'a devredilir; hata cleanup'ı yalnız kendi token'ını bırakır.
  [reading_image_comparison_raster.dart:114](../../app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart#L114)

- Eski veya tekrar dispose, başka sahibin rezervasyonunu değiştiremez.
  [reading_image_comparison_raster.dart:141](../../app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart#L141)

**Kanıt**

- Pending iptal ve resident sahiplik, native başlangıç sayacıyla ayrılır.
  [reading_image_comparison_raster_test.dart:76](../../app/mobile/test/reading_images/reading_image_comparison_raster_test.dart#L76)

- Aynı gate kullanan iki Cubit ve açık yeniden seçim doğrulanır.
  [reading_image_comparison_loaded_test.dart:130](../../app/mobile/test/reading_images/reading_image_comparison_loaded_test.dart#L130)

- Farklı gate altında gerçek HTTP teslimatı ve native ret ayrı gözlenir.
  [reading_image_consumer_qa_test.dart:253](../../app/mobile/test/reading_images/reading_image_consumer_qa_test.dart#L253)

- Kaynak piksel bütçesi küçük hedefe rağmen native öncesinde reddedilir.
  [reading_image_comparison_raster_test.dart:200](../../app/mobile/test/reading_images/reading_image_comparison_raster_test.dart#L200)

- Kabul matrisi, kaynak dönemleri ve açık kanıt sınırları birlikte izlenir.
  [verification.md:29](../../be/docs/evidence/p4b2-raster-reservation/verification.md#L29)
