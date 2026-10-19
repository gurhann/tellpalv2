---
title: 'P4b5: Düzeltmelerle tek yeni emülatör ölçümü'
type: 'feature'
created: '2026-10-19'
status: 'done'
baseline_commit: 'e11df87e9a7e03c1c3804a90538a8886dfae3bf1'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/architecture.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b5-retry-measurement-investigation.md'
---

<frozen-after-approval reason="Kullanıcıya sunulan tek tekrar planı">

## Amaç

P4b5 düzeltmelerini mevcut #94 PNG'lerinde tek yeni ölçümle doğrula.
Eski terminal kapalı kalır; bu görev P4b6 veya genel P4b kapanışı değildir.

## Sınırlar

**Daima:** Yeni kimlik/kök, aynı DB/borç/tavan; önceki kayıtlar korunsun.
120sn; 1 manifest/12 status/4 medya,17 HTTP/13.170.884 byte; login dahil22
verifier, ek25 delivery. Kurulum/ilk başlangıç/backend/giriş birer üst sınır.
Haklar çağrı öncesi rezerve edilir; hata terminaldir.

**Önce sor:** İkinci tekrar, yeni üretim/cihaz, erişim/cache veya üretim profili.

**Yapma:** Eski claim/onay açma, DB/borç reset, yeni storage/IPC/retry,
normal runtime/ses/cache değişikliği, push/dağıtım.

## Sınır durumları

| Girdi | Beklenen |
|---|---|
| Kapalı/karışık scope veya hash drift | Eski köke yazmadan ret; auth/medya açılmaz. |
| Driver geç kayıt / PID-isolate değişimi | Aynı30sn kapısı; ilk RPC ret tekrar edilmez. |
| Eksik giriş kanıtı | Window/login import yok. |
| Dört paint ve drain | Aynı lifetime/epoch; attempts farkı1/2/3/4, sonra4. |
| Hata/süre/kota | Sahipli cleanup; başarı veya yeni hak üretilmez. |

</frozen-after-approval>

## Kod haritası

[İnceleme](image-optimization/p4b5-retry-measurement-investigation.md): dosya/simge,
ikili scope, argv, önceki kanıt, test ve yürütme haritası.
[Sayısal öneri](../../be/docs/evidence/p4b5-retry-measurement/bounded-run-proposal-2026-10-19.json)
SHA: `06810424cfde11b22151aa4c719de448f5e4a1fa02d767c11765cac9627a2606`.

## Görevler ve kabul

- [x] `tools/image-optimization/p4b5_qa_scope.py`, `p4b5_consumer_qa.py`: iki sabit scope; eski default kapalı, yeni açık seçilir.
- [x] `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScope.java`, `P4b5ConsumerQaSetup.java`, `P4b5ConsumerQaLedger.java`: scope/proof/argv bağları; aynı borç.
- [x] `app/mobile/test_driver/reading_p4b5_retry_qa_entry.dart`, `reading_consumer_qa_probe.dart`: yeni compile-bound kapsam; eski entry korunur.
- [x] İncelemedeki `test_p4b5_qa_scope.py`, `test_p4b5_consumer_qa.py`, `P4b5ConsumerQaScopeTest.java`, mevcut P4b5 Java/Dart testleri: matris/native JSON/mutasyonlar.
- [x] `be/docs/evidence/p4b5-retry-measurement/verification.md`: tek gerçek terminal sonuç, tüketim ve cleanup raporu. Son inceleme/commit sonraki workflow adımıdır.

Given yerel kapılar ve üç pre-run inceleme, when tek onaylı koşu yürütülür,
then dört doğrulanmış paint veya terminal hata kanıtı kaydedilir; genel kabul açılmaz.

## Spec değişiklik kaydı

2026-10-19: Kullanıcı plan hazırlığını onayladı; gerçek deneme henüz onaylı değil.

## Doğrulama

İncelemedeki özel kaynak/test/build kopyası ve mevcut araçlar kullanılır.
Flutter/analyzer, Python/native interop, Java/gerçek sentetik PG ve kapsam mutasyonları
geçer. Yeni source→APK/installed SHA ve CP/Java/Python→run bağları üç pre-run
incelemeden geçmeden gerçek adım açılmaz. Koşuyu kök ajan yönetir; gerçek sonuç
son üç BMad incelemesine girer. Story key yoktur; sprint eşitlemesi atlanır.

## Uygulama devri

2026-10-19: Kullanıcı `A` ile somut planı, gerekli credential'sız MAC_EK
kopyasını ve kapılar sonrası tek sınırlı gerçek denemeyi onayladı. Frozen blok,
companion ve sayısal öneri salt okunurdur. Frozen SHA: `a4dbf42e11789c9bbb3327ffb7eec544520f556e9cb74970ef37dec617687af5`.

Bu kök ajanın BMad step-03 uygulamasıdır; aynı snapshot kullanılır, yeni
workflow/render/onay açılmaz. Story key yoktur. Uygulama ajanı kaynak, test,
offline APK ve izole sentetik PostgreSQL hazırlığını tamamlar. Gerçek QA DB/SDK/
auth/ADB/kurulum/başlangıç/backend/login/pencere aşamalarını çalıştırmaz.
Kök ajan üç bağımsız koşu öncesi incelemeyi kapatır, ardından tek gerçek denemeyi
yönetir. Sonuç ve cleanup gelene kadar beşinci görev açık kalır. Son BMad
incelemeleri, matris denetimi ve görevle sınırlı commit kök ajandadır.

Onaylı özel kök `/Volumes/MAC_EK/tellpal-qa-private/p4b5-retry-measurement-20261019-e11df87/`
altındadır. `receipts/protected-before.private.json` izinli yolları/korunan hash'leri,
`receipts/operational-tools.private.json` mevcut araçları,
`receipts/human-approval-01.private.json` bu onayı taşır. Eski makbuzlara yazılmaz.
Değişiklik bu izinli yollarla sınırlıdır; ek test helper'ı gerekiyorsa kök ajana bildir.
Dirty `main_image_experiment.dart` özel kopyada HEAD sürümünden alınır; untracked
local-lossless, gerçek config/keystore/token kaynak kopyasına girmez. Mevcut SDK,
Java, Gradle/Maven/pub cache ve Python interpreter kullanılır; indirme/kurulum yoktur.
Gerekli websocket için yalnız dar mevcut paket bağı kullanılır; geniş site-packages
stdlib'i gölgeleyemez. Local test/build hataları düzeltilebilir; gerçek deneme otomatik
tekrar edilmez. Eski source/CP/APK/actual/final-only klasörleri ve `.memlog.md` korunur.

Koşu öncesi devir; seçili source/APK/CP/Java/Python/prior-proof/run bağlarını,
tam komutları, geçen test/matris/mutasyon loglarını ve gerçek aşamaların hiç
çağrılmadığını açık verir. Ham sırlar veya VM adresi repo/mesaj/loga yazılmaz.

### Credential sırasının açıklaması

2026-10-19: Companion'daki 4–5. yürütme adımları esas alınır. Ön-Spring/SDK
DB/proof/CP kontrolünden sonra mevcut backend, SDK credential dosyasını yerel
hazırlıkta okur. Giriş aracının credential okuması ise local-ready ve giriş
rezervasyonundan sonra yapılır. Companion'daki genel “login rezervasyonundan
sonra okunur” ifadesi bu giriş aracına uygulanır. Mevcut backend başlatma sırası,
gerçek çağrı sınırları ve üretim kodu değişmez. Son rapor bu ayrımı açıklar.

## Uygulama sonucu

Üç pre-run inceleme, kök yürütücünün iki cleanup açığı kapandıktan sonra PASS
verdi. Tek gerçek deneme `arm / VM_DEADLINE` ile terminal oldu. Kurulum1 ve
ilk başlangıç1 tüketildi; backend/login/pencere/HTTP/medya0. Owned QA/forward
temizlendi, DB39/44.742.970 kaldı. Tekrar yapılmadı. İç RPC/zaman aşımı nedeni
ham kayıttan ayrılamadı; ölçüm ve genel P4b kabulü açık. Ayrıntılar doğrulama
raporundadır. Frozen matrisin geçen test eşleşmeleri aynı rapora işlendi.

## Son inceleme izi

Blind ve edge katmanları bulgu bildirmedi. Verification'ın iki `patch` bulgusu,
yeni kapsamın gerçek gate bağlantısı ve terminal prior-proof tüketicisi için
negatif test eklenerek kapandı. Final-only Python150/skip0 ve iki assertion
mutasyonu geçti; actual kaynak/CP/APK/28kanıt ve bytecode korunmuştur. Üç katmanın
kapanışında açık bulgu0. Bu test ekleri gerçek denemede çalıştırılmış sayılmaz.
İnsan kararı gerektiren kapsam değişikliği veya deferred bulgu yoktur.

## Önerilen inceleme sırası

**Ölçüme giriş**

- Bilinmeyen ve kapalı kapsamlar yan etkilerden önce reddedilir.
  [p4b5_consumer_qa.py:903](../../tools/image-optimization/p4b5_consumer_qa.py#L903)

- Önceki kanıt ve Python bağı incelenmiş run üzerinden doğrulanır.
  [p4b5_consumer_qa.py:470](../../tools/image-optimization/p4b5_consumer_qa.py#L470)

**Kimlik ve kapanış**

- Classpath, Java ve APK bağları Spring ve SDK hazırlığından önce denetlenir.
  [P4b5ConsumerQaSetup.java:187](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java#L187)

- Yeni ledger önceki borcu ve proof bağını korur.
  [P4b5ConsumerQaLedger.java:39](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedger.java#L39)

**Mobil bağ**

- Ayrı debug entry yalnız yeni derleme kapsamını seçer.
  [reading_p4b5_retry_qa_entry.dart:8](../../app/mobile/test_driver/reading_p4b5_retry_qa_entry.dart#L8)

**Doğrulama**

- Gerçek kapının bozuk makbuzda kurulumdan önce durduğu sınanır.
  [test_p4b5_qa_scope.py:73](../../tools/image-optimization/test_p4b5_qa_scope.py#L73)

- Terminal tüketicisi eksik veya yanlış prior-proof satırını reddeder.
  [test_p4b5_consumer_qa.py:329](../../tools/image-optimization/test_p4b5_consumer_qa.py#L329)

- Başarısız gerçek ölçüm, sıfır medya tüketimi ve açık kabul sınırları kaydedilir.
  [verification.md:157](../../be/docs/evidence/p4b5-retry-measurement/verification.md#L157)
