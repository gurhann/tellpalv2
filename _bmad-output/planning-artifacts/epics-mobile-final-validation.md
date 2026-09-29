---
title: TellPal Mobile Epics — Final Validation
date: 2026-09-29
status: remediation-required
workflow: bmad-create-epics-and-stories
step: step-04-final-validation
artifact: epics-mobile.md
implementationReadiness: not-established
---

# Final validation — TellPal mobile epics

## Verdict and evidence limits

Validation was performed; the workflow does not pass unconditionally. Seven epics contain 72 unique stories (12/10/21/6/5/7/11). All 28 active FRs, nine NFRs and 25 UX-DRs have story references. FR-17 remains withdrawn, with no implementing story. Reference coverage is complete; executable acceptance and readiness remain conditional.

The existing artifact records steps 1–3 as completed and step 4 as current. This review resumes that state at the user's request; it does not regenerate the inventory or change the approved seven-epic order. `stepsCompleted` remains unchanged because the final validation has not passed. No application code, source product decision, dependency contract or runtime evidence is changed.

Sources inspected: root AGENTS.md, backend project memory, ADR-0018, relevant ADR-0017, canonical backend architecture, mobile architecture spine, PRD/addendum/extraction/reconciliation sources, the historical design-system contract and its screen/tablet/asset/coverage inventories, and the referenced iOS Simulator report. No project-context.md was found. All frontmatter input paths exist in this workspace, including the external worktree report; that absolute path is not a portable repository attachment.

Checks combine document review with a read-only structural scan for story counts, required fields, Given/When/Then presence, requirement ranges, prerequisite references, placeholders and input-path existence. Every AD-1–12 Rule paragraph matches the adopted mobile spine. Neither these checks nor the historical Simulator report establish backend readiness, executed acceptance, signing compatibility or current store-policy compliance. No application build or test suite was run for this document review.

## 1. Functional coverage

The following map identifies principal acceptance owners rather than treating a mention of an FR as proof that it has been implemented.

| FR | Principal stories / contract boundary | Result |
| --- | --- | --- |
| 1 | 3.3/3.5/3.10/3.12/3.14–3.17; incremental shell 1.1/2.1 | Traced; session/reference gates open |
| 2 | 3.4–3.9 | Traced; provider, validation and action-link evidence required |
| 3 | 3.3/3.5/3.15/3.16 | Traced; conversion/import contracts open |
| 4 | 3.10/3.11/3.14–3.17 | Traced; both legacy schemas require field fixtures |
| 5 | 3.5/3.13/3.18–3.20 and 4.3 | Traced; deletion/notification contracts open |
| 6 | 1.1/1.2/1.5/1.8 and 2.1/2.7 | Traced; catalog/order/pagination/recommendation fixtures required |
| 7 | 1.9 and 2.7 | Traced; matching/count/pagination contract open |
| 8 | 1.2/1.3, 2.1/2.3/2.7 and 5.2 | Traced; shared identities and independent readiness required |
| 9 | 1.10/2.1/2.7/2.9/3.13/5.4 | Traced; findings V2/V5 apply |
| 10 | 1.1/1.2/1.8/1.9, 2.1–2.3/2.7, 6.1–6.7 | Traced; explicit coupled free-access check missing (V4) |
| 11 | 1.2/1.3 and 2.2/2.3/2.7 | Traced; explicit start and direct lullaby entry distinguished |
| 12 | 1.3/1.4/1.11/1.12 | Traced; production manifest/asset contracts open |
| 13 | 1.4–1.7, 2.6 and 6.2 | Traced; completion/control characterization open |
| 14 | 2.2–2.6/2.9/2.10 | Traced; real-device and exit/takeover/loop evidence required |
| 15 | 1.3/1.4/1.12 and 2.2/2.3/2.10 | Traced; OQ-2 matrix must include Wi-Fi and cellular cases |
| 16 | 1.5/1.7, 2.5/2.8/2.9 and 3.12/3.14–3.17 | Traced; durable history/duration/import contracts open |
| 17 | None | Correctly withdrawn; historical data preservation remains |
| 18 | 4.1–4.6 | Traced; support contracts and gate ownership V3 remain open |
| 19 | 6.1/6.3/6.4/6.6/6.7 | Traced; real store/RevenueCat/account fixtures required |
| 20 | 6.2/6.5/6.6 | Traced; actual offerings and supported promotion contracts required |
| 21 | 7.1–7.4 | Traced; all contracted event categories, privacy and deduplication included |
| 22 | 5.2/5.3/5.5 | Traced; backend/CMS publication delivery is external |
| 23 | 5.1/5.3 and 7.8 | Traced; tester permission remains separate from subscription |
| 24 | 5.2/5.3 and EXT-PUBLICATION/EXT-MEDIA | Traced; actual outputs and CMS processing-error/retry evidence required |
| 25 | 1.3/1.11, 2.2/2.3/2.9, 5.1/5.2/5.5 and 6.1–6.7 | Traced; admission/renewal/revocation/retention contracts open |
| 26 | 3.14/3.17 | Traced; account migration distinguished from device import |
| 27 | 3.14–3.17 and 7.7 | Traced; executable field/identity/conflict cases not finalized |
| 28 | 3.15/3.17/3.21 | Traced; durable import receipt and classified cleanup required |
| 29 | 3.1/3.2 and 7.7/7.9–7.11 | Traced; signed physical-device AD-5 gate remains open |

No unassigned active FR was found. V4/V5 concern acceptance precision within assigned scope, not new requirements. FR-22–24 have mobile acceptance owners and explicit external delivery ownership; the separate CMS/backend plan cannot be used as evidence that these capabilities are complete.

| NFR | Principal evidence owners |
| --- | --- |
| 1 | Feature lifecycle/stability stories; 1.12/2.10/7.6/7.10 |
| 2 | 1.3/1.12/2.10/7.6; agreed device-class budgets |
| 3 | 1.3/1.12/2.10/7.6; network/cache p50/p95 contexts |
| 4 | 1.5/1.7/2.8, migration stories, 7.7/7.11 |
| 5 | Feature admission/account isolation, 7.1–7.4/7.8/7.9 |
| 6 | Feature real-device checks, 3.1/3.2/7.5–7.7/7.9 |
| 7 | Feature visual checks and 7.5 candidate consolidation |
| 8 | Early feature diagnostics and 7.1–7.4/7.6 |
| 9 | 1.11/2.9 transfer continuation and safe restart; 1.12/2.10/3.21/5.3/5.5 cache/integrity/retention |

NFR-9 has explicit interrupted-transfer checks, separately from playback-position recovery. No offline manager or whole-story offline guarantee is introduced. OQ-2/OQ-10 must finalize the documented distinction between initial-exposure evidence and expansion observation; 99.5% remains the per-platform target.

| UX-DR | Feature acceptance owners (7.5 also consolidates retained UX-DR1–25) |
| --- | --- |
| 1–4 | 1.1/1.2/1.8/1.9/1.10 and 2.1/2.7, as applicable |
| 5–6 | 3.3 and provider entry 3.6/3.7; distinct membership onboarding 3.16 |
| 7–9 | 3.4–3.10 |
| 10–12 | 1.1/1.2/1.3/1.8/1.9 and 2.3/2.7 |
| 13–14 | 1.3–1.7/1.11/1.12; completion 1.5; takeover 2.6 |
| 15–16 | 2.1–2.7/2.10 |
| 17–20 | 3.8/3.11–3.13/3.18–3.20 and 4.1–4.3/4.6 |
| 21 | 1.5 premium hook and 6.1–6.7 |
| 22–25 | Relevant UI stories, shared feature acceptance checks and 7.5; real-device proof remains required |

The historical contract contains excluded parent/interactive behavior and older locale choices. Current scope overrides are explicit; those source statements do not restore excluded features. Mockups, Android screenshots and emulated tablet dimensions are not iOS/physical-tablet or successful-offering evidence.

## 2. Architecture implementation validation

There is no mandated starter template to clone. The spine requires evolving app/mobile with bootstrap introduced by its first consumer. Story 1.1 correctly owns the initial shell/toolchain prerequisite; forcing a separate template story would contradict that direction.

No story bulk-creates future tables/entities or authorizes cross-module internal references. Backend work stays behind separately verified contracts. AD-1–12 Rule paragraphs are identical to the current mobile spine: feature-first layers, BLoC/Cubit, auto_route, constructor-injected get_it/injectable composition, one app-scoped coordinator, backend admission, durable migration and revision isolation are retained. This is planned architecture compliance, not an audit of implemented Flutter code.

## 3. Story quality validation

All 72 stories have a delivery status, user statement, requirements, prerequisites and Given/When/Then criteria. No unresolved template placeholder or duplicate story ID was found. Outcomes generally cover success, failure/retry, stale work and relevant data/access boundaries.

No demonstrated need to split an approved story was established from document size alone. Migration/conversion/deletion stories (3.14–3.17/3.20), publication integrations and platform-wide evidence stories require resolved contracts before a single-agent execution boundary can be certified. 7.5/7.6 are evidence aggregation tasks; execute their agreed matrix in recorded work units while preserving the parent story's criteria. Avoid silently treating a mobile integration story as implementation of its entire external backend service.

V4/V5 require acceptance clarification. V1 prevents declaring all stories ready. Approved product outcomes in migration stories explicitly acknowledge that field-level executable criteria remain unfinished; that is a truthful planning boundary, but does not pass the workflow's readiness check.

## 4. Epic structure and file churn

Each epic has a user outcome: discovery/reading, listening, identity/data continuity, support, candidate testing, premium access and a verified/recoverable release. E7 includes explicit release evidence/operations work rather than pretending every item is a new UI feature.

Shared transport, router, DI, coordinator, cache and diagnostics will be extended across epics. The document justifies this overlap through incremental media-first feedback, later production identity integration and one shared media owner. No unsupported repeated-rebuild pattern or reason to consolidate the seven approved epics was found. Actual file churn cannot be measured before implementation.

## 5. Dependencies and open findings

Explicit required story prerequisites point backward. The structural scan's 3.3 → 3.16 reference is a statement that alternate onboarding is delivered later, not a required predecessor. Similarly, 1.9 accepts grouped presentation/reading destinations and assigns later listening destination acceptance to 2.7; 3.12 accepts existing account fixtures and leaves actual imported-data verification to migration stories. These limited acceptance boundaries are explicit and must remain so in implementation and release reporting.

E1/E2 can demonstrate their stated controlled-user domains without E3 application auth/migration implementation, after real backend contracts and authorized test access are verified. Full purchase journeys finish in E6, and full release acceptance in E7. These are incremental, independently useful slices, not a claim that E1/E2 deliver the complete replacement on their own. V3 remains an unresolved possible forward dependency for gated E4 actions.

### V1 — Implementation readiness cannot pass (blocking gate)

Evidence: epics-mobile.md Shared readiness checks (before Story 1.1), external dependency register, every story's Delivery status, and migration planning review. All ten EXT records still lack verified closure and named-assignee/work/contract/evidence links. The compatible generator/toolchain set and safe controlled access are not verified. AD-5 has only partial Simulator evidence; physical signed Android/iOS updates, registered sessions and populated retained records remain open. Migration/deletion and quantitative performance criteria are also not executable until their relevant OQs close.

Action: close prerequisites per consuming story, recording actual assignees, contracts, fixtures and outcomes. Do not require every later release gate before 1.1, and do not require AD-5 for controlled E1/E2 work. Conversely, approval of scope cannot substitute for any applicable prerequisite. No story was promoted to ready by this review.

### V2 — Canonical source reconciliation remains unfinished (P2)

Evidence: source prd.md FR-9 still presents TR/EN/DE and conditionally PT; its product-goal section still leaves framework choice open. epics-mobile.md explicitly supersedes these with TR/EN/PT and ADR-0018 Flutter. The approved iOS parental gate is recorded in Epic 6 but absent from the PRD/addendum product handoff.

Action: synchronize current product requirements and architecture-selection cross-references, preserving original historical evidence. Keep gate trigger/challenge/reset details open until verified; do not claim a store-policy requirement from the legacy behavior decision. Current epics have a clear precedence rule, so this is source drift rather than evidence that their chosen languages/framework are wrong.

### V3 — Resolve parental-gate ownership before affected Epic 4 acceptance (P2)

Evidence: Story 6.2 prerequisites and Epic 6 review require mapping gate coverage across purchase/restore/management and external links from Epic 4. Stories 4.4–4.6 contain announcement/link/store destinations but no applicable parental-gate prerequisite or criterion. Exact gated E4 actions remain unknown.

Action: characterize the legacy entry-point matrix before accepting any affected E4 action. If a destination requires the gate, introduce shared gate behavior with that first consuming story and let 6.2 extend it for premium journeys, or explicitly defer that affected action's acceptance to its assigned later integration. This review does not assume that every link is gated or reorder the approved epics. An unresolved conditional dependency cannot be declared passed.

### V4 — Add an explicit per-language STORY free-access acceptance case (P2)

Evidence: FR-10 requires a CMS language-level free selection to apply to both reading and audio-story experiences. Current listing/start criteria check each supplied backend access state; 6.1 checks subscriber category hiding. They do not directly assert that changing the CMS free selection for one STORY/language produces the intended paired reading/listening outcomes without changing another language.

Action: assign the coupled real-backend fixture/check to an existing consumer/integration story, for example 2.7 with final subscriber regression in E6. Include equivalent anonymous/registered non-subscribers, both STORY experiences and another language; verify locks/start decisions against the authoritative rule. This clarifies FR-10 and does not add a new story or product behavior.

### V5 — Remote language configuration failure lacks a defined acceptance outcome (P2)

Evidence: 1.10/5.4 specify valid test/live visibility and stale-response rejection. Historical contract extraction requires defined safe outcomes for invalid/unavailable configuration. Current language criteria do not specify unavailable configuration, malformed configuration or invalid audience data; the fallback contract remains open.

Action: resolve the safe configuration/fallback contract before 1.10 and add its unavailable/invalid/retry cases there, with 5.4 audience regression. Keep TR/EN/PT decisions intact and prove test-only languages/candidates cannot leak. Do not invent a persistence/TTL/default-language rule during validation.

## 6. Save and completion decision

The review result and report link are saved in epics-mobile.md. Story IDs, requirements and scope approvals are retained. This report recommends corrections; it does not silently amend their criteria or source decisions.

Step 4 remains open. Revalidate V2–V5 after document/acceptance clarification and V1 against actual per-story closure evidence. Until the required checks pass, the skill's “All validations complete! [C] Complete Workflow” success menu is not applicable. A later completion selection must not turn missing evidence into development readiness.
