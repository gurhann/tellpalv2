# İlk MAC_EK denemesinin bağlamı

Bu denemede prepare ve çevrimdışı pub get geçti. Gradle, 171 task sonunda
`BUILD SUCCESSFUL` bildirdi; build süresi 1 dakika 33 saniyeydi. APK oluştu.
Paket doğrulayıcısı kabul vermediği için driver başlatılmadı. Kabul edilen
case ve screenshot sayısı sıfırdır. Resmi [doğrulama sonucu](verification.md)
`NOT_RUN / BUILD_FAILED` durumundadır; burada build sonrası doğrulama hatası
ayrıca açıklanır.

İlk ret `apk_archive_rejected` kodudur. Cache'teki profile motorunun sembollü
`libflutter.so` girdisi 155.038.656 byte idi. Yardımcı aracın 128 MiB genel
okuma sınırı, APK incelemesi başlamadan bu girdiyi reddetti.

Salt okunur ek inceleme, APK'nın `arm64-v8a`, `armeabi-v7a` ve `x86_64`
içerdiğini gösterdi. Hedef yalnız ARM64'tü. Yerel Flutter Gradle plugin'i
ABI filtrelerini buildType üzerinde tanımlıyor; özel QA init script'inin
yalnız defaultConfig filtresi bu ayarı sınırlamamıştı. ARM64 AOT ELF,
fixture manifesti ve paket içindeki altı görselin byte/SHA kontrolü geçti.
Paketlenen motor ise sembol temizliği sonrasında upstream jar ile aynı
byte/SHA değerini taşımıyordu. `merged_native_libs` aşamasındaki motor
155.038.656 byte ve upstream jar ile aynı SHA256 değerindedir.
`stripped_native_libs` aşamasındaki motor 12.148.424 byte ve APK'daki motor
ile aynı SHA256 değerindedir. Bu iki ara çıktı, farkın build'in sembol
temizliği aşamasında oluştuğunu doğrular. Bu bulgular denemeyi PASS yapmaz.

2712 mevcut dosya/girdi için koruma kontrolü geçti. Temizlenmiş eski
`/private/tmp` ham kayıtları bu kapsama girmez; [saklama sınırı](../../retention-note.md)
sürer. Bu denemenin yeni ham kayıtları MAC_EK'tedir. Kaynak SHA256:
`cdbc3543e1896bdba66735c3b8b2d368d820c23539d939a98dc940d0913d2a66`.

Sonraki deneme ayrı kökte yapılır. Yalnız QA paketlemesi düzeltilir;
görsel encode/resize, yeni bulut işi, dependency indirme veya ürün akışı
değişikliği yapılmaz. Fiziksel cihaz, insan kalite kabulü ve üretim kapıları
açık kalır.
