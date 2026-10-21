# P5c — Yerel kayıpsız biçim denemesi

**Güncel teslim:** Çıktılar izinli kalıcı dizinde yeniden üretildi; 82 dosyanın
hash'i ilk koşuyla aynı. [Kısa sonuç ve yeni koşu](rerun-verification.md).
Aşağıdaki özel dizin kaybı ilk koşuya aittir; tarihsel kayıt korunur.

**Dosya doğrulaması tamamlandı:** 41 benzersiz kaynaktan 82 aday üretildi; 43 rol bağı özel raporda izleniyor. Her adayda bütün RGB byte’ları ve ham ICC birlikte eşleşti. Ölçü 1712×2778 kaldı. Bu sonuç P2, üretim, cihaz veya görsel kalite kabulü değildir.

**Özel çıktı erişimi sonradan kayboldu:** Son paket kontrolünde özel run dizini, galeri, 82 aday ve ham rapor/provenance artık bulunamadı. Ana ajan da bunu doğruladı. Uygulayıcı ve ana ajan silme veya taşıma işlemi yapmadı; kaybolmanın nedeni bilinmiyor. Otomatik temizlik açıklaması kanıtlanmış değildir. Aşağıdaki ölçümler ve hash’ler erişim kaybolmadan önce doğrulandı; tarihsel sonuçlar korunuyor. Özel dosyalar şu anda yeniden açılamadığı için yeni bir dosya incelemesi yapılamıyor. Ek encode bu aşamada başlatılmadı; kalıcı ve izinli bir özel dizinde yeniden çıktı üretimi açık iştir.

Ölçüm başlangıcı: `2026-10-21T11:13:54Z`. Toplam kaynak: **126.497.327 byte**. Kapak/detail ortak dosyası rol sayımında iki, dosya/byte sayımında bir kez yer alır.

## Kaynak grubuna göre boyut sonucu

| Pilot | Kaynak biçimi | Kaynak byte | PNG byte | PNG değişimi | WebP byte | WebP değişimi |
|---|---|---:|---:|---:|---:|---:|
| #18/TR · 17 kaynak | JPEG/RGB, profilsiz | 6.191.423 | 28.533.448 | %360,85 büyüme | 19.403.554 | %213,39 büyüme |
| #62/TR · 24 kaynak | PNG/RGBA, Display P3 | 120.305.904 | 91.936.919 | %23,58 tasarruf | 70.606.312 | %41,31 tasarruf |
| Toplam · 41 kaynak | İki grup | 126.497.327 | 120.470.367 | %4,76 tasarruf | 90.009.866 | %28,84 tasarruf |

#18’in 17 dosyasının tamamı her iki aday biçimde büyüdü. #62’de büyüyen dosya yok. Toplam PNG tasarrufu 6.026.960, WebP tasarrufu 36.487.461 byte’tır. JPEG’in geçmiş sıkıştırmasında kaybolan ayrıntı geri kazanılmaz; burada aynı decode edilmiş pikseller daha büyük kayıpsız dosyalara yazıldı.

## Kaynak başına boyutlar

Fark = aday byte − kaynak byte. Pozitif değer büyümedir. Asset kimlikleri dosya yolu veya yerel eşleme değildir.

| Pilot / asset | Kaynak byte | PNG byte | PNG farkı | WebP byte | WebP farkı |
|---|---:|---:|---:|---:|---:|
| #18 / 1950 | 361885 | 1725534 | +1363649 | 1188342 | +826457 |
| #18 / 1952 | 288768 | 1141855 | +853087 | 771374 | +482606 |
| #18 / 1954 | 214723 | 906366 | +691643 | 598166 | +383443 |
| #18 / 1956 | 256634 | 1093356 | +836722 | 657820 | +401186 |
| #18 / 1958 | 249503 | 1105256 | +855753 | 713578 | +464075 |
| #18 / 1960 | 482003 | 2465978 | +1983975 | 1735040 | +1253037 |
| #18 / 1962 | 353873 | 1349701 | +995828 | 781748 | +427875 |
| #18 / 1964 | 406040 | 1836819 | +1430779 | 1217046 | +811006 |
| #18 / 1966 | 333699 | 1536544 | +1202845 | 990222 | +656523 |
| #18 / 1968 | 258580 | 1109030 | +850450 | 699390 | +440810 |
| #18 / 1970 | 309746 | 1279683 | +969937 | 897830 | +588084 |
| #18 / 1972 | 490555 | 2472368 | +1981813 | 1793908 | +1303353 |
| #18 / 1974 | 337335 | 1375396 | +1038061 | 935778 | +598443 |
| #18 / 1976 | 556757 | 2924406 | +2367649 | 2223456 | +1666699 |
| #18 / 1978 | 346582 | 1604384 | +1257802 | 970158 | +623576 |
| #18 / 1980 | 460634 | 2212693 | +1752059 | 1519030 | +1058396 |
| #18 / 1982 | 484106 | 2394079 | +1909973 | 1710668 | +1226562 |
| #62 / 8303 | 4102023 | 3079408 | -1022615 | 2247538 | -1854485 |
| #62 / 8306 | 6067947 | 4620381 | -1447566 | 3652182 | -2415765 |
| #62 / 8308 | 5954956 | 4664915 | -1290041 | 3508232 | -2446724 |
| #62 / 8310 | 4691713 | 3579503 | -1112210 | 2727004 | -1964709 |
| #62 / 8312 | 5199334 | 3954349 | -1244985 | 3065182 | -2134152 |
| #62 / 8314 | 4286739 | 3257909 | -1028830 | 2447188 | -1839551 |
| #62 / 8316 | 4898269 | 3680000 | -1218269 | 2922008 | -1976261 |
| #62 / 8318 | 5376429 | 4241341 | -1135088 | 3225218 | -2151211 |
| #62 / 8320 | 5377951 | 4104682 | -1273269 | 3229462 | -2148489 |
| #62 / 8322 | 5951995 | 4541482 | -1410513 | 3629490 | -2322505 |
| #62 / 8324 | 4356674 | 3186266 | -1170408 | 2520516 | -1836158 |
| #62 / 8326 | 5064991 | 3806084 | -1258907 | 2962204 | -2102787 |
| #62 / 8328 | 4642041 | 3549207 | -1092834 | 2652386 | -1989655 |
| #62 / 8330 | 4595048 | 3533358 | -1061690 | 2620334 | -1974714 |
| #62 / 8332 | 4769273 | 3633392 | -1135881 | 2748312 | -2020961 |
| #62 / 8334 | 6327116 | 4999993 | -1327123 | 3743118 | -2583998 |
| #62 / 8336 | 4717906 | 3462116 | -1255790 | 2676736 | -2041170 |
| #62 / 8338 | 4567871 | 3501092 | -1066779 | 2605734 | -1962137 |
| #62 / 8340 | 5638085 | 4364787 | -1273298 | 3335896 | -2302189 |
| #62 / 8342 | 4821029 | 3696053 | -1124976 | 2884362 | -1936667 |
| #62 / 8344 | 4897522 | 3706213 | -1191309 | 2924486 | -1973036 |
| #62 / 8346 | 3110028 | 2366387 | -743641 | 1734806 | -1375222 |
| #62 / 8348 | 5662334 | 4396746 | -1265588 | 3436684 | -2225650 |
| #62 / 8350 | 5228630 | 4011255 | -1217375 | 3107234 | -2121396 |

## Piksel, profil ve metadata kanıtı

- **82/82** adayda tam RGB byte eşitliği; kaynak başına 14.267.808 RGB byte karşılaştırıldı. Yalnız hash eşitliğine dayanılmadı.
- **82/82** adayda ham ICC byte eşitliği. #62’nin 24 kaynağındaki 536 byte Display P3 profili 48 adayda aynen kaldı; SHA256 `20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab`.
- #18’in ICC’siz 17 kaynağına sRGB veya başka profil atanmadı. Renk doğruluğu ya da sRGB uygunluğu sonucu çıkarılmadı.
- #62’de alpha yalnız bütün değerler 255 olduğu kanıtlandıktan sonra kaldırıldı. #18 zaten RGB’ydi. Orientation etiketi yok; resize ve renk dönüşümü yapılmadı.
- **41/41** WebP’de RIFF/chunk düzeni ve VP8L kayıpsız kodlama doğrulandı. ICC/EXIF/XMP taşıyan VP8X container desteklendi. Eski P2 validator veya eski probe çalıştırılmadı.
- EXIF ve XMP encoder’a verildi. PNG’de 41 EXIF ve 41 XMP bloğu ham byte olarak aynı. WebP’de 41 XMP aynı; 41 EXIF karşılaştırması farklı. Ek salt okunur kontrolde farkın bütün dosyalarda yalnız altı byte `Exif\0\0` önekinin çıkarılması olduğu görüldü; kalan EXIF byte’ları aynı. Varlık, uzunluk, hash ve eşitlik sonuçları raporlandı.

## Koruma ve yürütme

- **41/41** kaynağın önce/sonra SHA256 değerleri eşit; CSV ve mapping içerikleri değişmedi. CSV SHA256 `9d7baf864996f46c283b90e35b7a9d582d748800865f3f2bd7a2148106c882c1`.
- Önceki **272** repo kanıt dosyasının önce/sonra manifestleri aynı. P5b’nin özel ön doğrulama raporu da kayıtlı `af1fbb6ae55c3144b2d91615ca77b5b5cc53917fbf469461a075f488c1d4190b` hash’iyle eşleşiyor.
- Bütün girdiler salt okunur açıldı. Yeni özel dizin `0700`, yeni dosyalar `0600` izinle exclusive oluşturuldu. Görseller, yerel eşleme ve özel yollar Git’e alınmadı.
- Kaynak kopyaları, 82 aday, galeri, ham rapor ve provenance dahil özel çıktı **337,331,174 byte**; 1 GiB sınırının altında. Kaynak başına 64 MiB/40 milyon piksel sınırı korundu. En uzun ön kontrol + encode alt süreç toplamı 19,104 saniye; kaynak başına 120 saniye sınırının altında.
- Ağ/DB/storage/credential/cihaz erişimi, upload, job, backfill veya deploy yapılmadı. Ek macOS `sandbox-exec` katmanı ortam tarafından başlatılamadı (`sandbox_apply: Operation not permitted`). Ölçüm mevcut kısıtlı ortamda yerel dosya API’leriyle yürüdü; ek kernel ağ engeli kanıtı iddia edilmiyor.
- Ölçüm başladıktan sonra runner/encoder/çıktı yardımcı kodları değişmedi. SHA’lar koşu sonrasında alındı; dosya mtime değerlerinin başlangıçtan eski olduğu ve ham rapor SHA’sı özel `provenance.json` içinde kaydedildi. Bu kayıt başlangıçta alınmış bir hash pini olarak sunulmuyor.
- Ham özel rapor SHA256: `8ca3840ace6c2c534de2d2e8c1754b75ca96f821f167ccc7afc3efa0343ebea2`. Galeri SHA256: `aed5250400e7cf55cb1d48fc3194fbbd6daede00031feab744b655cccf19ed0a`. Özel provenance SHA256: `8469fc5328426ddfadb8ff0622f42274d55ea7986e7a038bc3e82bfd6e2ca73f`.

## Yerel görsel inceleme ve açık sınırlama

HTML galerisinin 123 görsel referansı statik olarak denetlendi: 41 kaynak kopyası + 82 aday, hepsi mevcut yerel dosya; bütün görseller aynı 1712×2778 ölçü bilgisini ve eşit CSS sütunlarını kullanıyor. Dış kaynak ve script yok.

İki kapak ve iki yazılı sayfa (asset 1950, 1952, 8303, 8306), kaynak/PNG/WebP olmak üzere 12 dosya halinde salt okunur görüntüleyiciyle incelendi. Metin ve kompozisyon korunuyor. Tarayıcı `file:` protokolünü güvenlik politikasıyla reddettiği için galerinin tarayıcı render’ı doğrulanamadı; yasak başka tarayıcı veya HTTP sunucuyla dolanılmadı.

**Renk görünümü açık kalıyor:** #62 WebP önizlemesinde PNG’ye göre ton farkı izlenimi oluştu. RGB+ICC byte eşitliği dosya kanıtıdır; görüntüleyicinin veya Flutter’ın renk yönetimini doğrulamaz. Ton farkının codec kaybı olduğu kanıtlanmadı. Koşulsuz görsel kalite, cihaz veya üretim uygunluğu sonucu çıkarılmadı. Galeri yeniden erişilebilir olduğunda kullanıcı kendi yerel tarayıcısında inceleyebilir.

## Ayarlar, süreler ve testler

Python `3.14.6`; Pillow `12.3.0`; libwebp `1.6.0`; zlib `1.3.1.zlib-ng`. Platform: `macOS-27.0.1-arm64-arm-64bit-Mach-O`.

PNG: `compress_level=9`. WebP: `lossless=True, quality=100, method=6, exact=True`. Her kaynak seri ve süreli alt süreçte işlendi. Encode süresi hazırlanmış RGB’nin belleğe yazılmasını; decode süresi Pillow `open+load` işlemini kapsar. Alt süreç duvar süresi ayrıca kaydedildi. Bunlar mobil performans, gerçek transfer veya üretim benchmark’ı değildir.

```bash
python3 -m unittest discover -s tools/image-optimization -p 'test_p5c_local_lossless_compare.py' -v
python3 -m unittest discover -s tools/image-optimization -p 'test_p5b_local_source_preflight.py'
```

**P5c: 20/20**, **P5b regresyon: 22/22** geçti. P5c matrisi:

| Matris satırı | Çalışıp geçen kapsam |
|---|---|
| Geçerli | 41 sentetik kaynak, 82 aday, 43 rol; bütün RGB/ICC ve kaynak hash’leri; yerel galeri referansları |
| Kaynak farkı | Byte, SHA, rol ve geometri farkında ret; bütün ön kontroller bitmeden encode yok |
| Semantik fark | Şeffaf alpha, transparency metadata’sı, orientation ve yeni/eksik profil retleri |
| Aday farkı | Kayıplı/bozuk WebP; tek piksellik RGB farkı; RGB aynıyken yanlış ICC ayrı hata; başarısız aday dosyası korunur |
| Güvensiz çıktı | Var olan yol, symlink, repo içi yol, kaynak byte/piksel limiti, çıktı bütçesi ve alt süreç timeout retleri |

Tam dosya ölçümleri ve 41 önce/sonra kaynak hash’i: [summary.json](summary.json). Komutlar ve galeri kullanımı: [P5C_LOCAL_LOSSLESS.md](../../../../tools/image-optimization/P5C_LOCAL_LOSSLESS.md). Java/Flutter/CMS/P2 kuralları değişmedi.

## İnceleme sonrası doğrulama yaması — 2026-10-21

Üç bağımsız incelemenin blind ve edge katmanlarında bulgu çıkmadı. Verification katmanındaki iki orta önem bulgusu yalnız test kapsamı genişletilerek kapatıldı. Frozen intent, encoder/runner/çıktı kodu ve ilk tarihsel `summary.json` değiştirilmedi. Gerçek kaynaklarla yeni encode yapılmadı.

- Başarılı PNG ve WebP encode’undan sonra kaynak, CSV, mapping veya önceki kanıtın değişmesini sınayan **dört ayrı test** eklendi. Her test hem dönen raporda hem diskteki `report.json` içinde `BLOCKED` ve `PROTECTED_INPUT_CHANGED` arıyor. İki adayın başarıyla doğrulanmış olması ayrıca denetleniyor. Kaynak/CSV/mapping gerçek geçici dosyalardır; önceki kanıt kontrolü de gerçek geçici dosya ve değiştirilmemiş `protected_snapshot` kodunu kullanır. Her önce/sonra kanıt manifesti beklenen gerçek hash’e karşı doğrulanır.
- İki pilot ve bilinen byte boyutlarıyla toplam testi eklendi. Her pilotta iki benzersiz kaynak ve üç rol bulunur; ortak kapak/detail byte’ı iki kez sayılmaz. Format byte toplamları, pozitif/negatif `savedBytes`, `savedPercent` ve büyüyen asset listeleri açık beklenen değerlerle karşılaştırılır. #18 PNG için 400 → 600 byte, −200 byte/%−50 tasarruf; #18 WebP için 400 → 300 byte, 100 byte/%25 tasarruf beklenir. #62 PNG için 500 → 300 byte, 200 byte/%40; WebP için 500 → 400 byte, 100 byte/%20 beklenir. #62 WebP toplamda küçülürken büyüyen tek dosya da ayrıca listelenir.

```bash
python3 -m unittest discover -s tools/image-optimization -p 'test_p5c_local_lossless_compare.py'
python3 -m unittest discover -s tools/image-optimization -p 'test_p5b_local_source_preflight.py'
```

Güncel sonuç: **P5c 25/25**, **P5b regresyon 22/22** geçti; toplam **47 test**. İki test açığının kapandığı bellekte yapılan hata ekleme kontrolüyle de sınandı: `PROTECTED_INPUT_CHANGED` kontrolü kaldırılınca dört yeni koruma testinin tamamı başarısız oldu. `savedBytes` işareti, `savedPercent` işareti ve `growingFiles` listesi ayrı ayrı bozulduğunda yeni toplam testi her değişikliği yakaladı. Bu kontrol disk üzerindeki uygulama kodunu değiştirmedi.

İlk özel dosyaların kaybolması ve renk yönetimi sınırlamaları geçerlidir. Kalıcı, izinli özel dizinde gerçek çıktıları yeniden üretme işi ana ajan tarafından ayrıca yürütülecektir.
