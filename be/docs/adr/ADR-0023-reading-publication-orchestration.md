# ADR-0023: Internal Reading Publication Orchestration

## Status

Accepted bounded implementation scope on 2026-10-17 through the approved [P3 specification](../../../_bmad-output/implementation-artifacts/spec-p3-reading-publication-orchestration.md). Local verification and workflow review results are recorded separately in [P3 evidence](../evidence/p3-reading-publication/verification.md). Production activation, cloud execution and ordinary consumer migration remain closed.

## Context

ADR-0021 defines current-generation automatic promotion, while ADR-0022 provides captured-media preparation only. Legacy original uploads are mutable; registry metadata and editorial flags cannot certify immutable source authority, verified outputs or publication identity. Publish needs durable intent and restart recovery without conversion/storage inside database transactions.

## Decision

- Keep the feature disabled by default. `tellpal.content.reading-publication.enabled` additionally requires active `local` or `test`, no `prod`/`production`, and every explicit `ReadingImagePreparationApi.Role` test profile. The P2 engine retains its separate test-engine/non-production-prefix gate. No numeric production profiles are introduced.
- `ReadingSourceAdmissionApi` accepts an explicitly selected original and explicit test limits. Read the observed generation, validate supported actual MIME/full decode/SHA/dimensions, plan ownership, conditionally create a fresh backend-only `reading/admitted/{uuid}` object, reread it and atomically register a new ORIGINAL_IMAGE plus admission evidence. Preserve the original and require explicit content/page rebinding. Planned/orphan evidence survives failure. Different bytes need a new admitted ID; no automatic legacy sealing or rebinding.
- Signed uploads, upload-token minting, registry registration and unconditional writes reject the reserved namespace, including direct Firebase client calls. Reuse P2 SDK-only pinned/conditional storage; no storage process, IPC, dependency or retry layer is introduced. Admission is an application guarantee, not observation of arbitrary privileged external writes.
- Preserve P2 callers. The admitted path captures admission ID/epoch in source/revision identity. Complete profile identity includes the source-authority policy and implementation evidence. P3 rejects unadmitted IDs, including arbitrary legacy registry rows.
- `content` owns exact-language durable intent, desired generation, dispatch lease, current candidate and a separate retained LIVE generation/proof. `asset` owns immutable manifests, source eligibility epochs and mutable availability epochs. No asset-to-content dependency is added.
- All publication-relevant content/localization/page/access commands lock the content row before scope changes. Localized edits advance one language; page topology and content edits advance affected scopes. Empty pages produce an explicit invalid generation. Pure snapshot assembly replaces any REQUIRES_NEW snapshot call inside adoption; P2 snapshot callers keep their contract.
- Reconcile durable rows after commit and after restart. A claim fences attachment, failure and adoption with exact generation/lease/expiry. Scheduling is deduplicated by captured identity. The asset API exposes the complete effective role/limits/encoder/runtime identity; content captures it outside transactions and persists it with desired intent. Drift advances generation and detaches obsolete work even when limits are unchanged. Explicit Retry also releases the old job binding before recapture. Pending workers may be reconciled repeatedly; failed work requires explicit Retry/repeated Publish or a newly mutated generation. There is no unbounded automatic failure retry.
- Preparation and fresh independent pinned source/output verification run outside transactions. Fresh verification reserves an availability epoch before I/O and can publish success only against that unchanged epoch. Observed pinned-byte/MIME/hash/decode/missing-output failure or withdrawal invalidates later adoption; an older successful observation cannot overwrite a later failure. Capacity, parent-memory admission and elapsed-budget rejection are inconclusive observations: they cannot invalidate healthy retained LIVE.
- Adoption holds short DB locks in order: content aggregate, publication scope, source admissions in asset-ID order, availability. The asset DB-only guard compares the complete proof against captured work, ordered snapshot, admissions/epochs, profiles, job, revision, digest and availability epoch. Content reassembles current bindings and checks generation, current profiles and editorial eligibility before automatic adoption. No second approval exists.
- A failed/invalid replacement retains the prior eligible LIVE identity and its own generation. Retained lookup compares its own captured media rather than the desired page set. Archive/deactivation closes LIVE eligibility while preserving evidence; a later Publish cannot reopen that historical eligibility before a fresh adoption. Manual status/ready and legacy processing completion cannot create this LIVE contract.
- The authenticated status/retry endpoint exposes finite diagnostics, desired generation and eligible LIVE scope/generation/revision/manifest digest, without URLs or access grants. CMS separately polls only pending exact-language state, stops on terminal/network failure, supports explicit retry and preserves dirty inputs and selected language. Copy identifies the local/test scope honestly.

## Consequences

Ordinary public/listing/original/test-manifest consumers, existing visibility counters, text/audio behavior, sessions and access contracts remain unchanged. Internal LIVE is an image-publication identity and does not prove an entire retained reader experience or ordinary mobile rollout. No source deletion, backfill, cleanup, new cache/retention policy or cloud operation follows from this work.

P2 absolute in-flight storage duration, full production profile/resource/device qualification, broader normalization and P4 protected/versioned consumer integration remain open. Namespace protection cannot prevent a privileged external operator from mutating cloud objects; missing/corrupt pinned objects fail verification and close observed availability.

## Alternatives Considered

- Treat `PUBLISHED`/`COMPLETED` or manual-ready as proof: rejected; those retain legacy meaning.
- Seal a mutable legacy upload path in place: rejected; earlier signed or in-flight writes cannot be excluded by a DB flag.
- Prepare inside Publish/adoption transactions: rejected; external I/O must not hold content locks.
- Add revision approval: rejected by the approved automatic adoption scope.
- Replace ordinary delivery now: deferred to P4 and its independent access/profile/rollout gates.

## Related Files or Modules

- ADR-0021, ADR-0022, [architecture](../../../architecture.md), [project memory](../project-memory.md)
- V36 source admission, availability and content publication persistence
- `asset::api` source admission/publication-ready contracts; `content::api` publication status contract
- [Mandatory P3 investigation](../../../_bmad-output/implementation-artifacts/image-optimization/p3-preimplementation-investigation.md)

## Supersedes / Superseded By

None. Existing listing, page experiment, legacy delivery and P2 preparation contracts remain separate.
