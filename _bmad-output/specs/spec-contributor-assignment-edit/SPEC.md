---
id: SPEC-contributor-assignment-edit
companions:
  - assignment-edit-contract.md
  - ../spec-contributor-workflow/SPEC.md
  - ../spec-contributor-workflow/contributor-model-and-workflows.md
sources: []
---

> **Canonical contract.** Bu SPEC ve companions dosyaları, içerik contributor assignment düzenleme akışının uygulanabilir sözleşmesidir.

# İçerik contributor assignment düzenleme

## Why

Story 7 ile contributor kayıtları içerik detayında rol ve dil grupları altında görünür hale geldi; ancak mevcut assignment üzerinde kredi adı veya kapsam değişikliği yapılamıyor. Editör küçük bir düzeltme için atamayı kaldırıp yeniden oluşturmak zorunda kalıyor ve bu sırayı, bağlamı ve hata görünürlüğünü zayıflatıyor.

## Capabilities

- **CAP-1**
  - **intent:** Editör, içerik detayında mevcut contributor assignmentını düzenleyerek geçerli kredi bilgilerini güncelleyebilir.
  - **success:** Düzenleme formu mevcut değerlerle açılır; geçerli kaydetme sonrası aynı assignment güncel değerlerle görünür ve grup kuralları korunur.

- **CAP-2**
  - **intent:** Sistem, assignment düzenlemelerinde contributor rolü, dil kapsamı ve hedef grup bütünlüğünü korur.
  - **success:** Profilde bulunmayan rol, geçersiz dil kapsamı veya hedef grupta duplicate assignment isteği kalıcılaşmaz; kullanıcıya açıklayıcı hata gösterilir.

- **CAP-3**
  - **intent:** Editör, assignment düzenleme başarısız olduğunda mevcut veriyi kaybetmeden işlemi yeniden deneyebilir.
  - **success:** API hatasında önceki görünüm korunur, hata inline gösterilir ve form yeniden denemeye hazır kalır.

## Constraints

- Assignment verisi ve doğrulama mevcut `content` modülünde kalır; yeni modül sınırı veya internal çapraz bağımlılık oluşturulmaz.
- `assignmentId` kalıcı kimlik olarak korunur; istemci `sortOrder` hesaplamaz veya doğrudan yazmaz.
- Rol veya dil kapsamı değişirse hedef grubun sırası ve duplicate kontrolü transaction içinde backend tarafından belirlenir.
- Düzenleme endpoint’i admin yetkilendirmesi ve mevcut Problem Details hata biçimiyle belgelenir.
- CMS görünür metinleri Türkçe ve İngilizce i18n anahtarlarından üretir; mobil/public API davranışı değişmez.

## Non-goals

- Contributor profilinin görünen adını veya profil rollerini bu akıştan değiştirmek.
- Yeni contributor rolü eklemek.
- Sürükle-bırak kütüphanesi veya bağımsız contributor detay rotası eklemek.
- Public/mobile assignment API sözleşmesini değiştirmek.

## Success signal

Editör içerik detayında bir contributor satırında düzenlemeyi seçer, mevcut kredi adı ve kapsamı korunarak formu kaydeder; assignment aynı kimlikle güncellenir, hedef grup sırası backend kurallarına göre görünür ve hata durumunda veri kaybı yaşanmaz.

## Assumptions

- İlk taslakta contributor kimliği değiştirilemez; contributor değiştirme işlemi kaldırma ve yeni atama olarak kalır.
- Rol ve dil kapsamı değişiklikleri assignmentı hedef gruba taşır ve hedef grubun sonuna ekleme varsayımıyla tasarlanır; bu karar kullanıcı onayı bekler.

## Open Questions

- Düzenleme contributor kimliğinin değiştirilmesine izin verecek mi, yoksa yalnızca kredi adı, rol ve dil kapsamını mı kapsayacak?
- Rol veya dil kapsamı değişince assignment hedef grubun sonuna mı eklenmeli, yoksa mevcut sıra mümkünse korunmalı mı?
- Backend endpoint yolu `assignmentId` ile mi, yoksa contributor ve mevcut kapsam bileşimiyle mi tanımlanmalı?
