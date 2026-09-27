# TellPal 1.0.34 Crashlytics Detaylı Analiz Raporu

Analiz tarihi: 2026-04-14

Kapsam:
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_1.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_2.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_3.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_4.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_5.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_ios.csv`
- İlgili uygulama kodu ve Crashlytics entegrasyon noktaları

Kanıt kalitesi:
- Doğrudan Crashlytics CSV export verisi kullanıldı.
- Ancak bu export seti tam event dump değil, seçilmiş 6 örnek event içeriyor.
- Bu nedenle kök neden analizi güçlü, prevalans analizi sınırlı.

## Yönetici Özeti

- `1.0.34 (332)` içinde iki ana problem ailesi öne çıkıyor:
  - Android startup/RevenueCat kaynaklı timeout ve buna bağlı performans baskısı
  - iOS tarafında `firebase_service.dart` içinde fatal bir `Completer` yarış durumu
- En kritik bulgu iOS fatal crash:
  - `FirebaseService._downloadFileOnMainThread.<fn>`
  - `Bad state: Future already completed`
  - Bu gerçek bir kod hatası ve doğrudan uygulama içi concurrency bug'ına işaret ediyor.
- Android tarafındaki non-fatal kümeler aynı altyapı hattına bağlanıyor:
  - splash sonrası `setUserType()`
  - RevenueCat configure / identity / sync zinciri
  - kısa timeout pencereleri
  - foreground/background geçişi sırasında başlatılan işler
- Android ANR örneği doğrudan tek bir frame ile açıklanamıyor, ancak eldeki log ve breadcrumb'lar RevenueCat/paywall/lifecycle baskısını ana aday yapıyor.
- Önceki `1.0.33` raporunda baskın olan startup + RevenueCat gürültüsü `1.0.34` içinde hâlâ sürüyor. Buna ek olarak iOS tarafında yeni ve daha keskin bir fatal download yarış problemi görünür hale gelmiş.

## Genel Olay Tablosu

İncelenen export örnekleri:
- Android ANR: 1 örnek
- Android non-fatal startup timeout: 3 örnek
- Android non-fatal RevenueCat sync hatası: 1 örnek
- iOS fatal storage download crash: 1 örnek

Gözlenen issue kümeleri:
- `6bba884050263fdff281a8bc49a85b9b`
  - Android
  - ANR
  - `android.os.MessageQueue.nativePollOnce`
- `ab319f7ff5aefb09e68d082aea99d3a7`
  - Android
  - non-fatal
  - `startup_set_user_type_failed`
- `4794f093726edf9ab0a6feb570c7d3e3`
  - Android
  - non-fatal
  - `madde_13_revenuecat_sync_queue_guard_v1`
- `ee27ad1fcb013499eed34b11b4405ee0`
  - iOS
  - fatal
  - `FirebaseService._downloadFileOnMainThread.<fn>`

## Telemetry ve Sınırlamalar

- Observed: Mevcut kodda `FlutterError.onError` yalnızca `lib/main.dart` içinde tanımlı görünüyor.
- Observed: Bu repo snapshot'ında ikinci bir `FlutterError.onError` override'ı bulunmadı.
- Strong inference: `1.0.33` raporundaki çift handler problemi bu branch'te giderilmiş olabilir ya da o rapor eski kod snapshot'ına dayanıyor olabilir.
- Observed: `CrashlyticsReporter` scoped metadata set edip event sonrası resetliyor.
- Strong inference: `issue_id` ve `issue_fix_version` sızıntısı riski önceki sürüme göre azaltılmış.
- Weak inference: Yine de event export seti sınırlı olduğu için tüm issue ailelerinde telemetry temizliği tamamlandı demek için daha büyük export gerekir.

Kod referansları:
- `lib/main.dart:172`
- `lib/common/services/crashlytics_reporter.dart:76`
- `lib/common/services/crashlytics_reporter.dart:121`

## Incident 1

- Title: `iOS fatal - Firebase Storage ana thread download completer yarış durumu`
- Severity: yüksek
- Scope: iOS foreground story açma / story asset indirme akışı
- Evidence quality: doğrudan Crashlytics + kod korelasyonu

### Observed Evidence

- Crashlytics issue title:
  - `package:interactive_short_kid_stories_tellpal/common/services/firebase_service.dart - FirebaseService._downloadFileOnMainThread.<fn>`
- Platform split:
  - iOS
- Event type:
  - fatal
- Issue id / fix version:
  - `issue_id=unclassified`
  - `issue_fix_version=none`
- Reason:
  - `FlutterError - Bad state: Future already completed`
- Build / app version:
  - `1.0.34 (332)`
- Device / OS:
  - `iPhone12,1`
  - `iOS 26.4.0 (23E246)`
- Process state:
  - `FOREGROUND`
- Top stack frames:
  - `dart:async _AsyncCompleter.complete`
  - `lib/common/services/firebase_service.dart:3421`
- Breadcrumbs:
  - kullanıcı ana tabı açmış
  - `story_search` yapılmış
  - `clicked_story`
  - `opened_story`
  - crash story açılışından hemen sonra olmuş

### Android Analysis

- Observed:
  - Bu issue için Android export örneği yok.
- Strong inference:
  - Kod yolu platformdan bağımsız Dart tarafında yazılmış olduğu için Android'de de teorik olarak mümkün.
- Weak inference:
  - Ancak mevcut export setinde Android karşılığı görülmediği için iOS-first davranış olabilir.
- Most likely Android-specific trigger:
  - Firebase Storage stream callback sıralaması + timeout/cancel yarışı olursa benzer sonuç üretilebilir.
- Files / modules to inspect:
  - `lib/common/services/firebase_service.dart:3404`
  - `lib/common/services/firebase_service.dart:3421`

### iOS Analysis

- Observed:
  - `snapshotEvents.listen` içinde aynı `Completer<void>` için:
    - `onDone: () => completer.complete()`
    - `onError: (error) => completer.completeError(error)`
  - Timeout kolunda da `progressSubscription?.cancel()` ve `downloadTask.cancel()` çağrılıyor.
  - Crash tam `completer.complete()` satırında oluşmuş.
- Strong inference:
  - Stream callback, cancel/timeout veya terminal event kombinasyonu sonucu `Completer` ikinci kez tamamlanmış.
  - Özellikle Firebase Storage task lifecycle'ında cancel/error/done sinyallerinin kısa aralıkla gelmesi bu patterne uyar.
- Weak inference:
  - iOS plugin/event ordering Android'e göre daha farklı çalıştığı için yarış burada daha görünür olabilir.
- Most likely iOS-specific trigger:
  - Story açılırken başlayan storage download task'ında terminal event'lerin çift gelmesi ya da cancel sonrası `onDone` akması.
- Files / modules to inspect:
  - `lib/common/services/firebase_service.dart:3368`
  - `lib/common/services/firebase_service.dart:3407`
  - `lib/common/services/firebase_service.dart:3421`
  - `lib/common/services/firebase_service.dart:3429`

### Root Cause Ranking

1. `Completer` birden fazla kez tamamlanıyor, güven: yüksek
2. Timeout/cancel sonrası stream terminal event'inin yeniden akması, güven: orta-yüksek
3. Firebase Storage plugin callback sıralamasının iOS'ta farklı davranması, güven: orta

### Recommended Fixes

- Immediate mitigation:
  - `completer.complete()` ve `completeError()` öncesinde `if (!completer.isCompleted)` guard eklenmeli.
- Corrective code change:
  - `onDone` ve `onError` callback'leri idempotent hale getirilmeli.
  - Timeout kolunda task iptalinden sonra aynı future'a ikinci terminal sinyalin gelmesi güvenli karşılanmalı.
  - İmkansa `await downloadTask.whenComplete(...)` veya task result odaklı daha tekil bir completion modeli tercih edilmeli.
- Telemetry improvement:
  - `storage_path`, `download_scope`, `isolate_fallback`, `timeout_reached`, `task_cancelled` custom key'leri eklenmeli.
- Validation plan:
  - iOS cihazda story açılışında indirme başlayan akışlar hızlı tekrarlarla test edilmeli.
  - Ağ kesme/geri verme, uygulamayı background/foreground yapma ve timeout senaryoları denenmeli.

## Incident 2

- Title: `Android startup setUserType timeout zinciri`
- Severity: orta
- Scope: Android splash sonrası premium/user type senkronizasyonu
- Evidence quality: doğrudan Crashlytics + kod korelasyonu

### Observed Evidence

- Crashlytics issue title:
  - `StartupDeferredTaskCoordinator._runTask`
- Platform split:
  - Android
- Event type:
  - non-fatal
- Issue id / fix version:
  - export issue id: `ab319f7ff5aefb09e68d082aea99d3a7`
  - custom fix marker yok
- Reason:
  - `startup_set_user_type_failed`
- Build / app version:
  - `1.0.34 (332)`
- Devices:
  - `DREAMTECH VOLTA X`
  - `incar OT6`
  - `Infinix X6525D`
- OS:
  - Android `13` ve `14`
- Custom keys:
  - `startup_task_name=set_user_type_after_splash`
  - `startup_requires_foreground=true`
  - `startup_task_app_foreground=true`
- Top stack frames:
  - `dart:async Future.timeout.<fn>`
  - `lib/common/services/startup_deferred_task_coordinator.dart:51`

### Android Analysis

- Observed:
  - Splash sonrası `set_user_type_after_splash` görevi 6 saniye timeout ile çalıştırılıyor.
  - Task `requiresForeground=true`.
  - Bu task içinde `setUserType()` çağrılıyor.
  - `setUserType()` da `synchronizeRevenueCatState(source: 'set_user_type')` çağırıyor.
  - `synchronizeRevenueCatState()` içinde:
    - `checkPurchaseConfig()`
    - `synchronizeRevenueCatIdentity()`
    - `getCustomerInfo` / premium fallback
    - user type set etme
    zinciri tek akışta koşuyor.
- Strong inference:
  - 6 saniyelik timeout bu zincir için agresif.
  - Özellikle düşük seviye Android cihazlarda auth readiness + RevenueCat configure + network + identity geçişi birlikte bu süreyi aşıyor.
- Strong inference:
  - `requiresForeground` kontrolü task başlamadan önce yapılıyor; task başladıktan sonra lifecycle değişirse iş yine devam ediyor.
- Weak inference:
  - `process_state=BACKGROUND` görülen örnek, event kaydı sırasında lifecycle state ile app process state'in ayrıştığını gösteriyor olabilir.
- Most likely Android-specific trigger:
  - Splash akışında foreground sandığımız anda başlayan RevenueCat senkronu, kullanıcı arka plana geçince veya ekran değiştirilince tamamlanamıyor.
- Files / modules to inspect:
  - `lib/features/auth/presentation/pages/splash_screen.dart:174`
  - `lib/common/services/startup_deferred_task_coordinator.dart:42`
  - `lib/common/services/set_user_type_information_service.dart:197`
  - `lib/common/services/set_user_type_information_service.dart:245`

### iOS Analysis

- Observed:
  - Bu export setinde aynı issue'nin iOS örneği yok.
- Strong inference:
  - Kök orchestration Dart tarafında ortak olsa da bu sürümde görünür baskı Android tarafında.
- Weak inference:
  - iOS'ta aynı akış varsa daha düşük hacimde ya da farklı issue title altında toplanıyor olabilir.
- Most likely iOS-specific trigger:
  - varsa cold start + RevenueCat configure yarışları.
- Files / modules to inspect:
  - aynı orchestration dosyaları

### Root Cause Ranking

1. Startup içinde fazla işin tek timeout penceresine bağlanması, güven: yüksek
2. RevenueCat configure/identity/customerInfo zincirinin düşük cihazlarda geç tamamlanması, güven: yüksek
3. Lifecycle değişimi sonrası yine koşmaya devam eden task tasarımı, güven: orta

### Recommended Fixes

- Immediate mitigation:
  - `set_user_type_after_splash` timeout'u artırılmalı veya bu iş splash kritik yolundan çıkarılmalı.
- Corrective code change:
  - `setUserType()` zinciri ayrı alt görevlere bölünmeli:
    - configure readiness
    - identity sync
    - premium fetch
    - user type apply
  - foreground gereksinimi task başladıktan sonra da tekrar kontrol edilmeli.
- Telemetry improvement:
  - `revenuecat_step`, `configured`, `identity_synced`, `customer_info_source`, `lifecycle_state_at_timeout` eklenmeli.
- Validation plan:
  - düşük RAM Android cihazlarda cold start, paywall gösterimi, splash -> background -> foreground akışları test edilmeli.

## Incident 3

- Title: `Android RevenueCat attribute sync ağ/timeout gürültüsü`
- Severity: orta-düşük
- Scope: Android 3-day paywall attribute sync kuyruğu
- Evidence quality: doğrudan Crashlytics + kod korelasyonu

### Observed Evidence

- Crashlytics issue title:
  - `Purchases.syncAttributesAndOfferingsIfNeeded`
- Platform split:
  - Android
- Event type:
  - non-fatal
- Issue id / fix version:
  - `4794f093726edf9ab0a6feb570c7d3e3`
  - `madde_13_revenuecat_sync_queue_guard_v1`
- Reason:
  - `revenuecat_sync_attributes_retryable_failure`
- Exception:
  - `PlatformException(10, ... readableErrorCode: NetworkError ... underlyingErrorMessage: Connection reset)`
- Custom keys:
  - `revenuecat_configured_state=configured`
  - `revenuecat_failure_type=retryable`
  - `revenuecat_operation=sync_attributes_and_offerings`
  - `revenuecat_retry_count=1`
  - `revenuecat_sync_source=retry:save_or_update_3_day_paywall_attribute`
  - `revenuecat_app_foreground=true`

### Android Analysis

- Observed:
  - Hata `SharedPreferenceService._performSpecial3DayPaywallSync()` içinden geliyor.
  - Akış:
    - `Purchases.setAttributes()` timeout 4s
    - `Purchases.syncAttributesAndOfferingsIfNeeded()` timeout 4s
  - Queue retry mantığı mevcut ve event retry kaynağıyla raporlanmış.
- Strong inference:
  - Bu issue tek başına crash değil; retryable transport failure.
  - Ancak startup ve paywall zamanlamasına yakın çalıştığı için diğer startup gecikmelerini besliyor.
- Strong inference:
  - `Connection reset` gerçek ağ katmanı / RevenueCat API erişim sorunu; yerel data corruption değil.
- Weak inference:
  - Aynı anda çalışan `setUserType()` zinciri ve `Fetching purchase config` logları toplam baskıyı artırıyor olabilir.
- Most likely Android-specific trigger:
  - Zayıf ağ + foreground geçişi + RevenueCat işlemlerinin splash sonrasına çok erken bind edilmesi.
- Files / modules to inspect:
  - `lib/common/services/shared_preference_service.dart:302`
  - `lib/common/services/revenuecat_attribute_sync_queue.dart:137`

### iOS Analysis

- Observed:
  - Bu export setinde iOS örneği yok.
- Strong inference:
  - Sorun platform özgü olmaktan çok ağ ve zamanlama kaynaklı.
- Weak inference:
  - iOS'ta aynı guard olduğu için daha düşük hacimde görülebilir.
- Most likely iOS-specific trigger:
  - app inactive/foreground boundary yakınında RevenueCat network call.
- Files / modules to inspect:
  - aynı RevenueCat queue akışı

### Root Cause Ranking

1. Gerçek ağ/transport hatası, güven: yüksek
2. RevenueCat sync'in lifecycle açısından fazla erken tetiklenmesi, güven: orta-yüksek
3. Retry'lerin kullanıcı akışına çok yakın çalışması, güven: orta

### Recommended Fixes

- Immediate mitigation:
  - Bu event ailesi fatal yapılmamalı; mevcut non-fatal yaklaşım doğru.
- Corrective code change:
  - Queue koşmadan önce daha sert foreground/readiness kontrolü eklenmeli.
  - İlk sync daha geç başlatılmalı veya splash sonrası sakin bir pencereye alınmalı.
- Telemetry improvement:
  - `network_type`, `app_lifecycle_state`, `cold_start_elapsed_ms` alanları eklenmeli.
- Validation plan:
  - Ağ paket kaybı ve connection reset simülasyonu ile retry davranışı test edilmeli.

## Incident 4

- Title: `Android ANR - RevenueCat/paywall/lifecycle baskısı altında ana thread duraksaması`
- Severity: orta-yüksek
- Scope: Android kullanıcı akışı, paywall açılışı civarı
- Evidence quality: doğrudan Crashlytics ANR örneği + sınırlı korelasyon

### Observed Evidence

- Crashlytics issue title:
  - `Native method - android.os.MessageQueue.nativePollOnce`
- Platform split:
  - Android
- Event type:
  - ANR
- Issue id / fix version:
  - export issue id: `6bba884050263fdff281a8bc49a85b9b`
  - custom issue marker yok
- Reason / custom context:
  - `flutter_error_reason = thrown revenuecat_sync_attributes_retryable_failure`
  - `flutter_error_exception = TimeoutException: syncAttributesAndOfferingsIfNeeded timeout`
- Process state:
  - `FOREGROUND`
- Breadcrumbs:
  - `landing_anonym_login`
  - `show_paywall`
  - screen `PaywallActivity`
- Logs:
  - birden çok `Fetching purchase config`
  - yoğun lifecycle geçişleri
  - ilk saniyelerde çok yüksek `jank_rate`

### Android Analysis

- Observed:
  - ANR blame frame yalnızca `nativePollOnce`; doğrudan uygulama frame'i göstermiyor.
  - Aynı event içinde RevenueCat timeout context'i taşınıyor.
  - Paywall açılışı ve purchase config fetch'leri birbirine çok yakın.
- Strong inference:
  - Bu ANR örneği büyük olasılıkla tek bir blocking call'dan çok startup/paywall çevresindeki toplam ana thread baskısının sonucu.
- Strong inference:
  - `frame_stats` içinde ilk saniyelerde çok yüksek jank oranı görülmesi, UI thread yüklenmesini destekliyor.
- Weak inference:
  - RevenueCat tek kök neden olmayabilir; audio session, analytics, paywall UI ve lifecycle transition toplamı etkili olabilir.
- Most likely Android-specific trigger:
  - anonymous login -> paywall açılışı -> purchase config / attribute sync / lifecycle event'leri aynı pencereye binince main thread responsiveness düşüyor.
- Files / modules to inspect:
  - `lib/main.dart:53`
  - `lib/features/auth/presentation/pages/splash_screen.dart:174`
  - `lib/common/services/shared_preference_service.dart:302`
  - `lib/common/services/set_user_type_information_service.dart:245`

### iOS Analysis

- Observed:
  - Bu issue için iOS export örneği yok.
- Strong inference:
  - ANR semantiği Android'e özgü.
- Weak inference:
  - iOS karşılığı olasıysa jank veya watchdog termination olarak farklı yüzeyde görünür.
- Most likely iOS-specific trigger:
  - yok
- Files / modules to inspect:
  - yok

### Root Cause Ranking

1. Startup/paywall anında fazla concurrent iş yükü, güven: orta-yüksek
2. RevenueCat timeout ve retry baskısının UI thread dolaylı etkisi, güven: orta
3. Audio/paywall/activity geçişlerinin ek maliyeti, güven: orta

### Recommended Fixes

- Immediate mitigation:
  - paywall açılışı anında kritik olmayan RevenueCat sync işleri ertelenmeli.
- Corrective code change:
  - startup ve paywall çevresindeki async işler throttling ile ayrıştırılmalı.
  - ANR monitor telemetry'sine aktif iş listesi eklenmeli.
- Telemetry improvement:
  - `active_startup_tasks`, `paywall_visible`, `revenuecat_request_in_flight`, `audio_service_state` alanları eklenmeli.
- Validation plan:
  - düşük uç Android cihazda anonymous login -> paywall akışı profile edilmeli.

## Android ve iOS Platform Sonuçları

### Android

- Observed:
  - Baskın profil startup ve RevenueCat çevresinde toplanıyor.
  - `startup_set_user_type_failed` örnekleri birden fazla cihaz sınıfında görülmüş.
  - RevenueCat attribute sync hatası doğru biçimde retryable olarak sınıflanmış.
- Strong inference:
  - Android'deki ana sorun ağdan çok orchestration ve zamanlama.
  - Ağ hataları mevcut ama asıl ürün etkisi bunların splash/paywall kritik yoluna fazla yakın olması.
- Weak inference:
  - Eğer tam export alınırsa aynı kök nedenden türeyen daha fazla timeout ve ANR örneği çıkacaktır.

### iOS

- Observed:
  - En kritik olay doğrudan Dart kodundaki `Completer` misuse.
  - Fatal crash story açma akışına yakın bir noktada oluşmuş.
- Strong inference:
  - Bu bug düzeltmeden iOS stabilitesi için anlamlı risk devam eder.
- Weak inference:
  - Aynı yarış Android'de de latent olabilir fakat şu an export setinde görünmüyor.

## Kök Neden Sıralaması

1. `firebase_service.dart` içinde idempotent olmayan download completion yönetimi
   - Güven: yüksek
   - Neden: crash satırı doğrudan `completer.complete()` ve exception `Future already completed`

2. Splash sonrası `setUserType()` işinin fazla geniş bir RevenueCat zincirini 6 saniyeye sığdırmaya çalışması
   - Güven: yüksek
   - Neden: Crashlytics custom key'leri ve kod akışı birebir örtüşüyor

3. RevenueCat attribute sync işlemlerinin kullanıcı akışına çok yakın ve lifecycle açısından erken tetiklenmesi
   - Güven: orta-yüksek
   - Neden: connection reset örneği, retry kaynağı ve ANR context'i aynı aileyi işaret ediyor

4. Android paywall/startup çevresindeki toplam concurrent iş yükünün ANR riskini yükseltmesi
   - Güven: orta
   - Neden: jank ve lifecycle logları bunu destekliyor, ama tek suçlu frame yok

## Önerilen Aksiyon Planı

### P0

- `firebase_service.dart` içindeki `Completer` kullanımını idempotent hale getir.
- iOS cihazda story açılışına özel manuel doğrulama yap.

### P1

- `set_user_type_after_splash` işini splash kritik yolundan çıkar veya timeout'u artır.
- RevenueCat state sync'i alt adımlara böl ve her adımı ayrı telemetry ile raporla.

### P2

- 3-day paywall attribute sync işini paywall görünürlüğü ve foreground sakinleşmesinden sonraya ertele.
- Startup/paywall anındaki concurrent görev sayısını azalt.

### P3

- Daha geniş `1.0.34` tam export al ve issue hacimlerini doğrula.
- Özellikle iOS fatal issue'nin kaç farklı cihazda tekrarlandığını kontrol et.

## Doğrulama Planı

- iOS
  - story search -> story açılışı akışını tekrar et
  - ağ kes / geri ver
  - app background / foreground yap
  - aynı story'yi hızlı art arda aç
- Android
  - cold start düşük cihaz
  - anonymous login -> paywall
  - zayıf ağ altında RevenueCat sync
  - splash sırasında arka plana atıp geri dönme

## Unknowns

- Missing artifacts:
  - Tam event listesi, issue sayıları, affected users, first seen / last seen bilgileri yok.
  - iOS fatal issue için ek event örneği yok.
  - Android ANR için full thread dump ve daha geniş ANR örnek kümesi yok.
- Assumptions:
  - CSV exportler seçilmiş örnek event'ler ve tüm hacmi temsil etmiyor.
  - Kod snapshot'ı export alınan binary ile yeterince uyumlu kabul edildi.
- What would change the conclusion:
  - Eğer tam export iOS fatal issue'nin tekil bir edge case olduğunu gösterirse öncelik düşer.
  - Eğer Android ANR'lerde RevenueCat dışında başka baskın thread pattern'leri çıkarsa ANR kök neden sıralaması değişir.
