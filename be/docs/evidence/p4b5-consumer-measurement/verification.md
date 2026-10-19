# P4b5 — yerel hazırlık ve tek deneme sonucu

Tarih: 2026-10-19. Onaylı tek emülatör denemesi `arm` aşamasında `VM_REFUSED` ile
terminal kapandı. QA kurulum ve ilk başlangıç hakkı kullanıldı. Backend, login ve
ölçüm penceresi başlamadı; medya aktarılmadı. Yeni gerçek deneme yapılmadı.
Sahipliği doğrulanmış QA süreci ve forward eşlemesi kapatıldı. P4b5'in sınırlı
yürütme görevi terminal hata kaydı üretti; gerçek mobil ölçüm kabulü sağlanmadı.
Fiziksel kalite, peak/RSS/GPU, üretim profili ve genel tüketici kabulü açık kalır.

Özel kanıt kökü:
`/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d/`.
Aşağıdaki log ve makbuz yolları bu köke göredir. Ham JSON/token/VM adresleri repo'ya
alınmadı. Eski kanıtlar ve frozen blok korunuyor.

## Değişiklikler

- Ayrı `P4b5ConsumerQaSetup/Ledger/Storage` test harness'ı aynı V37 satırının
  `39 / 44.742.970` borcunu doğrular. Ek kota 25, toplam tavan 64 ve aynı prefix korunur.
  Tek 120 sn pencere, 1 manifest, 12 status, 4 sıralı pinned read, login dahil 22 verifier
  ve 13.170.884 medya byte sınırı uygulanır. Eski harness değiştirilmedi.
- Yeni debug entry compile-bound measurement/proposal/window/status kapsamını seçer.
  Eski probe parser'ı varsayılan 180 sn/18 status olarak kalır. Normal runtime, Cubit,
  router, ses ve cache kodu değiştirilmedi.
- Yeni Python controller numaralı source/classpath/APK/run bağları ve üç bağımsız
  inceleme ister. İlk gerçek claim run bağını sabitler. Düzeltme öncesi makbuzlar
  korunur; yeni bağlar yeni numarayla üretilir. Varsayılan komut yan etkisizdir.
- Ham v1 kaynak JSON'u, aynı lifetime, baseline B, paint farkları 1/2/3/4 ve bütün
  background/resume/closedUi/stop drain fazları birlikte doğrulanır. Kesin bool
  zorunludur; `stopped=false` drain kontrolünü atlatmaz.

## İlk hazırlığın korunan doğrulaması

| Kontrol | Sonuç / ham kanıt |
|---|---|
| Tüm izlenen Flutter testleri | 511/511; `logs/flutter-full-serial-0001.log` |
| Flutter analyzer |0 bulgu; `logs/flutter-analyze-0002.log` |
| Yeni probe / eski probe / P4b4 native odaklı |12/12; `logs/flutter-focused-0001.log` |
| Python regresyonu ve gerçek Dart JSON interop |125/125; `logs/python-final-pre-ready-0001.log` |
| Java unit/property/modül, Docker gerektirmeyen seçim |285/285; `logs/java-unit-regression-0001.log` |
| Restore sonrası yeni Java + modül |11/11; `logs/java-restored-0001.log` |
| Gerçek PostgreSQL/V37, yeni test |1/1; `logs/java-v37-0001.log` |
| Özgün HTTP controller IT gövdeleri |8/8; `logs/java-overlay-0001.log` |
| Özgün retained service IT gövdeleri |20/20; `logs/java-overlay-0002.log` |
| Özgün modül testi |2/2; `logs/java-overlay-0003.log` |
| Mutasyonlar |6/6 assertion ile yakalandı; `receipts/mutation-summary-0002.private.json` |
| Offline debug APK |`android/builds/build-0003`; BUILD SUCCESSFUL |
| Sentetik DB restart ve cleanup |64 /48.035.691 kalıcı; owned cluster kapalı; `receipts/synthetic-postgres-cleanup-0001.private.json` |

Resmî tam `./mvnw -o test` ilk çalışması başarısızdır: 323 testte 1 failure ve 42 error
vardı (`logs/java-full-0001.log`). Docker daemon kapalıdır; açılmadı. Kaynak kopyasının
ilk `.git`/AGENTS marker eksikliği de live-probe unit testlerini durdurmuştu. Marker
sonrası Docker gerektirmeyen 285 test geçti. PostgreSQL 18.6 ile yapılan ayrı overlay
kanıtı, resmî tam Maven suite başarısı olarak sunulmaz. Overlay yalnız test bağlantı
base sınıfını değiştirdi; özgün test gövdeleri değişmedi. Özgün sınıf hash'i restore
edildi (`receipts/synthetic-overlay-0001.private.json`). Final classpath özgün sınıftır.

İlk paralel tam Flutter çalışmasında mevcut venv `lib` bağı eksikliği nedeniyle bir
TLS fixture testi ve dış disk yükü altında üç dar süreli cache testi başarısızdı.
İlk log korundu (`logs/flutter-full-0001.log`). Mevcut venv config/lib bağlantısı
kuruldu; paket indirilmedi. Seri tam koşu 511 testi geçti. Ürün kodu değiştirilmedi.

Sentetik test DB'si gerçek QA DB'sinden ayrıdır. Homebrew PG 14 eksik ICU nedeniyle
başlayamadı; kurulu Postgres.app 18.6 kullanıldı. V37 SQL gerçek PostgreSQL'de çalıştı;
HTTP IT'leri kendi boş sentetik DB'lerinde Flyway zincirini kullandı. Yeni rezervasyon
restart ile kaldı. Bu cluster'ın yalnız owned süreçleri durduruldu.

## Matris ve gerçek native JSON zinciri

`reading_p4b5_qa_probe_test.dart` yeni scope, eski parser varsayılanı,12status/17HTTP
sınırı ve yanlış ilk configure sonrası tekrar reddini sınar. `gerçek loopback ve dört
native paint ham JSON phase kabulüne gider` testi gerçek loopback PNG aktarımı,
native codec, aynı epoch paint ve ham probe snapshot'larını `P4B5_RESOURCE_JSON`
olarak özel loga verir. Python
`test_real_dart_native_loopback_raw_json_is_accepted_by_phase_resources` bu ham diziyi
aynı phase kabul fonksiyonuna verir. Sentetik küçük görsel/kimlik kullanılır; bu test
#94 retained LIVE veya gerçek QA ölçümü değildir.

`test_nonzero_baseline_four_paints_and_stopped_false_drain_are_measured`,
`test_fifth_native_attempt_stays_terminal_through_all_drain_phases`,
`test_phase_drain_is_required_for_every_handle_slot_byte_even_stopped_false` ve
`test_every_control_and_http_ok_requires_exact_boolean` kaynak matrisini kapsar.
Geometri/rol/SHA/payload/epoch fixture oracle'ı eski saf yardımcıyla korunur.

Java `fullQuotaPreservesSeed39AndFsyncsFourPinnedReadsAnd22Verifiers`,
`exactApprovalAndAllInputProofHashesRefuseBeforeFileCreation`,
`pendingRollbackDeadlineReplayAndAuthReadOrderAreClosed`,
`springServiceReservesBeforeAuthAndReadFailureRemainsTerminal` ve
`realV37QuotaSurvivesNewBudgetInstanceAndFailedStorageIo` kalıcı kota/ön rezervasyon
zincirini sınar. Yerel başlangıç saat geri sarımı da terminaldir.

Mutasyonlar: attempt guard, beşinci decode, kesin bool, phase drain, Dart measurement
scope ve backend ek kota. İlk typed-flag mutasyonu eksik key hatasına ulaştığı için
assertion kanıtı sayılmadı; özel yanlış-tip assertion'ıyla tekrar yapıldı. Her iki
log korunur. Tüm kaynaklar restore edildi; Java sınıfları tekrar derlendi.

## Bağlar ve gerçek adım kapısı

İlk üç incelemede bulgu üreten ve artık seçilmeyen set:

- `receipts/source-binding-0004.private.json`: 1182 kaynak/test/metadata dosyası;
  SHA `24c89ab5f42a5ae154c209b97ceef7d0a5c915db8078c369cabf305840d1faa2`.
- `receipts/classpath-binding-0002.private.json`: 280 öğe, 1296 dosya;
  SHA `c80adae30f84af9bdc75b2fae18d90e065da997d5a11b23b6382e73f930b936d`.
- `receipts/selected-build-0002.private.json`: `build-0003`;
  APK SHA `e8b56aa620fd0c09092f43a6da46823242135c28f6cdc2eec62652d63bf6b7f3`.
- `receipts/run-binding-0002.private.json`:
  SHA `acb1a34dc880e1309b44eb7e7d555affdeaba6f3a0e6d6dcbd72aa8430661be4`.
- `receipts/backend-input-template-0002.private.json`: credential içermez;
  kaynak ve gerçek runtime classpath girişlerini taşır. Eski config kopyalanmaz;
  `historicalStateRoot` mevcut korunan eski state'e işaret eder.

İlk classpath makbuzunda sondaki boş Surefire öğesi yanlışlıkla repo köküne çözülüp
kapsamı genişletmişti. Gerçek adımda kullanılmaz. İlk build başladığında generated
Android registrant kapanışı eksikti. İlk build de seçilmez. Makbuzlar korunur;
`superseded-local-preparation` kaydı ayrımı tutar. Dirty giriş özel kopyada HEAD
sürümüyle alındı. Untracked lossless işleri ve gerçek config/keystore kopyalanmadı.

İlk üç inceleme bu seti kabul etmedi. Run-binding-0002 için PASS yazılmayacak.
Kök ajan yeni seçili run numarası için üç kapanmış incelemeyi CREATE_NEW üretir.
Her kayıt proposal/measurement/budgetRun, `sourceBindingSha256`,
`runBindingSha256`, `status=PASS`, kesin int `unresolvedFindings=0` ve farklı
`reviewerId` taşır. Bulgu kodu değiştirirse yeni source/build/classpath/run numarası
ve ona ait üç kapanmış inceleme gerekir. Eski kayıtlar değiştirilmez.

Yerel bağları yeniden üretmek için hazırlık yardımcısı
`/private/tmp/p4b5_prepare_bindings.py source|classpath|run` yolundadır. APK için
controller `--stage build` kullanılır. Üç incelemeden sonra gerçek controller
sırası `arm → reserve-backend → JVM başlatma + record-backend → login → open → run` olur. Bütün çağrılarda exact
`--proposal-sha256` ve mevcut ADB yolu açık verilir. JVM kök ajan tarafından yalnız
bir kez, bağdaki classpath ve Java25 binary ile `-Xmx1073741824` altında başlatılır.
`run`, awaited stop ve owned cleanup sonrasında terminal sonuç yazar. Hata yeni hak
üretmez. Gerçek adımlar bu yerel hazırlıkta çalıştırılmadı.

## Başlangıçta henüz local-ready olmayan backend cleanup'ı

Bilinen açık madde yerel kod ve testle kapatıldı. JVM'yi başlatan kök ajan,
`Popen` döndükten hemen sonra aynı PID için controller'ın `record-backend` aşamasını
çağırır. Bu yeni bir başlangıç değildir. Mevcut tek `backend-start` rezervasyonunu,
exact sınıf/`--execute` state yolunu ve PID/start kimliğini doğrular;
`backend/launch.private.json` dosyasını CREATE_NEW olarak yazar.

```text
python tools/image-optimization/p4b5_consumer_qa.py --stage record-backend --proposal-sha256 e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833 --pid <Popen.pid>
```

`local-ready` oluşmadan hata veya süre aşımı olursa mevcut `cleanup` aşaması bu
launch kaydını kullanır. Varsa JVM `startup.claim` PID'si de eşleşmelidir. Önce close
isteği, sonra en çok bir SIGTERM ve bir SIGKILL denenir. Her sinyal öncesi PID/start
ve command kimliği yeniden doğrulanır. Süre üst sınırı 5 saniyedir. Kimlik değişirse
sinyal gönderilmez. Çıkmış süreç tekrar durdurulmaz; zombie için de aynı PID/start kimliği gerekir.

Bu dal `backend-closed.json` üretmez. Sonucu açıkça
`processExitConfirmed=true`, `drainVerified=false`, `measurementSucceeded=false`
olarak kaydeder. Normal gerçek ölçüm kabulü ayrıca eşleşen native backend drain
makbuzunu ve süreç çıkışını ister; bu fallback başarıya çevrilemez.

`test_pre_ready_owned_backend_cleanup_is_bounded_and_never_claims_drain`, PID/start
replacement, yanlış startup claim, zaten çıkmış süreç ve zaman aşımı testleri geçti.
`test_real_local_pre_ready_cleanup_uses_pid_start_and_stops_owned_synthetic_child`
gerçek bir yerel Python child'ını `ps` PID/start zincirinden geçirip yalnız onu
durdurdu. Child yalnız uyudu; Java/backend/DB/SDK/auth çalıştırmadı. Bu testler dahil
125 Python testi `logs/python-final-pre-ready-0001.log` içinde geçti. Üç inceleme
bu somut yolu ve kök launcher'ın `record-backend` bağlantısını da denetlemelidir.

## F1–F5 düzeltmeleri ve yeni doğrulama

Üç bağımsız ilk incelemenin birleştirilmiş bulguları
`reviews/pre-run-findings-0002.private.json` içinde korunur. Eski bağlar, snapshot01,
mutasyonlar ve loglar değiştirilmedi. Bu düzeltmeler yalnız test harness/probe ve
yerel controller kapsamındadır.

| Bulgu | Düzeltme ve geçen test adı |
|---|---|
| F1 | Süreç çıkışı, native drain ve ölçüm sonucu ayrı alanlardır. `failure.json` yokluğu, açık `outcome=SUCCESS`, sağlıklı tam ledger ve nihai DB uzlaşısı gerekir. Context kapandıktan sonra yeni salt okunur bağlantı nihai sayıları alır. `finalOutcomeRequiresClosedHealthyLedgerAndPostContextDatabaseReconciliation`, `postContextCloseDatabaseReservationCannotBecomeSuccessfulOutcome`, `test_successful_backend_requires_healthy_terminal_ledger_and_post_close_db`, `test_ready_backend_exited_or_waited_exit_cannot_turn_failure_into_success` geçti. Önceden çıkmış ve beklerken çıkan süreçte failed makbuz, failure dosyası, DB delta ve bozuk ledger reddedildi. |
| F2 | Spring/SDK öncesinde incelenmiş run→source/classpath/selected-build→input→gerçek JVM entries/dosyalar/Java binary zinciri, tek backend-start claim'i ve üç farklı PASS inceleme doğrulanır. 1GiB heap bu kapıdadır. Ready aynı bağları taşır; controller bunları doğrular. `reviewedRunSourceClasspathJavaApkAndThreePassesBindBeforeAnyRuntimeIo`, `matchingForgedClasspathInputAndAllApprovedChainDriftsAreRejected`, `test_ready_binding_drift_is_rejected_before_backend_or_credentials` geçti. Eşleşen sahte CP+input, eski/farklı run/source, eksik/ek sınıf, farklı Java, claim/APK/review drift ve yanlış heap doğrudan reddedildi. |
| F3 | Ready ve pre-ready dalları aynı launch PID/start/command SHA sahipliğini her canlılık gözleminde ve sinyal öncesinde doğrular. Kimlik değişimi çıkış/drain kanıtı sayılmaz. `test_ready_backend_same_pid_and_argv_with_new_start_identity_never_signals_or_succeeds`, ready exit/wait ve mevcut pre-ready sahiplik testleri geçti; aynı PID/argv ile farklı start ve farklı start zombie hiçbir sinyal/başarı üretmedi. |
| F4 | Yalnız P4b5 probe snapshot'ı lifecycle/current epoch/son painted epoch/canlı attached raster gözlemini taşır. BeforeBackground paint4 loaded slot/epoch/kaynaklarına bağlıdır. Background/resume beklenmeyen stop, kapalı Cubit, credential/deadline kapanışını reddeder. `test_before_background_early_home_drain_epoch_slot_and_lifecycle_are_rejected`, `test_background_and_resume_cannot_hide_early_stop_closed_cubit_or_deadline` ve güncellenmiş gerçek loopback/native paint testi geçti. |
| F5 | Cleanup claim'i, cleanup sonucu, close isteği veya backend kapanış/hata kaydı bütün yeni gerçek aşamaları kapatır. Cleanup kendi terminal hata kaydını bırakır. `test_cleanup_zero_or_partial_consumption_closes_all_real_stages_before_io` sıfır/ara tüketimde arm/backend/login/open/run/record çağrılarını I/O öncesinde reddetti; terminal dosyası kaldırılsa da cleanup claim'i kapalı tuttu. |

Son test logları:

- `logs/flutter-review-full-0001.log`: seri 511/511. İlk hazırlıktaki seri çalışma
  gerekçesi geçerlidir; ilk başarısız paralel log korunur. Sonraki tek değişiklik
  test dosyasının Dart biçimidir; davranış değişmedi.
- `logs/flutter-review-analyze-0001.log`: analyzer sıfır bulgu.
- `logs/python-review-final-0001.log`:132/132; son tam Flutter logundaki ham
  `P4B5_RESOURCE_JSON` ve iki eski P4b4 native zinciri doğrudan Python'a verilir.
  Ham JSON değiştirilmez. Erken HOME/drain için gerçek background snapshot'ı,
  erken stop/credential/deadline kapanışı için gerçek stopped snapshot'ı yanlış
  fazda reddedilir. Alanları ayrı ayrı bozan matris de yukarıdaki testlerdedir.
- `logs/java-review-fixes-0002.log`: yeni/ilgili Java ve modül 14/14.
- `logs/java-review-restored-0003.log`: restore sonrası Docker dışı unit/property/modül
  288/288. Mutant sınıflar final classpath'e alınmaz.
- `logs/java-review-postgres-0001.log`: PostgreSQL18.6 üzerinde iki sentetik test.
  Mockito ile oluşturulan context'in close callback'i gerçek sentetik PostgreSQL'de
  ek rezervasyon yaptı; DB işlem sayısı52→53 oldu. Kapanış sonrası nihai hesap
  `V37_RECONCILIATION_FAILED` ile reddedildi. Bu test gerçek Spring/Tomcat kapanışını
  doğrulamaz. Gerçek QA DB'ye erişilmedi.
  `receipts/synthetic-postgres-review-0001.private.json` ve stop logu kümenin
  kapandığını kaydeder.
- `receipts/mutation-summary-0003.private.json`: F1 outcome, F2 heap, F3 start
  kimliği, F4 beforeBackground bağı ve F5 cleanup kapısı mutasyonlarının beşi de
  anlamlı assertion ile yakalandı. `logs/mutation-review-F1..F5-0001.log` ayrı
  loglardır. Önceki altı mutasyonun kayıtları korunur. Kaynaklar restore edildi.

Geniş Java seçimlerinin `java-review-restored-0001.log` ve `0002.log` kayıtları
Docker gerektiren sınıflar nedeniyle başarısızdır; sırasıyla327 test/39 error ve
289 test/1 error, assertion failure yoktur. Docker başlatılmadı. Son uygun seçim
`-Dtest=*Test,!*IntegrationTest,!TellPalApplicationSmokeTest` ile288 testi geçti.
Bu sonuç, başarısız resmî tam Maven veya ayrı sentetik DB/overlay sonucunun yerine
geçmez. Özgün HTTP8/service20/modül2 kanıtları ilk hazırlık loglarında korunur;
özgün test gövdeleri ve base sınıfı final kaynak/classpath'te restore edilmiştir.

## Bulgu kapanışı için seçili yeni set

Bu set üç bağımsız bulgu-kapanış incelemesinden geçti. Eski run-binding0002 için
PASS yazılmadı. Snapshot01/02 ve önceki kayıtlar korunur.

| Makbuz | Kimlik |
|---|---|
| `source-binding-0005.private.json` |1182 dosya; SHA `6af5766c1c5f25ee407300b033fe014978646714ab29f8be4548cdd6f363df44` |
| `classpath-binding-0003.private.json` |280 öğe/1298 dosya; SHA `d1497322a2ae16f6e5f1317a3a5a82726299ae6d1ef233b4bd2046ac3f588817` |
| `selected-build-0003.private.json` |SHA `1a4ef5f785e0cfe37c45169d1d8f95610414d6a79c249c814af49448e4368ad6`; `build-0004` |
| `build-0004/consumer.apk` |SHA `f58bdfc7f2e0edad9cadaa76042267b9646b9f60b90e59670d827e06c1ac18dd`; offline BUILD SUCCESSFUL |
| `run-binding-0003.private.json` |SHA `d0854140c462307683ed866e679278d16e8d41a88f0b05c57d6e5eba3ee4aa07` |
| `backend-input-template-0003.private.json` |Credential'sız; tam source/CP entries ve dosya kapanışı bu run'a bağlıdır. |

Makbuzlar `receipts/`, APK `android/builds/build-0004/` altındadır.
`reports/review-fixes-0001/` yeni/ilgili beş Java sınıfının XML/TXT raporlarını
korur. `receipts/local-handoff-0002.private.json` seçili dosya/hash'leri, test
log/hash'lerini ve bu raporun hash'ini taşır. Özgün overlay base sınıfının hash'i
`93f6108cdd2c2f13e4d8f43fa3a86344afad849af873a5b7935c9501edd65287` olarak
final derlemede de doğrulandı. CP dosya ve entry gerçek yollarında sapma yoktur.

Kök ajan bu run için `reviews/pre-run-0003-01.private.json` .. `03` yollarına üç
bağımsız PASS yazdı. Her kayıt aynı run/source SHA ve scope'a bağlıdır. Kaynak,
classpath, APK, Java ve korunan eski dosyalar yeniden doğrulandı. Bu kayıtlar son
BMad incelemesinin veya gerçek ölçüm kabulünün yerine geçmez. Önceki makbuzlar
değişmedi; Mockito anlatım düzeltmesi `receipts/root-report-correction-0001.private.json`
ile ayrı kaydedildi.

## Gerçek sonuç

Üç inceleme kapandıktan sonra salt okunur hazırlık geçti. Mevcut `emulator-5554`
822×1828, density320/DPR2, fontScale1 ayarındaydı; QA süreci yoktu. Aynı DB,
`gurhankucuk` kullanıcısı, Flyway37 ve `39 / 44.742.970` borcu doğrulandı.
Mevcut credential/config yolları bulundu; hazırlıkta içerikleri okunmadı.
`receipts/root-real-preflight-0001.private.json` bu kapıları kaydeder.

Kök launcher `arm` aşamasını yalnız bir kez çağırdı. `build-0004/consumer.apk`
kuruldu, QA ilk kez başlatıldı ve VM hizmet adresi bulundu. Yeni forward eşlemesi
açıldı. VM'nin eşleşen JSON-RPC yanıtında `error` bulunduğu için mevcut yardımcı
`VM_REFUSED` üretti. `android/baseline.private.json` oluşmadı. Yardımcı hata gövdesini
ve hangi RPC'nin reddedildiğini kaydetmediği için kesin kök neden belirlenemedi.
Driver extension'ın henüz hazır olmaması bir olasılıktır; kanıtlanmış neden değildir.
WebSocket/RPC hatası, görsel kalitesi veya mobil erişim politikası sonucu değildir.

| Gerçek tüketim / sonuç | Değer |
|---|---|
| QA kurulum / ilk başlangıç claim'i |1 /1; kullanıldı |
| Backend başlangıcı / QA login / pencere |0 /0 /0; başlamadı |
| Manifest / status / medya HTTP |0 /0 /0 |
| Medya payload / yeni rezervasyon |0 byte /0 byte |
| Ek delivery işlem |0 |
| Nihai DB işlemi / reserved byte |39 /44.742.970; salt okunur tekrar doğrulandı |
| DB tavanları |64 /67.108.864; değişmedi |
| Gerçek #94 paint / ekran |0 /0; başarı kanıtı yok |
| Gerçek v1 kaynak/epoch/phase kabulü |Yok; baseline bile alınamadı |
| Owned cleanup |Forward kaldırıldı; yalnız bu QA süreci durduruldu |
| Terminal sonuç |`FAILED_NO_RETRY`; otomatik tekrar yapılmadı |

120 saniyelik medya penceresi açılmadı. Ayrılmış kurulum/ilk başlangıç hakkı geri
verilmez; kullanılmayan backend/login/pencere hakları terminal kapı nedeniyle bu
denemeyi yeniden açmaz. `cleanup.private.json` içindeki `ok=true` yalnız sahipli
temizliğin sonucudur; ölçüm başarısı değildir. Backend başlamadığından backend drain
kanıtı yoktur. Önceki onaylar veya eski yürütücüler kullanılmadı.

Ham kayıtlar MAC_EK'te korunur:

- `terminal.private.json`, `receipts/terminal-failure-0001.private.json`:
  terminal durum ve `arm / VM_REFUSED`.
- `logs/actual-arm-0001.log`, `logs/actual-qa-logcat-0001.private.log`:
  başarısız başlangıç kayıtları. Logcat'te VM hizmet satırı vardır; Flutter unhandled
  exception veya Android fatal exception görülmedi. Bu gözlem kök nedeni kanıtlamaz.
- `cleanup.claim.private.json`, `cleanup.private.json`:
  yalnız sahipliği doğrulanmış süreç ve eşlemenin temizliği.
- `receipts/root-post-attempt-0001.private.json`:
  kapalı QA, değişmeyen gerçek DB borcu, sıfır medya ve ham log hash'i.
- `actual-executed-0001/manifest.private.json`:
  kurulan APK'ya bağlı 2202 credential'sız kaynak/derlenmiş hazırlık dosyasının
  ayrı kopyası. Backend sınıfları korunmuştur ama çalıştırılmamıştır.

Gerçek #94 için dört paint, seçilen rendition/payload, background/resume/UI close/
awaited stop ve kaynak kabulü açık kalır. Yerel 511 Flutter/132 Python/288 Java ve
sentetik PostgreSQL kanıtı bu eksik gerçek ölçümün yerine geçmez. Fiziksel Android,
iPhone, release/profile, kalite ve üretim sayısal profili kabulü verilmez.
P0 eksik ham kanıt, JPEG/WebP bulanıklığı ve örneğe özgü Android1×/iPhone0,75×
sınırları sürer. Yeni üretim, compression kazancı, genel erişim/cache/session/
retention kararı, dağıtım veya legacy poller değişikliği yoktur.

## Step04 — gerçek denemeden sonraki üç patch

Üç resmi cold incelemenin PATCH1–3 bulguları ayrı final-only kaynakta düzeltildi.
Gerçek sonuç bölümü ve önceki tüm makbuzlar korunur. Kurulu `build-0004` APK,
run0003, source0005, CP0003 ve actual-executed0001 bu yeni kodu içermemektedir.
Yeni kodla gerçek deneme, SDK/ADB/DB/auth çağrısı veya backend başlangıcı yapılmadı.
Terminal deneme tekrar açılmaz. Root in-review durumu ve kullanılmış haklar değişmedi.

Bütün yeni test çıktıları özel kökteki `post-run-fixes-0001/` altındadır. Eski
`source/` ve backend target sınıfları değiştirilmedi. Yeni Maven derlemesinin kendi
`source/be/target/` ağacı vardır; final-only classpath280 öğe/1300 dosyadır.

| Patch | Değişiklik ve anlamlı test matrisi |
|---|---|
| PATCH1 | Yalnız P4b5 `Vm`, Isolate stream'ine bir kez abone olur. `getIsolate.extensionRPCs` veya aynı isolate'ın `ServiceExtensionAdded/ext.flutter.driver` olayı gelmeden extension RPC göndermez. Aynı main isolate ve owned PID/start korunur; handshake/bekleme/RPC mevcut30sn başlangıç deadline'ını aşamaz. `test_extension_registration_list_late_event_race_and_foreign_event_obey_original_deadline` mevcut kayıt,25sn geç kayıt, yanıt öncesi kayıt yarışı ve yabancı isolate olayını sınar. `test_never_registered_isolate_exit_replacement_and_deadline_never_call_driver` hiç kayıt, isolate/main drift, exit/start, kayıt sonrası RPC yanıtında gelen exit/start ve deadline durumlarında driver çağrısı olmadığını doğrular. `test_owned_pid_start_and_deadline_guard_prevents_driver_before_and_during_wait` PID/start ve beklerken drift'i reddeder. `test_arm_uses_p4b5_protocol_and_same_owned_isolate_before_any_baseline` controller arm bağlantısını tamamen fake protokolle doğrular. Yeni başlangıç ve reddedilmiş extension RPC tekrarı yoktur. |
| PATCH2 | P4b5 RPC ret makbuzu CREATE_NEW0600 `vm-rpc-failure-NNNN.private.json` olur. Yalnız sabit proposal/measurement/budgetRun, allowlist stage/method ve geçerli int32 `rpcErrorCode` yazılır. Kod yanlış tipteyse alan yazılmaz. Params, token, VM URL, ham message/body/error/data saklanmaz. `test_rpc_failure_records_only_allowlisted_stage_method_numeric_code_once_without_secrets` sayısal/yanlış tip/dev sayı, gizli key/token/URL, bilinmeyen method/stage,0600 ve tek ret çağrısını sınar. Eski `helpers` dosyaları değişmedi. Eski gerçek `VM_REFUSED` için hangi RPC veya kodun geldiği hâlâ bilinmiyor; geriye dönük neden üretilmedi. |
| PATCH3 | Küçük `openCredentialWindow` seam'i gerçek `credentialGate → DB/admission → local gate → window/ready` sırasını kullanır. Geçici özel dosyalarla gerçek gate çağrılır. `actualCredentialWindowRejectsMissingWrongScopeReservationUidTokenAndExpiryBeforeLoginImport`23 eksik/yanlış login, measurement, reservation, UID, token path/biçim/header/signature kodlaması, payload scope ve süre örneğinde pencere/ready oluşmadığını, prerequisite callback'ine gidilmediğini ve LOGIN_RESERVED aktarılmadığını sınar. `actualCredentialWindowValidEvidenceImportsOnceThenPreservesDatabaseAdmissionAndLocalGateOrder` geçerli kanıtın bir kez pencere açtığını ve tekrarın dosyaları değiştirmediğini doğrular. Token kontrolü yerel biçim/scope/süre kontrolüdür; Firebase imza doğrulaması ve mevcut auth/DB/admission davranışı değiştirilmedi. |

| Son doğrulama | Sonuç / bu yeni alt köke göre log |
|---|---|
| Python regresyonu ve korunmuş native Dart→Python interop |136/136; `logs/python-patch-final-0002.log` |
| İlk odaklı VM matrisi |4/4; `logs/python-patch-focused-0001.log`; arm fake wiring son136 içindedir. |
| İlk yeni/ilgili Java + modül |16/16; `logs/java-patch-focused-0001.log` |
| Restore sonrası uygun Java unit/property/modül |290/290; `logs/java-patch-restored-regression-0001.log` |
| Patch guard mutasyonları |3/3 assertion ile yakalandı; `receipts/mutation-summary-0002.private.json`; Python PATCH1/2 logları0002, Java PATCH3 logu0001 |

PATCH1 registration guard kaldırılınca driver öncesi ret assertion'ı, PATCH2 makbuza
params eklenince dar alan/sızıntı assertion'ı, PATCH3 gerçek credentialGate çağrısı
kaldırılınca eksik login için pencere açılmama assertion'ı başarısız oldu. Kaynaklar
restore edildi; final Java sınıfları yeniden derlendi. Eski11 mutasyon kaydı korunur.

Python'daki gerçek uyuyan child/Popen temizleme testi bu takipte dışlandı; önceki
kanıtı korunur. Diğer güvenlik/faz/legacy testleri ve fake protokol matrisi çalıştı.
Native interop eski `logs/flutter-review-full-0001.log` içindeki değişmeyen ham
P4b5 ve P4b4 JSON'u kullanır. Flutter kaynakları değişmediği için511 test ve analyzer
tekrar çalıştırılmadı; önceki511/0 sonucu yalnız önceki Dart sürümünün kanıtıdır.
Java seçimi `-Dtest=*Test,!*IntegrationTest,!TellPalApplicationSmokeTest` idi.
Bu takipte Docker/PostgreSQL başlatılmadı; önceki başarısız tam Maven ve ayrı
sentetik DB kanıtlarının sınırı aynen geçerlidir.

Son seçili final-only makbuzlar bu alt kökün `receipts/final-only-source-0002.private.json`,
`final-only-classpath-0002.private.json`, `final-only-tests-0002.private.json` ve
`final-patch-handoff-0002.private.json` yollarındadır. Son Python/test kaynakları
`source-final-0002/`, aynı ve değişmeyen Java kaynak/sınıf çıktıları `source/be/target/`
altındadır. İlk final-only0001 yerel seti son isolate-event sınır kontrolünden önceki
ara hazırlıktır; seçilmez ve değiştirilmez. Java kaynakları0001 ile aynı olduğu için
290 test ve PATCH3 mutasyon kanıtı tekrar kullanılmaktadır. Kaynak/test/log/bytecode
hash'lerini ve immutable actual setine ayrı referansı taşırlar. Bunlar yeni bir
run binding, PASS inceleme veya gerçek ölçüm makbuzu değildir. Commit/staging,
yeni workflow/render/onay veya gerçek tekrar yapılmadı.

Ek hata etiketi ayrımı: Eski `root-driver-failure-0001` içindeki `CLEANUP_FAILED`,
root launcher AssertionError metninin genel `safe_code` etiketidir. Gerçek
`cleanup.private.json` sonucu `ok=true` idi. Asıl aşama hatası değişmeyen
`terminal-failure-0001` içindeki `stage=arm / code=VM_REFUSED` kaydıdır.

## Son inceleme ve görev kapanışı

Üç bağımsız resmi BMad incelemesinin üç patch bulgusu kapandı. Aynı incelemecilerin
seçili final-only0002 setindeki kapanış kontrolünde açık bulgu sayısı0 oldu.
Kök ajan1183 kaynak/1300 classpath dosyasını ve seçili log/XML hash bağlarını ayrıca
doğruladı. Son belge/inceleme bağlantıları kaynak veya test davranışını değiştirmez.

Spec içindeki beş görev, onaylı sınırlı uygulama ve terminal sonuç raporu kapsamında
kapanır. Gerçek mobil ölçüm başarısı sağlanmadı. Bu durum genel P4b, cihaz kalitesi
veya üretim kabulü değildir. Yeni gerçek deneme yapılmadı; push/dağıtım yoktur.
