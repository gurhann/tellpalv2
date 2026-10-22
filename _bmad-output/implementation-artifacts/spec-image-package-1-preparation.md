---
title: 'Paket 1: Gerçek JPEG/WebP hazırlama hattı'
type: feature
created: '2026-10-22'
status: done
baseline_commit: a6215ba7c4ca99759a0662ddf76c4be6b50d7493
review_loop_iteration: 0
context:
  - '{project-root}/standards/commenting-standard.md'
  - '{project-root}/be/docs/adr/ADR-0022-reading-image-preparation-engine.md'
---

<frozen-after-approval reason="2026-10-22 kullanıcı planı uygulamayı onayladı">

## Amaç

Onaylı kapanış planının Paket 1'ini gerçek local/test P2/P3 hattında uygula.
Eski PNG fit davranışını koru. Yeni açık politika JPEG'i aynen kopyalasın;
PNG/JPEG yeni yüklemeler için kayıpsız WebP üretimi hazır adaydan bağımsız çalışsın.

## Sınırlar

**Her zaman:** Tek typed format politikası; eski 16 argümanlı TestProfile ve
JSON geriye uyumlu PNG varsayımı. Koruma modu kaynak boyutunu korur;
width/height üst sınırdır. Ham RGB, ICC/XMP/normalize TIFF eşliği admission,
child, worker ve kaynak bağlı bağımsız stored reread boyunca doğrulanır.
P5g'nin strict parser/raw destination/tam VP8L tüketimini genelleştir.
Yalnız opak RGB8; bilinen pinli P3 ICC veya profilsiz RGB; başka semantik açık ret.
Eşit/büyüyen çıktı mevcut allowGrowth+dolu evidence kuralından geçer.
Profilde gerçek class/JAR/JVM, iki binary SHA/sürüm ve bütün ayarlar bulunur.
Deadline/disk/log ve Java+native toplam RSS, tek iş ve gerçek kill+exit korunur.

**Önce sor:** Yeni storage mimarisi, canlı erişim veya kapsam genişletme.
**Asla:** Canlı/Firebase/production DB/deploy/push; orijinal ve eski kanıt yazımı;
renk dönüşümü, resize/matte/orientation düzeltmesi; P4b5 açma; mobil/CMS değişikliği.
Production gate kapalı kalır; SDK-only mutlak storage deadline açığı saklanmaz.

| Durum | Davranış |
|---|---|
| JPEG koruma / PNG→WebP | Aynı JPEG byte; WebP tam ham RGB+metadata eşit |
| Eski profil/JSON | Eski PNG fit; yeni fingerprint; drift dönüşümden önce ret |
| Yanlış MIME/SHA/RGB/ICC/XMP/TIFF, alpha/animasyon/orientation/trailing | Açık ret; READY yok |
| Kaynak/çıktı/metadata/piksel/süre/RSS/disk/log sınırı | Sınırlı tahsis; süreç ağacı durur; eski LIVE korunur |
| Kayıp araç/bozuk araç/partial upload/retry/stale attempt | Başarı yok; atomik manifest ve fencing korunur |

</frozen-after-approval>

## Kod haritası

- `be/src/main/java/com/tellpal/v2/asset/api/ReadingImagePreparationApi.java`:
  TestProfile'a policy ekle; eski constructor ve null JSON varsayımı korunsun.
- Aynı `asset` altında `domain/reading/ReadingProfiles.java`, `ReadingWork.java`;
  `infrastructure/media/reading/ReadingImage{Inspector,Child,Process}.java`;
  `infrastructure/processing/reading/ReadingImageWorker.java`;
  `application/reading/Reading{SourceAdmissionService,ImagePreparationService,PublicationReadyService}.java`:
  PNG MIME/path, geometri, byte kabulü ve source bağlı reread aynı politikayı kullansın.
- `be/src/test/java/com/tellpal/v2/asset/reading/p5g/`: pinli parser/raw decoder
  mantığını main'e genelleştir; deney gate/1712x2778 sabitlerini taşıma.
- Encoder resmi libwebp 1.6.0 cwebp/webpmux; `-lossless -exact -q 100 -m 6`,
  tek thread. Raw RGB'den profilsiz canonical PNG yaz; kaynağı cwebp'e doğrudan
  verme. Metadata none encode; webpmux ile ICC/EXIF/XMP byte'larını ekle.
  Bağımsız TwelveMonkeys3.15.3 raw reader son dosyayı tam doğrulasın.
- Resmi paket/sürüm/SHA kaydı `/private/tmp/tellpal-package1-dependencies-path`
  işaretçisindeki `archives.json`; çalıştırılabilirler `libwebp-1.6.0-mac-arm64/bin/`.
  Tool directory environment/system property ile; child'a açık aktar; legacy
  profil araç gerektirmesin. Kaybolan/değişen binary ret üretsin.
- `be/Dockerfile`: aynı deployment JRE; Linux arm64/amd64 resmi SHA-pinli
  araçları paketle; production activation yok. Runbook yalnız bu değişikliği kaydetsin.

## İşler ve kabul

- [x] API/profil/manifest ve main preservation codec: eski PNG yolunu koru;
  metadata, geometri, MIME, identity ve tam eşlik doğrulamasını bağla.
- [x] Asıl child/process/worker: gerçek encode, değişmez MIME/yol, bağımsız
  kaynak eşliği ve native descendant kaynak/ölüm kontrolünü uygula.
- [x] `be/pom.xml`, `be/Dockerfile`, pinli araç kurulum betiği: reader/runtime
  bağımlılığını açık paketle. P5g eski classpath-yok assertion'ını yeni kapsama uyarla.
- [x] `be/src/test/java/com/tellpal/v2/asset/reading/`: matris, gerçek encoder,
  PostgreSQL/P3 regresyonu ve 41 gerçek kaynağın yeni çıktısını doğrula.
- [x] `be/docs/evidence/image-package-1/`, ADR-0022 ve proje hafızası:
  sade sonuç, gerçek test/runtime kanıtı ve açık üretim sınırlarını kaydet.

**Kabul:** Given yeni yükleme, when ana motor işler, then gerçek JPEG/WebP çıktı
bağımsız reread ile tamamlanır. Given eski profil, when aynı API kullanılır,
then mevcut PNG fit ve gate davranışı korunur. Given üretim profili, when çağrılır,
then kapalı kalır. Given hata, then yanlış READY veya eski LIVE kaybı oluşmaz.

## Plan değişiklikleri

Kullanıcı üç paketi ve ilk olarak Paket 1'i uygulamayı onayladı; yeniden onay
istenmez. Eski 88 kirli çalışma dosyası korunur. Paket 2/3 bu spec'e eklenmez.

Tam verify eski config binding regresyonunu buldu; canonical constructor
`@ConstructorBinding` ile düzeltildi. Yalnız annotation class kimliği değiştiği
JAR payload karşılaştırmasıyla doğrulandı. İlk 41 encode kimliği korunarak final
sınıflarla bağımsız tam RGB/metadata reread tekrar geçti. Final tam verify ve
native Linux ARM64 paketli JAR probe geçti; AMD64 emülasyon runtime timeout
nedeniyle yeterli sayılmadı. Ayrıntılar `be/docs/evidence/image-package-1/README.md`.

Step 4’te kabul edilen beş patch tamamlandı: OS guardian sahipliği, XMP orientation,
pHYs aspect, CI native zorunluluğu ve gerçek descendant RSS/mutant testi. Son tam
verify ve Linux ARM64 encode/fault probe geçti. Encoder/ham RGB çekirdeği aynı
kaldığından eski 41 üretim kimliği korunarak son parser/supervisor kimliğiyle 41
bağımsız reread yapıldı. Üç kimlik `review-final-identities.json` içinde ayrıdır.

## Doğrulama

Wrapper ile hedefli gerçek codec/process/DB testleri, sonra `./mvnw verify`.
Yerel Colima/PostgreSQL; Firebase kapalı. Paketli deployment JRE/Linux testi.
P5c girdileri ve orijinaller önce/sonra SHA korunur; çıktılar yeni özel dizinde.
41 kaynak için 17 byte-copy/24 yeni WebP, tam RGB/metadata; ham medya Git dışında.
Yerel corpus sınırları açık: 16MiB girdi/çıktı, 5M piksel, 256MiB parent rezerv,
256MiB heap+32MiB direct, toplam512MiB RSS,128MiB disk,60sn/iş,30dk/revision,
1MiB log, tek thread/iş; açık yerel growth evidence. Üretim bütçesi değildir.

## Yerel çalışma girdileri

Colima hazır: `DOCKER_HOST=unix:///Users/gurhankucuk/.colima/tellpal-image-local/docker.sock`,
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`. Varsayılan context değişmedi.
`/private/tmp/tellpal-package1-baseline.json` koruma kaydıdır; preExisting içinde
yalnız kabul edilen kapanış önerisi bu işte değişebilir; diğer 88 dosya korunur.
Yeni özel çıktı dizini `/private/tmp/tellpal-package1-run-path` işaretçisindedir.

## Önerilen inceleme sırası

**Giriş ve çıktı politikası**

- JPEG kopyası ile yeni WebP üretiminin açık politika sınırı.
  [ReadingImagePreparationApi.java:54](../../be/src/main/java/com/tellpal/v2/asset/api/ReadingImagePreparationApi.java#L54)

- Ham RGB üzerinden yeni WebP ve metadata paketleme.
  [ReadingPreservationEncoder.java:16](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingPreservationEncoder.java#L16)

**Bütünlük ve süreç sahipliği**

- İşe ait süreçlerin tamamı çıkmadan kapasite bırakılmaz.
  [ReadingProcessSupervisor.java:9](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingProcessSupervisor.java#L9)

- Native süreç grubu ve parent lifeline koruması.
  [reading-process-supervisor.c:63](../../be/tools/reading-process-supervisor.c#L63)

- Worker kaynağa bağlı bağımsız çıktı doğrulamasını uygular.
  [ReadingImageWorker.java:17](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/processing/reading/ReadingImageWorker.java#L17)

**Doğrulama ve çalışma ortamı**

- Eksik araç CI testini sessizce atlayamaz.
  [ReadingNativeTestTools.java:8](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingNativeTestTools.java#L8)

- 595 test, gerçek corpus ve Linux kanıtının kısa kaydı.
  [README.md:1](../../be/docs/evidence/image-package-1/README.md#L1)
