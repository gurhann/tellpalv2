## Deferred from: code review of spec-1-1-playback-isleme-hedeflerini-guvenilir-bicimde-ayirma (2026-09-06)

- `AssetProcessingPathBuilder` does not reject separator or traversal-like characters in `externalKey`; this predates Story 1.1 and is deferred for a dedicated storage-path hardening task.

## Deferred from: code review of spec-1-3-ortak-textless-kapak-sahipligi (2026-09-07)

- `planning-artifacts/epics.md` and the historical architecture spine still describe `textlessCoverMediaId` as the shared audio/lullaby/meditation cover. This predates Story 1.3; the canonical epic context, spec, ADR-0010, and project memory now carry the corrected ownership split.

## Deferred from: code review of spec-1-3-ortak-textless-kapak-sahipligi (2026-09-07)

- The full PUT metadata flow can overwrite a newer cover edited in another tab because the form submits a previously read source/listening-cover snapshot. This is pre-existing whole-record update behavior and needs a separate optimistic-locking or PATCH decision.
- The generic asset picker allows the parent metadata submit while a direct upload is still pending. This is pre-existing upload UX behavior and needs a dedicated coordination task.
- `_bmad-output/specs/spec-shared-textless-cover/SPEC.md` still describes one shared textless cover for audio stories, meditation, and lullabies. It is historical planning material; superseding or removing stale ownership documents is deferred.

## Deferred from: code review of spec-1-4-ninninin-ortak-playbackini-ve-baslik-only-localizationini (2026-09-07)

- Public/mobile registry and asset-bundle consumers still resolve localization-scoped processing. Story 1.4 intentionally limits the change to the admin content contract; mobile/public endpoint migration is a later roadmap item.
- The existing non-STORY processing worker requires an image listening cover, while Story 1.3 keeps that cover optional. Supporting coverless shared playback requires a separate asset-processing decision and is deferred without changing the new playback ownership model.
