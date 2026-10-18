---
title: 'P4a3: tüketici görsel alanının ortak ölçümü'
type: 'refactor'
created: '2026-10-18'
status: 'done'
baseline_commit: '46fec42fab212483882ced989ce2a68cc418cef1'
approval: '2026-10-18: A — Planı uygula'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/architecture.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/performance.md'
  - '{project-root}/be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
---

<frozen-after-approval reason="Somut P4a3 planı onay bekliyor">

## Amaç

**Sorun:** Seçici hazırdır. QA detail oranını kopyalar; normal reader iki farklı ölçüm yoluna sahiptir.

**Yaklaşım:** Detail, reader ve QA final alanını ortak presentation bileşeninden ölç. Slot/DPR/fit mevcut seçiciye ulaşsın. Normal P3 teslimatı açılmaz.

## Sınırlar

**Daima:** Detail oranı/cover, reader alanı/contain, kontroller, padding, blur, semantics ve scroll korunur. Domain saf Dart kalır. Türkçe çıktı; kirli dosyalar ve eski kanıtlar korunur.

**Önce sor:** Medya/erişim/cache/session politikası, profil/kaynak sınırı, kurulum veya cihaz/bulut çalıştırması gerekirse.

**Yapma:** Selector/facade veya transport/IPC/retry katmanı ekleme. Provider/cache/decode/prefetch, ses/grant/navigation/admission, LRU ve hero tavanlarını değiştirme. Original fallback yok. iPhone/P0/P4b/üretim kabulü verilmez.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Detail | Gerçek slot/DPR/cover; mevcut oran. |
| Reader'ın iki dalı | Ortak ölçüm; mevcut observeLayout ve contain. |
| Pencere/DPR/metin ölçeği | Yeni yerleşim ölçülür; eski geometri kullanılmaz. |
| QA kol değişimi | Slot sabit; mevcut iki eksenli seçim. |
| Geçersiz ihtiyaç/yetersiz çıktı | Mevcut tipli ret; fallback yok. |

</frozen-after-approval>

## Kod haritası

Mobil yollar `app/mobile/` altındadır.

- `lib/features/story_detail/presentation/view/story_detail_view.dart:158`: `_HeroArea`; mevcut oran/dekorasyon.
- `lib/features/story_detail/presentation/widgets/story_detail_hero.dart:17`: provider/decode korunur; fit aktarılır.
- `lib/story_player_page.dart:347`: yalnız `_buildReader`; `lib/story_reader_images.dart:5` sözleşmesi korunur.
- `lib/features/reading_images/presentation/view/reading_image_comparison_view.dart:91`: kopya oran/ölçüm; mevcut Cubit seçiciyi çağırır.
- `lib/features/reading_images/domain/use_case/select_reading_image_rendition.dart:44`: hazır saf seçici; yeniden yazılmaz.

## Görevler ve kabul

- [x] `lib/core/presentation/widgets/reading_image_viewport.dart`: ortak ölçüm ve mevcut detail oranı; yalnız Flutter presentation tipleri. Feature importu/seçim matematiği yok.
- [x] Kod haritasındaki dört UI dosyası: bileşeni bağla; reader'ın iki dalı aynı noktadan ölçülsün. Mevcut `StoryReaderGeometry` adapter'ı sürer. QA, BoxFit'i kendi domain fit'ine çevirir.
- [x] `test/reading_image_viewport_test.dart`, `test/story_detail/story_detail_view_test.dart`, `test/reading_images/reading_image_comparison_view_test.dart`, `test/image_experiment/reader_resolution_test.dart`: matris, gerçek rect/ölçüm eşitliği ve medya sırası. Mevcut seçici fixture/oracle kullanılır; formül kopyalanmaz.
- [x] Bu spec'e geçen testleri, kaynak kimliğini ve MAC_EK log yollarını kaydet.

**Kabul:**
- Given normal ekranlar, when yerleşirler, then ölçüm gerçek viewport rect/DPR/fit ile eşleşir; provider ve kontroller korunur.
- Given QA fixture, when ölçüm seçiciye verilir, then doğru aday veya shortage döner; geometri modu ağ açmaz.
- Given farklı dış kontroller, when alanlar kıyaslanır, then ortak ölçümün eşit ekran yüksekliği kanıtlamadığı belirtilir.

## Spec değişiklik kaydı

## Doğrulama

Mevcut private snapshot/SDK/cache ile MAC_EK'te: `flutter test --no-pub test/reading_image_viewport_test.dart test/reading_images test/story_detail test/image_experiment/reader_resolution_test.dart`; değişen dosyalarda `flutter analyze --no-pub`. Annotation/DI/Cubit/route değişmez. Normal metadata'da eksik P3 kimliği türetilmez. Yeni üretim/auth/DB/bulut yok; fiziksel kabul ertelenmiştir.

Özel çalışma kökü: `/Volumes/MAC_EK/tellpal-qa-private/p4a3-viewport-20261018-86b70c22`. Mevcut `operational-tools.private.json` araçları sabitler. `protected-before.private.json` içindeki 383 dosya ve onaylı blok hash'i değişmez; görev dışındaki kirli dosyalar da korunur. Yeni snapshot/loglar yalnız bu kökte oluşturulur.


## Uygulama ve doğrulama kaydı — 2026-10-18

`ReadingImageViewport`, son görsel slotunu Flutter presentation katmanında ölçer ve o yerleşimin `Size`, DPR ve `BoxFit` değerlerini verir. Detail oranı bu bileşene taşındı. Detail hero mevcut provider/decode yolunu kullanır ve fit değerini alır. Reader'ın iki dalı aynı ölçüm noktasından geçer; mevcut `StoryReaderGeometry` adapter'ı, `observeLayout` ve gevşek constraints altında merkezlenen iç renderer korunur. QA ölçümü kendi `ReadingImageFit` değerine çevirerek mevcut Cubit'e iletir.

Kullanıcı, gerekli yerel Flutter test kopyasının özel MAC_EK kökünde tutulmasını ayrıca açıkça onayladı. Import/export/part bağımlılık kapanışıyla 134 Dart dosyası; gerekli asset, fixture ve no-pub çözümleme dosyalarıyla toplam 155 kaynak girdisi kopyalandı. Native proje, signing veya gerçek kimlik/token dosyası alınmadı.

### Çalıştırılan kontroller

- İlk hedef koşu: `flutter test --no-pub --reporter expanded test/reading_image_viewport_test.dart test/reading_images test/story_detail test/image_experiment/reader_resolution_test.dart`. 196 test geçti; yeni test yardımcılarında beş hata bulundu. Üç QA testinde provider generic türü açık yazıldı. İki reader testinde fake HTTP debug callback'i `finally` içinde temizlendi. Uygulama kodunda bu hatalar için değişiklik gerekmedi.
- Düzeltme sonrası koşu: `flutter test --no-pub --reporter json test/reading_images/reading_image_comparison_view_test.dart test/image_experiment/reader_resolution_test.dart`. 18 test geçti; exit code `0`, atlanan test `0`.
- Sonuç, ilk tam koşu ve etkilenen iki dosyanın düzeltme sonrası koşusunun birleşimidir: hedef kümedeki 201 farklı test doğrulandı. Diğer 183 testin girdileri değişmedi.
- Değişen dokuz Dart dosyasında `flutter analyze --no-pub`: `No issues found!`, exit code `0`.
- Dokuz Dart dosyası formatlandı; `git diff --check` temiz.

### Kabul matrisi ve geçen testler

| Durum | Geçen kanıt |
|---|---|
| Detail gerçek slot/DPR/cover ve mevcut oran | `reading_image_viewport_test.dart`: `son rect/DPR/fit yeniden ölçülür; detail=true`; `story_detail_view_test.dart`: 24 `adaptive` örneği hero/viewport rect eşitliği, cover, oran, semantics, lock/blur, kontrol ve scroll beklentileri. |
| Reader'ın iki dalı | `reader_resolution_test.dart`: `reader ortak alanı yeniden ölçer ve medya sırasını korur; owned=false/true`. Gerçek slot, DPR, fit, padding/insets; 1×1 iç raster'ın intrinsic boyutu ve slot merkezinde kalması; `selected`→`prepare`, `observeLayout`→`display`. Mevcut `selected prefetch and display pin one provider; rapid stale selection cannot paint old page` de geçti. |
| Pencere/DPR/metin ölçeği | Ortak widget'ın `detail=false/true` matrisi; iki reader dalının aynı yaşam içindeki üç örneği; `QA pencere, DPR ve metin ölçeği değişince yeni rect seçiciye gider`. Telefon/tablet, yön ve kesirli DPR yeniden ölçülür. |
| QA kol değişimi, iki eksenli seçim ve ağsız mod | `kol değişimi sayfa alanını korur; metin ölçeği 1.0/1.5`: kapak ve iki sayfanın kol öncesi/sonrası rect eşitliği; ölçümün Cubit'e ulaşması; mevcut fixture/oracle ile aday ve shortage sonucu; repository çağrısı `0`. `ağsız geometri aynı slotta kaynak ve rendition kolunu değiştirir` de geçti. |
| Geçersiz ihtiyaç ve yetersiz çıktı | `reading_image_selection_test.dart`: `sonlu olmayan ve pozitif olmayan geometri tipli hatadır`, `tek eksen eksiği, kaynak upscale ve original fallback reddedilir`, `küçük matris bağımsız geometri ve exhaustive aday oracle ile eşleşir`; ortak viewport'ta `gerçek alan hazır fixture/seçici ve oracle ile eşleşir`. |

Ortak ölçüm, normal reader ve QA ekranının aynı yükseklikte slot ayırdığını kanıtlamaz. Dış kontroller ve yerleşimler farklıdır. Kanıt, her tüketicinin kendi gerçek slotunun doğru ölçülmesi ve mevcut davranışın korunmasıdır.

### Kaynak kimliği ve özel kanıt yolları

Tüm yollar şu özel köke göredir: `/Volumes/MAC_EK/tellpal-qa-private/p4a3-viewport-20261018-86b70c22`.

- Snapshot: `snapshot/`; ilk makbuz: `snapshot-initial-source.private.json`; yalnız iki test düzeltmesinin makbuzu: `snapshot-test-repair-02.private.json`.
- İlk koşu: `flutter-test-01.stdout.log`, `flutter-test-01.stderr.log`, `flutter-test-01.result.private.json`.
- Düzeltme koşusu: `flutter-test-02.stdout.jsonl`, `flutter-test-02.stderr.log`, `flutter-test-02.result.private.json`.
- Analiz: `flutter-analyze-01.stdout.log`, `flutter-analyze-01.stderr.log`, `flutter-analyze-01.result.private.json`.
- Son kaynak ve koruma makbuzu: `verified-source.private.json`. 155 girdinin repo/snapshot hash'leri eşit. Sıralı kaynak hash haritasının SHA-256 kimliği: `958e1203b91224ee5771062771ba414c1d0e25ffbb1e8b8e2993a87e6a484633`.
- `protected-before.private.json` içindeki 383 dosyada fark `0`. Onaylı blok SHA-256 değeri değişmedi: `92afb549c34a10f61c5f8add3be29d9f8cfea23a4c6d21993a0a0d60fe048c01`. Görev dışı kirli kaynaklar ve önceki kanıtlar korundu.

Fiziksel cihaz/kalite/performance kabulü ertelenmiştir. Normal P3 teslimatı ve iPhone/P0/P4b/üretim kabulü kapsam dışındaki kapılar olarak açık kalır.


## İnceleme düzeltmesi ve güncel doğrulama — 2026-10-18

Verification incelemesindeki tek patch bulgusu kapatıldı: geometry modu testleri, `state.raster` doluyken view'ün `geometry.fit` değerini renderer'a aktarmasını sınamıyordu. `reading_image_comparison_view_test.dart` dosyasına `yüklü QA view kapak/sayfa ve iki kolda ortak rect ve selector fit değerini rastera taşır` testi eklendi. Mevcut `NativeRepository`/`png` seam'i ve gerçek Cubit kullanılır. 1200×1200 kaynak ve 1000×1000 rendition PNG'leri gerçekten decode edilir. Kapak ve iki sayfanın canonical/rendition geçişlerinde şunlar doğrulanır:

- `ReadingImageComparisonRaster.fit`, ortak viewport'un cover/contain değeriyle eşleşir.
- Raster, clip slotu ve ortak viewport rect'leri eşittir; kol değişiminde rect korunur.
- Fiziksel ihtiyaç, required ve decode boyutları, gerçek rect/DPR/fit ile çağrılan mevcut seçicinin sonucuyla eşleşir. Canonical görüntü kaynak boyutunda, rendition seçilen decode boyutundadır.
- Cubit kapanışı gerçekten tamamlanır; image/buffer/descriptor/codec sahiplik sayaçlarının tümü sıfırdır.

Native Future'lar için test-only `_NativeWorkGate`, `tester.runAsync` zone'unda mevcut `super.run` seri kuyruğunu çalıştırır. Üretim gate'i ve medya politikası değişmedi. Yeni test yalnız bellekteki sentetik repository/PNG'yi kullanır; gerçek auth, yeni ağ çağrısı, cihaz veya bulut işlemi açmaz.

### Yeni koşular ve matris denetimi

- `snapshot-review-patch-03` içindeki ilk deneme sonlanmadı: diğer 197 test geçti, yeni test ve aynı dosyanın devamı bitmedi. Yalnız bu görevin test PID'sine SIGINT gönderildi. `flutter-test-03.interruption.private.json`, bu koşuyu açıkça başarı dışında tutar; süreç exit code `0` olsa da normal `done.success=true` kanıtı yoktur. Native Future/fake async zone sınırı test harness'ında ele alındı.
- Önceki snapshot değiştirilmeden `snapshot-review-patch-04` oluşturuldu. `flutter test --no-pub --timeout 30s --reporter json test/reading_images/reading_image_comparison_view_test.dart`: beş test geçti, atlanan `0`, normal `done.success=true`. Close işlemi üç saniyelik sınıra takılmadan tamamlandı.
- Aynı 04 kaynaklarında spec'in tam hedef kümesi tekrar çalıştırıldı: `flutter test --no-pub --reporter json test/reading_image_viewport_test.dart test/reading_images test/story_detail test/image_experiment/reader_resolution_test.dart`. **202 testin tamamı geçti; atlanan `0`, exit code `0`, normal `done.success=true`.**
- Aynı snapshot'taki dokuz değişen Dart dosyasında `flutter analyze --no-pub`: `No issues found!`, exit code `0`. `git diff --check` temiz.
- Önceki kabul matrisinin tüm satırları bu tam koşuda yeniden geçti. Loaded QA satırı, yeni testle doğrudan view'ün dolu raster dalına bağlandı. Ortak ölçüm, dış kontrolleri farklı normal reader ve QA ekranlarında eşit slot yüksekliği iddiası taşımaz.

### Yeni özel kanıtlar ve kaynak kimliği

Yollar aynı onaylı özel köke göredir: `/Volumes/MAC_EK/tellpal-qa-private/p4a3-viewport-20261018-86b70c22`.

- Snapshot kaynakları: `snapshot-review-patch-03.source.private.json`, `snapshot-review-patch-04.source.private.json`.
- Sonlanmayan deneme: `flutter-test-03.stdout.jsonl`, `flutter-test-03.stderr.log`, `flutter-test-03.result.private.json`, `flutter-test-03.interruption.private.json`.
- Bounded view koşusu: `flutter-test-04.stdout.jsonl`, `flutter-test-04.stderr.log`, `flutter-test-04.result.private.json`.
- Tam hedef koşusu: `flutter-test-05.stdout.jsonl`, `flutter-test-05.stderr.log`, `flutter-test-05.result.private.json`.
- Analiz: `flutter-analyze-02.stdout.log`, `flutter-analyze-02.stderr.log`, `flutter-analyze-02.result.private.json`.
- Güncel kaynak/matris makbuzu: `verified-source-review-patch-04.private.json`. Geçen 202 testin adlarını da içerir. 155 girdinin repo/snapshot hash'leri eşittir. Sıralı kaynak hash haritasının SHA-256 kimliği: `b3461926c97217a9f834a2f730012ffc91c5684417af7023f504f395774885e2`.
- `protected-before.private.json` içindeki 383 dosyada fark `0`; frozen SHA-256 değişmedi. Önceki log/makbuz hash'leri, önceki `snapshot/` girdileri ve `snapshot-review-patch-03` kaynakları korundu.

Bu düzeltme yalnız test kapsamını genişletir. Fiziksel cihaz, kalite/performans ve üretim kabul kapıları açık kalır.


## BMad inceleme sonucu

Üç bağımsız inceleme tamamlandı. Blind hunter ve edge-case hunter eylem gerektiren bulgu bildirmedi. Verification incelemesinin yüklü QA raster fit testi eksikliği `patch` olarak sınıflandırıldı ve kapatıldı. Son tam koşu 202/202, analiz temizdir. Yeniden planlama veya ertelenen yeni bulgu yoktur. Ham inceleme ve sınıflandırma kaydı özel kökteki `review-01/` altındadır.

## Önerilen inceleme sırası

**Ortak ölçüm**

- Son slot, DPR ve fit birlikte ölçülür; detail oranının tek sahibi burasıdır.
  [reading_image_viewport.dart:18](../../app/mobile/lib/core/presentation/widgets/reading_image_viewport.dart#L18)

**Tüketici bağlantıları**

- İki reader dalı aynı slotu ölçer; iç görselin mevcut boyutu ve merkezlemesi korunur.
  [story_player_page.dart:347](../../app/mobile/lib/story_player_page.dart#L347)

- QA ortak ölçümü mevcut seçiciye ve yüklü raster renderer’ına aktarır.
  [reading_image_comparison_view.dart:94](../../app/mobile/lib/features/reading_images/presentation/view/reading_image_comparison_view.dart#L94)

- Normal detail aynı oranı kullanır; mevcut dekorasyon ve kontroller korunur.
  [story_detail_view.dart:174](../../app/mobile/lib/features/story_detail/presentation/view/story_detail_view.dart#L174)

- Hero fit değerini alır; provider, cache ve decode sahipliği korunur.
  [story_detail_hero.dart:31](../../app/mobile/lib/features/story_detail/presentation/widgets/story_detail_hero.dart#L31)

**Doğrulama**

- Gerçek slot ölçümü farklı pencere, DPR ve metin ölçeklerinde mevcut seçiciyle sınanır.
  [reading_image_viewport_test.dart:11](../../app/mobile/test/reading_image_viewport_test.dart#L11)

- Yüklü QA ekranı gerçek decoder ile cover/contain, kaynak/rendition ve kapanış davranışını doğrular.
  [reading_image_comparison_view_test.dart:70](../../app/mobile/test/reading_images/reading_image_comparison_view_test.dart#L70)

- İki reader dalında küçük raster boyutu, slot ölçümü ve medya sırası doğrulanır.
  [reader_resolution_test.dart:341](../../app/mobile/test/image_experiment/reader_resolution_test.dart#L341)

- Detail matrisinde gerçek rect, semantics, blur, kontroller ve scroll korunur.
  [story_detail_view_test.dart:89](../../app/mobile/test/story_detail/story_detail_view_test.dart#L89)
