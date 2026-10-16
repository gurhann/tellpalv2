---
title: 'P1: define the image revision and readiness contract'
type: 'feature'
created: '2026-10-16'
status: 'done'
baseline_commit: '124a5b22fd324793a5d14220715c839617b6848b'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/standards/decision-record-standard.md'
---

<frozen-after-approval reason="human-owned P1 contract scope">

## Intent

**Problem:** Publication flags do not prove verified images. Page manifests and job completion lack listing's atomic, attempt-fenced guarantees.

**Approach:** Define the revision/readiness contract and architecture records before P2. Deliver documentation, not runtime rollout.

## Boundaries & Constraints

**Always:** `content` owns publication scope/generation; `asset` owns revisions/jobs. Initial scope: STORY/READING per language, localized reading cover/detail and ordered page illustrations. Keep source/listening/narration/LULLABY roles distinct; preserve existing listing placeholders.

Fingerprint actual source bytes and complete captured profile/encoder/normalization settings, not asset IDs alone. READY requires independently verified stored MIME/bytes/SHA/dimensions/decode for every required output, with atomic manifest/job completion. Fence success AND failure by job/attempt/lease/source/profile; adoption additionally matches desired publication generation. No conversion/storage I/O inside DB transactions.

A Publish request automatically starts preparation and automatically promotes the verified current revision when all required images are ready; no extra per-revision manual approval. READY differs from LIVE; content-owned promotion atomically checks generation, editorial/access eligibility and READY identity. Failed/stale candidates preserve eligible live/session revisions. Legacy flags do not authorize the new contract or migrate consumers.

Reuse only verified immutable role/source/profile outputs; no raw fallback or repeated derivative encoding. Equal/growing outputs require explicit profile admission. Unsupported alpha/orientation/color/animation stays ineligible. P0 Android1×/iPhone0.75× acceptance remains example/geometry-specific, with its raw-evidence limitation.

**Ask First:** Broader roles; changed access/cache/retention; numerical production profiles; changed existing publication behavior.

**Never:** Runtime code, migrations, dependencies, cloud/storage writes, deployment/backfill/deletion, or declaring full P0/OQ closure. Preserve ADR-0006/0008/0010/0017/0019 and module boundaries.

## I/O & Edge-Case Matrix

| Scenario | Contract outcome |
| --- | --- |
| Missing/corrupt output | No READY/promotion; retain eligible live revision; first publication remains unavailable. |
| Expired/replaced success or failure | Attempt diagnostics only; replacement remains untouched. |
| Changed bytes/profile, same asset ID | New fingerprint; reject obsolete adoption. |
| Duplicate/non-image edit | Reuse eligible media; no needless encoding or already-completed error. |
| Last page removed | Invalidate desired candidate; old snapshot cannot certify current content. |
| Create/update/manual-ready/legacy completion | No new-contract READY/LIVE proof. |

</frozen-after-approval>

## Code Map

Java paths use prefix `be/src/main/java/com/tellpal/v2/`:

- `asset/application/LibraryCoverPreparationService.java:25,34` — reuse atomic guarded completion; ready lookup misses same-ID byte changes.
- `asset/application/AssetProcessingService.java:153,167,225,263` — listing fences, page callbacks/follow-ups and completed-job rules.
- `asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java:138,172` — execution-time profile/hash; separate manifest/completion transactions.
- `content/application/ContentManagementService.java:135,163,190,206` — mutation bypasses; also inspect publication/status listener consumers.
- `content/application/StoryPageManagementService.java:143` — source ordering and empty-page invalidation.
- `be/src/main/resources/db/migration/V32__add_story_page_media_revisions.sql:43` — page FK constrains retained revisions.
- `_bmad-output/specs/spec-publish-time-image-optimization/` — parent constraints/P1 exit. `image-optimization/p0f-*` under implementation artifacts retains bounded P0 evidence.

## Tasks & Acceptance

**Execution:**

- [x] `be/docs/image-revision-readiness-contract.md` — define role matrix; typed identities and canonical hash examples; desired/candidate/ready/live transitions; public ownership/atomicity; restart/idempotency; conflict equality; current versus admitted lookup; retention independent of mutable page-row lifetime.
- [x] `be/docs/image-revision-readiness-contract.md` — add mutation/consumer compatibility and Given/When/Then examples for the matrix, reuse/growth, profile change, retained renewal and language isolation. Specify future schema/API obligations only.
- [x] `be/docs/adr/ADR-0021-image-revision-readiness-contract.md`, `architecture.md`, `be/docs/project-memory.md` — record accepted design, implementation-pending status and preserved legacy runtime.
- [x] `_bmad-output/specs/spec-publish-time-image-optimization/{SPEC.md,implementation-plan.md}` — link the contract; separate resolved invariants from remaining profile/normalization/access/cache/retention gates. Do not declare P2 or the whole publication workflow ready.

**Acceptance Criteria:**

- Given current sources, when reviewed, then each guarantee names its owner/example and distinguishes existing behavior from future implementation.
- Given incomplete qualification, when handing off P2, then unsupported profiles/public delivery remain gated.
- Given protected admitted media, when documenting renewal/deletion, then immutable identity never substitutes for authorization; no new TTL/cache permission is introduced.

## Spec Change Log

- 2026-10-16 human approval: “Evet, yayınla dediğimde görseller otomatik hazırlansın, hata olursa mevcut yayın devam etsin. P1 planını bu şekilde onaylıyorum, devam et.” Freeze automatic preparation and current eligible-revision promotion; preserve the prior publication on failure.

## Design Notes

Reuse listing's model; page hardening is later implementation. Separate media identity from publication generation so non-image edits can reuse verified media. Protect live/rollback/session references. No new module edge/shared media aggregate.

## Verification

Verify source anchors, links, state/ownership consistency and scenario coverage; run all three workflow review layers. `git diff --check` must pass. Documentation-only work requires no Maven/converter/device rerun. Keep downstream gates explicit. Audit every matrix row through documented Given/When/Then walkthroughs and log design-coverage results; these are not runtime-behavior tests. Recompute canonical hash examples independently.

## Completion and Review

P1 design completed on2026-10-16. The approved automatic Publish/preparation/current-eligible-promotion contract is recorded in ADR-0021 and canonical documents. Runtime code, migrations and production behavior remain implementation work. All three workflow reviewers returned before triage; two contract-detail findings were patched and verified without changing frozen intent. Seventeen design walkthroughs, independent canonical hash/escaping checks and source/link checks pass. These are documentation/design checks, not application acceptance tests. The complete reproduction program, raw output and review disposition are retained in `image-optimization/p1-contract-verification.md`. The pre-existing missing architecture tasks link is deferred.

## Suggested Review Order

**Publication behavior**

- Start with automatic preparation, current-generation promotion and failure-preserved publication.
  [image-revision-readiness-contract.md:94](../../be/docs/image-revision-readiness-contract.md#L94)

- Inspect transaction ownership, terminal-state fencing and restart-safe adoption.
  [image-revision-readiness-contract.md:113](../../be/docs/image-revision-readiness-contract.md#L113)

**Identity and compatibility**

- Review exact canonical serialization and same-value escape equivalence.
  [image-revision-readiness-contract.md:49](../../be/docs/image-revision-readiness-contract.md#L49)

- Check every mutation path and preserve existing consumers during versioned transition.
  [image-revision-readiness-contract.md:153](../../be/docs/image-revision-readiness-contract.md#L153)

- Keep bounded evidence separate from production and rollout gates.
  [image-revision-readiness-contract.md:197](../../be/docs/image-revision-readiness-contract.md#L197)

**Decision and validation**

- Read the accepted design and its implementation-pending boundary.
  [ADR-0021-image-revision-readiness-contract.md:13](../../be/docs/adr/ADR-0021-image-revision-readiness-contract.md#L13)

- Follow seventeen owner-specific examples; none claims runtime acceptance.
  [image-revision-readiness-contract.md:173](../../be/docs/image-revision-readiness-contract.md#L173)

- Reproduce retained documentation checks and inspect both review corrections.
  [p1-contract-verification.md:28](image-optimization/p1-contract-verification.md#L28)
