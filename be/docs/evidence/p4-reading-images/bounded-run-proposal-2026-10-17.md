# P4a2: tek gerçek görselli deneme planı

Tarih: 2026-10-17. Durum: **Önerildi; kullanıcı onayı ve çalıştırma kapıları bekleniyor.**
Kod temeli: `8f7adab818db262c9faa2385d299b3f851c3ddad`.
Sayısal kayıt: [bounded-run-proposal-2026-10-17.json](bounded-run-proposal-2026-10-17.json).

## Kullanıcının göreceği sonuç

Açık Android emülatöründe TR71'in seçilmiş kapağı ve ilk iki sayfası gerçek görsellerle açılır.
Kullanıcı aynı alanda tam boy canonical PNG kaynak ile hazırlanan PNG çıktısı arasında geçer.
Seçilen rendition, iki byte tabanı, decode boyutu/süresi ve görüntü kaydedilir. Bu ilk deneme
emülatör kabulüdür; fiziksel Android/iPhone kalite veya üretim profili onayı sağlamaz.

Kaynak adayları, yalnız yeni ücretsiz QA kopyası, canonical QA UID, authenticated teslimat ve
`private,no-store` için önceki onaylar korunur. Şimdi istenen karar aşağıdaki **tek denemenin
sayısal sınırlarıdır**. Genel erişim/cache/session veya üretim politikası değiştirilmez.

## Kaynak ve boyut

| Kaynak | Yerel canonical PNG byte | Kaynak boyutu |
|---|---:|---|
| Kapak, eski asset 9778 | 6.336.857 | 1712×2778 |
| Sayfa 1, eski asset 9816 | 5.941.567 | 1712×2778 |
| Sayfa 2, eski asset 9818 | 6.122.778 | 1712×2778 |

Üç dosyanın byte ve SHA-256 değeri bu hazırlıkta yeniden doğrulandı. Toplam 18.401.202 byte.
Hash'ler sayısal kayıtta sabittir; farklı dosya aynı denemeye alınamaz. Eski JPEG tabanı ayrıca
raporlanır; bu PNG'ler eski JPEG'den yaklaşık 5–6 kat büyüktür. Transfer kazancı henüz bilinmez.

Önerilen çıktı **822×1334 PNG**, P2 hedef kutusu **823×1334** olur. Bu değer, açık Medium_Phone'un
etkin `822×1828 / DPR 2.0` ölçümüne dayanır. Kapak `cover`, sayfalar `contain` kullanır; iki eksenli
tam görüntü ihtiyacı her ikisinde `822×1334` çıkar. P2 floor yuvarlaması için hedef kutusunun
genişliği bir piksel artırılır. Önceki 1080×1753 önerisi başka emülatör geometrisine aitti.

Üç rol için aynı deneme kutusu kullanılır: `READING_COVER`, `READING_DETAIL`, `READING_PAGE`.
İki sayfayla birlikte dört çıktı vardır. UI Kapak seçimi detail rolünü gösterir; manifestteki cover
rolü ayrıca bütünlük için üretilir, normal Library tüketicisinin kabulü sayılmaz. Her rolde tek
aday vardır. Mevcut iki eksenli seçici ve eksiklik hatası korunur; original fallback yoktur.
Çıktı kaynak byte'ına eşit veya büyükse iş reddedilir: **growth izni önerilmiyor**.

## Yerel ortam ve yalıtım

Mevcut yerel uygulama DB'si yalnız okunur. Özel bir yerel QA klonu oluşturulur; V37 yalnız klonda
uygulanır. Klonun adı, run kimliği ve `local/p4a2-<UUID>` prefix'i sayısal kayıtta sabittir. Yeni
tek QA içerik kimliği uygulama komutuyla tahsis edilip makbuza mühürlenir; 71 ve 93 seçilemez.
Bir Türkçe localization ve tam iki sıralı sayfa oluşturulur. Yalnız bu kopya ücretsiz olur;
kategori/home bağı eklenmez. Metin ve mevcut ses metadata referansları korunur, ses byte'ı yazılmaz.

Kaynaklar, yeni admitted kimlikleriyle kopyaya bağlanır. Legacy processing `PENDING` kalır.
Normal list/detail/pages/Library API'lerinin kopyayı göstermediği, üretim öncesi metadata
fixture'ıyla ve sonrasında gerçek yerel HTTP ile doğrulanır. Görünürlük yalıtımı geçmezse bulut
üretimi durur; visibility/processing bayrağı elle tamamlanmaz. Klondaki eski kayıtların baseline'ı
da karşılaştırılır. Gerçek DB, TR71, #93 ve #112 değiştirilmez.

Mevcut P3 hazırlık aracı deseni izlenir: scheduler bean tanımı context refresh öncesi kaldırılır;
yalnız property'ye güvenilmez. Legacy poller, genel P3 reconciler ve worker polling çalışmaz.
Yeni kopya için uygulamanın Publish komutu bir kez çağrılır. Ardından yalnız yeni ID için
`reconcile(id,tr)`, o ID'nin job'ı için `worker.run(jobId)`, sonra yine aynı scope için reconcile
çağrılır. LIVE, mevcut tam doğrulama ve otomatik adoption koduyla oluşur; DB'ye hazır/LIVE yazılmaz.
Başka scope veya ikinci job attempt çalıştırılmaz.

Backend ayrı loopback portunda ve yalnız klon DB ile başlar. ADB reverse yalnız o porta bağlanır.
Mevcut süreçler durdurulmaz. QA paket kimliği mevcut TellPal'dan ayrıdır. Hesap, mevcut geçici
`ImageExperimentCredentials` sağlayıcısıyla ve yalnız `jLRypTmoBrhhX21HsDJojurcKTC3` UID'siyle
bağlanır. Stub kapalı ve revoke kontrolü açık olur. Token derlenmez, UI'ye veya repoya yazılmaz;
normal uygulamaya auth paketi/akışı eklenmez. Giriş yalnız debugger/test kompozisyonuyla yapılır.

## Sayısal kaynak sınırları

MiB = 1.048.576 byte. Bu değerler çalışma reddi sınırlarıdır; ölçülmüş güvenli üretim bütçesi değildir.

| Alan | Öneri |
|---|---|
| Kaynak/çıktı | Her biri en çok 8 MiB; en çok 4.800.000 piksel, tek kare, mevcut RGB8 PNG kabulü |
| Backend JVM | `-Xmx1024m`, direct 128 MiB; P2 parent rezervasyonu 256 MiB |
| P2 encoder child | Heap 256 MiB, direct 16 MiB, RSS 512 MiB, temp 32 MiB |
| Hazırlama | Tek kapasite, tek encoder thread; işlem 30 sn, her servis yürütmesi için revision 180 sn |
| Teslimat | Encoded 8 MiB, 4.800.000 piksel, bellek hesabı 256 MiB, deadline 30 sn |
| Mobil | Tek owned image; encoded 8 MiB, buffer 24 MiB, decode 4.800.000 piksel/20 MiB, hesaplanan peak 128 MiB |
| Mobil zaman | Transfer 45 sn, decode 10 sn; admission/control penceresi 180 sn |
| Status | 10 saniyede bir, en çok 18 kontrol; hata halinde görsel temizlenir ve otomatik retry yapılmaz |
| Teslimat toplamı | V37 run sayacı en çok 64 mantıksal işlem ve 64 MiB storage payload rezervasyonu |

P2 worker lease'i mevcut kodda revision+5 sn, yani 185 sn olur; P3 claim lease'i mevcut 5 dakikadır.
Bu değerler değiştirilmez. Backend/mobil bellek rakamları SDK/engine/GPU/RSS toplamının ölçülmüş
tepe sınırı değildir. Gerçek göstergeler ayrıca kaydedilir; bulunamayan ölçüm açık bırakılır.
SDK socket inactivity süresi mutlak in-flight deadline değildir. 15 dakikalık yeni işlem başlatma
penceresi belirlenir; sonrasında yeni I/O/yayın yoktur. Başlamış SDK çağrısının mutlak sonlanması
kanıtlanmış sayılmaz; takılırsa yalnız sahipliği doğrulanmış QA süreçleri durdurulur ve çalışma başarısız kaydedilir.

## Bulut çağrıları ve byte sınırları

Tek run için sınırlar: **13 yeni nesne**, toplam kalıcı byte üst sınırı **104 MiB**;
**14 conditional create denemesi / 112 MiB write payload**;
**64 pinned read / 512 MiB read payload**; en çok **16 observeVersion** çağrısı.
Storage API yüzeyinde toplam üst sınır 94 mantıksal çağrıdır. Firebase verifier en çok 64 kez,
QA sign-in en çok bir kez çağrılır; birleşik mantıksal üst sınır 159 olur. Bunlar HTTP/TLS byte'ı,
SDK'nin iç chunk/auth istek sayısı veya faturalandırılan işlem sayısı değildir; o sayılar iddia edilmez.
Silme/list-bucket/backfill yoktur; ayrı run/prefix açıp sayaç sıfırlamak yoktur.

| Aşama | Create denemesi | Pinned read | Açık observe |
|---|---:|---:|---:|
| Üç canonical kaynağı create-only yükle ve doğrula | 3 | 3 | 0 |
| Üç kaynak admission'ı | 3 | 9 | 3 |
| Birinci scope reconcile / dört obligation capture | 4 | 8 | 4 |
| Tek job worker / gerçek encoder / kaydedilmiş çıktı doğrulaması | 4 | 12 | 0 |
| İkinci scope reconcile / kaynak ve çıktı doğrulaması / adoption | 0 | 8 | 0 |
| Altı karşılaştırma durumu ve V37 tavanı içindeki etkileşim payı | 0 | en çok 24 | 0 |

Kapak/detail aynı SHA'yı kullanır. Capture dört create dener fakat yalnız üç farklı source nesnesi
oluşturur. Beklenen tek 412 conflict'in iç metadata okuması observe tavanına dahildir. 13 nesne
tahmininin çağrı sayısına eşit olmamasının nedeni budur. Yeni namespace'te başka conflict beklenmez;
başka conflict veya hash farkında çalışma durur. Temel başarılı hazırlık/adoption yolu 40 read'dir.

V37 yalnız teslimatı kapsar. Hazırlık için P3 aracındaki tek kullanımlık özel makbuz deseniyle
aşamaların en kötü çağrı/byte payı **çağrıdan önce** kalıcı rezerve edilmelidir. Alt aşama tekrarına
izin verilmez; hata ve restart harcananı geri vermez. Ek storage process/IPC/retry katmanı kurulmaz.
Bu koruma henüz yeni gerçek fixture çalıştırıcısına bağlanmış veya test edilmiş sayılmaz.

## Başlatma kapıları ve eldeki kanıt

Yerel Java kontrolü gerçek `TestProfile`, delivery `Limits` ve parent heap admission'ından geçti;
üç rol fingerprint'i sayısal kayıtta bulunur. Yerel Dart kontrolü gerçek `ReadingComparisonLimits`
constructor'ından geçti. Bu kontroller hesap/token/HTTP/storage/encoder çağırmadı. Üç canonical
dosyanın hash'i ayrıca yeniden doğrulandı. Bu hazırlıktaki bulut çağrısı **0** oldu.

Çalıştırma öncesi aşağıdaki kapılar kapanmalıdır; kullanıcı kapsamı onaylasa bile başarısız kapı
bulut üretimini açmaz:

1. Sayısal profil ve bu tek çalışma kapsamı açıkça seçilir.
2. Tek kullanımlık makbuz/aşama rezervasyonu, scope/job sınırı, scheduler yokluğu, kesinti/restart
   ve normal API yalıtımı gerçek yerel servislerle sınanır. Yeni harness kodu BMad uygulama ve
   bağımsız incelemesinden geçer; bu plan o çalışmanın tamamlandığı iddiası değildir.
3. Kaynak DB salt okunur kimliği, klon ayrımı/V37, üç dosya hash'i, prefix, gerçek credential ve
   mevcut runtime fingerprint'leri yürütme öncesi yeniden doğrulanır. Drift varsa yeni aday üretilmez.
4. İlk bulut çağrısından önce salt okunur son preflight makbuzu yazılır. Tek run kilidi ve önerilen
   tavanlar mühürlenir. Eksik değer, yeni dosya/profil, dolu port veya belirsiz süreçte başlanmaz.

Başarılı canlı denemede job/revision/manifest/rendition kimlikleri, pinned generation/SHA/byte,
tam kaynak ve bounded decode boyutları, HTTP payload sayacı, görüntüler ve elde edilebilen kaynak
göstergeleri kaydedilir. Ekran/account/background/lease hatasında temizleme davranışı mevcut Cubit
yolunda izlenir; genel P4b yeterliliği çıkarılmaz. Kullanıcıdan ancak gerçek görseller geldikten
sonra yazı netliği ve görünür fark değerlendirmesi istenir.

## Kararın sınırı

Bu plan tek Türkçe QA kopyası ve açık Android emülatörün bu geometrisi içindir. Başka boyut, profil,
cihaz, kaynak veya ikinci başarısız çalışma otomatik yetkilendirilmez. JPEG/WebP sorunu, P0 eksik
ham kanıtı, fiziksel cihaz kabulü, normal mobil geçiş, P4b, üretim dağıtımı ve genel politika açıktır.
Başarısızlıkta kanıt ve kısmi nesneler tutulur; otomatik cleanup veya retry yapılmaz.
