# Discovery reconciliation review

Date: 2026-09-22. Read-only review of the discovery baseline against the PRD, addendum and decision log; this file is a supporting audit, not a new product decision.

Sources: `be/docs/mobile-delivery-discovery.md`; sibling `prd.md`, `addendum.md`, `.memlog.md`. The primary documents were reread after the parent agent's edits. Initial findings are preserved as an audit trail; the post-edit verification below is the current result.

## Authority and later decisions

Later explicit product decisions override the discovery baseline. In particular:

- Interactive branches/choices and parent mode/guides are excluded from v1. Do not restore them from the older extraction files.
- Search scope remains open and requires real contracts/evidence. `/story/search` and `StorySearchByCategoryType[]` do not prove that legacy search returns only stories; the earlier story-only description was an unsupported inference.
- RevenueCat and backend flow logging are v1 pre-release integrations, scheduled late in development, not post-release deferrals.
- The accepted target is at least 99.5% crash-free users per platform. The sampling window/size and memory/latency budgets remain open; the approximately 85% legacy number is unverified.
- Anonymous conversion to a newly registered account preserves all profile/preferences/history/progress. Existing-account login uses existing-account data without automatic merging.
- Explicitly accepted language visibility, interruption handling, cold-start/background resume behavior, one-time migration, deletion scope and gradual rollout must remain intact.

## Discovery section coverage in the input snapshot

| Discovery section | Existing PRD coverage | Gap or reconciliation needed |
|---|---|---|
| Purpose / deferred integrations | Pre-release integration section; FR-19–21 | Later decision correctly overrides original deferral. Introductory logging bullet still says event scope will be defined even though FR-21 defines the accepted categories; only fields/retention/retry details remain open. |
| 1. Mobile backend shape | Generic statement that architecture owns endpoint/DTO/framework decisions | Carry existing module-boundary direction into the architecture addendum: no new top-level mobile business module currently required; owning-module HTTP adapters; cross-module application APIs/events/IDs; verify Spring Modulith boundaries before implementation. This is inherited direction, not a fresh design. |
| 2. Content discovery | FR-6–8 and OQ-4 | Catalog ordering, eligibility, grouped experiences, editor/similar content, search result nature are present. Explicit lightweight list payload versus detail/start delivery separation is missing. Preserve unresolved search scope. |
| 3. Publication and revision | FR-22–25, UJ-7, OQ-8 | Strong coverage of independent scopes, one active candidate, replacement candidate/reset approvals, live continuity, rollback, tester isolation, failure gates and retention protection. Retention/cleanup parameters as backend/environment configuration and optional lighter metadata-only approval boundary are not explicit; latter remains an open technical boundary, not unconditional permission. |
| 4. Playback and delivery | FR-11–16 | First-page readiness, bounded Wi-Fi/cellular lookahead, manual/automatic progression, listening detail/start, reuse, background, direct lullaby and animation covered. Explicit v1 revisioned manifest/per-page image/audio delivery is absent as a binding direction. Story full-viewport aspect-preserving crop is only partly carried; centered crop is provisional and must stay provisional. Detail audio preparation must not require the complete track before playback. |
| 5. Media preparation | FR-15, FR-24–25, OQ-8 | Missing explicit rendition coverage (list thumbnails, applicable detail/player images, page images/audio, animated listening artwork), ordered asset manifest with identity/readiness/integrity, and short-lived protected URL direction. Access checks and no full-media access from visibility alone are present. Keep concrete sizes/codecs/TTL/cache strategy open. |
| 6. Identity and profiles | FR-1–5, user needs, migration sections | One profile, informational age, provider set, verification/reset, anonymous continuation and no silent email merge covered. Explicit Firebase authentication authority versus canonical backend application profile and verified UID mapping are only implicit. Catalog entry after identity choice and equal content eligibility for anonymous/registered users should be explicit without granting paid access. |
| 7. Language | FR-9–10 | Later decisions remain authoritative. On 2026-10-03 the user rejected automatic content-language fallback and allowed deliberately curated/labeled different-language categories. This supersedes the earlier conditional English-fallback direction; current PRD FR-9 governs. |
| 8. Access/free/subscription | FR-10, FR-19–20, FR-23, FR-25 | Strong coverage. Explicit non-subscriber-only category may mix free and locked items is not stated; clarify if describing existing discovery direction. Tester permission remains distinct from subscription. RevenueCat is required before release. |
| 9. Offline/cache | Scope exclusions; FR-15; OQ-8 | Explicit offline controls are correctly excluded and old ZIP limits not binding. Foundation requirements—stable revision/asset identity, completeness and resumable download support, distinction between evictable cache and retained offline content—are missing or overly generic. These do not authorize adding a v1 download-management UI. |
| Repository evidence | Opening disclaimer; FR-24; assumptions | Properly states backend not verified. Keep actual transformed media existence/readiness checks; a registered processing plan is not proof of a ready asset. |
| Sample content evidence | Image/RAM hypothesis and NFR-2–3 | Sample evidence can inform test fixtures but is not a required universal resolution/codec/page count or proven root cause. No need to copy sample numbers into binding FRs. |
| Next phase inputs | Source inventory links; NFR-6–7; OQ-1/4/8 | Add a compact evidence checklist: reference screenshots/flows, per-screen endpoints and real payload/error/auth examples, locked/free/test/unpublished/fallback states, phone/tablet playback/cache. No current runtime verification was performed. |
| Open implementation questions | OQ-2/3/4/7/8/9/10 | Keep endpoint/DTO/pagination/errors, fetch timing/lookahead, TTL/renewal/withdrawal, transforms, preparation orchestration, cache budget and operations open. Progress/resume and integration deferrals have later decisions and should not be reopened wholesale. |

## Priority findings from the input snapshot

1. **Decision drift:** NFR-1 and addendum still mark the numeric crash-free threshold open, contradicting the accepted 99.5% target. Only its evaluation method/window/sample remain open.
2. **Inherited delivery direction too implicit:** Record revisioned per-page manifest delivery, necessary media renditions/integrity, short-lived authorized URLs and offline-compatible asset foundations without selecting endpoint/schema/codec details.
3. **Architecture handoff omission:** Capture discovery §1 in addendum so downstream architecture does not accidentally infer an entirely new mobile business module.
4. **Overbroad replacement prose:** Introductory full-replacement/data-use statements and retained parent-mode preferences should distinguish preserved historical data from explicitly excluded parent/interactive UI. The treatment of excluded-content history in the new UI needs clarification; it is not permission to discard data or restore excluded features.
5. **Anonymous/logging summaries lag:** UJ-2 and earlier anonymous prose mention only profile/language while FR-3 preserves all anonymous data; logging summary lags accepted FR-21 event scope. Align summaries with the accepted detailed rules.
6. **Supporting extraction staleness:** `extract-contracts.md` predates later exclusions and decisions; it includes interactive and parent functionality as then-in-scope. Mark it historical or subordinate to current decisions rather than treating its findings as current authority.

## Post-edit verification

All nine discovery direction sections are now represented in the PRD or architecture addendum without overriding later accepted decisions. No remaining material omission of the discovery baseline was identified in this document comparison.

| Initial finding / section | Current result |
|---|---|
| Crash threshold drift | **Resolved.** NFR-1, success criteria and addendum consistently preserve at least 99.5% crash-free users per platform. Evaluation window/sample and memory/latency budgets remain open. |
| Backend module boundaries | **Resolved.** Addendum preserves owning-module adapters, cross-domain API/event/ID boundaries, no required new mobile business module, and Spring Modulith verification. |
| Small catalog responses | **Resolved.** FR-6 separates lightweight lists from detail/start delivery data; exact DTO/pagination contracts remain OQ-4. |
| Per-page delivery and preparation | **Resolved.** FR-12 explicitly carries revisioned manifests, separate page image/audio, first-page readiness and bounded lookahead. FR-15 covers bounded listening preparation/reuse, no complete-track prerequisite, portrait crop and provisional centered framing. |
| Media outputs and access | **Resolved.** FR-24 enumerates prepared renditions, ordered asset manifest and identity/readiness/integrity, with actual files required. FR-25 preserves short-lived authorized delivery, backend subscription checks and configured protected retention. |
| Publication nuances | **Resolved.** FR-23 states separate tester status and visible marker placement; lighter metadata-only approval is conditional and its boundary remains OQ-8. |
| Identity/profile ownership | **Resolved.** FR-1 places identity choice before catalog; FR-2 names Firebase authority and canonical backend profile; FR-10 preserves equal anonymous/registered eligibility under equivalent access conditions. |
| Mixed non-subscriber category | **Resolved.** FR-10 explicitly permits free and locked items together. |
| Offline foundation | **Resolved.** NFR-9 and addendum preserve stable identities, integrity/completeness, resumable foundation and distinction between cache and retained offline media. No v1 offline manager was introduced. |
| Evidence handoff | **Resolved.** Addendum and PRD evidence limits include screenshots, screen-to-endpoint mapping, redacted payload/errors, access/publication states and phone/tablet observations. Sample figures remain reported, not independently remeasured or binding parameters. |
| Summary drift | **Resolved.** UJ-2 preserves all anonymous profile/preferences/history/progress; introductory logging summary points to accepted FR-21 scope. Earlier anonymous prose naming profile/language is a narrower summary, not a restriction on FR-3. |
| Excluded features / historical reports | **Resolved as scope; implementation treatment remains open.** PRD explicitly excludes interactive/parent functions, retains FR-17 as withdrawn, and separates preserved history from playable features under OQ-3. Addendum marks older extraction reports as historical, not active scope. |
| Search evidence correction | **Resolved.** FR-7 and OQ-4 keep actual returned legacy types and target scope open; endpoint/model names are not treated as proof of story-only behavior. |
| Account deletion across common identity | **Explicitly open, not resolved by fiat.** FR-5/addendum/OQ-6 preserve new-system-only deletion as the accepted product outcome while requiring evidence for shared Firebase identity handling, subscription explanation and prevention of remigration restoring deleted data. |

Remaining open work is intentionally identified in OQ-1/2/3/4/6/7/8/9/10 rather than being silently selected by this reconciliation. In particular, search scope, common-identity deletion mechanics, removed-content history presentation, media schema/TTL/retention, crash measurement sufficiency and runtime evidence remain unresolved. This is document coverage, not approval of those technical decisions.

## Limits

This review does not inspect backend implementation, live Firebase/RevenueCat configuration, production data, media storage or Crashlytics. It establishes document consistency only. It does not choose Flutter/native, solve migration identity mapping, settle search scope, decide retention/TTL/codecs, or certify release readiness. The PRD remains draft while open decisions and evidence requirements persist.
