---
title: 'P4a2: sınırlı gerçek karşılaştırma çalıştırıcısı'
type: 'feature'
created: '2026-10-17'
status: 'done'
review_loop_iteration: 0
baseline_commit: '8f7adab818db262c9faa2385d299b3f851c3ddad'
context:
  - '{project-root}/be/docs/evidence/p4-reading-images/bounded-run-proposal-2026-10-17.md'
  - '{project-root}/be/docs/evidence/p4-reading-images/bounded-run-proposal-2026-10-17.json'
  - '{project-root}/be/docs/reading-image-comparison.md'
---

<frozen-after-approval reason="Kullanıcı 822×1334 profilini, plan sınırlarını ve test sonrası tek yürütmeyi açıkça onayladı.">

## Amaç

**Sorun:** Ekran yalnız geometri gösteriyor; gerçek fixture çalıştırıcısı eksik.
**Yaklaşım:** Onaylı JSON'dan tek kullanımlık QA aracı oluştur. Yerel test ve bağımsız inceleme sonrası gerçek yürütme ayrı operasyon adımıdır.

## Sınırlar

**Daima:** Plan SHA-256 `f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3`. Yerel DB klonu/V37, tek ücretsiz Türkçe kopya, üç kaynak hash'i, 823×1334 hedef/822×1334 PNG, growth kapalı. 13 nesne/14 create/64 read/16 observe; tüm JSON tavanları. Eski DB salt okunur; süreçler ve `.memlog.md` korunur. Türkçe; oturum modeli devralınır.

**Önce sor:** Yeni kaynak/profil, sınır yükseltme veya başarısız gerçek run'ı yeniden çalıştırma. Bu run'ın onayı tekrar sorulmaz.

**Asla:** Uzak DB/üretim, genel scheduler/poller, başka scope/job, manuel LIVE/COMPLETED, storage süreç/IPC/retry, silme/backfill/dağıtım, token derleme/repoda saklama, normal mobil akış değişikliği. Mevcut UID/geçici credential kullanılır.

## I/O ve hata matrisi

| Durum | Beklenen | Hata |
|---|---|---|
| Varsayılan çağrı | Dosya/credential/DB/ağ I/O yok | Yalnız kullanım özeti |
| Plan/hash/kapsam/DB farkı | Bulut öncesi red | Sır sızdırmadan dur |
| Bütçe/sayaç/restart | Çağrı öncesi kalıcı rezervasyon; tekrar yok | Harcanan korunur |
| Hazırlık | Sadece yeni scope/job; dört tam çıktı ve otomatik LIVE | Kısmi kanıt korunur |
| Public yalıtım/bozuk medya | Normal API'de görünmez; doğrulama hatası açık | Fallback yok |
| Mobil giriş/close/background | Aynı hesap/revision, bellek sahipliği ve sonlu kontroller | Credential/raster temizlenir |

</frozen-after-approval>

## Kod haritası

- `be/src/test/java/com/tellpal/v2/support/P3LocalCmsSetup.java`: scheduler kaldırma, makbuz, seed örneği; 91 kayıt varsayımı kullanılmaz.
- `be/tools/p3_local_cms.py`: güvenli env parser ve özel dosya deseni.
- `AssetStorageClient`: test delegate bütçesi deadline clone ile paylaşılır. `ReadingPublicationService.reconcile(id,tr)` iki, `ReadingImageWorker.run(UUID)` tek çağrıdır.
- `ReadingImageComparisonServiceIT`: gerçek endpoint/DB örneği. `app/mobile/lib/app/reading_image_comparison_{configuration,injection}.dart`: Harness ve DI.
- `tools/image-optimization/native_test_login.py`: mevcut gerçek QA login; secret dosyalar repo dışı.

## Görevler ve kabul

- [x] `be/tools/p4_local_comparison.py`, `be/tools/test_p4_local_comparison.py`: plan/approval, makbuz, yerel klon/launch korumaları.
- [x] `be/src/test/java/com/tellpal/v2/support/P4LocalComparison*.java`: gerçek Spring/SDK servis kompozisyonu, kalıcı test-run bütçesi, seed ve seçili yayın; fake storage ile PostgreSQL/HTTP kapsam testleri.
- [x] `tools/image-optimization/p4_reading_comparison.py`: güvenli mobil kompozisyon, rendition/payload/decode/owned/RSS/ekran kanıtı; gerekirse yalnız QA kancası/testi.
- [x] `be/docs/evidence/p4-reading-images/bounded-run-verification.md`: çalışan testler, inceleme ve gerçek çalışma kapısı ayrımı.

**Kabul:** Geçerli plan/klon verildiğinde tek fixture LIVE olur; normal API onu göstermez. Yanlış hash/sayaç/restart verildiğinde ek I/O başlamaz. Mobil kompozisyon verildiğinde secret kalıcılaşmaz/loglanmaz, görsel kimliği korunur. Yerel fixture fiziksel kalite kanıtı sayılmaz.

## Spec değişiklik kaydı

- 2026-10-17: Gerçek yürütme yerel pg_dump14/sunucu18 uyumsuzluğunu açığa çıkardı.
  Frozen amaç/sayılar korunarak, run başlamadan dört PostgreSQL aracının major eşleşmesi
  ve launcher giriş testleri eklendi. Başarısız run kaydı korunuyor; yeniden yürütme ayrı onay ister.

## Doğrulama

Wrapper `verify`, Python unittest; mobil değişirse ilgili widget/native HTTP testleri ve analyzer, annotation değişirse generator. Dry-run credential/buluta dokunmaz. İnceleme sonrası aynı onaylı run başlatılır.

## Önerilen inceleme sırası

**Tek yürütme ve girdi sınırı**

- Kaynak/hash/klon/credential kontrolleri bulut çağrısından önce tamamlanır.
  [p4_local_comparison.py:269](../../be/tools/p4_local_comparison.py#L269)

- Yalnız yeni fixture, seçili job ve otomatik LIVE orkestrasyonu çalışır.
  [P4LocalComparisonSetup.java:91](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonSetup.java#L91)

**Bütçe ve yalıtım**

- Aşama payı ve her I/O rezervasyonu kalıcıdır; restart kabul edilmez.
  [P4LocalComparisonLedger.java:42](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonLedger.java#L42)

- Gerçek Spring bağlamında scheduler kaldırılır; SDK ve auth aynı ledger ile sarılır.
  [P4LocalComparisonSetup.java:183](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonSetup.java#L183)

**Mobil kanıt ve sahiplik**

- Altı görünür durum kimlik, payload, paint ve kaynak kanıtıyla doğrulanır.
  [p4_reading_comparison.py:83](../../tools/image-optimization/p4_reading_comparison.py#L83)

- Aynı owned raster için tamamlanmış frame beklenir; stale epoch reddedilir.
  [reading_comparison_probe.dart:380](../../app/mobile/test_driver/reading_comparison_probe.dart#L380)

**Testler ve kanıt**

- Gerçek DI üzerinden bütçe sarması ve raw çağrı reddi sınanır.
  [P4LocalComparisonCompositionTest.java:17](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonCompositionTest.java#L17)

- Yerel testler, inceleme ve gerçek çalışma kapısı ayrı kaydedilir.
  [bounded-run-verification.md:1](../../be/docs/evidence/p4-reading-images/bounded-run-verification.md#L1)
