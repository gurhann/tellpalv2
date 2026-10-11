---
id: SPEC-image-optimization-1-5-native-measurement-admission
status: draft
created: 2026-10-11
updated: 2026-10-11
scope: image-optimization-p0c-1
companions:
  - native-readiness-contract.md
  - native-entry-proposal.md
  - native-measurement-protocol.md
  - ../spec-publish-time-image-optimization/SPEC.md
  - ../spec-publish-time-image-optimization/client-image-contract.md
  - ../spec-publish-time-image-optimization/rendition-evaluation.md
  - ../../../architecture.md
  - ../../../be/docs/project-memory.md
  - ../../../be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md
  - ../../../be/docs/adr/ADR-0019-library-home-catalog.md
sources:
  - ../../planning-artifacts/image-optimization/epics.md
  - ../../planning-artifacts/image-optimization/implementation-readiness.md
  - ../../implementation-artifacts/image-optimization/epic-1-context-r4.md
---

> **Canonical story contract.** Read this SPEC with its companions. The scope is Story 1.5 preparation; acceptance of this document does not accept, implement or verify OQ-6, admit native candidates or authorize Story 1.6 measurements.

# Story 1.5 — Prepare Native Image Measurement and Admission

## Why

The mobile/QA team needs a reproducible native experiment path before image quality and resource measurements can inform profile decisions. Current Library diagnostics and historical debug/emulator results do not establish physical-device readiness, native memory costs or first target-image paint. The controlled development entry also refuses release. Prepare an evidence-backed readiness package and a concrete entry/instrumentation proposal while preserving those boundaries.

## Capabilities

- **CAP-1**
  - **intent:** The evaluator can identify the devices, builds, tools and counters actually available for native image evaluation.
  - **success:** Every coverage cell distinguishes current physical observation, historical evidence, emulator/simulator, unavailable and unverified states, with provenance and explicit supported-floor gaps.
- **CAP-2**
  - **intent:** The team can review a bounded native experiment entry that preserves existing admission and application boundaries.
  - **success:** The proposal identifies exact app/native changes, artifact isolation, ephemeral authentication, access, instrumentation, rollback and verification; required human acceptance and separate implementation remain explicit. See `native-entry-proposal.md`.
- **CAP-3**
  - **intent:** The evaluator can determine which source/candidate/role combinations are eligible for a future native batch.
  - **success:** Admission binds exact verified identities and display demand, identifies missing source/role/quality evidence, and keeps offline codec verification separate from native admission and human quality approval.
- **CAP-4**
  - **intent:** The evaluator can specify comparable native journeys and observable image/resource boundaries.
  - **success:** The protocol defines three verifiable cache states, request/decode/target-frame association, actual journey transfer, separate memory counters, sampling/overhead, baseline drift and failure records; unavailable first paint never inherits image-ready latency. See `native-measurement-protocol.md`.
- **CAP-5**
  - **intent:** The team can hand off readiness without mistaking preparation for completed qualification.
  - **success:** `p0c-native-readiness.md`, `p0c-device-matrix.json` and linked entry/protocol proposals account for all required cells and remaining Story 1.6 gates. Missing hardware or instrumentation is an explicit bounded preparation result.

## Constraints

- Story 1.1 is the preparation prerequisite. Stories 1.2–1.4 supply evidence when available; their completion is not required to inventory readiness. Native execution requires Stories 1.2–1.5, an accepted/implemented/verified OQ-6 entry, admitted candidates, physical devices and a frozen protocol.
- Preserve the existing Library release refusal, local/test backend restriction, verified Firebase admission, empty-by-default server allowlists and server-owned audience fixtures. Catalog metadata grants no protected playback rights. Real authenticated QA uses `jLRypTmoBrhhX21HsDJojurcKTC3`; secrets and signed URLs stay outside repository evidence, telemetry and binaries.
- Preserve accepted artwork/framing, source and immutable account/access/revision identity, integrity, cancellation and composition-owned disposal. Retain current cache/profile/transport limits as controls. Android API 24 and iOS/iPadOS 15 remain the supported floors; build settings do not prove floor-device availability.
- Retained offline reference/output verification establishes only its admitted runtime and purpose. Source-resolution pixels, smaller files, LRU counts, synthetic pressure and debug/emulator results cannot qualify native quality, first paint, process memory or production safety.
- Keep image-ready, first rasterized onstage target frame and physical display presentation distinct. Keep transfer, all-variant storage, decoded/live/pending ownership and overlapping process/native counters separate; missing values require reasons.
- New publication profiles, numerical budgets, reuse/growth, normalization/role scope and publication/retention/cache policy remain parent decisions. Story 1.5 proposes and records prerequisites without adopting them.

## Non-goals

- Implementing or running the new entry, serving new candidate bytes, acquiring hardware, installing dependencies, changing native signing or conducting Story 1.6 measurements.
- Reader/listening/auth/paywall features, new production schemas or module edges, CMS/UI redesign, cache tuning, final codecs/budgets, source/live overwrite, backfill, cleanup or deployment.
- Declaring physical-device qualification, quality-qualified savings, production P95 or crash-free behavior from preparation or finite pilot evidence.

## Success signal

A reviewer can trace every readiness claim to an observation or explicit gap, inspect a concrete isolated entry/instrumentation proposal, and determine the exact acceptance/implementation/verification needed before a frozen native batch can start. Preparation can complete with documented unavailable devices/counters while Story 1.6 remains gated.

## Open Questions

- **OQ-6 — native entry:** Which exact target, platform credential-provisioning mechanism, candidate-delivery path and instrumentation package will be separately accepted, implemented and verified? Story 1.5 makes the proposal concrete; this spec does not close it.
- **Native batch readiness:** Which physical subset and role/candidate/cache/journey cells are actually available, and which sampling/repeat settings will be frozen for Story 1.6? Unavailable floor devices and reader/alpha/EXIF/animation coverage remain visible.
- **Historical OQ labels:** Story 1.4's findings label OQ-2/3/4 differently from the parent SPEC. Reconcile labels before adopting those decisions; `native-readiness-contract.md` records both meanings. OQ-6 agrees and preparation can proceed.
