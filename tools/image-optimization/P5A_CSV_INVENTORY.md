# P5a — Normal SELECT ve CSV

[SQL dosyasındaki](p5a_csv_inventory.sql) dört SELECT'i DataGrip'te ayrı ayrı
çalıştır. Her sonuç tablosunun tüm satırlarını, sütun başlıklarıyla CSV dışa aktar:

1. `hikayeler.csv`: hikâye/dil, editoryal durum ve kapak referansları.
2. `sayfalar.csv`: sayfa sırası, kayıtlı dil ve görsel referansları.
3. `medya.csv`: bu kaynakların referans verdiği medya metadata'sı.
4. `legacy_isler.csv`: kayıtlı eski iş durumları.

Bu dört dosyayı ayrı ayrı veya tek ZIP içinde sohbete ekle. Sonuç hücresini JSON
olarak kaydetmen gerekmez. Yalnız görünen sayfayı değil tam sonucu dışa aktar.
Boş sonuç varsa sütun başlıklarını koru ve sıfır satır olduğunu belirt.

Sorgular yalnız SELECT ve PostgreSQL'in zaman fonksiyonunu kullanır; INSERT,
UPDATE, DELETE, DDL, transaction/oturum ayarı, job veya dosya işlemi içermez.
Bağlantı bilgisi ve credential gerekmez; sorguları mevcut canlı bağlantında sen
çalıştırırsın. Ajan canlıya bağlanmaz.

Dört sorgu ayrı anlarda çalışabilir; `observed_at` zamanı her CSV'de bulunur.
Çıktılar tek atomik snapshot olarak sunulmaz. Dışa aktarımlar arasında veri
değişmişse eksik görünen bağ yeniden kontrol edilir; doğrudan bozuk veri sayılmaz.
Önceki JSON işleminin salt okunur/isolation kabulü bu dosyalara atfedilmez.
RLS uygulanıyorsa görünmeyen kayıtlar olabilir; sonuç kapsamı bağlantı rolünün
görebildiği kayıtlar olarak belirtilir. RLS, rol ve yetkiler değiştirilmez.

Kayıtlı bütün STORY kayıtları, pasif/taslak/arşiv durumlarıyla korunur. Sayfa
çevirileri başka dilden doldurulmaz. İlişkisiz medya, ses/metin gövdeleri,
cached URL, kullanıcı/admin/satın alma verisi ve serbest hata mesajları alınmaz.
`medya.csv` bağımsız tarihsel türevlerin tamamını kapsamaz; yalnız belirtilen
kaynak referanslarını içerir. Eski `COMPLETED` yeni optimizasyon kabulü değildir.
Piksel ölçüsü, gerçek dosya varlığı/hash'i ve görsel kalite ayrıca doğrulanmadıkça
bilinmiyor kalır. Dosyaları herkese açık yayımlama.
