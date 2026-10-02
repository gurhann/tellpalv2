---
title: Mobile implementation readiness
date: 2026-10-01
workflow: bmad-sprint-planning
intent: sprint-planning
gate: CONCERNS
scope: epics-mobile.md
tracking: pending-user-decision
---

# Mobile readiness assessment

**Historical assessment:** The [2026-10-02 refresh](./implementation-readiness-mobile-2026-10-02.md) supersedes this report for current readiness/tracking decisions. After clarifying product decisions, the user invoked SP and the separate mobile tracker was generated with the recorded concerns. The pending-decision statements below describe the October 1 assessment, not the current tracking state.

## Verdict

CONCERNS. The approved seven-epic, 72-story plan supports incremental delivery, beginning with Story 1.1's catalog companion work. It does not yet support unconditional implementation of every story without resolving recorded decisions. Missing future runtime evidence alone is not a planning failure. The concerns below identify actual undecided contracts or acceptance parameters and the point at which they must be resolved.

This assessment does not mark stories ready, pass the separate epic final-validation workflow, or authorize implementation or release. V2–V5 document corrections were recorded in the October 1 revalidation; repeating their historical findings as unresolved document defects would be inaccurate.

## Sources and scope

- [Mobile epics](./epics-mobile.md): source of story identities, approved scope, prerequisites, traceability and delivery boundaries.
- [PRD](./prds/prd-tellpalv2-2026-09-20/prd.md): active requirements and OQ decision register.
- [Mobile architecture](./architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md): adopted invariants and explicitly deferred contracts.
- [Final validation](./epics-mobile-final-validation.md): September 29 findings and September 30/October 1 remediation and structural revalidation.
- [Legacy audit](./epics-mobile-legacy-open-points-audit.md): reference behavior versus remaining replacement decisions.
- Root AGENTS.md, backend project memory, canonical architecture, ADR-0017 and ADR-0018, and flutter-scalable-app architecture guidance.
- Existing implementation-artifacts/sprint-status.yaml belongs to the separate CMS/backend epics. The configured docs project-knowledge directory and a project-context.md were not found; existing repository guidance supplies context.

This is a planning assessment, not a fresh source-code, visual-parity, provider, store-policy or device audit. Existing validation reports supply the full requirement-coverage baseline; no runtime test results are claimed.

## Findings and closure points

| ID | Concern and evidence | Required closure | Effect on Story 1.1 |
| --- | --- | --- | --- |
| MR-1 | The 2026-10-02 decision closes later-device import: continue with existing new-system account data and import no old local records. OQ-3/6 still lack initial-migration field mappings and verified deletion/remigration mechanics. Removed-content history/statistics retention with no unavailable-content opening was approved on 2026-10-02. New-system-only deletion scope is already decided. See PRD OQ-3/6, Stories 3.12, 3.14–3.17, 3.20–3.21. | Record source/target examples, safe disposition, retained-history/statistics fixtures, identity/idempotency rules and deletion boundaries before the affected migration/deletion implementation and acceptance. Use bmad-prd for unresolved product behavior and bmad-architecture for contract design. | Does not block catalog companion work. AD-5 remains required before Epic 3 auth/migration development. |
| MR-2 | The ten EXT records lack named execution assignments and verified contracts. OQ-4/8 leave discovery payloads and production media admission/renewal/revocation/retention parameters open. ADR-0017 is an experiment, not the production contract. | Assign companion work when each consuming story starts. For 1.1, define catalog order, preview limits, language/audience/error examples and authorized test access; verify actual backend results before mobile integration acceptance. Resolve production media contracts before affected reader/player integration. Use bmad-build for the authorized companion feature slice, with architecture decisions recorded as needed. | Starting 1.1 is allowed by its explicit sequence. It is not integration-ready or accepted. |
| MR-3 | OQ-2/10 leave device/network matrices, memory/latency budgets, observation samples/windows, release stages and stop/expand/recovery thresholds unspecified. Naming the user as store-release operator does not define these thresholds. See shared readiness checks and Stories 7.6/7.10/7.11. | Define applicable feature measurement criteria before affected acceptance, and first-exposure versus expansion evidence before rollout. Product owner and technical/QA/operations contributors must record the actual decisions; use bmad-prd and bmad-architecture as appropriate. | Does not prohibit the first feature slice; its toolchain and relevant UX checks still need verification. |
| MR-4 | The restore transfer product rule was approved on 2026-10-02; its production/sandbox identity and reconciliation contracts remain unverified. OQ-7/9 also leave provider/route formats, event fields/retry and SDK/data contracts pending; analytics retention is decided as no automatic expiration for now. Story 4.5 permits a future direct-paywall destination conditionally dependent on Story 6.2. | Settle each contract before its consumer. If a direct-paywall route is included in 4.5, explicitly defer that route's acceptance until 6.2 or revise the delivery sequence; do not claim an unconditional forward-dependency pass. Use bmad-prd for scope decisions and bmad-architecture for integration contracts. | No effect on Library listing. |

The plan preserves user-value epics and bounded story outcomes. The documented search/listening and controlled-session boundaries allow incremental acceptance. No new scope or story split is justified solely by document length. The conditional 4.5/6.2 dependency must remain visible when that destination contract is selected.

## Proposed tracking and first work unit

Keep the CMS/backend tracker intact. Both plans use Epic 1 and Story 1.1 identifiers, so merging them into the existing flat key space would conflate unrelated work.

Proposed mobile tracker: `_bmad-output/implementation-artifacts/mobile/sprint-status.yaml`.
Proposed mobile story directory: `_bmad-output/implementation-artifacts/mobile/`.
Source: `epics-mobile.md` only. Expected initial state: seven epics and 72 stories in backlog, seven optional retrospectives. Future mobile build sessions must explicitly use this tracker and epic source. Backlog tracking is not integration readiness.

The deterministic generator dry run succeeded: 7 epics, 72 stories, 79 backlog entries and 7 optional retrospectives; no illegal values, dropped orphans or upgrades from existing story files. Its two heading warnings refer to the document title and `Epic List` overview, not omitted epic/story definitions; all expected story counts were parsed. The dry run used a placeholder midnight timestamp; an actual write must use the execution time. The initial uv invocation failed in the sandbox's macOS configuration access; the approved retry succeeded. No status file was written.

After the tracking decision, the next implementation invocation is bmad-build for Story 1.1 only: verify the compatible toolchain, assign and implement the catalog companion contract, establish authorized test access, implement Library listing and collect backend/mobile acceptance evidence. Story 1.2 follows accepted 1.1; reader implementation starts at 1.3.

## Decision required by the invoked workflow

The sprint-planning readiness gate instructs: for CONCERNS, ask whether to proceed anyway or fix first. The recommendation is to create the isolated mobile backlog while preserving the above story-specific gates. This does not waive any gate or authorize guessing unresolved product behavior. No tracking file is written until that decision is received.

### Test-environment decision — 2026-10-02

The user approved an emulator/Simulator-first test matrix on 2026-10-02 because physical-device access is limited. Cover older/lower-capacity and current Android/iOS phone/tablet profiles virtually and use available physical devices for supplementary checks. Record host, virtual device/OS, build mode, network and cache context for memory/latency results; these results are not physical-device performance evidence. Signed store-upgrade, actual purchase and platform-specific evidence requirements remain separately tracked. Exact profile availability, memory/latency budgets and crash-free sample/window remain open. The 99.5% per-platform target is unchanged.

### Performance-limit approach — 2026-10-02

On 2026-10-02 the user approved determining numeric memory and launch/playback latency acceptance limits from measurements of the first working reading/listening flows in the approved test environments. Record representative content, environment, network/cache conditions and results, then establish the limits before accepting affected stories. Unexplained continuous memory growth, freezes and critical playback failures block acceptance. The measurement approach is decided; numeric limits and measurement evidence remain pending.

### Crash-free gate mapping — 2026-10-02

On 2026-10-02 the user approved initial limited distribution after critical tests and other applicable release prerequisites pass. Evaluate the per-platform target of at least 99.5% crash-free users using actual usage data before expanding distribution. Define the observation window and sufficient user sample in the release plan. Successful emulator tests do not establish this user metric. This decision sets the gate mapping, not the numeric window/sample or present permission to publish.

### Restore policy approved — 2026-10-02

The user approved the default RevenueCat transfer policy on 2026-10-02. Pre-registration purchases remain supported; a restore under another TellPal account using the purchasing store account transfers the subscription access, and the source loses that transferred entitlement. Identified login alone does not transfer purchases, and billing operations do not merge TellPal profiles/history. Production/sandbox settings, historical identity/aliases and backend reconciliation still require verification; no dashboard change or runtime acceptance is claimed.

### Analytics retention and storage decision — 2026-10-02

On 2026-10-02 the user chose no automatic deletion or age-based expiry for persisted analytics events for now. Existing Google Analytics data remains available, and backend events provide an additional long-term source. Start in the current backend database; a separate analytics database is a conditional future response to measured application/backend performance impact, not an immediate prerequisite. Mobile pending-queue bounds and detailed diagnostic policies remain separate contracts.

### Legacy-system shutdown decision — 2026-10-02

The user approved shutting down the legacy system only after the replacement's staged rollout completes and migration/subscription stability is verified, followed by their separate explicit shutdown approval. No fixed shutdown date is set; keep the legacy system running until that approval. This closes the product shutdown condition in OQ-10 and Story 7.10. Stability evidence criteria, rollout thresholds and rehearsed recovery remain release/operations work. This planning decision is not present authorization to shut down the legacy system.
