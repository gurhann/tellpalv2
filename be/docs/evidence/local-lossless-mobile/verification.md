# Yerel kayıpsız mobil QA — kısmi doğrulama

Fiziksel ölçüm: **NOT_RUN**. Derleme ve kod testleri geçti. Cihazdaki altı durumun kabulü tamamlanmadı.

| Kapı | Sonuç | Kanıt |
|---|---|---|
| Ağsız ayrı iPhone profile build | PASS | iphoneos, ayrı QA bundle ve mevcut development signing/codesign doğrulandı. |
| Flutter reading_images testleri | PASS | 97 test; QA matrisinde 17 test var. |
| Hedefli Flutter analyze | PASS | QA giriş, kompozisyon, driver ve test dosyaları temiz. |
| Python araç testleri | PASS | 20 test. |
| Fiziksel launch | FAIL / DRIVER_FAILED | Flutter drive, 600025 ms sonunda timeout ile durdu. |
| Kaydedilen tamamlanmış durum | 0 / 6 | Driver ölçüm kaydı oluşmadı. |
| Ekran kaydı | 0 | Screenshot oluşmadı. |
| RGB, paint, stop sahipliği ve RSS | NOT_RUN | Cihazda kabul edilmiş ölçüm yok. |

Kaynak/snapshot zinciri, altı mevcut asset, manifest, normal pubspec ve lock hash'leri korundu. Mevcut signing metadata'sı kullanıldı; yeni sertifika veya provisioning oluşturulmadı. Derleme SDK/Pods cache'iyle ağsız yapıldı. 232 korunan dosya ve eski signing output'taki 2248 girdi değişmedi. `.memlog.md` SHA256: `338cc79fa0194c80e533a1bfc01c0a3349959cf88a6dfd5177c9c8f3e9e1800c`.

Taze donanım ön kontrolü fiziksel iPhone 12 / iOS 26.7'yi doğruladı. Bu ön kontrol, uygulamada decode veya gösterim kabulü değildir. Nonce ve exact binary/device seçimi kodda vardır; driver çalışmadığından runtime nonce, mode ve VM endpoint doğrulaması NOT_RUN kaldı.

Son gözlenen bekleme aşaması Xcode debug otomasyonu başlangıcıydı. Flutter, Xcode kontrolü için genel Automation izin açıklaması, iOS 26 kablosuz debugging yavaşlığı ve Xcode başlangıç gecikmesi uyarısı verdi. Açık permission-denied hatası yok. Koordinatörün ayrı salt okunur gözlemlerinde Mac kilitli olduğu için Xcode UI incelenemedi; iPhone lockState `passcodeRequired:true`, `unlockedSinceBoot:true` verdi. Bunların timeout'un kesin nedeni olduğu kanıtlanmadı. Süre sınırı ve başarısız private makbuzlar korundu; yeni cihaz denemesi başlatılmadı.

Kod; asset okuma, checksum, decode ve RGB doğrulama sürelerini ayırır. RGB sayacı native readback, alpha taraması, RGB kopyası ve SHA256 doğrulamasının toplamıdır. Bu denemede cihaz süresi veya RSS örneği yoktur. Loaded RSS ileride ölçülürse readback/checksum sonrasını ve doğrulama ek yükünü içerir; saf decoder/RGBA resident farkı, peak, GPU belleği veya phys_footprint sonucu çıkarılamaz. Teorik RGBA tutarı görsel başına 4386192 byte'tır. Tek seri örneklerden format hızı genellenemez.

Renderer gözlenmedi (`unobserved`); engine revision renderer kanıtı sayılmadı. HTTP transferi ve gerçek OS background testi yapılmadı. Tam genişlikte reader uygunluğu, insan görsel kalite kabulü, fiziksel Android, P0/P4b ve üretim kapıları açıktır. Spec görevleri ve kabul ölçütleri done yapılmadı. Geniş kaynak review ve commit tamamlanmadı.
