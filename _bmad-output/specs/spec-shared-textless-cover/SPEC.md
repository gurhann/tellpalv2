---
id: SPEC-shared-textless-cover
companions: []
sources: []
---

> **Canonical contract.** This SPEC is the complete, preservation-validated contract for what to build, test, and validate.

# İçerik Türleri İçin Ortak Textless Kapak

## Why

Sesli hikâye, ninni ve meditasyon kullanıcıya metinsiz, tek bir kapakla sunulur. Ses dosyasının dil bazlı veya ortak olması kapak sahipliğini değiştirmez. Localization seviyesindeki kapak referansları bu ortak görseli tekrarlamaya ve diller arasında ayrıştırmaya zorlar.

## Capabilities

- **CAP-1**
  - **intent:** Editör bir içeriğin textless kapağını content düzeyinde tek kez yönetir.
  - **success:** Kapak değiştiğinde hedeflenen tüm dil ve deneyim projeksiyonları aynı yeni `IMAGE` asset'ini kullanır.

- **CAP-2**
  - **intent:** Mobil istemci sesli hikâye, ninni ve meditasyon için ortak textless kapağı seçilen locale'dan bağımsız alır.
  - **success:** Aynı içeriğin farklı dil response'ları aynı cover asset kimliğini döner; yalnızca dile bağlı editoryal alanlar veya ses farklılaşabilir.

- **CAP-3**
  - **intent:** CMS, ortak kapak yönetimini localization editörlerinden ayırır.
  - **success:** `LULLABY` ve `MEDITATION` localization formlarında kapak alanı bulunmaz; kapak yalnızca content-level editörde düzenlenir.

## Constraints

- Textless kapak `Content.textlessCoverMediaId` üzerinden tek `IMAGE` asset referansıdır ve localization'lara kopyalanmaz.
- Ses kapsamı kapak kapsamını belirlemez: STORY narration ve MEDITATION sesi localization'da, LULLABY sesi content düzeyinde olabilir.
- Kapaksız veya `IMAGE` olmayan asset ile ortak-kapak gerektiren mobil deneyim yayınlanamaz.
- STORY `READING` deneyimi, localization'a ait `ContentLocalization.coverMediaId` kullanır; bu kapak hikâye adını içerdiğinden dile göre değişebilir.

## Non-goals

- Ses dosyalarının sahipliğini değiştirmek.
- Kapak üzerine dile özgü metin eklemek.
- STORY `READING` kapağını textless ortak kapakla değiştirmek.

## Success signal

- Sesli STORY, LULLABY ve MEDITATION kartları her dilde tek, textless content-level kapağı kullanır; editör bu kapağı bir kez yönetir.

## Assumptions

- Meditasyon ve ninni textless kapakları tüm desteklenen dillerde görsel olarak güvenle yeniden kullanılabilir.
