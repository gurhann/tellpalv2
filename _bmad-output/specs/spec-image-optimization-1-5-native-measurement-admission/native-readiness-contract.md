# Native Readiness and Admission Contract

## Scope and dispatch

The original Story 1.5 preparation deliverables were completed at snapshot `3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd`. Their observations and source hashes remain historical. The selected-platform follow-on is governed by [native-pilot-scope.md](native-pilot-scope.md); neither that selection nor historical completeness establishes current runnable devices or native execution readiness.

| Field | Required value |
| --- | --- |
| Epic | `_bmad-output/planning-artifacts/image-optimization/epics.md`, Story 1.5 |
| Phase | P0c.1; IO-FR5/7, IO-NFR1/2/4/5/6, IO-UX1/3 |
| Prerequisite | Story 1.1 slot/source baseline; existing admission/build contracts |
| Output directory | `_bmad-output/implementation-artifacts/image-optimization/` |
| Tracker | `_bmad-output/implementation-artifacts/image-optimization/sprint-status.yaml` |
| Completion | Readiness/protocol and a reviewable OQ-6 proposal; native entry implementation and native results remain separate |
| Selected follow-on | Local physical iPhone 12 and Android Device Streaming physical Android; setup/preflight only until relevant entry and batch gates close |

Pass these explicit paths to `bmad-build`. Numeric `1.5` keys overlap with mobile/CMS plans. Leave `story_key` unset unless isolated resolver behavior is demonstrated; do not alter global BMad config or synchronize another tracker. Spec creation alone changes no sprint status.

## Deliverables and preparation sequence

1. Load the retained [slot/source baseline](../../implementation-artifacts/image-optimization/p0a-slot-source-inventory.md) and inspect current entry, backend admission, build, transport and cache seams. Record the checked source revision and relevant file hashes.
2. Inventory device/tool evidence without treating historical records as current availability. Read-only local device/tool discovery may be performed in the later preparation work; record command/time/result and preserve failures. Installing tools, provisioning/signing/installing builds or accessing authenticated services is not implied by discovery.
3. Populate `p0c-device-matrix.json`; every target category has an observed, historical-only, unverified or unavailable result and next action.
4. Produce `p0c-native-entry-proposal.md` with exact proposed patches and per-platform credential/delivery/rollback mechanics. Produce `p0c-native-measurement-protocol.md` with candidate/cache/journey/counter definitions; unresolved execution choices remain draft.
5. Produce `p0c-native-readiness.md` linking the matrix, proposal, protocol, checked source evidence and all remaining gates. Declare preparation completeness separately from native execution readiness.

Keep raw device identifiers, private tool traces, media, tokens and URLs in private storage outside Git. Public artifacts use an opaque device alias and sanitized evidence receipt/hash. A source or tool change invalidates the affected readiness claim until rechecked; a historical observation keeps its original provenance.

Do not update the old matrix's hashes to accommodate this spec revision. Its original verifier intentionally rejects changed current inputs. Replay the unchanged verifier/tests in a private archive of the preparation snapshot, and create new scope/readiness outputs for current pilot checks. The original source baseline `7e3ee8f58a210196ae6988dfa38481d5bd9a7957` remains distinct from the preparation snapshot.

## Device/tool evidence schema

The machine output uses `schema_version: 1`, `scope: image-optimization-p0c-1`, an observation timestamp, checked source revision and arrays named `devices`, `tools`, `coverage` and `gates`. Each observation carries `availability` (`observed`, `historical_only`, `unverified`, `unavailable` or `not_applicable`), a nullable value, evidence reference and reason when absent. No native results belong in this matrix.

| Record | Required fields |
| --- | --- |
| Device | Opaque alias; `kind` (physical/emulator/simulator); platform/form factor; observed model, OS/API/build, RAM value/unit/provenance; observed connection/runnable state/time; floor/control role; unavailable reason/next action |
| Build | Target/flavor, mode, application/bundle identity, source/artifact hashes, package-lock identities, Flutter/Dart/engine/renderer identities and supporting evidence; compilation and successful device execution are distinct |
| Tool | Exact name/version/build and executable/package identity where observable; platform permissions/attachment limits; available counters, units, scopes, sampling support and capture overhead status |
| Coverage | Required platform/form-factor/floor category, matching device aliases or explicit gap, intended role/journey, evidence age and verification needed before execution |
| Gate | Stable gate ID, status, affected cells, evidence/decision needed and next action; acceptance, implementation and verification references are separate nullable fields |

Include constrained physical Android phone/tablet, oldest supported available physical iPhone/iPad combinations and modern controls. Record Android API 24 and iOS/iPadOS 15 gaps explicitly. Do not invent a low-RAM cutoff or infer RAM/model from emulator settings. A connected device is not necessarily runnable with the frozen toolchain. An unknown renderer/counter stays unknown; family names or current documentation do not identify a built engine.

Reject duplicate aliases, missing required coverage rows, invalid timestamps, nonfinite values, boolean numeric values, negative byte counts, unknown units and an observed value without provenance. Unknown RAM/counters remain null rather than zero. Signed baseline drift is valid and belongs to the protocol rather than this inventory.

## Native candidate admission

Admission is a per-role/device/journey ledger for future Story 1.6, not a global codec verdict. It must bind:

- Exact corpus/freeze/reference, source asset/language/role, original SHA/bytes/MIME/dimensions and normalized reference identity.
- Exact candidate output SHA/bytes/actual MIME/dimensions, profile/settings and encoder/runtime identity; independent reread/decode evidence and immutable output relation.
- Native build/engine/renderer, actual slot/viewport/DPR/fit/alignment, unclamped two-axis demand, selected rendition and bounded aspect-preserving decode identity; source/profile/resource shortages remain separate.
- Delivery authorization, account/access scope, revision, verified local integrity, request/attempt ownership and cache namespace; a changed signed URL cannot redefine immutable content identity.
- Eligibility to run, pending human quality verdict, failures and deferred classes. Quality/resource acceptance needs its own evidence and agreed criteria after execution.

The [r3 trial protocol](../../implementation-artifacts/image-optimization/p0a-trial-protocol.md), [corpus ledger](../../implementation-artifacts/image-optimization/p0a-corpus.json), [encoder pilot](../../implementation-artifacts/image-optimization/p0b-encoder-pilot.md) and [worker findings](../../implementation-artifacts/image-optimization/p0b-worker-findings.md) retain exact predecessor identities. Two v7 source-resolution references, fourteen pairs and twenty-eight output-verified arms establish bounded offline codec evidence. They establish neither native admission nor reader slot/DPR adequacy. Recheck actual private files and freezes when proposing a native batch; do not copy a claimed hash as verification.

Current listing/recommendation/detail, legacy reader PoC and unimplemented reader/listening roles remain distinct. A fixture raster can qualify only its declared isolated display case; it cannot certify a real reader journey. Real alpha/EXIF rotation/mirroring, animation, non-STORY roles and missing native quality remain open. Synthetic rejection fixtures stay separate from quality specimens; a still frame cannot qualify animation. Retain the six historical JPEG controls and separate P3 color diagnostic without assigning them new device/resource observations.

## Gates for Story 1.6

| Gate | Required closure evidence |
| --- | --- |
| NATIVE-ENTRY | Accepted OQ-6 scope; implemented isolated target, credential/delivery paths and instrumentation; positive and negative verification with unchanged current release/admission guards |
| NATIVE-DEVICE | Actually available physical subset and exact build/tool compatibility; missing agreed floor coverage remains an explicit qualification gap |
| NATIVE-CANDIDATE | Purpose-scoped source/output/reference identities; role/geometry/delivery eligibility; source shortages and deferred classes accounted for |
| NATIVE-PROTOCOL | Frozen selected cells, journeys, cache states, order/repeats, counter/boundary methods, limits, settlement and evidence retention |
| NATIVE-DECISIONS | Relevant quality/resource criteria and unresolved OQ-2/3/4/5 dependencies identified; production policies are not inferred from experiment admission |

An incomplete native gate does not prevent bounded preparation from completing if it has a concrete cause and next action. It prevents the affected native execution or claim. Missing first-frame association may yield an explicitly image-ready-only experiment if that limitation is admitted in the frozen batch; it cannot pass a first-painted criterion.

## Acceptance and verification

| Criterion | Demonstrable preparation outcome |
| --- | --- |
| AC-1 / CAP-1 | Every required physical/emulated/floor/tool/counter cell is accounted for, with observed identities or explicit gaps; historical/debug evidence is labeled |
| AC-2 / CAP-2 | Proposed exact target/files, artifact and credential isolation, Firebase/access boundaries, rollback and tests are reviewable; OQ-6 acceptance/implementation/verification remain separate |
| AC-3 / CAP-3 | Native admission ledger requirements preserve predecessor identity/purpose, actual demand and deferred classes without declaring candidates qualified |
| AC-4 / CAP-4 | Three cache modes, target-frame/null semantics, process/native counters, actual sampling/overhead/drift and pressure provenance are defined |
| AC-5 / CAP-5 | All four linked deliverables exist with local links, consistent statuses and explicit Story 1.6 entry gates; no native result is claimed |
| AC-6 / all | Source/history/app/config/profile/cache and unrelated trackers are preserved; public outputs contain no private paths, raw IDs, tokens or URLs |

Use document/schema/link/privacy checks appropriate to these artifacts. If a verifier is implemented, exercise its substantive rejection rules with small synthetic records. App builds and native runs are not verification requirements for spec authoring or readiness-only documentation. Record checks actually performed and unavailable discovery separately.

## Source-label discrepancy

The parent [SPEC](../spec-publish-time-image-optimization/SPEC.md) records OQ-2 as corpus/slots/devices/budgets, OQ-3 as reuse/growth, OQ-4 as alpha/color/EXIF/animation/normalization, OQ-5 as publication/retention/protected caching and OQ-6 as native entry. The historical [Story 1.4 findings](../../implementation-artifacts/image-optimization/p0b-worker-findings.md) label codec/provider, worker budget and growth/reuse proposals OQ-2, OQ-3 and OQ-4 respectively. These labels disagree; neither artifact is rewritten here and no decision is considered resolved. Refer to each proposal by its subject and source until labels are reconciled. OQ-6 agrees across both sources.
