# P5a — Kullanıcının çalıştıracağı salt okunur sorgu

Tarih: 2026-10-21. Kullanıcı otomatik bağlantı aracı yerine gerekli sorguyu kendi
çalıştırıp ham veriyi paylaşmayı seçti. Ajan production bağlantısı açmadı ve
credential toplamadı. İlk JSON denemelerinden sonra kullanıcı dört normal SELECT
CSV'sini paylaştı; bu dosyaların metadata analizi tamamlandı. Güncel sonuç
[envanter raporunda](findings.md) ve [sayısal kayıtta](csv-inventory-analysis.json)
bulunur. Aşağıdaki eski sorgu denemeleri tarihsel kanıt olarak korunur.

## Teslim edilen dosyalar

- [SQL](../../../../tools/image-optimization/p5a_manual_inventory.sql): aynı
  snapshot'ta çekirdek STORY metadata'sı ve opsiyonel optimizasyon tablolarının şeması.
- [Kullanım](../../../../tools/image-optimization/P5A_MANUAL_INVENTORY.md): yeni ve boş
  oturum, tam JSON/CSV dışa aktarımı, hata halinde aynı bağlantıda rollback.
- [Plan](../../../../_bmad-output/implementation-artifacts/spec-p5a-production-readonly-inventory.md):
  manuel sorgu yaklaşımı; sonraki manifest/reading çıktıları ve katalog analizi bekliyor.

İlk v1 SQL SHA-256: `9cfde9d47ed667413c8c8bbb7e3116ecc16cd772f69da713024855bf9e504319`.

## Yerel doğrulama

Canlıdan bağımsız PostgreSQL 18.6 (Homebrew) kullanıldı. Depodaki V1–V36 SQL
migration'ları yalnız `/private/tmp/tellpal-p5a-verify` altında oluşturulan
geçici kümeye uygulandı. Sunucu TCP dinlemedi; testler bu dizindeki Unix socket
ve 55487 port kimliğiyle çalıştı. Production hedefi kullanılmadı. Bu testler
canlı PostgreSQL sürümü veya şemasının doğrulandığı anlamına gelmez.

İlk varsayılan PostgreSQL 14 kurulumu eksik ICU kütüphanesi nedeniyle başlayamadı.
Kurulum değiştirilmedi. PostgreSQL 18 başlangıcı sandbox shared-memory sınırına
takıldı; yalnız geçici yerel küme için yükseltilmiş çalıştırma kullanıldı.

Son SQL ile 11 kontrol geçti:

1. Gerçek migration şemasında boş katalog; `readOnly=on`, `repeatable read`.
2. STORY/dil/ortak kaynak, eksik sayfa çevirisi ve null byte korunması;
   metin gövdesi, cached URL, ilişkisiz medya ve MEDITATION çıktıya girmemesi.
3. Envanter öncesi/sonrası medya kayıt sayısı ve ID/byte toplamının aynı kalması.
4. Yalnız yerel DB'de denenen INSERT'in read-only transaction tarafından reddi.
5. Opsiyonel tablo yokluğunda `NOT_DEPLOYED` ve başarılı çekirdek sorgu.
6. 10.001 hikâyenin tam katalog gibi sunulmaması.
7. Eksik zorunlu sütunda sorgunun hata vermesi.
8. Textless kapak, listening kapak ve textless sayfanın ayrı medya bağları.
9. Diğer satır sınırları içindeyken yalnız reading yükümlülüğü sınırının aşılması.
10. RLS filtrelemesinde eksik başarılı çıktı yerine sunucu hatası.
11. Metadata byte korumasının, yalnız yerel testte küçültülen eşikle, JSON dizileri
    oluşturulmadan önce reddetmesi. Bu kontrol 60/64 MiB uç değer yük testi değildir.

## İnceleme ve düzeltmeler

Üç ayrı inceleme yapıldı. Blind incelemede bulgu yoktu. Edge-case incelemesinin
yeni/boş oturum, RLS filtrelemesinde hata ve büyük JSON öncesi byte sınırı
bulguları uygulandı. Verification-gap incelemesinin ayrı artwork bağları ve
reading yükümlülüğü sınırı için istediği testler eklendi; 11 kontrolün tamamı
son SQL ile yeniden geçti. `git diff --check` geçti.

## Kanıtın sınırı

İlk SQL optimizasyon tablolarının kayıtlarını toplamaz; bunlar `NOT_COLLECTED`
veya `NOT_DEPLOYED` olarak ayrılır. `coreComplete=true`, tamamlanmış optimizasyon
envanteri değildir; `complete=false`, `storageVerified=false`, `executable=false`
korunur. Snapshot'ta görülen metadata dosya varlığı/kalitesi veya üretim
profili/erişim/retention kabulü sağlamaz. P4b ölçüm eksikleri değişmez.

## Kullanıcı çıktısının alınması

İlk yapıştırılan metin tam 204.800 byte'ta JSON string'inin ortasında kesildi;
tam katalog olarak işlenmedi. Sonraki `inventory_json.csv` dosyası 13.131 byte,
tek hücre ve geçerli JSON'dur. [Doğrulama özeti](received-summary.json) yalnız
metadata ve sayımları içerir; ham medya yollarını içermez.

CSV'de `readOnly=off`, `isolation=read committed`, `coreComplete=false` ve
`LIMIT_OR_TRANSACTION_REJECTED` bulunur. Detay dizileri null'dır. Bütün sayılar
limit içindedir; ret nedeni işlem modunun istenen ayarlarda olmamasıdır. Kullanıcı
DataGrip/IntelliJ kullandığını belirtti; kesin yürütme sırası gözlenmedi. Bu çıktı
yazma yapıldığının kanıtı değildir, fakat salt okunur işlem kabulünü karşılamaz.

Gözlenen sayımlar: 78 STORY, 253 içerik-dil kaydı, 1.425 sayfa, 4.654 sayfa-dil
kaydı, 6.263 referans verilen medya kaydı, 102 legacy iş ve 5.160 reading
yükümlülüğü. Bunlar metadata sayımlarıdır; güncel storage doğrulaması değildir.
Listelenen 11 opsiyonel optimizasyon tablosu bu bağlantının `public` şemasında
`NOT_DEPLOYED` döner. Legacy türev dosyaların hiç bulunmadığı sonucu çıkarılamaz.
Tam çekirdek envanter için transaction modu korunarak yeniden dışa aktarım gerekir.
Ajan canlı bağlantı açmadı.

## İkinci CSV ve v2 transaction uyumu

İkinci CSV'nin snapshot zamanı `2026-10-21T07:40:07.479937+00:00`'dır.
[İkinci sonuç özeti](received-summary-second.json) yine `readOnly=off`,
`read committed` ve null ayrıntılar taşır; tam envanter olarak kabul edilmedi.
Gözlenen sayımlar önceki CSV ile aynıdır. Kesin istemci yürütme sırası bilinmiyor.

`p5a-manual-core-v2`, `BEGIN` sonrasında açıkça `SET TRANSACTION ISOLATION LEVEL
REPEATABLE READ, READ ONLY` kullanır. DataGrip rehberine Tx: Manual ve yeni
çıktıda sürüm kontrolü eklendi. Korumalar, veri seçimi ve yazma yasağı değişmedi.
Yeni SQL SHA-256: `ec43644dbd41d90ae73a31dedf2c2b5cd5e4e04fcbc4e33714456861a793cb0e`.

[Yerel v2 sonucu](local-verification-v2.json): önceki 11 kontrol ve beş transaction
kontrolü geçti. Yeni/önceden boş transaction içinde tam SQL doğru modu ve ayrıntıları
döndürür. Yalnız SELECT çalıştırılması eski CSV'deki ret sonucunu üretir.
Önceden veri sorgulanmış transaction'da mod değiştirme hata verir; yerel INSERT
read-only nedeniyle reddedilir. İlk kontrol betiğinin, yalnız önceden BEGIN
açılmasının eski davranışı üreteceği beklentisi yanlış çıktı; beklenti düzeltildi.
Bu hipotez canlıdaki kesin neden olarak sunulmaz. Production çalıştırması yoktur.

## Normal SELECT / CSV yönlendirmesi

Kullanıcı JSON yerine normal SELECT sonuçlarını CSV olarak dışa aktarmayı istedi.
[Yeni SQL](../../../../tools/image-optimization/p5a_csv_inventory.sql) dört ayrı
normal sonuç tablosu döndürür; JSON, oturum ayarı veya işlem modu nedeniyle sonuç
saklayan bir koruma içermez. DML/DDL, yan etkili fonksiyon ve dosya işlemi yoktur.
[Rehber](../../../../tools/image-optimization/P5A_CSV_INVENTORY.md), dört dosya
adını ve tam sonuç dışa aktarımını tanımlar. Önceki JSON sorguları tarihsel kalır.

[CSV yerel test sonucu](local-verification-csv.json), son SQL hash'iyle sekiz
kontrolün geçtiğini kaydeder. Testler yalnız önceki geçici yerel PostgreSQL'in
sentetik kopyasında yapıldı: dört SELECT'in salt okuma biçimi, eksik dil/sayfa
çevirisi, pasif/taslak STORY korunması, ortak/textless/listening kaynakları, null
byte, ilişkisiz veya hassas alanların dışarı çıkmaması, UTF-8/virgül/tırnak/satır
sonu CSV round-trip, read-only transaction uyumu ve başlıklı boş CSV.

Bu dört bağımsız çıktı aynı atomik snapshot sayılmaz. Kapsam bağlantı rolünün
görebildiği kayıtlardır; RLS/rol/yetki değişmez. Satır zamanları korunur ve
sorgular arası tutarsızlık önce zaman/kapsam farkı olarak incelenir. Bu hazırlık
aşamasında yeni CSV'ler henüz alınmamıştı; sonraki teslim ve analiz aşağıdadır.
Ajan canlı bağlantı açmadı.

## Dört CSV teslimi ve metadata analizinin kapanışı

Başlıksız dosyalar onaylı SELECT sütun sırasıyla okundu; ilk kayıtlar kaybedilmedi.
253 hikâye/dil, 4.654 sayfa/dil, 6.263 medya ve 102 legacy iş satırı var. Bunlar
78 benzersiz hikâye ve 1.425 benzersiz sayfaya karşılık gelir; önceki metadata
sayımlarıyla eşleşir. Dosya hash'leri ve sütun eşlemesi analiz JSON'undadır.
Kaynak dosyalar değiştirilmedi; ham medya object path'leri rapora kopyalanmadı.

Standart CSV analizi ve bağımsız pandas okuması şu sonuçlarda eşleşti: bütün
kimlik/satır sayıları, 9.252.083.241 toplam referanslı medya byte'ı, 4.906 benzersiz
okuma kaynağının 5.494.676.281 byte'ı, 133 okuma GIF'i, 342 eksik okuma SHA'sı,
4.654 tam sayfa/dil bağı ve #1/PT kaydındaki tek eksik kapak. Eksik tek kapak
cover/detail olarak iki yükümlülüğü etkiler; kaynak hacmi iki kez sayılmadı.

Blind ve edge-case incelemelerinde eyleme geçirilebilir bulgu yoktu.
Verification-gap incelemesinde doğrulama açığı bulunmadı; eski bekleme ifadelerinin
tamamlanmış analizle uyumsuzluğu düzeltildi. Son doğrulamada yerel Markdown
bağlantıları, sayı karşılaştırmaları ve kaynak hash'leri kontrol edildi;
`git diff --check` geçti.

P5a'nın bu metadata kapsamı tamamlandı. Sonuç storage, fiziksel kalite, immutable
admission, üretim profili, gerçek backfill veya rollback kabulü değildir.
Sorguların 6 dakika 18,51 saniyelik ayrı gözlemler olduğu ve bağlantı rolünün
görünürlük sınırı korunur. Canlı düzeltme, dönüşüm veya dağıtım yapılmadı.
