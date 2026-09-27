# Crashlytics Sorun Analizi (iOS + Android)

- Tarih: 13 Şubat 2026
- Kaynak dosyalar: `docs/ios-crashlytics.csv`, `docs/android-crashlytics.csv`
- Kapsam: Crashlytics export verilerindeki issue bazlı hata analizi
- Not: Bu doküman sadece sorunların nedenlerini içerir; çözüm içermez.

## 1) Veri Özeti

- Toplam event: 236
- Toplam fatal event: 29
- Toplam non-fatal event: 207

### iOS

- Event: 7
- Fatal: 5
- Non-fatal: 2
- Unique issue: 4
- Zaman aralığı: 1 Ocak 2026 (tek gün)
- Baskın sürüm: `1.0.32`

### Android

- Event: 229
- Fatal: 24
- Non-fatal: 205
- Unique issue: 17
- Zaman aralığı: 16 Ocak 2026 - 9 Şubat 2026
- Baskın sürüm: `1.0.32` (214 event)

## 2) Sistematik Kök Neden Kümeleri

1. Dosya sistemi ve görsel dosya yaşam döngüsü yarışı
- İndirme/zip açma/silme akışları ile aynı dosyaların UI tarafından okunması eşzamanlı ilerliyor.
- `Future.any` kullanımı zip açma tamamlanmadan başarıya düşebilen bir akış üretiyor.
- Sonuç: boş dosya, bulunamayan dosya, decode/decompress hataları.

2. Ağ bağımlı çalışma ve DNS çözümleme kırılganlığı
- `storage.googleapis.com`, `firebasestorage.googleapis.com`, `fonts.gstatic.com`, `api.revenuecat.com` host çözümleme hataları çok sayıda issue üretiyor.
- Bazı akışlarda aynı tür ağ hatası fatal sınıfına yükseliyor.

3. Yaşam döngüsü ve context güvenliği
- Ekran kapandıktan sonra zamanlanmış callback içinde `context` kullanımına bağlı null-check crash'i var.
- Arka plan/ön plan geçişlerinde servis init ve plugin kanal çağrılarında kırılma görülüyor.

4. Veri şeması/nullability varsayımları
- Remote Config ve user map alanlarında force unwrap/doğrudan cast kullanımı tekil ama fatal crash üretiyor.

5. Gözlemlenebilirlik gürültüsü
- ANR monitor arka planda çok sayıda "Potential ANR" kaydı üretiyor; eventlerin büyük bölümü uygulama BACKGROUND durumunda.

## 3) Madde Bazlı Sorun Envanteri

1. **[Android] `33c41d130c3fe4d52b496c587f143dbb` - Potansiyel ANR raporları**
- Gözlem: 176 event, fatal 0, tamamı `1.0.32`, süreç durumu `BACKGROUND`.
- Semptom: `Potential ANR detected (gap=...ms)`; gap dağılımı çok geniş (3 saniye ile ~33 dakika arası).
- Neden analizi: Bu kayıtlar uygulama gerçekten "ANR" olduğu için değil, watchdog yaklaşımının arka plan durumunda da uzun boşlukları hata olarak yazması nedeniyle yoğunlaşıyor. Eşik ve kayıt mekanizması `lib/utils/performance_monitor.dart:42`, `lib/utils/performance_monitor.dart:44`, `lib/utils/performance_monitor.dart:57`, `lib/utils/performance_monitor.dart:83` içinde; yaşam döngüsü kontrolü `lib/main.dart:251` civarında.

2. **[iOS] `20e23353cae91564b82e30ffba465def` - Potansiyel ANR raporu**
- Gözlem: 1 event, fatal 0, `BACKGROUND`.
- Semptom: `Potential ANR detected (gap=36905ms)`.
- Neden analizi: Android'deki ANR monitor paterni ile aynı sınıf; watchdog arka plan boşluklarını non-fatal olarak kayıtlıyor (`lib/utils/performance_monitor.dart:83`).

3. **[iOS] `bc74c6c5f6185f7862d42de2ddee18b7` - "Could not decompress image"**
- Gözlem: 3 event, fatal 3, sürüm `1.0.32`.
- Semptom: Görsel decode/decompress başarısızlığı.
- Neden analizi: Görsel baytlarının bozuk/eksik olması veya dosya içeriğinin decode edilebilir formatta olmaması durumunda oluşan hata global Flutter error handler üzerinden fatal kaydediliyor. Handler `lib/features/auth/presentation/pages/splash_screen.dart:176` içinde; fatal sınıflandırma filtresi bu sınıfı non-fatal'a düşürmüyor (`lib/features/auth/presentation/pages/splash_screen.dart:190`).

4. **[iOS] `038b7e49e0b3bcb3a52e52b5ed88ac5a` - Directory not empty (silme hatası)**
- Gözlem: 2 event, fatal 2, sürüm `1.0.32`.
- Semptom: `FileSystemException: Deletion failed ... Directory not empty`.
- Neden analizi: Hikaye klasörlerinin sync ve recursive silinmesi, aynı anda klasör üzerinde başka IO işlemleri varken tetikleniyor. Kritik silme noktaları `lib/common/services/firebase_service.dart:641`, `lib/common/services/firebase_service.dart:657`, `lib/common/services/firebase_service.dart:704`.

5. **[iOS] `82b8341c3a17053cc90e592d6a81e10c` - PathNotFoundException (resim dosyası yok)**
- Gözlem: 1 event, fatal 0, sürüm `1.0.32`.
- Semptom: `.../Documents/255-1/1.jpg` bulunamıyor.
- Neden analizi: Dosya varlığı kontrolü ile dosya okunması arasında yarış oluşuyor; dosya cleanup/silme akışında kayboluyor. UI tarafında dosya erişimi `lib/features/stories/presentation/widgets/story_loading_widget.dart:105` ve `Image.file` kullanım noktaları `lib/features/stories/presentation/pages/story_content.dart:770`, `lib/features/stories/presentation/pages/story_content.dart:798`.

6. **[Android] `d620bf15e6fbd61accd9c0a669d09f79` - Boş dosya (FileImage)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: `.../app_flutter/473-2/4.jpg is empty and cannot be loaded as an image`.
- Neden analizi: Dosya mevcut olsa da içerik doğrulaması yapılmadan görsel yükleniyor. `getFile` sadece varlık kontrolü yapıyor (`lib/common/services/firebase_service.dart:609`). Ayrıca zip extraction fallback akışında `Future.any` erken tamamlanma ihtimali taşıyor (`lib/common/services/firebase_service.dart:1652`), bu da eksik dosya setiyle akışın başarıya dönmesine izin verebiliyor.

7. **[Android] `ceb14bd9539faed792b3fb24e2211665` - Decoded image disposed (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: `Decoded image has been disposed`.
- Neden analizi: `cached_network_image` stream/completer yaşam döngüsü sırasında görüntü decode nesnesi dispose edildikten sonra frame akışı devam ediyor. Bu davranış `AdvancedCachedImage` ve hızlı widget lifecycle değişimleriyle tetikleniyor (`lib/common/widgets/advanced_cached_image.dart:32`).

8. **[Android] `b2b1f036388c719d666f878df555745f` - Stream disposed (non-fatal)**
- Gözlem: 1 event, fatal 0, sürüm `1.0.32`.
- Semptom: `Bad state: Stream has been disposed`.
- Neden analizi: 7. maddeyle aynı stream/completer yaşam döngüsü problemi, farklı noktadan non-fatal olarak yakalanmış hali.

9. **[Android] `db8b8ac28264602be9b88d553f00969d` - Google Fonts host lookup (fatal)**
- Gözlem: 9 event, fatal 9, sürüm `1.0.32`.
- Semptom: `fonts.gstatic.com` DNS çözümleme hatası.
- Neden analizi: Uygulama metin stilleri runtime Google Fonts fetch davranışına bağlı. Ağ çözümleme başarısızlığında font yükleme exception'ı üretiliyor ve fatal işaretleniyor. Font kullanımı `lib/config/theme/theme.dart:5`, `lib/config/theme/theme.dart:7`, `lib/features/parent_mode/presentation/page/content_parent_mode.dart:68`, `lib/features/parent_mode/presentation/page/info_parent_mode.dart:117`.

10. **[Android] `89073f4fa30992b5d106d111fd8c49d8` - Firebase Storage host lookup (non-fatal)**
- Gözlem: 14 event, fatal 0, sürüm `1.0.32`, süreç `BACKGROUND`.
- Semptom: `storage.googleapis.com` DNS çözümleme hatası.
- Neden analizi: Splash akışındaki global error handler ve Storage tabanlı görsel URL çağrıları ağ kesintilerinde aynı issue altında toplanıyor (`lib/features/auth/presentation/pages/splash_screen.dart:176`, `lib/common/services/firebase_service.dart:975`).

11. **[Android] `9863f9211fa1a5b6aed82fc9d73a2535` - firebasestorage host lookup (non-fatal)**
- Gözlem: 3 event, fatal 0, sürüm `1.0.32`.
- Semptom: `firebasestorage.googleapis.com` DNS çözümleme hatası.
- Neden analizi: Hikaye/asset indirme akışlarının ağ host çözümlemesine bağımlı olması.

12. **[Android] `8e4e79e566d3b6466d67c12c60801475` - Firebase Auth network-request-failed (non-fatal)**
- Gözlem: 3 event, fatal 0, sürüm `1.0.32`.
- Semptom: `firebase_auth/network-request-failed`.
- Neden analizi: Auth reload çağrıları ağ erişimi yokken başarısız oluyor; bu sınıf uygulama içinde non-fatal toplanıyor.

13. **[Android] `4794f093726edf9ab0a6feb570c7d3e3` - RevenueCat syncAttributes (non-fatal)**
- Gözlem: 7 event, fatal 0, sürümler `1.0.27`/`1.0.28`.
- Semptom: `api.revenuecat.com` host çözümleme hatası (`PlatformException code=10`).
- Neden analizi: RevenueCat attribute/offering senkronizasyonu ağ bağımlı; bu sürümlerde issue network kaynaklı kümelenmiş.

14. **[Android] `e9d633cde582352996b6a168dbc5e372` - RevenueCat getCustomerInfo (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.24`.
- Semptom: `api.revenuecat.com` host çözümleme hatası, ancak fatal olarak kaydedilmiş.
- Neden analizi: Aynı ağ sınıfı hata bazı sürüm/akışlarda fatal sınıflamasına düşüyor; sınıflandırma tutarsızlığı mevcut (`lib/main.dart:188`, `lib/features/auth/presentation/pages/splash_screen.dart:190`).
- Karar notu (15 Şubat 2026): Mevcut veri penceresinde `1.0.32` için bu issue görünmediği için Madde 14 aktif geliştirme kapsamından çıkarıldı; `docs/crashlytics-cozum-task-listesi.md` altında `SKIP / Backlog` olarak izleniyor.

15. **[Android] `2bb950adc27b8d21fad0ab9b154a3532` - RevenueCat invalid app user id (non-fatal)**
- Gözlem: 1 event, fatal 0, sürüm `1.0.32`.
- Semptom: `InvalidAppUserIdError` ile login başarısız.
- Neden analizi: Firebase kullanıcı kimliği ile RevenueCat app user state'i arasında uyuşmazlık/validasyon reddi oluşuyor. İlgili akış `lib/common/services/set_user_type_information_service.dart:200` ve `lib/common/services/set_user_type_information_service.dart:237`.

16. **[Android] `b762641e521da9f1491a98f4aeb2f0ee` - AudioService bind edilemiyor (fatal)**
- Gözlem: 7 event, fatal 7; sürüm dağılımı ağırlıkla `1.0.21` (6 event), ayrıca `1.0.32` (1 event); eventlerin çoğu `BACKGROUND`.
- Semptom: `Unable to bind to AudioService`.
- Neden analizi: Audio service konfigürasyonu splash başlangıcında çalışıyor (`lib/features/auth/presentation/pages/splash_screen.dart:181`) ve arka plan durumuna denk gelen lifecycle anlarında bind hatası oluşuyor. Ayrıca sürüm dağılımı, geçmiş release konfigürasyon farklılıklarını da işaret ediyor.

17. **[Android] `ba7e84aaed75108faaa1ebe6a52f6304` - RevenueCat UI FragmentActivity gereksinimi (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.14`.
- Semptom: `Make sure your MainActivity inherits from FlutterFragmentActivity`.
- Neden analizi: Bu kayıt eski bir sürümde activity taban sınıfı/eklentiler arası uyumluluk farklılığına işaret ediyor. Mevcut kaynakta `MainActivity` AudioService tabanından geliyor (`android/app/src/main/kotlin/com/scoustech/interactive_short_kid_stories_tellpal/MainActivity.kt:5`), issue eski release davranışıyla uyumlu bir iz bırakıyor.
- Karar notu (15 Şubat 2026): Mevcut veri penceresinde `1.0.32` için bu issue görünmediği için Madde 17 aktif geliştirme kapsamından çıkarıldı; `docs/crashlytics-cozum-task-listesi.md` altında `SKIP / Backlog` olarak izleniyor.

18. **[Android] `816ceb3acb685c039160e247c517543e` - Null check operator used on a null value (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: `_navigateHome` akışında null-check çökmesi.
- Neden analizi: Başarılı kayıt sonrası `Timer` ile gecikmeli navigation çalışıyor (`lib/features/auth/presentation/pages/setting_up_account_pages.dart:215`); widget yaşam döngüsü değiştiğinde `context` erişimi null-state'e düşebiliyor (`lib/features/auth/presentation/pages/setting_up_account_pages.dart:1127`).

19. **[Android] `09fad729e622881e9d33441d40ba3cbe` - Parent mode null-check (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: `getDailyFreeParentGuidanceBookId` içinde null-check crash.
- Neden analizi: Remote Config JSON içinden dönen kitap id'si null olabildiği halde zorunlu unwrap yapılıyor (`lib/features/parent_mode/presentation/page/parent_mode_view.dart:150`, `lib/features/parent_mode/presentation/page/parent_mode_view.dart:152`).

20. **[Android] `3cb7836a62e4a317c10abce3eb12c016` - Null is not subtype of int (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: Profil ekranında `totalReadingDuration` cast hatası.
- Neden analizi: User map içinden `int` beklenen alan null geldiğinde doğrudan atama crash'e dönüyor (`lib/features/profile/presentation/page/user_profile.dart:395`).

21. **[Android] `866b7a4551491553627e23deb1cf5540` - MissingPluginException cancel (fatal)**
- Gözlem: 1 event, fatal 1, sürüm `1.0.32`.
- Semptom: Firebase Database channel'ında `cancel` metodu bulunamıyor.
- Neden analizi: Plugin kanal yaşam döngüsünde (engine attach/detach veya lifecycle geçişi) `once(DatabaseEventType.value)` akışının iptal çağrısı native tarafta karşılanamıyor. DB çağrı noktaları `lib/common/services/firebse_real_database_service.dart:162` ve `lib/common/services/firebse_real_database_service.dart:209`.

## 4) Ek Teknik Gözlemler

1. Fatal sınıflandırma tutarsızlığı
- Benzer ağ tabanlı hatalar bazı yerlerde non-fatal, bazı yerlerde fatal görünüyor.
- İki ayrı sınıflandırma fonksiyonu bulunuyor: `lib/main.dart:188` ve `lib/features/auth/presentation/pages/splash_screen.dart:190`.

2. Dosya bütünlüğü doğrulaması eksikliği
- Hikaye görselleri için dosya varlığı kontrolü var, içerik/boyut doğrulaması tutarlı değil.
- Bu durum, "dosya var ama decode edilemiyor" sınıfı hataları açıklıyor.

3. iOS veri penceresi çok dar
- iOS export tek gün ve tek sürüm ağırlıklı. Bu nedenle iOS tarafında frekans yerine neden profili daha belirgin.
