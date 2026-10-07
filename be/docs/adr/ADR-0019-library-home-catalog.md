# ADR-0019: Library Home Catalog and Verified Listing Covers

## Status

Accepted implementation contract on 2026-10-04 for the user-approved Story 1.1 specification. Runtime acceptance is tracked separately in `library-1-1-evidence.md`; this ADR does not certify acceptance or authorize production deployment.

## Context

The media PoC and aggregate catalog endpoints do not provide independent CMS row order, explicit category language identity, server audience locks or bounded listing artwork. Story 1.1 needs a controlled real backend-to-CMS-to-Flutter home listing while preserving existing browsing and media behavior.

## Decision

- On 2026-10-07, the user designated existing Firebase UID `jLRypTmoBrhhX21HsDJojurcKTC3` as the persistent default for real authenticated development and QA flows. `tools/mobile-test-account.json` records the selection. Verify that this exact account exists and is enabled before obtaining a temporary session; use no replacement identity without an explicit user instruction. Keep tokens and credentials outside the repository. The preference does not change production configuration, account roles/custom claims/subscriptions, synthetic unit fixtures, or the empty-by-default backend test allowlist. Audience variants use isolated server fixture configuration with this same identity.
- `category` owns opt-in home positions, `ALL` / `NON_SUBSCRIBER` audience and nullable explicit content/curation language on each category localization. V33 leaves existing rows opted out. Occupied-position swaps use a complete per-language snapshot, revision precondition, transaction, advisory language lock and deferred uniqueness constraint. Single-row updates reject occupied positions. Home fields are read-only to ORM so existing category-localization edits cannot flush a stale snapshot over the JDBC-owned atomic configuration. Curation order remains in `category_contents` and is never rewritten by home configuration.
- `LibraryHomeQueryApi` composes category rows with `content::api` lightweight scalar previews and `asset::api` ready artwork. Filter active/published categories and exact declared-language, active/published/processed STORY localizations before the single server preview limit, initially five. Empty rows disappear without changing remaining order. Ordinary rows never substitute another language. A deliberately labeled mapped row uses its explicit published category localization/curation lane.
- Extend only the approved public module edge `user` → `category::api`. The local/test-only `/api/test/library` entry resolves real Firebase identity before the catalog read-only transaction. Its empty-by-default UID allowlist and subscriber-fixture subset remain server configuration. Listing locks do not authorize protected media. No production test controller, client premium selector or bearer credential UI is introduced.
- `LISTING_COVER` is separate from delivery/narration/page-media processing. V34 captures source and profile and stores a ready manifest only after both real image variants are uploaded, reread and verified. JPEG quality defaults to 85; maximum bounds are phone 360×480 and tablet 600×800, with aspect ratio preserved, no upscale and transparent pixels composed over white. Source limits default to 20 MiB and 40 million pixels; all profile values are configurable and included in immutable revision identity.
- Completion and failure both require the captured processing ID, attempt count, unexpired lease, source and profile under a pessimistic job lock. Ready publication and completion share one transaction. Stale worker callbacks, including rejected-publication catch callbacks, cannot mutate a replacement attempt. Query code signs existing ready assets and never starts preparation or loads full playback data.
- CMS `/categories/home` uses existing page/form/button primitives to configure each presentation language independently. The exact surface supports opt-in/out, adjacent occupied-position exchange, audience/mapping selection, atomic save, conflict feedback and explicit reload.
- Flutter keeps `main.dart` and its PoC tests. `main_library_dev.dart` is a fail-closed controlled entry with ephemeral debugger/integration harness configuration. The Library uses feature-first data/domain/presentation, pure Dart entities/repository contracts, generated DTOs, use case, Freezed Cubits, `auto_route`, and `get_it`/`injectable`. A dedicated app-owned transport and bounded account-scoped image cache dispose with composition; route-owned Cubits reject obsolete context/attempt callbacks. Disabled deferred controls preserve their reference appearance and semantics.
- Recorded catalog/artwork waits are finite: 15 seconds and 10 seconds. The 2026-10-04 real local Firebase fixture measured catalogs at 967–2019 ms and individual artwork transfers at 463–2332 ms before the final CMS baseline refresh. These values provide recovery thresholds for this controlled fixture rather than production latency guarantees. Tests deliberately exceed configured waits and reverse obsolete callbacks. Recalibrate against production evidence before a production transport is admitted.

## Consequences

Home configuration and listing preparation are explicit administration work. Preparation failures leave no half-ready manifest; unready/missing covers remain a card-level placeholder rather than suppressing an otherwise eligible preview. Generated-only asset registry rows are rejected. Failed variant uploads may leave isolated unreferenced objects; cleanup is operational work and does not make a revision ready.

The test audience is not a production entitlement contract. Detail, category pagination, search, authentication UX, protected playback, migration and release remain separate stories. Reference checks and unavailable native OS profiles remain acceptance gates, documented with their actual evidence.

## Alternatives Considered

- Aggregate content hydration: rejected for listing because it reads full assets and playback data.
- Automatic English padding: rejected by the approved selected-language-only contract.
- Per-category preview limits: rejected in favor of one server default.
- Reusing delivery plans as thumbnails: rejected because planned registry rows do not establish real decodable bytes.
- Unguarded target/kind failure callbacks: rejected because an expired worker could fail its replacement.

## Related Files or Modules

- `architecture.md`, `be/docs/project-memory.md`, ADR-0001/0002/0018
- `AGENTS.md`, `tools/mobile-test-account.json`, `tools/library-1-2/README.md`
- `category`, `content`, `asset`, `user`, `cms/src/features/categories/`, `app/mobile/`
- `_bmad-output/implementation-artifacts/mobile/library-home-contract.md`
- `_bmad-output/implementation-artifacts/mobile/library-1-1-evidence.md`

## Supersedes / Superseded By

No previous ADR is superseded. Existing endpoints and legacy processing kinds retain their contracts.
