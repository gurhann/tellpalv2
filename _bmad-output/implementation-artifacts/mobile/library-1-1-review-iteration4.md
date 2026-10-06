# Story 1.1 — completed review iteration 4 and iteration 5 loopback

Date: 2026-10-06. The human selected GPT-6.1 Sol with medium reasoning for all three independent layers. All three were launched before collecting results. The exact preserved full prompts were reviewed sequentially in untruncated bounded reads across checkpointed turns; an account usage interruption was resumed from each layer's own saved coverage and notes. Each final layer covered its entire 1,807,xxx-byte prompt and performed its required final cross-file checks. Blind Hunter returned no actionable findings. Edge Hunter returned four findings and Verification Gap returned four. No prior interrupted iteration-4 findings were reused.

Snapshot SHA256: `0995db39eaed7140378c928772d3b89316dc672ffa47d377c3bbb595d56f9b33`. Root reverified all345 inventory files and all three prompt hashes before diagnosis. Independent coverage/checkpoint records are preserved in the primary checkout at `_bmad-output/implementation-artifacts/mobile-1-1-iteration4-review-checkpoints/`. Binary metadata was read; no new pixel review is claimed by this review.

## Classified findings

| ID | Source | Severity | Route | Evidence and required action |
| --- | --- | --- | --- | --- |
| R1 | edge | medium | bad_spec | `LibraryCacheFiles.release` removes owner tracking before deleting partial files. Root injected one transient deletion failure, then called `clearAndClose`: the file survived. Require retained cleanup ownership and bounded failure/retry admission. |
| R2 | edge | medium | bad_spec | `_releaseRetired` removes its retirement record before deleting a file. Root used81 actual CacheManager corrupt-expired replacements, injected deletion failure at capacity retirement and proved the old file had no metadata/retirement ownership and survived close. Require retained orphan cleanup ownership, bounded retirement and handled timer failures. |
| R3 | edge | medium | bad_spec | `_close` never reaches repository disposal if enumeration/file cleanup throws; `_closing` permanently retains the failed future. Root injected metadata enumeration failure and verified two close calls left the repository open. Require repository closure even when cleanup fails, retained failed-file ownership and explicit failure propagation. |
| P1 | edge | medium | patch | Generated `num.toInt()` silently truncates fractional schemaVersion/previewLimit/category/content/cover identities and integer metadata. Root fixture1.9/5.9/1.9 decoded as1/5/1. Validate integer values through source DTO converters, regenerate normally and test actual HTTP/repository/Cubit invalidResponse. Do not edit generated code manually. |
| V1 | verification | medium | patch | Actual loopback tests never exceed the1MiB body limit. Add boundary success/error-body tests that reject overflow and verify native socket release; under-boundary valid responses must still work. |
| V2 | verification | medium | patch | No test fills eight active catalog requests. Hold eight real loopback connections, assert ninth admission opens no connection, then release one and prove independent slot reuse. |
| V3 | verification | medium | patch | Successful cover-renewal tests return identical editorial metadata. Return changed row/card order, names, locks and membership; prove the loaded editorial snapshot stays pinned and only matching category/language/content covers and expiry are renewed. |
| V4 | verification | high | patch | Stale-worker tests change attempt/lease via recovery before callback. Keep processingId/attempt/source/profile/captured lease unchanged and advance the clock exactly to/past expiry before recovery; prove publish/complete/fail reject the expired lease and leave job/manifest unchanged. |

No intent_gap, defer or reject findings. The eight claims have different required actions and were evaluated independently. Final severities are root's consumer-impact classifications, not reviewer rankings. The three non-frozen adapter omissions trigger iteration5; all five patches survive. Frozen intent remains unchanged.

## Reproduction

Root's temporary diagnostic tests passed while asserting the known-bad behavior in the unchanged iteration4 source: three probes for numeric truncation/partial ownership/close-resource leak and one81-replacement real CacheManager retired-file probe. The first diagnostic compile needed its test File.delete return type corrected; the first retirement injection was adjusted from two deletes to the actual single FileSystem-seam delete. The final passing probes are retained privately under `/private/tmp/tellpal-library-review-loop5/`. These are defect reproductions, not acceptance results.

## KEEP and scoped rollback

Preserve every prior KEEP instruction and the spec change log, including finite native catalog/artwork transport,8catalog admissions/1MiB and10artwork slots/80queue,80disk objects/10minutes,48decoded images/32MiB, actual-delivered-byte provenance, expiry validation, corrupt expired recovery, rollback after partial metadata writes, active-reader bytes, coalescing, warm reuse, current context/attempt/disposal guards, background catalog pinning, pinned CMS draft/revision, server identity/audience and selected language, original module edges, real fixtures, PoC, approved references/goldens and48dp semantics.

Roll back only the affected `LibraryCacheFiles` release/closure implementation and `LibraryImageCache` retirement/closure methods for re-derivation. Keep their existing public signatures and all unrelated methods. `LibraryCacheAdmission` must remain byte-identical. Exact pre-loop files are preserved under `/private/tmp/tellpal-library-review-loop5/` at repository-relative paths. No other implementation is reverted. P1/V1–V4 remain required.

Failing cleanup must retain ownership without accumulating unbounded file history or spinning on persistent failures. Timer cleanup errors must be handled. Cancellation/draining order and finally-closing the repository must preserve original errors rather than silently claiming successful cleanup. Temporary deletion failures must be recoverable by a bounded subsequent cleanup/close; persistent failures must be reported and keep further storage admission finite.

## Verification and remaining gates

Re-run all spec verification for corrected source, refresh affected hashes/current evidence and repeat required real CMS/HTTP/native acceptance. Earlier iteration4 modified-source acceptance is historical; unchanged consumers may retain evidence only with explicit source identity. Keep the named platform/provider/physical gaps explicit. Reuse only the existing authorized Firebase identity and isolated non-production prefix; never change user8080/5173 processes. Sprint remains review. Fresh Step4 review and Step5 human acceptance remain open; no stage/commit/push/deploy occurred.
