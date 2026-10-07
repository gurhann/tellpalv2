# Story 1.2 reference and state matrix

Date: 2026-10-07. Status: planned expected cases, not executed visual acceptance. The human approved using the existing Library visual language for loading, recoverable errors and unavailable detail when legacy captures are absent. [Prerequisite evidence](story-1-2-prerequisite-check.md) records the inspected images and their limits.

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
