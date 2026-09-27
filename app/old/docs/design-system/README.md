# TellPal mevcut arayüz referansı

Bu klasör v2 tasarım önerisi değil; **1.0.35+333 mevcut arayüzün** teknoloji ve dil bağımsız yeniden oluşturma sözleşmesidir. Önce [DESIGN.md](./DESIGN.md) görsel tokenlarını, sonra [EXPERIENCE.md](./EXPERIENCE.md) davranışı, [SCREENS.md](./SCREENS.md) ekran anatomilerini okuyun. [COVERAGE.md](./COVERAGE.md) 23 router yolunu ve router dışı yüzeyleri kanıt durumuyla denetler; [ASSET-MAP.md](./ASSET-MAP.md) UI ve uzak içerik varlıklarını ayırır. `evidence/` Android yakalamalarının seçilmiş kopyaları, `mockups/` ise temsili statik kompozisyonlardır; kanıt olarak mock değil PNG kullanılır.

Ham telefon görüntülerinin tamamının ekran/durum eşlemesi [RAW-SCREENSHOTS.md](./RAW-SCREENSHOTS.md) içindedir. **Tablet görünümü** için [TABLET.md](./TABLET.md) ve `evidence/tablet-emulated-*.png` referanslarını ayrıca okuyun.

Telefon referansı: Android emulator `Medium_Phone`, 1080×2400 px, 420 dpi (yaklaşık 411×914dp). Tablet boyutu denemesi aynı AVD'de geçici 1600×2560 px / 320 dpi (800×1280dp) ekranla yapıldı; gerçek tablet/iPad donanımı değildir. Tarih: 2026-09-25. iOS ve RevenueCat teklif yüzeyi doğrudan doğrulanmadı; eksikler matriste açık. Crashlytics dosyaları incelenmedi.
