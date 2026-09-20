# TellPal Android Emülatör Doğrulama Raporu

**Tarih:** 2026-09-20  
**Cihaz:** `Medium_Phone` / `emulator-5554` / Android emülatör  
**Build:** Debug APK, `1.0.35+333`  
**Kapsam:** Statik proje dokümanlarının gerçek runtime ekranları ve temel navigasyonla karşılaştırılması

## Sonuç

Uygulama Android emülatörde başarıyla derlendi, kuruldu ve çalıştı. Ana ekran, profil,
çocuk/ebeveyn modu ve ebeveyn günlük içerik akışları statik dokümanlarla uyumlu gözlendi.
Premium paywall tetikleme de doğrulandı; satın alma sonucu emülatörün Google Play Billing
servisi bulunmadığı için test edilemedi.

## Doğrulanan Akışlar

| Akış | Sonuç | Kanıt |
|---|---|---|
| Uygulama açılışı → kitaplık | Başarılı | `emulator-validation.png` |
| Hikâye vitrinleri ve görsel yükleme | Başarılı | `emulator-home2.png` |
| Profil sekmesi | Başarılı | `emulator-profile.png` |
| Çocuk/ebeveyn seçim diyaloğu | Başarılı | `emulator-user-type-dialog.png`, `emulator-parent-dialog.png` |
| Çocuk → ebeveyn modu | Başarılı | `emulator-parent-wait.png` |
| Günlük ücretsiz ebeveyn rehberi | Başarılı | `emulator-parent-wait.png` |
| Ebeveyn → çocuk modu | Başarılı | `emulator-child-return.png` |
| Premium hikâye bilgi ekranı | Başarılı | `emulator-premium-story.png` |
| RevenueCat PaywallActivity açılışı | Başarılı | `emulator-paywall.png`, Android activity log |
| Ninni/rahatla sekmesi | İlk açılışta loader gözlendi; son durumda paywall hata modalı akışa müdahale etti | `emulator-relax.png`, `emulator-relax-wait.png` |
| Ebeveyn favorileri → Haberci Nota bilgi ekranı | Başarılı; ücretsiz erişim | `emulator-haberci-info.png` |
| Haberci Nota → Hikâyeye Başla | Başarılı; içerik ilk sayfaya geçti | `emulator-haberci-content.png` |
| Haberci Nota sayfa ileri kaydırma | Başarılı; ikinci sayfa görsel/metni geldi | `emulator-haberci-page2.png` |
| Haberci Nota ikinci ZIP | Başarılı; `stories/63#2.zip` indirildi ve çıkarıldı | Android uygulama logu |
| Arama → Kaan Satranç Öğreniyor bilgi ekranı | Başarılı; 9 dakika, 5+ yaş ve “Hikâyeye Başla” göründü | `emulator-branch-info.png` |
| Kaan Satranç Öğreniyor normal ilerleme | Başarılı; sayfalar ileri kaydırıldı ve “Hikâyeyi Bitirdin!” ekranına ulaşıldı | `emulator-branch-page1.png`, `emulator-branch-page6.png` |
| Haberci Nota hızlı seri kaydırma | Uygulama çökmedi; ikinci ZIP başarıyla indirildi, fakat sayfa tamamlanması deterministik gözlenmedi | `emulator-interactive-fast-swipe-final.png`, Android logu |
| Uyku sekmesi → ninni vitrini | Başarılı; “Ninni ve Dinlendirici Müzikler” bölümü ve ilk kart “Dandini Dastana” yüklendi | `emulator-uyku-loaded.png` |
| Dandini Dastana → ninni oynatıcı | Başarılı; 10:19 ses dosyası otomatik oynadı, duraklat/devam ve “Çık” çalıştı | `emulator-ninni-dandini.png`, `emulator-ninni-paused.png` |
| Uyku → meditasyon bilgi ekranı | Başarılı; “Bilinçli Uyku Meditasyonu”, 6 dakika, 3+ yaş, özet/yazar/seslendiren alanları göründü | `emulator-meditasyon-first.png` |
| Bilinçli Uyku Meditasyonu → oynatıcı | Başarılı; 6:35 ses otomatik oynadı, ortak ses oynatıcı açıldı | `emulator-meditasyon-player.png` |
| Uyku → sesli kitap bilgi ekranı | Başarılı; “Zıt-Giller Kasabası”, 8 dakika, 4+ yaş, “Sesli Hikâyeye Başla” ve metadata göründü | `emulator-sesli-hikaye-first.png` |
| Zıt-Giller Kasabası → sesli hikâye oynatıcı | Başarılı; 8:00 ses otomatik oynadı ve ortak oynatıcı açıldı | `emulator-sesli-hikaye-player.png` |
| Uyku → kilitli meditasyon kartı | Paywall açıldı; emülatörde Google Play Billing olmadığı için `Error 3` modalı gösterildi | `emulator-uyku-locked-meditasyon-error.png`, Android activity/log |
| İlk çalıştırma → bildirim izni | Başarılı; Android izin penceresi “Allow TellPal to send you notifications?” metniyle açıldı | `emulator-auth-current.png` öncesi Android permission UI |
| İlk çalıştırma onboarding | Başarılı; üç tanıtım sayfası, “Atla” ve son sayfadaki “Başla” akışı görüldü | `emulator-auth-current.png`, `emulator-onboarding-page3.png` |
| Onboarding → hesap seçenekleri | Başarılı; e-posta/Google kayıt, mevcut hesapla giriş ve hesapsız devam seçenekleri açıldı | `emulator-onboarding-popup.png` |
| Hesapsız devam → ana ekran | Kısmi; önce RevenueCat `Error 3` modalı açıldı, yeniden başlatma sonrası anonim ana ekran geldi | `emulator-anonymous-relaunch2.png`, Android logu |
| Anonim profil sekmesi | Başarılı; “User”, ücretsiz üyelik çağrısı, bildirim, dil, çıkış ve `1.0.35` sürümü göründü | `emulator-profile-anonymous.png` |
| Ücretsiz üyelik → kayıt onboarding'i | Başarılı; eski/alternatif onboarding sayfası, `Sign Up`/`Login` üst aksiyonları ve Google/E-posta kayıt seçenekleri açıldı | `emulator-membership-entry.png`, `emulator-onboarding2.png` |
| E-posta kayıt formu | Başarılı; e-posta, en az 8 karakter parola, GDPR ve pazarlama onayı alanları göründü | `emulator-register-screen.png` |
| E-posta kayıt boş doğrulama | Başarılı; GDPR onayı olmadan “GDPR Fair Processing Notices needs to be accepted.” gösterildi | Android UI ağacı |
| E-posta giriş formu | Başarılı; e-posta, parola, giriş, parola unutma ve Google ile giriş seçenekleri göründü | `emulator-login-screen.png` |
| Boş giriş doğrulaması | Başarılı; “Email field cannot be empty.” ve “Password field cannot be empty.” gösterildi | Android UI ağacı |
| Parola sıfırlama formu | Başarılı; kayıtlı e-posta alanı ve doğrulama gönderme aksiyonu göründü; boş gönderimde e-posta hatası gösterildi | `emulator-forgot-password.png`, Android UI ağacı |
| Gerçek e-posta ile kayıt | Başarılı; test hesabı oluşturuldu ve oturum ana ekrana taşındı | `emulator-after-register.png`, `emulator-relaunch2.png` |
| Kayıt sonrası profil sihirbazı | Başarılı; amaç, yaş aralığı, favori tür, avatar ve çocuk adı adımları görüldü; Skip ile veri girmeden ilerlenebildi | `emulator-profile-step1.png`, Android UI ağacı |
| Kayıt sonrası profil ekranı | Başarılı; `User` profili, premium çağrısı ve e-posta doğrulama uyarısı göründü | `emulator-auth-profile.png` |
| Kayıtlı hesap → çıkış → tekrar giriş | Başarılı; çıkış onayı sonrası onboarding/hesap seçenekleri açıldı, aynı e-posta/parola ile login ana ekrana döndü | `emulator-logout-confirm.png`, `emulator-after-login.png` |
| Hesap ayarları → parola değiştir | Ekran açıldı; boş gönderimde eski ve yeni parola alanları için ayrı ayrı `Password field cannot be empty.` validasyonu görüldü; gerçek parola değiştirilmedi | `emulator-change-password.png`, Android UI ağacı |
| Profil düzenleme | Profil başlığındaki düzenleme simgesi `Update Profile` ekranını açtı; çocuk adı (`User`), profil görseli seçimi ve `Complete` düğmesi görüldü; kayıt yapılmadı | Android UI ağacı |

## Runtime Bulguları

### Ana ekran ve içerik

- Türkçe arayüz açıldı.
- “Kitaplık / Uyku / Profil” alt navigasyonu görünür ve çalışır durumda.
- REST/Storage içerikleri gerçek görsellerle yüklendi.
- Premium kartlarda kilit göstergesi görünür.

### Onboarding, anonim kullanım ve auth ekranları

- Uygulama verisi temizlenmiş ilk açılışta splash sonrasında Android bildirim izni istendi.
- İlk onboarding üç sayfadan oluşuyor: çocuklar/ebeveynler için değer önerisi, hikâye-
  meditasyon-kaynak vurgusu ve kütüphane/ninni/ebeveyn özetleri özellik listesi. “Atla”
  düğmesi her dokunuşta bir sonraki sayfaya ilerliyor; son sayfadaki “Başla” hesap seçenekleri
  bottom sheet'ini açıyor.
- Hesap seçenekleri e-posta veya Google ile kayıt, mevcut hesapla giriş ve “hesap olmadan
  devam et” seçeneklerini içeriyor. Apple seçeneği Android'de görünmüyor.
- Hesapsız devam etme sırasında emülatörde otomatik premium açılışı RevenueCat `Error 3`
  ile sonuçlandı. Uyarı kapatılamasa da uygulama yeniden başlatıldığında anonim ana ekran
  açıldı; bu, anonim durumun yerel tercihlerde kalıcılaştığını gösteriyor.
- Anonim profil ekranı gerçek profil yerine “User” gösteriyor ve “Tellpal'ın tüm
  özelliklerinden yararlanmak için üye olun” çağrısı sunuyor. Aynı ekranda bildirim anahtarı,
  dil seçimi, çıkış ve sürüm bilgisi bulunuyor.
- “Ücretsiz üyelik oluştur” anonim profilden `/onboarding` akışına dönüyor. Bu alternatif
  onboarding ekranında üstte `Sign Up` ve `Login`, özellik listesi ve Google/E-posta kayıt
  düğmeleri var.
- E-posta kayıt formu e-posta, en az 8 karakter parola, GDPR/Fair Processing onayı ve
  ticari ileti izni alanlarını içeriyor. Boş gönderimde önce GDPR onayı zorunlu tutuluyor.
- Giriş formunda e-posta, parola, Google ile giriş ve parola sıfırlama bağlantısı var. Boş
  form doğrulaması e-posta ve parola alanlarını ayrı ayrı işaretliyor.
- Parola sıfırlama ekranı kayıtlı e-postaya doğrulama gönderiyor; boş e-posta için alan
  hatası gösteriyor. Google/Apple hesabı kullanılmadı; e-posta hesabı ise aşağıdaki gerçek
  kayıt senaryosunda kullanıldı.
- Test e-posta hesabıyla kayıt gerçek Firebase Auth üzerinde başarıyla tamamlandı. Kayıt
  sonrasında uygulama beş adımlı profil sihirbazına geçti: kullanım amacı, yaş aralığı,
  favori türler, avatar ve çocuk adı. Her adımda `Skip` ile veri girmeden sonraki adıma
  ilerlenebildi.
- Profil sihirbazının son `Skip` adımından sonra uygulama RevenueCat satın alma denemesini
  otomatik tetikledi ve emülatörde `Error 3` verdi. Uygulama yeniden başlatıldığında oturum
  korunarak ana ekrana ulaşıldı.
- Kayıtlı hesap profilinde görünen ad varsayılan `User`; istatistikler `0 Finished Story` ve
  `0 Minutes`. Ayrıca "Your email has not been verified yet. Click to verify." uyarısı
  gösterildi. Bu turda doğrulama e-postası gönderme aksiyonuna basılmadı.
- Hesap ayarlarından çıkış onayı çalıştı. Çıkış sonrasında uygulama ilk onboarding sayfasına
  döndü; onboarding üç kez `Skip` ile geçilerek hesap seçeneklerine ulaşıldı. Aynı test
  bilgileriyle giriş başarılı oldu ve doğrudan ana ekran açıldı. E-posta doğrulama bağlantısı
  gerektiren bir engel gözlenmedi.
- Account Settings içindeki Change Password ekranında eski ve yeni parola alanları bulundu.
  Boş gönderimde iki alan için ayrı hata üretildi. Bu turda gerçek parola değiştirilmedi;
  test hesabının mevcut parolası korunuyor.
- Profil başlığındaki düzenleme simgesi, başlangıç sihirbazından ayrı bir `Update Profile`
  ekranı açıyor. Bu ekranda `Your Child's Name` alanı mevcut değer `User` ile dolu,
  `Select Profile Image` bölümü ve `Complete` düğmesi bulunuyor. Bu turda kaydetme yapılmadı.

### Profil

- Anonim kullanıcı profili “Kullanıcı” olarak açıldı.
- Ücretsiz üyelik oluşturma çağrısı, bildirim, dil ve çıkış satırları görünür.
- Ekrandaki sürüm metni `1.0.35` olarak göründü.

### Ebeveyn modu

- Çocuk/ebeveyn seçim modalı beklenen seçenekleri gösterdi.
- Ebeveyn modu açıldı ve “Günün Ücretsiz Özeti” içeriği gerçek veriyle geldi.
- “Otomatik Portakal” başlığı, görseli, özeti ve “Hemen Dinle” düğmesi göründü.
- Çocuk moduna geri dönüş çalıştı.

### Premium / RevenueCat

- Premium hikâye bilgi ekranı açıldı.
- “Dinlemek için Premium’a Geç” düğmesi RevenueCat `PaywallActivity` başlattı.
- Emülatör logunda `BILLING_UNAVAILABLE` / `PurchaseNotAllowedError` görüldü.
- Bu sonuç uygulama içi paywall tetiklemesinin çalıştığını, ancak satın alma/ürün
  tekliflerinin bu emülatörde doğrulanamadığını gösterir.

### Haberci Nota ücretsiz hikâyesi

- Ebeveyn favorileri bölümündeki ilk kartın hikâye kimliği loglarda `63` olarak görüldü.
- Bilgi ekranında başlık, 4 dakika süre, `3+ Yaş`, özet, seslendirme ve “Hikâyeye Başla”
  düğmesi göründü.
- Ücretsiz olduğu için RevenueCat paywall açılmadan içerik ekranına geçildi.
- İlk sayfada Nota görseli ve Türkçe hikâye metni yüklendi.
- İleri kaydırmada ikinci sayfa görseli/metni yüklendi.
- Loglarda `StorySecondPartCubit` başarı durumu, 28 dosyalı çıkarım ve audio session
  aktivasyonu görüldü.

### Uyku sekmesi: ninni, meditasyon ve sesli hikâye

- Alt navigasyondaki `Uyku` sekmesi `RelaxView` tarafından açılır; üstte yıldızlı
  `relax_header.jpg` hero alanı, ardından API'den gelen kategori vitrinleri gösterilir.
- Vitrin verisi `GET /story/with-category/listening?count=...` ile alınır. Kategori tipi
  `LULLABY` ise yatay ninni listesi; `MEDITATION` ve `AUDIO_STORY` ise ortak meditasyon/
  sesli-hikâye kart bileşeni kullanılır.
- Ninni kartı doğrudan `LullabyContentView` oynatıcısına gider. Dandini Dastana örneğinde
  10:19 toplam süre, müzik ve enstrüman metadata'sı, otomatik oynatma, duraklat/devam ve
  `Çık` ile geri dönüş gözlendi. Ninni sesleri `LoopMode.all` ile döngüye alınır.
- Meditasyon ve sesli hikâye kartı önce ortak `InfoMeditationView` bilgi ekranına gider.
  Bu ekranda kapak, başlık, süre, yaş, özet, yazar, seslendiren ve türe göre
  `Meditasyona Başla` veya `Sesli Hikâyeye Başla` düğmesi bulunur.
- Başlatma düğmesi içerik geçmişini Firebase Realtime Database'e açar, analitik olayı
  kaydeder ve aynı `LullabyContentView`/`PlayerManager` oynatıcısını açar. Meditasyon
  örneği 6:35, sesli hikâye örneği 8:00 toplam süre ile otomatik oynadı; bu iki türde
  `LoopMode.off` kullanılır.
- Premium meditasyon kartına dokunma bilgi ekranına gitmeden RevenueCat paywall'ını
  açar. Emülatörde paywall kabuğu açıldı ancak teklif yüklemesi `BILLING_UNAVAILABLE`
  nedeniyle `Error 3: The device or user is not allowed to make the purchase.` modalında
  kaldı; satın alma düğmesine basılmadı.
- “Hepsi” bağlantıları kategori kimliği/türü/açıklamasını `CategoryStoriesPageType.listWithStories`
  ile `/categoryStories` ekranına taşır. Bu doğrulama turunda bağlantının hedef liste
  ekranı ayrıca açılmadı; ilk kart → bilgi/oynatıcı zincirleri doğrulandı.

### İnteraktif hikâye akışı

- Arama ekranından `Haberci Nota` ve `Kaan Satranç Öğreniyor` ücretsiz/erişilebilir
  hikâyeleri açıldı. Her ikisinde de bilgi ekranı → “Hikâyeye Başla” → tam ekran sayfa
  okuyucu → “Hikâyeyi Bitirdin!” zinciri çalıştı.
- Kaan örneği 9 dakika ve 5+ yaş olarak gösterildi; altı sayfalık doğrulama sonrası
  tamamlanma ekranı görüntülendi.
- Haberci Nota'da normal geçişin yanında altı hızlı seri swipe denendi. İkinci ZIP
  (`storyId=63`, `version=2`) başarıyla indirildi ve uygulama çökmedi; ancak ekranın
  hangi son sayfada kalacağı hızlı seri olaylarda deterministik gözlenmedi.
- Kaynak modelinde `StoryPointEntity.next` ve `optionAnswer` dallanma alanları mevcut.
  Bu turda erişilebilir ücretsiz içeriklerde seçim kartı açılmadı; arama sonuçlarında
  seçim adayı görünen içerikler premium kilitli olduğundan gerçek bir şık seçimi ve
  alternatif dal doğrulanamadı.

## Performans Bulgusu

İlk açılış loglarında `Skipped 105 frames`, `Skipped 190 frames`; paywall açılışında ise
`Skipped 216 frames` görüldü. Bunlar debug/emülatör koşullarında ölçülmüştür ve üretim
performansı olarak yorumlanmamalıdır; yine de `main.dart` başlangıç işleri, görsel decode,
RevenueCat bağlantısı ve paywall geçişi için profil oluşturma adayıdır.

## Doğrulanamayan Alanlar

- Google/Apple gerçek kimlik doğrulaması.
- Kayıtlı e-posta için doğrulama e-postasının gönderilmesi ve bağlantı ile doğrulama.
- Profil sihirbazında gerçek amaç/yaş/favori/avatar/çocuk adı verileri doldurularak
  Realtime Database'e yazılan kişiselleştirilmiş profil.
- Google Play Billing ile gerçek ürün satın alma/restore.
- İki ZIP sınırında hızlı kaydırma.
- Hikâye içeriğinin tüm sayfaları, dallanma seçimi ve tamamlama geçmişi.
- Ses oynatma ve arka plan/foreground kesintileri.
- Uyku içeriklerinde arka plan/foreground kesintisi ve ekran kilidi sonrası devam.
- Hesap silme, parola değiştirme ve bildirim izinlerinin production davranışı.
- iOS runtime.

Bir normal sayfa kaydırması ve Kaan tamamlanma ekranı doğrulandı. Haberci Nota'da
yüksek hızlı seri kaydırma sırasında çökme görülmedi, fakat ilk/ikinci ZIP sınırında
geri-ileri yarışının doğru son sayfaya ulaştığı kanıtlanmadı. Dallanma seçimi için
premium içerik erişimi veya backend'den seçimli bir test hikâyesi gerekir.

Kalan alanlar için profil verileri doldurulmuş bir test hesabı, Play Billing lisanslı
cihaz/emülatör, Firebase/RevenueCat test ortamı ve manuel akış planı gerekir. Ayrıca Android
14/15 hedefinde ses servisinin
foreground izinleri (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) ayrıca doğrulanmalıdır; debug
logunda bu izinle ilgili `SecurityException` uyarısı görüldü, ancak emülatörde ses
oynatımı devam etti.

## Statik Dokümana Etkisi

Emülatör doğrulaması ana ekranın üç sekmeli yapısını, çocuk/ebeveyn modunu, parent mode
günlük ücretsiz içerik akışını, Uyku sekmesindeki ninni/meditasyon/sesli hikâye ortak
oynatıcı zincirlerini ve premium içerik → bilgi ekranı → RevenueCat paywall zincirini
doğruladı. Runtime'da loading/performance ve Android foreground-audio izin riski de
gözlendi.

> Kullanıcının talimatı gereği Crashlytics rapor/doküman klasörlerinin içeriği bu
> doğrulamaya dahil edilmemiştir.
