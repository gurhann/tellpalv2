# TellPal 1.0.34 Release Validation ve Gözlem Paketi

Tarih: 2026-04-14

## Regression Checklist

- Onboarding back:
  - root stack’te back ile crash olmamalı
- RTDB bootstrap:
  - app açılışında `setPersistenceEnabled()` sıralama hatası olmamalı
- Audio basic playback:
  - bir hikaye sesi aç, durdur, tekrar oynat
  - lullaby/relax akışında basic play-pause çalışmalı
- Anonymous login -> paywall:
  - cold start sonrası anonymous login akışı çalışmalı
  - paywall açılışı config fetch yüzünden kilitlenmemeli
- Story search -> story open -> asset download:
  - story açılışında download tamamlanmalı
  - background/resume sonrası fatal crash olmamalı

## Crashlytics Takip Sorguları

- `Future already completed`
- `startup_set_user_type_failed`
- `revenuecat_sync_attributes_retryable_failure`
- `android.os.MessageQueue.nativePollOnce`

## Eski / Yeni Issue Eşlemeleri

- `1.0.33` splash deferred startup timeout
  - `SplashScreenViewState._syncUserTypeAndRevenueInBackground`
  - `1.0.34` karşılığı:
  - `StartupDeferredTaskCoordinator._runTask`
  - `startup_set_user_type_failed`
- `1.0.33` RevenueCat queue timeout
  - `SharedPreferenceService._performSpecial3DayPaywallSync`
  - `1.0.34` karşılığı:
  - aynı aile, ama telemetry daha zengin
- `1.0.33` telemetry contamination
  - `1.0.34` içinde büyük ölçüde temizlenmiş, aynı issue title ile takip edilmemeli

## İlk 72 Saat Gözlem Notları

Bakılacak alanlar:

- issue count
- affected users
- platform split
- fatal / non-fatal dağılımı
- yeni regression var mı

Başarı sinyalleri:

- iOS `Future already completed` fatal’i sıfıra iner
- Android `startup_set_user_type_failed` hacmi düşer
- RevenueCat retryable sync event’leri spike üretmez
- `nativePollOnce` event’lerinde yeni diagnostic alanlar görünür

Risk sinyalleri:

- `Future already completed` farklı title ile geri döner
- RevenueCat configure success cache yanlış user identity ile eşleşir
- paywall açılışı veya premium status davranışı değişir

## 72 Saat Karar Matrisi

Detaylı go / hold / patch eşikleri için:

- `docs/1.0.34_crashlytcs/release_decision_matrix_72h_1.0.34.md`
