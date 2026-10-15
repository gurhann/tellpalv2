# P0C native quality, latency and process memory findings

Status: all planned native evidence collected; human quality acceptance remains open. The user reports sharper PNG references and slight blur in the other iPhone formats. No quality-qualified savings or rollout decision is granted.

## Scope and evidence

Content 71/page 1/TR and content 19/page 1/TR were compared using retained same-geometry JPEG q85 and WebP q85 outputs. Normalized-pixel-equal PNG references were used only in separate quality captures. Both platforms ran frozen release AOT builds. No re-encoding or production rollout occurred.

All 20/20 planned iPhone primary cells and 4/4 Android primary cells qualified. Each platform has 6/6 separately bound quality captures. Both platforms passed the five tiny-control boundaries: normal teardown, exact certificate pin, expired envelope, wrong UID and corrupt bytes. Original failures and unrun selections remain retained.

Application source SHA256: `f917af7931307fe6e70aeb1b23466e501bfd08424ace796fdd59ba87eff60516` (294 files). [Structured results](p0c-native-results.json) retain source/build/signer/installation/prerequisite and case/session/provider/attempt identities, ledger hashes, control evidence, paired differences, samples, captures and reservation cleanup hashes. Raw traces, originals, tokens and private paths remain outside Git.

## Timing and memory

iPhone entries are medians of five qualified observations per arm; Android entries are one observed value per arm, with no per-arm median, range or P95. Ready is dispatch→decoded-ready on one Dart monotonic clock. Target-paint timing and within-iPhone ranges remain in JSON; raster completion and physical presentation are unavailable.

| Platform | Source | Arm | Qualified/planned | Ready ms | Sampled onstage delta MiB | Final-release delta MiB |
|---|---:|---|---:|---:|---:|---:|
| ios | 71 | jpeg | 5/5 | 1277.54 | 12.80 | 12.05 |
| ios | 71 | webp | 5/5 | 1250.07 | 11.45 | 10.59 |
| ios | 19 | jpeg | 5/5 | 1325.91 | 12.33 | 11.42 |
| ios | 19 | webp | 5/5 | 1244.35 | 13.19 | 12.02 |
| android | 71 | jpeg | 1/1 | 1832.18 | 19.78 | 11.89 |
| android | 71 | webp | 1/1 | 1785.68 | 13.10 | 10.18 |
| android | 19 | jpeg | 1/1 | 1790.88 | 18.89 | 9.85 |
| android | 19 | webp | 1/1 | 1629.57 | 18.98 | 8.72 |

The onstage delta is that run’s sampled load/onstage maximum minus its 30-second baseline median. Final-release delta is the median of the final 10 seconds of the 30-second post-release phase minus baseline. Summaries aggregate these per-run deltas, not pooled samples. Owned render disposal/end-of-frame, eviction and release completion precede release observation. Baseline includes status polling.

iOS uses self `phys_footprint`; Android uses self `Debug.MemoryInfo.totalPss × 1024`. These OS accounting views are not equal counters and are never added to heap/GPU measurements. Per-window counts, nulls, expected 2 Hz counts, maximum gaps, observer durations and native read durations remain explicit in JSON. Android native reads take roughly 45–50 ms per sample and add observer cost; iOS read cost is much smaller. Sampled maxima are not true peaks. Positive release deltas do not establish a leak; package/GPU retention is unknown.

WebP’s observed ready times are lower for both sources on each platform, but this small uncontrolled pilot does not establish a codec-only causal speedup. Memory results vary by source: iPhone source 19 has a higher WebP onstage delta, while source 71 has a lower one. Android source 19 onstage deltas are nearly equal. No general RAM-saving claim is justified.

Requested geometry is a centered contain image in a 280×460 logical slot at DPR 3. Native observations retain actual slot/DPR and decode dimensions: Android reports DPR 2.625. Source 71 decodes at 840×1364 and source 19 at 778×1380. Source/arm pairing checks actual geometry and artifact/settings/scope bindings.

iPhone delivery used a private LAN HTTPS broker; Android used an owned physical Test Lab streaming proxy with exact ADB reverse. Every qualified call had a fresh process/session/namespace, absent exact cache keys and one verified complete transfer. OS/network-cold state is uncertified. Network/background load are uncontrolled. The first iPhone source-71 JPEG samples were thermal `fair`; the first WebP run transitioned fair→nominal; later iPhone observations were nominal. An overnight pause and LAN endpoint change are recorded. Android reservations may use different physical instances; equal device selectors do not establish hardware repeatability.

## Quality and file bytes

| Source | JPEG q85 bytes | WebP q85 bytes | Raw reduction from JPEG |
|---|---:|---:|---:|
| 71 | 669,608 | 294,450 | 56.03% |
| 19 | 508,346 | 244,442 | 51.91% |

These are same-geometry file-byte reductions, not quality-qualified savings or RAM reductions. Equal q85 settings do not imply equal visual quality between codecs. Every quality PNG has source/reference provenance and current foreground/portrait/hold/provider/layout/decode bindings before and after the native capture. Quality runs do not contribute timing/memory primary observations. Native screenshots do not prove physical panel color or presentation latency.

The user inspected the six labeled original iPhone captures and reported: “PNG references appear sharper; the other formats appear slightly blurry.” This unblinded observation is a sharpness concern, not a formal score for lettering, Turkish diacritics, fine lines, gradients, color and framing or an acceptance. The user accepted all six labeled original Android captures (“Android: hepsi kabul”). This is an unblinded acceptance after the earlier iPhone concern, not an independent blinded score. iPhone acceptance remains ungranted; combined quality-qualified savings remain unavailable. `qualityQualifiedSavings` remains null. The q85 candidates are not approved for an equal-quality substitution.

## Attempts and failures

iPhone: 21 actual primary attempts yielded 20 qualified cells after one failed ninth attempt. That attempt returned `NATIVE_TEARDOWN_REFUSED` (Xcode exit65); its subsequently copied diagnostic ledger was byte-identical to the preceding different case, so it was excluded. Cause remains unresolved. A fresh retry passed with the unchanged application/build after renewed installation audit and all five controls. The original eight earlier cells remain valid evidence with their original identities.

Android: 13 owned windows all reached authoritative FINISHED. Windows 1/3/5 stopped during clock preparation; window 4 timed out while allocation remained pending. Window 2 passed normal tiny control. In window 6 normal and exact-pin native boundaries were observed, but a host classifier incorrectly expected a later error after pairing. The tested host-only fix accepts the exact pre-pairing `launcher_pairing_unsettled` reason only for pin refusal; all zero-boundary/native markers remain mandatory. Independent replay and original ledger validation qualified retained controls without rewriting their original receipts. Window 7 passed the other three controls and two quality captures. Windows 8–11 each yielded one primary observation. Windows 12–13 completed the remaining four quality captures. Late selections explicitly stayed unrun under the original entry bounds. Host UTC timeouts before a reservation was created are separate no-allocation failures. Queue/setup/failure durations remain in cost accounting.

## Budget and cleanup

[The approved reconciliation policy](story-1-6-cloud-budget-reconciliation-r2.json) preserves USD10 total Test Lab usage cost and maximum13 windows. Each new pending window received six minutes before create; terminal windows use full owned root duration plus one second rounded upward. Original >195-second provider reserve, latest 105-second root dispatch, cancellation at280 seconds and cleanup by300 seconds were retained. Unknown allocation/cleanup stops execution.

[Final read-only reconciliation](story-1-6-final-cost-reconciliation.json) checked current catalog, project/month usage, current-principal inventory and every owned POST/FINISHED receipt. The old40-minute bound plus29 minutes for the13 closed windows yields69 minutes; fresh quota latest42/historical maximum45 is lower. After the project/month first30 free minutes at USD0.15/minute, the conservative usage-cost upper model is **USD5.85 / TRY286.55**, below the unchanged USD10/TRY489.82 limit. This is not a final invoice or a provider-enforced monetary cap. The13-window allocation limit is exhausted; no further reservation is authorized.

Cloud Console for project tellpal-ee0dd, Firebase Test Lab, 1–15 October 2026 displayed TRY0.00 and no rows when checked on15 October. This reported service subtotal is separate from the usage-cost model, may lag and is never extra budget credit. Other Cloud services and a final settled invoice are outside this reconciliation.

The owned backend and all recorded dynamic broker endpoints are closed; all13 cloud sessions are FINISHED, with owned connections/reverse mappings cleaned up. Shared database infrastructure was left running. Existing private credential cleanup obligations remain: retained QA credential/key material is outside Git and must follow the earlier ownership/revocation plan. No new signing key, IAM identity or production credential was created.

## Verification and limits

The frozen application passed96 focused Flutter tests, Dart analyzer, Swift typecheck/parse and release build/install/artifact audit. After final review, the host suite passed271 tests, including offline compilation/execution of the unchanged Kotlin sampler across five unit/error scenarios. Seven external Flutter regressions execute the actual cache-absence method with disk/pending/live/keepAlive states and the real probe; static analysis passed. The existing96 Flutter regressions also passed again without modifying the frozen mobile source tree. Eight explicitly named matrix tests ran and passed across the retained audit rows, without skipped coverage. All five final review findings were patched. Host observer revision r4 requires independently admitted expected dimensions, the target sequence inside the load phase and exact typed capture expectations. Every original primary ledger and quality capture passed read-only requalification under the stricter observer; all original files and result dictionaries remained equal. Archived expected dimensions come from the original frozen admitted manifest; provider/attempt expectations come from immutable original host run bindings. The old issued envelope was not recovered or claimed. Per-run quality fields preserve their original collection-stage state; current human decisions are separate in humanQualityReview. Evidence hashes and immutable host-observer revisions retain the original application freeze.

Android candidates are accepted for this observed layout; iPhone quality acceptance remains open after the reported sharpness concern. Full OQ-6, device floors/fleet, warm caches, rotation, memory pressure/lifecycle, enlarged text and physical reader review remain outside this pilot. One Android observation per arm establishes neither repeatability nor per-arm quantiles; five iPhone observations do not establish production P95 or fleet safety.
