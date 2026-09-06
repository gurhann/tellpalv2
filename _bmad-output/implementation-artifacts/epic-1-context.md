# Epic 1 Context: Editörler ses ve kapak varlıklarını tutarlı yönetir

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Tek bir canonical hikâyeye locale bazlı tam anlatım eklemeyi; ninninin ortak ses, textless kapak, süre, müzisyen ve enstrüman bilgisini ise bir kez yönetmeyi mümkün kılmak. Bu sahiplik sınırları yinelenen medya verisini ve eski bağımsız `AUDIO_STORY` kimliklerini önler; editörlerin her dilde yalnızca gerçekten yerelleşen bilgiyi değiştirmesini sağlar.

## Stories

- Story 1.1: Playback işleme hedeflerini güvenilir biçimde ayırma
- Story 1.2: Hikâyeye dil-bazlı tam anlatım ekleme
- Story 1.3: Ortak textless kapak sahipliğini uygulama
- Story 1.4: Ninninin ortak playback’ini ve başlık-only localization’ını yönetme
- Story 1.5: Ninni için katalogdan sıralı enstrüman seçme
- Story 1.6: Canonical `AUDIO_STORY` türünü güvenle kaldırma
- Story 1.7: CMS’te playback ve localization editörlerini ayrıştırma

## Requirements & Constraints

- Tam anlatım yalnızca `STORY` localization’ının isteğe bağlı verisidir; tek parça `AUDIO` asset’i ve süre içerir. Var olan sayfa bazlı sesler korunur ve tam anlatımın yerine geçmez.
- Ninninin playback verisi content düzeyinde tektir: ses, süre, ortak textless kapak, global `MUSICIAN` kredileri ve seçilmiş enstrümanlar diller arasında kopyalanamaz. Ninni localization’ı yalnızca başlık ve yayın durumunu kabul eder.
- Textless kapak, sesli hikâye sunumu, ninni ve meditasyon için tek bir pozitif `IMAGE` asset referansıdır. STORY okuma deneyimi, ayrı olarak locale-specific kapağını kullanmaya devam eder.
- Enstrümanlar serbest metin ya da contributor değildir. Kararlı kodlu, yerelleştirilmiş katalogdan seçilir; seçimler benzersiz, sıfırdan başlayan sıralı ve mevcut seçimleri bozmadan yeniden sıralanabilir olmalıdır. Kullanımdaki katalog kaydı silinemez; yalnızca yeni seçimlere kapatılabilir.
- Canonical `AUDIO_STORY` yeni content, category veya processing verisinde kabul edilmez. Şema daraltılmadan önce bu değeri kullanan mevcut content, category veya processing kayıtları açık bir hata ile migration’ı durdurmalıdır; veri dönüştürme/import kapsam dışıdır.
- Asset referansları pozitif kimlikte ve beklenen medya türünde doğrulanır. URL ve processing çıktıları content içinde kopyalanmaz; okuma sırasında asset modülünden çözülür.
- Değişiklikler Flyway, domain, REST, persistence, CMS davranışı ve Spring Modulith sınırları için orantılı test kapsamı sağlamalıdır.

## Technical Decisions

- `content` editoryal kimliği, playback sahipliğini, enstrüman seçimini ve read projection’larını sahiplenir. `asset` yalnızca asset kaydı, çözümleme ve operasyonel processing durumunu sahiplenir; modüller arası erişim public API üzerinden yapılır. `category` yalnızca canonical türlerle kürasyon yapar.
- `StoryNarration`, `ContentLocalization`ın isteğe bağlı aggregate child’ıdır. `LullabyPlayback`, `Content`ın tek ortak child’ıdır; ortak kapak mevcut `Content.textlessCoverMediaId` alanını kullanır. Önerilen kavramlar `InstrumentCatalog`, `InstrumentCatalogLocalization` ve sıralı `LullabyInstrument` ilişkileridir.
- Processing hedefi tiplenmiş olmalıdır: anlatım için `LOCALIZATION(contentId, languageCode)`, ninni playback’i için `CONTENT(contentId)`. Veritabanı hedef-kapsam/dil uyumsuzluklarını ve her hedefte yinelenen aktif kaydı engeller; content-scoped okuma `findByContent(contentId)` ile desteklenir.
- Story anlatım hatası yalnızca o locale’ın sesli deneyimini etkiler, okunabilir hikâyeyi veya diğer dilleri etkilemez. Ortak ninni processing hatası her locale projeksiyonunu gizler fakat editoryal başlık ya da yayın durumunu değiştirmez.
- Public sözleşmelerde canonical kimlik ile deneyim ayrıdır: sesli hikâye `canonicalType: STORY` ve `experienceType: AUDIO_STORY` olarak projekte edilir; `AUDIO_STORY` kalıcı bir content türü değildir. Public discovery/uyumluluk ayrıntıları bu epic’te uygulanmaz.

## UX & Interaction Patterns

- CMS, sahiplik seviyesine göre tek baskın düzenleme akışı sunar: story narration ilgili localization editöründe; textless kapak ve ninni playback’i content editöründe yer alır.
- LULLABY localization formlarında ses, süre, kapak, gövde/açıklama, müzisyen veya enstrüman alanları gösterilmez. MEDITATION localization’ında cover alanı gösterilmez; mevcut locale bazlı ses ve metin alanları korunur.
- Yeni content oluşturma ve filtre akışlarında `AUDIO_STORY` seçeneği bulunmaz. Asset seçiciler erişilebilir, etiketleri anlaşılır ve farklı viewport’larda kullanılabilir kalmalıdır; layout/interaction değişiklikleri component ve görsel regresyon kapsamı alır.

## Cross-Story Dependencies

- Story 1.1’in typed processing modeli, Story 1.2’nin narration processing’i ve Story 1.4’ün ortak lullaby processing’i için temel bağımlılıktır.
- Story 1.3’ün content-level cover sahipliği, Story 1.4’ün ortak playback editorü ve Story 1.7’nin form ayrımıyla birlikte uygulanmalıdır.
- Story 1.5, Story 1.4’ün shared lullaby aggregate’ine dayanır. Story 1.6, scoped processing migration’ı kurulduktan sonra canonical `AUDIO_STORY` şemasını korumalı biçimde kaldırmalıdır.
