# P3 yerel CMS denemesi

Bu araç tek Türkçe, tek sayfalı sentetik DRAFT STORY hazırlar. Mevcut 91 içerik ve medya bağları korunur. Gerçek Firebase SDK kullanılır. Üretim etkinleştirme, P4 tüketicileri ve kalite onayı bu denemenin kapsamında değildir.

Komutları depo kökünden Java 25 ile çalıştırın:

```bash
python3 be/tools/p3_local_cms.py
python3 be/tools/p3_local_cms.py --prepare
python3 be/tools/p3_local_cms.py --preflight
```

İlk komut kimlik bilgisi, ortam dosyası, ağ veya DB okumaz/yazmaz. `--prepare` yalnız test sınıflarını ve classpath'i mevcut Maven cache'iyle offline derler. Bağımlılıklar eksikse normal Maven hazırlığını ayrıca tamamlayın. `--preflight` gerçek web/security context'ini geçici loopback portunda açar; profil bağlamasını, JPA şemasını ve 91 mevcut kaydı salt okunur DB bağlantısıyla doğrular. Scheduler, admin bootstrap ve Flyway bu hazırlık context'inde kapalıdır. Bulut I/O yapılmaz ve ilk deneme kilidi tüketilmez. Java testi normal test keşfinde bulut işlemi çalıştırmaz.

Mevcut yerel backend sürecinin PID, komut ve port sahipliğini doğrulayın. Yalnız tanımlanan yerel süreci kontrollü durdurun. Araç kendiliğinden süreç durdurmaz; port doluysa hazırlık ve başlatmayı reddeder. Mevcut Chrome CMS oturumunu açık bırakın.

```bash
python3 be/tools/p3_local_cms.py --setup
python3 be/tools/p3_local_cms.py --start
```

Backend'in yönetilen komut oturumuyla birlikte açık kalması gerekiyorsa `python3 be/tools/p3_local_cms.py --start --foreground` kullanın. Bu seçenek çocuk JVM'nin bitmesini bekler; ayrı arka plan oturumu oluşturmaz. Uygulama çıktısı yine özel `backend.log` dosyasına yazılır. Sağlık kontrolünü başka komut oturumundan yapın; başlatma mesajı tek başına uygulamanın hazır olduğunu kanıtlamaz. Foreground modunda `backend.pid` yazılmaz; süreç sahipliği yönetilen komut oturumundan izlenir.

`--setup` yalnız onaylanan ilk bulut denemesi için bir kez çağrılır. `be/.env` ve mevcut süreç ortamı shell çalıştırmadan yüklenir; süreç ortamı önceliklidir. İstenirse `--env-file /özel/yol` ve `--java /jdk25/bin/java` belirtilebilir. Yerel DB, JWT ve CORS ayarları overlay'e taşınmaz; mevcut ayarlarla korunur. `local` profili açıkça etkinleştirilir. Uzak DB, üretim profili, farklı bucket, belirsiz Spring/JVM override'ları ve gerçek SDK yerine fake client reddedilir.

Etkin komutlarda varsayılan veya açıkça seçilen env dosyası bulunmak zorundadır; eksikse varsayılan ayarlara geçilmeden durulur. Format her satırda `KEY=value` veya `export KEY=value` biçimindedir. Yalnız tam satır `#` yorumları desteklenir. Değer içindeki `#` korunur: `KEY=abc#def`, `KEY='abc#def'` ve `KEY="abc # def"` geçerlidir. Satır sonu yorumlarını ayrı satıra taşıyın; `KEY=abc # yorum` belirsiz olduğu için reddedilir. Shell genişletmesi yapılmaz.

Özel dosyalar Git dışındaki `be/var/p3-local-cms/` altında tutulur. Dizin `0700`, makbuz/config/log dosyaları `0600` iznine sahiptir. `attempt.properties` ve `receipt.properties` tek kullanımlı korumadır. Hata olsa bile bu dosyaları silip yeniden denemeyin; yeni fixture/prefix veya başarısız kapsamı aşan deneme ek kullanıcı onayı gerektirir. Kısmi nesneler ve DB kayıtları korunur; cleanup yapılmaz. Kabul işlemi transaction dışında, seed işlemleri mevcut uygulama komutlarıyla ayrı transaction'larda çalışır. Seed sırasında otomatik Flyway değişikliği yapılmaz; mevcut yerel şemanın güncel olması gerekir.

`--setup`, tek kullanımlık işareti oluşturmadan önce `--preflight` kontrolünü tekrar çalıştırır. Başarısız preflight bulut yazma denemesi sayılmaz. Başarılı seed sonrasında aynı kontrolü yeniden çalıştırmayın; 91 kayıt/boş P3 önkoşulu artık geçerli değildir.

Setup makbuzu gerçek Spring `spring.datasource.url` değerini `databaseUrl` alanında saklar. Bu URL kullanıcı/parola/query içeremez; DB parolası ve JWT kaydedilmez. `--start`, seçilen yerel DB URL'si makbuzla birebir uyuşmuyorsa veya alan eksikse JVM başlatmadan durur. Başlatılan JVM de aynı URL'ye sabitlenir. Eski makbuzlar kendiliğinden güncellenmez; gerçek seed DB kimliği ayrıca doğrulanmadan eksik alan eklenmez.

Overlay yalnız P3 ve P2 test motorunu, üç açık sentetik rol profilini ve benzersiz `local/p3-cms-<UUID>` prefix'ini açar. Eski medya poller'ı kapalı tutulur; böylece seed'in legacy iş kayıtları ek bulut üretimi yapmaz. P3 scheduler ve gerçek encoder çalışır. İzlenen `application-local.yml` değişmez; overlay verilmeden normal başlatmada P3 kapalı kalır.

Spring ortam değişkenlerine overlay'den yüksek öncelik verir. Launcher yalnız preflight/setup/backend çocuk süreçlerinde `TELLPAL_ASSET_PROCESSING_POLLER_ENABLED=false` ve `TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX=<overlay UUID prefix'i>` değerlerini sabitler. Kaynak env dosyası, ana süreç ortamı, DB, JWT ve CORS değerleri değişmez.

Ortak path builder'ın mevcut tek segment davranışı korunur. Yalnız P2 ile uyumlu güvenli `local/test` alt prefix'leri ek olarak kabul edilir; boş/traversal segmentleri, backslash ve üretim alt prefix'leri reddedilir (ADR-0023). Preflight bu gerçek bean'leri kullanır; test için storage bean'i değiştirilmez.

Profilin `growth-evidence` alanı makine kimliğine girer ve yalnız ASCII kabul eder. Sabit teknik kod `P3_LOCAL_CMS_SYNTHETIC_RGB8_80X60_TO_40X30_PNG_GROWTH_APPROVED`, yalnız bu sentetik opak RGB8 80×60 kaynaktan 40×30 PNG çıktısında büyümenin açıkça kabul edildiğini belirtir. Geometri, kaynak/çıktı limitleri, prefix ve admission değişmez. Preflight artık gerçek `ReadingPublicationReadyApi` bean'i üzerinden tüm rollerin etkin profil kimliğini de hesaplar; yalnız ayar bağlamasının geçmesi yeterli değildir.

Makbuzdaki `contentId` ile mevcut CMS içerik detayına gidin. Türkçe sekmede **Yayınla** eylemini çalıştırın. Kartta **“Doğrulanmış okuma revizyonu benimsendi”** yazdığını doğrulayın. Revision ve manifest digest değerleri durum API’sinde/DB kanıtında bulunur; kart bu hash değerlerini göstermez. `GET /api/admin/contents/{contentId}/reading-publication?languageCode=tr` durum yanıtını ve `reading_publications`/`reading_image_jobs`/`reading_image_manifests` satırlarını kanıt olarak kaydedin. Saklanan çıktıların bağımsız pinned okuma, MIME, SHA, decode ve geometri doğrulaması motorun LIVE kabulünden önce çalışır; manuel hazır durumu bu kanıtı üretemez.

Kapsam: `tellpal-ee0dd.appspot.com` altında tek 80×60 opak RGB8 PNG; kapak/detail/sayfa aynı admitted asset'i kullanır. Çıktı 40×30 PNG'dir. Kaynak, admitted kopya, retained source ve üç rol çıktısı dahil en fazla altı kalıcı nesne; nesne başına 1 MB, toplam 6 MB sınırı vardır. Ses yalnız `LOCAL_STUB` kaydıdır; ses byte'ı yüklenmez.

Yerel doğrulamalar:

```bash
python3 -m unittest discover -s be/tools -p test_p3_local_cms.py -v
cd be
bash ./mvnw -q -Dtest=P3LocalCmsSetupTest,AssetStorageObjectPathBuilderTest,P3LocalCmsSeedIntegrationTest test
```

Son komuttaki entegrasyon testi Testcontainers PostgreSQL ve mevcut disk storage fixture'ını kullanır; bulut kimliği veya ağ depolaması kullanmaz. Aracın gerçek `admitAndSeed` akışı yeni admitted asset'i DRAFT Türkçe kapağa ve tek sayfaya bağlar; aynı içerik gerçek Publish/encoder/doğrulama akışıyla LIVE'a ulaşır. CI, Maven verify öncesinde `be/` altında `python3 -m unittest discover -s tools -p 'test_*.py' -v` çalıştırır.

2026-10-17 araç doğrulaması: 14 Java (9 hazırlık, 4 path builder, 1 gerçek seed entegrasyonu) ve 16 P3 Python testi geçti. CI ile aynı tüm launcher komutu 34/34 başarılıdır. Java ve Python dry-run çıktıları sıfır kimlik okuması/ağ/DB yazısı kapsamını gösterdi. Offline `--prepare` başarıyla tamamlandı. Ürün kodundaki tek değişiklik güvenli local/test alt prefix uyumudur; izlenen yerel config ve mevcut kayıtlar bu aşamada değiştirilmedi.

2026-10-17 gerçek yerel hazırlık sonucu: salt okunur Spring/DB preflight geçti. Onaylanan alanda kaynak oluşturma ve immutable kaynak kabulü başarılı oldu; `#93 — P3 yerel görsel denemesi` DRAFT olarak hazırlandı. Önceden var olan 91 içerik ve ilişkili kayıtlar, özel baseline ile seed sırasında ve sonrasında karşılaştırıldı; korundu. Backend `--start --foreground` ile aynı `8080` portunda çalışıyor; sağlık yanıtı `UP`. Chrome oturumu aynı `5173` adresini kullanıyor.

Doğrudan açılacak sayfa: `http://localhost:5173/contents/93?type=STORY&language=tr`.

Tam backend `bash ./mvnw -o verify` sonucu: 467 Surefire ve 1 Failsafe testi başarılı; hata/atlama yok. Sonradan eklenen ASCII profil kanıtı ve inceleme düzeltmeleri odaklı Java 14/14, P3 Python 16/16 ve tüm launcher Python 34/34 testleriyle doğrulandı. Ortak yol üreticisindeki dar düzeltme bu tam kontrolde yer aldı. İlk arka plan başlatması kalıcı çalışmadığından yönetilen foreground oturumu kullanıldı; seed tekrar edilmedi.

Chrome üzerinden demo `#93` için **Dili yayina al** çalıştırıldı. İlk profil kimliğinde ASCII dışı açıklama engeli çıktı; görsel işi oluşmadan teknik kod düzeltildi. Aynı kaynak, admission, prefix ve limitlerle mevcut yayın isteği sürdü; seed tekrarlanmadı. Generation `2`, ilk job attempt içinde otomatik `LIVE` oldu. Chrome’da **“Doğrulanmış okuma revizyonu benimsendi”** durumu gözlendi. Üç 40×30 PNG, gerçek kaydedilmiş nesnelerden bağımsız doğrulandı. LIVE sonrası eski 91 kayıt tekrar karşılaştırıldı ve korundu. Ayrıntı: [deneme kanıtı](evidence/p3-local-cms/verification.md).

Aynı denemeyi elle tekrarlamak isterseniz yalnız bu hikâyede **Dili arsivle**, ardından **Dili yayina al** kullanın. Yeni görsel yüklemeyin ve kaynak bağlarını değiştirmeyin; mevcut sentetik kimliklerle çalışın. Eski mobil/işleme sayacı bu P3 testinin sonucu değildir; doğrulanacak alan okuma revizyonu kartıdır.
