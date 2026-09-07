## Deferred from: code review of spec-1-1-playback-isleme-hedeflerini-guvenilir-bicimde-ayirma (2026-09-06)

- `AssetProcessingPathBuilder` does not reject separator or traversal-like characters in `externalKey`; this predates Story 1.1 and is deferred for a dedicated storage-path hardening task.

## Deferred from: code review of spec-1-3-ortak-textless-kapak-sahipligi (2026-09-07)

- `planning-artifacts/epics.md` and the historical architecture spine still describe `textlessCoverMediaId` as the shared audio/lullaby/meditation cover. This predates Story 1.3; the canonical epic context, spec, ADR-0010, and project memory now carry the corrected ownership split.
