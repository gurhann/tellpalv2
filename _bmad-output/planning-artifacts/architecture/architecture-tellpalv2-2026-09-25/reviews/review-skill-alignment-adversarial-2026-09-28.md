# Adversarial Review — Flutter Skill Alignment

Date: 2026-09-28

Verdict: **PASS for the approved architecture alignment.** No critical or high finding. This verdict does not authorize implementation before the documented contract and story prerequisites, and does not certify the existing PoC as compliant production code.

## Scope and evidence

Reviewed `ARCHITECTURE-SPINE.md`, the adopted architecture requirements in `../../epics-mobile.md`, the PRD and addendum, the installed `flutter-scalable-app` architecture/state/DI/navigation references, and the before snapshots at `/private/tmp/tellpal-ca-20260928/spine-before.md` and `epics-before.md`. This is an architecture-plan Reviewer Gate, not a runtime/code architecture audit or SDK compatibility test.

A mechanical comparison found all 12 complete AD blocks identical between spine and epics. AD-4, AD-5, AD-8, AD-9, AD-10 and AD-11 remain byte-identical to the prior spine. The entire epic document after the UX Design Requirements heading, including story acceptance criteria and readiness conditions, remains byte-identical to the prior epic snapshot.

## Independent implementation collision attempts

| Seam | Competing implementations attempted | Contract result |
| --- | --- | --- |
| Session and DI disposal | A reader route closes all injected services when popped; another feature expects background playback to survive. | Rejected by AD-3 and AD-12 (spine lines 63 and 117): route ownership ends at its factory BLoC and subscriptions; the composition root owns shared lifetime and the coordinator owns players. |
| Retained tabs and account changes | A retained catalog tab keeps account A state after account B becomes ready; a session feature clears only its own auth state. | Rejected by AD-6, AD-8 and AD-12 (lines 81, 93, 117): all account-scoped state/cache must be cleared before new ready admission; retained tab navigation does not authorize retaining account data. |
| Concurrent BLoC actions | Search returns an old-language result after a newer request; a reader page completion arrives after session takeover. | Rejected by AD-2 and AD-3 (lines 57, 63): ordering/cancellation and obsolete-generation rejection are mandatory; media transition authority remains central. Merely using BLoC does not satisfy these rules. |
| Media ownership | Listening constructs its own player and reports completion; reading independently retains a narration/music pair. | Rejected by AD-3: one coordinator serializes sources and alone produces progress/completion transitions; multiple channels are allowed only within that logical session. |
| Route identity | Catalog passes a complete cached entity, while a deep link passes only content ID and reloads in another language/revision. | Rejected by AD-6 and AD-10 (lines 81, 105): route payloads contain identifiers and required scalar context, destination admission remains mandatory, and playback holds its admitted language/revision. |
| Cache identity and renewal | A widget treats a cached signed URL as access; the player renews a URL against latest revision rather than admitted revision. | Rejected by AD-8/10 and UI conventions (lines 93, 105, 125): account/access scope, immutable revision, integrity and server admission also apply to the selected image package. |
| In-flight account data | A prior-account request finishes after switching and updates a current feature. | Rejected by AD-2/6/7. Tests must exercise late success and error responses; cancellation alone cannot demonstrate this invariant. |

The attempted incompatible behaviors cannot both satisfy the current rules. Lower-level choices such as the specific cancellation mechanism, event transformer, cache engine and exact backend manifest fields remain implementation or pre-consumer contract work rather than newly introduced initiative-level contradictions.

## Product and migration preservation

- AD-4 retains backend-owned idempotent migration, durable receipt requirements for local anonymous input, distinct new-account and existing-account paths, second-device non-remigration and legacy-safe deletion constraints. These correspond to PRD FR-26–28 and its migration rules; unresolved OQ-3/OQ-6 implementation details remain gated.
- AD-5 retains signed in-place upgrade proof before auth/migration stories and exact legacy app identifiers. Controlled catalog/media discovery does not silently qualify as upgrade evidence.
- AD-3 retains loop, interruption, background and cold-relaunch behavior; the changed state-management vocabulary does not delegate media ownership to routes.
- AD-8/10 retain candidate/live separation, tester/subscriber distinction, admission, immutable revision continuity, integrity and the separation of evictable cache from protected migration inputs. OQ-8 remains a required contract decision before relevant consumers.
- AD-9/11 retain purchase/restore and backend flow events before release, single event ownership/deduplication, coexistence, staged rollout and independent legacy shutdown/recovery decisions.
- No visual-parity, product-language, parent-gate, story-readiness or release acceptance criterion was removed from the epic document by this alignment.

## Non-blocking implementation watchpoints

1. **Low — Scope wording can be made more precise.** AD-2 says one owner per feature capability while AD-12 deliberately creates route/tab factory instances. An implementation should identify ownership by the route/tab or capability instance, and avoid interpreting AD-2 as a singleton of every feature BLoC. AD-12 already settles the lifetime, so this is a clarification opportunity rather than a gate failure.
2. **Verification boundary — Do not copy illustrative skill snippets as proofs.** The spine correctly adds stale-result rejection, central route admission and disposal requirements beyond minimal examples. Consuming stories need deterministic concurrency/account-switch tests and device lifecycle checks. This review ran none of those runtime checks and introduces no package-version claim.

Disposition: accept the architecture alignment; optionally clarify ownership-instance wording. Keep existing API, manifest, migration and toolchain gates in force.
