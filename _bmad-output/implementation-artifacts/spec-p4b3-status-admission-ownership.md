---
title: 'P4b3: QA status sonucunu doğru admission yaşamına bağla'
type: 'bugfix'
created: '2026-10-18'
status: 'done'
baseline_commit: 'b0ef8292a87127b81839a601053d658e952544d4'
human_approval: '2026-10-18: A — somut plan ve özel yerel kaynak/test kopyası onaylandı.'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b3-status-admission-investigation.md'
---

<frozen-after-approval reason="Kullanıcıya sunulan somut P4b3 planı">

## Amaç

QA'da sayfa/geometri/kol değişimi çizim epoch'unu yeniler; doğrulanmış admission
sürer. Bekleyen status reddi bu nedenle atılabilir. Status sonucunu çizimden ayrı,
halen güncel admission/istek sahipliğine bağla; mevcut temizleme kuralını uygula.

## Sınırlar

**Daima:** Medya epoch, LIVE anchor, mevcut hata kodları, P4b1 drain ve P4b2 rezervasyonu
korunsun. Emekli isteğin sonucu, aynı nesneyle yeniden admission yapılsa bile geçersizdir.
Eski kayıtlar ve ilgisiz değişiklikler korunsun.

**Önce sor:** Ürün/erişim/cache politikası, sayısal üretim bütçesi veya gerçek ortam işlemi.

**Yapma:** Yeni scheduler/DI/transport/retry/timeout, otomatik admission, fallback,
üretim/cihaz koşusu veya anlık sunucu revoke garantisi.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Aynı admission; seçim değişti; status ret/exception | `status_refused`; güncel iş/raster emekli edilir. |
| Aynı admission; seçim değişti; geçersiz/farklı LIVE | `live_changed`; eski kabul korunmaz. |
| Aynı admission; seçim değişti; geçerli aynı LIVE | Güncel sayfa/kol/geometri korunur; yeniden yükleme yoktur. |
| Retire/readmission; eski success/ret/exception | Yeni kabul ve state değişmez; aynı nesneye güvenilmez. |
| Account/background/lease sonrası eski sonuç | Admission veya eski paint canlanmaz. |
| Close sırasında status/medya bekliyor | Yeni iş yoktur; ortak close gerçek sonlanmayı bekler. |

</frozen-after-approval>

## Kod haritası

[İnceleme](image-optimization/p4b3-status-admission-investigation.md): yarışın kaynak
kanıtı, gerçek reader tetikleyicisi ve test/koruma haritası. Tek runtime alanı Cubit'tir.
Mevcut status cancellation yeniden kullanılır; metadata nesne eşitliği tek başına sahiplik değildir.

## Görevler ve kabul

- [x] `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart`: status sahipliğini medya epoch'undan ayır; emekli isteği reddet.
- [x] `app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart`: matris; bekleyen status ile seçim ve aynı nesneli yeniden admission yarışları.
- [x] `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart`: gerçek HTTP/status ve reader sayfa değişimi, güncel native raster cleanup'ı.
- [x] `be/docs/evidence/p4b3-status-admission/verification.md`: ilk hata, test/mutasyon, kaynak/hash ve inceleme kanıtı.

Given aynı admission için status beklerken, when reader sayfası değişir ve status
reddedilirse, then güncel görsel temizlenir; eski native iş bitmeden kapasite açılmaz.

## Spec değişiklik kaydı

## Tasarım notları

Çizim nesli ile kontrolün admission/istek yaşamı farklıdır. Mevcut private sahiplik ve
cancellation yolları kullanılır; yeni genel session politikası veya public API eklenmez.

## Doğrulama

İncelemedeki dar MAC_EK kaynak/test kopyası plan onayına dahildir. İlk hata korunur.
Mevcut SDK/cache ile `flutter test --no-pub test/reading_images test/story_detail`,
izlenen tam küme ve `flutter analyze --no-pub` geçsin. İncelemedeki mutasyonlar
assertion'ları bozsun; kaynak geri alınsın. Gerçek yerel HTTP/native test gereklidir.
Kaynak eşitliği, korunan hash'ler ve `git diff --check` doğrulansın. Üç BMad incelemesi
sonrası yalnız görev dosyaları yerel commit'e girsin. Genel P4b ve fiziksel kabul açık kalır.

## Uygulama devri

Bu, kök ajanın sürmekte olan BMad step-03 alt görevidir. Yeni workflow başlatma;
yeniden plan/onay isteme. Model oturumdan devralınır. Uygulama ajanı kod, özel
kaynak kopyası, yerel test/mutasyon/analyzer ve kanıtı tamamlar. Kök ajan üç bağımsız
inceleme ve görevle sınırlı yerel commit'i yürütür. Companion inceleme salt okunur kalır.

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b3-status-admission-20261018-b0ef829/`.
`receipts/protected-before.private.json` başlangıç korumasıdır: 3277 repo dosyası,
72 eski run, 192 P4a5, 296 P4b1 ve 281 P4b2 özel dosyası. Hash'leri doğrula;
eskilerini değiştirme. SDK/cache/interpreter yolları yalnız bu kökteki
`receipts/operational-tools.private.json` ve ek `receipts/operational-tools-02.private.json`
metadata'sından okunabilir. İkinci makbuz, önceki kanıtlı sentetik TLS interpreter yolunu ekler;
ilk makbuz değişmez. Credential yoktur.
Yeni makbuz/loglar sıralı, `0600`, CREATE_NEW; dizinler `0700` olsun.

İzlenen gerekli kopya kapanışı önceki P4b2 kaynak/test makbuzundan görülebilir;
yalnız yeni köke kopyala. SDK/paket/venv indirme veya kurma. Dirty girişin HEAD
istisnasını ve untracked local-lossless kapsam dışını açıkça kaydet. Companion'daki
matris, ilk başarısız assertion ve mutasyonlar uygulama kabulünün parçasıdır.

## Önerilen inceleme sırası

**Status sahipliği**

- Kontrol başlarken status isteğini kabul yaşamına bağlar; çizim neslini yakalamaz.
  [reading_image_comparison_cubit.dart:309](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L309)

- Güncel kabul, istek ve cancellation birlikte doğrulanır; eski sonuç yeni kabulü kapatamaz.
  [reading_image_comparison_cubit.dart:329](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L329)

- Normal sonuç ve exception aynı sahiplik korumasıyla mevcut temizleme yolunu kullanır.
  [reading_image_comparison_cubit.dart:346](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L346)

**Test ve kanıt**

- Seçim yarışı, aynı nesneli yeniden kabul ve lifecycle matrisi emekli sonuçları sınar.
  [reading_image_comparison_cubit_test.dart:216](../../app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart#L216)

- Gerçek HTTP cevabı UI sayfa değişiminden sonra bırakılır; native kaynaklar ve istekler denetlenir.
  [reading_image_consumer_qa_test.dart:279](../../app/mobile/test/reading_images/reading_image_consumer_qa_test.dart#L279)

- İlk hata, matris, mutasyon, kaynak koruması ve bağımsız inceleme kanıtlarını birleştirir.
  [verification.md:25](../../be/docs/evidence/p4b3-status-admission/verification.md#L25)
