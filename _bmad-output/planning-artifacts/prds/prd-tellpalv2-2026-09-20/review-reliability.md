# Reliability and Media Acceptance Review

Date: 2026-09-25

Intent: Validate only. The PRD and its accepted decisions were not changed.

## Gate verdict

**Suitable for architecture exploration, with explicit acceptance dependencies; not yet a release-ready acceptance contract.** No critical finding or reliability-specific blocker to starting architecture was identified. Findings: **1 high (release-measurement gate), 2 medium (product acceptance details to close with architecture), 0 low**. These are refinements of acknowledged open items, not evidence that the accepted replacement scope is wrong.

Sources reviewed: `prd.md`, `addendum.md`, `.memlog.md`, `reconcile-discovery.md`, and `be/docs/mobile-delivery-discovery.md`. Evidence locations below use current `prd.md` line numbers unless another file is named. This is document validation, not a runtime, crash-log, device, entitlement, or backend audit.

## R-REL-1 — High: freeze the measurement contract before collecting gate evidence

**Evidence:** Line 61: “Platform bazında crash-free kullanıcı oranı yayın değerlendirmesinde en az %99,5 olmalıdır.” The same paragraph explicitly leaves definition, window and sample open. Line 302 requires a sufficient measurement window/sample and separate examination of hangs and memory terminations. Line 308 conditions rollout expansion on sufficient usage and the 99.5% target. OQ-2, line 317, assigns a measurement plan to architecture but thresholds/evidence only to “yayın öncesi”. NFR-3, line 284, also leaves device/network-conditioned p50/p95 targets to be agreed.

**Why it matters:** The target itself is clear and must not be reopened. However, there is not yet a reproducible pass/fail rule for either the initial limited release or subsequent expansion. A small zero-crash cohort, mixed release versions, or a tool that misses memory terminations could yield a misleading pass. Late selection of a window, sample or latency budget after observing outcomes also weakens the acceptance gate. Existing counter-metrics correctly prohibit concealing failed devices; they do not yet define how evidence qualifies.

**Recommended resolution:** Complete the existing OQ-2 with a version/platform-scoped measurement definition, eligible-user denominator, collection coverage, minimum observation/sample sufficiency, treatment of insufficient evidence, and an explicit relationship between the preproduction device gate and the production expansion gate. Define how memory terminations/hangs are independently checked. Approve the device/network/content matrix and latency/memory acceptance budgets before collecting the evidence used for those gates. Do not invent numeric budgets in this review or use the reported 85% as a verified baseline.

**Owner / timing gate:** Mobile + QA + product owner. Architecture may begin now. Freeze the measurement plan and test budgets before acceptance qualification; freeze the initial-release/expansion protocol before limited rollout. Actual proof remains a release/expansion gate, not a prerequisite to architecture.

## R-REL-2 — Medium: separate protected-media mechanism choices from user-visible expiry/revocation policy

**Evidence:** FR-25, line 262: “aktif oynatma kabul edilen revizyona bağlı kalır” followed by “Süresi dolan medya erişimi, yayından çekme ve güvenlik istisnaları OQ-8'de tamamlanacaktır.” Line 264 requires short-lived URLs and backend authorization. OQ-8, line 322, groups URL lifetime/renewal and withdrawal with manifest, cache and processing parameters under backend/mobile architecture. Discovery line 46 also explicitly makes admitted-session continuity subject to final withdrawal/security rules.

**Why it matters:** TTL values, refresh transport and retention mechanisms are valid architecture deferrals. Whether an already-playing session continues, pauses recoverably, or stops when an entitlement expires or a revision is withdrawn is also an observable product/security decision. Without distinguishing routine URL expiry from loss of authorization or urgent withdrawal, valid implementations can differ in session continuity and premium access behavior while claiming compliance. This is an acknowledged open policy, not an existing contradiction or proven vulnerability.

**Recommended resolution:** Have architecture propose a small outcome table for routine URL expiry with authorization retained, transient renewal failure, authorization lost, ordinary publication rollback/withdrawal, and urgent security withdrawal. Ratify the visible stop/retry/resume and position-preservation outcomes with the product owner. Keep protocol, TTL, retry counts and storage implementation in architecture. Preserve the existing rule that public metadata/cache possession alone never grants access.

**Owner / timing gate:** Backend/mobile architecture with product owner for visible outcomes. No blocker to starting architecture; close before protected delivery/player acceptance stories are finalized (the existing OQ-8 epic gate).

## R-REL-3 — Medium: make paused-state and process-death lifecycle acceptance explicit

**Evidence:** FR-14, line 217: “Çağrı geldiğinde veya kulaklık çıkarıldığında oynatma duraklar; kullanıcı kendi eylemiyle yeniden başlatır.” FR-16, line 225: “Uygulama tamamen kapatılıp yeniden açıldığında ana ekrandan açılır” and “Uygulama arka planda kaldığında açık hikâye veya oynatma oturumu kaldığı yerden devam eder.” NFR-6, line 287, requires real-device background behavior validation.

**Why it matters:** The accepted cold-start/home rule and user-action-only interruption recovery are sound. The generic background continuation sentence can nevertheless be read as automatic resume even when the session was paused by the user or an interruption. It also lacks an explicit test classification for an operating-system-terminated process that the user never manually closed—important in a low-memory reliability project. This is wording/test precision, not a request to reopen the established lifecycle decision.

**Recommended resolution:** Add derived acceptance cases distinguishing an alive background session (preserve position and play/pause state), return after user/interruption pause (do not auto-resume), and process death followed by launch (apply the cold-start/home rule). Check that the chosen interpretation matches the accepted decision; only ask the owner if actual legacy evidence exposes a conflict. Add call/headphone removal while backgrounded and foreground-return cases to the device test plan. No framework or OS implementation mechanism belongs in the PRD.

**Owner / timing gate:** Mobile + QA; product owner only for a genuine behavioral conflict. Architecture may begin. Resolve before lifecycle acceptance stories/tests are approved.

## Confirmed strengths and non-findings

- The roughly 85% crash-free report and high-resolution-image/RAM explanation are explicitly unverified (lines 48–50). The addendum correctly treats rendition size, resident pages, prefetch and cache limits as investigation candidates. No framework choice or image root cause is justified by these documents alone.
- At least 99.5% crash-free users per platform is consistently approved; test-device memory termination and critical playback faults block release. Crash instrumentation is not postponed with business-flow logging (lines 61, 70, 244, 282, 289).
- First-page readiness, bounded preparation on cellular and Wi-Fi, no complete-story speculative download, no detail autoplay, and preservation of animation/readability are explicit (FR-11–15). Exact dimensions, codecs, manifest schema and cache budgets are legitimate design work, not missing PRD implementation detail.
- Race prevention for page/audio changes and competing players is explicit (lines 214, 217). Background audio, system controls, and user-action-only recovery after call/headphone interruptions are present.
- Actual generated-media availability/integrity—not merely processing plans—is required. Failed candidates cannot replace live revisions; testers and subscriptions remain separate; cleanup protects required revisions (FR-22–25).
- Counter-metrics prohibit attaining quality by deleting functionality, sacrificing media quality, concealing bad devices or losing migration data (lines 304–306).
- A full offline manager is explicitly excluded; it must not be smuggled into reliability remediation. Future-compatible asset identity/integrity remains an accepted foundation.

## Suggested disposition

Defer R-REL-1 and R-REL-2 to their existing OQ-2/OQ-8 owners with the tighter timing and product-signoff conditions above. R-REL-3 is a candidate for a narrow clarification after user authorization to edit. None authorizes changing the accepted threshold, platform floor, framework freedom, rollout strategy, or replacement scope. No evidence supports reopening the whole discovery process.
