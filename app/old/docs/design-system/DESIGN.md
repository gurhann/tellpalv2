---
name: TellPal 1.0.35 As-Is
description: Mevcut Flutter mobil arayüzünün platformdan bağımsız görsel sözleşmesi.
status: verified-with-gaps
updated: 2026-09-25
colors:
  canvas: '#F5F7F9'
  surface: '#FFFFFF'
  surface-muted: '#E4EAF0'
  surface-lavender: '#E2ECFD'
  ink: '#2C323F'
  ink-muted: '#445064'
  ink-subtle: '#6A81A6'
  primary: '#5462DE'
  primary-dark: '#1F32DD'
  primary-light: '#6371E9'
  on-primary: '#E2ECFD'
  secondary: '#E2123F'
  tertiary: '#00A077'
  error: '#E51D1D'
  outline: '#515E7C'
  story-placeholder: '#E6ECF4'
typography:
  display-large: {fontFamily: Lexend, fontSize: 57dp, fontWeight: 400, letterSpacing: 0dp}
  display-medium: {fontFamily: Lexend, fontSize: 45dp, fontWeight: 400, letterSpacing: 0dp}
  display-small: {fontFamily: Lexend, fontSize: 36dp, fontWeight: 400, letterSpacing: 0dp}
  headline-large: {fontFamily: Lexend, fontSize: 32dp, fontWeight: 400, letterSpacing: 0dp}
  headline-medium: {fontFamily: Lexend, fontSize: 28dp, fontWeight: 400, letterSpacing: 0dp}
  headline-small: {fontFamily: Lexend, fontSize: 24dp, fontWeight: 400, letterSpacing: 0dp}
  title-large: {fontFamily: Lexend, fontSize: 20dp, fontWeight: 600}
  title-medium: {fontFamily: Lexend, fontSize: 16dp, fontWeight: 500, letterSpacing: 0.15dp}
  title-small: {fontFamily: Lexend, fontSize: 14dp, fontWeight: 500, letterSpacing: 0.1dp}
  label-large: {fontFamily: Lexend, fontSize: 18dp, fontWeight: 500, letterSpacing: -0.6dp}
  body-large: {fontFamily: Lexend, fontSize: 16dp, fontWeight: 400, letterSpacing: 0.15dp}
  body-medium: {fontFamily: Lexend, fontSize: 14dp, fontWeight: 400, letterSpacing: 0.25dp}
  body-small: {fontFamily: Lexend, fontSize: 14dp, fontWeight: 400, letterSpacing: 0.4dp}
rounded:
  input: 12dp
  media-card: 16dp
  cover-frame: 20dp
  button: 24dp
  auth-sheet: 16dp
spacing:
  '1': 4dp
  '2': 8dp
  '3': 12dp
  '4': 16dp
  '6': 24dp
  '8': 32dp
  screen-gutter: 16dp
components:
  button-primary:
    background: '{colors.primary}'
    foreground: '{colors.on-primary}'
    height: 48dp
    radius: '{rounded.button}'
  input:
    height: 48dp
    horizontal-padding: 16dp
    radius: '{rounded.input}'
    border: '#E4EAF0'
  story-cover:
    aspect-ratio: '3:4'
    radius: '{rounded.media-card}'
    placeholder: '{colors.story-placeholder}'
---

## Brand & Style

Bu belge **mevcut** TellPal görünümünü tarif eder; v2 için yeniden tasarım önerisi değildir. Genel yüzey açık ve mavimsi beyaz, ana vurgu mavi-mor, tipografi yuvarlak hatlı Lexend'dir. Çocuk içeriklerinin kapak görselleri rengârenktir; arayüz kabuğu bu görselleri sade açık yüzeyler içinde sunar. Uyku ve kimlik doğrulama yüzeyleri illüstrasyonlu koyu mavi başlık alanlarıyla ana kabuktan ayrılır.

Her değerin güven düzeyi ayrı tutulur: **kod** doğrudan Flutter sabiti; **görüntü** Android emulator kanıtı; **çıkarım** yalnızca görüntü veya kodun dolaylı gösterdiği değer. Ekrana özel kurallar genel tokenı bastırır. Örneğin öykü bilgisi başlatma düğmesinde tek renk yerine iki uçlu gradient kullanılır.

## Colors

Temel renkler `lib/config/theme/theme.dart` içindeki `ColorScheme` ve `lib/config/theme/deprecated_colors.dart` sabitlerinden çevrildi. `#5462DE` birincil aksiyon ve seçili gezinme, `#E2123F` ikincil vurgu, `#00A077` üçüncül vurgu, `#2C323F` ana metin, `#F5F7F9` genel zemin olarak kullanılır. Bu renkler genel rolleridir; içerik kapaklarının iç renkleri token değildir.

Gözlenen istisnalar:

| Yüzey | Kural | Kanıt |
|---|---|---|
| Hikâye/meditasyon başlatma | Yukarıdan aşağı `#6371E9 → #1F32DD` | `info_stories.dart`, `meditation_info.dart`; `emulator-haberci-info.png` |
| Hikâye kartı yükleme | `#E6ECF4` placeholder | `story_item_widget.dart` |
| Kimlik doğrulama | Mavi yıldızlı `register_login_background.png` görseli ve beyaz form paneli | `login_user.dart`, `register_user.dart`; `emulator-login-screen.png` |
| Uyku başlığı | `relax_header.jpg`, altta `#E7EFFC → şeffaf` katman | `relax.dart`; `emulator-uyku-loaded.png` |
| Profilde anonim üyelik çağrısı | Düz mavi-mor panel, beyaz pill aksiyon | `emulator-profile.png` |

Gradient ve resim kaplamaları yeniden uygulamada ayrı bileşen varyantı olarak korunmalı; tek genel düğme veya arka plan rengine indirgenmemeli.

## Typography

Merkezi `TextTheme`, `GoogleFonts.lexendTextTheme()` üzerine kurulur (`theme.dart`). `Sacramento` çağrısı var fakat `copyWith` ile gösterilen roller Lexend'den türetilir. Paketlenmiş fontlar arasında Lexend, Merriweather, Roboto ve Sacramento bulunur; bir ekranda fontun gerçekten hangisiyle çizildiği kaynak kullanımına göre belirlenir. Tema rollerinin listesi üstteki YAML'dedir.

Metin uzaması için sabit karakter sayısı varsayılmamalı. Başlık ve açıklamalar alanlarına göre sarılmalı; kart etiketlerinde kısa metin ellipsis'e düşebilir. Görsellerin içine gömülü kitap adı, UI metni değildir ve çevrilemez; dil varyantı varsa ayrı görsel gerekir. `assets/images/landing/` içinde dil bazlı görseller vardır.

## Layout & Spacing

Android referans cihazı: **1080×2400 fiziksel px, 420 dpi** (`adb shell wm size/density`, 2026-09-25). Ölçek **2.625 px/dp**, mantıksal çerçeve yaklaşık **411.4×914.3 dp**. Ekran görüntüsü koordinatları fiziksel pikseldir; uygulama tariflerinin ölçü birimi dp'dir. Sistem durum/gezinme çubukları platformun güvenli alanı olarak ayrılır; resimli sayfalarda arka plan bu alanlara uzanabilir.

Tablet boyutu için aynı Android AVD geçici olarak **1600×2560 px / 320 dpi**, yani **800×1280dp** portreye alındı. Bu boyutta ayrı renk/font seti değil, bazı ekranlarda ayrı ölçü ve sütun dalları kullanılır. Örneğin auth iç payı 121dp, seçim gridleri üç sütun, kategori gridleri ise iki sütun kalır. Görüntüyle doğrulanan ve yalnız kodla bilinen varyantlar [TABLET.md](./TABLET.md) içinde ayrı etiketlenir. Gerçek tablet veya iPad piksel paritesi iddia edilmez.

Ana listelerin yatay payı çoğunlukla 16dp'dir. Dikey ritimde 4/8/12/16/24/32dp baskındır; tüm uygulama tek bir katı grid kullanmaz. `LoginUserView` form panelini ekran yüksekliğinin %25'inden başlatır, başlığı yaklaşık %13'te konumlandırır. Bu oransal kural ekran tarifinde korunur. Hikâye ve ebeveyn detayları uzun dikey kaydırma içerir. Kart listeleri yatay kaydırılır; ilk kart 16dp sol payla başlar.

## Elevation & Depth

Ana düz yüzeylerde gölge azdır. Hikâye kartında iç gölgeler ve kenar vurguları kullanılır (`story_item_widget.dart::_createDecoration`); basit dış gölgeyle eşdeğer değildir. Çoğu ayrım renk, geniş boşluk ve yuvarlatılmış kenarla sağlanır. Header görselleri altta gradient ile açık zemine karışır.

## Shapes

Form alanı 12dp köşeli, içerik/medya kartı 16dp, hikâye placeholder çerçevesi 20dp, birincil düğme 24dp köşelidir. Dairesel avatar ve kilit rozeti için tam yuvarlak biçim kullanılır. Kimlik doğrulama paneli yalnız üst köşelerde 16dp yuvarlatılır. Kart illüstrasyonu `cover` ile kırpılır; resmin kendi çizimini UI bileşeni gibi yeniden üretmeyin.

## Components

| Bileşen | Görsel sözleşme | Durumlar / kaynak |
|---|---|---|
| Alt gezinme | Üç eşit sekme; SVG ikon, seçili `primary`, diğerleri `ink-subtle`; beyaz yüzey | `home.dart`, `emulator-home2.png` |
| Birincil düğme | 48dp yükseklik, 24dp köşe, Lexend label; genel durumda düz `primary` | `theme.dart`; disabled ekran bazlı soluk mavi/gri |
| Hikâye bilgi aksiyonu | 343×48dp kod sabiti; mavi gradient; kilitli içerikte premium metni | `info_stories.dart`, `meditation_info.dart` |
| Metin alanı | Min 48dp, 16dp iç yatay pay, 12dp köşe, 1.5dp açık kenar | `theme.dart`, `message_box_widget.dart` |
| Kapak kartı | 3:4 kapak; 16dp kırpma, 20dp dış placeholder; kilit rozeti üst köşede | `story_item_widget.dart` |
| Yatay içerik bölümü | Bölüm başlığı, kısa alt metin, sağda “tümünü gör” aksiyonu, yatay kart rayı | `reading_stories.dart`, `relax.dart` |
| Profil ayar satırı | Sol ikon+etiket, sağ değer/anahtar/ok, alt ayrım çizgisi | `user_profile.dart`, `emulator-auth-profile.png` |
| Ses kartı | Görsel üstüne alt kenarda yarı saydam koyu başlık bandı; 16dp köşe | `lullaby_item_widget.dart`, `emulator-uyku-loaded.png` |
| Modal | Arka plan karartma, ortada açık yüzey; içerik tipine göre seçim/onay | `emulator-user-type-dialog.png`, `emulator-logout-confirm.png` |
| Hikâye okuyucu | Uzak ZIP'den gelen tam ekran illüstrasyon; metin çoğu zaman görselin parçası olabilir | `story_content.dart`, `emulator-haberci-content.png` |

İkon kimlikleri ve varlık kökenleri için [ASSET-MAP.md](./ASSET-MAP.md); davranış için [EXPERIENCE.md](./EXPERIENCE.md). Görsel kompozisyon örnekleri `mockups/` altındadır. Belge ile örnek mock çatışırsa bu belge ve ekran tarifi esas alınır.

## Do's and Don'ts

| Yap | Yapma |
|---|---|
| Mevcut ekranın gerçek varyantını ve kaynağını kaydet | Farklı kartları tek varsayımsal stile zorla |
| UI metnini alan kimliği olarak tarif et | Çeviri metnini koordinat hesabına sabitle |
| Uzaktan gelen kapak/öykü görselini asset olarak eşle | Öykü illüstrasyonunu CSS/UI ile yeniden çiz |
| Android gözlemi ve iOS çıkarımını ayrı etiketle | Android görüntüsünü iOS doğrulaması olarak sun |
