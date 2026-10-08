# Story 1.2 development acceptance

Date: 2026-10-08. Implementation and complete-diff review patches are verified within the approved emulator-first scope. The spec is done; the mobile tracker is review for human handoff. Earlier checkpoints in linked artifacts retain their historical scope.

## Verified behavior

Library cards open exact selected-story detail with separate content/UI language, server-controlled admission/access, localized credits/summary/recommendations and retained parent state on Back. Cover preparation, immutable-cache publication, disposal, retries and resource bounds have regression coverage. Reader and actual paywall actions remain deferred with truthful disabled semantics.

## Current evidence

| Gate | Result | Evidence |
| --- | --- | --- |
| Flutter | 223 default-parallel and223serial, no skips; analyzer/generation pass;101source/fiveSVG hashes match | [Build](library-1-2-flutter-build-evidence.json) |
| Frozen acceptance matrix | All9rows executed; approved intent unchanged | [Matrix](library-1-2-matrix-audit.json) |
| Backend | Configured Maven385Surefire+1Failsafe pass, including real PostgreSQL recommendation pagination beyond100rejected candidates | [Backend](library-1-2-review-backend-verification.json) |
| Native original matrix | 20current targets/550originalPNGs, four profiles and both audiences | [Native](library-1-2-native-evidence.json) |
| Actual platform controls | iOS24loaded/18failure and Android24loaded/18failure; six additional Android AX-only snapshots | [Accessibility](library-1-2-platform-ax-evidence.json) |
| Current reference inspection | Original raster/contrast observations plus separate12settled real404 cases/12PNGs, six enlarged raster inspections | [Reference](library-1-2-reference-inspection-evidence.json), [404](library-1-2-settled-unavailable-evidence.json) |
| Complete-diff review | Five findings patched; ten Flutter regressions and backend pagination coverage pass | [Review](library-1-2-review.md), [Regressions](library-1-2-review-regression-evidence.json) |

Native90source seal: `cb6f2759ac2e1f32493d7a67ed65772ebb6822eb869c0a19a6b5dcd77daced04`. Historical46passing runs/1387PNGs remain separately identified in the66-run/1937-PNG main inventory. Additional AX-only and404reference targets are outside the original20count. The historical76HTTP matrix keeps its original jar identity; recorded production-source compatibility and current native jar execution are separately documented.

## Qualifications and follow-up

Android tablet NON_SUBSCRIBER baseline includes two distinct real-control cover recoveries; SUBSCRIBER includes three. These are qualified successful recoveries; exact source-timeout causes are unproven. Earlier failed parallel suites, iOS8MiB capture failures, host AX failures and shutdown-container audit attempts remain unaccepted historical records. The human-approved iOS16MiB capture envelope is preserved; no selected wait/assertion/capture bound was weakened during review patch verification.

Actual iOS switch and recommendation/nested-Back interactions passed on phone/tablet. Physical CUA viewport scrolling remains tool-limited; original native SDK gestures independently prove scroll/card/Back retention. Controlled503/absent-cover fixtures do not prove provider outages; natural expiry targets separately exercise actual provider expiry/recovery. Final read-only cache audits pass without reset; unknown optional metadata lengths are explicit, not invented. Physical devices, minimum/older OS and production quantitative memory/latency remain release gaps.

Controlled detail EXT-CATALOG integration is complete for this consumer. Later consumers and production release remain open. [Publish-time image optimization](../../planning-artifacts/post-story-1-2-image-optimization.md) is recorded as separate planning: optimize cover and content images on publication using agreed resolution/compression choices; raw assets must not remain the delivery strategy.
