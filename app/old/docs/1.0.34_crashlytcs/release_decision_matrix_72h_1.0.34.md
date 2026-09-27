# TellPal 1.0.34 72 Saatlik Release Karar Matrisi

Tarih: 2026-04-15

Bu doküman, `1.0.34` issue aileleri için uygulanan güvenli fix'lerin release sonrası ilk 72 saatte nasıl değerlendirileceğini tanımlar.

Önemli not:

- Bu eşikler "operasyonel karar eşiği"dir.
- Kök nedenin kesin çözüldüğünü değil, rollout'un güvenle devam edip etmeyeceğini söyler.
- Ham issue count tek başına yeterli değildir; rollout yüzdesi değiştikçe count doğal olarak oynar.

## Ölçüm Kuralı

Öncelik sırası:

1. `affected users / 1k sessions`
2. `issue count / 1k sessions`
3. Ham `affected users`
4. Ham `issue count`

Kıyas yöntemi:

- Önce patch'siz `1.0.34` için son stabil 72 saatlik baseline kilitlenir.
- Karşılaştırma aynı rollout penceresinde yapılır.
- Eğer session normalize metriği yoksa, karar verirken `affected users` sayısı `issue count`'tan daha yüksek ağırlık alır.

## Baseline Snapshot

Release öncesi aşağıdaki 4 aile için tek satırlık baseline tablo hazırlanmalı:

- `Future already completed`
- `startup_set_user_type_failed`
- `revenuecat_sync_attributes_retryable_failure`
- `android.os.MessageQueue.nativePollOnce`

Kaydedilecek alanlar:

- toplam issue count
- affected users
- platform split
- rollout oranı
- session varsa `affected users / 1k sessions`

## Karar Seviyeleri

- `Green`: rollout devam edebilir
- `Yellow`: rollout artırılmaz, mevcut yüzde korunur, ek veri toplanır
- `Red`: rollout artışı durdurulur; gerekiyorsa hotfix / follow-up patch hazırlanır

## Ana Karar Tablosu

| Issue ailesi | Green | Yellow | Red | Aksiyon |
|---|---|---|---|---|
| `Future already completed` iOS fatal | İlk 72 saatte `0 event` ve `0 affected user` | `1 event` veya `1 affected user`, aynı stack ile sınırlı | `>=2 event`, `>1 affected user`, veya aynı path'in farklı title ile geri dönmesi | Green: rollout devam. Yellow: rollout `<=50%` tutulur, raw event incelenir. Red: rollout artışı durur, storage path hotfix açılır. |
| `startup_set_user_type_failed` | Baseline'a göre `affected users / 1k sessions` en az `%50` düşer ve gün bazında artış yoktur | Düşüş `%20-%50` arasındadır veya metrik baseline etrafında `+-20%` bandında yataydır | Düşüş `<%20`, metrik baseline üstüne `%20+` çıkar, veya splash/paywall/premium user report gelir | Green: rollout devam. Yellow: rollout sabitlenir, yeni raw sample toplanır. Red: RevenueCat startup follow-up patch açılır. |
| `revenuecat_sync_attributes_retryable_failure` | Baseline'a göre `affected users / 1k sessions` en az `%30` düşer; error code dağılımı çoğunlukla network/timeout kalır | Metrik baseline'a yakın seyreder ama spike üretmez; kullanıcı şikayeti yoktur | Metrik baseline üstüne `%30+` çıkar, `not_configured` ağırlığı belirgin artar, veya paywall/premium mismatch şikayeti gelir | Green: rollout devam. Yellow: rollout sabitlenir, queue telemetry örnekleri incelenir. Red: shared readiness/gate akışı için patch açılır. |
| `android.os.MessageQueue.nativePollOnce` | Baseline'dan kötü değildir ve yeni event'lerin çoğunda yeni diagnostic key'ler görünür | Hacim baseline'a yakın ya da `%20`'ye kadar daha kötüdür; diagnostic key'ler gelir ama aynı startup task baskın görünür | Hacim baseline'dan `%20+` kötüdür, yeni diagnostic key'ler görünmez, veya Android vitals/ANR user report bozulur | Green: rollout devam. Yellow: rollout sabitlenir, dominant task/fingerprint ayrıştırılır. Red: Android startup/ANR patch hattı açılır. |

## Global Release Gate

Tek bir issue ailesinden bağımsız olarak aşağıdakiler doğrudan `Red` kabul edilir:

- yeni bir fatal issue ilk 24 saatte `>=2 affected user`
- premium status / paywall / purchase restore için ilk 24 saatte `>=3` bağımsız kullanıcı raporu
- patch'ten sonra yeni bir auth veya story-open regression ailesi oluşması
- Crashlytics event'lerinde beklenen yeni telemetry key'lerinin sistematik olarak görünmemesi

## Zaman Penceresi Bazlı Karar

### 0-6 saat

Ana amaç:

- iOS fatal geri geliyor mu
- yeni build telemetry key'leri event'e düşüyor mu

Karar:

- `Future already completed` tekrar görünürse direkt `Yellow`, ikinci örnekte `Red`
- `nativePollOnce` event'lerinde yeni tanı alanları yoksa `Yellow`

### 6-24 saat

Ana amaç:

- RevenueCat startup ve attribute sync hacmi normalize oluyor mu
- support / QA tarafında kullanıcı etkisi var mı

Karar:

- `startup_set_user_type_failed` ve `revenuecat_sync_attributes_retryable_failure` baseline'a göre anlamlı düşüş göstermiyorsa rollout artmaz
- kullanıcı-facing premium/paywall şikayeti varsa doğrudan `Red`

### 24-72 saat

Ana amaç:

- trend gerçekten aşağı yönlü mü
- issue rename / regrouping yüzünden sahte iyileşme var mı

Karar:

- sadece issue title değil stack path ve `reason` alanı da izlenir
- eski issue düşüp yeni title yükseliyorsa bu "çözüldü" sayılmaz

## Rename / Regrouping Kontrolü

Şu eşleşmeler özellikle birlikte izlenmeli:

- `FirebaseService._downloadFileOnMainThread.<fn>` <-> aynı path altında farklı `Future already completed` title'ları
- `SplashScreenViewState._syncUserTypeAndRevenueInBackground` <-> `StartupDeferredTaskCoordinator._runTask` + `startup_set_user_type_failed`
- `SharedPreferenceService._performSpecial3DayPaywallSync` <-> `revenuecat_sync_attributes_retryable_failure`

Kural:

- Eski title kaybolup aynı kod yolu yeni title ile devam ediyorsa metrik düşmüş sayılmaz.

## Release Sonu Nihai Karar

72 saat sonunda:

- Tüm ana aileler `Green` ise rollout tamamlanır.
- En fazla 1 aile `Yellow`, geri kalanlar `Green` ise rollout sabit yüzdeyle sürer ve yeni feature release ertelenir.
- Herhangi 1 aile `Red` ise patch zorunlu kabul edilir.

## Önerilen Kısa Dashboard Görünümü

Tek ekranda şu 8 alan görünmeli:

- build version
- rollout oranı
- `Future already completed` affected users
- `startup_set_user_type_failed` affected users / 1k sessions
- `revenuecat_sync_attributes_retryable_failure` affected users / 1k sessions
- `nativePollOnce` affected users / 1k sessions
- yeni fatal issue var mı
- support'tan gelen premium/paywall regression var mı
