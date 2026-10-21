# P5b — Yerel araç ve sentetik doğrulama kanıtı

2026-10-21. Baz commit: `c8071716fc6b22afb817c5dfdaf741d3b800e08e`.

**Hazırlık aracı ve sentetik sınamalar tamamlandı. Gerçek pilot kaynak
doğrulaması ve P5b kabulü tamamlanmadı.** Kullanıcının başlıklı güncel CSV'si,
asset→dosya mapping'i ve orijinalleri henüz sağlanmadı. Bu kayıt gerçek kaynak
özelliği, kalite veya canlı storage sürümü gözlemi içermez.

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
