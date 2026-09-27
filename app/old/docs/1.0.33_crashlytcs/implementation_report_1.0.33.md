# TellPal 1.0.33 Crash Hardening Uygulama Raporu

Tarih: 2026-03-21

## Amaç

Bu rapor, 1.0.33 Crashlytics analizinden çıkan aksiyonların kod bazında nasıl uygulandığını, her değişikliğin hangi crash/failure kümesini hedeflediğini ve beklenen etkisini özetler.

## Uygulanan Değişiklikler

### T1. Telemetry bootstrap tekilleştirme

- Değişiklik:
  - Global `FlutterError.onError` ataması tek noktaya indirildi.
  - Splash ekranındaki ikinci crash handler override kaldırıldı.
  - Global zone error capture, ortak reporter üstünden issue metadata ile çalışacak şekilde düzenlendi.
- Dosyalar:
  - `lib/main.dart`
  - `lib/features/auth/presentation/pages/splash_screen.dart`
  - `lib/common/services/crashlytics_reporter.dart`
- Çözmesi gereken problem:
  - Aynı hatanın farklı bootstrapping noktalarından farklı metadata ile raporlanması.
  - Fatal/non-fatal kararının ekran bazlı override edilmesi.
  - Crashlytics custom key’lerinin bir ekrandan başka bir event’e sızması.
- Beklenen etki:
  - Tüm framework ve zone tabanlı hatalar tek capture hattından geçer.
  - Splash özelindeki crash handler sapmaları ortadan kalkar.
  - Issue gruplaması daha stabil hale gelir.

### T2. Event-scoped Crashlytics metadata

- Değişiklik:
  - `CrashlyticsReporter` eklendi.
  - `issue_id`, `issue_fix_version` ve event’e özel metadata set edilip event sonunda resetlenir hale getirildi.
  - RevenueCat, story image, realtime database ve lifecycle kaynaklı hata raporları reporter’a taşındı.
- Dosyalar:
  - `lib/common/services/crashlytics_reporter.dart`
  - `lib/common/services/shared_preference_service.dart`
  - `lib/common/services/set_user_type_information_service.dart`
  - `lib/common/services/firebase_service.dart`
  - `lib/common/services/firebse_real_database_service.dart`
  - `lib/common/services/audio_service_lifecycle_initializer.dart`
  - `lib/common/widgets/safe_file_image.dart`
  - `lib/features/parent_mode/presentation/page/parent_mode_view.dart`
- Çözmesi gereken problem:
  - Bir event için set edilen custom key’lerin sonraki event’lere sızması.
  - RevenueCat ve story-image issue metadata’sının karışması.
- Beklenen etki:
  - Crashlytics event’i kendi bağlamıyla raporlanır.
  - Yanlış issue mapping ve sticky key etkisi azalır.

### T3. Splash deferred startup coordinator

- Değişiklik:
  - Splash içindeki kritik olmayan startup işleri bağımsız deferred task’lere ayrıldı.
  - Foreground gereksinimi, timeout ve hata nedeni task bazında tanımlandı.
  - Announcement preload, notification prompt, daily ad refresh ve RevenueCat startup işleri bu coordinator üzerinden çalıştırılmaya başlandı.
- Dosyalar:
  - `lib/common/services/startup_deferred_task_coordinator.dart`
  - `lib/features/auth/presentation/pages/splash_screen.dart`
- Çözmesi gereken problem:
  - Splash ekranında tek zincir içinde çalışan çok sayıda yan iş yüzünden startup yarışları.
  - Kritik olmayan network işlerinin navigation akışını geciktirmesi.
  - Hangi deferred startup işinin hata verdiğinin Crashlytics’te seçilememesi.
- Beklenen etki:
  - Navigation bloklanmadan arka plan startup işleri çalışır.
  - Her startup hatası task adı ve task bağlamıyla izlenir.
  - Background durumunda kritik olmayan işlerin gereksiz tetiklenmesi azalır.

### T4. RevenueCat 3-day paywall queue sertleştirme

- Değişiklik:
  - `special_3_day_paywall` attribute sync akışı foreground-aware hale getirildi.
  - `not_configured`, timeout ve channel hazır olmama durumları retryable olarak ele alındı.
  - Telemetry’ye `configured_state`, `retry_source`, `app_foreground` gibi bağlam alanları eklendi.
- Dosyalar:
  - `lib/common/services/shared_preference_service.dart`
  - `test/common/services/revenuecat_attribute_sync_queue_test.dart`
- Çözmesi gereken problem:
  - RevenueCat init hazır olmadan attribute sync denenmesi.
  - Timeout veya method-channel readiness hatalarının hard fail gibi davranması.
  - Aynı attribute değerinin tekrar tekrar flood etmesi.
- Beklenen etki:
  - RevenueCat hazır değilken sync daha güvenli şekilde retry edilir.
  - Background ve readiness kaynaklı gürültü daha anlamlı sınıflanır.
  - Queue davranışı deterministikleşir.

### T5. RevenueCat identity ve premium orchestration tekilleştirme

- Değişiklik:
  - `setUserType()` canonical RevenueCat state sync akışına bağlandı.
  - `synchronizeRevenueCatState()` tekil orchestration noktası olarak eklendi.
  - Login ve premium recovery tarafındaki doğrudan duplicate RevenueCat identity çağrıları kaldırıldı.
- Dosyalar:
  - `lib/common/services/set_user_type_information_service.dart`
  - `lib/common/services/premium_status_recovery_service.dart`
  - `lib/features/auth/presentation/cubit/login_cubit.dart`
  - `lib/features/auth/presentation/pages/splash_screen.dart`
- Çözmesi gereken problem:
  - Splash, login ve recovery akışlarının aynı oturumda tekrar tekrar RevenueCat configure/identity/premium check başlatması.
  - UID alignment ve premium refresh adımlarının farklı yerlerde yarışması.
- Beklenen etki:
  - Aynı oturumda tek single-flight RevenueCat state sync çalışır.
  - Duplicate tetikleyiciler ana işi yeniden koşmak yerine aynı akışa bağlanır.
  - Premium durum geri kazanımı daha tutarlı hale gelir.

### T6. Android onboarding geri navigasyon bug fix

- Değişiklik:
  - Root stack’te `pop()` yerine `canPop()` guard ve fallback route kullanıldı.
  - Test eklendi.
- Dosyalar:
  - `lib/features/auth/presentation/pages/onboarding.dart`
  - `test/features/auth/presentation/pages/onboarding_test.dart`
- Çözmesi gereken problem:
  - Android tarafında `There is nothing to pop` fatal’i.
- Beklenen etki:
  - Onboarding back aksiyonu root stack’te crash üretmez.
  - Geri navigasyon deterministik hale gelir.

### T7. PlayerManager komut serializasyonu

- Değişiklik:
  - Ortak `AudioCommandQueue` eklendi.
  - `setAudioSource`, `seek`, `play`, `pause`, `stop` ve completion reset akışları serialize edildi.
  - Tekrarlanan stream listener bağlama kalıpları sadeleştirildi.
- Dosyalar:
  - `lib/common/services/audio_command_queue.dart`
  - `lib/common/services/player_manager_service.dart`
  - `test/common/services/audio_command_queue_test.dart`
- Çözmesi gereken problem:
  - Audio method-channel komutlarının üst üste binmesi.
  - `setAudioSource()` sonrası beklenmeyen `seek/play` yarışları.
  - Completion sırasında reset ve yeni source swap çakışmaları.
- Beklenen etki:
  - Audio komutları tek sıra halinde akar.
  - Loading/buffering sırasında yarış kaynaklı native/audio crash olasılığı düşer.

### T8. StoryContent lifecycle audio guard

- Değişiklik:
  - Story ekranındaki resume, page-change ve voice toggle akışları queue’ya bağlandı.
  - Stale completion event’lerinin yanlış sayfayı auto-advance etmesini engelleyen guard’lar eklendi.
  - Sayfa değişiminde eski sesin kontrollü durdurulması sağlandı.
- Dosyalar:
  - `lib/features/stories/presentation/pages/story_content.dart`
- Çözmesi gereken problem:
  - App resume sırasında `seek` ve `play` yarışları.
  - Hızlı sayfa değişiminde eski sayfaya ait playback completion’ın yeni sayfayı etkilemesi.
  - Voice kapatma/açma sırasında stale player state çakışmaları.
- Beklenen etki:
  - Story voice playback akışı daha deterministik hale gelir.
  - Yanlış auto-advance ve stale playback sorunları azalır.

### T9. Firebase Realtime Database bootstrap order

- Değişiklik:
  - `FirebaseDatabase.instance` erken oluşturulup singleton olarak DI container’a kaydedildi.
  - `setPersistenceEnabled(true)` tüm kullanımlardan önce çalışacak sıraya çekildi.
  - Service içindeki doğrudan `FirebaseDatabase.instance` erişimleri injected instance’a taşındı.
- Dosyalar:
  - `lib/injection_container.dart`
  - `lib/common/services/firebse_real_database_service.dart`
- Çözmesi gereken problem:
  - Realtime Database persistence ayarının ilk kullanımdan sonra set edilmesi.
  - Init-order kaynaklı Android crash veya plugin state bozulmaları.
- Beklenen etki:
  - Persistence enable sırası garanti altına alınır.
  - RTDB bootstrap daha güvenli olur.

### T10. Story asset transactionality

- Değişiklik:
  - Missing story image recovery akışı per-story single-flight hale getirildi.
  - Missing image report’ları throttle edildi.
  - `SafeFileImage` tarafında invalid/missing dosya davranışı daha güvenli hale getirildi.
- Dosyalar:
  - `lib/common/services/firebase_service.dart`
  - `lib/common/widgets/safe_file_image.dart`
- Çözmesi gereken problem:
  - Aynı story asset için eşzamanlı read/recover yarışları.
  - Aynı missing-image event’inin tekrar tekrar loglanması.
  - Bozuk veya silinmiş local image dosyalarının render aşamasında kararsız davranış göstermesi.
- Beklenen etki:
  - Tek asset için aynı anda birden fazla recovery koşturulmaz.
  - Crashlytics gürültüsü azalır.
  - Invalid local asset fallback davranışı daha güvenli hale gelir.

### T11. Overflow ve nativePollOnce noise policy

- Değişiklik:
  - `RenderFlex overflow` sınıfı global fatality kararında non-fatal olarak işaretlendi.
- Dosyalar:
  - `lib/main.dart`
- Çözmesi gereken problem:
  - Sub-pixel layout overflow olaylarının gerçek fatal crash gibi görünmesi.
- Beklenen etki:
  - Layout noise ile ürün etkili fatal crash’ler ayrışır.
- Durum:
  - Kısmi tamamlandı.
  - Overflow üreten spesifik ekranın ayrı UI düzeltmesi bu patch set içinde yapılmadı.

## Ek Güçlendirmeler

- `safe_file_image` Crashlytics geçişi reporter üstüne taşındı.
- `audio_service_lifecycle_initializer` non-fatal reporting reporter ile tutarlı hale getirildi.
- `parent_mode_view` içindeki günlük içerik hata raporları scoped event mantığına geçirildi.

## Doğrulama

- Tam test seti geçti: `flutter test`
- Hedeflenen çekirdek dosyalarda analyzer temiz geçti:
  - `lib/common/services/startup_deferred_task_coordinator.dart`
  - `lib/common/services/shared_preference_service.dart`
  - `lib/common/services/firebase_service.dart`
  - `lib/common/services/premium_status_recovery_service.dart`
  - `lib/features/stories/presentation/pages/story_content.dart`
- Eklenen veya genişletilen testler:
  - `test/common/services/crashlytics_reporter_test.dart`
  - `test/common/services/startup_deferred_task_coordinator_test.dart`
  - `test/common/services/audio_command_queue_test.dart`
  - `test/common/services/revenuecat_attribute_sync_queue_test.dart`
  - `test/features/auth/presentation/pages/onboarding_test.dart`

## Release Sonrası İzleme Notları

- Crashlytics’te onboarding back fatal kümesi düşmeli.
- RevenueCat `not_configured`, timeout ve readiness event’leri fatal yerine retryable/non-fatal gözükmeli.
- Story voice playback kaynaklı method-channel ve state yarışları düşmeli.
- Realtime Database init-order kaynaklı issue’lar kaybolmalı.
- Overflow cluster’ı fatal olarak görünmemeli; ekran bazlı UI fix gerekiyorsa ayrı patch açılmalı.
