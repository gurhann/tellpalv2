# Image Optimization P0 — Planning and Execution Readiness

Date: 2026-10-08. Scope: the dedicated P0 epic in `epics.md`, independently of Mobile Story 1.2 and the CMS/backend sprint. Assessed against the current image SPEC and research, not historical completion flags.

**Planning coverage: PASS. Overall execution readiness: CONCERNS. Story 1.1 bounded inventory: PASS/startable.**

## Scope of the Verdict

One evidence-focused epic has six independently reviewable work units. All seven scoped functional requirements, six nonfunctional constraints and three framing/quality requirements trace into story criteria. Dependencies refer only to earlier work or named external decisions/materials. P0 produces evidence independently of P1–P5. There is no new greenfield starter, schema, module edge or UX implementation to invent.

This is not an unconditional CE “all stories ready” declaration or full P0 acceptance. The user authorized separate P0 planning and continuation after unresolved profiles/budgets and staged experiments were explained; tracking records these explicitly gated backlog items without treating them as approved execution. No corpus, profile, checkpoint or native-entry approval is inferred.

## Open Execution Gates

| ID | Affected work | Required evidence/decision | Current consequence |
| --- | --- | --- | --- |
| IO-G1 | 1.2–1.4, 1.6 | Rights-cleared high-resolution masters, real text-bearing pages and alpha/EXIF/ICC/line/gradient coverage; frozen normalization/framing/protocol. Seven small covers are insufficient. | 1.2 stays backlog; no representative/full P0 claim. |
| IO-G2 | 1.3–1.4, 1.6 | Concrete packaged provider/binary scope and actual JPEG/WebP/PNG round-trip with identities and independent decode. | No final codec, universal savings or production dependency change. |
| IO-G3 | 1.5–1.6 | Actual device/tool availability and instrumentation; OQ-6 needs a concrete separately accepted, implemented and verified native entry preserving admission/release refusal. | 1.5 may prepare a proposal after 1.1; 1.6 cannot start from debug/simulator or proposal-only evidence. |
| IO-G4 | 1.4/1.6 handoff; future P1–P5 | OQ-2 budgets; OQ-3 reuse/byte acceptance; OQ-4 normalization/roles; OQ-5 ready/live/retention/protected cache. | Evidence informs proposals; no downstream policy adoption is dispatched. |

These concerns do not require undocumented choices to produce Story 1.1's inventory. Missing device/source cells are an accepted inventory outcome, not a waiver of later qualification.

## Source and Contract Reconciliation

- The scoped SPEC owns this follow-up. Parent PRD FR-15 and NFR-2/3/5/6/7/8 supply context without importing unrelated auth, migration, reader or release work.
- Parent emulator/Simulator-first functional QA is not physical-device performance evidence. The scoped physical-device gate remains; unavailable hardware stays explicit.
- ADR-0010/0017/0018/0019 and canonical architecture retain source/role ownership, module APIs, experiment admission, cache/disposal and source/profile/attempt boundaries. No durable architectural decision changes here.
- Small-JPEG/color findings and the 4 independent/11 single-primary-source research limits remain unchanged. No benchmark/native acceptance, production inventory or current-engine proof is asserted.

## Independent Review and Verification

Read-only reviewers assessed story sizing/gates and tracker safety. The coverage review identified missing worker rejection specimens; Story 1.2 now freezes truncated/MIME/byte/pixel rejection fixtures and Story 1.3 requires bounded actionable rejection evidence. Client corruption checks cannot substitute.

Sprint parsing must find exactly one epic and six numbered stories. Generic document/list/matrix headings may be flagged as non-dispatch headings; verify intended counts and exact keys rather than inventing omitted stories. Only the first story has an execution handoff file; others retain backlog and entry gates. No implementation or runtime acceptance is claimed by generation.

## Isolation and Next Work

Plan: `_bmad-output/planning-artifacts/image-optimization/epics.md`.
Status: `_bmad-output/implementation-artifacts/image-optimization/sprint-status.yaml`.
First handoff: `_bmad-output/implementation-artifacts/image-optimization/1-1-measure-current-image-slots-and-source-coverage.md`.

Use the handoff as explicit intent with `bmad-build`. Its listed paths are required scope, not a verified renderer override mechanism: installed snapshots embed shared configuration. Inspect resolved paths; unless isolated runtime resolution is demonstrated, leave `story_key` unset, load this scoped epic explicitly and synchronize only the dedicated tracker by explicit target. Root/mobile trackers have overlapping numeric IDs; project metadata does not namespace keys. Never regenerate them using this epic or change shared config to select this scope. No app, deployment, backfill or full P0 execution is authorized by this report.
