# P4b5 elle ölçüm: tek deneme başarısız kapandı

2026-10-20. Kullanıcı, [somut koşu teklifini](pre-run-ready-2026-10-20.md) `a` yanıtıyla onayladı. Source-0007/build-0012/run-binding-0006 ile yalnız bir kurulum ve bir ilk başlangıç yapıldı. Ağsız baseline kapısı `QA_OWNER_CHANGED` ile durdu. Terminal `FAILED_NO_RETRY` olarak kapalıdır. İkinci deneme yapılmadı. P4b5 ve P4b kabulü açık kalır.

## Ne oldu?

Başlangıçta mevcut emulator-5554, Postgres PID 973, ADB sunucusu PID 85991, emülatör PID 86322 ve mevcut Gradle daemon PID 39718 tanımlandı. Backend/CMS başlangıcı yapılmadı; başka süreç durdurulmadı. Ana diskte yaklaşık 7867 MiB, MAC_EK'te 324886 MiB boş alan vardı. ADB/ps ilk sandbox çağrısında işletim sistemi izni nedeniyle çalışmadı; yetkili yerel çağrıyla bağlantı doğrulandı. Bu kontrol QA uygulamasını başlatmadı.

Kaynak, APK, Python, önceki proof ve üç inceleme bağları tekrar doğrulandı. Ayrı onay kaydına eklenen açıklama alanları exact şema tarafından reddedildi. Gerçek claim oluşmadan bu hatalı yeni kayıt byte'larıyla `execution-approval-rejected-0001.private.json` dosyasında korundu; doğru alanları taşıyan onay kaydı oluşturuldu. Düzeltme ayrı makbuza bağlandı. Bu kayıt düzeltmesi yeni kurulum, backend veya giriş hakkı tüketmedi.

Seçilen APK bir kez kuruldu. Yüklü APK SHA'sı ilk başlangıçtan önce `5b38b105378d16ed71bda301587bbd93dac5d6ed0cbfd8789fb47820a65e98a5` olarak doğrulandı. İlk başlangıçta PID 5892 / startTicks 1159170 kaydedildi. `arm` sırasında süreç sahipliği denetimi `QA_OWNER_CHANGED` üretti. Host baseline kaydı oluşmadı; backend aşamasına geçilmedi.

## Bilinen neden ve belirsizlik

Ret nedeninin süreç sahipliği denetimi olduğu kesindir. Bunun gerçek PID değişimi veya uygulama çökmesi olduğu kanıtlanmadı. Hata anındaki ADB alt çağrılarının ayrı ham çıktıları kaydedilmediği için `pidof` sonucu ile startTicks karşılaştırmasından hangisinin ayrıştığı bilinmiyor. Süre sınırını veya sahiplik kapısını gevşetmek için kanıt yoktur.

Sonraki salt okunur tanıda aynı PID 5892, aynı startTicks 1159170 ve doğru paket komutu bulundu. Mevcut PID'nin logcat kaydı Flutter kitaplığının ve VM servisinin açıldığını gösterdi; alınan 8740 byte'lık kayıtta fatal crash kanıtı yoktu. Bu, bütün hataların yokluğunu veya uygulamanın hazır olduğunu kanıtlamaz. App-private baseline ve terminal-failure dosyaları sonraki tanı okumasında da yoktu. Uygulama başlangıcındaki bekleme ayrıca açıklanmalıdır.

İlk otomatik `cleanup.private.json`, süreç çıkışına PASS yazmıştı. Aynı süreç sonrasında bulunduğu için bu çıkış kanıtı kabul edilmedi. Eski makbuz değiştirilmedi. PID, başlangıç zamanı ve komut yeniden doğrulandıktan sonra yalnız bu denemeye ait QA paketi force-stop ile kapatıldı. Eski PID'nin `/proc` kaydının yokluğu ayrıca doğrulandı. Bu fark ve son temizlik `runtime-post-cleanup-audit-0001.private.json` içinde tutulur. Yeni QA başlangıcı veya tekrar ölçüm yapılmadı.

## Tüketim ve kanıt sınırı

| İşlem | Bu denemede tüketilen |
|---|---:|
| QA kurulumu | 1 |
| İlk QA başlangıcı | 1 |
| Backend başlangıcı | 0 |
| QA girişi / lookup / exchange / verification | 0 |
| Ölçüm penceresi | 0 |
| Manifest / status / medya HTTP | 0 / 0 / 0 |
| Medya payload | 0 byte |
| Doğrulanmış çizim / ekran kanıtı | 0 |

Backend state ve observations dizinleri oluşmadı; backend-start/login/window/measurement claim'leri yoktur. Güncel QA DB borcu veya Firebase/storage sorgulanmadı. Tarihsel V37 borcu yeni sorguyla doğrulanmış sayılmaz. Bu deneme V37 borcunu sıfırlamaz veya önceki hakları geri getirmez. Önceki iki denemenin 18 dosyası ve eski kayıtlar korunur.

Flutter 188 / Java 24 / Python 66 yerel test kanıtı tarihsel hazırlık kanıtıdır. Gerçek emülatör başarısı olarak kullanılamaz. Rendition seçimi, aktarılan medya byte'ı, çizim, background/resume ve kaynak sınırları bu denemede ölçülemedi. Fiziksel kalite ve önceki açık ürün kapıları kapanmadı.

## Kanıt dosyaları

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`. Bütün ham kayıtlar MAC_EK'te kaldı; repo içine token, ham log veya ekran kopyalanmadı.

| Dosya | SHA-256 |
|---|---|
| `receipts/runtime-summary-0001.private.json` | `cc0f86a10312df3d4ef19cd48af1cb9cb77de700278fe38020790afa73fa80d1` |
| `receipts/terminal-failure-0001.private.json` | `9779c85b12ae34e0c71610009e92851e62ddf9c5b14105cd90e0640f5751c419` |
| `terminal.private.json` | `96b5d65de4214bac3e3b7209967707646ca259b642033a0ab7cbe90c405b85d0` |
| `cleanup.private.json` | `21c574e30d82b82c9e0537e84c2a412b2530b9e1569380920647cfb6d13d40d4` |
| `receipts/runtime-owner-diagnostic-0001.private.json` | `f3f15a60e849c145e17641f711bdb67712e4a26c094e9e1f4f46a05825c1e93d` |
| `receipts/runtime-post-cleanup-audit-0001.private.json` | `4e579ae7ec2ea1654263aadf06494146438ecfedcdf6833b4e079a72bf45b7f6` |

Özet JSON diğer onay, claim, APK doğrulama, logcat ve geç-baseline tanı dosyalarının hash'lerini de içerir. İlk cleanup PASS değeri tek başına başarı kanıtı değildir; son audit ile birlikte okunmalıdır.

## Sıradaki dar iş

Yeni gerçek deneme açmadan, süreç keşfi ve çıkış doğrulaması için yerel test/sözleşme düzeltmesi hazırlanmalıdır. Tek bir boş `pidof` yanıtı, eski PID'nin gerçekten çıktığını kanıtlamamalıdır. PID/startTicks/paket kontrolü korunmalı; hata anının sınırlı ve sırsız alt çağrı kanıtı tutulmalıdır. App baseline oluşmaması ayrı incelenmelidir. Kök neden kanıtlanmadan süre uzatımı, VM kapısını atlama veya otomatik tekrar önerilmez. Yeni kurulum/başlangıç ancak ayrı somut plan ve yeni kullanıcı onayıyla açılabilir.

## Son üç inceleme

`p4b5_manual_blind_final`, `p4b5_manual_edge_v2` ve `p4b5_manual_verification_v2` aynı son değişiklik paketiyle başarısız denemenin raporunu değerlendirdi. Ek uygulanabilir bulgu veya doğrulama boşluğu bildirmedi. Bu sonuç ölçüm kabulü değildir; yukarıdaki bilinen iki açık konu korunur.

| Özel kayıt | SHA-256 |
|---|---|
| `reviews/post-run-0001-01.private.json` | `b9e52324edeab14c894963e62711b81c89b57455e23de361f40d57c4e985a66a` |
| `reviews/post-run-0001-02.private.json` | `5e64edf04ab2457a8de539a9e8b62b8512f7abe8e3e0465c4b75fdaa9197ced0` |
| `reviews/post-run-0001-03.private.json` | `c9862d3d02eece822d3571bb15a9574a143eda8e41e0b47e8216248b6bfb2b6b` |

Kaynak/APK/run bağları ve terminal kayıtları sabit kalır. Görevle sınırlı yerel commit kapanış içindir; push veya yeni gerçek deneme açmaz.

Kapanışta staged diff kontrolü, generator çıktısı `coordinator.freezed.dart:211` satırında tek boşluk uyarısı buldu. Önceki `git diff --check` çıktıları henüz untracked olan bu dosyayı kapsamıyordu. Bu tek biçim uyarısı ayrı kaydedildi; generator çıktısı ve gerçek APK'ya bağlı kaynak hash'i değiştirilmedi. Diğer görev dosyalarının staged diff kontrolü temizdir.
