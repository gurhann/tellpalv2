---
title: Mobil Epic Doğrulama Raporu — Yorum ve Eylem Planı
date: 2026-09-29
status: önerilen-eylem-planı
sourceReport: epics-mobile-final-validation.md
scope: Raporun yorumlanması ve sonraki işlerin sıralanması
---

# Mobil epic doğrulama raporunu nasıl yorumlamalısın?

Planın kapsamı ve ana yapısı yerinde. Yapılacak işler yedi epic ve 72 story içinde tanımlanmış; 28 etkin işlevsel gereksinim, dokuz kalite gereksinimi ve 25 UX gereksinimi story’lere bağlanmış. Mimari kurallar da kabul edilen mobil mimariyle uyumlu. Mevcut epic sırasını veya bütün story’leri yeniden yazmanı gerektiren bir sonuç bulunmadı.

Ancak bir gereksinimin story’de yer alması, uygulamaya başlayacak kişinin bütün ayrıntıları bildiği veya özelliğin çalıştığının kanıtlandığı anlamına gelmiyor. Rapordaki temel ayrım bu: **kapsam tanımlı; bazı kabul kontrolleri ve geliştirme önkoşulları henüz tamamlanmamış.**

Kaynaklar: [son doğrulama raporu](./epics-mobile-final-validation.md) ve [mobil epic planı](./epics-mobile.md). Bu doküman raporu uygulanabilir iş sırasına çevirir. Ürün kararlarını değiştirmez, açık sorulara çözüm seçmez ve herhangi bir story’yi hazır olarak işaretlemez.

## Rapordaki durumlar ne anlama geliyor?

| İfade | Pratik anlamı |
| --- | --- |
| Gereksinim izlenebilir | Hangi story’nin gereksinimi üstlendiği belli. |
| Kapsam onaylı | Story’nin planlanan ürün kapsamına dahil olması kabul edilmiş. |
| Önkoşullar bekleniyor | Geliştirmeden önce gerekli sözleşme, karar, erişim veya kanıt eksik. |
| Kabul kanıtı yok | Story’nin kabul kriterlerini geçtiğini gösteren uygulama/test sonucu henüz üretilmemiş. Planlama aşamasında bu beklenen bir durum. |
| `remediation-required` | Doğrulama yapıldı; rapordaki eksikler giderilip yeniden kontrol edilmeli. |
| P2 | Bulguların öncelik etiketi. Etkilenen işin kabulünü değiştiren somut eksikler olarak ele alınmalı; story önkoşullarının yerine geçmez. |

**Bütün story’lerin kabul testlerini bugün çalıştırmak zorunda değilsin.** Geliştirmeye başlamak için seçilen story’nin önkoşullarını kapatırsın; kabul kanıtını o story uygulanırken üretirsin. Buna karşılık, sözleşmesi ve kritik davranışı belirsiz bir story’yi “hazır” olarak kabul etmemelisin. Mevcut rapor, bütün plan için koşulsuz geliştirme hazırlığı onayı vermiyor.

## Önce hangi işleri yapmalısın?

Önerilen sıra: belge tutarlılığını düzelt; kabul kriterlerindeki boşlukları kapat veya ilgili kararın sahibini ve kapanış zamanını kaydet; ardından yalnız ilk çalışma paketinin önkoşullarını doğrula. Diğer epic’lerin sözleşmelerini ihtiyaç sırasına göre tamamla.

| Sıra | İş | Beklenen çıktı | İlgili bulgu |
| --- | --- | --- | --- |
| 1 | PRD ve epic’lerde güncel kararları aynı hale getir | Çelişkisiz ürün/mimari referansları | V2 |
| 2 | iOS ebeveyn kapısının giriş noktalarını ve sahibini belirle | Giriş noktası matrisi ve etkilenen story kriterleri | V3 |
| 3 | CMS ücretsiz seçimi için iki deneyimi birlikte doğrulayan kriter ekle | Mevcut story’ye bağlı somut kabul senaryosu | V4 |
| 4 | Dil yapılandırması hata davranışını kararlaştır | Hata/fallback sözleşmesi ve story kriterleri | V5 |
| 5 | Story 1.1’in teknik ve sözleşme önkoşullarını doğrula | Story’ye bağlı hazırlık kaydı | V1 |
| 6 | 1.1’i uygula ve kabul et; ardından 1.2’ye geç | Library listesi, sonra doğru detay açılışı | V1 |
| 7 | Sonraki story’lerin önkoşullarını ihtiyaç sırasıyla kapat | Her story için ayrı hazırlık ve kabul kanıtı | V1 |

İlk dört işin analizi sırasında 1.1 için araç/sözleşme incelemesi yürütülebilir. Sonraki epic’lere ait açık bir konunun her durumda 1.1’i engellediğini varsayma; hangi story’yi etkilediğini açıkça kaydet.

## V2: Belgelerde eski kararları temizle

**Yapılacak iş:** PRD ve gerekli ürün devri bölümlerini, zaten kabul edilmiş kararlarla uzlaştır.

- Güncel kullanılabilir diller Türkçe, İngilizce ve Portekizce. Almanca çeviri dosyalarının bulunması, Almancanın kendiliğinden kullanıcıya açılması anlamına gelmiyor.
- Yeni diller önce test kitlesine, sonra canlı kullanıma açılabiliyor. Dil görünürlüğü, aday içerik veya korumalı medya yetkisini kendiliğinden vermiyor.
- Flutter seçilmiş durumda; mimari referans ADR-0018 ve mobil architecture spine. PRD’de teknoloji seçimini hâlâ açık bırakan ifade bu referansla uzlaştırılmalı.
- iOS ebeveyn kapısı ürün kapsamına dahil. Bu, kapsam dışı bırakılan ebeveyn modu/rehberleriyle farklı bir davranış. Kapının tam tetikleme ve sıfırlama kuralları henüz doğrulanmış değil.

**Senin rolün:** Yeni karar vermek yerine, güncellenen metnin mevcut kararlarını doğru yansıttığını kontrol etmek. Gerçek bir ürün belirsizliği çıkarsa o noktayı karara bağlamak.

**Teknik işin sahibi:** Dokümanları güncelleyen kişi/ajan. Geçmiş kanıt belgelerini tarihsel kayıt olarak korumalı; eski ekran görüntülerini bugünkü ürün kararı gibi yeniden etiketlememeli.

**Tamamlanma ölçütü:** PRD’den ve epic dosyasından çalışan iki kişi aynı güncel dil, teknoloji ve ebeveyn kapısı kapsamını anlayabiliyor. Story kimlikleri ve mevcut kapsam korunuyor.

**2026-09-29 belge durumu:** PRD’de güncel TR/EN/PT kararı, Flutter seçimi ve iOS ebeveyn kapısının kapsamda olduğu işlendi; addendum teknoloji kararıyla uzlaştırıldı. Kapının giriş noktaları ve gerçek cihaz davranışı hâlâ doğrulama bekliyor.

## V3: iOS ebeveyn kapısı hangi işlemlerde gerekli?

Epic 6 kapıyı tanımlıyor; fakat Epic 4’teki duyuru, dış bağlantı veya mağaza değerlendirme girişlerinden bazıları da bu kapıyı gerektirebilir. Hangi girişlerin gerçekten etkilendiği bilinmeden Epic 4’ün bütün bu işlemlerini tamamlanmış saymak doğru olmaz.

**2026-09-29 görüşme kararı:** Giriş noktalarını varsaymak yerine eski iOS uygulamasında doğrulayacağız. 1.0.35+333 kaynak kodunda ortak `showPaywall` yolu debug dışı iOS’ta ebeveyn kapısını açıyor. Promosyon teklif yolu doğrudan RevenueCat paywall’unu çağırıyor; incelenen mağaza listesi ve değerlendirme çağrıları da ortak kapıdan geçmiyor. Bu fark, ürünün korunacak davranışı veya cihazda görülen sonuç olarak henüz kabul edilmedi. Temsilî iOS build’inde gerçek girişleri, başarısız/iptal/başarılı sonucu ve yeniden açılış durumunu ayrıca gözlemlemek gerekiyor.

**Yapılacak iş:** Referans iOS uygulamasında giriş noktalarını inceleyip aşağıdaki matrisi doldur. Her satır için “var/yok/doğrulanamadı” ve kanıt bağlantısı kaydet.

| İncelenecek giriş | Kaydedilecek bilgi |
| --- | --- |
| Kilitli içerik ve premium teklif açılışı | Kapı hangi eylemden önce gösteriliyor? |
| Satın alma, restore ve üyelik yönetimi | Her biri kapıya tabi mi; hangi koşulda? |
| Duyuru ve dış bağlantı | Hedef türüne göre davranış değişiyor mu? |
| Uygulama değerlendirme/mağaza bağlantısı | Kapı gösteriliyor mu? |
| İptal, yanlış cevap ve başarılı cevap | Hedef açılıyor mu; kullanıcı nereye dönüyor? |
| Tekrarlı dokunma, arka plana geçiş ve yeniden açılış | Önceki doğrulama ne kadar geçerli; ne zaman sıfırlanıyor? |

**Senin rolün:** Referans sürüm/cihaz erişimini sağlamak ve gözlenen davranışa göre ürün kapsamını teyit etmek. Güncel mağaza politikası hakkında bu referans davranışından sonuç çıkarılmamalı.

**Teknik işin sahibi:** Mobil geliştirici/QA. Matrise göre kapıyı ilk ihtiyaç duyan mevcut story’ye bağlamalı. Epic 4’te gerekiyorsa ortak kapı davranışı orada başlayabilir; Story 6.2 premium kullanımını genişletir. Alternatif olarak ilgili Epic 4 eyleminin kabulü, açıkça belirtilen sonraki entegrasyona bırakılabilir.

**Tamamlanma ölçütü:** Etkilenen girişlerin sahibi ve kabul zamanı belli; hiçbir story henüz olmayan bir kapıya gizlice bağımlı değil. İptal/başarısızlık gerekli hedefi açmıyor ve alternatif girişler kapıyı atlayamıyor.

## V4: Ücretsiz STORY kuralını iki deneyimde birlikte doğrula

Mevcut kriterler kartların kilit durumunu ve başlatma yetkisini kontrol ediyor. Eksik olan, aynı hikâyenin aynı dildeki CMS ücretsiz seçiminin **hem okuma hem sesli hikâye deneyiminde** doğru uygulanmasını birlikte sınayan açık senaryo.

**Yapılacak iş:** Bu kontrolü yeni story açmadan uygun mevcut story’ye bağla. Rapordaki öneri Story 2.7; abone senaryoları için Epic 6’da tekrar doğrulama.

Önerilen senaryoda iki deneyimi de hazır/yayınlı olan aynı STORY kullanılır. Bir dilde CMS ücretsiz seçimi değiştirilir; eşdeğer haklara sahip anonim ve kayıtlı abone olmayan kullanıcılar için okuma ve sesli deneyimin kilit/başlatma sonuçları kontrol edilir. Diğer dilin kuralı etkilenmemelidir. Ücretsiz seçim premium abonelik oluşturmamalı ve yayınlanmamış medyayı erişilebilir hale getirmemelidir.

**Senin rolün:** Senaryonun mevcut FR-10 kararını doğru anlattığını kontrol etmek.

**Teknik işin sahibi:** Backend/mobil/QA. Gerçek CMS/backend verisiyle beklenen sonuçları ve test örneklerini hazırlamak.

**Tamamlanma ölçütü:** Plan düzeltmesi için senaryo açıkça story’de yer alıyor ve doğrulayacak sözleşme/veri belli. Özelliğin kabulü için iki deneyimin gerçek backend sonuçları kanıtlanıyor. Bu iki aşama ayrı kaydedilmeli.

**2026-09-29 belge durumu:** İki deneyim ve ayrı dil sonuçlarını birlikte kontrol eden kriter Story 2.7’ye eklendi. Gerçek backend fixture’ı ve kabul sonucu henüz yok.

## V5: Dil yapılandırması alınamazsa ne olacak?

Doğru yapılandırmanın test/canlı dil görünürlüğü tanımlı. Yapılandırma alınamadığında veya bozuk olduğunda kullanılacak davranış açık değil. Bunu geliştiricinin varsayımına bırakmamalısın.

**Yapılacak iş:** Aşağıdaki durumlar için sözleşme oluştur; Story 1.10’a kriterleri, Story 5.4’e test/canlı kitle doğrulamasını bağla.

- İlk açılışta yapılandırma alınamaması.
- Daha önce alınmış yapılandırma varken ağ hatası oluşması.
- Bozuk dil listesi veya geçersiz test kitlesi bilgisi dönmesi.
- Yeniden deneme ve yapılandırmanın sonradan düzelmesi.
- Kayıtlı tercihin artık kullanıcıya açık olmayan bir dil olması.

**Senin rolün:** Kullanıcının göreceği seçim, bekleme, hata veya yeniden deneme davranışında ürün tercihi gereken noktaları netleştirmek.

**Teknik işin sahibi:** Backend/mobil. Daha önce alınan yapılandırmanın kullanımı, geçerlilik süresi, yetki kontrolü ve güvenli geri dönüş mekanizmasını teklif edip doğrulamak. Bu belge bir varsayılan dil veya saklama süresi seçmiyor.

**Tamamlanma ölçütü:** Hata senaryolarında beklenen kullanıcı sonucu açık. Test dilleri ve aday içerikler normal kullanıcılara sızmıyor; geçerli TR/EN/PT kararı korunuyor; cache veya hata yönetimi yetki kontrolünü atlamıyor.

## V1: Geliştirmeye hangi önkoşullarla başlayabilirsin?

V1 tek bir belge düzeltmesiyle kapanacak bir bulgu değil. Story’ler uygulanmadan önce yapılacak hazırlığın ve uygulandıktan sonra üretilecek kanıtın takip edilmesi gerekiyor. On dış bağımlılığın tamamını ilk gün kapatmak zorunda değilsin; ilk tüketen story için gerekli olan kısmı doğrulamalısın.

### İlk çalışma paketi: 1.1, sonra 1.2

**Story 1.1’den önce:**

- Flutter/Dart ve ilgili BLoC, auto_route, DI, Freezed/JSON kod üreticilerinin uyumlu sürümlerini belirle ve sabitle. Kod üretimi/analyze sonucu ile Android API 24 ve iOS/iPadOS 15 desteğinin uyumluluk kanıtını kaydet.
- Yetkili geliştirme/test erişimini mevcut güvenli imkanlarla belirle. Üretim yetkisini atlayan bir mekanizma veya uygulamaya gömülü test kimlik bilgisi oluşturma.
- EXT-CATALOG’un bu story’ye gereken bölümünü doğrula: CMS sırası, önizleme sınırı, içerik kimliği, dil/fallback, ücretsiz/kilitli görünürlük, boş sonuç ve hata/yeniden deneme.
- Gerçek backend’den gizli değerleri temizlenmiş örnek yanıtları ve beklenen veri kümesini kaydet. Sabit test verisi tek başına gerçek entegrasyon kabulü değildir.
- Library’nin referans ekran/durumlarını ve uygulanacak telefon/tablet, dil, metin ölçeği ve erişilebilirlik kontrollerini belirle.
- İşin sorumlusunu, kanıt yollarını ve kapanış sonucunu ilgili bağımlılık/story kaydına yaz.

Bu hazırlık tamamlanınca **1.1 yalnız Library listeleme kapsamıyla** uygulanabilir. Detay ve okuyucu aynı çalışmaya eklenmemeli.

**Story 1.2’den önce:** 1.1 kabul edilmiş olmalı. Detay sözleşmesi, doğru içerik/deneyim/dil kimliği, yükleme/hata/kullanılamayan içerik sonuçları, var/yok öneri örnekleri ve geri dönüş davranışı doğrulanmalı. Ardından seçilen kartın doğru hikâye detayına gitmesi uygulanır. Okuyucu Story 1.3’te başlar.

**AD-5, bu kontrollü keşif paketinin önkoşulu değil.** İmzalı güncelleme kanıtı toplama daha erken yürütülebilir; ancak Epic 3’teki auth/migration özelliklerinin geliştirilmesinden önce Android ve iOS kapısı geçilmelidir. Mevcut iOS Simulator raporu bu kapının tamamını karşılamıyor.

### Sonraki işlerde kapanacak başlıca konular

| Başlama noktası | Önce doğrulanması gerekenler |
| --- | --- |
| Okuyucu ve medya tüketimi | Gerçek manifest/medya çıktıları, başlatma yetkisi, revizyon/bütünlük, sınırlı hazırlık; story’ye göre yenileme/retention/indirme devam sözleşmeleri |
| Geçmiş ve süre kaydı | Kalıcı bekleyen kayıt, sabit olay kimliği, sunucu tekrar önleme, süre/tamamlanma kuralları ve işlem kapanınca toparlanma |
| Epic 3 auth/migration | Her iki platformda AD-5; ardından story’ye göre hesap eşleştirme, alan/kimlik taşıma, ikinci cihaz, kalıcı tamamlanma ve silme sözleşmeleri |
| Destek/bildirim | Geri bildirim sınırları, belge dilleri, izin/zamanlama, hedefler ve gerekli ebeveyn kapısı |
| Aday içerik testi | Tester/abone ayrımı, aday/canlı izolasyonu, hazırlık/onay/yenileme ve korunacak revizyonlar |
| Satın alma/restore | Gerçek ürün/teklifler, hesap ilişkisi, bekleyen/iptal/hata sonuçları, backend erişim doğrulaması |
| Yayın ve rollout | Gerçek cihaz matrisi, bellek/gecikme bütçeleri, crash-free örneklem/pencere, ilk dağıtım ve genişleme kapıları, prova edilmiş toparlanma planı |

## Her iş için hangi kaydı tutmalısın?

Her bulgu, dış bağımlılık veya story hazırlığı için aşağıdaki bilgileri kaydet. “Backend tarafı hazır” gibi genel bir ifade yerine dosya ve test sonucunu göster.

| Alan | Yazılacak bilgi |
| --- | --- |
| Kimlik | V2 gibi bulgu, EXT-CATALOG gibi bağımlılık ve ilgili story numarası |
| Sorumlu | Gerçek kişi/atanmış işi yürüten ajan; yalnız genel rol adı yeterli değil |
| İş bağlantısı | Eksik işi teslim edecek mevcut veya açıkça atanmış çalışma |
| Sözleşme/karar | Güncel belge, sürüm ve çözülen davranış |
| Örnek/kanıt | Gizli değerleri temizlenmiş veri, test sonucu veya ekran/cihaz kanıtının yolu |
| Durum | Açık, hazırlığı doğrulandı, uygulandı, kabul edildi gibi ayrı aşamalar |
| Kapanış | Ne doğrulandı; kim, hangi tarihte değerlendirdi; ne hâlâ açık? |

Uygulama sonrası kabul kanıtını hazırlanmış sözleşmeyle karıştırma. Örneğin “detay API sözleşmesi yazıldı”, Story 1.2’nin çalıştığı anlamına gelmez; yalnız bir önkoşulun kapanmış olabileceğini gösterir.

## Şimdi verebileceğin somut iş talebi

İlk görev için şu talep kullanılabilir:

> Son doğrulama raporundaki V2–V5 için gerekli belge ve kabul kriteri düzeltmelerini hazırla. Mevcut kararları koru; kanıt gerektiren davranışları varsayma. Açık ürün/teknik kararlarını sahibine ve etkilenen story’ye bağla. Ardından Story 1.1’in araç, yetkili test erişimi, EXT-CATALOG ve referans ekran önkoşullarını inceleyip hangilerinin doğrulandığını kanıtlarıyla kaydet. Uygulama koduna geçmeden önce kalan engelleri bildir.

Sonraki görev, 1.1’in önkoşulları doğrulanınca yalnız 1.1’i uygulamak olmalı. 1.1 kabul edildikten sonra 1.2 için aynı süreç izlenir.

V2–V5 düzeltmeleri tamamlandığında son doğrulama yeniden çalıştırılır. V1 ise ilgili story’lerin gerçek hazırlık kayıtlarıyla yeniden değerlendirilir. Sadece metin düzenlemek, açık sözleşmeleri veya AD-5 kanıtını tamamlanmış saydırmaz; workflow’un genel tamamlanma durumu ile belirli bir story’nin başlayabilirliği ayrı raporlanmalıdır.
