# P5d — Uygulamada çevrimdışı görsel karşılaştırması

**Kocaman Bir Sarılma** ve **Pofuduk Mısır Pops** hikâyelerinin dört görseli,
original, PNG ve kayıpsız WebP sürümleriyle ayrı Android QA paketinde
karşılaştırıldı. **12 durum ve 48 örnek tek oturumda tamamlandı.** Her durum için
bir ısınma ve üç ölçüm yapıldı. Bütün ölçümlerde gerçek Flutter frame süresi elde
edildi; 24 slot/yazı kesiti görüntüsü doğrulandı.

Bu emülatörün aynı Flutter backend'inde original↔PNG ve PNG↔WebP arasında
**iki native readback türünde de fark bulunmadı**. Bu, fiziksel Display P3 veya
insan kalite kabulü değildir. Bu iki kabul ve üretim profili kararı `NOT_RUN` kaldı.
Hız kazancı ya da bütün hikâyeler için sonuç çıkarılmadı.

## Kapsam ve sonuç

| İçerik / görsel | Rol | Orijinal | Dosyadaki ICC | Original↔PNG | PNG↔WebP |
|---|---|---|---|---|---|
| #18 / 1950 | Kapak | JPEG | Yok | 0 fark | 0 fark |
| #18 / 1952 | İlk sayfa | JPEG | Yok | 0 fark | 0 fark |
| #62 / 8303 | Kapak | PNG | Display P3 | 0 fark | 0 fark |
| #62 / 8306 | İlk sayfa | PNG | Display P3 | 0 fark | 0 fark |

Bütün dosyalar **1712×2778** boyutunda açıldı. Her karşılaştırmada `rawRgba` ve
`rawExtendedRgba128` için ayrı ayrı 19.023.744 kanal karşılaştırıldı. Sekiz
karşılaştırmanın tamamında değişen kanal, en büyük mutlak fark ve ortalama mutlak
fark sıfırdı. Tripletlerin native SHA-256 değerleri de eşleşti. Pillow RGB çıktısı
Flutter renk doğrusu olarak kullanılmadı; JPEG decoder farkı varsayılarak sonuç
silinmedi.

Kaynakta Display P3 ICC bulunmasına rağmen bütün 48 Flutter decode kaydı
`colorSpace=sRGB` döndürdü. Bu yüzden sonuç bu Skia/sRGB yoluna aittir; geniş gamut
panelde renk korunumu kanıtlanmış değildir.

## Ortam ve paket sınırı

- Koşu: `63b8351d1cf4`; tarih: 21 Ekim 2026; sonuç: `MEASURED`.
- Flutter 3.41.1, Dart 3.11.0; engine `3452d735bd38224ef2db85ca763d862d6326b17f`.
- Release, Skia; `EnableImpeller=false` ve aynı PID'nin opt-out logu doğrulandı.
- Yalnız `emulator-5554`, Android API 36, `arm64-v8a`, DPR 2.
- Paketler: `com.tellpal.p5d.offline` ve `com.tellpal.p5d.offline.test`.
- İki APK'da kurulum öncesi manifest/DEX denetimi geçti: INTERNET izni ve provider
  yok. Minimal Dart bağımlılık kapanışında native plugin yok; host plugin kaydetmiyor.
- Normal main/auth kompozisyonu, normal uygulama paketi, backend, Firebase, storage
  ve dağıtım değiştirilmedi. HTTP transferi ölçülmedi.

Pub ve Gradle offline çalıştı. Gradle repository metadata'sı aynı mevcut dosyaları
ilk denemede seçemediği için tam aynı engine/embedding, Guava ve AndroidX
koordinatlarının mevcut JAR/AAR/POM dosyaları özel yerel Maven deposuna kopyalandı.
Cache dosyasının SHA-1 dizin adı ve POM koordinatı kontrol edildi; SHA-256 makbuza
alındı. Sürüm değiştirilmedi, bağımlılık indirilmedi, global cache değiştirilmedi.
Mevcut Java 17, Android SDK ve debug signing kullanıldı; kurulum yapılmadı.

## Ekran ve gerçek 1:1 yazı kesiti

Kapaklar ortak detail viewport'ta `cover`, sayfalar `contain` ile çizildi. Her
triplette slot, DPR ve `FilterQuality.low` aynıydı. Kapak slotu 411×456,667;
sayfa slotu 411×866 logical pikseldi.

Yazı kesitleri `FilterQuality.none` ile çizildi. Kaynak başlangıçları aşağıdadır;
üç format aynı koordinatı kullandı. Bu ekranda kesit **822×600 kaynak pikseli →
822×600 fiziksel piksel** oldu.

| Görsel | Kaynak X | Kaynak Y | Görülen yazı alanı |
|---|---:|---:|---|
| 1950 | 400 | 760 | Kapak başlığının büyük harfleri |
| 1952 | 440 | 2160 | İlk sayfanın metin satırları |
| 8303 | 240 | 1410 | Kapak başlığının büyük harfleri |
| 8306 | 240 | 240 | Üst soldaki metin |

Native yakalama `PixelCopy` ile Flutter `SurfaceView` yüzeyinden, aynı fiziksel
822×1828 boyutta yapıldı. Android sistem katmanları bu kopyaya dahil değildir.
FlutterView/SurfaceView offset'i sıfırdı. Aynı Flutter frame'inde çizilen 64 piksellik
işaret; epoch, durum ve slot/crop bağını doğruladı. Host bu işaretin bütün piksellerini,
yüzey boyutunu, offset'i ve ROI geometrisini tekrar kontrol etti.

Asıl host doğrulama yolunda her crop PNG'nin fiziksel yazı dikdörtgeni, aynı koşunun SHA doğrulanmış
`rawRgba` tamponundaki kaynak ROI ile karşılaştırıldı. **12 kesitin her birinde
1.972.800 kanal birebir eşleşti; değişen kanal ve maksimum fark sıfırdı.** Böylece
1:1 ölçek ve kaynak konumu yalnız metadata veya farklı screenshot hash'ine dayanmadı.

Görüntü incelemesinde dört kaynağın yazı alanı görüldü; P3 kaynakların original/WebP
kapak ve sayfasında aynı harf ve renk görünümü gözlendi. Bu sınırlı gözlem, native
sayısal eşitlikten ayrı tutulur. İnsan kalite kabulü `NOT_RUN`'dır.

## Ölçümün anlamı

Aşağıdaki değerler 12 ısınma dışarıda bırakılarak 36 örneğin medyanı ve aralığıdır.
Süreler milisaniyedir; formatlar arasında hız sıralaması yapılmadı.

| Aşama | Medyan | En küçük–en büyük |
|---|---:|---:|
| Asset bundle okuma | 1,236 | 0,292–3,883 |
| Encoded SHA/ICC denetimi | 76,809 | 2,634–312,754 |
| Decode | 212,153 | 29,912–562,118 |
| rawRgba readback | 118,306 | 18,494–208,607 |
| rawExtendedRgba128 readback | 278,756 | 47,436–556,067 |
| FrameTiming build | 0,613 | 0,105–7,396 |
| FrameTiming raster | 4,681 | 0,759–26,424 |
| FrameTiming totalSpan | 13,475 | 1,389–92,950 |

Encoded checksum her 48 örnekte doğrulandı ve ölçüldü. İki readback API'si her
örnekte çağrıldı. Tam native SHA ve raw dosya yazımı yalnız üçüncü ölçümde yapıldı;
ısınma ve ara ölçümlerin ek raw checksum'u `NOT_MEASURED`, yazımı `NOT_RUN` ve
süre/hash alanları `null` oldu. Ölçülmeyen iş için süre veya hash üretilmedi.
Native checksum, yazım ve readback süreleri özel makbuzda ayrı alanlardır.

FrameTiming, boyanan gerçek `frameNumber` ve epoch ile bağlandı. Engine/framework
vsync zamanları ve farkları ayrı saklandı; iki zaman alanının mutlak eşitliği
varsayılmadı. `frameWaitMicros` medyanı 1025,832 ms'dir; release timing callback'inin
toplu teslimini bekler. Bu değer çizim veya açılma süresi değildir. Önceki sabit
150 ms bekleme çizim ölçümü olarak kullanılmadı.

RSS anlık süreç örneğidir. Isınma dışındaki önce/yüklü/sonra aralıkları sırasıyla
139,12–226,60 / 137,63–225,12 / 138,50–225,50 MiB oldu. Yüklü RSS readback, checksum
ve varsa dosya yazımını da içerir. Bunlar peak veya GPU belleği değildir. **192 MiB
tek iş için modellenen tampon bütçesidir; süreç RSS sınırı değildir.** Encoded sınır
32 MiB/dosya, piksel sınırı 4.756.000'dır. Son durumda image/buffer/descriptor/codec
sahiplik sayaçlarının tamamı sıfırdı.

## Kanıt ve koruma

Son özel dizin; runtime, 24 PNG, 24 raw dosya, APK denetimleri, renderer logu,
önce/sonra hash envanteri ve sonuç makbuzunu içerir. Görseller ve özel yollar Git'e
alınmadı. Temiz özet bu belgedir.

24 raw dosyanın toplamı 1.141.424.640 byte'tır. Yeniden ADB transferi yerine
`125c2f1c1b07` kapalı koşusunun bağımsız host kopyaları kullanıldı. **Her dosya yeni
native runtime'ın SHA-256 ve byte boyutuyla tekrar eşleştirildi**, bağımsız kopya
oluşturuldu ve hedef yeniden doğrulandı. Yeni 48 örneğin runtime'ı ve 24 PNG'si yeni
oturumdan geldi; eski/yarım ölçümler birleştirilmedi. Reuse kaynak/SHA bağı ayrı
özel makbuzdadır.

Kurulum öncesi boş alan 2.366.672.896 byte'tı. Guard, raw dataset + iki APK
46.173.336 byte + 256 MiB marjı ve en az 1,5 GiB boşluğu denetledi. Düşük alanda
kurulum başlamaz. Yalnız bu işte üretilmiş, bağımsız host kopyası SHA/boyutla
doğrulanmış cihaz raw kopyaları temizlendi. Kısmi dosyalar `PARTIAL` olarak arşivlendi;
başarı kanıtı sayılmadı. Eski P4/normal paketler ve host kanıtları korunmuştur.

Son koşuda 3.451 repo dosyasının ve 13 P5c girdisinin önce/sonra SHA envanteri aynıydı.
Başlangıçtaki 88 değişmiş/untracked dosya ayrıca SHA ile değişmeden doğrulandı.
Bağımsız koruma denetimi P5c'nin 123 dosyasını ve 41 orijinali de doğruladı.
Özel çıktı kökündeki 85.633 eski dosyanın boyut/mtime denetimi geçti. Yalnız bilinen
aktif emülatör stderr logu için önce boyut ve prefix SHA alındı; son koşuda eklenen
byte sıfırdı. Bütün eski QA dosyalarına tam SHA denetimi yapıldığı iddia edilmez.

Önceki denemelerde stale screenshot, boş timing, disk doluluğu, 30 saniye watchdog ve
ADB bağlantısı/UiAutomation `DeadObjectException` görüldü. Bunlar başarı kapsamından
çıkarıldı ve korundu. İlk stale koşunun aktif logu için başlangıç prefix SHA'sı yoktu;
sonradan alınan hash geriye dönük koruma ispatı sayılmadı. Son sürüm UiAutomation
kullanmaz. ADB read/pull/force-stop hataları sınırlı tekrar edilir; install/instrument
körlemesine tekrarlanmaz. Primary hata, cleanup hatası ve preservation sonucu ayrı
saklanır; cleanup hatası sonuç makbuzunu engellemez.

Sonuç makbuzu SHA-256:
`bd066b287ae51736bb19506a65ca46e84e10ee7d31b6ca196dffaa7c3eb1fe9a`.
ROI piksel makbuzu SHA-256:
`064d84cfdbbbd234f241975ac922ea3816b921308362c044cce4987aec517213`.

## Testler ve matris eşlemesi

Python **26/26**, hedefli Flutter **12/12** geçti. Hedefli Flutter analyzer temizdir.
Freezed/injectable çıktıları mevcut generator'larla üretildi. P5d DI binding'leri
normal uygulamanın tarama köklerinin dışında tutuldu; normal generated dosyaların
istenmeyen değişiklikleri geri alındı ve başlangıç içerikleri korundu.

| Spec matrisi | Doğrulama |
|---|---|
| Dosya/hash/ICC/boyut uyuşmazlığı | Yanlış report, eksik dosya, geçerli dosyada ayrı SHA/boyut/ICC/encoded sınır retleri; bozuk WebP chunk retleri |
| Cache/araç eksikliği | Geçici boş gerçek cache diziniyle exact JAR guard'ına ulaşıldı; ret üretildi, subprocess/download/install çağrılmadı |
| Ağ izni/provider/plugin | Sentetik APK negatif denetimleri ve iki gerçek APK'nın kurulum öncesi denetimi |
| Eşzamanlı seçim/background | Geç sonuç boyanmaz, ikinci decode başlamaz, bekleyen iş iptal edilir; gerçek oturum kaynak sayaçları sıfır |
| Renk/readback farkı | Farklı sentetik native tamponlarda gerçek delta raporu; insan/P3 kabulü `NOT_RUN`; format geometrisi değişikliği ret |

Ek testler yanlış mode/epoch/marker piksellerini, yüzey boyutunu/offset'ini, eski veya
boş timing'i, düşük disk alanını, yalnız aynı native hash ile bağımsız readback
kopyasını ve cleanup transport hatasında sonuç makbuzunun korunmasını kapsar.

Çalıştırılan hedefli kontroller:

```sh
python3 -m unittest discover -s tools/image-optimization -p test_p5d_offline_app.py
cd app/mobile
flutter --no-version-check --suppress-analytics test --no-pub test/reading_images/p5d_offline_test.dart
flutter --no-version-check --suppress-analytics analyze --no-pub lib/features/reading_images/p5d_offline lib/app/p5d_offline lib/app/p5d_offline_qa.dart lib/p5d_offline_bindings test/reading_images/p5d_offline_test.dart
```

Emülatör koşusu repo kökünden `python3 tools/image-optimization/p5d_offline_app.py
--report <doğrulanmış-P5c-report.json>` ile yapıldı. Çıktı kökü ve cihaz seçimi kodda
sabit; tekrar çalıştırma benzersiz özel dizin oluşturur. Eksik cache/signing/SDK veya
disk alanı halinde guard durur; indirme/kurulum başlatmaz.


## İnceleme sonrası PATCH doğrulaması

Altı inceleme bulgusu aynı kapsam içinde kapatıldı. Native yakalama, raster/decode,
readback ve çizim davranışı değiştirilmedi; emülatör yeniden çalıştırılmadı.
Observer kaydı, main'in de kullandığı aynı kompozisyon helper'ına taşındı. Test
constructor'ı release'te kapalıdır; üretim constructor'ı aynı sabit pinleri kullanır.

- ROI'nin native rawRgba ile piksel eşitliği artık `validate_presentations` ve asıl
  runner yolunun zorunlu parçasıdır. Geçerli PNG/raw fixture geçer; gerçek görüntüde
  kaynak satırı +1, yanlış ölçek ve yanlış içerik reddedilir.
- Manifest testleri bozuk sözleşmede hiçbir asset okunmadığını, geçerli PNG fixture
  ile initialize başarısını ve üretim pinlerinin gevşemediğini doğrular.
- Lifecycle testi kayıtlı observer'a binding üzerinden paused/inactive gönderir.
  Geç gelen geçerli veri iptal edilir; kaynaklar, callback, observer ve handler
  temizlenir, önceki binding durumu geri alınır.
- Global `(index,sample)` sırası tam seri sözleşmeyle kontrol edilir. Yer değiştiren
  dört örneklik bloklar yanlış etiketle raporlanmadan reddedilir.
- `missingMeasurementCases` ve `missingPresentationCases` ayrıdır. `missingCases`
  ikisini kapsar. Sample3 mevcut olsa bile eksik veya reddedilmiş slot/crop tam
  kabul sayılmaz; yalnız piksel doğrulaması geçmiş captureEvidence kabul edilir.
- Snapshot öncesinde host'ta 1.141.424.640 byte dataset + 2 GiB build bütçesi +
  8 MiB makbuz payı aranır. ENOSPC/result yazım hatasında bounded CLI/stdout özeti
  primary hata ve preservation sonucunu ayrı korur. Gerçek finalize yolu bu hata
  ve başarısız preservation kombinasyonuyla da test edilmiştir.

Yeni doğrulayıcı, kapalı `63b8351d1cf4` kanıtına salt okunur uygulandı. Ayrı doğrulama
kimliği `a4f0de98a286`'dır. 48 timing, 12 gerçek ROI ve iki native readback türündeki
sekiz karşılaştırma tekrar geçti. Ölçüm/sunum eksik listeleri boştur. Eski runtime,
24 PNG, 24 raw ve üç makbuzdan oluşan **52 dosyanın** SHA'ları önce/sonra aynıdır;
kapalı native koşunun makbuzu değiştirilmedi. Yeni sonuç makbuzu SHA-256:
`806c04817895605657a0b9d1377805192025769dff51d772515942aae4fca219`.

Bağımsız inceleme kapanışı: üç inceleme tamamlandı. Altı PATCH bulgusu düzeltildi;
son hedefli incelemelerde açık bulgu veya somut regresyon kalmadı.
