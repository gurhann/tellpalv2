# P5f — iPhone ölçümü ve sunucu uyumluluğu

**Yerel tercih netleşti:** Kocaman Bir Sarılma için orijinal JPEG korunur.
Pofuduk için kayıpsız WebP aday olarak kalır; PNG ikinci seçenektir.
iPhone denemesinde Pofuduk WebP biraz daha yavaş, ölçülen süreç belleği daha
düşüktü. İki örnek de önceden belirlenen yerel sınırları geçti.

**Canlı geçiş bekliyor (`HOLD`).** Mevcut yeni görsel hazırlama hattı bu dosyaları
kabul etmiyor. Bu, canlıdaki hikâyelerin çalışmadığı anlamına gelmez. Canlıya,
orijinallere veya sunucunun kabul kurallarına dokunulmadı.

## iPhone sonucu

iPhone 12'de çevrimdışı Release uygulamasıyla **144 ölçüm + 12 ısınma** tamamlandı.
Her dosya 12 kez ölçüldü; format sıraları dengelendi. 159 olay ve son mühür
doğrulandı (`SEALED_VERIFIED`). Süre 158,89 saniyeydi.

Aşağıdaki değerler medyandır. “Hazırlama + çizim”, ayrı ölçülen decode ve ilk
framework çizim aralıklarının toplamıdır; telefondaki panel sunum süresi değildir.

| Görsel | Orijinal | PNG | WebP |
|---|---:|---:|---:|
| Kocaman Bir Sarılma, kapak | 75,95 ms | 90,55 ms | 92,19 ms |
| Kocaman Bir Sarılma, sayfa | 75,09 ms | 73,71 ms | 75,12 ms |
| Pofuduk, kapak | 157,33 ms | 143,59 ms | 171,98 ms |
| Pofuduk, sayfa | 172,84 ms | 157,45 ms | 189,58 ms |

Pofuduk WebP'de bu süre orijinale göre **%9,3 ve %9,7 arttı**. Yalnız decode
artışı da **%7,8 ve %13,5** oldu. İkisi de yerel %20 sınırının içinde kaldı.
PNG bu iki örnekte daha hızlıydı; WebP'nin önceki dosya boyutu avantajı korunuyor
([P5f boyut kararı](../p5f-format-decision/decision.md)). Bu nedenle tercih
“WebP en hızlıdır” iddiasına dayanmıyor.

Pofuduk WebP'nin örneklenmiş süreç maksimumlarının medyanı orijinale göre
**32,7 MiB ve 31,4 MiB daha düşüktü**. Yerel bellek sınırı da geçti. Bu değer
tüm uygulama sürecini kapsar; görselin tek başına belleği, GPU belleği veya gerçek
tepe bellek değildir. Oturumdaki en yüksek örnek 194,94 MiB; en büyük örnekleme
boşluğu 11,305 ms idi.

**Sınır:** Dört görsel ve sıcak yerel dosyalar ölçüldü. HTTP indirme, gerçek
okuyucu gezinmesi, Android fiziksel cihazı ve P3 panel renk doğruluğu bu denemede
doğrulanmadı. Sonuç üretim profili veya bütün cihazlar için performans onayı değildir.

[Sayısal telefon kaydı](iphone-performance.json), ayrı süreleri, min/max değerleri,
aynı turdaki format farklarını ve kaynak/mühür kimliklerini içerir.

## Yerel sunucu sonucu

Taze derlenen gerçek `ReadingImageInspector`, P5c'nin 123 pinli dosyasını
salt okuyarak kontrol etti. **0 kabul / 123 ret.** Yerel format tercihindeki
41 dosyanın tamamı da ret aldı. WebP ve korunmuş renk/metadata bilgileri mevcut
motorun dar kapsamı dışında kalıyor. Dosyalar değiştirilmedi.

| Dosya grubu | Sayı | Gerçek ilk ret |
|---|---:|---|
| Kocaman Bir Sarılma, orijinal JPEG | 17 | `JPEG_UNSUPPORTED_METADATA_OR_ORIENTATION` |
| Aynı hikâyenin PNG adayı | 17 | `PNG_UNSUPPORTED_CHUNK:iTXt` |
| İki hikâyenin WebP adayı | 41 | `PNG_OR_JPEG_REQUIRED` |
| Pofuduk, orijinal PNG | 24 | `PNG_UNSUPPORTED_HEADER` |
| Pofuduk, PNG adayı | 24 | `PNG_UNSUPPORTED_CHUNK:iCCP` |

JPEG/PNG ImageIO okuyucuları mevcut, WebP okuyucusu yok. Okuyucu eklenmesi
tek başına inspector veya profil kabulünü değiştirmez. Deneme preparation
child/process, Spring, DB veya storage başlatmadı; dönüştürme yapmadı.

**Kanıt sınırı:** macOS/aarch64, Oracle OpenJDK `25-loom+1-11` kullanıldı.
Üretim Java imajı çalıştırılmadı; yerel Docker servisi kapalıydı. Bu sonuç
üretim imajının codec/renk davranışını doğrulamaz. Üretim kabulü kapalıdır.

Dört hedefli Java testi geçti; desteklenen sentetik RGB PNG gerçek inspector
ile kabul edildi. Yanlış pin, eksik dosya ve tekrar dosya manifestleri gerçek
CLI'da reddedildi; kısmi başarı raporu üretilmedi. İlk Maven çağrısı wrapper'ın
çalıştırma izni nedeniyle başlayamadı; aynı dosya değiştirilmeden `sh ./mvnw`
ile ağ engelli ve offline çalıştırıldı: 4 test, 0 hata, 0 skip.

[Sayısal sunucu kaydı](server-compatibility.json), dosya sonuçlarını, kaynak ve
çalıştırılan class hash'lerini, JVM binary kimliğini ve doğrulama sonuçlarını tutar.

## Koruma ve doğrulama

Eski QA verisi iki bağımsız kopyayla doğrulandı: 73 dosya, 8.218.019 byte.
Aynı deneme paketi güncellendi. iOS'un geliştirici sertifikası doğrulaması
ölçümden önce tamamlandı; ölçüm native kayıtta ağ `unsatisfied`, termal durum
`nominal`, düşük güç kapalı koşulunda yürüdü. Normal uygulama açılmadı.

Ölçüm sonrası 3.495 çalışma dosyası ve 1.106 eski kanıt girdisinin koruması geçti.
Bu kapsam P5c'nin 123 medyasını ve raporunu tam SHA ile içerir. Ayrıca 41 orijinal
dosya ile önceden bulunan 88 çalışma dosyası yeniden kontrol edildi; değişmedi.
Ham medya, uygulama verisi yedekleri ve günlükler Git dışında tutuldu.

24 P5f ve 15 P5e Python testi, 27 Flutter testi ve bir opt-in başlangıç testi geçti.
Dart analyze ve beş Swift host dosyasının typecheck'i temiz. Gerçek ölçüm
döngüsünü baştan sona yürüten test, önce ilk ısınmada duran iptal token'ı hatasını
üretti; düzeltmeden sonra 156 turu, tek mührü ve sıfır açık rasterı doğruladı.
Ölçülen son paket bu düzeltmeyi içerir. Eksik kayıt, yanlış mühür, sayaç/frame
hatası, kesinti, süre/bellek sınırı ve bozuk yedek retleri de sınandı.

Üç bağımsız incelemenin ardından hata ve kurulum korumaları sıkılaştırıldı.
Pilot bütçesinin eşik altı/eşit/üstü sonuçları, gerçek ekranın paint callback'i
ve gerçek native sampler'ın termal/düşük güç kesintileri test edildi. Native
kesinti testi Mac'te geçici UIKit/Flutter dublörleriyle aynı Swift kaynaklarını
çalıştırır; telefonda zorla termal/düşük güç kesintisi üretildiği iddiası değildir.

**Ölçülen paket ile son kaynak farkı:** İnceleme yamaları telefona kurulmadı;
yeni fiziksel ölçüm yapılmadı. Ölçülen artifact ve ham kayıt değişmedi. Aynı
ham kayıt sıkılaştırılmış süre doğrulayıcısından tekrar `SEALED_VERIFIED` geçti.
Ek korumalar testlerle doğrulandı; bu fark telefon performans sayıları için yeni
paketin ölçüldüğü şeklinde yorumlanmamalı. [İnceleme kaydı](review.md).
