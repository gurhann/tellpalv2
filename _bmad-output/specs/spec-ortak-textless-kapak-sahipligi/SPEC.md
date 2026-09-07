---
id: SPEC-ortak-textless-kapak-sahipligi
companions:
  - cover-ownership.md
sources: []
---

> **Canonical contract.** This SPEC and its companions are the complete, preservation-validated contract for this change.

# Kaynak ve Dinleme Kapaklarının Ayrı Sahipliği

## Why

Bir `STORY`nin iki farklı textless görsel ihtiyacı vardır: çeviride başlık eklenebilmesi için normal hikâye kapağının yazısız kaynak sürümü ve sesli hikâye deneyiminde kullanılan bağımsız kapak. Bu ikisini tek alan saymak, çeviri kaynaklarını sesli deneyimle karıştırır. Ninni ve meditasyonun da tüm dillerde ortak olan dinleme kapağı, content scope'ta ayrı sahiplenilmelidir.

## Capabilities

- **CAP-1**
  - **intent:** Editör, normal hikâye kapağının yazısız kaynak sürümünü sesli deneyim kapağından bağımsız biçimde yönetebilir.
  - **success:** `STORY` üzerindeki mevcut kaynak kapak referansı, localization'daki başlıklı okuma kapağından ve dinleme kapağından ayrı kalır; kaynak kapağın değişmesi diğer iki referansı değiştirmez.

- **CAP-2**
  - **intent:** Editör, `STORY`nin sesli deneyimi ile `MEDITATION` ve `LULLABY` için tek, ortak bir dinleme kapağı seçebilir veya kaldırabilir.
  - **success:** Her desteklenen canonical içerikte en fazla bir pozitif `IMAGE` dinleme kapağı kaydedilir; yanlış asset türü reddedilir ve aynı içeriğin tüm dilleri aynı referansı görür.

- **CAP-3**
  - **intent:** CMS ve admin okumaları, kapakların sahiplik seviyesini açıkça ayırarak doğru editörü ve doğru projeksiyonu sunar.
  - **success:** Dinleme kapağı yalnızca content-level editörde görünür; `STORY` localization'ındaki başlıklı kapak aynen korunur ve ortak dinleme kapağının değişmesi localization alanlarını, anlatımı, sayfa seslerini veya yayın durumunu değiştirmez.

## Constraints

- `Content.textlessCoverMediaId`, yalnızca `STORY`nin çeviri/illüstrasyon kaynağı olan yazısız normal kapak sürümüdür; sesli deneyim kapağı olarak yeniden yorumlanamaz.
- Dinleme kapağı için ayrı bir content-level asset referansı oluşturulur. Bu referans `STORY` anlatımı, `MEDITATION` ve `LULLABY` için kullanılabilir; localization-scoped değildir.
- Her iki content-level referans ve `ContentLocalization.coverMediaId` ayrı pozitif `IMAGE` asset referanslarıdır. Content modülü URL, varyant veya işleme çıktısı saklamaz.
- Dinleme kapağı isteğe bağlıdır. Yokluğu mevcut yayınlanabilirliği değiştirmez ve otomatik asset processing başlatmaz.

## Non-goals

- Canonical `AUDIO_STORY` türünü kaldırmak, eski bağımsız sesli hikâyeleri import etmek veya mobile/public discovery sözleşmesini değiştirmek.
- Ninninin ortak playback'i, müzisyen/enstrüman verisi veya playback processing'ini eklemek.
- Normal `STORY` localization kapağını content scope'a taşımak, kaynak kapağı kaldırmak veya sayfa görsellerini değiştirmek.

## Success signal

Bir editör, bir hikâyenin kaynak kapağını ve sesli hikâye kapağını ayrı asset'ler olarak yönetebilir. Sesli deneyim kapağı hangi dilde açılırsa açılsın aynı görünür; normal hikâyenin dil bazlı başlıklı kapağı ve kaynak kapağı değişmeden kalır.

## Assumptions

- Dinleme kapağı için yeni content-level referansın teknik alan adı uygulama tasarımında seçilecektir; bu alan mevcut `textlessCoverMediaId` ile aynı değildir.
