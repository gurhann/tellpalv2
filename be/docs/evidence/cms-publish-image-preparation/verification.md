# CMS Yayınla görsel hazırlığı doğrulaması

2026-10-23. [Onaylı spec](../../../../_bmad-output/implementation-artifacts/spec-cms-publish-image-preparation.md).
Kapsam yalnız yerel/testtir. Firebase, canlı DB/storage, dağıtım veya push yapılmadı.

## Sonuç

- İlk uygulamada `be/`: `bash ./mvnw verify` başarılı. Surefire: 603 test, 0 hata, 26 opt-in skip.
  Failsafe: 42 test, 0 hata, 3 opt-in skip. Toplam 616 test çalıştı.
- Yeni yayın, admin önizleme, JPEG/WebP ve gerçek CMS kabul testlerinde skip yoktur.
  Atlananlar mevcut P5g/corpus ve ayrı bulut/loopback fixture'larıdır; bu kabulün yerine sayılmadı.
- Native yol resmi SHA-pinli libwebp 1.6.0 `cwebp`/`webpmux` ve derlenmiş guardian ile çalıştı.
  `TELLPAL_READING_NATIVE_TESTS_REQUIRED=true` kullanıldı. Yalnız sahipli Colima `tellpal-image-local` kullanıldı.
- Review yaması sonrası `cms/`: `npm run build` başarılı; `npm run test -- --run`: 92 dosya, 400 test başarılı.
- Değişen yüzey: `npm run test:e2e:visual -- --grep 'content detail visual'`: 8/8 başarılı.
  Kart ve dialog 1280/1440, sabit oturum/zaman, light ve reduced-motion ile karşılaştırıldı.
- Gerçek Chrome kabulü normal CMS PNG sayfa ve JPEG kapak yükleme/save, tek Publish,
  otomatik scheduler/worker ile LIVE, iki görselin decode'u, manifest byte eşleşmesi, klavye,
  dirty başlık/dil korunması ve sonraki metin kaydında aynı işin kullanımını doğruladı.
  Gerçek Spring/PG/Flyway kullanıldı; yalnız storage leaf yerel disk ve admin hesabı sentetiktir.
  Manuel admit/rebind, hazırlama endpoint'i veya testin elle worker/reconcile çağrısı kullanılmadı.

## Matris → çalışan test

Kısa adlar `ReadingPublicationIntegrationTest` ve `ReadingPublicationAdminIntegrationTest` sınıflarındadır.

| Matris satırı | Doğrulama |
| --- | --- |
| Normal yayın | `normalMultipartUploadSaveAndSinglePublishProduceVerifiedPreviewAndPreserveOriginals`; `cmsUploadsBothImagesPublishesOnceAndDecodesThePreparedDialog` |
| Tekrar / metin değişikliği | `rawSourcesAreAdmittedOnceAndRestartReusesAcceptedBindingsWithoutStorageIO`; `metadataOnlyEditReusesJobAndLastPageRemovalExplicitlyInvalidatesWhileRetainingLive`; gerçek CMS kabulünde aynı job karşılaştırması |
| İlk hata | `firstCorruptUploadCannotExposePreparedImagesAndRequiresExplicitRetry` |
| Başarısız yeni aday | `failedUnsupportedReplacementRetainsLiveAndExplicitRetryDoesNotSpin`; `preservationPolicyAdoptsNewWebpAndFailedCandidateKeepsPreviousLive`; publication-actions retained durum testi |
| Yarış / kesinti | `admissionCannotOverwriteConcurrentSelectionOrEligibility` (edit/archive/deactivate/profile, 4 koşu); `restartAfterExpiredDispatchLeaseRecoversIntentAndRejectsOldAttachment`; `admissionOrphanEvidenceSurvivesInterruptedCreateAndPinnedSourceCorruptionFailsClosed` |
| Önizleme güvenliği | `normalMultipartUploadSaveAndSinglePublishProduceVerifiedPreviewAndPreserveOriginals`: yanlış dil/rol/sayfa/key/nesil, yetkisiz istek, kaynak kolu, bozuk byte ve `private,no-store`; `authenticatedCapabilityIsExplicitDisabledByDefaultAndLanguageValidationIsFinite` |
| CMS yaşamı | reading-publication-preview testleri: kapanışta revoke, geciken byte/iptal, ağ/boyut/decode hatası ve retry, başka dil/eski tuple reddi; form testleri: temiz/dirty kapak ve geç save dönüşü; query dil/AbortSignal testi; gerçek CMS ve 8 visual test |

Orijinal JPEG/PNG byte'ları depoda aynı kaldı. JPEG çıktısı aynı byte ve boyutta korundu;
PNG çıktısı lossless WebP olarak decode edildi. İlk LIVE öncesi yeni önizleme erişimi reddedildi.
`disabledAndProductionAdmissionPreserveLegacyAndNeverPrepare` ve mevcut legacy görünürlük testi geçti.

## Formal review düzeltmeleri

- Değişmemiş kapak/sayfa alanları açık preserve işaretiyle content kilidi altında güncel bağı korur.
  Yeni/dirty seçim uygulanır; alanı göndermeyen eski istemci false davranışını korur.
  `textSavesFromStaleEditorsPreserveAdmittedBindingsAndPendingJobWhileNewSelectionsApply`,
  PREPARING sırasında iki eski ham ID ile metin kaydını, admitted bağları ve aynı pending işi sınar.
  Kabul sayısı artmaz; yeni görsel seçimi işi ayırır. Form testleri temiz/dirty görsel ve metin korumasını sınar.
- Önizleme retry'ı aynı dilin güncel durumunu da alır. `refreshes exact-language status on retry and accepts
  the new eligible LIVE` ile gerçek `PublicationActions` kompozisyonundaki `keeps the parent preview dialog
  open when retry adopts a newer LIVE and closes it on language change` geçti. Dialog açık kalır;
  yalnız yeni doğru kimliğin byte'ı indirilir. Aynı içerik/dilde revision, liveGeneration ve manifestDigest
  uyuşmazlıkları parametrik reddedilir; indirme/çizim yapılmaz.
- `previewRejectsEligibilityLostDuringPinnedOutputRead` iki koşuda gerçek çıktı okuması sırasında
  archive/deactivate çağırır. Admin byte ucu görsel döndürmeden 409 ve `private,no-store` verir.
- `reading-preview-download.test.ts` gerçek API metoduyla tüm identity/role/page/key query alanlarını,
  özel key URL kodlamasını, bearer refresh'i ve aynı AbortSignal ile iptali doğrular.
- Yama sonrası hedefli Maven turu: 9 sınıf, 93 test, 0 hata, 0 skip, `BUILD SUCCESS`.
  Content management/publication, controller/admin HTTP, ReadingPublication ve Modulith sınıfları çalıştı.
  Tam backend suite'i gereksiz tekrarlanmadı. Native araçlar zorunlu kaldı; admin kabulü 6/6 ve gerçek
  CMS Chrome kabulü 1/1 tekrar geçti. Son 8/8 görsel karşılaştırması baseline değiştirmeden geçti.

Ham yama logları dış run'da `review-backend.log`, `review-cms-tests.log`, `review-cms-build.log`,
`review-content-visual.log` ve `review-verification.json` olarak saklandı. Tarayıcı kanıtı
`browser-b1f9cb00-b14f-4d0e-bad3-7aea1d0d973d` altındadır. Test fixture'ının sonraki test için truncate
işlemiyle önceki scheduler turu arasında tek `ContentNotFoundException` logu oluştu; test sonuçları
başarılıdır. Ürün akışında içerik silinmez, deaktive edilir.

## Sınırlar ve kanıt saklama

SDK'nin mutlak in-flight süre açığı, üretim/AMD64 ve fiziksel cihaz yeterliliği bu işte kapanmadı.
Mevcut normal tüketicinin ayrı sınırları korunur; üretim etkinleştirmesi iddia edilmez.

Tam eski görsel koleksiyonunun karşılaştırmasında 17 kapsam dışı ekran/eksik Darwin baseline farkı görüldü.
Bu baseline'lar değiştirilmedi. Yalnız değişen content-detail kartları ve iki yeni dialog baseline'ı tutuldu.

Ham medya, oturum, log ve gerçek tarayıcı ekranları repo dışındaki `cms-auto-publish-642e7bbfa1`
çalışma dizinindedir. `/private/tmp/tellpal-auto-publish-run-path` bu dizini gösterir.
Başlangıçta korunan 88 dosyanın SHA-256 değerleri aynı kaldı. Önceki kanıtlar değiştirilmedi.
