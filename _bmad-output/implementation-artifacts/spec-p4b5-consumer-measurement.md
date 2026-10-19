---
title: 'P4b5: Mevcut çıktılarla tek sınırlı QA emülatör ölçümü'
type: 'feature'
created: '2026-10-19'
status: 'done'
baseline_commit: 'd80884da6c0fcc5b252278924b0dd0b2a2545e27'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/architecture.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b5-consumer-measurement-investigation.md'
---

<frozen-after-approval reason="Kullanıcıya sunulan somut P4b5 planı">

## Amaç

P4b1–4'ü mevcut #94 PNG'leriyle tek gerçek QA emülatör denemesinde ölç. Yerel
hazırlık ve üç koşu-öncesi inceleme geçince sayısal önerideki tek hakkı kullan.

## Sınırlar

**Daima:** Yeni measurementId, aynı kalıcı bütçe borcu; eski kayıtlar korunsun.
120sn, 1 manifest/12 status/4 medya, en çok17 HTTP/13.170.884 byte; login dahil
22 verifier, ek25 delivery işlem. QA kurulum/ilk başlangıç/backend/login/pencere
birer hakkı geçmez. Her hak çağrı öncesi rezerve edilir; hata terminaldir.

**Önce sor:** Tekrar, yeni üretim/cihaz veya erişim/cache/üretim profili kararı.

**Yapma:** Borç/DB reset, eski onayı açma, yeni storage/IPC/retry, runtime/Cubit/ses/cache
ve normal tüketici değişikliği. Fiziksel kalite/peak kabulü veya push/dağıtım yoktur.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Kaynak/APK/claim/DB/proof drift | Auth/medya açılmaz; ret kaydı korunur. |
| Dört gerçek paint | Rol/SHA/byte/geometri/epoch ve v1 kaynak kabulü birlikte geçer. |
| Baseline B; dört paint | Attempts farkı1,2,3,4; aynı lifetime; ek decode reddedilir. |
| Background/resume/closedUi/stop | Phase drain açıkça doğrulanır; stopped=false kontrolü atlatamaz. |
| Legacy/eksik/yanlış tip | Ölçüm başarısı yoktur; bool truthiness veya sıfır fallback kullanılmaz. |
| Süre/kota/ortam/kimlik hatası | Tek deneme kapanır; owned cleanup; otomatik tekrar yoktur. |

</frozen-after-approval>

## Kod haritası

[İnceleme](image-optimization/p4b5-consumer-measurement-investigation.md): tam proof,
bütçe hesabı, yeni dosyalar, yeniden kullanım, aşamalar ve korunacak alanlar.
[Sayısal öneri](../../be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json)
SHA: `e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833`.

## Görevler ve kabul

- [x] `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java`, `P4b5ConsumerQaLedger.java`, `P4b5ConsumerQaStorage.java`: ayrı test harness; aynı V37 satırı ve ek kota.
- [x] `app/mobile/test_driver/reading_p4b5_qa_entry.dart`, `reading_consumer_qa_probe.dart`: compile-bound yeni scope; eski varsayılan korunsun.
- [x] `tools/image-optimization/p4b5_consumer_qa.py`: tekil aşamalar, kaynak/phase kabulü ve owned cleanup; eski yürütücü değişmez.
- [x] `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQa*Test.java`, `P4b5ConsumerQaIT.java`; `app/mobile/test/reading_images/reading_p4b5_qa_probe_test.dart`; `tools/image-optimization/test_p4b5_consumer_qa.py`: guard/matris ve gerçek yerel zincir.
- [x] `be/docs/evidence/p4b5-consumer-measurement/verification.md`: koşu, gerçek tüketim/ekran/kaynak ve cleanup sonucu; başarısızlık başarı sayılmaz.

Given yerel kapılar ve onay, when tek koşu yürütüldüğünde, then gerçek kanıt veya
terminal hata üretilir; yeni hak yaratılmaz. Üç koşu-öncesi inceleme sonrası gerçek
adımı kök ajan yönetir; son BMad incelemesi gerçek kanıtı da kapsar.

## Spec değişiklik kaydı

2026-10-19: Kullanıcı A ile somut planı ve tek sınırlı denemeyi onayladı. Frozen
blok, companion ve sayısal öneri salt okunur. Frozen SHA:
`38cdd007a6bb503391b4f622da47c186df62d3a4363c69b68740d2d0ab1c5560`.

## Uygulama devri

Bu çalışma kök ajanın yürüttüğü BMad step-03 uygulamasıdır; yeni workflow/render
veya onay başlatılmaz. Story key yoktur; sprint eşitlemesi atlanır. Uygulama ajanı
yalnız yerel hazırlığı tamamlar. Gerçek QA kurulumu/başlangıcı, backend, giriş,
gerçek QA DB/SDK/ADB/credential işlemlerini kök ajana bırakır. İzole sentetik yerel
testler ve offline APK hazırlığı yapılabilir. Üç koşu-öncesi incelemeyi kök ajan
yönetir; bulgular kapanmadan gerçek deneme açılmaz. Ajan hazır kaynak/APK/classpath
bağları, test/matris/mutasyon kanıtı ve uygulanabilir komutlarla koşu-öncesi devir
verir. Son görev gerçek koşu ve cleanup sonucu gelene kadar açık kalır. Kök ajan
gerçek denemeden sonra son BMad incelemelerini ve görevle sınırlı commit'i yönetir.

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d/`.
`receipts/protected-before.private.json` izinli yolları ve korunan hash'leri;
`receipts/operational-tools.private.json` mevcut araç yollarını;
`receipts/human-approval-01.private.json` kullanıcı onayını taşır. Üzerlerine yazılmaz.
Başlangıçta 3290 repo, 72 eski run, 192 P4a5, 296 P4b1, 281 P4b2, 260 P4b3 ve
299 P4b4 dosyası korundu. İzinli değişiklik listesi koruma makbuzundadır; yeni güvenli
sonuçlar evidence prefix'ine eklenebilir, proposal değiştirilmez. `.memlog.md`, dirty
giriş ve local-lossless çalışmaları korunur. Credential'sız kaynak kapanışı izlenen
Flutter/Android/backend/tools dosyalarıyla sınırlıdır. Dirty giriş özel kopyada HEAD
sürümüyle alınır; gerçek config/keystore/token ve untracked lossless alınmaz.

Mevcut bağımlılıklar kullanılır; pub/SDK/Java/Maven/Gradle kurulumu veya indirmesi
yapılmaz. Python websocket için P4b4 makbuzlarındaki dar mevcut paket yolu kullanılır;
geniş site-packages yolu stdlib typing'i gölgelediği için kullanılmaz. TLS testi için
özel kaynakta gerekli `.venv/bin/python` mevcut interpreter'a bağlanabilir. Yeni tüm
log/makbuzlar sıralı CREATE_NEW0600, dizinler0700 olur. Önceki kanıtlar değiştirilmez.
Araç veya kapsam engeli kök ajana bildirilir; gerçek hak tüketilmez. Yerel test/build
hataları düzeltilebilir, ancak gerçek deneme için otomatik tekrar yoktur.

## Doğrulama

Onay incelemedeki gerekli credential'sız MAC_EK kaynak/test/build kopyasını ve tek
sayısal denemeyi kapsar. Yerel Python/Flutter/Java, analyzer, PostgreSQL/V37 ve
HTTP/modül kontrolleri; native/loopback→JSON→phase kabulü ve mutasyonlar geçsin.
Kaynak→APK/classpath, matrisin geçmiş test adları ve koruma/frozen/diff-check kanıtlansın.
Sonra tek onaylı koşu; ham kayıtlar MAC_EK'te, yalnız güvenli rapor repo'da kalsın.
Son üç BMad incelemesi ve görevle sınırlı yerel commit yapılır. Genel P4b/üretim ve
fiziksel kabul açık kalır. Model oturumdan devralınır.

## Tek deneme sonucu

2026-10-19: Üç koşu-öncesi inceleme kapandı. Onaylı tek emülatör denemesi `arm`
aşamasında `VM_REFUSED` ile terminal kapandı. Kurulum/ilk başlangıç kullanıldı;
backend/login/pencere/medya başlamadı. Sahipli QA süreci ve forward temizlendi;
aynı DB borcu39/44.742.970 kaldı. Son görev terminal hata raporunu üretmiştir;
gerçek mobil ölçüm ve genel P4b kabulü açık kalır. Yeni gerçek deneme yapılmaz.

Rapor: [Doğrulama](../../be/docs/evidence/p4b5-consumer-measurement/verification.md).
Bu sonuç son üç BMad incelemesine sunulur.

## Önerilen inceleme sırası

**Tek kullanımlık ölçüm ve kapılar**

- Aşamalar tek onaya bağlanır; hata denemeyi kalıcı kapatır.
  [p4b5_consumer_qa.py:811](../../tools/image-optimization/p4b5_consumer_qa.py#L811)

- Kaynak, APK ve üç inceleme aynı çalışma kimliğinde doğrulanır.
  [p4b5_consumer_qa.py:362](../../tools/image-optimization/p4b5_consumer_qa.py#L362)

- Driver kaydı mevcut sürede beklenir; reddedilmiş RPC tekrar edilmez.
  [p4b5_consumer_qa.py:170](../../tools/image-optimization/p4b5_consumer_qa.py#L170)

- Giriş kanıtı, DB ve yayın kontrollerinden sonra pencere açılır.
  [P4b5ConsumerQaSetup.java:265](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java#L265)

- Sıralı medya ve çağrı kotaları işlem öncesinde rezerve edilir.
  [P4b5ConsumerQaLedger.java:75](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedger.java#L75)

**Kaynak gözlemi ve kapanış**

- Lifecycle, epoch ve canlı raster gözlemi aynı snapshot içinde tutulur.
  [reading_consumer_qa_probe.dart:377](../../app/mobile/test_driver/reading_consumer_qa_probe.dart#L377)

- Dört paint ve bütün drain fazları aynı ham kaynak zincirinde sınanır.
  [p4b5_consumer_qa.py:250](../../tools/image-optimization/p4b5_consumer_qa.py#L250)

- Yalnız sahipliği doğrulanan süreçler ve eşlemeler temizlenir.
  [p4b5_consumer_qa.py:787](../../tools/image-optimization/p4b5_consumer_qa.py#L787)

**Testler ve kanıt**

- Geç kayıt ve kayıt yarışı özgün başlangıç süresi içinde sınanır.
  [test_p4b5_consumer_qa.py:429](../../tools/image-optimization/test_p4b5_consumer_qa.py#L429)

- Bozuk giriş makbuzları gerçek pencere geçişinde reddedilir.
  [P4b5ConsumerQaSetupTest.java:163](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetupTest.java#L163)

- Yerel native paint kanıtı gerçek #94 koşusundan ayrı tutulur.
  [reading_p4b5_qa_probe_test.dart:167](../../app/mobile/test/reading_images/reading_p4b5_qa_probe_test.dart#L167)

- Gerçek terminal hata, sıfır medya tüketimi ve eksik kabul açıkça kaydedilir.
  [verification.md:242](../../be/docs/evidence/p4b5-consumer-measurement/verification.md#L242)
