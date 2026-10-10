---
title: 'Image Optimization P0a.2 — Source-Resolution Corpus and Trial Protocol'
type: chore
created: 2026-10-08
status: done
baseline_commit: 99ed6feb7a1290d15b7cdc4d02d66b6f16b7a1b6
review_loop_iteration: 3
context:
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/epic-1-context-r3.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p0a-reader-geometry-decision.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Historical small covers cannot qualify real text-bearing pages. Current reader slot/DPR evidence is unavailable and must not be invented to enable a codec experiment.

**Approach:** Freeze the r3 Story 1.2 preparation handoff for offline `source_resolution_codec_feasibility`, using exactly two existing static opaque CMS reading-page bindings without an EXIF orientation tag, at their unchanged source dimensions. Gürhan approved the concrete r3 proposal on 2026-10-10 with "Uygun". Keep reader/DPR/native quality and real alpha/EXIF qualification behind explicit open future gates. Missing current preparation requirements still prevent acceptance.

## Boundaries & Constraints

**Always:** Preserve all 60 originals, 55 CMS bindings, 62 byte observations, six historical JPEG controls plus the separate P3 diagnostic, prior preparation attempts and r1/r2 history. Keep source bytes, private locations, raw acquisition records, tokens and access URLs outside Git. Retain the user's rights declaration and the separately approved `p0a-normalization-v1` recipe: EXIF 1–8; tagged color to sRGB using relative colorimetric intent and black-point compensation; explicit sRGB assumption for untagged sources; straight alpha and hidden RGB; white `#FFFFFF` only in a separately labeled JPEG matte arm. Keep all five independently established synthetic checks mandatory. This pilot uses no resize, crop or upscale and proves no reader display demand. Keep source-owner role `reading_page` distinct from experiment purpose. Update only the dedicated image-optimization tracker and leave workflow `story_key` unset.

**Ask First:** Additional acquisition beyond the exhausted 60-original/four-STORY bound, any expanded pilot/quality claim, normalization changes or new dependency/runtime installation. Later reader/sizing/native work needs applicable geometry, compatible checked references and its own authorized revision.

**Never:** Encode candidate outputs or run worker-performance/native trials in this story; mutate CMS, originals, app/cache behavior, production profiles/budgets/reuse, Mobile1.2 status, dependencies or deployment. Keep OQ-2/3/4/6 open. Source dimensions or a missing unavailable-role list cannot establish reader/DPR adequacy.

## I/O & Edge-Case Matrix

| State | Expected result | Failure handling |
|---|---|---|
| Valid original and distinct CMS binding | Rehash/inspect actual bytes; preserve exact identities and binding ownership | Acquisition/decode failures remain explicit |
| Missing current text/ICC coverage or incorrect experimental geometry | Named missing gate or integrity diagnostic | No acceptance or affected comparison |
| Missing reader/DPR or real alpha/EXIF future evidence | Preserve explicit unavailable/missing evidence and open linked gates | No qualification claim; do not block approved source-resolution preparation |
| Transparent, EXIF-tagged or unknown metadata in selected pilot | Reject selection, including draft comparison bindings | No pilot or comparison admission |
| Truncated, MIME mismatch, excessive bytes/pixels | Separate deterministic rejection fixture, frozen limits and expected category | Bounded rejection; never quality evidence |
| Unknown metadata, repeated hash or malformed fields | Preserve unknowns/distinct bindings and all diagnostics | No inflated counts or diagnostic fail-fast |
| Changed reference/purpose, reader-demand claim or missing freeze/ledger | Reject invalid integrity/claim; report missing prerequisites incomplete | No silent cross-purpose admission or handoff |

</frozen-after-approval>

## Code Map

`O=_bmad-output/implementation-artifacts/image-optimization`.

- `O/p0a-corpus.json`, `O/p0a-trial-protocol.md`, `O/verify-p0a-corpus.py`, `O/test-verify-p0a-corpus.py` — amend the r2 contract while preserving its source/integrity/normalization checks and diagnostic precedence.
- `O/prepare-p0a-normalization.py` — unchanged SHA-256 `3ad752f0d8c04728630b65dbd90ff3c94c84119d7dc663932ba024f1fad82d40`. Reuse v7 canonical streams through exact source/pixel/runtime identities; no new preparation or encoding is required.
- `O/p0a-slot-source-inventory.{json,md}` and `O/verify-p0a-inventory.py` — retained Story 1.1 baseline; do not edit its pinned inputs, including `O/epic-1-context.md`.
- `O/p0a-reader-geometry-decision.md` — approved purpose/geometry boundary. Two bindings: `binding-content71-page1-tr-reading` at 1712×2778 and `binding-content19-page1-tr-reading` at 1284×2279. Their exact source and canonical pixel hashes are in the corpus/decision.
- `_bmad-output/specs/spec-publish-time-image-optimization/{client-image-contract,rendition-evaluation}.md` — future role/DPR demand and native contract remain prerequisites for their own qualification, without blocking this approved offline handoff.
- r2 preservation receipt identity `r2-preserved-before-source-resolution-amendment-v1`, SHA-256 `17be64c2bf1902f7a2391e41069770a3694264fe60d80035cfcd1365b3b15cd7`. Exact r2 corpus/protocol/spec/verifier/test/helper/context/decision/tracker snapshots are immutable private copies. Keep public hashes/opaque identities only.
- Existing private CLI inputs remain caller-supplied through `--source-root`, `--reference-root`, `--preparation-receipt` and `--freeze-manifest`. Existing v7 receipt SHA-256 `8c4dd77fb14f0d1448cf98ef623098b3da5b01743afc83c60b384085481b1f8d`; independent 60/60 report SHA-256 `8a6692fb7c27606100d2297181065e040870b4fe64e179337f6c6fbd32b2bd29`. Do not publish private paths.

## Design Notes

Use revision/snapshot IDs `p0a-story-1-2-2026-10-10-r3`, `corpus-p0a-2026-10-10-r3`, `protocol-p0a-2026-10-10-r3`. Retain the opaque/no-orientation eligibility predicate: fully decoded integer frame count exactly 1, known opaque pixels (no alpha channel or exact integer extrema `[255,255]`), explicitly absent EXIF orientation including rejection of tag value 1, and known ICC metadata. Unknown/bool/float/malformed values cannot qualify it. Preserve current corpus coverage and the three applicable missing real alpha/rotation/mirroring cells with linked open gates; keep synthetic checks separate.

Add explicit approved experimental purpose, exact source-resolution geometry evidence and purpose-scoped reference admission. Keep the two existing reader geometry rows unavailable/null and add a separate open reader qualification gate recording logical slot, DPR, fit, unclamped demand, source adequacy and native viewing quality as unavailable. Future claims require their own revision/evidence. A changed purpose or merely relabeled full-source reference never qualifies reader demand. Preserve all r2 reader geometry bypass protections for any row/pair claiming reader qualification. Source-resolved geometry must equal the known source axes and independently checked reference projection, bound to its source hash, CMS role, purpose and r3 revision.

Canonical pixel serialization remains ASCII `tellpal-p0a-rgba8-srgb-straight-v1`, one NUL, unsigned big-endian uint32 width/height, row-major packed RGBA8, no metadata/padding; SHA-256 covers the whole stream. Admit only the two selected existing v7 references for the approved offline purpose after source rehash/metadata checks and independent reference correctness checks. Other 58 references remain unadmitted. Do not regenerate pixels or change frozen oracles to obtain a pass.

Preregister exactly 14 same-geometry control/candidate pairs, seven per binding: JPEG q80 and q90; WebP lossy q80/q85/q90; WebP lossless; opaque PNG. Each uses normalized same-geometry JPEG q85 as its control, plus a separately labeled original-byte denominator. Pin all codec settings, pixel-input projection, runtime/provider discovery identities, pair IDs, source/binding/reference hashes, source-owner role, experimental purpose, geometry, r3 revision and planned `not_run` state. Control/candidate actual outputs remain unavailable. JPEG controls and candidates use RGB, subsampling 0, optimize false, progressive false; WebP lossy uses RGB, method 6, lossless false; WebP lossless uses RGB, lossless true, method 6, exact true; PNG uses RGB, compress_level 6, optimize false. Profile handling must be explicit and consistent with the normalized sRGB pixels. No matte arm enters this opaque pilot.

Use only the already installed Python/Pillow runtime for declared provider discovery, which is not encoder qualification. Current discovery is Python 3.14.6 / Pillow 12.3.0, JPEG 6.2, WebP feature declared with version 1.6.0 and zlib `1.3.1.zlib-ng`; capture installed binary identities without encoding. Status remains `unqualified_pending_story_1_3`; later execution must fail closed until concrete provider/runtime scope and actual outputs are qualified. This truthful pending provider qualification does not block the Story 1.2 preparation freeze; it blocks affected candidate comparisons. Any incompatible discovery/runtime change requires a new binding/revision.

Preserve seed 20261010, canonical sorted-key compact ASCII JSON, no NaN/infinity, pair-key and context hashes, deterministic interleaved adjacency/parity/tie-break rules, maximum 4096 pairs, one retry per scheduled attempt and prerequisite/ledger stop rules. Freeze all 14 pairs before output generation. Keep worker/native field applicability, distinct attempt IDs, raw failures, metric-valid counts and sampling limits. No native environments or observations are preregistered for this offline batch (`E=0`; native base/ceiling both 0). Preserve the future five scheduled observations per arm/cache-mode and `30*N*E` base / `60*N*E` ceiling contract without treating zero observations as a successful native trial.

Byte summaries require two verified actual values, compatible conditions and a positive named denominator; missing output is not zero. Keep every planned/failed/incomparable/rejected/pending/not-run pair visible with exclusion reasons. Raw byte deltas and visual-quality-passed savings remain separate; reader/native verdicts and quality-qualified savings stay pending. Preserve visual lettering/diacritic, line, gradient, color and framing criteria, viewing conditions, blinding limitations, evaluator identity and reassessment history. The experiment establishes no native/production quality, safety or P95.

Use the existing detached manifest schema and exact corpus/protocol byte hashes, normalization implementation hash and canonical prepared-reference-set hash. Keep corpus free of self-referential hashes. Generate the immutable detached freeze only after ready corpus/protocol bytes settle; any later corpus change requires a fresh manifest. Public freeze evidence should use only an opaque identity/hash; private locations stay outside Git. A missing/mismatched manifest prevents handoff.

## Tasks & Acceptance

- [x] Record the approval and r3 scope in the decision/context, corpus and protocol; preserve exact r1/r2 history and all retained source/binding/fixture/v7 reference identities. Keep the original/r2 contexts and pinned Story 1.1 inventory inputs unchanged.
- [x] Separate experimental source geometry/reference admission from the explicitly open reader and alpha/EXIF qualification gates. Make the two current text/ICC coverage cells ready only after purpose-scoped reference correctness/admission passes.
- [x] Preregister the finite 14-pair ledger with complete settings, source/reference/runtime binding, deterministic order, attempt limits, original-byte versus q85 denominators and truthful pending output/provider/visual/native states.
- [x] Update the verifier and meaningful regressions for r3 integrity, exact geometry/purpose boundaries, required future gates, reference membership/admission, ledger completeness/settings/duplicates/counts, detached freeze and mixed diagnostics. Preserve existing fixture, palette, normalization, source-scope, privacy and reader-demand bypass tests.
- [x] Verify actual private sources/reference streams/receipt, run the complete regression suite and unchanged inventory check, scan every generated deliverable including untracked files for access material, and check syntax/whitespace/local links.
- [x] Generate a detached immutable freeze after public bytes settle; rerun real verification with that manifest. Mark Story 1.2 ready for review only when all current preparation criteria pass. Do not mark Story 1.3 started or execute candidates.

**Acceptance Criteria:**

- Given the approved r3 purpose, when preparation completes, then all 60 original/source metadata and 55 CMS bindings are preserved, the two exact source-sized references are admitted only for offline codec feasibility, and reader/DPR/native plus real alpha/EXIF gates remain visibly open with no qualification claim.
- Given altered or missing current inputs, when verified, then `INVALID > INCOMPLETE > PASS` precedence retains every diagnostic; only a valid fully satisfied r3 preparation and matching detached manifest yields PASS. Failed unadmitted correctness is incomplete; incorrect admitted pixels/integrity are invalid.
- Given every selected binding/pair, when verified, then exact source ownership, opacity/tag-free metadata, experimental purpose, source/reference geometry/hash and revision agree; any reader-demand claim without affirmative applicable evidence is rejected.
- Given the frozen ledger, when handed to Story 1.3, then all 14 planned pairs and settings/order/counts are present, provider qualification/output values remain truthfully pending, native counts are zero, and no absent/failed output or unknown visual verdict qualifies savings or reader quality.
- Given preservation/privacy checks and the full suite, when Story 1.2 enters review, then all matrix behaviors have passing executed tests, the Story 1.1 baseline passes, detached corpus/protocol/reference hashes match actual bytes, and private access material is absent from all generated artifacts.

## Verification

Run `python3 O/test-verify-p0a-corpus.py` using restored private test inputs and `python3 O/verify-p0a-inventory.py`. Use the verifier CLI with actual private inputs and the new detached manifest; distinguish an expected missing-manifest INCOMPLETE preflight from the final PASS. Do not run tests concurrently with production-artifact privacy/real verification because privacy regression fixtures temporarily create intentionally invalid scratch files. Preserve earlier failed/pass receipts as revision history. Pin stable suite inputs/outputs and final settled corpus/protocol/code/proof identities in a private verification receipt. Syntax, whitespace and recursive privacy checks include all untracked deliverables. Do not broaden testing after required checks pass unless a new mutation or unresolved concern warrants it.

### Initial r3 preparation evidence — preserved history

The stable full suite passed **77 tests in 401.728 seconds**. Corpus, protocol, verifier, test and normalization-helper SHA-256 identities remained identical before and after the run. The unchanged Story 1.1 verifier passed 22 retained inputs, 17 fixed fixtures, 137 complete cells and 31 malformed projection rejections. Syntax, whitespace, local links and recursive privacy checks passed, including untracked deliverables. The final private-input preflight was intentionally `INCOMPLETE` with exactly one missing-manifest diagnostic; supplying the immutable detached freeze then produced `PASS` with zero diagnostics. No candidate/control output, worker-performance or native trial was executed.

Freeze identity: `freeze-p0a-source-resolution-2026-10-10-r3-v1`, SHA-256 `9c4933e4a53fb42a9eacba7ed6fcec603ddb5aae2719cb39bfc50fdc9a4ad52b`. Initial verification receipt identity: `r3-final-preparation-verification-v1`; its private snapshot binds the initial settled corpus/protocol/code and proof bytes. Public corpus/protocol bytes remain unchanged after freeze. This receipt is preserved as history; the final review verification below supersedes its verifier/test qualification. Story 1.3 remains backlog; provider/output, reader/native and real alpha/EXIF qualification remain pending/open.

Every matrix state below has executed passing coverage in [the complete regression suite](test-verify-p0a-corpus.py); listed names identify assertions, rather than planned tests.

| Matrix state | Executed passing tests |
| --- | --- |
| Valid original and distinct CMS binding | `test_valid_r3_preparation_passes_only_with_matching_detached_freeze`; `test_shared_hash_distinct_binding_remains_distinct`; `test_altered_source_is_invalid_and_incomplete_is_preserved` |
| Missing current text/ICC coverage or incorrect experimental geometry | `test_missing_current_text_or_icc_pilot_coverage_is_incomplete`; `test_experimental_geometry_is_bound_to_exact_source_reference_and_revision` |
| Missing reader/DPR or real alpha/EXIF future evidence | `test_narrow_scope_keeps_current_coverage_and_future_gates_separate`; `test_reader_gate_and_unavailable_fields_cannot_be_removed_or_fabricated` |
| Transparent, EXIF-tagged or unknown metadata in selected pilot | `test_draft_pilot_rejects_unknown_transparent_or_exif_tagged_sources`; `test_every_draft_comparison_checks_source_scope` |
| Truncated, MIME mismatch, excessive bytes/pixels | `test_rejection_fixtures_remain_separate_with_frozen_categories`; `test_valid_r3_preparation_passes_only_with_matching_detached_freeze` (actual bounded fixture bytes) |
| Unknown metadata, repeated hash or malformed fields | `test_missing_metadata_remains_open`; `test_shared_hash_distinct_binding_remains_distinct`; `test_malformed_containers_and_ids_retain_later_source_and_privacy_diagnostics` |
| Changed reference/purpose, reader-demand claim or missing freeze/ledger | `test_pilot_reference_projection_is_exact`; `test_r3_approval_and_experimental_purpose_are_bound`; `test_full_source_reference_cannot_claim_unavailable_role_geometry`; `test_missing_detached_manifest_prevents_handoff`; `test_ledger_missing_pair_cannot_be_hidden_by_forging_counts` |

### Review fixes and final verification

Three independent fresh-context reviews identified nine fix groups. The verifier now conserves retained source/binding identities and ownership links, derives counts, checks all 62 observations against actual bytes, hashes exact protocol bytes, preserves later diagnostics after malformed fields or file-read failures, requires visible future coverage gates, and rejects receipt symlinks before resolution. Regressions also assert CLI status/exit-code agreement and isolate scratch-file cleanup to uniquely owned paths. All fixes preserve the approved intent and scope.

The final stable suite passed **88 tests in 565.219 seconds**, with zero failures/errors. All five core file hashes were identical before and after this run, and all 17 named tests in the seven-row matrix above passed. The unchanged inventory verifier, syntax, whitespace, recursive privacy scan of all 16 deliverables, and 38 local links passed. Missing-manifest preflight remained the expected `INCOMPLETE`/exit 1; final actual private-input verification with the original r3 v1 detached manifest returned `PASS`, zero diagnostics, exit 0.

Final receipt identity: `r3-final-preparation-verification-v2`, SHA-256 `f5dc87ab8972ac459e931993f0fd8bb97bb8a1ee2cd595964b3e4d2c4790db9b`. Final suite log SHA-256: `e572068377ed261daf24514983c339dd7029da5a0b50e71d3e4fe208876fe72d`. The root independently rehashed all 26 immutable private proof files, checked the public snapshots and executed test mapping, and confirmed unchanged approved intent and corpus/protocol/helper bytes. The receipt preserves the pre-presentation spec snapshot; this completion record and review navigation do not change the frozen corpus/protocol.

Preparation is complete; the dedicated tracker remains `review` for the handoff, and the unrelated global sprint tracker is untouched because `story_key` is unset. Story 1.3 remains `backlog`. No candidate/control output, worker-performance or native trial was executed. Provider/output, reader/DPR/native, real alpha/EXIF and OQ-2/3/4/6 gates remain open.

## Spec Change Log

- 2026-10-10 — Gürhan approved the concrete source-resolution preparation proposal with "Uygun". r3 admits unchanged source-sized references only for offline codec feasibility; reader/DPR/native qualification stays separately open. The acquisition bound, opaque/no-EXIF predicate, five synthetic checks, normalized pixels, preservation, privacy and no-candidate/no-native/no-production boundaries remain in force.
- Earlier accepted elicitation, party review, normalization approval, rights declaration, bounded acquisition amendments and r2 scope/verification history are preserved in the exact immutable r2 spec and receipt identity recorded above. This focused active spec carries their operational safeguards; earlier verification does not certify r3.
- 2026-10-10 — Completed the r3 preparation and all nine review fix groups; final 88-test and manifest verification passed. Added a human review trail without changing the Code Map or frozen intent. Story 1.3 remains unstarted.

## Suggested Review Order

**Approved scope and frozen handoff**

- Start with the approved offline purpose and future reader qualification requirements.
  [p0a-reader-geometry-decision.md:30](p0a-reader-geometry-decision.md#L30)

- Bind admission to exact source dimensions while retaining unavailable reader evidence.
  [p0a-corpus.json:8665](p0a-corpus.json#L8665)

- Preregister fourteen codec pairs before provider qualification or output generation.
  [p0a-trial-protocol.md:21](p0a-trial-protocol.md#L21)

- Keep Story 1.3 output qualification separate from completed preparation.
  [epics.md:167](../../planning-artifacts/image-optimization/epics.md#L167)

**Integrity and diagnostic continuity**

- Conserve immutable source, binding, observation and ownership identities; derive their counts.
  [verify-p0a-corpus.py:411](verify-p0a-corpus.py#L411)

- Inspect actual bytes and retained observations without losing later diagnostics.
  [verify-p0a-corpus.py:522](verify-p0a-corpus.py#L522)

- Require purpose-scoped source geometry and independently checked reference projections.
  [verify-p0a-corpus.py:914](verify-p0a-corpus.py#L914)

- Validate finite pair settings, pending outputs and deterministic ordering.
  [verify-p0a-corpus.py:1012](verify-p0a-corpus.py#L1012)

- Bind the handoff to exact detached manifest bytes and preserved references.
  [verify-p0a-corpus.py:1157](verify-p0a-corpus.py#L1157)

**Verification and supporting policies**

- Distinguish preserved initial evidence from final review verification.
  [spec-1-2-corpus-trial-protocol.md:112](spec-1-2-corpus-trial-protocol.md#L112)

- Exercise identity deletion, forged counts and ownership corruption.
  [test-verify-p0a-corpus.py:236](test-verify-p0a-corpus.py#L236)

- Ensure malformed fields retain later integrity, privacy and incomplete diagnostics.
  [test-verify-p0a-corpus.py:1230](test-verify-p0a-corpus.py#L1230)

- Reuse unchanged normalization with explicit color, orientation and alpha policies.
  [prepare-p0a-normalization.py:98](prepare-p0a-normalization.py#L98)

- Keep the dedicated handoff in review and encoder execution in backlog.
  [sprint-status.yaml:40](sprint-status.yaml#L40)

- Record the selected session model as the durable BMad delegation policy.
  [ADR-0020-bmad-session-model-selection.md:13](../../../be/docs/adr/ADR-0020-bmad-session-model-selection.md#L13)

- Apply session inheritance at the repository instruction entry point.
  [AGENTS.md:45](../../../AGENTS.md#L45)

- Preserve workflow gates while removing the former fixed model ladder.
  [project-memory.md:63](../../../be/docs/project-memory.md#L63)
