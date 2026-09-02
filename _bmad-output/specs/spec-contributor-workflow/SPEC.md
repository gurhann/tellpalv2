---
id: SPEC-contributor-workflow
companions:
  - contributor-model-and-workflows.md
  - ../../../architecture.md
  - ../../../be/docs/adr/ADR-0009-registry-read-pagination.md
  - ../../../cms/docs/ui-standards.md
sources: []
---

> **Canonical contract.** Bu SPEC ve `companions:` altındaki dosyalar, geliştirilecek ve doğrulanacak contributor akışının eksiksiz sözleşmesidir.

# Contributor yönetimi ve içerik atama akışı

## Why

Editör bir hikâyeye yazar, çizer, seslendiren veya müzisyen eklerken tüm contributor kayıtlarında arama yapmak, aynı sonucu ikinci kez seçmek ve teknik sıralama alanlarını doldurmak zorunda kalıyor. Contributor profillerinin rol taşımaması, role göre keşfi ve bağlam içinde yeni kayıt oluşturmayı engelliyor; içerik üretiminin sık kullanılan bir parçası gereksiz yere yavaşlıyor.

## Capabilities

- **CAP-1**
  - **intent:** Editör, contributor profilinde bir veya daha fazla rol tanımlayabilir.
  - **success:** Profil en az bir benzersiz rol olmadan oluşturulamaz veya kaydedilemez; aynı profil birden çok rol taşıyabilir.

- **CAP-2**
  - **intent:** Editör, contributorı bir içeriğe profilindeki rollerden tam biriyle ve isteğe bağlı dil kapsamıyla atayabilir.
  - **success:** Atama, contributor profilinde bulunmayan bir rolü kabul etmez ve mevcut kredi adı ile dil kapsamı davranışını korur.

- **CAP-3**
  - **intent:** Editör, contributorları ad ve role göre ölçeklenebilir biçimde arayabilir.
  - **success:** Filtreleme, sıralama ve sayfalama veritabanında gerçekleşir; rol filtresi yalnızca uygun profilleri döndürür ve istemci tüm kayıtları belleğe almaz.

- **CAP-4**
  - **intent:** Editör, aradığı kişi yoksa atama akışından ayrılmadan yeni contributor oluşturup aynı içeriğe atayabilir.
  - **success:** Yazılan ad ve bağlamsal rol yeni profile aktarılır, kayıt oluşturulduktan sonra seçili içeriğe otomatik atanır ve başarısız atama yeniden denenebilir.

- **CAP-5**
  - **intent:** Editör, içerik detayında contributorları rol ve dil kapsamına göre gruplanmış biçimde yönetebilir.
  - **success:** Gruplar doğrudan ekle, düzenle, kaldır ve yeniden sırala eylemleri sunar; contributor bölümü seçili dil çalışma alanının hemen altında kalır.

- **CAP-6**
  - **intent:** Sistem, aynı görünen ada sahip mükerrer contributor profillerini engeller.
  - **success:** Baştaki/sondaki boşluklar ve harf büyüklüğü yok sayıldığında aynı olan ikinci ad reddedilir ve arayüz mevcut kaydı seçmeye yönlendirir.

- **CAP-7**
  - **intent:** Editör, contributor profilinin rollerini kullanım bütünlüğünü bozmadan güncelleyebilir.
  - **success:** Kullanılmayan rol kaldırılabilir; mevcut içerik atamasında kullanılan rol kaldırılamaz ve engel, kullanım sayısı ile etkilenen içerik bağlantılarını gösterir.

- **CAP-8**
  - **intent:** Sistem, mevcut contributorların rollerini tarihsel içerik atamalarından kayıpsız üretir.
  - **success:** Migration, her contributor için distinct atanmış rolleri profile taşır ve mevcut kredi, dil kapsamı ve sıralamaları değiştirmez.

- **CAP-9**
  - **intent:** Editör, contributor registry üzerinden rol bilgisiyle birlikte profil oluşturabilir, arayabilir, düzenleyebilir ve silebilir.
  - **success:** Registry rol etiketleri, ad/rol filtreleri ve veritabanı destekli sayfalama sunar; sonuç sayısı yalnızca yüklenmiş son kayıtları değil gerçek sorguyu temsil eder.

## Constraints

- Contributor profili en az bir, bir içerik ataması tam bir rol taşır; atama rolü profil rollerinden biri olmalıdır.
- İlk rol kataloğu yalnızca `AUTHOR`, `ILLUSTRATOR`, `NARRATOR` ve `MUSICIAN` değerlerini içerir.
- `AUTHOR`, `ILLUSTRATOR` ve `MUSICIAN` varsayılan olarak tüm dillere; `NARRATOR` seçili içerik diline atanır. Dil kapsamı gelişmiş ayarlardan değiştirilebilir.
- Yeni atama aynı rol ve dil grubunun sonuna otomatik eklenir. Kullanıcıya sayısal `sortOrder` alanı gösterilmez; sıralama yalnızca grup içinde doğrudan manipülasyonla değiştirilir.
- Contributor ve atama verileri mevcut `content` modülünün sahipliğinde kalır; yeni modül sınırı veya çapraz internal bağımlılık oluşturulmaz.
- Contributor registry sorguları ADR-0009'a uyar; uygulama veya istemci belleğinde toplu filtreleme, sıralama ya da sayfalama yapılmaz.
- Contributor bölümü içerik detay ekranında kalır ve ayrı bir günlük editör akışına taşınmaz.
- Görünen metinler CMS i18n katmanı üzerinden Türkçe ve İngilizce sağlanır.
- Tarihsel assignmentı olmayan contributorlar migration backfill sırasında `AUTHOR` rolü alır.
- Normalize edilmiş mevcut contributor adları çakışırsa migration kimlikleri otomatik birleştirmez; çakışmaları raporlayarak durur.

## Non-goals

- Contributor biyografisi, portresi, sosyal bağlantıları veya public profil sayfası eklemek.
- Çevirmen, editör veya mevcut dört rol dışında yeni rol tanımlamak.
- Mobil/public contributor API sözleşmelerini değiştirmek.
- İlk teslimde bağımsız contributor detay rotası oluşturmak.

## Success signal

Editör içerik detayında “Yazar ekle” eylemini başlatır, yalnızca yazar rolüne uygun kişilerde arama yapar; kişi yoksa adını girerek oluşturur ve aynı akışta hikâyeye atar. Dil kapsamı ve sıra varsayılan olarak doğru gelir, ham sıra numarası girilmez ve editör contributor registry sayfasına gitmek zorunda kalmaz.

## Assumptions

- Profil rolleri contributorın uygunluk/keşif bilgisidir; içerikteki gerçek kredi assignment üzerinde kalır.
- İlk teslimde profil yönetimi için registry dialogları ve içerik detayındaki kontroller yeterlidir.
