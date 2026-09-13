---
title: 'Stage meditation imports before localized body text exists'
type: 'feature'
created: '2026-09-13'
status: 'done'
review_loop_iteration: 0
baseline_commit: '03176e8b97e0cc40604e902c06f75afca945043e'
context:
  - C:/github/tellpalv2/be/docs/project-memory.md
  - C:/github/tellpalv2/be/docs/admin-api-rules.md
  - C:/github/tellpalv2/architecture.md
  - C:/github/tellpalv2/be/docs/adr/ADR-0010-content-cover-ownership.md
  - C:/github/tellpalv2/_bmad-output/implementation-artifacts/spec-meditation-firebase-import.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The meditation source has valid translated titles, descriptions, covers, and audio, but localized body texts will be supplied later. The current domain and importer reject the entire import, preventing safe staging of the available editorial and media data.

**Approach:** Permit an explicitly approved staged import that creates `MEDITATION` localizations as `DRAFT` with `processingStatus=PENDING` and a null body. Preserve publication and mobile-readiness gates so a body must exist before a localization can be published or become visible. Keep the existing localization update endpoint as the completion path when body text arrives.

## Boundaries & Constraints

**Always:** Require an explicit `--allow-missing-body` import option and `--no-publish`; never publish incomplete localizations. Allow missing body only for `MEDITATION` in `DRAFT`; continue requiring audio and validating all remote covers/ZIP/MP3s. Show every missing group/language in the preview and run report. Keep processing status API/worker-owned and `PENDING` on staged creation. Reject `PUBLISHED` meditation localizations without non-blank body text, including direct Admin API upsert and publish flows. Preserve source fingerprint, remote conflict preflight, read-back verification, and manual recovery semantics.

**Ask First:** None; the human has explicitly confirmed that meditation text will be added later and the current import should stage available assets only.

**Never:** Do not invent or copy placeholder body text from summaries, modify source CSV/assets, auto-publish, mark processing `COMPLETED`, resume/update/delete partial imports, add a migration when existing nullable storage is sufficient, or weaken STORY/LULLABY validation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|-----------------------------|----------------|
| STAGED_IMPORT | Valid remote assets, missing body, `--allow-missing-body --no-publish` | One content per group; DRAFT/PENDING localizations with null body; no publish calls | N/A |
| MISSING_FLAG | Missing body without explicit allow option | Existing safe behavior: no API writes | Actionable preflight error listing group/language |
| PUBLISH_GUARD | Missing body with publish requested or direct PUBLISHED upsert | Request/import is rejected before publication | Explain body is required for publication |
| BODY_COMPLETION | Existing DRAFT localization receives body through PUT update | Body is stored; status remains caller-controlled and processing remains caller/worker-owned | Existing validation/error contract |
| REMOTE_CONFLICT | Any generated external key already exists | No writes, regardless of body option | Remote preflight conflict |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- type-specific localization validation; allow staged DRAFT body omission while guarding PUBLISHED MEDITATION writes.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java` -- keep bodyless MEDITATION localizations out of mobile delivery.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java` -- publication invariant; reject a bodyless MEDITATION before status transition.
- `be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java` -- apply the same body invariant to archive transitions.
- `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java` and `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java` -- expose bodyless MEDITATION rows as actionable registry blockers.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java` -- existing POST/PUT localization contract used for staging and later body completion; no endpoint shape change expected.
- `be/docs/admin-api-rules.md` -- update verified MEDITATION required/ publication rules and CMS implications.
- `.codex/skills/import-tellpal-story/scripts/meditation_import_workflow.py` -- opt-in missing-body gate, preview wording, staged execution, and no-publish enforcement.
- `.codex/skills/import-tellpal-story/scripts/import_meditations.py` -- interactive `--allow-missing-body`/`--no-publish` arguments and confirmation flow.
- `.codex/skills/import-tellpal-story/scripts/inspect_meditations.py` -- matching read-only preview option.
- `.codex/skills/import-tellpal-story/scripts/tests/test_meditation_import.py` -- staged plan/workflow, option, preview, and rejection coverage.
- `be/src/test/java/com/tellpal/v2/content/domain/ContentTest.java`, `be/src/test/java/com/tellpal/v2/content/ContentPublicationServiceIntegrationTest.java`, `be/src/test/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java`, and `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java` -- domain, publication, registry, and Admin API guards.

## Tasks & Acceptance

**Execution:**
- [x] `Content.java` and `ContentPublicationPolicy.java` -- implement staged-DRAFT and published-body invariants -- keep incomplete meditations non-public while enabling import.
- [x] `meditation_import_workflow.py`, `import_meditations.py`, and `inspect_meditations.py` -- add explicit staged-import option and force no-publication -- avoid accidental incomplete publication.
- [x] `be/docs/admin-api-rules.md` -- document the revised verified rule -- keep CMS/editor behavior aligned.
- [x] `test_meditation_import.py`, `ContentTest.java`, and `ContentPublicationServiceIntegrationTest.java` -- cover staged creation, later body update, and all publication guards -- prove the matrix.

**Acceptance Criteria:**
- Given valid covers and ZIP MP3s with no body sources, when the operator runs both flags and confirms `import`, then all planned groups are created with null body, `DRAFT`, `PENDING`, and zero publication requests.
- Given the same source without `--allow-missing-body`, when preflight runs, then it stops before authentication or API mutation and lists every missing localization.
- Given a bodyless MEDITATION localization, when an Admin API request attempts `status=PUBLISHED` or the publish endpoint is called, then the request fails and the localization does not become published.
- Given a staged DRAFT localization, when the existing PUT localization endpoint receives non-blank body text, then the body is persisted without weakening audio or processing validation.

## Design Notes

The staging option is explicit because the normal importer remains strict. `--no-publish` is mandatory with missing bodies even though the CSV plan may otherwise request publication. A null body is an honest incomplete editorial state; a fabricated summary or placeholder would make readiness and translation quality indistinguishable from completed content.

## Verification

**Commands:**
- `python -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p "test_meditation_*.py"` -- expected: staged and strict importer tests pass.
- `cd be; ./mvnw -DskipTests=false -Dtest=ContentTest,ContentPublicationServiceIntegrationTest,JdbcContentRegistryReadRepositoryIntegrationTest,ContentAdminIntegrationTest test` -- expected: domain, publication, registry, and Admin API guards pass.
- `cd be; ./mvnw verify` -- expected: full backend verification passes; unrelated baseline failures are reported separately.
- `python -B .codex/skills/import-tellpal-story/scripts/inspect_meditations.py cms/yuklenecek_hikayeler/meditations/meditations.csv --service-account-json C:/dev/keys/tellpal-v2-firebase-storage-pk.json --allow-missing-body --no-publish` -- expected: five-group preview with explicit staged status and no writes.

## Suggested Review Order

**Staged importer contract**

- Plan construction makes incomplete body state explicit and opt-in.
  [`meditation_manifest.py:139`](../../.codex/skills/import-tellpal-story/scripts/meditation_manifest.py#L139)

- Remote preflight and execution preserve strict mode while staging DRAFT/PENDING localizations.
  [`meditation_import_workflow.py:71`](../../.codex/skills/import-tellpal-story/scripts/meditation_import_workflow.py#L71)

- CLI wiring requires both safety flags before authentication and any writes.
  [`import_meditations.py:28`](../../.codex/skills/import-tellpal-story/scripts/import_meditations.py#L28)

**Content lifecycle and delivery guards**

- Domain validation limits bodyless meditations to DRAFT/PENDING and preserves audio requirements.
  [`Content.java:587`](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L587)

- Publication and archive transitions reject incomplete meditation bodies before state mutation.
  [`ContentPublicationPolicy.java:21`](../../be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java#L21)

- Mobile delivery requires non-blank meditation body text even when processing is complete.
  [`ContentLocalization.java:140`](../../be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java#L140)

**Admin readiness projection**

- Database registry marks bodyless meditations actionable instead of ready to publish.
  [`JdbcContentRegistryReadRepository.java:29`](../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java#L29)

- CMS blockers expose the missing body reason for operator recovery.
  [`AdminContentQueryService.java:178`](../../be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java#L178)

**Verification and operational guidance**

- Integration tests cover body completion, archive/publication guards, and preserved processing metadata.
  [`ContentPublicationServiceIntegrationTest.java:175`](../../be/src/test/java/com/tellpal/v2/content/ContentPublicationServiceIntegrationTest.java#L175)

- Admin API registry coverage verifies completed processing cannot hide a missing body.
  [`ContentAdminIntegrationTest.java:113`](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java#L113)

- Python tests prove staged conflict protection, strict pre-auth gating, and flag forwarding.
  [`test_meditation_import.py:233`](../../.codex/skills/import-tellpal-story/scripts/tests/test_meditation_import.py#L233)

- Firebase command guidance documents the exact two-flag staged operation and later body completion.
  [`meditation-import.md:21`](../../.codex/skills/import-tellpal-story/references/meditation-import.md#L21)
