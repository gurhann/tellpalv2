# Story 1.2 reference and state matrix

Current verification decision (2026-10-08): see [development acceptance](library-1-2-development-acceptance.md). Current implementation, complete-diff review patches and native/reference/platform accessibility gates are verified; the spec is done and the mobile tracker is review for human handoff. Earlier dated checkpoints below retain their historical scope and do not override this decision.

Date:2026-10-07. Current213 no-skip Flutter tests and analyzer pass. Human-approved iOS16MiB/Android8MiB native screenshot envelopes are implemented. All20 required current native targets pass under current90-file source sealce5446d70fbd2cf4d841bcc93e69bf0f55749bd0496f40d505610d2a77e30b73 with strictly unchanged selected compiled closures/original source/installed-binary/fixture proof. Qualified explicit cover recoveries remain distinguished from uninterrupted healthy baselines; original Android tablet subscriber A28 timeout remains preserved and unexplained. Four settled fault replays retain24cases/51original captures each. Separate actual404 reference targets each pass six TR/EN/PT1.0/1.3 cases on iPhone/iPad; actual enlarged three-language original PNGs inspected on both profiles. Earlier transitional failure rasters remain historical. Current subscriber metadata references actually inspected on all four profiles. Normal-entry Android phone/tablet platformXML keeps exact unchanged73-file production/assets/pubspec compiled closures. Actual iOS platformAX/manual Mac unlock, card/heading beyondviewport, final BMad review and approved release gaps remain open; Story1.2 stays in progress. Publish-time cover/content optimization is recorded separately for planning after Story1.2. [Prerequisite evidence](story-1-2-prerequisite-check.md) records original reference limits; [additional settled unavailable evidence](library-1-2-settled-unavailable-evidence.json) records the separate reference target scope.

## Source and matrix

Use [free TR phone](../../../app/old/docs/design-system/evidence/emulator-haberci-info.png), [locked phone](../../../app/old/docs/design-system/evidence/emulator-premium-story.png) and [free EN tablet](../../../app/old/docs/design-system/evidence/tablet-emulated-story-info.png), with [SCREENS.md](../../../app/old/docs/design-system/SCREENS.md) and [TABLET.md](../../../app/old/docs/design-system/TABLET.md). The locked capture has a blank hero and mixed-language text; it is not successful cover or exact-language parity proof.

Reuse 411×914dp phone and 800×1280dp portrait tablet, TR/EN/PT UI, scales 1.0/1.3 and both controlled audiences. Include ordinary same-language and clearly labeled mapped-language entry. Match actual content, account/access, dimensions and language for direct legacy comparison; otherwise label the result as a newly agreed state. Add the available Android/iOS native phone/tablet profiles and record OS, build, source identity, device pixels/density, cache and network. Virtual evidence does not certify physical devices or unavailable OS versions.

## Expected cases

| Case | Expected surface and behavior |
| --- | --- |
| Loaded free / locked | Large clipped hero, circular back, age/duration pills, centered gradient CTA, narration preference, summary and ordered credits. Correct server lock/access; CTA cannot start a later story's reader/paywall. |
| Phone / tablet | Preserve the documented hero ratios and approximately 343dp centered CTA on tablet; do not uniformly enlarge the phone. Metadata and credits remain readable/reachable. |
| Loading | Existing Library canvas, tokens, loading text/progress and reachable back. No prior story appears as the new selection. |
| Network/timeout/5xx | Existing Library typography and localized error with explicit retry/back. One bounded current-context request; stale attempts have no effect. |
| Unavailable | Localized “story unavailable” message and back in the same visual language. No alternative-language substitution or stale summary. |
| Hero absent/corrupt/expired | Preserve hero geometry and metadata; artwork error/retry stays separate from detail failure. Renewal targets the same selected identity. |
| Populated / absent credits | Reference role/name presentation; missing values/roles are omitted without broken placeholders. Labels use UI language, names use backend values. |
| Valid / empty recommendations | Horizontal real cover cards in filtered CMS order; empty area is omitted. Selecting and returning restores the originating detail and its scroll/preference. |
| Rapid/repeated actions | One route for each repeated action; deliberate later selection remains possible. Old responses/images cannot replace the active identity. |
| Back to scrolled Library | Same vertical and row scroll, active context and retained catalog. No new Library fetch/state owner solely due to back. |
| Long text / enlarged scale | TR/EN/PT titles/summary/credits remain readable with reachable controls, safe areas and deliberate wrapping; full titles remain in semantics. |

## Acceptance evidence

For each case save actual reference/candidate state, language/content/access and device metadata, images, differences and observed result. Loaded captured states support legacy comparison; loading/error/unavailable and absent recommendations are contract-derived expected states, not invented legacy screenshots. No case is marked passed by this plan.

Use platform accessibility inspection for back/retry/card/CTA/narration roles, localized names and enabled/disabled/selected values. Require at least 48dp touch targets, normal-text contrast of 4.5:1, large-text and meaningful icon contrast of 3:1 over actual artwork, and safe-area/control reachability. Record contrast methodology and measurements. Failed checks block that screen's acceptance; missing minimum/older-OS or physical evidence remains explicit.

Keep Story 1.1's approved reference images and golden evidence intact. Add distinct Story 1.2 evidence rather than relabeling previous Library results. Quantitative memory/latency acceptance must use the epic's approved first-working-flow measurement process; bounded hero decoding alone is not performance proof.

The runtime Library route is enabled following the 76-check controlled HTTP prerequisite. `app/mobile/integration_test/story_detail_live_test.dart` now prepares distinct real card/recommendation/back/scroll and decoded-image captures for available native profiles. Its source alone does not mark any visual case passed; consolidate only successfully executed, inspected native outputs into the Story 1.2 evidence. Existing approved home captures and golden files are unchanged.

## Inspected A4 phone states

The historical A4 source-sealed Android API 36 phone target passed 16 journeys and the real unavailable case at 411×914dp (822×1828 pixels, DPR 2). [Native evidence](library-1-2-native-evidence.json) records all 49 acknowledged image hashes and exact build/source/dataset identities. Inspected private PNGs: `detail-tr-1.0-free-2-loaded`, `detail-pt-1.3-locked-3-loaded` and `detail-pt-1.3-locked-3-metadata`.

The TR free candidate uses the verified “Küçük Balık Poli” fixture, while the legacy reference depicts “Haberci Nota”; this is a new content state using the approved detail layout, not matched-content legacy parity. The half-height clipped hero, bottom metadata pills, centered approximately 343dp gradient CTA, narration panel and role icons preserve the reference structure. Candidate summary/credit values come from the actual backend; long fixture names wrap without overlap. Opaque token-colored back/pill surfaces and current Library typography/gradient differ from the legacy raster and are recorded rather than treated as pixel equality. The enlarged PT locked candidate has the server lock presentation, no launched reader/paywall, reachable narration preference and wrapped summary/ordered localized contributor roles.

These observations qualify the inspected states only. The 82 retained home files measured after A4 exceed the accepted 80-file bound and require a source repair/rerun; phone/tablet audience/fault/empty-area and platform AX gates remain open. Keep private PNGs outside the repository and retain the existing home goldens unchanged.


## Inspected A6 phone states

Android API 36 phone NON_SUBSCRIBER A6 passed all 16 detail journeys, the unavailable case and 49 acknowledged screenshots. Its exact source/build/dataset identity is recorded in [native evidence](library-1-2-native-evidence.json). The source aggregate is `211e015585b88a692759a77d2b8f8bff93d2c37dd1adf2d8ac236c54d15d8f2a`; this is the source before the subsequently diagnosed artwork-renewal phase repair. No cache reset was used. Read-only post-run disk/SQLite inspection found 68 home files/rows (2,061,760 bytes), 12 detail files/rows (480,094 bytes), no orphan/missing/duplicate paths, matching known lengths and 11 byte-identical pre-run home files. Vendor-created immutable metadata may omit its optional length; disk bounds use actual measured file sizes. This snapshot is not a transient peak measurement.

The inspected free TR/Portuguese loaded states show real verified fixture art, the half-height hero, opaque back/pill surfaces, centered gradient CTA and localized narration/summary/credit presentation. At Portuguese scale 1.3, role labels and long contributor names use stacked credit rows and remain readable. The Portuguese artwork is an existing Portuguese story fixture with aligned fixture metadata; it is not matched-content Haberci legacy parity. The inspected metadata raster captures recommendation cards in an initial placeholder frame, so it does not close the loaded-recommendation-cover reference case. Obtain actual decoded recommendation covers and record that state in the coordinated current-source rerun. Platform AX, remaining profiles/audiences/faults and natural-provider recovery remain open.

| Inspected private capture | SHA-256 |
| --- | --- |
| `detail-tr-1.0-free-2-loaded.png` | `6159312dd197b4d2223f55bfe66e68ec90d9d78e3af325393d5406e50eda721c` |
| `detail-pt-1.0-free-2-loaded.png` | `2628f51a7ea3ac94396b3e759e4d35c12c5e9d22557c96d50693dbac5f6fa3a2` |
| `detail-pt-1.3-locked-3-metadata.png` | `11af49ba82ab1dc84a660b7fd47aec2920f4cf5d946c1124e7f19e0593bf794c` |

Current source follow-up: A10 failed the first TR recommendation wait after one loaded-hero capture and zero completed journeys; the failed run is not accepted reference evidence. The subsequent timer-reader defect is proven by two actual SQLite/IO/HTTP/PNG red-to-green tests, but exact original missing-file provenance remains unproven. Current210-test source/codegen/analysis verification is complete; A11 retained-disk native rerun and platform AX/reference inspection remain pending.
