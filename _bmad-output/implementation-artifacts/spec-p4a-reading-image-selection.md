---
title: 'P4a1: okuma görsellerinde manifest ve geometri seçimi'
type: 'feature'
created: '2026-10-17'
status: 'done'
baseline_commit: '79b6cf4e64da7a41efc9185d4c003c412c0d63fe'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p4-preimplementation-investigation.md'
---

<frozen-after-approval reason="kullanıcının onaylayacağı sınırlı hazırlık kapsamı">

## Amaç

**Sorun:** Mobil kodda P3 kimliğine ve iki eksenli ihtiyaca göre rendition seçimi yoktur.

**Yaklaşım:** Saf Dart manifest doğrulayıcısı ve seçici ekle. Gerçek karşılaştırma P4a2, kaynak/yaşam döngüsü P4b olarak inceleme belgesindedir. Bu hazırlık teslimat veya tamamlanmış P4 değildir.

## Sınırlar

**Her zaman:** content/dil/READING/revision/digest, kaynak/profil/rol ve sıralı sayfa bütünlüğü. Geometri ve kabul edilmiş profiller açık girdidir. Descriptor kontrolü byte doğrulaması veya erişim kararı değildir. Domain Flutter/HTTP/Firebase'den bağımsızdır; AGENTS ve Flutter skill'i uygulanır.

**Önce kararlaştır:** P4a2 erişim/teslim/cache/session, kaynak/profil ve bulut çalıştırması; bu hazırlık bunlara bağlı değildir.

**Yapma:** Normal tüketici, ağ/storage/cache/UI/ses/auth değişikliği; üretim ayarı, original fallback, upscale, IPC veya retry katmanı. `.memlog.md` ve süreçleri koru.

## Girdi ve sınır durumları

| Verilen / işlem | Beklenen |
|---|---|
| Tam descriptor ve beklenen bağlam doğrulanırsa | Salt okunur doğrulanmış descriptor döner; erişim izni dönmez. |
| Rol/dil/revision/profil/kaynak/sayfa uyuşmazsa | Tipli hata; eksik kümeyi kısmi başarı sayma. |
| Pozitif slot/DPR ve contain/cover seçilirse | İki eksenli kırpılmamış fiziksel ihtiyaç hesaplanır. |
| Kabul edilmiş adaylar iki ekseni karşılarsa | En küçük piksel alanı; eşitlikte byte ve rendition kimliği. |
| Kaynak, çıktı veya açık decode bütçesi yetmezse | Ayrı shortage/resource hatası; original seçilmez. |

</frozen-after-approval>

## Kod haritası

- `app/mobile/lib/features/image_experiment/domain/entity/raster_case.dart:41`: mevcut geometri örneği; deney sayıları taşınmaz.
- `be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingWork.java:63`: manifest metadata ve floor-fit kuralı; salt okunur dayanak, Java tipi Flutter'a taşınmaz.

## Görevler ve kabul

Yollar `app/mobile/` altındadır; görevler sırayla uygulanır.

- [x] `lib/features/reading_images/domain/entity/reading_image_manifest.dart`: URL'siz immutable manifest/rol/kaynak/rendition/bağlam; collection'ları kopyala.
- [x] `lib/features/reading_images/domain/use_case/validate_reading_image_manifest.dart`: `Either` ile bağlam, tam rol/sayfa, pozitif metadata, hash ve kaynak/profil eşleşmesini doğrula.
- [x] `lib/features/reading_images/domain/use_case/select_reading_image_rendition.dart`: finite pozitif geometri, iki eksenli seçim, oranı koruyan decode, açık piksel bütçesi ve shortage reddi.
- [x] `test/reading_images/reading_image_manifest_test.dart`: yukarıdaki eksik/yanlış bağlam ve bütünlük durumlarını doğrula.
- [x] `test/reading_images/reading_image_selection_test.dart`: tall/wide, cover/contain, fractional DPR, pencere değişimi, eksik tek eksen ve fallback reddi; bağımsız hesapla sına.
- [x] `be/docs/evidence/p4-reading-images/verification.md`: çalıştırılan sonuçlar, kapsam ve mevcut cihaz yokluğunu kaydet.

**Kabul:** Geçerli descriptor verildiğinde seçim yapılırsa yalnız eşleşen uygun aday döner. Alan/DPR değişip yeniden hesaplanırsa yeni ihtiyaç kullanılır. Hatalı metadata doğrulanırsa tipli red döner.

## Spec değişiklik kaydı

## Tasarım notu

P2 rol başına tek aday üretir; selector bunu seçer veya yetersiz bulur. Merdiven eklenmez. Minimum seçim kalite onayı değildir. İhtiyaç kırpılmaz; decode ayrı sınırlanır. Gizli sayısal bütçe yoktur.

## Doğrulama

`app/mobile/` altında `flutter analyze`, `flutter test`; mevcut Library/detail/reader regresyonları korunur. Gerçek hikâye/transfer/decode/görüntü/kaynak kanıtı P4a2'de açıktır; mock/derleme bunun yerine geçmez. Koddan sonra BMad bağımsız incelemeleri tamamlanır.

## Önerilen inceleme sırası

**Kimlik ve metadata sınırı**

- Tam beklenen bağlam doğrulanmadan seçiciye descriptor verilmez.
  [validate_reading_image_manifest.dart:29](../../app/mobile/lib/features/reading_images/domain/use_case/validate_reading_image_manifest.dart#L29)

- Çıktı metadata kümesi P2 canonical manifest digest kimliğine bağlanır.
  [reading_image_manifest_digest.dart:9](../../app/mobile/lib/features/reading_images/domain/entity/reading_image_manifest_digest.dart#L9)

**Geometri ve seçim**

- Uygun aday iki ekseni karşılar; minimum alan, byte ve kimlikle seçilir.
  [select_reading_image_rendition.dart:41](../../app/mobile/lib/features/reading_images/domain/use_case/select_reading_image_rendition.dart#L41)

- Exact kesirler ihtiyaç ve decode hesabındaki fazladan piksel hatasını önler.
  [select_reading_image_rendition.dart:139](../../app/mobile/lib/features/reading_images/domain/use_case/select_reading_image_rendition.dart#L139)

**Destekleyici tipler ve kanıt**

- Kopyalanan koleksiyonlar doğrulama sonrasında metadata değişimini engeller.
  [reading_image_manifest.dart:129](../../app/mobile/lib/features/reading_images/domain/entity/reading_image_manifest.dart#L129)

- Bağımsız Java golden ve metadata değiştirme testleri digest bağını sınar.
  [reading_image_manifest_digest_test.dart:13](../../app/mobile/test/reading_images/reading_image_manifest_digest_test.dart#L13)

- Raporlanmış yuvarlama sınırları ve bağımsız seçim oracle sınanır.
  [reading_image_selection_test.dart:54](../../app/mobile/test/reading_images/reading_image_selection_test.dart#L54)

- Aynı bozuk context ve manifestin reddi, eşitlik kontrolünden bağımsız sınanır.
  [reading_image_manifest_test.dart:165](../../app/mobile/test/reading_images/reading_image_manifest_test.dart#L165)

- Son testler ve açık kalan gerçek cihaz sınırları burada kaydedilir.
  [verification.md:29](../../be/docs/evidence/p4-reading-images/verification.md#L29)
