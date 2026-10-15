# Story 1.6 reader-resolution extension findings

Date: 2026-10-15. Status: **complete with explicit Android unrun; human quality accepted; review patches verified**.

The measured r7 iPhone A/B/C primary runs are sealed and qualified for their recorded measurement contract. They provide one descriptive observation per arm in the actual `StoryPlayerPage`. The planned nominal 2 GB Android reader comparison produced no reader measurements: A stopped at the unchanged clock guard before installation or native entry, and C was not allocated. The human accepted all six quality captures as appearing okay. The smallest tested quality candidate is 0.75×: TR71 773 × 1254 and TR19 707 × 1254. Primary speed/memory/payload measurements were made at 1.0×, not 0.75×; the two conclusions remain separate.

The public machine-readable companion is `p0d-reader-resolution-results.json`. It records the consolidated identities, counters, receipts, unavailable fields, and terminal states. This report uses the private evidence labels and hashes below; it contains no private evidence locations or access URLs. Formal review findings and their patch verification are tracked separately in `p0d-reader-resolution-review.json`.

## Scope and frozen conditions

The extension compares six ordered, distinct textless pages from the retained source set in the existing reader, with `audio: null`. A uses full PNG files and full decode; B uses the same full PNG files with bounded decode; C uses directly resized PNG files with bounded decode. The ordinary reader's 24 MiB decoded-image LRU remains distinct from the original isolated experiment's 32 MiB limit. Narration and audio-dependent geometry remain untested.

The measured iPhone r7 release source identity is `faa3a37ba01862375d03963a28325a971878fea049aacc9118e91c1d4a33da93` across 310 files. All three runs recorded the same portrait geometry: a 390 × 557 logical-pixel reader slot, DPR 3, text scale 0.9411764705882353, padding left/top/right/bottom 0/47/0/34, and zero view insets. The slot demand is 1170 × 1671 physical pixels. The source-capped contain calculation yields a 1030 × 1672 candidate without cropping or upscaling; all six pages were source-sufficient under the frozen calculation.

Each arm used a fresh process and separate run. Its sequence contains approximately 20 seconds of baseline, 52 seconds in the reader with forward/back/rapid selection and a real OS background/return, 30 seconds after route exit without QA eviction, and 10 seconds after evicting only experiment-owned providers. The primary ledger was sealed before any later quality capture. Quality measurements are absent from these primaries. The primary-pairing receipt independently confirms the same six-page source, order, and reference across A/B/C, and identical full-PNG encoded hashes across A/B.

## Files, actual decode, transfers, and paint

File dimensions and native actual decode dimensions are separate observations. In particular, C's resized PNG files are **1030 × 1672**, while its native actual decode is **1030 × 1671**. Its independently verified expected decode is 1030 × 1671; replacing this with the encoded height would misreport the observed decode.

| iPhone arm | Encoded PNG dimensions | Native actual decode dimensions | Six-page PNG payload bytes | First page target paint after reader-phase start |
| --- | --- | --- | ---: | ---: |
| A: full file / full decode | 1712 × 2778 | 1712 × 2778 | 19,184,842 | 1,638.787 ms |
| B: full file / bounded decode | 1712 × 2778 | 1030 × 1672 | 19,184,842 | 1,672.502 ms |
| C: resized file / bounded decode | 1030 × 1672 | 1030 × 1671 | 9,613,566 | 1,591.573 ms |

Each page recorded one verified payload transfer. The totals are PNG response-body bytes only; they exclude HTTP status/header traffic and TLS/transport overhead. C's recorded six-page payload is 9,571,276 bytes (49.890%) smaller than A/B for these exact files. Human acceptance covers representative TR71/TR19 captures at the recorded iPhone geometry. These 1.0× payload savings are separate from the selected 0.75× quality-only candidate; they are not general network usage or a codec result.

| Page | A/B payload bytes | C payload bytes |
| --- | ---: | ---: |
| 1 | 3,891,075 | 1,818,236 |
| 2 | 3,910,622 | 1,866,709 |
| 3 | 3,228,632 | 1,653,648 |
| 4 | 2,676,776 | 1,477,669 |
| 5 | 2,662,268 | 1,472,000 |
| 6 | 2,815,469 | 1,325,304 |

Paint timing uses the target image's first paint minus that arm's reader-phase start in the same Dart monotonic clock. It excludes the preceding baseline and is not app launch time, isolated download time, or decode duration. The differences between these three single observations do not establish a causal speedup. A/B holds encoded files constant while changing decode bounds; A/C changes file resolution and decode together; B/C also differs in resampling and actual decode height.

## Sampled process memory and ownership

The native process counter is iOS `phys_footprint`. Values below are sampled process memory in bytes, including the process beyond image buffers. They are not Dart heap, image allocation totals, GPU memory, or continuous peaks.

| Arm | Phase | Median bytes | Sampled maximum bytes |
| --- | --- | ---: | ---: |
| A | Baseline | 98,953,408 | 102,893,760 |
| A | Reader onstage | 151,529,792 | 198,789,504 |
| A | Natural retention after route exit | 146,139,392 | 151,144,704 |
| A | After owned eviction | 113,297,664 | 117,999,872 |
| B | Baseline | 97,634,624 | 102,009,088 |
| B | Reader onstage | 146,401,632 | 191,498,624 |
| B | Natural retention after route exit | 140,560,704 | 144,738,624 |
| B | After owned eviction | 107,751,744 | 112,380,224 |
| C | Baseline | 97,913,088 | 102,172,864 |
| C | Reader onstage | 139,487,616 | 159,467,904 |
| C | Natural retention after route exit | 141,019,520 | 145,262,976 |
| C | After owned eviction | 107,841,920 | 112,658,816 |

The recorded onstage delta is reader sampled maximum minus baseline median. Natural-retention and owned-eviction deltas use their respective phase medians minus baseline median; these formulas must not be interchanged.

| Arm | Sampled onstage delta bytes | Natural-retention median delta bytes | Owned-eviction median delta bytes |
| --- | ---: | ---: | ---: |
| A | 99,836,096 | 47,185,984 | 14,344,256 |
| B | 93,864,000 | 42,926,080 | 10,117,120 |
| C | 61,554,816 | 43,106,432 | 9,928,832 |

Natural-retention snapshots recorded zero pending/live counts, with per-provider keep-alive counts of zero or one. Their decoded LRU totals were A 19,023,744 bytes, B 20,665,920 bytes, and C 20,653,560 bytes. The LRU byte counter is a cache-wide snapshot repeated beside provider observations; it must not be summed across those rows. After owned eviction, recorded pending/live/keep-alive counts and decoded LRU bytes were zero. The explicit reason remains `internal_settlement_unavailable`: these snapshots do not prove internal package/GPU settlement or complete process reclamation. Natural retention and the result of deliberate owned eviction remain separate outcomes. Residual process memory does not establish a leak.

## Sampling gaps, observer work, frames, and limits

Every arm collected 20 baseline, 52 reader, 30 natural-retention, and 10 owned-eviction samples at a nominal serial 1 Hz cadence. All 112 samples per arm and their native reads were valid, with zero null reads. The receipt's duration-based expected counts are 21/53/31/11; the actual counts remain 20/52/30/10. Endpoint counts and intervals are disclosed rather than treated as continuous coverage.

| Phase | A largest sample gap ms | B largest sample gap ms | C largest sample gap ms |
| --- | ---: | ---: | ---: |
| Baseline | 1,003.308 | 1,005.459 | 1,002.980 |
| Reader | 1,352.223 | 1,032.817 | 1,096.408 |
| Natural retention | 1,004.695 | 1,004.412 | 1,005.951 |
| Owned eviction | 1,003.340 | 1,002.916 | 1,003.455 |

| Arm | Recorded observer time across phases | Recorded native-read time across phases | Reader-phase callback frame count | Reader-phase callback maximum build / raster time |
| --- | ---: | ---: | ---: | ---: |
| A | 133.586 ms | 5.686 ms | 212 | 22.123 / 6.786 ms |
| B | 122.638 ms | 5.417 ms | 217 | 18.792 / 6.124 ms |
| C | 124.848 ms | 5.343 ms | 217 | 19.049 / 6.287 ms |

Observer and native-read durations are separately recorded counters; they are not added together as a complete overhead estimate. They do not account for all effects of instrumentation, transport, logging, capture, or scheduling. Frame figures aggregate `FrameTiming` batches under the phase active when the callback was delivered. A delayed batch can contain frames from an earlier phase, so these are callback-delivery-phase aggregates, not exact frame-occurrence-phase counts or maxima. Exact phase attribution is unavailable; there is no percentile distribution or P95 conclusion. The sealed ledgers contain A/B/C 414/414/416 records, zero dropped records, and 167,560/168,706/170,001 bytes respectively.

Each run recorded one OS background and one return, with background durations A 1,059 ms and B/C 1,121 ms. The sampling gaps above include the reader phase; activity during unsampled intervals is unknown. All 112 recorded thermal samples per arm were nominal. This does not qualify unobserved thermal behavior or sustained operation.

True peak memory, package/GPU retention, leak conclusion, repeatability, and P95 remain unavailable or null. The physical iPhone observation does not qualify the nominal 2 GB Android floor; iPhone low-RAM flag and Android-style memory class are unavailable. All three outer primary records bind to the same audited `deviceSelectorSha256`; the optional native `deviceUnitSha256` is null and does not erase that selector binding. Cross-reservation causal comparison remains false. These PNG observations support no JPEG/WebP performance, fleet-safety, or general memory-saving claim.

## Android terminal outcome

The selected device was a physical Moto E20 (`aruba`), API 30, nominal 2 GB. Read-only OS observations recorded MemTotal 1,855,712 KiB (1,900,249,088 bytes), ABI `armeabi-v7a`/`armeabi`, and `ro.config.low_ram=true`. The physical admission used the selected physical-device catalog, successful owned creation, and exact observed model/API/MemTotal. An unset emulator property was not used as physical proof, and storage capacity was not inferred to be RAM.

A failed the original two-second read-only clock bound before release installation, installed-APK audit, or native reader entry. Five samples placed the device approximately 9.58 seconds behind the recorded references under the retained diagnostic assumptions (median −9,582.225 ms; device-minus-host envelope −9,680.457 to −9,478.015 ms). This is not exact UTC proof. Neither clock nor the native validators was changed, and no guard padding was introduced.

Owned cleanup reached `FINISHED` after a 52,957 ms whole-root interval. The failure, debit, ownership, and cleanup receipts remain retained; there was no automatic retry or replacement window. C is explicitly **unrun, unallocated, and undebited**, because verified A reader geometry was unavailable. No Android reader slot/DPR/insets/text scale, actual decode, paint, payload, frames, lifecycle, retention, cache ownership, native PSS, native `isLowRamDevice`, or native memory class was observed. The OS low-RAM property is not substituted for the missing native observation. No Android OOM/crash or resolution-quality conclusion follows from a reader that never launched.

The historical Android attempt remains bound to source `15b827b410aaa2d3db0badc514a1d7d616c5d64570eb1257b682d9337de068db`; the later current iPhone source freeze does not retroactively change that identity. The 0.75× candidate is quality/stress only and has no Android performance result.

## Verification and retained failures

The measured r7 source passed 336 Flutter tests, 309 host tests, and analyzer, with zero failures, errors, or skips in the full local verification receipt. The native suite's 33 passing tests are historical r5 evidence, not a new r7 rerun: equality checks confirm all 116 native inputs unchanged. Five physical guard controls are also retained r5 evidence, alongside the current r7 normal guard. These distinctions preserve the age and scope of each verification result.

The diagnosis retained earlier failures while repairing test-harness scene restoration associated with SIGABRT, compacting the scoped ledger, and fixing baseline SQLite readiness using a proven metadata-open condition. These repairs qualify the current harness path; they do not erase failed attempts or turn failed reader observations into measurements.

## Cost and cleanup status

The reconciled retained-model upper bound across services is **USD 8.75523914272981**, comprising protected historical streaming USD 5.85, the retained six-minute extension debit USD 0.90, frozen other-services baseline USD 1.50523914272981 (TRY 73.73 at 48.98225000067947 TRY/USD), and USD 0.50 reserve. The closed Android root plus one second rounds up to one minute, but the prior six-minute debit is retained without refund. Only one new extension window was used.

The USD 9.65523914272981 figure is an unused hypothetical C preview; its additional USD 0.90 was neither spent nor debited and supplies no additional run permission. This model is not an invoice, provider hard cap, or account-wide spending lock. The other-services console baseline was not refreshed, so spending after that baseline remains unknown. The recorded current-principal inventory contained 34 `FINISHED` resources; it is not an all-principal account inventory.

Android owned cleanup is terminal, and **final iPhone quality-capture and local experiment cleanup are complete**. The final actual-address verification checked 31 configured LAN/loopback endpoints with socket connection probes and kernel listener inspection via `lsof`: every endpoint was unreachable, no matching listener remained, and the owned backend process was absent. The check includes allocated endpoints that never reached binding. The shared database was deliberately left running. These are explicit cleanup observations, separate from the sealed primary ledgers; the cloud inventory already recorded 34/34 current-principal resources `FINISHED`.

## Quality decision and remaining work

The separate full, 1.0×, and 0.75× quality arms now each have two source-bound captures for the TR71/TR19 lettering checks, six captures in total. All screenshots are 1170 × 2532 and retain the frozen reader geometry. Source/reference identities match across the three sizes for each content item, and every prepared PNG passes its per-size decoded-pixel equality check. Full-size files also match the original reference pixels; resized files are not claimed to be pixel-identical to the full originals.

| Quality evidence label | TR71 encoded PNG dimensions | TR19 encoded PNG dimensions | Capture count | Human verdict |
| --- | --- | --- | ---: | --- |
| `quality-full` | 1712 × 2778 | 1284 × 2279 | 2 | Accepted |
| `quality-100-r2` | 1030 × 1672 | 942 × 1671 | 2 | Accepted |
| `quality-075` | 773 × 1254 | 707 × 1254 | 2 | Accepted |

The first `quality-100` attempt was refused before native dispatch after a 25-second timeout reading the IAM project policy. Its failure and explicit pre-dispatch exclusion remain retained; no new cloud window was allocated and no guard was changed. `quality-100-r2` subsequently succeeded with a fresh guard. The rejected attempt supplies no quality capture or reader measurement.

The human reviewed the offline comparison and replied **“hepsi ok gibi”** (“all seem okay”). This is recorded verbatim as acceptance of the full/1.0×/0.75× captures for both TR71 and TR19 under the requested lettering, diacritics, thin-line, gradient, color and framing criteria. The smallest accepted **tested** quality candidate is **0.75×**, with TR71 773 × 1254 and TR19 707 × 1254. No finer search between factors or quality claim for other devices/content is implied. The capture-provenance receipts remain immutable with their original null human verdict and `physicalPresentationVerified: false`; the later explicit human verdict is a separate receipt. Reviewing native screenshots on the Mac does not establish direct physical-screen inspection.

Original Story 1.6/q85 artifacts and their reconciliation remain immutable. The original iPhone observation that PNG was sharper and q85 appeared blurred remains unresolved. Earlier Android q85 acceptance applies to its earlier observed layout and is not acceptance of this extension's PNG resolution. This extension does not reopen the original 13-window evidence or qualify original codec savings.

The measurement archive is verified. The explicit human verdict selects 0.75× among the tested quality candidates. Its primary speed/memory/payload comparison remains unmeasured, and the low-memory Android comparison remains unrun. This study makes no production resolution change.

## Evidence identity

Private originals are retained under these descriptive labels. Full public counter detail belongs in `p0d-reader-resolution-results.json`.

| Evidence label | SHA-256 |
| --- | --- |
| Sealed iPhone primary r7 | `ffdd2a2eb5fd2b960bf5b5e2e36d92d771feee2594817ce3817cdff1dfca1c86` |
| Primary source/order/reference and A/B PNG pairing r7 | `9e632a92d569e7883ffd1e2240e7dd1950228fb40d3cd780ce9679facc69b668` |
| iPhone A qualified result | `40bd4599da1592f2b65fbc9f5793049041f330dd5b0b5029515987324937177c` |
| iPhone B qualified result | `5c64d0bbae023784db28f94766f80a1557961597c7b4fc90f812384fbd403d75` |
| iPhone C qualified result | `8315aa644a7395c52252c99741fb75f718eda93860e3ddf7196bcb894f1bd939` |
| Android terminal and budget draft r1 | `b534864b67371c41ebe75b12e58c3c65dd5e741a547c89f1e5b001cf45aadde1` |
| Complete local verification r7 | `488e6db437352494e9b4901f69f915caee53ca1795ae60f5d989eba03a2dd93f` |
| Full-size quality capture collection | `e6601717b188c698b62c1e0c1fa5075160741123eda35920c43101e5125132aa` |
| 1.0× quality capture collection r2 | `c4455d42cc534e96b0cacc57119092c2494b9e54f88723d4390ff6cae3f77dba` |
| 0.75× quality capture collection | `c75505905a79ea6ccca446f7cb800f3354408f09c340033acffba076bba36d5a` |
| First 1.0× quality attempt pre-dispatch exclusion | `8d1eb7a5711c7ba8cc9f19e838b4d117dc11126b0c679ce82fb0802bce48a844` |
| Final actual-address listener cleanup | `7de9ea8e3d934f97d1ff027ecd46eb4ade9622de0cc42cfe201b6d6f48472827` |


## Verified private archival copy

The final measurement archive `reader-extension-final-evidence-r1.tar.gz` is 1,324,435,634 bytes, SHA-256 `03347b93bf9992bc21079d6fe9e268e90705b08e6b33736c585f48d067316211`. Its 48,139 entries include 36,578 logical files; the archive was read back to verify every logical file hash and symbolic-link target. Identical contents are represented by tar hard links. The owned current login token was removed before packaging, and original evidence files were retained at packaging. The earlier failed-build archive remains separate. Review fixes and their verification are retained in the separate verified supplement below; they are not new physical benchmark observations.

All six original quality captures were also copied, with equal hashes, to a private local review folder and an offline single-page comparison for the human review. No image was recompressed or uploaded by this delivery; the later human acceptance is recorded separately.


The temporary working copy was subsequently found removed for an unavailable cause. It was restored from the verified archive, with all 36,578 file hashes and symbolic-link targets checked again; no current login token was restored. Restoration receipt SHA-256: `8a412d1525372f9837ef84b32492c87bd026169c3927a60c146781eb77c857a6`. The private archive and the separate Desktop review copies remained intact.


## Post-review implementation and offline qualification

The final implementation source is `de707ee1fb8f40e6e847dc7c3c905a88b6e5195e230e117b6223ad669af6c366` (310 selected mobile files). It differs from the measured r7 release: review fixes ensure owned cleanup still completes when quality-binding retirement fails; additional real-cache, decode and admitted Activity/channel regressions strengthen verification. Host qualification now rejects a missing forward/back/rapid journey, preserves phase-crossing native reads as unavailable, validates lifecycle timestamps and refuses overflowing geometry products. SDK-dependent source execution is separate from portable Python discovery. No post-review physical benchmark is claimed.

Current local verification passed **340 Flutter tests, 312 portable host tests, 33 native guard tests, 8 explicit native host integration tests and 9 capture-qualifier tests**, with analyzer clean. Complete receipt SHA-256: `26f68bb19636c1ab20a64f6337dc453d1aafc5907fe618d2f79475b8e834a4cb`. The first host run retained one error from pre-existing process-group cleanup `EPERM`; the unchanged function matches baseline, and its isolated test plus the complete 312-test rerun passed. Root cause remains unavailable and is recorded for deferred work. The capture tests initially all passed but their historical main wrapper then tried to write through a removed temporary path; the original receipt was preserved and unittest discovery completed the same nine tests successfully. A prior native focused ENOSPC attempt is not counted as a pass; its temporary logs were lost with the working-directory removal, and the fresh eight-test native integration result is retained separately.

The stricter host qualifier was then applied offline to the three sealed primaries and three quality sessions. All three primaries remain qualified; their existing geometry, actual decode, transfer, paint, lifecycle and memory results are identical. Only explicit zero crossing-sample fields were added to the derived primary summaries. The quality sessions retain their original collection-time `awaiting_human_quality` state; later human acceptance remains a separate receipt. Original result files and seal hashes were not overwritten. Offline requalification receipt SHA-256: `4092e5b3089456cc1e4baadf7fca9073da9e7c9cdd0f16497efb8da1fdcb3f19`.


Three context-free review layers completed. Ten patch findings are resolved, and the pre-existing cleanup issue is deferred separately. The new real-cache tests were also challenged in two isolated private mutants: reducing reader capacity from six to four fails at the fifth provider with `attempt_capacity`; forcing the full A arm through resize fails its actual decoded-width assertion (12 expected, 1 observed). Both compile and reach the expected behavioral failure, with unchanged actual repository source and no infrastructure/timeout substitute. Mutation-proof receipt SHA-256: `01807c21da91f098898c468b03eb6feb9f9f7b919b87276ee72e735e60c6ab83`.


## Verified final supplement

`reader-extension-post-review-supplement-r1.tar.gz` is 39,773,439 bytes, SHA-256 `98981f49c809eb058e7c89d4d057e791ce4cf5afef9d27858c0eec467cddfebe`. Its 1,933 delta entries include 1,424 logical files, all read back with file-hash and symbolic-link-target verification. It preserves current mobile/tool sources, explicit human acceptance, post-review checks and causal mutants, stricter offline qualification, restoration records, and the original r7 host module recovered byte-for-byte against its preflight SHA. The source and tools stayed unchanged during packaging, and no live owned login token was included. The base archive remains intact.
