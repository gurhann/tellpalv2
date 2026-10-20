# DevTools consumer tek deneme sonucu

2026-10-20. **Onaylı tek deneme, ölçüm öncesi başlangıç kontrolünde başarısız oldu.** Dört görselin teslimatı veya P4b yaşam döngüsü kabulü tamamlanmadı. Yeni üretim, otomatik tekrar ve limit değişikliği yapılmadı.

## Yetki ve hazırlık

Kullanıcı [somut teklifi](single-run-proposal-2026-10-20.json) “A — Bu APK/run ile tek sınırlı ölçümü onayla” yanıtıyla onayladı. Teklif SHA-256 `fff01ae4780be54f1d40e9c9c03676f9f72ed84563277cc796137059da987de9`; seçili APK `build-0002`, SHA `c331c42ac10ba445a7835f8db26829539e262ddad3b879be9692e5d34847cbe7`; run-binding SHA `cceb45940273fc0b760cc99a8dc9a0c41c733263949e1bc8f27f7d18ad9da7a1`.

Gerçek kopyanın varsayılan köküyle kaynak/Python/classpath/build/run ve üç ön inceleme bağı doğrulandı. Mevcut `emulator-5554` ARM64, paket yokluğu ve geometri kontrolü geçti. Gerekli eski input/config hash’leri koruma kayıtlarıyla eşleşti. Aynı QA DB’ye mevcut kullanıcıyla salt okunur bağlantı doğrulandı; bu kontrol güncel retained kimlik ve bütçe kabulünün tamamlandığı anlamına gelmez. Yeni özel execution-approval, protected-before ve backend inputs dosyaları exclusive creation ve `0600` ile hazırlandı. Kaynak/APK/binding byte’ları değişmedi.

Kullanıcı düğmeleri ilerletmek için hazır olduğunu bildirdi. Eski DevTools Memory sayfası hâlâ önceki ağsız uygulamaya bağlıydı; bu bağlantı yeni consumer kanıtı sayılmadı. Yeni uygulamaya bağlantı, backend ve girişten önce yapılacaktı.

## Gerçek sonuç ve tüketim

| Aşama | Bu denemedeki sonuç |
|---|---|
| QA kurulumu | 1; APK hash’i kurulumdan sonra doğrulandı |
| İlk başlangıç | 1; PID 7194, başlangıç tick’i 2791172 |
| `arm` | 16,886 saniye; exit 2, `QA_PROCESS_COMMAND_CHANGED` |
| Uygulama baseline kabulü | Yok |
| Yeni consumer DevTools bağlantısı | Yok |
| QA backend başlangıcı / giriş | 0 / 0 |
| Ölçüm penceresi / host measurement | 0 / 0 |
| QA manifest / status / medya | 0 / 0 / 0; medya payload 0 byte |
| Dört paint / ekran / ACK | 0 / 0 / 0 |
| Otomatik tekrar / yeni üretim | 0 / 0 |

Kurulum ve ilk başlangıç hakları tüketildi; iade edilmedi. `backend-start`, `login`, `window` ve `measurement` claim’leri oluşmadı. Eski borç ve toplam tavanlar değiştirilmedi. QA backend çalışmadığı için bu denemeye ait sunucu teslimat sayacı veya retained kabulü üretilmedi; yukarıdaki sıfırlar bu QA yürütücüsünün açılmayan aşamalarını gösterir, makinenin tüm ağ trafiğini göstermez.

## Hata ve kapanış

Host, ilk olumlu PID/başlangıç kaydından sonraki sahiplik kontrolünde `QA_PROCESS_COMMAND_CHANGED` gördü. İlk cleanup da aynı kontrol nedeniyle başarısız oldu; `cleanup.ok=false` ve `FAILED_NO_RETRY` değiştirilmeden korundu. İlk ret anındaki ham cmdline byte’ları saklanmadığı için kalıcı süreç değişimi veya kesin kök neden iddia edilmez.

Daha sonraki salt okunur kontrolde aynı PID, aynı paket cmdline’ı ve başlangıç tick’i tekrar eşleşti. Bağlı ADB/claim ve olumlu süreç kimliği tekrar doğrulandıktan sonra yalnız bu QA süreci mevcut adapter ile kapatıldı. `/proc` yokluğu doğrulandı. Ek cleanup PASS, ilk başarısız cleanup kaydını başarıya çevirmedi. QA paketi kurulu kaldı; normal TellPal ve önceki ağsız QA sürecine müdahale edilmedi.

PID logcat’te ayrı bir uygulama bulgusu vardır: `support complete → injection → mount complete → platform → process → directories failed → failureMarker unavailable`. Bu bulgu host cmdline reddinin nedeni olarak gösterilemez. Crash buffer ve Android exit-info kesin bir neden vermedi. Uygulama tanısı hata ayrıntısını taşımadığı için yalnız aşama bilgisiyle kesin neden atanmadı.

Salt okunur ek tanıda `/data/data/.../files` ve `/data/user/0/.../files` yolları aynı inode/cihaz, uid/gid ve `0771` dizin iznine ulaştı. `readlink -f` her iki yolu yazıldığı biçimde döndürdü; bu sonuç canonical alias uyuşmazlığını göstermedi. Dart’ın gerçek support string’i ve resolve sonucu kaydedilmediği için bu alt koşul kesin biçimde elenmedi. Tanı sırasında uygulama başlamadı; izin veya cihaz dosyası değişmedi. İlk yerel tanı sarmalayıcısındaki `stderr=None` birleştirme hatası yalnız sarmalayıcıda düzeltildi; kapalı gerçek deneme tekrarlanmadı.

## Kanıtların konumu

Ham kayıtlar `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1` kökünde kalır. Eski kayıtlar ve başarısızlıklar korunur.

| Göreli özel kanıt | SHA-256 |
|---|---|
| `receipts/execution-approval-01.private.json` | `5faa5d2f11ca67bdfac3a6f517302f048b1c13b02e7e52efa7114df5d54575f2` |
| `receipts/terminal-failure-0001.private.json` | `7439aae77c38914258bd997fa9c59f89b03d13cc9bc81f89cba6e270be917af7` |
| `terminal.private.json` | `2190c8ed15b6b268c1eb1b350c7d8a701301e31208e3ed7487aca02d24a4f961` |
| `cleanup.private.json` | `5a11f3c2af03dc3015b9925baad3a7a7758ed9db8edb93ec04fa583a34957b6a` |
| `receipts/owned-post-terminal-cleanup-0001.private.json` | `7037f19138eb6509d721818be28adc0001b5cfd890c5b3a649828d53f1074379` |
| `receipts/support-alias-diagnostic-0001.private.json` | `2aa6b60d774b89ef10c1fd5cfbc3920e8071fe34c4d6780efde6b2be011773b4` |
| `logs/actual-arm-0001.private.log` | `6ad2ad5de0313393f87a0733bd5ceb55b873e76a8de881f98d2225cd8f571dae` |
| `logs/actual-pid-logcat-0001.private.log` | `0dcfbb9e4eba2ed96f8f2be8d5c278b3d3e06ea39868dca50d7ca68171c17e58` |

VM adresi, token, credential ve ham loglar repoya alınmadı. Bu deneme için DevTools köprüsü oluşturulmadı. Önceden açılmış DevTools gözlemcisi ve bağlantısı korundu.

## Açık kabul ve sonraki iş

Yeni Network/Memory örneği, rendition/geometri/decode/payload eşleştirmesi, dört çizim, Home/dönüş ve kaynak drain kanıtı yoktur. Önceki 197 Flutter / 80 Python / 26 Java testi ve analyzer sonucu aynı kaynakların yerel hazırlık kanıtıdır; bu denemede tekrar çalıştırılmadı ve gerçek ölçüm yerine sayılmaz. Fiziksel Android/iPhone, JPEG/WebP kalite kabulü ve genel üretim profili açık kalır. P0 ham kanıt sınırlaması korunur; P4b tamamlanmadı.

Sonraki teknik iş, uygulamanın `directories` başlangıç reddini ve host süreç kimliği okumasındaki geçici uyuşmazlığı ayrı ayrı teşhis etmektir. Güvenlik kontrollerini kaldırmak veya bu kapalı denemeyi yeniden açmak çözüm değildir. Yeni APK ya da yeni gerçek deneme bu sonuçla başlatılmaz; somut düzeltme ve gerekiyorsa yeni sınırlı deneme ayrı kapsamda değerlendirilir.
