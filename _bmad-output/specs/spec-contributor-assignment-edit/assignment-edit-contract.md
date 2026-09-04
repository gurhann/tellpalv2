# Assignment düzenleme kontratı

Bu companion, Story 8 implementasyonunda karar verilmesi gereken alanları ve hata davranışlarını taşır.

## Düzenlenebilir alanlar

- `creditName`: boş veya yalnızca boşluk ise `null`; trim edilmiş kredi adı.
- `role`: contributor profilinde bulunması zorunlu olan katalog değeri.
- `languageCode`: `null` tüm dilleri, geçerli kod ise tek içerik dilini temsil eder.
- `contributorId`: ilk taslakta salt okunur kimlik; değişim gerekiyorsa ayrı bir seçim ve duplicate kontrolü gerekir.

## Backend davranışı

- Güncelleme tek transaction içinde mevcut assignmentı yükler, yeni rol/kapsam uygunluğunu doğrular ve hedef grubun duplicate kuralını uygular.
- Grup değişiminde `sortOrder` istemciden alınmaz; hedef grubun sırası backend tarafından normalize edilir.
- Başarılı response güncel assignmentı ve gerekirse etkilenen eski/yeni grupların sıralı kayıtlarını taşır.
- Conflict, contributor rol uyumsuzluğu, geçersiz dil ve bulunamayan assignment mevcut Problem Details biçiminde döner.

## CMS akışı

1. Satırdaki düzenle eylemi mevcut assignment değerleriyle dialog açar.
2. Kaydetme sırasında alan doğrulaması yapılır ve düğme kilitlenir.
3. Başarıda assignment sorgusu yenilenir; panel doğru rol/dil grubunu gösterir.
4. Hata halinde dialog açık kalır, inline hata gösterilir ve kullanıcı tekrar deneyebilir.

## Karar bekleyen noktalar

Endpoint kimliği, contributor değişimi ve hedef gruba yerleştirme sırası netleşmeden frontend formu veya backend commandı uygulanmamalıdır.
