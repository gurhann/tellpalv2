# Ekran tarifleri — yeniden oluşturma sözleşmesi

Bu belge tek tek ekranların telefon kompozisyonunu tarif eder. Bütün ölçüler **dp**; telefon referans Android ekranı yaklaşık **411×914dp**. **Tablet varyantları ve 800×1280dp portre kanıtları [TABLET.md](./TABLET.md) içindedir**; buradaki telefon ölçülerini otomatik büyütmeyin. Ekran görüntüleri `evidence/` altındadır. Koordinat yerine akış ve oran belirtilen yerlerde Flutter kaynağı oran kullanır. `G` = Android görüntüsü, `K` = kod, `Y` = yalnızca çıkarım. Gerçek içerik metni alan adlarıyla yazılmıştır; metnin dili yerleşim kuralını değiştirmez. `DESIGN.md` tokenları ve `EXPERIENCE.md` davranışları bu ekran tarifleriyle birlikte okunur.

## Açılış, hesap ve profil kurma

### 01. Splash — `/`, `/welcome` · K

- **Yerleşim:** tam ekran açık zemin, ortada marka görseli; durum ve sistem gezinme çubukları platforma ait. İçerik yüklenirken başka kontrol yok.
- **Alanlar:** `brand.logo`; sonra ilk kullanım, anonim/kayıtlı oturum ve profil durumuna göre yönlendirme.
- **Varlık:** `assets/logo/` ve splash kaynakları; görsel eşlemesi `ASSET-MAP.md`.
- **Kanıt:** `lib/features/auth/presentation/pages/splash_screen.dart`. Elde ayrı, temiz splash görüntüsü yok; önceki `emulator-relaunch-after-register.png` geçici yükleme anını gösterir.

### 02. İlk kullanım tanıtımı — `/landingScreenView` · G+K

- **Yerleşim:** üstte kolaj tarzı kapak görselleri; ortada sayfa göstergesi ve büyük başlık, altında açıklama; en altta `action.skip` veya son sayfada `action.start`.
- **Durum:** üç sayfa; `Skip` mevcut davranışta birer sayfa ilerler. Son sayfa hesap seçenekleri alt panelini açar.
- **Metin alanları:** `onboarding.pageN.title`, `onboarding.pageN.description`, `action.skip`, `action.start`. Çeviri iki/üç satıra yayılabilir; üst kolaj sabit alan olarak kalır, metin bölgesi dikey büyür.
- **Kanıt:** `emulator-auth-current.png`, `emulator-onboarding-page3.png`, `emulator-onboarding-popup.png`; `landing_screen.dart`.

### 03. Alternatif onboarding — `/onboarding` · G+K

- **Yerleşim:** sayfanın üstünde illüstrasyon/özellik listesi; köşede kayıt/giriş aksiyonları; alt bölümde Google ve e-posta yöntemleri. Bu yüzey ilk kullanım tanıtımından ayrıdır.
- **Alanlar:** `benefits[]`, `action.register`, `action.login`, `action.google`, `action.email`.
- **Kanıt:** `emulator-membership-entry.png`, `emulator-onboarding2.png`.

### 04. Hesap seçenekleri alt paneli · G

- **Yerleşim:** ekran altından yükselen açık panel, başlık + e-posta ve Google CTA'ları; ayırıcı metin; mevcut hesapla giriş; en altta hesapsız devam. Arkada onboarding görünür.
- **Davranış:** seçim hedef formu veya anonim ana ekranı açar; Android'de Apple seçeneği gösterilmez.
- **Kanıt:** `emulator-onboarding-popup.png`, `emulator-loggedout-options.png`.

### 05. E-posta kayıt — `/registerUser` · G+K

- **Yerleşim:** mavi yıldızlı tam enli resim üstte; geri ve `action.login` üst köşelerde; ekran yüksekliğinin yaklaşık %13'ünde büyük başlık; yüksekliğin %25'inden itibaren üst köşeleri 16dp yuvarlatılmış beyaz form paneli. Klavye açıkken panel kayar.
- **Bileşenler:** iki 48dp+ alan, parola görünürlük simgesi, zorunlu veri işleme checkbox'ı, isteğe bağlı ticari ileti checkbox'ı, kullanıcı sözleşmesi bağlantısı, altta birincil `action.continue`.
- **Metin:** `field.email`, `field.password`, `consent.processing`, `consent.marketing`, `agreement.link`. Uzun onay metni sarılır; checkbox metnini sabit yüksekliğe kırpma.
- **Durum:** boş/yanlış alan hatası alan altında; başarılı kayıt profil kurulumuna geçer.
- **Kanıt:** `emulator-register-screen.png`, `emulator-after-register.png`; `register_user.dart`.

### 06. E-posta giriş — `/login` · G+K

- **Yerleşim:** kayıtla aynı arka plan + beyaz panel kompozisyonu; panelde e-posta/parola, tam genişlik giriş, parola unuttum bağlantısı, alt ayırıcı ve Google düğmesi.
- **Durum:** boş gönderimde iki alanın ayrı hatası; doğrulanmamış hesapta da başarılı giriş ana kabuğa gitti. `action.register` üst köşede.
- **Metin:** `field.email`, `field.password`, `action.login`, `action.forgotPassword`, `action.google`.
- **Kanıt:** `emulator-login-screen.png`, `emulator-after-login.png`; `login_user.dart`.

### 07. Şifre bağlantısı isteği — `/sendResetPassword` · G+K

- **Yerleşim:** standart auth görsel dili; e-posta girişi ve gönder CTA'sı. Boş e-posta alanında hata gösterir.
- **Kanıt:** `emulator-forgot-password.png`; `send_reset_password.dart` kaynağı ve route.

### 08. E-posta action sonucu — `/authAction` · K

- **Yerleşim:** route `mode` değerine göre e-posta doğrulama veya şifre sıfırlama ekranı. Üstte geri, ortada durum/alanlar, altta birincil CTA; başarısız/geçersiz link durumu ayrı mesajdır.
- **Metin:** `action.title`, `action.status`, `field.newPassword` ve `action.confirm` yalnız ilgili modda.
- **Kanıt:** `email_verify_screen.dart`, `reset_password_screen.dart`; link alıcısı test e-postasında doğrulanamadı. Bu yüzey için birebir ekran görüntüsü yok.

### 09. Profil kurulum giriş ekranı ve beş adım — `/enterUserProfile` · G+K

- **Giriş:** açık lavanta alan, ortada içerik kolajı, büyük açıklama; üstte `Skip`, altta `Get Started`.
- **Adım kabuğu:** üstte geri / `Profile` / `Skip`, altında beş segmentli ilerleme, ortada soru ve seçenekler, altta `Continue`. Seçim yoksa Continue disabled.
- **Sıra:** kullanım amacı (illüstrasyonlu liste), yaş aralığı (0–2, 3–4, 5–6, 7–9, 9+), favori türler (iki sütun görsel grid), avatar, çocuk adı. Her adım `Skip` ile geçilebilir.
- **Metin:** `setup.stepN.question`, `setup.stepN.options[]`, `field.childName`, `action.skip`, `action.continue`. Uzun sorular sarılır; seçenek resmi metin yerine geçmemelidir.
- **Kanıt:** `emulator-after-register.png`, `emulator-profile-step1.png`, `setting_up_account_pages.dart` ve adım widget'ları. Son adım sonrası Billing Error 3 görüldü; dolu veriyle başarı görünümü doğrulanmadı.

### 10. PDF belge görüntüleyici · K+G

- **Yerleşim:** üst başlık/geri, altında uzak PDF sayfası. Yükleniyor ve hata durumları mümkün.
- **Kanıt:** `pdf_viewer_screen.dart`, kayıt sırasında açılan GDPR ekranı runtime UI ağacı; korunmuş ekran görüntüsü yok.

## Ana kabuk ve içerik keşfi

### 11. Kitaplık — `/home`, sekme 1 · G+K

- **Yerleşim:** güvenli alan altında selamlama ve sağda çocuk/ebeveyn modu; tam genişlik premium gradient banner; yuvarlatılmış arama alanı. Altında dikey bölüm akışı: solda kalın başlık+opsiyonel emoji, sağda `action.all`, ikinci satır açıklama, yatay kapak rayı.
- **Ölçü:** 16dp yatay sayfa payı; kapak 3:4; ekran altındaki üç eşit sekme sabit. İçerik rayları kendi içinde yatay, ana ekran dikey kayar.
- **Durum:** ücretsiz/premium kart, farklı dil/bölüm sırası ve loading placeholder. Başlık uzayınca `All` aksiyonu kaybolmamalı; iki satırlık başlığa izin ver.
- **Kanıt:** `emulator-home2.png`, `emulator-relaunch2.png`; `home.dart`, `reading_stories.dart`.

### 12. Arama yüzeyi · G+K

- **Yerleşim:** arama alanı odaklanınca klavye; metin sonuç listesi veya grid. Boş/sorgusuz/sonuç yok durumları ayrı.
- **Metin:** `field.query`, `search.resultTitle`, `search.emptyMessage`.
- **Kanıt:** `emulator-branch-search-open.png`, `emulator-branch-search-kaan.png`, `emulator-interactive-search.png`; `reading_stories.dart` ve arama widget'ları.

### 13. Hikâye kategorisi — `/categoryStories` · K+G

- **Yerleşim:** ortalanmış başlıklı standart app bar, altında 16dp yan paylı iki sütun/ızgara kapak listesi; kart aspect ratio 3:4, premium kilidi üst köşede.
- **Durum:** kategori/arama sonucuna göre veri; liste boş veya yüklemede ekranın durum katmanı. Kategori başlığı uzun dilde sarılmalı veya standart app bar sınırına uygun kısalmalı.
- **Kanıt:** `category_stories.dart`, `stories_grid.dart`; `emulator-branch-search-kaan.png` keşif varyantına görsel referans. Kategori tam sayfa görüntüsü mevcut değil.

### 14. Hikâye bilgisi — `/storyInfo/:id` · G+K

- **Yerleşim:** üstte büyük, neredeyse ekran eninde kapak/hero; sol üstte dairesel geri; hero alt iki köşesinde süre ve yaş pill'leri. Alt açık zeminde ortalanmış 343×48dp gradient CTA, seslendirme anahtarı, açıklama, yazar/seslendiren/resimleyen metadata ve benzer hikâye bölümü.
- **Durum:** ücretsiz CTA okuyucuya gider; premium CTA premium açar, kapakta kilit/bulanıklık vardır. Uzun açıklama sayfayı kaydırır.
- **Alanlar:** `story.cover`, `story.duration`, `story.age`, `action.startOrPremium`, `story.voiceToggle`, `story.summary`, `story.credits[]`, `story.related[]`.
- **Kanıt:** `emulator-haberci-info.png`, `emulator-premium-story.png`; `info_stories.dart`.

### 15. Etkileşimli okuyucu — `/storyContent` · G+K

- **Yerleşim:** tüm ekranı dolduran ZIP sayfa görseli; ayrı UI kontrolleri geçici/overlay olabilir. Görselin içindeki illüstrasyon ve metin aynı medya kompozisyonunun parçası olabilir; sayfa başına hizalama görsel varlığından alınır.
- **Durum:** sayfa geçişi, ikinci ZIP yükleme katmanı, varsa seçim kartı, ses denetimi ve bitiş. Hızlı kaydırma/ZIP sınırı kaynakta hassas; bu belge akışı değiştirmez.
- **Kanıt:** `emulator-haberci-content.png`, `emulator-haberci-page2.png`, `emulator-interactive-page1.png`…`page6.png`; `story_content.dart`. Seçimli dalın gerçek Android görüntüsü yok.

### 16. Hikâye bitişi · K+G

- **Yerleşim:** okuyucudan çıkan tam sayfa tamamlanma görünümü; başarı mesajı, değerlendirme/benzer içerik ve olası premium çağrısı.
- **Kanıt:** `story_last_page_widget.dart`, `emulator-interactive-fast-swipe-final.png` ve önceki runtime raporu. Kanıt görüntüsü tam stabil bitiş kompozisyonu için sınırlı.

## Uyku ve ses

### 17. Uyku ana sekmesi — `/home` sekme 2 · G+K

- **Yerleşim:** 250dp yüksekliğinde `relax_header.jpg` üst alanı ve alta açık renge karışan gradient; ortada iki satıra sarılabilen açık renk başlık. Altında ninni, meditasyon ve sesli hikâye bölümleri; her biri başlık/açıklama/`All` ve yatay medya rayı.
- **Kart farkı:** ninni kartları geniş yatay poster ve alt koyu metin bandı; meditasyon/sesli hikâye kartları dikey kapak. Ücretli kartta kilit rozeti.
- **Kanıt:** `emulator-uyku-loaded.png`, `emulator-uyku-scroll1.png`, `emulator-uyku-scroll2.png`; `relax.dart`.

### 18. Meditasyon ve sesli hikâye bilgisi · G+K

- **Yerleşim:** hikâye bilgisine benzeyen hero, süre/yaş, altta gradient başlatma CTA ve açıklama/metadata. Meditasyon ile sesli hikâye aynı iskeletin içerik türü varyantıdır.
- **Durum:** ücretsiz başlat → ses oynatıcı; kilitli CTA → premium. `action.start` metni kategoriye göre değişir.
- **Kanıt:** `emulator-meditasyon-first.png`, `emulator-sesli-hikaye-first.png`; `meditation_info.dart`.

### 19. Ninni ve ortak ses oynatıcı · G+K

- **Yerleşim:** tam sayfa medya odaklı oynatıcı; kapak görseli, başlık, zaman/progress ve play/pause/çıkış kontrolleri. İçerik sayfası açılınca alt kabuk sekmeleri gizlenir.
- **Davranış:** ninni kartı doğrudan, meditasyon/sesli hikâye bilgi ekranından gelir. Ses otomatik başladı; duraklat/devam ve çıkış runtime'da çalıştı.
- **Kanıt:** `emulator-ninni-dandini.png`, `emulator-ninni-paused.png`, `emulator-meditasyon-player.png`, `emulator-sesli-hikaye-player.png`; `lullaby_content.dart`/ses oynatıcı.

## Ebeveyn modu

### 20. Çocuk/ebeveyn seçimi modalı · G+K

- **Yerleşim:** ana ekran üstündeki profil/mod seçicisinden açılan karartmalı modal; çocuk ve ebeveyn seçenekleri ikonlu büyük hedeflerdir.
- **Kanıt:** `emulator-user-type-dialog.png`, `emulator-parent-dialog.png`; `UserTypeButton`.

### 21. Ebeveyn rehberi vitrini — `/ParentGuidanceBook` · G+K

- **Yerleşim:** üst mod seçimi ve günlük ücretsiz özetin görsel/bilgi kartı; aşağıda yatay kategori listeleri ve içerik kartları. Ana çocuk sekme kabuğundan farklı bağlamdır.
- **Kanıt:** `emulator-design-parent-home.png` (yüklenmiş vitrin), `parent_mode_view.dart`, `free_parent_mode_widget.dart`. Günlük özet açık mavi büyük kart; üstte tam enli premium gradient CTA, altta yatay kilitli rehber kartları.

### 22. Ebeveyn kategori listesi — `/categoryParentGuidanceBooks` · K

- **Yerleşim:** standart app bar ve 16dp yan paylı rehber kart grid'i; kategori başlığı/kapak/lock durumları.
- **Kanıt:** `category_parent_mode.dart`, `parent_mode_grid.dart`; doğrudan Android görüntüsü yok.

### 23. Ebeveyn rehberi bilgisi · K

- **Yerleşim:** kapak/hero, başlık, süre/özet, dinleme veya okuma CTA'sı ve metadata. Günlük ücretsiz içerik ayrı görsel varyant olabilir.
- **Kanıt:** `info_parent_mode.dart`; ayrı detay görüntüsü yok.

### 24. Ebeveyn metin içeriği — `/ParentGuidanceBookContent` · K

- **Yerleşim:** açık mavi tonlu zemin üzerinde uzun metin, 16dp yatay pay, üst navigasyon; uzun kaydırmalı okuma.
- **Kanıt:** `content_parent_mode.dart`; doğrudan Android görüntüsü yok.

### 25. Ebeveyn sesli dinleme · K

- **Yerleşim:** ayrı tam ekran ses oynatıcı; kapak, içerik bilgisi, zaman/progress ve kontroller. Alt sekmeler gizlidir.
- **Kanıt:** `listen_parent_mode.dart`; doğrudan Android görüntüsü yok.

## Profil ve hesap

### 26. Profil — `/profile`, sekme 3 · G+K

- **Yerleşim:** üstte avatar/ad/edit alanı; kayıtlı profilde tamamlanan hikâye ve süre sayaçları, premium paneli, e-posta doğrulama uyarısı; altında bildirim anahtarı, dil, hesap ayarları, geri bildirim, sürüm. Anonimde sayaç yerine büyük üyelik paneli.
- **Kanıt:** `emulator-profile.png`, `emulator-auth-profile.png`; `user_profile.dart`.

### 27. Profil düzenleme — `/profileEdit` · G+K

- **Yerleşim:** standart app bar; `field.childName` dolu alan, görsel/renk avatar seçim bölümü, altta `action.complete`.
- **Kanıt:** `emulator-design-profile-edit.png`, Android UI ağacı (`User` varsayılan adı); `profile_edit.dart`. Avatar seçim alanı bu yakalamada boş göründü; gerçek seçenek görselleri için görsel kanıt yok.

### 28. Dil seçimi — `/changeLanguage` · G+K

- **Yerleşim:** app bar ve dil seçenekleri satırları; mevcut dil seçili görünür. Türkçe/İngilizce/Almanca kullanıcıya sunulur; Portekizce bayrağa bağlı, İspanyolca görünür listede yok.
- **Kanıt:** `emulator-design-language.png`, `change_language.dart`. Bu yakalamada Portekizce de görünüyordu; Remote Config durumu önceki turdan farklıdır.

### 29. Hesap ayarları — `/accountSettings` · G+K

- **Yerleşim:** app bar altında üç tam genişlik satır: parola değiştir, çıkış, hesap sil. Her satır ikon + label + yön oku kullanır; tehlikeli işlem ayrı onay ekranına/akışına gider.
- **Kanıt:** `emulator-account-settings.png`; `account_settings_view.dart`.

### 30. Parola değiştir — `/profileChangePassword` · G+K

- **Yerleşim:** üst app bar; eski ve yeni parola alanları, görünürlük kontrolü ve `Reset Password` birincil düğmesi. İki boş alan da ayrı hata gösterir.
- **Kanıt:** `emulator-change-password.png`; `change_password.dart`.

### 31. Hesap sil — `/deleteAccount` · G+K

- **Yerleşim:** app bar; üstte e-posta `primary` renkte, altında uyarı; parola alanı ve kırmızı tam genişlik son CTA. Silme işlemi bu tasarım çalışmasında tetiklenmez.
- **Kanıt:** `emulator-design-delete-account.png`, `delete_account.dart`.

### 32. Geri bildirim — `/feedback` · G+K

- **Yerleşim:** app bar, üst açıklama, büyük çok satırlı alan, sağ altta `0/200` sayacı ve tam genişlik gönder CTA'sı; gönderim durumuna göre yükleme/başarı/hata.
- **Kanıt:** `emulator-design-feedback.png`, `user_feedback_view.dart`.

### 33. Çıkış onayı modalı · G

- **Yerleşim:** karartma üzerinde ortalanmış açık panel; başlık, açıklama, yan yana `Cancel` ve `Logout`.
- **Kanıt:** `emulator-logout-confirm.png`.

### 34. Premium / RevenueCat yüzeyi · G+K

- **Yerleşim:** uygulama içi premium giriş CTA'sı ve kart kilidi uygulamaya ait; açılan PaywallActivity üçüncü taraf yüzeyidir. Ürün/teklif kompozisyonu emulator Billing hatası nedeniyle tam doğrulanmadı.
- **Durum:** `BILLING_UNAVAILABLE`/`Error 3` modalı doğrulandı. Bu hata ekranı normal paywall tasarımı olarak kopyalanmamalı.
- **Kanıt:** `emulator-paywall.png` (premium hikâye bilgisi), `emulator-after-billing-error.png`; runtime activity kaydı.

## Uzun metin, varlık ve platform kuralları

- Başlıklar ve açıklamalar **anlamsal alan** olarak uygulanır; sabit görüntü metni yalnız kapak/landing illüstrasyonunun bir parçasıysa korunur.
- Tam ekran kahraman görseli, hikâye sayfası, kapak, ikon ve UI arka planı `ASSET-MAP.md` sınıflarıyla ayrılır. Görsel eksikse eş boyutlu placeholder kullanılır; boş alan çökmez.
- Üst/alt sistem çubukları Android ve iOS'ta aynı piksel yüksekliğinde varsayılmaz. `dp` ölçüleri güvenli alan içindir; `hero`/okuyucu medya uygulama görünümüne göre kenara kadar uzanır.
- Tablet boyutu ana yüzeyleri 800×1280dp Android emülatör override ile görüntüyle doğrulandı; diğerleri yalnız kodla bilinir. Gerçek tablet donanımı ve iOS/iPad doğrudan doğrulanmadı. Yüzey bazında [TABLET.md](./TABLET.md) kanıt etiketine bakın.
