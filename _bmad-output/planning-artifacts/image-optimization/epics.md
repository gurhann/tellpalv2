---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories]
currentStep: step-04-final-validation
status: planning-recorded-with-execution-gates
created: 2026-10-08
scope: image-optimization-p0
validationDate: 2026-10-08
coverageValidation: PASS
sprintReadiness: CONCERNS
sprintReadinessReport: _bmad-output/planning-artifacts/image-optimization/implementation-readiness.md
sprintStatusFile: _bmad-output/implementation-artifacts/image-optimization/sprint-status.yaml
inputDocuments:
  - _bmad-output/specs/spec-publish-time-image-optimization/SPEC.md
  - _bmad-output/specs/spec-publish-time-image-optimization/client-image-contract.md
  - _bmad-output/specs/spec-publish-time-image-optimization/rendition-evaluation.md
  - _bmad-output/specs/spec-publish-time-image-optimization/brownfield.md
  - _bmad-output/specs/spec-publish-time-image-optimization/p0-small-jpeg-findings.md
  - _bmad-output/specs/spec-publish-time-image-optimization/implementation-plan.md
  - _bmad-output/planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/research.md
  - _bmad-output/planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/task-refinement.md
  - _bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md
  - architecture.md
  - be/docs/adr/ADR-0010-content-cover-ownership.md
  - be/docs/adr/ADR-0017-story-page-media-manifests.md
  - be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md
  - be/docs/adr/ADR-0019-library-home-catalog.md
---

# TellPal Image Optimization — P0 Epic Breakdown

## Overview

This is the separate post-Mobile-1.2 qualification work authorized in the conversation on 2026-10-08. It translates the image-optimization SPEC's P0a–P0c into evidence-producing work. It does not extend Mobile Story 1.2, certify full P0, or implement P1–P5. The existing mobile and CMS/backend epic/status files remain separate.

The scoped SPEC supplies the optimization requirements. Parent PRD FR-15 and NFR-2/3/5/6/7/8 provide context; this document does not redefine or claim full coverage of them. Existing architecture is adopted for read-only inventory and isolated experiments; production publication/schema/access decisions remain SPEC OQ-3–5. No new UX implementation or redesign is part of P0.

## Requirements Inventory

### Functional Requirements

- IO-FR1: Record actual listing, recommendation, detail, future reader and listening-art slots with logical constraints, DPR, fit/alignment, orientation, source ratio and text-safe framing. Distinguish current implementation, hypothetical calculations and device observations. [CAP-2; P0a]
- IO-FR2: Bind source/variant identity, observed dimensions, format and hashes to each role; expose missing sources and unclamped physical-pixel shortages. [CAP-2/4; P0a]
- IO-FR3: Qualify a representative high-resolution/page/text/alpha/orientation/color corpus while retaining the small-JPEG controls and their measured regressions. Freeze exact identifiers and experimental conditions before comparison. [CAP-4; P0a/P0b]
- IO-FR4: Compare JPEG controls and conditional actual WebP lossy/lossless plus relevant PNG at identical normalized geometry; prove the packaged encoder's byte-level round-trip and retain all outcomes. [CAP-4; P0b]
- IO-FR5: Establish a separately qualified native experiment entry with current admission, immutable identity, isolated caches and exact build/engine/renderer/tool evidence before profile/release-equivalent measurements. [CAP-2/4; P0c; OQ-6]
- IO-FR6: Compare identical physical-device journeys for legibility/framing, actual transfer, decoded dimensions, first onstage target-frame latency and peak/settled client/worker resources with raw failures. [CAP-4; P0c]
- IO-FR7: Produce a decision handoff for per-role profiles/budgets, compliant-source reuse and remaining readiness questions without silently adopting them. [CAP-1–5 dependencies; OQ-2–6]

### NonFunctional Requirements

- IO-NFR1: Preserve originals and historical measurements; isolate experiment files/caches and never overwrite live/source objects. Keep private paths, tokens and signed URLs outside tracked evidence.
- IO-NFR2: Separate transfer, all-variant storage, decoded LRU/live/pending ownership and process/native memory; no cache cap, pixel estimate, pilot sample or emulator result certifies production safety.
- IO-NFR3: Preserve inherited crop, aspect ratio, localization/role ownership, alpha/color/orientation and animation boundaries; no upscale or first-frame flattening certifies quality.
- IO-NFR4: Preserve Android API 24 and iOS/iPadOS 15, ADR-0018 architecture/disposal rules, immutable account/access identity, integrity, cancellation and existing controlled-entry release refusal.
- IO-NFR5: Freeze exact source/output/provider/build/tool identities and measurement boundaries. Label unavailable counters/frame association and record every failure, shortage, regression and sampling limitation.
- IO-NFR6: Keep current production profiles/cache limits as controls. Codec selection, new budgets, reuse/admission, publication/live retention and production rollout remain separate decisions.

### Additional Requirements

- `asset` owns transformation/storage/jobs and `content` owns publication scope through public module APIs/events/IDs; P0 adopts no new module edge, schema or publication gate.
- New-publications-first and separate historical backfill remain the approved rollout order. P0 does not authorize deployment, backfill, cleanup or production writes.
- Real authenticated QA uses Firebase UID `jLRypTmoBrhhX21HsDJojurcKTC3`; temporary credentials remain outside Git and existing server allowlists/admission remain binding.
- Planning and sprint artifacts use dedicated `image-optimization/` folders. Build sessions must receive the explicit epic, story directory and sprint-status paths; changing global config or passing only a SPEC path does not ensure isolation.
- The user authorized proceeding with this separate planning scope after unresolved profiles/budgets and staged P0 work were explained. Routine CE continuation uses that direction; it does not invent human profile acceptance, specimen approval, checkpoint preferences or OQ-6 approval.

### UX Design Requirements

- IO-UX1: Measure existing framing and title-safe regions under real slot/DPR/layout conditions, preserving inherited appearance.
- IO-UX2: Inspect supported orientations and phone/tablet/high-DPR/window conditions with native text/diacritic/fine-line/gradient/color/alpha cases before qualifying a candidate.
- IO-UX3: Record quality rejection and source/resource shortage explicitly; placeholder/precache completion is not successful target-image paint.

### FR Coverage Map

| Requirement | Epic | Outcome |
| --- | --- | --- |
| IO-FR1 | 1 | Actual slot and framing demand is traceable. |
| IO-FR2 | 1 | Sources/renditions and shortages are visible. |
| IO-FR3 | 1 | Representative corpus/protocol can be frozen. |
| IO-FR4 | 1 | Real encoder outputs are reproducibly comparable. |
| IO-FR5 | 1 | Native-entry and measurement prerequisites are explicit. |
| IO-FR6 | 1 | Physical-device journeys provide bounded quality/resource evidence. |
| IO-FR7 | 1 | Evidence informs explicit profile/readiness decisions. |

## Epic List

- **Epic 1 — Qualify readable artwork with measured delivery and memory costs.** The content/mobile team can assess which images meet actual display demand and compare verified candidates without accepting unreadable artwork, hiding shortages or assuming memory savings. It delivers evidence and an explicit decision handoff independently of future P1–P5 production implementation. Covers IO-FR1–7, IO-NFR1–6 and IO-UX1–3.

## Scope and Readiness

Use one evidence-focused epic rather than separate backend/client technical-layer epics. P0a maps actual demand and corpus, P0b compares real encoders/worker resources, and P0c prepares then executes native validation. Split each phase where missing material or approval separates preparation from acceptance. Stories may depend on earlier evidence, never future implementation.

The immediately executable slice is the current slot/source inventory. Later corpus and native execution remain backlog until their explicit entry conditions are satisfied. An inventory that documents missing material can complete its own bounded acceptance; that result does not close full P0 or accept unsupported roles. No production profile or budget is selected by planning.

## Epic 1: Qualify Readable Artwork with Measured Delivery and Memory Costs

The content/mobile team can trace actual display demand to retained sources and verified candidates, then assess quality, transfer and resource tradeoffs. The epic produces qualification evidence and a decision handoff; P1–P5 delivery implementation is outside its acceptance. IO-FR1–7, IO-NFR1–6 and IO-UX1–3 are covered below.

### Story 1.1: Measure Current Image Slots and Source Coverage

As a content/mobile engineer,
I want a traceable inventory of actual image slots and available source/rendition pixels,
So that we can identify shortages and select meaningful experiments without changing the accepted screens.

**Phase:** P0a.1. **Requirements:** IO-FR1/2; IO-NFR1–6; IO-UX1/3.
**Entry conditions:** Existing SPEC, source code and retained evidence are available. No authenticated runtime, new source purchase, device harness or production access is required to start.
**Completion boundary:** Code-backed baseline and explicit gaps. Physical observations are included only if actually collected; full P0a/corpus/native qualification remains open.
**Planned outputs:** `p0a-slot-source-inventory.md` and machine-readable geometry/source coverage under `_bmad-output/implementation-artifacts/image-optimization/`.

**Acceptance Criteria:**

**Given** accepted Library/recommendation/detail code and the separate old reader PoC,
**When** current image layouts and image-provider/decode paths are inspected,
**Then** each role records code location, logical slot formula/constraints, fit/alignment, source orientation/ratio and current decode/profile limits,
**And** accepted current, PoC-only and future/unimplemented roles are distinguished without implementing future screens.

**Given** declared logical viewport/DPR inputs and observed source/rendition dimensions,
**When** contain/cover demand is calculated,
**Then** unclamped required dimensions and source-shortage flags remain separate from clamped decode dimensions,
**And** tall/wide, fractional DPR, rotation/window and insufficient-source examples retain reproducible inputs and explicitly label assumed versus device-observed values.

**Given** retained source/rendition evidence,
**When** role/source coverage is mapped,
**Then** known IDs/hashes/MIME/dimensions and provenance are recorded without leaking private paths or URLs,
**And** missing masters/pages/alpha/orientation/color/animation evidence is listed with the next required action; seven small covers cannot close those gaps.

**Given** the baseline inventory is reviewed,
**When** the work unit closes,
**Then** all matrix cells have an observation, derivation or explicit unavailable/not-implemented state,
**And** originals, accepted UI/cache behavior, production settings, Mobile1.2 scope/status and historical experiment evidence remain unchanged.

### Story 1.2: Freeze a Representative Image Corpus and Trial Protocol

As a content/mobile evaluator,
I want exact representative specimens and comparison conditions,
So that profile experiments cannot cherry-pick favorable small covers or compare different geometries.

**Phase:** P0a.2. **Requirements:** IO-FR2/3/7; IO-NFR1/3/5/6; IO-UX1/2.
**Entry conditions:** Story 1.1 baseline; rights-cleared retained high-resolution masters and real text-bearing pages are available; any missing class remains an explicit gate.
**Completion boundary:** Frozen corpus/protocol with selected and deferred roles; no production profile or native quality acceptance.
**Planned outputs:** `p0a-corpus.json`, `p0a-trial-protocol.md`; actual source files remain in isolated private storage.

**Acceptance Criteria:**

**Given** the slot/source baseline and proposed evaluation matrix,
**When** a corpus is selected,
**Then** exact IDs/hashes/actual MIME/bytes/dimensions and role/language/provenance cover large illustrations, real title/text/diacritic pages, fine lines/gradients/dark art, alpha, EXIF rotation/mirroring and ICC cases,
**And** the six historical controls plus separate P3 diagnostic remain identifiable; unsupported animation/non-STORY roles are explicitly deferred rather than certified by a still frame.

**Given** malformed and resource-limit input risks,
**When** rejection specimens are frozen,
**Then** separately labeled synthetic truncated-image, mismatched-MIME, excessive-byte and excessive-pixel fixtures have exact hashes/metadata and expected actionable failure categories,
**And** they remain distinct from valid quality specimens and from client-only corruption scenarios.

**Given** all required source files are available and their preservation checks pass,
**When** trial conditions are frozen,
**Then** normalized reference geometry, encoder candidates, baselines, randomization, proposed five-cold/five-warm pilot distinctions, stage timing, sampling/settlement and raw evidence format are recorded before measurement,
**And** source bytes versus same-role q85 comparisons and storage versus journey transfer accounting remain distinct.

**Given** normalization/framing or specimen suitability needs a human choice,
**When** that choice is unresolved,
**Then** the affected trial cannot start and the exact pending choice is recorded,
**And** OQ-2/3/4 is not silently closed by corpus selection; missing representative sources keep this story incomplete.

### Story 1.3: Prove Real Encoder Outputs on a Bounded Pilot

As an image-pipeline evaluator,
I want real versioned codec outputs from a small controlled pilot,
So that a file extension or registry plan cannot stand in for an encoder qualification.

**Phase:** P0b.1. **Requirements:** IO-FR4; IO-NFR1/3/4/5/6; IO-UX2.
**Entry conditions:** Story 1.2 provides an identified frozen pilot subset, normalized reference pixels and accepted experiment settings. Any new dependency/runtime installation receives its concrete scope before execution.
**Completion boundary:** Isolated codec feasibility and exact runtime evidence; no application/deployment change or production codec choice.
**Planned outputs:** `p0b-encoder-pilot.json`, `p0b-encoder-pilot.md` and an isolated reproducible harness if needed.

**Acceptance Criteria:**

**Given** the same retained normalized pixels and geometry,
**When** the JPEG baseline, actual packaged WebP lossy/lossless and relevant PNG candidates are exercised,
**Then** provider/binary/runtime/settings identities and output SHA/MIME/dimensions/bytes are recorded and independently reread/decoded,
**And** lack of a functioning encoder is an explicit failed qualification; a writer list, build or planned `.webp` path cannot pass.

**Given** color/alpha/orientation/text-sensitive pilot cases,
**When** candidates are compared,
**Then** equal numeric q is never treated as equal visual quality, q100 is not presumed lossless and hidden transparent RGB policy is explicit,
**And** lossy/resize output is not called mathematically lossless; metadata removal alone is not evidence of unmanaged color.

**Given** isolated output paths and retained originals,
**When** retries or the harness fail,
**Then** source hashes remain unchanged, overwrite is refused, partial output is isolated and failures are retained,
**And** AVIF stays optional with exact target/fallback proof required before any later adoption.

**Given** the frozen worker rejection specimens,
**When** malformed, MIME-mismatched or excessive-byte/pixel inputs are exercised,
**Then** bounded failures and actionable rejection reasons are preserved in raw results,
**And** no invalid output is admitted or treated as a successful encode; client rejection evidence does not substitute for these worker checks.

### Story 1.4: Compare Frozen Candidates and Worker Resource Costs

As a content/mobile evaluator,
I want reproducible quality/byte/worker observations for the representative corpus,
So that profile proposals account for regressions and processing costs rather than assumed savings.

**Phase:** P0b.2. **Requirements:** IO-FR3/4/7; IO-NFR1–6; IO-UX2.
**Entry conditions:** Stories 1.2/1.3; actual bounded worker measurement environment and isolated resources; no production storage write is required.
**Completion boundary:** One frozen experiment batch with raw observations and a decision handoff. Additional batches can use the same protocol; no profile/budget/reuse policy is automatically adopted.
**Planned outputs:** `p0b-comparison.json`, `p0b-worker-findings.md` and linked reproducible scripts/settings.

**Acceptance Criteria:**

**Given** identical normalized geometry and frozen role-specific candidates,
**When** each source/output pair is evaluated,
**Then** hashes, actual bytes/dimensions/MIME, encoder identities and all growth/rejection cases are retained against the stated positive denominator,
**And** no combined phone/tablet storage total is labeled journey traffic or universal compression benefit.

**Given** full-decode controls and optional read-time-subsampling arms,
**When** worker work is observed,
**Then** read, transform, encoding-only, verification and end-to-end boundaries are distinguished where actually exercised,
**And** idle/peak/settled heap, native/process/container usage, temp disk, duration, concurrency and encoder threads are recorded with sampling limits and unavailable counters; no cloud-ready revision is claimed by a local experiment.

**Given** visual/resource results or already-small compliant bytes,
**When** the batch report is prepared,
**Then** quality concerns, source shortages and measured tradeoffs inform explicit OQ-2/3/4 proposals,
**And** verified source reuse, acceptable growth, final profiles and new resource budgets remain decisions awaiting acceptance.

### Story 1.5: Prepare Native Image Measurement and Admission

As a mobile/QA evaluator,
I want a concrete device, entry and instrumentation readiness package,
So that native measurements can start through an authorized reproducible path.

**Phase:** P0c.1. **Requirements:** IO-FR5/7; IO-NFR1/2/4/5/6; IO-UX1/3.
**Entry conditions:** Story 1.1 baseline and existing admission/build contracts. Device/tool availability can be inventoried; an unverified device is not presumed available.
**Completion boundary:** Readiness/protocol and reviewable OQ-6 proposal. This story neither implements a new native entry nor claims OQ-6 approval or device qualification.
**Planned outputs:** `p0c-native-readiness.md`, `p0c-device-matrix.json` and a concrete proposed entry/instrumentation change description.

**Acceptance Criteria:**

**Given** available device/tool records and existing app entry code,
**When** readiness is assessed,
**Then** physical versus emulator devices, model/OS/RAM where observed, oldest-floor gaps, build/engine/renderer/tool identities and available counters are recorded,
**And** unavailable or unverified hardware/instrumentation remains explicit rather than inferred from simulator settings.

**Given** the controlled Library entry's release refusal and current server admission,
**When** a profile/release-equivalent experiment path is proposed,
**Then** exact changes, artifact isolation, auth/access, rollback and verification are reviewable without weakening the existing guard,
**And** required human acceptance/implementation work is named before OQ-6 can close; this preparation alone does not authorize the entry.

**Given** the proposed native journey and candidate evidence contract,
**When** measurement instrumentation is specified,
**Then** network-cold, disk-warm/decoded-cold and decoded-warm identities, target-frame association, process/native counters, sampling/overhead and baseline drift are defined,
**And** image-ready cannot fill an unavailable first-painted field; synthetic pressure is distinguished from genuine OS pressure.

### Story 1.6: Measure Native Image Quality and Resource Costs

As a content/mobile evaluator,
I want physical-device quality and resource evidence for the frozen candidates,
So that image profile decisions reflect readable artwork and actual journey cost.

**Phase:** P0c.2. **Requirements:** IO-FR5/6/7; IO-NFR1–6; IO-UX1–3.
**Entry conditions:** Stories 1.2–1.5; OQ-6 resolved with an authorized implemented/verified entry; exact admitted corpus/candidates; available physical device subset and frozen measurement protocol. Any entry implementation is a separately scoped prerequisite, not hidden acceptance work in this story.
**Completion boundary:** A bounded predeclared native batch and explicit per-role/device coverage, including failures and remaining matrix gaps. Full P0 closes only after all agreed qualification coverage and profile decisions are accepted.
**Planned outputs:** `p0c-native-results.json`, `p0c-quality-resource-findings.md` and the evidence-backed P0 decision handoff.

**Acceptance Criteria:**

**Given** qualified physical constrained Android and oldest supported available iPhone/iPad combinations plus modern controls,
**When** identical control/candidate journeys run in the frozen cache/network/build conditions,
**Then** actual bytes/retries/prefetch, selected rendition/provider/decode dimensions, target-frame or explicit unavailable latency, peak/settled process/native memory and raw observations/failures are recorded,
**And** unavailable floor hardware and overlapping memory counters are not hidden or added together; debug/emulator/golden evidence does not substitute for this physical-device qualification.

**Given** representative tall/wide/text/color/alpha cases and supported layouts,
**When** native quality and lifecycle behavior are checked,
**Then** human legibility/framing verdicts cover normal/enlarged text where relevant, high DPR, orientation/window changes and the bounded scroll/detail-back/future-page/expiry/corruption/offline/account/background scenario batch,
**And** unimplemented reader/listening roles stay unqualified; no future production feature is required or implicitly certified by an experiment.

**Given** all batch observations including rejection, shortage, excessive resource use or actual termination,
**When** the result/decision handoff is prepared,
**Then** exact evidence, limitations, jetsam/OS reason where present and remaining role/device gaps are recorded alongside profile/budget/reuse proposals,
**And** a small pilot or descriptive quantile is not production P95/crash-free proof; P1–P5 starts only after its relevant decisions are separately resolved.

## Story Execution Matrix

| Story | Phase | Prerequisites | Current start decision |
| --- | --- | --- | --- |
| 1.1 | P0a.1 | Existing code/spec/evidence | Startable as bounded read-only inventory. |
| 1.2 | P0a.2 | 1.1; representative originals and frozen choices | Backlog; source/choice gates open. |
| 1.3 | P0b.1 | 1.2 pilot subset; concrete encoder runtime scope | Backlog; no encoder qualification inferred. |
| 1.4 | P0b.2 | 1.2/1.3; measured worker environment | Backlog. |
| 1.5 | P0c.1 | 1.1; observed device/tool availability | Preparatory work can follow 1.1 independently of codec experiments. |
| 1.6 | P0c.2 | 1.2–1.5; resolved/implemented OQ-6; physical devices | Backlog; native execution gates open. |

No sprint status value named `blocked` is introduced. Open gates live here and in the readiness report; backlog is not permission to execute a gated story. A story file or automatic `ready-for-dev` floor never overrides these conditions.

## Validation and Handoff

Requirements coverage, story structure and backward-only dependencies were reviewed. Final validation retains the execution concerns in `implementation-readiness.md`; the document does not assert all stories ready. SP created only the dedicated tracker with one epic/six stories: 1.1 is ready for its bounded inventory, five later stories remain backlog and the epic has not started.

The generator reports three generic non-dispatch headings (`P0 Epic Breakdown`, `Epic List`, `Story Execution Matrix`). All intended numbered headings and six exact keys were verified; no story was omitted. These warnings do not represent missing implementation work. Raw generation output is retained in `sprint-generation.json`; script validation reports no problems. Existing mobile/backend plans, trackers and shared BMad config remain unchanged.
