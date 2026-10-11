# P0c.1 Native Measurement Readiness Handoff

Scope: `image-optimization-p0c-1`, Story 1.5, 2026-10-11. Source baseline: `7e3ee8f58a210196ae6988dfa38481d5bd9a7957`. **Preparation checks: PASS. Native execution readiness: FALSE.** This handoff completes a reviewable readiness package with explicit unavailable hardware and instrumentation. It accepts no OQ-6 entry, candidate, native quality/resource budget or production policy.

The [canonical SPEC](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md) and [readiness contract](../../specs/spec-image-optimization-1-5-native-measurement-admission/native-readiness-contract.md) govern the package. The four deliverables are this handoff, the [device/tool matrix](p0c-device-matrix.json), [exact entry proposal](p0c-native-entry-proposal.md) and [measurement protocol proposal](p0c-native-measurement-protocol.md). The [local verifier](verify-p0c-readiness.py) verifies their evidence/source/link/privacy boundaries; proposals remain separate from acceptance, implementation and exercise.

## Observed availability and provenance

The matrix's collection timestamp is `2026-10-11T09:47:05.689925+03:00`. Device timestamps identify a discovery projection, not a live hardware or workload observation. Thirteen private evidence receipts have public SHA-256/source/file bindings. Four failed initial sandbox probes remain distinct from successful authorized/direct discovery. A sandbox service-access failure supplies no absence claim. Raw identifiers, command traces, private paths and mappings stay outside Git.

| Actual recorded discovery | Count / identity | Readiness consequence |
| --- | --- | --- |
| Connected ADB devices | 0 | No physical Android phone, tablet, constrained device or modern control is available from this collection. |
| Registered physical Apple devices | 1: `ios-physical-1`, iPhone 12, recorded iOS 26.7 / build 23H24 | Disconnected, paired and DDI services unavailable. OS/model are registered metadata; RAM and runnable compatibility remain null. It is neither current physical execution evidence nor iOS 15 floor coverage. |
| Available registered simulator profiles | 10 on iOS 27.0; all Shutdown | Supplementary virtual tooling only. Other registered runtime profiles remain explicitly unavailable; none was booted or substituted for physical coverage. |
| Qualified physical native devices | 0 | NATIVE-DEVICE remains open. |
| Installed/cached tool identities | ADB 1.0.41 / platform-tools 35.0.0-11411520; Xcode and xctrace 27.0 / 27A266a; xcrun 72; cached Flutter 3.41.1, Dart 3.11.0 / DevTools 2.54.1 | Version/cache identity only. Exact experiment executable/package identity, capture permissions, attachment and overhead remain unverified. |
| New experiment build / renderer | Not implemented / null | Historical virtual functional builds and cached SDK engine revision do not identify an experiment artifact, renderer, native execution or signing qualification. |

Six required physical categories have explicit gaps: constrained Android phone, constrained Android tablet, oldest available iPhone, oldest available iPad, modern Android control and modern Apple control. Three additional floor rows retain Android API 24, iPhone iOS 15 and iPad iPadOS 15 gaps. All nine rows have `native_qualified=false`, evidence bindings, age/recheck limitations, next actions and **proposed/unverified** current Library/recommendation/detail role and list/detail journey scope. No low-RAM cutoff or modern-control criterion was invented.

Thirteen counter rows remain null with provenance/reasons: Dart heap/external, raster cache, decoded LRU, live/pending owners, first target raster, Android RSS/PSS/private-dirty/native/graphics and Apple footprint/allocation/graphics. Source snapshots and installed tools cannot fill those values. Sampling support is unverified and overhead unmeasured; overlapping counters will remain separate.

## Preserved source and proposed native path

The matrix hashes 43 read-only inputs. Its initial 38 source bindings are retained; five additional hashes cover linked transport, hero, Firebase verifier, environment configuration and project SDK-history seams. The verifier rereads every current input and independently compares tracked inputs to the baseline commit. Existing Library entry/harness release refusal, 48-entry/32 MiB decoded controls, Android API 24, Apple 15 floors, verified Firebase/local-test admission, empty-by-default UID allowlists and server-owned audience fixtures are also checked directly. Real authenticated QA retains the default UID from [mobile-test-account.json](../../../tools/mobile-test-account.json); preparation performed no authenticated flow.

The entry proposal identifies exact separate Dart composition, Android opt-in mode/package/test targets and Apple app/UITest/configuration targets. It specifies runtime public-key pairing and pinned host HTTPS delivery with ephemeral credentials, server Firebase/admission verification, exact candidate manifest/byte checks, isolated mode-specific app/cache/build artifacts, explicit dependency-factory seams for reused screens, instrumentation, positive/negative verification and rollback. Its build/install/driver commands are future shapes and were not run. The host fixture path supports only its admitted fixture/product mapping; offline page bytes cannot automatically qualify listing, reader or protected production delivery.

The protocol independently specifies three verifiable cache identities; exact request/provider/decode/attempt ownership; actual response payload/retry/prefetch counting; immutable source/reference/candidate/build/role/geometry admission; same-process target-draw/frame-number association with a null fallback; separate monotonic clock domains and calibration uncertainty; nonoverlapping memory families; proposed 1 Hz cadence, five primary observations per selected cache-mode cell and 30 idle seconds; signed baseline drift, overhead and sample gaps; synthetic versus genuine pressure; finite proposed watchdogs, settlement failure, retries and retained not-run rows. None of those methods has been exercised on a native experiment process. Missing target-frame proof retains image-ready timing under its own name.

The retained [slot/source inventory](p0a-slot-source-inventory.md), [r3 trial protocol](p0a-trial-protocol.md), [encoder pilot](p0b-encoder-pilot.md) and [worker findings](p0b-worker-findings.md) remain purpose-scoped prerequisites: two source-resolution references, fourteen pairs and twenty-eight verified offline arms do not supply native role/demand/delivery/quality admission. Real alpha/EXIF rotation/mirroring, animation, reader/listening and unsupported roles remain explicit separate qualifications. The six historical JPEG controls and separate P3 diagnostic gain no new native observations.

## Remaining Story 1.6 gates and next actions

| Matrix gate | Current cause | Separate downstream action |
| --- | --- | --- |
| NATIVE-ENTRY | OQ-6 proposal only; acceptance, implementation and verification refs are individually null. | Review the exact proposal revision, then separately authorize implementation and record platform-specific positive/negative physical verification with existing guards preserved. |
| NATIVE-DEVICE | No connected qualified physical subset; floor/constrained/control gaps remain. | Select and provide actual physical hardware; reobserve OS/RAM/tool/build compatibility. Connection, acquisition, provisioning and install are outside preparation. |
| NATIVE-CANDIDATE | Offline codec bytes establish no native role/geometry/delivery/quality eligibility. | Freeze per-role/device/journey admission and independently rehash/reread actual private sources/references/outputs; retain shortages and deferred classes. |
| NATIVE-PROTOCOL | Proposed methods/defaults only; no executed instrumentation or batch freeze. | Freeze selected cells/scripts/order/repeats/limits; prove cache/ownership, clock/frame/null and counter/overhead capture on the exact admitted build. |
| NATIVE-DECISIONS | Applicable quality/resource criteria and parent dependencies remain open. | Resolve only criteria needed for the proposed bounded batch; production profiles/budgets, reuse/growth and publication/retention/protected caching retain separate decisions. |

The parent OQ meanings remain OQ-2 corpus/slots/devices/budgets, OQ-3 reuse/growth, OQ-4 alpha/color/EXIF/animation/normalization, OQ-5 publication/retention/protected caching and OQ-6 native entry. Historical Story 1.4 instead calls codec/provider, worker budgets and growth/reuse OQ-2/3/4. These labels conflict; both histories are preserved and proposals must be cited by subject/source until reconciled. OQ-6 agrees and stays open.

## Acceptance coverage and checks actually run

| Contract criterion | Package evidence |
| --- | --- |
| AC-1 / CAP-1 | Matrix accounts for six physical categories, three floor gaps, current disconnected/virtual and historical states, six installed tools and thirteen counter gaps. |
| AC-2 / CAP-2 | Entry proposal gives exact files/targets, isolated artifacts, platform credential/delivery trust boundaries, DI/ownership, rollback and paired success/refusal checks; OQ-6 receipts remain absent. |
| AC-3 / CAP-3 | Protocol admission/freeze/record schema preserves exact predecessor identity/purpose, two-axis demand, role/source shortage and deferred classes; no native candidate is admitted. |
| AC-4 / CAP-4 | Protocol defines three cache identities, clock/frame-null association, actual transfer, separate counters, cadence/overhead/settlement/signed drift and pressure/failure provenance. |
| AC-5 / CAP-5 | Four linked deliverables exist, with this matrix-derived false native verdict and all five native gates open. |
| AC-6 / all | Source hashes and baseline comparisons, guard/floor/default-UID checks, public privacy/local links/whitespace and isolated malformed matrices pass; source/history/config/cache/profile and unrelated trackers are preserved. |

Verification uses only Python standard-library reads and local read-only Git queries. It calls no device, build, install, signing, authenticated endpoint or network tool.

```sh
python3 _bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py
```

Public-only validation checks source/guard/schema/coverage/receipt binding/document privacy/local links while explicitly reporting that private receipt bytes and observation replay were not checked. The optional `--private-evidence-root "$PRIVATE_EVIDENCE_ROOT"` mode additionally hashes the thirteen exact private receipt files and independently replays device/tool/probe/summary projections; it never prints that root or raw receipt values. Private replay establishes consistency of saved discovery data, not fresh discovery or native qualification.

Review patches independently derive physical form factor from agreeing receipt product-family/model fields and simulator form factor from the exact device-type identifier. Unsupported/conflicting families fail rather than guessing. Simulator fields absent from the listing remain null with their exact gap availability/provenance; an invented observed value cannot pass. Each private receipt is read once into a byte snapshot, then the same snapshot is hashed and strictly parsed, retaining duplicate-key and all nonfinite-number rejection.

The permanent [standard-library regression suite](test_p0c_readiness.py) runs without private receipts, devices, builds, auth or network. It exercises the public CLI and malformed matrices, plus synthetic receipt classification, absent simulator metadata and single-snapshot hash/parse behavior:

```sh
python3 -B _bmad-output/implementation-artifacts/image-optimization/test_p0c_readiness.py
```

Actual review-patch rerun: **42 tests PASS**, including valid public CLI, the 19 malformed-matrix categories, strict JSON CLI/parser cases, supported/unsupported physical and simulator family classification, exact simulator null gap claims and hash/parse single-snapshot regressions. Public-only CLI and private replay both passed again: 43 source hashes, 60 local links, 13 exact private receipt hashes, nine coverage rows, thirteen null counter gaps and five open gates. Root's separate first eight malformed checks also rejected all eight. Python syntax and whitespace checks passed. These checks validate preparation consistency and rejection behavior; no device/build/auth/network or native workload was run.

Actual final run results and isolated malformed-matrix rejection counts are recorded in the [Story 1.5 verification record](spec-1-5-native-measurement-admission.md#verification). Every native gate remains open after those checks. No app build or native measurement was required or run. Story 1.5 is in review for preparation only; completed root review synchronized only its dedicated image-optimization tracker entry. Story 1.6 remains backlog.
