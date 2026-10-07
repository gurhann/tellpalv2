# Story 1.2 prerequisite check

Date: 2026-10-07. Baseline: `8380313c4ed32a471e68b83ad780827a92dffcc0` on local `main`.
Scope: preparation for [Story 1.2](../../planning-artifacts/epics-mobile.md#story-12-open-the-selected-story-detail-from-library), using the [mobile tracker](sprint-status.yaml). This is a repository/code/reference inspection, not executed integration or visual acceptance. No backend, CMS, or Flutter application implementation has changed. Story 1.2 remains `backlog`; the build specification is a draft.

## Result

Story 1.1's [human acceptance](library-1-1-final-acceptance.md) is present. Its home-list EXT-CATALOG closure does not establish detail readiness. Existing identity fields and some reference images are reusable. The detail payload/access/recommendation contract and matched-state evidence are not yet verified, so mobile integration readiness is not established.

| Prerequisite | Finding | Required next evidence |
| --- | --- | --- |
| Accepted 1.1 | Passed: final acceptance and tracker both say done | Preserve its qualified platform and release gaps |
| Content/experience/language identity | Present in Library previews; detail lacks explicit experience identity | Agreed detail request/response identity and exact-language fixtures |
| Detail metadata | Existing title, description, age, duration and assets; no public credits list | Metadata-only payload, contributor roles, real cover provenance |
| Access | Home audience is server-owned; existing public detail accepts caller `freeKey` and has `free`, without the home audience/lock projection | Controlled admission/access mapping; metadata must grant no protected-media rights |
| Errors | Existing 400/404 response markers; no captured Story 1.2 fixtures | Real invalid-language/content/unavailable responses plus client network/timeout/retry cases |
| Related content | Human approved same-category, same-language eligible items in CMS order; no current verified detail recommendation contract | Formalized filtering/identity contract, valid and empty real-backend fixtures |
| Reference surface | Free phone, locked phone and free tablet images exist; human approved Library-style loading/error/unavailable states | Capture the remaining matched states and label new expected states separately from legacy evidence |
| Navigation | Library has retained scroll keys; router has only LibraryRoute | New route identity, same-action deduplication, stale-result protection and observed back/scroll evidence |
| EXT-CATALOG detail | Open | Versioned detail contract, dataset/build identities and redacted real integration evidence |

## Backend and client evidence

- [ContentPreview](../../../be/src/main/java/com/tellpal/v2/content/api/ContentPreview.java) and [LibraryPreview](../../../app/mobile/lib/features/library/domain/entity/library_catalog.dart) already carry content ID, experience and explicit content language. An ordinary selected-language row and a deliberately different-language row must retain their respective content languages through detail. Do not revive the superseded automatic English fallback.
- [ContentMobileController](../../../be/src/main/java/com/tellpal/v2/content/web/mobile/ContentMobileController.java) currently exposes `GET /api/contents/{contentId}?lang=...&freeKey=...`. [PublicContentDetails](../../../be/src/main/java/com/tellpal/v2/content/api/PublicContentDetails.java) is a delivery view, not an agreed metadata-only Story 1.2 contract. Do not assume its asset payload establishes protected-media authorization.
- [PublicContentQueryService](../../../be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java) uses active content and exact published/completed localization. [PublicContentQueryMapper](../../../be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryMapper.java) does not expose contributor records or recommendations.
- [ContentMobileExceptionHandler](../../../be/src/main/java/com/tellpal/v2/content/web/mobile/ContentMobileExceptionHandler.java) includes `content_not_found` and `invalid_request`. Multiple unavailable reasons may legitimately share an unavailable response; Story 1.2 requires distinct loading/network/unavailable client outcomes, not necessarily disclosure of each internal reason. Confirm this mapping with fixtures rather than inventing extra error codes.
- [LibraryHomeTestQueryService](../../../be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java) supplies the established local/test UID and server audience pattern. [user/package-info.java](../../../be/src/main/java/com/tellpal/v2/user/package-info.java) permits asset/category public APIs but not a new content dependency. Any changed module edge requires explicit design agreement; this check approves none.
- [router.dart](../../../app/mobile/lib/app/router.dart) has only LibraryRoute. [library_view.dart](../../../app/mobile/lib/features/library/presentation/view/library_view.dart) retains `library-scroll` and row PageStorage keys. Enable only the consuming story's card/detail navigation and preserve unrelated disabled destinations and accepted cache/transport behavior.

No representative, redacted Story 1.2 real-backend success/error/related fixtures were found. Existing home-list fixtures must not be relabeled as detail proof. Source/test inspection is not a test run.

## Reference evidence and limits

The three images below were inspected during this check. [SCREENS.md](../../../app/old/docs/design-system/SCREENS.md#14-hikâye-bilgisi--storyinfoid--gk) and [TABLET.md](../../../app/old/docs/design-system/TABLET.md) describe the legacy detail surface.

| Image | Observed support | Limit |
| --- | --- | --- |
| [emulator-haberci-info.png](../../../app/old/docs/design-system/evidence/emulator-haberci-info.png) | TR free phone, large hero, back, age/duration, CTA, narration toggle, summary, three credit roles, similar-story heading | No visible recommendation cards or return interaction |
| [emulator-premium-story.png](../../../app/old/docs/design-system/evidence/emulator-premium-story.png) | Locked phone icon/CTA and detail metadata | Hero is blank in this capture; TR controls and EN summary do not establish current exact-language parity or a successful locked-cover state |
| [tablet-emulated-story-info.png](../../../app/old/docs/design-system/evidence/tablet-emulated-story-info.png) | EN free portrait tablet, hero, centered fixed-width CTA, narration, summary/credits | Emulated wide display; no locked tablet, real tablet/iPad or recommendation interaction proof |

Missing matched cases include loading, network failure/retry, unavailable/withdrawn language/content, absent and valid recommendations with return, locked tablet, TR/EN/PT at normal/enlarged scales, and return to a previously scrolled Library. The [Story 1.1 reference cases](library-1-1-reference-cases.md) only verify Library and cannot certify detail. Capture the legacy state when available; otherwise agree a clearly identified new state treatment before visual acceptance. Do not claim unsupported old behavior from code or screenshots.

Legacy [api-contracts.md](../../../app/old/docs/api-contracts.md) documents `/story/similarContent` but no sufficient selection/ranking rule. Legacy implementation source under `app/old/lib` is absent in this checkout. On 2026-10-07 the human agreed the new selection rule: same-category, same-content-language eligible stories in CMS order, excluding the current story and retaining server publication/access filtering. This is an agreed new contract direction, not verified legacy ranking parity or real-backend evidence.

## Build bootstrap repair

An earlier Story 1.1 review copied an existing reference into the extra snapshot path `review-prompts/references/deletion-check.md`; the [review record](library-1-1-review-triage-3.md) describes that intervention. The declared reference already existed at `references/deletion-check.md` with byte-identical content.

This run verified every declared output against the snapshot manifest, backed up the extra copy to `/private/tmp/tellpal-bmad-build-repair-2026-10-07/deletion-check.md`, and removed only the extra path. The prescribed `uv run --no-cache .../render_skill.py` command then exited 0 and returned the rendered workflow path. Generated snapshots must be consumed through their declared paths; this repair did not alter skill instructions or bypass verification.

The unrelated untracked generated `build/generated-spring-modulith/javadoc.json` was also preserved byte-identically at `/private/tmp/tellpal-bmad-build-repair-2026-10-07/generated-spring-modulith-javadoc.json` before removal from the working tree. The baseline working tree was then clean. Both backups remain available. The mobile Epic 1 context was regenerated because newer planning evidence invalidated its cache. Default CMS/backend story paths and trackers were not used.

## Next gate

Related-content selection and Library-style failure states were agreed on 2026-10-07. The [proposed contract](library-detail-contract.md) and [reference-case plan](library-1-2-reference-cases.md) make the next work reviewable; they do not establish executed evidence. Continue with [BMad Checkpoint 1](spec-1-2-open-the-selected-story-detail-from-library.md). Notify the human before long implementation so they can adjust the app's model setting. Produce and verify real contract/reference evidence before mobile integration acceptance; do not advance the tracker on this inspection alone.
