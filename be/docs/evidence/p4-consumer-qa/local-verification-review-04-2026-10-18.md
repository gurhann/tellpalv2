# P4a5 başlangıç cleanup kimliği ve adapter süre doğrulaması

Tarih: 2026-10-18. Bu kayıt, [ilk başlangıç düzeltmesi kaydını](local-verification-review-03-2026-10-18.md)
korur ve sonraki üç hedefli incelemenin iki bulgusunun dar düzeltmesini belgeler.
Önceki kayıt tarihsel ara devirdir; oradaki 64 başarılı test bu iki açığı kapsamıyordu.

`cancel_owned_launch`, ilk `force-stop` öncesinde mevcut PID'nin cmdline/startTicks kimliğini
okur. Önceden owned kimliği varsa birebir eşleşme gerekir. Değişmiş PID veya startTicks
durumunda durdurma komutu gönderilmeden cleanup başarısız olur. Owned kimliği henüz
bilinmiyorsa queued launch kanıtı ile o anda doğrulanan kimlik kullanılır. PID yoksa yalnız
kurulum/başlangıç niyetinin ayrı QA paketine ait kanıtıyla queued iptal yapılabilir.

Yeni testler gerçek cleanup helper'ını çağırır. Değişmiş PID ve yeniden kullanılan PID'nin
farklı startTicks durumlarında durdurma komutu sayısı sıfırdır. Süre testlerinde gerçek
`qa.Android` kullanılır; yalnız `subprocess.run` ve saat sınırı mock'tur. Son 0,1 saniyede
PID, cmdline ve stat çağrılarının kalan süreyi alt sürece aktardığı doğrulanır. Bloke PID
çağrısı kalan timeout kadar saati ilerletip 30 saniyede durur. Cleanup kimlik okumaları ve
bloke stop aynı iki saniyeyi paylaşır; başlangıç 30 saniye, bağımsız cleanup iki saniyedir.
Bu testler adapter içindeki timeout'un yeniden 15 saniyeye sabitlenmesini yakalar.

`python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py'`:
**68/68 geçti**. Son exclusive log `logs/python-review-08.log`; önceki loglar değişmedi.
`git diff --check` geçti. Yalnız iki Python dosyası değişti. Java, mobil, IT ve APK build
tekrarlanmadı. Özel kaynak kopyası ve hashler
`receipts/implementation-source-identity-review-04.private.json` ile
`receipts/implementation-audit-review-04.private.json` içinde korunur.

Önceki gerçek sonuç değişmedi: tek yerel waiting başlangıcı başarısızdı; root geç PID 13943'ü
doğrulayıp kapattı. Public yapılandırma GET'i bir; backend/login/Storage/medya penceresi sıfır.
Bu düzeltmede gerçek cihaz/ADB, backend, auth, Storage, APK build/kurulum veya start yapılmadı.
Yeni A onayı, reservation ve `-02` başlangıç dosyaları oluşturulmadı. Orijinal failure,
eski build kanıtları ve donmuş onay korundu. Sonraki tek yerel waiting başlangıcının açık
insan kararı ve dar kapısı önceki raporda tanımlıdır; bu rapor yeni izin vermez.

Gerçek cihaz üzerinde yeni korumalar henüz sınanmadı. Sahiplik veya iki saniyelik cleanup
süresi doğrulanamazsa sonuç başarısız kalır; başarı varsayılmaz. Gerçek medya koşusu,
P4b, fiziksel cihaz kalite ve peak kaynak kabulü açık kalır.
