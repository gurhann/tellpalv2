# MAC_EK alan incelemesi

Tarih: 2026-10-17. Ölçüm salt okunurdur. Kaynaklar: `diskutil info`, `diskutil list`, `df` ve `du`.

## Kapasite ve bölüm düzeni

Fiziksel USB disk `480,1 GB` boyutundadır. `MAC_EK`, bunun yalnız `118,5 GB` boyutundaki
Journaled HFS+ bölümüdür. Ölçüm sırasında kullanılan alan `117,8 GB`, boş alan yaklaşık
`715 MB` idi. `df` aynı boşluğu yaklaşık `682 MiB` gösterdi; birim farkı korunur.

Diskte ayrıca yaklaşık `360,1 GB` bölümlendirilmemiş alan vardır. Bu alan `MAC_EK` dosya sistemine
dahil değildir. Arada `829,4 MB` boyutunda Windows Recovery bölümü bulunur. Diskin kalan boşluğunu
kullanmak ayrı bir bölüm düzenleme işi gerektirir. Bu incelemede bölüm, dosya veya veri değiştirilmedi.

## Erişilebilen klasörlerin kullanımı

Boyutlar `du` tahsis edilmiş blok ölçümünden GiB olarak yuvarlanmıştır; dosya içerikleri okunmadı.

| Klasör / içerik | GiB |
|---|---:|
| `dev/android_sdk` | 24,7 |
| `dev/avd` | 7,8 |
| `dev/tellpal-api33-20261013-sh1cpcet` | 12,5 |
| `dev/tellpal-host-caches-r307` | 5,7 |
| `dev/tellpal-plugin-registration-20261013` | 12,1 |
| `dev/tellpal-reader-extension-evidence-20261015` | 2,3 |
| `gradle` | 12,2 |
| `gradle-cache` | 12,0 |
| `alpamis` | 8,2 |
| `Yeni Sesli Hikayeler` | 5,0 |
| `Android Studio.app` | 2,8 |
| `Docker.app` | 1,8 |
| `langflow` | 0,8 |

`dev` toplamı yaklaşık `65,1 GiB` olur. Geçmiş TellPal klasörleri birlikte yaklaşık `32,5 GiB`,
iki Gradle klasörü yaklaşık `24,2 GiB` tutar. Adlarında cache veya test bulunması güvenle
silinebilecekleri anlamına gelmez. Kanıt, credential, imzalama ve yeniden üretim bağımlılıkları
bu boyut taramasında sınıflandırılmadı.

`.Spotlight-V100`, `.TemporaryItems` ve `.Trashes` için macOS erişim reddetti. İçerikleri ve
boyutları ölçülmüş sayılmaz. Görülebilen klasörlerin toplamı tüm dosya sistemi kullanımına
eşit kabul edilmez. Hiçbir klasör silinmedi veya taşınmadı.
