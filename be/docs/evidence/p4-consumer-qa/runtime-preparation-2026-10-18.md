# P4a5 yapılandırma ve başarısız yerel başlangıç

Tarih: 2026-10-18. Bu kayıt, [ön çalışma durumundan](pre-run-status-2026-10-18.md)
sonraki gelişmeleri tutar. **Giriş, backend, 180 saniyelik pencere ve medya teslimatı
başlamadı.** İlk bekleme APK hazırlığı başarısız oldu; P4a5 tamamlanmış değildir.

## Mevcut yapılandırma bulundu

Kullanıcının gösterdiği `tellpal-v2-firebase-storage-pk.json`, eski #94 çalışmasının
`inputs.json` kaydındaki aynı servis hesabı dosyasıdır. Dosyanın içeriği değiştirilmedi
ve kopyalanmadı. Mevcut login yardımcısının zorunlu kıldığı izin için dosya `0644`
yerine `0600` yapıldı; önceki izin ve içerik hash'i özel makbuza kaydedildi.

Önceki gerçek komut sohbet geçmişinden doğrulandı: ek API yapılandırması aynı projenin
Firebase Hosting `GET /__/firebase/init.json` adresinden alınmıştı. Önceki geçici
`web-api.private.json` ve token dosyaları bugün yoktur. Senden yeniden uygulama config'i
indirmeni istemek gerekli değildi.

Aynı adrese tek salt okunur GET yapıldı. Yanıt 16 KiB ile sınırlandı; proje ve bucket
eşleşti. Mevcut API ayarı, özel QA kökünde `runtime-auth/web-api.private.json` dosyasına
`0600` olarak yazıldı. Yeni key, IAM/API değişikliği veya Firebase Management çağrısı
yapılmadı. Değerler sohbet, komut argümanı veya repo içine yazılmadı. Yerel login girdi
doğrulaması geçti; gerçek hesap sorgusu/token exchange/verifier çağrısı yapılmadı.

Bu tek Hosting config GET, manifest/status/medya teslimatlarından ayrı bir hazırlık
okumasıdır; yeni giriş veya Storage okuması değildir. Özel istek ve sonuç makbuzları
`receipts/existing-hosting-config-*.private.json` altındadır.

## Gerçek yerel hazırlık sonucu

Mevcut `build-0002` APK'sı `emulator-5554` üzerine kuruldu ve bekleme ekranı başlatıldı.
`am start` döndükten hemen sonra PID sorgusu boş geldi. Araç `QA_PID_UNAVAILABLE` ile
reddetti. İlk cleanup kaydı `NO_STARTED_PROCESS` bildirdi. Sonraki salt okunur sorguda
gecikmiş PID `13943` görüldü; ilk cleanup kaydı gerçek kapanış kanıtı değildir.

Root, yalnız ayrı QA paketinin cmdline/PID/startTicks kimliğini doğruladı, owned süreci
kapattı ve artık PID olmadığını doğruladı. Eski `tcp:53421 → tcp:39447` eşlemesi korundu.
Yeni forward/reverse, backend veya ölçüm penceresi açılmadı. İlk hata makbuzu değiştirilmedi:
`android/arm-failure.private.json`, SHA-256
`d5c7ceaa339b958535ed869c745f9f4c58001e31f88ccfabf08ae60af9ce8454`.
Geç süreç gözlemi ve gerçek cleanup, ayrı `receipts/late-arm-process-*.private.json`
kayıtlarındadır. Uygulama yeniden başlatılmadı.

## Düzeltilen koruma ve doğrulama

Tek `am start` sonrasında PID ve VM aynı 30 saniyelik hazırlık süresi içinde gözlenir.
İlk boş PID sonucu hemen başarısızlık sayılmaz. Cleanup, boş anlık PID sonucunu yeterli
saymaz; bu ayrı paketin kurulum/start niyetine dayanarak queued başlangıcı iptal eder
ve sonlu yokluk gözlemi yapar. Bilinen PID/startTicks değişmişse ilk stop komutundan
önce reddeder. Kalan süre gerçek ADB çağrılarına aktarılır; cleanup sınırı iki saniyedir.

Son Python kapısı **68/68 geçti**: `logs/python-review-08.log`. Önceki loglar korunur.
Yeni testler geç PID, queued başlangıç, değişen PID/startTicks için sıfır stop, gerçek
Android adapter'ında timeout aktarımı ve tek manuel başlangıç sınırını çalıştırır.
OS/ADB sınırları bu yeni testlerde mock'tur. Yeni koruma gerçek emülatörde henüz sınanmadı.
Java, mobil ve APK kaynakları değişmedi; build veya ilgili testler yeniden çalıştırılmadı.

Hedefli üç inceleme tamamlandı. İlk stop öncesi kimlik kontrolü ve adapter süre aktarımı
bulguları kapatıldı; son edge sonucu `[]`, doğrulama sonucu temizdir. Ayrıntılar
[ilk başlangıç düzeltmesi](local-verification-review-03-2026-10-18.md) ve
[son kimlik/süre kapısı](local-verification-review-04-2026-10-18.md) kayıtlarındadır.

## Karar bekleyen tek ek yerel başlangıç

[Onaylı planın](../../../../_bmad-output/implementation-artifacts/spec-p4a5-existing-consumer-smoke.md)
“Başarısızlıkta yeniden başlatma yok” sınırı nedeniyle otomatik tekrar yapılmaz.
Somut öneri, aynı emülatör ve aynı APK ile **yalnız bir ek yerel bekleme başlangıcıdır**.
Yeni üretim, yeni APK veya ek giriş/pencere/medya hakkı içermez. Yerel kapılar geçerse
önceden onaylı tek gerçek ölçüm aynı sayısal sınırlarla devam edebilir.

Kod yalnız bu olaya özel bir manuel kapı içerir. Kullanıcı açıkça onaylarsa
`android/local-waiting-approval-01.private.json`, eski hata SHA'sına ve `build-0002`
APK SHA'sına bağlanır. `decision=A`, `localWaitingOnly=true`, `maxStarts=1` gerekir.
Çağrıdan önce exclusive reservation yazılır; tekrar reddedilir. Yeni hata/niyet kayıtları
`-02` adlarını kullanır; ilk hata dosyası silinmez veya taşınmaz. **Gerçek onay ve
reservation dosyaları henüz oluşturulmadı.** Bu, otomatik veya genel retry mekanizması değildir.

Mevcut ölçüm sınırları değişmez: en çok bir ek QA girişi, tek 180 saniye, dört
rendition/13.170.884 byte, manifest 1/status en çok 18/HTTP en çok 23, login dahil ek
verifier en çok 28. Bunların hiçbiri bu hazırlıkta tüketilmedi. Eski toplam tavanlar
korunur; yeni başarısızlık otomatik tekrar hakkı doğurmaz.

Yeni ham çıktılar MAC_EK'teki mevcut özel QA kökündedir. İlk iki eski test ham logunun
kaybı, [ilk rapordaki](local-verification-2026-10-18.md) sınırlamasıyla korunur. Normal
mobil tüketici geçişi, P4b, üretim profili ve fiziksel kalite kabulü açık kalır.
