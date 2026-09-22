---
title: TellPal Mobile v1 Yeniden Yazım
status: draft
created: 2026-09-20
updated: 2026-09-20
---

# PRD: TellPal Mobile v1 Yeniden Yazım

Bu taslak, eski mobil uygulamanın yerini alacak sürüm için ürün kapsamını, kabul beklentilerini ve açık kararları bir araya getirir. Açık noktalar henüz karara bağlanmadığından uygulama geliştirmesine doğrudan onay veren nihai sözleşme değildir. Mevcut Türkçe karar belgesi aynı dilde genişletilmiştir.

Kaynak önceliği: bu görüşmede onaylanan kararlar → `be/docs/mobile-delivery-discovery.md` içindeki kesinleşmiş yön → `1.0.35+333` uygulaması ve `app/old/docs/` envanteri. Eski belgelerdeki teknoloji önerileri ve “backend yok” gibi eski repository açıklamaları yeni sistemin gerçeği sayılmaz. Yeni backend'in yetenekleri bu PRD çalışmasında kod veya runtime üzerinden doğrulanmamıştır.

Destekleyici kaynak özetleri: [akış ve kanıt envanteri](extract-legacy-flows.md), [veri ve sözleşme envanteri](extract-contracts.md). “Observed”, eski Android debug-emülatör raporunda gözlenmiş; “Partial”, akışın yalnız bir kısmı gözlenmiş; “Documented”, kaynak kod taramasında belgelenmiş anlamındadır. Bunlar bu çalışmada yeniden çalıştırılmış test sonuçları değildir. iOS ve gerçek cihaz paritesi henüz kanıtlanmamıştır.

## Ürün hedefi ve kapsam ilkesi

İlk sürüm, mevcut canlı TellPal mobil uygulamasının yerini alabilecek tam işlevli bir yeniden yazımdır. Mevcut görünüm, kullanıcı yolculukları ve kullanıcıya sunulan işlevler temel referanstır. Ürünün işleyişini değiştiren yenilikler sonraki aşamalarda ele alınacaktır.

Eski uygulamayla uyumluluk temel alınırken `be/docs/mobile-delivery-discovery.md` içinde açıkça kararlaştırılmış değişiklikler kabul edilmiş istisnalar olarak uygulanacaktır. Bu belgedeki açık sorular ve geçici varsayımlar onaylanmış değişiklik sayılmaz. PRD görüşmelerinde sonradan netleştirilen kararlar ilgili eski discovery kararlarını günceller; diğer görünür ürün değişiklikleri sonraki aşamalara bırakılır.

Kullanıcının zamanla ilgili açıklaması, PRD veya mühendislik çalışmasının aceleye getirilmesini gerektiren bir teslim tarihi değildir. İlk aşamada ürünün işleyişini yeniden tasarlamaya zaman ayrılmayacaktır; gerekli analiz, mimari değerlendirme ve güvenilirlik doğrulaması replacement hedefini desteklemek için yapılabilir.

Başarı, eski uygulamada yapılabilen işlemleri gerçekleştiren ve canlı sürümün yerini alabilecek bir sürümün yayınlanmasıdır. Yeni backende uyum ve crash problemlerinin azaltılması temel kalite hedefleridir. Flutter zorunlu değildir; teknoloji seçimi bu hedeflere göre mimari aşamada değerlendirilecektir.

## Desteklenen platformlar ve cihazlar

### Görünüm ve davranış referansı

Kullanıcı, eski uygulamanın `1.0.35+333` sürümünü görünüm ve davranış karşılaştırmaları için referans olarak onaylamıştır. `app/old/docs/` altındaki envanter ve gözlemler başlangıç kanıtıdır; belgelenmemiş veya runtime doğrulaması eksik akışlar bu sürüm üzerinden tamamlanacaktır. Bu onay sürümün mağazadaki en güncel sürüm olduğuna dair bağımsız bir doğrulama değildir. Discovery ve PRD'de açıkça kabul edilmiş değişiklikler karşılaştırmada beklenen istisnalardır; bilinen crash ve diğer hatalar korunacak davranış sayılmaz.

### Platform kapsamı

İlk replacement sürümü Android telefon ve tabletlerde Android 7.0 (API 24) ve üzerini; iPhone ve iPad cihazlarda iOS/iPadOS 15 ve üzerini destekleyecektir. Kullanıcı bu kapsamı onaylamıştır. Minimum sürümler teknoloji ve bağımlılık seçiminde kısıt olarak ele alınacak; uyumsuzluk çıkarsa destek kapsamı sessizce daraltılmayacaktır. Yayın doğrulaması her iki platformda telefon ve tablet deneyimlerini, minimum sürümleri ve temsilî güncel sürümleri kapsayacaktır.

## Kararlılık, performans ve bellek hedefleri

Eski sürümün kalitesi kabul eşiği değildir. Yeniden yazım, desteklenen cihazlarda mümkün olan en yüksek kararlılık ve akıcılığı hedefleyecektir. Kullanıcı mevcut crash-free oranının yaklaşık %85 olduğunu bildirmiştir; bu sayı henüz ölçüm kaynağıyla doğrulanmamıştır. Kullanıcı ölçümün kullanıcı/oturum bazını ve platform kapsamını hatırlamamaktadır. Oranın tanımı, zaman aralığı, platform ve sürüm kapsamı kararlılık incelemesinde kaynak üzerinden doğrulanacaktır; bu doğrulama PRD görüşmelerini engellemez. Yaklaşık %85 değeri doğrulanmış başlangıç ölçümü veya sayısal iyileşme hesabının temeli olarak kullanılmayacaktır.

Yüksek çözünürlüklü görsellerin fazla RAM tüketerek crashlere neden olduğu kullanıcı hipotezidir; doğrulanmış kök neden değildir. Crash kayıtları, bellek ölçümleri ve temsilî cihaz testleri nedenleri belirlemelidir. Teknoloji seçimi tek başına crashlerin çözüldüğüne kanıt sayılmayacaktır.

Kalite doğrulaması şu sonuçları kapsayacaktır:

- Uzun okuma/dinleme oturumlarında, ardışık içerik açmada ve hızlı sayfa değişiminde crash veya donma olmadan devam edebilme.
- Düşük bellekli desteklenen cihazlarda da kontrollü bellek kullanımı; tekrarlanan açma/kapatma döngülerinde sürekli bellek birikmesi olmaması. Ölçülebilir bütçeler cihaz sınıfı ve en ağır içerik örnekleriyle belirlenecektir.
- Görsel okunabilirlik ve kalite korunurken gereksiz bellek ve indirme tüketiminin azaltılması.
- Arka plan/ön plan geçişi, ekran kilidi, bağlantı kesintisi ve medya kesintilerinden sonra tanımlı şekilde toparlanma.
- Açılış, ilk sayfa/ilk ses hazırlığı ve sayfa geçişlerinin ölçülmesi; kullanıcı deneyiminin en yavaş örneklerinin de kabul kriterlerine dahil edilmesi.
- Bilinen, kritik kullanıcı akışlarını engelleyen crash, donma ve veri kaybı hatalarının yayın öncesinde giderilmesi.

Platform bazında crash-free kullanıcı oranı yayın değerlendirmesinde en az %99,5 olmalıdır. Kullanıcı bu hedefi onaylamıştır. Oranın tanımı, değerlendirme süresi ve örneklem büyüklüğü ayrıca belirlenecek; yetersiz kullanımda elde edilen hatasız test sonucu tek başına üretim kararlılığı kanıtı sayılmayacaktır. Bellek ve gecikme bütçeleri cihaz/ağ koşullarıyla belirlenecektir. Test cihazlarında bellek kaynaklı kapanma veya kritik oynatma hatası yayın engelidir. Crash ve performans tanılama, geliştirme/test sürecinde kullanılabilir olmalıdır.

## Yayın öncesi son entegrasyonlar

RevenueCat entegrasyonu ve kullanıcı akışlarının backende loglanması ilk replacement sürümünün kapsamındadır. Geliştirme sırasının son adımlarında tamamlanacak ve release öncesinde doğrulanacaktır; yayın sonrasına ertelenmiş özellikler değildir. Bu karar, mobile delivery discovery belgesindeki ertelemenin mevcut PRD kapsamında geliştirme sırasına ait olduğunu netleştirir.

- RevenueCat: mevcut abonelerin erişimi, satın alma, satın alımları geri yükleme ve abonelik erişim durumunun doğrulanması yayın öncesi kabul kapsamındadır.
- Kullanıcı akışlarının backende loglanması: izlenecek akışlar ve olaylar ile kaydedilecek veri kapsamı gereksinim çalışmasında tanımlanacak; entegrasyon ve doğrulama release öncesi son adımlarda tamamlanacaktır.

Bu entegrasyonların ihtiyaçları ilgili gereksinim ve mimari kararlarında baştan dikkate alınacaktır. Crash/hata tanılama, kullanıcı akışlarının loglanmasından ayrı bir güvenilirlik ihtiyacıdır; kullanıcı akışlarının loglanmasına ilişkin son aşama kararı crash tanılamanın zamanlamasını belirlemez.

## Mevcut verilerin korunması ve sürüm geçişi

Kullanıcı, eski sistemdeki her şeyin korunmasını istemektedir. Replacement sürümüne geçişte mevcut veri kapsamı daraltılmayacak; hesap, abonelik, profil veya kullanım geçmişi sıfırlanması kabul edilmeyecektir. Koruma, yalnızca eski verinin bir yedeğinin bulunmasını değil, kullanıcıya ait işlevsel bilgilerin yeni uygulamada kullanılabilir kalmasını da kapsar.

Eski uygulamanın `app/old/docs/data-models.md` envanterine göre geçiş incelemesi en az şu alanları kapsayacaktır:

- Hesap kimlikleri, oturum devamlılığı, anonim kullanım durumu, abonelikler ve erişim hakları.
- Her iki nesil profil verisi: ad, avatar, doğum tarihi/yaş aralığı, müşteri tipi, kullanım amaçları, favori türler ve pazarlama tercihleri.
- Dil ve çocuk/ebeveyn modu dahil mevcut kullanıcı tercihleri.
- Okunan içerik listeleri, okuma/dinleme geçmişi, tamamlanma durumu, süreler ve saklanmış kalan sayfa/ilerleme bilgisi.
- Geri bildirim kayıtları, promosyon kayıtları ve mevcut içeriklerle kullanıcı geçmişi arasındaki kimlik ilişkileri.
- Cihazda saklanan tercihler, açılış/onboarding durumu ve yerel kullanıcı kayıtları. Eski indirilmiş medya ve yeniden üretilebilir önbellekler aşağıdaki temizlik kararı kapsamındadır.

Bu envanter dokümantasyon kanıtıdır; canlı sistem ve cihazlarda bulunan verilerin eksiksiz listesi olarak kabul edilmeyecektir. Geçiş planında tüm kaynaklar ve şema nesilleri doğrulanacak, eski ve yeni kimlikler eşleştirilecek, eksik veya eşleştirilemeyen kayıtlar sessizce atılmayacaktır. Özellikle yalnız cihazda bulunan anonim kullanıcı verilerinin korunması ayrıca doğrulanacaktır.

Veriyi koruma gereksinimi aynı veritabanı şemasını veya dosya biçimini kullanma zorunluluğu getirmez. Teknik temsil değişebilir; kullanıcı verisinin anlamı ve ilişkileri korunmalıdır.

Kullanıcı, eski uygulamanın cihazda indirdiği medya dosyaları ve yeniden üretilebilir önbelleklerin taşınmasına gerek olmadığını, artık ihtiyaç duyulmayacak bu verilerin silinebileceğini belirtmiştir. Güncellemede eski medya/önbellekler temizlenecek; yeni uygulama ihtiyaç duyduğu medyayı yeni sistemden edinecektir. Bu karar profil, tercihler, geçmiş, ilerleme, anonim kimlik veya yalnız cihazda bulunan kullanıcı kayıtlarının silinmesine izin vermez. Temizlik hedefleri kullanıcı kayıtlarından ayrılacak ve migration için gerekli veri korunmadan temizlenmeyecektir. Eski sunucu verileri ve eski sürümü kullanan diğer cihazlar bu temizliğin kapsamında değildir.

Yayın kabulü, eski canlı sürüm üzerine güncelleme senaryolarında mevcut verilerin ve erişim haklarının korunduğunun doğrulanmasını içerecektir. Ayrıntılı geçiş testleri ve ölçülebilir kabul kriterleri PRD'nin devamında tanımlanacaktır.

### Anonim kullanıcıların geçişi

Eski uygulamayı hesapsız kullanan kişi, yeni sürüme geçtiğinde kayıt olmaya zorlanmadan anonim kullanıma devam edecektir. Yeni anonim kullanım Firebase Anonymous Auth kimliğiyle ilişkilendirilecek; cihazda mevcut profil, tercihler, geçmiş ve diğer korunacak kullanıcı kayıtları yeni anonim uygulama hesabına taşınacaktır. Bu karar, belgelerde eski anonim kullanımın yerel bir işaretle yönetildiği bilgisini dikkate alır; eski kullanıcı için doğrulanmış bir Firebase UID bulunduğu varsayılmayacaktır.

Kesinti veya yeniden deneme kullanıcı kayıtlarını çoğaltmamalı veya kaybettirmemelidir. Anonim geçişin tamamlandığı doğrulanmadan gerekli yerel kayıtlar temizlenmeyecektir. Eski indirilmiş medya ve yeniden üretilebilir önbelleklerin temizlenmesine ilişkin karar geçerlidir. Anonim kullanıcı daha sonra kayıtlı hesaba dönüştüğünde profil ve dil verilerinin korunması yönündeki discovery kararı da geçerliliğini korur.

### Eski ve yeni sistemin birlikte çalışması

Eski sistem, yeni sürüm yayınlandıktan sonra bir süre daha çalışmaya devam edecektir. Eski uygulama sürümünü kullanan kişiler eski sistemde kalacak; kullanıcı yeni uygulama sürümüne geçtiğinde migration yapılacaktır. Tüm kullanıcıların yayın gününde topluca geçirilmesi veya eski sistemin aynı anda kapatılması hedeflenmemektedir.

Kullanıcıya ait migration, hesabın ilk geçiş anına kadar eski sistemde oluşmuş verileri kapsamalıdır; yalnızca yeni sürümün yayınlandığı gün alınmış bir veri kopyası yeterli değildir. Kullanıcı bazlı geçiş, ortak içerik/kategori kimliklerinin ve geçmiş kayıtlarının önceden hazırlanmış eşleştirmelere ihtiyaç duyabileceği gerçeğini değiştirmez.

Migration hesap başına tek seferliktir. Aynı hesabın telefonu yeni sürüme geçmişken tableti eski sürümde kalabilir; tablet eski sistemden veri okumaya ve eski sistemle çalışmaya devam eder. İki sistem arasında kullanıcı geçmişi, ilerleme veya profil için sürekli eşitleme yapılmayacak; ilk migration sonrasında eski sistemde oluşan değişiklikler tekrar taşınmayacaktır. Tablet daha sonra güncellendiğinde aynı hesabın yeni sistemdeki mevcut verilerini kullanır; hesap migration'ını yeniden başlatmaz.

Tüm mevcut verilerin korunması gereksiniminin hesap migration'ı açısından zaman sınırı ilk geçiş anıdır. Sonrasında eski sistemde oluşan kayıtlar eski sistemde kalır; yeni sisteme aktarılmaları beklenmez. Bu karar kaynak kayıtların silinmesini gerektirmez ve satın alınmış erişim haklarının RevenueCat üzerinden doğrulanması gereksinimini kaldırmaz. Cihaza özgü yerel kayıtlar ve anonim kullanıcı kimliği ayrıca ele alınacaktır; bunların dönüşümü tamamlanmış hesap migration'ının yeniden çalıştırılması anlamına gelmez.

Veri koruma hedefinden türeyen kabul gereksinimleri:

- Yeni sürüme geçmemiş kullanıcıların eski uygulamadaki erişimi ve işlevleri devam eder.
- Geçiş sırasında kesinti veya yeniden deneme, kullanıcı verisinin kaybolmasına, kayıtların çoğalmasına veya yeni sistemdeki daha güncel verinin eski veriyle ezilmesine yol açmaz.
- Migration sırasında kullanıcıya uygulama dilinde “Verileriniz taşınıyor” durumu gösterilir. Bağlantı kesintisi veya hata halinde mevcut veriler korunur ve “Tekrar dene” seçeneği sunulur. Migration tamamlanmadan boş/sıfırlanmış profille normal kullanıma geçilmez ve süreç başarılı olarak işaretlenmez. Kullanıcı bu bekleme ve yeniden deneme davranışını onaylamıştır. Yeniden deneme tamamlanmış kayıtları çoğaltmaz veya daha güncel verileri ezmez.
- Eski verilerin korunması gereksinimi sürer; başarılı migration kaynak verilerin otomatik silinmesine izin vermez.

Açık geçiş kararları: cihaza özgü yerel kayıtların ve anonim kimliklerin teknik eşleştirme yöntemi; eski sisteme geri dönüş davranışı; eski sistemin kapanış koşulu ve birlikte çalışma süresi. Anonim geçişin kullanıcıya dönük davranışı yukarıda kararlaştırılmıştır. Kesilmiş bir migration'ın güvenli yeniden denenmesi, başarıyla tamamlanmış migration'ın tekrar çalıştırılmasından ayrılacaktır.

## Kullanıcılar ve temel ihtiyaçlar

Mevcut çocuk deneyimini kullanan kişiler, bildikleri ekranlardan hikâye okuyabilmeli, sesli içerik dinleyebilmeli ve hesaplarını yönetebilmelidir. Mevcut aboneler erişim haklarını; anonim kullanıcılar kayıt zorunluluğu olmadan kullanımını korumalıdır. İçerik ekibi CMS'de hazırladığı içeriği cihazda test edebilmeli ve onayladığı sürümü yayınlayabilmelidir.

İlk sürüm yeni bir hedef kitle, yeni keşif modeli veya yeni kullanıcı alışkanlığı yaratmayı hedeflemez. Tek hesapta bir profil vardır; profil yaşı bilgilendiricidir. Çocuk/ebeveyn modu seçimi bağımsız bir yetkilendirme rolü değildir.

### Kaynaklardan çıkarılmış yolculuk taslakları

Aşağıdaki akışlar legacy belgelerinden ve kabul edilen geçiş kararlarından çıkarılmıştır; kullanıcı araştırması veya yeni persona icadı değildir. Ayrıntıları referans sürümle kontrol edilecek, son incelemede onaylanacaktır.

- **UJ-1 — Mevcut kullanıcının güncellemesi:** Yeni sürüm açılır, hesap/geçiş durumu belirlenir, gerekiyorsa taşıma ekranı gösterilir. Başarılı geçişten sonra profil, geçmiş ve erişim hakları kullanılabilir. Kesinti tekrar denemeye götürür; boş profil oluşturmaz.
- **UJ-2 — Anonim başlangıç ve daha sonra kayıt:** İlk açılış akışı anonim devam veya kayıt/giriş sunar. Anonim kullanıcı katalogdan ücretsiz içerik tüketir; daha sonra yeni kayıtlı hesaba dönüşürken profil ve dilini korur. Var olan hesaba giriş, o hesabın verilerini kullanır.
- **UJ-3 — Hikâye keşfi ve okuma:** Ana ekran/kategori/aramadan içerik seçilir, detay görülür, okuma başlatılır. İlk sayfa hazır olduğunda anlatım açılır; otomatik veya manuel ilerleme kullanılır; tamamlanma/geçmiş güncellenir.
- **UJ-4 — Dinleme:** Sesli hikâye veya meditasyon detayından dinleme başlatılır; ninni kartı doğrudan oynatıcıyı açar. Kullanıcı arka planda dinlemeye devam eder, oynatıcı kontrollerini kullanır ve geri döner.
- **UJ-5 — Hesap ve destek işlemleri:** Profil, dil, geçmiş, hesap ve destek işlemleri mevcut giriş noktalarından yürütülür.
- **UJ-6 — Kilitli içeriğe erişim:** Kilitli içerik görülebilir; başlatma girişimi paywall açar. Satın alma veya geri yükleme sonrası doğrulanmış erişimle tüketim yapılır. Mağaza hatası sahte başarı veya kayıp erişim üretmez.
- **UJ-7 — İçeriğin cihazda test edilmesi:** Yetkili tester aday revizyonu açar ve test göstergesini görür. Normal kullanıcı mevcut canlı revizyonu kullanır. Onaylanmamış veya işlemeye hazır olmayan aday genel yayına çıkmaz.

## Terimler

- **Referans sürüm:** Görsel ve davranışsal karşılaştırmanın temeli olan `1.0.35+333`.
- **Replacement sürümü:** Mevcut mağaza uygulamasının güncellemesi olarak onun yerini alacak ilk yeni sürüm.
- **Hesap / profil:** Kimlik ve erişimle ilişkili hesap; bu hesap için tek uygulama profili.
- **Deneyim:** İçeriğin okuma veya dinleme biçimi. Aynı STORY okuma ve sesli hikâye deneyimleri sağlayabilir.
- **Revizyon / aday / canlı:** İçeriğin belirli sürümü; test/onay bekleyen sürüm; normal kullanıma yayınlanmış sürüm.
- **Tester:** Test adayına erişme yetkisi olan kişi; abonelikten ayrı yetki.
- **Migration:** Kullanıcı verisinin ilk geçişte yeni sisteme taşınması. Başarısız işlemin tekrarı yeni bir hesap migration'ı değildir.
- **Oynatma önbelleği:** Yeniden üretilebilir geçici medya. Kullanıcının kalıcı profil/geçmiş verisi değildir.
- **Parite:** Onaylı istisnalar dışında referans sürümün görünüm ve kullanıcı işlevlerinin karşılanması.

## İşlevsel gereksinimler

Aşağıdaki gereksinimler ürün davranışını tanımlar. Endpoint, DTO, mobil framework ve veri taşıma mekanizması mimari çıktılarda belirlenecektir. Legacy envanterinden türetilen ayrıntılar runtime kanıtı eksik olsa da sessizce kapsamdan çıkarılamaz; farklılıklar açık karara bağlanır.

### Açılış, kimlik ve profil — UJ-1, UJ-2, UJ-5

#### FR-1 — Açılış ve navigasyon
İlk kurulum, geri dönen kullanıcı ve migration gereken kullanıcı doğru giriş akışına yönlendirilir. Onboarding, ana hikâye, rahatlama ve profil yüzeyleri ile geri navigasyon referans sürümle karşılaştırılır. Yeniden açılış yanlışlıkla onboarding veya boş profil başlatmaz.

#### FR-2 — Kimlik doğrulama
E-posta, Google ve Apple giriş/kayıt; e-posta doğrulama ve parola sıfırlama desteklenir. Referans kayıt formundaki zorunlu onay, ayrı pazarlama tercihi ve e-posta/parola doğrulamaları karşılanır; eksik zorunlu alan/onay başarıya dönüşmez. İptal/hata yeni başarılı oturum sayılmaz. Firebase kimliği doğru uygulama hesabıyla eşleşir; benzer e-posta adresleri nedeniyle hesaplar sessizce birleştirilmez.

#### FR-3 — Anonim kullanım ve dönüşüm
Anonim devam Firebase Anonymous Auth ile mümkün olur. Anonim kullanıcı yeni kayıtlı hesap oluşturduğunda anonim dönemdeki profil, tercihler, geçmiş ve ilerleme yeni hesaba taşınır. Kullanıcı var olan hesaba giriş yaptığında o hesabın verileri kullanılır; anonim veri otomatik olarak birleştirilmez. Bu davranış kullanıcı tarafından onaylanmıştır. Eski uygulamadan anonim migration sırasında geçmişin korunması kararı da geçerlidir.

#### FR-4 — Profil kurulumu ve düzenleme
Referans sürümün profil kurulum alanları, atlanabilir adımları, avatar ve ad düzenleme işlevleri korunur. Migration her iki eski profil şemasının bilgisini korur; atlanan alanlar uygulamayı kullanmayı engellemez. Bir hesap için bir profil kullanılır.

#### FR-5 — Hesap ve tercih yönetimi
Çıkış, parola değiştirme, hesap silme, dil değiştirme ve bildirim tercihi mevcut işlevleriyle bulunur. Hesap değişiminde önceki hesabın özel verileri görünmez. Hesap silme yalnız yeni sistemdeki hesabı ve o sistemdeki kullanıcı verilerini kapsar. Eski sistemdeki kayıtlar ile eski uygulama sürümünü kullanan cihazlar etkilenmez. Kullanıcı bu kapsamı onaylamıştır.

### Katalog ve keşif — UJ-3, UJ-4

#### FR-6 — Ana ekran ve kategori
Ana ekran CMS sıralı kategori satırları ve sınırlı önizleme içerikleri sunar; tam kategoriye geçiş vardır. Dil/erişim kitlesi için uygun içeriği olmayan kategori gösterilmez. Tam listede içeriklerin atlanmaması ve tekrarlanmaması sayfalama kabulüne dahildir.

Editör seçimi ve benzer içerik yüzeyleri referans envanterine dahildir; seçilen öneri doğru içerik/deneyime gider. Uygulanabilir içerik bulunmadığında bozuk kart veya geçersiz hedef gösterilmez.

#### FR-7 — Genel arama
Arama kapsamı henüz kararlaştırılmamıştır. Eski uygulama `/story/search` üzerinden yalnız hikâye araması yapmaktadır; discovery belgesi ise uygun içerik türleri arasında genel içerik aramasını yönlendirmektedir. Eski istemcinin gerçek request/response örnekleri ve yeni backend hedef sözleşmesi incelenerek karar verilecektir. Sonuçlar her durumda bağımsız kişi kaydı değil içerik olur; katkı sağlayıcı bilgileri arama eşleşmesine katkı verebilir. Sonuçlar deneyim/tür bağlamını belirtir; boş sonuç ve ağ hatası birbirinden ayrılır.

#### FR-8 — İçerik ve deneyim ayrımı
Hem okuma hem sesli hikâye deneyimi olan STORY ilgili iki keşif grubunda görünebilir. Seçilen deneyim doğru detay/oynatmayı açar; diğer deneyimin işleme hatası hazır deneyimi engellemez.

#### FR-9 — Dil ve fallback
Uygulama ve içerik dili birlikte yönetilir, tercih profilde saklanır. Dil görünürlüğü referans sürümdeki gibidir: Türkçe, İngilizce ve Almanca açıktır; Portekizce yalnız mevcut uzaktan yapılandırma tarafından açıldığında görünür; İspanyolca kapalıdır. Dil değişiminde önceki dile ait önbellek sonucu yeni dilmiş gibi gösterilmez. İngilizce fallback yalnız uygun yayın ve erişim koşulları sağlanırsa kullanılabilir. Kullanıcı bu davranışı onaylamıştır.

#### FR-10 — Ücretsiz ve kilitli görünürlük
Ücretsiz ve kilitli içerik katalogda görünür; kilitli içeriği başlatma paywall açar. Dil bazlı CMS ücretsiz seçimi STORY'nin okuma ve sesli deneyimlerine uygulanır. Abone olmayanlara özel kategori CMS sırasıyla yalnız o kitleye gösterilir; aktif aboneye gösterilmez.

### Okuma ve dinleme — UJ-3, UJ-4

#### FR-11 — Detaydan başlatma
Hikâye, sesli hikâye ve meditasyon bilgi/detay yüzeyinden açık kullanıcı eylemiyle başlar. Ninni kartı doğrudan oynatıcıya ve kullanıcı tarafından başlatılmış dinlemeye gider. Detayı açmak kendiliğinden ses başlatmaz.

#### FR-12 — Sayfalı hikâye
Sayfa görseli ve anlatım sesi birlikte doğru sayfaya aittir. İlk sayfa hazır olunca başlayabilir; tüm hikâyeyi beklemek gerekmez. Anlatım bitişi animasyonla ilerler; manuel gezinme çalışır. Hazır olmayan sayfa yükleme durumuna geçer, sonsuz bekleme yerine toparlanabilir hata sunar.

#### FR-13 — Hikâye kontrolleri ve tamamlanma
Referans sürümdeki anlatım/müzik kontrolleri, otomatik ve manuel sayfa ilerletme ile tamamlanma davranışları korunur. Hızlı gezinme ile otomatik ilerlemenin çakışması yanlış sayfa veya çift ses üretmez. Tamamlanma ekranındaki değerlendirme, benzer içerik ve paywall girişleri envanterden doğrulanır. Etkileşimli hikâye dalları ve seçimleri replacement kapsamı dışındadır.

#### FR-14 — Sesli oynatıcı
Referans sürümün ninni, meditasyon ve sesli hikâye oynatma kontrolleri aynı kalır: oynat/duraklat, ses konumuna gitme, oynatıcıdan çıkış ve ilgili meta bilgiler gösterilir. Ninni döngüsel oynar; meditasyon ve sesli hikâye döngüsel oynamaz. Arka plan ve sistem medya kontrolleri desteklenir. Çağrı geldiğinde veya kulaklık çıkarıldığında oynatma duraklar; kullanıcı kendi eylemiyle yeniden başlatır. Ekrandan çıkış, geri dönüş ve başka içerik açılışında birbiriyle yarışan oynatıcılar oluşmaz.

#### FR-15 — Medya hazırlığı ve görsel sunum
İlk sayfa ve sınırlı ileri hazırlık Wi-Fi ve hücresel bağlantıda kullanılabilir; tüm hikâyeyi spekülatif indirme anlamına gelmez. Detayda sınırlı ses hazırlığı başlatma anında yeniden kullanılır. Ninni liste ve oynatıcı görselleri ayrıdır; mevcut animasyon tek statik görsele indirgenmez. Hikâye görsel oranı korunur; metin güvenli alanı ve tablet kırpması cihazlarla doğrulanır.

#### FR-16 — İlerleme, geçmiş ve istatistikler
Okuma/dinleme geçmişi, tamamlanma, saklanan ilerleme ve profil istatistikleri referans işlevleriyle korunur. Tek olayın tekrar işlenmesi tamamlanma veya süreyi iki kez saymaz. Uygulama tamamen kapatılıp yeniden açıldığında ana ekrandan açılır; kullanıcı doğrudan önceki hikâye/oynatıcıya geri götürülmez. Uygulama arka planda kaldığında açık hikâye veya oynatma oturumu kaldığı yerden devam eder. Kullanıcı bu davranışı onaylamıştır.

### Yardımcı yüzeyler — UJ-5

#### FR-18 — Destek ve iletişim
Geri bildirim gönderimi, doğrulama ve mevcut sınırlar; yerelleştirilmiş yasal/yardım belgeleri; uygulama değerlendirme çağrısı ve sürüm bilgisi; duyuru, bildirim izni ve deep-link giriş noktaları korunur. Başarısız gönderim başarılı gösterilmez. Duyuru/bildirim ve deep-link hedefleri geçerli ekrana gider veya anlaşılır durum sunar. Kullanıcıya görünen bildirim/duyuru/deep-link davranışı referans sürümle aynı kalır; teknik sağlayıcı veya SDK seçimi mimari aşamasında değişebilir. Mevcut operasyonel yapılandırmanın görünür özellikleri, ücretsiz içerikleri, duyuruları ve paywall tetiklerini yönetme ihtiyaçları envantere dahildir; aynı Remote Config anahtarlarının kullanılması şart değildir.

### Abonelik ve olay kaydı — UJ-6

#### FR-19 — Satın alma ve geri yükleme
RevenueCat üzerinden mevcut aboneliklerin tanınması, satın alma, geri yükleme ve erişim yenileme yayın öncesinde tamamlanır. İptal edilmiş ama ücretli dönemi bitmemiş abonelik erişimi sürer. Mağaza hatası veya satın alma iptali erişim verilmiş gibi gösterilmez; anonim/kayıtlı kimlik tek başına premium hakkı yaratmaz.

#### FR-20 — Promosyon ve paywall
Referans sürümdeki promosyon kodu/teklif, paywall giriş ve kapatma, üyelik yönetimi işlevleri envanterden doğrulanarak korunur. Hatalı/geçersiz promosyon tanımlı hata sunar. RevenueCat ürün/teklif kimlikleri ile teknik tetikleme/entegrasyon ayrıntıları gerçek entegrasyon sözleşmesinde belirlenecektir. Kullanıcı bu yaklaşımı onaylamıştır.

#### FR-21 — Kullanıcı akışlarının backend kaydı
Giriş/kayıt, migration, katalog-arama-detay görüntüleme, okuma/dinleme başlatma-tamamlama, paywall/satın alma/restore ve hata-yeniden deneme akışları release öncesinde backende kaydedilir. Olaylar içerik metni, medya URL'leri veya gereksiz kişisel veri içermez. Veri alanları, saklama süresi ve hata/tekrar politikası gerçek backend sözleşmesinde belirlenecektir. Kayıt başarısızlığı okuma/dinlemeyi durdurmamalı; yeniden deneme aynı olayı çoğaltmamalıdır. Crash tanılaması bu son entegrasyona bağımlı değildir.

### Yayın ve erişim — UJ-7

#### FR-22 — Dil ve deneyim bazlı yayın
Okuma ve sesli hikâye yayınları dil/deneyim bazında bağımsızdır. Bir kapsamda en fazla bir aktif aday bulunur. Adaydaki önemli değişiklik yeni bir aday revizyon oluşturur ve etkilenen test/onayları sıfırlar. Yeni aday hazırlanırken mevcut canlı revizyon kullanılabilir kalır; ilgili yayın kapsamıyla sınırlı rollback önceki canlı revizyonu geri getirebilir.

#### FR-23 — Tester erişimi ve onay
Tester, kilitli adaylar dahil yetkili olduğu test revizyonlarını açabilir; test/revizyon göstergesi görür. Normal kullanıcı aday alamaz. Gösterge yetkilendirme yerine geçmez. Medya değişikliği hazırlık ve yeniden cihaz testi gerektirir; genel yayın gerekli hazırlık ve açık onaydan sonra yapılır.

#### FR-24 — İşleme başarısızlığı
Medya hazırlama başarısızlığı ilgili adayın test/genel yayınını engeller; CMS hata ve yeniden deneme yolunu gösterir. Başarısız aday mevcut canlı içeriği değiştirmez. Üretilmiş medya hazır kabul edilmeden gerçekten erişilebilir ve bütün olmalıdır.

#### FR-25 — Korumalı medya ve revizyon sürekliliği
Liste/detay görünürlüğü korumalı tam medya erişimi vermez. Başlatma yetkisi kontrol edilir; aktif oynatma kabul edilen revizyona bağlı kalır. Süresi dolan medya erişimi, yayından çekme ve güvenlik istisnaları OQ-8'de tamamlanacaktır. Temizlik canlı, rollback ve oturum için gerekli revizyonları korur.

### Replacement ve migration — UJ-1, UJ-2

#### FR-26 — Hesap bazlı tek geçiş
İlk yeni sürüm kullanımı hesabın ilk migration'ını tetikler ve o ana kadar olan korunacak verileri taşır. Başarılı hesabı ikinci cihazda açmak migration'ı tekrarlamaz. Eski cihaz eski sistemde kalır; sonradan oluşan değişiklikler eşitlenmez.

#### FR-27 — Kayıpsız ve yeniden denenebilir taşıma
Eski/yeni içerik ve hesap kimlikleri doğru eşleşir. Tekrar deneme kayıt çoğaltmaz, daha yeni veriyi ezmez ve eksik eşleşmeleri sessizce atmaz. “Verileriniz taşınıyor” ve “Tekrar dene” deneyimleri uygulanır; tamamlanmamış migration boş profille normal kullanıma geçemez.

#### FR-28 — Yerel anonim veri ve temizlik
Anonim kullanıcı kayıt zorunluluğu olmadan geçer; korunacak yerel kayıtlar yeni anonim hesaba aktarılır. Eski medya/yeniden üretilebilir önbellek temizlenebilir; migration girdileri aktarım doğrulanmadan silinmez. Eski backend ve eski sürümlü diğer cihazların verisi bu temizliğe dahil değildir.

#### FR-29 — Mağaza güncellemesi olarak teslim
Replacement mevcut uygulamanın üzerine güncelleme olarak kurulabilmelidir; ayrı uygulama kurma ve hesabı baştan oluşturma zorunluluğu getirmez. Platform kimliği, imzalama, oturum ve yerel veri erişiminin seçilen teknolojiyle korunabildiği yayın öncesi doğrulanır. Eski sistemin eşzamanlı hizmeti devam eder.

## Çapraz kalite gereksinimleri

- **NFR-1 — Kararlılık:** FR-1–FR-29 kritik kabul senaryolarında bilinen crash, donma veya veri kaybı engeli kalmaz. Sayısal üretim kararlılığı eşiği OQ-2'de açık olup bu ifade tek başına yüzde yüz crash-free garantisi değildir.
- **NFR-2 — Bellek:** Düşük bellekli cihaz, en ağır içerik ve tekrarlı oturum testleri yapılır. Isınma sonrası tekrar döngülerinde açıklanamayan sürekli bellek artışı giderilir. Cihaz sınıfı başına tepe/kararlı kullanım bütçeleri OQ-2'de belirlenir.
- **NFR-3 — Akıcılık ve başlatma:** Soğuk/sıcak açılış, ilk sayfa/ilk ses, sayfa geçişi ve yeniden bağlanma ölçülür. Cihaz, ağ ve önbellek koşullarıyla p50/p95 hedefleri kararlaştırılır; eski performans kabul tavanı değildir.
- **NFR-4 — Veri bütünlüğü:** Migration ve tekrar deneme testleri korunacak alanlar ve kimlik ilişkilerinde kayıp/çoğalma göstermemelidir. Başarı, yalnız taşınan kayıt sayısıyla değil alan/ilişki doğrulamasıyla değerlendirilir.
- **NFR-5 — Erişim ve gizlilik:** Başka hesabın verisi veya yetkisiz revizyon/medya döndürülmez. Kimlik bilgileri, tokenlar ve özel medya URL'leri olay/crash kayıtlarına konmaz. Çocuk verisi, SDK kapsamı ve izinlerle ilgili mevcut davranışlar OQ-9 kapsamında doğrulanır.
- **NFR-6 — Uyumluluk:** Onaylanmış minimum işletim sistemi ve telefon/tablet kapsamı korunur. Gerçek cihazlarda oynatma, migration ve arka plan davranışı doğrulanır; yalnız emülatör başarısı yeterli değildir.
- **NFR-7 — Görsel parite:** Kararlaştırılmış referans ekranlar aynı cihaz boyutu, dil ve içerik koşullarında karşılaştırılır. Onaylı değişiklikler hariç görünür sapmalar kapanmadan ilgili ekran kabul edilmez; görüntü sıkıştırma metin okunabilirliğini bozamaz.
- **NFR-8 — Tanılama:** Crash, bellek kaynaklı sonlanma, donma ve oynatma hataları sürüm/platform/cihaz bağlamıyla araştırılabilir olmalıdır. Kullanıcı akış loglarıyla crash ölçümü ayrı veri amaçlarıdır.

## İlk sürüm kapsamı ve kapsam dışı işler

İlk sürüm; FR-1–FR-29 kapsamındaki onaylı işlevleri (FR-17 hariç), desteklenen platformları, eski kullanıcı verilerinin geçişini, yeni backend entegrasyonunu ve kabul edilmiş yayın/test yönünü kapsar. RevenueCat ve backend kullanıcı akış kaydı son geliştirme adımlarıdır, release sonrası işler değildir.

İlk sürümün kapsamı dışında: etkileşimli hikâye dalları ve seçimleri; ebeveyn modu ve ebeveyn rehberleri; ürünün UI/UX yeniden tasarımı; yeni keşif/kişiselleştirme özellikleri; çoklu profil; çoklu sağlayıcı hesap konsolidasyonu; eski/yeni kullanıcı verisinin sürekli eşitlenmesi ve kullanıcı kontrollü açık offline indirme yönetimi. Gelecekte offline kullanım mümkün olmalıdır; ilk sürümün medya temeli bunu engellememelidir. Legacy beş ZIP cache sınırı, iki ZIP formatı ve Flutter seçimi korunması gereken ürün davranışı değildir. Eski SDK'ların tümünün birebir taşınması ayrıca bir gereksinim değildir; kullanıcı işlevleri ve kabul edilen olay ihtiyaçları korunur.

## Başarı ölçütleri ve yayın kapıları

- **SM-1 — İşlevsel replacement:** Referans envanterindeki her kullanıcı işlevi karşılanmış veya açıkça onaylanmış istisnayla açıklanmış olmalıdır. Açık, kararsız işlev kaybı kalamaz. FR-1–FR-29'u doğrular.
- **SM-2 — Güvenli güncelleme:** Kayıtlı/anonim/abone kullanıcı ve kesintili migration testlerinde korunacak veri kaybı, kopya kayıt ve yanlış hesap eşleşmesi sıfır olmalıdır. FR-26–FR-29'u doğrular.
- **SM-3 — Üretim kararlılığı:** Platform bazında en az %99,5 crash-free kullanıcı oranı sağlanır. Değerlendirme süresi/örneklemi yeterli olmalı; donma ve bellek sonlanması ayrıca incelenmelidir. Test cihazlarındaki bellek kaynaklı kapanma ve kritik oynatma hatası yayın engelidir. NFR-1, NFR-2 ve NFR-8'i doğrular.
- **SM-4 — Medya deneyimi:** Onaylanacak cihaz/ağ matrisi ve gecikme bütçelerinde okuma/dinleme kabul testleri geçer. FR-11–FR-16 ve NFR-2, NFR-3'ü doğrular.
- **SM-C1 — Kaliteyi bozarak optimizasyon yok:** Bellek düşürmek için içerik/işlev atılmaz, görsel okunabilirlik ve ses bütünlüğü bozulmaz.
- **SM-C2 — Ölçümü gizleyerek kararlılık yok:** Hatalı cihazları rapordan çıkarmak, minimum sürümü sessizce yükseltmek veya tanılamayı kapatmak başarı sayılmaz.
- **SM-C3 — Hız için veri kaybı yok:** Migration süresi kısalsın diye geçmiş/tercih atılmaz; eksik taşıma başarıya çevrilmez.

Yayın kanıtı: referans parite kontrolü, gerçek cihaz kararlılık/performance raporu, güncelleme ve migration sonuçları, RevenueCat satın alma/restore/erişim doğrulaması, backend olay kaydı doğrulaması ve eski/yeni sistem birlikte çalışma testi. Yeni sürüm kademeli yayınlanır; yeterli kullanımda %99,5 crash-free kullanıcı hedefi, migration ve RevenueCat kanıtları sağlandıkça dağıtım genişletilir. Eski sistem, ürün sahibinin ayrıca belirleyeceği kapanış kararına kadar çalışır. Kullanıcı bu rollout yaklaşımını onaylamıştır. Mağaza yayınını geri alma veya yeni sistem hatasında toparlanma planı OQ-10 kapsamında tanımlanacaktır.

## Açık noktalar — son görüşmede toplanacak

Bu maddeler taslakta bilinçli olarak açık tutulmuştur. Hiçbiri örtük onay veya sessiz kapsam daraltması değildir. Mimari alanlar için çözüm burada seçilmeyecektir.

| ID | Karar / doğrulama | Sorumlu rol ve kapanış zamanı |
|---|---|---|
| OQ-1 | Referans oynatıcı kontrollerinin tam paritesi ve yeni backend'in gerekli medya/oynatma desteği | Ürün sahibi + backend/mobil; epikler öncesi kapsam ve uygunluk kontrolü |
| OQ-2 | Crash metriğinin kaynağı ve kök nedenleri; %99,5 crash-free kullanıcı hedefi için değerlendirme süresi/örneklemi; bellek ve gecikme bütçeleri, cihaz/ağ matrisi | Mobil/QA + ürün sahibi; mimari ölçüm planı, yayın öncesi eşikler ve kanıt |
| OQ-3 | Yeni sisteme sonradan gelen ikinci cihazın yalnız yerel kayıtları; anonim kimlik ve alan eşleştirmesi; hatalı/eksik legacy kayıtların çözümü | Mobil/backend; migration tasarımı ve testleri öncesi |
| OQ-4 | Arama kapsamı: eski uygulamadaki yalnız hikâye araması mı, discovery'deki genel içerik araması mı; gerçek legacy request/response örnekleri ve yeni backend hedef sözleşmesiyle karar verilecek. Kategori sıralaması ile boş/hata durumları da doğrulanacak. Dil görünürlüğü ve fallback kararlaştırıldı | Ürün sahibi + backend/mobil; UX/API sözleşmesi öncesi |
| OQ-7 | Backend olay alanları, saklama süresi ve hata/tekrar politikası. Olay kapsamı ve abonelik görünür davranışı kararlaştırıldı; RevenueCat ürün/teklif kimlikleri gerçek entegrasyon sözleşmesinde belirlenecek | Ürün sahibi + backend/mobil; son entegrasyonlardan önce sözleşme |
| OQ-8 | Korumalı medya erişim yenileme, yayından çekme, metadata-only onay sınırı; manifest, gerçek işleme/ön hazırlık ve retention parametreleri. Kullanıcı davranışı kuralları kararlaştırıldı, teknik çözüm gerçek backend/media kanıtıyla mimari aşamasında belirlenecek | Backend/mobil mimarisi; ilgili epikler öncesi |
| OQ-9 | Mevcut veri/SDK kapsamı ve mağaza hedef kitle beyanı. Bildirim/duyuru/deep-link kullanıcı davranışı ile izin akışı referans sürümle aynı kalacak | Ürün sahibi + mobil; yayın akışları kesinleşmeden önce |
| OQ-10 | Eski sistemin kesin kapanış koşulu, yeni sistem hatasında geri dönüş yaklaşımı ve iki sistemin ortak içerik/operasyon yönetimi. Kademeli rollout ve genişletme kanıtı kararlaştırıldı | Ürün sahibi + operasyon/backend; rollout öncesi |

## Varsayımlar ve kanıt sınırları

Ürün sahibinin kabul ettiği replacement kapsamı, legacy belgelerdeki kullanıcı işlevlerini kapsar; her ayrıntının gerçek cihazda çalıştığı henüz doğrulanmamıştır. UJ taslakları kaynaklardan çıkarılmıştır ve son görüşmede gözden geçirilecektir. Yeni backend'in bu gereksinimleri hazır karşıladığı varsayılmamaktadır. Yaklaşık %85 crash-free ve yüksek çözünürlük hipotezi doğrulanmış bulgu değildir. Açık noktalar kapanmadan belge `final` durumuna alınmayacaktır.
