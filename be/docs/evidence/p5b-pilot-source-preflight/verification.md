# P5b — Yerel araç ve sentetik doğrulama kanıtı

2026-10-21. Baz commit: `c8071716fc6b22afb817c5dfdaf741d3b800e08e`.

**Hazırlık aracı, metadata karşılaştırması ve yerel kaynak incelemesi tamamlandı.
P2/üretim kabulü ve canlı geçiş yapılmadı.** Araç tesliminde beklenen güncel CSV
ve 41 orijinal alındı; aşağıdaki ayrı bölümlerde incelendi. Yerel dosya
bütünlüğü doğrulandı. Metadata/alpha uyumluluğu değerlendirme bekliyor.
Görsel kalite ve güncel canlı storage sürümü doğrulanmadı.

## Üretilen araçlar

- [Tek pilot SELECT'i](../../../../tools/image-optimization/p5b_pilot_sources.sql):
  #18/TR ve #62/TR cover/detail/page bağlarını eksik bağları koruyarak okur.
- [Yerel giriş](../../../../tools/image-optimization/p5b_local_source_preflight.py):
  özel CSV ve JSON mapping'i okur; byte/SHA/MIME ile Pillow gözlemlerini raporlar.
  Ayrı `p5b_inventory.py`, `p5b_image_probe.py` ve `p5b_source_io.py` dosyaları
  CSV/rol sayımı, görüntü inceleme ve salt okunur dosya sınırlarını tutar.
- [Kullanım rehberi](../../../../tools/image-optimization/P5B_PILOT_PREFLIGHT.md):
  DataGrip dışa aktarımı, yerel eşleme, sınırlar ve çıkış kodları.

## Sentetik görsel ve girdi sınaması

```bash
python3 -m unittest discover -s tools/image-optimization \
  -p 'test_p5b_local_source_preflight.py'
tools/image-optimization/.venv/bin/python -m unittest discover \
  -s tools/image-optimization -p 'test_p5b_local_source_preflight.py'
```

İnceleme düzeltmelerinden sonra her iki çalışma **22 test / OK** verdi.
Ortamlar Python 3.14.6/Pillow 12.3.0 ve
projenin kilitli bağımlılığıyla Python 3.11.13/Pillow 12.0.0'dır. Ağ veya gerçek
kaynak kullanılmadı. Testler yeni `/private/tmp/p5b-synthetic-*` dizinlerinde
üretildi ve temizlikte silindi.

Sınanan davranışlar:

- Sentetik JPEG/PNG kümesinde 43 rol, 41 benzersiz kaynak ve byte'ların yalnız
  benzersiz asset başına sayılması. Gerçek P5a byte baz çizgisinden farkın ayrı görünmesi.
- İnceleme öncesi/sonrası bütün 41 kaynağın SHA-256 eşitliği.
- Byte/SHA/MIME uyuşmazlığında beklenen ve gözlenen değerler; eksik SHA,
  byte, kaynak, rol/sayfa/içerik/çeviri ve medya bağlarının engel olması.
- Eksik byte'ların toplam/delta alanında `null` kalması; eksik rol ile değişen
  benzersiz kaynak sayısının ayrı raporlanması.
- Yanlış/eksik başlık, kırpılmış CSV kaydı, farklı statement zamanları,
  kapsam dışı pilot, yinelenen rol, çelişkili metadata, bozuk/çift anahtarlı
  mapping, göreli yol ve CSV dışı mapping girdisi reddi.
- Bozuk, boş, kesilmiş görseller; eksik dosya, symlink, üst dizin symlink'i,
  dizin, FIFO ve 64 MiB üstü sparse dosyanın reddi.
- 40.000.000 piksel üstü sentetik PNG başlığında raster decode başlamadan ret.
- EXIF/orientation, ICC, alpha, opak alpha kanalı ve palette şeffaflığı gözlemleri;
  GIF ve çok kareli PNG reddi; P2 sonucunun `NOT_EVALUATED` kalması.
- Yeni `0600` rapor; girdi üzerine yazma reddi ve CSV byte'larının korunması;
  başarı/engel/yazma hatası için sırasıyla `0/1/2` çıkış kodları.

## Bağımsız inceleme sonrası düzeltmeler

Frozen intent ve pilot kapsamı korunarak yedi bulgu giderildi:

1. Ham EXIF bloğu varlığı etiket sayısından ayrıldı. Etiketsiz geçerli EXIF
   bloğunda `exifPresent=true`, `exifEntryCount=0` ve `EXIF_PRESENT` doğrulandı.
2. Mapping yolları dosya sistemi kodlamasına karşı sınanır. JSON'da tek UTF-16
   surrogate taşıyan yol, traceback yerine `MAPPING_PATH_INVALID`, yazılmış
   engel raporu ve CLI çıkışı `1` üretir.
3. Pilot grubunun metadata'sı global source metadata'sıyla çelişirse byte
   toplamı/delta `null` kalır. İki pilotun aynı asset ID'ye farklı byte değeri
   vermesi ayrı testle doğrulandı.
4. SQL placeholder üretimi pilot başına 997 sayfayla sınırlandı; gerçek
   `declared_page_count` ve kayıtlı sayfalar korunur. Okuyucu büyük beyanı
   `DECLARED_PAGE_COUNT_LIMIT` ile açıkça engeller.
5. Byte/MIME doğruyken yalnız SHA'nın yanlış olduğu test `sha256Match=false`,
   `SHA256MATCH_MISMATCH`, `BLOCKED` ve CLI çıkışı `1` sonucunu gerektirir.
   Normal kaynaklarda üç match alanı ayrı ayrı `true` olarak sınanır.
6. Gerçek sentetik PostgreSQL SELECT çıktısı yerel CSV/mapping/preflight
   hattından geçirildi. SHA sütunu boşalsa başarısız olacak byte/SHA/MIME
   kimlik kontrolleri eklendi.
7. Her pilotun rol/kaynak/byte/baz çizgisi farkları ayrı sınanır. Yalnız #18
   değiştiğinde #62 özetinin bütünüyle aynı kaldığı doğrulanır.

## Gerçek migration şemasında yerel SELECT sınaması

```bash
P5B_POSTGRES_BIN=/opt/homebrew/opt/postgresql@18/bin \
  tools/image-optimization/.venv/bin/python -m unittest discover \
  -s tools/image-optimization -p 'test_p5b_pilot_sql.py'
```

**3 test / OK**. Kullanılan mevcut yerel sürüm PostgreSQL 18.6'dır; PostgreSQL 15
üzerinde ayrıca çalışma yapılmadı. Test, repo içindeki **V1–V37 migration'larının
tamamını** yeni geçici kümeye uyguladı. TCP `listen_addresses=''` ile kapalıydı;
yalnız özel geçici dizinin Unix socket'i kullanıldı. Dış PostgreSQL bağlantı
ortam değişkenleri alt süreçlerden çıkarılır. Test mevcut DB/DSN veya credential
kabul etmez. Küme çalışma sonunda durduruldu ve silindi.

Normal 16/23 sayfalı pilotlar 43 rol ve 41 benzersiz asset verdi. Bütün sonuçlar
aynı `observed_at` değerini taşıdı. SELECT ayrıca salt okunur transaction'da geçti.
Eksik bağ senaryolarındaki her mutasyon yalnız geçici sentetik veriye uygulandı
ve rollback edildi:

- İçerik yokluğunda cover/detail/page placeholder satırları korundu.
- İçerik çevirisi yokluğu satırdan düşmedi.
- Beyan edilmiş eksik sayfa ve sayfa çevirisi görünür kaldı.
- NULL kapak iki eksik role yansıdı.
- Karşılığı bulunmayan medya kaydında referans ID korundu, `asset_id` NULL kaldı.
- `page_count` dışında kalan gerçek sayfa da sonuçta korundu.

Ek büyük beyan testinde #18'in `declared_page_count` alanı `2147483647` yapıldı;
997'lik placeholder sınırının dışındaki gerçek sayfa `1001` de eklendi.
Sonuç #18 için 1.000, iki pilot toplamında 1.025 satırla sınırlı kaldı; gerçek
beyan ve sayfa `1001` korundu. Okuyucu bu CSV'yi
`DECLARED_PAGE_COUNT_LIMIT` ile reddetti. SQL gerçek sayfaları kırpmaz; ayrıca
CSV okuyucusunun toplam 1.000 kayıt sınırı geçerlidir.

SELECT–okuyucu testinde 41 sentetik JPEG/PNG oluşturuldu; bunların gerçek
byte/SHA/MIME metadata'sı yalnız yeni geçici kümeye yazıldı. Salt okunur SELECT
çıktısı dosyaya aktarılıp mapping ile gerçek preflight girişine verildi.
43 rol/41 kaynak, her kaynak için beklenen/gözlenen kimlikler, üç match alanı
ve okuma sonrası hash'ler doğrulandı. Bu testin metadata güncellemeleri yalnız
geçici kümeyle birlikte silindi. P2 `NOT_EVALUATED` ve P5b kabulü
`NOT_ESTABLISHED` kaldı.

Sandbox içindeki ilk `initdb` denemesi shared-memory izni nedeniyle başlamadı.
Geçici yerel test komutu otomatik izin incelemesinden geçtikten sonra bu çalışma
başarıyla tamamlandı. Canlı bağlantı veya mevcut yerel uygulama DB'si kullanılmadı.
Backend Java kodu/şeması değişmedi; Maven testi bu araç değişikliği için çalıştırılmadı.

## Gerçek doğrulama için beklenen girdiler ve açık sınırlar

Kullanıcı kendi DataGrip/IntelliJ bağlantısından tek SELECT'in başlıklı CSV'sini
repo dışına almalı; benzersiz asset ID başına orijinali ve mutlak yerel yol
mapping'ini sağlamalıdır. Dosyalar yeniden kaydedilmemelidir. P5a beklentisi:

| Pilot | Rol bağı | Benzersiz dosya | Kayıtlı byte |
|---|---:|---:|---:|
| #18/TR | 18 | 17 | 6.191.423 |
| #62/TR | 25 | 24 | 120.305.904 |
| Toplam | 43 | 41 | 126.497.327 |

Araç güncel CSV farkını ölçer; bu değerleri gerçek ölçüm yerine yazmaz. Eksik
orijinal veya kayıtlı SHA doğrulanmış sayılmaz. Başarılı Pillow incelemesi P2'nin
dar PNG/JPEG container/renk/metadata sözleşmesini çalıştırmaz. EXIF/ICC/alpha gibi
`reviewSignals` ayrıca değerlendirilmelidir. Hiçbir kaynak normalize edilmez,
dönüştürülmez veya düzeltilmez.

Yerel raporun tamamlanması güncel canlı nesne/generation, immutable admission,
görsel kalite veya üretim profili kabulü değildir. Canlı nesnenin sürümü ve
güncel byte'ları yerel kopyadan doğrulanamaz. Dönüşüm, backfill, job, yayın,
deploy, GIF veya eksik PT kapak düzeltmesi yapılmadı. Özel CSV, mapping,
object_path, orijinal veya gerçek kaynak raporu repoya eklenmedi.

## Kullanıcının güncel pilot CSV'sinin kabulü

2026-10-21 tarihinde sağlanan başlıklı CSV, mevcut `parse_csv` ve
`validate_inventory` ile bellekte incelendi. Ham dosya değiştirilmedi.
Dosya 14.083 byte, 20 sütun ve 43 veri kaydı içeriyor. SHA-256:
`9d7baf864996f46c283b90e35b7a9d582d748800865f3f2bd7a2148106c882c1`.
Tek gözlem zamanı `2026-10-21T10:36:55.883938+00:00`
(Türkiye saatiyle 13:36:55). SQL'in canlıda hangi transaction ayarıyla
çalıştırıldığı bu CSV'den kanıtlanmaz; ajan canlıya bağlanmadı.

| Pilot | Rol bağı | Benzersiz kaynak | Kayıtlı MIME | Kayıtlı byte |
|---|---:|---:|---|---:|
| #18/TR Kocaman Bir Sarılma | 18 | 17 | image/jpeg | 6.191.423 |
| #62/TR Pofuduk Mısır Pops | 25 | 24 | image/png | 120.305.904 |
| Toplam | 43 | 41 | 17 JPEG + 24 PNG | 126.497.327 |

Metadata doğrulama hatası, eksik rol/medya bağı veya eksik kayıtlı SHA bulunmadı.
Sayfa kümeleri sırasıyla 1–16 ve 1–23; kapak/detail kaynağı dosya toplamına
yalnız bir kez katıldı. Pandas ile bağımsız sayımlar aynı sonuçları verdi.

P5a'nın üç ham girdisinin hash'leri önceki kanıtla eşleştirildi. Pilotların
rol→kaynak, sayfa ve çeviri kimlikleri ile provider/object_path/MIME/byte/SHA
alanları karşılaştırıldı; değişiklik bulunmadı. Bu sonuç yalnız bu iki pilot
ve karşılaştırılan metadata alanları içindir.

Yol içermeyen sayısal kayıt: [received-metadata-analysis.json](received-metadata-analysis.json).
Ham CSV ve object_path değerleri repoya alınmadı. Dosyaların kayıtlı SHA'sının
dolu olması, gerçek dosyanın SHA eşleşmesi değildir. Henüz orijinal dosya
sağlanmadığı için piksel/decode/EXIF/ICC/alpha ve gerçek byte doğrulaması
çalıştırılmadı. P2 `NOT_EVALUATED`; P5b durumu `AWAITING_LOCAL_ORIGINALS`.

## Kullanıcının gerçek yerel kaynaklarının incelenmesi

Önceki bölüm CSV'nin alındığı andaki durumu kaydeder. Ardından kullanıcı iki
pilotun görsellerinin bulunduğu klasörü sağladı. Klasörde #18/TR için kapak ve
16 sayfa JPEG, #62/TR için kapak ve 23 sayfa PNG bulundu. Başka görsel yoktu.
Dosyalar klasör, kapak adı ve sayfa numarasıyla 43 role/41 asset ID'ye eşlendi;
bütün eşlemeler CSV'deki kayıtlı SHA ile doğrulandı. Kapak/detail aynı dosyaya
bağlandı. Özel dosya eşlemesi ve tam çalışma raporu repo dışında tutuldu.

Mevcut `p5b_local_source_preflight.py`, Python 3.12.14/Pillow 12.3.0 ortamında
gerçek CSV ve bu eşlemeyle çalıştırıldı. Çıkış kodu `0`, sonuç
`LOCAL_OBSERVATIONS_COMPLETE`; genel/kaynak hatası yok. Her 41 dosyanın
hesaplanan SHA, byte ve gerçek MIME değeri CSV ile eşleşti. Toplam
126.497.327 byte. İnceleme öncesi/sonrası SHA kontrolü bütün kaynakların ve
CSV içeriğinin değişmediğini doğruladı. Hiçbir kaynak değiştirilmedi;
dönüşüm veya canlı bağlantı yapılmadı.

| Gözlem | #18/TR Kocaman Bir Sarılma | #62/TR Pofuduk Mısır Pops |
|---|---|---|
| Kaynak | 17 JPEG / RGB | 24 PNG / RGBA |
| Boyut | Tümü 1712 × 2778 piksel | Tümü 1712 × 2778 piksel |
| Kare | Tümü tek kare | Tümü tek kare |
| Yapı kontrolü ve tam decode | 17/17 başarılı | 24/24 başarılı |
| EXIF | 17 dosyada var | 24 dosyada var |
| EXIF orientation etiketi | Gözlenmedi | Gözlenmedi |
| ICC | Yok | 24 dosyada var |
| Alpha kanalı | Yok | 24 dosyada var |
| Gerçekten şeffaf piksel | Yok | Yok; alpha aralığı her dosyada [255, 255] |

Bu bulgular kaynak bozukluğu değildir. Ancak mevcut
`ReadingImageInspector` JPEG için EXIF taşıyan APP1 metadata'sını kabul etmez;
PNG için RGB8 ve dar bir chunk listesi ister. Gözlenen RGBA/EXIF/ICC özellikleri
bu dar sözleşmeyle doğrudan uyumlu değildir. Bu çıkarım kod kurallarıyla statik
karşılaştırmadır; Java P2 doğrulayıcısı çalıştırılmadı ve `p2Result` hâlâ
`NOT_EVALUATED`. ICC'nin semantik geçerliliği/renk doğruluğu değerlendirilmedi.

Sıradaki karar, metadata/renk ve tamamen opak alpha kanalının hazırlık
aşamasında nasıl ele alınacağıdır. Kabul kuralları gevşetilmedi, metadata
silinmedi, alpha kaldırılmadı, dosya yeniden kaydedilmedi. Piksel ve dosya
kontrolü; kalite, immutable admission, üretim izni veya güncel canlı nesne
sürümü kanıtı değildir. P5b geçiş kabulü açık kalır.

Özel yolları ve ham dosya eşlemesini içermeyen
[yerel gözlem özeti](local-source-observations.json) kaydedildi. Güncel durum:
`LOCAL_OBSERVATIONS_COMPLETE_COMPATIBILITY_REVIEW_REQUIRED`.
