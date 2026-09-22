# Contract, data, and integration extraction

Date: 2026-09-20. Supporting extraction for the mobile replacement PRD; not an approved implementation contract.

## Evidence and authority

- Legacy evidence: `app/old/docs/backend-services.md`, `data-models.md`, `api-contracts.md`, and `asset-inventory.md` (2026-07-28 source-code inventories, not proof of production behavior).
- Agreed direction: `be/docs/mobile-delivery-discovery.md` (2026-09-19).
- Later accepted decisions: sibling `prd.md`, read before this extraction. These override earlier deferrals where explicitly stated.
- Legacy statements that the repository has no backend refer to the old application inventory context, not the present TellPal v2 repository. Current backend capability must be revalidated.

## Requirements supported by the sources

| Area | Product requirement / preserved capability | Evidence |
|---|---|---|
| Authentication | Email/password registration and login, verification, reset, Google and Apple login, logout, and account deletion with reauthentication where necessary. Firebase remains authentication authority. | Backend services §3; discovery §6 |
| Anonymous identity | New anonymous continuation uses Firebase Anonymous Auth. Preserve anonymous profile/language on conversion; existing-account sign-in uses that account's profile. No silent account consolidation by matching email. | Discovery §6; accepted anonymous migration in PRD |
| Profiles | One profile per account; age informational. Preserve meaningful values from both legacy profile generations, including avatar, birthdate/age range, customer attributes, purposes/genres, and marketing preference. | Data models §3.1; discovery §6 |
| Catalog | Home rows, complete category lists, CMS ordering, editor selections, similar content, content-by-ID/history lists, and discovery of applicable story/listening types. Small list payloads; detail/start retrieves delivery data. | API contracts §§2–3; discovery §2 |
| Search | General content-only search across applicable content types; contributor metadata can influence relevance, but results are content. Group by type/experience where applicable. | Discovery §2 |
| Story experiences | One story may appear in reading and standalone audio groups. Preserve metadata/credits, imagery, narration, music and layout-related behavior supported by the reference. | Data models §2.1; discovery §§2,4 |
| Interactive stories | Preserve choice-driven story progression. Legacy nodes carry content, media, answer metadata and successors; 0/1/2 successors are interpreted as end/linear/choice. Preserve meaning without requiring identical wire format. | API contracts §2.2; data models §2.3 |
| Parent experience | Preserve parent categories, guide lists/detail, text/audio, metadata and premium/free access behavior as part of replacement. Legacy daily free selection is remotely configured. | API contracts §4; backend services §§2,6 |
| History | Preserve read IDs, duration summaries and individual sessions with content identity/type, timing, status, engagement and stored remaining-page information. Reconcile legacy content IDs with new content identities. | Data models §§3.1–3.2,5 |
| Feedback and promotions | Preserve feedback submission and applicable daily limit behavior, existing records, and promotion validation/redemption behavior. Inventory both feedback schema generations. | Data models §§3.3–3.4; backend services §§4,7 |
| Subscription | Existing subscribers retain access; purchase, restore and refreshed entitlement work before release. Cancelled subscriptions remain valid until their paid access expires. | Backend services §7; discovery §8; PRD late integration decision |
| Access | Full eligible catalog remains discoverable; locked content invokes paywall when started; CMS free selection can differ by language and covers story reading and listening. Non-subscriber category is hidden for active subscribers. Login type does not grant premium access. | Discovery §§6,8 |
| Publication | Visibility and availability are independent by language/experience. Ordinary users cannot receive test candidates; authorized testers can consume them with a visible test/revision indicator. Playback is tied to the admitted revision. | Discovery §3 |
| Media startup | Reading starts after first-page image/audio readiness; bounded lookahead prepares adjacent pages. Narration completion advances with animation; manual navigation can show loading. Avoid speculative full-story download. | Discovery §4 |
| Listening | Audio stories and meditations have detail and explicit Listen, bounded preparation without autoplay, and reuse prepared media on start. Lullaby tap opens player and starts playback; no added detail screen. Required background playback and animated lullaby artwork remain. | Discovery §4 |
| Media delivery | Revisioned manifest and individual page assets; publish readiness/integrity checks and correctly prepared renditions. Protected full-media URLs are supplied only after access checks, not merely because a locked item appears in catalog/detail. | Discovery §§4–5 |
| Language | UI/content language selected together and stored on profile; language can change. English fallback must respect publication and access. Do not equate translation files with enabled languages. | Discovery §7; asset inventory §3 |
| Failure handling | Auth, metadata, entitlement, media preparation/download, local persistence and history capture can partially succeed. Define recoverable end-to-end states; missing/corrupt/stale media must not crash or falsely signal successful readiness. | Backend services §11; API contracts §§5–7 |
| Remote configuration | Preserve applicable operational/product configuration behavior; invalid/unavailable configuration must have defined safe outcomes. Existing keys drive paywalls, announcements, parent visibility, free content, onboarding, review prompts and other presentation decisions. | Backend services §6 |
| Release diagnostics | Crash/performance instrumentation is available during development/testing. User-flow backend logging is integrated in the final release steps; event scope remains to be defined. | PRD accepted decisions; backend services §8 |

## Data preservation and coexistence constraints already accepted

- Migrate an account once when it first adopts the new app, using legacy data up to that first transition; do not periodically resynchronize subsequent old-system activity.
- Unupgraded devices continue using the old system. A later upgraded device on an already migrated account uses its new-system account, rather than rerunning account migration.
- Preserve account/profile/preferences/history/progress, subscription access, feedback/promotion records and identity relationships. Capture both legacy schema generations and meaningful local-only records; do not silently drop unmapped records.
- Convert legacy local anonymous data into a new anonymous identity without forcing registration. Legacy `is_anonymous` does not prove an existing Firebase anonymous UID.
- Separate account migration from device-local recovery; the accepted one-time account rule does not settle how unique local data on later-upgraded devices is reconciled.
- Remove old downloaded media and reproducible caches only after separating them from migration inputs and preserved user records. This does not authorize deleting old server data or data on unupgraded devices.
- Show migration progress; on failure preserve data and permit retry. Do not enter normal use with an empty profile while migration is incomplete. Retrying must not duplicate records or overwrite newer data.

## Legacy/new differences and unresolved boundaries

| Difference or gap | Status / treatment in PRD |
|---|---|
| RevenueCat and action logging deferred in discovery | Resolved: first release includes both; implement/verify in final pre-release steps. Crash diagnostics are earlier. |
| Local anonymous flag vs Firebase Anonymous Auth | Resolved product direction; technical identity mapping and local-only extraction remain open. |
| RTDB profiles/history vs new backend application profile | Preserve user-visible data/meaning. Actual target contracts, migration adapters and source reconciliation require design; do not copy the old schema by default. |
| ZIP/version cache vs per-page revision manifest | Approved delivery exception: no two-ZIP/five-ZIP constraint. Cache limits and resume semantics remain open. No first-release explicit offline manager requirement. |
| Legacy recursive choice tree vs discovery ordered-page manifest | Contract must represent branch choices and eligible successor assets. Do not flatten away interactivity or invent a branching manifest without design evidence. |
| Legacy category `AUDIO_STORY` vs one story with separate reading/listening releases | Map identity and histories without duplication or conflating experience-specific publication. Exact mapping unresolved. |
| Parent guide support vs discovery focused on CMS mobile content | Parent capability is legacy replacement scope, not automatically deferred because discovery omits details. Target API/content model and daily free behavior need explicit mapping. |
| Legacy public-looking REST client vs protected delivery | Legacy lack of visible bearer header is not proof of server access policy. Target must authenticate identity and enforce media access per discovery. |
| Five source locales vs actual selectable locales | Evidence: `tr/en/de` exposed, `pt` remotely gated, `es` commented out. Preserve reference behavior; do not newly enable every available translation. Effective production configuration needs capture. |
| Remote Config free selection vs CMS per-language free selection | CMS direction is accepted. Map daily parent/free presentation and old category rules explicitly; no unapproved deletion of parent functionality. |
| Anonymous conversion vs sign-in to existing account | Discovery says anonymous conversion preserves profile/language, existing sign-in uses existing profile; treatment of anonymous history on existing-account login remains unproven. |
| Persisted remaining page vs restart/resume behavior | Preserve existing stored data; precise new resume UX and branch reconstruction need reference-runtime evidence. |
| Provider SDKs and unused packages | Existing Adjust, analytics, messaging, performance and notification dependencies are evidence of possible integrations, not automatic requirements to reintroduce every SDK. Verify visible behavior and integration obligations. |
| Artwork crop and resolution | Aspect-preserving fill is direction; centered crop provisional. High-resolution imagery as crash cause is a hypothesis, not diagnosis. Representative phone/tablet and low-memory measurements required. |

## Evidence to collect before implementation acceptance

- Reference-version effective remote configuration and enabled locales/features; no secrets or private URLs in artifacts.
- Real request/response/error fixtures for catalog, search, history, profile and access states; existing client endpoint tables do not define the new contract.
- Source/target data inventory and identity mapping for both profile generations, feedback generations, promotions, session histories and device-only values.
- Legacy/new content mapping for parent guides, interactive branches, and shared reading/listening identity.
- Android/iOS phone/tablet recordings or screenshots for representative details/players, branch choices, fallback languages and failure states.
- Media readiness evidence: actual transformed files, manifests and protected URL behavior; registration of a processing plan is not proof of delivered optimized assets.
- Crash-free metric definition, platform/version/window and device memory measurements. Numeric gates remain open; approximately 85% is user-reported and unverified.

This extraction makes no framework choice, new endpoint/schema decision, numeric reliability promise, or additional product deferral.
