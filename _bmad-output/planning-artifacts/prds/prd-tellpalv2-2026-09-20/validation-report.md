# Validation Report — TellPal Mobile v1 Yeniden Yazım

- **PRD:** `/Users/gurhankucuk/Documents/GitHub/tellpalv2/_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/prd.md`
- **Rubric:** `.agents/skills/bmad-prd/assets/prd-validation-checklist.md`
- **Run at:** 2026-09-25T20:18:26Z
- **Grade:** Fair
- **Mode:** Validate only; PRD and addendum unchanged. Document review, not runtime or backend verification.

## Overall verdict

The draft has a coherent replacement thesis, explicit exclusions, strong migration invariants, and unusually honest evidence boundaries; it is suitable input for architecture discovery, not an unconditional implementation or release contract. Its principal weakness is that several parity-based requirements still lack a closed, approved acceptance baseline, while performance thresholds and open-item closure are not yet separated into clear architecture, implementation, and release gates. No finding below requires redesigning the product or choosing Flutter/native in the PRD.

The focused reviews add early feasibility gates for signed in-place upgrades and shared identity/entitlement coexistence, plus tighter release-measurement timing. No current reviewer found a blocker to starting architecture investigation. Four high, five medium and three low findings are recorded (12 reviewer findings, not 12 independent defects); the two measurement findings overlap. No critical finding was identified. The Fair grade describes acceptance completeness, not a rejection of the replacement strategy.

## Recommended next gate

- **Architecture may start now.** This validation does not finalize the PRD or authorize implementation.
- **Before architecture is ratified / foundational stories:** prove representative in-place upgrade and identity/entitlement compatibility. Keep RevenueCat implementation late as already agreed.
- **Before affected implementation acceptance:** approve the finite parity baseline; settle user-visible migration/media edge outcomes; clarify lifecycle cases and approve provisional performance budgets.
- **Before qualification and limited rollout:** freeze measurement definitions, sample/window sufficiency and separate initial-release from expansion gates; then collect the required evidence.
- Product-visible choices remain for the owner. Reviewer proposals are not accepted decisions. The appropriate follow-up is a scoped PRD Update, not renewed product discovery.

## Dimension verdicts

### Decision-readiness — adequate

The decisions are explicit: preserve the familiar product, replace the implementation, retain data, permit mixed old/new devices, and avoid continuous synchronization (`prd.md:18–24`, `103–118`). The statement that time constraints do not imply rushed engineering is useful and actionable (`22`). Technology choice is correctly deferred (`24`; `addendum.md:7–9`). Search scope is honestly unresolved rather than decided from an endpoint name (`187–188`).

The open-item table already gives accountable roles and revisit points (`314–324`). However, the last sentence makes *all* open questions prerequisites for final status, including operational matters legitimately scheduled later. That makes the handoff state unnecessarily ambiguous, although it does not prevent beginning architecture investigation.

### Substance over theater — strong

The draft avoids novelty claims and invented market positioning. The reported “yaklaşık %85” and image/RAM explanation are clearly identified as unverified evidence and a hypothesis (`prd.md:48–50`). Reliability concerns are specific to this product: repeated reader sessions, low-memory devices, image readability, playback lifecycle, and update data integrity (`52–61`). The future-offline foundation is bounded against a first-release offline UI commitment (`290,296`).

The journeys are explicitly source-derived rather than claimed customer research (`128`). Repetition between narrative decisions and FRs is somewhat long, but it preserves high-risk invariants rather than adding empty template furniture. No additional finding is warranted for length alone.

### Strategic coherence — strong

The thesis is clear: a store-update replacement that preserves familiar capabilities while improving backend fit and reliability (`prd.md:18–24,277–278`). Feature exclusions are explicit (`296`), and the migration cutoff resolves the apparent tension between “preserve everything” and unsynchronized old devices (`107–109`). RevenueCat and event logging are late implementation work, not silently postponed product scope (`65–70`).

Success criteria test replacement, safe update, stability, and media performance (`300–303`). The counter-metrics prohibit reducing functionality, hiding failing devices, and discarding migration data to make results look better (`304–306`). These metrics serve the thesis rather than measuring unrelated growth or activity. The publication/testing capabilities are justified by the content-delivery dependency, not an unexplained expansion into a new product.

### Done-ness clarity — thin

Many FRs have observable consequences: canceled authentication must not become a successful session (`163`), repeated events must not double-count completion (`225`), failed processing must leave live content intact (`257`), and migration must not expose a blank profile as normal success (`272`). Every active FR has at least one testable consequence; FR-17 is explicitly withdrawn, not missing.

However, testing a subset of a compound FR does not establish full replacement. Several capabilities still point to the reference application or an inventory that must be verified, without fixing the complete behavior and expected outcomes. The draft admits this limitation (`328–330`), making it a blocker for complete implementation acceptance rather than a reason to halt architecture discovery.

### Scope honesty — adequate

The draft explicitly disclaims implementation approval (`prd.md:10`), distinguishes historical observations from current evidence (`14`), and refuses to assume backend readiness (`12,328`). Scope removal is honest: interactive stories and parent guides are excluded, but their historical records cannot silently be deleted (`87,230,296`). Mixed-version divergence, no automatic account merging, and no first-release offline manager are explicit rather than hidden compromises.

There are nine grouped OQ rows (OQ-1–4 and OQ-6–10), zero literal `[ASSUMPTION]` callouts, and zero `[NOTE FOR PM]` callouts. This is acceptable for a draft feeding investigation, not for a green-light-to-build contract. One genuine provisional UI inference is described in prose but lacks the workflow's required assumption indexing.

### Downstream usability — adequate

The glossary defines reference version, replacement, experience, tester, revision, migration, cache, manifest, and parity (`prd.md:138–149`). FR IDs are stable and unique, the withdrawn FR-17 is explained, UJ references resolve, and SM references correctly exclude it (`153,282,300`). Addendum content separates architectural direction and investigation parameters from observed product requirements (`addendum.md:21–43`).

The journeys use actors by role, not named protagonists. This is a small rubric mismatch rather than evidence of an unclear target market; the source-derived nature of the journeys is transparent, and inventing customer research to satisfy a template would make the PRD worse.

### Shape fit — strong

The structure fits a launch-stakes, brownfield consumer replacement: source priority, platform constraints, data continuity, bounded journeys, grouped FRs, quality requirements, non-goals, release gates, and owned open questions. It does not force a new market/differentiation narrative or a redesign brief onto the work. The distinction between replacement behavior and new migration/test-publication requirements is apparent (`prd.md:20,128–136`).

Code/runtime evidence is not claimed where only documentation exists (`prd.md:12–14`; `addendum.md:39,43–45`). This review therefore treats external parity and backend suitability as pending verification, not proven inaccuracies in the source references. A full UX redesign phase is not required to improve this PRD.

## Findings by severity

### Critical (0)

None.

### High (4)

#### [Rubric / Done-ness clarity] The closed parity acceptance baseline is still missing

(§ FR-4, FR-13, FR-18, FR-20, SM-1, `prd.md:171,214,233,241,300,328–330`) — Phrases such as “referans sürümün profil kurulum alanları,” “tamamlanma davranışları korunur,” “geri bildirim gönderimi, doğrulama ve mevcut sınırlar,” and “envanterden doğrulanarak korunur” defer significant behavior to future discovery. SM-1 requires *every* reference function to be covered, but the PRD does not yet identify an approved, finite baseline whose items can each be accepted or explicitly excepted. *Fix:* bind a versioned companion acceptance inventory to `1.0.35+333`, including relevant screenshots, entry/exit conditions, validation/error outcomes, platform differences, and accepted exceptions. Keep detail in the companion rather than expanding every FR. Complete the affected baseline before implementation stories are committed; architecture investigation can proceed now.

#### [Parity / migration] PM-1 — in-place update feasibility is gated too late for a framework decision

**Evidence:** `prd.md:277–278`, FR-29, requires the selected technology to preserve platform identity, signing, session and local-data access, but says this is verified “yayın öncesi”. `prd.md:23` leaves Flutter/native open. `addendum.md`, “Mobile technology choice,” appropriately includes migration risk among selection criteria.

**Risk:** A late release-only check permits selecting and implementing a framework before proving that it can read the old installation's retained data and credentials. Server-side migration alone cannot establish local anonymous-data recovery. A blocked real upgrade would invalidate the central replacement requirement even if fresh installs work.

**Suggested fix:** Add an early architecture exit condition: a representative signed in-place upgrade proof on both platforms, including access to old persisted anonymous and registered-user records and the existing platform identity. This is a limited feasibility proof, not the full release test suite or a prescribed framework. Keep the comprehensive real-device/update matrix as a release gate.

**Owner / phase gate:** Mobile architect + QA; **before stories**, and before ratifying the framework/platform architecture. No need to block architecture investigation itself.

#### [Parity / migration] PM-2 — shared identity and paid-access coexistence must be designed before late feature integration

**Evidence:** `prd.md:320` schedules OQ-6's shared-Firebase isolation/no-remigration proof only “hesap silme uygulanmadan önce”; `prd.md:321` puts OQ-7's RevenueCat identifiers/contracts before the final integrations. `app/old/docs/backend-services.md:60–62` documents Auth UID as a common key for legacy profile and RevenueCat identity and distinguishes local anonymous mode from Firebase anonymous auth. FR-5 and FR-19 require old-device isolation and subscriber continuity; the addendum explicitly warns that deleting a shared Firebase identity cannot simply be presumed harmless to legacy clients.

**Risk:** The individual open questions are present, but the schedule allows account migration/authentication design to solidify before verifying a mutually compatible mapping among legacy identity, new account lifecycle and subscription entitlement. Discovering the incompatibility while implementing deletion or purchases would require reworking foundations and could jeopardize old-client access or new-system deletion semantics.

**Suggested fix:** Add a cross-cutting identity/entitlement architecture dependency covering registered and anonymous migration, conversion, existing-account login, purchase/restore ownership, new-system deletion, re-entry and simultaneous old-client use. Require evidence that the accepted product outcomes can coexist; escalate only a demonstrated impossibility to the product owner. Keep RevenueCat implementation in its approved late position, but prove identity compatibility early. Do not assume a particular UID-remapping or transfer policy.

**Owner / phase gate:** Backend/mobile architect; **before stories** that implement authentication, account migration or subscription identity. End-to-end paid-access and deletion/coexistence acceptance remains **before release**.

#### [Reliability / media] R-REL-1 — freeze the measurement contract before collecting gate evidence

**Evidence:** Line 61: “Platform bazında crash-free kullanıcı oranı yayın değerlendirmesinde en az %99,5 olmalıdır.” The same paragraph explicitly leaves definition, window and sample open. Line 302 requires a sufficient measurement window/sample and separate examination of hangs and memory terminations. Line 308 conditions rollout expansion on sufficient usage and the 99.5% target. OQ-2, line 317, assigns a measurement plan to architecture but thresholds/evidence only to “yayın öncesi”. NFR-3, line 284, also leaves device/network-conditioned p50/p95 targets to be agreed.

**Why it matters:** The target itself is clear and must not be reopened. However, there is not yet a reproducible pass/fail rule for either the initial limited release or subsequent expansion. A small zero-crash cohort, mixed release versions, or a tool that misses memory terminations could yield a misleading pass. Late selection of a window, sample or latency budget after observing outcomes also weakens the acceptance gate. Existing counter-metrics correctly prohibit concealing failed devices; they do not yet define how evidence qualifies.

**Recommended resolution:** Complete the existing OQ-2 with a version/platform-scoped measurement definition, eligible-user denominator, collection coverage, minimum observation/sample sufficiency, treatment of insufficient evidence, and an explicit relationship between the preproduction device gate and the production expansion gate. Define how memory terminations/hangs are independently checked. Approve the device/network/content matrix and latency/memory acceptance budgets before collecting the evidence used for those gates. Do not invent numeric budgets in this review or use the reported 85% as a verified baseline.

**Owner / timing gate:** Mobile + QA + product owner. Architecture may begin now. Freeze the measurement plan and test budgets before acceptance qualification; freeze the initial-release/expansion protocol before limited rollout. Actual proof remains a release/expansion gate, not a prerequisite to architecture.

### Medium (5)

#### [Rubric / Decision-readiness] Separate PRD finalization from downstream gate closure

(§ Open points / Evidence limits, `prd.md:317–324,330`) — “Açık noktalar kapanmadan belge `final` durumuna alınmayacaktır” treats URL TTL, cache budgets, release sample size, and the legacy shutdown decision like unresolved product scope. The same table schedules these for architecture or rollout, creating an unclear completion condition. *Fix:* classify each open item as a product-decision prerequisite, architecture/design deliverable, implementation prerequisite, or release/operations gate; allow explicitly owned downstream work to remain open at PRD sign-off. This is handoff clarification, not a request to select technical mechanisms now.

#### [Rubric / Done-ness clarity] Performance and measurement thresholds need an earlier decision gate

(§ NFR-1–3 / OQ-2 / SM-3–4, `prd.md:61,282–284,302–303,317`) — “p50/p95 hedefleri kararlaştırılır” and “yayın öncesi eşikler” acknowledge missing bounds but allow selecting budgets very late. Memory stability, startup latency, and sample/window definitions are central success criteria; deciding them after implementation can make acceptance circular. *Fix:* require architecture/measurement work to propose representative device/network/content conditions and provisional budgets before affected implementation acceptance criteria are approved; finalize the production observation window/sample and release thresholds before rollout. Preserve the accepted 99.5% target and do not treat the reported 85% as a verified baseline.

#### [Parity / migration] PM-3 — acknowledged migration edge policies still prevent a complete acceptance oracle

**Evidence:** `prd.md:318`, OQ-3 explicitly leaves unique local records on a later-upgraded device and removed-content history presentation open. `prd.md:285`, NFR-4, requires field/relationship validation; `prd.md:300–301`, SM-1/SM-2, require no undecided functional loss and zero lost retained data. The coexistence section correctly limits account migration to the first transition and does not authorize ongoing synchronization.

**Risk:** This is a deliberately open product/acceptance decision, not a contradiction. Until disposition and presentation are chosen, QA cannot determine whether retaining a unique local record outside normal history satisfies the user-visible preservation requirement, nor how unavailable/removed content should appear. Counting imported records is explicitly insufficient.

**Suggested fix:** Close OQ-3 with concrete examples: an already-migrated account whose second device has unique retained local data; history referencing removed interactive/parent content; and a retained history entry whose content identity cannot be mapped. For each, record intended preservation, visible outcome and retry/failure acceptance. Separate pre-transition data from intentionally unimported later legacy activity. Any solution must preserve the accepted no-repeat-account-migration rule; do not silently introduce synchronization or deletion.

**Owner / phase gate:** Product owner + mobile/backend; **before stories** for migration/history. May be resolved during architecture discovery with product confirmation when the outcome is user-visible.

#### [Reliability / media] R-REL-2 — separate protected-media mechanism choices from user-visible expiry/revocation policy

**Evidence:** FR-25, line 262: “aktif oynatma kabul edilen revizyona bağlı kalır” followed by “Süresi dolan medya erişimi, yayından çekme ve güvenlik istisnaları OQ-8'de tamamlanacaktır.” Line 264 requires short-lived URLs and backend authorization. OQ-8, line 322, groups URL lifetime/renewal and withdrawal with manifest, cache and processing parameters under backend/mobile architecture. Discovery line 46 also explicitly makes admitted-session continuity subject to final withdrawal/security rules.

**Why it matters:** TTL values, refresh transport and retention mechanisms are valid architecture deferrals. Whether an already-playing session continues, pauses recoverably, or stops when an entitlement expires or a revision is withdrawn is also an observable product/security decision. Without distinguishing routine URL expiry from loss of authorization or urgent withdrawal, valid implementations can differ in session continuity and premium access behavior while claiming compliance. This is an acknowledged open policy, not an existing contradiction or proven vulnerability.

**Recommended resolution:** Have architecture propose a small outcome table for routine URL expiry with authorization retained, transient renewal failure, authorization lost, ordinary publication rollback/withdrawal, and urgent security withdrawal. Ratify the visible stop/retry/resume and position-preservation outcomes with the product owner. Keep protocol, TTL, retry counts and storage implementation in architecture. Preserve the existing rule that public metadata/cache possession alone never grants access.

**Owner / timing gate:** Backend/mobile architecture with product owner for visible outcomes. No blocker to starting architecture; close before protected delivery/player acceptance stories are finalized (the existing OQ-8 epic gate).

#### [Reliability / media] R-REL-3 — make paused-state and process-death lifecycle acceptance explicit

**Evidence:** FR-14, line 217: “Çağrı geldiğinde veya kulaklık çıkarıldığında oynatma duraklar; kullanıcı kendi eylemiyle yeniden başlatır.” FR-16, line 225: “Uygulama tamamen kapatılıp yeniden açıldığında ana ekrandan açılır” and “Uygulama arka planda kaldığında açık hikâye veya oynatma oturumu kaldığı yerden devam eder.” NFR-6, line 287, requires real-device background behavior validation.

**Why it matters:** The accepted cold-start/home rule and user-action-only interruption recovery are sound. The generic background continuation sentence can nevertheless be read as automatic resume even when the session was paused by the user or an interruption. It also lacks an explicit test classification for an operating-system-terminated process that the user never manually closed—important in a low-memory reliability project. This is wording/test precision, not a request to reopen the established lifecycle decision.

**Recommended resolution:** Add derived acceptance cases distinguishing an alive background session (preserve position and play/pause state), return after user/interruption pause (do not auto-resume), and process death followed by launch (apply the cold-start/home rule). Check that the chosen interpretation matches the accepted decision; only ask the owner if actual legacy evidence exposes a conflict. Add call/headphone removal while backgrounded and foreground-return cases to the device test plan. No framework or OS implementation mechanism belongs in the PRD.

**Owner / timing gate:** Mobile + QA; product owner only for a genuine behavioral conflict. Architecture may begin. Resolve before lifecycle acceptance stories/tests are approved.

### Low (3)

#### [Rubric / Scope honesty] Make the provisional crop assumption extractable

(§ FR-15 / Evidence limits, `prd.md:222,326–330`; `addendum.md:39`) — “Merkezden kırpma geçici varsayımdır” is appropriately qualified but is not tagged/indexed. A downstream reader extracting only FR-15 could lose its unresolved status when consolidating assumptions. *Fix:* give the centered-crop assumption a stable identifier, mark it inline and include it in a compact assumptions index with the representative-device/text-safe-area validation condition. Do not convert it into an approved product decision.

#### [Rubric / Downstream usability] Journey actors are not named or consistently state-scoped

(§ UJ-1–7, `prd.md:128–136`) — Labels such as “Mevcut kullanıcının güncellemesi” and “Hesap ve destek işlemleri” do not carry a named protagonist and often omit account/subscription/device state. Migration and entitlement acceptance depend on these states. *Fix:* when confirming the existing journeys, add minimal illustrative named actors and only relevant state (for example, an existing subscriber updating a phone while a tablet stays old); explicitly retain their illustrative/source-derived status. No persona workshop or new redesign journey is needed.

#### [Parity / migration] PM-4 — linked historical extracts can still be mistaken for current requirements

**Evidence:** `extract-contracts.md:20,22–23,55,57,74` still describes general search, interactive/parent capability preservation and open numeric reliability gates as requirements. `extract-legacy-flows.md` also contains historical branch/parent test needs. The current PRD's source-priority section, exclusions and addendum correctly supersede those statements, so this is not a current PRD scope defect.

**Risk:** A downstream worker opening an extraction directly may schedule deliberately removed features or reopen already accepted decisions. The existing date/authority notes mitigate but do not fully prevent this.

**Suggested fix:** Preserve historical evidence; add a prominent superseded-status note with explicit pointers to the current search, excluded-feature, anonymous conversion and reliability decisions. Do not rewrite the old inventory to pretend those legacy capabilities never existed.

**Owner / phase gate:** Document maintainer; **before stories** consume these extracts. Safe editorial follow-up subject to update authorization.

## Mechanical notes

- Finding count: **0 critical, 1 high, 2 medium, 2 low**. No current finding blocks beginning architecture investigation; the high finding blocks calling the replacement acceptance baseline implementation-ready.
- Active FRs total 28, with FR-17 retained as an explicitly withdrawn ID. UJ-1–7, SM-1–4, SM-C1–3, and NFR-1–9 are unique; checked cross-references resolve.
- OQ-5 is absent from the current open-item table. Stable numbering need not be repaired by renumbering; a short resolved/retired note would make the gap intentional to downstream readers.
- The assumptions section explains evidence limits well, but the centered-crop assumption has no literal tag/index roundtrip; see the low finding above.
- UJs have role labels but no named protagonists; see the low finding above.
- `prd.md:93` promises detailed migration acceptance criteria “PRD'nin devamında.” NFR-4 and SM-2 provide invariant-level acceptance, while field mappings and fixtures remain future work. Clarify that detailed fixtures belong to the downstream migration plan, not an already completed later section.
- The append-only memlog includes superseded intermediate statements (for example, open crash targets and event scope), followed by later accepted decisions. Current PRD text correctly reflects the later decisions; historical log entries should not be rewritten.
- Review is against the current draft only; historical `review-draft.md` findings were not imported as current findings.

## Reviewer context and strengths

### Functional parity and migration safety

Source: `review-parity-migration.md`

Date: 2026-09-25
Mode: Validate only; no product decisions or PRD changes made.

#### Verdict

**Conditional pass for architecture discovery; not ready for implementation stories.** The PRD faithfully preserves the accepted replacement, one-time migration and dual-system rules. No critical finding or blocker to starting architecture investigation was found. Two high findings concern when essential feasibility evidence must be obtained, not a request to reopen accepted product decisions. Counts: 0 critical, 2 high, 1 medium, 1 low.

#### Source extraction orientation

- Read current `prd.md`, `addendum.md`, `.memlog.md`, both legacy extraction reports and `be/docs/mobile-delivery-discovery.md`. Checked legacy `app/old/docs/backend-services.md` and `data-models.md` for identity and local-data evidence. Source-code inventory is not live runtime proof.
- Later user decisions correctly override discovery: RevenueCat and flow logging are first-release requirements implemented late; search scope remains payload-dependent; interactive stories and parent guides are excluded.
- Preserved decisions include reference build `1.0.35+333`, minimum platform versions, existing visible controls, cold-start home/background-session continuity, locale gating and fallback, anonymous-to-new-account preservation and no automatic merge into an existing account.
- Migration correctly distinguishes first successful account transition, safe retry and later device-local conversion. Old clients remain on the old backend; subsequent old-system activity is intentionally not synchronized. This is **not** a defect or data-loss finding.
- Old downloaded media/cache cleanup is explicitly separated from retained profile/history/progress and migration inputs. Historical records for removed content are not authorized for deletion. New-system-only deletion and no resurrection through remigration are recorded, rather than assumed solved.
- No test execution, current backend capability proof, actual subscription transaction, signing verification or device migration was performed in this review.

#### Findings

- **Before architecture:** No newly discovered product blocker. Architecture may investigate the current, explicit requirements without deciding open user-visible behavior on the user's behalf.
- **Before stories:** Prove in-place upgrade feasibility and identity/entitlement coexistence; resolve the OQ-3 acceptance cases and already recorded parity/search evidence gaps. Keep known open items visible rather than claiming full readiness.
- **Before release:** Execute full real-device parity, migration/retry/cleanup, subscriber continuity, account deletion/no-resurrection and old/new-client coexistence tests. Runtime evidence is still outstanding, as the PRD already acknowledges.

### Reliability and media acceptance

Source: `review-reliability.md`

Date: 2026-09-25

Intent: Validate only. The PRD and its accepted decisions were not changed.


**Suitable for architecture exploration, with explicit acceptance dependencies; not yet a release-ready acceptance contract.** No critical finding or reliability-specific blocker to starting architecture was identified. Findings: **1 high (release-measurement gate), 2 medium (product acceptance details to close with architecture), 0 low**. These are refinements of acknowledged open items, not evidence that the accepted replacement scope is wrong.

Sources reviewed: `prd.md`, `addendum.md`, `.memlog.md`, `reconcile-discovery.md`, and `be/docs/mobile-delivery-discovery.md`. Evidence locations below use current `prd.md` line numbers unless another file is named. This is document validation, not a runtime, crash-log, device, entitlement, or backend audit.



#### Confirmed strengths and non-findings

- The roughly 85% crash-free report and high-resolution-image/RAM explanation are explicitly unverified (lines 48–50). The addendum correctly treats rendition size, resident pages, prefetch and cache limits as investigation candidates. No framework choice or image root cause is justified by these documents alone.
- At least 99.5% crash-free users per platform is consistently approved; test-device memory termination and critical playback faults block release. Crash instrumentation is not postponed with business-flow logging (lines 61, 70, 244, 282, 289).
- First-page readiness, bounded preparation on cellular and Wi-Fi, no complete-story speculative download, no detail autoplay, and preservation of animation/readability are explicit (FR-11–15). Exact dimensions, codecs, manifest schema and cache budgets are legitimate design work, not missing PRD implementation detail.
- Race prevention for page/audio changes and competing players is explicit (lines 214, 217). Background audio, system controls, and user-action-only recovery after call/headphone interruptions are present.
- Actual generated-media availability/integrity—not merely processing plans—is required. Failed candidates cannot replace live revisions; testers and subscriptions remain separate; cleanup protects required revisions (FR-22–25).
- Counter-metrics prohibit attaining quality by deleting functionality, sacrificing media quality, concealing bad devices or losing migration data (lines 304–306).
- A full offline manager is explicitly excluded; it must not be smuggled into reliability remediation. Future-compatible asset identity/integrity remains an accepted foundation.


#### Suggested disposition

Defer R-REL-1 and R-REL-2 to their existing OQ-2/OQ-8 owners with the tighter timing and product-signoff conditions above. R-REL-3 is a candidate for a narrow clarification after user authorization to edit. None authorizes changing the accepted threshold, platform floor, framework freedom, rollout strategy, or replacement scope. No evidence supports reopening the whole discovery process.


## Historical review

review-draft.md (2026-09-20) was read as historical context only. It predates the reconciled PRD; its removed-feature, already-resolved behavior and open-threshold statements are not carried forward as current requirements or counted findings.

## Reviewer files

- `review-rubric.md`
- `review-parity-migration.md`
- `review-reliability.md`
- `review-draft.md` — historical, excluded from current severity counts.

