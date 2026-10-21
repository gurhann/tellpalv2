# P5c — Kalıcı çıktılar ve kısa sonuç

İki hikâyenin 41 görseli için 82 ayrı PNG/WebP kopyası üretildi. Orijinaller ve
canlı ortam değiştirilmedi. Ölçüler aynı kaldı; kayıplı sıkıştırma veya renk
dönüşümü yapılmadı. Her çıktıda bütün pikseller ve renk profili kaynakla eşleşti.

| Hikâye | Mevcut dosyalar | PNG kopyaları | Kayıpsız WebP kopyaları |
|---|---:|---:|---:|
| Kocaman Bir Sarılma | 6,19 MB | 28,53 MB | 19,40 MB |
| Pofuduk Mısır Pops | 120,31 MB | 91,94 MB | 70,61 MB |

MB burada 1.000.000 byte'tır. Kocaman Bir Sarılma'nın mevcut JPEG'leri bu
denemedeki iki alternatiften daha küçük. Pofuduk'ta PNG **%23,58**, WebP
**%41,31** daha küçük. Bunlar dosya boyutlarıdır; uygulamanın gerçek veri
transferi veya hızı ölçülmedi.

**Renk görünümü henüz onaylanmadı.** Pofuduk'un Display P3 profili aynen
korundu. Önizleme aracında WebP için ton farkı izlenimi oluştu. Dosyadaki
piksellerin/profilin eşit olması, görüntüleyicinin veya Flutter'ın bunları aynı
göstereceğini kanıtlamaz. WebP'nin uygulamadaki görünümü ayrıca doğrulanmalıdır.
Galeri HTML'si hazır; tarayıcı görünümü araç politikası nedeniyle doğrulanamadı.

## İlk koşu ve kalıcı yeniden üretim

İlk ölçüm başarılıydı; daha sonra geçici çıktı dizini bulunamadı. Nedeni
bilinmiyor. [İlk ölçüm](summary.json) değiştirilmeden tarihsel kayıt olarak
korundu. Dosyaları teslim edebilmek için aynı kod ve ayarlarla, izinli kalıcı
konuşma dizininde yeni bir özel koşu yapıldı. Bu konum repo dışındadır.

Yeni koşu **41 kaynak / 82 aday / 43 rol** ile tamamlandı. Bütün 82 adayın
SHA-256 değeri ilk ölçümdeki değerle aynı; galeri HTML'si de byte olarak aynı.
Bu nedenle önceki 12 dosyalık görsel inceleme aynı içeriklere aittir. Yeni
koşunun galerisinin tarayıcıda render edildiği iddia edilmez.

Kaynakların ve CSV/eşlemenin içerikleri değişmedi. Koşu sırasında önceki
274 kanıt dosyası ve önceden değişmiş 88 çalışma dosyası korundu. Üç uygulama
kaynağı için koşu öncesinde alınan SHA değerleri koşu sonrasında da aynı.
Yeni özel çıktı toplamı **337.364.858 byte**; 1 GiB sınırının altında. Adaylar,
kaynak kopyaları, galeri ve ham rapor kalıcı özel dizinde mevcut. Görseller,
eşleme ve özel yollar Git'e eklenmedi.

[Yeni koşunun sayısal kanıtı](rerun-summary.json), her kaynağın önce/sonra
hash'ini, 82 adayın ilk ölçümle eşitliğini ve tam boyut sonuçlarını içerir.
Özel ham rapor ve ön koşu kaydı ayrıca saklandı.

## Test ve inceleme

P5c'nin **25 testi**, P5b'nin **22 regresyon testi** geçti. Üç bağımsız inceleme
katmanı çalıştı. İki doğrulama açığı ek testlerle kapatıldı: sonradan değişen
girdinin başarıyı engellemesi ve tasarruf/büyüme hesaplarının doğruluğu.
Kontroller bellek içinde kasıtlı bozulduğunda yeni testler hataları yakaladı.

Yerel deney tamamlandı. Java/Flutter/CMS, P2 kabul kuralları ve canlı sistem
değişmedi. Bu sonuç üretim, cihaz veya görsel kalite kabulü değildir.
