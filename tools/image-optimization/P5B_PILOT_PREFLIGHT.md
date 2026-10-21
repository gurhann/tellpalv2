# P5b — Pilot kaynaklarının yerel ön doğrulaması

Bu araç yalnız #18/TR `story.kocaman-bir-sarilma` ve #62/TR
`story.pofuduk-misir-pops` içindir. Ajan canlı DB veya storage'a bağlanmaz.
Kullanıcı CSV'yi kendi DataGrip/IntelliJ bağlantısından dışa aktarır ve
orijinalleri kendisi sağlar. Araç ağ, credential, DB veya storage istemcisi kullanmaz.

## 1. Tek SELECT ve başlıklı CSV

[p5b_pilot_sources.sql](p5b_pilot_sources.sql) dosyasındaki tek SELECT'i kendi
bağlantınızda çalıştırın. Salt okunur bağlantı/transaction kullanın. Sonuçtaki
bütün satır ve sütunları dışa aktarın; DataGrip grid sayfalaması veya seçili
hücrelerle sonucu kırpmayın.

CSV ayarları: UTF-8 (BOM olabilir), virgül ayırıcı, çift tırnak quote ve **sütun
başlıkları dahil**. NULL için boş hücre kullanın (`NULL` ve `\N` de tanınır).
Başlıkları yeniden adlandırmayın. Sorgu 20 sütun üretir. Eksik içerik/çeviri,
beyan edilmiş eksik sayfa ve karşılıksız medya referansı LEFT JOIN sayesinde
görünür kalır. Beyan dışında kalan gerçek sayfalar da korunur.

Placeholder üretimi pilot başına en çok 997 sayfayla sınırlıdır. SQL gerçek
`declared_page_count` değerini değiştirmez; okuyucu 997 üstü beyanı
`DECLARED_PAGE_COUNT_LIMIT` ile engeller. Gerçek kayıtlı sayfalar bu sınırla
kırpılmaz. CSV için ayrıca toplam 1.000 kayıt yerel sınırı vardır.

`source_asset_id` rolün referansıdır; `asset_id` karşılık gelen medya kaydıdır.
İkincisi boşsa referans korunmuş olsa da medya bağı eksiktir. `observed_at`
tek SQL gözleminin zamanıdır. SQL'den ölçü, piksel, decode, dosya varlığı veya
storage generation/sürüm kanıtı çıkarılmaz.

## 2. Orijinaller ve dosya eşleme

CSV, orijinaller, mapping ve rapor **repo dışında özel bir dizinde** tutulur.
Girdilerde ve üst dizinlerinde symlink kullanmayın. macOS'ta `/tmp` yerine
gerçek `/private/tmp` yolunu kullanın. Araç repo içindeki girdi/çıktıyı reddeder.

CSV'de referans verilen her benzersiz `source_asset_id` için orijinal dosyanın
yerel kopyasını sağlayın. Thumbnail, ekran görüntüsü veya yeniden kaydedilmiş
dosya kullanmayın. DB `object_path` alanı yerel dosya yolu değildir; kullanıcı
eşlemeyi doğrular. Araç dosya indirmez ve eksik dosyayı başka rolden/dilden tamamlamaz.

`mapping.json`, asset ID'yi **ondalık string anahtar** ve **mutlak yerel yol**
değeriyle eşler. Yinelenen anahtar, göreli yol ve CSV dışı asset ID reddedilir.
Aşağıdaki kimlikler yalnız sentetik örnektir; gerçek ID'leri CSV'den alın:

```json
{
  "1800": "/private/tmp/p5b-private/originals/cover.jpeg",
  "1801": "/private/tmp/p5b-private/originals/page-01.png"
}
```

Bu örnek mapping tam pilot girdisi değildir. Kapak ve detail aynı asset ID'yi
kullanıyorsa tek mapping girdisi yeterlidir. Dosya uzantısından format çıkarılmaz.

## 3. Yerel çalıştırma

Repo kökünde mevcut Pillow bağımlılığını kurun ve yeni bir rapor yolu seçin:

```bash
uv sync --project tools/image-optimization --frozen
uv run --project tools/image-optimization --frozen python \
  tools/image-optimization/p5b_local_source_preflight.py \
  --csv /private/tmp/p5b-private/pilot.csv \
  --mapping /private/tmp/p5b-private/mapping.json \
  --report /private/tmp/p5b-private/preflight-01.json
```

Çıktı dizini önceden mevcut olmalıdır. Rapor yalnız yeni normal dosyaya `0600`
izinle yazılır. Var olan dosya veya hardlink üzerine yazılmaz. CSV, mapping ve
orijinaller salt okunur açılır; içerikleri değiştirilmez. Yerel inceleme sınırları
dosya başına **64 MiB** ve tek raster için **40.000.000 piksel**dir. CSV en çok
2 MiB/1.000 kayıt, mapping en çok 1 MiB/1.000 girdidir. Bunlar üretim profili değildir.

PNG/JPEG için önce yapı doğrulanır; dosya yeniden açılıp tam decode edilir.
Ham EXIF bloğu varlığı ile EXIF etiket sayısı ayrı gözlenir; boş EXIF bloğu da
`exifPresent=true` ve `EXIF_PRESENT` üretir. EXIF/orientation, ICC varlığı/uzunluğu/hash'i, mode, ölçü, alpha kanalı,
şeffaflık metadata'sı ve gerçekten şeffaf piksel varlığı gözlenir. ICC'nin
semantik geçerliliği veya renk doğruluğu değerlendirilmez. GIF/diğer formatlar ve
çok kareli dosyalar hata ile raporlanır; düzleştirilmez/dönüştürülmez.

## 4. Raporu yorumlama

- `roles`: Her rol/sayfa bağının CSV gözlemi. `object_path` ve yerel dosya yolları rapora kopyalanmaz.
- `sources`: Her benzersiz asset için beklenen/gözlenen byte, SHA, MIME ve Pillow gözlemleri.
  Eksik SHA veya orijinal `UNVERIFIED`/engel üretir. Bilinmeyen değer `null` kalır.
- `reviewSignals`: EXIF, orientation, ICC, alpha ve RGB dışı mode gibi dönüşüm öncesi
  ayrıca incelenmesi gereken gözlemler. Bu liste P2 doğrulayıcısının yerine geçmez.
- `pilots` ve `baselineComparison`: P5a ile güncel CSV rol/dosya sayısı ve benzersiz
  kayıtlı byte toplamı farkları. `localSourceTotals` yalnız okunabilen yerel dosya
  miktarını verir; eksik yerel kaynak varsa toplam byte `null` olur.

P5a baz çizgisi #18 için 18 rol/17 dosya/6.191.423 byte, #62 için
25 rol/24 dosya/120.305.904 byte; toplam **43 rol/41 dosya/126.497.327 byte**.
Cover/detail rolleri ayrı sayılır; ortak kaynak byte'ı yalnız bir kez toplanır.
Eksik/çelişkili byte metadata'sı toplamı `null` yapar. Farkın tek başına varlığı
kaynakları değiştirmez; hatasız güncel envanterin farklı sayımları ayrıca görünür.

Çıkış kodu `0`: CSV ve sağlanan yerel kaynakların gözlemleri tamamlandı
(`LOCAL_OBSERVATIONS_COMPLETE`). Kod `1`: girdi/kaynak engeli; `2`: kullanım veya
rapor yazma hatası. `0`, P5b'nin gerçek pilot kabulü veya üretim izni değildir.
`p2Result` daima `NOT_EVALUATED`; `realPilotValidation` ve `p5bCompletion`
`NOT_ESTABLISHED` kalır. Aracın yerel dosyanın gerçekten güncel canlı orijinal
olduğunu bağımsız doğrulaması mümkün değildir.

Yerel SHA eşleşmesi yalnız CSV'deki kayıtlı SHA ile kopyanın byte eşitliğini
gösterir. Canlı nesne/generation, immutable admission, kalite, üretim kabulü,
dönüşüm/backfill/yayın/deploy ve GIF/eksik PT kapak düzeltmesi bu kapsamda değildir.
Özel raporu ve girdileri Git'e eklemeyin. Gerçek girdiler henüz sağlanmadıysa
yalnız sentetik test kanıtı kaydedilir ve gerçek pilot doğrulaması açık kalır.

## 5. Sentetik doğrulama

```bash
python3 -m unittest discover -s tools/image-optimization \
  -p 'test_p5b_local_source_preflight.py'
```

SQL sınaması mevcut migration'ları yeni geçici PostgreSQL kümesine uygular,
sentetik kayıtları yazar, TCP'yi kapalı tutar ve sonunda kümeyi siler. Mevcut
bir veritabanı veya canlı bağlantı parametresi kabul etmez. PostgreSQL binary
dizininizi belirtin (PostgreSQL 15 ve üzeri):

```bash
P5B_POSTGRES_BIN=/opt/homebrew/opt/postgresql@18/bin \
  python3 -m unittest discover -s tools/image-optimization -p 'test_p5b_pilot_sql.py'
```

Güncel kanıt ve açık girdiler: [verification.md](../../be/docs/evidence/p5b-pilot-source-preflight/verification.md).
