# Kapsam ve kanıt matrisi

Kaynak doğrusu 2026-09-25 tarihinde çalışan Android uygulaması ile Flutter kaynağıdır. `G` = `evidence/` içindeki doğrudan Android görüntüsü, `K` = kaynak kod; `—` = doğrudan görüntü yok. Router'daki **23 yolun tamamı** aşağıdadır. Aynı yolun farklı sekme/durumları ayrıca listelenir. Bu matrisin amacı boşluğu saklamak değil, yeniden üretimde nerede çıkarım gerektiğini göstermektir. iOS ve tablet hiçbir satırda görüntüyle doğrulanmadı.

| Router yolu | Yüzey / SCREENS | Android kanıtı | Kod kanıtı | Durum |
|---|---|---|---|---|
| `/` | 01 splash | — | `splash_screen.dart` | K |
| `/welcome` | 01 splash/yönlendirme | — | `app_routes.dart`, `splash_screen.dart` | K |
| `/landingScreenView` | 02 tanıtım | `emulator-onboarding-page3.png` | `landing_screen.dart` | G+K |
| `/onboarding` | 03 hesap başlangıcı | `emulator-onboarding2.png` | `app_routes.dart` ve auth ekranı | G+K |
| `/enterUserProfile` | 09 profil kurma | `emulator-profile-step1.png` | `setting_up_account_pages.dart` | G+K; tüm beş adım görünmüyor |
| `/registerUser` | 05 kayıt | `emulator-register-screen.png` | `register_user.dart` | G+K |
| `/home` | 11 kitaplık, 17 uyku | `emulator-home2.png`, `emulator-uyku-loaded.png` | `home.dart`, `relax.dart` | G+K |
| `/ParentGuidanceBook` | 21 ebeveyn vitrini | `emulator-design-parent-home.png` | `parent_mode_view.dart` | G+K |
| `/ParentGuidanceBookContent` | 24 ebeveyn metni | — | `content_parent_mode.dart` | K |
| `/categoryParentGuidanceBooks` | 22 ebeveyn kategorisi | — | `category_parent_mode.dart` | K |
| `/profile` | 26 profil | `emulator-profile.png`, `emulator-auth-profile.png` | `user_profile.dart` | G+K |
| `/storyInfo/:id` | 14 hikâye bilgisi | `emulator-haberci-info.png`, `emulator-premium-story.png` | `info_stories.dart` | G+K |
| `/storyContent` | 15 okuyucu | `emulator-haberci-content.png`, `emulator-haberci-page2.png` | `story_content.dart` | G+K; seçim dalı eksik |
| `/profileEdit` | 27 profil düzenleme | `emulator-design-profile-edit.png` | `profile_edit.dart` | G+K; avatar varyantı eksik |
| `/login` | 06 giriş | `emulator-login-screen.png` | `login_user.dart` | G+K |
| `/categoryStories` | 13 hikâye kategorisi | — | `category_stories.dart` | K |
| `/changeLanguage` | 28 dil | `emulator-design-language.png` | `change_language.dart` | G+K |
| `/deleteAccount` | 31 hesap silme | `emulator-design-delete-account.png` | `delete_account.dart` | G+K; silme tetiklenmedi |
| `/profileChangePassword` | 30 parola değiştirme | `emulator-change-password.png` | `change_password.dart` | G+K |
| `/authAction` | 08 e-posta bağlantı sonucu | — | `email_verify_screen.dart`, `reset_password_screen.dart` | K; test postası alınamadı |
| `/sendResetPassword` | 07 sıfırlama isteği | `emulator-forgot-password.png` | `send_reset_password.dart` | G+K |
| `/accountSettings` | 29 hesap ayarları | `emulator-account-settings.png` | `account_settings_view.dart` | G+K |
| `/feedback` | 32 geri bildirim | `emulator-design-feedback.png` | `user_feedback_view.dart` | G+K |

## Router dışı yüzeyler ve önemli durumlar

| Yüzey | Kanıt | Sonuç / eksik |
|---|---|---|
| 04 hesap seçenekleri paneli | `emulator-onboarding-popup.png` | G; Google/Apple sağlayıcı sonuçları gözlenmedi |
| 10 PDF görüntüleyici | `pdf_viewer_screen.dart` | K; kalıcı temiz görüntü yok |
| 12 arama | `emulator-branch-search-open.png`, `emulator-branch-search-kaan.png` | G; boş ve sonuçlu durum |
| 16 hikâye bitişi | `emulator-interactive-fast-swipe-final.png` | K + sınırlı G; stabil bitişi tekrar yakalamak gerekir |
| 18 meditasyon bilgisi | `emulator-meditasyon-first.png` | G |
| 18 sesli hikâye bilgisi | `emulator-sesli-hikaye-first.png` | G |
| 19 ninni oynatıcı | `emulator-ninni-dandini.png`, `emulator-ninni-paused.png` | G; play/pause |
| 19 meditasyon/sesli hikâye oynatıcı | `emulator-meditasyon-player.png`, `emulator-sesli-hikaye-player.png` | G |
| 20 çocuk/ebeveyn modalı | `emulator-user-type-dialog.png` | G |
| 23 ebeveyn rehberi bilgi sayfası | `info_parent_mode.dart` | K; detay görüntüsü yok |
| 25 ebeveyn dinleme | `listen_parent_mode.dart` | K; oynatıcı görüntüsü yok |
| 33 çıkış onayı | `emulator-logout-confirm.png` | G; onay eylemi ayrıca eski akış çalışmasında denendi |
| 34 RevenueCat teklif ekranı | `emulator-after-billing-error.png` | Hata G; teklif kompozisyonu gözlenemedi |
| Premium hikâye bilgi/kilit | `emulator-premium-story.png` | G; uygulamaya ait, RevenueCat ekranı değil |
| Uyku kilitli meditasyon | `emulator-uyku-locked-meditasyon.png` | G; Billing hatası ayrıca görüldü |

Telefon görüntülerinin ham koleksiyonu proje kökündeki **105** `emulator-*.png` dosyasıdır; [RAW-SCREENSHOTS.md](./RAW-SCREENSHOTS.md) hepsini eşler. Tablet için ayrıca [TABLET.md](./TABLET.md) ve sekiz `evidence/tablet-emulated-*.png` yakalaması bulunur. Tablet kanıtı, gerçek tablet donanımı değil 800×1280dp ekran override'ıdır. `SCREENS.md` tarifi olmayan yüzey yoktur; yalnız K olan satırlar piksel düzeyinde yeniden oluşturma için açık kanıt boşluğudur. Crashlytics kapsam dışındadır.
