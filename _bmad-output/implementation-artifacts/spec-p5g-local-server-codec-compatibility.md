---
title: 'P5g: Renk koruyan yerel sunucu uyumluluğu'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: '847fa87e4ff6438bc1cd47094833b5a0bb7bc8b0'
review_loop_iteration: 0
context:
  - '{project-root}/standards/commenting-standard.md'
  - '{project-root}/be/docs/adr/ADR-0022-reading-image-preparation-engine.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Amaç

İki hikâyenin seçilen 41 dosyasını Java'da yerel olarak doğrula ve yeni dizine
koruyarak aktar: 17 JPEG aynen, 24 mevcut kayıpsız WebP adayı. JPEG yeniden
kodlanmaz. WebP'nin bütün ham RGB örnekleri, ICC/XMP ve normalize TIFF içeriği
orijinal PNG ile eşleşir. Bu, ayrı codec denemesidir; P2/P3 kabulü değildir.

## Sınırlar

**Her zaman:** Salt okunur pinli P5c; tam decode; 1712×2778, tek kare, opak
8-bit RGB. Ham raster kullan; `getRGB()`/renk dönüşümü yok. Ayrı fingerprint:
kaynak/adayı, bütün ayarlar, class/JAR/JVM kimliği. Bağımsız çıktı reread.
Yeni create-only dizin; 41 orijinal, 124 P5c girdisi, P5f kanıtı ve 88 eski
çalışma dosyasının önce/sonra SHA koruması.

**Önce sor:** Üretim bağımlılığı, ana P2/P3 kurallarını genişletme veya başka
codec/renk dönüşümü yaklaşımı.

**Asla:** Canlı/Firebase/DB/Spring/cihaz/deploy/push; eski medya/kanıt yazımı,
silme, resize, profil silme/atama, yeniden encode, READY/LIVE/pointer değişimi.
İnternet yalnız resmi belge/Maven bağımlılığı içindir; deney ağ engellidir.

| Durum | Beklenen davranış |
|---|---|
| 17 JPEG / 24 WebP | Tam byte koruma; tam decode ve kaynak eşliği; 41/41 reread |
| Yanlış pin/MIME/ICC/RGB/XMP/EXIF | Açık ret; eksik bilgi başarı sayılmaz |
| Kayıplı/animasyon/şeffaf/orientation/bozuk/trailing | Decode öncesi veya tam doğrulamada ret |
| >16 MiB, >5M piksel, >64 KiB metadata | Tahsis öncesi ret; sınırlı metadata açma |
| Child >30 sn veya >512 MiB RSS | Durdur; gerçek exit bekle; başarı üretme |
| Çakışma/eksik çıktı/codec/koruma hatası | Üzerine yazma yok; tam başarı manifesti yok |

Child heap 256 MiB; tek iş; toplam çıktı 256 MiB; günlük/yanıt 1 MiB.
Bu sayılar yalnız yerel deneydir; üretim bütçesi değildir.

</frozen-after-approval>

## Kod haritası

- `be/src/test/java/com/tellpal/v2/asset/reading/P5fLocalCompatibilityProbe.java:46`:
  P5c pin/manifest ve JVM/class kimliği örneği. Eski dosyayı değiştirme.
- `tools/image-optimization/p5c_lossless_codec.py:71`: strict RIFF/VP8L örneği;
  EXIF'te yalnız `Exif\0\0` öneki ayrılır, TIFF içeriği değişmez.
- `be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java:69`:
  dar PNG/sRGB kimliği korunur; Child/Worker/Inspector değiştirilmez.
- `com.twelvemonkeys.imageio:imageio-webp:3.15.3`: test-only yeni reader;
  writer yok. `getRawImageType(0)` destination ve mantıksal RGB raster bantları
  gerekir. Varsayılan decode sRGB'ye çevirebilir; sentinel testi şarttır.
- P5c rapor SHA: `960d9945c87598bea04144a493d118a16df8e29f7ce85745c87e8d08ceefa000`.
  Yerel rapor ve koruma baseline yolları CLI'dan açık alınır; fixture keşfi yok.

## İşler ve kabul

- [x] `be/pom.xml`: kapalı `p5g-local-codec` Maven profili; yalnız test-scope
  reader. Normal test derlemesi ve production classpath değişmesin.
- [x] `be/src/test/java/com/tellpal/v2/asset/reading/p5g/`: ayrı profil,
  container/metadata kontrolü, raw decoder, supervised child ve CLI runner.
  Reflection ile reader seç; profile/property kapalıyken fail closed.
- [x] Aynı dizinde testler: matris; P3 sentinel; gerçek child/exit, immutable
  reread ve tüm corpus. Sahte başarı raporu eski kanıtın yerini almaz.
- [x] `be/docs/evidence/p5g-local-server-compatibility/`: sade sonuç ve
  sayısal kayıt; medya/ham günlükler Git dışında. Mevcut P2 retleri ayrı göster.

**Kabul:** Given pinli 41 kaynak, when yerel CLI çalışır, then 41 yeni çıktı
tam doğrulanır ve koruma geçer. Given kapalı profil, when normal backend/test
çalışır, then yeni codec veya kabul açılmaz. Given hata, when çalışma biter,
then tanı saklanır ve bütün corpus için başarı üretilmez.

## Plan değişiklikleri

- Codec ve renk okuma yaklaşımı değişmedi. macOS ağ sandbox içinde `ps`
  kullanılamadığı için RSS, mevcut `proc_pid_rusage` API'sinden okundu.
  Sayaç hatası başarı sayılmaz; gerçek süreç ve sınır testleri uygulanır.
- Implementer ajanı kapasite hatasıyla durdu; kalan iş ana oturumda tamamlandı.
- İncelemede bulunan beş uygulama/test eksikliği düzeltildi. Codec değişmedi;
  aynı pinli VP8L decoder'ın tükettiği bitler ayrıca doğrulanır. Raw destination
  RGB karşılaştırması korunur. Desteklenmeyen TIFF SubIFDs açıkça reddedilir.

## Doğrulama

Wrapper ile profil testlerini ve normal profilde DB gerektirmeyen seçili
P5f/P2 regresyonlarını çalıştır. Tam backend/DB suite kapsam dışıdır (`NOT_RUN`).
Kaynakları derleyerek gerçek child/corpus çalıştır; dependency çözümünden sonra
ağı engelle. Log/SHA, provider ve runtime kimliği tut; skip başarı değildir.
Üretim JRE/container çalıştırılmadıysa `NOT_RUN`; bu adım onu zorunlu kılmaz.
Yerel Java uyumluluğu, fiziksel renk onayı veya üretim geçişi olarak sunulmaz.

## Çalıştırma girdileri

- P5c raporu: `/Users/gurhankucuk/.codex/visualizations/2026/10/21/01a15d22-a8a6-7b62-a0b1-4838e13bbee2/p5c-lossless-r2-c48dfb97/report.json`.
- Koruma baseline: `/Users/gurhankucuk/.codex/visualizations/2026/10/21/01a15d22-a8a6-7b62-a0b1-4838e13bbee2/p5g-baseline-cfc20e8f602e.json`.
- Bu baseline 88 eski çalışma dosyasını, 41 orijinali, 124 P5c girdisini ve yedi P5f kanıtını içerir.
- `story_key` yoktur; sprint eşlemesi uygulanmaz.

## Gerçek sonuç

- Ağ engelli profil: 26 test geçti; normal profilde DB'siz 12 test geçti.
  Hata ve skip yok. Matris eşlemesi [doğrulama kaydında](../../be/docs/evidence/p5g-local-server-compatibility/dogrulama.md).
- Gerçek CLI: exit 0; 41/41 çıktı; 17 JPEG + 24 WebP; 76.797.735 byte.
- 82 child ile tam raw RGB/metadata ve bağımsız reread doğrulandı.
- 88 eski çalışma dosyası, 41 orijinal, 124 P5c girdisi, yedi P5f kanıtı değişmedi.
- [Sade sonuç](../../be/docs/evidence/p5g-local-server-compatibility/sonuc.md) ve
  [sayısal kanıt](../../be/docs/evidence/p5g-local-server-compatibility/kanit.json).
- Production admission kapalı; üretim JRE/container, fiziksel renk ve tam DB
  suite `NOT_RUN`. Yeni kalıcı politika veya üretim varsayılanı yok.

## İnceleme sonucu

Üç bağımsız katmanda beş benzersiz bulgu `patch` olarak işlendi ve düzeltildi.
Açık bulgu yok. Son sürümde 26 profil ve 12 normal profil testi, gerçek CLI
ve 41/41 corpus yeniden geçti. Donmuş onay bloğu değişmedi. `story_key` yok;
sprint durumu eşlemesi uygulanmadı.

## Suggested Review Order

**Yerel deney girişi**

- Yeni dizinde doğrula, kopyala ve yeniden oku; başarıyı koruma kontrolüne bağla.
  [P5gRunner.java:28](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gRunner.java#L28)

**Piksel ve metadata sınırları**

- Renk dönüşümü yapmadan ham RGB al; aynı codec ile bit tüketimini denetle.
  [P5gRawDecoder.java:20](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gRawDecoder.java#L20)

- TIFF içeriğini koru; orientation ve desteklenmeyen alt IFD bağlantısını reddet.
  [P5gMetadata.java:13](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gMetadata.java#L13)

- RIFF sınırlarını ve yalnız opak kayıpsız VP8L yapısını kabul et.
  [P5gWebp.java:17](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gWebp.java#L17)

- PNG kaynaklarını CRC, geometri ve sınırlı metadata açmayla doğrula.
  [P5gPng.java:16](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gPng.java#L16)

**Süreç ve kanıt koruması**

- Ağ, süre, RSS ve günlük sınırlarını uygula; gerçek çıkışı bekle.
  [P5gSupervisor.java:27](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gSupervisor.java#L27)

- macOS resident size sayacı yoksa başarı üretme.
  [P5gRss.java:30](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gRss.java#L30)

- Kaynak/adayı ayarlar ve gerçek class/JAR/JVM kimliğiyle bağla.
  [P5gIdentity.java:14](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gIdentity.java#L14)

- Önceki dosyaları ve sabit corpus üyeliğini salt okunur SHA ile koru.
  [P5gCorpus.java:54](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gCorpus.java#L54)

**Destekleyici doğrulamalar**

- Gerçek child reddini ve koruma değişikliğinde başarısızlığı sınayan testleri incele.
  [P5gProcessTest.java:151](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gProcessTest.java#L151)

- RIFF içinde gizlenen trailing byte ve raw renk sentinel testlerini incele.
  [P5gCodecTest.java:108](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gCodecTest.java#L108)

- Kapalı profil ve eksik codec durumunu reddet; test ayarını geri yükle.
  [P5gGateTest.java:9](../../be/src/test/java/com/tellpal/v2/asset/reading/p5g/P5gGateTest.java#L9)

- Yeni okuyucuyu yalnız açık test profiline sınırla.
  [pom.xml:239](../../be/pom.xml#L239)

- Yerel sonucu ve üretim kabulünün kapalı kaldığını sade özetten doğrula.
  [sonuc.md:1](../../be/docs/evidence/p5g-local-server-compatibility/sonuc.md#L1)
