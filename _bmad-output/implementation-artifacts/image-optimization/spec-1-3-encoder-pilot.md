---
title: 'Image Optimization Story 1.3 — Real Encoder Pilot'
type: chore
created: 2026-10-10
status: done
baseline_commit: cc7734b4709a3dd1af0426d5f7bddfd70cbcd224
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p0a-trial-protocol.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Provider discovery proves no actual encoder output.

**Approach:** Execute the frozen r3 source-resolution pilot using installed Python/Pillow, producing separately verified codec evidence without changing the preparation freeze.

## Boundaries & Constraints

**Always:** Reuse the two admitted v7 references at 1712×2778 and 1284×2279. Preserve inputs, ownership, purpose and frozen settings/order. Run 14 pairs with separate JPEG85 controls: 28 initial arms, maximum 56 attempts. Retain failures/not-run rows. Keep image bytes, private paths/logs outside Git; preserve history and Story 1.1/1.2 artifacts.

**Ask First:** Runtime mismatch, installation, acquisition, normalization/settings changes or expanded claims. Encoding requires approval of this concrete spec.

**Never:** Change CMS, app/cache, dependencies, production, deployment, unrelated trackers or existing `build/`. Claim Java/Railway/mobile, quality, savings or resource qualification. Reader, real alpha/EXIF and OQ-2/3/4/6 gates stay open.

## I/O & Edge-Case Matrix

| Scenario | Input/state | Expected behavior | Failure handling |
|---|---|---|---|
| Valid pilot | Matching frozen inputs/runtime | Closed outputs independently reread/decoded | Missing encoder fails qualification |
| Prerequisite failure | Changed/missing source, reference, freeze or runtime | No encoding/admission | Stop; retain not-run rows |
| Rejection fixtures | Truncated, MIME mismatch, excessive bytes/pixels | Four frozen rejection categories | Unexpected acceptance fails gate |
| Lossless arm | PNG/WebP lossless | Exact decoded reference RGB | Mismatch rejects output |
| Attempt failure | Encode/readback failure or timeout | One distinct frozen retry | Retain both failures |
| Unsafe paths | Existing batch, symlink or input overlap | Refuse overwrite | Preserve isolated partials |
| Forged results | Missing/duplicate arms or counters | No aggregate PASS | Derive counts; retain diagnostics |

</frozen-after-approval>

## Code Map

`O=_bmad-output/implementation-artifacts/image-optimization`. Existing inputs are read-only.

- `O/p0a-corpus.json` — `trial_plan`: exact pairs/settings/order/IDs, provider digest and denominators.
- `O/p0a-trial-protocol.md:21` — codec/color settings; `:29` order/retries; `:43` evidence boundaries.
- `O/verify-p0a-corpus.py` — `canonical:148`, `same:152`, discovery `:170`, hashing `:189`, `Report:366`, `validate:1114`; reuse its unchanged contract.
- `O/prepare-p0a-normalization.py:159` — canonical RGBA serialization; no regeneration.
- `O/spec-1-2-corpus-trial-protocol.md:116` — parent freeze/final receipts; `O/p0a-reader-geometry-decision.md:30` — approved purpose.

## Tasks & Acceptance

**Execution:**

- [x] `O/run-p0b-encoder-pilot.py` — admit prerequisites/probe fixtures; create exclusive batch; encode/retry sequentially. Verify canonical opacity before RGB projection; freeze execution scope before outputs.
- [x] `O/verify-p0b-encoder-pilot.py` — reopen outputs separately; verify hashes/MIME/bytes/geometry, static opacity, omitted metadata and lossless equality. Bind input/settings/runtime; derive completeness/counts.
- [x] `O/test-p0b-encoder-pilot.py` — cover matrix, runtime/order/ID tampering, control reuse, null bytes, corruption, exhaustion and privacy; distinguish synthetic checks.
- [x] `O/p0b-encoder-pilot.json`, `O/p0b-encoder-pilot.md` — run approved batch; publish sanitized identities, bytes, outcomes and limitations; retain immutable private scope/outputs/logs/receipt.
- [x] `O/sprint-status.yaml`, `O/spec-1-3-encoder-pilot.md` — update only Story 1.3 after workflow gates; preserve Story 1.2 status.

**Acceptance Criteria:**

- Given admitted inputs, when execution finishes, then all 14 pairs have separate controls/candidates and actual identities; PASS requires 28 verified arms and four expected fixture rejections.
- Given failed/incomplete observations, when reporting, then missing bytes stay null, counts derive from attempts and only verified outputs qualify codecs.
- Given final verification, when handing off, then source/reference/frozen-file hashes remain unchanged, visual/reader/native verdicts remain pending and quality-qualified savings remain null.

## Spec Change Log

## Design Notes

Scope: local macOS arm64 Python 3.14.6/Pillow 12.3.0 matching frozen discovery. Preserve save settings/metadata policy; record omitted lossless quality's effective effort default 80. No resizing or quality-equivalence claims.

Before encoding, an immutable scope receipt binds corpus/protocol/freeze/reference-set/preparation/discovery/code hashes, effective defaults/limits and exact schedule/IDs; actual output qualification remains separate.

Private output root must resolve outside the repository; reject symlinks and ancestor/descendant overlap with inputs. Use exclusive private writes/isolated partials. Watchdogs: one process, 120 seconds/encode/readback, 64 MiB/output, 300 seconds/preflight; no production budgets. Source/reference/freeze/runtime integrity failure stops the batch. Output failure/timeout gets one frozen retry; exhaustion marks that arm failed and continues remaining ledger. Intake: 20 MiB before read; 40,000,000 pixels before decode. Persist attempts first. Verifier must not import encoder; disclose shared-Pillow limitation.

## Verification

Use matching Python: `python3 O/test-p0b-encoder-pilot.py`, unchanged corpus verifier with private CLI inputs, then inventory verifier. Run/verify pilot using those inputs/new `--output-root`. Sequence tests and real/privacy checks. Rehash inputs before/after; check syntax, links, `git diff --check` and privacy including untracked files. Retain logs/code/scope/result hashes privately. No application builds.

Executed batch `encoder-pilot-r3-v2`: 28/28 arms independently verified, four fixture rejections, zero diagnostics. [Result](p0b-encoder-pilot.json) and [report](p0b-encoder-pilot.md) record the evidence boundary. All 64 tests ran and passed; none skipped. Source/reference/frozen-input/build preservation hashes match before/after. Unchanged corpus and inventory CLI checks passed.

Matrix audit (each named test appears with `ok` in the retained suite log):

| Matrix row | Passing coverage |
|---|---|
| Valid pilot | `test_synthetic_valid_inventory_only_and_14_separate_controls`, `test_real_save_worker_path_on_tiny_synthetic_all_frozen_settings` |
| Prerequisite failure | `test_prerequisite_failure_retains_all_not_run_and_no_encode` |
| Rejection fixtures | `test_truncated_intake_rejected`, `test_mime_mismatch_intake_rejected`, `test_excessive_bytes_rejected_before_read`, `test_excessive_pixels_rejected_before_decode` |
| Lossless arm | `test_png_lossless_exact_equality`, `test_webp_lossless_exact_equality` |
| Attempt failure | `test_encode_failure_retry_retains_both_attempts`, `test_exhaustion_retains_two_failures_and_next_arm_runs` |
| Unsafe paths | `test_existing_batch_or_receipt_never_overwritten`, `test_root_repo_overlap_ancestor_descendant_and_symlinks_rejected` |
| Forged results | `test_missing_arm_cannot_be_hidden_with_forged_counts`, `test_counts_cannot_claim_zero_failed_or_extra_verified_arms` |

Review closure: all ten patch findings were fixed; the stable 64-test suite, fresh v2 pilot, separate verifier, unchanged parent checks and final preservation/privacy/link checks passed. Three reviewer layers completed; no intent/spec changes or deferred findings. See [review closure](p0b-encoder-pilot.md#review-closure).

## Suggested Review Order

**Execution and frozen scope**

- Admit the frozen schedule, persist scope, then encode each arm sequentially.
  [run-p0b-encoder-pilot.py:170](run-p0b-encoder-pilot.py#L170)

- Bind inputs, runtime, settings, defaults, code and attempt identities before output creation.
  [verify-p0b-encoder-pilot.py:284](verify-p0b-encoder-pilot.py#L284)

**Private boundaries and finite failure handling**

- Guard direct worker writes and project admitted opaque pixels without renormalization.
  [run-p0b-encoder-pilot.py:62](run-p0b-encoder-pilot.py#L62)

- Stop on integrity drift; retain attempt receipts before encoding and permit one retry.
  [run-p0b-encoder-pilot.py:113](run-p0b-encoder-pilot.py#L113)

- Bound parent runtime discovery so prerequisite failures cannot hang the batch.
  [verify-p0b-encoder-pilot.py:243](verify-p0b-encoder-pilot.py#L243)

**Independent output qualification**

- Check actual bytes, JPEG tables, geometry, metadata and exact lossless pixels.
  [verify-p0b-encoder-pilot.py:312](verify-p0b-encoder-pilot.py#L312)

- Preserve execution history, reject forged counts and require complete independently qualified arms.
  [verify-p0b-encoder-pilot.py:405](verify-p0b-encoder-pilot.py#L405)

**Evidence and supporting checks**

- Review real candidate bytes, qualification limits and closure of ten patch findings.
  [p0b-encoder-pilot.md:1](p0b-encoder-pilot.md#L1)

- Exercise actual worker saves and assert literal frozen JPEG quantization tables.
  [test-p0b-encoder-pilot.py:644](test-p0b-encoder-pilot.py#L644)

- Keep Story 1.3 in review within the image-optimization tracker.
  [sprint-status.yaml:42](sprint-status.yaml#L42)
