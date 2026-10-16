# ADR-0021: Image Revision and Readiness Contract

## Status

Accepted P1 design on 2026-10-16; implementation pending. The human approved automatic image preparation on Publish, automatic promotion of the verified current eligible revision, and preservation of the previous eligible publication on failure. This ADR authorizes no runtime rollout, production profile, deployment, backfill or deletion.

The later bounded local/test implementations are recorded in [ADR-0022](ADR-0022-reading-image-preparation-engine.md) and [ADR-0023](ADR-0023-reading-publication-orchestration.md). Production qualification and ordinary consumer migration remain separate gates.

## Context

Editorial `PUBLISHED`, localization `COMPLETED`, registry plans and manual-ready commands do not prove verified stored images. Listing already has atomic guarded manifest/job completion, while page experiment publication and job completion are separate and its terminal callbacks lack the same captured attempt fencing. Source-ID-only ready lookup cannot detect changed bytes under an unchanged ID. Mutable page FKs also constrain retained revisions.

The approved [P1 specification](../../../_bmad-output/implementation-artifacts/spec-p1-image-revision-readiness-contract.md) requires a contract before downstream implementation. Existing ADR-0006/0008/0010/0017/0019 and legacy delivery remain intact.

## Decision

Adopt [Image Revision and Readiness Contract](../image-revision-readiness-contract.md) as the future implementation contract:

- Initial scope is STORY/READING per exact language: localized reading cover/detail and every ordered localized page illustration. Keep source artwork, listing, listening, narration and LULLABY roles distinct; preserve current listing placeholders.
- `content` owns publication scope/generation, ordered source bindings, editorial/access checks and live pointers. `asset` owns byte/profile identities, durable jobs and verified immutable manifests. Public APIs/events/IDs preserve current module boundaries; no new shared media aggregate or module edge.
- Identity captures actual retained source-byte SHA-256 and the complete effective profile, encoder and normalization descriptor. Publication generation is separate, allowing non-image edits to reuse verified media. Same asset ID or profile label is insufficient freshness proof.
- Publish automatically records durable preparation intent; current verified READY media is automatically promoted by `content` after an atomic generation, identity and eligibility comparison. No additional per-revision manual approval is required. READY and LIVE are distinct.
- Every required output passes independent stored MIME, bytes, SHA, dimensions and decode verification. Manifest publication and owning job completion share one guarded transaction. Both success and failure require `PROCESSING` state plus job/attempt/lease/source/profile ownership; the first committed terminal outcome wins. Identical terminal replay is read-only proof; contradictory callbacks cannot reverse completion/failure or mutate immutable READY, and explicit retry obtains a new captured attempt. Expired/replaced callbacks affect only original-attempt diagnostics. Download/conversion/upload/signing stay outside database transactions.
- Durable scheduling, deduplication and adoption reconciliation survive restart. Duplicate completed matching work is reusable success; conflict handling compares full immutable manifest equality rather than key existence. Old completion cannot adopt a superseded generation.
- Reuse requires verified immutable role/source/profile admission; a compliant original may be admitted by verified copy/reference. No raw fallback or repeated lossy generation. Equal/growing outputs require explicit profile admission and evidence; this decision selects no such production profile.
- Keep eligible live/rollback/session references on failure. Retained snapshots must survive mutable page-row removal; last-page removal explicitly invalidates the current desired candidate. Immutable identity never grants access, renewal or cache permission.
- Create/update/status/manual-ready and legacy completion cannot certify the future new contract. Existing consumers remain until an explicit versioned transition. The contract defines future schema/API obligations only.

Unsupported alpha/orientation/color/animation semantics remain ineligible. Numerical profiles, normalization admission, production access/cache/renewal/revocation/retention policy and rollout remain gates. P0 Android 1.0× / iPhone 0.75× PNG decisions apply only to the displayed examples/geometries, with the recorded raw-evidence retention limitation. Neither full P0 nor P2 nor the complete publication workflow is declared ready.

## Consequences

Future implementation must harden the page pipeline, capture complete immutable source/profile evidence, provide atomic asset finalization and content adoption, audit every mutation path, and prove restart/concurrency guarantees against PostgreSQL. Retained media persistence must not depend on mutable page-row lifetime. This is future work, not an assertion about existing tables or endpoints.

Failures do not replace an eligible live revision. First publication remains unavailable through the new contract until complete verification and promotion. Archive/revocation still applies to retained/live media; retention is not authorization. Exact protected delivery and cleanup policies need separate concrete decisions.

P1 changes documentation only. Existing publication flags, manual commands, legacy `DELIVERY`, original Hero/public page responses, tester allowlists and listing placeholders continue their current behavior. No runtime code, migrations, dependencies, cloud writes or deployment are included.

## Alternatives Considered

- Reinterpret legacy `COMPLETED` or manual-ready as readiness: rejected because neither proves stored outputs or complete role membership.
- Add separate manual approval for every READY revision: rejected by the approved automatic Publish/preparation/promotion decision.
- Extend page jobs without atomic completion and failure fencing: rejected because stale callbacks and partial commits could certify or mutate the wrong attempt.
- Key reuse only by source asset ID/profile label: rejected because bytes and effective settings can change while those labels stay equal.
- Replace public/legacy consumers immediately: rejected because versioned admission, normalization, profiles and migration remain independently gated.

## Related Files or Modules

- [Contract and design walkthroughs](../image-revision-readiness-contract.md)
- [Canonical architecture](../../../architecture.md), [project memory](../project-memory.md)
- [Parent SPEC](../../../_bmad-output/specs/spec-publish-time-image-optimization/SPEC.md) and [implementation phases](../../../_bmad-output/specs/spec-publish-time-image-optimization/implementation-plan.md)
- `content`, `asset`, existing `user` delivery/admission adapters
- ADR-0001/0002/0006/0008/0010/0017/0019
- `V32__add_story_page_media_revisions.sql` and listing's existing guarded finalization as implementation anchors

## Supersedes / Superseded By

None. ADR-0017/0019 remain the existing experiment/listing contracts. This ADR defines the additional future STORY/READING contract without retroactively certifying or migrating either path.
