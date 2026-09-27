# Good-spine rubric review — TellPal Mobile Replacement

## Verdict

**Revise before using this initiative spine to split implementation work.** The mechanical linter passes (`total_findings: 0`), and the spine establishes useful mobile boundaries for feature state, playback ownership, migration gating, transport, and store-update compatibility. It does not yet cover several initiative-wide seams that separate teams or epics could implement incompatibly. Most notably, the operational release envelope is nearly silent, and `Deferred` leaves some cross-cutting decisions without a shared invariant or a sufficiently early revisit gate.

This is an independent semantic review of the current draft, not a change to the spine. The PRD itself is draft and has open questions; findings below ask the spine to preserve the common boundary or identify an explicit open decision, not to invent a resolved product answer.

## Critical and high findings

### High — Operational rollout and recovery is an omitted dimension

**Evidence:** The spine's AD-9 covers environment injection and development diagnostics (lines 90–94), and the seed states only that CI/release builds preserve app identity and OS minima (line 116). `Deferred` mentions rollout *thresholds* (line 132), but never names the release controller, staged rollout and expansion evidence, production crash/entitlement monitoring, failure containment, app-version/Remote Config coordination, recovery approach, or old-system shutdown/coexistence owner. The PRD's success gates require staged rollout, migration and RevenueCat evidence, and continued old-system operation; OQ-10 explicitly asks for rollback/recovery and shutdown conditions. The legacy release guide also distinguishes binary release from Remote Config activation (`app/old/docs/deployment-guide.md`, sections 2, 5, 8–9).

**Divergence:** A mobile release epic could assume a store-only staged rollout, while backend/operations could switch routing or content globally, leaving no coherent containment or rollback path when migration or entitlement fails. Deferring numerical thresholds is reasonable; leaving the operational *boundary and decision gate* absent is not.

**Disposition:** **Discuss / add invariant or explicit open item.** State which system controls cohort exposure and environment configuration, what must remain available during coexistence, and which evidence gates expansion. Keep numerical thresholds and final shutdown criteria open with an OQ-10 revisit before release planning.

### High — Candidate/test/live media authorization has no governing rule

**Evidence:** The capability map combines reliability and release verification under AD-3/5/8/9 (lines 118–126), but none of those rules fixes the FR-22–25 release boundary. The convention to carry content/language/experience/revision and pin a playback revision (line 103) is helpful, yet there is no invariant that candidate access requires tester authorization independently of subscription, that test/live release scopes are distinct, that a failed candidate cannot displace live media, or that protected media is checked at start/renewal. Exact server authorization is wholly in `Deferred` (line 130). The PRD requires these behaviors in FR-22–25, and the addendum explicitly says the production authorization matrix is still to be derived.

**Divergence:** Catalog, player, backend API, and CMS release work could use different meanings of “available” or “authorized”; one might serve an unapproved candidate through a cached URL while another assumes a live revision. This is a shared capability contract, even if endpoint fields remain deferred.

**Disposition:** **Add a boundary invariant; defer protocol detail.** Bind discovery, admitted playback revision, candidate tester access, protected URL renewal, and failed-candidate isolation to a common publication/access decision. Keep concrete endpoints, manifest schema, and withdrawal exceptions for the contract work, with a gate before those epics.

### High — Account lifecycle is only partially covered

**Evidence:** AD-4 binds registered/anonymous upgrades and second devices (lines 60–64), AD-8 separates cache from user data (lines 84–88), and the identity convention forbids email-based merge (line 102). No rule or `Deferred` item covers the PRD's *distinct* anonymous-to-new-account transfer versus sign-in to an existing account without merge (FR-3), or new-system-only account deletion with a shared Firebase identity and prevention of remigration (FR-5, OQ-6). The PRD treats both as user-visible commitments, while the technical deletion path remains open.

**Divergence:** Authentication, profile, migration, and purchase epics could each choose a different meaning of “same account,” yielding data merge/loss, deletion of legacy identity, or resurrection from the legacy source after deletion.

**Disposition:** **Add an explicit lifecycle invariant and open decision.** Fix the no-automatic-merge and no-remigration-after-deletion constraints; mark the shared-Firebase deletion mechanism and subscription consequences as OQ-6 to resolve before account deletion implementation. Do not imply that Firebase identity itself can safely be deleted yet.

### High — Durable events and user progress can diverge under retry

**Evidence:** AD-8 says the backend owns progress (line 88); AD-9 says event delivery failures must not block playback (line 94). The PRD requires a repeated completion event not to count twice (FR-16) and failed delivery/retry not to duplicate user-flow events (FR-21), while migration has its own idempotence rule in AD-4. The event `Deferred` item names schemas and retention but omits retry identity/idempotence (line 134). The canonical backend architecture already describes client-generated `event_id` as an idempotency key (`architecture.md`, section 8.5), so this is also a missed brownfield convention.

**Divergence:** The reader, player and telemetry epics could emit separate identifiers on retry, or treat progress as both a direct write and an event side effect, causing double completion/duration and mismatched analytics.

**Disposition:** **Add a cross-feature event identity rule.** Require stable logical event identity across retry and one authoritative progress mutation path; defer field schema, batching, transport and retention to the backend contract.

## Medium findings

### Offline foundation is thinner than the accepted contract

The content/media identity convention (line 103) and bounded revision-keyed cache rule (line 88) cover two parts of NFR-9. They do not fix verifiable asset completeness/integrity or resumable transfer, both explicit first-release foundation requirements in the PRD addendum and NFR-9. Leaving cache budgets and timing to measurements is sound, but two builders could choose manifests and cache entries with incompatible completeness semantics. **Disposition:** Add a minimal manifest/cache integrity invariant; defer exact schema and byte-range strategy to OQ-8.

### Support/configuration/deep-link surfaces are not mapped

FR-18 includes feedback, localized legal/help documents, ratings, version display, announcements, notification permission and deep-link behavior, plus operational configuration of free content and paywall triggers. The spine lists FR-18 in frontmatter but has no corresponding capability-map row or owner; AD-6's route graph could accept deep links, but no route/config boundary is stated. OQ-9 is absent from `Deferred`. **Disposition:** Map this capability to a feature/shared boundary and identify OQ-9 as a pre-release compatibility gate. The exact SDK and screen detail can remain below spine altitude.

### Performance diagnosis lacks an architectural ownership point

AD-9 requires crash, playback and memory diagnostics *during development* (line 94), and `Deferred` holds budgets until measurement (line 132). The PRD also requires cold/warm start, first-page/audio and p50/p95 measurement (NFR-3), plus platform-specific crash-free evidence in staged production rollout. There is no common definition of where the app emits timing and memory observations or how release evidence uses them. **Disposition:** Add an instrumentation/release-evidence boundary; keep numeric budgets under OQ-2.

### Named packages are not verified-current in the delivered artifact

AD-3/6/7 name `just_audio`, `audio_service`, `audio_session`, `go_router`, Riverpod and handwritten clients; concrete versions are intentionally deferred (line 133). The memlog records package documentation and a `go_router`/Flutter SDK compatibility caveat, so the selection is not baseless. The spine itself does not cite a current verification result or SDK baseline for each binding, and the current PoC contains only `just_audio` as a direct dependency. The linter's zero findings therefore proves format, not compatibility or current-version fit. **Disposition:** Keep the compatibility gate explicit and record verified versions/OS support before production dependency integration; avoid presenting the present PoC as platform proof.

## Checklist walk

| Criterion | Assessment |
| --- | --- |
| Real divergence points fixed for level below | **Partial.** Feature/state/media/migration ownership is strong; release operations, publication/access, account lifecycle and event identity remain open across epics. |
| Every AD enforceable and prevents its stated divergence | **Mostly.** AD-1/2/4/5/6/7/8 have inspectable rules. AD-3's “implement the PRD” and AD-9's general diagnostic/release clauses are too broad to serve as complete acceptance contracts by themselves. |
| Nothing under Deferred permits incompatible units | **No.** Server authorization/idempotency protocol, local-device migration, and event retry policy can change how multiple epics behave. Details may stay deferred, but the shared invariant and revisit gate should not. |
| Named technology verified-current | **Unproven in spine.** Memlog contains research and a compatibility caveat; package versions and platform fit are pending. No claim here that any named package is obsolete. |
| Brownfield reality ratified | **Mostly.** Flutter PoC, test-only endpoint and legacy app identity are handled honestly. The backend event idempotency convention and release/Remote Config separation are underrepresented. |
| PRD capabilities covered | **Partial.** Main journeys are mapped; FR-3/5, FR-18, FR-22–25, FR-16/21 and NFR-3/9 have material gaps noted above. |
| Inherited parent spine respected | **Not applicable.** No parent spine is declared in frontmatter; the 2026-09-06 spine is not named as inherited. |
| Every initiative dimension decided, deferred or open | **No.** The operational/environmental envelope extends beyond environment injection; rollout, recovery, coexistence/shutdown and production observability are missing or too implicit. |

## Evidence and limits

Sources reviewed: this spine and `.memlog.md`; `reviewer-gate.md`; PRD `prd.md` and `addendum.md`; `architecture.md`; `be/docs/project-memory.md`; `be/docs/mobile-delivery-discovery.md`; `app/mobile/README.md` and `pubspec.yaml`; legacy `app/old/docs/deployment-guide.md`. The deterministic linter completed successfully with no findings. This review did not run the mobile app, verify store/signing access, inspect production provider settings, or independently check current package releases; those remain evidence gates, not assumed failures.
