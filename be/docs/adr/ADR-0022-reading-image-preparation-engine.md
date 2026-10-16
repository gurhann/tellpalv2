# ADR-0022: Isolated STORY Reading Image Preparation Engine

## Status

Accepted implementation boundary on 2026-10-16 through explicit P2 decisions and approval of the [P2 specification](../../../_bmad-output/implementation-artifacts/spec-p2-verified-reading-image-preparation.md). The bounded engine is implemented and its tests/packaged conversion pass; workflow reviews are complete. [Evidence](../evidence/p2-reading-preparation/verification.md) separates those checks from unapproved production profiles, live Firebase proof and deployment.

## Context

ADR-0021 fixes the desired publication/readiness contract. The existing page experiment cannot supply that contract: manifest publication and job completion are separate, profile/source identity is incomplete, retained page FKs depend on mutable rows and ordinary object uploads can overwrite paths. Existing experiment profiles and P0 quality observations do not admit a production STORY reading profile.

The human selected three bounded P2 decisions: implement the engine with explicit local test profiles while production profiles remain disabled; accept only narrow supported image semantics; and expose internal preparation evidence without new delivery rights or deletion. The human then approved the concrete implementation specification with “A”.

## Decision

- Keep the initial scope exact-language STORY/READING: localized reading cover/detail and the complete ordered localized page set. `content` captures scalar bindings; `asset` captures actual bytes, profiles, work, attempts and verified immutable manifests. No new asset-to-content module dependency or shared media aggregate is needed.
- Implement an additive internal engine. Existing listing, page experiment, legacy flags, manual-ready endpoints and consumers preserve their contracts. P2 READY is captured-media evidence, not current publication generation, LIVE promotion or access authorization. P3 remains responsible for durable Publish intent, mutation invalidation and eligible current-generation adoption.
- Require explicit test-profile admission and reject production activation. Numerical test settings and synthetic growth evidence cannot become production defaults. No production profile is admitted by this ADR. PNG is the first engine output format; geometry remains explicit and no-upscale. Encoding PNG is lossless, but resizing removes detail and has no blanket quality approval.
- Admit only the implemented, explicitly described subset of single-frame opaque 8-bit sRGB PNG/JPEG with identity orientation. Reject unsupported metadata/color conversion, alpha, EXIF/mirroring, animation, malformed containers and source shortage. Do not inherit the legacy white matte or silently discard unsupported semantics. Broader admission requires qualification and a new complete profile identity.
- Hash actual retained source bytes and complete effective role/profile descriptors, including exact encoder/runtime, normalization, geometry, admission and operational settings. Use generation-pinned source reads and conditional immutable writes. Source IDs, path existence, profile labels and registry metadata alone cannot prove freshness or readiness.
- Persist the captured complete revision and durable attempt ownership. Finalize the complete manifest and owning job atomically under source/profile/job/attempt/lease checks and a PROCESSING precondition. Lease time is refreshed after lock acquisition and before commit. First terminal outcome wins; identical replay is read-only proof; contradiction or stale callbacks cannot mutate terminal state or replacement work. Explicit retry creates a new claimed attempt. Conflict success requires full immutable equality.
- Run conversion outside DB transactions in a supervised child JVM. Explicit byte/pixel/full-raster, heap/direct/native/process, temporary-space, time, concurrency and thread settings participate in admission and identity. Parent admission checks actual maximum and available heap; one monotonic revision budget bounds capture, storage, revalidation and finalization without changing captured profile identity. Observe process termination before releasing local capacity. Lease expiry and cancelling a future do not prove teardown. Test/runtime constraints do not establish production peak-memory or distributed-capacity qualification.
- Independently reread stored object versions and verify complete membership, actual format/storage MIME, byte count, SHA-256, exact dimensions and full decode. Integrity/availability state is separate from historical manifest identity. Retained page/localization provenance is scalar and survives editable row removal.
- Configure the reading-only SDK clone with the SDK default non-idempotent no-retry handler for both operation classifications, preserving the original legacy client. `maxAttempts=1` alone does not prevent inner HTTP media resumptions. Include the policy, adapter/handler implementation identity and honest timeout semantics in new profile fingerprints. Socket read timeouts are inactivity limits; between-call monotonic checks cannot preempt a continuously progressing blocking read. Absolute in-flight duration, including slow trickles/authentication, remains an explicit production-qualification gap. No new storage process/IPC architecture is admitted by this SDK-only correction.
- Preserve originals, retained revisions and isolated orphan ownership. Add no public/test delivery endpoint, signed URL, HTTP cache policy, session right, renewal/retention interval or cleanup job. No cloud production mutation, deployment, backfill or deletion is authorized.

## Consequences

The engine can be tested with actual local objects, Java encoders and PostgreSQL races while production admission stays closed. Repeated identical work can reuse only independently verified immutable output; a larger PNG requires explicit profile growth admission and evidence. A failed candidate leaves historical verified media intact but grants no visibility to the first publication.

Preparation snapshots are not a substitute for P3 current-source/generation comparison. The API must state that boundary so callers cannot promote captured bytes merely because they are READY. Unsupported real sources fail explicitly rather than being silently normalized.

The host and official deployment-base JRE need separate runtime evidence. Local filesystem or mocked GCS tests do not establish live Firebase generation/precondition behavior. Missing or skipped evidence must remain explicit. Full production profile/resource/device and protected-delivery qualification remain open.

The [storage diagnostic](../reading-image-storage-probe.md) and [original failed body-stall evidence](../evidence/p2-storage-probe/focused-tests-attempt-4.txt) motivated the approved SDK-only correction. A diagnostic process watchdog can contain its own run; it does not supply an absolute production storage deadline or qualify slow-trickle behavior. Live execution remains separately authorized and has not occurred in this implementation.

## Alternatives Considered

- Reuse the existing experiment's 1440/q85/white-matte defaults: rejected; they are not admitted production reading profiles or complete P1 descriptors.
- Add preparation directly to Publish/CMS now: deferred to P3 to keep this slice testable without changing publication semantics or access.
- Run conversion in an interruptible application thread: rejected; a timeout/cancellation does not prove decoder teardown or isolate heap failure.
- Treat a matching path/registry/job flag as success: rejected; independent versioned stored-byte proof and full conflict equality are required.
- Select a universal 1×/0.75× or JPEG/WebP setting from P0: rejected; acceptance is example/geometry-specific and sharpness/raw-evidence limits persist.

## Related Files or Modules

- [P1 contract](../image-revision-readiness-contract.md), [ADR-0021](ADR-0021-image-revision-readiness-contract.md)
- [Implementation investigation and decision record](../../../_bmad-output/implementation-artifacts/image-optimization/p2-preimplementation-investigation.md)
- [Actual codec preflight evidence](../evidence/p2-imageio-preflight/README.md)
- `content::api`, `asset::api`, asset reading preparation/storage/persistence/process implementation
- ADR-0001/0002/0006/0008/0010/0017/0019

## Supersedes / Superseded By

None. This implements a bounded test-only portion of ADR-0021 without migrating existing experiment, listing or publication contracts. Production qualification and P3 remain separate.
