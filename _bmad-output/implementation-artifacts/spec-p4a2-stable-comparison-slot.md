---
title: 'P4a2: kol değişiminde sabit karşılaştırma alanı'
type: 'bugfix'
created: '2026-10-17'
status: 'done'
route: 'one-shot'
baseline_commit: 'b6a69284e9db18d3f4b7172615de7801e7b23d8c'
---

# P4a2: kol değişiminde sabit karşılaştırma alanı

## Amaç

**Sorun:** Çalışan Android emülatörde kaynak/rendition düğmesinin değişen metni `Wrap` satır sayısını ve sayfa görseli alanını değiştiriyordu. Bu, iki kolun aynı alanda karşılaştırılması kabulünü bozuyordu.

**Yaklaşım:** Düğmenin iki metni için aynı alanı ayır. Tam rect eşitliğini iki yönde ve iki metin ölçeğinde sına. Gerçek ağsız emülatörde kapak ve iki sayfanın altı durumunu önce/sonra kaydet. Normal tüketici, auth, cache ve backend değişmez. Sayısal profil, bulut çalışması ve fiziksel kalite/performans kabulü açık kalır.

## Önerilen inceleme sırası

- Düğmenin metni değişirken ayrılan alan sabit kalır.
  [reading_image_comparison_view.dart:68](../../app/mobile/lib/features/reading_images/presentation/view/reading_image_comparison_view.dart#L68)

- Emülatördeki önce/sonra ölçümü ile fiziksel cihaz ve kalite sınırlarını birlikte oku.
  [emulator-geometry-2026-10-17.md:31](../../be/docs/evidence/p4-reading-images/emulator-geometry-2026-10-17.md#L31)

- Tam rect eşitliği iki kol yönünde, kapakta ve iki sayfada sınanır.
  [reading_image_comparison_view_test.dart:12](../../app/mobile/test/reading_images/reading_image_comparison_view_test.dart#L12)

- Dolu bölüm ile fiziksel diskteki bölüm dışı alanı ayır; temizlik yapılmadı.
  [mac-ek-space-2026-10-17.md:5](../../be/docs/evidence/p4-reading-images/mac-ek-space-2026-10-17.md#L5)
