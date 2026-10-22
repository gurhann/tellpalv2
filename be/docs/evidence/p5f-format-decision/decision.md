# P5f — İki hikâye için format kararı

**Kocaman Bir Sarılma'nın mevcut JPEG'leri korunacak. Pofuduk Mısır Pops için
kayıpsız WebP tercih edilen aday olacak.** Bu, iki hikâyenin mevcut dosyalarına
dayanan yerel format tercihidir. Canlı geçişi onaylamaz; sunucunun üretim kabulü
kapalıdır. Karar tarihi: 22 Ekim 2026.

## Boyut ve seçim

| Hikâye | Görsel | Orijinal | PNG adayı | Kayıpsız WebP adayı | Karar |
|---|---:|---:|---:|---:|---|
| Kocaman Bir Sarılma (#18) | 17 | 6,19 MB | 28,53 MB | 19,40 MB | Orijinal JPEG |
| Pofuduk Mısır Pops (#62) | 24 | 120,31 MB | 91,94 MB | 70,61 MB | Kayıpsız WebP adayı |

MB = 1.000.000 byte. Her görsel ayrı karşılaştırıldı: #18'de 17/17 JPEG,
#62'de 24/24 WebP en küçük seçenek. Toplamların gizlediği bir dosya istisnası yok.

Pofuduk'ta WebP **49.699.592 byte (%41,31)** kazandırıyor. PNG adayı da
%23,58 küçük; karşılaştırma ve ikinci aday olarak saklanıyor. WebP, bu PNG
adayından ayrıca 21.330.607 byte küçük. PNG'nin kullanıma alınması da aynı
renk, cihaz ve sunucu kabul koşullarına bağlı.

Bu seçim uygulanabilirse iki hikâyenin toplam dosya boyutu
**126.497.327 → 76.797.735 byte** olur: **%39,29 azalma**. Bunlar 41 benzersiz
dosyanın toplamlarıdır; gerçek indirme trafiği, cache veya depolama maliyeti
ölçümü değildir. Kocaman Bir Sarılma'yı yeniden kodlamak dosyaları büyütüyor.

## Görüntü hakkında ne biliyoruz?

[P5c](../p5c-local-lossless/rerun-verification.md) deneyinde 82 adayın tamamında
çözülmüş RGB pikselleri ve ICC renk profili kaynakla eşleşti. Çözünürlük
1712×2778 kaldı; kayıplı sıkıştırma ve renk dönüşümü uygulanmadı. Bu adımda
123 yerel dosya ve 41 orijinalin boyut/SHA-256 değerleri yeniden doğrulandı.

WebP'deki EXIF kaydı, kaynağın altı byte'lık `Exif\0\0` başlığını taşımıyor.
Başlık dışındaki EXIF içeriği ve okunan etiketler 41/41 çiftte aynı. ICC ve XMP
byte'ları da korunmuş. Bu nedenle rapordaki ham EXIF eşitsizliği içerik kaybı
olarak yorumlanmaz; bütün dosyanın byte olarak aynı olduğu da söylenmez.

[P5e](../p5e-iphone-offline/verification.md) ile iPhone'da dört seçili görselin
üç formatı, iki görünümde çevrimdışı açıldı; 24/24 kayıt mühürlendi. Kullanıcı
önceki incelemesinde fark görmediğini bildirdi. Son mühürlü oturum için ayrıca
yeni bir kalite beyanı alınmadı (`humanQualityAcceptance=NOT_RUN`). Bu kanıt
41 görselin tamamının veya fiziksel Display P3 renk doğruluğunun onayı değildir.

## Hız ve bellek sınırı

**WebP daha hızlı ilan edilmiyor.** [P5d](../p5d-offline-app/verification.md)
Android emülatörü ölçümünde Pofuduk'un iki örneği WebP'de daha yavaş çözüldü:

| Görsel | Orijinal | PNG | WebP |
|---|---:|---:|---:|
| Kapak (8303) | 321,251 ms | 247,795 ms | 379,475 ms |
| İlk sayfa (8306) | 329,683 ms | 269,755 ms | 531,351 ms |

Bunlar sabit sırayla çalıştırılmış üçer ölçümün medyanıdır; ısınma örnekleri
dışarıdadır. Dosya okuma süresinden ayrı decode ölçümüdür. Fiziksel telefonun
sayfa açılma süresini veya ağdan indirmeyle oluşacak toplam etkiyi göstermez.
WebP tercihi boyut kazancına dayanır; bu hız riski kapanmadan canlıya taşınamaz.

**Bellek kazancı kanıtlanmadı.** Aynı çözünürlükte tek tam RGBA8 görüntü tamponu
her format için teorik olarak 19.023.744 byte'tır. Bu hesap gerçek süreç/GPU
belleği veya tepe kullanım değildir. P5d'nin RSS örnekleri ölçümün ek readback
ve yazma işlerini de içerir; P5e'de hız/bellek ölçümü yoktur.

## Canlı geçişten önce kalanlar

1. Fiziksel cihazda hız ve bellek karşılaştırması yapılmalı. Kabul eşikleri
   ölçümden önce yazılmalı; son mühürlü oturumdan ayrı görsel kalite kabulü
   tamamlanmalı. Dosyadaki profil eşitliği, ekrandaki renk doğruluğu yerine geçmez.
2. Sunucuda P3/metadata içeriğini koruyan gerçek codec profili doğrulanmalı.
   [Mevcut P2](../../reading-image-preparation.md) yalnız dar sRGB girdilerini
   ve PNG çıktısını local/test kapsamında kabul ediyor; üretim yolu kapalı.
   Lossless WebP profili yok. Pofuduk'un P3/ICC bilgisi PNG adayında da bu
   sınıra takılıyor. JPEG'i koruma kararı da #18'in mevcut metadata'sının P2
   tarafından kabul edildiği anlamına gelmiyor. Sessiz profil silme veya sRGB
   dönüşümü bu kararın parçası değildir.
3. Bu koşullar sağlanınca kaynak/rol eşlemesi, değişmez yeni çıktı yolları ve
   geri dönüşü gösteren yazmasız geçiş provası hazırlanmalı. Canlı işlem ve
   daha geniş hikâye grubuna geçiş ayrıca onaylanmalı.

**P5f'nin yerel format tercihi tamamlandı; üretim geçişi `HOLD`.** Bu kayıt
genel üretim profillerini, ADR-0022/0023'ü veya P4 erişim/tüketici kapılarını
değiştirmez. Canlıya bağlanılmadı, orijinaller ve mevcut adaylar değiştirilmedi;
yeni dönüştürme, cihaz işlemi ve uygulama/sunucu kodu değişikliği yapılmadı.

Tam byte değerleri, seçilen 41 dosyanın hash'leri, kanıt kimlikleri ve kontrol
sonuçları [sayısal karar kaydında](summary.json) bulunur.
