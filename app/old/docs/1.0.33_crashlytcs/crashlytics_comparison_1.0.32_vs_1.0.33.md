# TellPal Crashlytics Karşılaştırma Raporu

Başlık: `1.0.32` ve `1.0.33` Crashlytics sorun profili karşılaştırması

Tarih: 2026-03-20

## Kapsam

- `1.0.32` kaynağı:
  - `docs/1.0.32_crashlytcs/crashlytics-sorun-analizi.md`
- `1.0.33` kaynakları:
  - `docs/1.0.33_crashlytcs/tellpal_ios_1.0.33_crashlyts_logs.csv`
  - `docs/1.0.33_crashlytcs/tellpal_android_1.0.33_crashlyts_logs.csv`
  - `docs/1.0.33_crashlytcs/crashlytics_detailed_report_1.0.33.md`

## Kanıt Kalitesi ve Sınırlamalar

- `1.0.33` için kanıt kalitesi yüksek:
  - ham Crashlytics CSV export verisi mevcut
  - event, fatality, issue title, custom key ve breadcrumb düzeyinde inceleme yapıldı
- `1.0.32` için kanıt kalitesi orta:
  - ham CSV dosyaları mevcut değil
  - karşılaştırma, daha önce hazırlanmış analiz raporuna dayanıyor
- Bu yüzden `1.0.33 -> yok` sonucu her zaman "kesin çözüldü" anlamına gelmez.
- En doğru yorum:
  - `1.0.33 penceresinde gözlenmedi`
  - `yüksek olasılıkla düzeldi veya belirgin biçimde azaldı`

## Yönetici Özeti

- `1.0.32`: 236 event, 29 fatal
- `1.0.33`: 280 event, 28 fatal
- Tarih penceresi aynı değil:
  - `1.0.32` genel pencere: 1 Ocak 2026 - 9 Şubat 2026
  - `1.0.33` genel pencere: 2 Mart 2026 - 19 Mart 2026
- Toplam fatal sayısı neredeyse aynı kaldı, ama hata profili ciddi biçimde değişti.
- `1.0.32` daha parçalı bir profil sergiliyor:
  - ANR gürültüsü
  - Google Fonts DNS hataları
  - dosya sistemi / görsel bütünlüğü sorunları
  - nullability crash'leri
  - eski AudioService / plugin sınıfı problemler
- `1.0.33` daha konsantre bir profile kaymış durumda:
  - splash startup orchestration sorunları
  - RevenueCat sync ve identity yarışları
  - audio player lifecycle yarışları
  - Android navigation hatası
  - Firebase Realtime Database init sırası problemi

Kısa sonuç:
- Bazı eski, dağınık ve yüksek gürültülü issue aileleri temizlenmiş görünüyor.
- Buna karşılık `1.0.33` içinde yeni ve daha ürün-akışı odaklı regressions gelmiş.
- En önemlisi, sorunlar artık daha çok startup ve medya/navigation akışlarında yoğunlaşıyor.

## Sayısal Karşılaştırma

### Genel

- `1.0.32`
  - Toplam event: 236
  - Fatal: 29
  - Non-fatal: 207
- `1.0.33`
  - Toplam event: 280
  - Fatal: 28
  - Non-fatal: 252

### Tarih Aralıkları

- `1.0.32`
  - Genel pencere: `1 Ocak 2026 - 9 Şubat 2026`
  - iOS pencere: `1 Ocak 2026` tek gün
  - Android pencere: `16 Ocak 2026 - 9 Şubat 2026`
- `1.0.33`
  - Genel pencere: `2 Mart 2026 10:24 UTC - 19 Mart 2026 07:06 UTC`
  - iOS pencere: `12 Mart 2026 09:46 UTC - 19 Mart 2026 01:36 UTC`
  - iOS fatal pencere: `12 Mart 2026 09:46 UTC - 17 Mart 2026 21:33 UTC`
  - Android pencere: `2 Mart 2026 10:24 UTC - 19 Mart 2026 07:06 UTC`
  - Android fatal pencere: `15 Mart 2026 23:28 UTC - 18 Mart 2026 02:01 UTC`

### Kaba Yoğunluk Karşılaştırması

Not:
- `1.0.33` yoğunlukları ham timestamp üzerinden hesaplandı.
- `1.0.32` yoğunlukları önceki rapordaki tarih aralıklarından türetilmiş yaklaşık değerlerdir.
- `1.0.32` genel yoğunluğu tam adil bir metrik değildir; çünkü iOS yalnızca tek günlük pencereye sahipken Android daha geç başlıyor.

- `1.0.32`
  - Genel yaklaşık yoğunluk: `~5.9 event/gün`, `~0.7 fatal/gün`
  - iOS yaklaşık yoğunluk: `7 event/gün`, `5 fatal/gün`
  - Android yaklaşık yoğunluk: `~9.2 event/gün`, `~1.0 fatal/gün`
- `1.0.33`
  - Genel yaklaşık yoğunluk: `~15.6 event/gün`, `~1.6 fatal/gün`
  - iOS yaklaşık yoğunluk: `~7.3 event/gün`, `~1.0 fatal/gün`
  - Android yaklaşık yoğunluk: `~12.3 event/gün`, `~1.1 fatal/gün`

### Yorum

- Observed: Toplam event sayısı arttı.
- Observed: Fatal sayısı çok az düştü.
- Observed: `1.0.33` daha kısa bir zaman penceresinde daha fazla event üretiyor.
- Strong inference: Geniş ama seyrek dağılan crash profili, daha dar ama daha yoğun birkaç hata kümesine dönüştü.

## Zaman Penceresi Yorumu

### `1.0.32`

- iOS verisi tek güne sıkışmış durumda.
- Android verisi yaklaşık 25 günlük pencereye yayılıyor.
- Genel sayı yüksek görünse de bunun büyük bölümü ANR gürültüsü ve dağınık non-fatal event'ler.

### `1.0.33`

- Android verisi yaklaşık 18 günlük pencereye yayılıyor.
- iOS verisi yaklaşık 8 günlük pencereye yayılıyor.
- Fatal event'ler tüm pencereye homojen dağılmıyor; özellikle `15-18 Mart 2026` arasında kümeleniyor.

### Sonuç

- Observed: Mutlak event sayıları birbirine yakın değil; `1.0.33` daha yüksek.
- Observed: Zaman penceresi de daha kısa.
- Strong inference: `1.0.33`'te sorunlar daha yoğun bir tempoda üretilmiş.

## `1.0.33` İçindeki Kritik Kümelerin Tarih Aralıkları

Bu bölüm, `1.0.33` içinde yeni veya öne çıkan hata ailelerinin hangi günlerde yoğunlaştığını gösterir.

- `splash_deferred_user_type_sync_failed`
  - Pencere: `11 Mart 2026 05:16 UTC - 19 Mart 2026 07:06 UTC`
  - Not: uzun süreye yayılmış ve sürekli tekrar eden bir aile
- `revenuecat_sync_attributes_*`
  - Pencere: `2 Mart 2026 10:24 UTC - 19 Mart 2026 05:06 UTC`
  - Not: en geniş tarih penceresine sahip aile; sürekli arka plan gürültüsü üretiyor
- `GoRouterDelegate.pop`
  - Pencere: `18 Mart 2026 01:00:51 UTC - 18 Mart 2026 01:01:03 UTC`
  - Not: dar zaman penceresinde kümelenmiş, muhtemelen aynı kullanıcı akışı ya da aynı sürüm davranışı
- `MethodChannelAudioPlayer.seek` + `checkInterruption`
  - Pencere: `15 Mart 2026 16:02 UTC - 18 Mart 2026 00:24 UTC`
  - Not: belirgin şekilde Mart ortasında ortaya çıkıyor
- `FirebaseDatabaseHostApi.setPersistenceEnabled`
  - Pencere: `18 Mart 2026 01:49 UTC - 18 Mart 2026 02:01 UTC`
  - Not: dar kümelenme; belirli startup senaryolarına bağlı olma ihtimali yüksek

## Problem Profilindeki Değişim

### `1.0.32` baskın karakteri

- `Potential ANR` gürültüsü çok baskın
- Ağ/DNS tabanlı font ve storage hataları baskın
- Dosya sistemi / image lifecycle kaynaklı fatal'ler var
- Null-check ve data schema crash'leri var
- Bazı eski platform/config kaynaklı problemler görülüyor

### `1.0.33` baskın karakteri

- Splash sırasında timeout ve orchestration hataları baskın
- RevenueCat sync ve identity akışları çok daha görünür
- Audio service seviyesinden çok player/seek/interruption seviyesine kayan crash'ler var
- Fatal'lerin büyük bölümü birkaç issue üzerinde yoğunlaşmış:
  - `GoRouterDelegate.pop`
  - `MethodChannelAudioPlayer.seek`
  - `AudioPlayer._setPlatformActive.checkInterruption`
  - `FirebaseDatabaseHostApi.setPersistenceEnabled`

## Çözülen veya Belirgin Azalan Sorunlar

### 1. Potential ANR gürültüsü

`1.0.32`
- Android: 176 event, fatal 0
- iOS: 1 event, fatal 0

`1.0.33`
- Ham CSV içinde `Potential ANR detected` gözlenmedi

Durum:
- Observed: `1.0.33` verisinde bu issue ailesi yok.
- Strong inference: ANR monitor gürültüsü ya kapatılmış ya da daha doğru filtrelenmiş.

Karar:
- Yüksek güvenle iyileşmiş.

### 2. Google Fonts DNS fatal hataları

`1.0.32`
- Android: 9 event, 9 fatal
- Semptom: `fonts.gstatic.com` host lookup

`1.0.33`
- Ham CSV içinde `fonts.gstatic.com` hatası yok

Durum:
- Observed: Bu issue ailesi `1.0.33` penceresinde görünmüyor.
- Strong inference: Runtime font fetch bağımlılığı büyük ölçüde kaldırılmış veya etkisi bastırılmış.

Karar:
- Büyük olasılıkla çözülmüş.

### 3. Nullability / veri şeması crash'leri

`1.0.32`
- `Null check operator used on a null value`: 1 fatal
- `Parent mode null-check`: 1 fatal
- `Null is not a subtype of int`: 1 fatal

`1.0.33`
- Aynı semptom ailesi ham CSV içinde gözlenmedi

Durum:
- Observed: Bu doğrudan veri schema / force unwrap crash'leri `1.0.33` içinde görünmüyor.

Karar:
- İyileşmiş veya en azından aktif veri penceresinde tekrar etmemiş.

### 4. Eski AudioService bind problemi

`1.0.32`
- `Unable to bind to AudioService`: 7 fatal
- Not: ağırlıkla daha eski sürümlerde, ama pencere içinde görülüyordu

`1.0.33`
- Bu semptom ham CSV'de yok

Durum:
- Observed: Eski service bind katmanı hatası görünmüyor.
- Strong inference: Audio problem tamamen çözülmemiş olsa da katman değiştirmiş.

Karar:
- Eski audio service bind bug'ı iyileşmiş.
- Ancak audio hatası yeni formda devam ediyor.

### 5. Eski RevenueCat UI / FragmentActivity uyumsuzluğu

`1.0.32`
- Eski sürümlerde 1 fatal

`1.0.33`
- Gözlenmedi

Karar:
- Eski tarihli platform entegrasyon sorunu aktif değil.

### 6. Realtime Database `MissingPluginException cancel` fatal

`1.0.32`
- 1 fatal

`1.0.33`
- Aynı issue id veya aynı semptom doğrudan görünmedi

Durum:
- Observed: Eski "cancel / missing plugin" fatal'i yok.
- Strong inference: O spesifik plugin detach/cancel formu iyileşmiş.

Karar:
- Eski DB plugin cancel hatası aktif değil.
- Ancak aynı modül içinde yeni bir init-order problemi gelmiş.

## Devam Eden ama Şekil Değiştiren Sorunlar

### 7. Dosya sistemi / story asset yaşam döngüsü sorunları

`1.0.32`
- iOS `Directory not empty`: 2 fatal
- iOS `PathNotFoundException`: 1 non-fatal
- Android `empty image file`: 1 fatal
- iOS `Could not decompress image`: 3 fatal

`1.0.33`
- `story_image_missing_before_read`: 4 non-fatal
- `Stream has been disposed`: 1 non-fatal
- `Decoded image has been disposed` ve `empty file` semptomları görünmüyor

Durum:
- Observed: Eski ağır image/file corruption fatal'leri görünmüyor.
- Observed: Aynı alan tamamen temizlenmemiş; artık daha çok `missing before read` şeklinde ve non-fatal.
- Strong inference: Bu alan tam çözülmemiş, ancak "hard crash / hard corruption" profilinden "guarded missing asset" profiline çekilmiş.

Karar:
- Kısmi iyileşme var.
- Kök neden ailesi devam ediyor.

### 8. Storage / ağ bağımlılığı

`1.0.32`
- `storage.googleapis.com` host lookup: 14 non-fatal
- `firebasestorage.googleapis.com` host lookup: 3 non-fatal
- `firebase_auth/network-request-failed`: 3 non-fatal

`1.0.33`
- Storage/network splash ailesi: yaklaşık 30 event, çoğu non-fatal
- Cross-platform görülebiliyor
- Splash handler altında gruplanıyor

Durum:
- Observed: Ağ kökenli storage hataları bitmemiş.
- Observed: Fatal olmaktan çok guarded non-fatal profile kaymış.
- Strong inference: containment iyileşmiş, root cause çözülmemiş.

Karar:
- Devam ediyor.
- Şiddeti daha kontrollü ama hacmi hâlâ anlamlı.

### 9. RevenueCat identity sorunu

`1.0.32`
- `InvalidAppUserIdError`: 1 non-fatal

`1.0.33`
- RevenueCat identity family: en az 3-4 non-fatal event
- `revenuecat_identity_sync_failed`
- `revenuecat_not_configured_for_identity_sync`

Durum:
- Observed: RevenueCat kimlik katmanı hâlâ problemli.
- Strong inference: Eski "invalid app user id" problemi doğrudan aynı biçimde görünmese de daha geniş bir identity sync yarışına dönüşmüş.

Karar:
- Devam ediyor.
- Sorunun kapsamı büyümüş.

### 10. Stream/image lifecycle gürültüsü

`1.0.32`
- `Decoded image has been disposed`: 1 fatal
- `Stream has been disposed`: 1 non-fatal

`1.0.33`
- `Stream has been disposed`: 1 non-fatal
- `Decoded image has been disposed`: görünmüyor

Durum:
- Observed: düşük hacimli lifecycle sorunu tamamen kaybolmamış.
- Observed: fatal versiyonu artık görünmüyor.

Karar:
- Kısmi iyileşme var.
- Düşük hacimli olarak devam ediyor.

## Yeni Gelen veya Belirgin Biçimde Kötüleşen Sorunlar

### 11. Splash deferred user/revenue sync timeout

`1.0.32`
- Bu issue ailesi raporda yok

`1.0.33`
- 119 event
- Fatal değil
- En büyük issue kümesi

Durum:
- Observed: `1.0.33`'ün en baskın yeni problemi bu.
- Strong inference: startup orchestration `1.0.33` ile daha kırılgan hale gelmiş.

Karar:
- Net yeni issue.

### 12. RevenueCat sync queue / timeout problemi

`1.0.32`
- Report içinde bu aile daha eski sürümlerde (`1.0.27/1.0.28`) 7 non-fatal olarak görünüyordu
- `1.0.32` aktif penceresinde baskın issue değildi

`1.0.33`
- `SharedPreferenceService._performSpecial3DayPaywallSync`: 86 event
- `Purchases.syncAttributesAndOfferingsIfNeeded`: 6 event
- Çekirdek semptom: timeout ve network/non-network ayrımı

Durum:
- Observed: RevenueCat sync ailesi `1.0.33` içinde çok daha yüksek hacimle geri dönmüş.
- Strong inference: Yeni queue mimarisi veya splash ile etkileşimi yeni gürültü ve timeout profili üretmiş.

Karar:
- Belirgin regresyon.

### 13. Android onboarding navigation fatal

`1.0.32`
- Böyle bir issue yok

`1.0.33`
- `GoRouterDelegate.pop`: 13 event, 13 fatal

Durum:
- Observed: kullanıcı aksiyonu ile doğrudan çarpan yeni bir fatal bug.

Karar:
- Net yeni issue.

### 14. Audio player seek/interruption fatal'leri

`1.0.32`
- Eski audio problemi service bind katmanındaydı
- just_audio seek/interruption fatalleri görünmüyordu

`1.0.33`
- `MethodChannelAudioPlayer.seek`: 6 fatal
- `AudioPlayer._setPlatformActive.checkInterruption`: 4 fatal

Durum:
- Observed: audio problemi yeni katmana taşınmış.
- Strong inference: service bootstrap iyileşmiş ama player command sequencing bozulmuş.

Karar:
- Net yeni regresyon.

### 15. Firebase Realtime Database persistence init-order fatal

`1.0.32`
- Eski DB problemi plugin cancel/detach sınıfındaydı

`1.0.33`
- `FirebaseDatabaseHostApi.setPersistenceEnabled`: 2 fatal
- Semptom: `setPersistenceEnabled()` çok geç çağrılıyor

Durum:
- Observed: aynı modül içinde yeni ama farklı sınıfta bir bug ortaya çıkmış.

Karar:
- Net yeni issue.

### 16. Crashlytics telemetry kirlenmesi artık ilk sınıf risk

`1.0.32`
- Rapor, fatal sınıflandırma tutarsızlığından söz ediyordu

`1.0.33`
- Sadece fatal sınıflandırma değil, `issue_id`, `reason`, issue title ve gerçek hata ailesi arasında sızıntı görüldü
- Özellikle splash ve RevenueCat event'lerinde bağlam karışıyor

Durum:
- Observed: telemetry problemi daha görünür ve analizi bozacak seviyede.

Karar:
- Worsened instrumentation problem.

### 17. RenderFlex overflow event'leri

`1.0.32`
- Bu aile raporda yok

`1.0.33`
- iOS ağırlıklı 3 event, 2 fatal

Durum:
- Observed: küçük UI overflow, Crashlytics'te gereğinden sert görünüyor.

Karar:
- Yeni issue veya yeni görünür olmuş telemetry davranışı.

## Çözülenler, Devam Edenler, Yeni Gelenler

### Çözülen veya aktif pencerede kaybolanlar

- `Potential ANR` gürültüsü
- `Google Fonts host lookup`
- `Decoded image has been disposed` fatal
- `empty image file` fatal
- `Directory not empty` fatal
- `PathNotFoundException` story image
- nullability / schema crash'leri
- eski `AudioService` bind fatal'i
- eski `MissingPlugin cancel` fatal'i
- eski `FlutterFragmentActivity` gereksinim hatası

### Devam eden ama form değiştirenler

- story asset lifecycle / file race
- storage/network bağımlılığı
- RevenueCat identity alanı
- image stream lifecycle gürültüsü

### Yeni veya kötüleşenler

- splash deferred startup sync timeout
- RevenueCat sync queue timeout/regresyonu
- Android onboarding `GoRouter.pop` fatal
- just_audio seek/interruption fatal'leri
- FirebaseDatabase persistence init-order fatal
- telemetry context contamination
- RenderFlex overflow event'leri

## Yorum: `1.0.32` -> `1.0.33` kalite kayması

Observed:
- Fatal toplamı sabit kalmış.
- Ama fatal'lerin tipi daha ürün-akışı odaklı hale gelmiş.

Strong inference:
- `1.0.32`'de çok sayıda eski/altyapısal ve dağınık problem vardı.
- `1.0.33` bu dağınık problemlerin bir kısmını temizlemiş.
- Buna karşılık startup orchestration, RevenueCat state management ve audio player sequencing tarafında yeni kompleksite üretmiş.

Özellikle dikkat çeken dönüşümler:

- ANR gürültüsü -> splash timeout gürültüsü
- image corruption / empty file fatal -> guarded missing asset non-fatal
- AudioService bind fatal -> just_audio seek/interruption fatal
- DB plugin cancel fatal -> DB init-order fatal
- düşük hacimli RevenueCat identity hatası -> yüksek hacimli RevenueCat sync/orchestration problemi

## Önceliklendirme Önerisi

Bu karşılaştırmaya göre bir sonraki düzeltme sırası şöyle olmalı:

1. Android `GoRouterDelegate.pop` fatal
2. Splash deferred startup orchestration
3. RevenueCat sync queue ve identity coordination
4. Audio player command sequencing
5. FirebaseDatabase init-order
6. Telemetry cleanup
7. Story asset lifecycle hardening

## Sonuç

`1.0.33`, `1.0.32`'ye göre bazı eski ve dağınık crash ailelerini temizlemiş görünüyor. Özellikle ANR gürültüsü, font DNS fatal'leri, nullability crash'leri ve bazı eski image/file corruption varyantları artık görünmüyor. Ancak uygulama kalite profili daha iyi hale gelmiş değil; yalnızca problem odağı değişmiş.

Yeni sürümde hata yoğunluğu daha çok üç alanda toplanmış:

- splash startup orchestration
- RevenueCat sync/identity akışları
- audio/navigation kullanıcı akışları

Bu nedenle `1.0.33` için ana odak "eski altyapısal crash temizliği" değil, "çalışan ama yarışan sistemlerin doğru sırada, doğru lifecycle anında işletilmesi" olmalı.
