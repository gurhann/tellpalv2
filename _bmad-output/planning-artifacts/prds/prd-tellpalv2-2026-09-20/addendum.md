# TellPal Mobile v1 PRD Addendum

## Downstream Architecture Decisions

### Mobile technology choice

Flutter is not a fixed requirement for the rewrite. Flutter and native iOS/Android development may be compared during architecture work against the product requirements, migration risk, delivery time, crash reduction, media/playback reliability, maintainability, testing, and operational cost.

The PRD must define externally observable capabilities and parity expectations without prematurely selecting the implementation technology.

### Image memory and stability investigation

The user reports an approximately 85% crash-free rate and suspects high-resolution images cause excessive RAM usage. Neither the metric definition nor the root cause has been verified. Inspect crash and termination evidence alongside device memory measurements before attributing failures to image resolution or selecting a framework as the remedy.

Evaluate image rendition sizing, decoded-image dimensions, concurrently resident pages, bounded prefetch, cache budgets, and resource release during repeated navigation. These are investigation candidates, not selected implementation mechanisms. Include low-memory supported devices and the heaviest content samples; preserve visual readability. The accepted crash-free user target is at least 99.5% per platform; the measurement window/sample and numerical memory/latency budgets remain open. Memory-related termination or critical playback failures on test devices block release.

## Discovery reconciliation — 2026-09-22

Source: `be/docs/mobile-delivery-discovery.md`, dated 2026-09-19. The following preserves its architectural direction for downstream work; it does not select endpoint schemas or establish that the current backend implements it. Later accepted user decisions in the PRD take precedence.

### Backend ownership and boundaries

The discovery does not require a separate top-level mobile business module. Mobile HTTP adapters stay close to owning business modules; cross-domain composition uses application APIs, events, or stable IDs. Validate the final contract against Spring Modulith boundaries before implementation. Architecture work must consult the canonical repository architecture, project memory and applicable ADRs before ratifying this direction against current code.

Firebase Auth is the authentication authority. A verified Firebase UID maps to the backend's canonical application user/profile. Preserve the accepted new-system-only account deletion behavior without assuming that deleting a shared Firebase identity can leave the legacy application unaffected. Identity treatment, subscription consequences and prevention of remigration after deletion require design evidence (OQ-6).

### Delivery foundation and parameters still to select

The first-release direction is a revisioned manifest and separate per-page image/audio assets. It is not constrained by the first-three-pages/remainder ZIP scheme. ZIP packaging is a later option only if measurements or offline requirements justify it. The exact manifest schema, fetch point (detail/start), adjacent asset batch size, protected URL lifetime/renewal and short-term backend caching remain open.

Stable revision/asset identity, completeness/integrity checks and resumable-download support belong in the foundation. Distinguish evictable playback cache from explicitly retained offline content. Cache budgets, eviction and complete-story semantics remain design work; no first-release offline download manager has been approved.

Listing thumbnails, detail/player renditions where needed, page images/audio and animated listening artwork must have actual prepared outputs, not merely registered plans. Select dimensions, codecs, quality budgets, orchestration, idempotency, retries and readiness gates using representative files and runtime evidence. Metadata-only lighter approval is conditional on no generated-media change.

URL generation is not identified by discovery as the principal cost concern; measure storage, download operations and egress rather than treating that statement as a benchmark. Retention and cleanup are backend/environment parameters and must protect live, rollback and admitted-session revisions.

### Supplied story sample — reported evidence, not remeasured

Discovery describes 24 numbered JPG/MP3 page pairs and two Turkish covers. Images are approximately 1712 × 2778; audio is mono MP3 at approximately 192 kbps/48 kHz. Total size is approximately 24 MB and narration approximately 8 minutes 45 seconds; the first three pages are approximately 3.4 MB before packaging. This supports investigating bounded startup; it does not establish optimal packaging or a production RAM/crash cause. Portrait artwork is shared across phone/tablet; centered crop remains provisional pending text-safe-area validation on representative devices.

### Evidence required before implementation stories

Collect reference screenshots for home/category/search/detail/player/auth/paywall/language/profile; screen-to-endpoint mappings; representative request/response/error fixtures with secrets and credentials redacted; free/locked/test/unpublished/fallback cases; and phone/tablet playback/cache observations. Inspect current backend adapters and actual processing outputs rather than treating the discovery's earlier inspection as acceptance evidence. Derive the mobile API contract, authorization matrix, manifest schema and migration/coexistence plan from these inputs.

Removed interactive stories and parent guides remain excluded. Historical extraction reports describing them are source evidence, not active scope requirements. Legacy search types remain unverified: `/story/search` and `StorySearchByCategoryType[]` do not prove story-only results without payload evidence.
