# P5c — Yerel kayıpsız biçim karşılaştırması

Araç yalnız #18/TR ve #62/TR için P5b'de doğrulanan 41 kaynağı kullanır. CSV'nin
SHA256 değeri, 43 rol bağı, 41 benzersiz asset, kayıtlı byte toplamları ve her
kaynağın byte/SHA/MIME değerleri yeniden doğrulanır. Bütün kaynakların ön kontrolü
bitmeden encode başlamaz. Kapak/detail ortak kaynağı bir kez dönüştürülür.

## Çalıştırma

Mevcut Python/Pillow ortamını kullanın. Yeni paket, ağ bağlantısı veya eski
`probe.py`/P2 doğrulayıcısı gerekmez. Girdiler ve çıktı repo dışında olmalıdır.
P5b'nin özel `mapping.json` dosyası her asset ID'yi mutlak kaynak yoluna eşler.
Kaynakları, mapping'i, CSV'yi veya görselleri Git'e eklemeyin.

```bash
python3 -m unittest discover -s tools/image-optimization \
  -p 'test_p5c_local_lossless_compare.py' -v

python3 -B tools/image-optimization/p5c_local_lossless_compare.py \
  --csv /private/tmp/p5b-private/pilot.csv \
  --mapping /private/tmp/p5b-private/mapping.json \
  --output-dir /private/tmp/p5c-private-new-run
```

`--output-dir` henüz var olmamalıdır. Araç dizini `0700`, her yeni dosyayı
`0600` izinle exclusive oluşturur. Üst dizinlerde symlink ve repo içindeki yollar
reddedilir. macOS'ta `/tmp` yerine gerçek `/private/tmp` yolunu kullanın.
Başarısız çalışmanın dosyaları korunur; yeni denemede başka bir dizin seçin.
Hiçbir kaynak veya önceki sonuç silinmez ya da üzerine yazılmaz.

Kaynak başına 64 MiB ve 40 milyon piksel sınırı uygulanır. Ölçü kesinlikle
1712×2778 kalır. Ön kontrol ve encode alt süreçlerinin toplamı kaynak başına
120 saniyeyle sınırlıdır. Çalışma seridir. Galeri kopyaları, adaylar, HTML ve
rapor dahil toplam çıktı bütçesi 1 GiB'dır; encoder bellekte de kalan bütçeyi
aşamaz. Ana rapor için 2 MiB ayrılır. İlk hatada yeni encode başlatılmaz.

## Dönüşüm ve kanıt

Kaynak tam decode edilip RGB byte referansı oluşturulur. RGBA yalnız alpha'nın
bütün değerleri 255 ise RGB'ye alınır; şeffaf piksel, orientation etiketi, yeni
profil veya beklenmeyen format/mode otomatik düzeltilmez. #62'nin 536 byte Display
P3 ICC'si hash ile sabitlenir ve çıktıda ham byte eşitliği aranır. #18'de ICC
yoktur; dosyaya sRGB ataması veya sRGB doğruluk onayı verilmez. Renk dönüşümü,
resize ve kayıplı encode yapılmaz.

Her kaynak için tek PNG (`compress_level=9`) ve tek WebP
(`lossless=True, quality=100, method=6, exact=True`) üretilir. EXIF ve XMP encoder'a
verilir; kaynak/aday varlık, byte uzunluğu, SHA256 ve eşitlik sonucu kaydedilir.
Codec EXIF taşıyıcısını değiştirebilir; bu durum `metadataEqual.exif=false` olarak
görünür. ICC her durumda aynen korunmak zorundadır. Orijinaller değiştirilmez.

Her aday diskten yeniden okunur ve tam decode edilir. Bütün RGB byte'ları ile
ham ICC birlikte eşit olmalıdır. WebP RIFF uzunluğu, chunk düzeni, VP8L başlığı,
ölçüsü ve sürümü ayrıca denetlenir. ICC/EXIF/XMP taşıyan VP8X container kabul
edilir; kayıplı VP8, animasyon ve beklenmeyen chunk'lar reddedilir.

Özel `report.json`; format, dosya byte/SHA, ölçü, roller, runtime sürümleri,
ayarlar, encode/decode süreleri, metadata ve kaynakların önce/sonra SHA
kontrollerini içerir. `be/docs/evidence` altındaki önceki dosyalar da önce/sonra
hash'lenir. Yerel yollar raporda tutulmaz; yalnız galeri dosya adları bulunur.
Kaynak kopyaları `source-<asset>.*`, adaylar `<asset>.png` ve `<asset>.webp` adını
taşır. Galeri kopyaları aday sayısına katılmaz.

Çıkış kodu `0`: `LOCAL_LOSSLESS_COMPLETE`; `1`: girdi/dönüşüm/doğrulama engeli;
`2`: güvenli çıktı dizini oluşturulamadı veya komut kullanımı hatalı. Hata kodu,
aktif asset ve aşama özel raporda kalır. Başarısız aday silinmez ve başarı sayılmaz.

## Yerel görsel inceleme

Yeni özel dizindeki `gallery.html` dosyasını tarayıcıda açın. Her satır aynı
asset için kaynak/PNG/WebP'yi eşit sütunlarda, aynı ölçekte gösterir. Görsele
tıklamak 1712×2778 dosyayı açar. Kapakları ve yazılı sayfaları inceleyin.
Galeri yalnız aynı dizindeki dosyalara bağlanır; script, CDN, font veya başka
dış kaynak kullanmaz. Kaynak kopyasının byte'ları orijinalle aynıdır.

Tarayıcının ICC/WebP desteği görüntüyü etkileyebilir. Galeri yerel görsel inceleme
içindir; cihaz veya fiziksel kalite kabulü değildir. Tam RGB eşitliği JPEG'in
geçmiş sıkıştırmasında kaybolan ayrıntıyı geri getirmez. JPEG'den elde edilen
kayıpsız adaylar büyüyebilir. Süreler yalnız yerel Python/Pillow gözlemidir;
mobil performans, gerçek transfer veya üretim kazancı değildir.

Kayıtlı gerçek deney: [verification.md](../../be/docs/evidence/p5c-local-lossless/verification.md).
P2, üretim, cihaz ve kalite kabulü açık kalır. Java/Flutter/CMS/P2 kuralları veya
canlı veri değişmez; upload, job, backfill ve deploy yapılmaz.
