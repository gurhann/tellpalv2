---
title: 'P4a5: mevcut #94 çıktılarıyla QA rota provası'
type: 'feature'
created: '2026-10-18'
status: 'done'
baseline_commit: '571a79b2b07941caffa0a3541f21d347138f82a5'
approval: '2026-10-18: A — plan ve tek gerçek deneme onaylandı'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4a5-consumer-smoke-investigation.md'
---

<frozen-after-approval reason="Somut yerel araç ve tek gerçek çalışma önerisi onay bekliyor">

## Amaç

**Sorun:** P4a4 QA rotaları yerel testten geçti; mevcut #94 ile gerçek teslimatları sınanmadı.

**Yaklaşım:** Ayrı test araçları, gerçek CTA/router üzerinden detail→sayfa1→sayfa2→detail akışında dört rendition/paint ve kapanış kaydetsin. Eski araçlar ve kanıtlar korunsun.

## Sınırlar

**Daima:** Kanonik UID, aynı #94/tr retained LIVE/manifest/profil, mevcut V37 runId ve tüketim; gerçek slot/DPR/fit, no-store, tek Cubit/anchor. Yeni özel kaynak/build/log/ekranlar MAC_EK’te; credential/token kopyası yok. Normal Library/ses/cache ve 43 eski kirli dosya korunur.

**Onay kapsamı:** Aşağıdaki JSON SHA’sıyla tek öneri; yerel kaynak kopyası/araç/test/debug APK hazırlığı ve inceleme sonrası tek gerçek emülatör denemesi. Kapılar sonrası tek 180 saniye; gerekirse en çok bir ek login, kümülatif giriş tavanı 3. Dört pinned read/13.170.884 byte; manifest1/status≤18, HTTP≤23; login dahil ek verifier≤28. Diğer eski toplam tavanlar değişmez. Başarısızlıkta yeniden başlatma yok.

**Önce sor:** Pencere/giriş/okuma artışı, farklı kimlik/çıktı/cihaz, gerekli kurulum, ürün kodu veya erişim politikasında genişleme.

**Yapma:** Encode/upload/create/observe/admission/publish/reconcile/worker, backfill/silme/dağıtım; sayaç reseti veya yeni V37 runId. Sahte LIVE, güncel encoder ile eski fingerprint’i yeniden adlandırma, canonical/original fallback, gizli DPR azaltma, yeni transport/storage process/IPC/retry yok. PENDING/poller/Library değişmez. P4b/P0/fiziksel kabul kapanmaz.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Eksik onay/kapalı/pending veya yanlış ledger/kimlik | SDK/medya başlamadan ret; eski dosya değişmez. |
| Geçerli dört UI geçişi | Tek manifest/anchor; rol/page sırası, iki eksen ve byte/decode/paint eşleşir. |
| Shortage, bozuk çıktı veya LIVE/erişim değişimi | Açık başarısız kanıt; fallback veya yeni anchor yok. |
| Ek istek/expired/tekrar/clock rollback | Çağrı öncesi ret; harcanan pay geri verilmez. |
| Background/return/UI close | Görsel temizlenir, ek I/O yok; native drain ve yalnız owned süreç/eşlemeler kapanır. |

</frozen-after-approval>

## Kod haritası

Ayrıntılı dosya/simge ve kapı sırası context belgesindedir. Üretim kodu değişmez.

- `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart:196`: gerçek router/HTTP/codec örneği; mevcut QA app/Cubit/owned raster kullanılır.
- `be/src/test/java/com/tellpal/v2/support/P4LocalComparisonSetup.java:165`: media-serving kompozisyon örneği; seed/execute yolu ve eski ledger çağrılmaz.
- `be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingPublicationReadyService.java:145`: retained kontrolü; güncel encoder fingerprint’iyle yeniden üretim gerektirmez.
- Sayısal sözleşme: `be/docs/evidence/p4-consumer-qa/bounded-run-proposal-2026-10-18.json`; SHA `079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6`. Eski profile kaynak sınırları korunur; üretim kararı değildir.

## Görevler ve kabul

- [x] `be/src/test/java/com/tellpal/v2/support/P4ConsumerQa{Ledger,Storage,Setup}.java` ve ilgili yeni testler: yalnız mevcut SDK teslimatı, kalıcı read/auth rezervasyonu ve kapılar; eski araçlar değişmez.
- [x] `app/mobile/test_driver/reading_consumer_qa_{entry,probe}.dart` ve `test/reading_images/reading_consumer_qa_probe_test.dart`: ayrı debug girişinde gerçek UI akışı, güvenli runtime config ve paint/sahiplik kanıtı.
- [x] `tools/image-optimization/p4_consumer_qa.py`, `test_p4_consumer_qa.py`: offline build, bağlama, sınırlı UI sürüşü, dört teslimat, hata ve cleanup.
- [x] `be/docs/evidence/p4-consumer-qa/`: mevcut ortam/kimlik/sayaçları yeniden denetle; yerel kapılar ve üç inceleme sonrası onaylı tek gerçek koşuyu çalıştır; hassas verisiz sonuç ve sınırları kaydet.

**Kabul:** Given onaylı öneri ve geçen yerel kapılar, when tek gerçek deneme çalışır, then dört UI paint’i pinned byte/kimlik/gerçek geometriyle kanıtlanır ve sahiplik kapanır. Ortam/süre/kimlik hatasında sonuç başarısız/açık kalır; yeniden deneme veya tamamlanmış kabul yoktur. Genel tüketici, fiziksel kalite ve peak kaynak sonucu çıkarılmaz.

## Spec değişiklik kaydı

## Doğrulama ve kanıt sahipliği

Kanonik kapsam yukarıdaki frozen blok ve sayısal öneridir. Özel kök
`/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb` altında
kaynak kopyası, offline build/test kayıtları ve gerçek koşu kanıtları ayrı tutulur.
Credential/token repo'ya veya test kopyasına alınmadı. Eski hata, ledger, reservation,
rapor ve hash bağları korunur. `protected-before.private.json` 3241 repo dosyasını,
72 eski run dosyasını ve önceki planları; kurtarma mührü 94 özel dosyayı korur.

Model oturumdan devralındı; override yapılmadı. Yerel hazırlık ve hedefli incelemelerden
sonra gerçek tek çalışmayı root yürüttü. Gerçek sonuç, sentetik test stdout'u ile
ikame edilmedi. Normal BMad son inceleme tamamlandı; dar görev yerel kayıt aşamasına alındı.
Bu spec bir Epic story değildir; `story_key` yoktur.

## 2026-10-18 hazırlık ve hata tarihçesi

- **İlk yerel devir:** İki ayrı offline debug APK, 59 Python/12 Java testi ve üç ön
  inceleme tamamlandı. Önceki Flutter 234/234, analyzer ve gerçek PostgreSQL/Tomcat/modül
  30/30 kanıtı korundu. O aşamada web API yapılandırma dosyasının yolu eksikti; gerçek
  koşu henüz başlamamıştı. [Ön çalışma raporu](../../be/docs/evidence/p4-consumer-qa/pre-run-status-2026-10-18.md)
  bu tarihsel durumu tutar.
- **Yapılandırma ve ilk QA başlangıcı:** Mevcut Hosting config'i bir salt okunur GET
  ile hazırlandı. İlk başlangıç geç PID nedeniyle `QA_PID_UNAVAILABLE` ile reddedildi;
  owned süreç temizlendi. Backend/login/pencere açılmadı. Sonraki 68 Python testi ve
  hedefli incelemelerden sonra kullanıcı tek ek bekleme başlangıcını onayladı.
  [İlk başlangıç raporu](../../be/docs/evidence/p4-consumer-qa/runtime-preparation-2026-10-18.md)
  değişmeden saklanır.
- **İlk backend reddi:** Ek bekleme VM kapısı geçti. Backend komutunda açık yerel DB
  kullanıcısı eksik olduğu için var olmayan `tellpal` rolüne bağlanıp durdu. Mevcut
  `gurhankucuk` hesabıyla salt okunur klon/borç doğrulandı; yeni rol oluşturulmadı.
  İlk backend ledger'ı yeni harcama olmadan kapandı. Login/medya/pencere sıfır kaldı;
  QA/probe ve yeni eşleme temizlendi. [Ret raporu](../../be/docs/evidence/p4-consumer-qa/local-backend-refusal-2026-10-18.md)
  bu başarısızlığı tutar.
- **Onaylı dar kurtarma:** Kullanıcı [yerel kurtarma planını](image-optimization/p4a5-local-runtime-recovery.md)
  A ile onayladı. Aynı APK/emülatör için yalnız bir ek QA ve backend hazırlığı verildi.
  Açık DB kullanıcı girdisi, ayrı exclusive rezervasyonlar, `backend-02`/`connection-03`
  bağları ve hedefli inceleme düzeltmeleri 85 Python/19 Java testinden geçti. Önceki
  giriş/pencere/byte tavanları yükseltilmedi. Bu kapılardan sonra aşağıdaki tek gerçek
  koşu yürüdü; önceki bekleyen girdi ve uygulanmamış kurtarma durumları güncel değildir.

## 2026-10-18 güncel gerçek sonuç ve son offline patch

**Dar P4a5 kabulü geçti.** Detail → sayfa 1 → sayfa 2 → detail akışında aynı retained
LIVE/manifest/profil ile dört gerçek UI paint'i, dört read ve **13.170.884 byte**
doğrulandı. Tek manifest, bir status ve altı mobil HTTP çağrısı yapıldı. Login dahil
ek verifier rezervasyonu 11, ek login 1 oldu. Backend drain/exit ve ayrı son kontrolde
QA/backend PID yokluğu ile owned eşlemelerin temizliği doğrulandı. Eski dosyalar korundu.

[Gerçek sonuç ve sınırlar](../../be/docs/evidence/p4-consumer-qa/verification-2026-10-18.md)
`implementation-source-identity-runtime-recovery-03.private.json` kaynak kimliğine aittir.
Gerçek koşudan sonra yalnız kapanış gözlemi, credentialGate test kapsamı ve bu tarihçe
onarımı offline uygulandı. İlk son patch kapısı **91 Python/20 Java** testiydi; `04` kaynak kimliği
bu cleanup/test/belge patch'ini ayırır. Yeni gerçek başlangıç, login veya ölçüm yapılmadı;
`final-runtime-verification-01.private.json` makbuzu değişmedi.

[Son offline patch raporu](../../be/docs/evidence/p4-consumer-qa/local-runtime-recovery-final-patch-2026-10-18.md)
yeni log, kaynak kimliği ve koruma audit'ini tutar. Mobil/IT kaynakları değişmediği için
önceki 234/30 kanıtı korunur; yeni APK, Colima veya kurulum yoktur. Bu devir sırasında
frontmatter `in-review` idi; son inceleme tamamlanınca `done` olarak güncellendi. Bu patch yeni bir gerçek koşu hakkı vermez. Fiziksel kalite, genel mobil geçişi,
üretim profili ve P4b dar P4a5 kabulünün dışındadır.

Son tek kapanış yarışı offline giderildi. [Kapanış raporu](../../be/docs/evidence/p4-consumer-qa/local-runtime-recovery-close-race-2026-10-18.md) **93 Python/20 Java** kapısını ve `05` kaynak kimliğini kaydeder. Yeni gerçek koşu yapılmadı.

## Önerilen inceleme sırası

**Giriş ve yerel kapılar**

- Tek başlangıç, DB hesabı ve retained LIVE sırasını buradan izle.
  [P4ConsumerQaSetup.java:42](../../be/src/test/java/com/tellpal/v2/support/P4ConsumerQaSetup.java#L42)

- DB kullanıcı önceliği ürün varsayılanına düşmeyi engeller.
  [P4ConsumerQaSetup.java:229](../../be/src/test/java/com/tellpal/v2/support/P4ConsumerQaSetup.java#L229)

**Tüketim ve kimlik**

- Pinned read ve giriş borcu aynı sonlu ledger içinde korunur.
  [P4ConsumerQaLedger.java:100](../../be/src/test/java/com/tellpal/v2/support/P4ConsumerQaLedger.java#L100)

**Gerçek UI ve sahiplik**

- Ayrı debug girişi normal uygulama akışını korur.
  [reading_consumer_qa_entry.dart:8](../../app/mobile/test_driver/reading_consumer_qa_entry.dart#L8)

- Rol, epoch, byte ve owned kaynak gözlemleri birlikte toplanır.
  [reading_consumer_qa_probe.dart:229](../../app/mobile/test_driver/reading_consumer_qa_probe.dart#L229)

**Sürüş ve kapanış**

- Dört gerçek CTA geçişi tek pencere içinde doğrulanır.
  [p4_consumer_qa.py:732](../../tools/image-optimization/p4_consumer_qa.py#L732)

- Tek owned stop sonrası bounded kimlik ve yokluk gözlenir.
  [p4_consumer_qa.py:338](../../tools/image-optimization/p4_consumer_qa.py#L338)

**Kanıt ve testler**

- Gerçek sonuçları, sayaçları ve açık kapsam sınırlarını karşılaştır.
  [verification-2026-10-18.md:1](../../be/docs/evidence/p4-consumer-qa/verification-2026-10-18.md#L1)

- DB başlangıç ortamı ve credential kapısı gerçek çağrı sınırında sınanır.
  [P4ConsumerQaSetupTest.java:14](../../be/src/test/java/com/tellpal/v2/support/P4ConsumerQaSetupTest.java#L14)
