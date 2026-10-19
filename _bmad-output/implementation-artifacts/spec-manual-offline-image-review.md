---
title: 'Ağsız elle görsel kontrol ekranı'
type: 'feature'
created: '2026-10-19'
status: 'done'
route: 'one-shot'
---

# Ağsız elle görsel kontrol ekranı

## Amaç

**Sorun:** Mevcut QA ekranı otomasyon komutunu bekliyor; kullanıcı görselleri elle inceleyemiyor.

**Yaklaşım:** Mevcut altı PNG/kayıpsız WebP dosyasını ayrı yerel girişte Başlat → Sonraki → Bitir ile göster. 22 test ve analiz geçti. Onaylı iki eski QA paketinin APK yedekleri doğrulandı; paketler kaldırılıp yeni APK kuruldu. Altı gerçek emülatör ekranının görsel alanı yerel PNG referansıyla piksel piksel eşleşti. Ekran kullanıcı için Başlat durumunda teslim edildi. Bu iş yalnız kontrol ekranının hazırlanmasını tamamlar; insan kalite kabulü, fiziksel cihaz ve P4b ölçümü açık kalır.

## Önerilen inceleme sırası

- Yalnız ayrı native debug girişini açar.
  [main_manual_reading_images.dart:9](../../app/mobile/lib/main_manual_reading_images.dart#L9)

- Elle ilerleme ve Başlat anında geometri sabitleme.
  [manual_reading_images_qa.dart:62](../../app/mobile/lib/app/manual_reading_images_qa.dart#L62)

- Kullanıcı kontrolü ve kabul sınırları.
  [manual-reading-images-qa.md:1](../../be/docs/manual-reading-images-qa.md#L1)

- Kurulum engeli ve saklanan kanıtlar.
  [manual-offline-images-verification.md:1](../../be/docs/evidence/manual-offline-images-verification.md#L1)

- Sıra, iptal ve geometri regresyonları.
  [manual_reading_images_qa_test.dart:1](../../app/mobile/test/reading_images/manual_reading_images_qa_test.dart#L1)
