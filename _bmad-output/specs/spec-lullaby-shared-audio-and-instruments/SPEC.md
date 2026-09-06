---
id: SPEC-lullaby-shared-audio-and-instruments
companions:
  - lullaby-data-and-api-contract.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate.

# Paylaşımlı Ninni Ses Kaydı ve Enstrüman Kataloğu

## Why

Ninniler sözsüz enstrümantal eserlerdir: tüm dillerde aynı ses dosyası, kapak, süre, müzisyen bilgisi ve enstrümanlar kullanılır; yalnızca ninninin adı yerelleştirilir. Mevcut localization-merkezli ses modeli ortak veriyi tekrar etmeye zorlar ve mobildeki enstrüman bilgisini desteklemez.

## Capabilities

- **CAP-1**
  - **intent:** Editör bir ninninin ortak ses kaynağını, kapağını ve süresini içerik düzeyinde tek kez yönetir.
  - **success:** Ses veya kapak güncellendiğinde ninninin her dildeki mobil sunumu aynı yeni asset'i kullanır; localization'larda yinelenen asset bağları oluşmaz.

- **CAP-2**
  - **intent:** Editör ninniye enstrüman kataloğundan bir veya daha fazla enstrüman seçer ve görünme sırasını belirler.
  - **success:** “Piyano” ve “Glockenspiel” seçilmiş bir ninni, mobil response'ta aynı sırada iki katalog enstrümanı olarak döner; serbest metin veya tekrar eden seçim kaydedilemez.

- **CAP-3**
  - **intent:** Editör her dilde ninninin yalnızca görünen adını yönetir.
  - **success:** Yeni bir ninni localization'ı oluştururken ses, kapak, süre, müzisyen veya enstrüman alanları gösterilmez ya da istenmez; yalnızca geçerli başlık kaydedilir.

- **CAP-4**
  - **intent:** Mobil istemci seçilen dildeki ninni adıyla beraber ortak oynatılabilir sesi, müzisyen kredilerini ve sıralı enstrüman listesini alır.
  - **success:** Ekrandaki müzik, enstrüman, başlık ve oynatıcı verileri tek ninni kimliği için tutarlı döner; localization değişince yalnızca başlık değişir.

## Constraints

- `LULLABY` canonical içerik türü olarak kalır; ortak ses/kapak verisi localization'a taşınmaz veya kopyalanmaz.
- Enstrümanlar katalog referanslarıdır; ninni-enstrüman ilişkisi en az bir kayıt, benzersizlik ve kalıcı görüntüleme sırası garanti eder.
- `MUSICIAN` contributor kredisi mevcut content-level contributor modeliyle kalır; enstrüman bir kişi ya da contributor rolü değildir.
- Ninni localization'ı başlık dışında içerik veya medya alanı taşımaz.

## Non-goals

- Ninni sesini, kapağını veya enstrüman listesini dile göre değiştirmek.
- Serbest metin enstrüman girişi sağlamak.
- `STORY`, `MEDITATION` ya da sesli hikâyelerin ses sahipliğini bu çalışmada değiştirmek.
- Müzisyen contributor rolünü enstrüman listesiyle değiştirmek.

## Success signal

- CMS editörü ortak ninni üretim bilgisini bir kez, her dil için adı ise ayrı yönetir; mobil oyuncu hangi dili kullanırsa kullansın aynı ses/kapak/enstrümanlarla doğru yerelleştirilmiş adı gösterir.

## Assumptions

- Ninni kapağı metinsizdir ve tüm dillerde ortak kullanılabilir.
- Enstrüman kataloğu başlangıçta yönetilen referans veri olarak sağlanır; başlangıç Türkçe listesi Çelesta, Bell, Keman, Rhodes, Glockenspiel, Arp, Vibrafon ve Yaylı Orkestra'dır. Katalog yönetimi bu çalışmanın CMS yüzeyine dahil değildir.
- Enstrüman kataloğunun kararlı kimliği dil bağımsızdır; backend, görünen adı seçilen dile göre katalog localization'ından döner.
