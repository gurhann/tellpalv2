# Image Revision and Readiness Contract

Status: accepted P1 design, 2026-10-16; **implementation pending**. This document defines future schema/API obligations, not deployed behavior. [ADR-0021](adr/ADR-0021-image-revision-readiness-contract.md) records the decision. The approved [P1 specification](../../_bmad-output/implementation-artifacts/spec-p1-image-revision-readiness-contract.md) is its scope authority; the [parent SPEC](../../_bmad-output/specs/spec-publish-time-image-optimization/SPEC.md) retains downstream qualification and delivery gates.

## Scope and ownership

The initial publication scope is `(contentId, STORY, languageCode, READING)`, with an exact supported language and no fallback. `content` owns the desired publication generation, source bindings, ordered page snapshot, editorial/access eligibility and live pointer. `asset` owns immutable image identities, preparation jobs, verification evidence and ready manifests. `user` and existing delivery adapters retain authentication/admission responsibilities. Neither module imports another module's entities, repositories or infrastructure. Public module APIs/events/IDs carry immutable value snapshots; there is no new module edge or shared media aggregate.

| Role | Source owner and binding | Initial contract requirement |
| --- | --- | --- |
| `READING_COVER` / `READING_DETAIL` | `content`: exact-language localization `cover_media_id` | Both role obligations must be present in the required-role snapshot. They may reference the same verified output only if both admitted role/profile contracts are satisfied. Profile ladders and dimensions remain unselected. |
| `READING_PAGE` | `content`: each ordered page's exact-language `illustration_media_id` | Every page has a verified required output set; preserve page identity, order and language. An empty page set is invalid for this STORY reading scope. |
| Existing `LISTING_COVER` | Exact-language cover, under ADR-0019 | Existing job/manifest and card-placeholder behavior remain unchanged. It does not prove reading-cover/detail/page readiness; cross-role reuse requires explicit compatible role/profile admission. |
| STORY textless cover/page source artwork | Content/page source fields, under ADR-0006/0010 | Authoring inputs, never a replacement for localized reading artwork. Editing only these fields does not silently replace a localized source. |
| Shared listening cover / STORY narration | Content-scoped listening cover; separate narration audio | Outside the initial image scope. No narration/audio/ZIP readiness is inferred. |
| LULLABY static listing and playback/detail | Distinct `listing_cover_media_id` and `listening_cover_media_id` | Outside the initial scope. Static and potentially animated artwork remain distinct; no flattening or generic cover job. |
| MEDITATION / other roles | Existing owners | Outside the initial scope; require a separate approved extension. |

This contract preserves ADR-0006, ADR-0008, ADR-0010, ADR-0017 and ADR-0019. Existing environment-prefixed Firebase ownership, tester allowlists, publication flags and consumers remain operationally unchanged by P1.

## Typed identities and canonical fingerprints

The following are conceptual types, not new Java classes, tables or endpoints:

| Type | Owner | Meaning and equality |
| --- | --- | --- |
| `PublicationScope` | `content` | Positive content ID, canonical type, exact lowercase `LanguageCode.value()` language and experience. Every pointer/job association validates the entire scope. |
| `PublicationGeneration` | `content` | Monotonically advancing value within one scope. Identifies the desired publication snapshot, independently of media bytes. Repeated identical Publish commands may return the same outstanding generation. |
| `SourceBindingSnapshot` | `content`, with `asset` source evidence | Role, localized binding identity and ordered page IDs/numbers, plus observed immutable source identities. Missing localization or zero pages is explicit invalid input, not an old snapshot. |
| `SourceFingerprint` | `asset` | SHA-256 of actual retained encoded source bytes, accompanied by observed byte count/MIME and immutable storage generation/version or verified immutable capture. The byte digest is the reuse identity; the complete observation proves the binding. An asset ID, path, timestamp or registry checksum alone is insufficient. |
| `ProfileFingerprint` | `asset` | SHA-256 of the complete captured role-specific transformation/admission descriptor below. A display label such as `q85` or mutable configuration version alone is insufficient. |
| `RenditionKey` | `asset` | Domain-separated hash of role, actual source-byte fingerprint and complete profile fingerprint. It excludes publication generation, job attempt and signed URL. |
| `MediaRevisionKey` | `asset` | Domain-separated hash of scope and ordered role/source/profile obligations, including page IDs/numbers. Page reorder/removal changes it; a non-image edit can preserve it. |
| `ReadyIdentity` | `asset` | `(MediaRevisionKey, ReadyManifestDigest)`, where the digest binds the complete immutable verified output manifest. It is bound to the scope; it is not an access grant. |
| `AttemptToken` | `asset` | Job ID, attempt number, lease identity/expiry, captured source-set fingerprint and profile-set fingerprint. Both terminal callbacks require the complete captured token. |

### Capture and freshness

Source inspection, bounded download, hashing, decoding and immutable source capture occur **outside database transactions**. The source-admission API must bind its observation to the current source binding/version before a generation can adopt it. Future source registration/replacement must provide a durable immutable byte version and invalidate affected desired candidates when that version changes. If a mutable storage path cannot provide that guarantee, copy/capture and verify immutable source bytes before admission, or reject it; an unversioned path cannot certify freshness.

The desired snapshot names those actual bytes. Replacing bytes behind the same asset ID requires a new source fingerprint and invalidation before adoption; a lookup keyed only by that ID cannot return an eligible current result. The worker rereads the pinned source version or verifies the captured SHA before transforming. A source/profile capture that loses a binding-version comparison is discarded/retried, not attached to a newer generation. P1 does not implement immutable source capture or claim that existing object paths already enforce it.

### Captured profile descriptor

The descriptor is immutable and contains every effective setting, including defaults: contract/schema and role version; accepted input formats and frame/pixel/byte limits; output variants, geometry/rounding, fit, aspect/no-upscale rule and resampler; normalization order and orientation/mirroring, alpha/background, color/ICC/bit-depth and animation policy; codec, exact encoder/provider/binary and runtime identity, quality, chroma subsampling, progressive/optimization/lossless settings and metadata handling; reuse rules and equal/growing-byte admission; and operational bounds for duration, memory, temporary space, concurrency and encoder threads. Every additional effective implementation setting must be captured, rather than silently inheriting a host default. A changed setting creates a new profile fingerprint even when a human version label or asset ID is unchanged.

Unsupported alpha, orientation/mirroring, color or animation semantics remain ineligible until a qualified explicit profile covers them. Existing white-matte/JPEG experiment behavior is not automatic admission for this contract. Production numbers, codecs and normalization policies remain gated; no values in the examples select them.

### Canonical hash examples

Define `H(domain, value) = lowerhex(SHA-256(ASCII(domain) + LF + canonical(value)))`. Parse JSON into typed values before canonicalization; reject duplicate decoded member names, non-ASCII decoded strings/names, floats and undeclared fields/defaults. Canonical form is UTF-8 JSON with lexicographically sorted decoded ASCII member names, no separator whitespace/BOM/trailing newline, arrays in semantic order, decimal integers without leading zeros (`0`, never `-0`), and lowercase `true`/`false`/`null` only where the schema permits those values. Strings retain their decoded characters with exactly this escaping policy, for both member names and values:

- U+0020–U+007E are literal ASCII bytes, except double quote U+0022 is `\"` and backslash U+005C is `\\`. Slash U+002F is always literal `/`, never `\/`; printable characters never use `\u` escapes.
- Backspace U+0008, tab U+0009, line feed U+000A, form feed U+000C and carriage return U+000D use exactly `\b`, `\t`, `\n`, `\f` and `\r`, respectively.
- All remaining U+0000–U+001F controls and DEL U+007F use exactly six ASCII characters `\u00xx`, with two lowercase hexadecimal digits; no literal controls, alternate escapes or uppercase hex. Other decoded code points are ineligible for this schema rather than silently normalized.

Equivalent input escape spellings therefore yield identical canonical bytes. No case folding, slash/path normalization or other semantic string rewrite is performed. IDs/enum values are typed by member names and schema. The profile's complete descriptor is hashed first; nested fingerprints use the resulting lowercase 64-character hex. Unknown schema/domain versions are rejected. Production descriptors must encode all effective settings listed above; arbitrary strings cannot stand in for an omitted descriptor.

These small **serialization fixtures are not images or deployable profiles**. `b"abc"` and `b"abd"` stand for two observed encoded-byte sequences solely to demonstrate same-ID byte changes. The fixture profile deliberately has only `fixtureSetting`; it is never eligible for READY.

```text
SHA-256(b"abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
SHA-256(b"abd") = a52d159f262b2c6ddb724a61840befc36eb30c88877a4030b65cbe86298449c9
domain = tellpal.profile.fixture.v1
canonical = {"fixtureSetting":"A","schema":1}
profile A = 109be3ad7fffcd6e1a5aadd356ff20ee43fb10451b8cc72b5afe891e513bc6b1
canonical changed profile = {"fixtureSetting":"B","schema":1}
profile B = 7ee75c51c36ccf81b746d265721125d1cf6b16c93817ee5463e26ce8aedc5a74
domain = tellpal.rendition.fixture.v1
canonical = {"profileSha256":"109be3ad7fffcd6e1a5aadd356ff20ee43fb10451b8cc72b5afe891e513bc6b1","role":"READING_PAGE","schema":1,"sourceSha256":"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}
rendition A = c7605439b944114472f45b18e37c386c91c5e21047fc099114edf1ac9c58ffd6
same asset ID, b"abd" instead = f617e4dfe5639dbcb8d7a9acda9dd18bdcaedfa3fd979eab84292950410f0f17
same b"abc", profile B instead = bf8ec629c8d885530a2e78b83f8ff8b8814ed343b93ccb0d1ef2e34af9110daa
domain = tellpal.revision.fixture.v1
canonical = {"contentId":7,"experience":"READING","language":"tr","pages":[{"pageId":11,"pageNumber":1,"renditionKey":"c7605439b944114472f45b18e37c386c91c5e21047fc099114edf1ac9c58ffd6"}],"schema":1,"type":"STORY"}
revision = feade7c9ee646245e2e95669ab3fd2c672ca2bedd4cf1ca678fe5d7605e7e81e
same media, publication generation 8 instead of 7 = same revision
```

The revision fixture includes content/language/experience, role and ordered page identity. Changing language, role, page identity/order, source bytes or profile changes the corresponding canonical input. Publication generation is deliberately absent: generation 8 may reuse the same media while publication policy is rechecked. Asset IDs remain provenance bindings in the desired snapshot; matching an ID never overrides a different byte fingerprint. See the independent recomputation record below.

This additional **non-profile serialization fixture** exercises printable characters, slash, quote/backslash and every control-escape category. The two JSON inputs decode to the same member name and string; their lexical escape choices cannot change identity:

```text
domain = tellpal.serialization.fixture.v1
input A = {"text":"A/\"\\\b\f\n\r\t\u0000\u001f\u007f"}
input B = {"\u0074ext":"\u0041\/\u0022\u005c\u0008\u000c\u000a\u000d\u0009\u0000\u001F\u007F"}
canonical A = {"text":"A/\"\\\b\f\n\r\t\u0000\u001f\u007f"}
canonical B = {"text":"A/\"\\\b\f\n\r\t\u0000\u001f\u007f"}
digest A = 1344b5810263686899aad5407311faa026d5da31ebf32a9eea34f65a32884234
digest B = 1344b5810263686899aad5407311faa026d5da31ebf32a9eea34f65a32884234
```

## Desired, candidate, READY and LIVE

These are separate dimensions, not aliases for legacy status enums:

| State | Owner and transition |
| --- | --- |
| `DESIRED(g)` | `content` durably records Publish intent, generation, ordered bindings and required roles, or records an invalid/withdrawn desired snapshot. |
| `CANDIDATE(g, revision)` | `content` associates the current desired generation with captured immutable source/profile obligations. `asset` schedules durable preparation or returns matching verified reusable media. |
| `PREPARING` / `FAILED` | `asset` owns job state/diagnostics. A missing output, unsupported profile or current attempt failure prevents READY. |
| `READY(revision, manifestDigest)` | `asset` atomically stores the complete verified immutable manifest and completes its owning revision-preparation job. All required outputs and verification receipts are present; there is no partial READY. |
| `LIVE(scope, g, ReadyIdentity)` | `content` atomically adopts exactly that READY identity for the still-desired generation, after current editorial/access eligibility checks. READY alone does not make media ordinarily deliverable. |
| `SUPERSEDED` / `WITHDRAWN` | `content` invalidates obsolete intent. Its media may remain verified/retained but cannot be promoted as current by an old completion. |

An authorized **Publish request automatically starts durable preparation and automatically promotes the verified current revision once all required images are ready**, with no extra per-revision manual approval. Conversion is asynchronous; a command can return a pending result. A repeated Publish/retry for identical current obligations returns existing work/READY and retries the idempotent adoption step. The legacy generic “already completed” error is not the new-contract response for reusable completed work.

A source/order/profile change advances or invalidates the desired generation and requires a newly bound candidate. Metadata-only edits can advance the content generation while keeping the media revision; the new generation explicitly attaches the verified current media and rechecks policy without re-encoding. A changed embedded title requires a new actual localized image, not an assumption that a text edit modified image bytes. Withdrawing/archiving/deactivating cancels promotion eligibility. Relevant access-policy changes also invalidate pending adoption and are rechecked at promotion.

For first publication, absence of a valid READY/adopted revision leaves the new contract unavailable. For replacement, pending/failed/stale work preserves the previous **eligible** live revision and protected session references. Retention is not unconditional visibility: archive, revocation or lost entitlement still applies. “Keep the old publication” does not resurrect an ineligible revision or grant new access. P1 changes no existing runtime publication behavior.

## Atomicity, fencing and restart

1. **Content intent transaction:** `content` checks command preconditions, records generation and binding/order version plus durable preparation intent. Dispatch must survive a restart/commit failure through a durable intent/outbox or equivalent reconciliation; an in-memory after-commit event alone is insufficient. Hashing/storage work is deferred.
2. **Capture and prepare:** public asset contracts observe/pin sources outside transactions, then short database transactions compare the binding/generation and schedule the captured full role set. Unique scope/request and revision/work identities deduplicate overlapping requests. A current job owns a complete revision's required outputs; intermediate verified outputs may be reusable but do not independently certify that revision.
3. **Worker claim transaction:** `asset` atomically claims the durable job and captures `AttemptToken`. Profile changes during execution cannot alter captured work. Lease recovery supersedes the attempt and preserves diagnostics separately.
4. **External work:** bounded download, conversion or admitted copy/reference, upload and independent stored-object reread happen outside transactions. Every required output must pass observed MIME, actual positive byte count, SHA-256, exact dimensions and decode checks against the captured profile; generated metadata or a registry row alone is insufficient. Verification binds an immutable storage object/version, not a path that may later be overwritten.
5. **Asset finalization transaction:** `asset` locks the current job, requires state **`PROCESSING`**, and compares job ID, attempt, exact lease identity/expiry, unexpired ownership, source-set and profile-set fingerprints before any terminal transition. It validates the complete ordered verified manifest and writes manifest plus job `COMPLETED` in **one commit**. Conflict/error rolls back both. If some outputs fail, no READY is written. No storage/conversion calls occur here.
6. **Failure transaction:** the same lock, **`PROCESSING` precondition** and full token comparisons apply before committing `FAILED`. **The first committed terminal outcome wins for each captured attempt**, even if a contradictory callback carries the same still-unexpired token. `COMPLETED` cannot become `FAILED` from late failure; `FAILED` cannot become `COMPLETED`/READY from late success. An identical terminal replay returns read-only idempotent proof of the already committed outcome (and exact manifest equality for success); it performs no new completion, manifest insertion or follow-up transition. A contradictory replay is rejected or recorded only as original-attempt diagnostics, without changing terminal state or any immutable READY record. An explicit retry requeues eligible failed work and obtains a new captured attempt/token on claim; it never reopens the old attempt. A rejected stale/contradictory success followed by its catch/failure callback remains non-mutating. Expired/replaced callbacks may append diagnostics keyed to their original attempt, but cannot fail, complete, queue a follow-up for or overwrite the replacement. Lease expiry after a valid committed completion does not erase READY; read-only replay cannot extend ownership or create new readiness, and adoption/retention still use their separate eligibility checks.
7. **Content adoption transaction:** completion notification/reconciliation asks `content` to compare current desired generation, full scope, candidate source/profile/order identity, editorial/access eligibility and exact asset READY identity, then changes the live pointer atomically. READY lookup goes through `asset::api` within the short database consistency boundary; no foreign entity/repository access or storage signing occurs there. Immutable READY records make delayed notifications safe; integrity withdrawal/availability state must participate in the same eligibility comparison. A changed generation rejects old adoption even when the asset finalization was valid.

The durable adoption step is replayable: restart after READY but before promotion leaves a discoverable pending desired generation; reconciliation promotes only if it still matches. Restart before/after an atomic commit yields either the old state or the complete new state. A crash after upload can leave isolated unreferenced objects; later attempts may reuse them only after full verification and matching identities. Nothing is deleted by this contract. Concurrent adoption uses compare-and-set/locking and generation constraints; at most one current live pointer exists per scope.

### Idempotency and conflict equality

Future database constraints must enforce uniqueness of scope/generation, current intent/live pointers, rendition identity and media revision identity, plus page identity/order and complete required-output membership. An implementation must demonstrate these invariants against PostgreSQL; this document does not choose table/column names or ship a migration.

An insert conflict is success only after **full equality**: schema/scope, ordered role/page/source/profile set, immutable object provider/path/version, observed MIME, bytes, SHA, dimensions and verification/admission facts all match the stored manifest. Operational creation time, notification count and worker attempt are not part of semantic equality. A different output for the same input revision (including nondeterministic encoder output) is a conflict/integrity failure; do not overwrite the object or certify success by key existence. A new profile/version is required where intended output semantics change. Same-output key reuse does not skip stored-byte verification or authorize a new publication generation.

If storage loss/corruption is discovered after READY, delivery and future adoption fail closed for that output; the verification record remains historical evidence rather than proof of current availability. A separately tracked integrity/availability outcome prevents it being silently reused. Repair uses a new verified immutable identity or an explicitly equality-checked restoration, without silently substituting raw bytes or mutating admitted identity. Handling must preserve eligible alternatives under current access policy; it does not promise storage can never be corrupted.

## Reuse, byte growth and lookup modes

Only verified immutable role/source/profile outputs can be reused. A retained original that already satisfies an admitted role/profile may be copied or referenced as a rendition after independent verification and immutable output registration; “original” is not a fallback label. The admitted source and all transformation/normalization requirements must match. An arbitrary original URL, prior derivative or legacy `DETAIL_*` registry row is ineligible. Do not repeatedly lossy-encode derivatives: create required variants directly from the retained original/canonical admitted source, or reuse an already qualifying immutable output.

The explicit profile must state whether equal-size/growing output is allowed and why (for example, qualified geometry/fidelity where size reduction is not promised). Default absence of such admission rejects equality/growth as successful optimization; it does not serve raw bytes instead. The source/output byte comparison and admission reason are manifest evidence. P1 resolves this rule, **not** any numerical threshold, codec or specific production profile admission.

| Lookup | Owner and proof |
| --- | --- |
| Current desired readiness | `content` passes current scope/generation plus full ordered byte/profile obligations to `asset::api`. A matching READY can be attached/reused; newer timestamp, same asset ID or latest old manifest cannot substitute. A current invalid/empty desired snapshot returns unready. |
| Ordinary new-contract delivery | Delivery checks the content-owned eligible LIVE pointer and current authorization/admission; it does not serve an unpromoted READY candidate. A failed replacement can leave the previous eligible LIVE accessible. |
| Explicit retained/admitted revision | Delivery resolves exact immutable `ReadyIdentity` plus existing authorized session/admission scope and retention eligibility. It need not equal the new desired sources. Knowledge of an ID or possession of an expired URL grants no right. |

Renewal may change a signed transport URL while preserving admitted byte identity. It must revalidate the caller/account, language/experience, session/admission, current entitlement/revocation and retained-reference eligibility under the existing approved policy. An unavailable decision denies renewal; immutable identity does not replace authorization. P1 introduces no new session duration, TTL, cache permission, offline entitlement or public/shared caching. Exact production EXT-MEDIA/EXT-PUBLICATION, response `Cache-Control`, retention duration and revocation/renewal policy remain separately gated. Existing tester allowlists and local/account cache controls are unchanged.

## Retention independent of page editing

Future persistence must retain the immutable page snapshot, order, source/output versions and provenance independently of mutable `story_pages`/localization-row lifetime. A retained manifest may store the former page ID as historical scalar provenance; a restrictive FK to a mutable page must not block normal editing or deletion, nor may cascading page removal erase live/session media. This is a future schema obligation: V32 currently has a restrictive `page_id` FK and is not modified by P1.

Protect live, rollback, authorized active-session and pending-job/source references before any later cleanup decision. Removing the last page invalidates the current desired candidate even though no replacement job can run; an old non-empty snapshot cannot certify the now-empty story. The eligible old live/session snapshot remains independently retained, subject to current editorial/access rules. No retention interval, garbage-collection job, deletion permission or backfill is authorized here.

## Mutation and consumer compatibility

All obligations below are **future implementation requirements**; current legacy behavior continues until a separately reviewed versioned rollout.

| Entry path / consumer | Current evidence | Required new-contract behavior and owner |
| --- | --- | --- |
| Publish/archive | `ContentPublicationService.publishLocalization:42` records editorial publication without image orchestration; archive is separate. | `content` records automatic preparation/promotion intent or withdraws it. No editorial status alone creates READY/LIVE. |
| Localization create/update | `ContentManagementService:135,163` accepts editorial and processing fields. | `content` captures/invalidate relevant generation and bindings; client-supplied flags cannot write READY/LIVE. A direct `PUBLISHED` value cannot bypass the new gate. |
| Processing-status/manual-ready | `ContentManagementService:190,206` can set legacy `COMPLETED`. | Remain legacy semantics; no new-contract image proof or live-pointer write. |
| Content active/access/type/cover changes | Content-owned metadata/reference mutations. | `content` rechecks eligibility and invalidates affected scopes. A change outside the initial supported scope cannot be certified by the STORY/READING contract. |
| Page add/remove/reorder/localization | `StoryPageManagementService:143` captures ordered sources; empty lists return early at line 153. | `content` advances affected language generations, including explicit invalid empty state. Localized image change affects that language; shared page structure affects all reading languages. Audio-only changes do not force image encoding. |
| Source bytes/processing profile | Listing ready lookup currently uses asset ID/profile label; page executor captures profile at execution time. | `asset` captures actual immutable byte/profile fingerprints; change invalidates candidate/reuse even with unchanged IDs/labels. Coordinate through public contracts. |
| Worker/admin complete/fail/retry/recovery | `AssetProcessingService:153,167,225,263` guards listing, while generic/page paths differ. | `asset` accepts terminal transitions only from `PROCESSING` under full ownership; first committed outcome wins. Identical terminal replays return read-only proof; contradictory callbacks cannot reverse the outcome or mutate READY. Explicit retry captures a new attempt/current obligations. |
| Legacy `DELIVERY` listener | `AssetProcessingStatusListener:19` copies only `DELIVERY` state into legacy localization status. | Keep that mapping; it cannot manufacture new-contract READY/LIVE. Narration/audio/ZIP completion remains separate. |
| Listing/home | ADR-0019 verified listing and placeholders. | Preserve current thumbnails and placeholder eligibility. Reading readiness does not silently suppress existing cards or certify a different listing role. |
| Original Hero/public pages/asset bundles | Hero validates/signs originals; public pages resolve localized original IDs; legacy bundle uses delivery records. | Preserve until explicit versioned transition. New consumers require admitted LIVE/retained proof and expose verified role metadata; no raw fallback. |
| Page test manifest | `TestStoryPageMediaController:73` authorizes allowlisted Firebase users and compares current page IDs/order/source IDs; explicit retained revision skips that current-source comparison. | Preserve the experiment and its admission. Existing manifests/test endpoints are not new-contract certification or production access. New lookup must use full byte/profile identity and authorized retained mode. |

Source anchors are relative to `be/src/main/java/com/tellpal/v2/`, except [V32](../src/main/resources/db/migration/V32__add_story_page_media_revisions.sql). The inspected baseline is `124a5b22fd324793a5d14220715c839617b6848b`; these are source observations, not reproduced runtime races. In particular, listing already atomically saves its manifest/completion (`asset/application/LibraryCoverPreparationService:34`), while page manifest publication and completion are separate (`asset/infrastructure/media/RegisteringAssetProcessingJobExecutor:172,177`); listing's full guarantee cannot be attributed to pages today.

## Given/When/Then design walkthroughs

These are contractual walkthroughs, **not runtime tests**. Every outcome names the responsible owner; future implementation needs actual storage/PostgreSQL/integration evidence.

| ID | Given | When | Then |
| --- | --- | --- | --- |
| D01 First publication / complete set | No live revision; an eligible STORY/TR/READING Publish intent and qualified profile | All cover/detail/ordered-page outputs pass independent stored MIME/bytes/SHA/dimension/decode verification | `asset` atomically writes full READY and completes the current job; `content` automatically promotes the matching generation. No extra manual approval. |
| D02 Missing/corrupt output | A valid candidate, with either no previous live or an eligible live R1 | One required output is missing, truncated, wrong MIME/SHA/dimensions or undecodable | `asset` refuses READY and fails only its owned attempt; `content` keeps R1 eligible, or keeps first publication unavailable. No partial-manifest success/raw fallback. |
| D03 Stale success and failure | Attempt A's lease expired; B owns the replacement | A reports success, failure, or failure from its rejected-success catch block | `asset` records A-only diagnostics and changes neither B nor its READY/job state. Even A's valid output cannot promote an obsolete generation. |
| D04 Same-ID changed bytes | Asset 51 was captured with SHA(`abc`); ID 51 now denotes admitted SHA(`abd`) | A ready lookup or old worker tries to attach the old fingerprint | `asset` returns no current match; `content` rejects obsolete adoption. A newly captured generation can prepare/reuse the new bytes. Hash fixture demonstrates unequal keys. |
| D05 Duplicate/non-image edit | R1 is verified for current byte/profile obligations | Publish is repeated or description metadata changes without any image-byte/order/profile change | `content` attaches the matching media to the current generation and rechecks policy; `asset` returns existing work/proof without needless encoding or “already completed” failure. |
| D06 Last page removed | A non-empty candidate/old live snapshot exists | The last mutable page is removed | `content` explicitly invalidates desired readiness; no job is scheduled from the stale snapshot and no old completion certifies empty current content. Retained eligible live/session data survives independently. |
| D07 Mutation/status bypass | New-contract READY/LIVE is absent | Create/update supplies `PUBLISHED`/`COMPLETED`, manual-ready is called, or legacy `DELIVERY` completes | `content` may retain existing legacy semantics; neither module creates new-contract READY/LIVE from those flags. New consumers stay unavailable. |
| D08 Verified reuse | An immutable existing output matches actual source SHA and every required role/profile/admission setting | A new generation requests identical media, or a formally qualifying original is independently verified and admitted by copy/reference | `asset` reuses verified bytes without another lossy generation; `content` separately adopts the current generation. A derivative fed repeatedly through an encoder is not compliant reuse. |
| D09 Equal/growing bytes | A required candidate is equal to or larger than its source | The profile omits growth admission, or explicitly permits it with qualified fidelity/geometry evidence | `asset` rejects the first case; it may certify the second only with observed comparison and admission reason in the manifest. P1 admits no concrete production profile by this example. |
| D10 Profile/encoder change | Source IDs/bytes and human profile label stay equal | Quality/geometry/encoder/normalization/effective default changes | `asset` computes a different complete profile fingerprint; current lookup misses old output, and `content` rejects old-generation adoption. Revert/reuse requires full matching admitted identity. |
| D11 Retained renewal | Session S was authorized for retained R1; current desired is R2 | R1 URL expires and S requests renewal | Delivery rechecks S/account, exact scope, current authorization and retained eligibility. It may issue a new URL for the same immutable R1 under existing policy, or deny; R2 currency is not required for retained lookup. No new TTL/cache right follows. |
| D12 Language isolation | TR and EN have independent desired/live scopes | A TR image changes, EN Publish completes, or a shared page is reordered | `content` invalidates TR only for the localized change; EN work cannot satisfy TR. Shared order changes invalidate both affected language snapshots. No language fallback. |
| D13 Restart/conflict | Uploads exist or READY committed before notification; a duplicate insert races | Worker/process restarts or notification repeats | `asset` verifies reusable objects and applies full conflict equality; manifest and job remain all-or-nothing. `content` reconciles pending adoption by generation; a different manifest under the same key fails. |
| D14 Superseded generation / policy | Candidate g7 becomes READY after g8 is desired, or content is archived/access revoked | g7 completion is delivered | `content` rejects adoption atomically. Retention does not restore eligibility; any old LIVE delivery still checks current policy. |
| D15 Mutable page lifetime | A live/rollback/authorized-session revision includes page P | P's localization or page row is removed during editing | Future persistence retains immutable snapshot/provenance/output references independently; mutable-page deletion neither erases the revision nor confers access. V32 hardening remains implementation work. |
| D16 Unsupported semantics / role | An alpha/ICC/orientation/animated input or LULLABY role lacks a qualified admitted profile | Preparation or cross-role reuse is requested | `asset` refuses certification for that scope/profile; no silent white matte, flattening or generic role substitution. Existing listing placeholders and legacy runtime retain their own contracts. |
| D17 Duplicate/contradictory terminal callbacks | Attempt A has a valid token and is `PROCESSING` | Success commits before a late failure, or failure commits before a late success, even before lease expiry; an identical success/failure is replayed | Under the locked `PROCESSING` precondition, `asset` preserves the first committed outcome: `COMPLETED` and its immutable READY never become `FAILED`, and `FAILED` never becomes READY from A's success. Identical replay returns existing proof without mutation; contradiction is rejected/original-attempt diagnostics only. A failed job requires explicit retry and a newly claimed token. Later expiry/replacement still cannot mutate READY or another attempt. |

## Qualification and handoff gates

The [P0f findings](../../_bmad-output/implementation-artifacts/image-optimization/p0f-quality-closure-findings.md) accept Android 1.0× PNG and iPhone 0.75× PNG only for the displayed TR71/TR19 examples and recorded portrait geometries. Android 0.75× was rejected for blurry embedded text; JPEG/WebP q85 remained excluded from selection because sharpness concerns were unresolved. Factors are geometry-specific, not production profiles or fleet-wide minima. The [retention audit](../../_bmad-output/implementation-artifacts/image-optimization/p0f-evidence-retention.json) records unavailable original native ledgers/sidecars/build/cleanup/private-test evidence; surviving screenshots/structured records do not close that independent-audit limitation. P1 does not rerun or complete P0.

Resolved by P1: initial role/ownership boundary; typed byte/profile/revision identity; required-output verification; atomic manifest/job finalization; success/failure ownership; automatic preparation/current-generation promotion; no flag bypass; verified reuse and explicit growth-admission rule; current versus retained lookup distinction; page-lifetime-independent retention invariant.

Still gated: representative corpus/geometry/device/resource qualification; numerical production profiles and exact deployed encoder/provider verification; alpha/color/orientation/animation normalization; concrete compliant-source/growth profile admission; production authorization and EXT-MEDIA/EXT-PUBLICATION integration; precise protected HTTP cache/renewal/revocation/session/retention policy; versioned consumer migration, historical backfill, rollout and cleanup. These require their own concrete decisions/evidence. P2 and the whole publication workflow are **not declared ready** by completion of this document.

## P1 verification record

2026-10-16 documentation review: all six approved edge-matrix rows map to D02–D07; reuse/growth/profile/retained-renewal/language cases map to D08–D12; automatic publish, restart/conflict, policy, retention and unsupported semantics map to D01 and D13–D16. D17 covers same-attempt duplicate/contradictory terminal callbacks and the `PROCESSING`/first-committed-outcome rule. Each walkthrough specifies owner, inputs and outcome; no scenario is labelled an executed application test.

Canonical fixture values were recomputed independently from the documented UTF-8 JSON/domain framing using Python JSON/`hashlib` and an independent explicit ASCII serializer/system `shasum -a 256`; existing golden hashes stayed unchanged, and both equivalent-escape inputs produced identical canonical bytes/digests. The [durable verification report](../../_bmad-output/implementation-artifacts/image-optimization/p1-contract-verification.md) retains the check program and raw output. New local Markdown targets and inspected source symbols/anchors were checked, and `git diff --check` passed. A broader link scan also found the pre-existing missing `architecture.md` target `.kiro/specs/tellpal-v2-backend/tasks.md`; it is outside this change. The main BMad workflow must record the separate adversarial, blind and verification-gap review outcomes; this implementation record alone does not certify those reviews. This documentation-only task runs no Maven/converter/device/cloud work and supplies no new runtime acceptance evidence.
