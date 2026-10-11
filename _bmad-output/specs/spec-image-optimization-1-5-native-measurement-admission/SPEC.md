---
id: SPEC-image-optimization-1-5-native-measurement-admission
status: draft
created: 2026-10-11
updated: 2026-10-11
scope: image-optimization-p0c-pilot-r1
companions:
  - native-readiness-contract.md
  - native-entry-proposal.md
  - native-measurement-protocol.md
  - native-pilot-scope.md
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

> **Canonical story contract.** Read this SPEC with its companions. Story 1.5 preparation is retained as historical evidence. The user selected local iPhone 12 and Android Device Streaming and authorized continued pilot setup. Device selection does not verify OQ-6, admit candidates or close Story 1.6 measurement gates.

# Story 1.5 — Prepare Native Image Measurement and Admission

## Why

The mobile/QA team needs a reproducible native experiment path before image quality and resource measurements can inform profile decisions. The selected pilot uses a local physical iPhone 12 and a remote physical Android phone through Android Device Streaming. Current diagnostics and historical debug/emulator results do not establish runnable devices, native memory costs or first target-image paint. Continue setup and preflight from the retained preparation package while preserving the controlled development entry's release refusal.

## Capabilities

- **CAP-1**
  - **intent:** The evaluator can identify the devices, builds, tools and counters actually available for native image evaluation.
  - **success:** Every coverage cell distinguishes selected hardware/provider, current physical observation, historical evidence, emulator/simulator, unavailable and unverified states, with provenance and explicit supported-floor gaps.
- **CAP-2**
  - **intent:** The team can review a bounded native experiment entry that preserves existing admission and application boundaries.
  - **success:** The proposal identifies exact app/native changes, artifact isolation, ephemeral authentication, access, instrumentation, rollback and verification; acceptance, implementation and verification remain separate. Selected-platform setup/preflight can proceed independently. See `native-entry-proposal.md` and `native-pilot-scope.md`.
- **CAP-3**
  - **intent:** The evaluator can determine which source/candidate/role combinations are eligible for a future native batch.
  - **success:** Admission binds exact verified identities and display demand, identifies missing source/role/quality evidence, and keeps offline codec verification separate from native admission and human quality approval.
- **CAP-4**
  - **intent:** The evaluator can specify comparable native journeys and observable image/resource boundaries.
  - **success:** The protocol defines three verifiable cache states, request/decode/target-frame association, actual journey transfer, separate memory counters, sampling/overhead, baseline drift and failure records; unavailable first paint never inherits image-ready latency. See `native-measurement-protocol.md`.
- **CAP-5**
  - **intent:** The team can hand off readiness without mistaking preparation for completed qualification.
  - **success:** Retained preparation outputs and a separately versioned selected-pilot preflight account for required cells and remaining Story 1.6 gates. Missing hardware, client/account access or instrumentation is an explicit result; historical observations are not relabeled current.

## Constraints

- Story 1.1 is the preparation prerequisite. Stories 1.2–1.4 supply evidence when available; their completion is not required to inventory readiness. Native execution requires Stories 1.2–1.5, an accepted/implemented/verified OQ-6 entry, admitted candidates, physical devices and a frozen protocol.
- Preserve the existing Library release refusal, local/test backend restriction, verified Firebase admission, empty-by-default server allowlists and server-owned audience fixtures. Catalog metadata grants no protected playback rights. Real authenticated QA uses `jLRypTmoBrhhX21HsDJojurcKTC3`; secrets and signed URLs stay outside repository evidence, telemetry and binaries.
- Preserve accepted artwork/framing, source and immutable account/access/revision identity, integrity, cancellation and composition-owned disposal. Retain current cache/profile/transport limits as controls. Android API 24 and iOS/iPadOS 15 remain the supported floors; build settings do not prove floor-device availability.
- Retained offline reference/output verification establishes only its admitted runtime and purpose. Source-resolution pixels, smaller files, LRU counts, synthetic pressure and debug/emulator results cannot qualify native quality, first paint, process memory or production safety.
- Keep image-ready, first rasterized onstage target frame and physical display presentation distinct. Keep transfer, all-variant storage, decoded/live/pending ownership and overlapping process/native counters separate; missing values require reasons.
- Limit pilot conclusions to the actual iPhone 12 and reserved physical Android device/build/cells. Streaming video timing is not device paint or physical color evidence. Verify host-service reachability without credentials before session handoff; selected cloud access does not authorize unbounded paid reservations.
- Preserve preparation snapshot `3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd` and original source baseline/evidence hashes. Recheck affected current claims after source/tool changes; use a private historical snapshot for the original verifier.
- New publication profiles, numerical budgets, reuse/growth, normalization/role scope and publication/retention/cache policy remain parent decisions. Story 1.5 proposes and records prerequisites without adopting them.

## Non-goals

- In this setup increment: changing app/native entry or signing, exchanging credentials, serving candidate bytes, acquiring hardware, unattended cloud reservations/installations, or conducting Story 1.6 measurements before its gates close.
- Reader/listening/auth/paywall features, new production schemas or module edges, CMS/UI redesign, cache tuning, final codecs/budgets, source/live overwrite, backfill, cleanup or deployment.
- Declaring physical-device qualification, quality-qualified savings, production P95 or crash-free behavior from preparation or finite pilot evidence.

## Success signal

A reviewer can reproduce historical preparation validation, run a private selected-pilot preflight, and identify the exact setup, entry and batch gates remaining for local iPhone 12 and Android Device Streaming. Missing devices/counters can complete preparation while affected measurements remain gated.

## Open Questions

- **OQ-6 — native entry:** Which exact target, platform credential-provisioning mechanism, candidate-delivery path and instrumentation package will be separately accepted, implemented and verified? Story 1.5 makes the proposal concrete; this spec does not close it.
- **Selected-device readiness:** Is the local iPhone 12 connected/runnable, which remote physical Android model/OS is reserved, and which Streaming client/project/IAM/quota and counter permissions are available? The platform choice is resolved; actual availability remains to be verified.
- **Native batch readiness:** Which role/candidate/cache/journey cells and sampling/repeat settings will be frozen for the selected devices? Unavailable floor devices and reader/alpha/EXIF/animation coverage remain visible.
- **Historical OQ labels:** Story 1.4's findings label OQ-2/3/4 differently from the parent SPEC. Reconcile labels before adopting those decisions; `native-readiness-contract.md` records both meanings. OQ-6 agrees and preparation can proceed.
