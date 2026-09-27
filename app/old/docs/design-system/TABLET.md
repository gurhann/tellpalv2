# Tablet görünümü — mevcut uygulama

Bu belge yeni tablet tasarımı önermiyor; bugünkü Flutter uygulamasının geniş ekrandaki **mevcut** kurallarını kaydediyor. Kanıt düzeyleri: **G** = tablet boyutuna geçirilmiş Android emülatöründe görüldü, **K** = koddan doğrulandı, **Y** = henüz gözlenmedi. Görüntüler `evidence/tablet-emulated-*.png` dosyalarındadır. Telefon ve tabletin ortak renk/font tokenları [DESIGN.md](./DESIGN.md), akışlar [EXPERIENCE.md](./EXPERIENCE.md) içindedir.

## Referans ve sınır

- Kurulu ayrı tablet AVD yoktu. `Medium_Phone` AVD'nin ekranı geçici olarak **1600×2560 px, 320 dpi = 800×1280dp** yapıldı; uygulama zorla kapatılıp yeniden açıldı. Böylece Android `ScreenUtil.isTablet` dalı çalıştı. Deneme sonrası özgün **1080×2400 px / 420 dpi** ayarları geri yüklendi ve doğrulandı. Bu, 800dp portre geniş ekran davranışını kanıtlar; tablet donanımı, gerçek tablet sistem çubukları veya iPad rasterizasyonunu kanıtlamaz.
- `lib/core/utils/screen_utils.dart::checkIsTablet2()` açılışta ekranın kısa kenarını dp cinsinden hesaplar: **600dp ve üzeri tablet**. Değer başlangıçta `lib/main.dart` içinde bir kez hesaplanır; aynı oturumda pencere yeniden boyutlandırılmasına dinamik uyum doğrulanmadı.
- `lib/main.dart` yalnız `portraitUp` yönünü ayarlar. `ios/Runner/Info.plist` iPad için de yalnız portreyi bildirir. Yatay tablet yerleşimi **mevcut ürünün doğrulanmış bir durumu değildir**.
- Genel tema aynı kalır; tablet için ayrı renk sistemi yoktur. Daha geniş ekran her zaman daha fazla sütun anlamına gelmez: ana içerik rayları yatay kalır, hikâye ve ebeveyn kategori gridleri kodda iki sütun kalır. Yalnız belirli form/seçim gridleri üç sütuna çıkar.

## Görüntüyle doğrulanan yüzeyler

| Yüzey | 800×1280dp portre görünüm | Görüntü | Kaynak |
|---|---|---|---|
| Açılış | Ortalanmış logo, geniş açık boşluk; bu yakalama yükleme anıdır | `tablet-emulated-splash.png` | `splash_screen.dart`; tablet Lottie varyantı kodda |
| Kitaplık | 16dp dış pay korunuyor; premium banner ve arama tam enli. İçerik rayları grid'e dönüşmüyor; aynı 3:4 kapakların daha çoğu aynı anda görülüyor. Üç sekmeli alt çubuk genişliğe yayılıyor | `tablet-emulated-home-loaded.png` | `home.dart`, `reading_stories.dart`, `story_item_widget.dart` |
| Hikâye bilgisi | Hero neredeyse ekran eni ve yüksekliğin büyük bölümünü kaplıyor; süre/yaş pill'leri alt köşelerde. Gradient başlangıç CTA'sı **ortalanmış, sabit yaklaşık 343dp**; altında seslendirme, özet ve krediler | `tablet-emulated-story-info.png` | `info_stories.dart` |
| Uyku | Üst resimli alan tüm genişliğe geriliyor, başlık ortalı. Meditasyon ve sesli hikâye dikey kapak raylarında daha fazla öğe; ninni geniş yatay kart rayı olarak kalıyor. Yükleme placeholder'ı aynı kart çerçevesini koruyor | `tablet-emulated-sleep.png` (yükleme), `tablet-emulated-sleep-loaded.png` | `relax.dart`, medya kartları |
| Profil | Avatar/ad üstte uzun yatay panel; iki sayaç yan yana. Premium panel ve ayar satırları neredeyse tüm genişliğe yayılıyor; **dar, ortalı form kolonuna dönmüyor**. Alt boşluk belirgin | `tablet-emulated-profile.png` | `user_profile.dart` |
| Çocuk/ebeveyn seçimi | Karartma üzerinde **ortalanmış, sınırlı genişlikli** modal; iki büyük satır ve kapatma ikonu | `tablet-emulated-mode-dialog.png` | `usert_type_button.dart` |
| Ebeveyn vitrini | Premium CTA ortalanmış dar sütunda; günlük ücretsiz özet açık mavi kartı neredeyse tam enli ve çok yüksek. Kapak ortada yaklaşık 300×400dp; özet metni solda; dinleme CTA'sı kartın dibinde. İçerik kısa olduğunda ortada **çok büyük boş alan** oluşuyor. Bu mevcut davranış, önerilen düzeltme değil | `tablet-emulated-parent.png` | `parent_mode_view.dart`, `free_parent_mode_widget.dart` |

## Yalnız kodla doğrulanan tablet varyantları

| Yüzey | Telefon → tablet farkı | Kaynak / durum |
|---|---|---|
| İlk kullanım kolajı | Kapak genişliği **150 → 250dp**; metin/alt CTA yatay payları **16 → 121/150dp**; hesap seçenekleri sheet yüksekliği ekranın **%75 → %60**'ı, yatay iç pay **16 → 121dp**, üst iç pay **24 → 74dp** | `landing_screen.dart`; K, görüntü yok |
| Kayıt/giriş | Beyaz panel içindeki formun yatay payı **16 → 121dp**, üst payı **48 → 121dp**, alt payı **0 → 40dp**. Kayıtta klavye açılınca panel üst konumu telefonda ekranın %13'üne çekilir, tablette %25'te kalır | `register_user.dart`, `login_user.dart`; K |
| Profil kurma/düzenleme | Amaç/görsel ve avatar seçimleri **2 → 3 sütun** | `enter_user_profile.dart`, `profile_edit.dart`; K |
| Hikâye kategori grid'i | **2 sütun korunur**; öğeler arası yatay/dikey boşluk **16 → 32dp**. Ninni kategorisinde **1 sütun** korunur | `stories_grid.dart`; K |
| Ebeveyn kategori grid'i | **2 sütun korunur**; öğe boşluğu **16 → 32dp** | `parent_mode_grid.dart`; K |
| Hikâye/meditasyon hero varyantı | Hero aspect ratio telefon durumuna göre **0.75/0.90**, tablet dalında **1.1**. Gerçek görsel kırpması içerik türüne bağlı | `info_stories.dart`, `meditation_info.dart`; tablet hikâye bilgisi G, meditasyon K |
| Ebeveyn günlük özet | Kapak **150×200 → 300×400dp**; kart aspect ratio **0.75 → 1**; bazı dikey paylar iki katına çıkar | `free_parent_mode_widget.dart`; G+K |
| Ebeveyn rehber bilgisi | Kapak **243×324 → 364×486dp**; aspect ratio dalları 1.1/1.5; bazı metinler **12→20dp** | `info_parent_mode.dart`; K |
| Ebeveyn metni/oynatıcısı | Metin bazı alanlarda **14→24dp**; üst boşluk artar, medya/oynatıcı kontrolü büyür | `content_parent_mode.dart`, `listen_parent_mode.dart`; K |
| Hikâye bitişi | Önerilen içerik kapağı **150×200 → 300×400dp** | `story_last_page_widget.dart`; K |
| Splash animasyonu | Tablet için ayrı `icon_variant4_animation_tablet.json` | `splash_screen.dart`; animasyonun tam akışı gözlenmedi |

## Yeniden oluşturma sınırı

Tablet görünümünü telefon ekranını eşit oranda büyüterek üretmeyin. Kodun ayrı dal verdiği yerde yukarıdaki ölçü/sütun kuralını kullanın; vermediği yerde mevcut ekranın genişleme biçimini görüntüyle koruyun. Özellikle ebeveyn kartındaki büyük boşluk, profilin tam genişliğe yayılması ve kategori gridlerinin iki sütunda kalması bugünkü ürünün özellikleridir. Bunların iyileştirilmesi v2 ürün/tasarım kararı gerektirir, bu belgenin parçası değildir.

**Açık kanıt boşlukları:** gerçek Android tablet, iPad, yatay yön, tablet onboarding/auth formları, kategori gridlerinin gerçek ekranı, profil kurma/düzenleme, ses oynatıcıları, ebeveyn detay/okuyucu, RevenueCat teklif düzeni. Bunlar ekran tariflerinin tablet paritesi olarak sunulmamalı.
