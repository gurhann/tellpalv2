## Deferred from: code review of spec-1-1-playback-isleme-hedeflerini-guvenilir-bicimde-ayirma (2026-09-06)

- `AssetProcessingPathBuilder` does not reject separator or traversal-like characters in `externalKey`; this predates Story 1.1 and is deferred for a dedicated storage-path hardening task.
