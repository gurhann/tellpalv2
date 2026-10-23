# Önceki ham kayıtların saklama sınırı

18 Ekim 2026'da kullanıcı, ana diskte alan açarken önceki
`/private/tmp/tellpal-local-lossless-*` geçici klasörlerini temizlediğini doğruladı.
Bu klasörlerdeki build, launch, driver ve araç logları artık eski konumlarında
bulunmuyor. iPhone signing preflight dosyası da bulunamadı. Önceki fiziksel
denemeler bu nedenle ham girdilerinden yeniden doğrulanamıyor.

Depodaki önceki 12 sonuç/bağlam dosyası, MAC_EK'e kopyalanan yedekle SHA256
üzerinden karşılaştırıldı ve aynı bulundu. Bu belgeler değiştirilmedi. Mevcut
altı PNG/WebP fixture dosyasının boyut ve SHA256 değerleri de doğrulandı.
Bu kontrol, temizlenen ham kayıtları geri getirmez.

İkinci emülatör denemesi için önceki yürütmede prepare ve offline pub get
başarısı, ardından `compileFlutterBuildProfile` hatası gözlendi. APK veya driver
sonucu oluşmadı; kabul edilen case ve screenshot sayısı sıfırdı. Ana disk
doluluğu (`Errno 28`) hata makbuzu ve public rapor yazımını da engelledi.
Derleme hatasının kesin kök nedeni kanıtlanmadı. Bu metin o gözlemlerin
kurtarma özetidir; doğrulanmış ham deneme kaydı değildir.

Yeni görev kaynakları ve mevcut raporların 30 dosyalık kopyası MAC_EK'teki
0700 özel QA kökünde doğrulandı. Kurtarma makbuzunun SHA256 değeri:
`658d52cb0285b0b926e5d42eaf9ec19c73f0fb76504210be2d8367ac35ebfe9e`.
Bu makbuz, başarısız eski ham deneme kopyasının tamamlanamadığını da belirtir.

Yeni deneme ayrı bir çıktı kökü ve yeni kaynak/cihaz/nonce bağı kullanır.
Yeni build, ekran ve ham ölçüm dosyaları MAC_EK'te saklanır. Yeni denemenin
koruma sonucu yalnız başlangıcında mevcut olan dosyaları kapsar. Fiziksel
Android/iPhone, insan kalite kabulü, P0, P4b ve üretim kapıları açık kalır.
