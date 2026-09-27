# Ham emülatör yakalamaları envanteri

Proje kökündeki `emulator-*.png` yakalamaları aşağıda **ekran ve durum** bazında eşlenir. Dosya adları bazen tarihsel çalışma bağlamını taşır; kullanıcıya görünen gerçek ekran türü için `SCREENS.md` ve seçilmiş `evidence/` PNG'leri esas alınır. Bu envanter ham koleksiyondaki her dosyayı kapsar; yalnız tasarım kanıtı olan 48 dosya taşınabilir seçkiye kopyalandı. `wait`, `start`, `current`, `relaunch`, `prep` gibi adlar çoğunlukla yükleme/geçiş anıdır, son kompozisyon kanıtı değildir.

## Açılış, onboarding, kayıt ve giriş

| Durum | Ham dosyalar |
|---|---|
| İlk kullanım / tanıtım sayfaları | `emulator-auth-current.png`, `emulator-onboarding-page3.png`, `emulator-onboarding-last-page.png`, `emulator-onboarding-current2.png`, `emulator-onboarding2.png`, `emulator-membership-entry.png`, `emulator-onboarding-popup.png`, `emulator-loggedout-options.png`, `emulator-loggedout-account-options.png`, `emulator-loggedout-current.png`, `emulator-onboarding-anonymous-home.png` |
| Kayıt formu ve koşullar | `emulator-register-screen.png`, `emulator-register-ready-screen.png`, `emulator-register-form-current.png`, `emulator-register-check2.png`, `emulator-register-prep.png`, `emulator-register-popup2.png`, `emulator-register-back-from-gdpr.png`, `emulator-register-toolbar-check.png`, `emulator-register-no-stylus.png`, `emulator-register-no-stylus2.png`, `emulator-after-register.png` |
| Giriş / oturum durumları | `emulator-login-screen.png`, `emulator-login-again.png`, `emulator-after-login.png`, `emulator-after-logout.png`, `emulator-after-logout2.png`, `emulator-forgot-password.png`, `emulator-relaunch-after-register.png` |
| Çocuk profili kurma | `emulator-profile-step1.png`, `emulator-after-profile-skip.png`, `emulator-child-profile-entry.png`, `emulator-child-return.png` |

## Kitaplık, keşif ve okuyucu

| Durum | Ham dosyalar |
|---|---|
| Kitaplık / yeniden yükleme | `emulator-home2.png`, `emulator-relaunch2.png`, `emulator-anonymous-relaunch.png`, `emulator-anonymous-relaunch2.png`, `emulator-anonymous-after-error.png`, `emulator-validation.png` |
| Arama ve sonuç varyantları | `emulator-branch-search.png`, `emulator-branch-search-open.png`, `emulator-branch-search-kaan.png`, `emulator-branch-search-hayal.png`, `emulator-branch-search-hayal-loaded.png`, `emulator-interactive-search.png`, `emulator-interactive-search-info.png` |
| Haberci Nota bilgi/okuma | `emulator-haberci-info.png`, `emulator-haberci-start.png`, `emulator-haberci-content.png`, `emulator-haberci-page2.png` |
| Dallanan hikâye bilgi ve sayfalar | `emulator-branch-info.png`, `emulator-branch-page1.png`, `emulator-branch-page2.png`, `emulator-branch-page3.png`, `emulator-branch-page4.png`, `emulator-branch-page5.png`, `emulator-branch-page6.png`, `emulator-interactive-home.png`, `emulator-interactive-info.png`, `emulator-interactive-start.png`, `emulator-interactive-page1.png`, `emulator-interactive-page2.png`, `emulator-interactive-page3.png`, `emulator-interactive-page4.png`, `emulator-interactive-page5.png`, `emulator-interactive-page6.png`, `emulator-interactive-fast-swipe-final.png` |
| Premium bilgi ve Billing hatası | `emulator-premium-story.png`, `emulator-paywall.png`, `emulator-after-billing-error.png`, `emulator-post-error.png` |

## Uyku, ebeveyn ve hesap

| Durum | Ham dosyalar |
|---|---|
| Uyku ana sekmesi, scroll ve yükleme | `emulator-uyku-tab.png`, `emulator-uyku-start.png`, `emulator-uyku-home.png`, `emulator-uyku-loaded.png`, `emulator-uyku-scroll1.png`, `emulator-uyku-scroll2.png`, `emulator-uyku-after-ninni.png`, `emulator-uyku-list-after-audio.png`, `emulator-relax.png`, `emulator-relax-wait.png` |
| Ninni/meditasyon/sesli hikâye | `emulator-ninni-dandini.png`, `emulator-ninni-paused.png`, `emulator-meditasyon-first.png`, `emulator-meditasyon-player.png`, `emulator-sesli-hikaye-first.png`, `emulator-sesli-hikaye-player.png`, `emulator-uyku-locked-meditasyon.png`, `emulator-uyku-locked-meditasyon-error.png` |
| Ebeveyn modu / seçim | `emulator-parent.png`, `emulator-parent-wait.png`, `emulator-design-parent-home.png`, `emulator-parent-dialog.png`, `emulator-user-type-dialog.png` |
| Profil / ayarlar | `emulator-profile.png`, `emulator-profile-anonymous.png`, `emulator-profile-complete.png`, `emulator-auth-profile.png`, `emulator-design-profile-edit.png`, `emulator-design-language.png`, `emulator-account-settings.png`, `emulator-change-password.png`, `emulator-design-delete-account.png`, `emulator-design-feedback.png`, `emulator-logout-confirm.png` |

İki özel yanlış okuma riski: `emulator-paywall.png` gerçek RevenueCat teklifinin değil premium hikâye bilgi ekranının kanıtıdır; `emulator-parent-wait.png` yükleme anıdır, dolu ebeveyn vitrini `emulator-design-parent-home.png` ile doğrulanır.
