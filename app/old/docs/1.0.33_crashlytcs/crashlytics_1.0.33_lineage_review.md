# TellPal 1.0.33 Crashlytics Lineage Review

Başlık: `1.0.33` sorunlarının `1.0.32` geçmişiyle yeniden değerlendirilmesi

Tarih: 2026-03-20

Kaynaklar:
- `docs/1.0.33_crashlytcs/crashlytics_detailed_report_1.0.33.md`
- `docs/1.0.33_crashlytcs/crashlytics_comparison_1.0.32_vs_1.0.33.md`
- `docs/1.0.33_crashlytcs/tellpal_ios_1.0.33_crashlyts_logs.csv`
- `docs/1.0.33_crashlytcs/tellpal_android_1.0.33_crashlyts_logs.csv`
- `docs/1.0.32_crashlytcs/crashlytics-sorun-analizi.md`

## Amaç

Bu doküman, `1.0.33` içindeki sorunları yalnızca tek başına değil, `1.0.32` ile olan soy ilişkisi üzerinden tekrar sınıflandırır.

Sorular:
- Hangi sorunlar gerçekten yeni?
- Hangi sorunlar eski bir kök nedenin yeni yüzü?
- Hangi alanlarda çözüm stratejisini değiştirmek gerekiyor?

## Kısa Sonuç

`1.0.33` içindeki sorunlar üç sınıfa ayrılıyor:

1. Eski sürümden gelen ve biçim değiştiren sorunlar
2. Eski bir zayıflığın üstüne eklenmiş net regresyonlar
3. Gerçekten yeni ve lokal bug'lar

En önemli sonuç:
- `1.0.33` içindeki en büyük iki hacimli küme, tamamen yeni bağımsız sorunlar değil.
- Bunlar büyük ölçüde eski sürümden gelen ağ, RevenueCat, lifecycle ve dosya bütünlüğü kırılganlıklarının; `1.0.33` içinde yeni startup orchestration ile birleşmiş hali.

Bu yüzden çözüm yaklaşımı da değişmeli:
- Sadece tek tek exception patch etmek yetmez.
- Önce sorunun "hangi aileden geldiğini" doğru ayırmak gerekiyor.

## Üst Düzey Sınıflandırma

### A. Önceki sürümden gelen / devam eden aileler

- dosya sistemi ve story asset yaşam döngüsü
- storage/network bağımlılığı
- RevenueCat identity ve sync kırılganlığı
- image stream / lifecycle kırılganlığı
- telemetry sınıflandırma tutarsızlığı

### B. `1.0.33` ile büyüyen veya yeni biçim alan aileler

- splash deferred startup timeout zinciri
- RevenueCat 3-day paywall sync timeout patlaması
- audio player seek / interruption yarışları
- FirebaseDatabase init-order sorunu

### C. Net yeni lokal bug'lar

- Android onboarding `GoRouterDelegate.pop`
- küçük UI overflow event'lerinin fazla sert raporlanması

## 1.0.33 Sorunlarının Soy Analizi

### 1. Telemetry context contamination

`1.0.32` ilişkisi:
- `1.0.32` raporu fatal sınıflandırma tutarsızlığına zaten işaret ediyordu.
- Yani telemetry/raporlama problemi yeni değil.

`1.0.33` durumu:
- Problem büyümüş.
- Artık sadece fatal/non-fatal tutarsızlığı yok.
- `issue_id`, `issue_fix_version`, `reason` ve issue title birbirine bulaşıyor.

Sınıflandırma:
- Önceki sürümden gelen problem
- `1.0.33` içinde kötüleşmiş

Yorum:
- Bu artık bir "gözlemlenebilirlik problemi" değil, doğrudan analiz doğruluğunu bozan P0 seviyesinde bir problem.

Çözüm yaklaşımı nasıl değişmeli:
- Önceden bu alan daha çok "iyileştirme" gibi duruyordu.
- Şimdi öncelik yükselmeli.
- Çünkü hangi hatanın gerçekten yeni, hangisinin miras olduğuna güvenle karar verebilmek için telemetry önce temizlenmeli.

Öneri:
- `FlutterError.onError` tek noktaya indirilmeli.
- Generic custom key sızıntısı durdurulmalı.
- Event bazlı metadata set/reset standardı getirilmeli.

### 2. Splash deferred startup sync timeout

`1.0.32` ilişkisi:
- Aynı issue title `1.0.32` raporunda yok.
- Ama altında yatan bileşenler `1.0.32` içinde zaten vardı:
  - storage/network lookup hataları
  - RevenueCat identity kırılganlığı
  - auth/network kırılganlığı

`1.0.33` durumu:
- 119 event ile en büyük küme
- Teknik olarak yeni issue
- Kök neden olarak tam yeni değil; eski kırılgan bileşenlerin yeni orchestration altında birleşmiş hali

Sınıflandırma:
- Yeni yüzlü birleşik regresyon
- Kök neden tamamen yeni değil

Yorum:
- Bu issue'yu "timeout arttıralım" diye çözmek yanlış olur.
- Çünkü bu aslında ayrı ayrı çalışan eski zayıflıkları splash içinde tek darboğaza toplamış.

Çözüm yaklaşımı nasıl değişmeli:
- `setUserType`, RevenueCat identity sync ve paywall attribute sync birbirinden ayrılmalı.
- Startup'ta yalnızca gerçekten zorunlu iş kalsın.
- Splash sona ermeden RevenueCat state makinesi tamamlanmaya zorlanmamalı.

Öneri:
- `1.0.33` için bu issue bağımsız değil, "startup coordinator" projesi olarak ele alınmalı.

### 3. RevenueCat 3-day paywall sync timeout

`1.0.32` ilişkisi:
- RevenueCat sync ailesi daha önce de vardı.
- `1.0.32` raporunda aynı issue id ailesi eski sürümlerde network kaynaklı görünüyordu.
- RevenueCat identity alanı da zaten problemliydi.

`1.0.33` durumu:
- Hacim patlamış.
- Semptom artık sadece saf network error değil.
- Timeout, `not_configured`, lifecycle/readiness problemi de işin içine girmiş.

Sınıflandırma:
- Eski aile
- `1.0.33` içinde belirgin regresyon

Yorum:
- Bunu artık "RevenueCat ağ problemi" diye etiketlemek yanlış.
- Asıl konu:
  - config readiness
  - lifecycle
  - startup sırası
  - retry sınıflandırması

Çözüm yaklaşımı nasıl değişmeli:
- Sadece code `10` retry etmek yetmez.
- Timeout ve `not_configured` da retryable kabul edilmeli.
- Queue, app foreground ve identity hazır olmadan çalışmamalı.
- Aynı attribute değeri için gereksiz sync tetiklenmemeli.

Revize öncelik:
- Bu alan tek başına değil, startup coordinator ve RevenueCat state machine ile birlikte çözülmeli.

### 4. RevenueCat identity sync failures

`1.0.32` ilişkisi:
- `InvalidAppUserIdError` zaten vardı.
- Yani kimlik eşleme katmanı eski sürümden beri kırılgandı.

`1.0.33` durumu:
- Sorun daha genel hale gelmiş:
  - timeout
  - `not_configured`
  - transition failure

Sınıflandırma:
- Önceki sürümden gelen kök neden
- `1.0.33` içinde kapsamı genişlemiş

Yorum:
- Bu sorunun özü network değil.
- Bu bir kimlik durum makinesi eksikliği.

Çözüm yaklaşımı nasıl değişmeli:
- Firebase UID ile RevenueCat user id senkronizasyonu tek bir otorite altına alınmalı.
- `synchronizeRevenueCatIdentity()` startup boyunca birkaç ayrı yerden tetiklenmemeli.
- "Gerekli mi?" kararı cached son state ile verilmeli.

Revize öneri:
- RevenueCat için ayrı bir coordinator/service state machine tasarlanmalı.

### 5. Storage/network splash errors

`1.0.32` ilişkisi:
- `storage.googleapis.com`
- `firebasestorage.googleapis.com`
- `firebase_auth/network-request-failed`
- Bunların hepsi zaten vardı.

`1.0.33` durumu:
- Hâlâ var, ama artık splash handler altında daha çok gruplanıyor.
- Ayrıca telemetry kirlenmesi yüzünden bazen başka issue'larla iç içe görünüyor.

Sınıflandırma:
- Eski sürümden gelen devam eden sorun

Yorum:
- Bu alan yeni değil.
- Sadece `1.0.33` içinde startup bağlamında daha görünür hale gelmiş.

Çözüm yaklaşımı nasıl değişmeli:
- Buraya ayrı crash fix yazmak yerine:
  - cache-first
  - lazy image loading
  - startup sırasında zorunlu olmayan network çağrılarını erteleme
  - beklenen transport error'larını widget düzeyinde absorbe etme

### 6. Story image missing before read

`1.0.32` ilişkisi:
- Aynı aile zaten vardı:
  - `Directory not empty`
  - `PathNotFoundException`
  - `empty file`
  - `Could not decompress image`

`1.0.33` durumu:
- Şimdi aynı problem `story_image_missing_before_read` olarak ve non-fatal biçimde görülüyor.

Sınıflandırma:
- Eski sorunun daha güvenli ama tam çözülmemiş hali

Yorum:
- Bu iyi bir işaret:
  - sert crash azalıyor
  - guard katmanı eklenmiş
- Ama kök neden çözülmüş değil:
  - read/write yarışları
  - staging/live swap
  - cleanup anında dosya okunması

Çözüm yaklaşımı nasıl değişmeli:
- Bu alanda artık decode/decompress özel fix peşine koşmak yerine
- klasör transactionality ve read-after-write garantisi üzerine odaklanılmalı.

Revize öneri:
- Story asset lifecycle tek bir atomic state modeliyle yönetilmeli.

### 7. Stream disposed / image lifecycle noise

`1.0.32` ilişkisi:
- `Decoded image has been disposed` fatal
- `Stream has been disposed` non-fatal

`1.0.33` durumu:
- Fatal biçimi kaybolmuş
- Düşük hacimli non-fatal devam ediyor

Sınıflandırma:
- Önceki sürümden gelen kalıntı
- Kısmi iyileşme var

Yorum:
- Bu artık büyük problem değil.
- Bu alan düşük önceliğe çekilebilir.

Çözüm yaklaşımı nasıl değişmeli:
- Büyük mimari iş gibi değil, cleanup/refinement işi olarak ele alınmalı.

### 8. Audio failures

`1.0.32` ilişkisi:
- Audio hatası vardı ama service bind seviyesindeydi.
- `Unable to bind to AudioService`

`1.0.33` durumu:
- Audio problemi devam ediyor ama katman değiştirmiş:
  - `MethodChannelAudioPlayer.seek`
  - `checkInterruption`

Sınıflandırma:
- Aynı problem alanı
- Yeni mekanizma
- Eski çözüm odağı artık yanlış

Yorum:
- `1.0.32` perspektifiyle bakılırsa audio tarafında "service bind" düzeltmesi yeterli sanılabilir.
- Karşılaştırma gösteriyor ki asıl sorun artık player komut sıralaması ve lifecycle eşzamanlılığı.

Çözüm yaklaşımı nasıl değişmeli:
- Service init odaklı müdahaleler artık öncelik olmamalı.
- Yeni öncelik:
  - `setAudioSource` await
  - `seek` await
  - player command serialization
  - interruption/loading sırasında komut reddetme

Revize öneri:
- Audio için command queue / state guard eklenmeli.

### 9. Firebase Realtime Database problemi

`1.0.32` ilişkisi:
- Eski sorun: plugin cancel / detach / missing plugin
- Yeni sorun: `setPersistenceEnabled()` init-order

Sınıflandırma:
- Aynı modül
- Farklı kök neden
- Yeni regresyon

Yorum:
- Burada "DB alanı zaten sorunluydu" demek doğru ama yetersiz.
- Eski hata çözülmüş olabilir; yeni hata başka bir mimari sıradan geliyor.

Çözüm yaklaşımı nasıl değişmeli:
- Plugin guard mantığına odaklanmak yerine bootstrap sırasını düzeltmek gerekiyor.
- `FirebaseDatabase.instance` kullanımını erken bootstrap öncesi yasaklamak gerekir.

### 10. Android onboarding `GoRouterDelegate.pop`

`1.0.32` ilişkisi:
- Önceki sürüm raporunda böyle bir issue yok.

Sınıflandırma:
- Net yeni lokal bug

Yorum:
- Bu issue kök neden ailesiyle karışmıyor.
- Hızlıca ve doğrudan kapatılması gereken bir regression.

Çözüm yaklaşımı nasıl değişmeli:
- Bu alan için mimari çözüm değil, küçük ve güvenli guard fix yeterli.

### 11. RenderFlex overflow

`1.0.32` ilişkisi:
- Önceki raporda görünmüyor.

`1.0.33` durumu:
- Yeni görünür olmuş.
- Ama büyük olasılıkla telemetry daha agresif olduğu için görünür.

Sınıflandırma:
- Muhtemelen yeni veya yeni görünür olmuş
- Düşük öncelik

Yorum:
- Bu issue tek başına ürün önceliği olmamalı.
- Önce telemetry temizlenmeli, sonra gerçekten kullanıcı etkisi var mı bakılmalı.

## Revize Edilmiş Çözüm Stratejisi

Karşılaştırma sonrası çözüm stratejisi değiştirilmeli.

Önceki bakış:
- Her issue ayrı bug gibi ele alınabilirdi.

Yeni bakış:
- Issue'ların önemli kısmı aynı kök ailelerin farklı yansımaları.

Bu yüzden çözüm planı şu eksende olmalı:

### P0. Telemetry cleanup

Neden:
- Telemetry kirliyse diğer ilerlemeyi ölçemezsiniz.

Kapsam:
- tek `FlutterError.onError`
- event-scoped custom key
- reason / issue id sızıntısını engelleme

### P1. Startup coordinator refactor

Neden:
- `splash_deferred_user_type_sync_failed`
- RevenueCat sync patlaması
- identity sync
- splash storage/network gürültüsü

Bunların hepsi büyük oranda aynı startup orchestration probleminden besleniyor.

Kapsam:
- splash sonrası ertelenebilir işleri ayır
- foreground gating
- bağımsız guarded background task'ler
- timeout stratejisini iş bazında ayır

### P1. RevenueCat state machine

Neden:
- eski issue ailesi devam ediyor
- `1.0.33` içinde büyümüş

Kapsam:
- configure, identity, attribute sync tek akışta
- retry sınıflandırmasını düzelt
- duplicate trigger azalt

### P1. Hızlı lokal regressions

Kapsam:
- Android `GoRouter.pop`
- FirebaseDatabase init-order

Neden:
- küçük müdahale ile yüksek fayda

### P2. Audio command sequencing

Neden:
- kullanıcıya çarpan fatal'ler var
- ama çözüm service bind değil, player sequencing

Kapsam:
- serialize komutlar
- `await` eksiklerini kapat
- interruption-safe playback

### P2. Story asset transactionality

Neden:
- sert crash azalmasına rağmen aynı kök aile bitmemiş

Kapsam:
- staging/live swap güvenliği
- read lock / write lock tutarlılığı
- last-known-good fallback

### P3. UI overflow ve düşük hacimli lifecycle gürültüsü

Neden:
- ürün etkisi düşük
- önce üst seviye aileler çözülmeli

## Son Karar

`1.0.33` detaylı raporu tek başına okunduğunda birçok issue bağımsız ve yeni gibi görünebilir.
Karşılaştırma raporu dikkate alındığında daha doğru resim şu:

- gerçek anlamda tamamen yeni olanlar az
- büyük hacimli yeni kümeler, eski kırılgan sistemlerin yeni orchestration altında birleşmiş hali
- bu yüzden çözüm stratejisi tek tek exception patch'leri değil, kök ailelere göre düzenlenmeli

En net miras alanlar:
- RevenueCat
- storage/network
- story asset lifecycle
- telemetry

En net yeni regresyonlar:
- Android onboarding pop
- FirebaseDatabase init-order
- audio player command race

En kritik stratejik değişiklik:
- `1.0.33` için önce telemetry ve startup orchestration düzeltilmeden, alttaki issue sayılarını sağlıklı okumak zor olacak.
