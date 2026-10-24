---
title: 'Linux RSS ölçümünde süreç kapanış yarışını düzelt'
type: 'bugfix'
created: '2026-10-24'
status: 'done'
review_loop_iteration: 0
baseline_commit: 90af526ff421fc16d78537293b90daa857f8db3c
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** CI'da ilk dönüşüm RSS ölçümünde düşüyor; generation değişimine ulaşmıyor. Kod kapanış yarışını ve Linux terminal durumunu tam ayırmıyor. Diagnostic kesin CI alt nedenini göstermiyor.

**Approach:** Son liveness kontrolünü ekle; Linux terminal `Z/X` kanıtını yaşayan/ölçülemeyen süreçten ayır. Diagnostic'i ayrıştır; gerçek Linux'ta doğrula.

## Boundaries & Constraints

**Always:** RSS/deadline/disk/log, descendant toplamı ve gerçek wait/teardown korunsun. Sıfır RSS yalnız doğrulanmış terminal/exit içindir; kaynak/kapasite gerçek çıkışa kadar tutulur. İşlemci kimliği fingerprint'e bağlanır.

**Ask First:** Push/deploy, canlı erişim, kapsam veya kaynak sınırı değişimi.

**Never:** Ölçülemeyen canlı sürece sıfır RSS, kör timeout artışı, mevcut testleri devre dışı bırakma/gevşetme, fixture'da gerçek korumayı bypass etme, guardian protokolü değişimi, bulut veya orijinal dosya değişimi.

## I/O & Edge-Case Matrix

| Durum | Beklenti |
|---|---|
| Bounded wait false, son liveness false | Yanlış RSS hatası yok; gerçek çıkış temizliği korunur |
| Linux `Z/X`, VmRSS yok, Java bildirimi bekliyor | Terminal kanıtı kabul; dış döngü gerçek wait bitene kadar kapasiteyi tutar |
| Canlı ve geçerli VmRSS | Gerçek RSS sayılır; aşım işi durdurur |
| Canlı/bilinmeyen, eksik/bozuk/okunamayan RSS | Açıklayıcı `RSS_PROBE_UNAVAILABLE` hatası ve gözlenmiş teardown |
| Normal exit, timeout/interrupt veya descendant aşımı | Mevcut korumalar ve sonraki işin kapasitesi doğru kalır |

</frozen-after-approval>

## Code Map

Java yolları `be/src/` altındadır.

- `main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java:127`: root probe, 20 ms confirmation ve Linux status ayrımı; ana düzeltme noktası.
- `main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java:33`: process class byte kimliği yakalanır. Yeni yardımcı class gerekirse kimliği de bağlanır.
- `test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java:149`: mevcut exit/live/cleanup testleri genişletilir.
- `test/java/com/tellpal/v2/asset/reading/ReadingPreparationIntegrationTest.java:252`: başarısız gerçek hazırlama/reuse senaryosu.
- `test/java/com/tellpal/v2/asset/reading/ReadingProcessOwnershipTest.java`: mevcut guardian/descendant kontrolleri.
- Yeni `test/java/com/tellpal/v2/asset/reading/ReadingImageLinuxRssTest.java`: gerçek Linux `/proc` ve kontrollü zombie fixture'ı; Linux doğrulamasında çalışması zorunlu.
- `ReadingImageProcess.treeResidentMemoryKiB`: gerçek Linux koşusunda guardian üyesinin `State=R` snapshot'ı `VmRSS` içermeden okundu. Aynı 20 ms sınırında tekrar ölçüm yapılır: geçerli RSS aynen toplanır; sıfır fallback yalnız terminal/exit kanıtındadır. Yaşayan ölçülemeyen üyede hata korunur.

## Tasks & Acceptance

**Execution:**
- [x] Process testinde false-wait/son-exit yarışını önce kırmızı üret; canlı karşı örneği koru.
- [x] Process class'ında son liveness/terminal ayrımını ekle. `RSS_PROBE_UNAVAILABLE` prefix'iyle dosya/okuma/eksik RSS/parse nedenlerini koru.
- [x] Linux fixture'ını `fork`/`waitid(WNOWAIT)` ile kur, finally'de reap et. Parser testlerini tüm platformlarda çalıştır.
- [x] Guardian üyesindeki RSS/kapanış yarışını önce kırmızı üret; terminal/exit doğrulamasını aynı 20 ms sınırında ekle. Yaşayan hata karşı örneği ve gerçek teardown korunsun.
- [x] Java 25 Linux'ta yeni testleri ve integration sınıfını üç kez çalıştır. Mimariyi raporla; Linux kanıtı eksikken kabulü kapatma.
- [x] Pinli araçlarla tam test/verify sonuçlarını kaydet. Test ortamını eski durumuna döndür.

**Acceptance Criteria:**
- Given terminal/confirmed exit, when RSS okunamazsa, then yanlış ret yoktur ve gerçek çıkıştan önce kapasite bırakılmaz.
- Given yaşayan ölçülemeyen veya sınırı aşan süreç, when monitor çalışırsa, then hata ve tam teardown gerçekleşir.
- Given Linux fixture, when tekrarlar çalışırsa, then byte/manifest/reuse ve süreç temizliği doğrulanır.
- Given tam doğrulama, when testler biterse, then failure/error yoktur; opt-in/platform skip'leri raporlanır.

## Spec Change Log

- Gerçek Linux tam koşusu, aynı kapanış yarışının descendant yolunu açığa çıkardı: `ReadingPreservationTest.webpPayloadTrailingDataAndPixelMutationCannotPass` üye PID için `RSS_PROBE_UNAVAILABLE:LINUX_STATUS_MISSING_RSS:state=R` aldı. Onaylı terminal/exit ayrımı bu yola da uygulanır. Root düzeltmesi, gerçek RSS toplamı, profil sınırları ve guardian protokolü korunur.

## Verification

Son üretim kodu Java 25.0.4.1, Ubuntu 24.04.5 ve Linux ARM64 üzerinde doğrulandı.
WebP araçları 1.6.0 ve kaynak kimliği doğrulanan guardian kullanıldı; native test marker'ı zorunluydu.
Wrapper `sh mvnw -B` ile çağrıldı. Geçici VM belleği 4 GiB idi; uygulama profilleri,
256 MiB test süreç sınırı ve parent admission oranları değiştirilmedi.

| Kontrol | Sonuç |
|---|---|
| Root kapanış yarışı, düzeltme öncesi | 1 test; beklenen `RSS_PROBE_UNAVAILABLE` hatası |
| Guardian üyesi yarışı, düzeltme öncesi | 1 test; beklenen `state=R/MISSING_RSS` hatası |
| Son odaklı Linux koşusu | 34 test, 0 failure/error/skip; Linux RSS sınıfında 8 test çalıştı |
| Son üretim koduyla hazırlama entegrasyonu | 3 ayrı Maven koşusu; her birinde 18 test, 0 failure/error/skip |
| Son Python launcher koşusu | 47/47 başarılı |
| Tam `test` | 611 test, 0 failure/error, 27 skip |
| Tam `verify` Surefire | 611 test, 0 failure/error, 27 skip |
| Tam `verify` Failsafe | 42 test, 0 failure/error, 3 skip |
| İnceleme sonrası diagnostic assertion kontrolü | Linux RSS sınıfı 8 test, 0 failure/error/skip |

Surefire atlamaları: P5g yerel koşu izni yok (23), üç ayrı package1 corpus/reread girdisi yok (3),
canlı CMS kabul izni yok (1). Failsafe atlamaları: `tellpal.package3.acceptance` yok (1),
izole nihai JDBC seçilmedi (1), izole sentetik JDBC seçilmedi (1).
Bu izinler açılmadı. `git diff --check` temizdir.

### Matris denetimi

Beş satırın tamamı son 34 testlik Linux koşusunda çalışıp geçen 16 yöntem kontrolüyle eşleşti.
Ayrıntılı yöntem/sınıf eşlemesi kanıt arşivindeki `matrix-audit.json` içindedir.

| Matris satırı | Geçen kontrol |
|---|---|
| False bounded wait sonrası gerçek çıkış | Root regresyonu, hem true hem false wait sonucu |
| Linux terminal ve geciken Java bildirimi | Z/X parser, gerçek zombie/reap, gerçek guardian üyesi çıkışı |
| Geçerli RSS ve aşım | Gerçek `/proc`, root/descendant aşımı, retry sonrası gerçek RSS toplamı |
| Yaşayan ölçülemeyen süreç | Eksik/bozuk/okunamayan parser, root ve guardian canlı hata karşı örnekleri |
| Timeout/interrupt/descendant temizliği | Gerçek PID çıkışı, kapasite/dizin koruma, sonraki iş ve ilgisiz süreç |

### Kanıt ve sınırlar

Ham loglar, XML raporları, komutlar ve kaynak SHA manifestleri repository dışında saklanır:
`/Users/gurhankucuk/.codex/visualizations/2026/10/21/01a15d22-a8a6-7b62-a0b1-4838e13bbee2/rss-exit-fix-2026-10-24-6o280gci`.
Tam koşular `v2.1-*`, üç hazırlama tekrarı `v2-preparation-repeat-*`, son assertion kontrolü
`review-assertion-*` adı taşır. Son değişiklik yalnız testteki descendant diagnostic assertion'ıdır;
tam kapılarda doğrulanan üretim kodu değişmemiştir.

İlk ortam denemeleri de gizlenmeden korunur: eksik altı sentetik JSON ve repository marker'ları
tamamlandı; 2 GiB VM'deki heap admission hatası 4 GiB test ortamında tekrar etmedi.
Ubuntu 26/uutils ilk koşusunda mevcut ownership fixture'ı timeout'tan önce gerçek RSS sınırına ulaştı.
Fixture yükü, beklenen hata ve kaynak koruması gevşetilmedi; son tam koşular Ubuntu 24/GNU ortamındadır.

Bu kanıt gerçek Linux ARM64 içindir. GitHub'ın hatalı koşusunun runner sürümü ve AMD64 sonucu
burada doğrulanmadı; CI yeniden çalıştırılmadı. İlk CI alt nedeninin kesinliği iddia edilmez.
Son parser/guardian tanıları bir sonraki olası hatada PID, root/descendant ve okuma nedenini ayırır.
Linux durum kaynağı: https://docs.kernel.org/filesystems/proc.html

Tam kapıların host/container denetiminde 761 kaynak, altı JSON ve AGENTS dosyası eşleşti.
Orijinal 41 görsel ve önceki arşivin 88 dosyası değişmedi; onaylı intent hash'i korundu.
Tüm özel test container'ları kaldırıldı. Colima profili önceki 2 GiB/kapalı durumuna döndü;
Docker context `desktop-linux` kaldı. Host wrapper modu 100644 olarak korundu.

## Yürütme notu

Bağımsız inceleme ve yerel commit ana ajanın sonraki BMad adımlarıdır; uygulama ajanı commit oluşturmaz. Test logları repository dışında saklanır. Başlatılan özel Colima/container kaynakları sonunda başlangıç durumuna döndürülür.

Önceki yerel test işi sonunda özel `tellpal-image-local` Colima profili kapalı, Docker context `desktop-linux` idi. Kullanmadan önce salt okunur doğrula; context değiştirmeden özel profili kullanabilirsin. Önceki loglar/pinli host araçlar `/tmp/tellpal-lease-precision.PQNclF/` altındadır. Linux test altyapısı ve byte kimliği kanıtları `be/docs/evidence/image-package-1/README.md` içinde bulunur. Maven wrapper özgün modu 100644; geçici executable değişirse geri al. Linux platform testlerinin Linux koşusunda gerçekten çalışması zorunludur; Mac skip kabul kanıtı olmaz.

## İnceleme

Üç bağımsız katman tamamlandı. Blind ve edge-case incelemeleri bulgu üretmedi.
Verification-gap incelemesi, descendant diagnostic önekinin testte açıkça doğrulanmamasını buldu.
Bulgu düşük önem/`patch` olarak ele alındı. Assertion güçlendirildi; aynı üretim koduyla sekiz Linux testi yeniden geçti.
Kalan açık bulgu yoktur. Sprint eşitlemesi `story_key` olmadığı için uygulanmadı.

## Suggested Review Order

**RSS ve kapanış doğrulaması**

- Root ve guardian üyelerinin RSS toplamı korunur.
  [ReadingImageProcess.java:105](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java#L105)

- Geçici hatada gerçek ölçüm veya kanıtlı çıkış aranır.
  [ReadingImageProcess.java:112](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java#L112)

**Linux durum ayrımı**

- Terminal Z/X durumları ve okuma nedenleri ayrılır.
  [ReadingImageProcess.java:166](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java#L166)

**Regresyon ve kaynak koruması**

- False wait sonrasında gerçek çıkış tekrar hataya dönüşmez.
  [ReadingImageProcessTest.java:170](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java#L170)

- Gerçek zombie, kapasiteyi reap tamamlanana kadar tutar.
  [ReadingImageLinuxRssTest.java:52](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageLinuxRssTest.java#L52)

- Gerçek guardian; kapanış, canlı hata ve yeniden ölçülen RSS aşımını sınar.
  [ReadingImageLinuxRssTest.java:85](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageLinuxRssTest.java#L85)

**İşlemci kimliği**

- Değişen process class byte kimliği profil descriptor içinde doğrulanır.
  [ReadingImageProcessTest.java:258](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java#L258)
