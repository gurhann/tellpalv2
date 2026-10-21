---
title: 'P5d: uygulamada çevrimdışı görsel karşılaştırması'
type: 'feature'
created: '2026-10-21'
status: 'done'
baseline_commit: 'c634592af83dd5a6f518d44064b2c97c9ad516f4'
review_loop_iteration: 0
context:
  - '/Users/gurhankucuk/Documents/GitHub/tellpalv2/.codex/skills/flutter-scalable-app/SKILL.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** P5c dosya eşitliği, Flutter’da aynı renk ve okunabilirliği kanıtlamaz.

**Approach:** Android emülatöründe ayrı, ağsız QA paketiyle #18 ve #62’nin kapak/ilk sayfasını karşılaştır: 1950, 1952, 8303, 8306; original/PNG/WebP, toplam 12 durum. Yeni dönüşüm yapma.

## Boundaries & Constraints

**Always:** P5c mevcut dosyalarını SHA/ICC ile doğrula. Orijinalleri, eski QA dosyalarını ve kirli çalışma ağacını koru. Feature-first, Freezed Cubit, injectable/get_it, auto_route kullan. Tek raster sahipliği; seri decode, background/stop temizliği. Kapakta ortak detail slot/cover, sayfada contain; üç formatta aynı slot/DPR/filter. Tam boyut 1712×2778; 1:1 yazı kesiti ayrıca göster.

**Ask First:** Yeni bağımlılık indirme, SDK/signing kurulumu, fiziksel cihaz veya kapsam genişletme.

**Never:** Backend, Firebase, auth, storage, canlı ağ, deploy, yayın; normal uygulama paketini değiştirme. Tüm hikâyeler veya fiziksel Display P3 için kabul çıkarma.

## I/O & Edge-Case Matrix

| Durum | Beklenen davranış |
|---|---|
| Dosya/hash/ICC/boyut uyuşmazlığı | Build/gösterim öncesi ret; fallback yok |
| Cache/araç eksikliği | İndirmeden dur; gerekçeyi raporla |
| Ağ izni/provider/plugin içeren APK | Kurmadan reddet |
| Eşzamanlı seçim/background | Eski sonucu boyama; kaynakları bırak |
| Renk/readback farkı | Farkı kaydet; otomatik kalite geçişi verme |

</frozen-after-approval>

## Code Map

- `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart`: owned decode/dispose ve painter; yeniden kullan.
- `app/mobile/lib/core/presentation/widgets/reading_image_viewport.dart`: gerçek kapak geometrisi.
- `app/mobile/lib/app/local_lossless_qa/`: yaşam döngüsü örneği; değiştirme. Eski 822×1334, altı durum, bare VP8L pinleri yeni dosyalara uymaz.
- `tools/image-optimization/local_lossless_emulator.py`: snapshot/hash örneği; host localhost kısıtı emülatörü ağsız yapmaz.
- `be/docs/evidence/p5c-local-lossless/rerun-verification.md`: yaşayan dosya kökü ve kanıtlar. `report.json` özel çıktıdadır; ilk `/private/tmp` çıktısı kayıptır.
- Yerel Flutter `painting.dart`: `rawRgba` gamut daraltabilir; `colorSpace` ve desteklenirse `rawExtendedRgba128` ayrıca ölçülür.

## Tasks & Acceptance

**Execution:**
- [x] `app/mobile/lib/features/reading_images/p5d_offline/`: manifest repository, Cubit, ekran ve testler. Encoded üst sınır 32 MiB/dosya, piksel 4.756.000, tek iş için modellenen tampon bütçesi 192 MiB; süreç RSS sınırı olarak sunma.
- [x] `app/mobile/lib/app/p5d_offline_qa.dart` ve ayrı DI/router: normal main/auth başlangıcından bağımsız kompozisyon.
- [x] `tools/image-optimization/p5d_android_host/`: yalnız gereken Dart kaynaklarını alan minimal Android host şablonu. Firebase/native plugin/provider yok; uygulama ve instrumentation APK’larında INTERNET yok. ADB instrumentation + MethodChannel ile kontrol/sonuç; VM service gerektirme. Manifest/APK ve package bağımsızlığını kurulumdan önce denetle.
- [x] `tools/image-optimization/p5d_offline_app.py`: yeni özel snapshot/output, mevcut cache ile offline release build, yalnız `emulator-5554` üzerinde yeni QA paketini kur/çalıştır/durdur. Durum başına 30 saniye watchdog; aşılırsa yalnız QA paketini force-stop et. Mevcut `/Volumes/MAC_EK/tellpal-qa-private` altında benzersiz dizin; eski çıktı üzerine yazma. Çalışma öncesi/sonrası kaynak hash kanıtı.
- [x] `tools/image-optimization/test_p5d_offline_app.py` ve `app/mobile/test/reading_images/p5d_offline_test.dart`: matristeki retler, cleanup, kaynak/format eşleme ve rapor sınırları.
- [x] `be/docs/evidence/p5d-offline-app/verification.md`: Türkçe sade sonuç; özel dosyalar/ekran görüntüleri dışarıda, Git’te yalnız temiz özet.

**Acceptance Criteria:**
- Given doğrulanmış 12 durum, when ağ izinsiz paket çalışır, then her durumu aynı Flutter backend’inde göster; build/renderer/API/ABI/slot/DPR ve kabul edilen örnek sayısını kaydet.
- Given her durum, when bir ısınma ve üç seri ölçüm yapılır, then asset okuma, checksum, decode, readback ve frame sürelerini ayır; RSS önce/yüklü/sonra örneklerini peak/GPU diye sunma.
- Given aynı native backend, when raster karşılaştırılır, then original↔PNG ve PNG↔WebP farklarını ayrı raporla. JPEG decoder yuvarlamasını codec kaybı sayma; Pillow RGB hash’ini Flutter renk doğrusu yapma.
- Given ekran görüntüleri ve 1:1 kesitler, when incelenir, then renk/yazı gözlemi ile sayısal bulguyu ayır; fiziksel cihaz ve insan kalite kabulünü `NOT_RUN` bırak.

## Spec Change Log

## Verification

- Python unittest, hedefli Flutter test/analyze ve mevcut generator’lar offline çalışır; eksik cache halinde durulur.
- 12 durum tamamlanır veya eksik durumlar açıkça raporlanır. APK ağ izni/provider denetimi ve kaynak koruma hash’leri geçer. Hız kazancı veya üretim profili kararı verilmez.


## Önerilen inceleme sırası

**Giriş ve çevrimdışı sınır**

- Ayrı release girişini ve normal uygulamadan bağımsız başlangıcı incele.
  [p5d_offline_qa.dart:16](../../app/mobile/lib/app/p5d_offline_qa.dart#L16)

- Kurulum öncesi ağ izni ve provider reddini kontrol et.
  [p5d_offline_app.py:274](../../tools/image-optimization/p5d_offline_app.py#L274)

**Görsel sahipliği ve sunum**

- Sabit manifest, dosya kimliği ve üretim pinlerini izle.
  [p5d_manifest_repository.dart:10](../../app/mobile/lib/features/reading_images/p5d_offline/data/p5d_manifest_repository.dart#L10)

- Seri decode, ölçüm ve kaynak kapatmayı izle.
  [p5d_cubit.dart:46](../../app/mobile/lib/features/reading_images/p5d_offline/presentation/p5d_cubit.dart#L46)

- Flutter yüzeyinden fiziksel piksel yakalamayı kontrol et.
  [P5dInstrumentation.java:75](../../tools/image-optimization/p5d_android_host/app/src/androidTest/java/com/tellpal/p5d/P5dInstrumentation.java#L75)

**Kanıt ve hata yolları**

- İşaret, gerçek ROI pikselleri ve 1:1 ölçeği doğrula.
  [p5d_offline_app.py:432](../../tools/image-optimization/p5d_offline_app.py#L432)

- Eksik ölçüm, sunum, koruma ve makbuz hatalarını incele.
  [p5d_offline_app.py:615](../../tools/image-optimization/p5d_offline_app.py#L615)

**Destekleyici doğrulama**

- Manifest ve gerçek lifecycle olaylarının testlerini kontrol et.
  [p5d_offline_test.dart:67](../../app/mobile/test/reading_images/p5d_offline_test.dart#L67)

- Sade sonuçtan ölçüm sınırlarına ve koruma kanıtına ilerle.
  [verification.md:1](../../be/docs/evidence/p5d-offline-app/verification.md#L1)
