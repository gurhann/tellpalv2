# TellPal — video tabanlı CMS eksik analizi ve yol haritası

Tarih: 5 Eylül 2026  
Hedef: Kullanıcının teyidiyle, önce videolardaki deneyimi yeni CMS/backend ile eksiksiz karşılamak. Yeni ürün özellikleri bu planın dışında.

## Sonuç

Mevcut CMS içerik üretiminin önemli bölümünü karşılıyor: dört içerik türü, yerelleştirmeler, hikâye sayfaları, sayfa sesleri/görselleri, katkıda bulunanlar, kategori kürasyonu, ücretsiz içerikler ve hikâye önizleyicisi var. Yeniden bir temel içerik editörü yapmak gerekmiyor.

Öncelikli boşluklar: mobil arama sözleşmesi, Kitaplık/Uyku ekranlarının editoryal yerleşimi ve kategori sırası, katkıda bulunanların mobil yanıta aktarılması, ninni enstrüman bilgisi ve benzer içeriklerin seçimi/sunumu. Türkçe arayüzde İngilizce içerik sunumunun dil kuralı ayrıca netleştirilmeli.

Bu belge uygulama değişikliği veya onaylanmış yeni mimari kararı değildir. Kayıttaki deneyimi karşılamak için önerilen iş kapsamı ve kabul ölçütleridir.

## İnceleme yöntemi ve sınırlar

- Kaynaklar: [1_hikaye_icerigi.mp4](C:/Users/gurha/Downloads/1_hikaye_icerigi.mp4) — 50,50 saniye; [2_kategoriler_ve_hikaye_arama.mp4](C:/Users/gurha/Downloads/2_kategoriler_ve_hikaye_arama.mp4) — 48,41 saniye; [3_uyku_menusu.mp4](C:/Users/gurha/Downloads/3_uyku_menusu.mp4) — 92,02 saniye. Toplam yaklaşık 3 dakika 11 saniye.
- Üç kaydın tamamına yayılan yaklaşık üç saniyelik aralıklarla 64 kare çıkarıldı; akış tabloları ve seçili kareler görsel olarak incelendi. Aşağıdaki zamanlar yaklaşık konumlar/aralıklardır; kare kare kesintisiz oynatma incelemesi yapılmadı.
- Ses kanallarının konuşma dökümü çıkarılmadı; işitilen içerik veya ses kalitesi hakkında hüküm verilmedi. Oynatma davranışı için ekran kontrolleri ve zaman göstergeleri kanıttır.
- CMS ve backend karşılaştırması çalışma kopyasındaki kaynak koda dayanır. Canlı CMS/mobil uygulama çalıştırılmadı; prod sürümünün bu kodla aynı olduğu doğrulanmadı.
- “Var” kaynak kodda destek bulunduğunu, “Eksik” incelenen kodda gereken sözleşme/alanın bulunmadığını, “Kısmi” desteğin akışın bir bölümünü karşıladığını belirtir. Çalışma zamanı kabul testlerinin geçtiği anlamına gelmez.
- Videolardaki metinler ürünün gözlenen içeriğidir; kullanıcı talimatı olarak değerlendirilmedi.

## 1. Gözlenen kullanıcı akışları

### Video 1 — hikâye içeriği

| Yaklaşık zaman | Gözlem | CMS/backend karşılığı |
|---|---|---|
| 00:04–00:06 | Kitaplık; Premium'a Geç alanı, arama girişi, Ebeveynlerin Favorileri ve İngilizce Hikâyeler şeritleri; başlık, alt açıklama, Hepsi bağlantısı | Kategori metadatası, seçili içerikler, sıralama, yüzey yerleşimi ve içerik dili |
| 00:07–00:11 | Haberci Nota detayında kapak, 4 Dakika, 3+ Yaş, Hikâyeye Başla, Seslendirme anahtarı, açıklama, Yazan/Seslendiren/Resimleyen | İçerik yerelleştirmesi, yaş bilgisi, katkıda bulunanlar, kullanılabilir ses |
| 00:13–00:44 | Tam ekran resimli sayfalar, yatay sayfa geçişleri; 00:37 civarı Çıkış/Ses kontrolleri, noktalı sayfa göstergesi ve Sayfa: 6/14 | Sıralı, dile bağlı sayfa görselleri ve sesleri; etkileşim mobil istemcide |
| 00:46–00:48 | Hikâyeyi Bitirdin, dönüş düğmesi ve Benzer Hikâyeler şeridi | Bitiş akışı ve ilgili içerik listesi |

Sayfa yazıları resimle bütünleşmiş görünüyor. Görüntü tek başına bunların dosyaya gömülü metin mi yoksa uygulama katmanı mı olduğunu kanıtlamaz. Mevcut locale bazlı illüstrasyon modeli bu kullanım için uygundur; görüntüden yola çıkarak ayrı metin alanını kaldırmak doğru olmaz.

### Video 2 — kategori ve arama

| Yaklaşık zaman | Gözlem | CMS/backend karşılığı |
|---|---|---|
| 00:04–00:11 | Ebeveynlerin Favorileri → açıklamalı, iki sütunlu hikâye listesi | Kategori detay bilgisi ve sıralı kürasyon |
| 00:13–00:29 | Boş aramada görselli kategori kutuları; farklı içerik türlerinin kategorileri aynı ekranda; Aile ve Arkadaşlık detayına geçiş | Kategori keşfi, kategori sırası ve yerelleştirilmiş kategori görselleri |
| 00:31–00:41 | “hafta” sorgusu; Hikâyeler (4) ve Sesli Hikâyeler (2) başlıkları | Kullanıcı araması, tür bazlı gruplama ve toplamlar |
| 00:43–00:48 | Sorgunun temizlenmesiyle kategorilere, ardından Kitaplık'a dönüş | Boş sorgu durumu ve mobil gezinme |

“hafta” sonucunda başlığında bu kelime görünmeyen kartlar da var. Bu, yalnızca başlık aramasının eşdeğerlik için yetersiz kalabileceğini gösterir; mevcut uygulamanın açıklama, hikâye metni, anahtar kelime veya başka bir kaynakta aradığı videodan belirlenemez.

### Video 3 — uyku

| Yaklaşık zaman | Gözlem | CMS/backend karşılığı |
|---|---|---|
| 00:07–00:11 | Dinlendirici Ninniler & Meditasyonlar üst alanı; Ninni ve Dinlendirici Müzikler, Meditasyon, Sesli Kitaplar şeritleri | Uyku yüzeyi, banner ve sıralı kategori bölümleri |
| 00:13–00:29 | Dandini Dastana yükleme → doğrudan oynatıcı; kapak, Müzik: Ali Kaan Uysal, Enstrüman: Piyano - Glockenspiel, toplam 10:19, duraklatma ve süre çubuğunda ileri konuma geçiş | LULLABY, müzisyen kredisi, enstrüman alanı, ses teslimi; oynatıcı mobilde |
| 00:31–00:44 | Bilinçli Uyku Meditasyonu detayı → Meditasyona Başla → oynatıcı; açıklama, 6 Dakika, 3+ Yaş, yazar ve seslendiren; oynatıcı toplamı 6:35 | MEDITATION metadata/credits/ses; kart süresi ile gerçek ses süresi farklı sunumlar |
| 00:49–00:53 | Zıt-Giller Kasabası sesli hikâye detayı; 8 Dakika, 4+ Yaş, açıklama, yazar/seslendiren, Sesli Hikâyeye Başla | AUDIO_STORY detay desteği. Bu örnekte oynatıcının açılması kayıtta doğrulanmıyor |
| 01:01–01:16 | Aramadan Meditasyon ve Sesli Kitaplar kategorilerine geçiş | Ortak kategori keşfi ve tür uyumlu kürasyon |
| 01:19–01:31 | “zıt” sorgusu; Hikâyeler (2), Sesli Hikâyeler (1), altta Aradığına Benzer Sonuçlar | Tür bazlı arama ve ek benzer sonuçlar |

Yükleme ekranları ve görsel placeholder'ları gözleniyor. Ağ, önbellek ve sunucu koşulları bilinmediğinden bunlar bu aşamada performans hatası olarak sınıflandırılmadı.

## 2. Mevcut destek ve açıklar

| ID | Gereksinim | Durum | Somut fark / yapılacak iş |
|---|---|---|---|
| G01 | STORY / AUDIO_STORY / MEDITATION / LULLABY | Var | Dört tür mevcut. Uyku için yeni bir içerik türü gerekmiyor. [S1, S2] |
| G02 | Kapak, başlık, açıklama, yaş, süre, dil | Var | Mevcut alanları gerçek içerikle doğrula. `ageRange` tek sayı; kayıttaki “3+” gösteriminin minimum yaş anlamı kabul verisinde sabitlenmeli. [S2, S3] |
| G03 | Hikâye sayfası, görsel, metin, ses, sıra | Var | Locale bazlı sayfa desteği ve CMS önizleyicisi mevcut. Mobilde sayfa/ses geçişleriyle eşdeğerlik doğrulanmalı. [S2, S4] |
| G04 | Yazar, seslendiren, resimleyen, müzik kredisi | Kısmi | CMS/backend contributor kayıtları ve rolleri var; mobil içerik detay cevabı bunları taşımıyor. Dil, kredi adı ve sıra korunarak public cevaba eklenmeli. [S2, S5] |
| G05 | Ninni enstrüman bilgisi | Eksik | Müzisyen rolü var; “Piyano - Glockenspiel” için içerik/enstrüman alanı yok. Enstrümanı kişi adı veya rol gibi kullanmayan, yerelleştirilebilir bir alan kararlaştırılmalı. [S5] |
| G06 | Kategori adı, açıklaması, kutu görseli | Var | Var olan kategori yerelleştirmeleri kullanılmalı; bu alanlar yeniden yapılmamalı. [S6] |
| G07 | Kategori içindeki içerik sırası | Var | Dil bazlı `displayOrder` mevcut. [S7] |
| G08 | Kategorilerin ekrandaki sırası ve yüzeyi | Eksik | Public kategori listesi slug'a göre sıralı. Kitaplık, Uyku ve arama kategorilerinin editoryal sırası/yerleşimi modellenmemiş. [S7, S8] |
| G09 | Uyku banner'ı ve yüzey bölümleri | Eksik | Kategori metinleri var; ekran banner'ı ve yüzeye göre bölüm yapılandırması yok. Genel bir sayfa oluşturucu yerine videodaki yüzeyleri kapsayan sınırlı düzenleme önerilir. [S6, S8] |
| G10 | Türkçe arayüz içinde İngilizce Hikâyeler | Kısmi / karar gerekli | Yerelleştirme var; mevcut kategori yanıtı kategoriyle içerik için aynı dili kullanıyor. UI dili ve içerik seçiminin dili ayrı ele alınmalı. Videoda İngilizce kartlar var; içlerine girilmediği için gerçek okuma dili doğrulanmadı. [S7, S9] |
| G11 | Kullanıcıya açık arama, tür başlıkları ve sayıları | Eksik | Public içerik listesinde arama parametresi yok; admin araması bunu karşılamıyor. Sorgu kapsamı, Türkçe eşleme, grup toplamları ve sayfalama tanımlanmalı. [S1, S10] |
| G12 | Hikâye sonundaki Benzer Hikâyeler | Eksik | Related içerik ilişkisi/public endpoint bulunmadı. İlk eşdeğerlik için sıralı editoryal seçim yeterli olabilir; kayıt seçimin nasıl yapıldığını göstermiyor. [S2, S3, S10] |
| G13 | Aradığına Benzer Sonuçlar | Eksik / kural belirsiz | Public arama yok; benzer sonuç kaynağı da modellenmemiş. G12 ile aynı algoritma olduğu varsayılmamalı. [S1, S10] |
| G14 | Kilitli ve ücretsiz kartlar | Kısmi | CMS ücretsiz erişim yönetimi ve public `isFree` mevcut. Kayıtta kilitli içeriğe basılarak erişim sınırı denenmedi; abonelik yetkisi/ödeme akışı bu kanıtla tamamlanmış sayılamaz. [S2, S11] |
| G15 | Seslendirme aç/kapa, oynat/duraklat, seek, çıkış | Veri desteği var / mobil doğrulama gerekli | Sayfa sesi ve optimize ses teslimi var. Kontroller mobil davranıştır; bunlar için ayrı CMS ayarları zorunlu değil. [S2, S4] |
| G16 | Aynı eserin hikâye/sesli hikâye sürümleri | Kısmi / ihtiyaç doğrulama | İki türde ayrı kayıtlar mümkün. Aynı eser ilişkisi yok; kayıt yalnızca ayrı arama gruplarını gösteriyor. Zorunlu yeni ilişki olarak ilk faza eklenmemeli. [S3] |

### Kaynak kod kanıtları

- S1 — [ContentMobileController.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/web/mobile/ContentMobileController.java:33): public liste filtreleri `lang`, `type`, `freeKey`; detay ve sayfa endpoint'leri.
- S2 — [ContentMobileResponses.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/web/mobile/ContentMobileResponses.java:13): mobil içerik/sayfa/asset alanları; detayda contributor listesi bulunmuyor.
- S3 — [Content.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/Content.java:42), [ContentType.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/ContentType.java:6), [ContentLocalization.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java:30): içerik kimliği, dört tür ve metadata.
- S4 — [story-content-preview-dialog.tsx](C:/github/tellpalv2/cms/src/features/story-pages/components/story-content-preview-dialog.tsx:89), [story-page-schema.ts](C:/github/tellpalv2/cms/src/features/story-pages/schema/story-page-schema.ts:37), [ContentPublicationPolicy.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java): önizleyici, sayfa girdileri ve yayın kapısı. Taslakta nullable ses alanı, sessiz hikâyenin yayımlanabileceği anlamına gelmiyor; yayın politikası ayrı.
- S5 — [contributor-admin.ts](C:/github/tellpalv2/cms/src/features/contributors/api/contributor-admin.ts:5), [content-contributor-schema.ts](C:/github/tellpalv2/cms/src/features/contributors/schema/content-contributor-schema.ts:13), [ContentContributor.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java:20): roller, dil, kredi adı ve sıra; enstrüman yok.
- S6 — [category-localization-form.tsx](C:/github/tellpalv2/cms/src/features/categories/components/category-localization-form.tsx:96), [PublicCategoryView.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/category/api/PublicCategoryView.java:10): kategori adı/açıklaması/görseli.
- S7 — [PublicCategoryQueryService.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/category/application/query/PublicCategoryQueryService.java:49): kategori listesinde slug sırası, kategori içinde dile bağlı içerik sırası ve aynı dille içerik çözümleme.
- S8 — [router.tsx](C:/github/tellpalv2/cms/src/app/router.tsx), [CategoryMobileController.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/category/web/mobile/CategoryMobileController.java:22): mevcut CMS route/public API envanteri. Ekran yerleşimi/banner yönetimi için karşılık bulunmadı.
- S9 — [V7__create_category_tables.sql](C:/github/tellpalv2/be/src/main/resources/db/migration/V7__create_category_tables.sql:70): kürasyon dilindeki kategori ve içerik yerelleştirmesi gereksinimi.
- S10 — [PublicContentQueryService.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java:60): mevcut public katalog okuması; sorgu/gruplu arama/benzerlik sözleşmesi yok. Liste tüm aktif içeriği çekip uygulama belleğinde filtreliyor; yeni arama bu kalıbı büyütmemeli.
- S11 — [V8__create_content_free_access_table.sql](C:/github/tellpalv2/be/src/main/resources/db/migration/V8__create_content_free_access_table.sql:1), [PublicContentQueryService.java](C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java:166): dil ve erişim anahtarıyla ücretsiz içerik çözümleme.

## 3. Önerilen geliştirme sırası

Süre tahmini verilmedi: mobil kaynak kodu/entegrasyonu ve mevcut içerik verisi görülmeden takvim taahhüdü güvenilir olmaz. Aşağıdaki fazlar bağımlılık sırasıdır.

### Faz 0 — eşdeğerlik veri seti ve açık kurallar

Çıktı: Her içerik türünden en az bir gerçek örnek; Haberci Nota için 14 sayfalık örnek, Dandini Dastana ve Bilinçli Uyku Meditasyonu için ses/kredi örnekleri; kategori listesi ve sırası; `hafta` ve `zıt` için beklenen arama sonuç kimlikleri.

Netleştirilecekler:

1. “hafta” hangi alanlarda aranıyor? Görünen dört hikâye ve iki sesli hikâyenin eşleşme nedeni nedir?
2. Benzer Hikâyeler ile Aradığına Benzer Sonuçlar editoryal mi, kurallı mı? Aynı mantık olmak zorunda değiller.
3. İngilizce Hikâyeler seçimi İngilizce okumaya mı götürüyor? Arama sadece seçili içerik dilinde mi, birden fazla dilde mi?
4. Kitaplık/Uyku/arama kategori sırası ve görünürlüğü bağımsız mı? Videodaki düzen başlangıç veri seti olacak.

Kabul: Gözlenen her akış için beklenen sonuç ve kaynak içerik ID'si listelenir. Belirsiz kararlar sessiz varsayımla veritabanı tasarımına dönüşmez.

### Faz 1 — mevcut veriyi mobil deneyime tamamlamak

- **PAR-01 — Katkıda bulunanları mobil detayda sun:** rol, görüntülenecek kredi adı, seçili dil/genel dil kuralı ve sıra. Sahip: backend + mobil; CMS'deki mevcut atama ekranını kullan.
- **PAR-02 — Ninni enstrümanı:** içerik bağlamında düzenlenebilir alan ve public karşılığı. Sahip: CMS + backend + mobil. Dil davranışı tanımlanır; contributor kişi kaydı enstrüman yerine kullanılmaz.
- **PAR-03 — Yayınlanabilir örnek katalog:** mevcut CMS ile kapaklar, sayfalar, sesler, metadata ve ücretsiz örnekleri tamamla. Eksik asset/yayın kontrolleri mevcut yayın kapısından geçer.

Kabul: Hikâye, meditasyon ve ninni detay/oynatıcısı videodaki kredi bilgilerini gösterebilir; Dandini Dastana enstrümanı CMS'de değiştirildiğinde mobil yanıtta değişir. Kart dakikası ile oynatıcıdaki gerçek saniye süresi karıştırılmaz. Ses kapatma, ses dosyasını veya yayın gereksinimini kaldırmaz.

### Faz 2 — Kitaplık, Uyku ve kategori keşfinin yönetimi

- **PAR-04 — Ekran bölümleri:** Kitaplık/Uyku için bölüm sırası, bağlı kategori, görünürlük; gerektiğinde kategori varsayılanından farklı yerelleştirilmiş başlık/açıklama ve içerik dili. Sahip: CMS + backend; mobil sözleşmeye göre render eder.
- **PAR-05 — Arama kategori sırası:** arama keşfindeki kategori kutularının sırası/görünürlüğü. İçerik kürasyon sırasından ayrı yönetilir.
- **PAR-06 — Uyku üst alanı:** videodaki banner'ın metin/görsel yönetimi. Mevcut kategori ad/açıklama/görselleri tekrar modellenmez.
- **PAR-07 — İngilizce şerit:** Faz 0 dil kararı doğrultusunda UI dili ile içerik dilini ayrı taşı. Mevcut dil bazlı yayın/kürasyon doğrulamasını yanlış dile veri kopyalayarak aşma.

Kabul: Editör bir bölümün yerini CMS'den değiştirir, yeni mobil build olmadan katalog verisinde sıra değişir. Kitaplık, Uyku ve arama ekranlarında seçilen kategoriler istenen sırada gelir. İngilizce bölümün Türkçe arayüz başlığı ve seçilen içerik dili doğru kalır. “Hepsi” doğru kategori/dile gider. Yayın dışı içerik görünmez.

Tasarım kısıtı: ADR-0007'deki kategori–içerik türü uyumu korunur. Bir yüzeyin farklı türde kategorileri sunması, tek kategorinin karışık içerik türleri kabul etmesini gerektirmez. Yeni mimari/dil kararı kesinleşince ilgili ADR ve project-memory güncellenir.

### Faz 3 — arama eşdeğerliği

- **PAR-08 — Public arama:** Faz 0'da doğrulanan alanlar üzerinden `q`, dil ve isteğe bağlı tür; veritabanı tarafında filtreleme, kararlı sıralama ve sayfalama. Türkçe `ı/i/İ/I`, tire ve boşluk davranışı test örnekleriyle sabitlenir.
- **PAR-09 — Sonuç grupları:** Hikâyeler / Sesli Hikâyeler gibi tür başlıkları ve grup toplamları; toplam, o sayfada yüklenen kart sayısından ayrılır. Sorgu temizlenince kategori keşfine dönüş mobilde uygulanır.
- **PAR-10 — Aradığına Benzer Sonuçlar:** ana eşleşmelerden ayrılmış, Faz 0'da kararlaştırılan aday kaynağı; tekrarların ve görünmez içeriklerin elenmesi. Sırf bu kayıt için öneri altyapısı veya yapay zekâ sistemi kurulmaz.

Kabul: Sabit karşılaştırma veri setinde `hafta` için 4 hikâye/2 sesli hikâye; `zıt` için 2 hikâye/1 sesli hikâye beklenen kimliklerle döner. Bu sayılar üretim kataloğu büyüdüğünde sabit ürün kuralı değildir. Boş sorgu, sıfır sonuç, Türkçe harfler, hızlı değişen sorgular ve sonraki sayfa için doğru davranış doğrulanır.

### Faz 4 — hikâye sonu ve uçtan uca kabul

- **PAR-11 — Benzer Hikâyeler:** ilk tercih olarak içerik/dil başına sıralı editoryal seçim; otomatik seçim gerekiyorsa doğrulanan kurala göre. Mobil yanıt ve CMS düzenlemesi birlikte tamamlanır.
- **PAR-12 — Üç kayıtla kabul turu:** gerçek mobil uygulamada Kitaplık → hikâye → 14 sayfa → bitiş → benzer hikâye; arama → kategori → arama; Uyku → ninni/meditasyon → oynatıcı → geri akışları tekrar edilir.

Kabul: İlgili içerik listesi kendisini, tekrarları, pasif/yayımlanmamış veya seçili dilde görünmez içerikleri içermez; editoryal sırayı korur. Kilit/ücretsiz görünümü, anlatımı aç/kapa, sayfa geçişi, seek, çıkış ve yükleme/hata durumu doğrulanır. Mevcut CMS önizleyicisi tekrar yapılmaz; gerekirse eşdeğerliği engelleyen somut farkı giderilir.

## 4. Sorumluluk sınırı

| CMS | Backend | Mobil |
|---|---|---|
| İçerik, locale, sayfa/asset, contributor, enstrüman, kategori ve kürasyon, ekran sıraları, ilgili içerik seçimi | Veri kuralları, yayın görünürlüğü, dil çözümleme, public katalog/arama/credits/related sözleşmeleri, medya teslimi | Sayfa kaydırma, okuyucu kontrolleri, ses aç/kapa, oynat/duraklat/seek, sayfa sayacı, bitiş ekranı, arama sunumu ve gezinme |

CMS'de her mobil düğme için bir ayar açılması gerekmiyor. Kontrollerin ihtiyaç duyduğu içerik ve kurallar yönetilebilir olmalı.

## 5. Bu kapsamda eklenmeyecek işler

- Uyku zamanlayıcısı, çalma listesi, otomatik kuyruk, çevrimdışı indirme yönetimi, kişisel favoriler ve yeni kişiselleştirme: kayıtlarda doğrulanmıyor.
- “Ebeveynlerin Favorileri” kişisel favori düğmesi değil, gözlenen bir kategori adıdır.
- Profil ayrıntıları ve abonelik satın alma/paywall akışı: menü veya CTA görünse de akışları kaydedilmemiş.
- Aynı eserin hikâye/sesli hikâye bağlantısı ve yeni etiket taksonomisi: arama/benzerlik kararı gerektirmedikçe ayrı ürün geliştirmesi olarak açılmayacak.
- Görsel yeniden tasarım ve performans optimizasyonu: bu ilk yol haritasının hedefi değil; ölçülmüş eşdeğerlik engeli çıkarsa ele alınır.

## 6. Doğrulama durumu

Tamamlanan: üç videonun görsel örnekleme incelemesi, kaynak kod envanteri, var/eksik ayrımı, öncelik ve kabul ölçütleri. Uygulama kodu değiştirilmedi, test veya deployment çalıştırılmadı.

Sonraki somut adım: Faz 0'daki arama/benzerlik/dil kurallarını mevcut uygulama verisi veya mobil kaynak koduyla doğrulayıp PAR-01–PAR-12 işlerini uygulama hikâyelerine dönüştürmek. Mobil kaynak kodunun bu depoda bulunmaması uçtan uca kabul için bağımlılıktır; CMS/backend analizini engellemez.
