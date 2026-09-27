# Adversarial review — TellPal mobile architecture spine

Reviewed: `ARCHITECTURE-SPINE.md` (2026-09-26 draft), through the BMad adversarial lens. This review tests whether independent feature teams can obey every adopted AD and still deliver incompatible behavior. It does not treat intentionally deferred endpoint fields or package versions as defects by themselves. The concern is where no binding cross-feature contract, owner, or acceptance gate exists when those choices must converge.

## Verdict

The spine supplies useful module and safety boundaries, but is not yet a sufficient build substrate for parallel auth, migration, catalog, purchase, and media work. Two teams can satisfy AD-1–AD-9 while choosing incompatible state, identity, media, and access semantics. The deferred production contracts are appropriate only if their resolution is an explicit prerequisite for all consuming stories, not merely a per-feature task.

## Adversarial construction

Both teams use one Flutter app, Riverpod feature owners, one application media coordinator, one authenticated transport, a backend-owned idempotent account migration, central routing, and separate durable and evictable storage. Both keep the PoC route out of production.

| Cross-feature seam | Team A, compliant implementation | Team B, compliant implementation | Integration failure |
| --- | --- | --- | --- |
| Session and migration state | Auth exposes `authenticated(uid)` immediately; migration owns an independent `pending/importing/complete` state, and the router waits for both. | Auth exposes only `ready(profileId)` after backend status; migration emits completion to auth; the router watches auth alone. | Sign-in, startup, retries, and account switch differ in which state is authoritative and when protected screens unlock. |
| Anonymous upgrade | Firebase `linkWithCredential` retains the anonymous UID and backend profile. | A new Firebase UID is created and the old anonymous profile is imported into it. | One backend identity mapping and migration protocol cannot accept both without an explicit branch and provenance. |
| Device import | Acknowledgement means an HTTP `202` accepted upload; local records are marked imported. | Acknowledgement means a terminal backend receipt covering each record, then local records are marked imported. | Interrupted processing can lose data on A's device while B safely retries; both read “acknowledged” differently. |
| Catalog and media identity | `contentId` plus language and revision identifies the playable manifest; `experience` selects client presentation. | Distinct IDs for story, narration, and listening rows identify media; revision is looked up afresh on renewal. | Deep links, progress, cache entries, and URL renewal do not address the same content or admitted revision. |
| Entitlement and playback | Catalog's access snapshot admits playback; media coordinator trusts that admission until session ends. | The backend checks access at every protected manifest/URL renewal and the coordinator reacts to revocation. | Expiration, purchase restore, account switch, and background playback yield different results. |
| Media lifetime | Route-independent session keeps its revision and source after navigation; cache retains referenced bytes. | Session remains allocated, but navigation clears its source and cache eviction can remove active bytes. | Both satisfy “route disposal does not dispose the session,” while route changes or URL expiry break playback in B. |

## Findings

Each finding states a trigger, a concrete guard, and the consequence. There is no ranking: all are cross-team compatibility questions to settle before their consumers implement against divergent assumptions.

### 1. Session authority is named but not modeled

- **Location:** AD-2, AD-4, AD-6; Capability → Architecture Map.
- **Trigger condition:** Auth, migration, access, and profile each own feature state, while the router derives redirects from an “authoritative session/migration/access state” whose owner, union of states, and precedence are unspecified.
- **Guard snippet:** Define one application session projection with explicit `signedOut`, `resolvingIdentity`, `migrationPending`, `migrationFailed`, `ready`, and `switchingAccount` transitions. Name which feature publishes each input and the only component that decides route admission. Specify startup and token-expiry behavior.
- **Potential consequence:** Both teams above meet AD-2/AD-6 yet can unlock catalog at different times, loop between gates, or show prior-user state during account switching.

### 2. The Firebase UID to backend-user mapping lacks a lifecycle invariant

- **Location:** Consistency Conventions — Identity; AD-4 and AD-8.
- **Trigger condition:** The spine distinguishes Firebase UID from backend profile ID but does not say whether a UID is a permanent one-to-one key, whether profile creation is atomic with migration status, or how anonymous linking and provider changes affect the mapping.
- **Guard snippet:** State the identity relation and its allowed transitions as a backend-owned contract. Include identity provenance and a response that identifies the canonical backend user before profile/progress mutations. Forbid clients from creating or merging profiles by email or inferred ID.
- **Potential consequence:** Auth, profile, purchase, and migration adapters can each address a different backend user while individually following the listed identity convention.

### 3. Anonymous upgrade has two incompatible meanings

- **Location:** AD-4; Consistency Conventions — Identity; Deferred.
- **Trigger condition:** The PRD requires anonymous data to move into a newly registered account and forbids automatic merge into an existing account, but the spine does not choose Firebase credential linking versus creating a replacement UID, nor identify who transfers the anonymous backend profile.
- **Guard snippet:** Add an explicit transition table for anonymous → new account and anonymous → existing account: Firebase operation, source/destination UID, backend profile action, failure recovery, and the rule that existing-account sign-in leaves anonymous data unmerged unless a separate approved action exists.
- **Potential consequence:** Two compliant auth/migration teams can implement opposite UID continuity and silently merge data or strand progress. This must also be reconciled with PRD FR-2 and OQ-3.

### 4. Migration completion and device-import acknowledgement are ambiguous

- **Location:** AD-4; Deferred first and second bullets.
- **Trigger condition:** “Terminal status,” “verified completion,” and “import acknowledged” do not define whether success means upload accepted, batch processed, every record mapped, or user profile available. Account migration and device-only import are separate but their route-gating relationship is not stated.
- **Guard snippet:** Define distinct account-migration and device-import state machines, durable operation IDs, per-record acknowledgements or a verified batch receipt, and exact client deletion/retention rules. Make terminal success contingent on validated persisted records; distinguish retryable, conflict, and unmappable outcomes.
- **Potential consequence:** A client can delete sole local copies after a queued `202`, or release the migration gate while required device records are unresolved. PRD FR-26–FR-28 requires no silent omission or loss.

### 5. Completed account migration conflicts with later device-only records without a precedence rule

- **Location:** AD-4; AD-8; Deferred second bullet.
- **Trigger condition:** A second device must not rerun account migration, but it may contain unique local records; the spine defers OQ-3 without a common merge/duplicate identity rule for those records.
- **Guard snippet:** Block migration acceptance stories until OQ-3 yields a record taxonomy, stable source IDs, duplicate detection, conflict precedence, unmappable-record treatment, and an import API that never reopens account migration. State whether pending local data blocks normal use.
- **Potential consequence:** Teams can both preserve local inputs but one discards them as already migrated and another overwrites newer backend progress. The account-level invariant alone does not protect device-only data.

### 6. New-system account deletion can resurrect migrated data

- **Location:** AD-4 and Consistency Conventions — Identity; Deferred.
- **Trigger condition:** The PRD addendum's OQ-6 requires new-system-only deletion without deleting the shared legacy Firebase identity or remigrating deleted data. The spine says account migration is terminal but has no deletion/tombstone transition.
- **Guard snippet:** Make deletion a backend identity-lifecycle state with a durable remigration prohibition keyed to the verified UID and legacy source identity, plus explicit subscription and anonymous-account handling. Resolve OQ-6 before account-deletion or migration acceptance criteria.
- **Potential consequence:** A later sign-in can recreate a deleted profile from the still-live legacy backend, or deletion of Firebase identity can unexpectedly disable the old app.

### 7. Content and experience identity can diverge across feature adapters

- **Location:** Consistency Conventions — Content/media identity; Capability → Architecture Map.
- **Trigger condition:** “Carry content ID, language, experience and revision separately” does not specify canonical content type, experience identifiers, language fallback identity, or the keys used for deep links, progress, and access. Backend ADR-0012 treats `AUDIO_STORY` as a STORY experience, not a canonical content type.
- **Guard snippet:** Publish one shared immutable `ContentRef`/`ExperienceRef` vocabulary and mapping examples for illustrated STORY, STORY narration, MEDITATION, and LULLABY, including selected versus resolved language. Require catalog, reader/listening, progress, cache, and event adapters to use it.
- **Potential consequence:** Two teams can both carry four fields but persist narration under different IDs or languages, yielding missing progress, wrong assets, or cross-experience access checks.

### 8. Manifest admission is undefined at the client/backend boundary

- **Location:** AD-3, AD-7, AD-8; Consistency Conventions — Content/media identity; Deferred first bullet.
- **Trigger condition:** The spine promises an “admitted revision” but never says which response admits it, what ordered pages and assets it binds, whether it includes hashes/sizes, or which component checks completeness before playback. ADR-0017's current allowlisted experiment manifest is explicitly not the production contract.
- **Guard snippet:** Before reader/listening stories, ratify a production manifest example with content/experience/language/revision, ordered asset IDs, integrity metadata, access outcome, and error variants. Name the admission owner and ensure every downstream media command carries the same immutable manifest identity.
- **Potential consequence:** Catalog, reader, and media teams can accept different revisions or mismatched page sets, causing wrong-page audio, impossible cache verification, or accidental dependence on the test route.

### 9. Renewal and retention do not protect an admitted playback revision

- **Location:** AD-3; Consistency Conventions — Content/media identity; Deferred third bullet.
- **Trigger condition:** The client is told to preserve an admitted revision through URL renewal, but the production backend contract and revision-retention handshake are deferred. A backend may expose only current-revision URLs or clean old objects after publication.
- **Guard snippet:** Define a revision-addressed renewal operation and retention horizon/lease that covers active playback, background suspension, app restart, and rollback. Specify behavior when entitlement changes or an asset is unavailable, including whether playback stops or may finish from already authorized bytes.
- **Potential consequence:** A story updated mid-session renews to mixed media or fails after URL expiry, despite the client obeying AD-3 and carrying the old revision.

### 10. “One media session” does not specify command arbitration or state ownership

- **Location:** AD-3; Design Paradigm diagram; AD-2.
- **Trigger condition:** Reader and listening controllers both command the coordinator, but the spine leaves start/takeover, stop, seek, completion, interruption, route change, and cold-relaunch transitions undefined. “Rejects stale completions” covers only one race.
- **Guard snippet:** Publish a media session state machine and command contract with a monotonically changing session token, takeover semantics, origin-independent controls, resume policy, and which component writes progress/completion. Include page versus continuous-audio playback and loop mode.
- **Potential consequence:** Independent features can each issue valid commands that interleave into duplicate completion events, wrong progress, or playback continuing under the wrong account and access context.

### 11. Account switching needs a teardown barrier across state, media, and cache

- **Location:** AD-6, AD-8; AD-3.
- **Trigger condition:** AD-8 orders account-private cache purging, but does not order it relative to cancelling in-flight responses, stopping the application-scoped media session, clearing feature states, or accepting the next account's access decision.
- **Guard snippet:** Define one account-switch transaction: invalidate session generation, cancel/ignore old-user requests, stop and clear protected media, purge account-scoped cache, reset feature owners, then resolve the next UID/profile/access before navigation. Key private cache by UID and revision as well as content fields.
- **Potential consequence:** Old-user catalog responses or audio can appear after the new user signs in; a revision-only cache key can return private bytes across accounts.

### 12. Access authority and revocation timing are unspecified

- **Location:** AD-6, AD-8, AD-9; Capability → Architecture Map.
- **Trigger condition:** Backend owns access decisions and RevenueCat handles purchase/restore, but the spine does not say which backend response is authoritative after a purchase, at playback start, during URL renewal, or on resumed background playback. A “fresh access decision” has no time or action boundary.
- **Guard snippet:** Define an authorization matrix for list/detail/manifest/renewal/cache read and a single entitlement refresh sequence after purchase, restore, expiry, account change, and app resume. State whether an already admitted session may complete after revocation. Keep RevenueCat client state advisory until backend access confirms it.
- **Potential consequence:** One team unlocks premium playback from a local RevenueCat result while another waits for backend confirmation; stale cache or background sessions can outlive entitlement.

### 13. The media cache lacks a cross-feature identity and lifetime contract

- **Location:** AD-8; Consistency Conventions — Content/media identity; Deferred third bullet.
- **Trigger condition:** “Bounded, evictable, revision-keyed” leaves cache key composition, partial-download validity, checksums, eviction of active assets, and stale signed-URL handling to separate reader/listening implementations. The PRD requires an integrity/completeness and resumable-download foundation.
- **Guard snippet:** Specify cache entry identity as account/access scope + immutable asset ID/revision + language/experience where relevant; validate length/hash before exposing bytes; use atomic partial-to-complete promotion; lease active entries; and prohibit cached URL/token persistence as content identity.
- **Potential consequence:** A resumed page download can be mistaken for a complete image, active audio can disappear mid-playback, or a new localization can reuse stale bytes.

### 14. Release-required event delivery has no shared event identity

- **Location:** AD-9; Deferred final bullet.
- **Trigger condition:** Backend flow events are release-required and must not block reading; the PRD also says retries must not duplicate events. The spine defers the schema without establishing a shared event ID, producer ownership, or once-per-transition emission for actions spanning feature and media owners.
- **Guard snippet:** Before event implementation, define event producer per user transition, immutable event ID/idempotency key, durable retry queue boundary, privacy fields, and server deduplication outcome. In particular, assign playback start/completion to either media or feature state, never both.
- **Potential consequence:** Background retries duplicate completion and purchase events, or feature and coordinator each log the same action while satisfying the non-blocking rule.

## Exit criterion for the spine

Keep the spine lean: add a compact shared contract inventory rather than endpoint field lists. For each seam above, name the owner, canonical type/state transition, and decision/acceptance artifact that blocks its first consuming story. The highest leverage artifacts are a session/identity state machine, account/device migration protocol, content-and-manifest identity contract, media session/retention contract, and access matrix. The current `Deferred` section already names some of these; it should make their cross-feature dependency and gate explicit.

## Evidence considered

- `ARCHITECTURE-SPINE.md`, AD-1–AD-9, consistency conventions, and deferred decisions.
- `prd.md`, especially FR-2, FR-26–FR-29, NFR-9, OQ-3/OQ-6/OQ-7 and release evidence.
- `addendum.md`, especially identity ownership, manifest/revision foundation, retention, and required pre-implementation evidence.
- Backend `architecture.md` and ADR-0017: canonical STORY experience semantics and the current allowlisted manifest's experimental boundary.
