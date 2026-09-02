---
name: TellPal CMS — Ürün Keşif Günlüğü
status: active
started: 2026-09-02
review-cadence: P2W
next-review: 2026-09-16
---

# Ürün Keşif Günlüğü

Bu belge kullanıcı cevaplarını, yorumları ve açık kararları birbirinden ayırır.
Kesinleşen kararların tarihçesi `.memlog.md` içinde tutulur; `DESIGN.md` ve
`EXPERIENCE.md` yalnızca tasarım sonuçları olgunlaştığında güncellenir.

## Çalışma kuralları

- Her soru tek bir karar ihtiyacını hedefler; kullanıcı cevabı yorumsuz saklanır.
- Her cevabın altında yorum, kanıt ve güven seviyesi yer alır.
- Açık veya çelişkili konular `Açık kararlar` bölümüne eklenir; kesinleşmiş
  olanlar memlog'a taşınır.
- İki haftada bir önceki ve güncel güven seviyesi karşılaştırılır. Yeni somut
  kanıt, karar değişikliği veya çelişki beklemeden ayrıca kaydedilir.

## Güven ölçeği

| Seviye | Anlamı |
| --- | --- |
| C0 — bilinmiyor | Henüz cevap veya kanıt yok. |
| C1 — hipotez | İlk varsayım var; kullanıcı doğrulaması yok. |
| C2 — çalışma kararı | Kullanıcı eğilimi veya sınırlı kanıt var; değişebilir. |
| C3 — teyitli | Kullanıcı açıkça karar verdi. |
| C4 — doğrulandı | Gerçek kullanım, test veya ölçüm kararı destekledi. |

## Oturum 01 — İçerik kayıt ekranı

**Tarih:** 2026-09-02  
**Amaç:** Contents yüzeyinin işini ve gelecek dashboard ile sınırını netleştirmek.

| Kimlik | Soru / kullanıcı cevabı | Yorum | Güven |
| --- | --- | --- | --- |
| Q-001 | “Burası sadece hikâyeler için değil; sesli hikâye, meditasyon ve ninni içerikleri de olacak.” | Kapsam `story list` değil, tür bağımsız `content registry` olmalı. | C3 |
| Q-002 | “Genel durumu görmek, ulaşmak istediğim hikâyeye hızlıca ulaşıp onda ne eksik var düzenlemek; daha sonra güncelleme yapmak.” | Birincil işler: sağlık görünümü, içerik bulma, yayın engelini görme, detayda düzenleme. | C3 |
| Q-003 | Genel bilgi için “ayrı dashboard”. | Contents operasyonel kayıt ekranı kalır; genel sayısal görünüm ayrı bir ana yüzeydir. | C3 |
| Q-004 | Eksik sinyali: “yayınlanabilirlik”. | Ham alan eksikleri değil, seçili dilde yayına engel olan koşullar listelenir. | C3 |
| Q-005 | Yayınlanabilirlik: “seçili dil”. | Liste ve durumlar tek aktif dil bağlamında hesaplanır. | C3 |
| Q-006 | Arama: “başlık, anahtar, ID”. | Arama seçili dildeki başlığı, `externalKey` değerini ve `contentId` değerini kapsar. | C3 |
| Q-007 | Satır ana aksiyonu: “detay düzenleyiciyi aç”. | Satıra tıklama ilgili içeriğin detayını seçili dil bağlamıyla açar; satır içi düzenleme yoktur. | C3 |
| Q-008 | Dashboard önceliği: “tür × dil sağlık matrisi”. | Her hücre toplam, yayına hazır/yayında ve aksiyon gerekli sayılarını özetlemelidir; detay görünümleri sonraki keşif konusudur. | C3 |
| Q-009 | Durum modeli: “üç net durum”. | Seçili dil için `aksiyon gerekli`, `yayına hazır`, `yayında` gösterilir. Sistem işlem kodları ana etiket değil, gerekçe olur. | C3 |
| Q-010 | İnceleme ritmi: “takvim bazlı”, “iki haftada bir”. | Periyodik netlik değerlendirmesi iki haftada bir yapılır. | C3 |
| Q-011 | Eksiklik ayrıntısı: “tek öncelikli neden”. | Tarihsel karar; Q-021 ile geçersiz kılındı. | C3 |
| Q-012 | Filtre yoğunluğu: “odaklı filtreler”. | Toolbar’da tür, seçili dil ve üçlü durum görünür; diğer filtreler başlangıçta gösterilmez. | C3 |
| Q-013 | Varsayılan sıralama: “son güncellenen”. | Son güncellenen ilk sıralamadır; arama ve filtreler bu sıralamayı bozmaz, yalnızca sonucu daraltır. | C3 |
| Q-014 | Başlangıç dili: “sabit Türkçe”. | İlk Contents açılışının varsayılan dili Türkçe'dir. | C3 |
| Q-015 | Dil değişince: “seçilen dili koru”. | Kullanıcı dili değiştirdiğinde dil URL’de korunur; yenileme ve paylaşılan bağlantı aynı dil bağlamını açar. | C3 |
| Q-016 | “Mobil uygulamada görüntülenebilmesi için adı, açıklaması, her bir sayfanın görsel ve ses dosyaları ve sayfanın texti sanırım ilk aşama bunlar.” | `STORY` için ilk yayınlanabilirlik kontrol listesi: seçili dilde başlık, açıklama, tüm sayfalarda metin, görsel ve ses. “Sanırım” ifadesi nedeniyle bu henüz teyitli karar değil; içerik kapak görselinin zorunluluğu ayrıca açıkta tutulur. | C2 |
| Q-017 | Hikâye kapak görseli: “zorunlu”. | Seçili dildeki hikâye kapak görseli mobil yayınlanabilirliğin zorunlu koşuludur. | C3 |
| Q-018 | “Diğer tipleri şimdilik geçip ilerde dönebilir miyiz? Sisteme şu ana kadar story girdim; diğer tipleri eklediğimde detaylarına bakarız.” | Bu keşif turu `STORY` gerçek verisi ve akışıyla sınırlı. Sesli hikâye, meditasyon ve ninni yayınlanabilirlik kuralları ilgili içerik ilk kez eklendiğinde ayrı oturumda çıkarılacak. | C3 |
| Q-019 | “Bu durum genelde olmaz; uygulama Türk geliştiriciler tarafından geliştiriliyor, içerikler ilk olarak Türkçe üretiliyor.” | Türkçe içerik üretiminin kaynak dilidir. Türkçe lokalizasyonu olmayan içerik, normal çalışma akışının değil, nadir ithalat/veri istisnasının parçasıdır. | C3 |
| Q-020 | CMS’in günlük kullanıcısı: “editör kullanacak şimdilik”. | İlk persona editördür. Contents; teknik yönetim ekranı değil, anlaşılır engel gerekçeleri ve net düzenleme aksiyonları olan editoryal çalışma yüzeyi olarak tasarlanır. | C3 |
| Q-021 | “Burada bir öncelik yok. Yayına engel durum varsa uyarı çıkmalı ve belki üzerine gelince hepsi listelenmeli.” | Seçili dilde bir veya daha fazla yayın engeli varsa satır `aksiyon gerekli` uyarısı verir; engeller arasında öncelik tanımlanmaz. Tüm engellerin etkileşimle görünmesi C2 çalışma kararıdır; erişilebilir ayrıntı deseni netleştirilecektir. | C3 / C2 |
| Q-022 | Açıklama, lokalize kapak gibi gerçek publish önkoşulu olsun mu? “Evet olsun.” | STORY için açıklama zorunluluğu teyit edildi. Liste ve publish komutu bu ortak koşulu uygulamalıdır. | C3 |

## Açık kararlar

| Kimlik | Karar ihtiyacı | Neden açık | Sonraki kanıt / soru |
| --- | --- | --- | --- |
| O-001 | `STORY` yayınlanabilirlik kuralı | Başlık, açıklama, lokalize kapak ve her sayfada metin/görsel/ses, STORY publish kontrol listesidir. Kod ve test sonrasında C4'e yükseltilecektir. | Ortak policy ve uçtan uca test ile doğrula. |
| O-005 | Sesli hikâye, meditasyon ve ninni yayınlanabilirlik kuralları — ertelendi | Bu türler için henüz gerçek içerik veya doğrulanmış editoryal akış yoktur. | İlgili tür ilk kez CMS’e eklendiğinde keşif oturumunu yeniden aç. |
| O-002 | Seçili dilde lokalizasyonu olmayan içerik davranışı — ertelendi | Türkçe kaynak dil olduğu için bu normal editoryal akış değildir; yalnızca nadir ithalat/veri istisnasında ortaya çıkar. | İthal içerik veya Türkçe dışı kaynak akışı geldiğinde davranışı tanımla. |
| O-003 | Dashboard drill-down sözleşmesi | Tür × dil hücresine tıklamanın Contents’e hangi filtrelerle gitmesi gerektiği belirlenmedi. | Dashboard keşif turunda matris hücresi aksiyonunu kararlaştır. |
| O-004 | “Son güncellenen” zaman kaynağı | Mevcut liste API’si güncelleme zamanı döndürmüyor. | Backendde içerik ve lokalizasyon değişikliğinin hangi zaman damgası esas alınacağını teknik olarak doğrula. |
| O-006 | Tüm yayın engellerini açma deseni | Kullanıcı engellerin satırdan görülebilmesini istiyor, fakat “üzerine gelince” ifadesi henüz kesin etkileşim sözleşmesi değildir. | Masaüstünde hover/focus, klavyede Enter/Space ve dokunmatikte tap ile çalışan popover mı; yoksa satır içi açılım mı olacağını gerçek ekran üzerinde doğrula. |

## Artefakt durumu

- `contents-registry-first-draft.html` — 2026-09-02 tarihinde kullanıcı tarafından ilk taslak olarak kabul edildi. Bu onay, içerik hiyerarşisini ve ekranın kapsamını kapsar; üretim davranışı için O-001, O-004 ve O-006 açıkları teknik tasarımda ele alınacaktır.

## İki haftalık değerlendirme şablonu

1. Her C0–C2 kaydını tekrar sor veya yeni kanıtla değerlendir.
2. C3 kararlarını kullanım/test kanıtı varsa C4'e yükselt; çelişki varsa gerekçesiyle C2'ye indir.
3. Açık kararların etkisini ve sonraki sorusunu güncelle.
4. Yeni veya değişen kesin kararları `.memlog.md` içine ekle.
5. Sonraki değerlendirme tarihini iki hafta ileri al.
