# Mobile Delivery Discovery Baseline

Status: Discovery baseline — agreed product direction and implementation questions; not yet an implementation specification.

Date: 2026-09-19

## Purpose

This document consolidates the decisions made while defining how the TellPal backend and a new mobile application should work together. It is intended to guide the next phase, where the legacy application's screens and endpoint traffic will be compared with the new API contract.

The current scope is the mobile delivery of CMS-managed content. RevenueCat integration and application-action analytics are intentionally deferred.

## Confirmed direction

### 1. Mobile backend shape

- A separate top-level mobile business module is not required at this stage.
- Mobile HTTP adapters should remain close to their owning business modules, while cross-domain composition should use application APIs, events, or stable identifiers.
- The final API contract must be checked against the existing Spring Modulith boundaries before implementation.

### 2. Content discovery

The initial mobile experience should support:

- Home sections and curated category rows.
- Category listing with CMS-defined ordering.
- A general content-only search across all applicable content types.
- Search relevance based on content metadata, including contributor information, while returning content results rather than contributor records.
- Grouped presentation by content type/experience where appropriate.
- A story appearing in both reading and audio-story discovery groups when both experiences are available.
- Hiding categories that have no eligible content for the current language/audience.
- Small list responses; full delivery data belongs to the detail/start flow.

The home screen currently shows a limited number of items per category and provides access to the complete category listing. The exact pagination, sorting, empty-state, and home composition contracts remain to be written.

### 3. Publication and revision model

- Publication applies independently by language and by story experience.
- A story can have independent reading and audio-story release states.
- Test/device-preview publication is a distinct intermediate state. Authorized tester accounts may consume test candidates, including locked candidates, but ordinary users must not receive them.
- Test candidates should carry a visible test/revision indicator in detail or player surfaces. This is not an authorization mechanism.
- There should be at most one active candidate for a content + language + experience scope.
- Candidate revisions are immutable in practice: a material edit creates a replacement candidate and resets affected testing/approval.
- A live revision remains available while a candidate is being edited or processed.
- A previous live revision can be restored through a scoped rollback operation.
- An active playback session remains pinned to the revision admitted at session start, subject to the final withdrawal/security rules.
- General release requires explicit approval after required media preparation succeeds.
- Media changes require preparation and renewed device testing. Metadata-only changes may use a lighter approval flow, provided they do not alter generated media.
- Processing failures block test/public release of the affected release unit, expose the failure and retry path in CMS, and do not replace an already-live revision.
- Retention and cleanup parameters belong to backend/environment configuration. Cleanup must protect live, rollback, and session-required revisions.

### 4. Story playback and delivery

The new mobile application may be designed independently of the current Flutter application's two-ZIP limitation.

For illustrated stories:

- Each page has its own image and narration audio asset.
- Playback starts when page one image and audio are ready; it does not wait for the whole story.
- During page playback, the next page and preferably a bounded lookahead are prepared.
- When narration ends, the client advances with an animation. The user may navigate manually.
- Manual navigation may show a loading state under poor connectivity; adjacent-page readiness is the normal objective, not an absolute guarantee.
- The first-page prefetch decision applies on both Wi-Fi and cellular connections and does not authorize whole-story speculative downloading.
- Story artwork is portrait-oriented and shared across phone and tablet. The viewport should be filled with aspect-preserving crop; text-safe margins in the source artwork are intentional. Centered crop is a provisional assumption that must be verified on representative devices.
- A story opens through a detail screen containing its information/description before consumption.

For audio stories and meditations:

- Both have a detail screen and an explicit Listen action.
- Bounded opening-audio preparation is acceptable on the detail screen without autoplay or requiring the whole track before playback.
- The prepared player/cache should be reused when the user starts playback.
- Background playback is required.

For lullabies:

- There is no separate detail screen in the current experience.
- Tapping a list item opens the player and starts user-initiated playback.
- Listing artwork and playback artwork remain separate ownership concepts; animated playback artwork must not be flattened as the only rendition.

The first-release delivery direction is a revisioned manifest with separate per-page image/audio assets. The previous strategy of splitting a story into a first-three-pages ZIP and a remainder ZIP is not required for the new client. ZIP packaging may return later only if measurements or future offline requirements justify it.

### 5. Media preparation

The publish pipeline should eventually produce and validate:

- Listing thumbnails.
- Detail/player renditions where needed.
- Page image assets.
- Page narration audio assets.
- Listening artwork, including animation where required.
- A revisioned manifest with ordered pages, asset identity, and readiness/integrity information.

The client should receive protected media through short-lived URLs after backend access checks. URL generation itself is not expected to be a material cost; storage, download operations, and network egress are the relevant cost drivers. URL lifetime and short-term backend caching are implementation parameters.

The backend must not issue protected full-media URLs merely because a locked item is visible in a list or detail screen. Public metadata and protected playback access must remain separate.

### 6. Identity and profiles

- On first launch, the user chooses registration or anonymous continuation before entering the normal catalog flow.
- Anonymous continuation creates a Firebase Anonymous Auth user.
- Registered authentication uses Firebase Auth with email, Google, and Apple in the initial scope.
- Email verification and password reset are included in the initial account flow.
- Firebase Auth is the authentication authority; the existing backend/local user record remains the canonical application profile.
- The verified Firebase UID maps to the local application user/profile.
- Anonymous profile and language data must survive conversion to a registered account.
- Signing in to an existing account uses that account's existing profile data.
- Multi-provider credential linking/account consolidation is deferred. Accounts must not be silently merged because provider email values look similar.
- The first release has one profile per account. Profile age is informational only.
- Anonymous and registered users may access the same content; login type alone does not grant paid access.

### 7. Language

- Application language and content language are managed together in the initial experience.
- The selected language is stored on the account/profile.
- Users can change the selected language.
- Content publication and free-access selection can differ by language.
- If the selected language is unavailable for a content item, English may be used as the fallback in the initial release.
- Language fallback must not silently expose an unpublished or unauthorized experience.

### 8. Access, free content, and subscription boundary

- Users can discover the full visible catalog even without a subscription.
- Paid items appear locked; attempting to start them opens the paywall.
- CMS-designated free content is playable without paid access.
- Free selection may differ by language and applies to both reading and standalone listening for a STORY.
- A non-subscriber-focused home category may contain both free and locked content. It is shown only to users without active subscription access and follows CMS ordering.
- Subscription users should not see that non-subscriber-only category.
- A cancelled subscription remains active until the paid access period expires; RevenueCat will later provide the trusted entitlement state.
- Tester permission is separate from subscription entitlement and can allow access to test candidates.
- RevenueCat purchase, restore, entitlement refresh, and server-side subscription verification are deferred, but the API must leave a clear boundary for them.

### 9. Offline and cache direction

- A future version must support offline use.
- Explicit offline download management is not required in the first release.
- The current client's five-ZIP local cache is historical behavior, not a constraint for the new client.
- The future design should distinguish evictable playback cache from explicitly retained offline content.
- Stable revision/asset identity, completeness checks, and resumable downloads should be designed into the manifest foundation even if the first release does not expose offline controls.

## Repository evidence to revalidate

Earlier inspection indicated that the backend already has mobile adapters for content, category, and user concerns, and response models that refer to covers, packages, optimized audio, and expiring URLs. It also indicated that some media-processing adapters currently describe/register processing plans rather than proving that transformed files are present in storage.

These findings are leads for implementation planning, not acceptance evidence. They must be rechecked against the current branch and runtime behavior before work is scheduled.

## Sample content evidence

The supplied sample story contains 24 numbered page JPG/MP3 pairs and two Turkish cover images. The page images are portrait-oriented at approximately 1712 × 2778 pixels; page audio is mono MP3 at approximately 192 kbps/48 kHz. The folder is approximately 24 MB, with approximately 8 minutes 45 seconds of narration. The first three pages are approximately 3.4 MB before packaging.

This supports a bounded first-page/adjacent-page startup strategy. It does not prove that a fixed three-page ZIP is optimal for every story or device.

## Next phase inputs

Before defining the implementation stories, collect from the legacy application:

1. Screenshots of home, category, search, detail, player, login, paywall, language, and profile flows.
2. The endpoint list used by each screen.
3. Representative request/response payloads, including authentication headers and error responses.
4. Examples of locked, free, test, unpublished, and language-fallback content.
5. Playback/cache behavior observed on both phone and tablet.

Use these artifacts to produce the concrete mobile API contract, authorization matrix, manifest schema, and migration/coexistence plan.

## Open implementation questions

- Exact endpoint paths, response DTOs, pagination, ordering, and error contracts.
- Whether a full manifest is fetched at detail or start, and how many adjacent assets are requested at once.
- URL time-to-live, renewal, cache policy, and behavior after a revision is withdrawn.
- Backend entitlement checks before protected media delivery while RevenueCat is deferred.
- Real image/audio transformation, thumbnail dimensions, codecs, quality budgets, and animated artwork handling.
- Processing orchestration, idempotency, retry, readiness checks, and publish gating.
- Persistent progress/resume behavior after application restart.
- Offline cache budget, eviction, download resume, and complete-story semantics.
- Coexistence period and compatibility requirements for the legacy mobile client.
- Observability and crash/performance instrumentation; action analytics remains deferred unless separately approved.
