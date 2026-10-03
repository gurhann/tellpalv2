# Mobile Library Home — Story 1.1 Contract Review

Date: 2026-10-02

Updated: 2026-10-03

Status: `v0.1-draft` — source comparison and proposed consumer contract; preview default, CMS category-order management and no automatic content-language fallback adopted, overall contract not implemented or accepted.

Scope: mobile Story 1.1 only. EXT-CATALOG remains open.

## Purpose and evidence boundary

Define the minimum Library home contract and compare it with the existing backend. This draft
does not change canonical architecture, assign a delivery owner, implement an endpoint, or raise
the mobile story's backlog status. Endpoint names and new field names below are proposals.
Product behavior already agreed in the PRD remains binding; missing implementation decisions
are explicitly open.

Sources inspected: current backend source at repository HEAD `88fe346b4b40`, the working-tree
[mobile story](../../_bmad-output/planning-artifacts/epics-mobile.md#story-11-discover-eligible-content-on-the-library-home-screen),
[Epic 1 context](../../_bmad-output/implementation-artifacts/mobile/epic-1-context.md),
[PRD FR-6/9/10](../../_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/prd.md),
[canonical backend architecture](../../architecture.md), ADR-0001/0002/0009/0010/0012/0017,
and the [mobile spine](../../_bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md).
The epic/context had pre-existing local edits; this comparison uses their current contents.
No live backend/Remote Config requests or application tests were executed. Existing tests below
are inspected coverage, not a claim that they passed in this review.

## Existing endpoints

| Endpoint | Current behavior | Use for Story 1.1 |
| --- | --- | --- |
| `GET /api/categories?lang=...&type=STORY` | Active categories, exact-language published localization, ordered by slug; returns category metadata and `premium`. | Existing identities/localizations are reusable; not a complete home contract. |
| `GET /api/categories/{slug}/contents?lang=...&freeKey=...` | Visible curated content in the requested language's `display_order`; no preview limit or subscriber context. | Existing curation/order can support the companion work. |
| `GET /api/contents?lang=...&type=STORY&freeKey=...` | Active visible content ordered by ID; free-set membership; delivery-asset bundle. | Not a curated home response. |

These controllers do not consume a Firebase Authorization header or subscriber context.
Canonical architecture permits anonymous metadata listing; adding personalized audience
resolution requires an explicit boundary design, not silently requiring every catalog caller
to create a production account.

## Requirement-to-code comparison

| Requirement | Source finding | Gap / required companion work |
| --- | --- | --- |
| CMS category row order | `PublicCategoryQueryService.listCategories` sorts by `Category.getSlug`. `Category` and `CategoryLocalization` have no home-order field. `display_order` belongs to `CategoryContent`. | Implement the adopted CMS capability to edit category row order, persist it in the backend and return it through the home contract. Keep it independent of within-category card order. Slug/ID order cannot substitute for editorial order. |
| Limited previews, preserving editorial order | Curated IDs are ordered by `CategoryContent.displayOrder`; content lookup preserves supplied order and removes invisible items. Every eligible result is returned. | Implement the adopted server-configurable default of 5 eligible previews per row and deterministic selection after eligibility filtering; apply bounded reads before loading delivery graphs. |
| Omit categories with no eligible previews | `listCategories` checks active/published category state without checking curated eligible items. | Compose row eligibility with content eligibility; discard empty rows after language/audience decisions. |
| Non-subscriber-only category | Current category has `premium`; neither query filters by subscriber state. No explicit audience field exists. | Model audience independently from premium/free access. Do not reinterpret `premium` as non-subscriber-only without an approved mapping. |
| Backend lock state | `PublicContentSummary.free` comes from the localized free-access set and optional caller-supplied key. There is no effective subscriber unlock decision in this projection. | Return effective access/lock metadata from a verified server context. `isFree` alone is insufficient for subscriber rendering. |
| Publication and explicit content language | Queries look up only the exact requested language. Content visibility checks active content plus `PUBLISHED`/`COMPLETED`. | Exact-language lookup matches the 2026-10-03 rejection of automatic fallback. Add explicit different-language category mapping where deliberately curated; enforce publication/access and retain the chosen item language through detail/start. |
| Content/experience identity | Summary exposes canonical `type`, content ID and language, without an experience field. | For this Library slice, identify reading STORY explicitly; do not add canonical `AUDIO_STORY` or return listening results by accident. |
| Small list DTO and media separation | Category content DTO includes phone/tablet detail covers, ZIP package references and optimized audio. Bundle mapping copies cached URL metadata. | Create a cover-only list projection. Do not issue protected full-media URLs through a home response. Verify usable thumbnail delivery separately. |
| Success/empty/error semantics | Category/content advice produces `ProblemDetail` for invalid input/not found. No home-specific errors, request budget or artwork waiting budget exists. | Define home response/error behavior and mobile retry/timeout rules; a thumbnail failure must remain a card-level failure. |
| Real-backend evidence | `PublicDeliveryIntegrationTest` covers exact-language visibility, default free-key resolution, curated content order and generated asset fields. | Add home-specific coverage and execute it; verify actual authorized audiences and thumbnail transfers against the same dataset as mobile. |

Source anchors:

- [Category queries](../src/main/java/com/tellpal/v2/category/application/query/PublicCategoryQueryService.java)
  and [category entity](../src/main/java/com/tellpal/v2/category/domain/Category.java).
- [Content queries](../src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java),
  [visibility policy](../src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java)
  and [free-set resolution](../src/main/java/com/tellpal/v2/content/application/ContentFreeAccessService.java).
- [Category DTOs](../src/main/java/com/tellpal/v2/category/web/mobile/CategoryMobileResponses.java),
  [asset bundle mapping](../src/main/java/com/tellpal/v2/asset/application/ContentAssetBundleService.java)
  and [existing delivery tests](../src/test/java/com/tellpal/v2/content/web/mobile/PublicDeliveryIntegrationTest.java).

## Proposed home contract

Prefer one bounded, grouped response for Library, tentatively
`GET /api/categories/home?lang={requestedLanguage}`. This is an additive route proposal;
the existing category/detail endpoints retain their own contracts. Register a literal home route
explicitly if it shares the current slug namespace.

The alternative is extending category metadata and fetching previews per category. It reuses
the endpoint shape but needs multiple requests, partial-row failure semantics and a consistent
audience/language baseline across calls. A grouped response fits Story 1.1's catalog-level
loading/error/empty behavior and keeps editorial selection on the backend. Grouping does not
by itself provide a consistent database snapshot; define the read consistency in the companion work.

| Proposed field | Meaning |
| --- | --- |
| `contractVersion` | Home response schema version; initial proposal `1`. |
| `requestedLanguageCode` | Validated selected application/catalog presentation language. |
| `previewLimit` | Effective server-controlled positive bound per returned row; adopted initial default 5. |
| `categories[]` | Ordered, non-empty eligible Library rows; array order is authoritative. |
| `categoryId`, `slug` | Stable category identity and existing route lookup value. |
| `languageCode`, `name`, `description` | Published category presentation localization in the selected application language; description may be absent. |
| `contentLanguageCode` | Explicit category content language: selected language for ordinary rows, CMS-configured language for a clearly labeled different-language row. Mapping is proposed; no automatic substitution. |
| `items[].contentId`, `contentType`, `experienceType` | Canonical content identity plus explicit experience. Proposed values in this slice: `STORY`, `READING`. |
| `items[].languageCode`, `title` | Published reading-result language/title, matching the category's explicit content language. Preserve this language when opening detail/start. |
| `items[].access.isFree`, `isLocked` | CMS free-set membership and effective backend lock decision; these are different facts. |
| `items[].artwork` | Phone/tablet thumbnail references only; nullable when no usable artwork is available. |
| Thumbnail `assetId`, `url`, `expiresAt` | Asset identity and usable delivery URL; expiry nullable only for a documented non-expiring delivery mechanism. |

An access-context identifier can be added if needed by the verified transport contract; it is not
an entitlement credential. Do not accept `isSubscriber=true`, tester flags or arbitrary free-set
keys from the app as proof of access. The existing `freeKey` behavior is source evidence, not
automatic approval to expose the same selector in the new home contract.

### Selection and state rules inherited from Story 1.1

1. Resolve a trusted audience and the requested language. Listing visibility never grants full-media access.
2. Select published eligible Library categories in the order edited in CMS and persisted by the
   backend. Preserve that relative order after eligibility filtering; row reordering does not
   change within-category card order. Hide non-subscriber-only categories
   from active subscribers; ordinary categories remain discoverable under the agreed rules.
3. Resolve curated reading content only in the category's explicit content language. Ordinary
   categories use the selected language; no automatic English or other-language substitution is
   allowed. A CMS-curated, clearly labeled category such as “English Stories” may intentionally
   present English content in a Turkish UI. Never bypass language visibility, publication or
   access checks; return item language and preserve it through detail/start without changing UI preference.
4. Preserve editorial content order, then apply the server-configurable preview bound (initial default 5) to eligible items. An early
   invisible item must not consume an eligible preview slot. Remove rows with no eligible items.
5. Return title, lock state and thumbnail metadata without page/audio/ZIP/manifests. Missing or failed
   artwork preserves the card and does not convert a successful catalog into an error.
6. `200` with `categories: []` means a successful empty catalog. Invalid input is a request error;
   transport/server/access-context failures are distinct from emptiness. Proposed error format:
   existing `ProblemDetail` conventions with stable `errorCode`; exact status/code matrix remains open.
7. Mobile keys each request attempt by language/access context plus attempt identity. Earlier success,
   error and image completions cannot overwrite current state, including after timeout/retry.
   Context comparison alone is insufficient when two attempts use the same context.

This draft does not assume candidate/live revision support is implemented. Current localization
and processing checks are reusable baseline behavior, not proof of the complete adopted release
model. The first listing fixture must state which published dataset it uses; Story 1.1 does not
silently complete later candidate management or media-admission stories.

The existing curation trigger in `V7__create_category_tables.sql` requires category and content
localizations in the link's language. A Turkish category presentation deliberately showing English
content needs an explicit mapping between presentation language and content/curation language.
Define that mapping against the current constraints before implementing HC-5; never reinterpret
a missing translation as permission to substitute English.

## Preview-limit reference evidence

Read-only inspection of the legacy checkout at
`/Users/gurhankucuk/Documents/GitHub/tellpal` (HEAD `e0c31b8872da`) found:

- `lib/features/stories/presentation/pages/stories.dart:34` passes
  `FirebaseRemoteConfigService().storyItemCountToShow` into the category fetch.
- The cubit/use case/repository forwards that count to `/story/with-category?count=...`.
- `lib/config/firebase/firebase_remote_config_settings.dart:63` sets the bundled default to **5**.

The source inspection establishes the bundled default. On **2026-10-03**, Gürhan confirmed that
the live system also uses **5** and accepted **5 eligible previews per category** as the new
server-configurable initial default. Live-value evidence is user confirmation, not a Remote Config
or backend request executed in this review. If fewer than five eligible items exist, return those
items without padding; omit an empty row. Runtime selection/order must still be checked against
explicit expected identities. Per-category overrides have not been requested or adopted.

## CMS category-order decision

On **2026-10-03**, the user approved adding CMS management of Library category row order to
the Story 1.1 backend/CMS companion scope. Editors can change and save the row order; the
backend persists it and mobile renders the returned order. Category row order and the existing
within-category content order are separate controls. The user also approved independent category
row order per language on 2026-10-03: editing the Turkish order must not change the English or
Portuguese order. Filtering hidden/empty categories preserves
the relative order of the remaining rows. Storage design, surface scope, initial ordering
of existing records, tie handling and the CMS interaction remain implementation-contract decisions.
No ordering schema, admin API or CMS control has been implemented by this review.

## Content-language decision

On **2026-10-03**, the user explicitly rejected automatic content-language fallback, superseding
the earlier English-fallback permission. A Turkish ordinary row with three eligible Turkish items
returns three items even if additional English versions exist. A row without eligible items in its
declared content language is omitted. Missing detail/start localization has an unavailable outcome;
it is never silently served in another language.

CMS may deliberately expose a separate, clearly labeled “English Stories” category in the Turkish
UI. Its English items remain English through detail/reading; UI preference stays Turkish. This
category still respects language/test visibility, publication, audience and media access. Its
mapping/curation representation is implementation work. The English UI startup default when no
language setting is accessible remains a separate existing rule. Support-document language and
UI-artwork behavior remain their own contracts.

## Decision status before implementation/integration

| ID | Decision needed | Proposed direction / constraint |
| --- | --- | --- |
| HC-1 | CMS category-order management — adopted 2026-10-03; implementation details open | Editors manage Library row order independently per language in CMS; backend persists and returns the selected language's order independently from card order. Changing one language's order must not change another's. Add the capability to the Story 1.1 backend/CMS companion work. Settle surface scope, storage, existing-record initialization and tie behavior before implementing; retain stable category IDs. |
| HC-2 | Preview default and configuration ownership — adopted 2026-10-03 | Server-configurable initial default **5 eligible previews per category**. User confirmed the live value is also 5. Implement and verify the limit; per-category overrides are not part of this decision. |
| HC-3 | Non-subscriber category audience mapping | Add an explicit audience concept; do not overload current `premium`. Identify the actual fixture category. |
| HC-4 | Trusted subscriber/test context and composition owner | Existing purchase public API exposes catalog maintenance, not an effective user entitlement query. Firebase verification establishes identity, not subscription. Settle a server-side development/test mechanism and module-safe API dependencies before using either audience. |
| HC-5 | No automatic content-language fallback — adopted 2026-10-03; explicit category mapping open | Ordinary rows use only eligible content in the selected language. Deliberately curated/labeled different-language categories are allowed; define their presentation-to-content-language mapping and propagate explicit item language to detail/start. Do not fill missing translations automatically or alter UI preference. |
| HC-6 | Thumbnail selection, URL freshness and failure/retry budget | Reuse verified localized reading-cover variants where appropriate. Define expiry/renewal and missing-variant behavior; no full-media bundle. Set catalog/artwork waits before acceptance. |
| HC-7 | Home bounds, consistency and errors | Set total response/category bounds, deterministic tie rules and transaction/read consistency; define 400/auth/context/unavailable outcomes and empty success. No arbitrary category truncation. |

Canonical category dependencies currently allow `content::api` and `asset::api`, not `user::api`
or `purchase::api`. Purchase already depends on content/user. Adding reciprocal dependencies can
create a module cycle. This draft leaves the authenticated composition owner open; resolve it
against the existing dependency graph before coding. Do not introduce a central presentation
module or import another module's internals to make the route work.

Existing local/test Firebase stubs and the allowlisted media experiment are useful test capabilities,
but neither supplies verified subscriber state to the current catalog. The stub validator excludes
non-local/test configurations. The media experiment allowlist is not a subscription grant.
Do not copy the PoC token-input UI into the product shell.

## Story 1.1 closure evidence to collect

Use the proposed evidence area
`_bmad-output/implementation-artifacts/mobile-contracts/EXT-CATALOG/story-1-1/`
only when real artifacts are produced. No such evidence is created by this draft.

| Case | Required expected outcome |
| --- | --- |
| Multiple rows with CMS order differing from slug/ID order; save and reload a changed order | Saved order survives CMS/backend reload and matches mobile's expected ordered category identities; card order is unchanged. Hidden/empty-row filtering preserves remaining row order. |
| Different Turkish/English category orders; edit and reload only the Turkish order | Each language returns its own saved order; changing Turkish leaves the English order and within-category card order unchanged. |
| More eligible items than the configured limit; invisible items before/between eligible items | Exact bounded ordered preview identities, with no lost slots. |
| Draft/archived/inactive or empty category/content | Excluded as specified; successful fully empty home remains `200`. |
| Non-subscriber and active subscriber against the same dataset | Restricted category present/absent respectively; per-card lock state checked independently. |
| Turkish ordinary row with 3 eligible Turkish items and 2 items available only in English | Return the 3 Turkish items; no English padding. Omit the row if no Turkish item qualifies. |
| Explicit “English Stories” row presented in Turkish UI | CMS explicitly selects English content; labels communicate that language. Item language reaches detail/start; UI preference stays Turkish. No unauthorized/test-only or unpublished content is exposed. |
| Missing/expired/corrupt/slow thumbnail | Title, lock state and geometry survive; other rows remain in the successful catalog state. |
| Timeout/retry and reversed language/audience/attempt completions | Only current-context/current-attempt results affect visible state. |
| Payload inspection | No page bodies, playback audio, ZIP packages or protected full-media URLs. |

Capture contract version, backend build and timestamp, sanitized test identities, effective language/
authorized audience, fixture/expected-order files, real request/response and thumbnail-transfer
results, and the mobile comparison against that same baseline. Refresh affected evidence after
relevant CMS changes. Record the named assignee, companion work and **Story 1.1-only** closure
decision in EXT-CATALOG. Detail, full-category browsing and search remain outstanding.

UI acceptance separately needs the agreed reference device/text-scale/contrast matrix and
disabled-control/artwork-error states. This source comparison closes none of those checks.
