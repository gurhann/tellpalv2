# TellPal 1.0.33 -> 1.0.34 Crashlytics Karşılaştırma Raporu

Analiz tarihi: 2026-04-14

## Kapsam

Bu rapor şu kaynaklara dayanır:

- `docs/1.0.33_crashlytcs/crashlytics_detailed_report_1.0.33.md`
- `docs/1.0.33_crashlytcs/crashlytics_1.0.33_lineage_review.md`
- `docs/1.0.33_crashlytcs/implementation_report_1.0.33.md`
- `docs/1.0.34_crashlytcs/crashlytics_detailed_report_1.0.34.md`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_1.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_2.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_3.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_4.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_5.csv`
- `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_ios.csv`
- Mevcut repo kodu

## Kanıt Kalitesi ve Sınırlar

- Observed:
  - `1.0.33` için daha geniş bir analiz ve issue hacim bilgisi var.
  - `1.0.34` için ise elimizde tam export değil, seçilmiş 6 örnek event bulunuyor.
- Strong inference:
  - Bu yüzden tek bir “toplam crash yüzde kaç düştü” rakamı üretmek teknik olarak güvenilir olmaz.
- Bu raporun ana hedefi:
  - hangi problem ailelerinde gerçek ilerleme olduğunu göstermek
  - hangi alanlarda iyileştirmenin daha çok telemetry/hardening seviyesinde kaldığını ayırmak
  - `1.0.33`’te yapılan işlerin neden bazı alanlarda kullanıcı etkisini belirgin biçimde azaltmadığını açıklamak

## Yönetici Özeti

- `1.0.33` döneminde üç büyük problem vardı:
  - telemetry kirlenmesi
  - startup + RevenueCat orchestration gürültüsü
  - birkaç net fatal bug kümesi
- `1.0.34` örnek exportlerinde net görülen ilerleme:
  - telemetry çok daha temiz
  - onboarding `pop` fatal’i görünmüyor
  - RTDB `setPersistenceEnabled()` fatal’i görünmüyor
  - audio crash aileleri bu örneklerde görünmüyor
- `1.0.34` örnek exportlerinde net görülen yetersizlik:
  - splash sonrası `setUserType()` / RevenueCat zinciri hâlâ kritik sorun ailesi
  - RevenueCat queue gürültüsü devam ediyor
  - yeni bir iOS fatal storage/download concurrency bug’ı ortaya çıkmış

Kısa karar:

- Observed:
  - `1.0.33`’te yapılan işler eski bazı fatal kümeleri bastırmış veya temizlemiş görünüyor.
- Strong inference:
  - Ancak bu işlerin önemli bölümü “kök nedeni kaldırma” değil “gözlemlenebilirliği iyileştirme ve belirli crash’leri guard ile bastırma” seviyesinde kaldı.
- Strong inference:
  - Bu yüzden `1.0.34`’te sistem daha iyi rapor veriyor, ama startup/RevenueCat hattı hâlâ aşırı ağır ve yarışa açık.

## Tarih Penceresi

- `1.0.33` analizinde kullanılan olay penceresi:
  - yaklaşık `2 Mart 2026 - 19 Mart 2026`
- `1.0.33` hardening uygulama raporu tarihi:
  - `21 Mart 2026`
- `1.0.34` eldeki export örneklerinin olay tarihi:
  - `8 Nisan 2026 - 12 Nisan 2026`

Yorum:

- Observed:
  - `1.0.33` düzeltmelerinden sonra en az yaklaşık 2-3 haftalık bir üretim gözlem penceresi oluşmuş.
- Strong inference:
  - Yani `1.0.34` örnekleri, `1.0.33` hardening işlerinin sahadaki ilk etkisini okumak için anlamlı bir pencere.

## Aile Bazında Delta Analizi

### 1. Telemetry context contamination

`1.0.33` durumu:

- `lib/main.dart` ve `lib/features/auth/presentation/pages/splash_screen.dart` içinde ayrı `FlutterError.onError` akışları vardı.
- Custom key taşması yüzünden event reason ve issue id’ler birbirine karışıyordu.
- Bu yüzden hangi cluster gerçekten yeni, hangisi yanlış etiketlenmiş net değildi.

`1.0.34` durumu:

- Observed:
  - Mevcut kodda `FlutterError.onError` tek noktada `lib/main.dart:172` civarında tanımlı.
  - `CrashlyticsReporter` event-scoped metadata set/reset yapıyor: `lib/common/services/crashlytics_reporter.dart:76`, `:121`.
  - `1.0.34` export örneklerinde RevenueCat event’leri artık:
    - `revenuecat_failure_type`
    - `revenuecat_retry_count`
    - `revenuecat_sync_source`
    - `configured_state`
    gibi anlamlı bağlamlarla geliyor.
- Strong inference:
  - Bu alan ciddi biçimde iyileşmiş.

İlerleme değerlendirmesi:

- Güvenli yorum:
  - yüksek ilerleme
- Yaklaşık etki bandı:
  - `%70-%90`

Neden etkili oldu:

- Çünkü bu iş doğrudan teknik borcun kendisine vurdu:
  - tek handler
  - scoped metadata
  - reset stratejisi

Neden sınırlı kaldı:

- Bu iyileştirme analiz kalitesini artırdı.
- Ama ürün akışındaki ağır startup/revenuecat zincirini hafifletmedi.

### 2. Splash deferred startup timeout

`1.0.33` durumu:

- En büyük küme:
  - `_SplashScreenViewState._syncUserTypeAndRevenueInBackground`
  - `119 event`
- Kök sorun:
  - splash içinde fazla işin tek zincire bindirilmesi

`1.0.34` durumu:

- Observed:
  - Aynı aile yeni isimle devam ediyor:
    - `StartupDeferredTaskCoordinator._runTask`
    - `startup_set_user_type_failed`
  - Task adı artık net:
    - `startup_task_name = set_user_type_after_splash`
  - Kodda iş ayrıştırılmış:
    - `lib/features/auth/presentation/pages/splash_screen.dart:111`
    - `lib/features/auth/presentation/pages/splash_screen.dart:174`
- Ama aynı task içinde hâlâ ağır zincir var:
  - `setUserType()`
  - `synchronizeRevenueCatState()`
  - `checkPurchaseConfig()`
  - `synchronizeRevenueCatIdentity()`
  - `getCustomerInfo` / premium fallback
  - user type apply
  - referanslar:
    - `lib/common/services/set_user_type_information_service.dart:197`
    - `:245`
    - `:264`
    - `:271`
    - `:274`

İlerleme değerlendirmesi:

- Güvenli yorum:
  - düşük-orta ilerleme
- Yaklaşık etki bandı:
  - `%20-%35`

Neden `1.0.33` çözümü tam etki bulmadı:

- Observed:
  - Startup işi artık “tek giant splash function” yerine task olarak ölçülüyor.
- Strong inference:
  - Ama task içine gömülen işin kendisi küçülmedi.
- Strong inference:
  - Yani problem çözümlenmedi, sadece daha iyi isimlendirilip sınırlandı.
- Strong inference:
  - Bu bir observability win, ama critical-path win değil.

Asıl eksik:

- `set_user_type_after_splash` hâlâ startup sonrası ağır bir RevenueCat state machine’i 6 saniyeye sığdırmaya çalışıyor:
  - `lib/features/auth/presentation/pages/splash_screen.dart:185`

### 3. RevenueCat 3-day paywall sync queue

`1.0.33` durumu:

- Büyük hacimli cluster:
  - `SharedPreferenceService._performSpecial3DayPaywallSync`
  - `86 event`
- Sorun:
  - timeout, `not_configured`, network ve readiness aynı torbada ele alınıyordu

`1.0.34` durumu:

- Observed:
  - Queue hâlâ event üretiyor.
  - Ama artık event daha doğru sınıflanıyor:
    - `reason = revenuecat_sync_attributes_retryable_failure`
    - `error_code = 10`
    - `configured_state = configured`
    - `retry_source = retry:save_or_update_3_day_paywall_attribute`
  - Sync artık enqueue modeliyle çalışıyor:
    - `lib/common/services/shared_preference_service.dart:224`
  - Queue retry politikası açık:
    - `lib/common/services/revenuecat_attribute_sync_queue.dart:137`

İlerleme değerlendirmesi:

- Güvenli yorum:
  - telemetry ve sınıflandırma alanında orta-yüksek ilerleme
  - gerçek stability alanında düşük-orta ilerleme
- Yaklaşık etki bandı:
  - telemetry açısından `%60-%80`
  - kullanıcı etkisi açısından `%25-%40`

Neden `1.0.33` çözümü tam etki bulmadı:

- Observed:
  - Queue mimarisi iyileşti.
- Strong inference:
  - Ama queue’nun yaptığı asıl iş hâlâ aynı dış sisteme gidiyor:
    - `Purchases.setAttributes()`
    - `Purchases.syncAttributesAndOfferingsIfNeeded()`
    - `lib/common/services/shared_preference_service.dart:302`
- Strong inference:
  - Yani semptomların sınıflandırması düzeldi, fakat dış bağımlılık ve zamanlama baskısı kaldı.
- Strong inference:
  - Bu nedenle “noise daha anlamlı oldu”, ama “noise tamamen kayboldu” diyemiyoruz.

### 4. RevenueCat identity / configure orchestration

`1.0.33` hedefi:

- single-flight mantığı
- duplicate identity/configure akışlarını azaltmak

`1.0.34` sahadaki durum:

- Observed:
  - `SetUserTypeInformationService` içinde gerçekten single-flight guard’lar var:
    - `_isConfiguring`
    - `_isIdentitySyncInProgress`
    - `_isRevenueCatStateSyncInProgress`
    - referans: `lib/common/services/set_user_type_information_service.dart:60`, `:62`, `:64`
- Ama aynı anda ikinci startup entrypoint de var:
  - `main()` içinde `unawaited(_initializeRevenueCat())`
  - `lib/main.dart:133-134`
  - bu da `checkPurchaseConfig()` çağırıyor:
    - `lib/main.dart:64`
- Splash sonrası ayrı giriş daha var:
  - `set_user_type_after_splash`
  - `lib/features/auth/presentation/pages/splash_screen.dart:174`

İlerleme değerlendirmesi:

- Güvenli yorum:
  - kavramsal iyileştirme var
  - global startup koordinasyonu eksik
- Yaklaşık etki bandı:
  - `%25-%45`

Neden `1.0.33` çözümü tam etki bulmadı:

- Strong inference:
  - Single-flight yalnızca service içindeki belirli akışları birleştiriyor.
- Strong inference:
  - Ama RevenueCat için hâlâ birden fazla üst-seviye giriş noktası var.
- Observed:
  - `1.0.34` ANR ve timeout örneklerinde birden fazla `Fetching purchase config` log’u görülüyor.
  - log kaynağı:
    - `lib/common/services/set_user_type_information_service.dart:1235`
- Strong inference:
  - Bu, “aynı subsystem daha kontrollü” demek; “aynı subsystem tek kez tetikleniyor” demek değil.

Bu yüzden:

- `1.0.33`’te duplicate path’ler azaldı
- ama tam merkezi orchestrator kurulmadığı için concurrency baskısı bitmedi

### 5. Onboarding `There is nothing to pop` fatal

`1.0.33` durumu:

- net ürün etkili fatal bug
- `13 fatal event`

`1.0.34` durumu:

- Observed:
  - Kod fix’i yerinde:
    - `navigateOnboardingBack()` artık `context.canPop()` guard kullanıyor
    - referans: `lib/features/auth/presentation/pages/onboarding.dart:16`
  - `1.0.34` export örneklerinde bu issue görünmüyor.

İlerleme değerlendirmesi:

- Güvenli yorum:
  - yüksek olasılıkla çözüldü
- Yaklaşık etki bandı:
  - `%90-%100`

Neden etkili oldu:

- Çünkü bu lokal ve deterministik bir bug’dı.
- Guard eklemek doğrudan kök nedeni ortadan kaldırdı.

### 6. Firebase Realtime Database `setPersistenceEnabled()` sırası

`1.0.33` durumu:

- net init-order fatal
- Android tarafında küçük ama keskin bir cluster

`1.0.34` durumu:

- Observed:
  - DI bootstrap artık doğru sırada:
    - `FirebaseDatabase firebaseDatabase = FirebaseDatabase.instance;`
    - `firebaseDatabase.setPersistenceEnabled(true);`
    - sonra singleton registration
    - referans: `lib/injection_container.dart:82-84`
  - `1.0.34` örnek exportlerinde bu issue görünmüyor.

İlerleme değerlendirmesi:

- Güvenli yorum:
  - yüksek olasılıkla çözüldü
- Yaklaşık etki bandı:
  - `%90-%100`

Neden etkili oldu:

- Çünkü bu da lokal ve sıralama tabanlı bir bug’dı.
- Bootstrap sırası doğrudan düzeltildi.

### 7. Audio lifecycle / player komut yarışı

`1.0.33` durumu:

- iOS `MethodChannelAudioPlayer.seek`
- Android `checkInterruption`
- net fatal cluster’lar

`1.0.34` durumu:

- Observed:
  - Kodda komut serializasyonu var:
    - `AudioCommandQueue`
    - `PlayerManager` içinde queue kullanımı
    - referans:
      - `lib/common/services/audio_command_queue.dart`
      - `lib/common/services/player_manager_service.dart:29`
      - `:150`
  - `1.0.34` örnek exportlerinde audio fatal ailesi görünmüyor.

İlerleme değerlendirmesi:

- Güvenli yorum:
  - muhtemel orta-yüksek ilerleme
- Yaklaşık etki bandı:
  - `%50-%80`

Neden kesin konuşamıyoruz:

- `1.0.34` tam export yok.
- Elimizdeki 6 örnek event içinde audio olmaması, hacmin sıfırlandığını kesin kanıtlamaz.

### 8. Splash storage / network / image noise

`1.0.33` durumu:

- splash altında beklenen ağ hataları da Crashlytics’e yoğun gürültü üretiyordu

`1.0.34` durumu:

- Observed:
  - `CrashEventDeduper` içinde storage host lookup ve stream disposed için dedupe var:
    - `lib/common/services/crash_event_deduper.dart:18`
    - `:58`
  - `1.0.34` örnek exportlerinde eski splash image/network gürültüsü baskın görünmüyor.

İlerleme değerlendirmesi:

- Güvenli yorum:
  - orta ilerleme
- Yaklaşık etki bandı:
  - `%40-%70`

Neden sınırlı:

- Bu alan daha çok noise-control seviyesinde iyileşti.
- Ağ bağımlılığı kökten kalkmadı.

### 9. Story asset / storage alanı

`1.0.33` hedefi:

- transactionality
- single-flight story writes
- missing-image noise azaltma

`1.0.34` durumu:

- Observed:
  - story write lock altyapısı var:
    - `lib/common/services/firebase_service.dart:1184`
  - missing image recovery tarafı da lock üzerinden gidiyor:
    - `:1513`
  - Buna rağmen `1.0.34` iOS fatal’i yine storage/download hattından geliyor:
    - `_downloadFileOnMainThread.<fn>`
    - `Bad state: Future already completed`

İlerleme değerlendirmesi:

- Güvenli yorum:
  - kısmi ilerleme + yeni regresyon
- Yaklaşık etki bandı:
  - eski read/write yarışları açısından `%30-%50`
  - ama yeni concurrency bug yüzünden net kullanıcı etkisi nötr hatta bazı senaryolarda negatif olabilir

Neden `1.0.33` çözümü tam etki bulmadı:

- Strong inference:
  - Transactionality geliştikçe download/extraction path’i daha karmaşık hale geldi.
- Observed:
  - `_downloadFileOnMainThread()` artık:
    - progress stream
    - completer
    - timeout
    - cancel
    - isolate fallback
    kombinasyonuyla çalışıyor:
    - `lib/common/services/firebase_service.dart:3368`
    - `:3407`
    - `:3421`
    - `:3429`
- Strong inference:
  - Yani eski race’leri azaltırken yeni bir completion race üretilmiş.

## Neden 1.0.33’te Çözülen Şeyler 1.0.34’te Beklenen Kadar Etki Göstermedi?

### 1. Bazı işler kök neden değil, gözlemlenebilirliği düzeltti

- Telemetry fix’leri çok değerliydi.
- Ama bu fix’ler startup yükünü, RevenueCat round-trip’lerini veya ağ çağrılarını azaltmadı.

Sonuç:

- Crash raporu daha dürüst hale geldi.
- Ama kullanıcıya çarpan orchestration maliyeti aynı kaldı.

### 2. Splash zinciri parçalandı, fakat ağır iş aynı task’in içinde kaldı

- `1.0.33` düşüncesi doğruydu:
  - splash’i task’lere böl
- Eksik kalan kısım:
  - `set_user_type_after_splash` task’i hâlâ kendi içinde mini bir monolith

Sonuç:

- `1.0.34`’te artık hangi task patlıyor biliyoruz
- ama task’in patlamasına yol açan ağır RevenueCat zinciri değişmedi

### 3. RevenueCat için single-flight var, ama global startup ownership yok

- Service içinde guard var
- Fakat sistem seviyesinde hâlâ paralel girişler var:
  - `main()` bootstrap
  - splash deferred task
  - paywall attribute queue

Sonuç:

- duplicate work azalmış olabilir
- ama startup baskısı ve config fetch tekrarı bitmemiş

### 4. Queue/readiness iyileşti, fakat dış bağımlılık aynı kaldı

- `1.0.33`’te retry classification iyileşmiş
- Fakat RevenueCat yine aynı network ve SDK readiness’e bağlı

Sonuç:

- “yanlış sınıflanan hata” azaldı
- “gerçek timeout / gerçek network / gerçek readiness problemi” sürüyor

### 5. Bazı lokal bug’lar gerçekten çözüldü, ama bunlar toplam riskin sadece bir kısmıydı

- onboarding pop
- RTDB init order
- muhtemelen audio komut yarışı

Bu fix’ler iyi ve gerekliydi.

Ama `1.0.33`’te en büyük hacimli problem aileleri bunlar değildi.

En büyük hacimli aileler şunlardı:

- startup orchestration
- RevenueCat sync/configure/identity
- lifecycle ve ağ readiness kırılganlığı

Bu yüzden lokal fix’ler sahada hissedildi, ama genel “uygulama başlangıç stabilitesi” algısını kökten değiştirmedi.

### 6. Story/storage hardening yeni bir yan etki üretmiş olabilir

- Observed:
  - `1.0.34`’te en ciddi yeni bulgu storage download completer race
- Strong inference:
  - Bu, hardening sırasında eklenen fallback/cancel/timeout mekaniklerinin yeni bir hata yüzeyi açtığını gösteriyor

Sonuç:

- bazı eski dosya sistemi riskleri azalırken
- yeni bir concurrency regression doğmuş

## Genel İlerleme Tahmini

Tek bir sayı vermek güvenilir değil. Yine de aile bazlı ilerlemeyi ağırlıklı okuyunca en makul tablo şu:

- Observed:
  - telemetry kalitesi belirgin biçimde arttı
  - eski birkaç keskin fatal cluster görünmez oldu
- Strong inference:
  - startup/RevenueCat reliability alanında ilerleme sınırlı kaldı
- Strong inference:
  - yeni iOS fatal regression bu kazanımların bir kısmını geri aldı

Bu nedenle genel tabloyu şöyle özetlemek daha doğru:

- Eski belirgin fatal bug kümeleri açısından:
  - orta-yüksek ilerleme
- Startup + RevenueCat ana reliability hattı açısından:
  - düşük-orta ilerleme
- Net kullanıcı etkisi açısından:
  - orta seviyede ilerleme, fakat homojen değil

Konservatif genel bant:

- `%35-%55` arası gerçek ilerleme

Daha agresif ama yine savunulabilir yorum:

- Eğer onboarding, RTDB ve audio cluster’ları gerçekten kaybolduysa:
  - `%50-%65` arası

Ama:

- `1.0.34` tam export olmadan bundan daha yüksek bir başarı oranı söylemek teknik olarak savunulabilir değil.

## En Kritik Sonuçlar

1. `1.0.33`’te yapılan telemetry ve lokal fatal fix’ler değerli ve büyük ölçüde doğru yönde.
2. `1.0.34`’te startup/RevenueCat hattı hâlâ en büyük sistemik zayıflık.
3. RevenueCat için “single-flight” var ama “single owner startup orchestrator” yok.
4. Story/storage tarafında eski yarışlar kısmen bastırılırken yeni bir iOS concurrency regression ortaya çıkmış.
5. Bir sonraki büyük kazanım telemetry değil, startup kritik yolunu gerçekten hafifletmekten gelecek.

## Sonraki Adım Önerisi

### P0

- `firebase_service.dart` içindeki iOS fatal `Completer` yarışını düzelt

### P1

- RevenueCat için tek üst-seviye startup entrypoint bırak
- `main()` bootstrap ile splash task’lerinden birini kaldır veya merkezi orkestratöre bağla

### P2

- `setUserType()` içini alt safhalara böl
- timeout’u tüm zincire değil, her alt adıma ayrı uygula

### P3

- `1.0.34` için tam Crashlytics export al
- özellikle audio ve onboarding cluster’larının gerçekten sıfırlanıp sıfırlanmadığını doğrula
