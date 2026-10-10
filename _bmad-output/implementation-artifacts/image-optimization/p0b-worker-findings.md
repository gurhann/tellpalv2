# P0b.2 Frozen Candidate and Local Worker Findings

Batch `worker-measurement-r3-v1`. Independent verification: **PASS**. [Machine evidence](p0b-comparison.json) retains every execution attempt, output/resource validity, raw-derived phases, endpoint observations, coverage and positive named byte denominators.

## Scope and observation counts

One approved observation per arm used the two admitted v7 canonical references at 1712×2778 and 1284×2279. Each encoder read the entire admitted pixel stream, checked it, projected RGBA to RGB and saved the frozen settings. A fresh child fully reopened/decoded the new file with the unchanged inspector. The separately encoded JPEG q85 controls retain their own attempts. Original compressed-source decode/normalization, resize, upload and cloud job completion were unexercised.

| Observation | Derived count |
|---|---:|
| pairs | 14 |
| scheduled arms | 28 |
| actual attempts | 28 |
| successful attempts | 28 |
| failed attempts | 0 |
| retries | 0 |
| output verified arms | 28 |
| resource valid arms | 28 |
| failed arms | 0 |
| not run arms | 0 |

All new output-verified hashes match their actual separately reread Story 1.3 counterparts. Scope SHA-256: `7b28e780e1ffae4f5c1e3b5d08f434e4ba5681337f904435b1e928c249fd7ef4`. Private execution-result SHA-256: `55d2e73aeba64cf594d68135a69ae8b6a40ffa8efe5cf5279fc5a34062bb9f6f`. Parent public SHA-256: `fd00e5ccfffc6a4a898284d80e2d98c699359cd337aa6aaf1d3d768560287f3a`. The private final inventory binds immutable raw records and outputs; no raw image or path is published.

## Actual bytes and candidate worker observations

Values below describe actual candidate attempts; all fourteen controls and all twenty-eight arms remain individually available in the JSON. CPU is the user+system delta for encoding only. RSS columns are bytes converted to MiB (1,048,576 bytes). Sampled and kernel peaks are distinct observations; neither is an encoding-only peak. Output decode is a separate process and its RSS appears in the JSON.

| Pair | JPEG q85 bytes | Candidate bytes | Encode wall / CPU seconds | Encoder idle → settled MiB | Sampled lifetime peak / kernel high-water MiB | Output verification seconds |
|---|---:|---:|---:|---:|---:|---:|
| content 71 / jpeg-q80 | 669,608 | 553,639 | 0.0212 / 0.0206 | 35.89 → 109.38 | 109.38 / 109.39 | 0.1182 |
| content 71 / jpeg-q90 | 669,608 | 907,837 | 0.0222 / 0.0218 | 35.92 → 109.36 | 109.36 / 109.38 | 0.1196 |
| content 71 / webp-lossy-q80 | 669,608 | 217,122 | 0.3895 / 0.3808 | 35.92 → 86.38 | 120.53 / 120.53 | 0.1512 |
| content 71 / webp-lossy-q85 | 669,608 | 294,450 | 0.4344 / 0.4330 | 35.83 → 106.02 | 122.02 / 122.02 | 0.1725 |
| content 71 / webp-lossy-q90 | 669,608 | 439,634 | 0.5307 / 0.5296 | 36.00 → 124.66 | 124.66 / 124.67 | 0.1585 |
| content 71 / webp-lossless | 669,608 | 2,801,580 | 2.4357 / 2.4315 | 35.92 → 220.06 | 220.06 / 220.06 | 0.1817 |
| content 71 / png-opaque | 669,608 | 4,374,954 | 0.2673 / 0.2660 | 35.95 → 109.19 | 109.19 / 109.36 | 0.1681 |
| content 19 / jpeg-q80 | 508,346 | 427,585 | 0.0183 / 0.0153 | 35.95 → 81.45 | 81.45 / 81.47 | 0.0774 |
| content 19 / jpeg-q90 | 508,346 | 692,371 | 0.0197 / 0.0169 | 35.84 → 70.19 | 81.36 / 81.36 | 0.0758 |
| content 19 / webp-lossy-q80 | 508,346 | 176,204 | 0.3067 / 0.2946 | 35.95 → 77.39 | 88.56 / 88.56 | 0.0982 |
| content 19 / webp-lossy-q85 | 508,346 | 244,442 | 0.3686 / 0.3590 | 35.67 → 78.14 | 89.31 / 89.31 | 0.1019 |
| content 19 / webp-lossy-q90 | 508,346 | 373,532 | 0.4542 / 0.4417 | 35.97 → 92.00 | 92.00 / 92.02 | 0.1050 |
| content 19 / webp-lossless | 508,346 | 1,934,590 | 1.6232 / 1.6225 | 35.97 → 151.16 | 151.16 / 151.17 | 0.1175 |
| content 19 / png-opaque | 508,346 | 2,969,536 | 0.2357 / 0.2207 | 26.77 → 28.58 | 75.12 / 75.12 | 0.1353 |

The observed WebP lossy q80–q90 candidates occupy 32.4%–73.5% of their same-geometry JPEG q85 controls. JPEG q80 occupies 82.7%–84.1%, while JPEG q90 grows to 135.6%–136.2%. WebP lossless grows to 3.81–4.18 times and PNG to 5.84–6.53 times the controls; both also grow relative to their original sources.

Raw candidate-minus-denominator deltas and candidate/denominator ratios are preserved for both original-source bytes and same-geometry JPEG q85 bytes. Positive and negative deltas are retained. These are storage comparisons; repeated controls and source denominators are not a journey-traffic estimate. Equal numeric codec quality does not establish equal visual quality. WebP lossless keeps its omitted quality setting (installed effective effort default 80); WebP q100 is not used as a losslessness claim.

## Phase costs, live endpoints and disk

Across valid arms, active read+projection+encoding+verification wall spans 0.1428–2.7028 seconds. Instrumented attempt wall spans 70.8182–73.9897 seconds. The latter includes two process starts, guards, five-second idle and thirty-second live settle windows per child, observer work and acceptance; it is not image latency or a production job budget.

Bootstrap/guards and cleanup have their own event-bounded wall/CPU records. The bootstrap event interval starts after interpreter imports and request loading; earlier startup remains included in the retained child lifecycle and instrumented attempt wall, without an invented CPU split. Phase CPU comes from child RUSAGE_SELF user/system seconds. Terminal wait4 user/system CPU and byte-valued kernel ru_maxrss cover the entire child lifetime. They are never relabelled encoding-only or summed with overlapping memory counters.

Encoder and verification children retain live idle and settled endpoint captures on their own PID; settled windows begin at cleanup-end. Signed settled-minus-idle RSS deltas range -23.9688–184.1406 MiB. A negative delta is a valid observation. This single run does not establish a leak, stable release behavior, heap size or device safety.

Retained batch footprint is 27,250,862 logical bytes and 28,487,680 allocated bytes (st_blocks×512), deduplicated by device/inode. Per-attempt output, TMPDIR, evidence and retained cumulative footprint remain separate. Disk observations at 1 Hz, boundaries and final receipts disclose sampled temporary lower bounds; retained cumulative bytes are not an active-temp peak.

## Coverage and measurement limits

The supervisor captured 2,863 observations, including 2,023 periodic ticks, with 0 capture failures. 109/112 active phases had no periodic sample; those phase peaks are null with a short/delayed-phase reason. Child maximum periodic gaps span 1.0057–1.3967 seconds; maximum tick lateness spans 0.0097–0.3985 seconds. Boundary snapshots retain actual capture timestamps/delays, not exact boundary values.

RSS uses externally sampled macOS proc_pidinfo PROC_PIDTASKINFO=4 with the 96-byte arm64 ABI. All 56 children observed one OS thread at idle, at the sampled lifetime peak and at settled capture. These task counts do not reveal codec thread configuration or codec parallelism. The installed SDK unit/ABI sources and code hashes are bound in evidence. No inaccessible dyld-cache library hash is invented, and ps/KiB is not a fallback.

Per-child collector CPU spans 0.4269–3.5760 seconds; measured sampling/boundary work spans 0.3304–3.4762 seconds. Raw sampling-call cost, observer wall/CPU, planned/actual timestamps, lateness, count and gap are retained. Workload perturbation is unavailable; no subtraction or speed percentage is applied.

Python total heap, separate native allocator, PSS, private dirty and graphics memory are unsupported/null. Java heap/container counters are inapplicable/null. The macOS 27.0.1 build 26A434 arm64/M2/16 GiB environment used Python 3.14.6/Pillow 12.3.0. OS file cache, background load and thermal state were uncontrolled. Fresh processes provide process-local baselines and do not establish disk-cold inputs. No Java, Railway, native/mobile or production resource inference is made.

## OQ-2/3/4 decision handoff

- **OQ-2 — provider/codec qualification proposal:** keep this installed Python/Pillow runtime as offline evidence. Request equivalent Java/Railway provider, packaging and deterministic-output qualification before selecting a production encoder. Separately qualify reader/native readability, color, lettering and framing; observed byte differences alone cannot select a codec.
- **OQ-3 — worker budget proposal:** use recorded phase/lifetime ranges and full-reference geometry to frame a separately approved production measurement. Add original-source decode/normalization, job orchestration, upload/cloud reread, admitted concurrency and production counters there. Do not adopt the local watchdogs, one observation or descriptive ranges as P95, resource budgets or device-safe limits.
- **OQ-4 — growth/reuse proposal:** retain lossless/PNG growth relative to both source and JPEG q85 denominators when evaluating preservation requirements. Review reuse/growth acceptance with visual/native evidence and exact source/profile/revision identities; choose no threshold, fallback or reuse policy from this batch.

Visual, reader and native verdicts remain pending; quality-qualified savings remain null. Reader/DPR, source adequacy, real alpha, EXIF rotation/mirroring and OQ-2/3/4/6 remain open. Six historical small-JPEG controls and the separate P3 diagnostic retain historical identities without new resource observations. Full P0, production profiles and policy adoption remain outside this handoff.

## Verification and reproduction

Run [the collector](run-p0b-worker-measurement.py) with the frozen private inputs, actual parent batch and a new exclusive private batch root; then run [the separate verifier](verify-p0b-worker-measurement.py). Existing/symlink/overlapping roots are refused. Raw logs, samples, phase events, requests and terminal records are immutable and bounded. Spawn/encode/readback failures have one distinct retry; integrity, required instrumentation, batch deadline, disk cap or cancellation stops preserve the remaining not-run ledger.

85 synthetic tests passed with no skips, including a real tiny Pillow save/meter seam with the full five/30-second live windows. New orchestration tests exercise retry exhaustion/continuation, missing output without borrowed parent bytes, null comparisons, integrity/instrumentation stop and retained executed history. These synthetic fixtures are separate from actual qualification. The unchanged 64-test pilot suite, corpus CLI, inventory CLI and parent actual batch reread also passed. Source/freeze/predecessor/build preservation, syntax, local links, whitespace and recursive public privacy were audited.

## Review patches and historical source witnesses

Review patches strengthen unexercised collector failure paths and final evidence checks. Owned descendants are terminated even after their leader is reaped, with the recorded TERM/two-second grace/KILL sequence and a single exact leader reap. Buffered pipe-close failures cannot bypass finalized raw logs or terminal receipts. The final verifier retains independently valid output when resource evidence is invalid; it rejects absent periodic sampling, missing tail coverage and terminal CPU below prior cumulative event counters. The two-microsecond CPU tolerance covers the installed Darwin rusage timeval precision at both observations. Delayed ticks remain disclosed and short-phase peaks remain null.

The measurement used the exact sources retained under [worker-measurement-r3-v1-code](worker-measurement-r3-v1-code/run-p0b-worker-measurement.py): [measured collector](worker-measurement-r3-v1-code/run-p0b-worker-measurement.py), [measured verifier/child](worker-measurement-r3-v1-code/verify-p0b-worker-measurement.py) and [measured protocol](worker-measurement-r3-v1-code/p0b-worker-measurement-protocol.md). They preserve the original scope-bound bytes. They are source witnesses, not executable reproduction entry points; their relative imports are not relocated or rewritten. Git restores checkout write permissions, so final verification relies on exact rehashed bytes, strict file inventory and symlink rejection rather than source checkout permissions. Immutable private raw evidence retains its existing read-only requirement.

Use the current top-level verifier with the original private inputs and `--batch-root`, adding `--historical-code-root` pointing to this three-file witness directory. This option is final-verification-only, requires the recorded original scope SHA-256 and rehashes every witness against that scope. It is refused for a measured verification child. The current collector and worker request guards still require their current code identities for new runs. The updated collector did not capture the historical observations, and the updated verifier performed no physical recapture of RSS.

The enhanced final verification passed against the unchanged 28-arm actual batch. All pre-review public fields, observations, outputs, counts and private scope/result identities match the original snapshot exactly; the only new field is `verification_provenance`, which separates the historical `measurement_code` from the current `final_verification_code`. Original pre-review public SHA-256: `e08ae5e6fc11564c14d7f4e5dc13eb2e0600d678ab4c700890615d4150671102`. Updated comparison SHA-256: `1914b17fc73e510d0efee60252661b8982a7193ba0df22a378afc9343deb2796`.

The current review-patched suite passed 109 tests with no skips in 48.115 seconds. Its regressions cover both descendant termination cases, closed stdin finalization, output-valid/resource-invalid separation, missing/delayed periodic coverage, forged terminal user/system CPU, native proc_taskinfo resident/thread sentinel mapping and wrong ABI return size, two independently named positive shrink/growth denominators, historical witness drift and the final-only mode boundary. The unchanged 64-test pilot suite passed in 3.580 seconds; inventory/corpus CLI and predecessor actual reread passed again. The immutable actual batch was not rerun or rewritten. Story 1.4 remains ready for review.

Current final verifier source SHA-256: `3ac9387b2e9ad93c8cdfc38c048fe9b17143d767a77436ac42e4d1929bb66d4a`.
