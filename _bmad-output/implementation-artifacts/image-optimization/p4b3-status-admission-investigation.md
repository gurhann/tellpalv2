# P4b3: Status sonucunun admission sahipliği

Tarih: 2026-10-18. Başlangıç HEAD: `b0ef8292a87127b81839a601053d658e952544d4`.
Branch: `codex/image-optimization-p3-publication`. Bu kayıt salt kaynak incelemesidir;
tekrarlama testi, build veya gerçek ortam işlemi henüz yapılmadı. Bir Epic story değildir.
Model oturumdan devralınır. Mevcut dirty/untracked işleri koruyarak aynı çalışmayı
sürdürme talimatı geçerlidir; bunlar yeni görev değişiklikleri değildir.

## Seçilen boşluk

Admission, doğrulanmış ve sabit LIVE kimliğine bağlı görsel kabulüdür. Sayfa, geometri
veya karşılaştırma kolu değişirken aynı admission kullanılabilir. Buna karşın Cubit
her seçimde çizim `_epoch` değerini artırır. `_checkStatus()` bu çizim neslini yakalayıp
hem normal sonucu hem exception'ı `_owns(epoch)` ile süzer. Yeni seçim sonucu beklenen
status reddi, geçersiz manifest veya LIVE değişimi bu yüzden atılabilir. Sonraki poll
veya lease'e kadar güncel görsel işi devam edebilir.

Bu çıkarım kaynak akışına dayanır; henüz başarısız test kaydı üretilmedi. Ancak
tetikleyici gerçek QA reader'ın `_changePage()` yolunda vardır. ADR-0024 status
hatası/LIVE değişiminde temizleme ister. Aynı admission için geçerli kontrol,
yalnız çizim değişti diye geçersiz sayılmamalıdır.

Görünür seçim zaten render epoch ve seri gate ile korunur; yeni scheduler için
kanıtlı ihtiyaç bulunmadı. P4b1 status drain, P4b2 native ön rezervasyonu tamamlandı.
[P4b2 kanıtındaki](../../../be/docs/evidence/p4b2-raster-reservation/verification.md)
474 test önceki dilime aittir; P4b3 test sonucu sayılmaz.

## Kod haritası

Satırlar başlangıç HEAD'ine aittir; uygulamada sembolle doğrulanır.

| Yol / satır | Rol ve sınır |
|---|---|
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:48–78` | `_retire` admission'ı temizler; status/medya iptali ve lifecycle sahipliği burada. |
| Aynı dosya `:87–146,357–382` | `select`/`changeChoice` render neslini yeniler, admission'ı korur; retry retire eder. Render epoch korunur. |
| Aynı dosya `:167–189,287–307` | Doğrulanmış admission ve anchor benimsenir; lease/poll/control yalnız bu kabule bağlanır. Sayaç veya süreler değiştirilmez. |
| Aynı dosya `:310–354` | `_check` ve `_checkStatus`: hatalı render-epoch bağı. Tek runtime değişiklik alanı. |
| Aynı dosya `:386–403` | P4b1 ortak close/drain, status Future sahipliği ve seri gate korunur. |
| `app/mobile/lib/features/reading_images/domain/entity/reading_image_comparison.dart:17–27` | Mevcut status iptal nesnesi yeniden kullanılabilir. Yeni public sözleşme gerekmez. |
| `app/mobile/lib/app/reading_image_consumer_qa/reader_screen.dart:46–69` | Gerçek sayfa değişimi aynı Cubit'te `changeChoice` ve route replace yapar. Salt okunur. |
| `app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart:14–126,130–355` | Pending status/medya, ManualClock, close ve status hata testlerinin temeli. Seçimle yarışan status yok. |
| `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart:55–217,815–902,1032–1099` | Gerçek loopback/status ve parent kapanışı, lifecycle/status/LIVE testleri yeniden kullanılır. |
| `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart` | P4b2 rezervasyonu ve native cleanup değişmez; yeni testler mevcut gözlemleri okur. |

## Sahiplik kararı

Status, geldiği admission/istek yaşamına bağlı kalır. Çizim epoch'u yalnız medya
seçimi ve geç paint için kullanılır. Status için mevcut cancellation, halen güncel
admission ve görünür/kapanmamış Cubit sahipliği yeniden kullanılır. Gerekirse bu
ayrım Cubit içinde private tutulur; yeni DI, domain API veya genel oturum modeli eklenmez.

- Aynı admission'da sayfa/slot/DPR/fit/kol seçimi status sonucunu eskitemez.
- Güncel kontrolün ret/exception sonucu mevcut `status_refused`, geçersiz veya farklı
  LIVE sonucu mevcut `live_changed` yolunu kullanır. Güncel medya iptal edilir;
  raster ve rezervasyon gerçek cleanup kurallarına göre bırakılır.
- Geçerli, aynı LIVE cevabı güncel sayfayı/kolu/geometriyi değiştirmez veya yeniden
  yüklemez. Status eski bir çizimi boyayamaz; medya epoch denetimleri aynen kalır.
- Retire/readmission önceki kontrolü geçersiz kılar. Yeni manifest çağrısı **aynı
  nesneyi** ve aynı LIVE değerlerini döndürse bile eski isteğin sonucu yeni kabulü
  kapatamaz. Yalnız nesne/değer eşitliğine dayanılmaz; eski iptal/sahiplik atlanamaz.
- Background/account/lease/close sonrası sonuçlar state'i canlandıramaz. Close
  yine status ve medya Future'larının gerçek sonlanmasını bekler.

Bu bir erişim politikası değişikliği değildir. ADR-0024'ün mevcut kontrollü QA
kuralı doğru yaşam süresine uygulanır. Polling aralığında anlık sunucu revoke
garantisi, yeni otomatik admission, retry veya LIVE anchor değiştirme eklenmez.

## Anlamlı doğrulama

Önce mevcut kodda seçim ile bekleyen status reddini yarıştıran bir testin assertion
hatası kaydedilir. Sonra sahiplik düzeltmesi uygulanır; ilk başarısız kanıt korunur.

- Unit: bekleyen status sırasında sayfa seçimi, geometri değişimi ve `changeChoice`.
  Her tetikleyicide ret, exception, farklı LIVE ve geçerli aynı LIVE sonuçları
  sınanır. Ret güncel işi emekli etmeli; geçerli cevap güncel seçimi korumalıdır.
- Unit: retire/readmission sonrası eski success/ret/exception; özellikle repository
  aynı admission nesnesini geri versin. Eski kontrol yeni kabulü veya hata nedenini
  değiştirmemeli. Account/background/lease ve P4b1 close testleri korunmalıdır.
- Gerçek loopback → data source → repository → Cubit → QA reader testinde status
  cevabı bekletilir. Gerçek sayfa değişiminden sonra ret veya farklı LIVE serbest
  bırakılır; güncel image bırakılır, eski epoch boyanmaz, cleanup sonrası rezervasyon
  sıfırlanır. Sadece Cubit'e doğrudan mock callback çağrısı yeterli değildir.
- Aynı gerçek zincirde geçerli status güncel sayfayı korusun; status yüzünden fazladan
  manifest/byte isteği veya otomatik retry başlamasın. Mevcut yerel HTTP fixture'ı
  genişletilebilir; üretim transport'u değişmez.

Özel test kopyasında davranış mutasyonları: status'u tekrar render epoch'a bağlama;
emekli admission/istek sahipliğini etkisizleştirme; exception kolunda aynı güncel
sahiplik korumasını bozma. İlgili testler assertion'dan kırılmalı; derleme hatası
kanıt sayılmaz. Kaynak her denemeden sonra hash ile geri alınır. Fazladan yedek
korumanın tek başına kaldırılması etkisizse bu, test açığı olarak sunulmaz.

## Yerel çalışma ve koruma kapsamı

Somut plan onayı, gerekli özel kaynak/test kopyasını ve yerel doğrulamayı kapsar:
`/Volumes/MAC_EK/tellpal-qa-private/p4b3-status-admission-20261018-b0ef829/`.
Yalnız izlenen gerekli Flutter kaynak/test/fixture/paket varlıkları ile mevcut
sentetik TLS test modülleri alınır. SDK/cache/interpreter yeniden kullanılır;
paket/SDK/venv kurulmaz veya indirilmez. Credential, token, `.env`, gerçek servis
config'i ve ilgisiz untracked dosyalar kopyalanmaz. Dirty `main_image_experiment.dart`
özel kopyada kayıtlı HEAD sürümü olabilir; repo dosyası korunur. Kaynak eşitliği
bu istisnayla sınırlanır; bütün dirty/untracked ağacın test edildiği söylenmez.

Dizinler `0700`, yeni log/makbuzlar `0600` ve CREATE_NEW olur. Uygulama öncesi
repo, `be/var` ve önceki P4a5/P4b1/P4b2 kanıtları/frozen hash'leri mühürlenir.
Yalnız spec, görevdeki Cubit, iki test dosyası ve yeni kanıt klasörü değişir.
Bu inceleme belgesi read-only kalır. `.memlog.md`, dirty giriş, local-lossless
işleri ve eski başarısız kayıtlar korunur. Çalışan süreçlere dokunulmaz.

Mevcut SDK ile `flutter test --no-pub test/reading_images test/story_detail`, izlenen
tam Flutter test kümesi ve `flutter analyze --no-pub` geçer. Matris test adları ham
çıktıda görünmüyorsa yalnız ilgili testler sıralı reporter ile kaydedilir. Dar ve
tam sayılar toplanmaz. Kaynak/dependency kapanışı ve hash'ler, `git diff --check`,
üç bağımsız BMad incelemesi ve yalnız görev dosyalarının yerel commit'i tamamlanır.
Push veya PR yayını yapılmaz. Model oturumdan devralınır.

## Ayrı kalan işler ve kararlar

P4b2 gözlemleri henüz mevcut Dart QA probe/Python ölçüm doğrulayıcısına taşınmamıştır;
bu, sonraki rezervasyon ölçümü iddiasının ayrı hazırlık işidir. Eski P4a5 kayıtları
sonradan P4b2 kanıtı sayılmaz ve yeniden yazılmaz. Bu görev probe/controller'ı değiştirmez.

Normal Library/detail/PoC reader, ses, cache/provider/prefetch ve erişim kuralları
korunur. EXT-MEDIA/EXT-PUBLICATION, URL yenileme, HTTP cache, session/retention,
üretim profili ve genel P4b kabulü açık kalır. Yeni gerçek backend/DB/Firebase/storage,
giriş/pencere, ADB/APK/cihaz, encoder/upload/üretim, silme/dağıtım veya fatura sorgusu
yoktur. Fiziksel kalite/RSS/GPU/peak kanıtı üretilmez. P0 ham kanıt sınırlaması ve
JPEG/WebP bulanıklığı bu görevle kapanmaz.
