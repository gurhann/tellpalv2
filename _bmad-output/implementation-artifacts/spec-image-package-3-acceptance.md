---
title: 'Paket 3: Tek yerel entegrasyon kabulü ve mobil epic devri'
type: chore
created: '2026-10-22'
status: done
baseline_commit: 5b69a1a0e43167371d12bfae1ef1968c91d683d0
review_loop_iteration: 0
context:
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
---

<frozen-after-approval reason="Kullanıcının onayladığı üç paketlik son kabul">

## Amaç ve sınırlar

Gerçek yerel backend'i normal Flutter uygulamasına bağla. Tek kabul kaydıyla görsel
altyapısı geliştirmesini kapat ve Story 1.3'e devret. P1'in 41 kaynak, metadata,
Linux ve yayın kanıtını; P2'nin tam testlerini yeniden kullan.

**Her zaman:** Yerel PostgreSQL/disk; gerçek Spring ve normal Flutter DI/LibraryApp,
HTTP, manifest, CachedNetworkImage ve engine decode. Yalnız storage leaf ve token
verifier sentetik olabilir. Gerçek/sentetik sınırı ve test sonucu açık yazılır.
Aktif iş bitmeden sahiplik bırakılmaz; başarısız/atlanmış test PASS değildir.
**Önce sor:** Ürün veya mimari kapsam değişikliği.
**Asla:** Canlı/Firebase/deploy/push, orijinal/eski kanıt yazımı, yeni codec/telefon
ölçümü; tam reader/audio/session/paywall; Story 1.2'yi done veya 1.3'ü uygulanmış saymak.

| Durum | Beklenen |
|---|---|
| Yeni yerel JPEG ve PNG yüklemesi | Gerçek hazırlama→READY→LIVE→normal HTTP→normal detail/sayfa çizimi; JPEG aynen, PNG çıktısı WebP |
| Gecikmeli aktarım ve ekrandan ayrılma | Byte/SHA/rendition/decode izlenebilir; çizim kalkar; gerçek cleanup sonrası permit boşalır |

</frozen-after-approval>

## Kod haritası

- `be/src/test/java/com/tellpal/v2/content/application/publication/ReadingImageConsumerIT.java` fixture/hazırlama örneği. Yeni tek loopback IT'de content API override yok. `AssetStorageConfiguration.java:34` bean `firebaseAssetStorageClient`, tip `AssetStorageClient`; yalnız leaf `@TestBean` ile `ReadingFixtures.DiskStorage` olur. Diğer normal bean'ler gerçek kalır. `PostgresIntegrationTestBase` gerçek PG15/Flyway sağlar; verifier sentetik UID kullanır.
- `app/mobile/lib/app/injection.dart:22`, `LibraryApp` ve generated DI gerçek opt-in ile kullanılır. `StoryDetailHero` capability'si testte elle kurulmaz. Normal detail rotası ve bağımsız page bileşeni gerçek repository/transport kullanır; `Source`/manifest fake'i yok. Test kapsamlı boş HttpOverrides ile Flutter test binding'in HTTP400 override'ı aşılır; ürün seam'i eklenmez.
- Java IT açık localhost random port/config ile tek bounded Flutter child başlatabilir; süre/exit ve descendant cleanup zorunlu. Credential yalnız environment/bellek, kaynak/ham log Git dışında. Seçili zorunlu kabulde Flutter yokluğu başarısızlıktır.
- P1/P2 README ve JSON kanıtları salt okunur. Kabul kaydı bunlara bağlanır; eski fingerprint/sonuçlar yeni koşu gibi etiketlenmez.
- `_bmad-output/planning-artifacts/post-story-1-2-image-optimization.md`, `epics-mobile.md` ve onaylı sprint-change proposal: yalnız yeni Türkçe durum/devir. Protected88 ve mobil tracker yazılmaz; önceden kirli ana plan yerine yeni kapanış kaydına bağlantı bu temiz plan belgelerinde tutulur.

## İşler ve kabul

- [x] Tek gerçek Spring→normal LibraryApp/detail ve sayfa widget kabulünü ekle/çalıştır. Sayısal test sınırları açık, default gate kapalı; production/Firebase yok.
- [x] JPEG/WebP gerçek MIME/SHA/byte, kullanılan rendition, decode ve gerçek bırakmayı kaydet. İki frozen matris satırı çalışan testle eşleşsin.
- [x] `be/docs/evidence/image-infrastructure-acceptance/` altında kısa Türkçe tek sonuç ve özet oluştur; K3.1–3.5'i P1/P2/yeni kabul kanıtına bağla.
- [x] Mobil epic'e devir bağlantılarını güncelle. 1.1 done/1.2 review/1.3 backlog korunur. Story 1.3'e typed görsel API/bileşen, açık audio/start sözleşmesi ve üretim işleri sahipli devredilir.

**Kabul:** Normal app opt-in kaldırılırsa test başarısız olur. API ve Flutter fixture yanıtlarıyla değiştirilemez. Kapanış yalnız geliştirme kabulüdür; üretim arşivi optimize edilmiş sayılmaz.

## Doğrulama

Yeni loopback IT ve Flutter testi zorunlu seçilerek geçer. Sadece test/belge değiştiyse P2'nin 603 çalışan backend ve 654 tam/son hedefli Flutter+analyze kanıtı kullanılır; yeni test analyze edilir. Codec/41/telefon/tam verify tekrar yok. Yeni üretim kodu yaması gerekirse değişen alanın anlamlı regresyonu çalışır.

Çalışma ayarları: fake-client=true, prefix=test, test-engine=true, processing poller=false, publication poll-delay=3600000, auth stub=false/check-revoked=true, Library allowlist=synthetic-consumer, consumer enabled=true ve benzersiz run-id. Consumer encoded/pixel/memory/deadline/lease/total-operation/total-byte sınırları ve publication profilleri açık verilir. Docker/native araçlar P2 run/baseline işaretçilerinden çözülür; yeni özel P3 run dizini kullanılır. SDK mutlak storage deadline ve AMD64 yeterliliği üretime özgü açık işler olarak korunur.

Koruma kaydı `/private/tmp/tellpal-package3-baseline.json`; yeni run işaretçisi `/private/tmp/tellpal-package3-run-path`. P2 review V2 bu gerçek normal root testiyle kapanır; P2 kanıt dosyaları değişmez, kapanış buradan bağlanır. Korunan 88 dosyanın yedeği `/private/tmp/tellpal-package2-protected-worktree`. Docker `DOCKER_HOST=unix:///Users/gurhankucuk/.colima/tellpal-image-local/docker.sock`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`; native araçlar `/private/tmp/tellpal-package1-dependencies-path` ve `/private/tmp/tellpal-package1-run-path` üzerinden çözülür.

## Gerçek kabul sonucu

Tek loopback kabulü `attempt-07` ile geçti: Maven 1/0 hata/0 skip, iç Flutter 1/1 ve dört gerçek engine çizimi. İki frozen satır aynı çalışmış testte JPEG/WebP detail/sayfa ve gecikmeli istemci iptali olarak doğrulandı. Analyzer temiz. [Tek kapanış kaydı](../../be/docs/evidence/image-infrastructure-acceptance/README.md) K3.1–K3.5 ve Story 1.3 devrini bağlar.

Son incelemenin üç test-harness yaması aynı tek kabulde tekrar doğrulandı. Yeni RUN alt dizini, sunucu beklemesinin kontrollü bırakılması ve zorunlu child cleanup uygulandı. Genel süreç ağacı garantisi bu testten çıkarılmaz. [İnceleme sonucu](../../be/docs/evidence/image-infrastructure-acceptance/review-triage.json) son kök denetimini içerir.

## Önerilen inceleme sırası

**Kabul ve devir**

- Tek sonuç, yeniden kullanılan kanıt ve Story 1.3 devri buradan okunur.
  [README.md:1](../../be/docs/evidence/image-infrastructure-acceptance/README.md#L1)

**Gerçek backend ve uygulama**

- Yalnız iki sentetik leaf ile gerçek hazırlama ve normal HTTP kurulur.
  [ReadingImageLibraryLoopbackIT.java:85](../../be/src/test/java/com/tellpal/v2/content/application/publication/ReadingImageLibraryLoopbackIT.java#L85)

- Gerçek root ve rotalar JPEG/WebP çizimini çalıştırır.
  [reading_image_library_loopback_test.dart:64](../../app/mobile/test/reading_images/reading_image_library_loopback_test.dart#L64)

**Sahiplik ve koşu yalıtımı**

- Release dosyası hatası zorunlu süreç temizliğini atlatmaz.
  [ReadingImageLibraryLoopbackIT.java:162](../../be/src/test/java/com/tellpal/v2/content/application/publication/ReadingImageLibraryLoopbackIT.java#L162)

- İstemci bırakıldığında sunucunun hâlâ beklediği kaydedilir.
  [reading_image_library_loopback_test.dart:218](../../app/mobile/test/reading_images/reading_image_library_loopback_test.dart#L218)

**Epic ve takip**

- Görsel bağımlılığı kapanır; audio/start ve üretim sahipliği açık kalır.
  [epics-mobile.md:784](../planning-artifacts/epics-mobile.md#L784)

- Çalışmış test, gerçek SHA ve önceki başarısız koşular ayrı tutulur.
  [summary.json:2](../../be/docs/evidence/image-infrastructure-acceptance/summary.json#L2)
