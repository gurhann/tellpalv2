---
title: 'P4b5 — yeni APK ile tek sınırlı tekrar'
type: 'chore'
created: '2026-10-20'
status: 'in-progress'
review_loop_iteration: 0
baseline_commit: '3ec09d1e124080d9ffebf5ec4cd098cbd6a177b8'
context: ['{project-root}/AGENTS.md']
---

<frozen-after-approval reason="İnsan onayından sonra amaç yalnız kullanıcı tarafından değiştirilebilir">

## Amaç

Süreç onarımını yeni APK'ya taşı; mevcut #94 PNG'leriyle tek emülatör ölçümünü ayrı onaya hazırla. Önceki üç deneme kapalı kalsın.

## Sınırlar

**Her zaman:** [Teknik ek](image-optimization/p4b5-manual-retry-measurement.md) ve [sayısal öneri](../../be/docs/evidence/p4b5-manual-retry-measurement/bounded-run-proposal-2026-10-20.json) planın parçasıdır. Yeni sabit kapsam, ayrı paket ve MAC_EK dizini kullanılır. Eski kayıtlar ve borç korunur. Tek ilk başlangıçta ağsız baseline → aynı PID'de ölçüm → son kapanış uygulanır. Tek pencere 120 saniye; en çok 1 manifest, 12 status, 4 medya / 13.170.884 byte, toplam 17 HTTP. Birer kurulum, ilk başlangıç, backend başlangıcı ve giriş; eski üç başlangıç iade edilmez.

**Önce onay:** Plan onayı yalnız yerel uygulama, credential'sız kopya, test, offline APK ve üç incelemeyi açar. Gerçek deneme somut APK/run üzerinden ayrıca onaylanır. Limit değişimi ve yeni tekrar ayrı karar gerektirir.

**Asla:** Yeni üretim, upload, eski appdata/kanıt silme, DB klonu/sıfırlama, otomatik tekrar, original fallback, fiziksel cihaz veya dağıtım yoktur. Normal erişim, ses, gezinme ve cache korunur.

| Durum | Sonuç |
|---|---|
| Yerel kapılar ve üç inceleme geçer | Somut APK/run teklif edilir; gerçek başlangıç bekler |
| Ayrı onay ve geçerli baseline | Aynı süreçte dört çizim ve lifecycle kanıtı |
| Eksik proof, kimlik, süre veya cleanup | Başarısız terminal; tekrar yok |

</frozen-after-approval>

## Kod haritası

Teknik ekteki sembol haritası uygulanır: mevcut manual controller/adapter, Java test harness'i ve Flutter bootstrap paylaşılır. Yeni ince entry yalnız sabit kapsamı seçer. Eski varsayılanlar korunur.

## Görevler ve kabul

- [x] `tools/image-optimization/`, `be/src/test/java/.../support/`, `app/mobile/test_driver/`: yeni sabit kapsamı ve üç geçmiş proof'unu bağla; tam olumlu kapı ve ret testlerini ekle.
- [ ] Teknik ekteki sırayla yerel test → kaynak dondurma → kopya/pub → APK → bağlar → üç inceleme; somut gerçek koşu teklifinde dur.
- [ ] Ayrı onaydan sonra tek denemeyi yürüt; yeni rapor, üç son inceleme ve görevle sınırlı yerel commit oluştur.

**Kabul:**
- Verilen ayrı onay yokluğu; yürütme çağrıldığında gerçek claim, kurulum ve giriş sıfır kalır.
- Verilen onaylı kapsam; geçerli baseline sonrası ölçüm yürütüldüğünde aynı süreçte dört çizim ve kaynak bırakma kanıtlanır.
- Verilen eksik kanıt veya hata; deneme kapandığında başarı üretilmez ve eski tüketim geri verilmez.

Yerel testler veya emülatör başarısı tüm P4b'yi kapatmaz.

## Spec değişiklik kaydı

### Kaynak ön incelemesi — 2026-10-20

2026-10-20 yönlendirmesi: Kullanıcı önce DevTools ile mevcut görsellerde ağsız bağlantı ve sade plan istedi. Bu manual-retry yürütmesi açılmadı. Kaynaklar, payload sürümleri, retler ve aşağıdaki iki açık inceleme görevi korunur. Yeni yol: [DevTools planı](spec-p4b5-devtools-offline-observation.md). Bu not eski onayı, claim'i veya tüketimleri değiştirmez.

Bu inceleme APK/run veya gerçek yürütme kabulü değildir. Kopyalama izni beklenirken yalnız kaynak ve yerel testler incelendi. İki doğrulama boşluğu yerel hazırlık kapsamında giderilir; gerçek çalışma onayı değişmez.

- [ ] [Review][Patch] Gerçek `ManualController.run` pencere testini iki manual kapsamla çalıştır; gönderilen configuration ve kabul makbuzunun seçili kimliğini doğrula. `tools/image-optimization/test_p4b5_manual_consumer_qa.py:689`
- [ ] [Review][Patch] Gerçek cleanup transport matrisini iki manual kapsamla çalıştır; yalnız seçili paketin durdurulduğunu ve ardından `/proc` yokluğu arandığını doğrula. `tools/image-optimization/test_p4b5_manual_consumer_qa.py:785`

## Doğrulama

Teknik ekteki Python, Flutter/analyzer ve Java/PostgreSQL kapıları skipsiz geçer. Eski/yeni kapsam, import, paket ve proof mutasyonları reddedilir. Gerçek ölçümde seçilen rendition, payload, geometri, dört paint/ACK ve kapanış kaynakları kaydedilir. Fiziksel kalite ve üretim kararları açık kalır.

### 2026-10-20 yerel ilerleme

78 Python, 232 Flutter ve 26 Java testi skipsiz geçti; analyzer ve diff kontrolü temiz. Üç gerçek tarihsel zincirin 50 dosya hash’i salt okunur doğrulandı. [Yerel uygulama kaydı](../../be/docs/evidence/p4b5-manual-retry-measurement/local-implementation-2026-10-20.md) test sınırlarını ve bekleyen adımları açıklar.

MAC_EK kaynak kopyası otomatik onay incelemesi tarafından iki kez reddedildi. [Somut payload](../../be/docs/evidence/p4b5-manual-retry-measurement/copy-payload-audit-2026-10-20.json): 1166 dosya / 8.552.437 byte; SHA-256 `8c3ed7a5dcef734bed9abc19481150a7f3d31f98b34ffd2bc6097c37fca28580`. Doğrudan kopya onayı bekleniyor. APK/run ve üç ön inceleme yoktur. Gerçek claim, kurulum, ilk başlangıç, backend, giriş ve pencere sıfırdır. Spec `in-progress` kalır; gerçek yürütme ayrıca onay gerektirir.

Son onay incelemesi: önceki payload’a doğrudan onay ve 144 byte fark kanıtıyla güncel payload tekrar sunuldu; güncel byte toplamı için açık onay olmadığı gerekçesiyle üçüncü ret geldi. [Append-only ret kaydı](../../be/docs/evidence/p4b5-manual-retry-measurement/approval-review-block-0003-2026-10-20.json) tutuldu. Kaynak/kopya/APK/gerçek deneme sınırları korunur.
