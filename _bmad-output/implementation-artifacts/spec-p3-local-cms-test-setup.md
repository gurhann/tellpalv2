---
title: 'P3 yerel CMS test hazırlığı'
type: 'chore'
created: '2026-10-17'
status: 'done'
baseline_commit: '2e8c62af9057df99efded22669252a35961aeab9'
approval: 'Kullanıcı 2026-10-17 tarihinde A ile planı ve belirtilen ilk bulut denemesini onayladı.'
review_loop_iteration: 0
context:
  - '{project-root}/be/docs/adr/ADR-0023-reading-publication-orchestration.md'
  - '{project-root}/be/docs/admin-api-rules.md'
---

<frozen-after-approval reason="kullanıcı onayından sonra kapsamı koru">

## Amaç

**Sorun:** Çalışan CMS ve backend, varsayılan kapalı P3 akışını gerçek görsellerle denemeye hazır değil. 91 mevcut içerik var; P3 tabloları boş.

**Yaklaşım:** Mevcut Firebase SDK ve uygulama API’leriyle tek sayfalı, Türkçe, ayrı bir sentetik DRAFT STORY hazırla. Açık yerel test ayarlarıyla backend’i çalıştır; mevcut Chrome oturumunda Yayınla → hazırlama → LIVE akışını doğrula.

## Sınırlar

**Her zaman:** Mevcut 91 içerik ve medya bağlarını koru. Ayarları `be/var/p3-local-cms/` altında Git dışında tut; izlenen `application-local.yml` dosyasını değiştirme. `local` profilini açıkça etkinleştir; mevcut yerel DB, JWT ve CORS ayarlarını koru. Kabul transaction dışında çalışsın; kimlikleri/kısmi hata kanıtını sakla. Sessiz tekrar çalıştırma yok.

**Onaya sunulan bulut kapsamı:** `tellpal-ee0dd.appspot.com` bucket’ında benzersiz `local/p3-cms-<UUID>` prefix’i altında tek 80×60 opak RGB8 PNG oluştur. Aynı görseli kapak/detail/tek sayfa için kullan. Kaynak, admitted kopya, retained source ve üç rol çıktısı dahil en fazla altı kalıcı nesne üret. Nesne başına 1 MB, toplam en fazla 6 MB. Ses yalnız LOCAL_STUB kaydıdır. Başarısız bulut işlemini otomatik yeniden başlatma.

**Ayrıca onay al:** Başka içerik, prefix veya bucket’a yazma; farklı geometri/limit; yeni fixture ya da başarısız çalışmanın kapsamını aşan bulut denemesi.

**Kapsam dışı:** Üretim etkinleştirme/kalite onayı, P4 erişimi, dağıtım, backfill, silme; yeni storage katmanı, SDK dışı retry/IPC, yeni admission REST endpoint’i ve CMS tasarım değişikliği.

## Girdi/çıktı ve hata durumları

| Durum | Beklenen davranış |
|---|---|
| Dry-run | Kimlik bilgisi okumadan, ağ veya DB yazısı yapmadan kapsamı göster. |
| Eksik ayar, uzak DB veya prod profil | Yazmadan dur; yalnız sonlu hata nedeni göster. |
| Kabul/seed yarıda kalır | Kanıtı ve nesneleri koru; mevcut içerikleri değiştirme; kendiliğinden tekrar yazma. |
| Kullanıcı deneme hikâyesinde Yayınla der | Gerçek scheduler/encoder ve kaydedilmiş çıktı doğrulamasıyla LIVE; eksik/bozuk çıktı başarısız. |

</frozen-after-approval>

## Kod haritası

- Başlangıç: `2e8c62af9057df99efded22669252a35961aeab9`; ilgisiz `.memlog.md` değişikliğini koru.
- `be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationProperties.java` — açık local profil ve üç rol gerekir.
- `be/src/main/java/com/tellpal/v2/asset/api/ReadingSourceAdmissionApi.java` — yeni kimliği yalnız demoya bağla.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/ReadingObjectStorage.java` — mevcut pinned/create-only SDK işlemleri.
- `be/src/main/java/com/tellpal/v2/content/application/{ContentManagementService,StoryPageManagementService}.java` — seed için mevcut komutları kullan.
- `be/src/test/java/com/tellpal/v2/asset/reading/ReadingFixtures.java` — yalnız sentetik profil: 40×30; kaynak/çıktı 1 MB; 100.000 piksel; parent 16 MB; heap/direct/process 64/8/256 MiB; disk 2,1 MB; kaynak/revizyon 5/30 saniye; concurrency/encoder 1; açık test growth kanıtı.
- `cms/src/features/contents/components/publication-actions.tsx` — kapalı akışta kart gizlenir; Chrome oturumu açık.

## İşler ve kabul

- [x] `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetStorageObjectPathBuilder.java` ve odaklı testi — mevcut tek segment davranışını korurken yalnız güvenli `local/test` alt prefix'lerini P2 sözleşmesiyle uyumlu kabul et; traversal/boş segment/backslash ve prod alt prefix'lerini reddet. Gerçek preflight ortak builder'ın onaylanan deneme alanını reddettiğini gösterdi.
- [x] `be/docs/adr/ADR-0023-reading-publication-orchestration.md` ve `be/docs/project-memory.md` — yalnız yerel deneme prefix uyumunu ve rehberini kaydet; üretim varsayılanını değiştirme.
- [x] `be/src/test/java/com/tellpal/v2/support/P3LocalCmsSetup.java` — açık çağrılan test aracı; varsayılan dry-run, mevcut API’ler, fixture/makbuz; ürün paketine ekleme.
- [x] `be/tools/p3_local_cms.py` — mevcut yerel ortamı sırları yazdırmadan yükle; açık komutla hazırlık/başlatma; var olan süreci tanımadan durdurma.
- [x] `be/src/test/java/com/tellpal/v2/support/P3LocalCmsSetupTest.java` ve `be/tools/test_p3_local_cms.py` — yan etkisiz dry-run, kapsam reddi ve yeniden çalıştırma korumasını doğrula.
- [x] `be/var/p3-local-cms/` — dış config, özel makbuz ve log; açık backend restart sonrası aynı CMS bağlantısı.
- [x] `be/docs/p3-local-cms-test.md` — kısa Türkçe başlatma ve deneme adımları; gerçek sonuç ve sınırlar.
- [x] `be/tools/p3_local_cms.py` ve helper makbuzu — setup/start DB kimliğini sabitle; eksik env dosyasını reddet; `#` içeren değerleri koru.
- [x] `.github/workflows/backend-verify.yml` — mevcut backend kontrolüne launcher Python test komutunu ekle; altyapı veya dağıtım değiştirme.
- [x] `be/src/test/java/com/tellpal/v2/support/P3LocalCmsSeedIntegrationTest.java` — gerçek helper admission→seed bağlarını ve aynı içerikte Publish→LIVE sonucunu izole disk/PostgreSQL ile doğrula.

**Kabul ölçütleri:**
- Yerel ortamda araç çalışınca mevcut kayıtlar korunur, yalnız demo oluşur.
- CMS’de demo yayınlanınca LIVE kartı görülür; revision/manifest digest durum API’si veya DB ile doğrulanır.
- Normal başlatmada overlay yoksa P3 kapalı kalır.
- Kaydedilmiş nesneler bağımsız doğrulanmadan demo LIVE olmaz.

## Spec değişiklik günlüğü

- Gerçek salt okunur preflight, legacy poller env önceliğini ve ortak path builder'ın tek segment kısıtını ortaya çıkardı. Çocuk süreç ayarlarını sabitleyen guard ve yalnız local/test alt prefix uyumu eklendi. Aynı onaylı UUID alanı, altı nesne sınırı, mevcut kayıtların korunması ve sıfır yeni storage katmanı şartı korunur.
- İlk kimlik hesaplamasındaki ASCII engeli, profil kanıtının teknik ASCII koduyla tutulmasını ve preflight'ta gerçek etkin profil kimliğinin hesaplanmasını gerektirdi. Kaynak kabulü, geometri ve limitler değişmedi. Son inceleme, DB/env korumaları ve kayıtlı test kapsamı için beş patch üretti; [inceleme kaydı](../../be/docs/evidence/p3-local-cms/review.md) bunları izler. Mevcut LIVE sonucu ve kaynak kimlikleri korunur.

## Doğrulama

Odaklı Java/Python testleri, yan etkisiz dry-run, mevcut içeriklerde önce/sonra bütünlük karşılaştırması. İzinli ilk denemede gerçek Publish/status/PostgreSQL kanıtı ve Chrome sonucu. Uygulama kodu değişirse ilgili tam doğrulama da gerekir.

## Yürütme durumu

Demo #93, admitted asset #11256 ile gerçek CMS Publish sonrasında generation 2 / LIVE oldu; job ilk attempt içinde COMPLETED. Üç çıktı 40×30 PNG; fresh pinned doğrulama başarılı. Chrome’da “Doğrulanmış okuma revizyonu benimsendi” gözlendi. Mevcut 91 kayıt LIVE sonrası da korundu. Tam verify 467 Surefire + 1 Failsafe; Son inceleme sonrası odaklı Java 14/14, P3 Python 16/16 ve tüm launcher Python 34/34 başarılı; gerçek helper seed entegrasyonu dahil. Offline hazırlık geçti. Kaynak/seed/admission tekrarlanmadı. [Gerçek kanıt](../../be/docs/evidence/p3-local-cms/verification.md) ve [rehber](../../be/docs/p3-local-cms-test.md) güncel; beş inceleme bulgusu düzeltildi ve doğrulandı. Backend/CMS çalışır durumda bırakıldı. Uzak CI, push veya dağıtım yapılmadı.

## Önerilen inceleme sırası

**Giriş ve kapsam**

- Tek kullanımlık hazırlık ve başlatma komutları onaylı sentetik denemeyi sınırlar.
  [p3_local_cms.py:242](../../be/tools/p3_local_cms.py#L242)

- Gerçek uygulama API’leri admission sonucunu yeni DRAFT hikâyeye bağlar.
  [P3LocalCmsSetup.java:158](../../be/src/test/java/com/tellpal/v2/support/P3LocalCmsSetup.java#L158)

**Hedef ve depolama korumaları**

- Makbuzdaki DB kimliği doğrulanmadan backend başlamaz; mevcut süreç durdurulmaz.
  [p3_local_cms.py:205](../../be/tools/p3_local_cms.py#L205)

- Env dosyası zorunludur; değerler shell çalıştırmadan yüklenir.
  [p3_local_cms.py:76](../../be/tools/p3_local_cms.py#L76)

- Yalnız güvenli local/test alt prefix’leri mevcut depolama yoluyla uyumlu hale gelir.
  [AssetStorageObjectPathBuilder.java:74](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetStorageObjectPathBuilder.java#L74)

**Kanıt ve kullanım**

- Gerçek encoder, kayıtlı çıktı doğrulaması ve korunmuş mevcut kayıtlar burada belgelenir.
  [verification.md:1](../../be/docs/evidence/p3-local-cms/verification.md#L1)

- Yerel başlatma, mevcut demo ve tekrar deneme adımları açıklanır.
  [p3-local-cms-test.md:1](../../be/docs/p3-local-cms-test.md#L1)

**Testler ve otomatik kontrol**

- Gerçek helper bağları aynı içerikte doğrulanmış LIVE sonucuna ulaşır.
  [P3LocalCmsSeedIntegrationTest.java:55](../../be/src/test/java/com/tellpal/v2/support/P3LocalCmsSeedIntegrationTest.java#L55)

- Hazırlık korumaları ve sentetik profil kimliği regresyonları doğrulanır.
  [P3LocalCmsSetupTest.java:13](../../be/src/test/java/com/tellpal/v2/support/P3LocalCmsSetupTest.java#L13)

- Eski tek segment davranışı ve reddedilecek alt yollar korunur.
  [AssetStorageObjectPathBuilderTest.java:9](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/storage/AssetStorageObjectPathBuilderTest.java#L9)

- Başlatıcı kapsamı, DB makbuzu ve env ayrıştırma korumaları sınanır.
  [test_p3_local_cms.py:13](../../be/tools/test_p3_local_cms.py#L13)

- Python testleri mevcut backend doğrulamasından önce otomatik çalışır.
  [backend-verify.yml:37](../../.github/workflows/backend-verify.yml#L37)
