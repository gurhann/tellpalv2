---
title: 'Contributor atama deneyimini sağlamlaştırma'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 0
baseline_commit: '3e5a87a0a228c50f0e3c0497bcf9d73307ca634e'
context:
  - 'AGENTS.md'
  - 'cms/AGENTS.md'
  - 'cms/docs/ui-standards.md'
  - 'be/docs/project-memory.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contributor atama düzenleyicisi, bir profilin sahip olmadığı rolün seçilmesine izin veriyor; hata metinleri Türkçe arayüzde İngilizce görünebiliyor. Rol-kapsamlı aramada mevcut ancak istenen rolü olmayan bir profil, yinelenen ad uyarısından sonra çözümsüz kalıyor; uzun sonuç listeleri de seçimden sonraki ana eylemi görünmez kılıyor.

**Approach:** Atama okuma/yazma sözleşmesine profil rollerinin anlık görüntüsünü ekleyip düzenleme seçeneklerini bununla sınırlandıracağız. Sunucu, atama alanı kuralları için kararlı hata kodları döndürecek; CMS bunları TR/EN mesajlara çözecek. Kayıt defterinde bulunan fakat rolü eksik profil için editöre açık, onaylı bir “role ekle ve ata” yolu verilecek; picker sonucu kendi sınırlı kaydırma alanına alınarak seçili eylemler görünür tutulacak.

## Boundaries & Constraints

**Always:** Mevcut `3e5a87a0a228c50f0e3c0497bcf9d73307ca634e` davranışını koru; backend değişikliklerini `content` modülünde tut; API hata kararlarını `errorCode` üzerinden yap; kullanıcıya görünen yeni metinleri TR ve EN ekle; profil rol güncellemesinde mevcut rolleri koru; dialogda seçili rol eski/eksik bir yanıtta gelirse görünür kalmalı fakat yeni geçersiz rol seçilememeli; 390/768/1280/1440 genişliklerinde yatay taşma olmamalı.

**Ask First:** Mevcut profilde yeni rol eklemenin kullanıcı tarafından açıkça onaylanmadığı bir otomasyon gerekirse; atama API sözleşmesini admin CMS dışındaki tüketicileri kıracak biçimde değiştirmek gerekirse; mevcut profil verisini birleştirmek yerine üzerine yazmak gerekirse.

**Never:** Rol bilgisini isim eşleşmesinden veya yalnızca mevcut atamadan tahmin etme; sunucunun `detail` metnini kontrol akışının kaynağı yapma; 409 sonrasında görünmeyen bir sonucu seçilmiş gibi gösterme; geniş bir ortak dialog primitive yeniden tasarımı yapma; kullanıcı değişikliklerini geri alma ya da Contributor Story 1–8 kapsamını değiştirme.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Geçerli rol düzenleme | Atama yanıtında profil rolleri `YAZAR` içerir | Düzenleme listesinden yalnız bu roller seçilir ve kayıt başarılı olur | Bilinmeyen hata genel, yerelleştirilmiş geri dönüş verir |
| Geçersiz rol düzenleme | Profilde `MÜZİSYEN` yoktur | Rol seçeneklerinde görünmez; eski/stale atanmış rol yalnız okunabilir bağlamda korunur | Sunucu yine reddederse `contributor_role_not_supported` yerelleştirilir |
| Mevcut profil, rol eksik | Rol-kapsamlı arama boş, oluşturma 409 döndürür | Profil özeti ve açık “rolü ekle ve ata” onayı gösterilir; var olan roller birleşir, sonra atama yapılır | İptal hiçbir profil/atama değişikliği yapmaz; güncelleme hatası yerelleştirilir |
| Uzun picker sonucu | 25+ rol uygun kayıt | Sonuçlar sınırlı, kaydırılabilir bölgede kalır; seçim sonrası kapsam/kredi/kaydet eylemi görünürdür | Klavye ve ekran okuyucu rolleri korunur |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- atama alanı kurallarının sahibi.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementResults.java` ve `ContentManagementMapper.java` -- atama yanıtına `contributorRoles` anlık görüntüsü taşır.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java` -- alan hatalarını kararlı admin problem kodlarına eşler.
- `cms/src/features/contributors/api/contributor-admin.ts` ve `model/contributor-view-model.ts` -- genişletilmiş sözleşmeyi doğrular ve UI modeline dönüştürür.
- `cms/src/features/contributors/components/edit-content-contributor-dialog.tsx` -- güvenli rol seçimi ve hata sunumu.
- `cms/src/features/contributors/components/assign-contributor-dialog.tsx` -- eksik rol çözümü ve uzun sonuç düzeni.
- `cms/src/components/feedback/problem-alert.tsx` ile contributor hata çözümleyicisi -- `errorCode` temelli yerelleştirme.
- `cms/src/i18n/messages.ts`, `cms/docs/ui-standards.md`, `be/docs/project-memory.md` -- metin ve kalıcı çalışma kararları.

## Tasks & Acceptance

**Execution:**
- [x] Content atama sonucu ve admin DTO’larına `contributorRoles` ekle; GET/POST/PUT yanıtlarının aynı profil anlık görüntüsünü vermesini sağla.
- [x] `Content` içindeki desteklenmeyen rol, içerikte olmayan dil kapsamı ve yinelenen atama ihlallerini anlamlı domain hatalarına dönüştür; handler’da sırasıyla 400/400/409 ve kararlı `errorCode` üret.
- [x] CMS şema/modelini genişlet; düzenleme dialogunda seçenekleri `contributorRoles` ile filtrele ve eski yanıtların seçili değerini güvenli biçimde göster.
- [x] Contributor için ortak hata çözümleyicisi ve TR/EN mesajları ekle; bilinen kodlarda ham İngilizce `detail` metnini gösterme, bilinmeyen problemler için mevcut geri dönüşü koru.
- [x] 409 mevcut profil/rol-eksik durumunu profil özetinden ayırt et; kullanıcı açıkça onaylarsa mevcut rolleri birleştirip profili güncelle, ardından atamayı yap. Başarısız veya iptal akışında sahte seçim üretme.
- [x] Picker sonuçlarını sınırlı kaydırma alanına taşı; seçilen kayıt formu ve ana eylem sonuç listesi altında erişilebilir kalacak şekilde düzenle. Bu davranışı `cms/docs/ui-standards.md` içinde kalıcı picker kuralı olarak kaydet.
- [x] `be/docs/project-memory.md` içine kalıcı ekip varsayımını ekle: küçük, izole düzeltmeler dışında planlama, uygulama, doğrulama ve inceleme için uygun BMad becerisi kullanılır.
- [x] Backend controller/integration testlerini; CMS şema, dialog ve hata yerelleştirme testlerini ekle veya güncelle. Uzun liste etkileşimi için component ve Playwright genişlik regresyonu ekle.

**Acceptance Criteria:**
- Given profil yalnızca `YAZAR` rolüne sahip, when editör atamayı düzenler, then `MÜZİSYEN` seçeneği sunulmaz ve geçersiz istek yapılamaz.
- Given backend atama kuralını reddeder, when CMS dili Türkçe veya İngilizce, then kullanıcı kararlı koda karşılık gelen yerelleştirilmiş açıklamayı görür.
- Given aynı isimde profil bulunur fakat seçilen rol yoktur, when editör açıkça onaylar, then profilin eski rolleri korunarak seçilen rol eklenir ve atama oluşur.
- Given picker 25 veya daha fazla sonuç gösterir, when editör bir sonuç seçer, then kapsam/kredi alanları ve kaydet eylemi aynı dialog içinde erişilebilir kalır.

## Spec Change Log

## Design Notes

Atama yanıtındaki `contributorRoles`, düzenleme zamanında ek sorgu ve isim eşleşmesi gerektirmeyen tutarlı bir sunucusal anlık görüntüdür. `role`, atamanın rolünü; `contributorRoles` ise profil yetkinliklerini ifade eder. Profil güncellemesi replace-semantics taşıdığı için istemci, yalnız yeni rolü değil mevcut roller ile birleşmiş tam kümeyi gönderir.

## Verification

**Commands:**
- `cd be && ./mvnw test` -- content admin sözleşmesi, hata kodları ve rol birleştirme testleri başarılı.
- `cd cms && .\\node_modules\\.bin\\tsc.cmd -b` -- CMS tür kontrolü başarılı.
- `cd cms && .\\node_modules\\.bin\\vitest.cmd run` -- contributor dialog, şema ve yerelleştirme regresyonları başarılı.
- `cd cms && npm run test:e2e` -- contributor akışındaki responsive/etkileşim koruması başarılı.
- `cd cms && npm run build` -- üretim derlemesi başarılı.

**Manual checks (if no CLI):**
- Yerel CMS’te Türkçe ve İngilizce ile mevcut-rol-eksik senaryosunu, geçersiz düzenleme seçeneğini ve uzun sonuç seçimini 390/768/1280/1440 genişliklerinde doğrula.

## Suggested Review Order

**Domain validation and API contract**

- Atama kuralları, profil rolü ve dil kapsamı doğrulamasını tek aggregate içinde uygular.
  [`Content.java:269`](../../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L269)

- Hata türlerini admin API’de kararlı kodlara ve alan özelliklerine dönüştürür.
  [`ContentAdminExceptionHandler.java:148`](../../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java#L148)

- Profil rol snapshot’ını atama response’larına deterministik biçimde taşır.
  [`ContentManagementMapper.java:64`](../../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementMapper.java#L64)

**CMS resolution and editor behavior**

- Rol eksik duplicate profili ID ile çözümler ve açık onaydan sonra atamayı sürdürür.
  [`assign-contributor-dialog.tsx:141`](../../../cms/src/features/contributors/components/assign-contributor-dialog.tsx#L141)

- Editör seçeneklerini profil snapshot’ına göre sınırlar ve stale değeri korur.
  [`edit-content-contributor-dialog.tsx:76`](../../../cms/src/features/contributors/components/edit-content-contributor-dialog.tsx#L76)

- Bilinen backend kodlarını TR/EN metinlere çevirirken bilinmeyen fallback’i korur.
  [`contributor-problems.ts:10`](../../../cms/src/features/contributors/lib/contributor-problems.ts#L10)

- Sonuç listesini sınırlı kaydırma alanında tutarak form ve ana eylemi erişilebilir bırakır.
  [`assign-contributor-dialog.tsx:379`](../../../cms/src/features/contributors/components/assign-contributor-dialog.tsx#L379)

**Verification and durable defaults**

- Yeni picker davranışının genişlik ve klavye regresyonlarını güvenceye alır.
  [`assign-contributor-dialog.test.tsx:111`](../../../cms/src/features/contributors/components/assign-contributor-dialog.test.tsx#L111)

- BMad becerilerinin kalıcı ekip çalışma varsayımını kaydeder.
  [`project-memory.md:49`](../../../be/docs/project-memory.md#L49)

