# Story 1.1 Review Triage — Iteration 2

Date: 2026-10-05. Three fresh context-free review layers completed in the separate review chat `01a10a82-84f2-7293-b8e8-654fc8149b40`. All reviewed source matches the snapshot. No reviewer changed source or ran tests. Raw layers returned nine entries; overlapping catalog-refresh and CMS revision claims are represented below without dropping the distinct error/revision triggers.

| ID | Finding and evidence | Severity | Route |
| --- | --- | --- | --- |
| R1 | LibraryCubit automatic signed-cover refresh invokes foreground load, discards the successful catalog and turns refresh error/timeout/empty into a screen error/empty. | high | bad_spec |
| R2 | LibraryCard retry evicts a completed disk file but retains the same cache/provider key; flutter_cache_manager can reuse the timed-out in-flight transfer for the new image attempt. | high | bad_spec |
| R3 | CategoryHomeRoute renders query.error instead of an editor even when prior data and a local draft exist, losing edits on background GET failure. | high | bad_spec |
| R4 | CategoryHomeRoute keys the editor by live query revision; a successful background/reconnect refetch with a newer revision destroys the local draft. | high | bad_spec |
| R5 | LibraryCard changes selected asset at the 600dp boundary but retains the old ArtworkCubit identity; new asset callbacks/retry are rejected. | medium | bad_spec |
| V1 | Repository/DTO invalid-response guards lack real repository transport tests for mismatched language, duplicate row IDs and preview-limit overflow. | medium | patch |
| V2 | CMS audience/content-language controls lack user interaction plus persisted-field/reload assertions; row-order tests cannot detect these fields being dropped. | medium | patch |
| V3 | PostgreSQL eligibility-before-limit coverage never crosses the 100-candidate boundary, so an early-stop regression could omit a later eligible row. | medium | patch |

No intent gap or new product decision is identified. The frozen intent already requires catalog preservation, current-attempt isolation, recoverable drafts and responsive phone/tablet artwork. The non-frozen implementation plan did not specify ownership of automatic refresh, active transfers, CMS editing snapshots and selected image providers precisely enough. Re-derive the three affected source files after scoped rollback; preserve all unrelated working implementation and user changes. Verification patches survive this loopback.

## KEEP

Preserve the feature-first Clean Architecture, typed Either repository, Freezed/Cubit state, auto_route, get_it/injectable interfaces; legacy tokens, fonts, glyphs, goldens and 48dp disabled semantics; exact language/default-five/server audience rules; catalog/context/image attempt and disposal rejection; real immutable bounded JPEG manifests and LISTING_COVER guarded worker lifecycle; account-scoped 80-object/10-minute disk and 48-entry/32-MiB decoded cache limits; sanitized diagnostics and in-memory-only development credentials; original main.dart media PoC and its existing tests. Preserve all backend/CMS APIs, V33/V34, fixtures and source files outside the three scoped rollback paths. No redesign, production mutation, signing change, search/detail/paywall/auth/migration expansion or new module edges.

Scoped pre-rollback files are backed up under `/private/tmp/tellpal-library-review-loop2/` at their original repository-relative paths. Read these only as preservation references, not implementations to restore blindly.

Seven generated probe/build metadata files found outside the old review packet are inventory observations, not application defects. Do not commit generated caches or change unrelated snapshots to conceal raster drift.
