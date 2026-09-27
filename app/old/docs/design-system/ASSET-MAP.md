# Görsel varlık haritası

Bu harita **UI kabuğu** ile **içerik medyası**nı ayırır. `DESIGN.md` yalnız kabuğun renk/ölçü tokenlarını verir. Kapak ve hikâye illüstrasyonları kitap/öğe verisidir; içlerindeki yazı, renk, karakter ve kompozisyon bir arayüz tokenı değildir. Dosya adları mevcut Flutter deposuna göredir; hedef teknoloji aynı göreli dosyaları kullanabilir veya eşdeğerleriyle eşleyebilir.

| Rol | Kaynak varlık veya veri | Kullanım / sınır | Kanıt |
|---|---|---|---|
| Marka / launcher | `assets/logo/app_logo.jpg`, `assets/logo/app_logo_foreground.jpg`; platform splash ayarları | Splash/launcher bağlamı; launcher kompozisyonunu uygulama içi logo sanma | `assets/logo/`, `splash_screen.dart` |
| Tablet splash animasyonu | `assets/animations/icon_variant4_animation_tablet.json` | `ScreenUtil.isTablet` dalında telefon animasyonundan ayrı Lottie; tam animasyon akışı görüntüyle doğrulanmadı | `splash_screen.dart`, `TABLET.md` |
| Birincil UI fontu | `assets/fonts/Lexend-VariableFont_wght.ttf` ve ağırlık dosyaları | Tema rolleri; bütün içerik resmindeki yazı bu font değildir | `theme.dart`, `pubspec.yaml` |
| Diğer paketli fontlar | `assets/fonts/Merriweather-*`, `Roboto-*`, `Sacramento-Regular.ttf` | Yalnız kaynakta kullanıldığı yüzeyde aktar | `assets/fonts/`, `theme.dart` |
| Kimlik formu başlık resmi | `assets/images/register_login_background.png` | Kayıt ve giriş üst alanı; panelden ayrı katman | `login_user.dart`, `register_user.dart` |
| Uyku başlık resmi | `assets/images/relax_header.jpg` | Uyku sekmesi, 250dp başlık ve fade | `relax.dart` |
| İlk kullanım kolajı | `assets/images/landing/{tr,en,pt}/story1.jpg`…`story12.jpg` | Dil bazlı görsel seti; gömülü görsel yazı bağımsız çevrilemez | `landing_screen.dart` |
| Onboarding arka planı | `assets/images/` altında Remote Config ile seçilen `onboardingBackgroundImage` | Değişken varyant; tek sabit arka plan varsayma | `onboarding.dart` |
| Alt gezinme | `assets/icons/icon_navigate_bar_read.svg`, `icon_navigate_bar_rest.svg`, `icon_navigate_bar_profile.svg` | 3 sekme ikonları, durum rengi UI tokenıyla | `home.dart` |
| Arama / yön / kilit / meta | `assets/images/icon_search.svg`; `assets/icons/icon_arrow_right.svg`, `lock.svg`, `lock_big.svg`, `icon_clock.svg`, `icon_microphone.svg`, `icon_pen.svg`, `icon_palette.svg` | Küçük UI sembolleri; telifli içerik görseliyle karıştırma | `reading_stories.dart`, `info_stories.dart` |
| Premium / hesap | `assets/images/premium_start.svg`, `assets/icons/account_setting_icon.svg`, `language_icon.svg`, `logout_icon.svg`, `account_delete_icon.svg` | Uygulamanın çizdiği CTA ve ayar satırları | `reading_stories.dart`, `user_profile.dart` |
| Profil kurulum seçenekleri | `assets/newIcons/*.svg`, `assets/images/setting_up_account_pages.png` | Yaş/amaç/tür seçeneklerinde yerel ikon ve illüstrasyon | `setting_up_account_pages.dart` |
| Oynatıcı / hikâye paneli | `assets/icons/icon_start.svg`, `icon_stop.svg`, `ic_backward.svg`, `ic_forward.svg`; `assets/images/story_panel_*.svg`, `voice_icon*.svg`, `music_icon*.svg` | Ses ve okuyucu kontrol durumları | `lullaby_content.dart`, `story_content.dart` |
| Dinamik kapak/hero | REST/Firebase verisinden URL; `CachedNetworkImage`, `RobustNetworkImage`, `Image.network` | Hikâye, ninni, meditasyon ve ebeveyn içerik kartları. Görsel alanının en-boy/kırpma kuralını uygula; resmi UI ile yeniden çizme | `story_item_widget.dart`, `meditation_info.dart`, `parent_mode_item_widget.dart` |
| Hikâye sayfa medyası | Firebase Storage ZIP indir/çıkar akışı | Okuyucuda tam sayfa illüstrasyon ve bazen gömülü yazı; birinci/ikinci ZIP sınırı hassas | `story_content.dart`, `firebase_service.dart` |
| Ebeveyn günlük özet kapağı | `Image.network(imageName)` | Günün ücretsiz kitabına ait, değişken veri | `free_parent_mode_widget.dart`, `emulator-design-parent-home.png` |
| RevenueCat paywall | RevenueCat/Store ürün verisi ve SDK yüzeyi | Uygulama banner/kilit CTA'sından ayrıdır; Billing Error 3 dışında gerçek teklif düzeni gözlenmedi | `emulator-after-billing-error.png`, runtime activity |

## Aktarma kuralları

1. Bir varlığı `UI` veya `içerik` diye etiketle; `UI` ikonları token renklendirmesi alabilir, kapaklar alamaz.
2. Uzak görsel gecikirse kart/hero ölçüsünü koru ve `DESIGN.md` placeholder rengini kullan. Hata ve boş durumunu `EXPERIENCE.md` ile eşle.
3. `landing/{tr,en,pt}` seti görsel-lokalizasyon eşlemesidir; yazı uzunluğuna göre metin katmanını serbest bırak. Almanca ve başka dilde görüntü seti yoksa mevcut fallback davranışını ayrıca doğrula.
4. iOS paywall, sistem çubuğu ve font rasterizasyonu Android görüntülerinden türetilemez; piksel doğruluğu iddia etme.
