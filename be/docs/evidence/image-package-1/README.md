# Paket 1: JPEG/WebP hazırlama kanıtı

Bu çalışma yalnız açık yerel/test P2/P3 profilini kapsar. Üretim hazırlama gate’i,
P4b5 ve normal mobil/CMS tüketicileri açılmadı. Firebase, production DB, deploy ve
push yapılmadı. Orijinaller ve eski aday/kanıt dosyaları yeniden yazılmadı.

Dosya listesi ve kapanış durumu [uygulama raporundadır](implementation-report.json).

## Uygulanan davranış

- `FormatPolicy.PNG_FIT` eski 16 alanlı constructor ve eksik/null JSON alanı için
  varsayılandır. Eski PNG floor-fit davranışı korunur; yeni class/descriptor
  fingerprint’i eski pending işi dönüşümden önce reddeder.
- `PRESERVE_JPEG_OR_LOSSLESS_WEBP` kaynak boyutunu korur; width/height üst sınırdır.
  JPEG aynı byte’larla kopyalanır. PNG kaynak yeni canonical ham RGB PNG üzerinden
  resmi libwebp 1.6.0 `cwebp -lossless -exact -q 100 -m 6 -metadata none` ile encode
  edilir. Tek thread kullanılır. ICC/EXIF/XMP `webpmux` ile eklenir.
- RGB8 ve tüm alpha örnekleri 255 olan RGBA8 kaynaklar kabul edilir. Alpha kanalının
  bulunması tek başına ret değildir; tek bir şeffaf piksel reddedilir. Pinli536 byte
  P3 ICC veya profilsiz RGB kabul edilir. Renk dönüşümü/resize/matte/orientation
  düzeltmesi yapılmaz.
- Bağımsız TwelveMonkeys 3.15.3 raw destination reader tam RGB, ICC, XMP ve normalize
  TIFF eşliğini doğrular. VP8L decoder’ın tükettiği bitlerin byte tavanı tam payload
  sonu olmalıdır. EOF buffer’ında bitOffset 8..64 olması mümkündür; bu durum trailing
  byte kabulü üretmez. Küçük metadata’sız çıktı ve eklenmiş payload byte testi vardır.
- Stored reread yeniden pinli kaynağı okur. Doğru çıktı SHA/MIME/geometrisi tek
  başına yeterli değildir; kaynak RGB/metadata eşliği de gerekir. Eşit veya büyüyen
  JPEG/WebP mevcut açık `allowGrowth` ve dolu evidence kuralından geçer.
- Koruma politikasında guardian, Java worker ve native alt süreçler ayrı `setsid`
  grubuyla sahiplenilir. Guardian worker PID’sini grup durana kadar reap etmez;
  yeni native başlangıcı ve ani Java ölümü ancestry polling’inden bağımsız temizlenir.
  Lifeline kapanışı yalnız bu gruba SIGSTOP/SIGKILL gönderir. Linux subreaper bütün
  orphan çocukları toplar; macOS kernel grubunda çalışabilir üye kalmadığı doğrulanır.
  Guardian çıkmadan kapasite/dizin bırakılmaz. Guardian+Java+native RSS toplamı,
  deadline, disk ve 1 MiB log sınırı uygulanır. RSS örneklemesi sürekli peak ölçümü
  değildir; parent rezervi ayrıdır. Legacy PNG tek Java child ile native dizinsiz çalışır.
- XMP TIFF namespace’indeki orientation attribute/element biçimleri prefix’ten
  bağımsız reddedilir. DTD/dış entity/ağ erişimi kapalı ve metadata sınırı altında SAX
  kullanılır; handler class SHA’sı descriptor’dadır. PNG pHYs yoğunlukları eşit olmalıdır;
  kare olmayan piksel oranı reddedilir. Diğer XMP byte’ları değiştirilmez.

## Çalışan test karşılıkları

[Odaklı çalışma](focused-tests.json) 71 testte 0 failure/error/skip içerir.
Bunların P2/P3 bölümü gerçek PostgreSQL 15.19 ve yerel disk storage fixture’larıyla
çalışır. Daha sonra eklenen metadata ve native süreç ağacı sınırı testleri final
codec ve tam verify kaydında ayrıca yer alır.

[Nihai tam verify](full-verify-summary.json) **BUILD SUCCESS**: Surefire 587 test,
0 failure/error, 26 skip; Failsafe 36 test, 0 failure/error, 2 skip. Toplam **595** test
çalıştı. Codec 11, süreç sahipliği 2, CI guard 1, eski süreç 12, P2 preparation 18,
guard 10 ve P3 publication 22 testinde skip yoktur. [Matris](matrix-verification.json)
ve [beş bulgunun sonuç kaydı](review-patches-result.json) geçen testleri gösterir.

28 skip açık gate nedeniyledir: eski P5g codec/process/corpus 23 testinde
`p5g.local.enabled` yoktur; üç Paket 1 encode/annotation-reread/patch-reread testi
özel parametreler olmadan varsayılan koşuda kapalıdır. Bu üçü ayrı gerçek koşularında
birer test ve 0 skip ile geçti. P4b5’in iki entegrasyon testi için izole sentetik/
nihai JDBC seçilmedi. Eski telefon ve benchmark deneyleri tekrar açılmadı.

[Önceki binding sonrası verify](before-review-full-verify-summary.json) 590 çalışan
test içerir; tarihsel kayıttır. [Son JAR karşılaştırması](review-jar-identity.json),
mutanttan geri konan gerçek loop’u ve probe edilen JAR’ın verify JAR’ıyla byte eşliğini
doğrular. [Üç ayrı kimlik](review-final-identities.json) ilk 41 üretimi, son bağımsız
41 reread’i ve son Linux ARM64 JAR’ını; C/derleyici/binary SHA’larını ayırır.

[Bağımsız kök koruma denetimi](root-protection-audit.json), eski 88 çalışma dosyası,
41 orijinal, 124 P5c ve 7 P5f kanıtının SHA eşliğini doğrular. Spec’in donmuş
bloğu [son patch sonrası denetimde](review-protection-audit.json) de
[kayıtlı SHA](frozen-block-sha256.txt) ile aynıdır:
`1dd8d666e5143c9aefee3f1c06b54f0ec5b6d570a4919265bb1373a957bbad5d`.

| Spec matrisi | Test karşılığı |
|---|---|
| JPEG aynı byte; PNG’den yeni WebP; tam RGB+metadata | `realChildCreatesNewWebpAndCopiesJpegWithExactRawProof`, `preservationPolicyCompletesMixedJpegWebpAndSourceBoundStoredReread`, `Package1CorpusTest` |
| Eski constructor/JSON PNG fit; yeni kimlik; dönüşüm öncesi drift ret | `legacyConstructorAndMissingOrNullJsonPolicyKeepPngFit`, `oldPngEncoderIdentityGetsDistinctWorkAndIsRejectedBeforeConversion`, eski gerçek child testleri |
| Yanlış MIME/SHA/RGB/ICC/XMP/TIFF, alpha/animasyon/orientation/trailing ret | `persistedWrongMimeShaDimensionsAndEncodedLimitCannotVerify`, `storedRereadBindsSourceEvenIfCandidateShaAndDimensionsAreValid`, `transparentAlphaAnimationOrientationUnknownIccAndTrailingAreRejected`, `xmpAndNormalizedTiffMustSurviveByteForByte`, `webpPayloadTrailingDataAndPixelMutationCannotPass` |
| Byte/metadata/piksel/parent bellek/süre/RSS/disk/log sınırı ve gerçek exit | `preservationSourcePixelAndMetadataAllocationsAreBounded`, `realChildPreservesEveryRgbPixelAndCompressesAgainstUncompressedControl`, `parentHeapAdmissionRejectsUnboundedReservationBeforeChildLaunch`, `deadlineAndTotalNativeRssDiskLogLimitsObserveEntireProcessTreeExit` |
| Kayıp/bozuk araç, partial upload/retry/stale attempt; atomik manifest | `missingOrChangedToolsFailClosedAndLegacyNeedsNoTools`, `missingBinaryAfterCaptureRejectsBeforeConversion`, `restartAfterPartialObjectsReverifiesAndCompletesWithoutPartialReady`, `explicitRetryAndLateTerminalCallbacksRemainFenced`, PostgreSQL guard/rollback testleri |
| Eşit/büyük çıktı için açık growth kanıtı | `jpegCopyEqualityNeedsExplicitGrowthEvidenceAndRetryKeepsAtomicManifest`, mevcut eşit/büyüyen PNG testleri |
| P3 yeni WebP LIVE; başarısız replacement eski LIVE’ı korur | `preservationPolicyAdoptsNewWebpAndFailedCandidateKeepsPreviousLive` |
| Üretim gate’i kapalı | `sourceByteLanguageChangesAndInvalidOrProductionInputsFailClosed`, mevcut P3 production guard testleri |

## 41 kaynak encode ve final sınıflarla yeniden okuma

[İlk tam üretim sonucu](corpus-result.json) ve [çalışan test](corpus-tests.xml): **PASS**,
17 JPEG byte kopyası ve 24 yeni WebP. Bütün kaynaklarda tam ham RGB/metadata eşliği,
source bağlı bağımsız stored reread ve gerçek child exit doğrulandı. 24 PNG’nin
hepsinde pinli ICC, 41 kaynağın hepsinde XMP ve TIFF vardı; bunlar korunarak geçti.

| Kaynak/çıktı | Adet | Kaynak byte | Çıktı byte |
|---|---:|---:|---:|
| JPEG → aynı JPEG |17|6.191.423|6.191.423|
| PNG → yeni WebP |24|120.305.904|70.606.312|
| Toplam |41|126.497.327|76.797.735|

41 işin toplam wall süresi 468.711 ms oldu. Bu süre child başlatma, encode, decoder
ve bağımsız stored doğrulamayı içerir; saf encode süresi veya benchmark değildir.
Kaynak/geometri/kalite kabulü genelleştirilmez.

Ham kaynak ve çıktılar Git dışındaki yeni özel `corpus-final` dizinindedir;
konumu `/private/tmp/tellpal-package1-run-path` işaretçisi belirler. İlk kısmi koşu
final descriptor sabitlenince kontrollü child exit ile durduruldu ve ayrı tutuldu;
encode kanıtı yalnız kayıtlı 41 koşusudur. Orijinaller 41, P5c 124 ve P5f 7 dosyada
önce/sonra SHA eşitliği testin başarısının zorunlu koşuluydu.

## Constructor binding düzeltmesinin kimliği

İlk tam verify, eski 16 alanlı yerel config’in Spring binding’inde
`SYNTHETIC_PROFILES_REQUIRED` regresyonunu buldu. Canonical record constructor’ına
`@ConstructorBinding` eklendi. Mevcut config 9 ve codec 9 hedefli testinin 18’i geçti.
[JAR payload karşılaştırması](binding-identity.json), yalnız `TestProfile.class`
annotation metadata’sının değiştiğini gösterir. Constructor talimatları/imzaları,
bütün diğer class/JAR payload’ları ve codec/process/worker/native ayarları bu tarihsel
annotation düzeltmesinde aynıdır. Sonraki inceleme yamaları için bu iddia kullanılmaz.

41 çıktı tekrar encode edilmedi. [Final sınıflarla bağımsız yeniden okuma](annotation-reread-result.json)
41 kaynak/çıktı çiftinde tekrar PASS verdi. Test, iki tam descriptor arasında yalnız
`TestProfile` class SHA farkına izin verir; tam RGB ve ICC/XMP/normalize TIFF eşliği
ile kaynak/çıktı SHA’larını yeniden doğrular. `corpus-result.json` ilk encode profil
kimliğini korur. Yeni reread kimliği ayrı kaydedilir; eski manifest/fingerprint yeni
üretim kimliği olarak yeniden etiketlenmez.

## İnceleme düzeltmeleri

| Bulgu | Gerçek test karşılığı |
|---|---|
| R1 süreç sahipliği | `ReadingProcessOwnershipTest`: geç spawn ve ani worker ölümü; bütün PID’ler çıkar, ilgisiz süreç yaşar, dizin/kapasite ancak sonra bırakılır. Linux probe aynı hataları PID1 altında da çalıştırır. |
| R2 XMP orientation | Namespace alias’ları, attribute/element ve orientation 1/6 ret; DTD/XXE ret; diğer XMP ve 41 gerçek kaynak korunur. |
| R3 pHYs aspect | Birim 0/1 için eşit yoğunluk kabulü; farklı yoğunluk açık ret. |
| R4 CI zorunluluğu | [Yerel YAML/komut denetimi](ci-native-check-result.json); `requiredMarkerWithoutToolsFailsRatherThanSkipping`. Remote CI çalıştırılmadı. |
| R5 descendant RSS | 192 MiB toplam sınırında Java worker tek başına sınır altında; 140 MiB tahsis ayrı OS alt JVM’sindedir. [Parent-only mutant](parent-rss-mutant-result.json) testi düşürür; [özgün loop](review-targeted-tests.json) geçer. |

[Son parser/supervisor ile 41 bağımsız reread](review-patch-reread-result.json) ve
[çalışan test](review-patch-reread-tests.xml) ilk üretim fingerprint’ini korur;
`newEncoding=false`. Encoder, ham RGB/core class’ları, decoder JAR’ları ve iki WebP
binary kimliği ilk üretimle aynıdır. Orijinal 41 üretim, annotation reread ve bu son
reread ayrı kayıtlardır. Orientation/pixel-aspect nedeniyle reddedilen gerçek kaynak yoktur.

## Paketli Linux/JRE

[Gerçek executable JAR probe’u](PackagedPreservationProbe.java), aynı deployment
`eclipse-temurin:25-jre` üzerinde gerçek `ReadingImageProcess` ve paketli child’ı
çalıştırdı. [Linux arm64 sonucu](linux-arm64-runtime.json) ve özel dizindeki ham log
PASS: Temurin 25.0.4.1+1-LTS/aarch64, yeni WebP, byte-copy JPEG ve eski 40×30 PNG fit.
[JAR SHA-256](jar-sha256.txt) test edilen binary’yi belirtir. Profil gerçek motor
class’ları, XMP handler’ı, decoder JAR’ları, JVM, iki WebP binary’si ve guardian
SHA/sürüm/derleyici/flag kimliğini içerir. [Koşu receipt’i](review-linux-runtime-receipt.json)
son JAR ve container sınırlarını kaydeder. Linux PID1 altında geç spawn ve ani worker
ölümü testlerinde bütün sahipli PID’ler çıktı; ilgisiz süreç yaşadı. RSS fault’ında
Java worker 77.880 KiB ile 192 MiB altında kaldı; alt süreç 133.008 KiB olduğunda
yalnız aggregate sınırı aşıldı ve iki PID de çıktı.

Container ağsız ve root filesystem salt okunurdu; bir CPU, 1 GiB bellek/swap,
128 PID ve 128 MiB `/tmp` tmpfs kullanıldı. Probe class’ı salt okunur test image’ına
paketlendi; Colima özel `.codex` dizinini bind mount etmediği için çıktı yalnız bu
çalışmaya ait geçici Docker volume’ünden özel host dizinine kopyalandı. Ardından
container ve volume silindi. Bunlar sentetik test sınırlarıdır; üretim bütçesi değildir.

[Resmi arşivler ve SHA’lar](archives.json), kurulum betiği ve runtime kontrolünde
pinlidir. [Linux arm64 araç kurulumu](linux-arm64-tools-result.json) geçti. İlk AMD64
emülasyon denemesi (özel ham log), arşiv SHA doğrulamasından sonra tar
çıktı dosyası açılışında `Function not implemented` ile durdu. Betik yalnız iki sabit
üyeyi `tar -xzOf` ile stdout’tan yazarak bu emülasyon yolunu atlattı; arşiv/binary
byte’ları değişmedi. [Son AMD64 araç kurulumu](linux-amd64-runtime.json) geçti.

İlk üretim JAR’ıyla alınan 20 sn profilli [AMD64 JAR probe’u](linux-amd64-runtime.json), emülasyonda
`CHILD_TIMEOUT` verdi. AMD64 runtime PASS sayılmadı ve ek deneme açılmadı. Native
ARM64 kanıtı esas alınır; AMD64 için üretim/runtime yeterliliği çıkarılamaz.
Bu inceleme yamalarında AMD64 yeniden derlenmedi veya çalıştırılmadı.

## Kapsam sınırları

Yerel 41 kaynak profili 16 MiB girdi/çıktı, 5 M piksel, 256 MiB parent rezervi,
256 MiB heap + 32 MiB direct, 512 MiB Java+native RSS, 128 MiB disk, 60 sn/iş,
30 dk/revision, 1 MiB log ve tek thread/iş kullanır. Growth kanıtı yalnız bu yerel
corpus içindir; sayısal değerler üretim varsayılanı değildir.

SDK-only storage akışının mutlak in-flight deadline açığı devam eder. Soket
inactivity timeout’u sürekli ilerleyen slow-trickle okumasına mutlak süre sınırı
sağlamaz. Fiziksel Android/iPhone kabulü, genel tüketici geçişi ve üretim profil/
kapasite/dağıtık çalışma yeterliliği bu kanıttan çıkarılamaz.

Ham `.log` dosyaları commit’li artifact değildir. Özel run dizininde saklanır;
[log SHA kaydı](raw-log-hashes.json) dosya adını, boyutunu ve SHA-256 değerini tutar.
