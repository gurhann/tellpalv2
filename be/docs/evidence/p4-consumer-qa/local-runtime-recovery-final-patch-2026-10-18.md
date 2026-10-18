# P4a5: gerçek koşu sonrası son offline patch

Tarih: 2026-10-18. [Gerçek koşu sonucu](verification-2026-10-18.md) korunur:
dört paint, dört read ve 13.170.884 byte ile dar P4a5 kabulü geçti.
Bu son tur yalnız cleanup yardımcısı, test kapsamı ve spec tarihçesini düzeltir.
Yeni gerçek başlangıç, login, ölçüm, DB, ADB veya SDK çağrısı yapılmadı.

## Kaynak kimliği ve gerçek kanıt ayrımı

Gerçek koşu `receipts/implementation-source-identity-runtime-recovery-03.private.json`
kaynak kimliğiyle yürüdü. Root'ın `receipts/final-runtime-verification-01.private.json`
makbuzu QA/backend PID yokluğunu, owned eşleme temizliğini ve byte/DB sınırlarını
ayrıca doğruladı. Bu dosyalar ve gerçek koşunun ham kayıtları değiştirilmedi.

Yeni `implementation-source-identity-runtime-recovery-04.private.json` yalnız bu son
offline patch'i tanımlar. Yeni kapanış helper'ı gerçek koşuda yeniden çalıştırılmış
sayılmaz. Aynı mobil/APK ve Java runtime kaynakları korunur; Java değişikliği yalnız
`P4ConsumerQaSetupTest.java` dosyasındadır. Önceki mobil 234/234, analyzer ve gerçek
PostgreSQL/Tomcat/modül 30/30 kanıtı geçerliliğini korur; tekrar çalıştırılmadı.

## Üç düzeltme

1. `close_owned_qa()`, aynı PID/cmdline/startTicks doğrulandıktan sonra yalnız bir
   owned `force-stop` gönderir. Sonrasında en çok iki saniye boyunca kalan süreyi
   paylaşan PID/kimlik sorguları yapar. `STOPPED_OWNED_QA` yalnız PID yokluğu gözlenince
   döner. Aynı süreç canlı kalırsa veya kimlik değişirse cleanup başarısızdır.
   Başka süreç durdurulmaz; ikinci stop, restart veya retry eklenmedi.
2. Yeni Java testi gerçek `credentialGate()` yolunu 0600 sentetik reservation/receipt
   ve `loginReserved=false` ledger ile çalıştırır. `LOGIN_RESERVED 3 1 1 1 21` satırı
   pencere öncesi yazılır. İlk verifier 22, 27 ek verifier'ın sonuncusu 48 olur;
   28'inci çağrı yeni satır yazmadan `AUTH_BUDGET` ile reddedilir. Son satır
   `CLOSED 0 0 28 0 0 0` olur. Ürün Java kodu değişmedi.
3. Ana spec'in non-frozen son bölümleri kısa tarihçe olarak düzenlendi. İlk eksik
   config, QA başlangıç hatası ve DB rol reddi geçmiş aşamalar olarak açıklandı;
   başarılı güncel sonuç ensona taşındı. Eski raporlar ve frozen bloklar korundu.
   Frontmatter `in-review` kaldı. Yeni gerçek koşu yetkisi eklenmedi.

## Offline doğrulama

| Kapı | Sonuç | Yeni özel ham kayıt |
|---|---|---|
| Python kurtarma ve eski P4 matrisi | 91/91 geçti | `logs/python-runtime-recovery-08.log` |
| Java ledger/composition/setup | 20/20 geçti; hata/atlama yok | `logs/java-runtime-recovery-05.log` |
| Kaynak, eski dosya, frozen, APK ve whitespace | Geçti | `receipts/implementation-audit-runtime-recovery-04.private.json` |

Python'daki altı yeni test, gerçek Android adapter'ını fake subprocess sınırında ve
gerçek `run()` cleanup yolunu fake cihazla sınar. Gecikmeli PID çıkışı başarıdır;
iki saniye canlı kalan süreç ve replacement başarısızdır. Her durumda en çok bir
force-stop vardır; replacement sürecine başka çağrı gönderilmez. Test stdout'undaki
paint/başlangıç mesajları sentetik sonuçlardır.

Java 04 ilk koşusunda yeni fixture'ın macOS `/var` alias'ı, gerçek private-file
kapısında `PRIVATE_FILE_REQUIRED` üretti. Test dizini `toRealPath()` ile normalize
edildi; Java 05 geçti. Başarısız 04 logu ayrı tutuldu. Java logundaki
`TEST_STOP_BEFORE_CONTEXT_DB_SDK`, önceki caller testinin beklenen kontrollü duruşudur.

Komutlar özel kaynak kopyasında mevcut Python matrisi ve offline Maven wrapper
hedef testleridir. Yeni kurulum/indirme, Colima, mobil veya IT çalışması yapılmadı.
Son patch öncesi 176 özel dosya ve 12 eski repo raporu ayrıca mühürlendi; token ve
gerçek ham kayıtlar yalnız hash ile denetlendi, kopyalanmadı. Ana ve kurtarma frozen
blokları, 3241 korunan repo dosyası, 72 eski run ve önceki 94 özel dosya korundu.

Bu patch gerçek koşuyu tekrarlamaz. P4b, fiziksel cihaz kalitesi, üretim profili ve
genel tüketici geçişi açık kalır. Normal BMad son inceleme/kayıt adımı root'a devredilir.
