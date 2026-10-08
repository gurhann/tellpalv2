---
artifact_type: execution-handoff
story_status: review
story_id: '1.1'
story_key: 1-1-measure-current-image-slots-and-source-coverage
phase: P0a.1
created: 2026-10-08
epic_file: _bmad-output/planning-artifacts/image-optimization/epics.md
planning_artifacts: _bmad-output/planning-artifacts/image-optimization
implementation_artifacts: _bmad-output/implementation-artifacts/image-optimization
sprint_status: _bmad-output/implementation-artifacts/image-optimization/sprint-status.yaml
kernel: _bmad-output/specs/spec-publish-time-image-optimization/SPEC.md
---

# Image Optimization 1.1 — Current Slots and Source Coverage

This is an executable intent/handoff for the separate image-optimization tracker. It is not a generated bmad-build implementation spec and must enter Build's normal investigation/planning path. Read Story 1.1 in the dedicated epic and the kernel's companions for its complete acceptance contract. The bounded inventory and automated Build review are complete; the dedicated sprint remains in review for human inspection. See `spec-1-1-slot-source-inventory.md` and `p0a-slot-source-inventory.md`; no native measurements are claimed.

## Work Unit

Produce a code-backed matrix of current listing/recommendation/detail image slots, sources/renditions and fitted physical-pixel demand. Separate accepted implementation from reader PoC/future/unimplemented listening/reader surfaces. Record source/profile shortages and missing representative source classes. Hypothetical viewport/DPR arithmetic must remain distinguishable from measured device layouts.

The outcome is a baseline inventory plus explicit gaps, not full P0a qualification. No new screen, source fetching from production, rendering change, codec/profile/cache configuration or authenticated QA is required for this first work unit.

## Execution Context

The required scope paths are:

- `planning_artifacts`: `_bmad-output/planning-artifacts/image-optimization`
- `implementation_artifacts`: `_bmad-output/implementation-artifacts/image-optimization`
- Epic source: `_bmad-output/planning-artifacts/image-optimization/epics.md`
- Sprint source: `_bmad-output/implementation-artifacts/image-optimization/sprint-status.yaml`
- Story key: `1-1-measure-current-image-slots-and-source-coverage`

Pass this handoff file as intent to `bmad-build`. Do not pass the kernel folder plus story ID: no `stories.yaml` was created there. Do not change shared BMad config to select this work.

The installed renderer embeds configured paths in immutable snapshots and has no verified per-invocation path-override CLI. Listing paths here does not override those snapshots. Inspect actual resolved paths before story-key resolution or sprint sync. Unless isolated resolution is demonstrated, keep `story_key` unset, read this epic/handoff explicitly and synchronize only the dedicated tracker by explicit target after normal workflow transitions. Disable numeric-story resolution against the default root/mobile scope: it could select a different Story 1.1. Never silently fall back to another tracker.

## Tasks

1. Read required repository/backend/Flutter guidance and the scoped epic/SPEC companions. Inspect current layout/decode/cache and relevant backend rendition profiles using code evidence.
2. Derive role-specific slot/fit/alignment constraints and reproducible contain/cover calculations. Keep unclamped demand separate from bounded decoding, including tall/wide, fractional-DPR, rotation/window and shortage cases.
3. Map existing redacted source/rendition evidence with hashes/dimensions/provenance; record missing master/page/alpha/EXIF/ICC/animation coverage without creating or certifying substitutes.
4. Write `p0a-slot-source-inventory.md` and a machine-readable input/calculation/coverage artifact in this folder. Review arithmetic, source citations, completeness/privacy and unchanged original evidence. Report device-only observations unavailable unless actually collected.

## Verification and Exit

Check each Story 1.1 criterion, reproduce arithmetic from saved inputs and validate local references. Prefer focused artifact checks over unrelated application suites for this read-only work. Source inspection is not native measurement or proof of blur/OOM/savings. Preserve originals, accepted UI/cache behavior and historical measurements; subsequent stories keep their entry gates even if this inventory completes.

No profile/budget, compliant-source reuse or native-entry decision is made here. No human checkpoint preferences are invented. Follow Build review/acceptance before changing this story to done.
