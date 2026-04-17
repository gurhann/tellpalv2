export type TranslationParams = Record<
  string,
  string | number | boolean | null | undefined
>;

export type MessageValue = string | ((params?: TranslationParams) => string);

export const enMessages = {
  "app.language": "Language",
  "app.locale.en": "English",
  "app.locale.tr": "Turkish",
  "app.openNavigation": "Open navigation",
  "app.cmsNavigation": "CMS navigation",
  "app.goBack": "Go back",
  "app.retry": "Retry",
  "app.bytesUnit": "bytes",
  "app.requestId": "Request ID",
  "app.unknownError": "Something went wrong.",
  "nav.contents.label": "Contents",
  "nav.contents.description": "Editorial records and localization flows.",
  "nav.categories.label": "Categories",
  "nav.categories.description": "Category metadata and curation workspaces.",
  "nav.contributors.label": "Contributors",
  "nav.contributors.description": "Credits, names, roles, and assignments.",
  "nav.freeAccess.label": "Free Access",
  "nav.freeAccess.description": "Access keys and grant visibility.",
  "nav.media.label": "Media",
  "nav.media.description": "Advanced asset registry, previews, and debug.",
  "nav.mediaProcessing.label": "Media Processing",
  "nav.mediaProcessing.description": "Packaging state and retry operations.",
  "layout.brand": "TellPal CMS",
  "layout.workspaceTitle": "Editorial Workspace",
  "layout.workspaceDescription":
    "Route skeleton for the CMS shell, navigation, and protected layout.",
  "route.contentsDetail.title": "Content Detail",
  "route.contentsDetail.description":
    "Edit core metadata, localizations, and publication actions.",
  "route.storyPages.title": "Story Pages",
  "route.storyPages.description":
    "Manage story page structure and localized page payloads.",
  "route.categoryDetail.title": "Category Detail",
  "route.categoryDetail.description":
    "Manage category metadata, localizations, and curation.",
  "route.workspace.title": "Workspace",
  "route.workspace.description": "Admin route skeleton for the CMS workspace.",
  "auth.loading.title": "Restoring admin session",
  "auth.loading.description":
    "Checking the stored refresh token and rebuilding workspace access before routing.",
  "auth.loading.wait": "Please wait while the CMS verifies your session.",
  "auth.login.heroTitle": "Editorial operations, one secure workspace",
  "auth.login.heroDescription":
    "This workspace gives editors controlled access to content, categories, assets, processing jobs, contributors, and free-access rules.",
  "auth.login.sessionModelTitle": "Session model",
  "auth.login.sessionModel.accessToken":
    "Access token stays memory-only on the client.",
  "auth.login.sessionModel.refresh":
    "Refresh restores the session during the next app boot.",
  "auth.login.sessionModel.unauthorized":
    "Unauthorized requests clear session state centrally.",
  "auth.login.routeTargetTitle": "Current route target",
  "auth.login.routeTargetDescription":
    "After a successful sign-in the app will continue to {targetPath}. Return-to-target navigation is preserved for protected routes.",
  "auth.login.adminAccess": "Admin Access",
  "auth.login.formTitle": "Sign in to TellPal CMS",
  "auth.login.formDescription":
    "Use your admin username and password. A valid refresh token will be stored locally to restore the session on the next app boot.",
  "auth.login.username": "Username",
  "auth.login.password": "Password",
  "auth.login.passwordPlaceholder": "Enter your password",
  "auth.login.sessionBehavior": "Session behavior",
  "auth.login.sessionBehavior.storage":
    "Access token stays in memory and refresh token stays in local storage.",
  "auth.login.sessionBehavior.target":
    "After sign-in you will land on {targetPath}.",
  "auth.login.submit": "Sign in",
  "auth.login.pending": "Signing in...",
  "auth.login.reviewAdminApi": "Review admin API",
  "auth.logout": "Log out",
  "auth.logout.pending": "Signing out...",
  "auth.error.incorrectCredentials": "Incorrect credentials",
  "auth.error.incorrectCredentialsDetail":
    "Username or password is incorrect. Check the credentials and try again.",
  "auth.error.accountDisabled": "Account disabled",
  "auth.error.accountDisabledDetail":
    "This admin account is disabled. Contact an operator to restore access.",
  "auth.error.signInFailed": "Sign-in could not be completed. Try again.",
  "data.loadingTitle": "Loading records",
  "data.loadingDescription":
    "The workspace is requesting the latest data for this table.",
  "data.emptyTitle": "No records found",
  "data.emptyDescription":
    "Adjust the current filters or create the first record for this workspace.",
  "language.tabs.label": "Language tabs",
  "language.emptyTitle": "No languages available",
  "language.emptyDescription":
    "Add or enable at least one language before opening a localized workspace.",
  "language.label.english": "English",
  "language.label.turkish": "Turkish",
  "language.label.spanish": "Spanish",
  "language.label.portuguese": "Portuguese",
  "language.label.german": "German",
  "placeholder.minimalTitle": "This page is intentionally minimal.",
  "placeholder.minimalDescription":
    "The route shell is in place and can be expanded with module specific data, forms, and backend actions as the workspace grows.",
  "placeholder.responsibilitiesTitle": "Available responsibilities",
  "placeholder.responsibilitiesDescription":
    "This route is intended to host the following capabilities.",
  "notFound.title": "Route not found",
  "notFound.description":
    "The requested CMS route does not exist in the current skeleton.",
  "assets.assetDetailFallback": "Asset detail",
  "assets.notAvailable": "Not available",
  "assets.assetIdentity": "Asset identity",
  "assets.provider": "Provider",
  "assets.created": "Created {value}",
  "assets.cachedDownloadUrl": "Cached download URL",
  "assets.available": "Available",
  "assets.notCached": "Not cached",
  "assets.expires": "Expires {value}",
  "assets.metadataSnapshot": "Metadata snapshot",
  "assets.metadataPresent": "Metadata present",
  "assets.metadataPending": "Metadata pending",
  "assets.updated": "Updated {value}",
  "assets.metadataTitle": "Metadata",
  "assets.metadataDescription":
    "Update MIME type, byte size, and checksum while keeping provider and object path read-only.",
  "assets.cachedUrlTitle": "Cached download URL",
  "assets.cachedUrlDescription":
    "Refresh the stored signed URL snapshot without leaving the asset detail workspace.",
  "assets.lastCached": "Last cached: {value}",
  "assets.loadingDetail": "Loading asset detail",
  "assets.loadingDetailDescription":
    "The CMS is requesting the selected asset record from the admin API.",
  "assets.noAssetSelected": "No asset selected",
  "assets.noAssetSelectedDescription":
    "Select an asset from the recent registry to inspect its metadata.",
  "assets.previewTitle": "Preview",
  "assets.previewDescription":
    "Inspect image assets inline or play audio assets directly from the signed Firebase download URL snapshot.",
  "assets.previewKind.image": "Image preview",
  "assets.previewKind.audio": "Audio preview",
  "assets.previewKind.unavailable": "Preview unavailable",
  "assets.previewLastRefreshed": "Last refreshed: {value}",
  "assets.previewExpires": "Expires: {value}",
  "assets.previewUnavailableArchiveTitle":
    "Preview unavailable for archive assets",
  "assets.previewUnavailableArchiveDescription":
    "Archive assets stay inspectable through metadata and cached download URL controls, but they do not render inline previews.",
  "assets.previewLoadingTitle": "Loading preview",
  "assets.previewLoadingDescription":
    "The CMS is preparing a fresh signed URL before rendering the asset preview.",
  "assets.previewLoadFailedTitle": "Preview could not be loaded",
  "assets.previewLoadFailedDescription":
    "The preview request failed before the browser could load the asset.",
  "assets.previewLoadBrowserDescription":
    "The browser could not render the signed asset URL. Refresh the preview to request one more signed URL snapshot.",
  "assets.retryPreview": "Retry preview",
  "assets.imageAlt": "Preview of asset #{assetId}",
  "assets.audioAria": "Audio preview for asset #{assetId}",
  "assets.previewUnavailableDescription":
    "No preview URL is currently ready for this asset.",
  "assets.table.asset": "Asset",
  "assets.table.kind": "Kind",
  "assets.table.provider": "Provider",
  "assets.table.mediaType": "Media Type",
  "assets.table.mimeType": "MIME Type",
  "assets.table.assetId": "Asset #{assetId}",
  "assets.table.metadataPending": "Metadata pending",
  "assets.table.byteSizeUnavailable": "Byte size unavailable",
  "assets.table.emptyTitle": "No media assets yet",
  "assets.table.emptyDescription":
    "No media assets are available yet. Asset registration and picker flows arrive in the next asset tasks.",
  "assets.table.errorTitle": "Asset registry unavailable",
  "assets.table.errorDescription":
    "The recent asset registry could not be loaded from the admin API.",
  "assets.table.loadingTitle": "Loading asset registry",
  "assets.table.loadingDescription":
    "The CMS is requesting recent media asset metadata from the admin API.",
  "assets.table.summaryTitle":
    "{count} {count, plural, one {asset} other {assets}}",
  "assets.table.summaryDescription": "{images} images / {cached} cached URLs",
  "assets.table.toolbarTitle": "Asset registry",
  "assets.table.toolbarDescription":
    "Recent asset metadata is now bound to the shared library table.",
  "route.uiLabs.title": "UI Labs",
  "route.uiLabs.description":
    "Hidden prototype routes for comparing CMS layout and UX variants.",
  "route.mockupLabs.title": "Variant A Mockups",
  "route.mockupLabs.description":
    "Hidden fixture-backed routes that preview the winning Variant A shell.",
} as const satisfies Record<string, MessageValue>;

export const trMessages = {
  "app.language": "Dil",
  "app.locale.en": "İngilizce",
  "app.locale.tr": "Türkçe",
  "app.openNavigation": "Navigasyonu aç",
  "app.cmsNavigation": "CMS navigasyonu",
  "app.goBack": "Geri dön",
  "app.retry": "Tekrar dene",
  "app.bytesUnit": "bayt",
  "app.requestId": "İstek Kimliği",
  "app.unknownError": "Bir şeyler yanlış gitti.",
  "nav.contents.label": "İçerikler",
  "nav.contents.description": "Editoryal kayıtlar ve yerelleştirme akışları.",
  "nav.categories.label": "Kategoriler",
  "nav.categories.description":
    "Kategori metadata ve kürasyon çalışma alanları.",
  "nav.contributors.label": "Katkıda Bulunanlar",
  "nav.contributors.description": "Krediler, isimler, roller ve atamalar.",
  "nav.freeAccess.label": "Ücretsiz Erişim",
  "nav.freeAccess.description": "Erişim anahtarları ve görünürlük izinleri.",
  "nav.media.label": "Medya",
  "nav.media.description":
    "Gelişmiş asset kayıtları, önizlemeler ve hata ayıklama.",
  "nav.mediaProcessing.label": "Medya İşleme",
  "nav.mediaProcessing.description":
    "Paketleme durumu ve yeniden deneme işlemleri.",
  "layout.brand": "TellPal CMS",
  "layout.workspaceTitle": "Editoryal Çalışma Alanı",
  "layout.workspaceDescription":
    "CMS kabuğu, navigasyon ve korumalı yerleşim için rota iskeleti.",
  "route.contentsDetail.title": "İçerik Detayı",
  "route.contentsDetail.description":
    "Temel metadata, yerelleştirmeler ve yayın aksiyonlarını düzenleyin.",
  "route.storyPages.title": "Hikâye Sayfaları",
  "route.storyPages.description":
    "Hikâye sayfa yapısını ve yerelleştirilmiş sayfa içeriklerini yönetin.",
  "route.categoryDetail.title": "Kategori Detayı",
  "route.categoryDetail.description":
    "Kategori metadata, yerelleştirmeler ve kürasyonu yönetin.",
  "route.workspace.title": "Çalışma Alanı",
  "route.workspace.description":
    "CMS çalışma alanı için yönetici rota iskeleti.",
  "auth.loading.title": "Yönetici oturumu geri yükleniyor",
  "auth.loading.description":
    "Yönlendirme öncesinde kayıtlı refresh token kontrol ediliyor ve çalışma alanı erişimi yeniden kuruluyor.",
  "auth.loading.wait": "CMS oturumunuzu doğrularken lütfen bekleyin.",
  "auth.login.heroTitle": "Tek güvenli çalışma alanında editoryal operasyonlar",
  "auth.login.heroDescription":
    "Bu çalışma alanı editörlere içerik, kategoriler, asset’ler, işleme işleri, katkıda bulunanlar ve ücretsiz erişim kuralları için kontrollü erişim sağlar.",
  "auth.login.sessionModelTitle": "Oturum modeli",
  "auth.login.sessionModel.accessToken":
    "Access token istemci tarafında yalnızca bellekte tutulur.",
  "auth.login.sessionModel.refresh":
    "Refresh token uygulamanın bir sonraki açılışında oturumu geri getirir.",
  "auth.login.sessionModel.unauthorized":
    "Yetkisiz istekler oturum durumunu merkezi olarak temizler.",
  "auth.login.routeTargetTitle": "Geçerli rota hedefi",
  "auth.login.routeTargetDescription":
    "Başarılı girişten sonra uygulama {targetPath} yoluna devam eder. Korunan rotalar için hedefe geri dönüş korunur.",
  "auth.login.adminAccess": "Yönetici Erişimi",
  "auth.login.formTitle": "TellPal CMS’e giriş yapın",
  "auth.login.formDescription":
    "Yönetici kullanıcı adınızı ve parolanızı kullanın. Geçerli bir refresh token yerel olarak saklanır ve uygulamanın bir sonraki açılışında oturumu geri getirir.",
  "auth.login.username": "Kullanıcı adı",
  "auth.login.password": "Parola",
  "auth.login.passwordPlaceholder": "Parolanızı girin",
  "auth.login.sessionBehavior": "Oturum davranışı",
  "auth.login.sessionBehavior.storage":
    "Access token bellekte, refresh token ise yerel depolamada tutulur.",
  "auth.login.sessionBehavior.target":
    "Girişten sonra {targetPath} yoluna yönlendirileceksiniz.",
  "auth.login.submit": "Giriş yap",
  "auth.login.pending": "Giriş yapılıyor...",
  "auth.login.reviewAdminApi": "Yönetici API’sini incele",
  "auth.logout": "Çıkış yap",
  "auth.logout.pending": "Çıkış yapılıyor...",
  "auth.error.incorrectCredentials": "Hatalı bilgiler",
  "auth.error.incorrectCredentialsDetail":
    "Kullanıcı adı veya parola yanlış. Bilgileri kontrol edip tekrar deneyin.",
  "auth.error.accountDisabled": "Hesap devre dışı",
  "auth.error.accountDisabledDetail":
    "Bu yönetici hesabı devre dışı. Erişimi geri yüklemek için bir operatöre ulaşın.",
  "auth.error.signInFailed": "Giriş işlemi tamamlanamadı. Tekrar deneyin.",
  "data.loadingTitle": "Kayıtlar yükleniyor",
  "data.loadingDescription":
    "Bu tablo için en güncel veriler çalışma alanı tarafından isteniyor.",
  "data.emptyTitle": "Kayıt bulunamadı",
  "data.emptyDescription":
    "Mevcut filtreleri ayarlayın veya bu çalışma alanı için ilk kaydı oluşturun.",
  "language.tabs.label": "Dil sekmeleri",
  "language.emptyTitle": "Kullanılabilir dil yok",
  "language.emptyDescription":
    "Yerelleştirilmiş çalışma alanını açmadan önce en az bir dili ekleyin veya etkinleştirin.",
  "language.label.english": "İngilizce",
  "language.label.turkish": "Türkçe",
  "language.label.spanish": "İspanyolca",
  "language.label.portuguese": "Portekizce",
  "language.label.german": "Almanca",
  "placeholder.minimalTitle": "Bu sayfa bilinçli olarak minimal tutuldu.",
  "placeholder.minimalDescription":
    "Rota kabuğu hazır ve çalışma alanı büyüdükçe modüle özel veri, form ve backend aksiyonlarıyla genişletilebilir.",
  "placeholder.responsibilitiesTitle": "Mevcut sorumluluklar",
  "placeholder.responsibilitiesDescription":
    "Bu rota aşağıdaki yetenekleri barındırmak üzere tasarlanmıştır.",
  "notFound.title": "Rota bulunamadı",
  "notFound.description": "İstenen CMS rotası mevcut iskelette bulunmuyor.",
  "assets.assetDetailFallback": "Asset detayı",
  "assets.notAvailable": "Mevcut değil",
  "assets.assetIdentity": "Asset kimliği",
  "assets.provider": "Sağlayıcı",
  "assets.created": "Oluşturuldu {value}",
  "assets.cachedDownloadUrl": "Önbellekteki indirme URL’si",
  "assets.available": "Hazır",
  "assets.notCached": "Önbelleğe alınmamış",
  "assets.expires": "Bitiş {value}",
  "assets.metadataSnapshot": "Metadata özeti",
  "assets.metadataPresent": "Metadata mevcut",
  "assets.metadataPending": "Metadata bekleniyor",
  "assets.updated": "Güncellendi {value}",
  "assets.metadataTitle": "Metadata",
  "assets.metadataDescription":
    "Provider ve object path salt okunur kalırken MIME type, byte size ve checksum alanlarını güncelleyin.",
  "assets.cachedUrlTitle": "Önbellekteki indirme URL’si",
  "assets.cachedUrlDescription":
    "Asset detay çalışma alanından ayrılmadan saklanan signed URL özetini yenileyin.",
  "assets.lastCached": "Son önbellekleme: {value}",
  "assets.loadingDetail": "Asset detayı yükleniyor",
  "assets.loadingDetailDescription":
    "Seçilen asset kaydı yönetici API’sinden isteniyor.",
  "assets.noAssetSelected": "Asset seçilmedi",
  "assets.noAssetSelectedDescription":
    "Metadata’sını incelemek için son kayıt listesinden bir asset seçin.",
  "assets.previewTitle": "Önizleme",
  "assets.previewDescription":
    "Görsel asset’leri satır içinde inceleyin veya signed Firebase indirme URL’si üzerinden ses asset’lerini doğrudan oynatın.",
  "assets.previewKind.image": "Görsel önizleme",
  "assets.previewKind.audio": "Ses önizleme",
  "assets.previewKind.unavailable": "Önizleme yok",
  "assets.previewLastRefreshed": "Son yenileme: {value}",
  "assets.previewExpires": "Bitiş: {value}",
  "assets.previewUnavailableArchiveTitle": "Arşiv asset’leri için önizleme yok",
  "assets.previewUnavailableArchiveDescription":
    "Arşiv asset’leri metadata ve önbellekteki indirme URL’si üzerinden incelenebilir, ancak satır içinde önizlenmez.",
  "assets.previewLoadingTitle": "Önizleme yükleniyor",
  "assets.previewLoadingDescription":
    "CMS asset önizlemesini render etmeden önce yeni bir signed URL hazırlıyor.",
  "assets.previewLoadFailedTitle": "Önizleme yüklenemedi",
  "assets.previewLoadFailedDescription":
    "Tarayıcı asset’i yüklemeden önce önizleme isteği başarısız oldu.",
  "assets.previewLoadBrowserDescription":
    "Tarayıcı signed asset URL’sini render edemedi. Yeni bir signed URL özeti istemek için önizlemeyi yenileyin.",
  "assets.retryPreview": "Önizlemeyi tekrar dene",
  "assets.imageAlt": "Asset #{assetId} önizlemesi",
  "assets.audioAria": "Asset #{assetId} için ses önizlemesi",
  "assets.previewUnavailableDescription":
    "Bu asset için şu anda hazır bir önizleme URL’si yok.",
  "assets.table.asset": "Asset",
  "assets.table.kind": "Tür",
  "assets.table.provider": "Sağlayıcı",
  "assets.table.mediaType": "Medya Türü",
  "assets.table.mimeType": "MIME Türü",
  "assets.table.assetId": "Asset #{assetId}",
  "assets.table.metadataPending": "Metadata bekleniyor",
  "assets.table.byteSizeUnavailable": "Bayt boyutu yok",
  "assets.table.emptyTitle": "Henüz medya asset’i yok",
  "assets.table.emptyDescription":
    "Henüz kullanılabilir medya asset’i yok. Asset kayıt ve picker akışları sonraki asset görevlerinde genişletilecek.",
  "assets.table.errorTitle": "Asset kayıt listesi kullanılamıyor",
  "assets.table.errorDescription":
    "Son asset kayıt listesi yönetici API’sinden yüklenemedi.",
  "assets.table.loadingTitle": "Asset kayıt listesi yükleniyor",
  "assets.table.loadingDescription":
    "CMS, son medya asset metadata’sını yönetici API’sinden istiyor.",
  "assets.table.summaryTitle":
    "{count} {count, plural, one {asset} other {asset}}",
  "assets.table.summaryDescription":
    "{images} görsel / {cached} önbelleklenmiş URL",
  "assets.table.toolbarTitle": "Asset kayıt listesi",
  "assets.table.toolbarDescription":
    "Son asset metadata’sı artık paylaşılan kütüphane tablosuna bağlı.",
  "route.uiLabs.title": "UI Labs",
  "route.uiLabs.description":
    "CMS duzen ve deneyim varyantlarini karsilastiran gizli prototip rotalari.",
  "route.mockupLabs.title": "Variant A Mockups",
  "route.mockupLabs.description":
    "Kazanan Variant A kabugunu gosteren gizli fixture tabanli rotalar.",
} as const satisfies Record<keyof typeof enMessages, MessageValue>;

export type TranslationKey = keyof typeof enMessages;
