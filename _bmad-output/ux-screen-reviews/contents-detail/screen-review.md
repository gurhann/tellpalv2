# Contents Detail ekranı review

## Kapsam

Bu tur yalnızca `cms/src/app/routes/mockups/content-detail.tsx` mockup ekranını ele aldı. Production Contents Detail route’una ve TellPal domain belgelerine karşı fonksiyon/domain kontrolü yapıldı. Production ekranına değişiklik yapılmadı.

## Fonksiyon özeti

- Locale seçimi, yeni localization başlatma ve seçili locale bağlamını story pages’e taşıma.
- Locale başlığı/açıklaması, süre, cover, story narration, mobil görünürlük, processing ve publication durumu.
- Story preview ve story pages handoff.
- Ortak content metadata: type, external key, age range, active.
- Story’nin ortak listening cover’ı ile locale cover’ının ayrı yönetimi.
- Contributor assign/edit/reorder/unassign aksiyonları.
- Dil bağımsız source cover ve sayfa source görselleri.
- Asset seçme veya yeni görsel/ses yükleme girişleri.

## Tespit edilen sorunlar ve yapılan düzeltmeler

1. `Localized audio` ifadesi story narration ile sayfa sesini birbirine karıştırıyordu. Kart `Story narration` olarak ayrıştırıldı ve sayfa sesinin Story pages alanında yönetildiği açıklandı.
2. Story’nin ortak listening cover fonksiyonu görünmüyordu. Locale cover ve source cover’dan ayrı bir content-level asset kartı eklendi.
3. Locale alanları ve yaşam döngüsü statik görünüyordu. Compact form alanları, locale save, visibility, publication ve processing aksiyonları eklendi.
4. Production’daki story preview akışı görünmüyordu. Locale-aware preview dialog; play/pause ve sayfa ileri/geri kontrolleri eklendi.
5. Contributor listesi yalnızca bilgi gösteriyordu. Assign/edit/reorder/unassign affordance’ları eklendi; sıralama ve kaldırma mockup state’inde çalışır.
6. Shared metadata salt okunur görünüyordu. Type read-only kalacak şekilde external key, age range ve active alanları düzenlenebilir mockup formuna taşındı.

Kaynak görseller bölümü kullanıcının önceki kararı doğrultusunda ekranın en altında tutuldu.

## Domain uyumu

- `AUDIO_STORY` ayrı content type olarak eklenmedi; story narration, STORY’nin locale özelliği olarak gösterildi.
- Story narration ile story page audio ayrı tutuldu.
- Shared listening cover, localized reading cover ve textless/source cover birbirine karıştırılmadı.
- Domain karşılığı bulunmayan yeni kolon, odak alanı veya uydurma metadata eklenmedi.

## Doğrulama

Build, hedefli ESLint, mockup route testleri ve review artifact validator başarılıdır. Playwright görsel snapshot kontrolü, makinede Chromium binary’si bulunmadığı ve indirme zaman aşımına uğradığı için çalıştırılamadı.

Bu ilk turdaki açık karar noktası: mockup’taki contributor ve localization dialoglarının sonraki turda daha ayrıntılı form akışına mı dönüşeceği, yoksa mevcut production bileşenlerinin birebir prototiplenmesinin mi tercih edileceği.

## İterasyon 2 — type-specific detail mockupları

Kullanıcı geri bildirimiyle Contents registry’deki Meditation ve Lullaby kayıtları da detay ekranına bağlandı. Tek bir Story şablonunu diğer türlere taşımak yerine, her tür için domain’e uygun bir detay varyantı oluşturuldu:

- Meditation: locale başlığı/açıklaması, locale ses asset’i, süre ve ortak listening cover.
- Lullaby: locale metadata’sı, ortak playback ses/süresi, enstrüman yönetimi girişi, static listing cover ve animated playback cover.
- Story: mevcut locale, narration, preview, story pages ve source images akışı korunuyor.

Registry’deki üç type tablosunun satırları artık kendi detay mockup rotasına açılıyor. Bu amaçla dynamic `:contentId` mockup route’u ve 2 yeni route testi eklendi. Production route ve API davranışı değiştirilmedi.

## İterasyon 3 — detail hiyerarşisi ve yoğunluk geri bildirimi

Kullanıcının geri bildirimiyle üç type-specific detay varyantı aynı hiyerarşiye hizalandı:

- Dil bağımsız metadata ve içerik seviyesindeki asset’ler, dil çalışma alanından önce konumlandırıldı.
- Dil sekmeleri compact sunuma alındı; sekme içinde tekrar eden `Published` / `In review` metinleri kaldırıldı ve durum renk tonu ile korunuyor.
- Ayrı `mobil görünürlüğü değiştir` aksiyonu kaldırıldı. Locale yaşam döngüsü ve operasyon özeti korunurken ikinci bir görünürlük kontrolü gösterilmiyor.
- Yüklenmiş görsel ve sesler büyük durum panelleri yerine sınırlı görsel thumbnail ve kompakt waveform/player önizlemesiyle gösteriliyor. Asset yönetme, ekleme, seçme ve yükleme girişleri korunuyor.

Bu değişiklikler yalnızca mockup kapsamındadır; production Contents Detail route’una uygulanmamıştır. Story’nin kaynak görselleri yine ekranın en altında kalır.

## İterasyon 4 — tekrar eden içeriklerin sadeleştirilmesi

- Locale çalışma alanındaki salt-okunur başlık/açıklama özet kartı kaldırıldı; başlık ve açıklama artık yalnızca düzenlenebilir form alanlarında gösteriliyor.
- FormSection açıklamaları kaldırıldı. Bölüm başlıkları, gerekli form label’ları ve fonksiyonel asset açıklamaları korunuyor.
- Bu sadeleştirme yalnızca bilgi tekrarını azaltır; locale seçimi, düzenleme, kaydetme, yayınlama ve asset yönetimi fonksiyonlarını değiştirmez.

Bu iterasyonda CMS build, hedefli ESLint ve mockup route testleri tekrar çalıştırıldı; 10 test başarılıdır.

## Düzeltme — son geri alma

Kullanıcının “İçerik detayı” ifadesiyle kastettiği alanın shared metadata bölümü değil, sayfa üstündeki eyebrow, başlık ve açıklama bloğu olduğu netleştirildi. Bu nedenle son yanlış yorumlanan Story metadata/aksiyon yerleşimi geri alındı; mockup bir önceki doğrulanmış yapısına döndürüldü.

## İterasyon 6 — Story içerik detayı panelinin kaldırılması

- Story için ayrı `İçerik detayı` bölümü kaldırıldı.
- External key, yaş aralığı, aktiflik ve ortak listening cover yönetimi dil çalışma alanının içinde kompakt ortak context satırına taşındı.
- Tür badge’i, locale-aware `Hikâyeyi önizle`, `Hikâye sayfalarını aç` ve `Dil ekle` aksiyonları dil çalışma alanı başlığında bir araya getirildi.
- Back-to-registry aksiyonu sayfa başlığında bırakıldı; Story pages bağlantısında seçili dil bağlamı korunuyor.

Bu değişiklik Story mockup’ına uygulanmıştır. Meditasyon ve Ninni’nin type-specific içerik alanları bu iterasyonda değiştirilmemiştir.

## Düzeltme — kapsamın netleştirilmesi

Kaldırılan alan yalnızca sayfa üstündeki `İçerik detayı`, hikâye adı ve açıklama kartıdır. Shared metadata bölümü eski yerleşiminde bırakıldı; geri dön, önizle ve Story pages aksiyonları dil çalışma alanı başlığına taşındı.

## İterasyon 7 — Meditation ve Lullaby başlık kartlarının kaldırılması

Story ile aynı sadeleştirme Meditation ve Lullaby detay varyantlarına uygulandı:

- Üstteki eyebrow, içerik adı ve açıklama kartı kaldırıldı.
- İçerik listesine dönüş aksiyonu ilgili locale workspace başlığına taşındı ve mevcut locale durum badge’i korundu.
- Meditation ses/süre; Lullaby playback, enstrüman ve kapak varyantları değişmeden bırakıldı.
