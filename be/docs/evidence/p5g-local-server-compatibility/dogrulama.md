# P5g doğrulama kaydı

## Kapsam ve sonuç

Tarih: 22 Ekim 2026. Baseline commit:
`847fa87e4ff6438bc1cd47094833b5a0bb7bc8b0`.

`p5g-local-codec` profili yalnız test-scope TwelveMonkeys WebP reader
`3.15.3` getirir. Production bağımlılığı, `src/main` kodu ve P2/P3 kuralları
değişmedi. Yeni WebP üretilmedi. 17 JPEG ve P5c'deki 24 mevcut WebP byte
olarak yeni dizine kopyalandı.

Profil testi ve ayrı CLI çalışması **41/41** doğrulandı. Her dosya için ilk
okuma ve kaydedilmiş çıktının bağımsız yeniden okunması olmak üzere 82 child
çalıştı. Her karşılaştırma 1712×2778 görüntünün **14.267.808 RGB byte**
değerini kapsar. JPEG'in kaynak/çıktı SHA'sı aynı; WebP'nin raw RGB, ICC,
XMP ve normalize TIFF içeriği kaynak PNG ile eşittir. EXIF normalizasyonu
yalnız `Exif\0\0` önekini ayırır. Renk dönüşümü veya `getRGB()` kullanılmaz.

P3 sentinel testi, varsayılan WebP okumasının örnekleri değiştirdiğini;
`getRawImageType(0)` destination kullanımının bütün örnekleri koruduğunu
gösterdi. Fiziksel ekran rengi bu adımda ölçülmedi.

VP8L bitstream tüketimi ayrıca **aynı pinli codec** ile sınanır. Primary
reader'ın chunk sonuna ilerlemesi tek başına yeterli sayılmaz. Codec'in
bit okuyucusunda tüketilen son bitin byte sınırı, VP8L payload sonuyla
eşleşmelidir. Bu yerel doğrulama `3.15.3` iç alan adlarına bağlıdır;
alan/API değişimi başarı yerine hata üretir. RGB karşılaştırmasının raw
destination yöntemi korunur. Dayanak:
[pinli reader](https://github.com/haraldk/TwelveMonkeys/blob/twelvemonkeys-3.15.3/imageio/imageio-webp/src/main/java/com/twelvemonkeys/imageio/plugins/webp/WebPImageReader.java),
[pinli bit okuyucu](https://github.com/haraldk/TwelveMonkeys/blob/twelvemonkeys-3.15.3/imageio/imageio-webp/src/main/java/com/twelvemonkeys/imageio/plugins/webp/LSBBitReader.java).

Mevcut P2 inspector çıktısı ayrıca kaydedildi: 17 JPEG için
`JPEG_UNSUPPORTED_METADATA_OR_ORIENTATION`, 24 WebP için
`PNG_OR_JPEG_REQUIRED`. Yerel codec başarısı P2 kabulüne çevrilmedi.

## Çalıştırma ve sınırlar

Bağımlılık çözümünden sonra Maven parent, CLI parent ve child süreçlerin
ağı macOS `sandbox-exec` ile engellendi. Maven offline çalıştı. Spring,
Firebase, DB, Docker, telefon veya deployment başlatılmadı.

Child başına heap 256 MiB, direct memory 32 MiB, tek işlemci, 30 saniye ve
512 MiB RSS sınırı; aynı anda tek iş. Encoded girdi 16 MiB, piksel 5 milyon,
metadata 64 KiB, toplam çıktı 256 MiB, child stdout+stderr toplamı 1 MiB.
Timeout/bellek/günlük ihlalinde gerçek süreç çıkışı beklenir; başarısız
corpus için tamamlanmış manifest üretilmez. Çıktı dizini ve dosyalar
`CREATE_NEW` ile oluşturulur.

RSS, macOS'un mevcut `proc_pid_rusage` API'sinden Java FFM ile örneklenir;
ek native kütüphane kurulmadı. İlk `ps` tabanlı denemeler işletim sistemi
kısıtı nedeniyle başarısızdır ve başarılı kanıt sayılmaz. Son kayıt yalnız
native sayacın çalıştığı denemeye aittir. CLI'da en yüksek örneklenen RSS
**236.568.576 byte (yaklaşık 226 MiB)**, en uzun child **1.454 ms**.
Bu değerler gerçek tepe bellek veya üretim performans yeterliliği değildir.

## Test kanıtı

Profil: `./mvnw -o -Pp5g-local-codec -Dtest=P5g*Test` ve açık
`p5g.report`, `p5g.baseline`, `p5g.repo`, yeni `p5g.output` girdileriyle
`test`. **26 test; hata, başarısızlık ve skip yok.**

| Matris | Çalışan test |
|---|---|
| 17 JPEG, 24 WebP; tam decode/reread/koruma | `all41PinnedFilesPassIndependentChildRereadAndFullPreservation` |
| Yanlış pin/MIME/ICC/RGB/XMP/TIFF | `reportPinAndOutputCollisionFailClosed`, `correctCopyIsImmutableAndRereadRejectsChangedOrMissingOutputAndWrongPin`, `mimeAndTruncationAndTrailingAreRefused`, `rgbIccXmpAndTiffMismatchesAreDistinctFailures` |
| Kayıplı/animasyon/alpha/orientation/bozuk/trailing | `lossyAnimationAlphaVersionAndCorruptHeaderAreRefusedBeforeDecode`, `actualTransparentRasterIsRefused`, `exifPrefixOnlyIsNormalizedAndEveryOrientationIsRefused`, `mimeAndTruncationAndTrailingAreRefused` |
| Encoded/piksel/metadata/açma sınırları | `pixelEncodedAndMetadataLimitsAreRefused` |
| Süre/RSS/gerçek exit/tek iş | `exactOperationalBoundariesRejectOnlyAboveLimit`, `effectiveBudgetsCannotExceedProfile`, `realTimeoutKillsChildAndWaitsForExit`, `realRssLimitKillsChildAndWaitsForExit`, `interruptWaitsForExitAndCapacityIsSingleJob` |
| Çakışma/eksik yanıt/koruma/başarısız corpus | `reportPinAndOutputCollisionFailClosed`, `realNonzeroAndMissingResponseCannotSucceed`, `failureAfterCreateHasDiagnosticAndNoCorpusSuccess`, `changedProtectionPinOrMembershipCannotCreateOutput`, corpus testi |
| Ağ ve günlük bütçesi | `actualNetworkIsDeniedAndLogsAreCapped` |
| Kapalı profil ve eski kabul | `closedPropertyFailsBeforeCodecResolution`, `currentP2StillRejectsP3PngAndWebp` |

Bağımsız üç inceleme tamamlandı. Beş bulgu düzeltildi: test property temizliği,
desteklenmeyen TIFF SubIFDs reddi, VP8L içindeki trailing byte kontrolü,
doğru SHA'lı fakat farklı RGB/XMP/TIFF için gerçek child reddi ve çalışma
sırasında değişen koruma girdisi için negatif test. Sonuncusu yalnız geçici
sentetik dosyayı değiştirir. Orijinal veya eski kanıta yazmaz.

Ek testler: `trailingBytesInsideVp8lAreRefusedDespiteValidRiffAndChunkLengths`,
`realChildRejectsCorrectPinsWithDifferentRgbOrMetadata`,
`changedProtectionAfterStartPreventsSuccessManifest`.

Normal profilde, DB'siz ve ağ engelli **12 test** geçti: `P5gGateTest` (3),
`P5fLocalCompatibilityProbeTest` (4), `ReadingImageProcessTest` (5):
`validCrcAnimationColorAndOversizedMetadataAreExplicitlyRejected`,
`persistedWrongMimeShaDimensionsAndEncodedLimitCannotVerify`,
`parentHeapAdmissionRejectsUnboundedReservationBeforeChildLaunch`,
`canonicalFramingAndRoleSpecificProfileIdentity`,
`storageRejectsWrongVersionMissingTruncatedAndTransactionIo`.
Normal classpath'te yeni SPI'nin bulunmadığı assertion ile doğrulandı.

Gerçek `P5gRunner.main` CLI, profil testinin kaydettiği aynı Java/classpath
ile ayrıca çalıştırıldı: exit 0; yaklaşık 79,30 saniye; 41 çıktı ve
76.797.735 byte. Kaynak/adayı, ayarlar, class/JAR/JVM kimliği ve her dosyanın
fingerprint'i manifestte saklanır.

Tam backend/DB test suite: **NOT_RUN**; onaylı kapsam DB erişimini yasaklar.
Üretim JRE/container: **NOT_RUN**. Bu adımda fiziksel renk onayı:
**NOT_RUN**. Production admission: **NOT_ADMITTED**.

## Kanıtların yeri

Git'teki `kanit.json`, CLI manifestinin SHA'sını, ayrıntılı dosya sonuçlarını,
runtime kimliğini ve test özetlerini taşır. JAR yollarında kullanıcı dizini
kaldırılmıştır; fingerprint'ler değiştirilmemiş özel manifesti referans alır.
Medya, tam classpath, komutlar, Surefire raporları ve ham günlükler Git dışında
`p5g-native-final-b5ffde1cbee9` çalışma dizininde tutulur. Başarısız eski
denemelerin üzerine yazılmadı.

Önce/sonra SHA koruması: 88 eski çalışma dosyası, 41 orijinal, 124 P5c
girdisi ve yedi P5f kanıtı değişmedi. Yerel kabul olumlu; üretim geçişi kapalı.
