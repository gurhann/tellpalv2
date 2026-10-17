# P4a1 manifest ve geometri doğrulaması

Tarih: 2026-10-17, Europe/Istanbul.
Başlangıç commit'i: `79b6cf4e64da7a41efc9185d4c003c412c0d63fe`.
Kapsam: `_bmad-output/implementation-artifacts/spec-p4a-reading-image-selection.md`.

## Uygulanan davranış

- `reading_images/domain` URL taşımayan immutable descriptor, kimlik, rol/bağ,
  sabitlenmiş kaynak metadata'sı, profil ve rendition tiplerini içerir. Listeler ve
  iç içe profil koleksiyonları kopyalanarak değişikliğe kapatılır.
- Doğrulayıcı beklenen content/dil/READING/revision/digest/job kimliğini, kapak/detail
  bağını, tam ve sıralı farklı sayfaları, kaynak eşleşmesini, profil kümesini,
  pozitif metadata'yı, SHA-256 biçimini ve P2 rendition kimliğini denetler.
  Dil, backend'in mevcut `tr/en/es/pt/de` canonical kümesiyle sınırlandırılır.
  `ReadingWork.Manifest` metadata yapısı yeniden kurulur ve P2 canonical SHA-256
  digest'i beklenen kimlikle karşılaştırılır. Çıktı yolu, sürümü, byte sayısı ve
  SHA'sı bu bağ kurulmadan doğrulanmış wrapper üretilmez.
  Boyutlar P2'nin floor-fit kuralına uymalıdır. Eksik descriptor kısmen kabul edilmez.
- Yalnız doğrulayıcının oluşturabildiği doğrulanmış descriptor seçiciye verilir.
  Seçici slot ve DPR'yi her çağrıda yeniden hesaplar. Fiziksel slotun ceil işleminden
  sonra `contain`/`cover` ölçek karşılaştırması ve iki eksenin ceil işlemi BigInt
  kesirleriyle yapılır. İhtiyaç kaynak boyutuna kırpılmaz. Kabul edilmiş profiller arasından
  iki ekseni karşılayan en küçük alan, ardından byte ve rendition kimliği seçilir.
- Decode boyutları da tek exact kesirle hesaplanır ve rendition boyutunu aşmaz. Piksel bütçesi
  zorunlu girdidir. Kaynak, rendition ve decode kaynağı yetersizlikleri ayrı tipli
  redlerdir. Orijinale düşme veya upscale yolu eklenmedi.

## Çalıştırılan kontroller

Komutlar `app/mobile/` altında çalıştırıldı.

| Komut | Sonuç |
|---|---|
| `dart format lib/features/reading_images test/reading_images` | Başarılı. |
| `flutter test test/reading_images` | İnceleme onarımından sonra 52 test başarılı. |
| `flutter analyze` | Başarılı; `No issues found!`. |
| `flutter test` | İnceleme onarımından sonra 392 test başarılı. Mevcut Library/detail/reader ve deney testleri dahil. |

Yeni testler yanlış/eksik kimliği, yanlış kaynak/nesne sürümünü, eksik/yinelenen/sırasız
rolleri ve sayfaları, profil/metadata/hash/MIME/byte/kimlik/floor-fit hatalarını ve
koleksiyonların sonradan değiştirilememesini kapsar. Seçim testleri tall/wide,
contain/cover, fractional DPR, pencere ve DPR değişimi, alan/byte/kimlik sıralaması,
tek eksen eksiği, kaynak yetersizliği, açık decode bütçesi ve fallback reddini kapsar.
96 geometri kombinasyonu bağımsız tamsayı kesri ve exhaustive aday oracle'ıyla sınanır.

İlk analizde yeni dosyalarda yedi süslü parantez biçim uyarısı görüldü; düzeltildi.
SDK'nin `engine.stamp` cache yazısı sandbox dışında kaldığı için ilk tam test denemesi
başlayamadı. İzinli tekrar ve son analiz başarılıdır. Anotasyon değişmediğinden
`build_runner` gerektiren yeni üretim yoktur.

## Bağımsız inceleme sonrası tek onarım turu

İlk sürümde 28 yeni test ve toplam 368 test geçmişti. Bağımsız incelemeler double
yuvarlama hatalarını, maskelenmiş bağ bütünlüğü testlerini, eksik allowGrowth kabul
yolunu, çıktıları digest'e bağlamayan ilk kontrolü ve fazla geniş dil kabulünü buldu.
Bu eksikler aşağıdaki kalıcı regresyonlarla onarıldı. Son analiz öncesi canonical
helper'daki bir süslü parantez uyarısı da düzeltildi.

| Bulgu | Düzeltme ve çalışan test adı | Test sayısı |
|---|---|---|
| F1 | İhtiyaç exact BigInt kesrinden hesaplanır. `F1 exact ihtiyaç sınırı` testleri 1005×670→375×250, 2400×1600→204×136 ve 11×77→9×63 sınırlarını sınar. `F1 BigInt çapraz çarpımları int64 taşmasını önler` büyük alanlarda geometri ve sıralamayı sınar. | 4 |
| F2 | Decode exact kesirden hesaplanır. `F2 exact decode 218x218 tam bütçede kabul edilir` 47524 pikseli kabul eder. `F2 exact decode 218x218 bütçeden tek piksel fazla ise reddedilir` 47523 bütçesini reddeder. | 2 |
| F3 | `F3 aynı bozuk descriptor ve context incompleteBindings` aynı bozuk listeyi her iki girdiye verir: duplicate pageId, sırasız pageNumber, yanlış sayfa rolü ve farklı cover/detail kaynağı. Doğrudan `incompleteBindings` beklenir. | 4 |
| F4 | `F4 allowGrowth false/true aynı ... byte çıktısını reddeder/kabul eder` 1000000 ve 1000001 byte için false/true çiftlerini sınar. Bu sentetik kabul, üretim büyüme izni değildir. | 2 |
| F5 | `F5 gerçek Java P2 canonical golden ve digest birebir eşleşir`; `F5 sabit digest altında çıktı ... değişimi reddedilir` path/version/SHA/byte varyantları; aynı bozuk digest, çıktı dizisi sırası, snapshot bağı, Unicode metadata ve content/dil/revision alanlarının digest bağını sınayan beş ek test. | 10 |
| F6 | `F6 context ve descriptor aynı zz dilini taşısa da invalidIdentity` ve `F6 backend canonical dil kümesinin tamamı kabul edilir`. | 2 |

Toplam 24 regresyon eklendi; önceki 28 test korundu. F3 için kontrollü mutasyon
denemesi de yapıldı: her iki `_completeBindings` çağrısı geçici kaldırıldığında
dört F3 testi de başarısız oldu (`+0 -4`). Kaynak dosyası `finally` ile birebir geri
yüklendi; ardından son tam test paketi çalıştırıldı. Mutasyon kalıcı kodda yoktur.

### Bağımsız canonical golden

Golden, mevcut `be/target/classes` altındaki gerçek `ReadingWork.Manifest` ve
`ReadingIdentity` sınıflarıyla üretildi. Backend, DB veya bulut başlatılmadı.
Üretici kaynak: `be/docs/evidence/p4-reading-images/P4ManifestGolden.java`.
Java çıktısı `app/mobile/test/reading_images/fixtures/p2_manifest_golden.json`
dosyasında tutulur. Golden; sıralı dizileri, null pageId'yi, P2 alan adlarını,
tırnak/ters eğik çizgi kaçışını, kısa kontrol kaçışlarını ve `\\u0001/\\u001f/\\u007f`
ASCII kaçışlarını kapsar. Beklenen digest:
`e2b4ff6ff58310a42e25d300f8f0187ed73d36c8d9ceb718052cda595d4f71e9`.

Depo kökünden yeniden üretim:

```sh
javac -cp be/target/classes -d /private/tmp be/docs/evidence/p4-reading-images/P4ManifestGolden.java
java -cp be/target/classes:/private/tmp P4ManifestGolden /private/tmp/p4-manifest-golden.json
cmp /private/tmp/p4-manifest-golden.json app/mobile/test/reading_images/fixtures/p2_manifest_golden.json
```

## Kanıtın sınırı ve açık işler

Descriptor denetimi P2'nin `revision/snapshot/outputs/binding/stored` metadata
yapısını kurar; sıralı dizileri ve ASCII canonical escaping kuralını korur.
`tellpal.reading.manifest.v1` domain SHA'sı yeniden hesaplanıp beklenen identity
digest'iyle karşılaştırılır. Rendition kimliği de rol, kaynak SHA'sı ve profil
parmak izinden P2 domain ayracıyla yeniden hesaplanır. P2 manifestinin içermediği
orijinal kaynak metadata'sı ve kabul edilmiş profil metadata'sı ayrıca beklenen
bağlamla eşleştirilir. Job/experience kimliği beklenen bağlamla doğrulanır.
Descriptor kontrolü gerçek nesne byte'larının SHA/MIME/boyut doğrulaması veya erişim
izni değildir. İlk sürümün eksik digest kontrolü yukarıdaki onarım geçmişinde korunur.

Çok adaylı test descriptor'ları yalnız seçici sıralamasını sınayan sentetik
girdilerdir. Mevcut P2'nin rol başına tek çıktısı değişmedi; varyant merdiveni üretilmedi.

Başlangıç incelemesinde bağlı Android cihaz bulunmadığı, fiziksel iPhone'un çevrimdışı
olduğu kaydedildi. Bu uygulamada cihaz/simülatör başlatılmadı. Fiziksel cihazda gerçek
aktarım, decode, görüntü kalitesi, peak/settled bellek ve yaşam döngüsü kanıtları
`unavailable` durumundadır; başarılı testler bu kanıtların yerine geçmez.

Normal tüketici, UI, ağ/storage/cache, ses/auth, üretim ayarı ve backend uygulaması
değiştirilmedi. `.memlog.md` ve mevcut süreçler korundu. P4a2 teslimat/karşılaştırma
ve P4b kaynak/yaşam döngüsü kararları açıktır; bu çalışma tamamlanmış P4 değildir.
Bağımsız üç BMad incelemesinin bulguları bu tek onarım turunda uygulandı.
Son kabul ve iş akışı durumunu ana oturum yönetir.
