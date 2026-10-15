# Story 1.6 selected 0.75× actual-reader observations

Date: 2026-10-15. Status: **iPhone and physical low-memory Android primary comparisons measured; quality scope remains limited**.

The accepted 0.75× candidate now has a valid six-page speed/memory observation on the physical iPhone. Its verified PNG payload and sampled onstage process-memory increase were smaller than full PNG in this run. It did not paint faster than 1.0×, and its natural-retention memory was higher. These single observations do not establish a universal speed or memory improvement.

The final physical low-memory Moto E20 allocation completed valid A/full, selected0.75× and C1.0× observations with the same unit, geometry and signed release. All three authenticated closures were observed. The earlier failed0.75× attempt remains unqualified and separate. This speed/memory follow-up is complete; Story1.6's broader quality gate remains open. No production resolution policy was changed.

The machine-readable companion is `p0e-selected075-performance-results.json`. Original p0c/p0d artifacts and their failed attempts remain unchanged.

## Conditions and interpretation

Both platforms used fresh release builds from mobile source `f570e4d098fbeb1663843f7f24ab520e3b57079296cfe9051bec2342ed116b25` (310 selected files), existing signers, and the canonical QA UID. Each condition used a fresh process/session in the existing `StoryPlayerPage`, six distinct textless pages, eighteen selections, a real OS background/return, and `audio: null`. The observed phases were approximately 20 seconds baseline, 52 seconds reader, 30 seconds natural retention after route exit, and 10 seconds after experiment-owned eviction.

The full, 1.0× and 0.75× files were PNG. Resized candidates came directly from independently admitted v7 originals with LANCZOS, RGB, compression level 9, optimization disabled and no intermediate resize. PNG encoding is lossless; resizing still removes spatial detail. At 0.75× the display must enlarge the smaller image. The original pixel-sufficiency result correctly remains `insufficient_pixels` with legacy `qualification=unavailable`. Separate `intentionalUndersamplingPerformanceEligibility=observed_samples` requires independent original-source, resize, exact-byte, geometry and observation checks. It does not declare the reduced image source-sufficient or override human quality scope.

## Physical iPhone comparison

The iPhone 12 retained the human-reviewed portrait geometry: 390 × 557 logical pixels, DPR 3, text scale 0.9411764705882353, padding 0/47/0/34 and zero view insets. All selected conditions share the signed build, selector, source/order and actual geometry.

| Selected arm | Encoded PNG | Actual native decode | Six-page response-body bytes | First page paint after reader start |
| --- | --- | --- | ---: | ---: |
| A: full PNG | 1712 × 2778 | 1712 × 2778 | 19,184,842 | 1,593.392 ms |
| C: 1.0× PNG | 1030 × 1672 | 1030 × 1671 | 9,613,566 | 1,462.724 ms |
| Selected 0.75× PNG | 773 × 1254 | 773 × 1254 | 5,866,703 | 1,497.456 ms |

The 0.75× response-body total is 69.420% smaller than full PNG and 38.975% smaller than 1.0× for these exact six files. It excludes HTTP headers, TLS and transport overhead. Paint uses the first target paint minus that run's reader-phase start in the same monotonic clock; it is not app launch, isolated download or decode duration. The 0.75× observation was 95.936 ms earlier than full PNG and 34.732 ms later than 1.0×. Run order and single observations preclude a causal speedup claim.

Native memory is sampled iOS `phys_footprint`, covering the process beyond image buffers. Onstage delta is reader sampled maximum minus that run's baseline median. Retention deltas use phase median minus baseline median.

| Selected arm | Sampled onstage delta MiB | Natural-retention median delta MiB | Owned-eviction median delta MiB |
| --- | ---: | ---: | ---: |
| A: full PNG | 72.844 | 35.578 | 9.195 |
| C: 1.0× PNG | 58.328 | 42.281 | 10.688 |
| Selected 0.75× PNG | 53.125 | 45.609 | 9.547 |

The 0.75× sampled onstage delta was 27.070% smaller than full PNG. Its natural-retention delta was higher than both other arms, so the results support no general retention reduction. Owned eviction and natural retention remain separate observations. Cache snapshots do not establish package/GPU settlement or a leak.

Each selected arm recorded 112 valid samples and zero null or phase-crossing samples. The first A attempt was retained and excluded because a reader gap of 2,040,280 microseconds exceeded the unchanged 2,000,000-microsecond limit. Its one bounded local repeat passed. The 0.75× native run passed; its initial host qualification encountered buffers already retired by broker teardown. Re-admitting the same immutable manifest and original source, with unchanged private/public hashes, qualified the original sealed ledger. No failure row was removed and no substitute native run was invented.

Frame aggregates in the JSON are attributed to the phase active when the callback arrived. They do not provide exact frame-occurrence-phase attribution or P95. Sampling maxima are not continuous peaks; observer and native-read durations do not measure all instrumentation effects. Repeatability, true peak, fleet safety, sustained thermal behavior and leak conclusions remain unavailable.

## Final physical low-memory Android comparison

The selected observations are all from the final allocation, `session-11`, with actual MemTotal 1,900,249,088 bytes, native memory class 128 MiB and native `isLowRamDevice=true`. They share the same independently observed physical-unit identity and installed artifacts. The geometry is 411.42857142857144 × 631.4285714285714 logical pixels, DPR 1.75, text scale 1, top padding 26.857142857142858, zero other padding and zero view insets. All three source-bound reader sessions completed, including actual OS background/return and authenticated app revocation.

| Selected arm | Encoded PNG / actual native decode | Six-page body bytes | First page paint after reader start | Sampled onstage PSS delta MiB |
| --- | --- | ---: | ---: | ---: |
| A: full PNG | 1712 × 2778 / 1712 × 2778 | 19,184,842 | 2,844.564 ms | 21.111 |
| Selected 0.75× PNG | 511 × 829 / 511 × 829 | 2,803,334 | 1,736.032 ms | 14.176 |
| C: 1.0× PNG | 681 × 1105 / 681 × 1105 | 4,691,469 | 1,799.716 ms | 12.219 |

The smaller factor reduced body bytes and the observed first paint versus full PNG in this run. Its sampled onstage PSS increase was smaller than full PNG but **larger than 1.0×**. Resolution therefore did not produce a monotonic memory result. These are one observation per condition in fixed A → 0.75× → C order; they do not prove causal speedups, repeatability or a universal memory optimum. Different target geometry also means Android's 0.75× files differ from the iPhone's 0.75× files.

| Selected arm | Natural-retention median delta MiB | Owned-eviction median delta MiB |
| --- | ---: | ---: |
| A: full PNG | 9.938 | 9.545 |
| Selected 0.75× PNG | 8.356 | 10.606 |
| C: 1.0× PNG | 8.371 | 8.342 |

Each selected Android condition collected 112 valid PSS samples, zero null and zero phase-crossing samples. Maximum reader gaps were 1,157,944 / 1,002,379 / 1,004,122 microseconds for A / 0.75× / C respectively, within the unchanged 2,000,000 limit. All 112 thermal observations per arm were unavailable; no thermal conclusion is substituted. PSS is process memory, not image-only memory or a continuous peak, and cannot be compared numerically with iOS `phys_footprint`. The JSON retains phase medians/maxima, observer/native-read durations, lifecycle timestamps, callback-delivery frame aggregates and cache ownership evidence.

The final allocation closed `FINISHED`. All 11 new allocation cleanup receipts are terminal. The sixth allocation was manually aborted before installation/native after an erroneous controller launch following a wrong-interpreter control invocation; its watchdog receipt proves terminal ownership, but its parent whole-root duration is unavailable. No missing parent duration is fabricated. No billing/usage/price/exchange query was made.

## Retained earlier Android observations and refusals

An earlier Moto E20 ran API30 with MemTotal1,900,457,984bytes, native memory class128MiB and native `isLowRamDevice=true`. Its physical unit differs from the selected final unit; its valid A is retained as historical evidence and is not mixed into the final comparison. Physical admission uses the provider's physical-device catalog, successful owned creation and exact OS observations. Native `isPhysicalDevice` is null and the qemu property unavailable; neither is fabricated as positive physical proof.

That earlier qualified A recorded the same nominal geometry as the final unit. Full PNG decoded at1712×2778, transferred19,184,842body bytes and first painted after2,825.213ms. Its112PSS samples were valid and its onstage delta20.907MiB. These values remain distinct from the selected final A's2,844.564ms/21.111MiB.

The following 0.75× attempt reached the complete phase at 114,263,109 microseconds. A status request was recorded at 114,734,096, followed by `status_unavailable` at 115,239,866 and `session_inactive` revocation at 115,240,086. The native launcher refused success because authenticated app revocation was not observed. Its exported 405-row, zero-drop ledger was reconstructed from all chunks with the original digest verified. The 112 readable memory samples are diagnostic only; they supply **no accepted performance comparison**.

The native diagnostic stage was `DART_TLS_PIN_VALIDITY`, but these observations do not establish a bad TLS pin. They also do not establish a two-second timeout: the failure appeared less than one second after the complete phase. The `status_request` event does not distinguish GET status from POST revoke. Host broker state at the original close was not retained, leaving the root cause unavailable. Host diagnostics now preserve future finite request/response/close state and complete failed ledgers while retaining the original refusal. No unproven mobile timing or trust change is justified by this attempt.

C1.0× was unrun in that earlier allocation because the sequential comparison stopped on the preceding failure. Its raw terminal summary labels failed0.75× `unrun`; the retained native ledger proves an attempt, so this report distinguishes attempted-but-unqualified from C's absence of execution in that allocation. Final selected observations do not change those original failures.

The first allocation was refused before installation/native when the provider moved expiry from approximately root+470 to root+498seconds; it closed after39.013s. The remaining seven-minute profile retained original terminal evidence and all prior debits; its second allocation closed after327.829s. The human later explicitly lifted the USD10 cap and requested completion. Subsequent expiry/clock/preparation refusals and the final successful allocation remain separately recorded; none was automatically relabelled as a benchmark.

The final run used a separately labelled1500ms transport / full±2000ms UTC-envelope observer after legitimate, measured application-delay clock correction through the existing owned-device shell. Every endpoint of five samples and the union after independent Apple/Google reference uncertainty must remain inside±2000ms. This is a change from the retained earlier500ms transport criterion, not an unchanged observer claim. The original observer and setter remain intact. Correction is derived from five measured offsets, bounded to750ms, with at most three passes and one calibration invocation per root; no arbitrary validity pad is added. Midpoint precision assumes transport behavior and is not exact UTC proof. Native `issuedAt<=now`,30s bootstrap, expiry, TLS, UID and integrity checks remain exact and unchanged. The final12-minute profile provides bounded preparation time; it does not extend earlier resources. Detailed failed protocol attempts and regression receipts are in the separate spec and private originals.

## Quality scope and remaining work

The earlier human reply **“hepsi ok gibi”** accepts full/1.0×/0.75× native screenshots for the two TR71/TR19 examples at the retained iPhone geometry. It selects 0.75× among tested candidates for those examples. It does not accept every story page, any smaller untested factor, Android 0.75× quality or direct physical-screen inspection. The previous iPhone JPEG/WebP q85 blur concern remains unresolved; these PNG resolution measurements do not qualify codec performance.

Android speed/memory observations are now complete. Android resolution-quality capture/review remains separate and unaccepted. The earlier close failure's exact cause remains unavailable; successful later closures do not prove its cause. No further paid performance retry is required for this follow-up. Current account spending has not been freshly verified, following the user's instruction.

## Verification and evidence

The unchanged signed source passed 340 Flutter tests across disjoint scopes, 33 native guard tests, eight explicit native host integration tests and analyzer. Host suites progressed 324 → 327 → 333 → 339 → 348 → 350 → 354; these are successive suites, not totals to add. The latest portable host suite passed 354 tests, including four new regressions; its focused subset passed 130. Final private Android operator controls passed 33, including the actual caller-to-synthetic-calibrator path. The first 350-test attempt hit sandbox localhost EPERM errors and is retained as failed; the same suite passed with local socket access. Two rounds of three context-free review layers produced nine resolved patch findings, with no intent gap, spec loopback or deferral. Three requested causal mutants failed at their expected behavioral assertions. Final host patches defer eligible result publication until timely lease completion, reject malformed model types with finite failure receipts and verify real caller-to-driver lease forwarding before dispatch. These patches preserve the original raw physical observations and signed mobile source; no additional physical retry is required. Earlier control-fixture, wrong-interpreter and preparation-budget errors remain retained and excluded from pass claims.

| Evidence label | SHA-256 |
| --- | --- |
| Sealed current iPhone attempts | `cb126b1aa9a9ab5b7969d05f9b9667c20689afb6a013ecd9c5c5ec363bee3e06` |
| Android A qualified result | `dcc5f05cf4a2b3fb390d5c641740b6ee9cdbd9a05c943a99171aa63c49d760c3` |
| Failed Android 0.75× full ledger | `a091de98f7858014205e4acf4da6e3a8a5d5d30ef1221307d13b9f0bb90c9329` |
| Original implementation verification | `e667c70f5ecc213e11f2e57ef2aa5bf853ad60ddef3606fa5d2033d1cfdc8d2d` |
| Post-teardown reload verification | `f46957f87ce6323d547ecd6d1b536321d827c90fb6f78ec915de2e11517e18e8` |
| Finite remaining-window verification | `3e01aa97f0e3ba62da1f098bd54ef8e34f154146af6a8cb22323eeeea45d322c` |
| Passive terminal-observation verification | `e32c6fe4604b8e127c418a82fa1cbd54a0ecb7f901de0f6ddb6c3fb39a6be06e` |

The final host verification receipt is `d78ba53bc9d696c5564242f7c1ebecf36f54abed3910800d3d9c999b162bcf81`; final operator control receipt `dd9b62b992737b860e559af93ae2ac879921124a2a35c3d3180cb46515018871`.

## Cleanup and archive

All 11 owned cloud roots are terminal, the 17 recorded local endpoints are closed, the owned QA backend is stopped and two owned login-token files are removed. The shared database remains running. Cleanup receipt SHA-256: `55d15aaf360206ea6f5494a0e7d6175f762d2aaf1902cc2a6f4244240ba2d0b7`.

The private `selected075-final-evidence-r1.tar.gz` archive is verified: 571,039,511 bytes; SHA-256 `60d535e77971990d12dd82135448e6e4dc7f81dbc0d00446bc324958ab80237d`. Readback verified all 8,419 logical file hashes and symlink targets across 10,860 entries, including 2,984 deduplicated hardlinks. The index SHA-256 is `f63f54c4c23e40eb6bf3dac30cfa08901340c24f07b430c1a277301e7d122816` and archive-receipt SHA-256 `df9962a90e78eb5f1bcf97883c215cf223b3fa733ec4185a7751fa589f2128e6`. Originals remain intact and live owned login tokens are absent. The archived publication snapshot precedes this archive-receipt insertion, explicitly labelled `before_archive_receipt_insertion` to avoid a self-referential archive claim.
