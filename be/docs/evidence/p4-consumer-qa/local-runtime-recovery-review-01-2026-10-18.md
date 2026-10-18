# P4a5 kurtarma: hedefli inceleme düzeltmeleri

Tarih: 2026-10-18. Bu kayıt [ilk kurtarma devrine](local-runtime-recovery-implementation-2026-10-18.md)
eklenen dar düzeltmeleri tutar. Önceki rapor, log, makbuz ve frozen bloklar korunur.
Gerçek runtime kabulü açık kalır; bu alt ajan gerçek ADB, backend, DB, Firebase,
auth veya medya çağrısı yapmadı. Root'ın ayrı salt okunur Java/JDBC ve cihaz ön
kontrolleri bu test sonuçlarından ayrıdır.

## Kapatılan bulgular

1. **Reverse çağrısının yan etki sonrası hata vermesi:** `run()` önce seçilen cihazdaki
   reverse listesini doğrular. Hedef portta mevcut eşleme varsa komut ve cleanup yoktur.
   Yeni `backend-02/reverse-intent.private.json` içinde proposal, serial, PID/startTicks,
   device/host portları ve başlangıçtaki yokluk kaydedilir. Cleanup sahipliği, ADB çağrısından
   önce bellekte de ayrılır. Çağrı timeout olsa bile yalnız hedef port ve aynı remote ile
   eşleşen yeni kayıt temizlenir; farklı remote veya bozuk liste açık cleanup hatasıdır.
   Diğer reverse/forward eşlemeleri korunur.
2. **HOME sonrası örtük yeni başlangıç:** HOME öncesi, mevcut `am start` dönüşünden hemen
   önce ve hemen sonra aynı PID/cmdline/startTicks zorunludur. Gözlenen ölüm/değişimde
   dönüş çağrısı yapılmaz. Çağrı sonrası değişim sonucu başarısızdır. Başarılı yerel kontrol
   `foreground-return.private.json` dosyasına yazılır. Bu dosya
   `atomicProcessRaceExcluded: false` taşır: kontrol ile komut arasındaki OS yarışı mutlak
   olarak dışlanmış sayılmaz. Yeni retry, Android katmanı veya APK eklenmedi.
3. **İkinci JVM'in aktif denemeyi bozması:** `run()` yalnız bu çağrı startup claim'ini
   kazandıktan sonra `failure.json` yazabilir. Aynı veya başka süreç sahibinin claim'i
   yüzünden reddedilen çağrı, aktif backend'e hata dosyası bırakmaz. İlk kendi claim'ini
   kazanan çağrının sonraki gerçek hatası hâlâ kaydedilir.
4. **Datasource caller doğrulama boşluğu:** Yeni test gerçek `context()` yolunu çağırır.
   Gerçek Spring Boot `environmentPrepared` olayında kullanıcı hem etkin değerden hem
   `commandLineArgs` kaynağından doğrulanır. Dosya ve sistem özelliğindeki `tellpal`
   varsayılanı yenilir. Test, context/bean/DB/SDK oluşmadan kontrollü exception ile durur.
   `context()` içinden username argümanı kaldırılırsa bu test geçmez.
5. **Recovery run caller doğrulama boşluğu:** `ActualRunSafetyTest`, eski kapalı backend ve
   mühürlü kanıtları içeren recovery fixture'ıyla gerçek `run()` yolunu çalıştırır.
   Ayrı QA/backend rezervasyonu, claim, yeni auth ve `connection-03` kullanılır.
   `mobile.attempt.json`, reverse intent ve backend cleanup yalnız `backend-02/` ile
   ilişkilendirilir. Eski backend/connection/ledger dosyaları byte düzeyinde aynı kalır.
6. **Gerçek Android kurulum yolu:** Root'ın salt okunur ön kontrolünde görülen
   `/data/app/~~.../.../base.apk` biçimi artık kabul edilir. Aynı gerçek arm testindeki
   sentetik fixture iki tilde içerir. Shell karakterleri, whitespace, path traversal,
   çoklu/split APK ve `base.apk` dışı sonuçlar reddedilir. Kurulum yapılmaz.

## Çalışan kapılar

| Kapı | Sonuç | Yeni özel ham kayıt |
|---|---|---|
| Python kurtarma ve eski P4 matrisi | 85/85 geçti | `logs/python-runtime-recovery-07.log` |
| Java ledger/composition/setup | 19/19 geçti; hata/atlama yok | `logs/java-runtime-recovery-03.log` |
| Kaynak ve özel kopya | Beş kaynak/fixture aynı SHA | `receipts/implementation-source-identity-runtime-recovery-03.private.json` |
| Eski dosya/frozen/APK/whitespace denetimi | Geçti | `receipts/implementation-audit-runtime-recovery-03.private.json` |

Python 06 ara koşusu da 85/85 geçti. Son 07 koşusu HOME öncesi ölüm ve APK yolundaki
negatif girişleri de doğrular. Java 03 logundaki `TEST_STOP_BEFORE_CONTEXT_DB_SDK`
stack trace'i caller testinin beklenen duruşudur; Maven sonucu BUILD SUCCESS'tir.
Mock test stdout'undaki `FOUR_UI_PAINTS`, `CONSUMER_WAITING` ve rezervasyon mesajları
gerçek çalışma kanıtı değildir.

Yeni `run()` testleri gerçek yardımcıları çağırır; OS/ADB/VM/native auth sınırları mock'tur.
Reverse yan etki sonrası timeout'ta owned mapping kaldırılır. Önceden hedef reverse varsa
ne rebind ne remove ne intent oluşur. HOME öncesi ölümde HOME/dönüş; HOME sonrası ölüm,
PID/startTicks/cmdline değişiminde dönüş launch sayısı sıfırdır. Dönüş sonrası değişimde
başarı veya süreklilik makbuzu oluşmaz. Recovery başarı fixture'ı dört paint kaydı üretir;
bu sentetik sonuç gerçek ölçümü kapatmaz.

Java claim testi gerçek `run()`/catch ve CREATE_NEW claim yolunu kullanır; yalnız sabit
özel yol/hash ön kapısı test geçici dizinine uyarlanır. Aynı/başka sahipte claim ve aktif
ledger korunur, failure dosyası oluşmaz. İlk kendi claim'inden sonraki hata dosyası oluşur.

Komutlar önceki devirdeki offline Python ve hedef Java komutlarıdır. Yeni paket,
transport, mobil/üretim kaynak değişikliği, APK build veya DB IT yoktur. Gerçek başlangıç,
tek login/pencere ve dört gerçek teslimat root'a aittir. İnceleme kapanışı, P4b, üretim
profili ve fiziksel kalite kabulü bu raporla tamamlanmış sayılmaz.
