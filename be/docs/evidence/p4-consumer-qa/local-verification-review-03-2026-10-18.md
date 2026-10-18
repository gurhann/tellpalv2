# P4a5 gerçek hazırlık hatası ve geç PID koruması

Tarih: 2026-10-18. Bu kayıt, [önceki kapanış düzeltmesinden](local-verification-review-02-2026-10-18.md)
sonraki gerçek yerel bekleme hazırlığını ve ona bağlı dar Python düzeltmesini tutar.
Özel kök `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.

## Gerçek sonuç: hazırlık başarısız, medya çalışması açılmadı

Root, eski sohbet komutundaki aynı projenin mevcut public Hosting
`GET /__/firebase/init.json` yapılandırmasını tek salt okunur GET ile aldı.
`runtime-auth/web-api.private.json` özel dosyası hazırlandı; yeni key oluşturulmadı.
Kullanıcının seçtiği mevcut Admin dosyası 0644 yerine 0600 yapıldı; içerik veya kopyası değişmedi.
Bu yapılandırma GET'i, auth veya Storage okuması değildir.

Onaylı ilk bekleme hazırlığında mevcut `build-0002` APK kuruldu. Tek `am start` döndü.
Hemen sonraki boş `pidof`, `QA_PID_UNAVAILABLE` hatası üretti. Eski cleanup'ın
`NO_STARTED_PROCESS` sonucu, geç başlayan süreci dışlamıyordu. Root'ın sonraki salt okunur
sorgusunda PID 13943 görüldü. Root paket/cmdline/startTicks kimliğini doğruladı, yalnız bu
owned QA sürecini kapattı ve çıkışını doğruladı. Eski ADB forward korundu.

| Bu gerçek hazırlıktaki yeni işlem | Sonuç |
|---|---|
| Public Hosting yapılandırma GET'i | 1 salt okunur GET |
| APK bekleme başlatması | 1; hazırlık başarısız |
| Backend başlatma | 0 |
| Yeni login/verifier/Storage çağrısı | 0 |
| Yeni 180 saniyelik pencere/medya teslimatı | 0 |

Gerçek kanıtlar `android/arm-failure.private.json`,
`receipts/late-arm-process-before-cleanup.private.json`,
`receipts/late-arm-process-cleanup.private.json` içindedir. Eski başarısız makbuzun SHA'sı
`d5c7ceaa339b958535ed869c745f9f4c58001e31f88ccfabf08ae60af9ce8454` olarak korunur.
Bu hazırlık dört UI paint'i, güncel retained/storage doğrulaması veya P4a5 kabulü değildir.

## Dar kod düzeltmesi

Tek `am start`, PID gözlemi ve VM keşfi aynı 30 saniyelik deadline'ı paylaşır.
İlk boş PID geçici kabul edilir; yeni start çağrısı yapılmadan sınırlı gözlem sürer.
ADB timeout'ları ve poll uykuları kalan süreyle sınırlıdır.

Kurulum, immutable APK SHA'sı, başlangıçta PID olmaması ve tam start component'i çağrı öncesi
exclusive launch makbuzuna yazılır. Hata cleanup'ı boş anlık PID üzerinden başarı üretmez.
Bu kanıta dayanarak yalnız ayrı QA paketinin queued başlangıcı iptal edilir. Sonlu gözlemde
beliren PID/startTicks doğrulanabilir; tek ek cleanup stop'u sonrası yokluk gözlenir.
Cleanup iki saniyeyle sınırlıdır. Süre veya sahiplik doğrulanamazsa cleanup açıkça başarısızdır;
`NO_STARTED_PROCESS` başarısı üretilmez. Başka paket/PID/forward kapatılmaz.

## Test ve hash devri

Son Python kapısı `logs/python-review-07.log`: **64/64 geçti**.
`python-review-06.log` önceki 64 başarılı testi korur. Komut:
`python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py'`.
Testlerde cihaz/OS/ADB sınırları mock'tur; çıktıdaki waiting/build başarı metinleri gerçek
başlatma veya APK derlemesi sayılmaz.

İki yarış testi ilk boş PID sonrasında geç PID'yi aynı tek start'ta yakalar; 30 saniye sonunda
başarısızlık, gözlem/cleanup arasında başlayan owned süreç, queued iptal ve cleanup deadline'ında
başarısızlık davranışını sınar. Üç küçük kapı testi eski failure ile onaysız ret, doğru tek
waiting onayıyla bir rezervasyon ve ikinci çağrıda ret, yanlış SHA/scope/maxStarts reddini doğrular.

Yalnız iki Python kaynağı değişti:

| Kaynak | SHA-256 |
|---|---|
| `p4_consumer_qa.py` | `31f951d934569c3521d690f9aaa3327cbd7111ffc20c5c0dcbdfd36a5e6b94d2` |
| `test_p4_consumer_qa.py` | `49bd70a98fe54dc81d6eb8bed42afee975ca7036c16e417d69c57d74f9a0a159` |

Yeni kaynak/koruma makbuzları `receipts/implementation-audit-review-03.private.json` ve
`implementation-source-identity-review-03.private.json` içindedir. `git diff --check` geçti.
Donmuş blok/proposal, 3241 korunan repo dosyası, iki salt okunur plan ve 72 eski run dosyası
korunur. Java, mobil, IT veya APK build yeniden çalıştırılmadı. Bu düzeltme diliminde gerçek
cihaz, backend, auth, Storage, APK kurulumu veya arm çağrısı yapılmadı.

## Ayrı karar bekleyen tek yerel waiting hazırlığı

Eski failure dosyası varken varsayılan `--arm`, cihaz I/O'sundan önce reddedilir.
Bu kayıt yeni izin değildir. Root yalnız aşağıdaki dar karar için açık insan onayı ister:
aynı emülatörde, aynı immutable `build-0002` APK ile bir ek yerel waiting başlangıcı.
Backend/login/pencere/Storage hakkı eklenmez. Yeni root veya genel retry akışı yoktur.

Açık onay gelirse root yalnız şu özel dosyayı 0600 ve CREATE_NEW ile oluşturur:
`android/local-waiting-approval-01.private.json`.

```json
{
  "proposalSha256": "079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6",
  "originalArmFailureSha256": "d5c7ceaa339b958535ed869c745f9f4c58001e31f88ccfabf08ae60af9ce8454",
  "decision": "A",
  "localWaitingOnly": true,
  "maxStarts": 1,
  "buildName": "build-0002",
  "apkSha256": "102521e8c77a30784e3f2af185f5e810d539edfb69f76eea8aa4a541a9685166"
}
```

Root sonra mevcut `--arm` komutunu bir kez yürütür. Araç cihazdan önce
`android/local-waiting-reservation-01.private.json` payını exclusive/fsync ile ayırır.
Harcanmayan pay geri verilmez. Yeni launch/forward makbuzları `-02` adını kullanır;
yeni hata yalnız `android/arm-failure-02.private.json` olur. Orijinal failure aynen kalır.
İkinci reservation/start, yanlış SHA/APK veya farklı kapsam ilk cihaz çağrısından önce reddedilir.
Bu onay veya reservation dosyası henüz oluşturulmadı; yeni gerçek hazırlık yapılmadı.

Gerçek Android/ADB üzerinde yeni yarış koruması henüz sınanmadı. İki saniyelik cleanup payı
etkin sistemde yetmezse sonuç başarısız/açık kalır; başarı varsayılmaz. Gerçek medya koşusu,
P4b, fiziksel cihaz kalite ve peak kaynak kabulü açık kalır.
