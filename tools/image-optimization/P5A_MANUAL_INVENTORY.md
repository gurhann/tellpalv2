# P5a — Kullanıcının çalıştırdığı salt okunur sorgu

[SQL dosyasının](p5a_manual_inventory.sql) tamamını canlı PostgreSQL'in sorgu editöründe,
önceden açık transaction bulunmayan yeni ve boş bir oturumda, aynı bağlantıda çalıştır.
Bağlanmadan önce seçili projenin TellPal, ortamın production
ve servisin Postgres olduğunu kontrol et. Dosya bağlantı veya credential içermez.

Son SELECT'in döndürdüğü **`inventory_json` hücresinin tam değerini** `.json` olarak dışa
aktar ve bu sohbete dosya olarak ekle. Büyük çıktıyı sohbet metnine yapıştırma.
Editörün hücre boyutu sınırı nedeniyle metni kesmediğini kontrol et. CSV olarak dışa
aktarırsan tek hücrelik CSV dosyasını olduğu gibi gönderebilirsin.

## DataGrip / IntelliJ

Canlı bağlantı için yeni ve boş bir query console aç. Bu konsolun işlem modu
seçicisinden **Tx: Manual** seç; böylece komutlar arasında otomatik commit yapılmaz.
[JetBrains transaction modu belgesi](https://www.jetbrains.com/help/datagrip/submitting-and-reverting-changes.html).
Settings → Database →
Query Execution → For selection execute alanında **Exactly as a single statement**
seç. Ardından SQL dosyasının tamamını, `BEGIN` ve sondaki `ROLLBACK` dahil, seçip
Execute ile çalıştır. Yalnız `WITH ... SELECT` bölümünü çalıştırma. Bu seçenek,
seçilen komutları tek parça halinde gönderir:
[JetBrains sorgu yürütme belgesi](https://www.jetbrains.com/help/datagrip/query-execution.html).

Sonuçta `readOnly=on`, `isolation=repeatable read` ve, sınırlar aşılmadıysa,
`coreComplete=true` beklenir. `complete=false` ilk çekirdek dökümü için normaldir.
`readOnly=off`/`read committed` çıktısında ayrıntı dizileri koruma nedeniyle null
kalır; bu dosya tam envanter sayılmaz. Böyle bir çıktı tek başına veri yazıldığını
göstermez.

Güncel sorgu sürümü **`p5a-manual-core-v2`**'dir; dışa aktarılan yeni dosyada
`queryVersion` değerinin bu olduğunu kontrol et. İstemcinin önceden boş bir
transaction açması durumunu da karşılamak için `BEGIN` sonrasında açıkça
`SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY` kullanır.
Bu komut ilk veri sorgusundan önce çalışmalıdır. Daha önce veri sorgulanmış bir
transaction'da hata alırsan yeni ve boş oturum kullan; korumayı kaldırma.
Bu, olası bir istemci davranışına karşı uyum düzeltmesidir; önceki başarısız
çıktıların kesin yürütme nedeni gözlenmedi.
[PostgreSQL SET TRANSACTION belgesi](https://www.postgresql.org/docs/18/sql-set-transaction.html).

Sorgu `REPEATABLE READ READ ONLY` transaction açar; yalnız SELECT ve transaction'a
özel süre ayarları kullanır; sonunda `ROLLBACK` yapar. Uygulama verisi, görsel,
iş durumu ve kalıcı ayar değiştirmez. Hata olursa aynı bağlantıda `ROLLBACK;`
çalıştır ve hatayı paylaş; script'i değiştirilmiş bir sürümle tekrar çalıştırma.

İlk çıktı tüm STORY kayıtlarının dil/kapak/sayfa bağlarını, yalnız referans verilen
medya metadata'sını ve legacy iş durumlarını içerir. Taslak, arşiv ve pasif kayıtlar
korunur. Metin gövdeleri, sesler, cached URL'ler, kullanıcı/admin/satın alma verisi,
token ve serbest hata mesajları dışarı aktarılmaz. Object path'ler medya referansıdır;
raporu herkese açık yayımlama.

Satır güvenliği (RLS) veriyi filtreleyecekse sorgu eksik kataloğu başarılı saymak
yerine hata verir; yetki veya policy değiştirilmez.

Yeni reading/listing/page-manifest tablolarının yalnız şeması ve varlık durumu ilk
çıktıya girer. Tabloların kayıtları bu sorguda okunmaz: `NOT_COLLECTED` eksik
toplamayı, `NOT_DEPLOYED` tablo yokluğunu gösterir. Sonraki salt okunur sorguyu bu
gerçek şemaya göre hazırlayacağız; optimizasyon envanteri şimdiden tamamlandı sayılmaz.

Sorgu 10 saniyelik statement sınırı, 1 saniyelik lock sınırı ve 15 saniyelik boşta
transaction sınırı kullanır. En fazla 10.000 hikâye, bölüm başına 100.000 kayıt,
100.000 reading yükümlülüğü ve 64 MiB çıktı kabul eder. Sınır aşımında eksik veri
tam envanter gibi sunulmaz. Büyük JSON oluşturulmadan önce seçilen satırların
metadata toplamı 60 MiB ile sınırlandırılır. Eşzamanlı veri değişimleri tek snapshot
içinde karışmaz.

Dosya varlığı, gerçek piksel ölçüleri/kalitesi ve storage hash'i bu sorguyla
doğrulanmaz. Genel medya kaydında bulunmayan ölçüler için değer uydurulmaz.
Legacy `COMPLETED`, yeni READY/LIVE veya geçiş onayı sayılmaz.
