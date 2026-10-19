# P4b5 tekrar ölçümü: inceleme ve yürütme sınırı

Tarih: 2026-10-19. Başlangıç commit'i: `e11df87e9a7e03c1c3804a90538a8886dfae3bf1`.
Branch: `codex/image-optimization-p3-publication`. Bu bir Epic story veya P4b6 değildir.
Kullanıcı yalnız tekrar planını hazırlamayı onayladı. Bu incelemede Git/kaynak ve
mevcut güvenli özel makbuzlar okundu; test/build/ADB/DB/auth/ağ çağrısı yapılmadı.
İki BMad inceleme ajanı oturum modelini devraldı; model override kullanılmadı.
İlgisiz dirty/untracked işler, özellikle `.memlog.md` ve local-lossless kayıtları korunur.

## Yeni öneri ve tarihsel durum

[Sayısal öneri](../../../be/docs/evidence/p4b5-retry-measurement/bounded-run-proposal-2026-10-19.json)
SHA: `06810424cfde11b22151aa4c719de448f5e4a1fa02d767c11765cac9627a2606`. Status `PROPOSED_NOT_APPROVED_NOT_EXECUTED` kalır;
sonraki insan onayı ayrı özel makbuzdur. Bu öneri/frozen plan onaydan sonra salt okunur olur.

- Yeni measurementId: `p4b5-retry-afde2e82-70d1-430a-9094-d4532d5cd798`.
- Yeni özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-retry-measurement-20261019-e11df87`. Henüz oluşturulmadı.
- Aynı budgetRunId: `p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`.
- Aynı DB: `tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09`; kullanıcı `gurhankucuk`.
- Aynı storage prefix: `local/p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`.
- Aynı UID: `jLRypTmoBrhhX21HsDJojurcKTC3`; aynı #94/tr retained LIVE ve
  `be/src/test/resources/p4/consumer-retained-ready.json` SHA
  `5cd354c3210b3a6af017bc807287c3d6f80e6ed81bcae19946da1730dd1d44b7`.

Eski P4b5 `arm / VM_REFUSED` ile kapalıdır. Kurulum1/ilk başlangıç1 tüketildi;
backend/login/pencere/medya0. Son salt okunur DB doğrulaması39/44.742.970'tir;
bugünkü canlı durum kanıtı sayılmaz. Eski backend ledger yoktur. Eski cleanup.ok,
süreç/eşleme temizliğidir; native backend drain veya ölçüm başarısı değildir.
Hangi RPC ve hata kodunun geldiği kaydedilmedi; driver kayıt yarışı yalnız olasılıktır.

Seçili PATCH1–3 kaynağı `post-run-fixes-0001/receipts/final-patch-handoff-0002.private.json`
SHA `4987f287417b5beb72118d11de1ca003ea0a500c4759046d32cc3d2650be91ad` ile bağlıdır.
Python136, Java290 ve üç assertion mutasyonu geçmiştir. Final-only closure gerçek
onay veya yeni pre-run PASS değildir. Eski APK/run/source/CP yeni yürütmede seçilmez.

## Tek denemenin hesapları

120sn pencere yerel ve credential kapılarından sonra açılır. En çok1 manifest,
12 status,4 medya ve17 HTTP; sıra detail→sayfa1→sayfa2→detail, entry `[1,2,3,1]`.
Her medya3.292.721 byte, toplam13.170.884 payload byte'tır. Login/lookup/exchange/login
verifier birer üst sınırdır. Login dahil verifier22, ek V37 delivery25'tir.
Aynı genel tavanlar korunur; başarısız rezervasyon geri verilmez.

| Sayaç | Önceki | Azami ek | Son toplam / değişmeyen tavan |
|---|---:|---:|---:|
| Pinned read |51|4|55/64|
| Read byte |256.812.666|13.170.884|269.983.550/536.870.912|
| Firebase verifier |31|22|53/64|
| QA login |3|1|4|
| Storage logical |73|4|77/94|
| Combined logical |107|27|134/159|
| V37 operation |39|25|64/64|
| V37 reserved byte |44.742.970|13.170.884|57.913.854/67.108.864|

Hesap: HTTP1+12+4=17; verifier1+1+12+8=22; delivery21+4=25;
combined4+22+1=27. Mantıksal sayılar SDK iç HTTP/OAuth veya fatura toplamı değildir.
Yeni denemede en çok1 kurulum,1 ilk başlangıç,1 backend ve1 giriş olur. Yalnız eski
P4b5 ile bu teklifin toplamı kurulum2/ilk başlangıç2'dir; eski haklar iade edilmez.

## Kaynak haritası ve seçilen dar değişiklik

Satırlar başlangıç commit'indedir; uygulamada semboller yeniden doğrulanır.
Yürütücüler kopyalanmaz. Python ve Java'da yalnız iki sabit immutable test kapsamı
bulunur: eski P4b5 varsayılanı ve bu tek açık tekrar. Serbest measurement/root/hash
override veya runtime mesajından kapsam öğrenme yoktur; global monkeypatch yapılmaz.
Yeni kapsam yalnız `--scope p4b5-retry-afde2e82-70d1-430a-9094-d4532d5cd798` ile seçilir.

| Yol / simge | Somut işlem |
|---|---|
| `tools/image-optimization/p4b5_qa_scope.py` (yeni) | İki kapalı kapsamın immutable kimlik/proposal/root/entry tablosu; bilinmeyen seçim yan etki öncesi reddedilir. |
| `tools/image-optimization/p4b5_consumer_qa.py:20–39,83–207,362–405` | Scope Controller/VM/receipt/runtime_configuration boyunca açık taşınır. Eski default kimliği korunur. |
| Aynı dosya `:533–592,723–785,811–862` | Scope'a bağlı build/installed SHA/arm ve tam backend argv sahipliği; kapalı/eski scope retleri catch içinden eski köke failure yazamaz. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScope.java` (yeni) | Python ile aynı iki sabit kapsam; sayısal profil, UID, budgetRun ve DB seçilebilir alan olmaz. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java:28–107,142–224,265–309` | Root/proposal/ready/startup ve credentialWindow validator'larına scope taşınır. Yeni kapsam argv'si tam `--scope <kimlik> --execute <state>` olur; eski iki argüman korunur. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedger.java:17–71` | Eski constructor eski scope'a delegasyon yapar. Yeni instance scope approval/OPEN/window ve backend-closed makbuzunun kimliğini bağlar; `PRIOR_ATTEMPT` satırı önceki-deneme kanıtı SHA'sını taşır. Yedi alanlı `CLOSED reads bytes auth manifests statuses media` biçimi korunur. |
| `app/mobile/test_driver/reading_consumer_qa_probe.dart:23–46,426–450` | `ConsumerQaExpectedScope.p4b5Retry` sabiti eklenir; legacy ve eski p4b5 literal değerleri korunur. |
| `app/mobile/test_driver/reading_p4b5_retry_qa_entry.dart` (yeni) | Küçük ayrı debug entry yalnız yeni compile-bound kapsamı seçer. Eski entry byte düzeyinde korunur. |
| `tools/image-optimization/test_p4b5_qa_scope.py` (yeni), `test_p4b5_consumer_qa.py` | İki kapsam ve ret/claim/argv/hash matrisi; eski testler ve PATCH1–3 korunur. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScopeTest.java` (yeni), aynı dizindeki Setup/Ledger/Composition/IT testleri | Yeni kapsam approval/credentialWindow/quota/V37; çapraz kapsam ve önceki borç reddi. |
| `app/mobile/test/reading_images/reading_p4b5_qa_probe_test.dart` | Aynı native zincir iki scope için tekrar kullanılır; yeni ham JSON marker'ı `P4B5_RETRY_RESOURCE_JSON`, eski marker aynen kalır. |
| `be/docs/evidence/p4b5-retry-measurement/verification.md` (yeni) | Seçili kaynak/test bağları, gerçek tüketim veya terminal hata, cleanup, sınırlar ve son incelemeler. |

`P4b5ConsumerQaStorage.java`, eski `p4_consumer_qa.py`, `p4_reading_comparison.py`,
P4b4 kaynak helper/doğrulayıcı, retained fixture ve bütün `be/src/main/**` salt okunur kalır.
Normal app/router/Cubit/selector/raster/ses/cache ile CMS değişmez. Test-only scope
bu üretim katmanlarında yeni mimari karar veya erişim hakkı oluşturmaz; ADR değişmez.

## Korumalar ve bağ zinciri

Yeni özel kökte onaydan sonra credential'sız gerekli kaynak/test/build kopyası,
tmp/log/raporlar tutulur. Token/credential/key kopyalanmaz; mevcut özel yollar login
rezervasyonundan sonra okunur. Repo'ya yalnız güvenli rapor gelir. Yeni makbuz/log0600,
dizin0700 ve CREATE_NEW/fsync olur; eski bütün kökler salt okunur korunur.

Önerideki12 eski proof dosyasının tam SHA'ları önce doğrulanır. P4a5'in752byte tarihsel
ledger'ı özgün hash'le kalır. Eski P4b5 terminal+cleanup+post-attempt+actual-seal+claim
zinciri, kurulum/start1 ve sıfır auth/storage/V37 delta kanıtıdır. Yeni özel kökteki
`receipts/prior-attempt-proof-0001.private.json` bu öneriye ve eski proof'lara bağlanır;
backend önce doğrular, ledger `PRIOR_ATTEMPT <sha>` ile kaydeder. Yeni ledger eski
P4b5'te ledger varmış gibi davranmaz. DB drift/başka iş tüketimi varsa gerçek koşu açılmaz.

Kaynak→offline APK→selected build→kurulu APK SHA ve backend kaynak→derlenmiş dosyalar→
gerçek CP entries/closure→Java25 binary/1GiB heap→run bağı yeniden kurulur. Python
interpreter binary, controller/helper ve gerekli import dosyaları da kaydedilir;
parent launcher exact interpreter/script/CLI/run hash'ini bağlar. Credential içeren
klasörler, geniş site-packages ve ilgisiz kaynaklar bu kapanışa alınmaz.
Eski actual/final-only makbuzları tarihsel referanstır; yeni kaynak/run kanıtı değildir.

Yeni run-binding hashlenen `priorAttemptProofFile`, `priorAttemptProofSha256`,
`pythonBindingFile`, `pythonBindingSha256` alanlarını içerir. Backend ve launcher,
incelenmiş run'daki bu SHA'ları gerçek dosyalarla ve çalıştırılan Python binary/script/
import kapanışıyla karşılaştırır. Sonradan yalnız ledger'a yazmak yeterli değildir.

İlk gerçek claim yeni run SHA'yı dondurur. Üç farklı pre-run PASS aynı yeni scope,
source ve run SHA'larına bağlı, `unresolvedFindings` kesin int0 olmalıdır. Kaynak veya
bağ değişirse o sete ait üç kapanış yenilenir. İlk onaylı workflow kullanılır; uygulama
ajanı yeni workflow/render/onay başlatmaz. Story key yoktur; sprint eşitlemesi atlanır.

## Sıralı gerçek kapılar

1. Yerel testler, kaynak/APK/CP/Python bağları ve üç pre-run inceleme geçer. Önce
   gerekli current-process envanteri salt okunur alınır; ilgisiz süreç durdurulmaz.
2. Mevcut emulator-5554,822×1828,density320,DPR2,fontScale1 ve kapalı QA doğrulanır.
   Yeni emulator açılmaz/ayar değiştirilmez. Aynı DB/user/V37/borç salt okunur yeniden
   doğrulanır. Eski kayıt geçerli olsa da bugünkü ortamın yerine sayılmaz.
3. En çok1 install claim çağrıdan önce alınır; APK kurulur ve kurulu SHA ilk start'tan
   önce doğrulanır. Sonra1 initial-start claim ile tek AM başlangıcı yapılır. Aynı
   PID/start/main isolate ve driver kayıt kapısı mevcut30sn içinde geçer; boş baseline
   B alınır. Reddedilmiş RPC tekrar edilmez. Hata güvenli stage/method/int32 koduyla
   kaydedilir. Bu kapı geçmeden backend, giriş veya medya başlamaz.
4. Baseline tekrar geçince1 backend-start claim ve startup reservation alınır. Aynı
   Java25/1GiB ve scope'a bağlı tam argv ile tek Popen yapılır; PID/start/command hemen
   kaydedilir. Ön-Spring/SDK DB/proof/CP kontrolü geçer. Startup watchdog180sn'dir.
5. Local-ready sonrası300sn kapıda baseline/PID/DB değişmemişse en çok1 QA login
   lookup/exchange/verifier rezervasyonu credential okumadan önce fsync edilir.
   Token yalnız özel dosya/bellekte kalır. Yeni config indirme veya rol değişimi yoktur.
6. CredentialWindow gerçek gate'inden sonra tek120sn pencere açılır; detail→sayfa1→
   sayfa2→detail UI ile sürülür. Ölçüm sırasında kullanıcı ekrana dokunmaz. Mevcut
   kontrol profilinde status10sn/12, lease180sn üst sınır, transfer45sn/decode10sn'dir.
7. Dört paint ve beforeBackground/background/resume/closedUi/awaited stop aynı
   lifetime/epoch/slot zincirinde doğrulanır. Attempts B+1/2/3/4, sonraB+4; active
   handle/slot/byte drain fazlarında0. Her paint fixture rol/rendition/SHA/MIME/byte/
   iki eksenli geometriyi karşılar. Eksik, legacy veya yanlış tip başarı değildir.
8. Owned QA/mapping ve backend kapanır; final DB ledger ile uzlaşır. Backend context
   kapanışından sonra yeni readonly DB kesiti gerekir. Süreç çıkışı tek başına başarı
   değildir. Pre-ready cleanup≤5sn; yalnız aynı PID/start/command için en çok1 TERM/KILL,
   `drainVerified=false/measurementSucceeded=false`. Kapanış/cleanup claim'i bütün yeni
   gerçek aşamaları kapatır. Hata terminaldir; ikinci gerçek deneme otomatik açılmaz.
9. Ham kanıt MAC_EK'te saklanır; gerçek sonuç son üç BMad incelemesine girer. Yalnız
   görev dosyaları yerel commit'e alınır. Push/PR/dağıtım yoktur.

## Test ve kabul matrisi

| Senaryo | Beklenen sonuç |
|---|---|
| Eski default / explicit eski / yeni / bilinmeyen seçim | Eski kimlik korunur; yeni yalnız açık seçimle; bilinmeyende yan etki yok. |
| Kapalı eski veya yeni scope / yanlış root | Eski köke yeni failure dahil hiçbir dosya mutasyonu yok; claim iadesi yok. |
| Karışık proposal/measurement/approval/ready/login/window/RPC receipt | Doğru scope yerine geçemez; backend/auth/medya açılmaz. |
| APK/source/installed SHA/CP/Java/Python/argv/run/review drift | İlk uygun yan etkiden önce ret; sahipliği belirsiz süreç sinyallenmez. |
| Eski borç kanıtı / sıfır delta / DB39 uyuşmazlığı | Yeni ledger/pencere açılmaz; tavan yükseltilmez veya DB resetlenmez. |
| Geç driver kaydı / hiç kayıt / main değişimi / PID replacement | Mevcut30sn içinde aynı isolate; ilk ret tekrar edilmez; auth0. |
| RPC code yanlış tip / body'de token, key, URL | Dar makbuz yalnız allowlist stage/method ve int32 kod; secret sızıntısı0. |
| Eksik/bozuk credential / geçerli makbuz / ikinci açılış | Gerçek openCredentialWindow kapısı login import öncesi ret veya tek açılış. |
| Dört native paint / beşinci girişim / phase drain / legacy | Yeni ham JSON Python v1+phase kabulüne gider; yanlış varyant/tip/attempt/drain reddedilir. |
| Close/cleanup veya başarısız başlangıç | Terminal; owned cleanup ayrı kanıt; yeni gerçek hak doğmaz. |

Onaydan sonra yalnız yeni özel kopyada izlenen tam Flutter testleri seri, analyzer ve
scope odaklı testler çalışır. Annotation değişmezse build_runner çalıştırılmaz.
Native ham JSON iki scope için yeni logdan alınır; Python'da eski
`P4B5_DART_JSON_LOG` ve yeni `P4B5_RETRY_DART_JSON_LOG` test girdileri seçilir. Eski
136/290/511 sayıları yeni test başarısı gibi taşınmaz. Loopback/native yerel fixture
sentetiktir; #94 bulut/cihaz başarısı yerine geçmez. Scope, hazır kayıt kapısı ve gerçek
credentialGate çağrısı silinen mutasyonlar meaningful assertion ile yakalanmalıdır.

Komutlar yeni özel kopyada, mevcut interpreter/SDK ile çalışır:

- `flutter test --no-pub --concurrency=1`; `flutter analyze --no-pub`.
- Gerekli ham log env'leriyle `python -m unittest discover -s tools/image-optimization -p 'test_p4b5*.py'`;
  eski controller/resource testleri de regresyona girer. Gerekli interop testi skip edilirse kapı geçmez.
- `be/` altında `./mvnw -o test`; full sonuç korunur. Docker yoksa tam suite başarılı
  gösterilmez. Alternatif unit/property/modül seçimi
  `./mvnw -o '-Dtest=*Test,!*IntegrationTest,!TellPalApplicationSmokeTest' test` olur;
  yeni `P4b5ConsumerQaScopeTest` ve mevcut Setup/Ledger/Composition testleri içindedir.
  Mevcut PG18.6 binary ile ayrı sentetik kümede yeni scope'un kalıcı V37 kotası ve
  context kapanışı sonrası uzlaşısı `P4b5ConsumerQaIT` üzerinden doğrulanır; gerçek
  credentialWindow geçişi SetupTest'te auth/DB olmadan ayrıca sınanır. Aynı SQL/fixture
  kalır. `ReadingImageComparisonTestControllerIT`, `ReadingImageComparisonServiceIT`
  ve `ApplicationModulesTest` özgün test gövdeleri de çalışır. Önceki8/20/2 sayıları
  kapsam referansıdır; yeni başarı sayıları ham çıktılardan alınır. Bağlantı base
  sınıfı geçici yerel PG overlay'i kullanırsa özgün `PostgresIntegrationTestBase`
  kaynak ve bytecode'u restore edilir; final CP hash'i doğrulanır. Overlay ve final
  kaynak/sınıf setleri ayrıdır; mutant/overlay sınıfları gerçek backend CP'ye giremez.
  Bu eşdeğer sınırlı kontroller geçmeden gerçek kapı açılmaz; eksik kontrol atlanmaz.
  Yalnız owned sentetik cluster kapanır. Alternatif yalnız bu QA kapısını destekler;
  PostgreSQL15, resmî Testcontainers veya başarısız tam Maven suite kabulü değildir.
- Mevcut controller'ın offline `build` aşaması yeni explicit scope/proposal ile kullanılır;
  debug QA paketi aynı kalır, yeni entry bağlanır. Release signing/store/CI YAML değişmez.

Bu görev yalnız emülatör P4b5 kanıtıdır. Genel P4b; normal Library/detail/reader,
erişim/URL yenileme/HTTP cache/session/retention ve üretim sayısal profil kararları
ayrı kalır. Fiziksel Android/iPhone, kalite ve peak/RSS/GPU kabulü yoktur. P0 eksik ham
kanıt ve örneğe özgü Android1×/iPhone0,75× sınırları sürer; JPEG/WebP bulanıklığı veya
sıkıştırma kazancı çözülmüş sayılmaz. Eski asset_processing112/legacy poller korunur.
