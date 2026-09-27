# TellPal 1.0.33 Crashlytics Detaylı Analiz Raporu

Analiz tarihi: 2026-03-20

Kapsam:
- `docs/1.0.33_crashlytcs/tellpal_ios_1.0.33_crashlyts_logs.csv`
- `docs/1.0.33_crashlytcs/tellpal_android_1.0.33_crashlyts_logs.csv`
- İlgili uygulama kodu ve Crashlytics entegrasyon noktaları

Kanıt kalitesi:
- Doğrudan Crashlytics CSV export verisi kullanıldı.
- Kod korelasyonu yapıldı.
- Bazı event'lerde `issue_id`, `issue_fix_version` ve `reason` alanlarında telemetry kirlenmesi görüldüğü için bu alanlar tek başına kaynak kabul edilmedi.

## Yönetici Özeti

- Toplam 280 event incelendi.
- Android tarafında 222 event ve 20 fatal var.
- iOS tarafında 58 event ve 8 fatal var.
- En büyük hacimli sorunlar gerçek crash değil, startup sırasında çalışan splash, RevenueCat, ağ ve lifecycle yarışları.
- En net ürün etkili fatal sorunlar:
  - Android onboarding geri navigasyon hatası
  - Android Firebase Realtime Database `setPersistenceEnabled()` sırası
  - iOS ve Android audio lifecycle yarışları
- Ek olarak Crashlytics entegrasyonunda çift `FlutterError.onError` tanımı ve yapışkan custom key kullanımı yüzünden raporlama bağlamı kirleniyor.

## Gözlenen Veriler

- Android build: `1.0.33 (327)`
- iOS build çoğunluğu: `1.0.33 (328)`, az sayıda `1.0.33 (326)`
- En büyük issue kümeleri:
  - `SplashScreenViewState._syncUserTypeAndRevenueInBackground`: 119 event, 0 fatal
  - `SharedPreferenceService._performSpecial3DayPaywallSync`: 86 event, 0 fatal
  - `SplashScreenViewState.configureApp.<fn>`: 30 event, 3 fatal
  - `GoRouterDelegate.pop`: 13 event, 13 fatal
  - `MethodChannelAudioPlayer.seek`: 6 event, 6 fatal
  - `AudioPlayer._setPlatformActive.checkInterruption`: 4 event, 4 fatal
  - `FirebaseDatabaseHostApi.setPersistenceEnabled`: 2 event, 2 fatal

## Telemetry ve Raporlama Problemleri

### 1. Crashlytics bağlamı kirleniyor

Durum:
- Hem `lib/main.dart` içinde hem de `lib/features/auth/presentation/pages/splash_screen.dart` içinde `FlutterError.onError` yeniden atanıyor.
- Bazı event'lerde issue title ile custom key `issue_id` farklı hata ailelerini gösteriyor.
- Bazı event'lerde `flutter_error_reason` alanı gerçek hata ile uyuşmuyor.

Kanıt:
- `lib/main.dart:170`
- `lib/features/auth/presentation/pages/splash_screen.dart:213`
- CSV örneklerinde `GoRouterDelegate.pop` event'inde RevenueCat reason taşması görüldü.
- CSV örneklerinde bazı RevenueCat event'lerinde `madde_10` issue key'i taşmış göründü.

Olası neden:
- `FirebaseCrashlytics.instance.setCustomKey()` global ve yapışkan davranıyor.
- Bir event için set edilen key'ler sonraki event'e sızıyor.
- Çift `FlutterError.onError` tanımı event sınıflandırmasını tutarsız hale getiriyor.

Etkisi:
- Gerçek issue kümelerini yanlış yorumlama riski oluşuyor.
- Fatal ve non-fatal sınıflandırması ekran sırasına göre değişebiliyor.

Çözüm önerileri:
- `FlutterError.onError` tek noktada tanımlanmalı.
- Ekran bazlı ikinci override kaldırılmalı.
- `issue_id` ve `issue_fix_version` sadece ilgili `recordError` öncesi set edilmeli.
- Event sonrası temizlenebilecek key'ler için reset stratejisi düşünülmeli.
- Splash içindeki `configureApp()` içinde global handler tekrar kurulmasın.

## Detaylı Sorunlar

### 2. Splash deferred startup sync timeout

Durum:
- En büyük problem kümesi bu.
- Android: 94 event
- iOS: 25 event
- Fatal değil, fakat başlangıç akışında yoğun hata üretip diğer problemlerin üstünü örtüyor.

Kanıt:
- Issue title: `_SplashScreenViewState._syncUserTypeAndRevenueInBackground`
- Hata: `TimeoutException after 0:00:06.000000`
- Reason: `splash_deferred_user_type_sync_failed`
- Event'lerin önemli kısmı `BACKGROUND` ya da inactive/background geçişine yakın.

Kod referansları:
- `lib/features/auth/presentation/pages/splash_screen.dart:136`
- `lib/features/auth/presentation/pages/splash_screen.dart:149`
- `lib/features/auth/presentation/pages/splash_screen.dart:157`
- `lib/features/auth/presentation/pages/splash_screen.dart:163`
- `lib/common/services/set_user_type_information_service.dart:194`
- `lib/common/services/set_user_type_information_service.dart:207`
- `lib/common/services/set_user_type_information_service.dart:1195`

Olası neden:
- Splash sırasında tek zincir içinde şu işler başlatılıyor:
  - `setUserType()`
  - RevenueCat kimlik eşleştirme
  - 3 günlük paywall attribute sync
- Bu akış auth readiness, RevenueCat configure, storage, network ve lifecycle state ile yarışıyor.
- 6 saniyelik timeout, startup için fazla kısa ve tek zincir üzerinde çalışıyor.

Platform değerlendirmesi:
- Android'de hem foreground hem background dağılımı var; düşük seviye cihazlarda daha sık.
- iOS'ta event'lerin önemli bölümü inactive/background sınırlarında.

Çözüm önerileri:
- Splash içindeki işleri birbirinden ayırın.
- Tek `try/catch` altında zincirlemek yerine her işi ayrı guarded task olarak çalıştırın.
- RevenueCat ve user-type sync işlemlerini splash tamamlandıktan sonra başlatın.
- App gerçekten `foregroundActive` olmadan kritik olmayan ağ çağrılarını başlatmayın.
- Timeout sürelerini iş bazlı yönetin; tek timeout ile tüm zinciri kesmeyin.

### 3. RevenueCat 3-day paywall sync timeout ve yanlış hata sınıflaması

Durum:
- Android: 80 event
- iOS: 6 event
- Hata ailesi, startup sonrasında background queue üzerinden çalışıyor.

Kanıt:
- Issue title: `SharedPreferenceService._performSpecial3DayPaywallSync`
- Hata: `TimeoutException after 0:00:04.000000`
- Reason: `revenuecat_sync_attributes_non_network_failure`
- Ayrı olarak `revenuecat_sync_attributes_network_code_10` event'leri de var.

Kod referansları:
- `lib/common/services/shared_preference_service.dart:224`
- `lib/common/services/shared_preference_service.dart:242`
- `lib/common/services/shared_preference_service.dart:261`
- `lib/common/services/shared_preference_service.dart:284`
- `lib/common/services/shared_preference_service.dart:343`
- `lib/common/services/revenuecat_attribute_sync_queue.dart:54`
- `lib/common/services/revenuecat_attribute_sync_queue.dart:120`

Olası neden:
- Queue düşük öncelikli background scheduler ile çalışıyor.
- `Purchases.isConfigured`, `setAttributes` ve `syncAttributesAndOfferingsIfNeeded` startup readiness ile yarışıyor.
- Timeout veya `not_configured` gibi durumlar retryable readiness problemi olmasına rağmen `non_network_failure` olarak ele alınabiliyor.
- 4 saniye timeout özellikle soğuk başlangıç, TestFlight ve düşük kaliteli ağda dar.

Platform değerlendirmesi:
- Android çok daha sorunlu; arka plan geçişleri ve Play Billing tarafı daha kırılgan.
- iOS'ta hacim düşük ama aynı pattern görülüyor.

Çözüm önerileri:
- `TimeoutException`, `not_configured`, channel hazır olmama gibi durumları retryable olarak sınıflayın.
- Queue çalışmadan önce app lifecycle foreground kontrolü ekleyin.
- RevenueCat config hazır olmadan attribute sync başlatmayın.
- Retry stratejisini sadece code `10` ile sınırlamayın.
- Telemetry içine `lifecycle_state`, `configured_state`, `retry_source`, `app_foreground` gibi alanlar ekleyin.

### 4. RevenueCat identity sync yarış problemi

Durum:
- Hacmi düşük ama kök neden açısından önemli.
- `revenuecat_identity_sync_failed` ve `revenuecat_not_configured_for_identity_sync` event'leri var.

Kanıt:
- iOS ve Android'de `SetUserTypeInformationService._executeIdentityTransition`
- Timeout: `0:00:05`
- Ayrı olarak `revenuecat_not_configured_for_identity_sync` exception'ı var.

Kod referansları:
- `lib/common/services/set_user_type_information_service.dart:251`
- `lib/common/services/set_user_type_information_service.dart:260`
- `lib/common/services/set_user_type_information_service.dart:283`
- `lib/common/services/set_user_type_information_service.dart:330`
- `lib/common/services/set_user_type_information_service.dart:357`
- `lib/common/services/set_user_type_information_service.dart:568`

Olası neden:
- Firebase user state stabil hale gelmeden RevenueCat kimliği eşitlenmeye çalışılıyor.
- RevenueCat configure tamamlanmadan kimlik geçişi tetiklenebiliyor.
- Startup sırasında hem `checkPurchaseConfig()` hem de `validateAndFixRevenueCatUserIdentification()` dolaylı olarak aynı sisteme yük bindiriyor.

Etkisi:
- Yanlış kullanıcıya bağlanma riski
- Premium durumunun gecikmeli ya da hatalı hesaplanması
- Startup timeout gürültüsü

Çözüm önerileri:
- Identity sync'i splash zincirinden çıkarın.
- Son başarılı RevenueCat app user id kaydını kullanıp gereksiz transition'ları azaltın.
- Configure ve identity sync'i tek bir single-flight orchestration altında birleştirin.
- `Purchases.isConfigured` false ise doğrudan retry queue'ya alın, anında error üretmeyin.

### 5. Splash sırasında storage/network/image fetch hataları

Durum:
- Splash error handler altında çok sayıda network image ve storage fetch hatası gruplanmış.
- Bu grubun önemli kısmı gerçek crash değil.

Kanıt:
- iOS: `Failed host lookup: storage.googleapis.com`
- Android: `Network is unreachable`, `Connection abort`, `HttpException`
- Bazı event'lerde avatar ve cover image URL'leri hata içinde yer alıyor.

Kod referansları:
- `lib/features/auth/presentation/pages/splash_screen.dart:187`
- `lib/features/auth/presentation/pages/splash_screen.dart:213`
- `lib/common/services/firebase_service.dart:2426`
- `lib/common/services/firebase_service.dart:2447`

Olası neden:
- Uygulama daha ağ kararlı değilken cover/avatar image istekleri başlatılıyor.
- Splash sırasında widget error'ları global `FlutterError.onError` tarafından Crashlytics'e akıyor.
- Beklenen ağ problemleri ile gerçek crash sinyalleri aynı seviyede raporlanıyor.

Platform değerlendirmesi:
- Android'de ağ erişimi ve OEM arka plan kısıtları daha belirgin.
- iOS'ta inactive/background -> foreground geçişine yakın offline DNS hataları baskın.

Çözüm önerileri:
- Splash ekranında image fetch'i zorunlu hale getirmeyin.
- Cache-first ve lazy-load kullanın.
- Network image hatalarını widget seviyesinde `errorBuilder` ile yutun.
- Beklenen transport hatalarını global fatal/non-fatal pipeline'dan ayırın.

### 6. Android onboarding geri navigasyon fatal hatası

Durum:
- 13 fatal event
- Kullanıcıya doğrudan çarpan gerçek bug

Kanıt:
- Issue title: `GoRouterDelegate.pop`
- Hata: `GoError: There is nothing to pop`
- Breadcrumb: `back_from_reg_before_clicked_continue`

Kod referansları:
- `lib/features/auth/presentation/pages/onboarding.dart:62`
- `lib/features/auth/presentation/pages/onboarding.dart:69`
- `lib/features/auth/presentation/pages/onboarding.dart:288`

Olası neden:
- `PopScope(canPop: false)` ile sistem geri hareketi manuel ele alınıyor.
- `_goBack()` içinde stack kontrolü olmadan `context.pop()` çağrılıyor.
- Route stack kökteyken `pop()` çağrısı fatal fırlatıyor.

Çözüm önerileri:
- `context.canPop()` guard ekleyin.
- `pop` mümkün değilse fallback route'a `go()` kullanın.
- Bu akışı widget test ile güvenceye alın.

Örnek yaklaşım:
- `if (context.canPop()) { context.pop(); } else { context.go('/landingScreenView'); }`

### 7. Audio lifecycle ve player komut yarışı

Durum:
- iOS: `MethodChannelAudioPlayer.seek` 6 fatal
- Android: `AudioPlayer._setPlatformActive.checkInterruption` 4 fatal

Kanıt:
- iOS breadcrumb'leri `clicked_audio_story`, `opened_audio_story`
- Android breadcrumb'leri `clicked_lullaby`, `opened_navigation_bar_second_tab`
- Hatalar foreground'da ve kullanıcı etkileşimi sırasında oluşuyor.

Kod referansları:
- `lib/features/stories/presentation/pages/story_content.dart:96`
- `lib/features/stories/presentation/pages/story_content.dart:114`
- `lib/features/stories/presentation/pages/story_content.dart:394`
- `lib/features/stories/presentation/pages/story_content.dart:409`
- `lib/common/services/player_manager_service.dart:106`
- `lib/common/services/player_manager_service.dart:158`
- `lib/common/services/player_manager_service.dart:215`
- `lib/common/services/player_manager_service.dart:266`

Olası neden:
- `setAudioSource()` bazı yerlerde `await` edilmeden çağrılıyor.
- Resume sırasında `seek()` await edilmeden ardından `play()` tetikleniyor.
- Player source swap, interruption ve lifecycle resume olayları birbiriyle yarışıyor.
- iOS method channel bu pencereye daha hassas.

Çözüm önerileri:
- Tüm `setAudioSource()` çağrılarını `await` edin.
- Tüm `seek()` çağrılarını `await` edin.
- Player komutlarını tek serial queue üzerinden yönetin.
- `loading/buffering` durumunda ikinci komut kabul etmeyin.
- Interruption sonrası player state reset stratejisi ekleyin.

### 8. Android Firebase Realtime Database persistence init sırası bozuk

Durum:
- 2 fatal event
- Hata mesajı çok net ve doğrudan platform kural ihlali gösteriyor.

Kanıt:
- `Calls to setPersistenceEnabled() must be made before any other usage of FirebaseDatabase instance`

Kod referansları:
- `lib/injection_container.dart:89`
- `lib/injection_container.dart:90`
- `lib/common/services/firebse_real_database_service.dart` içinde çok sayıda `FirebaseDatabase.instance.ref()` kullanımı mevcut.

Olası neden:
- Bazı startup ya da process recreation akışlarında `FirebaseDatabase.instance` daha önce kullanılıyor.
- Sonrasında `setPersistenceEnabled(true)` çağrılıyor.

Çözüm önerileri:
- `setPersistenceEnabled(true)` çağrısını mümkün olan en erken bootstrap noktasına taşıyın.
- Aynı instance'ın farklı yerlerde erken erişimini engelleyin.
- `FirebaseDatabase.instance` doğrudan kullanımını azaltıp DI üzerinden tek instance'a zorlayın.

### 9. Story image file missing before read

Durum:
- Android 3, iOS 1 non-fatal event
- Hacim düşük ama veri bütünlüğü açısından önemli

Kanıt:
- Exception: `Story image file missing before read`
- Custom key'lerde `story_id`, `story_version`, `image_expected_path`, `image_integrity_type=story_asset_missing`

Kod referansları:
- `lib/common/services/firebase_service.dart:1377`
- `lib/common/services/firebase_service.dart:1391`
- `lib/common/services/firebase_service.dart:1402`
- `lib/common/services/firebase_service.dart:1424`

Olası neden:
- Story klasörü refresh/recovery akışı sırasında dosya kısa süreli kayboluyor.
- Read path ile write/recovery path tam senkronize değil.
- Lifecycle geçişlerinde story directory cleanup ve refresh çakışabiliyor.

Çözüm önerileri:
- Read sırasında staging/live swap penceresini daha sıkı kilitleyin.
- Aynı story için eş zamanlı refresh ve read davranışını merkezileştirin.
- Missing file sonrası ilk denemede sadece report edip devam etmek yerine tek recovery pipeline kullanın.

### 10. iOS ve kısmen genel UI overflow event'leri gereğinden sert raporlanıyor

Durum:
- iOS'ta 2 fatal `RenderFlex overflow`
- Gerçek uygulama çökmesi değil, layout taşması

Kanıt:
- Hata: `A RenderFlex overflowed by 0.570 pixels on the right`
- Global error handler üzerinden Crashlytics'e fatal benzeri düşmüş.

Kod referansları:
- Global handler noktaları:
  - `lib/main.dart:170`
  - `lib/features/auth/presentation/pages/splash_screen.dart:213`

Olası neden:
- Küçük layout overflow hata olarak raporlanıyor.
- Handler bu tür UI hatalarını fatal sınıfına fazla yakın ele alıyor.

Çözüm önerileri:
- Overflow kaynağı bulunan ekran için `Expanded/Flexible` kullanın.
- Non-critical layout assertion'ları Crashlytics'e fatal olarak göndermeyin.
- Dar ekran ve farklı device width senaryoları için widget test ekleyin.

### 11. Native `android.os.MessageQueue.nativePollOnce` gürültüsü

Durum:
- Android'de 5 non-fatal event
- Tek başına anlamlı bir root cause göstermiyor

Kanıt:
- Anlamlı Dart exception yok
- Lifecycle ve audio breadcrumb'leri etrafında gözüküyor

Olası neden:
- ANR benzeri, idle looper veya başka bir üst seviye sorunun yan etkisi olabilir.
- Bu CSV ile tek başına aksiyon çıkarmak için yeterli bağlam yok.

Çözüm önerileri:
- Düşük öncelikli izleme konusu olarak bırakın.
- ANR raporları, Android Vitals veya native tombstone ile birlikte tekrar inceleyin.

## Kök Neden Sıralaması

1. Startup akışında splash, auth, RevenueCat ve network işlerinin gereğinden fazla iç içe çalışması
2. Crashlytics entegrasyonunda çift handler ve yapışkan custom key kullanımı
3. Audio player komutlarının senkronize edilmemesi
4. Android onboarding pop akışında stack guard eksikliği
5. Firebase Realtime Database persistence init sırasının garanti altına alınmaması
6. Story asset refresh/read akışında kısa süreli dosya tutarsızlığı

## Önerilen Aksiyon Önceliği

1. Android onboarding `context.pop()` hatasını düzelt
2. `FlutterError.onError` tanımını tekilleştir
3. Splash deferred startup zincirini parçala
4. RevenueCat sync ve identity akışlarını lifecycle-aware hale getir
5. Audio player komutlarını serialize et ve `await` eksiklerini kapat
6. FirebaseDatabase persistence init sırasını en erken bootstrap aşamasına taşı
7. Story image refresh/read kilitlemesini güçlendir

## Doğrulama Planı

- Soğuk başlangıç testi:
  - çevrimdışı
  - zayıf ağ
  - background -> foreground dönüş
  - anonim kullanıcı
  - giriş yapmış kullanıcı
- Android onboarding geri hareket testi:
  - route stack boşken fatal olmamalı
- RevenueCat testi:
  - `not_configured`
  - timeout
  - offline
  - retry
- Audio testi:
  - play
  - seek
  - resume
  - interruption
  - source swap
- Telemetry testi:
  - art arda farklı hata üretildiğinde `issue_id` ve `reason` sızmamalı

## Sonuç

Bu veri setinde en büyük hacimli problem, uygulamanın splash sırasında çok fazla işi paralel başlatması ve bunları kısa timeout'lar altında aynı hata ailesine toplaması. En kritik gerçek kullanıcı etkili bug'lar ise Android onboarding navigation hatası, Android FirebaseDatabase persistence init problemi ve audio lifecycle yarışları. Bunların yanında Crashlytics telemetry mimarisi temizlenmeden üretim verisini doğru yorumlamak giderek zorlaşacaktır.
