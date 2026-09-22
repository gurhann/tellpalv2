# Draft PRD consistency and coverage review

Date: 2026-09-20. Scope: current `prd.md`, `addendum.md`, `.memlog.md`, `extract-legacy-flows.md`, and `extract-contracts.md`. This is a document-level draft review, not final approval, backend verification, live-app testing or legal/store-policy assessment. Open matters are collected here for the final discussion, as requested.

## Overall finding

The draft preserves the explicit conversation decisions: full replacement with accepted discovery exceptions; technology choice remains open; supported operating-system/device floor; reference 1.0.35+333; improved reliability without treating the reported 85% as verified; late but pre-release RevenueCat and flow logging; one-time account migration with coexistence and no subsequent synchronization; local anonymous transition; discardable media/cache separated from preserved records; blocking, retryable migration UX. No direct reversal of those decisions was found. The addendum is consistent with the PRD and correctly treats image-memory causes and framework remedies as unproven.

## Findings to resolve in the draft or final discussion

| Priority | Location | Gap and supporting evidence | Recommended disposition |
|---|---|---|---|
| Medium | FR-18 / OQ-9 | App/store rating prompts and the completion-screen rating action have no explicit requirement or named open item. Both appear in the legacy component extraction. General parity language covers them implicitly but gives no reviewable acceptance target. | Add rating prompts/completion actions to the support/completion inventory and evidence checklist; determine actual trigger and result from the reference. This does not require a new product decision merely to retain them. |
| Medium | FR-2, FR-4 / OQ-9 | Registration notice acceptance, separate marketing consent and field validation were observed, but authentication requirements omit these visible behaviors. Preserving old marketing data does not specify new registration behavior. | Explicitly include reference registration validation/notice/marketing-preference behavior; leave current wording and platform obligations for the existing verification item rather than asserting compliance. |
| Medium | FR-14 / OQ-5 | Player parity is stated generically. Lullaby looping versus meditation/audio-story non-looping, seek, explicit exit and displayed duration/credits are not individually mapped. The extraction distinguishes observed controls from source-only loop policy. | Add a compact player acceptance inventory with evidence status. Keep interruption/headphone/system-control decisions open as already planned. |
| Medium | SM-1, source priority, evidence boundaries | The draft references the legacy inventory broadly but does not point to the sibling extraction matrices or preserve per-capability evidence status within an acceptance checklist. It could be mistaken that all 29 FR groups have the same evidence strength. | Link the two extraction files as supporting inputs and use their observed/partial/documented statuses for downstream acceptance tracking. Explicitly retain social auth, billing, branching, populated history and iOS runtime as unverified evidence needs. |
| Medium | FR-3 / UJ-2 / preserved-data principle | New-account anonymous conversion promises profile/language retention; existing-account sign-in has an explicit merge restriction. The fate of newly accumulated anonymous history/progress on conversion into a new registered account is not expressly stated, despite general preservation language and FR-16. | Clarify that new-account identity conversion preserves applicable anonymous user records, or collect the remaining behavior as an explicit open issue. Do not invent a merge into an existing account. |
| Low | FR-6, FR-13 / SM-1 | Similar-content recommendations, completion recommendations and metadata/credits are only indirectly included by reference parity. Extraction documents these user-visible affordances; editor selections likewise appear in the contract extraction. | Name these in catalog/detail/completion acceptance coverage so implementation cannot accidentally drop them under broad FR wording. |
| Low | FR-18 / OQ-9 | Remote configuration is mentioned through individual features, but invalid/missing operational configuration fallback is not named. Legacy controls include daily free guides, announcements, onboarding and paywall decisions. | Add configuration-unavailable acceptance coverage for the affected existing functions; do not require retaining the old SDK or keys. |

## Boundaries correctly left open

- Numerical quality targets, sample size, measurement window and representative device/network budgets are pending. SM-3 properly prevents an unmeasured claim of readiness; this review does not invent target percentages.
- Shared Firebase identity and account deletion across old/new systems need a decision (OQ-6). One-time data migration does not settle entitlement refresh or account-deletion semantics.
- Local-only records from a later-upgraded device remain distinct from rerunning account migration (OQ-3); no background synchronization should be inferred.
- Branch-aware delivery, parent-guide backend support, revision/access renewal, effective locales, fallback, resume and interruption behavior remain explicit design or reference-evidence needs.
- Existing SDK inventory is not automatic authorization to carry every SDK into the new application.

## Draft readiness

The document is suitable for continued requirements refinement and an end-of-session open-point review. It is not ready to be labeled final or used as evidence that the new backend, stores, billing, migration or crash targets are already verified. The above gaps are mostly acceptance specificity and traceability omissions, not a need to reopen the user's settled replacement scope.
