# P4b5 manual retry yerel uygulama kaydı

2026-10-20. Durum: yerel kod ve test kapıları geçti. MAC_EK kopyası otomatik onay incelemesi tarafından iki kez reddedildi. Somut kaynak/APK/run ve üç bağımsız ön inceleme henüz yoktur. Gerçek deneme onayı verilmemiştir.

## Uygulanan değişiklik

Python, Java ve Dart kataloglarına sabit `p4b5-manual-retry-af5cbc33-ee49-41ef-aa04-986cb3af292f` eklendi. Paket `com.tellpal.tellpal_mobile_poc.readingmanualretryqa`, entry `test_driver/reading_consumer_manual_retry_entry.dart` oldu. Eski varsayılanlar korunur. İki manual kapsam aynı controller, adapter, bootstrap, DI ve coordinator zincirini kullanır; yeni router veya worker yoktur.

Yeni kapsam üç geçmişin exact öneri/proof zincirini ve 50 dosya hash’ini ister. Üçüncü denemenin `FAILED_NO_RETRY / arm / QA_OWNER_CHANGED` kaydı, üç tüketilmiş başlangıç ve V37 borcu değişmez. Yeni paketin yokluğu ilk kurulum claim’inden önce doğrulanır. `pm path` için exit 0/1, boş stdout ve boş stderr kabul edilir; kurulu paket, izin veya transport hatası reddedilir. Yeni kurulum `install -r` kullanmaz.

Manual-aile kaynak dizini numaralıdır. Yerel plan onayı ve exact ayrı gerçek yürütme onayı korunur. VM yolları iki manual kapsamı da reddeder. Owned Python modüllerinin yüklenmiş `__file__` yolu doğrudan seçili kaynak dosyası olmalıdır. Eski owned import girdisi, eksik `__file__` ve yanlış komut öneki kabul edilmez.

Yeni native prefix `P4B5_MANUAL_RETRY_RESOURCE_JSON:` olur. Zarf ve her gerçek probe sample’ı measurementId, proposalSha256 ve budgetRunId taşır. Eski örneğin yalnız dış etiketinin değiştirilmesi reddedilir. İki manual kapsamın overflow terminali, raster-ready, protokol, dört paint/ACK ve lifecycle akışı ortak kalır.

## Doğrulama

- Python: gerekli dört Dart JSON env girdisiyle `python3 -m unittest discover -s tools/image-optimization -p 'test_p4b5*.py'`; **78 test, 0 skip, PASS**.
- Flutter: credential’sız `/private/tmp/p4b5-manual-retry-tests/mobile` kopyasında `flutter pub get --offline`, `flutter test --no-pub test/reading_images`; **232 test, 0 skip, PASS**. Bu geçici kopya görev dışı mevcut local-lossless testlerini de içerir; planlanan MAC_EK kaynak/APK kabulü değildir.
- Aynı geçici Flutter kopyasında `flutter analyze --no-pub`: **0 bulgu**.
- Backend: mevcut Java 25, wrapper ve offline cache ile `sh ./mvnw -o -Dtest='P4b5ConsumerQa*Test,P4b5ConsumerQaIT' test`; **26 test, 0 skip, PASS**. Wrapper çalıştırılabilir işaretli olmadığı için `sh` kullanıldı.
- İki IT yeni scope ile ayrı sentetik PostgreSQL 18 DB’lerinde gerçek V37 kotasını sınadı. Gerçek QA DB okunmadı, klonlanmadı veya sıfırlanmadı. Göreve ait geçici sunucu kapatıldı.
- Tam olumlu yeni-scope Java fixture son `validateReviewedRuntime` kapısını geçti. Tarihsel özel dosya sınırı sentetik hash fixture’ıyla mock edildi; kaynak/CP/APK/Python/onay kapıları gerçektir. Ek salt okunur kontrolde üç gerçek tarihsel zincirin 50 dosyası da doğrulandı.
- SourceDirectory, entry, paket, prefix, import, exact onay, geçmiş audit/başlangıç tüketimi ve native iç/dış kimlik mutantları reddedildi. Gerçek bootstrap testleri yeni ince entry’den initialize/DI/protokol zincirine girer. Mevcut `/proc` sahiplik/yokluk ve cleanup ret testleri korundu.
- `git diff --check` temizdir. Ham yerel test logları repoya alınmadı; yol ve SHA’ları [makine kaydındadır](local-validation-2026-10-20.json).

Python’ın sentetik süreç testinde macOS Python launcher exec’i tamamlanmadan alınan argv hash’i değişebiliyordu. Fixture artık çocuğun hazır dosyasını bekler; üretim sahiplik kontrolü gevşetilmedi.

## Bekleyen somut kopya

[Payload audit’i](copy-payload-audit-2026-10-20.json) **1166 dosya / 8.552.437 byte** içerir. SHA-256: `8c3ed7a5dcef734bed9abc19481150a7f3d31f98b34ffd2bc6097c37fca28580`.

Hedef: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1/source-0001`.

Sınıflar: 379 Flutter kaynak/asset/test dosyası; 699 backend kaynak/test dosyası; 65 QA araç/UID dosyası; dört bağlı öneri; 19 backend build/geliştirici metadata dosyası. Audit her dosyanın göreli yolunu, repo/kopya byte sayısını ve SHA’sını listeler. Credential, token, servis hesabı, API config, `.env`, PEM veya keystore dosyası yoktur. Tek service_account metin eşleşmesi açıkça sentetik SDK unit-test fixture’ıdır. Görev dışı dirty `main_image_experiment.dart` repoda korunur; kopya için HEAD sürümü seçilmiştir. `.memlog.md` ve local-lossless kodu değiştirilmemiştir.

İlk otomatik ret, bu dosyaları yeni hedefe aktarmak için özgül kullanıcı yetkisi görmediğini belirtti. Kullanıcının plan onayı, teknik ekteki sabit hedef ve credential dışlama audit’iyle aynı işlem yeniden sunuldu. İkinci ret de doğrudan güvenilir payload/hedef onayı olmadığını belirtti. Ret dolanılmadı. Yeni MAC_EK kökü oluşmadı. APK başka hedefte hazırlanmadı.

Açık kopya onayı sonrası manifest hash’leri tekrar doğrulanarak sıralı kopya → offline pub → package_config self-root → kaynak bağı → offline APK → Java/Python/APK/run bağları → üç bağımsız inceleme tamamlanmalıdır. Ancak bundan sonra somut gerçek koşu ayrıca onaya sunulabilir.

## Açık sınırlar

Gerçek QA kurulum/ilk başlangıç/backend/giriş/pencere ve medya çağrıları **sıfırdır**. Eski üç hak iade edilmedi. Gerçek deneme, üç son inceleme ve görevle sınırlı yerel commit yapılmadı. Fiziksel kalite, RSS/GPU, JPEG/WebP, P0 ham kanıt ve bütün P4b kabulü açık kalır. Yerel testler emülatör başlangıcı veya üretim onayı değildir.

## Son onay incelemesi kaydı

Kullanıcı `99831cdb…` SHA’lı 8.552.293 byte payload’ı doğrudan onayladı. Son iki dosyadaki 144 byte’lık yerel koruma/test farkı açıkça bildirildi. Eski manifest ters diff’ten yeniden oluşturuldu ve orijinal SHA-256 ile byte düzeyinde birebir eşleşti; [eski onaylı manifest](copy-payload-audit-approved-99831cdb-2026-10-20.json), [güncel sabit manifest](copy-payload-audit-current-8c3ed7a5-2026-10-20.json) ve [144 byte fark kanıtı](copy-payload-delta-99831cdb-to-8c3ed7a5-2026-10-20.json) ayrı dosyalarda korundu.

Aynı hedefe güncel payload kopyası bu kanıtla üçüncü kez otomatik incelemeye sunuldu. [Kesin ret](approval-review-block-0003-2026-10-20.json), doğrudan kullanıcı onayının önceki 8.552.293 byte için olduğunu ve güncel 8.552.437 byte için açık onay bulunmadığını belirtti. İşlem durduruldu; yeni MAC_EK kökü oluşmadı. Kaynaklar değiştirilmedi. Kopya, APK, run bağları, üç ön inceleme ve gerçek deneme bekliyor.
