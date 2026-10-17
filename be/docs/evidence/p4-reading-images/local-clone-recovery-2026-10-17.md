# Yerel klon adımına bir kez devam önerisi

Durum: **Kullanıcı onayıyla uygulandı; Java bağlantı izninde durdu.**

Kullanıcının “Onaylıyorum” yanıtından sonra aşağıdaki tek devam uygulandı.
PostgreSQL18 araçlarıyla klon oluşturma ve restore başarılı oldu. İlk Java süreci,
Postgres.app bağlantı iznini doğrulayamadığı için migration ve fixture başlamadan
kapandı. [Devam makbuzu](execution-resume-1-2026-10-17.json) bu sonucu kaydeder.
İlk hata ve bu devamın özel makbuzları korunuyor. Java yeniden başlatılmadı.

Sonraki ayrı izin onayıyla Java 25.0.2 için Postgres.app `Allow` ayarı uygulandı ve
UI üzerinden doğrulandı. Genel izin koruması açık kaldı. Bu izin onayı denemeyi yeniden
başlatmayı kapsamıyordu; başarısız yürütme ve ledger korundu.

İlk yürütme PostgreSQL14 dump istemcisi ile18 sunucu uyumsuzluğunda durdu.
[Başarısızlık makbuzu](execution-attempt-2026-10-17.json) ve ilk attempt korunuyor.
Klon DB, backend, storage ledger veya yeni görsel oluşmadı. Mevcut Postgres.app18
araçları gerçek salt okunur schema kontrolünden geçti.

## Somut kapsam

Aynı run/prefix, aynı üç kaynak hash'i,823×1334 hedef/822×1334 PNG, tüm ilk plan
tavanları ve tek worker korunur. Normal uygulama, kaynak DB ve legacy112 korunur.
Öneri yalnız başarısız yerel klon adımını bir kez tamamlayıp ilk Java yürütmesini başlatmaktır.
Yeni profil, yeni bulut prefix'i veya ikinci login önerilmez.

1. Kaynak/hash, credential süresi/mührü ve klon yokluğu yeniden salt okunur doğrulanır.
   Dört PostgreSQL aracı `/Applications/Postgres.app/Contents/Versions/18/bin`
   üzerinden aynı major18 olarak seçilir.
2. Ek insan onayı, ilk attempt'i değiştirmeyen ayrı ve exclusive `resume-1.approval.json`
   makbuzuna yazılır. İlk boş dump aynı byte/hash ile ayrı başarısızlık dosyası olarak korunur.
3. Kaynak salt okunur yeni dump'a alınır. Yalnız sabit yeni yerel klon oluşturulup restore edilir.
   Klon halen yok ve kaynak V36 ise bu adım uygulanır.
4. Mevcut mühürlü input/config ile `P4LocalComparisonSetup` ilk kez başlatılır.
   `ledger.log` hâlâ yoktur; Java'nın CREATE_NEW koruması ve tüm çağrı tavanları geçerlidir.
5. Otomatik LIVE ve yalıtım kanıtı geçerse hazırlanmış ayrı emülatör uygulaması altı durumu ölçer.
   Sonuç, byte/decode/bellek/ekran ve lifecycle kanıtıyla raporlanır.

Her aşama tek seferliktir; yeni bir hata yeni karar ister. İlk login'in süresi veya mührü
geçersizleşirse bu öneri kendi başına ikinci login yetkisi vermez. Token/UI/log/repoya taşınmaz.
Bu plan üretime dağıtım, genel tüketici geçişi, backfill veya silme kapsamı içermez.
