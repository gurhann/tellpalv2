# TellPal CMS Frontend Detailed Task Backlog

Bu dokuman, `cms/` altinda kurulacak TellPal CMS on yuz uygulamasi icin uygulanabilir ve modul bazli gorev kirilimini tanimlar.
Kaynak gercekler repo icindeki mevcut backend kodu, `architecture.md`, `.kiro/specs/tellpal-v2-backend/*.md` ve admin controller yuzeyidir.

## Kaynaklar

- `architecture.md`
- `.kiro/specs/tellpal-v2-backend/requirements.md`
- `.kiro/specs/tellpal-v2-backend/design.md`
- `be/src/main/java/com/tellpal/v2/admin/web/admin/AdminAuthController.java`
- `be/src/main/java/com/tellpal/v2/content/web/admin/*.java`
- `be/src/main/java/com/tellpal/v2/category/web/admin/*.java`
- `be/src/main/java/com/tellpal/v2/asset/web/admin/*.java`
- `be/docs/admin-api-rules.md`

## CMS Rulebook Notu

- `M03-T02` ve sonrasi CMS query/form/mutation task'larina baslamadan once `be/docs/admin-api-rules.md` okunmalidir.
- Yeni bir backend validation veya precondition ogrenilirse once kod veya test kaynaginda dogrulanir, sonra rulebook guncellenir, sonra frontend task devam eder.

## UI Standards Notu

- UI kalite ve regresyon task'lari feature backlog'dan ayri olarak `cms/docs/ui-regression-task-list.md` icinde takip edilir.
- CMS layout ve interaction degisiklikleri `cms/docs/ui-standards.md` ve `cms/AGENTS.md` ile uyumlu olmalidir.

## Mevcut Gercek Durum

- Backend admin API hazir ve testlerle korunuyor.
- Repo icinde `cms/` workspace'i artik mevcut; bu backlog kalan feature ve integration islerini yonetir.
- Backend controller gercegi ile eski tasarim notlari bire bir ortusmuyor; bu dokuman controller gercegini esas alir.
- Tasarim sistemi olarak `shadcn/ui` kullanilacak.
- Kapsam `Editorial Core` ile sinirlidir; `user`, `event` ve `purchase` admin ekranlari bu backlog disindadir.

## Frontend Kararlari

- Uygulama konumu: `cms/`
- Stack: React 19 + TypeScript + Vite + Tailwind CSS + `shadcn/ui`
- Paket yonetimi: `npm`
- Routing: `react-router-dom`
- Server state ve mutation: `@tanstack/react-query`
- Form ve validation: `react-hook-form` + `zod`
- HTTP istemcisi: refresh-aware typed `fetch` wrapper
- Auth persistence: access token memory'de, refresh token `localStorage` icinde tutulur
- App boot davranisi: `localStorage` icinde refresh token varsa bootstrap sirasinda refresh denenir
- UI feedback: `shadcn/ui` + `sonner`
- v1 disi: SSR, GraphQL, OpenAPI codegen, BFF, mock-temelli production benzeri akislar

## Hedef Route Yuzeyi

- `/login`
- `/contents`
- `/contents/:contentId`
- `/contents/:contentId/story-pages`
- `/categories`
- `/categories/:categoryId`
- `/media`
- `/media-processing`
- `/contributors`
- `/free-access`

## Frontend API Client Modulleri

- `adminAuth`
- `contentAdmin`
- `storyPageAdmin`
- `categoryAdmin`
- `categoryCurationAdmin`
- `assetAdmin`
- `assetProcessingAdmin`
- `contributorAdmin`
- `freeAccessAdmin`

## Ortak Sozlesmeler

- `ProblemDetail`
- `AdminSession`
- `AuthTokens`
- `LanguageCode`
- `PagedListState` olmadan basit `recent/limit` list state
- `MutationFeedback`
- `LanguageTabState`
- Backend DTO -> UI view model mapper'lari

## Mevcut Backend Admin HTTP Yuzeyi

Asagidaki endpoint'ler backend tarafinda fiilen mevcuttur:

- Auth:
  - `POST /api/admin/auth/login`
  - `POST /api/admin/auth/refresh`
  - `POST /api/admin/auth/logout`
- Content:
  - `POST /api/admin/contents`
  - `PUT /api/admin/contents/{contentId}`
  - `POST /api/admin/contents/{contentId}/localizations/{languageCode}`
  - `PUT /api/admin/contents/{contentId}/localizations/{languageCode}`
  - `PATCH /api/admin/contents/{contentId}/localizations/{languageCode}/processing-status`
  - `POST /api/admin/contents/{contentId}/localizations/{languageCode}/publish`
  - `POST /api/admin/contents/{contentId}/localizations/{languageCode}/archive`
  - `POST /api/admin/contents/{contentId}/story-pages`
  - `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}`
  - `DELETE /api/admin/contents/{contentId}/story-pages/{pageNumber}`
  - `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}/localizations/{languageCode}`
- Contributors:
  - `POST /api/admin/contributors`
  - `GET /api/admin/contributors`
  - `PUT /api/admin/contributors/{contributorId}`
  - `POST /api/admin/contents/{contentId}/contributors`
- Categories:
  - `POST /api/admin/categories`
  - `GET /api/admin/categories/{categoryId}`
  - `GET /api/admin/categories/{categoryId}/localizations`
  - `PUT /api/admin/categories/{categoryId}`
  - `POST /api/admin/categories/{categoryId}/localizations/{languageCode}`
  - `PUT /api/admin/categories/{categoryId}/localizations/{languageCode}`
  - `POST /api/admin/categories/{categoryId}/localizations/{languageCode}/contents`
  - `PUT /api/admin/categories/{categoryId}/localizations/{languageCode}/contents/{contentId}`
  - `DELETE /api/admin/categories/{categoryId}/localizations/{languageCode}/contents/{contentId}`
- Assets:
  - `POST /api/admin/media`
  - `GET /api/admin/media`
  - `GET /api/admin/media/{assetId}`
  - `PUT /api/admin/media/{assetId}/metadata`
  - `POST /api/admin/media/{assetId}/download-url-cache/refresh`
- Asset processing:
  - `POST /api/admin/media-processing`
  - `GET /api/admin/media-processing`
  - `GET /api/admin/media-processing/{contentId}/localizations/{languageCode}`
  - `POST /api/admin/media-processing/{contentId}/localizations/{languageCode}/retry`
- Free access:
  - `POST /api/admin/free-access`
  - `GET /api/admin/free-access`
  - `DELETE /api/admin/free-access/{accessKey}/languages/{languageCode}/contents/{contentId}`

## Bilinen Backend Gap Track

Bu gap'ler frontend modullerinin ilgili tasklarini bloke eder ve mock ile gecistirilmez.

- `BG01`: Content list/get/delete admin endpoint'leri eksik
- `BG02`: Category list/delete ve localization-read admin endpoint'leri eksik
- `BG03`: Contributor delete ve content-contributor unassign endpoint'leri eksik

## Dependency Order

- `M01 -> M11 -> M02`
- `BG01 -> M03`
- `BG01 -> M04`
- `BG02 -> M06`
- `BG02 -> M07`
- `BG03 -> M05`
- `M01 + M11 + M02 -> M08`
- `M01 + M11 + M02 + M03 -> M09`
- `M01 + M11 + M02 + M03 -> M10`

## Task Sizing Rules

- Her task tek coding turunda tamamlanabilecek kadar dar olmalidir.
- Task'lar birincil olarak bir route, bir veri client'i veya bir ortak UI slice'ini hedeflemelidir.
- Her task dosya kapsamini kucuk tutmali ve bir sonuc uretmelidir.
- Her task icin kabul kriteri ve dogrulama adimi acik olmalidir.
- Frontend task'lari backend gap'leri gizlememelidir.

## M01 App Shell

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

`cms/` altinda calisan React/Vite uygulamasi, ortak admin layout'u, route iskeleti, token-aware app bootstrap ve temel `shadcn/ui` tasarim sistemi olusur.

### Exit Criteria

- `npm run build` basarili olur.
- Tum hedef route'lar router seviyesinde tanimlanir.
- Korunmus route'lar session yoksa `/login` ekranina yonlenir.
- `shadcn/ui` ve Tailwind tabanli ortak layout calisir.

### Task Dependency Order

- `M01-T01 -> M01-T02 -> M01-T03 -> M01-T04 -> M01-T05`

### Atomic Tasks

#### M01-T01 `DONE` Bootstrap the CMS workspace

- Objective:
  - Vite tabanli React + TypeScript uygulamasini `cms/` altinda kurmak.
- Files to create or modify:
  - `cms/package.json`
  - `cms/tsconfig.json`
  - `cms/vite.config.ts`
  - `cms/index.html`
  - `cms/src/main.tsx`
  - `cms/src/App.tsx`
- Dependencies:
  - None
- Acceptance criteria:
  - `npm install` sonrasi development server acilabilir.
  - TypeScript strict ayarlari acik olur.
  - `src/` altinda app entrypoint calisir.
- Verification steps:
  - `cd cms && npm install`
  - `cd cms && npm run build`

#### M01-T02 `DONE` Install Tailwind and shadcn foundation

- Objective:
  - Tailwind, `shadcn/ui` altyapisi, utility helper'lari ve theme token tabanini kurmak.
- Files to create or modify:
  - `cms/tailwind.config.ts`
  - `cms/postcss.config.js`
  - `cms/components.json`
  - `cms/src/index.css`
  - `cms/src/lib/utils.ts`
  - `cms/src/components/ui/**`
- Dependencies:
  - `M01-T01`
- Acceptance criteria:
  - `shadcn/ui` primitive'leri projede kullanilabilir olur.
  - Renk, radius, spacing ve surface token'lari CSS variables ile tanimlanir.
  - Button, input, card, dialog, table, tabs, sheet, select, toast primitive'leri hazir olur.
- Verification steps:
  - `cd cms && npm run build`

#### M01-T03 `DONE` Create the app shell and route skeleton

- Objective:
  - Sol navigation, top bar, breadcrumb slot'u ve protected route yapisini kurmak.
- Files to create or modify:
  - `cms/src/app/router.tsx`
  - `cms/src/app/routes/**`
  - `cms/src/components/layout/app-shell.tsx`
  - `cms/src/components/layout/side-nav.tsx`
  - `cms/src/components/layout/top-bar.tsx`
- Dependencies:
  - `M01-T02`
- Acceptance criteria:
  - Tum hedef route'lar skeleton sayfalarla tanimlanir.
  - Protected route wrapper'i auth state'e gore redirect yapar.
  - Active nav state ve 404 route calisir.
- Verification steps:
  - `cd cms && npm run build`

#### M01-T04 `DONE` Add app providers and environment wiring

- Objective:
  - Query client, toast provider, env parser ve error boundary wiring'ini eklemek.
- Files to create or modify:
  - `cms/src/app/providers.tsx`
  - `cms/src/lib/env.ts`
  - `cms/src/lib/query-client.ts`
  - `cms/src/components/system/app-error-boundary.tsx`
  - `cms/.env.example`
- Dependencies:
  - `M01-T03`
- Acceptance criteria:
  - `VITE_API_BASE_URL` ve ilgili env degerleri parse edilir.
  - Query retry politikalari admin UI icin ayarlanir.
  - Beklenmeyen runtime hatalari ortak hata ekraninda gosterilir.
- Verification steps:
  - `cd cms && npm run build`

#### M01-T05 `DONE` Add base toolchain scripts

- Objective:
  - Lint, test, format-check ve e2e komut iskeletini eklemek.
- Files to create or modify:
  - `cms/package.json`
  - `cms/eslint.config.js`
  - `cms/vitest.config.ts`
  - `cms/playwright.config.ts`
  - `cms/src/test/setup.ts`
- Dependencies:
  - `M01-T04`
- Acceptance criteria:
  - `build`, `lint`, `test`, `test:e2e` script'leri tanimlanir.
  - Vitest jsdom kurulumu hazir olur.
  - Playwright smoke yapisi calismaya hazir olur.
- Verification steps:
  - `cd cms && npm run lint`
  - `cd cms && npm run test -- --runInBand`

## M11 Shared UI and Data

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Tum feature modullerinin tekrar kullanacagi admin client, auth-aware request wrapper, form katmani, data table altyapisi ve ortak `shadcn/ui` wrapper'lari olusur.

### Exit Criteria

- Tum API client modulleri ortak HTTP katmanini kullanir.
- `ProblemDetail` parsing merkezi hale gelir.
- Ortak data table, language switcher ve action bar kaliplari feature'lar tarafindan tuketilebilir olur.

### Task Dependency Order

- `M11-T01 -> M11-T02 -> M11-T03 -> M11-T04 -> M11-T05`

### Atomic Tasks

#### M11-T01 `DONE` Build the shared API client core

- Objective:
  - Refresh-aware request wrapper, typed response parser ve `ProblemDetail` mapper'ini kurmak.
- Files to create or modify:
  - `cms/src/lib/http/client.ts`
  - `cms/src/lib/http/problem-details.ts`
  - `cms/src/lib/http/json.ts`
  - `cms/src/types/api.ts`
- Dependencies:
  - `M01-T04`
- Acceptance criteria:
  - Yetkili ve yetkisiz istekler tek giris noktasindan gecer.
  - 401 durumunda refresh denemesi tek-flight mantigiyla calisir.
  - `ProblemDetail` payload'lari UI seviyesinde anlamli hale gelir.
- Verification steps:
  - `cd cms && npm run test -- src/lib/http`

#### M11-T02 `DONE` Add shared admin API modules

- Objective:
  - Route bazli kullanilacak typed client dosyalarini yaratmak.
- Files to create or modify:
  - `cms/src/features/auth/api/admin-auth.ts`
  - `cms/src/features/contents/api/content-admin.ts`
  - `cms/src/features/contents/api/story-page-admin.ts`
  - `cms/src/features/categories/api/category-admin.ts`
  - `cms/src/features/categories/api/category-curation-admin.ts`
  - `cms/src/features/assets/api/asset-admin.ts`
  - `cms/src/features/assets/api/asset-processing-admin.ts`
  - `cms/src/features/contributors/api/contributor-admin.ts`
  - `cms/src/features/free-access/api/free-access-admin.ts`
- Dependencies:
  - `M11-T01`
- Acceptance criteria:
  - Her backend endpoint icin named method bulunur.
  - Frontend route katmani ham `fetch` cagrisi yapmaz.
  - Mevcut olmayan backend gap'leri client seviyesinde acik TODO yerine backlog bagimliligi olarak not edilir.
- Verification steps:
  - `cd cms && npm run build`

#### M11-T03 `DONE` Create reusable form and feedback primitives

- Objective:
  - RHF + Zod tabanli form wrapper'lari, mutation toast desenleri ve loading button kalibini kurmak.
- Files to create or modify:
  - `cms/src/components/forms/form-section.tsx`
  - `cms/src/components/forms/submit-button.tsx`
  - `cms/src/components/forms/field-error.tsx`
  - `cms/src/components/feedback/empty-state.tsx`
  - `cms/src/components/feedback/problem-alert.tsx`
- Dependencies:
  - `M01-T02`
  - `M11-T01`
- Acceptance criteria:
  - Form ekranlari ortak validation ve submit pattern'i kullanir.
  - `ProblemDetail` alan hatalari forma baglanabilir.
  - Async mutation sirasinda disabled/loading state standardize olur.
- Verification steps:
  - `cd cms && npm run test -- src/components/forms`

#### M11-T04 `DONE` Create table, filter, and language switching patterns

- Objective:
  - Feature'larin kullanacagi ortak admin table, filter bar ve language tab bilesenlerini kurmak.
- Files to create or modify:
  - `cms/src/components/data/data-table.tsx`
  - `cms/src/components/data/filter-bar.tsx`
  - `cms/src/components/language/language-tabs.tsx`
  - `cms/src/components/language/language-badge.tsx`
- Dependencies:
  - `M11-T03`
- Acceptance criteria:
  - Table ve filter kalibi content, category, asset ve contributor ekranlarinda tekrar kullanilabilir olur.
  - Language tabs localized entity ekranlari icin ortak state uretir.
- Verification steps:
  - `cd cms && npm run test -- src/components/data`

#### M11-T05 `DONE` Create shared DTO mappers and query helpers

- Objective:
  - Backend DTO'larini feature view model'lerine ceviren mapper'lari ve query key helper'larini eklemek.
- Files to create or modify:
  - `cms/src/lib/query-keys.ts`
  - `cms/src/features/contents/model/**`
  - `cms/src/features/categories/model/**`
  - `cms/src/features/assets/model/**`
  - `cms/src/features/contributors/model/**`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
- Acceptance criteria:
  - UI katmani raw backend sekli yerine mapper ciktilarini tuketir.
  - Query keys merkezi ve tutarli hale gelir.
- Verification steps:
  - `cd cms && npm run build`

## M02 Auth

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Admin login, refresh, logout ve expired-session yonetimi calisir; korunmus route'lar session durumuna gore dogru davranir.

### Exit Criteria

- Login sonrasi kullanici korunmus route'lara erisir.
- Sayfa yenilemede refresh token varsa session yeniden olusturulur.
- Refresh basarisizsa session temizlenir ve `/login` ekranina yonlendirilir.

### Task Dependency Order

- `M02-T01 -> M02-T02 -> M02-T03 -> M02-T04`

### Atomic Tasks

#### M02-T01 `DONE` Implement auth storage and session bootstrap

- Objective:
  - Access token memory store, refresh token persistence ve bootstrap refresh akislarini kurmak.
- Files to create or modify:
  - `cms/src/features/auth/model/session-store.ts`
  - `cms/src/features/auth/lib/token-storage.ts`
  - `cms/src/features/auth/providers/auth-provider.tsx`
- Dependencies:
  - `M11-T01`
  - `M11-T02`
- Acceptance criteria:
  - Session bootstrap sadece bir kez refresh dener.
  - Logout tum token state'ini temizler.
  - Refresh sirasinda paralel istekler tek refresh sonucunu bekler.
- Verification steps:
  - `cd cms && npm run test -- src/features/auth`

#### M02-T02 `DONE` Build the login screen

- Objective:
  - `shadcn/ui` tabanli login formu, validation ve hata gosterimini uygulamak.
- Files to create or modify:
  - `cms/src/app/routes/login.tsx`
  - `cms/src/features/auth/components/login-form.tsx`
  - `cms/src/features/auth/schema/login-schema.ts`
- Dependencies:
  - `M01-T03`
  - `M11-T03`
  - `M02-T01`
- Acceptance criteria:
  - Kullanici adi ve sifre validasyonu istemci tarafinda calisir.
  - 401 ve 403 yanitlari ayri mesajlarla gosterilir.
  - Basarili login sonrasi varsayilan route `/contents` olur.
- Verification steps:
  - `cd cms && npm run test -- src/features/auth/components`

#### M02-T03 `DONE` Wire protected navigation and logout UX

- Objective:
  - Protected route guard, session loading durumu ve logout aksiyonunu layout'a baglamak.
- Files to create or modify:
  - `cms/src/app/router.tsx`
  - `cms/src/components/layout/top-bar.tsx`
  - `cms/src/features/auth/components/logout-button.tsx`
- Dependencies:
  - `M02-T02`
- Acceptance criteria:
  - Auth loading sirasinda flicker olmadan bekleme ekrani gorunur.
  - Logout aksiyonu backend logout endpoint'ini cagirdiktan sonra login'e doner.
- Verification steps:
  - `cd cms && npm run test -- src/app/router`

#### M02-T04 `DONE` Add auth integration and e2e smoke

- Objective:
  - Auth feature'i icin integration ve Playwright smoke kapsamini eklemek.
- Files to create or modify:
  - `cms/src/features/auth/auth.integration.test.tsx`
  - `cms/e2e/auth.spec.ts`
- Dependencies:
  - `M02-T03`
- Acceptance criteria:
  - Login, refresh bootstrap ve logout senaryolari testlerle korunur.
  - Expired refresh token halinde redirect davranisi test edilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/auth`
  - `cd cms && npm run test:e2e -- auth`

## M03 Content Studio

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Editor'ler content kayitlarini listeleyebilir, olusturabilir, duzenleyebilir, localization sekmelerini yonetebilir ve publish/archive aksiyonlarini uygulayabilir.

### Exit Criteria

- Content liste ve detay akislari calisir.
- Create/update/localization/publish/archive akislari UI'dan tetiklenebilir.
- Processing durumu localization seviyesinde gorulebilir.

### Task Dependency Order

- `M03-T01 -> M03-T02 -> M03-T03 -> M03-T04 -> M03-T05`

### Atomic Tasks

#### M03-T01 `DONE` Define content feature routes and page layout

- Objective:
  - Content list ve detail route'lari icin sayfa kabuklarini kurmak.
- Files to create or modify:
  - `cms/src/app/routes/contents/index.tsx`
  - `cms/src/app/routes/contents/detail.tsx`
  - `cms/src/features/contents/components/content-page-shell.tsx`
- Dependencies:
  - `M01-T03`
  - `BG01`
- Acceptance criteria:
  - `/contents` ve `/contents/:contentId` route'lari page shell ile acilir.
  - List ve detail ekranlari ortak toolbar ve action bolgesi kullanir.
- Verification steps:
  - `cd cms && npm run build`

#### M03-T02 `DONE` Build content list and detail queries

- Objective:
  - Content liste ve detay sorgularini query hooks ile baglamak.
- Files to create or modify:
  - `cms/src/features/contents/queries/use-content-list.ts`
  - `cms/src/features/contents/queries/use-content-detail.ts`
  - `cms/src/features/contents/components/content-list-table.tsx`
  - `cms/src/features/contents/components/content-summary-card.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `BG01`
- Acceptance criteria:
  - Liste ekraninda type, active ve external key bilgileri gorunur.
  - Detail ekraninda temel metadata ve localization listesi gosterilir.
  - Not found ve empty state ayri gosterilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/contents/queries`

#### M03-T03 `DONE` Build content create and update forms

- Objective:
  - Content create/update form'larini RHF + Zod + `shadcn/ui` ile kurmak.
- Files to create or modify:
  - `cms/src/features/contents/schema/content-schema.ts`
  - `cms/src/features/contents/components/content-form.tsx`
  - `cms/src/features/contents/mutations/use-save-content.ts`
- Dependencies:
  - `M11-T03`
  - `M03-T02`
- Acceptance criteria:
  - STORY ve non-STORY tipleri icin uygun alanlar gorunur.
  - Kaydetme sonrasi liste/detail cache'i guncellenir.
  - Conflict ve validation hatalari gosterilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/contents/components/content-form`

#### M03-T04 `DONE` Build localization editing and publication controls

- Objective:
  - Localization tabs, create/update formu ve publish/archive aksiyonlarini eklemek.
- Files to create or modify:
  - `cms/src/features/contents/components/localization-tabs.tsx`
  - `cms/src/features/contents/components/content-localization-form.tsx`
  - `cms/src/features/contents/components/publication-actions.tsx`
  - `cms/src/features/contents/mutations/use-content-localization-actions.ts`
- Dependencies:
  - `M11-T04`
  - `M03-T03`
- Acceptance criteria:
  - Dil sekmesi bazinda localization olusturma ve guncelleme calisir.
  - Publish/archive aksiyonlari optimistic olmayan, dogrulanabilir mutation olarak isler.
  - Processing status badge'i localization seviyesinde gorulur.
- Verification steps:
  - `cd cms && npm run test -- src/features/contents/components/localization-tabs`

#### M03-T05 `DONE` Add content feature integration and e2e coverage

- Objective:
  - Content create/edit/publish akislarini testlerle kilitlemek.
- Files to create or modify:
  - `cms/src/features/contents/content.integration.test.tsx`
  - `cms/e2e/content.spec.ts`
- Dependencies:
  - `M03-T04`
- Acceptance criteria:
  - Happy path, validation, 401/403 ve conflict davranislari test edilir.
  - Playwright smoke create -> edit -> publish akislarini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/contents`
  - `cd cms && npm run test:e2e -- content`

## M04 Story Page Editor

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Story tipindeki content kayitlari icin sayfa ekleme, guncelleme, silme ve localization duzenleme akislarina sahip editor ekranlari olusur.

### Exit Criteria

- Story page listesi `contentId` baglaminda gorulebilir.
- Sayfa ekleme/silme/guncelleme ve localization edit akislarinin UI'si calisir.
- Non-STORY content'lerde bu editor gizlenir.

### Task Dependency Order

- `M04-T01 -> M04-T02 -> M04-T03 -> M04-T04`

### Atomic Tasks

#### M04-T01 `DONE` Add story page route and conditional access

- Objective:
  - Story page editor route'unu content detail'e baglamak ve non-STORY kayitlarda kapatmak.
- Files to create or modify:
  - `cms/src/app/routes/story-pages.tsx`
  - `cms/src/features/contents/components/story-page-entry-link.tsx`
  - `cms/src/features/story-pages/guards/story-content-guard.tsx`
- Dependencies:
  - `M03-T02`
  - `BG01`
- Acceptance criteria:
  - `/contents/:contentId/story-pages` sadece STORY iceriklerde erisilebilir olur.
  - Non-STORY kayitlarda kullanici uygun bir bilgi mesaji gorur.
- Verification steps:
  - `cd cms && npm run test -- src/features/story-pages`

#### M04-T02 `DONE` Build story page list and mutation hooks

- Objective:
  - Story page liste, add, update ve delete query/mutation katmanini kurmak.
- Files to create or modify:
  - `cms/src/features/story-pages/queries/use-story-pages.ts`
  - `cms/src/features/story-pages/mutations/use-story-page-actions.ts`
  - `cms/src/features/story-pages/components/story-page-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `M04-T01`
- Acceptance criteria:
  - Page number ve illustration media bilgileri listelenir.
  - Add/update/delete islemleri query invalidation ile calisir.
- Verification steps:
  - `cd cms && npm run test -- src/features/story-pages/queries`

#### M04-T03 `DONE` Build page and localization editors

- Objective:
  - Sayfa metadata ve localized body/audio alanlarini tek editor akisi altinda toplamak.
- Files to create or modify:
  - `cms/src/features/story-pages/components/story-page-form.tsx`
  - `cms/src/features/story-pages/components/story-page-localization-form.tsx`
  - `cms/src/features/story-pages/schema/story-page-schema.ts`
- Dependencies:
  - `M11-T03`
  - `M04-T02`
- Acceptance criteria:
  - Page metadata ve localization form'lari ayri fakat bagli calisir.
  - Audio media secimi ve bos bodyText durumlari dogru ele alinir.
- Verification steps:
  - `cd cms && npm run test -- src/features/story-pages/components`

#### M04-T04 `DONE` Add story page integration and e2e coverage

- Objective:
  - Story page editor akislarini testlerle kilitlemek.
- Files to create or modify:
  - `cms/src/features/story-pages/story-pages.integration.test.tsx`
  - `cms/e2e/story-pages.spec.ts`
- Dependencies:
  - `M04-T03`
- Acceptance criteria:
  - Add, edit, delete ve localization upsert senaryolari test edilir.
  - Playwright smoke bir story content uzerinde tam editor akisini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/story-pages`
  - `cd cms && npm run test:e2e -- story-pages`

## M05 Contributors

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Contributor listesi, olusturma, yeniden adlandirma ve content'e atama akislarina sahip editor modulu olusur; silme ve unassign akislari backend gap kapandiginda tamamlanir.

### Exit Criteria

- Contributor listesi goruntulenir.
- Create, rename ve assign akislarinin UI'si calisir.
- Delete ve unassign gorevleri gap track bagimliligiyla acik sekilde ayrilir.

### Task Dependency Order

- `M05-T01 -> M05-T02 -> M05-T03 -> M05-T04`

### Atomic Tasks

#### M05-T01 `DONE` Build contributor list and detail-less management page

- Objective:
  - Liste agirlikli contributor yonetim sayfasini kurmak.
- Files to create or modify:
  - `cms/src/app/routes/contributors.tsx`
  - `cms/src/features/contributors/queries/use-contributors.ts`
  - `cms/src/features/contributors/components/contributor-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
- Acceptance criteria:
  - Recent contributor listesi limit parametresiyle yuklenir.
  - Empty ve error state'ler ayri gosterilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/contributors`

#### M05-T02 `DONE` Build contributor create and rename workflows

- Objective:
  - Create ve rename dialog/form akislarini eklemek.
- Files to create or modify:
  - `cms/src/features/contributors/components/contributor-form-dialog.tsx`
  - `cms/src/features/contributors/schema/contributor-schema.ts`
  - `cms/src/features/contributors/mutations/use-contributor-actions.ts`
- Dependencies:
  - `M11-T03`
  - `M05-T01`
- Acceptance criteria:
  - Create ve rename mutasyonlari listeyi gunceller.
  - Validation ve 404/409 hatalari gosterilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/contributors/components`

#### M05-T03 `DONE` Build content contributor assignment UX

- Objective:
  - Content detail icinden contributor secme, rol verme ve siralama akislarini kurmak.
- Files to create or modify:
  - `cms/src/features/contributors/components/content-contributor-panel.tsx`
  - `cms/src/features/contributors/components/assign-contributor-dialog.tsx`
  - `cms/src/features/contributors/schema/content-contributor-schema.ts`
- Dependencies:
  - `M05-T02`
  - `M03-T02`
- Acceptance criteria:
  - Kullanici content baglaminda contributor atayabilir.
  - Role, languageCode, creditName ve sortOrder alanlari desteklenir.
- Verification steps:
  - `cd cms && npm run test -- src/features/contributors/components/content-contributor-panel`

#### M05-T04 `DONE` Add contributor tests and gap-aware placeholders

- Objective:
  - Test kapsamini eklemek ve delete/unassign aksiyonlari icin acik gap placeholder'lari gostermek.
- Files to create or modify:
  - `cms/src/features/contributors/contributors.integration.test.tsx`
  - `cms/e2e/contributors.spec.ts`
  - `cms/src/features/contributors/components/missing-actions-note.tsx`
- Dependencies:
  - `M05-T03`
  - `BG03`
- Acceptance criteria:
  - Create, rename ve assign akislarinin testleri bulunur.
  - Delete ve unassign icin UI metni backend gap'e referans verir; sahte buton calismaz.
- Verification steps:
  - `cd cms && npm run test -- src/features/contributors`
  - `cd cms && npm run test:e2e -- contributors`

## M06 Category Studio

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Kategori listeleme, detay, create/update ve localization yonetimi icin editor modulu olusur.

### Exit Criteria

- Category list ve detail ekranlari calisir.
- Localization create/update akislarina sahip form'lar tamamlanir.
- Premium ve active alanlari UI uzerinden duzenlenebilir olur.

### Task Dependency Order

- `M06-T01 -> M06-T02 -> M06-T03 -> M06-T04 -> M06-T05`

### Atomic Tasks

#### M06-T01 `DONE` Add category list and detail route shells

- Objective:
  - Category list ve detail sayfa kabuklarini kurmak.
- Files to create or modify:
  - `cms/src/app/routes/categories/index.tsx`
  - `cms/src/app/routes/categories/detail.tsx`
  - `cms/src/features/categories/components/category-page-shell.tsx`
- Dependencies:
  - `M01-T03`
  - `BG02`
- Acceptance criteria:
  - `/categories` ve `/categories/:categoryId` route'lari tanimlanir.
  - Shell katmani list ve detail toolbar'ini barindirir.
- Verification steps:
  - `cd cms && npm run build`

#### M06-T02 `DONE` Build category list and detail queries

- Objective:
  - Category listesi ve tekil detay sorgusunu query hook'lariyla baglamak.
- Files to create or modify:
  - `cms/src/features/categories/queries/use-category-list.ts`
  - `cms/src/features/categories/queries/use-category-detail.ts`
  - `cms/src/features/categories/components/category-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `M06-T01`
- Acceptance criteria:
  - Slug, type, premium ve active bilgileri listede gorunur.
  - Detail ekraninda localization ozetleri yer alir.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories/queries`

#### M06-T03 `DONE` Build category create, update, and localization forms

- Objective:
  - Category base formu ve localization formunu eklemek.
- Files to create or modify:
  - `cms/src/features/categories/components/category-form.tsx`
  - `cms/src/features/categories/components/category-localization-form.tsx`
  - `cms/src/features/categories/schema/category-schema.ts`
  - `cms/src/features/categories/mutations/use-category-actions.ts`
- Dependencies:
  - `M11-T03`
  - `M06-T02`
- Acceptance criteria:
  - Create/update akislarinda slug/type/premium/active alanlari desteklenir.
  - Localization ekraninda imageMediaId ve status alanlari yonetilebilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories/components`

#### M06-T04 `DONE` Add category integration and e2e coverage

- Objective:
  - Category CRUD ve localization akislarini testlerle korumak.
- Files to create or modify:
  - `cms/src/features/categories/categories.integration.test.tsx`
  - `cms/e2e/categories.spec.ts`
- Dependencies:
  - `M06-T03`
- Acceptance criteria:
  - Happy path ve validation/error senaryolari test edilir.
  - Playwright smoke create -> edit -> localization update akislarini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories`
  - `cd cms && npm run test:e2e -- categories`

#### M06-T05 `DONE` Hydrate category localizations from backend

- Objective:
  - Category detail ekranindaki localization tablarini backend read endpoint'i ile kalici hale getirmek.
- Files to create or modify:
  - `cms/src/features/categories/api/category-admin.ts`
  - `cms/src/features/categories/queries/use-category-localizations.ts`
  - `cms/src/features/categories/mutations/use-category-localization-actions.ts`
  - `cms/src/app/routes/categories/detail.tsx`
- Dependencies:
  - `M06-T04`
  - `BG02-T04`
- Acceptance criteria:
  - Localization create/update sonrasi sekmeler backend source-of-truth ile gorunur.
  - Sayfa refresh sonrasi kaydedilen localization sekmeleri kaybolmaz.
  - Localization read hatasi empty state ile karismaz.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories`
  - `cd cms && npm run test:e2e -- categories`

## M07 Category Curation

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Localized category curation listesi icin content ekleme, sira degistirme ve kaldirma akislarina sahip operasyon paneli olusur.

### Exit Criteria

- Kullanici bir category localization altinda curated content listesi gorebilir.
- Content ekleme, order guncelleme ve remove islemleri calisir.

### Task Dependency Order

- `M07-T01 -> M07-T02 -> M07-T03 -> M07-T04`

### Atomic Tasks

#### M07-T01 `DONE` Add curation workspace to category detail

- Objective:
  - Category detail ekranina dil bazli curation bolumu eklemek.
- Files to create or modify:
  - `cms/src/features/categories/components/category-curation-panel.tsx`
  - `cms/src/features/categories/components/category-language-workspace.tsx`
- Dependencies:
  - `M06-T02`
  - `BG02`
  - `BG01`
- Acceptance criteria:
  - Her dil sekmesinde ayrik curation alani gosterilir.
  - Secili dil olmadan curation aksiyonu aktif olmaz.
- Verification steps:
  - `cd cms && npm run build`

#### M07-T02 `DONE` Build curation add and reorder workflows

- Objective:
  - Content ekleme ve displayOrder guncelleme akislarini kurmak.
- Files to create or modify:
  - `cms/src/features/categories/components/add-curated-content-dialog.tsx`
  - `cms/src/features/categories/components/curation-order-editor.tsx`
  - `cms/src/features/categories/mutations/use-category-curation-actions.ts`
- Dependencies:
  - `M11-T03`
  - `M07-T01`
- Acceptance criteria:
  - Kullanici contentId ve displayOrder ile kayit ekleyebilir.
  - Order update sonrasi liste yeniden yuklenir veya yerel olarak guncellenir.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories/components/add-curated-content-dialog`

#### M07-T03 `DONE` Build curation list and remove action

- Objective:
  - Curated content listesini gormek ve kaldirma aksiyonunu eklemek.
- Files to create or modify:
  - `cms/src/features/categories/components/curation-table.tsx`
  - `cms/src/features/categories/queries/use-category-curation.ts`
- Dependencies:
  - `M07-T02`
- Acceptance criteria:
  - Curated content satirlari contentId ve displayOrder ile gosterilir.
  - Remove aksiyonu confirm dialog ile calisir.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories/queries/use-category-curation`

#### M07-T04 `DONE` Add curation integration and e2e coverage

- Objective:
  - Add, reorder ve remove akislarini testlerle korumak.
- Files to create or modify:
  - `cms/src/features/categories/curation.integration.test.tsx`
  - `cms/e2e/category-curation.spec.ts`
- Dependencies:
  - `M07-T03`
- Acceptance criteria:
  - Invalid localization state ve backend conflict durumlari test edilir.
  - Playwright smoke curation add -> reorder -> remove senaryosunu kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/categories`
  - `cd cms && npm run test:e2e -- category-curation`

## M08 Asset Library

- Status: `DONE`
- Owner: `unassigned`

### Hedef Sonuc

Recent media asset listesi, detay gorunumu, metadata update ve signed URL refresh operasyonlarini sunan asset kutuphanesi modulu olusur.

### Exit Criteria

- Recent asset listesi ve tekil detail gorunumu calisir.
- Metadata update ve cached URL refresh aksiyonlari UI'dan tetiklenebilir.
- Diger feature'lar tarafindan kullanilacak ortak asset picker bileseni bulunur.

### Task Dependency Order

- `M08-T01 -> M08-T02 -> M08-T03 -> M08-T04`

### Atomic Tasks

#### M08-T01 `DONE` Build asset library page and list query

- Objective:
  - Asset listesi icin route ve recent query altyapisini kurmak.
- Files to create or modify:
  - `cms/src/app/routes/media.tsx`
  - `cms/src/features/assets/queries/use-recent-assets.ts`
  - `cms/src/features/assets/components/asset-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `M02-T03`
- Acceptance criteria:
  - Recent asset listesi limit parametresiyle cekilir.
  - Asset kind, provider, objectPath ve mimeType kolonlari gorunur.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets`

#### M08-T02 `DONE` Build asset detail sheet and metadata form

- Objective:
  - Asset secildiginde acilan detail sheet'i ve metadata duzenleme formunu eklemek.
- Files to create or modify:
  - `cms/src/features/assets/components/asset-detail-sheet.tsx`
  - `cms/src/features/assets/components/asset-metadata-form.tsx`
  - `cms/src/features/assets/schema/asset-schema.ts`
- Dependencies:
  - `M08-T01`
  - `M11-T03`
- Acceptance criteria:
  - Detail gorunumu `GET /api/admin/media/{assetId}` ile beslenir.
  - Metadata update mutation'i form uzerinden calisir.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets/components/asset-detail-sheet`

#### M08-T03 `DONE` Build signed URL refresh and shared asset picker

- Objective:
  - Signed URL refresh aksiyonu ve feature'larin kullanacagi ortak asset picker bilesenlerini eklemek.
- Files to create or modify:
  - `cms/src/features/assets/components/refresh-download-url-button.tsx`
  - `cms/src/features/assets/components/asset-picker-dialog.tsx`
  - `cms/src/features/assets/components/asset-picker-field.tsx`
- Dependencies:
  - `M08-T02`
- Acceptance criteria:
  - Refresh aksiyonu basarili oldugunda UI guncellenir.
  - Asset picker content/category/story page formlarinda tekrar kullanilabilir olur.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets/components/asset-picker-dialog`

#### M08-T04 `DONE` Add asset integration and e2e coverage

- Objective:
  - Asset list/detail/update/refresh akislarini testlerle korumak.
- Files to create or modify:
  - `cms/src/features/assets/assets.integration.test.tsx`
  - `cms/e2e/assets.spec.ts`
- Dependencies:
  - `M08-T03`
- Acceptance criteria:
  - Metadata update ve signed URL refresh davranislari test edilir.
  - Playwright smoke asset list -> detail -> update -> refresh akislarini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets`
  - `cd cms && npm run test:e2e -- assets`

## M09 Processing Console

- Status: `TODO`
- Owner: `unassigned`

### Hedef Sonuc

Media processing job listesi, localization bazli durum sorgusu, yeni process schedule ve retry aksiyonlarini sunan operasyon paneli olusur.

### Exit Criteria

- Recent processing listesi gorulebilir.
- Bir localization icin status sorgulanabilir.
- Schedule ve retry aksiyonlari UI uzerinden tetiklenebilir.

### Task Dependency Order

- `M09-T01 -> M09-T02 -> M09-T03 -> M09-T04`

### Atomic Tasks

#### M09-T01 `TODO` Build processing console route and recent jobs table

- Objective:
  - Processing console route'unu ve recent jobs tablosunu kurmak.
- Files to create or modify:
  - `cms/src/app/routes/media-processing.tsx`
  - `cms/src/features/assets/queries/use-recent-processing-jobs.ts`
  - `cms/src/features/assets/components/processing-job-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `M03-T02`
- Acceptance criteria:
  - Job listesi state, languageCode, contentId ve updatedAt alanlariyla gosterilir.
  - Empty, loading ve error state'ler ayrilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets/queries/use-recent-processing-jobs`

#### M09-T02 `TODO` Build localization status lookup and detail panel

- Objective:
  - Tek bir localization icin status sorgusu ve detay panelini eklemek.
- Files to create or modify:
  - `cms/src/features/assets/components/processing-status-search.tsx`
  - `cms/src/features/assets/components/processing-detail-card.tsx`
- Dependencies:
  - `M09-T01`
- Acceptance criteria:
  - Kullanici contentId + languageCode kombinasyonuyla status sorgulayabilir.
  - 404 durumunda ayri not found state gosterilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets/components/processing-status-search`

#### M09-T03 `TODO` Build schedule and retry workflows

- Objective:
  - Schedule ve retry form akislarini, content baglamindan veri tasiyacak sekilde eklemek.
- Files to create or modify:
  - `cms/src/features/assets/components/schedule-processing-dialog.tsx`
  - `cms/src/features/assets/components/retry-processing-dialog.tsx`
  - `cms/src/features/assets/schema/processing-schema.ts`
  - `cms/src/features/assets/mutations/use-processing-actions.ts`
- Dependencies:
  - `M11-T03`
  - `M09-T02`
  - `M03-T02`
- Acceptance criteria:
  - Schedule formu contentType, externalKey, coverSourceAssetId, audioSourceAssetId ve pageCount alanlarini destekler.
  - Retry aksiyonu sadece failed kayitlarda gorunur.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets/components/schedule-processing-dialog`

#### M09-T04 `TODO` Add processing integration and e2e coverage

- Objective:
  - Processing console akislarini testlerle korumak.
- Files to create or modify:
  - `cms/src/features/assets/processing.integration.test.tsx`
  - `cms/e2e/media-processing.spec.ts`
- Dependencies:
  - `M09-T03`
- Acceptance criteria:
  - Schedule, status lookup ve retry senaryolari test edilir.
  - Playwright smoke failed job retry akisini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/assets`
  - `cd cms && npm run test:e2e -- media-processing`

## M10 Free Access

- Status: `TODO`
- Owner: `unassigned`

### Hedef Sonuc

`accessKey` bazli free access listesi, grant ve revoke akislarini sunan editor modulu olusur; `default` seti acik sekilde gorunur.

### Exit Criteria

- `default` ve explicit `accessKey` gorunumleri desteklenir.
- Grant ve revoke aksiyonlari UI'dan tetiklenebilir.
- Content ve language baglamlari UI'da gorunur.

### Task Dependency Order

- `M10-T01 -> M10-T02 -> M10-T03 -> M10-T04`

### Atomic Tasks

#### M10-T01 `TODO` Build free-access page and query workflow

- Objective:
  - `accessKey` filtresiyle calisan liste ekranini kurmak.
- Files to create or modify:
  - `cms/src/app/routes/free-access.tsx`
  - `cms/src/features/free-access/queries/use-free-access-list.ts`
  - `cms/src/features/free-access/components/free-access-table.tsx`
- Dependencies:
  - `M11-T02`
  - `M11-T04`
  - `M02-T03`
- Acceptance criteria:
  - `accessKey` yokken ve varken listeleme desteklenir.
  - `default` seti gorsel olarak ayristirilir.
- Verification steps:
  - `cd cms && npm run test -- src/features/free-access`

#### M10-T02 `TODO` Build grant workflow

- Objective:
  - Grant free-access formunu ve content/language secim akislarini eklemek.
- Files to create or modify:
  - `cms/src/features/free-access/components/grant-free-access-dialog.tsx`
  - `cms/src/features/free-access/schema/free-access-schema.ts`
  - `cms/src/features/free-access/mutations/use-free-access-actions.ts`
- Dependencies:
  - `M11-T03`
  - `M10-T01`
  - `M03-T02`
- Acceptance criteria:
  - accessKey, contentId ve languageCode alanlari ile grant olusturulur.
  - Duplicate conflict dogru mesaja doner.
- Verification steps:
  - `cd cms && npm run test -- src/features/free-access/components/grant-free-access-dialog`

#### M10-T03 `TODO` Build revoke and contextual linking behavior

- Objective:
  - Free-access satirlarindan revoke aksiyonunu ve ilgili content'e gecisi eklemek.
- Files to create or modify:
  - `cms/src/features/free-access/components/revoke-free-access-button.tsx`
  - `cms/src/features/free-access/components/free-access-content-link.tsx`
- Dependencies:
  - `M10-T02`
- Acceptance criteria:
  - Revoke confirm dialog ile calisir.
  - Satirdan ilgili content detail ekranina gecis saglanir.
- Verification steps:
  - `cd cms && npm run test -- src/features/free-access/components`

#### M10-T04 `TODO` Add free-access integration and e2e coverage

- Objective:
  - Free-access grant/list/revoke akislarini testlerle korumak.
- Files to create or modify:
  - `cms/src/features/free-access/free-access.integration.test.tsx`
  - `cms/e2e/free-access.spec.ts`
- Dependencies:
  - `M10-T03`
- Acceptance criteria:
  - Unknown key listeleme, duplicate grant ve revoke davranislari test edilir.
  - Playwright smoke grant -> list -> revoke akislarini kapsar.
- Verification steps:
  - `cd cms && npm run test -- src/features/free-access`
  - `cd cms && npm run test:e2e -- free-access`

## BG01 Backend Gap: Content Admin List, Get, Delete

- Status: `DONE`
- Owner: `backend`

### Hedef Sonuc

CMS content studio'nun ihtiyac duydugu list, detail read ve delete endpoint'leri admin HTTP yuzeyine eklenir.

### Exit Criteria

- Frontend `/contents` ve `/contents/:contentId` ekranlari mevcut create/update/localization akislarina ek olarak backend tarafindan tam desteklenir.

### Task Dependency Order

- `BG01-T01 -> BG01-T02 -> BG01-T03`

### Atomic Tasks

#### BG01-T01 `DONE` Add content admin read endpoints

- Objective:
  - `GET /api/admin/contents` ve `GET /api/admin/contents/{contentId}` endpoint'lerini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java`
  - `be/src/main/java/com/tellpal/v2/content/api/**`
  - `be/src/main/java/com/tellpal/v2/content/application/**`
- Dependencies:
  - None
- Acceptance criteria:
  - Liste ve detail response'lari frontend'in ihtiyac duydugu metadata ve localization ozetlerini dondurur.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContentAdmin* test`

#### BG01-T02 `DONE` Add content delete workflow

- Objective:
  - `DELETE /api/admin/contents/{contentId}` endpoint'ini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java`
  - `be/src/main/java/com/tellpal/v2/content/application/**`
  - `be/src/main/java/com/tellpal/v2/content/domain/**`
- Dependencies:
  - `BG01-T01`
- Acceptance criteria:
  - Delete davranisi domain kurallarina gore dogrulanir ve uygun hata yanitlari uretir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContentAdmin*IntegrationTest test`

#### BG01-T03 `DONE` Add backend tests for content read/delete

- Objective:
  - Yeni endpoint'ler icin controller ve integration test kapsamini eklemek.
- Files to create or modify:
  - `be/src/test/java/com/tellpal/v2/content/web/admin/**`
- Dependencies:
  - `BG01-T02`
- Acceptance criteria:
  - Auth, happy path, not found ve conflict davranislari test edilir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContentAdmin* test`

## BG02 Backend Gap: Category Admin List, Delete, and Localization Read

- Status: `DONE`
- Owner: `backend`

### Hedef Sonuc

Category studio icin gereken listeleme ve silme endpoint'leri admin HTTP yuzeyine eklenir.

### Exit Criteria

- `/categories` ekrani ve delete aksiyonlari backend destekli hale gelir.

### Task Dependency Order

- `BG02-T01 -> BG02-T02 -> BG02-T03 -> BG02-T04`

### Atomic Tasks

#### BG02-T01 `DONE` Add category list endpoint

- Objective:
  - `GET /api/admin/categories` endpoint'ini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java`
  - `be/src/main/java/com/tellpal/v2/category/api/**`
  - `be/src/main/java/com/tellpal/v2/category/application/**`
- Dependencies:
  - None
- Acceptance criteria:
  - Liste response'i slug, type, premium, active ve localization ozetlerini dondurur.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*CategoryAdmin* test`

#### BG02-T02 `DONE` Add category delete endpoint

- Objective:
  - `DELETE /api/admin/categories/{categoryId}` endpoint'ini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java`
  - `be/src/main/java/com/tellpal/v2/category/application/**`
  - `be/src/main/java/com/tellpal/v2/category/domain/**`
- Dependencies:
  - `BG02-T01`
- Acceptance criteria:
  - Delete davranisi curated content ve localization durumlariyla uyumlu domain kurallari kullanir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*CategoryAdmin*IntegrationTest test`

#### BG02-T03 `DONE` Add backend tests for category list/delete

- Objective:
  - Yeni category endpoint'leri icin test kapsamini tamamlamak.
- Files to create or modify:
  - `be/src/test/java/com/tellpal/v2/category/web/admin/**`
- Dependencies:
  - `BG02-T02`
- Acceptance criteria:
  - Auth, happy path, validation ve conflict/not-found davranislari test edilir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*CategoryAdmin* test`

#### BG02-T04 `DONE` Add category localization list endpoint

- Objective:
  - `GET /api/admin/categories/{categoryId}/localizations` endpoint'ini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java`
  - `be/src/main/java/com/tellpal/v2/category/api/**`
  - `be/src/main/java/com/tellpal/v2/category/application/**`
  - `be/src/test/java/com/tellpal/v2/category/web/admin/**`
  - `be/docs/admin-api-rules.md`
- Dependencies:
  - `BG02-T03`
- Acceptance criteria:
  - Localization response'i create/update wire shape'i ile ayni alanlari dondurur.
  - Localization olmayan category icin `200 []` dondurulur.
  - Unknown category icin `404 category_not_found` dondurulur.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*CategoryAdmin* test`

## BG03 Backend Gap: Contributor Delete and Unassign

- Status: `TODO`
- Owner: `backend`

### Hedef Sonuc

Contributor modulu icin eksik delete ve content bagindan cikarma endpoint'leri tamamlanir.

### Exit Criteria

- CMS contributor ekraninda delete ve unassign aksiyonlari backend destekli hale gelir.

### Task Dependency Order

- `BG03-T01 -> BG03-T02 -> BG03-T03`

### Atomic Tasks

#### BG03-T01 `TODO` Add contributor delete endpoint

- Objective:
  - `DELETE /api/admin/contributors/{contributorId}` endpoint'ini eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java`
  - `be/src/main/java/com/tellpal/v2/content/application/**`
  - `be/src/main/java/com/tellpal/v2/content/domain/**`
- Dependencies:
  - None
- Acceptance criteria:
  - Delete kurallari mevcut content baglariyla uyumlu hale getirilir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContributorAdmin* test`

#### BG03-T02 `TODO` Add contributor unassign endpoint

- Objective:
  - Content uzerindeki contributor bagini kaldiran endpoint'i eklemek.
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java`
  - `be/src/main/java/com/tellpal/v2/content/application/**`
- Dependencies:
  - `BG03-T01`
- Acceptance criteria:
  - Unassign aksiyonu content + contributor + language baglaminda calisir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContributorAdmin*IntegrationTest test`

#### BG03-T03 `TODO` Add backend tests for contributor delete/unassign

- Objective:
  - Yeni contributor endpoint'leri icin controller ve integration test kapsamini eklemek.
- Files to create or modify:
  - `be/src/test/java/com/tellpal/v2/content/web/admin/**`
- Dependencies:
  - `BG03-T02`
- Acceptance criteria:
  - Auth, happy path, not found ve conflict davranislari test edilir.
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=*ContributorAdmin* test`

## Test Strategy

- Unit/component: Vitest + React Testing Library
- Integration: feature-level query + mutation akis testleri
- E2E: Playwright smoke suite
- Error coverage:
  - validation errors
  - unauthorized/expired session
  - backend `ProblemDetail`
  - optimistic olmayan mutation rollback ihtiyaci olmayan standard admin akislar

## Oncelikli Uygulama Sirasi

1. `M01`
2. `M11`
3. `M02`
4. `BG01`, `BG02`, `BG03`
5. `M03`
6. `M06`
7. `M08`
8. `M04`
9. `M05`
10. `M07`
11. `M09`
12. `M10`

## Progress Log

- `2026-03-28`: CMS frontend task backlog dokumani olusturuldu.
