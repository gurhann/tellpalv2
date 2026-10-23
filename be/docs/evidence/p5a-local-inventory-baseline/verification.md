# P5a — Yerel envanter hazırlığı

Tarih: 2026-10-20. Kapsam: depodaki mevcut iki JSON kaydının salt okunur incelenmesi. Üretim kataloğu, veritabanı veya storage envanteri çıkarılmadı. Bu rapor bir geçiş dry-run'ı veya yeni doğrulama sonucu değildir.

## Mevcut girdiler

| Kayıt | İçerik | Kullanılabilir bilgi | Kanıtın sınırı |
| --- | --- | --- | --- |
| [P3 yerel sonuç](../p3-local-cms/live-result.json) | #93, `tr` | `INTERNAL_LOCAL_TEST`; tarihsel LIVE generation 2; cover/detail/page için üç 40×30 PNG çıktı kaydı | 80×60 sentetik kaynak demosudur. Gerçek hikâye, güncel storage doğrulaması veya üretim kalite onayı değildir. |
| [P4 retained fixture](../../../src/test/resources/p4/consumer-retained-ready.json) | #94, `tr` | Generation 4; revision/manifest kimlikleri; cover/detail ve sıralı iki sayfa bağı; kaynak/çıktı raster kimlikleri; her bağ için bir rendition | Test fixture'ıdır. Güncel yayın, erişim veya saklama uygunluğunu kanıtlamaz. Profil satırları fingerprint/geometri içerir; tam profil descriptor'ı içermez. |

#94 fixture'ında cover ve detail aynı asset/localization kimliğini kullanır. Sayfa numaraları 1 ve 2'dir. Kaynak ve çıktı kayıtları provider, object key, generation, MIME, byte, SHA ve boyut alanlarını içerir. Bu alanların bulunması, nesnelerin şu anda mevcut veya doğrulanmış olduğu anlamına gelmez. Dört rol bağı dört farklı kaynak dosya demek değildir.

Bu iki kayıt ürün kataloğunu temsil etmez. Eski yürütücü PID, port, run ve pencere alanları yeni işlem yetkisi olarak kullanılmaz.

## Sonraki dar iş

P5a için yalnız açıkça verilen yerel girdiyi okuyacak bir envanter/dry-run aracı planlanabilir. İlk sözleşmesi şu ayrımı korumalıdır:

- İçerik, dil, revision, rol ve sıralı sayfa bağları.
- Kaynağın beyan edilen kimliği ile bağımsız doğrulama kanıtı.
- Profil fingerprint'i ile tam descriptor ve kullanım onayı.
- Legacy iş durumu ile yeni sözleşmenin readiness kanıtı.
- Hesaplanabilen plan ile eksik bilgi yüzünden engellenen plan.

Eksik kaynak, profil veya bağ bilgisi engel olarak raporlanır. Sayısal profil, çıktı byte/SHA, storage yolu veya READY/LIVE sonucu uydurulmaz. Legacy `COMPLETED` yeni sözleşme kabulü sayılmaz. Kaynak dosyalar, pointer'lar ve mevcut teslimat değiştirilmez.

Bu turda araç uygulanmadı veya dry-run çalıştırılmadı. Uygulama öncesinde girdi/çıktı sözleşmesi ve anlamlı yerel testleri içeren somut BMad planı hazırlanmalıdır.

## Açık kapılar

Gerçek geçişten önce erişim/abonelik ve yayın uygunluğu, medya teslimi/URL yenileme/HTTP cache, session/retention, üretim profili ve kalite kabulü netleşmelidir. P4b'nin gerçek detail/reader teslimatı, yaşam döngüsü ve kaynak kullanımı ölçümü açık kalır. Android/iPhone örnekleri genel profil onayı değildir; P0 ham kanıt sınırlaması korunur.

Bu açık kapılar yerel planlamayı durdurmaz; gerçek cohort/backfill, pointer değişimi ve dağıtım kabulünü sınırlar. Bu rapor bunların hiçbirine yetki vermez.

## Yapılan kontrol

Yalnız iki depodaki JSON kaydının alanları ve rol bağları okundu. Medya dosyası indirilmedi/açılmadı; credential okunmadı; backend, uygulama, cihaz, giriş, ölçüm veya bulut işlemi başlatılmadı. Yeni üretim, dosya silme ve süreç durdurma yapılmadı. Önceki kayıtlar ve sınırlar korunur.

P4b5 yürütme kapanışı ve teknik borç: [devir notu](../image-optimization-handoff-2026-10-20.md).

## 2026-10-21 — Kapsam değişikliği

Kullanıcı yerel örnek aracı yerine canlı hikâyelerin salt okunur envanterini istedi. [Güncel P5a taslağı](../../../../_bmad-output/implementation-artifacts/spec-p5a-production-readonly-inventory.md) bu önerinin yerini alır. Yukarıdaki iki kayıt tarihsel yerel bulgu olarak korunur; canlı katalog veya güncel storage kanıtı değildir. Bu belge güncellemesinde canlı bağlantı açılmadı; araç uygulanmadı ve envanter çalıştırılmadı.
