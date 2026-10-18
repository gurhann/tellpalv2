# P4a5 yerel kurtarma: offline uygulama devri

Tarih: 2026-10-18. [Onaylı kurtarma planının](../../../../_bmad-output/implementation-artifacts/image-optimization/p4a5-local-runtime-recovery.md)
kaynak ve offline test dilimi tamamlandı. **Gerçek başlangıç veya ölçüm yapılmadı.**
Üç hedefli inceleme ve gerçek çalışma root'a aittir. Spec `in-progress` kalır;
normal BMad son inceleme/commit kapısına geçilmedi.

## Değişen davranış

- `P4ConsumerQaSetup.java`, yalnız sabit özel kökün `backend-02/` dizinini kabul eder.
  `inputs.private.json` içinde açık `localDbUsername: "gurhankucuk"` zorunludur.
  Eksik, boş, farklı veya yanlış tipli değer Spring/SDK öncesinde reddedilir.
  Değer `--spring.datasource.username=gurhankucuk` komut satırı ayarına aktarılır;
  ortam/default `tellpal` rolüne düşemez. Gerçek DB bağlantısında da `current_user`
  doğrulanır. Ürün `application.yml` varsayılanı değişmez.
- İlk backend failure/ledger, cleanup, salt okunur DB kanıtı, devir, önceki yerel
  reservation/connection, 94 dosyalık mühür ve insan A kararı sabit SHA'larla doğrulanır.
  İlk ledger yalnız eski borç ve `CLOSED 0 0 0 0 0 0` taşımalıdır. Pencere, yeni harcama
  veya yeni başarısızlık kapıyı kapatır. Önceki hiçbir dosya taşınmaz ya da yeniden açılmaz.
- `p4_consumer_qa.py --reserve-backend`, yalnız offline dosya denetimi yapar;
  yeni backend payını JVM çağrısından önce exclusive/fsync ile tüketir.
  Java ayrıca `startup.claim.private.json` dosyasını CREATE_NEW ile yazar.
  Aynı reservation ikinci JVM başlangıcını açmaz.
- Kurtarma arm'ı, cihaz çağrısından önce ayrı QA payını tüketir. Kurulum yapmaz;
  mevcut paketin `pm path`/`sha256sum` sonucunu aynı APK SHA'sına bağlar.
  Yeni intent/failure/connection adları `-03`, yeni yerel onay/reservation adları `-02` olur.
  Login ve run yalnız `backend-02/` ile `android/connection-03.private.json` kullanır.
  Backend PID'si JVM claim'ine; connection yeni onay, QA reservation ve aynı APK'ya bağlanır.
  Pasif connection/VM ön kontrolü backend payı veya JVM claim'i gerektirmez;
  login/run için iki payın da ayrılması ve JVM claim'i zorunludur.

## Kesin dosya şemaları

Özel kök aşağıdaki örneklerde `P4_PRIVATE` olarak gösterilir:
`/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.

Yeni A kaydı `android/local-waiting-approval-02.private.json` dosyasıdır.
Tam, hassas verisiz JSON sözleşmesi [ortak fixture'dadır](../../../src/test/resources/p4/consumer-recovery-approval.json).
Python `recovery_expected()` ve Java `recoveryApproval()` bu aynı sözleşmeyi sınar.
Alan ekleme/çıkarma, farklı hash, sayısal alanlarda float/bool veya kapsam artışı reddedilir.
Bu uygulama gerçek A/reservation/backend girdilerini oluşturmadı.

Root incelemelerden sonra dosyaları CREATE_NEW/0600, yeni dizini 0700 ile hazırlar:

```python
import sys
from pathlib import Path
sys.path.insert(0, 'tools/image-optimization')
import p4_consumer_qa as qa
p4_private = qa.PRIVATE
qa.write_json(p4_private / qa.RECOVERY_APPROVAL, qa.recovery_expected())
state = p4_private / 'backend-02'
state.mkdir(mode=0o700)
qa.write_json(state / 'inputs.private.json', {
    'repoRoot': str(qa.ROOT), 'localDbUsername': 'gurhankucuk'})
qa.write_new(state / 'approval.json', qa.private_file(p4_private / 'backend/approval.json', 4096))
```

İki ayrı reservation, `recovery_reservation(approval_sha, kind)` şemasını taşır:

```json
{
  "proposalSha256": "079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6",
  "runId": "p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09",
  "recoveryApprovalSha256": "<yeni A dosyasının gerçek SHA-256 değeri>",
  "kind": "QA_START veya BACKEND_START",
  "state": "RESERVED_NO_AUTOMATIC_RETRY",
  "maxStarts": 1,
  "backendDirectory": "backend-02",
  "connectionFile": "android/connection-03.private.json"
}
```

QA kaydı `android/local-waiting-reservation-02.private.json`, backend kaydı
`backend-02/startup.reservation.private.json` olur. `kind` dosyaya göre tek tam değerdir.
Java claim'i `recoveryApprovalSha256`, `startupReservationSha256` ve gerçek `pid` taşır.
Yeni connection eski şemaya ek olarak `recoveryApprovalSha256` ve `qaReservationSha256`
taşır. Eksik veya eşleşmeyen bağ login/run öncesinde reddedilir.

## Root için gerçek çalışma sırası

Bu komutlar burada çalıştırılmadı. CWD repo köküdür. Önce üç hedefli inceleme,
kaynak/kopya/APK kimliği, root'ın aynı Java 25/JDBC ile salt okunur DB/rol/heap ön
kontrolü ve bütün eski hash'ler geçmelidir. Başarısızlıkta yeni başlangıç yapılmaz.

1. Yukarıdaki yeni onay/girdileri hazırla. Aynı emülatörü bir kez beklemeye aç:

   ```sh
   python3 tools/image-optimization/p4_consumer_qa.py --arm --adb /Volumes/MAC_EK/dev/android_sdk/platform-tools/adb
   ```

   Araç QA payını cihaz çağrısından önce ayırır. Root mevcut sistem Python/websocket
   ortamıyla yeni connection üzerinden pasif VM snapshot'ını doğrular. APK build/kurulum yoktur.

2. Backend payını JVM'den önce ayır:

   ```sh
   python3 tools/image-optimization/p4_consumer_qa.py --reserve-backend
   ```

   Ardından mevcut Java 25 ile yalnız bir JVM çağrısı yapılır:
   `java -Xmx1g -cp <mevcut-test-classes:classes:dependency-classpath> com.tellpal.v2.support.P4ConsumerQaSetup --execute <P4_PRIVATE>/backend-02`.
   Mevcut classpath kanıtı `receipts/backend-test-classpath-review-01.private.txt`,
   derlenmiş sınıflar `source/be/target/{test-classes,classes}` altındadır.
   `JAVA_HOME`, binary SHA, heap, loopback ve kapalı poller/scheduler/Flyway korunur.
   Yeni launch ve runtime logu yalnız `backend-02/` altında CREATE_NEW ile tutulur.

3. `backend-02/local-ready.json`, claim PID/komutu, V37 borcu ve retained LIVE geçtikten
   sonra tek login, mevcut Firebase venv'iyle yürür:

   ```sh
   tools/image-optimization/.venv/bin/python tools/image-optimization/p4_consumer_qa.py --login --adb /Volumes/MAC_EK/dev/android_sdk/platform-tools/adb --admin-credential <kullanıcı-seçimli-mevcut-0600-Admin-dosyası> --web-api-config /Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb/runtime-auth/web-api.private.json
   ```

   Login reservation credential okumadan önce yazılır. Eski 10 saniyelik helper,
   doğrudan private token dosyası ve kümülatif login/verifier bağları korunur.

4. Root CREATE_NEW ile yalnız `backend-02/open.request` yazar; yeni `ready.json`
   oluşunca tek 180 saniyelik pencere sistem Python sürücüsüyle yürür:

   ```sh
   python3 tools/image-optimization/p4_consumer_qa.py --run --adb /Volumes/MAC_EK/dev/android_sdk/platform-tools/adb
   ```

   Dört rendition/13.170.884 byte, manifest 1/status en çok 18/HTTP en çok 23,
   login dahil ek verifier en çok 28 ve eski toplam borç/tavanlar değişmez.
   Gerçek geometri, paint, byte/sayaç ve sahiplik kapanışı MAC_EK'te kaydedilir.

## Offline doğrulama

| Kapı | Sonuç | Yeni özel ham kayıt |
|---|---|---|
| Python kurtarma + eski P4 sürücü matrisi | 79/79 geçti | `logs/python-runtime-recovery-05.log` |
| Java ledger/composition/setup | 17/17 geçti; atlama/hata yok | `logs/java-runtime-recovery-02.log` |
| Kaynak/özel kopya | Beş değişen kaynak/fixture aynı SHA | `receipts/implementation-audit-runtime-recovery-02.private.json` |
| Korunan dosyalar | 3241 repo + 2 plan + 72 eski run + 94 özel dosya değişmedi | Aynı makbuz |
| Ana ve kurtarma frozen blokları | İki hash korundu | Aynı makbuz |
| `git diff --check` | Geçti | Aynı makbuz |

Python komutu `python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py' -v`;
Java komutu özel kopyanın `be/` dizininde mevcut wrapper ve Java 25 ile
`sh ./mvnw -o -Dtest=P4ConsumerQaLedgerTest,P4ConsumerQaCompositionTest,P4ConsumerQaSetupTest test` idi.
SDK delegate, ADB, OS süreçleri ve login sınırları testlerde mock edildi; gerçek hizmet,
ledger ve Spring sarmalarının önceki kompozisyon testleri de çalıştı.

Yeni matris; yanlış/eksik A, her eski hash'in değişmesi, ilk ledger'da harcama/pencere,
DB kullanıcı düşüşü, ayrı/exclusive rezervasyonlar ve JVM claim'i, yeni hatadan sonra
kalan payın kapatılması, aynı APK/connection/PID bağı ve yeni backend'de çağrı öncesi login
rezervasyonunu kapsar. Eski süre/HTTP/medya/paint/cleanup testleri de geçti.

İlk Python 01 koşusu yalnız özel kopyadaki 49 consumer testini çalıştırdı. 02 koşusu
eski P4 regresyonuna ait hassas olmayan JSON fixture eksikliğiyle 76 testte iki hata verdi.
Eksik test ve fixture özel kopyaya eklendi; 03 koşusu 77/77, 04 ve son 05 koşuları 79/79 geçti.
05 koşusu, pasif connection ön kontrolünün backend payını gerektirmediğini de sınar.
Bütün loglar ayrı tutuldu. Java 01 koşusu 16/16, son 02 koşusu 17/17 geçti.
Yeni üretim/mobil kaynak değişmediği için APK/mobil/gerçek DB IT yeniden çalıştırılmadı.

Gerçek auth/Storage/başlangıç/pencere/paint sonucu henüz yoktur. Önceki hata başarılı
drain olarak sunulmaz. P4b, genel tüketici geçişi, üretim profili ve fiziksel kalite
kabulü açık kalır. Başarısızlık yeni hak üretmez.
