# P5a — Canlı hikâye metadata envanteri

2026-10-21. Kullanıcının canlı bağlantısından dışa aktardığı dört CSV birleştirildi.
Bu kapsamda metadata envanteri tamamlandı. Hiçbir dosya indirilmedi, canlı bağlantı
açılmadı veya veri değiştirilmedi. Sonuç geçiş/dönüşüm yetkisi ve storage doğrulaması
değildir. Makine tarafından okunabilir sayımlar ve içerik/dil ayrıntıları
[analiz kaydında](csv-inventory-analysis.json) bulunur.

## Sonuç

78 hikâyenin 253 dil kaydı, 1.425 sayfası ve 4.654 yerelleştirilmiş sayfa bağı var.
Aktiflik ve editoryal durum alanlarına göre 78 hikâyenin tamamı aktif, 253 dil
kaydının tamamı `PUBLISHED`. Bu durumlar görsellerin yeni sözleşmeye hazır olduğunu
göstermez.

| Dil | Hikâye kaydı | Sayfa görseli bağı | Okuma kaynaklarının kayıtlı byte toplamı |
|---|---:|---:|---:|
| Türkçe (`tr`) | 78 | 1.425 | 2.015.089.837 |
| İngilizce (`en`) | 77 | 1.401 | 1.956.382.989 |
| Portekizce (`pt`) | 67 | 1.260 | 1.201.118.095 |
| Almanca (`de`) | 29 | 539 | 306.019.451 |
| İspanyolca (`es`) | 2 | 29 | 16.065.909 |
| Toplam | 253 | 4.654 | 5.494.676.281 |

Dil sayılarının farklı olması çeviri kapsamıdır; her hikâyenin her dilde olması
zorunluluğu bu veriden çıkarılmadı. Mevcut her içerik/dil kaydı için beklenen bütün
sayfa çevirileri ve görsel referansları bulunuyor. Sayfa sayıları içerikte beyan
edilen sayıyla eşleşiyor; sıralar 1'den başlayarak kesintisiz. Yinelenen kimlik,
farklı içeriğe bağlanan sayfa veya NULL olmayan referansın medya CSV'sinde
karşılıksız kalması görülmedi. Tek zorunlu eksik aşağıdaki okuma kapağıdır.

## Geçiş öncesi somut bulgular

### 1. Haberci Nota'nın Portekizce okuma kapağı eksik

`content_id=1`, `localization_id=3`, `language_code=pt` kaydında
`reading_cover_media_id=NULL`. Kayıt başlığı `Melody - A Pardal Mensageira`.
Editoryal durum `PUBLISHED`, legacy durum `COMPLETED`.

Bu tek eksik kaynak hem `READING_COVER` hem `READING_DETAIL` yükümlülüğünü eksik
bırakıyor. Diğer dilden kapak veya yazısız kaynak otomatik atanmadı. Legacy
tamamlanma bayrağının yeni readiness kabulü olmadığı bu kayıtta doğrudan görülüyor.
Kaynak: başlıksız `hikayeler.csv` dosyasının ikinci CSV kaydı, sorgunun 11. sütunu.

### 2. Okuma kaynaklarında 133 GIF kaydı var

Okuma rollerinde 4.906 benzersiz kaynak var: 4.709 JPEG, 64 PNG ve 133 GIF MIME
kaydı. GIF'lerin 132'si sayfa, biri kapak kaynağıdır. Kapak/detail aynı kaynak
olduğu için GIF rol bağı toplamı 134, benzersiz GIF kaynağı 133'tür.

Bu kayıtlar sekiz hikâyenin 16 içerik/dil kapsamına dağılır; hepsi TR/EN'dir:

- #6 Nota ve Haftanın Günleri
- #7 Nota ve Mevsimler
- #10 Zıpzıp Kuki ile Çiko
- #14 Ritmi Bozuk Saat
- #16 Meşe Palamudu ile Ay Çekirdeği
- #17 Bir Hıçkırık Meselesi
- #19 Dedektif Lily
- #23 Fancy'nin Çorapları

MIME kaydı animasyon veya gerçek dosya formatı doğrulaması değildir. GIF'lerin
frame sayısı bilinmiyor. [Dar P2 kabulü](../../adr/ADR-0022-reading-image-preparation-engine.md)
PNG/JPEG içindir; bu kayıtlar otomatik düzleştirme veya GIF→PNG dönüşümü varsayımıyla
geçirilemez. Ayrı kaynak/animasyon değerlendirmesi gerekir.

### 3. Byte kimliği ve piksel bilgisi eksik

6.263 referanslı medya kaydının 470'inde SHA-256 alanı yok. Bunların 342'si okuma
kaynağıdır: 20 kapak ve 322 sayfa. Eksik SHA 22 içerik/dil kapsamını etkiliyor.
Verilmiş SHA değerleri biçim kontrolünden geçti; gerçek dosyaya karşı doğrulanmadı.
Kayıtlı hash'in bulunması güncel byte veya immutable admission kanıtı değildir.

Bütün 6.263 kaydın pozitif byte değeri var. Piksel ölçüsü, orientation, alpha,
renk profili, frame sayısı ve generation bu CSV'lerde bulunmuyor. Boyut/kalite
profili ve tahmini kazanç seçmek için bu bilgiler yeterli değil.

### 4. Kaynak toplamları ile uygulama transferi ayrı

| Kapsam | Benzersiz medya kaydı | Kayıtlı byte toplamı |
|---|---:|---:|
| Bütün referanslı medya | 6.263 | 9.252.083.241 (9,25 GB) |
| Yalnız okuma cover/detail/page kaynakları | 4.906 | 5.494.676.281 (5,49 GB) |

GB burada 1.000.000.000 byte'tır. Bunlar DB metadata toplamlarıdır; bucket'ta
ölçülmüş gerçek storage, bütün tarihsel türevler veya uygulamanın bir yolculukta
indirdiği miktar değildir. Medya CSV'si yalnız kaynak referanslarını kapsar ve
hepsi `ORIGINAL_IMAGE` olarak kayıtlıdır; başka optimize türevlerin hiç bulunmadığı
sonucu çıkarılmaz.

Okuma kaynaklarının 1.236'sı 1 MiB, 120'si 5 MiB, 35'i 10 MiB üzerindedir.
Bunlar açıklayıcı boyut dilimleridir; onaylı profil sınırları değildir. En büyük
okuma kaydı #969'dur: MIME `image/gif`, 21.445.950 byte (21,45 MB).

5.160 zorunlu okuma rol bağı vardır; iki bağ aynı eksik PT kapak nedeniyle NULL'dır.
Mevcut 252 kapak hem cover hem detail rolünde kullanılır. Aynı dosya bu iki role
sayılıp kaynak hacmi iki kez toplanmadı. Rol bağları üzerinden tekrarlarıyla
toplamak 5.712.098.425 byte verir; benzersiz kaynak toplamı 5.494.676.281 byte'tır.

### 5. Legacy işler görsel hazırlama kanıtı değil

253 içerik/dil kaydında legacy durum 65 kez `COMPLETED`, 188 kez `PENDING`.
Ayrı iş CSV'sindeki 102 kaydın tamamı `STORY_NARRATION/COMPLETED`; bunlar görsel
job'ı değildir. Bu bayraklardan yeni reading manifest veya LIVE kabulü üretilmedi.

Önceki 10:40 Türkiye saatli şema çıktısında 11 opsiyonel reading/listing/page
optimizasyon tablosu `public` şemasında `NOT_DEPLOYED` idi. Dört yeni CSV bu tablo
varlığını yeniden ölçmez. Önceki şema bulgusu güncel dosya yokluğu veya hiç
optimizasyon yapılmadığı iddiasına çevrilmez.

## Ayrı artwork alanları

10 yazısız kapak, 182 yazısız sayfa ve 32 listening kapak alanı NULL. Bu alanlar
yerelleştirilmiş reading kaynağının yerine kullanılmadı. Salt STORY/READING
kapsamında bu NULL'lar tek başına eksik okuma görseli veya yayın hatası sayılmadı.

## Sonraki doğrulama için örnekler

Bunlar kullanıcıya sunulan adaylardır; cohort seçimi veya çalıştırma yapılmadı.

| Örnek | Neden | Benzersiz okuma kaynağı ve kayıtlı toplam |
|---|---|---|
| #18/TR Kocaman Bir Sarılma | Daha küçük JPEG kontrolü; bütün kaynakların kayıtlı SHA alanı var | 17 dosya, 6.191.423 byte |
| #62/TR Pofuduk Mısır Pops | Büyük PNG örneği; kayıtlı SHA alanları var | 24 dosya, 120.305.904 byte |
| #14/TR Ritmi Bozuk Saat | GIF değerlendirmesi gerektiren örnek; otomatik P2 geçişine alınmaz | 16 dosya, 124.332.513 byte |

Önce eksik PT kapak kararı ve GIF kapsamı netleştirilir. Sonra açıkça seçilecek
kaynaklarda salt okunur dosya/ölçü/byte doğrulaması planlanabilir. Üretim profili,
P4b ölçümleri, erişim/cache/session/retention, gerçek backfill ve rollback kabulü
açık kalır. Bu envanter kaynak düzeltme, dönüştürme veya dağıtım başlatmaz.

## Kaynak ve doğrulama sınırı

Kaynaklar `hikayeler.csv`, `sayfalar.csv`, `medya.csv` ve `legacy_işler.csv`.
Dosyalar başlıksızdır; sütunlar [gönderilen SELECT sırasından](../../../../tools/image-optimization/p5a_csv_inventory.sql)
atanarak ilk kayıtlar korunmuştur. Satır sayıları sırasıyla 253, 4.654, 6.263 ve
102'dir; önceki metadata sayımlarıyla eşleşir. Kaynak hash'leri, sütun eşlemesi,
tam sayımlar ve 253 kapsamın ayrıntıları analiz JSON'unda bulunur. Kaynak CSV'ler
değiştirilmedi; ham medya object path'leri bu rapora/JSON'a kopyalanmadı.

Sorgular 21 Ekim 2026 Türkiye saatiyle 10:55:27–11:01:46 arasında alınmıştır;
6 dakika 18,51 saniyelik ayrı gözlemlerdir. Tek atomik snapshot veya bağlantı
rolünün görebildiğinden daha geniş katalog kapsamı iddia edilmez. Satırlar,
kimlikler ve boyut toplamları standart CSV işleme ile hesaplandı; bağımsız pandas
okumasıyla sayımlar, kaynak byte toplamları, GIF/SHA adetleri, beklenen bütün sayfa
bağları ve tek eksik kapak tekrar doğrulandı. Dosya varlığı/kalitesi ölçülmedi.
