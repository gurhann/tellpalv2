# P4b5 elle tüketici ölçümü — inceleme düzeltmesi 0003

Tarih: 2026-10-20. Source-0006 incelemelerindeki tek high bulgu düzeltildi. [V2](verification-v2-2026-10-20.md), [ilk düzeltme](verification-review-patch-2026-10-20.md) ve [ikinci düzeltme](verification-review-followup-2026-10-20.md) raporları korunur. Son kod yeni üç bağımsız incelemeye teslim edilir. Gerçek koşu kapısı kapalıdır.

## Dar davranış düzeltmesi

İlk `record-backend` hata verdiğinde backend `local-ready.json` üretmiş, host ise `launch.private.json` dosyasını henüz yazmamış olabilir. Cleanup şimdi launch eksik ve backend-start reservation mevcutsa önce reservation/run/classpath/Java bağlarını ve exact PID/argv/başlangıç kimliğini doğrular. Sahipliği kanıtlanan launch CREATE_NEW ile yazılır. Ardından ready varsa mevcut `close_ready_backend` ve drain doğrulaması; ready yoksa mevcut `cleanup_before_ready` çalışır.

Mevcut sağlam, bozuk veya yabancı launch overwrite edilmez. Kimlik ispatlanamazsa backend sonucu FAILED kalır; yabancı sürece sinyal verilmez. Retry açılmaz. Önceki credential/QA süreç/reverse bağımsız cleanup davranışları korunur.

## Testler ve regresyon kanıtı

| Kapı | Sonuç |
|---|---|
| İlgili Python paketi | 22/22 geçti; skip yok |
| Tam P4b5 Python paketi | 66/66 geçti; skip yok |
| `git diff --check` | Temiz |
| Flutter/Java kanıtı | Önceki 188 Flutter ve 24 Java testi; byte eşitliğiyle korundu, tekrarlanmadı |

Yeni gerçek üretici/CLI testi `arm` ve `reserve_backend` metodlarını çalıştırır. Ready kaydı ilk record çağrısından önce yazılır. Gerçek `main`, `ready`, `binding_links`, `backend_identity`, `close_ready_backend` ve terminal ledger/drain validator birlikte çalışır; yalnız Android ve `ps` sınırları sentetiktir. Gerçek sinyal yasaklanır.

Yanlış ADB / erken failure marker × dokuz durum toplam 18 varyantla sınandı: hazırlanmış sahipli süreç, ölü süreç, değişmiş başlangıç kimliği, yabancı komut, farklı startup PID’si, mevcut sağlam launch, bozuk JSON launch, yabancı PID launch ve bozuk drain. Eksik launch sahiplik kanıtından sonra oluşturulur. Mevcut launch byte’ları korunur. Başarılı owned ready kapanışı `drainVerified=true` ve `measurementSucceeded=true` ile gerçek validator’dan geçer. Erken failure marker veya bozuk drain, süreç kapansa bile backend FAILED kalır. Kimliği ispatlanamayan dallarda close request/sinyal üretilmez; credential ve sahipli QA süreci bağımsız temizlenir. Birincil CLI ret kodu ve terminal FAILED_NO_RETRY doğrulanır.

İki bellek içi mutasyon ayrıca sınandı. Ready dalını recovery önüne geri almak 10 alt senaryoda; ready drain’i atlayıp before-ready cleanup kullanmak 6 alt senaryoda test tarafından reddedildi. Kaynak dosyaları mutasyon için değiştirilmedi. Önceki producer→consumer, 24 ilk-kayıt varyantı ve gerçek run timestamp/ACK kontrolleri tam pakette korunur.

Özel kökteki yeni loglar:

- `receipts/python-manual-review-ready-race-0001.log`
- `receipts/python-tests-review-ready-race-0001.log`
- `receipts/python-review-ready-race-mutations-0001.log`
- `receipts/pub-offline-review-ready-race-0001.log`

## Bu turun kesin dosya listesi

- `tools/image-optimization/p4b5_manual_consumer_qa.py`
- `tools/image-optimization/test_p4b5_manual_consumer_qa.py`
- `be/docs/evidence/p4b5-manual-consumer-measurement/verification-review-0003-2026-10-20.md`

Önceki 32 kod/test/config dosyalık görev listesi v2 raporunda korunur. Source-0006’ya göre yalnız iki Python kaynak/test dosyası değişti.

## Son dondurulmuş kaynak ve APK bağı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.
Kaynak: `source-0007`; APK: `android/builds/build-0012/consumer.apk`.
Paket: `com.tellpal.tellpal_mobile_poc.readingmanualqa`.
APK SHA-256: `5b38b105378d16ed71bda301587bbd93dac5d6ed0cbfd8789fb47820a65e98a5`.

Bütün Python düzeltmeleri ve 22+66 test bitirildikten sonra iki dosyanın SHA’sı donduruldu. Yalnız gerekli 1162 kaynak ve 1025 değişmeyen class dosyası taşındı. Kopyalama, offline pub, kaynak bağlama/build, closure/run bağlama ve kök doğrulaması sırayla yürütüldü; her bağımlı işlem öncekinin exit_code=0 sonucunu bekledi. Build sırasında veya sonrasında kod/test değişmedi. Source-0006/build-0011 dahil tüm eski kanıtlar korundu.

Son bağ 1162 kaynak, 1303 classpath ve 1388 Python girdisini kapsar. Repository ve kopyadaki bütün bağlı dosyaların SHA’sı son kez eşit bulundu. `app/mobile/build.yaml` hem `files` hem `repositoryFiles` içinde aynı hash ile yer alır. Package_config self root source-0007’dir. Kullanılan Android/package_config girdileri ve derlemenin 205 JSON/depfile/properties/CMake girdisi tarandı; source-0002/3/4/5/6 referansı yoktur.

`source-root-validation-0005`, başarılı 188 Flutter ve 24 Java testinin çalıştığı source-0002 ile 1076 mobil/Java kaynak girdisinin ve 1025 class dosyasının byte eşitliğini kaydeder. Python closure son iki dosyayı kapsar. Kaynak/APK/Python/classpath ve iki eski denemenin proof zinciri self-check’ten geçti.

| Kayıt | SHA-256 |
|---|---|
| `source-binding-0012.private.json` | `dfa1384c70e29d422c721f813ea19ae45e2f6e41a095d817b74b7800b124a975` |
| `selected-build-0006.private.json` | `217dcb8bb9111135acfa6808150323a07a527ccf8ef4d011b5bbb5ef62c0b8a7` |
| `classpath-binding-0007.private.json` | `51aae9f72bd909526fac41e92bc9699128458810069d0dd3ba5a1e7a7837d0b3` |
| `python-binding-0006.private.json` | `e599fd90947db558c055cf5d4863e76040ce31b0f1c12a824f8d48aebb894eb6` |
| `run-binding-0006.private.json` | `5566ff98e5d54f8b08c877340d040d0c3723803c02db9ca38c4a20febdf891df` |
| `local-preparation-0006.private.json` | `c6731d42652cfa5239d2a77e04b03780fa56d21dfa8b5bc4fb07c309f43fb269` |
| `source-root-validation-0005.private.json` | `0592912c1f8a86748f81a2d48b3d4ec6b3050615c17c846629afa2a4b8ce2224` |

Dondurulan kaynak SHA’ları:

| Dosya | SHA-256 |
|---|---|
| `tools/image-optimization/p4b5_manual_consumer_qa.py` | `f5bdf16e3ee3181e02bec1fbfe91da8169b98b363f950eba7fd19f45cf6a8cdf` |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` | `bdc7a493eb4828c60ba0880809c898084f1680048db710e35f4cc50eaa9c99ac` |

## Açık kapılar

Son kaynak/run bağı için üç yeni bağımsız inceleme üst ajan tarafından yapılacaktır. Gerçek execution approval veya install/initial-start/backend-start/login/window/measurement claim’i oluşturulmadı. Gerçek cihaz, backend veya ağ akışı başlatılmadı; kurulum, Firebase girişi, ölçüm ve medya isteği yapılmadı.

Kimliği ispatlanamayan süreç veya bozuk launch/drain için FAILED sonucu korunur; başka süreç durdurulmaz. Yerel sentetik testler gerçek cihaz/QA DB/storage/auth uygunluğunu veya fiziksel kaliteyi kanıtlamaz. V2 raporundaki gerçek koşu bütçeleri ve açık P4b kalite/kabul kapıları korunur.
