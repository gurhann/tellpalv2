---
title: 'P4b1: QA kapanışında status işini sonlandır'
type: 'bugfix'
created: '2026-10-18'
status: 'done'
baseline_commit: '35d0bb384b0aba642e4cbfb8cc0a4000fdccf909'
approval: '2026-10-18: A — dar plan, özel kaynak/test kopyası ve yerel doğrulama onaylandı'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4b1-status-drain-investigation.md'
---

<frozen-after-approval reason="Somut P4b1 planı kullanıcı onayını bekliyor">

## Amaç

QA `close()` medya işini bekliyor, status HTTP Future'ını beklemiyor. Parent erken kapanmış sayılabilir. Aynı kapanış, iptal edilmiş bütün mevcut işleri gerçekten beklesin.

## Sınırlar

**Daima:** Mevcut Cubit/DI/router/transport/epoch/anchor/native gate korunsun. Kapanış başladığında yeni iş ve geç paint reddedilsin. İptal bildirimi sonlanma kanıtı değildir. Aynı anda çağrılan close ortak kapanışı beklesin. Eski dosya/hash ve `.memlog.md` korunsun.

**Önce sor:** Ürün/cache/erişim politikası, sayısal üretim sınırı, backend/normal tüketici değişikliği veya gerçek cihaz/bulut işlemi.

**Yapma:** Yeni scheduler/transport/IPC/retry/timeout, prefetch, encode/upload, login/pencere, sayaç reseti, silme veya dağıtım. Status medya kuyruğuna alınmaz. QA no-store istisnası normal cache'e taşınmaz.

## Girdi ve sınır durumları

| Durum | Beklenen |
|---|---|
| Status yok | Mevcut medya/native kapanışı korunur. |
| Status bekliyor | İptal bildirilir; Future sonlanmadan close tamamlanmaz. |
| İptale rağmen geç success/error | Yeni admission/paint veya yakalanmamış hata oluşmaz. |
| Close sırasında seçim/timer/hesap/visibility | Yeni I/O başlamaz; eski epoch canlanmaz. |
| İki close; status ve medya bekliyor | Aynı kapanış ikisini de bekler. |
| Parent ve gerçek yavaş status gövdesi | Socket/client işi sonlanır; sonra route kapanır. |

</frozen-after-approval>

## Kod haritası

[İnceleme ve test haritası](image-optimization/p4b1-status-drain-investigation.md): Cubit `_check/retire/close`; parent `_drain`; mevcut data-source cancellation/finally. Yeni mimari katman gerekmez.

## Görevler ve kabul

- [x] `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart`: status Future sahipliği, kapanış iş kabulü ve ortak drain.
- [x] `app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart`: tablodaki bütün durumlar; bekletilmiş status ve medya completion'ları.
- [x] `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart`: gerçek loopback status ve parent kapanış sırası; mevcut selector/native regresyonu.
- [x] `be/docs/evidence/p4b1-status-drain/verification.md`: test, mutasyon, hash ve inceleme kanıtı; fiziksel/üretim sınırları. Uygulama kanıtı hazırdır; bağımsız inceleme sonuçları sonraki workflow adımında eklenir.

Given çalışan QA akışı, when parent kapanırken status bekliyorsa, then iptal edilen status ve medya sonlanmadan route kapanmaz; sonrasında yeni I/O/paint olmaz.

Given korunan normal tüketiciler, when regresyonlar koşarsa, then Library/detail/reader ses, cache ve gezinme davranışları aynı kalır.

## Spec değişiklik kaydı

- 2026-10-18: Uygulama ve yerel doğrulama tamamlandı. Son kaynakta 468 test, temiz analyzer ve üç koruma için dört mutasyon kanıtı kaydedildi. Üç bağımsız inceleme ve yerel commit bekleniyor; frozen kapsam değişmedi.

## Doğrulama

İncelemedeki açık yerel kopya/log kapsamı plan onayına dahildir. MAC_EK özel kopyasında mevcut SDK/cache ile `flutter test --no-pub test/reading_images test/story_detail`, izlenen tam test kümesi ve `flutter analyze --no-pub` geçsin. Status-await/iptal/iş kabulü mutasyonları ilgili testleri bozsun; eski ham loglar korunarak kaynak geri alınsın. `git diff --check`, kaynak eşitliği ve kapsam dışı hash'ler doğrulansın. Üç BMad incelemesi sonrası yalnız görev dosyaları yerel commit'e girsin.

Gerçek Firebase/storage/cihaz koşusu yoktur. P4b'nin çoklu-owner bellek rezervasyonu, üretim profili, cache/session/retention ve fiziksel kalite kapıları açık kalır.

## Onaylı uygulama devri

Kullanıcı A ile bu somut planı ve özel kaynak/test kopyasını onayladı. Özel kökte
`receipts/protected-before.private.json` 3269 repo, 72 eski run ve 192 eski P4a5 özel
dosyasını; frozen hash'i ve mevcut dirty durumunu mühürler. `receipts/operational-tools.private.json`
mevcut SDK yollarıdır. Bunlar ve önceki kayıtlar değişmez. Yalnız bu spec, üç görev
kaynak/test dosyası ve yeni kanıt klasörü değişebilir; context incelemesi salt okunurdur.
İzlenen gerekli kaynak/fixture/paket varlıkları özel kopyaya alınabilir; credential,
token, gerçek servis config'i ve ilgisiz untracked dosyalar alınmaz. Log/makbuzlar
ayrı numaralı CREATE_NEW dosyalardır. Model oturumdan devralınır; override yapılmaz.

## Tamamlanan doğrulama

Son kaynakta 468 test geçti; analyzer temiz. Dört mutasyon hatayı yakaladı.
Üç bağımsız incelemede açık bulgu yok. Ses/gezinti kanıtını netleştiren belge
düzeltmesi kapandı; kod ve test kimlikleri değişmedi. Önceki kayıtlar korundu.

## Önerilen inceleme sırası

**Kapanış sahipliği**

- Ortak kapanış, iptal edilen medya ve status işi sonlanmadan tamamlanmaz.
  [reading_image_comparison_cubit.dart:387](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L387)

- Status Future sahipliği medya kuyruğundan bağımsız tutulur.
  [reading_image_comparison_cubit.dart:326](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L326)

**İş kabulü**

- Kapanış başladıktan sonra yeni seçim ve eski epoch işi kabul edilmez.
  [reading_image_comparison_cubit.dart:98](../../app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart#L98)

**Kanıt**

- İki completion sırası, geç sonuçlar ve ortak kapanış birlikte sınanır.
  [reading_image_comparison_cubit_test.dart:130](../../app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart#L130)

- Gerçek HTTP sonlanması, status Future tamamlanması ve parent kapanışı ayrı gözlenir.
  [reading_image_consumer_qa_test.dart:712](../../app/mobile/test/reading_images/reading_image_consumer_qa_test.dart#L712)

- Matris, ham kayıtlar, mutasyonlar ve kanıt sınırları son kaynakla eşleştirilir.
  [verification.md:50](../../be/docs/evidence/p4b1-status-drain/verification.md#L50)
