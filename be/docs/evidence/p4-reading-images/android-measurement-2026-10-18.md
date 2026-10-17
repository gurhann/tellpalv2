# #94 Android emülatör karşılaştırması

18 Ekim 2026: **Altı görüntü durumu ve kapanış doğrulandı.** Mevcut #94 için
kapak/detail ile sıralı iki sayfanın canonical PNG kaynağı ve 822×1334 PNG çıktısı
aynı widget alanında gösterildi. Yeni üretim yapılmadı. Fiziksel cihaz kalite kabulü
ve genel P4 tamamlanmış değildir.

[Sayısal kanıt](android-measurement-2026-10-18/verification.json), seçilen rendition,
SHA-256, kimlik, gerçek HTTP payload, decode boyutu, tamamlanan paint, bellek örnekleri,
yaşam döngüsü ve toplam sayaçları içerir. Token, özel URL ve storage anahtarı içermez.

## Yetki ve kapsam

Kullanıcı “180 saniye ve gerekirse 1 ek giriş onaylı” dedi. Önceki oturumun süresi
yetersizdi. Yerel kapılar geçtikten sonra aynı kanonik QA hesabına yalnız bir ek
giriş yapıldı; tüm denemelerde toplam giriş sayısı 2 oldu. Eski sayaçlar sıfırlanmadı.

Kod commit'i: `5203084cb31633a8b745345b3bbb4fa90788cd7e`.
Run: `p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`.
İlk JSON planının SHA-256 değeri değişmedi:
`f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3`.

Ek pencere, yerel LIVE ve oturum kapılarından sonra 00.18.48,426'da açıldı;
son tarihi 00.21.48,426 idi (Europe/Istanbul). Başarılı kanıt 00.20.56,351'de,
pencere açılışından 127.925 ms sonra yazıldı. İlk 900 saniyelik pencere değiştirilmedi.
Yerel kontroller, hazırlık ve altı durum için kullanılan süre bu ek pencereye dahildir.

Ölçüm ayrı debug QA paketinde, API 36 ARM64 Android emülatöründe yapıldı:
822×1828 ekran, 320 dpi, DPR 2, font ölçeği 1. Fiziksel Android veya iPhone kullanılmadı.
Normal uygulamanın paketi, Library/detail/reader, ses ve cache kodu değiştirilmedi.

## Kimlik ve geometri

Tüm durumlar #94 / `tr`, aynı job, revision, manifest ve LIVE generation 4 ile geçti.
Job: `bb9ae40b-e5ab-4054-885e-0b1fc14e841e`.
Revision: `3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d`.
Manifest: `d4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2`.

| Alan | Gerçek mantıksal alan | DPR | Fit | Gerekli tam raster |
|---|---:|---:|---|---:|
| Kapak/detail (`readingDetail`) | 411×456,667 | 2 | cover | 822×1334 |
| Sayfa 1 ve 2 (`readingPage`) | 411×694 | 2 | contain | 822×1334 |

Sayfa kutusu fiziksel olarak 822×1388'dir. `contain`, görsel oranını koruyarak
yaklaşık 822×1334 alan boyar. `cover`, tam rasterın bir bölümünü 822×914 alanda
gösterir. Seçilen çıktı her iki eksendeki raster ihtiyacını karşıladı.
Kaynakların tamamı 1712×2778, çıktıların tamamı 822×1334 olarak decode edildi.
Kaynak gösterimi yalnız açık QA seçimiyle yapıldı; hata halinde original fallback yoktu.

Bu fixture her rol için tek uygun çıktı taşır. Bu ölçüm, çoklu üretim profilleri
arasında en küçük varyant seçiminin cihaz matrisi kanıtı değildir. Ayrı `readingCover`
çıktısı backend'de vardır; bu altı durum yalnız detail ve iki sayfayı gösterir.

## Gerçek payload ve süre

| Sıra / gösterim | Alınan payload (byte) | HTTP (ms) | Yükleme (ms) | Transfer sonrası loaded üst sınırı (ms) | PSS / RSS (KiB) |
|---|---:|---:|---:|---:|---:|
| 1. Detail PNG çıktı | 3.292.721 | 1.866 | 3.840 | 347 | 205.213 / 194.152 |
| 2. Detail PNG kaynak | 6.336.857 | 2.314 | 3.043 | 679 | 209.752 / 197.344 |
| 3. Sayfa 1 PNG kaynak | 5.941.567 | 2.112 | 2.705 | 579 | 225.645 / 213.024 |
| 4. Sayfa 1 PNG çıktı | 3.292.721 | 1.479 | 1.625 | 132 | 204.923 / 192.396 |
| 5. Sayfa 2 PNG çıktı | 3.292.721 | 1.442 | 1.696 | 242 | 206.821 / 194.300 |
| 6. Sayfa 2 PNG kaynak | 6.122.778 | 1.886 | 2.447 | 532 | 220.313 / 207.828 |

İlk yükleme manifest alımını da içerir. Transfer sonrası değer checksum, buffer ve
codec işlemlerini birlikte kapsar; saf decode süresi değildir. Tek örnekle hız veya
üretim performansı sonucu çıkarılmaz. HTTP süreleri yerel backend üzerinden gerçek
teslimat yolunu içerir. Byte değerleri istemcinin aldığı gövdedir; TLS/header veya SDK
iç çağrılarının toplamı değildir.

Üç çıktı toplam **9.878.163 byte**, üç canonical kaynak **18.401.202 byte** aktardı.
Kaynak PNG tabanına göre azalma **%46,32** oldu. Altı görsel toplamı **28.279.365 byte**;
bir manifest ve iki status gövdesiyle gözlenen dokuz HTTP isteği **28.294.926 byte** aldı.

Eski JPEG metadata toplamı **3.293.215 byte** idi. Yeni PNG toplamı bunun yaklaşık
**3 katıdır**. Eski JPEG bu cihazda karşılaştırma kolu olarak gösterilmedi.
Canonical PNG'ye göre azalma, eski JPEG'e göre transfer kazancı sayılmaz.

## Ekran kanıtları

Altı ekran görüntüsü görsel olarak incelendi; beklenen hikâye, kapak ve sıralı sayfalar
görünüyor. Bu inceleme kullanıcının metin netliği kabulü veya fiziksel cihaz onayı değildir.

| Alan | Canonical PNG kaynak | Doğrulanmış PNG çıktı |
|---|---|---|
| Kapak/detail | [Kaynak](android-measurement-2026-10-18/02-entry-1-source.png) | [822×1334](android-measurement-2026-10-18/01-entry-1-rendition.png) |
| Sayfa 1 | [Kaynak](android-measurement-2026-10-18/03-entry-2-source.png) | [822×1334](android-measurement-2026-10-18/04-entry-2-rendition.png) |
| Sayfa 2 | [Kaynak](android-measurement-2026-10-18/06-entry-3-source.png) | [822×1334](android-measurement-2026-10-18/05-entry-3-rendition.png) |

## Kaynak kullanımı ve kapanış

Her tamamlanan render'da sahip olunan image sayısı 1; buffer, descriptor ve codec
sayaçları 0'dı. Arka plana geçiş, geri dönüş ve stop sonrası dördü de 0 oldu.
Geri dönüş yeniden kabul gerektiren hata durumunda kaldı; otomatik medya isteği
yapmadı. Üç yaşam döngüsü anında HTTP olay sayısı 9 olarak kaldı.

Başlangıç debug süreç örneği PSS/RSS 175.291/201.144 KiB; kapanış örneği
207.602/196.144 KiB idi. Tablo anlık süreç örneklerini gösterir. Yakalanmış tepe
bellek, GPU toplamı, kaçak testi veya üretim bellek sınırı ölçümü değildir.
Seri altı durum, P4b'nin genel transfer/decode eşzamanlılık kapasitesini kanıtlamaz.

Stop sonrası credential ve deadline kapandı. VM stop/socket, ADB reverse ve forward
temizliği geçti. [Son ekran](android-measurement-2026-10-18/07-closed.png) kapalı QA
durumunu gösterir. Yalnız bize ait backend, child `close.request` ile kapandı;
ledger `CLOSED MEDIA_CONTINUATION_RELEASED` kaydetti ve PID'nin bittiği doğrulandı.
Test için açılan Colima, konteyner kalmadığı doğrulandıktan sonra kapatıldı.
Docker bağlamı `desktop-linux` olarak korundu. Diğer backend/frontend süreçleri durdurulmadı.

## Kümülatif sınırlar ve korunan kayıtlar

| Sayaç | Önceki | Son / sınır |
|---|---:|---:|
| Create denemesi | 14 | 14 / 14 |
| Yazma payload | 74.711.347 | 74.711.347 / 117.440.512 byte |
| Farklı yeni nesne | 13 | 13 / 13 |
| Farklı nesne byte | 68.374.490 | 68.374.490 / 109.051.904 byte |
| Pinned read | 41 | 47 / 64 |
| Okuma payload | 215.362.417 | 243.641.782 / 536.870.912 byte |
| Observe | 8 | 8 / 16 |
| Verifier, login doğrulamaları dahil | 4 | 20 / 64 |
| QA sign-in | 1 | 2 / 2 (ek açık onay) |
| Storage mantıksal çağrı | 63 | 69 / 94 |
| Birleşik mantıksal çağrı | 68 | 91 / 159 |
| V37 teslimat işlemi | 4 | 25 / 64 |
| V37 teslimat byte rezervasyonu | 3.292.721 | 31.572.086 / 67.108.864 byte |

Mantıksal çağrı, HTTP isteği ve faturalandırılan işlem farklı sayımlardır. Verifier
sayısı önceki girişleri içerir; birleşik toplam ayrıca sign-in çağrılarını sayar.
Bu pencere yalnız 6 pinned read ekledi; create/observe/worker/publish çalışmadı.

Son salt okunur DB denetimi kaynak V36, klon V37 ve iki DB'de legacy #112 PENDING
sonucunu verdi. Önceden bulunan satırlar 11 iş tablosunda, JSON ve UTC zamanlarıyla
yapısal olarak karşılaştırıldı; hepsi korundu. #94 aynı job ve LIVE generation 4'te kaldı.

Parent ledger SHA-256 değeri değişmedi:
`60086ea1ad1bdccbcc2423481fcd5a20cd4777602c3e87b1c3636732b298b839`.
Yeni child ledger bu dosyanın byte'larıyla başlar. Önceki Java hata log/PID/ledger
hash'leri ve ilgisiz `.memlog.md` hash'i tekrar eşleşti. Önceki başarısız mobil ve
süre kapısı kayıtları korundu; başarı kaydı onların üzerine yazılmadı.

## Yerel doğrulama ve açık kapılar

Gerçek ek girişten önce tam Maven paketi **490 testle**, Python mobil 27 ve launcher 13
testle geçti. Üç BMad inceleme katmanı ve hedefli yeniden incelemeler açık bulgu bırakmadı.
Flutter kaynakları ve APK bu ek pencerede değiştirilmedi.

Bu dilimin tamamlanan sonucu mevcut #94'ün emülatörde gerçek teslimat ve görüntü
karşılaştırmasıdır. Sıradaki kalite adımı kayıtlı kaynak/çıktı çiftlerini incelemek ve
aynı aday profili fiziksel Android/iPhone üzerinde doğrulamaktır. Yeni üretim gerekmez;
yeni oturum veya ağ penceresi gerektiğinde ayrı somut sınırlar belirlenir.

Genel erişim politikası, medya teslim yöntemi, URL yenileme, HTTP cache,
session/retention, üretim profilleri ve P4b karara bağlı kalır. Hesap değişimi,
URL süresi dolması, precache/display tutarlılığı ve normal Library/detail/reader
entegrasyonu bu altı durumla tamamlanmış sayılmaz. Android'deki önceki 0,75× bulanıklık,
1× “net” yorumu ve iPhone'daki bazı 0,75× kabulleri genel profile dönüştürülmedi.
JPEG/WebP bulanıklığı ve P0'ın eksik ham kanıt sınırlaması açıktır.
