# Legacy user-flow parity extraction

Date: 2026-09-20. Reference build: 1.0.35+333. This is supporting evidence for the replacement PRD, not an independent scope decision.

## Evidence and interpretation

Sources: `app/old/docs/screen-flow-map.md`, `component-inventory.md`, `emulator-validation.md`, and `test-strategy.md`.

**Observed** means recorded in the Android debug-emulator report, not newly tested here or proven in production. **Partial** means a screen or part of a journey was observed but its outcome was not. **Documented** means source-derived documentation only. No iOS execution was established. Documented capabilities remain parity candidates; lack of runtime proof does not authorize dropping them. Defects and implementation choices such as hard-coded ZIP boundaries are not product behavior to reproduce.

## Capability inventory

| Area | User-visible legacy capability | Evidence status and limits |
|---|---|---|
| First use | Notification permission, three onboarding pages, skip/next, final start action and account-choice sheet | Observed. Legacy Skip advances one page. |
| Entry | Continue without an account; persistent anonymous access across restart; anonymous profile and membership call to action | Partial: initial flow was blocked by an emulator Billing error; restart reached anonymous home. |
| Entry | Email registration/login, Google login/registration, Apple login/registration where applicable | Email registration, logout and subsequent login observed. Google/Apple actual authentication unverified; Apple absent on Android as expected by report. |
| Entry | Required registration notice consent, separate marketing consent, email and password validation | Form and empty-submit validation observed; documentation records minimum eight-character registration password. |
| Entry | Missing profile triggers setup; existing profile enters home; persisted session survives restart | Setup and session restoration observed for test email account; full startup state matrix documented. Legacy destructive cleanup is not a migration requirement. |
| Profile setup | Purpose, age range, favorite genres, avatar, child name; per-step skip | Screens and skip path observed. Saving fully populated values unverified. Alternative older DOB/customer-type/occupation model documented. |
| Account recovery | Password-reset request, reset action link, email-verification action link and verification reminder | Reset form/validation and reminder observed; successful email dispatch, link completion and password reset unverified. |
| Navigation | Persistent Library, Sleep/Relax and Profile tabs; child/parent selection; returning to child mode | Observed. Parent/child mode is content context, not an authorization role. |
| Discovery | Carousel, category grid, horizontal lists, story cards, category listings and all-items links | Home and cards observed. Some category targets, including Sleep all-items lists, not separately executed. |
| Search | Story search after at least three characters with debounce; result-to-detail navigation | Search-to-free-story journey observed; exact debounce behavior source documented. General cross-content search is a separate approved discovery exception. |
| Story details | Cover, title, age, duration, summary and credits; start action; premium lock and upgrade action | Observed for free and premium examples. Route-by-ID detail loading documented. |
| Reading | Full-screen illustrated/text pages, forward navigation, voice/music and reader controls, completion screen | Normal paging and completion observed. Exact complete control inventory and all voice/music combinations need characterization. |
| Interactive reading | Branch choice changes the active path; audio completion can advance pages | Documented only: available free examples did not expose choices; candidate premium content was inaccessible for full test. |
| Reader delivery | Loading while required assets arrive, use of downloaded assets, recovery from missing/corrupt assets, content revision refresh | Successful first/second ZIP download observed. Failure/retry, offline reuse, interruption and revision outcomes remain unverified. Preserve user outcomes, not the ZIP mechanism. |
| Completion | Story-finished view, rating, similar content and paywall opportunities | Completion screen observed. Rating, recommendations and completion-paywall actions appear in component inventory but were not executed. |
| Sleep discovery | Lullabies, meditations and audio stories, per-type collections and premium gating | Free example of each type and premium meditation gate observed. |
| Lullaby player | Card opens player directly; automatic playback, duration, music/instrument information, pause/resume, seek and exit; looping | Autoplay, duration, pause/resume and exit observed. Seek and loop policy documented; loop completion not proven. |
| Meditation/audio-story player | Detail before playback; cover, age, duration, summary, author/narrator, type-specific start action; automatic playback without loop | Details and autoplay observed for each type; full-duration completion and no-loop behavior source documented. |
| Audio lifecycle | Background/foreground and lock-screen continuation, interruptions, exclusive/correct active audio, safe exit | Intended behavior/test needs documented, not validated. Android foreground-service permission warning observed despite playback continuing. |
| Parent mode | Daily free guide, category lists, guide cover/title/summary, premium locks and guide detail | Mode switch and daily guide with listen action observed. Category/deep detail journeys and premium outcome not fully validated. |
| Parent guides | Read guide, listen to guide, record start/finish and duration | Source documented; actual reader/player completion not runtime verified. |
| Profile | Display name/avatar, completed-story count, accumulated minutes, reading/listening history and progress | Default test profile, zero counters and verification reminder observed. Non-empty history, progress restoration and persisted counters unverified. |
| Profile editing | Change child name and avatar; source also describes DOB/age-related profile data | Current edit screen observed, saving not executed. Do not equate older model fields with visible current edit controls. |
| Language | Choose language and refresh localized UI/content and relevant caches | Entry observed; completed language switch, restart persistence and all-language behavior unverified. |
| Notifications | Permission status/toggle and operating-system permission interaction | Initial permission and profile toggle observed; production permission transitions unverified. |
| Account management | Change password using old/new password, logout confirmation, account deletion with reauthentication when required | Logout/login observed. Change-password form/empty validation only. Actual password change and deletion unverified. |
| Support/legal | Daily-limited feedback, localized legal/help PDFs, app rating prompt and version information | Version observed. Feedback delivery/limit, PDF display and app-rating prompt source documented only. |
| Membership | Premium state, locked content, offerings/paywalls, entitlement synchronization, purchase/restore, promotion-code entry | Locks and paywall launch observed; Billing unavailable prevented offerings/purchase/restore validation. Promotion entry component documented only. |
| Runtime messaging | Connectivity/error/loading views, alerts/popups, announcement imagery and scheduled advertising/paywall decisions | Components/startup tasks documented; trigger inventory, dismissal and failure recovery need characterization. |

## Data and behavior acceptance implications

- Preserve user profile attributes, preferences, identity and subscription rights, history, completion, time and progress wherever they exist in the old system. A default-profile screenshot is not evidence that real users have no populated data.
- Preserve visible content taxonomy and navigation, detail metadata, child/parent context, localization, premium access policy and intended playback/reader outcomes, except for explicitly accepted discovery changes.
- Characterize all entry paths to premium content: card, detail, completion and startup/registration. The Billing error that trapped the legacy UI is a defect, not parity.
- Trace reader start, completion, duration and remaining-page outcomes independently from logging implementation. Late delivery of backend flow logging does not justify omitting user-visible history or progress.
- Package count and the legacy page-index-three boundary are test evidence for a class of delayed-media race, not a required future backend contract.

## Verification gaps to close before release

1. Social authentication and missing/existing profile variants; successful email verification/reset; non-empty profile creation/edit persistence; deletion and reauthentication.
2. Licensed-device/store test purchase and restore, anonymous-to-registered entitlement continuity, promotion code behavior and old paid-user access after migration.
3. Real branching story fixtures, backward navigation after choice, page/voice/music controls, auto-advance and persisted completion/progress.
4. Slow/failing downloads, interrupted/retried loading, corrupt/empty/stale assets, rapid forward/back paging and leaving during download. Each must reach a recoverable outcome without infinite loading, wrong image/audio or stale callbacks.
5. Physical-device audio under lock, app backgrounding, interruptions and repeated enter/exit; Android foreground-service behavior and all iOS runtime paths.
6. Non-empty history/counters, parent-guide reading/listening, daily-free-guide locale/day rules, all-items categories, language change/restart, feedback limits, PDFs, announcements and ratings.
7. Visual baseline coverage across approved phone/tablet platforms and languages. Existing Android screenshots are partial, not a complete parity specification.
8. Low-memory/long-session image and audio profiling. Emulator skipped-frame logs are not production measurements; the user's approximate 85% crash-free figure and large-image hypothesis are not a verified metric or cause.

The existing suite documents 27 test files and 88 test/testWidgets declarations, focused on service/runtime guards. It does not establish full end-to-end parity. Reuse its characterization intent across whatever mobile technology is chosen; Flutter commands and Cubit structures are not PRD constraints.
