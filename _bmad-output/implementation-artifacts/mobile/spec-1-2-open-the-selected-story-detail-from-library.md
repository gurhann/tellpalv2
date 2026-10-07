---
title: 'Story 1.2 — Open selected story detail'
type: 'feature'
created: '2026-10-07'
status: 'in-progress'
review_loop_iteration: 0
baseline_commit: '8380313c4ed32a471e68b83ad780827a92dffcc0'
tracker: '_bmad-output/implementation-artifacts/mobile/sprint-status.yaml'
story_key: '1-2-open-the-selected-story-detail-from-library'
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - '_bmad-output/implementation-artifacts/mobile/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Accepted Library cards cannot open detail. Existing delivery APIs lack the agreed credits, recommendation and audience-aware metadata contract.

**Approach:** Verify the backend companion and reference states, then enable detail navigation under controlled development access.

## Boundaries & Constraints

**Always:** Preserve accepted Story 1.1. Follow ADR-0018, public module APIs, server admission/access, bounded transport/artwork waits and attempt guards. Carry content ID, reading experience and content language; keep UI language separate. Support phone/tablet and TR/EN/PT at scales 1.0/1.3. Verify prerequisites before acceptance.

**Ask First:** Changes to the approved recommendation/failure-state policies, new module edges or reference redesign. Notify the human before starting the long implementation phase and leave time to adjust the model setting.

**Never:** Automatic language substitution, client entitlement decisions, protected playback payloads, reader/paywall/auth/migration implementation, production mutation, credentials in UI/evidence, or snapshot file additions.

## I/O & Edge-Case Matrix

| Input/state | Expected behavior | Recovery |
| --- | --- | --- |
| Ordinary or mapped-language card | Matching identity, hero/title/age/duration/summary/credits | Never substitute another language |
| Detail opens | Reference layout and access-aware action | No automatic reading/narration |
| Free/locked metadata | Server access presentation | No protected-media authority; deferred CTA cannot launch reader/paywall |
| Loading/network timeout | Distinct loading or recoverable error | Current-context bounded retry |
| Withdrawn content/localization | Agreed unavailable state | No stale detail or fallback |
| Repeated taps/reordered callbacks | One route per repeated action; current identity only | Reject obsolete completions |
| Return to Library | Previous stack and vertical/horizontal scroll | Retain Library ownership |
| Eligible recommendation selected/back | Exact-language reading detail; originating detail restored | Same identity/access/stale guards |
| No eligible recommendations | No invalid card/target | Defined absent-area treatment |

</frozen-after-approval>

## Code Map

Backend paths below are relative to `be/src/main/java/com/tellpal/v2/`.

- `content/web/mobile/ContentMobileController.java:48` — existing delivery lacks detail metadata.
- `content/application/query/PublicContentQueryService.java:106` — exact-language eligibility.
- `user/application/LibraryHomeTestQueryService.java:18` — admission and existing edges.
- `app/mobile/lib/app/router.dart:6` — baseline LibraryRoute and PoC.
- `app/mobile/lib/features/library/presentation/view/library_view.dart:21` — baseline scroll and disabled cards.
- [Prerequisite check](story-1-2-prerequisite-check.md) — verified sources, images, gaps and acceptance limits.

## Tasks & Acceptance

**Execution:**
- [ ] `library-detail-contract.md` — implement and verify the proposed metadata contract and its exact [file map](library-detail-contract.md#implementation-map); keep existing module edges.
- [ ] `library-1-2-reference-cases.md` — capture its cases; distinguish legacy evidence from approved new failure-state treatments.
- [x] `be/src/main/java/com/tellpal/v2/content/api/StoryDetailMetadataQueryApi.java`, `asset/api/StoryDetailHeroQueryApi.java`, `category/api/LibraryStoryDetailQueryApi.java` — add module contracts/implementations and category composition.
- [ ] `be/src/test/java/com/tellpal/v2/category/LibraryStoryDetailIntegrationTest.java` and `library-1-2-http-evidence.json` — execute the companion's test map and real fixtures before mobile integration.
- [ ] `app/mobile/lib/features/story_detail/` — add domain/data/state/screen/view layers, generated models, injected dependencies and every matrix outcome.
- [ ] `app/mobile/lib/app/{router,injection}.dart` and `app/mobile/lib/features/library/presentation/widgets/library_card.dart` — connect routes/card taps; retain scroll, shared services and unrelated controls.
- [ ] `app/mobile/test/story_detail/` — test every matrix row, including repeated actions, stale results and nested return.
- [ ] `library-1-2-evidence.md` — record backend/native proof, source/build identity, accessibility and detail EXT-CATALOG closure; retain release gaps.

**Acceptance Criteria:**
- Given the agreed real dataset, when card and recommendation journeys run, then every [Story 1.2 criterion](../../planning-artifacts/epics-mobile.md#story-12-open-the-selected-story-detail-from-library) and matrix row has current, traceable passing evidence.
- Given the accepted PoC and Library, when detail is added, then their unaffected behavior and tests remain valid.

## Spec Change Log

## Design Notes

On 2026-10-07 the human approved Checkpoint 1 and companion policies after the implementation notice. Use the human-selected model; approval is not executed evidence.

## Verification

- Before mobile integration, from `be/`: `./mvnw verify` and real-fixture checks — contract, PostgreSQL and module evidence pass.
- From `app/mobile/`: `dart run build_runner build --delete-conflicting-outputs`, `flutter analyze`, `flutter test` — generation, analysis and meaningful regressions pass.
- Observe matched phone/tablet detail states, semantics, 48dp controls, artwork contrast, scales and back/scroll behavior; record actual environment and unmet platform limits.

Progress: [evidence](library-1-2-evidence.md) records implemented backend/Flutter layers and passing automated checks. Runtime cards, real authenticated HTTP/native/reference/accessibility acceptance and detail EXT-CATALOG remain pending; do not advance to Step 4.
