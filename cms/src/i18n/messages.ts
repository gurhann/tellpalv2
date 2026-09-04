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
  "app.notAvailable": "—",
  "nav.contents.label": "Contents",
  "nav.contents.description": "Editorial records and localization flows.",
  "nav.categories.label": "Categories",
  "nav.categories.description": "Category metadata and curation workspaces.",
  "nav.contributors.label": "Contributors",
  "nav.contributors.description": "Credits, names, roles, and assignments.",
  "contributors.title": "Contributors",
  "contributors.description": "Manage contributor records, roles, and usage.",
  "contributors.searchLabel": "Search contributors",
  "contributors.searchPlaceholder": "Search by display name",
  "contributors.roleLabel": "Role",
  "contributors.allRoles": "All roles",
  "contributors.refresh": "Refresh",
  "contributors.create": "Create contributor",
  "contributors.total": "{count} records",
  "contributors.page": "Page {page}",
  "contributors.table": "Contributor table",
  "contributors.columnContributor": "Contributor",
  "contributors.columnInitials": "Initials",
  "contributors.columnRoles": "Roles",
  "contributors.columnUsage": "Usage",
  "contributors.columnUpdated": "Updated",
  "contributors.actions": "Actions",
  "contributors.rename": "Rename",
  "contributors.delete": "Delete",
  "contributors.clearFilters": "Clear filters",
  "contributors.idLabel": "Contributor #{id}",
  "contributors.renameAria": "Rename {name}",
  "contributors.deleteAria": "Delete {name}",
  "contributors.emptyTitle": "No contributors yet",
  "contributors.emptyDescription":
    "Create the first contributor or connect a backend environment that already has contributor records.",
  "contributors.loadingTitle": "Loading contributors",
  "contributors.loadingDescription":
    "The CMS is requesting contributor registry entries from the admin API.",
  "contributors.role.author": "Author",
  "contributors.role.illustrator": "Illustrator",
  "contributors.role.narrator": "Narrator",
  "contributors.role.musician": "Musician",
  "contributors.form.createTitle": "Create contributor",
  "contributors.form.editTitle": "Edit contributor",
  "contributors.form.displayName": "Display name",
  "contributors.form.roles": "Roles",
  "contributors.form.cancel": "Cancel",
  "contributors.form.createDescription":
    "Register a new contributor in the shared editorial registry. The same record becomes available anywhere content credits are assigned.",
  "contributors.form.editDescription":
    "Update the shared display name shown across contributor pickers and future credit assignment workflows.",
  "contributors.form.createSubmit": "Create contributor",
  "contributors.form.editSubmit": "Save rename",
  "contributors.form.createPending": "Creating contributor...",
  "contributors.form.editPending": "Saving contributor...",
  "contributors.form.createSuccess": "Contributor created.",
  "contributors.form.editSuccess": "Contributor updated.",
  "contributors.form.displayNamePlaceholder": "Annie Case",
  "contributors.form.displayNameHint":
    "Names are trimmed before submit and appear in the shared contributor registry immediately after save.",
  "contributors.form.genericError":
    "Contributor changes could not be saved. Try again.",
  "contributors.form.validation.displayNameRequired":
    "Display name is required.",
  "contributors.form.validation.displayNameTooLong":
    "Display name must be 120 characters or fewer.",
  "contributors.form.validation.rolesRequired": "Select at least one role.",
  "contributors.picker.title": "Assign contributor",
  "contributors.picker.description":
    "Choose an existing {role} or create one without leaving this content.",
  "contributors.picker.searchLabel": "Search {role}s",
  "contributors.picker.searchPlaceholder": "Search by display name",
  "contributors.picker.resultsLabel": "Matching contributors",
  "contributors.picker.loading": "Loading role-scoped contributors...",
  "contributors.picker.emptyTitle": "No matching contributors",
  "contributors.picker.emptyDescription":
    "No {role} matches “{name}”. You can create this contributor and assign them now.",
  "contributors.picker.createAndAssign": "Create and assign",
  "contributors.picker.assign": "Assign contributor",
  "contributors.picker.assignPending": "Assigning contributor...",
  "contributors.picker.assignSuccess": "Contributor assigned.",
  "contributors.picker.assignError": "Assignment failed. Try again.",
  "contributors.picker.createError":
    "Contributor could not be created. Try again.",
  "contributors.picker.duplicateUseExisting":
    "This name already exists. Select the existing contributor from the results.",
  "contributors.picker.nameRequired": "Enter a contributor name.",
  "contributors.picker.creditName": "Credit name",
  "contributors.picker.scope": "Credit scope",
  "contributors.picker.allLanguages": "All languages",
  "contributors.picker.creditNamePlaceholder": "Optional credit override",
  "contributors.picker.scopeHint":
    "The default scope is applied for this role.",
  "contributors.picker.cancel": "Cancel",
  "contributors.picker.retry": "Retry assignment",
  "contributors.picker.errorTitle": "Contributor assignment failed",
  "contributors.panel.title": "Contributor credits",
  "contributors.panel.summary": "{count} assignments",
  "contributors.panel.summaryOne": "1 assignment",
  "contributors.panel.openRegistry": "Open registry",
  "contributors.panel.loading": "Loading contributor assignments...",
  "contributors.panel.emptyTitle": "No contributor assignments yet",
  "contributors.panel.emptyDescription":
    "Add credits from a role-specific contributor group.",
  "contributors.panel.noAssignments": "No assignments in this role.",
  "contributors.panel.addSuffix": "add",
  "contributors.panel.assignFirst": "Assign contributor",
  "contributors.panel.moveUp": "Move assignment up",
  "contributors.panel.moveDown": "Move assignment down",
  "contributors.panel.reorderError":
    "The order could not be saved. The previous order was restored.",
  "contributors.unassign.action": "Unassign",
  "contributors.unassign.aria": "Unassign {name}",
  "contributors.unassign.title": "Unassign contributor",
  "contributors.unassign.description":
    "This removes only the selected role and language scope from the current content item.",
  "contributors.unassign.scope": "{role} / {language} / Sort {sort}",
  "contributors.unassign.cancel": "Cancel",
  "contributors.unassign.confirm": "Unassign contributor",
  "contributors.unassign.loading": "Removing contributor assignment...",
  "contributors.unassign.pending": "Removing...",
  "contributors.unassign.success": "Contributor assignment removed.",
  "contributors.unassign.error":
    "The contributor assignment could not be removed.",
  "contributors.delete.title": "Delete contributor",
  "contributors.delete.description":
    "This removes the shared contributor record. Content assignments must be cleared first.",
  "contributors.delete.loading": "Deleting contributor...",
  "contributors.delete.success": "Contributor deleted.",
  "contributors.delete.inUse":
    "Remove the contributor from content assignments first.",
  "contributors.delete.error": "The contributor could not be deleted.",
  "contributors.delete.pending": "Deleting...",
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
    "Legacy signed URL metadata remains visible for mobile/public delivery diagnostics.",
  "assets.lastCached": "Last cached: {value}",
  "assets.loadingDetail": "Loading asset detail",
  "assets.loadingDetailDescription":
    "The CMS is requesting the selected asset record from the admin API.",
  "assets.noAssetSelected": "No asset selected",
  "assets.noAssetSelectedDescription":
    "Select an asset from the recent registry to inspect its metadata.",
  "assets.previewTitle": "Preview",
  "assets.previewDescription":
    "Inspect image assets inline or play audio assets through the backend preview stream.",
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
    "The CMS is preparing a backend preview token before rendering the asset.",
  "assets.previewLoadFailedTitle": "Preview could not be loaded",
  "assets.previewLoadFailedDescription":
    "The preview request failed before the browser could load the asset.",
  "assets.previewLoadBrowserDescription":
    "The browser could not render the backend preview stream. Refresh the preview to request a new preview token.",
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
  "app.notAvailable": "—",
  "nav.contents.label": "İçerikler",
  "nav.contents.description": "Editoryal kayıtlar ve yerelleştirme akışları.",
  "nav.categories.label": "Kategoriler",
  "nav.categories.description":
    "Kategori metadata ve kürasyon çalışma alanları.",
  "nav.contributors.label": "Katkıda Bulunanlar",
  "nav.contributors.description": "Krediler, isimler, roller ve atamalar.",
  "contributors.title": "Katkıda Bulunanlar",
  "contributors.description":
    "Katkıda bulunan kayıtlarını, rollerini ve kullanımını yönetin.",
  "contributors.searchLabel": "Katkıda bulunan ara",
  "contributors.searchPlaceholder": "Görünen ada göre ara",
  "contributors.roleLabel": "Rol",
  "contributors.allRoles": "Tüm roller",
  "contributors.refresh": "Yenile",
  "contributors.create": "Katkıda bulunan oluştur",
  "contributors.total": "{count} kayıt",
  "contributors.page": "Sayfa {page}",
  "contributors.table": "Katkıda bulunan tablosu",
  "contributors.columnContributor": "Katkıda bulunan",
  "contributors.columnInitials": "Baş harfler",
  "contributors.columnRoles": "Roller",
  "contributors.columnUsage": "Kullanım",
  "contributors.columnUpdated": "Güncellendi",
  "contributors.actions": "Eylemler",
  "contributors.rename": "Düzenle",
  "contributors.delete": "Sil",
  "contributors.clearFilters": "Filtreleri temizle",
  "contributors.idLabel": "Katkıda bulunan #{id}",
  "contributors.renameAria": "{name} adını düzenle",
  "contributors.deleteAria": "{name} kaydını sil",
  "contributors.emptyTitle": "Katkıda bulunan bulunamadı",
  "contributors.emptyDescription":
    "İlk katkıda bulunan kaydını oluşturun veya kayıt içeren bir backend ortamına bağlanın.",
  "contributors.loadingTitle": "Katkıda bulunanlar yükleniyor",
  "contributors.loadingDescription":
    "Katkıda bulunan kayıtları yönetici API’sinden isteniyor.",
  "contributors.role.author": "Yazar",
  "contributors.role.illustrator": "İllüstratör",
  "contributors.role.narrator": "Anlatıcı",
  "contributors.role.musician": "Müzisyen",
  "contributors.form.createTitle": "Katkıda bulunan oluştur",
  "contributors.form.editTitle": "Katkıda bulunanı düzenle",
  "contributors.form.displayName": "Görünen ad",
  "contributors.form.roles": "Roller",
  "contributors.form.cancel": "İptal",
  "contributors.form.createDescription":
    "Paylaşılan editoryal kayıt defterine yeni bir katkıda bulunan ekleyin. Aynı kayıt, içerik kredilerinin atandığı her yerde kullanılabilir.",
  "contributors.form.editDescription":
    "Katkıda bulunan seçimlerinde ve gelecekteki kredi atama akışlarında gösterilen görünen adı güncelleyin.",
  "contributors.form.createSubmit": "Katkıda bulunan oluştur",
  "contributors.form.editSubmit": "Değişiklikleri kaydet",
  "contributors.form.createPending": "Katkıda bulunan oluşturuluyor...",
  "contributors.form.editPending": "Katkıda bulunan kaydediliyor...",
  "contributors.form.createSuccess": "Katkıda bulunan oluşturuldu.",
  "contributors.form.editSuccess": "Katkıda bulunan güncellendi.",
  "contributors.form.displayNamePlaceholder": "Ayşe Yılmaz",
  "contributors.form.displayNameHint":
    "İsimler gönderimden önce kırpılır ve kaydedildikten hemen sonra paylaşılan katkıda bulunan kayıtlarında görünür.",
  "contributors.form.genericError":
    "Katkıda bulunan değişiklikleri kaydedilemedi. Tekrar deneyin.",
  "contributors.form.validation.displayNameRequired": "Görünen ad zorunludur.",
  "contributors.form.validation.displayNameTooLong":
    "Görünen ad en fazla 120 karakter olabilir.",
  "contributors.form.validation.rolesRequired": "En az bir rol seçin.",
  "contributors.picker.title": "Katkıda bulunan ata",
  "contributors.picker.description":
    "Bu içerikten ayrılmadan mevcut bir {role} seçin veya oluşturun.",
  "contributors.picker.searchLabel": "{role} ara",
  "contributors.picker.searchPlaceholder": "Görünen ada göre ara",
  "contributors.picker.resultsLabel": "Eşleşen katkıda bulunanlar",
  "contributors.picker.loading": "Role göre katkıda bulunanlar yükleniyor...",
  "contributors.picker.emptyTitle": "Eşleşen katkıda bulunan yok",
  "contributors.picker.emptyDescription":
    "“{name}” adlı {role} bulunamadı. Şimdi oluşturup atayabilirsiniz.",
  "contributors.picker.createAndAssign": "Oluştur ve ata",
  "contributors.picker.assign": "Katkıda bulunanı ata",
  "contributors.picker.assignPending": "Katkıda bulunan atanıyor...",
  "contributors.picker.assignSuccess": "Katkıda bulunan atandı.",
  "contributors.picker.assignError": "Atama başarısız oldu. Tekrar deneyin.",
  "contributors.picker.createError":
    "Katkıda bulunan oluşturulamadı. Tekrar deneyin.",
  "contributors.picker.duplicateUseExisting":
    "Bu ad zaten mevcut. Sonuçlardan mevcut katkıda bulunanı seçin.",
  "contributors.picker.nameRequired": "Katkıda bulunan adı girin.",
  "contributors.picker.creditName": "Kredi adı",
  "contributors.picker.scope": "Kredi kapsamı",
  "contributors.picker.allLanguages": "Tüm diller",
  "contributors.picker.creditNamePlaceholder": "İsteğe bağlı kredi adı",
  "contributors.picker.scopeHint": "Bu rol için varsayılan kapsam uygulanır.",
  "contributors.picker.cancel": "İptal",
  "contributors.picker.retry": "Atamayı tekrar dene",
  "contributors.picker.errorTitle": "Katkıda bulunan ataması başarısız",
  "contributors.panel.title": "Katkıda bulunan kredileri",
  "contributors.panel.summary": "{count} atama",
  "contributors.panel.summaryOne": "1 atama",
  "contributors.panel.openRegistry": "Kayıt listesini aç",
  "contributors.panel.loading": "Katkıda bulunan atamaları yükleniyor...",
  "contributors.panel.emptyTitle": "Henüz katkıda bulunan ataması yok",
  "contributors.panel.emptyDescription":
    "Rol özelindeki gruplardan kredi ekleyin.",
  "contributors.panel.noAssignments": "Bu rolde atama yok.",
  "contributors.panel.addSuffix": "ekle",
  "contributors.panel.assignFirst": "Katkıda bulunan ata",
  "contributors.panel.moveUp": "Atamayı yukarı taşı",
  "contributors.panel.moveDown": "Atamayı aşağı taşı",
  "contributors.panel.reorderError":
    "Sıra kaydedilemedi. Önceki sıra geri yüklendi.",
  "contributors.unassign.action": "Kaldır",
  "contributors.unassign.aria": "{name} atamasını kaldır",
  "contributors.unassign.title": "Katkıda bulunan atamasını kaldır",
  "contributors.unassign.description":
    "Bu işlem yalnızca seçili rol ve dil kapsamını mevcut içerikten kaldırır.",
  "contributors.unassign.scope": "{role} / {language} / Sıra {sort}",
  "contributors.unassign.cancel": "İptal",
  "contributors.unassign.confirm": "Atamayı kaldır",
  "contributors.unassign.loading": "Katkıda bulunan ataması kaldırılıyor...",
  "contributors.unassign.pending": "Kaldırılıyor...",
  "contributors.unassign.success": "Katkıda bulunan ataması kaldırıldı.",
  "contributors.unassign.error": "Katkıda bulunan ataması kaldırılamadı.",
  "contributors.delete.title": "Katkıda bulunanı sil",
  "contributors.delete.description":
    "Bu işlem paylaşılan katkıda bulunan kaydını siler. Önce içerik atamaları temizlenmelidir.",
  "contributors.delete.loading": "Katkıda bulunan siliniyor...",
  "contributors.delete.success": "Katkıda bulunan silindi.",
  "contributors.delete.inUse":
    "Önce katkıda bulunanı içerik atamalarından kaldırın.",
  "contributors.delete.error": "Katkıda bulunan silinemedi.",
  "contributors.delete.pending": "Siliniyor...",
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
    "Legacy signed URL metadata mobil/public teslimat tanısı için görünür kalır.",
  "assets.lastCached": "Son önbellekleme: {value}",
  "assets.loadingDetail": "Asset detayı yükleniyor",
  "assets.loadingDetailDescription":
    "Seçilen asset kaydı yönetici API’sinden isteniyor.",
  "assets.noAssetSelected": "Asset seçilmedi",
  "assets.noAssetSelectedDescription":
    "Metadata’sını incelemek için son kayıt listesinden bir asset seçin.",
  "assets.previewTitle": "Önizleme",
  "assets.previewDescription":
    "Görsel asset’leri satır içinde inceleyin veya backend önizleme stream’i üzerinden ses asset’lerini oynatın.",
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
    "CMS asset önizlemesini render etmeden önce backend önizleme token’ı hazırlıyor.",
  "assets.previewLoadFailedTitle": "Önizleme yüklenemedi",
  "assets.previewLoadFailedDescription":
    "Tarayıcı asset’i yüklemeden önce önizleme isteği başarısız oldu.",
  "assets.previewLoadBrowserDescription":
    "Tarayıcı backend önizleme stream’ini render edemedi. Yeni bir önizleme token’ı istemek için önizlemeyi yenileyin.",
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
