# P4b5 tekrar ölçümü: doğrulama kaydı

Tarih: 2026-10-19. Başlangıç: `e11df87e9a7e03c1c3804a90538a8886dfae3bf1`.
Bu kayıt yalnız `p4b5-retry-afde2e82-70d1-430a-9094-d4532d5cd798` kapsamını izler.
Genel P4b veya P4b6 kabulü açılmaz.

## Yerel uygulama ve sınır

Python ve Java yalnız eski P4b5 ile yeni açık tekrar kapsamını seçer. Eski varsayılan
kimlik ve iki argümanlı Java girişi korunur. Yeni Java girişi tam
`--scope <measurementId> --execute <state>` biçimindedir. Bilinmeyen/kapalı kapsam
ve karışık kimlik, eski köke failure makbuzu dahil yazmadan reddedilir.
Yeni Flutter debug entry, kapsamını derleme sabitinden alır. Eski entry değişmedi.

Önceki P4b5’in 12 dosyası önerideki SHA’larla yeniden doğrulanır.
Yeni prior-proof, eski backend ledger varmış gibi davranmaz. Yeni ledger
`PRIOR_ATTEMPT <sha>` satırını, aynı `39 / 44.742.970` borcu ve değişmeyen
V37 tavanlarını taşır. Kurulu APK SHA’sı ilk başlangıç claim’inden önce denetlenir.
Python executable, gerçek binary, controller ve import dosyaları; kaynak, APK,
Java/classpath ve run ile bağlanır. Backend sahipliği tam argv/PID/başlangıç kimliğini ister.

Özel kaynak kökü:
`/Volumes/MAC_EK/tellpal-qa-private/p4b5-retry-measurement-20261019-e11df87/source`.
Dirty `main_image_experiment.dart` HEAD’den alındı. Local-lossless kaynakları,
gerçek config, token ve keystore kaynak kopyasına alınmadı. SDK/interpreter/paket
indirilmedi. `PYTHONPATH` yalnız mevcut websocket paketini gösteren dar
`python-imports` dizinidir; geniş site-packages stdlib’in önüne eklenmedi.

Companion’daki “onaylı değil / kök henüz yok” cümleleri planın yazıldığı anı
anlatır ve salt okunur tarihsel metindir. Sonraki `A` onayı ayrı
`receipts/human-approval-01.private.json` makbuzundadır. Frozen blok, companion
ve sayısal öneri değiştirilmedi.

## Yerel doğrulama

| Kapı | Sonuç | Özel ham log |
|---|---|---|
| Flutter tam seri suite | 513 geçti | `logs/flutter-test-0002.log` |
| Flutter analyzer | Sorun yok | `logs/flutter-analyze-0001.log` |
| Python P4 regresyonları | 148 geçti, skip 0 | `logs/python-final-0002.log` |
| Java unit/property/modül alt kümesi | 295 geçti | `logs/maven-unit-final-0002.log` |
| Yeni scope sentetik V37 ve context sonrası uzlaşı | 2 geçti | `logs/maven-synthetic-qa-0002.log` |
| Özgün service / HTTP / modül testleri | 20 / 8 / 2 geçti | `logs/maven-synthetic-comparison-0002.log` |

Python iki kapsamın yeni native ham JSON marker’larını aynı yeni Flutter logundan
okudu. Dört gerçek sentetik decode/paint ve phase drain, Python v1/phase kabulüne
girdi. Bu loopback fixture, #94 bulut/cihaz başarısı değildir.

Tam `./mvnw -o test` başarılı gösterilmez: Docker bulunamadı. İlk özel kopyada
eksik repo işareti de dört hata üretti; işaret tamamlandıktan sonra izinli alt küme
geçti. Ham başarısız sonuç `logs/maven-full-0003.log` içinde korunur.
Flutter’ın ilk özel kopyasında eksik bağımlı fixture/interpreter yolları düzeltildi;
tam seri suite yeniden çalıştırıldı. Hatalı ilk loglar silinmedi.

Mevcut PostgreSQL 18.6 ile yalnız yeni owned sentetik küme kullanıldı.
İlk sentetik başlangıç uzun Unix socket yoluyla sunucu açmadan reddedildi.
Kısa owned tmp socket ile ikinci yerel test hazırlığı geçti. Gerçek QA DB
kullanılmadı. Sentetik cluster kapatıldı. Özgün `PostgresIntegrationTestBase`
kaynağı ve bytecode’u birebir restore edildi; final normal derleme yapıldı.
Makbuz: `receipts/synthetic-pg-result-0002.private.json`.
Bu sonuç PostgreSQL 15/Testcontainers veya tam Maven suite kabulü değildir.

## Mutasyonlar

Final kaynak/CP dışındaki `mutations-0002` içinde:

- Runtime scope kontrolü silinince altı `ValueError not raised` assertion’ı oluştu.
- Driver hazır kayıt çağrısı silinince beklenen `isolate/1` yerine `None` yakalandı.
- Gerçek `openCredentialWindow` içindeki credentialGate çağrısı silinince eksik
  giriş kanıtının ret assertion’ı başarısız oldu.

Üç süreç de beklenen nonzero sonuçla kapandı. Makbuz:
`receipts/mutation-results-0002.private.json`. İlk driver mutasyon kopyasında
fixture yolu eksikti; o hata kanıt sayılmadı ve doğru ağaçta yeniden doğrulandı.
Mutant veya overlay sınıfları final backend classpath’ine alınmadı.

## Credential sırası

Somut sıra: ön-Spring/SDK proof, source, CP, Java ve DB kapıları; ardından mevcut
backend SDK yerel credential hazırlığı; local-ready; login claim/rezervasyon;
sonra native login worker’ın credential okuması. Setup kaynak akışında
`validateReviewedRuntime` ve ilk salt okunur DB kontrolü, `context(...)` çağrısından
öncedir. Yanlış bağ testleri Spring/SDK hazırlığına ulaşmadan reddedilir.

Mevcut storage configuration, context refresh sırasında service-account dosyasını
okur. Test-only storage wrapper, bean oluşturulduktan sonra uygulanır; bu okumayı
geciktiren yeni lazy/guard eklenmedi. Verifier ve medya çağrıları mevcut ledger
rezervasyonlarını izler. Companion’ın genel credential cümlesi, somut backend→login
sırasına uygun biçimde login worker yüklemesi için yorumlandı. Root bu ayrımı
gerçek yürütme öncesinde kayıt altına aldı. Üretim kodu değiştirilmedi.

## Gerçek sonuç ve son kapılar

Yerel uygulamanın devir anında gerçek QA DB/SDK/auth/ADB/kurulum/başlangıç/backend/
login/pencere aşaması çağrılmadı. Gerçek hak tüketimi sıfırdır.
Offline APK ve bağ makbuzları sonraki devir bölümünde kaydedilir.
Üç bağımsız pre-run inceleme, tek gerçek sonuç, owned cleanup, nihai DB uzlaşısı,
üç son inceleme ve görevle sınırlı yerel commit kök ajanındadır. Bu bölüm bu
adımlar tamamlanana kadar açıktır.

## Seçilen yerel dosyalar

Devir: `receipts/implementation-handoff-0001.private.json`. Bütün seçimler
`0001` numaralıdır. Kaynak 1.026 dosya, backend classpath 280 entry / 1.302
dosya ve Python import kapanışı 1.387 dosyadır.

| Bağ | SHA-256 |
|---|---|
| Source | `5860776468783fd75cdd94f91dc96ea559fd9b82143ed0cb9b8a4b82f9b5522b` |
| Classpath | `452e2a668658013fc11a7d6bf622cdf9b0c1271887e61c92d16429c0cc67b4d8` |
| Python | `f6317d74b924ba14dfa1e1ef788d4286572db811ea0e93ca1867cff79aaee055` |
| Selected build | `097a60921665267e58efbd3a4e2c073e215b9704f591b94e0205296afaf4dc1b` |
| APK | `5a4200f6102a341859250074474a37c25ebd006781f2534ba8101d5b592155dc` |
| Run | `6fca2e3a750f4b07779cb5f86c54af6a6a6fb9562e86627af1de8c4ad472bea9` |
| Kök yürütücü | `bd788da12577eb8f16a7f8172a6f2f249e2bcf5a40cb6eaf6324c40d51129f56` |

Kök yürütücü `ops/root-run-0002.py` olarak MAC_EK'te saklanır. Tam Java argv
ve Python prefix'i `receipts/launch-command-0001.private.json` ile eşleştirildi.
Yürütücü, üç pre-run makbuzunun kendi SHA'sını da taşımasını zorunlu tutar.
Bu dosyaların hazırlanması gerçek kurulum veya başlangıç hakkı tüketmedi.

Kök koruma denetimi geçti: 3.296 repo dosyası ve önceki P4a5/P4b1–5
kayıtları, eski actual/final classpath dosyaları ve frozen blok korundu.
`.memlog.md` ve kullanıcının diğer dirty/untracked işleri değişmedi.

## Koşu öncesi bulgular ve dar düzeltme

Üç bağımsız inceleme ilk yürütücüde iki operasyon açığı buldu; bu sürüme PASS
verilmedi. İlk dosya `ops/root-run-0001.py`, SHA
`9a608408cfbedd85cf9a561fb114c5ccb1edab9b032e78291636e4b19cfbe827`
ile korunur.

- Backend hata marker'ı, ayrı cleanup CLI aşamasını da kapatıyordu. Yeni
  yürütücü, daha önce doğruladığı aynı parent `Controller` ile yalnız mevcut
  owned temizliği çağırır. Kapalı CLI ve yeni gerçek aşamalar açılmaz.
- Java Popen ile launch kaydı arasında pahalı bir CLI gate vardı. Kayıt timeout
  alırsa backend temizliğe seçilemiyordu. Yeni yürütücü PID/start/tam argv
  kimliğini Popen hemen ardından saklar. Erken hata halinde eksik kayıt yalnız
  aynı parent'ın child'ı için tamamlanır. Mevcut sınırlı cleanup ve reap yolu
  kullanılır. Sahiplik doğrulanamazsa sinyal veya temizlik başarısı üretilmez.

Kaynak/CP/Python/APK/run bağları değişmedi. Yalnız özel kök yürütücüsü ve seçim
makbuzu `0002` oldu. Beş yeni yerel regresyon geçti: terminal marker sonrası
owned QA temizliği; marker varken parent kimlik kaydı; eksik launch ile gerçek
sentetik Python child'ın TERM/reap edilmesi; PID kimliği değişiminde sinyal
verilmemesi; belirsiz sahiplikte başarı verilmemesi. Bu testlerde Java, ADB,
gerçek QA DB, SDK veya auth çalışmadı. Özel test ve log:
`ops/root-tests-0002.py`, `logs/root-driver-regression-0002.log`.
Makbuz: `receipts/root-driver-regression-0002.private.json`.

İlk inceleme bulguları ve düzeltme kayıtları korunur. Sonraki üç bağımsız kapanış
yeni yürütücü için PASS verdi. Reviewer kimlikleri `p4b5_retry_prerun_backend`,
`p4b5_retry_prerun_mobile`, `p4b5_retry_prerun_evidence`; açık bulgu sayıları
kesin `0`'dır. Makbuzlar `reviews/pre-run-0001-01..03.private.json` içindedir.
Hepsi aynı source/run ve yeni root driver SHA'sına bağlıdır.

## Tek gerçek denemenin sonucu

**Ölçüm başarısız: `arm / VM_DEADLINE`; tekrar yapılmadı.** Güncel salt okunur
kapıda mevcut emulator-5554 geometrisi, QA sürecinin kapalı olması ve aynı
V37 borcu doğrulandı. Üç pre-run PASS'ten sonra tek APK kuruldu. Kurulu SHA,
ilk başlangıçtan önce seçili APK ile eşleşti. Tek ilk başlangıç yapıldı.

VM bağlantı ve forward dosyaları oluştu; boş baseline dosyası oluşmadı.
Hata, backend rezervasyonundan önce geldi. Logcat'te Dart VM servisinin açıldığı
görülür. `VM_DEADLINE`, toplam 30 saniyelik hazırlık veya 10 saniyelik iç RPC
sınırı kaynaklı olabilir. Ham kayıt hangi iç adımın süre aşımına uğradığını
ayıramaz; kök neden doğrulanmış değildir. RPC ret cevabı/method/int32 hata kodu
makbuzu yoktur. Sayısal kod veya driver kayıt yarışı nedeni uydurulmadı.

| Hak / tüketim | Bu deneme |
|---|---:|
| QA kurulum / ilk başlangıç | 1 / 1 |
| Backend başlangıcı / QA girişi / ölçüm penceresi | 0 / 0 / 0 |
| Manifest / status / medya HTTP | 0 / 0 / 0 |
| Görsel payload byte | 0 |
| Ek pinned read / verifier / storage logical | 0 / 0 / 0 |
| V37 operation, önce → sonra | 39 → 39 |
| V37 reserved byte, önce → sonra | 44.742.970 → 44.742.970 |

V37 tavanları 64 / 67.108.864 olarak kaldı. Önceki genel borç ve tavanlar
değişmedi; başarısız kurulum/ilk başlangıç hakları geri verilmedi. Önceki
P4b5 ile bu tekrarın toplamı iki kurulum ve iki ilk başlangıçtır.

Owned forward kaldırıldı; yalnız bu QA süreci durduruldu. Cleanup makbuzu
`ok=true` ve terminal `FAILED_NO_RETRY` oldu. Son salt okunur ADB kontrolü
QA PID'sinin ve sahipli forward'ın olmadığını doğruladı. Backend hiç başlamadı;
backend dizini veya ledger'ı yok. Bu cleanup sonucu native backend drain veya
ölçüm başarısı değildir. Son salt okunur DB sorgusu aynı borcu doğruladı.
İlgisiz backend/frontend süreçlerine müdahale edilmedi.

Çalıştırılan source/CP/Python/APK dosyaları tekrar hash'lendi ve mühürlendi:
`receipts/actual-executed-snapshot-0001.private.json`, SHA
`8d389b69f3285d61cbe14b0fd9fa1e5d589bf0bdc7ccd6c96752efd2120827a6`.
Kanıtlar: `terminal-failure-0001`, `root-post-attempt-database-0001`,
`post-attempt-readonly-diagnostics-0001` özel makbuzları; `cleanup.private.json`
ve `logs/post-attempt-logcat-0001.private.log`. Ham kayıtlar MAC_EK'tedir.

## Matris denetimi

Frozen bloğun her satırı çalışıp geçen kontrollerle eşleşir. Python 148 testte
skip yoktur; native JSON iki kapsam için aynı yeni Flutter logundan alınmıştır.

| Frozen beklenti | Geçen kapsam |
|---|---|
| Kapalı/karışık scope veya hash drift | `test_p4b5_qa_scope.py`: kapalı/yanlış root/proof/Python/argv; Java ScopeTest ve SetupTest: source/CP/Java/APK/review drift |
| Geç driver/PID/isolate ve tek ret | Python `test_extension_registration_list_late_event_race_and_foreign_event_obey_original_deadline`, `test_never_registered_isolate_exit_replacement_and_deadline_never_call_driver`, güvenli RPC testi |
| Eksik giriş kanıtı | Java `actualCredentialWindowRejectsMissingWrongScopeReservationUidTokenAndExpiryBeforeLoginImport`, yeni scope ret testi ve credentialGate mutasyonu |
| Dört paint ve aynı lifetime/epoch/drain | Flutter iki kapsamın gerçek loopback native testi; Python `test_retry_native_loopback_json_preserves_four_paints_and_phase_drain` ve faz/attempt negatif testleri |
| Hata/süre/kota ve owned cleanup | Java Ledger/Setup/gerçek sentetik V37 testleri; Python owned child/timeout/terminal testleri; beş root regresyonu; tek gerçek terminal ve salt okunur cleanup/DB kanıtı |

## Açık kabul sınırları

Bu koşuda #94 görselleri görüntülenmedi; seçili rendition/aktarılan byte/native
kaynak kullanımı ölçümü üretilemedi. Yerel native loopback kanıtı bunun yerine
geçmez. Fiziksel Android/iPhone, kalite, peak RSS/GPU ve üretim performansı kabulü
açıktır. Yeni üretim, profil değişikliği veya JPEG/WebP kalite kabulü yoktur.
P0'ın eksik ham kanıt sınırı ve önceki cihaz örneklerinin genel üretim ayarı
olmadığı kaydı korunur.

Bu görev tek sınırlı denemenin terminal sonucunu kaydeder. P4b genel kapanışı,
normal tüketici geçişi, erişim/URL yenileme/HTTP cache/session-retention ve üretim
profili kapıları açık kalır. Yeni gerçek deneme bu kaydın devamı olarak açılamaz.
Son inceleme ve görevle sınırlı yerel commit, gerçek denemeden ayrı workflow adımlarıdır.

## Son inceleme ve yalnız test düzeltmesi

Üç BMad katmanı birlikte çalıştı. Blind ve edge katmanları bulgu bildirmedi.
Verification katmanının iki bulgusu `high / patch` olarak sınıflandı: yeni
kapsamın gerçek kapı bağlantısı ve terminal `PRIOR_ATTEMPT` tüketicisi için
doğrudan negatif regresyonlar eksikti. İkisi mevcut iki Python test dosyasında
kapatıldı; runtime, Java, Dart, APK veya gerçek koşu kaynakları değiştirilmedi.

- Gerçek `arm → gate → retry_bindings` yolu bozuk prior-proof/Python makbuzunda
  kurulum claim'i ve cihaz çağrısı öncesinde reddediliyor. Bağlantı çağrısı silinen
  mutant iki assertion hatasıyla yakalandı.
- Yeni kapsamın kapanış tüketicisi doğru tek `PRIOR_ATTEMPT` satırını geçiriyor;
  eksik, yinelenmiş ve yanlış SHA'yı, dış ledger hash'leri tutarlı olsa da reddediyor.
  Karşılaştırması kaldırılan mutant üç assertion hatasıyla yakalandı.

Yeni ilgili tam Python seçimi **150 geçti, skip 0**. Final-only kaynak/test ve
loglar `post-run-tests-0001/` altındadır. `-B` ve
`PYTHONDONTWRITEBYTECODE=1` ile actual kaynak bytecode'u da korunmuştur.
3.723 bağlı dosya ve 28 actual kanıt dosyası değişmedi. Gerçek deneme tekrarlanmadı.
Bu sonuç, 148 testlik koşu öncesi kanıtı güncellemez; sonraki test düzeltmesidir.
Java/Flutter/runtime değişmediği için bu aşamada yeniden derlenmedi.

| Final-only kanıt | SHA-256 |
|---|---|
| Kaynak manifesti | `03644a819ce63be5eb3ab6d4f34994595c876f826395e7d5fb380b9c44ee49f5` |
| Python 150 logu | `f280d0f16658afed3bb6a8d4d787ab3812196ce632b4faf9c637f475ce418b6e` |
| Final-only kapanış | `01bbad57fe834fdf2be603cc231e55b80f6abdeb93d2d9e635df91c8025def23` |

Üç katman test eklerini tekrar inceledi. Blind: bulgu yok; edge: boş bulgu;
verification: iki bulgu kapandı, açık bulgu0. Raporun ölçüm başarısızlığı ve genel
P4b sınırları aynen geçerlidir. Yerel commit yalnız görev dosyalarını kapsar.

## Koşu öncesi somut devir

Seçilen kaynak özel kopyadadır. Offline debug APK başarıyla üretildi; kurulmadı.
`source-binding-0001` 1.026 dosyayı, `classpath-binding-0001` 280 entry ve
1.302 dosyayı, `python-binding-0001` 1.387 dosyayı bağlar.

| Bağ | SHA-256 |
|---|---|
| Source binding | `5860776468783fd75cdd94f91dc96ea559fd9b82143ed0cb9b8a4b82f9b5522b` |
| APK, `android/builds/build-0001/consumer.apk` | `5a4200f6102a341859250074474a37c25ebd006781f2534ba8101d5b592155dc` |
| Selected build | `097a60921665267e58efbd3a4e2c073e215b9704f591b94e0205296afaf4dc1b` |
| Classpath binding | `452e2a668658013fc11a7d6bf622cdf9b0c1271887e61c92d16429c0cc67b4d8` |
| Python binding | `f6317d74b924ba14dfa1e1ef788d4286572db811ea0e93ca1867cff79aaee055` |
| Run binding | `6fca2e3a750f4b07779cb5f86c54af6a6a6fb9562e86627af1de8c4ad472bea9` |
| Backend input template | `3729014b69ca39c29b8809e2014d5cb17d4f6058f2ccbfd42b654595c5f750b9` |

Bütün makbuzların generation numarası `0001`dir. Tam seçim:
`receipts/implementation-handoff-0001.private.json`. Tam Java argv, Python prefix,
environment ve mevcut ADB/aapt yolları `receipts/launch-command-0001.private.json`
içindedir. Java komutu seçili Java25 binary, `-Xmx1g`, `-cp`, seçili entry listesi,
Setup sınıfı ve yeni scope’un tam argümanlarından oluşur.

`pythonExecutable` mevcut repo `.venv/bin/python` yoludur; `pythonBinary` onun
canonical binary hedefidir. Bu ayrım login spawn’ın mevcut venv paketlerini bulmasını
korur. Controller kesin özel `source/tools/image-optimization/p4b5_consumer_qa.py`
dosyasıdır. Komut prefix’i executable/controller/`--scope`/yeni kimlik/
`--proposal-sha256`/öneri SHA sırasını kullanır. Sonuna yalnız seçilen stage ve
onun izinli argümanları gelir. Bunlar yerel devir seçimleridir; gerçek aşama çağrısı değildir.

`receipts/local-closure-0001.private.json` kaynak, CP, Java ve çalışan Python runtime
bağlarının yeniden doğrulandığını ve gerçek claim bulunmadığını kaydeder. Gerçek
backend dizini dahi oluşturulmadı. Önceki makbuzlar korunur; yeni dosyalar 0600,
dizinler 0700’dür. Sonraki incelemeler aynı source/run SHA’larına bağlanmalıdır.
