# TellPal 1.0.34 Safe Fix Task List

Tarih: 2026-04-14

Bu liste, `1.0.34` Crashlytics bulguları ve `1.0.33 -> 1.0.34` fark analizi dikkate alınarak hazırlanmıştır.

## Ticket 1

- Başlık: `P0 - iOS storage download fatal: Future already completed`
- Kanıt:
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_ios.csv`
  - issue title: `FirebaseService._downloadFileOnMainThread.<fn>`
  - exception: `Bad state: Future already completed`
- Risk: düşük
- Dokunulan alan:
  - ana servis: `lib/common/services/firebase_service.dart`
  - yardımcı: `lib/common/services/download_completion_guard.dart`
- Acceptance criteria:
  - `onDone`, `onError`, timeout/cancel yarışında ikinci terminal completion crash üretmez.
  - Story download success/failure çıktıları mevcut davranışı korur.
  - Geç gelen completion event’leri yalnızca ignore edilir.
- Validation:
  - `test/common/services/download_completion_guard_test.dart`
  - iOS story open / background / resume manuel senaryosu
- Non-goals:
  - story validation kurallarını değiştirmek
  - download mekanizmasını refactor etmek

## Ticket 2

- Başlık: `P1 - RevenueCat config/state check single-flight sertleştirme`
- Kanıt:
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_2.csv`
  - issue title: `StartupDeferredTaskCoordinator._runTask`
  - reason: `startup_set_user_type_failed`
- Risk: orta
- Dokunulan alan:
  - ana servis: `lib/common/services/set_user_type_information_service.dart`
  - yardımcı: `lib/common/services/revenuecat_config_gate.dart`
- Acceptance criteria:
  - Aynı session ve aynı auth identity altında duplicate config fetch reuse edilir.
  - Başarılı config sonucu kısa süreli success cache ile tekrar kullanılabilir.
  - Premium/user type kararı değişmez.
- Validation:
  - `test/common/services/revenuecat_config_gate_test.dart`
  - Android cold start + anonymous login -> paywall
- Non-goals:
  - paywall görünürlük mantığını değiştirmek
  - RevenueCat identity akışını baştan tasarlamak

## Ticket 3

- Başlık: `P1 - RevenueCat attribute sync readiness-aware telemetry ve baskı azaltımı`
- Kanıt:
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_3.csv`
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_ios.csv`
  - reason: `revenuecat_sync_attributes_retryable_failure`
- Risk: orta
- Dokunulan alan:
  - ana servis: `lib/common/services/shared_preference_service.dart`
  - yardımcı: `lib/common/services/startup_diagnostics_registry.dart`
- Acceptance criteria:
  - Attribute sync, shared config check sonucunu kullanır.
  - `config_inflight_reused`, `startup_elapsed_ms`, `app_lifecycle_state` telemetry’ye eklenir.
  - Queue caller path’e geri taşınmaz.
- Validation:
  - mevcut queue testleri
  - zayıf ağ altında iOS/Android startup smoke
- Non-goals:
  - retry semantiğini daraltmak
  - entitlement mantığını değiştirmek

## Ticket 4

- Başlık: `P2 - Android ANR tanısı için startup diagnostics snapshot`
- Kanıt:
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_1.csv`
  - `docs/1.0.34_crashlytcs/crashlytcs_1.0.34_android_2.csv`
  - issue title: `android.os.MessageQueue.nativePollOnce`
- Risk: düşük
- Dokunulan alan:
  - ana servis: `lib/utils/performance_monitor.dart`
  - yardımcı: `lib/common/services/startup_deferred_task_coordinator.dart`
- Acceptance criteria:
  - ANR event’i aktif startup task adlarını ve RevenueCat diagnostic snapshot’ını taşır.
  - Yeni telemetry duplicate ANR üretmez.
  - Existing cooldown davranışı korunur.
- Validation:
  - `test/utils/anr_monitor_lifecycle_test.dart`
- Non-goals:
  - ANR threshold/cooldown değiştirmek
  - audio/navigation/RTDB tarafına dokunmak

## Ticket 5

- Başlık: `P2 - Release validation ve 72 saatlik gözlem paketi`
- Kanıt:
  - `1.0.33 -> 1.0.34` karşılaştırma raporu
  - `1.0.34` yeni issue aileleri
- Risk: düşük
- Dokunulan alan:
  - dokümantasyon
- Acceptance criteria:
  - regression checklist hazır olur
  - release sonrası izlenecek Crashlytics sorguları hazır olur
  - eski ve yeni issue eşlemeleri net olur
- Validation:
  - `docs/1.0.34_crashlytcs/release_validation_1.0.34.md`
- Non-goals:
  - rollout otomasyonu yazmak

