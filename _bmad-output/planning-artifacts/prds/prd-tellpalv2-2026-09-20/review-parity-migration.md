# Functional parity and migration safety review

Date: 2026-09-25
Mode: Validate only; no product decisions or PRD changes made.

## Verdict

**Conditional pass for architecture discovery; not ready for implementation stories.** The PRD faithfully preserves the accepted replacement, one-time migration and dual-system rules. No critical finding or blocker to starting architecture investigation was found. Two high findings concern when essential feasibility evidence must be obtained, not a request to reopen accepted product decisions. Counts: 0 critical, 2 high, 1 medium, 1 low.

## Source extraction orientation

- Read current `prd.md`, `addendum.md`, `.memlog.md`, both legacy extraction reports and `be/docs/mobile-delivery-discovery.md`. Checked legacy `app/old/docs/backend-services.md` and `data-models.md` for identity and local-data evidence. Source-code inventory is not live runtime proof.
- Later user decisions correctly override discovery: RevenueCat and flow logging are first-release requirements implemented late; search scope remains payload-dependent; interactive stories and parent guides are excluded.
- Preserved decisions include reference build `1.0.35+333`, minimum platform versions, existing visible controls, cold-start home/background-session continuity, locale gating and fallback, anonymous-to-new-account preservation and no automatic merge into an existing account.
- Migration correctly distinguishes first successful account transition, safe retry and later device-local conversion. Old clients remain on the old backend; subsequent old-system activity is intentionally not synchronized. This is **not** a defect or data-loss finding.
- Old downloaded media/cache cleanup is explicitly separated from retained profile/history/progress and migration inputs. Historical records for removed content are not authorized for deletion. New-system-only deletion and no resurrection through remigration are recorded, rather than assumed solved.
- No test execution, current backend capability proof, actual subscription transaction, signing verification or device migration was performed in this review.

## Findings

### PM-1 — High: in-place update feasibility is gated too late for a framework decision

**Evidence:** `prd.md:277–278`, FR-29, requires the selected technology to preserve platform identity, signing, session and local-data access, but says this is verified “yayın öncesi”. `prd.md:23` leaves Flutter/native open. `addendum.md`, “Mobile technology choice,” appropriately includes migration risk among selection criteria.

**Risk:** A late release-only check permits selecting and implementing a framework before proving that it can read the old installation's retained data and credentials. Server-side migration alone cannot establish local anonymous-data recovery. A blocked real upgrade would invalidate the central replacement requirement even if fresh installs work.

**Suggested fix:** Add an early architecture exit condition: a representative signed in-place upgrade proof on both platforms, including access to old persisted anonymous and registered-user records and the existing platform identity. This is a limited feasibility proof, not the full release test suite or a prescribed framework. Keep the comprehensive real-device/update matrix as a release gate.

**Owner / phase gate:** Mobile architect + QA; **before stories**, and before ratifying the framework/platform architecture. No need to block architecture investigation itself.

### PM-2 — High: shared identity and paid-access coexistence must be designed before late feature integration

**Evidence:** `prd.md:320` schedules OQ-6's shared-Firebase isolation/no-remigration proof only “hesap silme uygulanmadan önce”; `prd.md:321` puts OQ-7's RevenueCat identifiers/contracts before the final integrations. `app/old/docs/backend-services.md:60–62` documents Auth UID as a common key for legacy profile and RevenueCat identity and distinguishes local anonymous mode from Firebase anonymous auth. FR-5 and FR-19 require old-device isolation and subscriber continuity; the addendum explicitly warns that deleting a shared Firebase identity cannot simply be presumed harmless to legacy clients.

**Risk:** The individual open questions are present, but the schedule allows account migration/authentication design to solidify before verifying a mutually compatible mapping among legacy identity, new account lifecycle and subscription entitlement. Discovering the incompatibility while implementing deletion or purchases would require reworking foundations and could jeopardize old-client access or new-system deletion semantics.

**Suggested fix:** Add a cross-cutting identity/entitlement architecture dependency covering registered and anonymous migration, conversion, existing-account login, purchase/restore ownership, new-system deletion, re-entry and simultaneous old-client use. Require evidence that the accepted product outcomes can coexist; escalate only a demonstrated impossibility to the product owner. Keep RevenueCat implementation in its approved late position, but prove identity compatibility early. Do not assume a particular UID-remapping or transfer policy.

**Owner / phase gate:** Backend/mobile architect; **before stories** that implement authentication, account migration or subscription identity. End-to-end paid-access and deletion/coexistence acceptance remains **before release**.

### PM-3 — Medium: acknowledged migration edge policies still prevent a complete acceptance oracle

**Evidence:** `prd.md:318`, OQ-3 explicitly leaves unique local records on a later-upgraded device and removed-content history presentation open. `prd.md:285`, NFR-4, requires field/relationship validation; `prd.md:300–301`, SM-1/SM-2, require no undecided functional loss and zero lost retained data. The coexistence section correctly limits account migration to the first transition and does not authorize ongoing synchronization.

**Risk:** This is a deliberately open product/acceptance decision, not a contradiction. Until disposition and presentation are chosen, QA cannot determine whether retaining a unique local record outside normal history satisfies the user-visible preservation requirement, nor how unavailable/removed content should appear. Counting imported records is explicitly insufficient.

**Suggested fix:** Close OQ-3 with concrete examples: an already-migrated account whose second device has unique retained local data; history referencing removed interactive/parent content; and a retained history entry whose content identity cannot be mapped. For each, record intended preservation, visible outcome and retry/failure acceptance. Separate pre-transition data from intentionally unimported later legacy activity. Any solution must preserve the accepted no-repeat-account-migration rule; do not silently introduce synchronization or deletion.

**Owner / phase gate:** Product owner + mobile/backend; **before stories** for migration/history. May be resolved during architecture discovery with product confirmation when the outcome is user-visible.

### PM-4 — Low: linked historical extracts can still be mistaken for current requirements

**Evidence:** `extract-contracts.md:20,22–23,55,57,74` still describes general search, interactive/parent capability preservation and open numeric reliability gates as requirements. `extract-legacy-flows.md` also contains historical branch/parent test needs. The current PRD's source-priority section, exclusions and addendum correctly supersede those statements, so this is not a current PRD scope defect.

**Risk:** A downstream worker opening an extraction directly may schedule deliberately removed features or reopen already accepted decisions. The existing date/authority notes mitigate but do not fully prevent this.

**Suggested fix:** Preserve historical evidence; add a prominent superseded-status note with explicit pointers to the current search, excluded-feature, anonymous conversion and reliability decisions. Do not rewrite the old inventory to pretend those legacy capabilities never existed.

**Owner / phase gate:** Document maintainer; **before stories** consume these extracts. Safe editorial follow-up subject to update authorization.

## Gate summary

- **Before architecture:** No newly discovered product blocker. Architecture may investigate the current, explicit requirements without deciding open user-visible behavior on the user's behalf.
- **Before stories:** Prove in-place upgrade feasibility and identity/entitlement coexistence; resolve the OQ-3 acceptance cases and already recorded parity/search evidence gaps. Keep known open items visible rather than claiming full readiness.
- **Before release:** Execute full real-device parity, migration/retry/cleanup, subscriber continuity, account deletion/no-resurrection and old/new-client coexistence tests. Runtime evidence is still outstanding, as the PRD already acknowledges.
