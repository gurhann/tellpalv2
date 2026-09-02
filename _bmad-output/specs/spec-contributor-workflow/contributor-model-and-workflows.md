# Contributor model ve akış sözleşmesi

## Domain modeli

```text
Contributor
  id
  displayName
  normalizedDisplayName
  roles: Set<ContributorRole> (en az 1)

ContentContributor
  contentId
  contributorId
  role (Contributor.roles içinde tam 1 değer)
  languageCode? (null = tüm diller)
  creditName?
  sortOrder (aynı content + role + language grubu içinde)
```

Rol, profil üzerinde keşif ve uygunluk bilgisidir. Bir içerikte gösterilecek gerçek kredi; rol, dil kapsamı, kredi adı ve sırasıyla birlikte `ContentContributor` üzerinde kalır.

## Kalıcılık ve migration

- Contributor rollerini contributor kimliğine bağlı çok değerli bir ilişki olarak sakla; aynı contributor/rol çifti veritabanında benzersiz olmalıdır.
- `displayName` yazma sırasında trim edilir. `normalizedDisplayName`, trim edilmiş adın case-insensitive karşılığıdır ve benzersizliği veritabanı kısıtıyla korunur.
- Mevcut roller `content_contributors` kayıtlarındaki distinct `(contributor_id, role)` çiftlerinden backfill edilir.
- Backfill, mevcut `content_contributors` satırlarını veya kredi metadata'sını yeniden yazmaz.
- Tarihsel assignmentı olmayan contributorlar backfill sırasında `AUTHOR` rolü alır.
- Migration, normalize ad benzersizliği uygulanmadan önce mevcut adları trim ve case-insensitive biçimde tarar. Çakışma varsa contributor ID ve adlarını hata raporuna dahil edip durur; otomatik merge yapmaz.
- Duplicate preflight başarılı olduktan sonra zorunlu rol ve normalize ad benzersizliği kısıtları etkinleştirilir.

## Flyway çalıştırma sözleşmesi

- Değişiklik bir sonraki sıralı migration olarak `V20__add_contributor_roles_and_normalized_name.sql` dosyasında uygulanır.
- Migration PostgreSQL transaction desteğiyle çalışır; duplicate preflight şema değişikliğinden önce yürütülür.
- Duplicate bulunursa açık bir exception ile çakışan contributor ID/adlarını raporlar. Transaction geri alınır ve backend başlamaz.
- Projede `spring.flyway.enabled=true` olduğu için migration backend başlangıcında otomatik çalışır; IntelliJ'den Spring Boot uygulamasını başlatmak yeterlidir.
- Docker akışında backend container başladığında aynı otomatik migration çalışır. Yalnız PostgreSQL çalıştırılıp backend IntelliJ'den açılırsa migration yine IntelliJ başlangıcında uygulanır.
- İsteğe bağlı manuel çalıştırma `be` dizininden `./mvnw flyway:migrate` veya Windows'ta `.\\mvnw.cmd flyway:migrate` ile yapılır; Maven plugin mevcut yerel `localhost:5432/tellpal_v2` ayarını kullanır.
- Başarıyla uygulanan sürüm `flyway_schema_history` tablosuna yazılır ve sonraki başlangıçlarda yeniden çalıştırılmaz.
- Duplicate nedeniyle duran migration, veriler düzeltildikten sonra backend veya manuel migrate komutu yeniden çalıştırılarak tekrar denenir.

## Admin API sözleşmesi

### Contributor registry read modeli

Yeni registry read sözleşmesi şu sorgu bağlamını destekler:

- `q`: görünen ad araması
- `role`: opsiyonel contributor rolü
- `page`: sıfır tabanlı sayfa
- `size`: varsayılan 25, en çok 100
- varsayılan sıra: son güncellenen önce, eşitlikte ID azalan

Yanıt her satır için en az şunları taşır:

- contributor kimliği ve görünen adı
- roller
- rol bazlı kullanım sayıları
- toplam içerik kullanım sayısı
- son güncelleme zamanı

Mevcut `GET /api/admin/contributors` sözleşmesi ilk teslimde korunur; sayfalı CMS registry ve picker için ayrık bir database-backed read endpoint kullanılır.

### Profil komutları

- Create: `displayName`, zorunlu ve benzersiz `roles[]`
- Update: `displayName`, zorunlu ve benzersiz `roles[]`
- Bir rol kaldırılmadan önce o contributor/rol için assignment kullanımı kontrol edilir.
- Kullanılan rol kaldırma isteği conflict problem detail döndürür; response kullanım sayısını ve etkilenen içerik referanslarını taşır.
- Normalize ad çakışması conflict problem detail döndürür ve mevcut contributor kimliğini taşır.

### Assignment komutları

- Assignment rolü contributor profilinde yoksa istek reddedilir.
- Varsayılan dil kapsamı CMS tarafından bağlama göre gönderilir; backend gelen kapsamı doğrular.
- Yeni assignment için sıra, aynı content/role/language grubunun sonuna transaction içinde atanır.
- Reorder komutu aynı grubun contributor assignment kimliklerini eksiksiz ve benzersiz bir sıra olarak kabul eder; grup dışı veya eksik ID'leri reddeder.

## İçerik detayı UX akışı

Contributor bölümü seçili dil çalışma alanının hemen altında yer alır. Mevcut krediler rol başlıkları altında, dil kapsamı alt gruplarıyla gösterilir.

```text
Contributorlar

Yazarlar
  Ayşe Yılmaz · Tüm diller                         Düzenle  Kaldır
  + Yazar ekle

Seslendirenler
  Mehmet Kaya · Türkçe                             Düzenle  Kaldır
  + Seslendiren ekle
```

Her rol başlığındaki ekleme eylemi picker'ı o rolle açar. Picker:

1. Rolü görünür ve hazır seçili bağlam olarak gösterir.
2. Aramayı yazarken database-backed sonuçları getirir.
3. Sonuçları ayrı bir select gerektirmeden doğrudan seçilebilir satırlar olarak sunar.
4. Exact normalize ad bulunmazsa `“{ad}” adında yeni {rol} oluştur` eylemini gösterir.
5. Inline create içinde görünen ad ve roller bulunur; başlatan rol seçili ve kaldırılamaz halde kalır.
6. Oluşturma sonrası contributorı seçili içerik, rol ve varsayılan dil kapsamına atar.
7. Assignment başarısız olursa oluşturulan profili kaybetmez; aynı seçimle yeniden deneme sunar.

Dil kapsamı ve kredi adı “Ek ayarlar” altında kalır. Sıra alanı formda gösterilmez.

## Varsayılan dil kapsamları

| Rol | Varsayılan kapsam |
| --- | --- |
| `AUTHOR` | Tüm diller |
| `ILLUSTRATOR` | Tüm diller |
| `MUSICIAN` | Tüm diller |
| `NARRATOR` | İçerik detayında seçili dil |

Editör gelişmiş assignment ayarlarından kapsamı değiştirebilir.

## Sıralama

- Yeni assignment kendi content/role/language grubunun sonuna eklenir.
- Bir grupta iki veya daha fazla assignment olduğunda sürükle-bırak tutamacı ve klavye erişilebilir yukarı/aşağı alternatifi sunulur.
- Optimistic görünüm backend conflict durumunda önceki sıraya döner ve inline hata verir.
- Sıra başka rol veya dil grubuna taşınamaz; rol/dil değişikliği assignment düzenleme işlemidir.

## Contributor registry UX

- Shared `RegistryToolbar` kullanılır.
- Arama ve rol filtreleri URL'de korunur.
- Tablo kolonları: contributor, roller, kullanım özeti, son güncelleme, eylemler.
- Create/Edit dialogunda görünen ad ve çoklu rol seçimi zorunludur.
- Rol kaldırma engeli, kullanım sayısını ve etkilenen içeriklere bağlantıları aynı dialogda gösterir.
- Tablo shared pagination primitive kullanır; sonuç sayısı backend toplamıdır.

## Hata ve edge-case matrisi

| Senaryo | Beklenen davranış |
| --- | --- |
| Role uygun sonuç var | Satır tek tıklamayla seçilir ve assignment detayına geçilir. |
| Sonuç yok | Yazılan ad ve seçili rol ile inline create sunulur. |
| Normalize ad zaten var | Yeni kayıt oluşturulmaz; mevcut contributor seçilebilir halde gösterilir. |
| Contributor birden çok role sahip | Role göre tüm uygun aramalarda görünür; içerik ataması tek rol taşır. |
| Kullanılan rol kaldırılmak istenir | Conflict; kullanım sayısı ve içerik bağlantıları gösterilir. |
| Inline create başarılı, assignment başarısız | Oluşturulan profil seçili kalır ve assignment yeniden denenir. |
| İki editör aynı gruba eşzamanlı ekler | Backend geçerli son sırayı transaction içinde üretir; duplicate sıra bırakmaz. |
| Reorder eksik veya yabancı ID içerir | İstek reddedilir ve mevcut sıra korunur. |
| Contributor sonuçları 25'i aşar | Backend toplamıyla sayfalama yapılır; istemci yalnız istenen sayfayı tutar. |

## Test sözleşmesi

Backend:

- Contributor en az bir ve birden fazla rol invariant testleri
- Normalize ad uniqueness PostgreSQL entegrasyon testi
- Mevcut assignmenttan rol backfill migration testi
- Rol kullanımı varken kaldırma conflict testi
- Role/ad filtreli registry sayfalama ve deterministic sıra entegrasyon testi
- Assignment rolünün profil rollerinde bulunması testi
- Otomatik sıra ve aynı grup reorder transaction testleri
- Spring Modulith boundary doğrulaması

CMS:

- Role göre açılan picker ve database-backed arama testleri
- Sonuç satırından doğrudan assignment testi
- Inline create + otomatik assignment ve retry testi
- Varsayılan dil kapsamı matrisi testi
- Duplicate ada mevcut kayda yönlendirme testi
- Rol gruplama ve erişilebilir reorder testi
- Kullanılan rol kaldırma conflict görünümü testi
- Contributor registry URL filtreleri ve shared pagination testi
- İçerik detayında contributor bölümünün locale workspace sonrasında bulunduğunu doğrulayan component/route testi
- Masaüstü içerik detayı ve contributor registry görsel regression baseline'ı
