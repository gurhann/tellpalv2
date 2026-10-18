# P4a5 ikinci kapanış incelemesi: forward timeout sahipliği

Tarih: 2026-10-18. Bu kayıt, [önceki dar düzeltme kaydının](local-verification-review-01-2026-10-18.md)
ardından gelen tek edge bulgusunun kapanışıdır. Önceki bütün kaynak/inceleme/test/build makbuzları korunur.

## Düzeltilen yol

ADB forward eşlemesini oluşturup komut cevabı timeout olunca eski `tcp:0` yolunda host portu
bilinemiyordu. Yeni yol, host portunu yerel loopback socket üzerinden komuttan önce seçer.
ADB tablosunda bu host portunun başlangıçta bulunmadığını doğrular. VM logunun geldiği QA
PID/startTicks kimliğini yeniden denetler. Proposal, emulator, PID/startTicks, host/device port
ve başlangıç yokluk bilgisi `android/arm-forward-intent.private.json` dosyasına çağrı öncesi
fsync ile yazılır.

Cleanup adayı komuttan önce atanır. Sonra yalnız
`forward --no-rebind tcp:<bilinen-host-port> tcp:<owned-VM-port>` çağrılır. Komut eşlemeyi
oluşturup cevabı kaybetse bile cleanup aynı emulator/host/device üçlüsünü doğrulayabilir.
Yalnız bu üçlü eşleşirse silme yapılır. Farklı hedefe veya cihaza ait eşleme korunur.
`--remove-all`, tahmini başka port veya yeni retry yoktur. Port aralıkta başka işlemce alınırsa
bağlama reddi başarısız hazırlık olarak kalır; var olan eşlemeyi değiştirme yetkisi doğmaz.

## Doğrulama ve devir

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.
Son Python sonucu `logs/python-review-05.log`: **59/59 geçti**.
`python-review-04.log` ara 59 başarılı testi korur. Komut:
`python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py'`.

Yeni test, mock ADB sunucusunda eşlemeyi önce gerçekten oluşturur, ardından
`subprocess.TimeoutExpired` üretir. Komuttan önce intent makbuzunu ve owned PID/startTicks
bağını denetler. Sonra yalnız yeni eşlemenin ve QA sürecinin temizlendiğini doğrular.
Başka test, seçilen host portunda önceden var olan eşlemeye karşı forward komutu/intent
üretilmediğini ve eski eşlemenin silinmediğini doğrular. Mevcut VM keşfi, makbuz hatası ve
cleanup hata testleri de geçti.

Yalnız `tools/image-optimization/p4_consumer_qa.py` ve `test_p4_consumer_qa.py` değişti.
Kaynak/hash/koruma makbuzları `receipts/implementation-audit-review-02.private.json` ve
`implementation-source-identity-review-02.private.json` içindedir. `git diff --check` geçti.
Donmuş blok/proposal, 3241 korunan repo dosyası ve 72 eski çalışma dosyası korunur.

Java, mobil, IT, APK build, cihaz, backend, auth, storage veya arm bu dilimde çalıştırılmadı.
OS/ADB sınırları testlerde mock edildi. Root'ın mevcut offline `build-0001` ve `build-0002`
çıktıları değiştirilmedi. Gerçek Android/ADB davranışı root'ın bekleme bağlama kapısında,
tek login/pencere/medya koşusu ise mevcut yetki ve girdi kapılarıyla açık kalır.
