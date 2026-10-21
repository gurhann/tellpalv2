---
title: 'P5a — Canlı hikâyelerin salt okunur envanteri'
type: 'chore'
created: '2026-10-21'
status: 'done'
baseline_commit: '0c32d7a0f54244bb0ac3eb66036bec77008a156b'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="Kullanıcı 2026-10-21'de normal SELECT sonuçlarını CSV olarak paylaşmayı seçti">

## Amaç ve sınırlar

Canlı STORY kataloğunu değiştirmeden envanter çıkar. Kullanıcı sorguları kendi
DataGrip bağlantısında çalıştırıp CSV dosyalarını paylaşır; ajan canlıya bağlanmaz
ve credential istemez. Yeni tercih: büyük JSON hücresi yerine normal sütun ve
satırlardan oluşan SELECT sonuçları.

- Dört sabit SELECT: hikâye/dil/kapak; sayfa/dil/görsel; yalnız bu kaynakların
  referans verdiği medya; STORY legacy iş durumları. Pasif/taslak/arşiv kayıtları
  korunur; eksik dil veya görsel başka dilden doldurulmaz.
- Sorgular uygulama verisi, dosya, job veya kalıcı/oturum ayarı değiştirmez.
  INSERT/UPDATE/DELETE/DDL, migration, Publish/retry, medya üretme/kopyalama/silme,
  pointer/erişim değişimi, storage taraması ve deploy yoktur.
- Metin/ses gövdesi, cached URL, token/lease, hata mesajı ve kullanıcı/admin/satın
  alma verisi dışa aktarılmaz. Bilinmeyen boyut/profil/hash tahmin edilmez.
- Her CSV'de sorgu zamanı bulunur. Ayrı SELECT'ler aynı atomik snapshot sayılmaz;
  kapsam bağlantı rolünün görebildiği kayıtlardır. Eski JSON çıktılarının işlem
  modu kabulü bunlara atfedilmez; RLS/rol/yetki değiştirilmez.
- Analiz metadata envanteridir: storage doğrulaması veya yürütülebilir geçiş
  planı değildir. Referanslı medya, bütün tarihsel türevlerin kataloğu sayılmaz.
  Legacy COMPLETED yeni READY/LIVE kabulü sağlamaz.

| Durum | Beklenen |
|---|---|
| Dil/sayfa çevirisi yok | LEFT JOIN null satırı korur |
| Ortak asset | CSV bağları korunur; medya kaydı tek kez alınır |
| Boş sonuç | Başlıklı sıfır satırlı CSV; yokluk belirtilir |
| Eksik sütun/yetki | Hata paylaşılır; sorgu uydurulmuş alanla sürdürülmez |
| Sorgular arası bağ farkı | Zaman/kapsam farkı incelenir; doğrudan veri bozukluğu ilan edilmez |

</frozen-after-approval>

## Kod haritası

- `ops/railway/README.md`: production hedefi; paylaşılan bucket, prefix ayrımı. Deploy script'i çalıştırılmaz.
- `be/src/main/resources/db/migration/V4__create_media_assets_table.sql`: byte/MIME/SHA nullable; genel width/height/version alanı yok.
- `be/src/main/resources/db/migration/V5__create_content_tables.sql` ve V15/V16/V18/V24: içerik/dil/sayfa, localized illustration ve ayrı kaynak/listening bağları.
- Aynı dizindeki V12/V22/V32/V34: legacy jobs ve ayrı page/listing manifestleri; V35/V36: reading jobs/work/manifests/admission/availability/publications. Yalnız ilgili STORY referansları okunur.
- `be/src/main/java/com/tellpal/v2/asset/api/ReadingImagePreparationApi.java:23`: exact-language rol/sıra kuralları.
- `be/docs/evidence/p5a-local-inventory-baseline/verification.md`: geçmiş yerel bulgu; canlı sonuç değildir.

## Görevler ve kabul

- [x] `tools/image-optimization/p5a_csv_inventory.sql`: yalnız SELECT ile dört normal sonuç tablosu.
- [x] `tools/image-optimization/P5A_CSV_INVENTORY.md`: dört CSV dosyası ve dışa aktarma adımları.
- [x] Dört SELECT'i geçici yerel PostgreSQL'de eksik bağ, ortak kaynak, metadata seçimi ve boş CSV senaryolarıyla doğrula.
- [x] Kullanıcının dört canlı CSV'sini okuyup kapsam/sayım/bağ ve eksik kanıt raporunu çıkar; tam katalog/atomic snapshot iddiasında bulunma.

## Değişiklik kaydı

- 2026-10-21: Yerel örnek aracı yerine canlı salt okunur envanter istendi.
- 2026-10-21: Kullanıcı SQL'i kendi çalıştırmayı seçti; otomatik bağlantı aracı kaldırıldı.
- 2026-10-21: JSON çıktıları yanlış işlem modu nedeniyle ayrıntı vermedi. v2 explicit SET TRANSACTION ve DataGrip rehberi yerelde sınandı.
- 2026-10-21: Kullanıcı normal SELECT ve CSV istedi. Dört SELECT'e geçildi; önceki JSON sürümleri ve kanıtları tarihsel olarak korunur. Tek-snapshot/işlem modu kabulü yeni çıktılarda varsayılmaz.

## Doğrulama

[Yerel doğrulama kaydı](../../be/docs/evidence/p5a-production-inventory/verification.md)
önceki JSON denemeleri ve CSV sorgu testlerini ayrı tutar. Dört kullanıcı CSV'sinin
[metadata raporu](../../be/docs/evidence/p5a-production-inventory/findings.md) ve
[sayısal analizi](../../be/docs/evidence/p5a-production-inventory/csv-inventory-analysis.json)
hazırdır. P4b/üretim kalite ve erişim kapıları açıktır; hiçbir geçiş çalıştırılmadı.

## Önerilen inceleme sırası

- Kapsam, eksik kapak ve gerçek geçiş sınırları.
  [findings.md:1](../../be/docs/evidence/p5a-production-inventory/findings.md#L1)
- CSV sütunları, rol bağları ve canlıya yazmayan sorgular.
  [p5a_csv_inventory.sql:1](../../tools/image-optimization/p5a_csv_inventory.sql#L1)
- Kaynak hash'leri, dil/rol sayımları ve içerik/dil ayrıntıları.
  [csv-inventory-analysis.json:1](../../be/docs/evidence/p5a-production-inventory/csv-inventory-analysis.json#L1)
- Yerel sorgu testleri, bağımsız analiz ve üç inceleme.
  [verification.md:1](../../be/docs/evidence/p5a-production-inventory/verification.md#L1)
