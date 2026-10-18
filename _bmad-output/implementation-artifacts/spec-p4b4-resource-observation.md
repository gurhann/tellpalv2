---
title: 'P4b4: QA kaynak rezervasyonunu ölçüm JSON’unda doğrula'
type: 'feature'
created: '2026-10-18'
status: 'done'
baseline_commit: '52098d94f89c9743710d213c43bdcf318486108b'
human_approval: '2026-10-19: Yapalım — somut plan ve gerekli özel kaynak/test kopyası onaylandı.'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b4-resource-observation-investigation.md'
---

<frozen-after-approval reason="Kullanıcıya sunulan somut P4b4 planı">

## Amaç

İki QA probe'u P4b2 rezervasyonunu henüz aktarmıyor. Mevcut getter'ları aynı sürümlü
JSON'a bağla; saf yerel Python doğrulamasıyla ölçüm/eksik kanıt ayrımını koru.

## Sınırlar

**Daima:** Eski alanlar, kayıtlar ve kabul sınırları korunsun. Yeni `resourceObservation`
v1 bloğu `lifetimeId`, integer `reservedSlots`, kanonik onluk string byte/attempts
taşısın. Aynı isolate yaşamının sayacı resetlenmez; baseline sıfır varsayılmaz.

**Önce sor:** Gerçek koşu, erişim/cache veya sayısal üretim profili kararı.

**Yapma:** Eski run'ı yeniden açma; runtime/Cubit/transport, yeni scheduler/retry/IPC,
servis veya CLI ekleme. Bulut, giriş, cihaz, üretim ve RSS/GPU/peak kabulü yoktur.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Gerçek native pending; image sıfır | Slot/byte dolu olabilir; image ile eşitlenmez. İptal bildirimi kapasiteyi açmaz. |
| Resident; ikinci native ret | Doğru slot/byte JSON'a çıkar; ret attempts'ı artırmaz. İki probe tutarlıdır. |
| Gerçek cleanup/drain | Dört active ve slot/byte sıfır; artan attempts korunur. Eksik drain reddedilir. |
| Aynı yaşam; sıfır olmayan baseline | Kayıpsız fark; azalan sayaç reddedilir. Farklı yaşamlar tek dizide birleştirilmez. |
| Eski şemasız kayıt | `legacy/measured=false`; yeni sayaç/delta uydurulmaz. Eski başarı yeniden yazılmaz. |
| V1 iddiası; eksik/yanlış tip/format/sürüm | Tipli ret; null/bool/kesir/negatif/karma legacy-v1 kabul edilmez; sıfır fallback yoktur. |

</frozen-after-approval>

## Kod haritası

[İnceleme](image-optimization/p4b4-resource-observation-investigation.md): alanlar,
satırlar, yerel gerçek JSON zinciri, tip/lifetime kuralları ve koruma kapanışı.
Runtime, eski Python yürütücüler ve onay zincirleri salt okunur kalır.

## Görevler ve kabul

- [x] `app/mobile/test_driver/reading_resource_observation.dart`: ortak read-only v1 helper.
- [x] `app/mobile/test_driver/reading_consumer_qa_probe.dart`, `app/mobile/test_driver/reading_comparison_probe.dart`: mevcut map yayılımını helper'a bağla.
- [x] `tools/image-optimization/p4_resource_observation.py`: yalnız verilen değerleri doğrula; legacy ve yeni iddiayı ayır.
- [x] `app/mobile/test/reading_images/reading_resource_observation_test.dart`, `app/mobile/test/reading_images/reading_consumer_qa_probe_test.dart`, `app/mobile/test/reading_images/reading_comparison_probe_test.dart`: gerçek getter/native/HTTP ve JSON kabulü.
- [x] `tools/image-optimization/test_p4_resource_observation.py`: tip/sıra/drain matrisi; gerçek Dart JSON'unu rapora kadar doğrula.
- [x] `be/docs/evidence/p4b4-resource-observation/verification.md`: test/mutasyon/hash ve inceleme kanıtı.

Given gerçek yerel decode ve probe çıktısı, when JSON Python'a verildiğinde, then
pending/resident/drain ve sayaç farkı doğru raporlanır; yetki veya fiziksel ölçüm üretilmez.

## Spec değişiklik kaydı

## Doğrulama

İncelemedeki gerekli credential'sız MAC_EK kaynak/test kopyası plan onayına dahildir.
Mevcut SDK/cache/interpreter ile focused ve izlenen tam Flutter testleri, analyzer,
yeni Python ve eski yürütücü testleri geçsin. Native/loopback → probe → gerçek JSON →
Python zinciri ve incelemedeki mutasyonlar kanıtlansın. Matris test adları geçmiş
ham çıktıda görünsün; tekrar sayıları toplanmasın. Kaynak/koruma/frozen/diff-check,
üç BMad incelemesi ve yalnız görev dosyalarının yerel commit'i tamamlanır. Genel
P4b, üretim ve fiziksel kabul açık kalır. Model oturumdan devralınır.

## Uygulama devri

Bu, kök ajanın sürmekte olan BMad step-03 alt görevidir. Yeni workflow başlatma;
yeniden plan/onay isteme. Model oturumdan devralınır. Uygulama ajanı görev kodu,
özel kaynak kopyası, test/analyzer/mutasyon ve kanıtı tamamlar. Kök ajan sonraki
üç bağımsız incelemeyi ve görevle sınırlı yerel commit'i yürütür. Companion salt
okunur kalır; frozen block değişmez.

Onaylı özel kökün tarihli adı korunur:
`/Volumes/MAC_EK/tellpal-qa-private/p4b4-resource-observation-20261018-52098d9/`.
`receipts/protected-before.private.json` başlangıç korumasıdır: 3279 repo, 72 eski
run, 192 P4a5, 296 P4b1, 281 P4b2 ve 260 P4b3 özel dosyası. SDK ve mevcut sentetik
TLS interpreter yollarını yalnız yeni `receipts/operational-tools.private.json`
metadata'sından oku. Yeni log/makbuzlar sıralı, `0600`, CREATE_NEW; dizinler `0700`.

Önceki P4b3 `receipts/19-source-closure.private.json` gerekli izlenen kaynak/test
ve fixture kapanışını gösterir. Yalnız yeni köke kopyala; yeni Python modülü ve
eski yürütücü testlerinin gereken izlenen kapanışını da ekle. Mevcut SDK/cache ve
interpreter kullanılır; kurulum/indirme yoktur. Dirty girişin HEAD istisnasını,
untracked local-lossless kapsam dışını ve yeni dosya kapanışını açıkça kaydet.
Dart'tan gerçek JSON'a, oradan Python raporuna kadar yerel kanıt gerekli; yalnız
iki ayrı sentetik fixture yeterli değildir. Eski yürütücülerin hiçbir gerçek
çalıştırma komutunu çağırma; yalnız yerel testleriyle regresyonu doğrula.

## Önerilen inceleme sırası

**Ortak gözlem**

- Mevcut getter’ları tek v1 JSON sözleşmesine bağlar; sayaç ve isolate yaşamı korunur.
  [reading_resource_observation.dart:12](../../app/mobile/test_driver/reading_resource_observation.dart#L12)

- Consumer state, snapshot ve paint gözlemleri mevcut yayılımla ortak helper’dan gelir.
  [reading_consumer_qa_probe.dart:330](../../app/mobile/test_driver/reading_consumer_qa_probe.dart#L330)

- Comparison probe aynı alanları üretir; eski active sayaçların anlamı korunur.
  [reading_comparison_probe.dart:267](../../app/mobile/test_driver/reading_comparison_probe.dart#L267)

**Yerel kabul ve kayıpsız sayım**

- Legacy ve v1 ayrılır; yalnız aynı yaşamın sıralı örneklerinden fark hesaplanır.
  [p4_resource_observation.py:59](../../tools/image-optimization/p4_resource_observation.py#L59)

- Drain yalnız görselin kalkmasını yeterli saymaz; rezervasyon ve tüm active handle’lar denetlenir.
  [p4_resource_observation.py:102](../../tools/image-optimization/p4_resource_observation.py#L102)

**Test ve kanıt**

- Gerçek native pending, iptal ve cleanup iki probe’un JSON değerleriyle doğrulanır.
  [reading_resource_observation_test.dart:46](../../app/mobile/test/reading_images/reading_resource_observation_test.dart#L46)

- Gerçek loopback ve codec çıktısı Python’a aktarılacak ham JSON’u üretir.
  [reading_comparison_probe_test.dart:384](../../app/mobile/test/reading_images/reading_comparison_probe_test.dart#L384)

- Gerçek Dart JSON’u rapora ulaşır; tip, lifetime, baseline ve drain regresyonları korunur.
  [test_p4_resource_observation.py:144](../../tools/image-optimization/test_p4_resource_observation.py#L144)

- Sonuçları, ilk hataları, mutasyonları ve fiziksel ölçüm sınırını birlikte kaydeder.
  [verification.md:11](../../be/docs/evidence/p4b4-resource-observation/verification.md#L11)
