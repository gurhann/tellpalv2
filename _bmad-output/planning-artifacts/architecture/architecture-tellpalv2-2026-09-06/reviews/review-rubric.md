# Rubric Review — Architecture Spine

## Verdict

**Changes requested.** Mechanical integrity is clean (`lint_spine.py`: 0 findings), and the spine correctly establishes the important identity/ownership direction. However, it leaves three feature-level divergence points unresolved: LULLABY publication validity, the two independent visibility rules for STORY, and the asynchronous asset-to-content state handoff. These would allow separately-built units to ship incompatible behavior.

## Checklist assessment

| Good-spine criterion | Assessment | Evidence / finding |
| --- | --- | --- |
| Fixes real divergence points for the level below | **Partial** | Content identity, owner boundaries and catalog-backed instruments are fixed. Publication/readiness and processing-completion behavior are not fully fixed. |
| Every AD rule is enforceable and prevents its stated divergence | **Partial** | AD-1 through AD-4 are mostly enforceable. AD-5 declares ownership/scope without defining the cross-module completion protocol; AD-6 does not define the required visibility predicates. |
| Deferred items cannot let units diverge | **Partial** | The public compatibility rollout is properly deferred to API owners. The instrument-label ownership question is not deferred consistently: the source spec leaves it open, while the spine silently selects backend-localized labels. |
| Ratifies brownfield reality | **Partial** | The Spring Modulith and `content`/`asset` split align with `architecture.md`, and the rules identify the existing localization-centered model to change. The spine needs an explicit event/API seam for completion status rather than leaving a builder to choose one. |
| Covers driving capabilities | **Partial** | Both specs are represented, but LULLABY CAP-2's minimum-one-instrument publication rule and story CAP-2's audio-vs-reading visibility matrix do not survive as binding rules. |
| Parent-spine compatibility | **N/A** | No parent spine is declared. |
| Every feature-altitude dimension is decided, deferred, or open | **Partial** | API/CMS/schema ownership are covered. Processing transition and instrument-label localization are neither explicitly decided nor cleanly deferred. The inherited operational environment need not be re-decided for this feature, but the completion path that operates within it must be bound. |
| Named technology is verified-current | **N/A** | This feature spine pins no new technology/version; it inherits the existing stack. |

## Findings

### High — LULLABY can be published with zero instruments

**Where:** AD-4 and the LULLABY capability map.

**Why it matters:** The driving contract requires one or more catalog instruments and says a ninni cannot be saved or published if the catalog is empty. AD-4 prevents duplicates and free text, but does not require at least one selected `LULLABY_INSTRUMENT` at the save/publish boundary. A CMS implementation can therefore accept zero instruments while a mobile/query implementation assumes one exists.

**Resolution:** Add an enforceable rule to AD-4 (or AD-3) that a LULLABY playback is invalid for save/publish/visibility unless it has at least one ordered instrument link. State the application validation and database/model-level safeguard appropriate to the aggregate.

### High — STORY reading and audio-discovery visibility are not independently bound

**Where:** AD-2, AD-5 and AD-6.

**Why it matters:** The audio-story contract requires two different predicates: `READING` follows the existing published/visible localization rule, while `AUDIO_STORY` additionally requires a valid processed full-narration asset. Audio processing failure must remove only the audio projection, not the reading projection. AD-6 says summaries expose readiness, but not the predicates or their non-interference. A query builder, publisher and processing listener could consequently make an audio failure hide the whole story localization, or expose an audio card before readiness.

**Resolution:** Bind explicit predicates, e.g. `isReadingVisible(localization)` remains the existing localization visibility rule; `isAudioStoryVisible(localization)` equals reading visibility plus valid narration asset plus narration processing `COMPLETED`. Bind that narration state changes never change reading editorial/publication state.

### High — Asset completion has no defined module-boundary handoff

**Where:** AD-1 and AD-5.

**Why it matters:** The spine assigns media processing to `asset` and playback/readiness to `content`, and introduces `LOCALIZATION`/`CONTENT` scopes. It does not say how a completed/failed asset process changes the content-owned narration or LULLABY playback state. Existing boundary rules prohibit direct cross-module domain access, so independently-built workers may use an internal repository, a synchronous callback, an `asset.api` command, or an application event; those alternatives differ in transaction and retry semantics.

**Resolution:** Add one rule naming the completion contract: the event/`asset.api` result payload (including owner scope and IDs), the sole `content` handler that updates readiness, and idempotency/out-of-order behavior. Keep the asset module responsible for processing records and the content module responsible for experience visibility.

### Medium — Instrument-label localization is decided inconsistently with the source spec

**Where:** AD-4 and the “Public contracts” convention.

**Why it matters:** The LULLABY spec explicitly leaves backend-localized labels versus mobile-owned code translations open. The spine specifies both `localized display labels` in the catalog and locale-resolved `displayName` in the public contract, but does not create an AD for that choice or remove/defer the open question. This can produce incompatible catalog schema and mobile API work.

**Resolution:** Either add an AD that makes backend localization authoritative (and specifies the fallback/required localization rule), or defer it explicitly and expose only stable instrument codes/IDs until API owners decide.

### Medium — Capability references are ambiguous across the two source specs

**Where:** frontmatter `binds` and AD Binds fields.

**Why it matters:** Both source specs define CAP-1 through CAP-4. The bare frontmatter list and AD-1’s `CAP-1, CAP-2, CAP-3, CAP-4` cannot identify which spec is bound. Later traceability and story planning can silently attach a decision to the wrong capability.

**Resolution:** Use qualified identifiers consistently, such as `story-audio/CAP-2` and `lullaby/CAP-2`, including frontmatter and all AD Binds fields.

## Positive observations

- The canonical identity vs. presentation distinction in AD-2 directly protects the requested UI behavior without retaining `AUDIO_STORY` as a stored canonical type.
- AD-3 correctly places shared LULLABY playback and contributor credits at content scope, preventing locale duplication.
- AD-8 appropriately preserves the explicit out-of-scope boundary for a future independent-audio-story import.

## Mechanical check

`python .../lint_spine.py --workspace .../architecture-tellpalv2-2026-09-06` returned **0 findings**: no placeholders, duplicate/non-monotonic AD IDs, missing AD fields, or unpinned stack-table entries.
