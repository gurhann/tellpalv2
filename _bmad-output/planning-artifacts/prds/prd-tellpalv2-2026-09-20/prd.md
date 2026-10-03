---
title: TellPal Mobile v1 Yeniden Yazım
status: draft
created: 2026-09-20
updated: 2026-10-03
---

# PRD: TellPal Mobile v1 Yeniden Yazım

Bu taslak, eski mobil uygulamanın yerini alacak sürüm için ürün kapsamını, kabul beklentilerini ve açık kararları bir araya getirir. Açık noktalar henüz karara bağlanmadığından uygulama geliştirmesine doğrudan onay veren nihai sözleşme değildir. Mevcut Türkçe karar belgesi aynı dilde genişletilmiştir.

Kaynak önceliği: bu görüşmede onaylanan kararlar → `be/docs/mobile-delivery-discovery.md` içindeki kesinleşmiş yön → `1.0.35+333` uygulaması ve `app/old/docs/` envanteri. Eski belgelerdeki teknoloji önerileri ve “backend yok” gibi eski repository açıklamaları yeni sistemin gerçeği sayılmaz. Yeni backend'in yetenekleri bu PRD çalışmasında kod veya runtime üzerinden doğrulanmamıştır.

Destekleyici kaynak özetleri: [akış ve kanıt envanteri](extract-legacy-flows.md), [veri ve sözleşme envanteri](extract-contracts.md). “Observed”, eski Android debug-emülatör raporunda gözlenmiş; “Partial”, akışın yalnız bir kısmı gözlenmiş; “Documented”, kaynak kod taramasında belgelenmiş anlamındadır. Bunlar bu çalışmada yeniden çalıştırılmış test sonuçları değildir. iOS ve gerçek cihaz paritesi henüz kanıtlanmamıştır.

## Ürün hedefi ve kapsam ilkesi

İlk sürüm, mevcut canlı TellPal mobil uygulamasının yerini alabilecek tam işlevli bir yeniden yazımdır. Mevcut görünüm, kullanıcı yolculukları ve kullanıcıya sunulan işlevler temel referanstır. Ürünün işleyişini değiştiren yenilikler sonraki aşamalarda ele alınacaktır.

Eski uygulamayla uyumluluk temel alınırken `be/docs/mobile-delivery-discovery.md` içinde açıkça kararlaştırılmış değişiklikler kabul edilmiş istisnalar olarak uygulanacaktır. Bu belgedeki açık sorular ve geçici varsayımlar onaylanmış değişiklik sayılmaz. PRD görüşmelerinde sonradan netleştirilen kararlar ilgili eski discovery kararlarını günceller; diğer görünür ürün değişiklikleri sonraki aşamalara bırakılır.

Kullanıcının zamanla ilgili açıklaması, PRD veya mühendislik çalışmasının aceleye getirilmesini gerektiren bir teslim tarihi değildir. İlk aşamada ürünün işleyişini yeniden tasarlamaya zaman ayrılmayacaktır; gerekli analiz, mimari değerlendirme ve güvenilirlik doğrulaması replacement hedefini desteklemek için yapılabilir.

Başarı, eski uygulamada yapılabilen işlemleri gerçekleştiren ve canlı sürümün yerini alabilecek bir sürümün yayınlanmasıdır. Yeni backende uyum ve crash problemlerinin azaltılması temel kalite hedefleridir. Mimari aşamada Flutter seçilmiş ve bu seçim `be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md` ile kabul edilmiştir. Bu teknoloji kararı, kullanıcıya görünen işlevlerin veya veri koruma yükümlülüğünün azaltılmasına izin vermez.

### Discovery ile uzlaştırılmış kararlar

19 Eylül discovery belgesinin dokuz yönlendirme bölümü 22 Eylül tarihinde PRD ve sonraki kullanıcı kararlarıyla karşılaştırılmıştır. Ayrıntılı karşılaştırma [discovery uzlaştırma notundadır](reconcile-discovery.md). Discovery'nin teknik yönü [addendum.md](addendum.md) içinde mimari girdisi olarak korunur; endpoint veya şema burada kesinleştirilmez.

- RevenueCat ve kullanıcı akış kayıtları, sonraki karar gereği ilk release öncesi tamamlanır.
- Discovery genel içerik aramasını önerir; son kullanıcı kararı arama kapsamını gerçek request/response incelemesine bırakmıştır.
- Etkileşimli hikâyeler ve ebeveyn rehberleri son kullanıcı kararıyla kapsam dışıdır. Eski envanterde bulunmaları bu işlevleri geri getirmez.
- Manifest, medya erişimi ve işleme için discovery yönü korunur; kesin şema, süreler ve teknik mekanizmalar mimari aşamasında gerçek çıktılarla doğrulanır.
- Migration, oynatma yaşam döngüsü, dil görünürlüğü ve %99,5 kararlılık hedefi sonraki görüşmelerle netleştirilmiştir. Bunlar discovery'deki eski açık sorular nedeniyle yeniden belirsiz sayılmaz.

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
- Kullanıcı akışlarının backende loglanması: akış kapsamı FR-21'de onaylanmıştır; teknik olay alanları ve saklama politikası sözleşmede tanımlanacak, entegrasyon ve doğrulama release öncesi son adımlarda tamamlanacaktır.

Bu entegrasyonların ihtiyaçları ilgili gereksinim ve mimari kararlarında baştan dikkate alınacaktır. Crash/hata tanılama, kullanıcı akışlarının loglanmasından ayrı bir güvenilirlik ihtiyacıdır; kullanıcı akışlarının loglanmasına ilişkin son aşama kararı crash tanılamanın zamanlamasını belirlemez.

## Mevcut verilerin korunması ve sürüm geçişi

Kullanıcı, eski sistemdeki her şeyin korunmasını istemektedir. Replacement sürümüne geçişte mevcut veri kapsamı daraltılmayacak; hesap, abonelik, profil veya kullanım geçmişi sıfırlanması kabul edilmeyecektir. Koruma, yalnızca eski verinin bir yedeğinin bulunmasını değil, kullanıcıya ait işlevsel bilgilerin yeni uygulamada kullanılabilir kalmasını da kapsar.

Eski uygulamanın `app/old/docs/data-models.md` envanterine göre geçiş incelemesi en az şu alanları kapsayacaktır:

- Hesap kimlikleri, oturum devamlılığı, anonim kullanım durumu, abonelikler ve erişim hakları.
- Her iki nesil profil verisi: ad, avatar, doğum tarihi/yaş aralığı, müşteri tipi, kullanım amaçları, favori türler ve pazarlama tercihleri.
- Dil dahil mevcut kullanıcı tercihleri. Eski çocuk/ebeveyn modu değeri tarihsel veri olarak değerlendirilebilir; ebeveyn modu yeni uygulamada etkinleştirilmeyecektir.
- Okunan içerik listeleri, okuma/dinleme geçmişi, tamamlanma durumu, süreler ve saklanmış kalan sayfa/ilerleme bilgisi.
- Geri bildirim kayıtları, promosyon kayıtları ve mevcut içeriklerle kullanıcı geçmişi arasındaki kimlik ilişkileri.
- Cihazda saklanan tercihler, açılış/onboarding durumu ve yerel kullanıcı kayıtları. Eski indirilmiş medya ve yeniden üretilebilir önbellekler aşağıdaki temizlik kararı kapsamındadır.

Bu envanter dokümantasyon kanıtıdır; canlı sistem ve cihazlarda bulunan verilerin eksiksiz listesi olarak kabul edilmeyecektir. Geçiş planında tüm kaynaklar ve şema nesilleri doğrulanacak, eski ve yeni kimlikler eşleştirilecek, eksik veya eşleştirilemeyen kayıtlar sessizce atılmayacaktır. Özellikle yalnız cihazda bulunan anonim kullanıcı verilerinin korunması ayrıca doğrulanacaktır.

2026-10-02 kullanıcı kararı: Kaldırılan içeriklere, etkileşimli hikâyelere ve ebeveyn rehberlerine ait kayıtlar geçmişte görünür ve istatistiklerde korunur. Artık mevcut olmayan içerik açılamaz; geçmiş kaydı silinmez.

Veriyi koruma gereksinimi aynı veritabanı şemasını veya dosya biçimini kullanma zorunluluğu getirmez. Teknik temsil değişebilir; kullanıcı verisinin anlamı ve ilişkileri korunmalıdır.

Kullanıcı, eski uygulamanın cihazda indirdiği medya dosyaları ve yeniden üretilebilir önbelleklerin taşınmasına gerek olmadığını, artık ihtiyaç duyulmayacak bu verilerin silinebileceğini belirtmiştir. Güncellemede eski medya/önbellekler temizlenecek; yeni uygulama ihtiyaç duyduğu medyayı yeni sistemden edinecektir. Bu karar profil, tercihler, geçmiş, ilerleme, anonim kimlik veya yalnız cihazda bulunan kullanıcı kayıtlarının silinmesine izin vermez. Temizlik hedefleri kullanıcı kayıtlarından ayrılacak ve migration için gerekli veri korunmadan temizlenmeyecektir. Eski sunucu verileri ve eski sürümü kullanan diğer cihazlar bu temizliğin kapsamında değildir.

Yayın kabulü, eski canlı sürüm üzerine güncelleme senaryolarında mevcut verilerin ve erişim haklarının korunduğunun doğrulanmasını içerecektir. Ayrıntılı geçiş testleri ve ölçülebilir kabul kriterleri PRD'nin devamında tanımlanacaktır.

### Anonim kullanıcıların geçişi

Eski uygulamayı hesapsız kullanan kişi, yeni sürüme geçtiğinde kayıt olmaya zorlanmadan anonim kullanıma devam edecektir. Yeni anonim kullanım Firebase Anonymous Auth kimliğiyle ilişkilendirilecek; cihazda mevcut profil, tercihler, geçmiş ve diğer korunacak kullanıcı kayıtları yeni anonim uygulama hesabına taşınacaktır. Bu karar, belgelerde eski anonim kullanımın yerel bir işaretle yönetildiği bilgisini dikkate alır; eski kullanıcı için doğrulanmış bir Firebase UID bulunduğu varsayılmayacaktır.

Kesinti veya yeniden deneme kullanıcı kayıtlarını çoğaltmamalı veya kaybettirmemelidir. Anonim geçişin tamamlandığı doğrulanmadan gerekli yerel kayıtlar temizlenmeyecektir. Eski indirilmiş medya ve yeniden üretilebilir önbelleklerin temizlenmesine ilişkin karar geçerlidir. Anonim kullanıcı daha sonra yeni kayıtlı hesaba dönüştüğünde profil, dil dahil tercihler, geçmiş ve ilerleme korunur; var olan hesaba girişte o hesabın verileri kullanılır ve otomatik birleştirme yapılmaz.

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

Açık geçiş kararları: cihaza özgü yerel kayıtların ve anonim kimliklerin teknik eşleştirme yöntemi; eski sisteme geri dönüş davranışı. Eski sistem için sabit kapanış tarihi belirlenmez: yeni sürümün kademeli yayını tamamlanıp migration ve abonelik akışlarının kararlılığı doğrulandıktan sonra kullanıcının ayrı kapanış onayı alınır. Bu onaya kadar eski sistem çalışmaya devam eder (2026-10-02 kararı). Anonim geçişin kullanıcıya dönük davranışı yukarıda kararlaştırılmıştır. Kesilmiş bir migration'ın güvenli yeniden denenmesi, başarıyla tamamlanmış migration'ın tekrar çalıştırılmasından ayrılacaktır.

## Kullanıcılar ve temel ihtiyaçlar

Mevcut çocuk deneyimini kullanan kişiler, bildikleri ekranlardan hikâye okuyabilmeli, sesli içerik dinleyebilmeli ve hesaplarını yönetebilmelidir. Mevcut aboneler erişim haklarını; anonim kullanıcılar kayıt zorunluluğu olmadan kullanımını korumalıdır. İçerik ekibi CMS'de hazırladığı içeriği cihazda test edebilmeli ve onayladığı sürümü yayınlayabilmelidir.

İlk sürüm yeni bir hedef kitle veya kullanıcı alışkanlığı yaratmayı hedeflemez; keşifte yalnız onaylanmış discovery değişiklikleri uygulanır. Tek hesapta bir profil vardır; profil yaşı bilgilendiricidir. Ebeveyn modu kapsam dışıdır.

### Kaynaklardan çıkarılmış yolculuk taslakları

Aşağıdaki akışlar legacy belgelerinden ve kabul edilen geçiş kararlarından çıkarılmıştır; kullanıcı araştırması veya yeni persona icadı değildir. Ayrıntıları referans sürümle kontrol edilecek, son incelemede onaylanacaktır.

- **UJ-1 — Mevcut kullanıcının güncellemesi:** Yeni sürüm açılır, hesap/geçiş durumu belirlenir, gerekiyorsa taşıma ekranı gösterilir. Başarılı geçişten sonra profil, geçmiş ve erişim hakları kullanılabilir. Kesinti tekrar denemeye götürür; boş profil oluşturmaz.
- **UJ-2 — Anonim başlangıç ve daha sonra kayıt:** İlk açılış akışı normal kataloğa girmeden anonim devam veya kayıt/giriş sunar. Anonim kullanıcı katalogdan ücretsiz içerik tüketir; daha sonra yeni kayıtlı hesaba dönüşürken profil, tercihler, geçmiş ve ilerlemesini korur. Var olan hesaba giriş, o hesabın verilerini kullanır; otomatik birleştirme yapılmaz.
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
- **Manifest:** Bir revizyona ait sıralı sayfaları, medya kimliklerini ve hazır olma/bütünlük bilgisini tanımlayan teslim kaydı; kesin şeması mimari aşamasında belirlenir.
- **Parite:** Onaylı istisnalar dışında referans sürümün görünüm ve kullanıcı işlevlerinin karşılanması.

## İşlevsel gereksinimler

Aşağıdaki gereksinimler ürün davranışını tanımlar. Endpoint, DTO, mobil framework ve veri taşıma mekanizması mimari çıktılarda belirlenecektir. Legacy envanterinden türetilen ayrıntılar runtime kanıtı eksik olsa da sessizce kapsamdan çıkarılamaz; farklılıklar açık karara bağlanır. FR kimlikleri sabittir; kapsamdan çıkarılan FR-17 yeniden numaralandırılmaz.

### Açılış, kimlik ve profil — UJ-1, UJ-2, UJ-5

#### FR-1 — Açılış ve navigasyon
İlk kurulum, geri dönen kullanıcı ve migration gereken kullanıcı doğru giriş akışına yönlendirilir. Onboarding, ana hikâye, rahatlama ve profil yüzeyleri ile geri navigasyon referans sürümle karşılaştırılır. Yeniden açılış yanlışlıkla onboarding veya boş profil başlatmaz.

İlk kurulumda normal kataloğa girişten önce kayıt/giriş veya anonim devam seçimi yapılır. Bu seçim, geri dönen kullanıcının geçerli oturumunu gereksiz yere sıfırlamaz.

#### FR-2 — Kimlik doğrulama
E-posta, Google ve Apple giriş/kayıt; e-posta doğrulama ve parola sıfırlama desteklenir. Referans kayıt formundaki zorunlu onay, ayrı pazarlama tercihi ve e-posta/parola doğrulamaları karşılanır; eksik zorunlu alan/onay başarıya dönüşmez. İptal/hata yeni başarılı oturum sayılmaz. Firebase kimliği doğru uygulama hesabıyla eşleşir; benzer e-posta adresleri nedeniyle hesaplar sessizce birleştirilmez.

Kimlik doğrulama otoritesi Firebase Auth'tur; doğrulanmış Firebase UID, backend'deki uygulama kullanıcısı/profiliyle eşleştirilir. Yeni sistemde uygulama profili için esas kayıt backend'dedir. Eski sistemin paralel çalışması bu yeni profil sahipliğini değiştirmez.

#### FR-3 — Anonim kullanım ve dönüşüm
Anonim devam Firebase Anonymous Auth ile mümkün olur. Anonim kullanıcı yeni kayıtlı hesap oluşturduğunda anonim dönemdeki profil, tercihler, geçmiş ve ilerleme yeni hesaba taşınır. Kullanıcı var olan hesaba giriş yaptığında o hesabın verileri kullanılır; anonim veri otomatik olarak birleştirilmez. Bu davranış kullanıcı tarafından onaylanmıştır. Eski uygulamadan anonim migration sırasında geçmişin korunması kararı da geçerlidir.

#### FR-4 — Profil kurulumu ve düzenleme
Referans sürümün profil kurulum alanları, atlanabilir adımları, avatar ve ad düzenleme işlevleri korunur. Migration her iki eski profil şemasının bilgisini korur; atlanan alanlar uygulamayı kullanmayı engellemez. Bir hesap için bir profil kullanılır.

#### FR-5 — Hesap ve tercih yönetimi
Çıkış, parola değiştirme, hesap silme, dil değiştirme ve bildirim tercihi mevcut işlevleriyle bulunur. Eski istemci bildirim iznini açılışta ertelenmiş görev olarak, OS durumu `denied` ise ister; profil anahtarı OS izin durumunu gösterir ve gerektiğinde cihaz ayarlarına yönlendirir. Bildirim tercihi referans kapsamda OS izin anahtarıdır; ayrı, hesap bazlı bir uygulama anahtarı eklenmez. Hesap değişiminde önceki hesabın özel verileri görünmez. Hesap silme yalnız yeni sistemdeki hesabı ve o sistemdeki kullanıcı verilerini kapsar. Eski sistemdeki kayıtlar ile eski uygulama sürümünü kullanan cihazlar etkilenmez. Kullanıcı bu kapsamı onaylamıştır.

Paylaşılan Firebase kimliğiyle bu etkinin nasıl sınırlandırılacağı ve yeniden girişin silinen veriyi eski sistemden geri taşımamasının nasıl sağlanacağı OQ-6'da teknik doğrulama olarak tutulur. Hesap silme kararından mağaza aboneliğinin otomatik iptali sonucu çıkarılmaz; kullanıcıya açıklanan abonelik etkisi sözleşmede netleştirilecektir.

### Katalog ve keşif — UJ-3, UJ-4

#### FR-6 — Ana ekran ve kategori
Ana ekran CMS sıralı kategori satırları ve sınırlı önizleme içerikleri sunar; tam kategoriye geçiş vardır. Dil/erişim kitlesi için uygun içeriği olmayan kategori gösterilmez. Tam listede içeriklerin atlanmaması ve tekrarlanmaması sayfalama kabulüne dahildir.

2026-10-03 tarihinde kullanıcı, canlı Library'de kategori başına 5 önizleme kullanıldığını doğruladı ve yeni Library için backend'den yapılandırılabilir başlangıç limitini 5 olarak onayladı. Limit uygunluk filtrelerinden sonra uygulanır; daha az uygun içerik varsa yalnız mevcut içerikler gösterilir, boş kategori gösterilmez. Bu doğrulama kullanıcı beyanıdır; bu görüşmede canlı backend/yapılandırma sorgusu çalıştırılmadı. Kategoriye özel limitler bu karara dahil değildir.

2026-10-03 tarihinde kullanıcı, Library ana ekranındaki kategori satırlarının sırasını CMS'den değiştirme ve kaydetme yeteneğinin eklenmesini onayladı. Kategori satırı sırası dil bazında bağımsız yönetilebilir; bir dilin sırasını değiştirmek diğer dillerin sırasını değiştirmez. Backend bu sırayı kalıcı saklar ve seçilen dilin mobil yanıtında uygular; kategori satırı sırası, kategori içindeki kart sırasından ayrı yönetilir. Görünmeyen veya boş kategoriler elendiğinde kalan satırların göreli sırası korunur. Bu yetenek Story 1.1'in backend/CMS eşlikçi işine dahildir; veri modeli, yüzey kapsamı ve yönetim API'si uygulama sözleşmesinde netleştirilecektir.

Liste yanıtları küçük içerik özetleridir; tam teslim/oynatma verisi detay veya başlatma akışında alınır. Ana ekran satır bileşimi, sayfalama ve hata sözleşmesi OQ-4'te belirlenecektir; CMS kategori sırası onaylı kuraldır.

Editör seçimi ve benzer içerik yüzeyleri referans envanterine dahildir; seçilen öneri doğru içerik/deneyime gider. Uygulanabilir içerik bulunmadığında bozuk kart veya geçersiz hedef gösterilmez.

#### FR-7 — İçerik araması
Kullanıcının eski uygulama ekran görüntüleri okuma hikâyesi, sesli hikâye, meditasyon ve ninni sonuçlarını doğrular. `/story/search` ve `StorySearchByCategoryType[]` istemci sözleşmesi bu grupları destekler; gerçek sunucu eşleşmesi ve alan kapsamı yine örnek yanıtlarla doğrulanacaktır. Önceki “yalnız hikâye” ifadesi geçerli kapsam değildir. Sonuçlar bağımsız kişi kaydı değil içeriktir; içerik ve katkı sağlayıcı bilgileri eşleşmede kullanılabilir. Sonuçlar uygun olduğunda deneyim/türe göre gruplanır; boş sonuç ve ağ hatası birbirinden ayrılır. Eski Flutter istemcisinde arama üç karakterden itibaren 500 ms beklemeyle başlar; yeni istemci bu görünür davranışı korur. Sunucu eşleşmesi, sıralama ve sayfalama gerçek yanıtlarla doğrulanacaktır.

#### FR-8 — İçerik ve deneyim ayrımı
Hem okuma hem sesli hikâye deneyimi olan STORY ilgili iki keşif grubunda görünebilir. Seçilen deneyim doğru detay/oynatmayı açar; diğer deneyimin işleme hatası hazır deneyimi engellemez.

#### FR-9 — Dil ve açık içerik dili seçimi
Uygulama dili ve normal katalog içerik dili birlikte yönetilir, tercih profilde saklanır. Sonraki kullanıcı kararları gereği Türkçe, İngilizce ve Portekizce uygulamanın mevcut, sürekli kullanılabilir dilleridir; uzaktan yapılandırma bu üç dili kapatmaz. Almanca veya başka bir dil yalnız çeviri dosyası bulunduğu için kendiliğinden açılmaz. Yeni bir dil önce yetkili test kitlesine, gerekli kontroller tamamlandıktan sonra tüm kullanıcılara açılır. Kullanıcının 2026-10-01 kararına göre testten genel kullanıma geçiş CMS’den yönetilir; CMS durumunun backend/yapılandırma üzerinden mobile güvenli aktarımı uygulama sözleşmesinde doğrulanır. Bu karar mobil sürümün mağaza dağıtımını CMS’ye taşımaz. Teste özel dil normal kullanıcıya dil seçiminde, normal veya özel dil kategorilerinde ya da doğrudan içerik isteğinde görünmez. Uzaktan yapılandırma alınamazsa yeni test dili yalnız aynı kullanıcıya ait önceden doğrulanmış ve hâlâ geçerli test yetkisi önbellekte varsa görünür; eksik, geçersiz, süresi dolmuş veya başka kullanıcıya ait yetki dili açmaz. Önbellek geçerlilik süresi ve doğrulama yöntemi uygulama sözleşmesinde belirlenecektir. Hiçbir dil ayarına erişilemiyorsa uygulama İngilizce başlar; uzaktan yapılandırma alınamadı diye mevcut üç dil kaybolmaz. Bu başlangıç kuralı, erişilebilir durumdaki kayıtlı kullanıcı tercihinin sessizce değiştirilmesi anlamına gelmez. Dil değişiminde önceki dile ait önbellek sonucu yeni dilmiş gibi gösterilmez.

2026-10-03 tarihinde kullanıcı otomatik içerik dili fallback'ini kesin olarak reddetti; bu karar önceki İngilizce içerik fallback iznini geçersiz kılar. Normal kategoriler yalnız seçilen dilde uygun ve yayınlı içerik gösterir. Örneğin 5 hikâyeden yalnız 3'ünün Türkçesi uygunsa Türkçe katalogda 3 kart gösterilir; diğer 2 hikâyenin İngilizcesi otomatik eklenmez. İlgili dilde uygun içerik kalmayan kategori gizlenir; detay/başlatma isteğinde eksik dil başka bir dille tamamlanmaz.

CMS, Türkçe uygulama kullanan kişiye açıkça “İngilizce Hikâyeler” gibi ayrı bir kategori sunabilir. Böyle bir kategori kasıtlı olarak İngilizce içerik gösterir; karttan detay ve oynatmaya aynı açık içerik dili taşınır, uygulama dili/tercihi otomatik değişmez. Bu kategori normal kategorilerin eksik çevirilerini doldurmaz; görünürlük, yayın, test kitlesi ve erişim kontrolleri yine uygulanır. Uygulama/kategori sunum dili ile kategorinin açık içerik dili arasındaki eşleme uygulama sözleşmesinde tanımlanacaktır.

#### FR-10 — Ücretsiz ve kilitli görünürlük
Ücretsiz ve kilitli içerik katalogda görünür; kilitli içeriği başlatma paywall açar. Dil bazlı CMS ücretsiz seçimi STORY'nin okuma ve sesli deneyimlerine uygulanır. Abone olmayanlara özel kategori CMS sırasıyla yalnız o kitleye gösterilir; aktif aboneye gösterilmez.

Bu kategori ücretsiz ve kilitli içerikleri birlikte barındırabilir. Anonim ve kayıtlı kullanıcılar aynı içerik erişim kurallarına tabidir; aynı dil, yayın ve erişim hakkı koşullarında yalnız giriş türü nedeniyle farklı içerik hakkı oluşmaz.

### Okuma ve dinleme — UJ-3, UJ-4

#### FR-11 — Detaydan başlatma
Hikâye, sesli hikâye ve meditasyon bilgi/detay yüzeyinden açık kullanıcı eylemiyle başlar. Ninni kartı doğrudan oynatıcıya ve kullanıcı tarafından başlatılmış dinlemeye gider. Detayı açmak kendiliğinden ses başlatmaz.

#### FR-12 — Sayfalı hikâye
Sayfa görseli ve anlatım sesi birlikte doğru sayfaya aittir. İlk sayfa hazır olunca başlayabilir; tüm hikâyeyi beklemek gerekmez. Anlatım bitişi animasyonla ilerler; manuel gezinme çalışır. Hazır olmayan sayfa yükleme durumuna geçer, sonsuz bekleme yerine toparlanabilir hata sunar.

İlk sürümün discovery yönü, revizyonlu manifest ile her sayfanın ayrı görsel ve anlatım sesinin teslimidir. İlk sayfanın görseli ve sesi hazır olduğunda tüketim başlar; sonraki sayfa ve sınırlı ileri hazırlık yapılır. Zayıf bağlantıda manuel geçişin yükleme göstermesi kabul edilebilir; bitişik sayfanın her koşulda hazır olması garanti edilmez. Manifestin detayda mı başlatmada mı alınacağı ve kesin şeması OQ-8'de açık kalır.

#### FR-13 — Hikâye kontrolleri ve tamamlanma
Normal resimli hikâyelerdeki eski arka plan müziği özelliği artık kullanım dışıdır; yeni okuyucuda ayrı müzik kontrolü gereksinimi yoktur. Anlatım kontrolü, otomatik ve yalnız kaydırmayla yapılan manuel sayfa ilerletme ile tamamlanma davranışları korunur. Alttaki sayfa noktaları konum göstergesidir; dokunarak sayfa seçimi yapmaz. Normal sayfada görsele tek dokunuş üst/alt kontrol panellerinin görünürlüğünü değiştirir, sayfayı değiştirmez. Kullanıcının iPhone gözlemine göre okuyucudaki Ses kapatıldığında hikâye anlatımı durur, görseller görünür kalır ve sayfa geçişleri manuel yapılır; aynı sayfada Ses yeniden açıldığında anlatım o sayfanın başından başlar. Ses açıkken sayfanın anlatımı bitince bir sonraki sayfaya otomatik geçilir ve o sayfanın anlatımı başlar; kullanıcı kaydırarak sayfa değiştirirse önceki sayfanın sesi kesilir ve gidilen sayfanın anlatımı baştan oynar. Ses açıkken son sayfanın anlatımı bitince tamamlanma ekranı kendiliğinden açılır; Ses kapalıyken son sayfadan bir kez daha ileri kaydırıldığında açılır. Hızlı gezinme ile otomatik ilerlemenin çakışması yanlış sayfa veya çift ses üretmez. Tamamlanma ekranında benzer hikâyeler gösterilir. Hikayeler Ekranına Dön eyleminden sonra koşula bağlı olarak puanlama daveti gösterilebilir; otomatik paywall akışı yalnız Android’de başlayabilir ve öncesinde ayrı bir premium teklif penceresi yoktur. Hikâye tamamlanmadan okuyucudaki Çıkış seçildiğinde de aynı otomatik paywall yapılandırması yalnız Android’de, premium olmayan kullanıcı için uygulanır; anlatım durur ve hikâye bilgi ekranına dönülür. iOS’ta hikâye bitişi veya erken çıkış otomatik paywall tetiklemez; iOS’taki diğer doğrudan paywall girişleri her açılışta yeni çocuk kilidi gerektirir. Android’de çocuk kilidi gösterilmez. Tamamlanma ekranındaki Hikayeler Ekranına Dön eyleminden açılan paywall’daki X ana sayfaya, Android okuyucusundaki erken Çıkış eyleminden açılan paywall’daki X ise hikâye bilgi ekranına döner. Android’de aynı dönüşte puanlama ve paywall koşulları birlikte sağlanırsa önce puanlama gösterilir ve paywall o dönüş için atlanır; akışlar üst üste veya art arda açılmaz. Eski kaynak kodundaki tetikleme dalları incelenmiştir; etkin Remote Config değerleri ve gerçek cihazdaki tüm sonuçlar henüz doğrulanmamıştır. Ekranı kaydırmak bu akışları başlatmaz. Etkileşimli hikâye dalları ve seçimleri replacement kapsamı dışındadır.

#### FR-14 — Sesli oynatıcı
Referans sürümün ninni, meditasyon ve sesli hikâye oynatma kontrolleri aynı kalır: oynat/duraklat, ses konumuna gitme, oynatıcıdan çıkış ve ilgili meta bilgiler gösterilir. Ninni döngüsel oynar; meditasyon ve sesli hikâye döngüsel oynamaz. Arka plan ve sistem medya kontrolleri desteklenir. Çağrı geldiğinde veya kulaklık çıkarıldığında oynatma duraklar; kullanıcı kendi eylemiyle yeniden başlatır. Ekrandan çıkış, geri dönüş ve başka içerik açılışında birbiriyle yarışan oynatıcılar oluşmaz.

#### FR-15 — Medya hazırlığı ve görsel sunum
İlk sayfa ve sınırlı ileri hazırlık Wi-Fi ve hücresel bağlantıda kullanılabilir; tüm hikâyeyi spekülatif indirme anlamına gelmez. Detayda sınırlı ses hazırlığı başlatma anında yeniden kullanılır. Ninni liste ve oynatıcı görselleri ayrıdır; mevcut animasyon tek statik görsele indirgenmez. Hikâye görsel oranı korunur; metin güvenli alanı ve tablet kırpması cihazlarla doğrulanır.

Sesli hikâye ve meditasyon detayındaki hazırlık tüm ses dosyasının indirilmesini gerektirmez ve otomatik oynatma başlatmaz. Telefon ve tablette aynı portre hikâye görseli, oranı korunarak ekranı dolduracak şekilde kırpılır. Merkezden kırpma geçici varsayımdır; metin güvenli alanlarıyla temsilî cihazlarda doğrulanmadan kesin kural sayılmaz.

#### FR-16 — İlerleme, geçmiş ve istatistikler
Okuma/dinleme geçmişi, tamamlanma, saklanan ilerleme ve profil istatistikleri referans işlevleriyle korunur. Tek olayın tekrar işlenmesi tamamlanma veya süreyi iki kez saymaz. Uygulama tamamen kapatılıp yeniden açıldığında ana ekrandan açılır; kullanıcı doğrudan önceki hikâye/oynatıcıya geri götürülmez. Uygulama arka planda kaldığında açık hikâye veya oynatma oturumu kaldığı yerden devam eder. Kullanıcı bu davranışı onaylamıştır.

### Yardımcı yüzeyler — UJ-5

#### FR-17 — Geri çekildi: ebeveyn rehberleri
Kullanıcı kararıyla kapsam dışıdır; bu kimlik yeniden kullanılmaz. Eski belgelerdeki ebeveyn rehberi akışları uygulama gereksinimi değildir. Tarihsel kullanıcı kayıtlarının korunması OQ-3'te ayrıca ele alınır.

#### FR-18 — Destek ve iletişim
Geri bildirim gönderimi, doğrulama ve mevcut sınırlar; yerelleştirilmiş yasal/yardım belgeleri; uygulama değerlendirme çağrısı ve sürüm bilgisi; eski kaynaktan doğrulanan duyuru ve bildirim izni giriş noktaları korunur. Kullanıcı 2026-10-01 tarihinde bildirimden hedefe gitme ve deep-link açılışını eski uygulamada bulunmayan yeni sürüm özelliği olarak onaylamıştır; hedef/format ve güvenli yönlendirme sözleşmesi uygulama sırasında doğrulanacaktır. Kullanıcının iPhone gözlemine göre tamamlanma sonrası puanlama davetindeki X ana sayfaya döner; “Bizi Değerlendir” seçilince boş yıldızlı uygulama içi puanlama penceresi açılır; “Şimdi Değil” ana sayfaya döner. Yıldız seçilince mavi yıldızlı “Gönder” durumuna geçilir; bu durumdaki “Vazgeç” ana sayfaya döner. “Gönder”e basılınca “Yorum Yaz”/“Tamam” eylemleri olan teşekkür ekranı açılır. “Yorum Yaz” App Store’u açar; “Tamam” ana sayfaya döner. Mağazaya yönlendirme tek başına gönderilmiş değerlendirme kanıtı değildir. Başarısız gönderim başarılı gösterilmez. Onaylanan bildirim/deep-link hedefleri geçerli ekrana gider veya anlaşılır durum sunar; duyuru hedefi ancak ayrı bir ürün kararıyla eklenir. Eski kaynakta duyuru yalnız otomatik dönen görsel karusel ve kapatılabilir büyük görsel olarak doğrulanmıştır; hedefe yönlendiren duyuru veya bildirim/deep-link açılış işleyicisi aynı Flutter kaynak taramasında bulunmamıştır. Bu nedenle duyuru hikâyesinin referans kapsamı görsel karusel ve büyük görsel görüntülemedir; hedefe yönlendirme ancak ayrı bir ürün kararıyla eklenir. Portekizce duyuru görseli yoksa İngilizce görsel onun yerine gösterilmez; uygun görsel bulunmayan duyuru atlanır. Bildirim/deep-link yönlendirmesinin ürün kapsamı onaylıdır; hedef/format ve entegrasyon sözleşmesi gerekir. Teknik sağlayıcı veya SDK seçimi mimari aşamasında değişebilir. Mevcut operasyonel yapılandırmanın görünür özellikleri, ücretsiz içerikleri, duyuruları ve paywall tetiklerini yönetme ihtiyaçları envantere dahildir; aynı Remote Config anahtarlarının kullanılması şart değildir.

Eski uygulama kaynak kodunda hikâye bitişi puanlama daveti `rate_my_app.shouldOpenDialog` ile değerlendirilir. `RateMyApp.init()` bu düğmeye her basıldığında çağrıldığından paket sayacı uygulama açılışı yerine bu dönüşlerde artar. Kullanıcının 2026-09-30 kararıyla yeni sürümde de eşik gerçek uygulama açılışına değil tamamlanan hikâyeden “Hikayeler Ekranına Dön” basışlarına bağlıdır. Eşik ve erteleme süresi yapılandırmadan alınır; yerel varsayılanlar üç dönüş ve yedi gündür, etkin Remote Config değerleri doğrulanmamıştır. Davetteki X erteleme olayını kaydeder; yeniden gösterim için süre ile ek hikâye dönüşü eşiği birlikte sağlanır. “Bizi Değerlendir” seçildikten sonra sistem değerlendirme penceresi kullanılabiliyorsa tekrar göstermeme durumu, sistem penceresindeki “Gönder” sonucu beklenmeden kaydedilir; mağazaya gerçekten değerlendirme gönderildiği bilinmez.

### Abonelik ve olay kaydı — UJ-6

#### FR-19 — Satın alma ve geri yükleme
RevenueCat üzerinden mevcut aboneliklerin tanınması, satın alma, geri yükleme ve erişim yenileme yayın öncesinde tamamlanır. Kullanıcının 2026-10-02 onayına göre, eski uygulamadaki gibi hesap oluşturmadan abonelik satın alınabilir; satın alma öncesinde kayıt zorunlu tutulmaz. İptal edilmiş ama ücretli dönemi bitmemiş abonelik erişimi sürer. Mağaza hatası veya satın alma iptali erişim verilmiş gibi gösterilmez; anonim/kayıtlı kimlik tek başına premium hakkı yaratmaz.

2026-10-02 restore kararı: RevenueCat için hedef davranış “Transfer to new App User ID” seçeneğidir. Aynı mağaza hesabının geçerli aboneliği farklı TellPal hesabından geri yüklendiğinde yeni hesap premium erişim kazanır ve önceki hesabın erişimi kaldırılır. İki ayrı özel RevenueCat kimliği arasında yalnız giriş yapmak aboneliği taşımaz. Profil ve okuma geçmişi bu işlemle birleştirilmez veya taşınmaz. Canlı/sandbox ayarı, mevcut kimlik/alias ilişkileri ve backend uzlaştırması entegrasyonda doğrulanır.

#### FR-20 — Promosyon ve paywall
Referans sürümde promosyon kodu, aktif kod kaydındaki `offering_id` ile RevenueCat teklifini açar; aynı istemci yolunda doğrudan premium hak verme davranışı görülmemiştir. Paywall giriş ve kapatma ile üyelik yönetimi işlevleri envanterden doğrulanarak korunur. Hatalı/geçersiz promosyon tanımlı hata sunar. RevenueCat ürün/teklif kimlikleri ile teknik tetikleme/entegrasyon ayrıntıları gerçek entegrasyon sözleşmesinde belirlenecektir. Kullanıcı bu yaklaşımı onaylamıştır.

Kullanıcının 2026-09-30 açıklamasına göre iOS ve Android paywall arayüzü RevenueCat üzerinden yönetilir; uygulama içinde ayrıca kodlanmış bir paywall ekranı yoktur. Referans görüntüde kapatma, haftalık/aylık/yıllık seçenekler, abonelik başlatma, geri yükleme ve yasal bağlantılar görülür. Görüntüdeki fiyatlar, indirim oranları ve seçili plan sabit gereksinim değildir; kabul sırasında gerçek RevenueCat teklifi ve mağaza ürünü esas alınır.

Kullanıcının 2026-09-28 ve 2026-09-29 kararlarıyla, eski iOS uygulamasındaki ebeveyn kapısı replacement kapsamındadır; bu kapı, kapsam dışı ebeveyn modu/rehberlerinden ayrıdır. iOS'ta doğrudan paywall her açıldığında ebeveyn kapısı yeni bir dört rakamlı diziyle yeniden tamamlanır; önceki başarılı geçiş veya sayı dizisi aynı oturumda bile sonraki açılışı serbest bırakmaz. Referans iPhone ekranında dört rakam yazıyla gösterilir; dört giriş alanı ve Vazgeç eylemi vardır. Kilidin başlığı, yönergesi, sayı sözcükleri ve eylemleri seçili uygulama diline göre yerelleştirilir. Kullanıcının cihaz gözlemine göre doğru rakamlar girilince ayrı onay olmadan paywall açılır. Yanlış girişte pencere açık, paywall kapalı kalır; kullanıcı girişi silip yeniden deneyebilir. Vazgeç seçilince pencere kapanır, paywall açılmaz ve kullanıcı geldiği ekranda kalır. Android'de bu kapı gösterilmez. Promosyon kodu giriş ekranı her kullanımda kendi kilit işlevini görür; bu ekrandan açılan paywall öncesinde ikinci bir ebeveyn kapısı gösterilmez. Epic 4'te yalnız doğrudan paywall açan duyuru ve bağlantı hedefleri kapıdan geçer; diğer uygulama dışı hedeflere ek kapı konulması kullanıcı kararıyla App Review bu nedenle ret verene kadar ertelenmiştir. Bu tercih, Apple Kids Category 1.3 metnindeki uygulama dışı bağlantı kuralıyla bilinen bir yayın riski oluşturur ve uyumun doğrulandığı şeklinde raporlanamaz. Bu nedenle App Review ret verirse kapı eklenip yeniden gönderimden önce doğrulanır. Her paywall açılışında yeniden sorma, kullanıcının ayrıca verdiği ürün kararıdır.

#### FR-21 — Kullanıcı akışlarının backend kaydı
Giriş/kayıt, migration, katalog-arama-detay görüntüleme, okuma/dinleme başlatma-tamamlama, paywall/satın alma/restore ve hata-yeniden deneme akışları release öncesinde backende kaydedilir. Olaylar içerik metni, medya URL'leri veya gereksiz kişisel veri içermez. Veri alanları ve hata/tekrar politikası gerçek backend sözleşmesinde belirlenecektir; analiz olaylarının saklanmasında aşağıdaki onaylı karar geçerlidir. Kayıt başarısızlığı okuma/dinlemeyi durdurmamalı; yeniden deneme aynı olayı çoğaltmamalıdır. Crash tanılaması bu son entegrasyona bağımlı değildir.

2026-10-02 analiz kararı: Google Analytics’teki mevcut verilere ek olarak backend olay kayıtları bağımsız bir kaynak olarak uzun dönemli ürün analizlerini destekler. Sunucuda kaydedilmiş analiz olayları için şimdilik otomatik silme veya yaşa bağlı süre sınırı uygulanmaz; 90 günlük öneri kabul edilmemiştir. Mevcut backend veritabanıyla başlanır. Analiz verilerinin uygulama/backend performansını olumsuz etkilediği ölçümlerle görülürse ayrı bir analiz veritabanı değerlendirilir; bu ayrıştırma ilk sürümün önkoşulu değildir. Mobil bekleyen olay kuyruğunun sınırları ve ayrıntılı teknik tanılama kayıtlarının kuralları ayrı sözleşmelerdir.

### Yayın ve erişim — UJ-7

#### FR-22 — Dil ve deneyim bazlı yayın
Okuma ve sesli hikâye yayınları dil/deneyim bazında bağımsızdır. Bir kapsamda en fazla bir aktif aday bulunur. Adaydaki önemli değişiklik yeni bir aday revizyon oluşturur ve etkilenen test/onayları sıfırlar. Yeni aday hazırlanırken mevcut canlı revizyon kullanılabilir kalır; ilgili yayın kapsamıyla sınırlı rollback önceki canlı revizyonu geri getirebilir.

#### FR-23 — Tester erişimi ve onay
Tester, kilitli adaylar dahil yetkili olduğu test revizyonlarını açabilir; test/revizyon göstergesi görür. Normal kullanıcı aday alamaz. Gösterge yetkilendirme yerine geçmez. Medya değişikliği hazırlık ve yeniden cihaz testi gerektirir; genel yayın gerekli hazırlık ve açık onaydan sonra yapılır.

Test yayını genel yayından ayrı ara durumdur. Tester yetkisi abonelik hakkından ayrıdır; görünür test/revizyon göstergesi detay veya oynatıcıda sunulur. Üretilen medyayı değiştirmeyen yalnız metadata değişiklikleri daha hafif onaydan geçebilir; hangi alanların bu koşulu sağladığı OQ-8'de tanımlanır.

#### FR-24 — İşleme başarısızlığı
Medya hazırlama başarısızlığı ilgili adayın test/genel yayınını engeller; CMS hata ve yeniden deneme yolunu gösterir. Başarısız aday mevcut canlı içeriği değiştirmez. Üretilmiş medya hazır kabul edilmeden gerçekten erişilebilir ve bütün olmalıdır.

Hazırlanacak ve doğrulanacak çıktılar: liste küçük görselleri; gerektiğinde detay/oynatıcı görselleri; sayfa görselleri ve anlatım sesleri; gerektiğinde animasyonlu dinleme görselleri; sıralı sayfa, varlık kimliği ve hazır olma/bütünlük bilgisi taşıyan revizyonlu manifest. Yalnız işleme planı veya kayıt oluşturulması, dosyaların üretilmiş ve kullanılabilir olduğuna kanıt değildir. Boyutlar, codec, kalite bütçesi ve işleme orkestrasyonu OQ-8'de belirlenecektir.

#### FR-25 — Korumalı medya ve revizyon sürekliliği
Liste/detay görünürlüğü korumalı tam medya erişimi vermez. Başlatma yetkisi kontrol edilir; aktif oynatma kabul edilen revizyona bağlı kalır. Süresi dolan medya erişimi, yayından çekme ve güvenlik istisnaları OQ-8'de tamamlanacaktır. Temizlik canlı, rollback ve oturum için gerekli revizyonları korur.

Korumalı medya backend erişim kontrolünden sonra kısa ömürlü URL'lerle sunulur; URL ömrü ve yenileme mekanizması henüz seçilmemiştir. Metadata erişimi, detayda ön hazırlık veya cache varlığı korumalı tam medyaya erişim kontrolünü atlatamaz. İlk sürüm yayınına kadar backend'in güvenilir abonelik erişimi kontrolü tamamlanmalıdır; RevenueCat'in son adım olması erişim kontrolünü isteğe bağlı yapmaz. Saklama ve temizlik parametreleri backend/ortam yapılandırmasından yönetilir.

### Replacement ve migration — UJ-1, UJ-2

#### FR-26 — Hesap bazlı tek geçiş
İlk yeni sürüm kullanımı hesabın ilk migration'ını tetikler ve o ana kadar olan korunacak verileri taşır. Başarılı hesabı ikinci cihazda açmak migration'ı tekrarlamaz. Eski cihaz eski sistemde kalır; sonradan oluşan değişiklikler eşitlenmez.

#### FR-27 — Kayıpsız ve yeniden denenebilir taşıma
Eski/yeni içerik ve hesap kimlikleri doğru eşleşir. Tekrar deneme kayıt çoğaltmaz, daha yeni veriyi ezmez ve eksik eşleşmeleri sessizce atmaz. Kullanıcının 2026-10-02 kararına göre ikinci cihaz güncellendiğinde yeni sistemdeki mevcut hesap verisiyle devam eder. İkinci cihazdaki eski yerel kayıtlar, hesapta karşılığı bulunmasa da hesaba aktarılmaz veya birleştirilmez; hesap migration’ı tekrarlanmaz. Yerel temizlik ayrı kurallara tabidir. “Verileriniz taşınıyor” ve “Tekrar dene” deneyimleri uygulanır; tamamlanmamış migration boş profille normal kullanıma geçemez.

#### FR-28 — Yerel anonim veri ve temizlik
Anonim kullanıcı kayıt zorunluluğu olmadan geçer; korunacak yerel kayıtlar yeni anonim hesaba aktarılır. Eski medya/yeniden üretilebilir önbellek temizlenebilir; migration girdileri aktarım doğrulanmadan silinmez. Eski backend ve eski sürümlü diğer cihazların verisi bu temizliğe dahil değildir.

#### FR-29 — Mağaza güncellemesi olarak teslim
Replacement mevcut uygulamanın üzerine güncelleme olarak kurulabilmelidir; ayrı uygulama kurma ve hesabı baştan oluşturma zorunluluğu getirmez. Platform kimliği, imzalama, oturum ve yerel veri erişiminin seçilen teknolojiyle korunabildiği yayın öncesi doğrulanır. Eski sistemin eşzamanlı hizmeti devam eder.

## Çapraz kalite gereksinimleri

2026-10-02 performans kararı: Bellek ve açılış/oynatma gecikmesi için sayısal kabul sınırları, ilk çalışan okuma/dinleme akışlarının onaylı test ortamlarında ölçülmesinden sonra belirlenir. Ölçüm sonuçları ve önerilen sınırlar ilgili story kabulünden önce kaydedilip değerlendirilir. Açıklanamayan sürekli bellek artışı, donma ve kritik oynatma hataları kabulü engeller; bu karar sayısal sınırların zaten belirlendiği anlamına gelmez.

2026-10-02 test ortamı kararı: Cihaz erişimi sınırlı olduğu için Android emülatörleri ve iOS Simulator ana test ortamlarıdır. Eski/düşük kapasiteli ve güncel telefon/tablet profilleri bu ortamlarda kapsanır; mevcut fiziksel cihazlar tamamlayıcı kontrollerde kullanılır. Bellek ve gecikme sonuçları host, sanal cihaz/OS, build modu ve ağ/cache koşullarıyla kaydedilir; fiziksel cihaz performansı olarak sunulmaz. İmzalı mağaza güncellemesi, gerçek satın alma ve platforma özgü davranış için gereken kanıtlar ayrıca izlenir.

- **NFR-1 — Kararlılık:** Etkin FR-1–FR-29 gereksinimlerinin (geri çekilmiş FR-17 hariç) kritik kabul senaryolarında bilinen crash, donma veya veri kaybı engeli kalmaz. Platform bazında en az %99,5 crash-free kullanıcı hedefi kararlaştırılmıştır; OQ-2'de açık olanlar ölçüm süresi/örneklemi ve cihaz bazlı bütçelerdir. Test cihazlarında bellek kaynaklı kapanma veya kritik oynatma hatası yayın engelidir.
- **NFR-2 — Bellek:** Düşük bellekli cihaz, en ağır içerik ve tekrarlı oturum testleri yapılır. Isınma sonrası tekrar döngülerinde açıklanamayan sürekli bellek artışı giderilir. Cihaz sınıfı başına tepe/kararlı kullanım bütçeleri OQ-2'de belirlenir.
- **NFR-3 — Akıcılık ve başlatma:** Soğuk/sıcak açılış, ilk sayfa/ilk ses, sayfa geçişi ve yeniden bağlanma ölçülür. Cihaz, ağ ve önbellek koşullarıyla p50/p95 hedefleri kararlaştırılır; eski performans kabul tavanı değildir.
- **NFR-4 — Veri bütünlüğü:** Migration ve tekrar deneme testleri korunacak alanlar ve kimlik ilişkilerinde kayıp/çoğalma göstermemelidir. Başarı, yalnız taşınan kayıt sayısıyla değil alan/ilişki doğrulamasıyla değerlendirilir.
- **NFR-5 — Erişim ve gizlilik:** Başka hesabın verisi veya yetkisiz revizyon/medya döndürülmez. Kimlik bilgileri, tokenlar ve özel medya URL'leri olay/crash kayıtlarına konmaz. Çocuk verisi, SDK kapsamı ve izinlerle ilgili mevcut davranışlar OQ-9 kapsamında doğrulanır.
- **NFR-6 — Uyumluluk:** Onaylanmış minimum işletim sistemi ve telefon/tablet kapsamı korunur. Genel kalite matrisi onaylı emülatör/Simulator ortamlarında yürütülür; mevcut fiziksel cihazlarda tamamlayıcı oynatma/arka plan kontrolleri yapılır. İmzalı güncelleme ve fiziksel platform kanıtı gerektiren özel senaryolar ayrıca doğrulanır.
- **NFR-7 — Görsel parite:** Kararlaştırılmış referans ekranlar aynı cihaz boyutu, dil ve içerik koşullarında karşılaştırılır. Onaylı değişiklikler hariç görünür sapmalar kapanmadan ilgili ekran kabul edilmez; görüntü sıkıştırma metin okunabilirliğini bozamaz.
- **NFR-8 — Tanılama:** Crash, bellek kaynaklı sonlanma, donma ve oynatma hataları sürüm/platform/cihaz bağlamıyla araştırılabilir olmalıdır. Kullanıcı akış loglarıyla crash ölçümü ayrı veri amaçlarıdır.
- **NFR-9 — Gelecekte offline kullanım için temel:** Revizyon/varlık kimliği sabit, bütünlük ve tamlık doğrulanabilir, indirmeye devam etmeye uygun bir medya temeli tasarlanır. Silinebilir oynatma önbelleği ile gelecekte kullanıcının açıkça saklayacağı offline içerik ayrılır. İlk sürüm kullanıcı kontrollü offline indirme ekranı veya tam hikâyenin offline hazır olduğu garantisi sunmaz; cache bütçesi, boşaltma ve devam semantiği OQ-8'de belirlenecektir.

## İlk sürüm kapsamı ve kapsam dışı işler

İlk sürüm; FR-1–FR-29 kapsamındaki onaylı işlevleri (FR-17 hariç), desteklenen platformları, eski kullanıcı verilerinin geçişini, yeni backend entegrasyonunu ve kabul edilmiş yayın/test yönünü kapsar. RevenueCat ve backend kullanıcı akış kaydı son geliştirme adımlarıdır, release sonrası işler değildir.

İlk sürümün kapsamı dışında: etkileşimli hikâye dalları ve seçimleri; ebeveyn modu ve ebeveyn rehberleri; ürünün UI/UX yeniden tasarımı; yeni keşif/kişiselleştirme özellikleri; çoklu profil; çoklu sağlayıcı hesap konsolidasyonu; eski/yeni kullanıcı verisinin sürekli eşitlenmesi ve kullanıcı kontrollü açık offline indirme yönetimi. Gelecekte offline kullanım mümkün olmalıdır; ilk sürümün medya temeli bunu engellememelidir. Legacy beş ZIP cache sınırı, iki ZIP formatı ve Flutter seçimi korunması gereken ürün davranışı değildir. Eski SDK'ların tümünün birebir taşınması ayrıca bir gereksinim değildir; kullanıcı işlevleri ve kabul edilen olay ihtiyaçları korunur.

## Başarı ölçütleri ve yayın kapıları

2026-10-02 yayın ölçümü kararı: İlk sınırlı dağıtım, kritik testler ve ilgili diğer yayın önkoşulları geçtiğinde başlayabilir. Platform bazında en az %99,5 crash-free kullanıcı hedefi, dağıtım genişletilmeden önce gerçek kullanım verisiyle değerlendirilir. Gözlem süresi ve yeterli kullanıcı sayısı yayın planında netleştirilir; az sayıda başarılı emülatör testi bu hedefin kanıtı sayılmaz.

- **SM-1 — İşlevsel replacement:** Referans envanterindeki her kullanıcı işlevi karşılanmış veya açıkça onaylanmış istisnayla açıklanmış olmalıdır. Açık, kararsız işlev kaybı kalamaz. FR-1–FR-29'un etkin gereksinimlerini doğrular; geri çekilmiş FR-17 uygulanmaz.
- **SM-2 — Güvenli güncelleme:** Kayıtlı/anonim/abone kullanıcı ve kesintili migration testlerinde korunacak veri kaybı, kopya kayıt ve yanlış hesap eşleşmesi sıfır olmalıdır. FR-26–FR-29'u doğrular.
- **SM-3 — Üretim kararlılığı:** Platform bazında en az %99,5 crash-free kullanıcı oranı sağlanır. Değerlendirme süresi/örneklemi yeterli olmalı; donma ve bellek sonlanması ayrıca incelenmelidir. Test cihazlarındaki bellek kaynaklı kapanma ve kritik oynatma hatası yayın engelidir. NFR-1, NFR-2 ve NFR-8'i doğrular.
- **SM-4 — Medya deneyimi:** Onaylanacak cihaz/ağ matrisi ve gecikme bütçelerinde okuma/dinleme kabul testleri geçer. FR-11–FR-16 ve NFR-2, NFR-3'ü doğrular.
- **SM-C1 — Kaliteyi bozarak optimizasyon yok:** Bellek düşürmek için içerik/işlev atılmaz, görsel okunabilirlik ve ses bütünlüğü bozulmaz.
- **SM-C2 — Ölçümü gizleyerek kararlılık yok:** Hatalı cihazları rapordan çıkarmak, minimum sürümü sessizce yükseltmek veya tanılamayı kapatmak başarı sayılmaz.
- **SM-C3 — Hız için veri kaybı yok:** Migration süresi kısalsın diye geçmiş/tercih atılmaz; eksik taşıma başarıya çevrilmez.

Yayın kanıtı: referans parite kontrolü, onaylı emülatör/Simulator matrisi ve erişilebilen fiziksel cihazlardan ortamı belirtilmiş kararlılık/performance raporu, güncelleme ve migration sonuçları, RevenueCat satın alma/restore/erişim doğrulaması, backend olay kaydı doğrulaması ve eski/yeni sistem birlikte çalışma testi. Yeni sürüm kademeli yayınlanır; kullanıcı 2026-10-01 kararıyla mobil sürümün her dağıtım aşamasını kendisi onaylayıp mağaza panellerinden yönetecektir. Yeterli kullanımda %99,5 crash-free kullanıcı hedefi, migration ve RevenueCat kanıtları sağlandıkça dağıtımın genişletilmesi değerlendirilir; kesin kohort, gözlem ve durdurma eşikleri OQ-10’da belirlenecektir. Eski sistem için sabit kapanış tarihi yoktur. Kademeli yayın tamamlanıp migration ve abonelik akışlarının kararlılığı doğrulandıktan sonra kullanıcının ayrı kapanış onayı alınır; bu onaya kadar eski sistem çalışır. Kullanıcı bu rollout yaklaşımını onaylamıştır. Mağaza yayınını geri alma veya yeni sistem hatasında toparlanma planı OQ-10 kapsamında tanımlanacaktır.

## Açık noktalar — son görüşmede toplanacak

Bu maddeler taslakta bilinçli olarak açık tutulmuştur. Hiçbiri örtük onay veya sessiz kapsam daraltması değildir. Mimari alanlar için çözüm burada seçilmeyecektir.

| ID | Karar / doğrulama | Sorumlu rol ve kapanış zamanı |
|---|---|---|
| OQ-1 | Referans oynatıcı kontrollerinin tam paritesi ve yeni backend'in gerekli medya/oynatma desteği | Ürün sahibi + backend/mobil; epikler öncesi kapsam ve uygunluk kontrolü |
| OQ-2 | Crash metriğinin kaynağı ve kök nedenleri; %99,5 crash-free kullanıcı hedefi için değerlendirme süresi/örneklemi; ilk çalışan okuma/dinleme ölçümlerinden türetilecek bellek/gecikme sınırları ve kesin sanal cihaz/ağ profilleri. Ana test ortamı emülatör/Simulator, mevcut fiziksel cihazlar tamamlayıcıdır | Mobil/QA + ürün sahibi; mimari ölçüm planı, yayın öncesi eşikler ve kanıt |
| OQ-3 | İkinci cihazın mevcut yeni hesap verisiyle devam etmesi ve eski yerel kayıtların ek aktarımının yapılmaması kararlaştırıldı. Açık kalanlar: ilk taşımanın anonim kimlik/alan eşleştirmesi, hatalı/eksik kayıtları. Kaldırılmış içerik kayıtları geçmişte görünür ve istatistiklerde korunur; mevcut olmayan içerik açılmaz | Mobil/backend + gerektiğinde ürün sahibi; migration tasarımı ve testleri öncesi |
| OQ-4 | Kullanıcı ekran görüntüleri eski aramada okuma hikâyesi, sesli hikâye, meditasyon ve ninni gruplarını doğruladı; istemcide arama üç karakter ve 500 ms beklemeyle başlar. Gerçek request/response örnekleriyle alan eşleşmesi, küçük liste DTO'ları, ana ekran bileşimi, sayfalama, sıralama ve hata sözleşmesi doğrulanacak. CMS kategori sırası, dil görünürlüğü ve fallback kararlaştırıldı | Backend/mobil + ürün sahibi; UX/API sözleşmesi öncesi |
| OQ-6 | Yalnız yeni sistem hesabını silme kararının ortak Firebase kimliğiyle uygulanması; silinen verinin tekrar migration ile geri gelmemesi ve abonelik etkisinin açıklanması. Ürün kararı korunur; teknik uygulama henüz kanıtlanmadı | Backend/mobil + ürün sahibi; hesap silme uygulanmadan önce |
| OQ-7 | Analiz olayları için şimdilik otomatik silme/süre sınırı yoktur; mevcut backend veritabanı kullanılır, ölçülen performans etkisi olursa ayrı analiz veritabanı değerlendirilir. Açık kalanlar backend olay alanları ve hata/tekrar politikasıdır. Olay kapsamı, kayıt olmadan satın alma ve varsayılan restore transfer kuralı kararlaştırıldı; RevenueCat ürün/teklif kimlikleri, canlı/sandbox ayarı, kimlik/alias eşleştirmesi ve backend uzlaştırması gerçek entegrasyon sözleşmesinde doğrulanacak | Ürün sahibi + backend/mobil; son entegrasyonlardan önce sözleşme |
| OQ-8 | Korumalı URL ömrü/yenileme, yayından çekme, metadata-only onay sınırı; manifest şeması ve alınma anı; gerçek dönüşüm, ileri hazırlık, retry/idempotency; retention ve cache bütçesi/boşaltma/indirmeye devam parametreleri. Discovery yönü korunur; kesin çözüm gerçek backend/media kanıtıyla mimari aşamasında belirlenecek | Backend/mobil mimarisi; ilgili epikler öncesi |
| OQ-9 | Mevcut veri/SDK kapsamı ve mağaza hedef kitle beyanı. İzin ve görsel duyuru akışı eski sürüm referansını korur; bildirim/deep-link hedef yönlendirmesi onaylı yeni sürüm özelliğidir. Portekizce duyuruda İngilizce görsel fallback'i kullanılmaz | Ürün sahibi + mobil; yayın akışları kesinleşmeden önce |
| OQ-10 | CMS, yeni dillerin tester’dan herkese açılmasını yönetir. Mobil uygulama sürümünün aşamalarını kullanıcı bizzat onaylayıp mağaza panellerinden yönetir. Eski sistemin kapanış koşulu kararlaştırıldı: kademeli yayın tamamlanır, migration ve abonelik akışlarının kararlılığı doğrulanır, ardından kullanıcının ayrı kapanış onayı alınır; sabit tarih yoktur. Mağaza dağıtım kohortları, genişletme/durdurma eşikleri, kararlılık kanıtının ölçütleri ve hata toparlanması açık teknik/operasyon kararlarıdır | Ürün sahibi + operasyon/backend; mobil rollout öncesi |

## Varsayımlar ve kanıt sınırları

Epik ve uygulama sözleşmelerinden önce referans ekranlar, ekran–endpoint eşleştirmeleri, gizli değerlerden arındırılmış request/response/hata örnekleri, ücretsiz/kilitli/test/yayınlanmamış/fallback vakaları ve telefon/tablet medya gözlemleri tamamlanmalıdır. Bu girdiler mobil API sözleşmesi, yetki matrisi, manifest şeması ve migration/birlikte çalışma planını besler. Backend'deki önceki adapter/işleme gözlemleri güncel kod ve gerçek çıktılarla yeniden doğrulanacaktır; PRD güncellemesi bu doğrulamaların yapıldığı anlamına gelmez.

Ürün sahibinin kabul ettiği replacement kapsamı, legacy belgelerdeki kullanıcı işlevlerini kapsar; her ayrıntının gerçek cihazda çalıştığı henüz doğrulanmamıştır. UJ taslakları kaynaklardan çıkarılmıştır ve son görüşmede gözden geçirilecektir. Yeni backend'in bu gereksinimleri hazır karşıladığı varsayılmamaktadır. Yaklaşık %85 crash-free ve yüksek çözünürlük hipotezi doğrulanmış bulgu değildir. Açık noktalar kapanmadan belge `final` durumuna alınmayacaktır.
